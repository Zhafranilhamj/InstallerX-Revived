# Verification diagnostics

Commit: f654d8e81aa21de8ac8b9f50ca167459faac09bf
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
gradle/actions: Writing build results to /home/runner/work/_temp/.gradle-actions/build-results/format-1794068005179.json

[Incubating] Problems report is available at: file:///home/runner/work/InstallerX-Revived/InstallerX-Revived/build/reports/problems/problems-report.html

Deprecated Gradle features were used in this build, making it incompatible with Gradle 10.

You can use '--warning-mode all' to show the individual deprecation warnings and determine if they come from your own scripts or plugins.

For more on this, please refer to https://docs.gradle.org/9.8.1/userguide/command_line_interface.html#sec:command_line_warnings in the Gradle documentation.

BUILD SUCCESSFUL in 1m 7s
13 actionable tasks: 9 executed, 4 from cache
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
> Task :app:prePreviewReleaseBuild UP-TO-DATE
> Task :app-process:preReleaseBuild UP-TO-DATE
> Task :hidden-api:preReleaseBuild UP-TO-DATE
> Task :hidden-api:generateReleaseResources
> Task :app:generatePreviewReleaseBuildConfig
> Task :hidden-api:packageReleaseResources
> Task :hidden-api:processReleaseNavigationResources
> Task :app-process:compileReleaseAidl
> Task :app-process:extractReleaseSupportedLocales
> Task :app-process:processReleaseNavigationResources FROM-CACHE
> Task :app-process:generateReleaseBuildConfig
> Task :app-process:generateReleaseResources FROM-CACHE
> Task :app-process:packageReleaseResources
> Task :hidden-api:javaPreCompileRelease
> Task :app-process:javaPreCompileRelease FROM-CACHE
> Task :app-process:extractDeepLinksRelease
> Task :app-process:parseReleaseLocalResources
> Task :hidden-api:parseReleaseLocalResources
> Task :app-process:generateReleaseRFile
> Task :hidden-api:generateReleaseRFile
> Task :hidden-api:compileReleaseKotlin NO-SOURCE
> Task :app-process:extractProguardFiles
> Task :app-process:processReleaseManifest
> Task :app-process:prepareLintJarForPublish
> Task :app-process:checkReleaseAarMetadata
> Task :app-process:extractDeepLinksForAarRelease
> Task :app-process:generateReleaseAssets UP-TO-DATE
> Task :app-process:mergeReleaseAssets
> Task :app-process:prepareReleaseArtProfile
> Task :app-process:writeReleaseAarMetadata
> Task :app-process:writeReleaseLintModelMetadata
> Task :app:prepareLibraryDefinitionsPreviewRelease
> Task :app:extractPreviewReleaseSupportedLocales
> Task :app:mapPreviewReleaseSourceSetPaths
> Task :app:generatePreviewReleaseResources
> Task :app:generatePreviewReleaseLocaleConfig
> Task :app:compilePreviewReleaseAidl
> Task :app:processPreviewReleaseNavigationResources
> Task :hidden-api:compileReleaseJavaWithJavac
> Task :hidden-api:processReleaseJavaRes NO-SOURCE
> Task :hidden-api:extractProguardFiles
> Task :hidden-api:processReleaseManifest
> Task :hidden-api:bundleLibRuntimeToJarRelease
> Task :hidden-api:bundleLibCompileToJarRelease
> Task :app:packagePreviewReleaseResources
> Task :app:extractProguardFiles
> Task :app:javaPreCompilePreviewRelease
> Task :app:extractDeepLinksPreviewRelease
> Task :app:parsePreviewReleaseLocalResources
> Task :hidden-api:generateReleaseLintModel
> Task :app-process:compileReleaseKotlin NO-SOURCE
> Task :hidden-api:createFullJarRelease
> Task :hidden-api:prepareLintJarForPublish
> Task :hidden-api:checkReleaseAarMetadata
> Task :hidden-api:extractDeepLinksForAarRelease FROM-CACHE
> Task :hidden-api:extractReleaseAnnotations
> Task :hidden-api:generateReleaseAssets UP-TO-DATE
> Task :hidden-api:mergeReleaseAssets
> Task :hidden-api:mergeReleaseGeneratedProguardFiles
> Task :hidden-api:mergeReleaseConsumerProguardFiles
> Task :hidden-api:prepareReleaseArtProfile
> Task :hidden-api:writeReleaseAarMetadata
> Task :app:processPreviewReleaseMainManifest
> Task :hidden-api:mergeReleaseJavaResource
> Task :hidden-api:syncReleaseLibJars
> Task :app:generatePreviewReleaseRFile
> Task :hidden-api:bundleReleaseLocalLintAar
> Task :hidden-api:writeReleaseLintModelMetadata
> Task :app-process:compileReleaseJavaWithJavac
> Task :app-process:bundleLibCompileToJarRelease
> Task :app-process:bundleLibRuntimeToJarRelease
> Task :app-process:processReleaseJavaRes NO-SOURCE
> Task :app-process:createFullJarRelease
> Task :app-process:generateReleaseLintModel
> Task :app-process:extractReleaseAnnotations
> Task :app-process:mergeReleaseGeneratedProguardFiles
> Task :app-process:mergeReleaseConsumerProguardFiles
> Task :app-process:mergeReleaseJavaResource
> Task :app-process:syncReleaseLibJars
> Task :app-process:bundleReleaseLocalLintAar
> Task :hidden-api:lintAnalyzeRelease
> Task :app-process:lintAnalyzeRelease
> Task :app:kspPreviewReleaseKotlin
> Task :app:compilePreviewReleaseKotlin
w: file:///home/runner/work/InstallerX-Revived/InstallerX-Revived/app/src/main/java/com/rosan/installer/framework/activity/BiometricsAuthenticationActivity.kt:63:47 'val authResultChannel: Channel<Boolean>' is deprecated. Use channelFor(sessionId).
w: file:///home/runner/work/InstallerX-Revived/InstallerX-Revived/app/src/main/java/com/rosan/installer/ui/page/main/tools/QrScannerPage.kt:107:8 Annotation 'androidx.camera.core.ExperimentalGetImage' is not annotated with '@RequiresOptIn'. '@OptIn' has no effect.

