// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.data.engine.executor

import com.rosan.installer.domain.engine.exception.ModuleInstallException
import com.rosan.installer.domain.engine.model.packageinfo.AppEntity
import com.rosan.installer.domain.engine.model.source.DataEntity
import com.rosan.installer.domain.settings.model.preferences.RootMode
import java.io.File
import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.StandardOpenOption
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ModuleInstallerUtilsTest {
    private lateinit var tempDirectory: File

    @BeforeTest
    fun setUp() {
        tempDirectory = Files.createTempDirectory("module-installer-utils-test").toFile()
    }

    @AfterTest
    fun tearDown() {
        tempDirectory.deleteRecursively()
    }

    @Test
    fun `uses the materialized backing path instead of the original source identity`() {
        val localFile = File(tempDirectory, "module.zip").apply { writeText("module") }
        val data = DataEntity.FileEntity(localFile.absolutePath).apply {
            source = DataEntity.FileEntity("/smb%3A%2F%2Fserver%2Fmodule.zip")
        }

        val path = ModuleInstallerUtils.getModulePathOrThrow(module(data))

        assertEquals(localFile.absolutePath, path)
    }

    @Test
    fun `rejects a descriptor display path that was not materialized`() {
        val backingFile = File(tempDirectory, "backing.zip").apply { writeText("module") }
        val displayPath = "/smb%3A%2F%2Fserver%2Fmodule.zip"
        val data = DataEntity.FileDescriptorEntity(
            path = displayPath,
            startOffset = 0L,
            length = backingFile.length(),
            channelFactory = {
                FileChannel.open(backingFile.toPath(), StandardOpenOption.READ)
            },
            descriptorFactory = { error("The path validation test must not request an fd") },
        )

        assertFailsWith<ModuleInstallException> {
            ModuleInstallerUtils.getModulePathOrThrow(module(data))
        }
    }

    @Test
    fun `single quotes every shell metacharacter in the module path`() {
        val hostilePaths = listOf(
            "/data/local/tmp/it's.zip",
            "/data/local/tmp/say \"hi\".zip",
            "/data/local/tmp/$(reboot).zip",
            "/data/local/tmp/`id`.zip",
            "/data/local/tmp/*.zip",
            "/data/local/tmp/with space.zip",
            "/data/local/tmp/new\nline.zip",
        )

        for (path in hostilePaths) {
            val command = ModuleInstallerUtils.buildShellCommandString(RootMode.Magisk, path)

            // The whole path must live inside one single-quoted segment, so the shell never
            // expands it. Expected value computed independently of the production helper.
            val quoted = "'" + path.replace("'", "'\\''") + "'"
            assertTrue(command.endsWith(quoted), "Unquoted path in: $command")
            assertFalse(command.contains("\"$path\""), "Double-quoted path in: $command")
        }
    }

    @Test
    fun `a quote in the path cannot terminate the quoted segment`() {
        val command = ModuleInstallerUtils.buildShellCommandString(
            RootMode.KernelSU,
            "/data/local/tmp/x'; reboot; echo '.zip",
        )

        assertEquals(
            "ksud module install '/data/local/tmp/x'\\''; reboot; echo '\\''.zip'",
            command,
        )
    }

    @Test
    fun `each root implementation keeps its own binary`() {
        assertEquals(
            "magisk --install-module '/tmp/m.zip'",
            ModuleInstallerUtils.buildShellCommandString(RootMode.Magisk, "/tmp/m.zip"),
        )
        assertEquals(
            "apd module install '/tmp/m.zip'",
            ModuleInstallerUtils.buildShellCommandString(RootMode.APatch, "/tmp/m.zip"),
        )
        assertFailsWith<IllegalStateException> {
            ModuleInstallerUtils.buildShellCommandString(RootMode.None, "/tmp/m.zip")
        }
    }

    private fun module(data: DataEntity): AppEntity.ModuleEntity = AppEntity.ModuleEntity(
        id = "test",
        name = "Test",
        version = "1",
        versionCode = 1,
        author = "Tester",
        description = "",
        data = data,
    )
}
