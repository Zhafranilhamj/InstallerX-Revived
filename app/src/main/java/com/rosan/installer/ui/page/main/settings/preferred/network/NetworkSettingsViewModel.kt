// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.main.settings.preferred.network

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rosan.installer.domain.settings.repository.AppSettingsRepository
import com.rosan.installer.domain.settings.repository.BooleanSetting
import com.rosan.installer.domain.settings.repository.StringSetting
import com.rosan.installer.domain.settings.usecase.settings.UpdateSettingUseCase
import com.rosan.installer.domain.virustotal.model.VirusTotalApiKeyFormat
import com.rosan.installer.domain.virustotal.model.VirusTotalErrorKind
import com.rosan.installer.domain.virustotal.model.VirusTotalException
import com.rosan.installer.domain.virustotal.usecase.ValidateVirusTotalApiKeyUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class NetworkSettingsViewModel(
    appSettingsRepo: AppSettingsRepository,
    private val updateSetting: UpdateSettingUseCase,
    private val validateVirusTotalApiKey: ValidateVirusTotalApiKeyUseCase,
) : ViewModel() {

    private val validationState = MutableStateFlow(VirusTotalKeyValidation())
    private var validationJob: Job? = null

    val state: StateFlow<NetworkSettingsState> = combine(
        appSettingsRepo.preferencesFlow,
        validationState,
    ) { prefs, validation ->
        NetworkSettingsState(
            allowInternetAccess = prefs.allowInternetAccess,
            networkSourceMode = prefs.networkSourceMode,
            networkSourceModeWarningAcknowledged = prefs.networkSourceModeWarningAcknowledged,
            httpProfile = prefs.labHttpProfile,
            githubUpdateChannel = prefs.githubUpdateChannel,
            customGithubProxyUrl = prefs.customGithubProxyUrl,
            virusTotalApiKey = prefs.virusTotalApiKey,
            virusTotalScanMode = prefs.virusTotalScanMode,
            virusTotalKeyValidation = validation,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = NetworkSettingsState(),
    )

    init {
        // Re-validate a stored key when the page opens so Normal and Strict mode stay unlocked
        // without making the user press "Check key" again after every visit.
        viewModelScope.launch {
            val storedKey = appSettingsRepo.preferencesFlow.first().virusTotalApiKey
            if (storedKey.isNotBlank()) {
                validateKey(storedKey, debounce = false)
            }
        }
    }

    fun dispatch(action: NetworkSettingsAction) {
        when (action) {
            is NetworkSettingsAction.ChangeInternetAccess -> viewModelScope.launch {
                updateSetting(BooleanSetting.AllowInternetAccess, action.enabled)
            }

            is NetworkSettingsAction.ChangeNetworkSourceMode -> viewModelScope.launch {
                updateSetting(
                    StringSetting.NetworkSourceMode,
                    action.mode.value,
                )
            }

            is NetworkSettingsAction.ConfirmNetworkSourceMode -> viewModelScope.launch {
                updateSetting(StringSetting.NetworkSourceMode, action.mode.value)
                updateSetting(BooleanSetting.NetworkSourceModeWarningAcknowledged, true)
            }

            is NetworkSettingsAction.ChangeHttpProfile -> viewModelScope.launch {
                updateSetting(
                    StringSetting.LabHttpProfile,
                    action.profile.name,
                )
            }

            is NetworkSettingsAction.ChangeGithubUpdateChannel -> viewModelScope.launch {
                updateSetting(
                    StringSetting.GithubUpdateChannel,
                    action.channel.name,
                )
            }

            is NetworkSettingsAction.ChangeCustomGithubProxyUrl -> viewModelScope.launch {
                updateSetting(
                    StringSetting.CustomGithubProxyUrl,
                    action.url,
                )
            }

            is NetworkSettingsAction.ChangeVirusTotalApiKey -> {
                viewModelScope.launch {
                    updateSetting(StringSetting.VirusTotalApiKey, action.key)
                }
                validateKey(action.key, debounce = true)
            }

            is NetworkSettingsAction.ChangeVirusTotalScanMode -> viewModelScope.launch {
                updateSetting(
                    StringSetting.VirusTotalScanMode,
                    action.mode.value,
                )
            }

            NetworkSettingsAction.ValidateVirusTotalApiKey -> {
                validateKey(state.value.virusTotalApiKey, debounce = false)
            }
        }
    }

    /**
     * Validates the key locally first, then asks VirusTotal to confirm it. Remote validation is
     * debounced so typing a 64-character key does not fire dozens of requests.
     */
    private fun validateKey(key: String, debounce: Boolean) {
        validationJob?.cancel()

        val trimmed = key.trim()
        if (trimmed.isEmpty()) {
            validationState.value = VirusTotalKeyValidation()
            return
        }

        if (!VirusTotalApiKeyFormat.isValid(trimmed)) {
            validationState.value = VirusTotalKeyValidation(
                status = VirusTotalKeyValidationStatus.INVALID_FORMAT,
            )
            return
        }

        if (!state.value.allowInternetAccess) {
            validationState.value = VirusTotalKeyValidation(
                status = VirusTotalKeyValidationStatus.NETWORK_ERROR,
            )
            return
        }

        validationState.value = VirusTotalKeyValidation(
            status = VirusTotalKeyValidationStatus.VALIDATING,
        )

        validationJob = viewModelScope.launch {
            if (debounce) delay(VALIDATION_DEBOUNCE_MS)
            val result = validateVirusTotalApiKey(trimmed)
            result.fold(
                onSuccess = {
                    validationState.value = VirusTotalKeyValidation(
                        status = VirusTotalKeyValidationStatus.VALID,
                    )
                },
                onFailure = { error ->
                    validationState.value = error.toValidationState()
                },
            )
        }
    }

    private fun Throwable.toValidationState(): VirusTotalKeyValidation {
        val kind = (this as? VirusTotalException)?.kind
        return when (kind) {
            VirusTotalErrorKind.INVALID_API_KEY -> VirusTotalKeyValidation(
                status = VirusTotalKeyValidationStatus.INVALID_KEY,
            )

            VirusTotalErrorKind.QUOTA_EXCEEDED -> VirusTotalKeyValidation(
                status = VirusTotalKeyValidationStatus.QUOTA_EXCEEDED,
            )

            VirusTotalErrorKind.NETWORK_ERROR -> VirusTotalKeyValidation(
                status = VirusTotalKeyValidationStatus.NETWORK_ERROR,
            )

            else -> VirusTotalKeyValidation(
                status = VirusTotalKeyValidationStatus.UNKNOWN,
            )
        }
    }

    private companion object {
        const val VALIDATION_DEBOUNCE_MS = 600L
    }
}
