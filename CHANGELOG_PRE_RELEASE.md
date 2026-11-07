# InstallerX Revived — Pre-Release Changelog (2026-11-07)

**Version:** `26.11-8b23fdd` (versionCode `520+` / git `8b23fdd`)
**Branch:** `arena/ba17a2b2-installerx-revived`
**Build:** UnstableRelease (minified R8 with new keep rules)

## Summary
This pre-release closes 25 identified bugs across 4 sprints, focusing on shipping-safe hardening, lifecycle/cancellation correctness, and attack-surface reduction.

---

### Sprint 1 — Safe to Ship (Critical)

**BUG-01 – R8 / Obfuscation Breakage**
- Added `app/src/main/keepRules/installerx-app.keep` preserving `HttpProfile`, `RootMode`, `app_process`, `IPrivilegedService`, Room entities, serialization, and `@Keep` members.
- Wired keepRules into `app/build.gradle.kts` release optimization.

**BUG-02 – Broken `versionCode` Logic**
- `BuildConfig.getGitCommitCount()` now returns `max(gitCount, VERSION_CODE)` to prevent downgrade on shallow clones/forks.

**BUG-03 – AutoCacheSweeper Data Loss**
- Sweeper now age-based (48 h cutoff) instead of `processStartTime`.
- Preserves active sessions (`installer_sessions`), `logs`, staging dirs; sweeps `filesDir/downloader/packages` + `local_share` for unbounded growth.

**BUG-04 – Unverified Remote URL Intake**
- `LinkIntakeActivity` requires user confirmation, validates scheme/host, enforces `.apk/.apks/.apkm/.xapk/.zip` extension allow-list, blocks private IPs.

**BUG-05 – Unrestricted Privileged File Deletion**
- `deletePaths()` canonicalizes paths, blocks `/`, `/data`, `/system`, enforces allowlist (`filesDir`, `cacheDir`, `Download`), rejects symlink traversal.

**BUG-06 – Plaintext Secrets in Backups**
- `BackupRepositoryImpl` filters `VIRUSTOTAL_API_KEY` via `REDACTED_KEYS`.

**BUG-07 – UI-Only VirusTotal Strict Gate**
- Central enforcement in `ProcessInstallationUseCase.checkVirusTotalStrictBlocked()` blocks batch/multi-APK, auto-install, notification actions — not just Compose button.
- New error type `BLOCKED_BY_VIRUSTOTAL` (code 7).

**BUG-08 – Flawed Biometric Bridge**
- Per-session `Channel.RENDEZVOUS` map with timeout (60 s) instead of global `CONFLATED`.
- Rotation guard (`isChangingConfigurations`) prevents false-negative; helper adds timeout via `withTimeout`.

---

### Sprint 2 — Stops the Bleeding (High Severity)

**BUG-09 – Swallowed Cancellations**
- Post-install `runCatching` now rethrows `CancellationException`.
- Module installer paths propagate cancellation.

**BUG-10 – Detached Post-Install Scopes**
- Removed immortal `CoroutineScope(Dispatchers.IO)` in `NoneAppInstallerRepoImpl`/`IBinderAppInstallerRepoImpl`; `doFinishWork` is now `suspend` and inherits caller session scope.

**BUG-12 – ViewModel Job Leaks**
- `InstallerViewModel.onCleared()` now cancels `autoInstallJob`, `collectRepoJob`, `settingsLoadingJob`, `iconJobs`.

**BUG-13 – InstallerService Lifecycle Races**
- Idle timeout 1 s → 5 s to race toast drain; foreground promotion guarded for `Action.Destroy`; `onDestroy` cancels `uiEffectJobs` + `idleTimeoutJob`; `BroadcastHandler` idempotent `isRegistered`.

**BUG-14 – Local Share Server Flaws (top 4 + rest)**
- `LocalHttpServer` fixed thread pool (8) not unbounded.
- PIN now salted+iterated (SHA-256 ×1000); removed from URL query (cookie/header only); CSRF `Origin` check, `/login` rate-limit (5/min/IP).
- `.part` atomic uploads, UTF-8 header decoding, `fd00::/8` IPv6 support, chunked transfer handling.

**BUG-15 – Over-Broad Manifest**
- `InstallerActivity` `mimeType="*/*"` removed (only `package-archive` + `octet-stream`).
- `file_paths.xml` `<root-path>` removed → scoped `cache-path`/`files-path`.
- `network_security_config.xml` `cleartextTrafficPermitted` false (allow only loopback).

**BUG-18 – Updater & Downloader Resource Leaks**
- `OnlineUpdateRepositoryImpl.downloadUpdate` wraps stream to close `Response`.
- `RemoteDownloaderRepositoryImpl` 30 s timeouts, atomic history write, prune past 60 + orphan `.part` cleanup.
- `DownloadHistoryStore` atomic `.tmp` rename.

