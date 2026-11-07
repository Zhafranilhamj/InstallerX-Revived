// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.data.settings.local.room

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Guards the exported Room schema history.
 *
 * A real migration test needs `MigrationTestHelper` on a device (`androidTest`), which is not part
 * of this project's CI. This JVM test covers the failure mode that actually reaches users: a schema
 * version bump without a matching exported JSON (or a missing historical schema), which makes an
 * upgrade path unverifiable and silently unrecoverable.
 */
class RoomSchemaExportTest {
    @Test
    fun `every schema version between the oldest supported and the current version is exported`() {
        val directory = schemaDirectory()
        val exported = directory.listFiles()
            .orEmpty()
            .mapNotNull { it.name.removeSuffix(".json").toIntOrNull()?.takeIf { _ -> it.name.endsWith(".json") } }
            .toSet()

        assertTrue(exported.isNotEmpty(), "No Room schema JSON files found in ${directory.absolutePath}")
        for (version in OLDEST_SCHEMA_VERSION..INSTALLER_ROOM_SCHEMA_VERSION) {
            assertTrue(
                version in exported,
                "Room schema $version is missing from ${directory.absolutePath}; export it with the room plugin",
            )
        }
    }

    @Test
    fun `the newest exported schema matches the declared database version`() {
        val directory = schemaDirectory()
        val newest = directory.listFiles()
            .orEmpty()
            .mapNotNull { file -> file.name.removeSuffix(".json").toIntOrNull()?.let { it to file } }
            .maxByOrNull { it.first }

        assertTrue(newest != null, "No exported Room schema found")
        assertEquals(INSTALLER_ROOM_SCHEMA_VERSION, newest.first)
        val versionField = Regex("\"version\"\\s*:\\s*(\\d+)").find(newest.second.readText())
        assertEquals(
            INSTALLER_ROOM_SCHEMA_VERSION.toString(),
            versionField?.groupValues?.get(1),
            "The exported schema's version field disagrees with @Database(version = ...)",
        )
    }

    /** Gradle runs unit tests from the module directory, but tolerate a repository-root cwd too. */
    private fun schemaDirectory(): File {
        val candidates = listOf(File("schemas"), File("app/schemas"))
        val root = candidates.firstOrNull { it.isDirectory }
            ?: error("Room schema directory not found from ${File(".").absolutePath}")
        return root.listFiles()?.firstOrNull { it.isDirectory } ?: root
    }

    private companion object {
        /** Oldest schema the app still migrates from (auto-migrations start at 3). */
        const val OLDEST_SCHEMA_VERSION = 3
    }
}
