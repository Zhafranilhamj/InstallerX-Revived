# Verification diagnostics

Commit: d8e010603b81cd6fd3b7486d4d5ab5c75e7e2727
Formatting outcome: success
Tests outcome: failure

## apksig probe
```
apksig 9.4.1 -> HTTP 200
apksig 9.5.0 -> HTTP 200
apksig 9.6.0 -> HTTP 404
apksig 10.0.0 -> HTTP 404
```

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
> Task :spotlessKotlinApply
> Task :spotlessKotlinGradle
> Task :spotlessKotlinGradleApply
> Task :spotlessApply
gradle/actions: Writing build results to /home/runner/work/_temp/.gradle-actions/build-results/format-1794068468767.json

[Incubating] Problems report is available at: file:///home/runner/work/InstallerX-Revived/InstallerX-Revived/build/reports/problems/problems-report.html

Deprecated Gradle features were used in this build, making it incompatible with Gradle 10.

You can use '--warning-mode all' to show the individual deprecation warnings and determine if they come from your own scripts or plugins.

For more on this, please refer to https://docs.gradle.org/9.8.1/userguide/command_line_interface.html#sec:command_line_warnings in the Gradle documentation.

BUILD SUCCESSFUL in 1m 1s
13 actionable tasks: 9 executed, 4 from cache
```

## tests.log (tail)
```
> Task :hidden-api:processReleaseNavigationResources FROM-CACHE
> Task :hidden-api:parseReleaseLocalResources FROM-CACHE
> Task :hidden-api:generateReleaseRFile FROM-CACHE
> Task :hidden-api:compileReleaseKotlin NO-SOURCE
> Task :hidden-api:javaPreCompileRelease FROM-CACHE
> Task :hidden-api:bundleLibCompileToJarDebug
> Task :hidden-api:compileReleaseJavaWithJavac FROM-CACHE
> Task :app-process:compileDebugKotlin NO-SOURCE
> Task :hidden-api:bundleLibCompileToJarRelease
> Task :hidden-api:processReleaseJavaRes NO-SOURCE
> Task :hidden-api:extractProguardFiles
> Task :hidden-api:bundleLibRuntimeToJarRelease
> Task :hidden-api:createFullJarRelease
> Task :hidden-api:prepareLintJarForPublish
> Task :hidden-api:processReleaseManifest
> Task :hidden-api:checkReleaseAarMetadata
> Task :app:packageUnstableDebugResources
> Task :app:processUnstableDebugNavigationResources
> Task :app:javaPreCompileUnstableDebug
> Task :app:parseUnstableDebugLocalResources
> Task :app-process:compileDebugJavaWithJavac
> Task :app-process:bundleLibCompileToJarDebug
> Task :app-process:compileReleaseKotlin NO-SOURCE
> Task :app:checkUnstableDebugAarMetadata
> Task :hidden-api:generateReleaseLintModel
> Task :app:compileUnstableDebugNavigationResources
> Task :app:generateUnstableDebugRFile
> Task :hidden-api:extractDeepLinksForAarRelease FROM-CACHE
> Task :hidden-api:extractReleaseAnnotations
> Task :hidden-api:generateReleaseAssets UP-TO-DATE
> Task :hidden-api:mergeReleaseAssets
> Task :hidden-api:mergeReleaseGeneratedProguardFiles
> Task :hidden-api:mergeReleaseConsumerProguardFiles
> Task :hidden-api:prepareReleaseArtProfile
> Task :hidden-api:writeReleaseAarMetadata
> Task :hidden-api:mergeReleaseJavaResource
> Task :hidden-api:syncReleaseLibJars
> Task :hidden-api:bundleReleaseLocalLintAar
> Task :hidden-api:writeReleaseLintModelMetadata
> Task :app-process:compileReleaseJavaWithJavac
> Task :app-process:bundleLibCompileToJarRelease
> Task :app-process:processDebugJavaRes NO-SOURCE
> Task :app-process:bundleLibRuntimeToJarDebug
> Task :app-process:processReleaseJavaRes NO-SOURCE
> Task :app-process:bundleLibRuntimeToJarRelease
> Task :app-process:generateReleaseLintModel
> Task :app-process:createFullJarRelease
> Task :app-process:extractReleaseAnnotations
> Task :app-process:mergeReleaseGeneratedProguardFiles
> Task :app-process:mergeReleaseConsumerProguardFiles
> Task :app-process:mergeReleaseJavaResource
> Task :app-process:syncReleaseLibJars
> Task :app-process:bundleReleaseLocalLintAar
> Task :app:mergeUnstableDebugResources
> Task :app:createUnstableDebugCompatibleScreenManifests
> Task :app:extractDeepLinksUnstableDebug
> Task :app:processUnstableDebugMainManifest
> Task :app:processUnstableDebugManifest
> Task :app:prePreviewReleaseBuild UP-TO-DATE
> Task :app:compilePreviewReleaseAidl FROM-CACHE
> Task :app:generatePreviewReleaseBuildConfig
> Task :app:prepareLibraryDefinitionsPreviewRelease
> Task :app:extractPreviewReleaseSupportedLocales
> Task :app:mapPreviewReleaseSourceSetPaths
> Task :app:generatePreviewReleaseLocaleConfig FROM-CACHE
> Task :app:generatePreviewReleaseResources FROM-CACHE
> Task :app:packagePreviewReleaseResources
> Task :app:processPreviewReleaseNavigationResources FROM-CACHE
> Task :app:processUnstableDebugManifestForPackage
> Task :app:parsePreviewReleaseLocalResources
> Task :app:generatePreviewReleaseRFile FROM-CACHE
> Task :app:processUnstableDebugResources
> Task :app:javaPreCompilePreviewRelease FROM-CACHE
> Task :app:preUnstableDebugUnitTestBuild UP-TO-DATE
> Task :app:javaPreCompileUnstableDebugUnitTest FROM-CACHE
> Task :app:extractProguardFiles
> Task :app:extractDeepLinksPreviewRelease FROM-CACHE
> Task :app:processPreviewReleaseMainManifest
> Task :hidden-api:lintAnalyzeRelease
> Task :app-process:lintAnalyzeRelease
> Task :app:kspPreviewReleaseKotlin
> Task :app:kspUnstableDebugKotlin
> Task :app:compilePreviewReleaseKotlin
> Task :app:compileUnstableDebugKotlin
w: file:///home/runner/work/InstallerX-Revived/InstallerX-Revived/app/src/main/java/com/rosan/installer/framework/activity/BiometricsAuthenticationActivity.kt:63:47 'val authResultChannel: Channel<Boolean>' is deprecated. Use channelFor(sessionId).
w: file:///home/runner/work/InstallerX-Revived/InstallerX-Revived/app/src/main/java/com/rosan/installer/ui/page/main/tools/QrScannerPage.kt:107:8 Annotation 'androidx.camera.core.ExperimentalGetImage' is not annotated with '@RequiresOptIn'. '@OptIn' has no effect.

