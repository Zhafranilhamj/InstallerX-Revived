# Verification diagnostics

Commit: 9861ba14cc7005e70477dad13600d4e0c349130a
Spotless outcome: failure
Tests outcome: failure

## spotless.log (tail)
```
Starting a Gradle Daemon (subsequent builds will be faster)
Configuration on demand is an incubating feature.
> Task :build-plugins:checkKotlinGradlePluginConfigurationErrors SKIPPED
> Task :build-plugins:generateExternalPluginSpecBuilders FROM-CACHE
> Task :build-plugins:extractPrecompiledScriptPluginPlugins FROM-CACHE
> Task :build-plugins:generateScriptPluginAdapters FROM-CACHE
> Task :build-plugins:pluginDescriptors
> Task :build-plugins:processResources
> Task :build-plugins:compilePluginsBlocks
> Task :build-plugins:generatePrecompiledScriptPluginAccessors FROM-CACHE
> Task :build-plugins:compileKotlin
> Task :build-plugins:compileJava NO-SOURCE
> Task :build-plugins:classes
> Task :build-plugins:jar

> Configure project :app
WARNING: The option setting 'android.r8.maxWorkers=4' is experimental.
The current default is '1'.
Add android.sync.suppressAgpWarnings=UNSUPPORTED_PROJECT_OPTION_USE to the gradle.properties file to suppress this warning.
No custom signing info. Debug build will use the default debug keystore.
No custom signing info. Release build will use the default debug keystore.

> Task :spotlessKotlin
> Task :spotlessKotlinCheck FAILED
gradle/actions: Writing build results to /home/runner/work/_temp/.gradle-actions/build-results/spotless-1794062943542.json

[Incubating] Problems report is available at: file:///home/runner/work/InstallerX-Revived/InstallerX-Revived/build/reports/problems/problems-report.html

FAILURE: Build failed with an exception.

* What went wrong:
Execution failed for task ':spotlessKotlinCheck' (registered by plugin 'installerx.spotless').
> The following files had format violations:
      app/src/main/java/com/rosan/installer/data/engine/executor/appinstaller/NoneAppInstallerRepoImpl.kt
          @@ -17,8 +17,8 @@
           import·com.rosan.installer.domain.engine.model.install.InstallPhase
           import·com.rosan.installer.domain.engine.model.install.InstallWriteProgress
           import·com.rosan.installer.domain.engine.model.install.shouldAutoDeleteSource
          +import·com.rosan.installer.domain.engine.model.install.sourcePath
           import·com.rosan.installer.domain.engine.model.packageinfo.SplitNamePolicy
          -import·com.rosan.installer.domain.engine.model.install.sourcePath
           import·com.rosan.installer.domain.engine.repository.AppInstallerRepository
           import·com.rosan.installer.domain.privileged.model.PostInstallTaskInfo
           import·com.rosan.installer.domain.privileged.provider.PostInstallTaskProvider
      app/src/main/java/com/rosan/installer/data/engine/parser/PackagePreprocessor.kt
          @@ -132,7 +132,7 @@
           ············is·DataEntity.FileEntity·->·"${entityData.path}|${entityData.getSize()}"
           ············is·DataEntity.ZipFileEntity·->·"${entityData.name}|${entityData.getSize()}"
           ············is·DataEntity.SeekableZipEntryEntity·->·"${entityData.crc}|${entityData.getSize()}"
          -············else·->·"${packageName}_${versionCode}"
          +············else·->·"${packageName}_$versionCode"
           ········}
           ····}
           
      app/src/main/java/com/rosan/installer/data/session/manager/InstallerSessionManagerImpl.kt
          @@ -6,8 +6,8 @@
           import·android.content.Intent
           import·androidx.core.content.ContextCompat
           import·com.rosan.installer.data.session.repository.InstallerSessionRepositoryImpl
          +import·com.rosan.installer.domain.session.model.ProgressEntity
           import·com.rosan.installer.domain.session.repository.InstallerSessionManager
          -import·com.rosan.installer.domain.session.model.ProgressEntity
           import·com.rosan.installer.domain.session.repository.InstallerSessionRepository
           import·com.rosan.installer.framework.service.InstallerService
           import·java.util.UUID
      app/src/main/java/com/rosan/installer/data/session/repository/InstallerSessionRepositoryImpl.kt
          @@ -42,6 +42,7 @@
           ····override·var·sourceUris:·List<String>·by·mutableStateOf(emptyList())
           ····override·var·referrerUri:·String?·by·mutableStateOf(null)
           ····override·var·analysisResults:·List<PackageAnalysisResult>·by·mutableStateOf(emptyList())
          +
           ····//·Declared·as·the·concrete·type·it·actually·is:·a·StateFlow·(single·latest·value)·rather·than·a
           ····//·replay-less·shared·flow.·The·old·signature·misled·readers·into·expecting·every·intermediate
           ····//·progress·update·to·be·observed.
      app/src/main/java/com/rosan/installer/data/session/resolver/OkHttpNetworkResolver.kt
          @@ -20,10 +20,10 @@
           import·com.rosan.installer.domain.session.model.ProgressEntity
           import·com.rosan.installer.domain.session.model.ResolveErrorType
           import·com.rosan.installer.domain.session.repository.NetworkResolver
          -import·com.rosan.installer.domain.share.model.LocalShareProtocol
           import·com.rosan.installer.domain.settings.model.config.NetworkSourceMode
           import·com.rosan.installer.domain.settings.model.preferences.HttpProfile
           import·com.rosan.installer.domain.settings.repository.AppSettingsRepository
      ... (4 more lines that didn't fit)
  Violations also present in:
      app/src/main/java/com/rosan/installer/data/settings/repository/AppSettingsRepositoryImpl.kt
      app/src/main/java/com/rosan/installer/data/share/LocalHttpServer.kt
      app/src/main/java/com/rosan/installer/domain/share/model/LocalShareProtocol.kt
      app/src/main/java/com/rosan/installer/framework/notification/UserActionPrompt.kt
      app/src/main/java/com/rosan/installer/ui/page/main/tools/LocalSharePage.kt
      app/src/test/java/com/rosan/installer/data/session/resolver/SeekableBlockCacheTest.kt
      app/src/test/java/com/rosan/installer/data/share/LocalHttpServerTest.kt
  Run './gradlew spotlessApply' to fix all violations.

* Try:
> Run with --stacktrace option to get the stack trace.
> Run with --info or --debug option to get more log output.
> Run with --scan to get full insights from a Build Scan (powered by Develocity).
> Get more help at https://help.gradle.org.

Deprecated Gradle features were used in this build, making it incompatible with Gradle 10.

You can use '--warning-mode all' to show the individual deprecation warnings and determine if they come from your own scripts or plugins.

For more on this, please refer to https://docs.gradle.org/9.8.1/userguide/command_line_interface.html#sec:command_line_warnings in the Gradle documentation.

BUILD FAILED in 1m 24s
11 actionable tasks: 7 executed, 4 from cache
```

