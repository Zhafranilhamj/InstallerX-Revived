# InstallerX Revived (Community Edition)

**English** | [简体中文](README_CN.md) | [Español](README_ES.md) | [日本語](README_JA.md) | [Deutsch](README_DE.md)

[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)
[![Latest Release](https://img.shields.io/github/v/release/wxxsfxyzm/InstallerX?label=Stable)](https://github.com/wxxsfxyzm/InstallerX/releases/latest)
[![Prerelease](https://img.shields.io/github/v/release/wxxsfxyzm/InstallerX?include_prereleases&label=Beta)](https://github.com/wxxsfxyzm/InstallerX/releases)
[![Telegram](https://img.shields.io/badge/Telegram-2CA5E0?logo=telegram&logoColor=white)](https://t.me/installerx_revived)

> A modern and functional Android app installer. (You know some birds are not meant to be caged, their feathers are just too bright.)

Looking for a better app installer? Try **InstallerX**!

InstallerX Revived is a modern Android package installer and the community-maintained continuation of the original [InstallerX](https://github.com/iamr0s/InstallerX) project.

It is designed to replace limited stock or OEM package installers with a cleaner interface, broader package support, configurable installation profiles, and privileged workflows through Shizuku, Root, Dhizuku, or the system package manager mode.

## Documentation

The full user guide, installation instructions, advanced options, system integration notes, and FAQ are maintained on the [documentation site](https://wxxsfxyzm.github.io/InstallerX-Revived-Website/).

## Highlights

- **Package formats:** APK, APKS, APKM, XAPK, APK files inside ZIP archives, and batch APK installation.
- **Dialog batch selection:** clearing a single-version package row clears its files; selecting it again restores the file choices from before clearing, including split and dex metadata choices. This applies to both UI styles within the same analysis; individual file edits or explicit APK-wide selection changes supersede the saved choices.
- **Install flows:** dialog installation, background notification installation, automatic installation, silent installation when privileges allow it, and Android 16+ Live Activity progress on supported systems.
- **Authorizers:**
  - **Root:** can perform all privileged operations, but may be slower because of cold `app_process` startup.
  - **Shizuku:** obtains shell or root capabilities depending on how it is activated, and is usually faster than direct Root.
  - **Dhizuku:** can perform DevicePolicyManager-based operations such as default installer locking and app installation, but is limited for other privileged tasks.
  - **None:** is fully limited by the system, but can silently install when InstallerX is running as the system package installer.
- **Profiles:** define how installation and uninstallation requests are handled, including install mode, authorizer override, installer/requester metadata, target user, DexOpt, auto-delete behavior, split selection, blacklist policy, and signature gates.
- **System integration:** InstallerX can be locked as the default installer from the Home page status card, used with LSPosed modules such as [InxLocker](https://github.com/Chimioo/InxLocker), or installed as a replacement system package manager by advanced users.
- **Modern UI:** Material 3 Expressive and Miuix interface styles, dark mode, dynamic color, advanced palettes, system icon packs, colorful dialogs, standard notifications, Live Activity, and Xiaomi HyperOS-style island notifications on supported Xiaomi devices.
- **Safety controls:** package-name and SharedUID blacklists, signature mismatch and unknown-signature policy gates, permission preview, install flags, and one-time smart suggestions for selected blocked cases.
- **VirusTotal scanning:** automatic SHA-256 hash lookup in Normal mode (no upload, advisory only), optional manual upload, and mandatory upload-before-install in Strict mode, which refuses files that VirusTotal actually detects as malware. The API key is validated in Network settings, is required before either scanning mode can be selected, and is stored locally.

## Tools

- **OBB support:** `.obb` entries inside XAPK/APKM containers are extracted automatically to `Android/obb/<package>/`, and standalone OBB files selected with an APK are attached to that install. Writes try direct filesystem I/O, then the existing Shizuku/Root privileged user service, then a user-granted SAF tree (grant it once from **Settings -> Preferred -> Tools -> Local sharing**). OBB failures never abort the APK install.
- **Local sharing:** a built-in HTTP server and browser dashboard accept APK/APKS/APKM/XAPK/OBB uploads from a PC or phone on the same network. Access is limited to local-network peers and can be protected by an optional 4-8 digit PIN. The dashboard QR code can be scanned by another device running InstallerX.
- **Send to TV:** scan the QR code shown by the TV-side dashboard (or enter its address and PIN), pick a package, and push it directly to the TV over HTTP.
- **App manager:** inspect installed user and system apps with SDK levels, APK size, permissions, signature digests, install/update/last-used times, sort/filter by name, install date, size or last used time, and batch uninstall with a status log. Last-used times require the optional usage access permission.
- **Remote downloader:** download packages from HTTP(S) URLs with local download history. `http(s)` package links, shared URLs from messaging apps, and `installerx://download?url=...` / `installerx://tv?u=...&p=...` deep links are handled by the link intake activity and passed to the normal installer. Downloads and local sharing never run while the in-app network access setting is off.

## Supported Android Versions

- **Full support:** Android SDK 34 - 37.0
- **Limited support:** Android SDK 26 - 33

Limited support means InstallerX may work, but some features can be unavailable or behave differently because of Android framework, OEM, or authorizer limitations.

## Downloads

- **Stable releases:** https://github.com/wxxsfxyzm/InstallerX-Revived/releases/latest
- **Alpha builds:** https://github.com/wxxsfxyzm/InstallerX/releases
- **CI builds:** https://github.com/wxxsfxyzm/InstallerX-Revived/actions/workflows/auto-preview-dev.yml
- **Telegram channel:** https://t.me/installerx_revived

When reporting bugs, please reproduce them on the latest Alpha or CI build whenever possible, because issues in Stable may already be fixed.

InstallerX is now published as a single APK. Network access is controlled by an in-app setting. The release filename continues to include `online` only for compatibility with older in-app update clients; it no longer identifies a separate build variant.

### VirusTotal scanning

Network settings include an optional VirusTotal integration. Paste a 64-character VirusTotal API key to validate it; the app shows "Ready to scan" when the key is accepted. **Normal** and **Strict** stay greyed out until the key has been validated, so no scan mode can be selected without a working key. Then choose a scan mode:

- **Off:** no VirusTotal requests are made.
- **Normal:** the install confirmation screen starts a **SHA-256 hash lookup** automatically, without uploading any file. **Scan with VirusTotal** repeats the lookup and **Upload files for scanning** remains available for a full manual scan. A report never blocks installation, and the lookup is skipped while network access is off or no valid API key is stored.
- **Strict:** the file is uploaded automatically and installation is blocked until the scan finishes. If the file is larger than 650 MB or the network request fails, the app shows an error and allows installation with a warning.

Uploads up to and including 32 MB use the standard `/files` endpoint. Larger files, up to and including 650 MB, first request a single-use upload URL from `GET /files/upload_url`; the file is then streamed to that validated VirusTotal URL with the API key, and upload redirects are not followed. Files above 650 MB or with an unknown size are rejected before upload. Queued analyses keep polling while the scan is active instead of failing after a fixed two-minute window; closing the installer session cancels the scan.

The install screen uses a compact idle status card and separate hash-scan and upload cards. Scanning, completed reports, and failures expand the details inline inside the status card; tap the card to collapse or expand them afterwards. Scan results list the detection count and every flagging engine with the name each engine reported. Detections that match the patterns VirusTotal routinely reports for repackaged or optimised APKs are shown in amber as **Suspicious**, with the original caution message unchanged; Strict mode still allows installing those. A report that is a real malicious verdict blocks installation in Strict mode.

Expanded reports include VirusTotal's community harmless/malicious vote totals and reputation. You can vote harmless or malicious with the VirusTotal account associated with your API key, if that account has voting permission. Votes are public and do not change the antivirus verdict or Strict mode's install gate. Totals are refreshed from VirusTotal after a successful vote; a voting or score-loading failure never changes the scan result.

The API key is stored locally on the device and is only sent to VirusTotal. Scanning and community requests are disabled when the in-app network access setting is off or the scan mode is Off.

## Building

InstallerX Revived is an Android Gradle project.

### Prerequisites

- **JDK 25** with `JAVA_HOME` configured correctly.
- Android SDK / Android Studio with the required platform and build tools installed.
- GitHub Packages credentials for the snapshot `miuix` dependency.

### GitHub Packages Authentication

GitHub Packages requires authentication even for public packages. Add your GitHub username and a classic personal access token with the `read:packages` scope to your global Gradle properties file:

- Linux / macOS: `~/.gradle/gradle.properties`
- Windows: `%USERPROFILE%\.gradle\gradle.properties`

```properties
gpr.user=YOUR_GITHUB_USERNAME
gpr.key=YOUR_PERSONAL_ACCESS_TOKEN
```

Do not commit these credentials to this repository.

### Build Commands

For a local debug build:

```bash
./gradlew assembleUnstableDebug
```

For a PR-style test build with a separate application id:

```bash
./gradlew assemblePreviewDebug -PAPP_ID="com.rosan.installer.x.revived.test"
```

## Common Questions

### Where should I report bugs or ask questions?

Use [GitHub Issues](https://github.com/wxxsfxyzm/InstallerX-Revived/issues) for reproducible bugs and concrete feature requests. Good suggestions are welcome. Use [GitHub Discussions](https://github.com/wxxsfxyzm/InstallerX-Revived/discussions) or the [Telegram channel](https://t.me/installerx_revived) for general questions and compatibility discussion.

Before opening an issue, please read [CONTRIBUTING.md](../CONTRIBUTING.md) for the required logs and reproduction details.

### I cannot lock InstallerX as the default installer

Some OEM systems strictly control the default package installer. Open the default installer page from the Home page status card and try locking from there. If the ROM still prevents it, use an LSPosed module such as [InxLocker](https://github.com/Chimioo/InxLocker).

### HyperOS says installing system apps requires a valid installer

This is an OEM security restriction. InstallerX can declare installer metadata through profiles, and on HyperOS it uses `com.android.shell` as the default compatibility installer package. Shizuku or Root is required for this workflow; Dhizuku is not enough.

### Notification installation progress is stuck

Some ROMs restrict background services aggressively. Set InstallerX to unrestricted background/battery mode if notification installation stalls. InstallerX cleans up its foreground service shortly after the installation task finishes.

### How do I replace the system package manager?

This is a high-risk advanced workflow. The short version is: use Core Patch to overwrite the APK, flash a matching module, or integrate the matching package into `super` / a ROM build. Before flashing or packaging, verify the package name, mount path, and permissions file for your ROM.

See the system integration guide for details: https://wxxsfxyzm.github.io/InstallerX-Revived-Website/guide/system-integration

## Localization

Help translate InstallerX Revived on Weblate:

https://hosted.weblate.org/engage/installerx-revived/

[![Localization Status](https://hosted.weblate.org/widget/installerx-revived/strings/multi-auto.svg)](https://hosted.weblate.org/engage/installerx-revived/)

## License

Copyright (C) [iamr0s](https://github.com/iamr0s) and [InstallerX Revived Contributors](https://github.com/wxxsfxyzm/InstallerX-Revived/graphs/contributors)

InstallerX Revived is released under the [GNU General Public License v3](http://www.gnu.org/licenses/gpl-3.0).

If you base your work on InstallerX Revived, you must comply with the open-source license terms of the specific source version you use.

## Acknowledgements

This project uses code from, or is based on the implementation of, the following projects:

- [iamr0s/InstallerX](https://github.com/iamr0s/InstallerX)
- [tiann/KernelSU](https://github.com/tiann/KernelSU)
- [RikkaApps/Shizuku](https://github.com/RikkaApps/Shizuku)
- [zacharee/InstallWithOptions](https://github.com/zacharee/InstallWithOptions)
- [vvb2060/PackageInstaller](https://github.com/vvb2060/PackageInstaller)
- [compose-miuix-ui/miuix](https://github.com/compose-miuix-ui/miuix)

## Star History

<a href="https://www.star-history.com/?repos=wxxsfxyzm%2FInstallerX-Revived&type=date&legend=top-left">
 <picture>
   <source media="(prefers-color-scheme: dark)" srcset="https://api.star-history.com/chart?repos=wxxsfxyzm/InstallerX-Revived&type=date&theme=dark&legend=top-left&sealed_token=NIZn5WH0-dVDGHKTPTVKAke10T3j7OrdIZ6rTbhT4zwN5AyJhj5aJhc36MEg2ZD5FoBrDoxgF3jzozO8cpBCQoJ65WbUGZZestdrI42Rnv8QVdEvE9Jz-qfghs5RpT5BeRHLuDH2NLyDNypEg34_XaPsTDAkx6DZAl0bUc0tpa0C2xzKBPih_ELq-sP2" />
   <source media="(prefers-color-scheme: light)" srcset="https://api.star-history.com/chart?repos=wxxsfxyzm/InstallerX-Revived&type=date&legend=top-left&sealed_token=NIZn5WH0-dVDGHKTPTVKAke10T3j7OrdIZ6rTbhT4zwN5AyJhj5aJhc36MEg2ZD5FoBrDoxgF3jzozO8cpBCQoJ65WbUGZZestdrI42Rnv8QVdEvE9Jz-qfghs5RpT5BeRHLuDH2NLyDNypEg34_XaPsTDAkx6DZAl0bUc0tpa0C2xzKBPih_ELq-sP2" />
   <img alt="Star History Chart" src="https://api.star-history.com/chart?repos=wxxsfxyzm/InstallerX-Revived&type=date&legend=top-left&sealed_token=NIZn5WH0-dVDGHKTPTVKAke10T3j7OrdIZ6rTbhT4zwN5AyJhj5aJhc36MEg2ZD5FoBrDoxgF3jzozO8cpBCQoJ65WbUGZZestdrI42Rnv8QVdEvE9Jz-qfghs5RpT5BeRHLuDH2NLyDNypEg34_XaPsTDAkx6DZAl0bUc0tpa0C2xzKBPih_ELq-sP2" />
 </picture>
</a>