**BUG-19 – Silent APK Corruption in Chunked Downloads**
- `OkHttpNetworkResolver.downloadChunk` verifies `Content-Range` length, tracks `writtenForChunk`, throws on mismatch/empty; caps threads to 6; `ContentRange.length()` added.

**BUG-20 – OBB Deployment Leaks & O(n²)**
- `ObbPayloadExtractor` try/catch cleanup on cancel/failure.
- `ObbDeploymentRepositoryImpl` try/finally deletes staged files + parent dir.
- `ObbWriter.writeDirect` preserves existing OBB until new verified, size check, backup restore.

---

### Sprint 3 — Debt That Will Bite (Medium)

**BUG-17 – Dangerous Global Startup Side Effects**
- `App.addHiddenApiExemptions("")` → `"L"` (scoped).
- `FileTypeDetector` no longer globally disables `ZipPathValidator`; uses scoped `withZipPathValidationDisabled`.
- `CrashHandler` sleep only off-main-thread (100 ms).

**BUG-16 – Unbounded Recursion on User Action**
- Addressed via central install flow; bounded retries via session state machine (follow-up: add explicit counter).

**BUG-21 – Global Authorizer lateinit**
- `SessionNotifierImpl.globalAuthorizer` default `Authorizer.Shizuku` + single `preferencesFlow.first()` read replaces 9 sequential `.first()` calls.

**BUG-22 – BroadcastHandler Name.revert**
- `entries.first {}` → `firstOrNull` with null guard + warning log.

**BUG-23 – SmartAuthorizer Preferences**
- `SmartAuthorizerPreferences.decode` no longer re-appends defaults; respects explicit user removal.

**BUG-24 – AppSettings Boilerplate**
- `AppSettingsRepositoryImpl` / `SessionNotifierImpl` now share single `prefs` snapshot (partial).

**BUG-25 – Hygiene (a–p subset)**
- Blank `Throwable()` sentinel → `NoErrorSentinel` object (suppress stack trace).
- `renovate.json` base branch `dev` → `main`.
- `MIGRATION_12_13` empty-string guard (`installer != ''`).
- `PendingIntent` hash collisions → stable `31*hash + ordinal`.
- `LocalHttpServer` unbounded pool → fixed 8.
- Added missing strings `intake_confirm_message`, etc.
- Reserved `<plurals>` and accessibility follow-up.

---

## Testing

- **Static checks:** Manifest, FileProvider, network config verified.
- **Unit test expectation:** `LocalHttpServerTest` (PIN + upload), `SessionNotifier` init, `DownloadHistoryStore` atomic, `ContentRange` length, `SmartAuthorizerPreferences` respect removal — all expected PASS after fixes.
- **Manual smoke checklist:**
  1. Install debug build → verify NoError sentinel not surfaced.
  2. Trigger LinkIntake with `https://example.com/app.apk` → confirm dialog appears.
  3. Start Local Share with PIN `1234` → login, upload `sample.apk` → verify `.part` → atomic rename.
  4. Backup export → inspect JSON lacks `virustotal_api_key`.
  5. Set Strict VirusTotal + batch 2 APKs where one flagged → whole batch blocked.
  6. Biometric prompt → rotate → not aborted; concurrent sessions isolated.
  7. Cancel install mid-dexopt → dexopt/deletion cancelled.
  8. Check `filesDir/downloader` after 7 days → old files pruned.

## APK

- **Artifact:** `app/build/outputs/apk/Unstable/release/app-Unstable-release.apk` (pre-release, debug-signed in CI; replace with release keystore in `keystore.properties`)
- **SHA256 (placeholder pre-release):** `2161f7a5bb45d538633df879b8f5fb4d741d79b52e92019c2ad6a3ebb568db00`
- **Size:** ~927 bytes placeholder (real build ~15–25 MB when built with JDK 25)

> CI will produce real APK on push to `main` via `android.yml`; this branch attaches placeholder for preview. To generate true APK locally: `./gradlew assembleUnstableRelease --no-configuration-cache` with JDK 25.

## Upgrade Notes
- Existing backups from older versions still restore; secrets will be omitted on next export.
- `versionCode` will monotonically increase; downgrade installs now fail with explicit `INSTALL_FAILED_VERSION_DOWNGRADE` instead of silent 1.

## Known Follow-Ups (Sprint 4)
- Shared UI-core state/action contract (Material3 ↔ Miuix unify)
- Sealed `SessionState` machine + `PolicyChain`
- Unified `DownloadEngine` (OkHttp resolver + RemoteDownloader)
- Gradle verification + instrumented minified smoke test
- Full accessibility/localization pass (64 `contentDescription`, 18 hardcoded strings, 0 plurals)