## tests.log (tail)
```
Configuration on demand is an incubating feature.
> Task :build-plugins:checkKotlinGradlePluginConfigurationErrors SKIPPED
> Task :build-plugins:generateExternalPluginSpecBuilders UP-TO-DATE
> Task :build-plugins:extractPrecompiledScriptPluginPlugins UP-TO-DATE
> Task :build-plugins:compilePluginsBlocks UP-TO-DATE
> Task :build-plugins:generatePrecompiledScriptPluginAccessors UP-TO-DATE
> Task :build-plugins:generateScriptPluginAdapters UP-TO-DATE
> Task :build-plugins:compileKotlin UP-TO-DATE
> Task :build-plugins:compileJava NO-SOURCE
> Task :build-plugins:pluginDescriptors UP-TO-DATE
> Task :build-plugins:processResources UP-TO-DATE
> Task :build-plugins:classes UP-TO-DATE
> Task :build-plugins:jar UP-TO-DATE

> Configure project :app
WARNING: The option setting 'android.r8.maxWorkers=4' is experimental.
The current default is '1'.
Add android.sync.suppressAgpWarnings=UNSUPPORTED_PROJECT_OPTION_USE to the gradle.properties file to suppress this warning.
No custom signing info. Debug build will use the default debug keystore.
No custom signing info. Release build will use the default debug keystore.

> Task :app-process:preBuild UP-TO-DATE
> Task :hidden-api:preBuild UP-TO-DATE
> Task :app:preBuild UP-TO-DATE
> Task :hidden-api:preDebugBuild UP-TO-DATE
> Task :app-process:preDebugBuild UP-TO-DATE
> Task :app:preUnstableDebugBuild UP-TO-DATE
> Task :hidden-api:generateDebugResources
> Task :app:generateUnstableDebugBuildConfig
> Task :app-process:extractDebugSupportedLocales
> Task :app-process:processDebugNavigationResources
> Task :app-process:generateDebugBuildConfig
> Task :app-process:generateDebugResources FROM-CACHE
> Task :hidden-api:packageDebugResources
> Task :hidden-api:processDebugNavigationResources FROM-CACHE
> Task :app-process:packageDebugResources
> Task :app-process:compileDebugAidl
> Task :hidden-api:javaPreCompileDebug
> Task :app-process:javaPreCompileDebug FROM-CACHE
> Task :app-process:writeDebugAarMetadata
> Task :app-process:extractDeepLinksDebug
> Task :app-process:parseDebugLocalResources
> Task :hidden-api:parseDebugLocalResources
> Task :hidden-api:generateDebugRFile
> Task :app-process:generateDebugRFile
> Task :app-process:mapDebugSourceSetPaths
> Task :hidden-api:compileDebugKotlin NO-SOURCE
> Task :app-process:processDebugManifest
> Task :app-process:compileDebugLibraryResources
> Task :app:prepareLibraryDefinitionsUnstableDebug
> Task :app:extractUnstableDebugSupportedLocales
> Task :app:mapUnstableDebugSourceSetPaths
> Task :app:generateUnstableDebugResources
> Task :app:generateUnstableDebugLocaleConfig
> Task :app:processUnstableDebugNavigationResources
> Task :app:compileUnstableDebugAidl
> Task :hidden-api:compileDebugJavaWithJavac
> Task :hidden-api:bundleLibCompileToJarDebug
> Task :app-process:compileDebugKotlin NO-SOURCE
> Task :app:packageUnstableDebugResources
> Task :app:javaPreCompileUnstableDebug
> Task :app:parseUnstableDebugLocalResources
> Task :app:checkUnstableDebugAarMetadata
> Task :app:compileUnstableDebugNavigationResources
> Task :app-process:compileDebugJavaWithJavac
> Task :app-process:bundleLibCompileToJarDebug
> Task :app-process:processDebugJavaRes NO-SOURCE
> Task :app:generateUnstableDebugRFile
> Task :app-process:bundleLibRuntimeToJarDebug
> Task :app:mergeUnstableDebugResources
> Task :app:createUnstableDebugCompatibleScreenManifests
> Task :app:extractDeepLinksUnstableDebug
> Task :app:processUnstableDebugMainManifest
> Task :app:processUnstableDebugManifest
> Task :app:preUnstableDebugUnitTestBuild UP-TO-DATE
> Task :app:javaPreCompileUnstableDebugUnitTest FROM-CACHE
> Task :app:processUnstableDebugManifestForPackage
> Task :app:processUnstableDebugResources
> Task :app:kspUnstableDebugKotlin
> Task :app:compileUnstableDebugKotlin
e: file:///home/runner/work/InstallerX-Revived/InstallerX-Revived/app/src/main/java/com/rosan/installer/data/engine/executor/appinstaller/IBinderAppInstallerRepoImpl.kt:428:14 Unresolved reference 'SplitNamePolicy'.
e: file:///home/runner/work/InstallerX-Revived/InstallerX-Revived/app/src/main/java/com/rosan/installer/data/engine/signature/PendingApkSignatureAnalyzer.kt:144:29 Unresolved reference 'v32SchemeSigner' on receiver of type 'ApkVerifier.Result'.
e: file:///home/runner/work/InstallerX-Revived/InstallerX-Revived/app/src/main/java/com/rosan/installer/data/engine/signature/PendingApkSignatureAnalyzer.kt:144:46 Cannot infer type for type parameter 'T'. Specify it explicitly.
e: file:///home/runner/work/InstallerX-Revived/InstallerX-Revived/app/src/main/java/com/rosan/installer/data/engine/signature/PendingApkSignatureAnalyzer.kt:144:53 Cannot infer type for type parameter 'T'. Specify it explicitly.
e: file:///home/runner/work/InstallerX-Revived/InstallerX-Revived/app/src/main/java/com/rosan/installer/data/engine/signature/PendingApkSignatureAnalyzer.kt:144:83 Unresolved reference 'isVerifiedUsingV32Scheme' on receiver of type 'ApkVerifier.Result'.
e: file:///home/runner/work/InstallerX-Revived/InstallerX-Revived/app/src/main/java/com/rosan/installer/data/engine/signature/PendingApkSignatureAnalyzer.kt:145:33 Unresolved reference 'classicalSignerInfo'.
e: file:///home/runner/work/InstallerX-Revived/InstallerX-Revived/app/src/main/java/com/rosan/installer/data/engine/signature/PendingApkSignatureAnalyzer.kt:145:67 Cannot infer type for type parameter 'T'. Specify it explicitly.
e: file:///home/runner/work/InstallerX-Revived/InstallerX-Revived/app/src/main/java/com/rosan/installer/data/engine/signature/PendingApkSignatureAnalyzer.kt:145:67 Cannot infer type for type parameter 'R'. Specify it explicitly.
e: file:///home/runner/work/InstallerX-Revived/InstallerX-Revived/app/src/main/java/com/rosan/installer/data/engine/signature/PendingApkSignatureAnalyzer.kt:145:73 Inapplicable candidate(s): fun format(certificate: X509Certificate): SignatureCertificateInfo
e: file:///home/runner/work/InstallerX-Revived/InstallerX-Revived/app/src/main/java/com/rosan/installer/data/engine/signature/PendingApkSignatureAnalyzer.kt:146:27 Unresolved reference 'pqcSignerInfo'.
e: file:///home/runner/work/InstallerX-Revived/InstallerX-Revived/app/src/main/java/com/rosan/installer/data/engine/signature/PendingApkSignatureAnalyzer.kt:146:55 Cannot infer type for type parameter 'T'. Specify it explicitly.
e: file:///home/runner/work/InstallerX-Revived/InstallerX-Revived/app/src/main/java/com/rosan/installer/data/engine/signature/PendingApkSignatureAnalyzer.kt:146:55 Cannot infer type for type parameter 'R'. Specify it explicitly.
e: file:///home/runner/work/InstallerX-Revived/InstallerX-Revived/app/src/main/java/com/rosan/installer/data/engine/signature/PendingApkSignatureAnalyzer.kt:146:61 Inapplicable candidate(s): fun format(certificate: X509Certificate): SignatureCertificateInfo
e: file:///home/runner/work/InstallerX-Revived/InstallerX-Revived/app/src/main/java/com/rosan/installer/data/engine/signature/PendingApkSignatureAnalyzer.kt:147:33 Cannot infer type for type parameter 'T'. Specify it explicitly.
e: file:///home/runner/work/InstallerX-Revived/InstallerX-Revived/app/src/main/java/com/rosan/installer/data/engine/signature/PendingApkSignatureAnalyzer.kt:147:33 Cannot infer type for type parameter 'R'. Specify it explicitly.
e: file:///home/runner/work/InstallerX-Revived/InstallerX-Revived/app/src/main/java/com/rosan/installer/data/engine/signature/PendingApkSignatureAnalyzer.kt:147:33 Candidate 'fun <T, R> T.let(block: (T) -> R): R' is inapplicable because of a receiver type mismatch.
e: file:///home/runner/work/InstallerX-Revived/InstallerX-Revived/app/src/main/java/com/rosan/installer/data/engine/signature/PendingApkSignatureAnalyzer.kt:147:39 None of the following candidates is applicable:

fun <T> listOf(element: T): List<T>:
  Inapplicable candidate(s): fun <T> listOf(element: T): List<T>

fun <T> listOf(vararg elements: T): List<T>:
  Inapplicable candidate(s): fun <T> listOf(vararg elements: T): List<T>

fun <T> listOf(): List<T>:
  Inapplicable candidate(s): fun <T> listOf(): List<T>
e: file:///home/runner/work/InstallerX-Revived/InstallerX-Revived/app/src/main/java/com/rosan/installer/data/engine/signature/PendingApkSignatureAnalyzer.kt:150:20 Unresolved reference 'classicalSignerInfo' on receiver of type 'Any'.
e: file:///home/runner/work/InstallerX-Revived/InstallerX-Revived/app/src/main/java/com/rosan/installer/data/engine/signature/PendingApkSignatureAnalyzer.kt:151:18 Cannot infer type for type parameter 'R'. Specify it explicitly.
e: file:///home/runner/work/InstallerX-Revived/InstallerX-Revived/app/src/main/java/com/rosan/installer/data/engine/signature/PendingApkSignatureAnalyzer.kt:151:24 Inapplicable candidate(s): fun format(certificate: X509Certificate): SignatureCertificateInfo
e: file:///home/runner/work/InstallerX-Revived/InstallerX-Revived/app/src/main/java/com/rosan/installer/data/engine/signature/PendingApkSignatureAnalyzer.kt:151:32 Cannot infer type for type parameter 'C'. Specify it explicitly.
e: file:///home/runner/work/InstallerX-Revived/InstallerX-Revived/app/src/main/java/com/rosan/installer/data/engine/signature/PendingApkSignatureAnalyzer.kt:151:32 Cannot infer type for type parameter 'R'. Specify it explicitly.
e: file:///home/runner/work/InstallerX-Revived/InstallerX-Revived/app/src/main/java/com/rosan/installer/data/engine/signature/PendingApkSignatureAnalyzer.kt:157:50 Cannot infer type for type parameter 'T'. Specify it explicitly.
e: file:///home/runner/work/InstallerX-Revived/InstallerX-Revived/app/src/main/java/com/rosan/installer/data/engine/signature/PendingApkSignatureAnalyzer.kt:157:67 Unresolved reference 'it'.
e: file:///home/runner/work/InstallerX-Revived/InstallerX-Revived/app/src/main/java/com/rosan/installer/data/engine/signature/PendingApkSignatureAnalyzer.kt:159:41 Argument type mismatch: actual type is 'Any', but 'List<SignatureCertificateInfo>' was expected.
e: file:///home/runner/work/InstallerX-Revived/InstallerX-Revived/app/src/main/java/com/rosan/installer/data/engine/signature/PendingApkSignatureAnalyzer.kt:160:47 Unresolved reference 'size'.
e: file:///home/runner/work/InstallerX-Revived/InstallerX-Revived/app/src/main/java/com/rosan/installer/data/engine/signature/PendingApkSignatureAnalyzer.kt:169:62 Unresolved reference 'classicalSignerInfo' on receiver of type 'Any'.
e: file:///home/runner/work/InstallerX-Revived/InstallerX-Revived/app/src/main/java/com/rosan/installer/data/engine/signature/PendingApkSignatureAnalyzer.kt:291:13 Unresolved reference 'isVerifiedUsingV32Scheme'.
e: file:///home/runner/work/InstallerX-Revived/InstallerX-Revived/app/src/main/java/com/rosan/installer/ui/activity/InstallerActivity.kt:449:78 Unresolved reference 'value' on receiver of type 'Flow<ProgressEntity>'.

> Task :app:compileUnstableDebugKotlin FAILED
> Task :app:copyRoomSchemas NO-SOURCE
gradle/actions: Writing build results to /home/runner/work/_temp/.gradle-actions/build-results/tests-1794063019843.json

[Incubating] Problems report is available at: file:///home/runner/work/InstallerX-Revived/InstallerX-Revived/build/reports/problems/problems-report.html

FAILURE: Build failed with an exception.

* What went wrong:
Execution failed for task ':app:compileUnstableDebugKotlin' (registered by plugin 'com.android.internal.application').
> A failure occurred while executing org.jetbrains.kotlin.compilerRunner.btapi.BuildToolsApiCompilationWork
   > Kotlin compiler: UNRESOLVED_REFERENCE
       Kotlin compiler error
         Unresolved reference 'SplitNamePolicy'.
         Location: /home/runner/work/InstallerX-Revived/InstallerX-Revived/app/src/main/java/com/rosan/installer/data/engine/executor/appinstaller/IBinderAppInstallerRepoImpl.kt:428

* Try:
> Run with --stacktrace option to get the stack trace.
> Run with --info or --debug option to get more log output.
> Run with --scan to get full insights from a Build Scan (powered by Develocity).
> Get more help at https://help.gradle.org.

Deprecated Gradle features were used in this build, making it incompatible with Gradle 10.

You can use '--warning-mode all' to show the individual deprecation warnings and determine if they come from your own scripts or plugins.

For more on this, please refer to https://docs.gradle.org/9.8.1/userguide/command_line_interface.html#sec:command_line_warnings in the Gradle documentation.

BUILD FAILED in 1m 16s
58 actionable tasks: 45 executed, 4 from cache, 9 up-to-date
```

## failing test methods
```
```
