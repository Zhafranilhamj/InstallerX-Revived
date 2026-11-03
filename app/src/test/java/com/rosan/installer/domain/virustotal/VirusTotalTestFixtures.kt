// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.domain.virustotal

import com.rosan.installer.domain.settings.model.preferences.AppPreferences
import com.rosan.installer.domain.settings.model.preferences.VirusTotalScanMode
import com.rosan.installer.domain.settings.repository.AppSettingsRepository
import java.lang.reflect.Proxy
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

@Suppress("UNCHECKED_CAST")
internal fun virusTotalSettings(preferences: Flow<AppPreferences>): AppSettingsRepository = Proxy.newProxyInstance(AppSettingsRepository::class.java.classLoader, arrayOf(AppSettingsRepository::class.java)) { _, method, _ ->
    check(method.name == "getPreferencesFlow") { "Unexpected settings call: ${method.name}" }
    preferences
} as AppSettingsRepository

internal fun virusTotalSettings(preferences: AppPreferences): AppSettingsRepository = virusTotalSettings(flowOf(preferences))

internal fun testAppPreferences(
    virusTotalScanMode: VirusTotalScanMode = VirusTotalScanMode.OFF,
    virusTotalApiKey: String = "",
    allowInternetAccess: Boolean = false,
) = AppPreferences(
    authorizer = com.rosan.installer.domain.settings.model.config.Authorizer.None,
    alwaysUseRootInSystem = false,
    customizeAuthorizer = "",
    hideIdenticalInstallComparisons = false,
    showDialogInstallExtendedMenu = false,
    expandDialogTemporarySettingsByDefault = false,
    showSmartSuggestion = false,
    disableNotificationForDialogInstall = false,
    showDialogWhenPressingNotification = false,
    closeSessionCountDown = 0,
    notificationSuccessAutoClearSeconds = 0,
    versionCompareInSingleLine = false,
    sdkCompareInMultiLine = false,
    showOPPOSpecial = false,
    checkAppSignature = false,
    checkSourceStamp = true,
    checkSplitPackageSignatures = false,
    showSignatureInfoOnMatch = false,
    showSignatureDetails = false,
    installerRequireBiometricAuth = com.rosan.installer.domain.settings.model.config.BiometricAuthMode.entries.first(),
    uninstallerRequireBiometricAuth = false,
    showLiveActivity = false,
    useMiIsland = false,
    useMiIslandBypassRestriction = false,
    useMiIslandOuterGlow = false,
    useMiIslandBlockingIntervalMs = 0,
    autoSilentInstall = false,
    longClickBackgroundInstall = false,
    tryMultipleAuthorizersOnInstall = false,
    smartAuthorizerCandidates = emptyList(),
    showMiuixUI = false,
    preferSystemIcon = false,
    showLauncherIcon = false,
    userSetLSPosedActive = false,
    detectXposedModule = false,
    quickOpenLSPosed = false,
    managedInstallerPackages = emptyList(),
    managedBlacklistPackages = emptyList(),
    managedSharedUserIdBlacklist = emptyList(),
    managedSharedUserIdExemptedPackages = emptyList(),
    uninstallFlags = 0,
    networkSourceMode = com.rosan.installer.domain.settings.model.config.NetworkSourceMode.entries.first(),
    networkSourceModeWarningAcknowledged = false,
    allowInternetAccess = allowInternetAccess,
    githubUpdateChannel = com.rosan.installer.domain.settings.model.preferences.GithubUpdateChannel.entries.first(),
    customGithubProxyUrl = "",
    virusTotalApiKey = virusTotalApiKey,
    virusTotalScanMode = virusTotalScanMode,
    labRootEnableModuleFlash = false,
    labRootShowModuleArt = false,
    labRootMode = com.rosan.installer.domain.settings.model.preferences.RootMode.entries.first(),
    labHttpProfile = com.rosan.installer.domain.settings.model.preferences.HttpProfile.entries.first(),
    labHttpSaveFile = false,
    labSetInstallRequester = false,
    labTapIconToShare = false,
    labShowFilePath = false,
    labShowInstallInitiator = false,
    labInstallWithoutUserAction = false,
    labRespectPlatformInstallPolicy = false,
    labEnableAbiOverride = false,
    labTryDifferentArchitecture = false,
    enableFileLogging = false,
    themeMode = com.rosan.installer.domain.settings.model.preferences.theme.ThemeMode.entries.first(),
    paletteStyle = com.rosan.installer.domain.settings.model.preferences.theme.PaletteStyle.entries.first(),
    colorSpec = com.rosan.installer.domain.settings.model.preferences.theme.ThemeColorSpec.entries.first(),
    useDynamicColor = false,
    useMiuixMonet = false,
    useAppleFloatingBar = false,
    seedColorInt = 0,
    wallpaperSeedColorInt = 0,
    useDynColorFollowPkgIcon = false,
    useDynColorFollowPkgIconForLiveActivity = false,
    useBlur = false,
    predictiveBackAnimation = com.rosan.installer.domain.settings.model.preferences.PredictiveBackAnimation.entries.first(),
    predictiveBackExitDirection = com.rosan.installer.domain.settings.model.preferences.PredictiveBackExitDirection.entries.first(),
)
