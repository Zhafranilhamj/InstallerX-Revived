// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.data.engine.parser

import com.rosan.installer.core.device.model.Architecture
import com.rosan.installer.domain.engine.model.source.FilterType
import com.rosan.installer.domain.engine.model.source.SplitType
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Golden cases taken from real bundles (Play split APKs, APKS/APKM/XAPK repacks) so the naming
 * conventions cannot silently regress.
 */
class SplitNameUtilTest {
    @Test
    fun `config splits are classified by qualifier`() {
        assertSplit("split_config.arm64_v8a", SplitType.ARCHITECTURE, FilterType.ABI, Architecture.ARM64.arch)
        assertSplit("split_config.armeabi_v7a", SplitType.ARCHITECTURE, FilterType.ABI, Architecture.ARM.arch)
        assertSplit("split_config.x86_64", SplitType.ARCHITECTURE, FilterType.ABI, Architecture.X86_64.arch)
        assertSplit("split_config.xxxhdpi", SplitType.DENSITY, FilterType.DENSITY, "xxxhdpi")
        assertSplit("split_config.xhdpi", SplitType.DENSITY, FilterType.DENSITY, "xhdpi")
        assertSplit("split_config.en", SplitType.LANGUAGE, FilterType.LANGUAGE, "en")
        assertSplit("split_config.zh", SplitType.LANGUAGE, FilterType.LANGUAGE, "zh")
    }

    @Test
    fun `base prefixed splits stay architecture or density splits`() {
        assertSplit("base-arm64_v8a", SplitType.ARCHITECTURE, FilterType.ABI, Architecture.ARM64.arch)
        assertSplit("base-xhdpi", SplitType.DENSITY, FilterType.DENSITY, "xhdpi")
    }

    @Test
    fun `feature splits with an embedded config keep the feature type`() {
        assertSplit("split_feature_map.config.en", SplitType.FEATURE, FilterType.LANGUAGE, "en")
        assertSplit("split_feature_map.config.xxhdpi", SplitType.FEATURE, FilterType.DENSITY, "xxhdpi")
        assertSplit(
            "split_feature_map.config.arm64_v8a",
            SplitType.FEATURE,
            FilterType.ABI,
            Architecture.ARM64.arch,
        )
    }

    @Test
    fun `plain feature splits are not mistaken for config splits`() {
        assertSplit("split_feature_map", SplitType.FEATURE, FilterType.NONE, null)
        assertSplit("split_feature_map_debug", SplitType.FEATURE, FilterType.NONE, null)
        // A qualifier that is too long to be a language code is not a language split.
        assertSplit("split_feature_somethinglong", SplitType.FEATURE, FilterType.NONE, null)
    }

    @Test
    fun `numeric densities without a resource alias are still densities`() {
        assertSplit("split_config.420dpi", SplitType.DENSITY, FilterType.DENSITY, "420dpi")
    }

    private fun assertSplit(
        splitName: String,
        expectedType: SplitType,
        expectedFilter: FilterType,
        expectedConfigValue: String?,
    ) {
        val metadata = splitName.parseSplitMetadata()
        assertEquals(expectedType, metadata.type, "type of $splitName")
        assertEquals(expectedFilter, metadata.filterType, "filterType of $splitName")
        assertEquals(expectedConfigValue, metadata.configValue, "configValue of $splitName")
    }
}