> Task :app:compilePreviewReleaseKotlin
w: file:///home/runner/work/InstallerX-Revived/InstallerX-Revived/app/src/main/java/com/rosan/installer/framework/activity/BiometricsAuthenticationActivity.kt:63:47 'val authResultChannel: Channel<Boolean>' is deprecated. Use channelFor(sessionId).
w: file:///home/runner/work/InstallerX-Revived/InstallerX-Revived/app/src/main/java/com/rosan/installer/ui/page/main/tools/QrScannerPage.kt:107:8 Annotation 'androidx.camera.core.ExperimentalGetImage' is not annotated with '@RequiresOptIn'. '@OptIn' has no effect.

> Task :app:compileUnstableDebugJavaWithJavac
> Task :app:bundleUnstableDebugClassesToRuntimeJar
> Task :app:compilePreviewReleaseJavaWithJavac
> Task :app:processUnstableDebugJavaRes
> Task :app:bundleUnstableDebugClassesToCompileJar
> Task :app:generatePreviewReleaseLintReportModel
> Task :app:kspUnstableDebugUnitTestKotlin

> Task :app:compileUnstableDebugUnitTestKotlin
w: file:///home/runner/work/InstallerX-Revived/InstallerX-Revived/app/src/test/java/com/rosan/installer/data/share/LocalHttpServerTest.kt:82:49 Unnecessary non-null assertion (!!) on a non-null receiver of type 'String'.

> Task :app:compileUnstableDebugUnitTestJavaWithJavac NO-SOURCE
> Task :app:copyRoomSchemas NO-SOURCE
> Task :app:processUnstableDebugUnitTestJavaRes
> Task :app:testUnstableDebugUnitTest
> Task :app:lintAnalyzePreviewRelease

> Task :app:lintReportPreviewRelease
Wrote HTML report to file:///home/runner/work/InstallerX-Revived/InstallerX-Revived/app/build/reports/lint-results-PreviewRelease.html
Wrote SARIF report to file:///home/runner/work/InstallerX-Revived/InstallerX-Revived/app/build/reports/lint-results-PreviewRelease.sarif
Lint found 1171 errors, 95 warnings and 5 hints. First failure:
/home/runner/work/InstallerX-Revived/InstallerX-Revived/app/src/main/java/com/rosan/installer/framework/packageupdate/SelfUpdateRecoveryManager.kt:228: Error: Call requires API level 28 (current min is 26): android.content.pm.PackageInfo#getLongVersionCode [NewApi]
                    actualNewVersionCode = packageInfo?.longVersionCode ?: history.newVersionCode,
                                                        ~~~~~~~~~~~~~~~


> Task :app:lintPreviewRelease FAILED
Lint found 1171 errors, 95 warnings, 5 hints. First failure:

/home/runner/work/InstallerX-Revived/InstallerX-Revived/app/src/main/java/com/rosan/installer/framework/packageupdate/SelfUpdateRecoveryManager.kt:228: Error: Call requires API level 28 (current min is 26): android.content.pm.PackageInfo#getLongVersionCode [NewApi]
                    actualNewVersionCode = packageInfo?.longVersionCode ?: history.newVersionCode,
                                                        ~~~~~~~~~~~~~~~

   Explanation for issues of type "NewApi":
   This check scans through all the Android API calls in the application and
   warns about any calls that are not available on all versions targeted by
   this application (according to its minimum SDK attribute in the manifest).

   If you really want to use this API and don't need to support older devices
   just set the minSdkVersion in your build.gradle or AndroidManifest.xml
   files.

   If your code is deliberately accessing newer APIs, and you have ensured
   (e.g. with conditional execution) that this code will only ever be called
   on a supported platform, then you can annotate your class or method with
   the @TargetApi annotation specifying the local minimum SDK to apply, such
   as @TargetApi(11), such that this check considers 11 rather than your
   manifest file's minimum SDK as the required API level.

   If you are deliberately setting android: attributes in style definitions,
   make sure you place this in a values-vNN folder in order to avoid running
   into runtime conflicts on certain devices where manufacturers have added
   custom attributes whose ids conflict with the new ones on later platforms.

   Similarly, you can use tools:targetApi="11" in an XML file to indicate that
   the element will only be inflated in an adequate context.


The full lint text report is located at:
  /home/runner/work/InstallerX-Revived/InstallerX-Revived/app/build/intermediates/lint_intermediate_text_report/PreviewRelease/lintReportPreviewRelease/lint-results-PreviewRelease.txt
gradle/actions: Writing build results to /home/runner/work/_temp/.gradle-actions/build-results/tests-1794068527343.json

[Incubating] Problems report is available at: file:///home/runner/work/InstallerX-Revived/InstallerX-Revived/build/reports/problems/problems-report.html

FAILURE: Build failed with an exception.

* What went wrong:
Execution failed for task ':app:lintPreviewRelease' (registered by plugin 'com.android.internal.application').
> Lint found errors in the project; aborting build.
  
  Fix the issues identified by lint, or create a baseline to see only new errors.
  To create a baseline, run `gradlew updateLintBaseline` after adding the following to the module's build.gradle file:
  ```
  android {
      lint {
          baseline = file("lint-baseline.xml")
      }
  }
  ```
  For more details, see https://developer.android.com/studio/write/lint#snapshot
  
  Lint found 1171 errors, 95 warnings, 5 hints. First failure:
  
  /home/runner/work/InstallerX-Revived/InstallerX-Revived/app/src/main/java/com/rosan/installer/framework/packageupdate/SelfUpdateRecoveryManager.kt:228: Error: Call requires API level 28 (current min is 26): android.content.pm.PackageInfo#getLongVersionCode [NewApi]
                      actualNewVersionCode = packageInfo?.longVersionCode ?: history.newVersionCode,
                                                          ~~~~~~~~~~~~~~~
  
     Explanation for issues of type "NewApi":
     This check scans through all the Android API calls in the application and
     warns about any calls that are not available on all versions targeted by
     this application (according to its minimum SDK attribute in the manifest).
  
     If you really want to use this API and don't need to support older devices
     just set the minSdkVersion in your build.gradle or AndroidManifest.xml
     files.
  
     If your code is deliberately accessing newer APIs, and you have ensured
     (e.g. with conditional execution) that this code will only ever be called
     on a supported platform, then you can annotate your class or method with
     the @TargetApi annotation specifying the local minimum SDK to apply, such
     as @TargetApi(11), such that this check considers 11 rather than your
     manifest file's minimum SDK as the required API level.
  
     If you are deliberately setting android: attributes in style definitions,
     make sure you place this in a values-vNN folder in order to avoid running
     into runtime conflicts on certain devices where manufacturers have added
     custom attributes whose ids conflict with the new ones on later platforms.
  
     Similarly, you can use tools:targetApi="11" in an XML file to indicate that
     the element will only be inflated in an adequate context.
  
  
  The full lint text report is located at:
    /home/runner/work/InstallerX-Revived/InstallerX-Revived/app/build/intermediates/lint_intermediate_text_report/PreviewRelease/lintReportPreviewRelease/lint-results-PreviewRelease.txt

* Try:
> Run with --stacktrace option to get the stack trace.
> Run with --info or --debug option to get more log output.
> Run with --scan to get full insights from a Build Scan (powered by Develocity).
> Get more help at https://help.gradle.org.

Deprecated Gradle features were used in this build, making it incompatible with Gradle 10.

You can use '--warning-mode all' to show the individual deprecation warnings and determine if they come from your own scripts or plugins.

For more on this, please refer to https://docs.gradle.org/9.8.1/userguide/command_line_interface.html#sec:command_line_warnings in the Gradle documentation.

BUILD FAILED in 3m 54s
146 actionable tasks: 110 executed, 27 from cache, 9 up-to-date
```

## failing test methods
```
```
