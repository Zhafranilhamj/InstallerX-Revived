// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.core.resParser

/**
 * Framework resource ID assigned to android:versionCodeMajor in Android 9 (API 28).
 * Older runtimes do not define the attribute, but APK binary XML can still contain its ID.
 * See AOSP frameworks/base/core/res/res/values/public.xml (android-9.0.0_r1).
 */
internal const val VERSION_CODE_MAJOR_ATTR_ID = 0x01010576