> Task :app:compilePreviewReleaseJavaWithJavac
> Task :app:copyRoomSchemas NO-SOURCE
> Task :app:generatePreviewReleaseLintReportModel
> Task :app:lintAnalyzePreviewRelease

> Task :app:lintReportPreviewRelease
Wrote HTML report to file:///home/runner/work/InstallerX-Revived/InstallerX-Revived/app/build/reports/lint-results-PreviewRelease.html
Wrote SARIF report to file:///home/runner/work/InstallerX-Revived/InstallerX-Revived/app/build/reports/lint-results-PreviewRelease.sarif
Lint found 1172 errors, 95 warnings and 5 hints. First failure:
/home/runner/work/InstallerX-Revived/InstallerX-Revived/app/src/main/java/com/rosan/installer/framework/notification/UserActionPrompt.kt:85: Error: Call requires permission which may be rejected by user: code should explicitly check to see if permission is available (with checkPermission) or explicitly handle a potential SecurityException [MissingPermission]
        manager.notify(notificationId, notification)
        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~


> Task :app:lintPreviewRelease FAILED
Lint found 1172 errors, 95 warnings, 5 hints. First failure:

/home/runner/work/InstallerX-Revived/InstallerX-Revived/app/src/main/java/com/rosan/installer/framework/notification/UserActionPrompt.kt:85: Error: Call requires permission which may be rejected by user: code should explicitly check to see if permission is available (with checkPermission) or explicitly handle a potential SecurityException [MissingPermission]
        manager.notify(notificationId, notification)
        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

   Explanation for issues of type "MissingPermission":
   This check scans through your code and libraries and looks at the APIs
   being used, and checks this against the set of permissions required to
   access those APIs. If the code using those APIs is called at runtime, then
   the program will crash.

   Furthermore, for permissions that are revocable (with targetSdkVersion 23),
   client code must also be prepared to handle the calls throwing an exception
   if the user rejects the request for permission at runtime.


The full lint text report is located at:
  /home/runner/work/InstallerX-Revived/InstallerX-Revived/app/build/intermediates/lint_intermediate_text_report/PreviewRelease/lintReportPreviewRelease/lint-results-PreviewRelease.txt
gradle/actions: Writing build results to /home/runner/work/_temp/.gradle-actions/build-results/tests-1794068066981.json

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
  
  Lint found 1172 errors, 95 warnings, 5 hints. First failure:
  
  /home/runner/work/InstallerX-Revived/InstallerX-Revived/app/src/main/java/com/rosan/installer/framework/notification/UserActionPrompt.kt:85: Error: Call requires permission which may be rejected by user: code should explicitly check to see if permission is available (with checkPermission) or explicitly handle a potential SecurityException [MissingPermission]
          manager.notify(notificationId, notification)
          ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
  
     Explanation for issues of type "MissingPermission":
     This check scans through your code and libraries and looks at the APIs
     being used, and checks this against the set of permissions required to
     access those APIs. If the code using those APIs is called at runtime, then
     the program will crash.
  
     Furthermore, for permissions that are revocable (with targetSdkVersion 23),
     client code must also be prepared to handle the calls throwing an exception
     if the user rejects the request for permission at runtime.
  
  
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

BUILD FAILED in 3m 38s
89 actionable tasks: 76 executed, 4 from cache, 9 up-to-date
```

## failing test methods
```
```
