# Verification diagnostics

Commit: 2d75749520c7bd6b3cb3089e84e4987e6d8e649f
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
gradle/actions: Writing build results to /home/runner/work/_temp/.gradle-actions/build-results/format-1794068970013.json

[Incubating] Problems report is available at: file:///home/runner/work/InstallerX-Revived/InstallerX-Revived/build/reports/problems/problems-report.html

Deprecated Gradle features were used in this build, making it incompatible with Gradle 10.

You can use '--warning-mode all' to show the individual deprecation warnings and determine if they come from your own scripts or plugins.

For more on this, please refer to https://docs.gradle.org/9.8.1/userguide/command_line_interface.html#sec:command_line_warnings in the Gradle documentation.

BUILD SUCCESSFUL in 1m 27s
13 actionable tasks: 9 executed, 4 from cache
```

## tests.log (tail)
```
> Task :hidden-api:bundleLibCompileToJarRelease
> Task :hidden-api:bundleLibRuntimeToJarRelease
> Task :hidden-api:processReleaseJavaRes NO-SOURCE
> Task :hidden-api:createFullJarRelease
> Task :hidden-api:extractProguardFiles
> Task :hidden-api:prepareLintJarForPublish
> Task :hidden-api:checkReleaseAarMetadata
> Task :hidden-api:processReleaseManifest
> Task :hidden-api:extractDeepLinksForAarRelease FROM-CACHE
> Task :app:packageUnstableDebugResources
> Task :app:javaPreCompileUnstableDebug
> Task :app:parseUnstableDebugLocalResources
> Task :app-process:compileDebugJavaWithJavac
> Task :hidden-api:generateReleaseLintModel
> Task :app-process:compileReleaseKotlin NO-SOURCE
> Task :app-process:bundleLibCompileToJarDebug
> Task :app:checkUnstableDebugAarMetadata
> Task :app:compileUnstableDebugNavigationResources
> Task :hidden-api:extractReleaseAnnotations
> Task :hidden-api:generateReleaseAssets UP-TO-DATE
> Task :app:generateUnstableDebugRFile
> Task :hidden-api:mergeReleaseAssets
> Task :hidden-api:mergeReleaseGeneratedProguardFiles
> Task :hidden-api:mergeReleaseConsumerProguardFiles
> Task :hidden-api:prepareReleaseArtProfile
> Task :hidden-api:mergeReleaseJavaResource
> Task :hidden-api:syncReleaseLibJars
> Task :hidden-api:writeReleaseAarMetadata
> Task :hidden-api:bundleReleaseLocalLintAar
> Task :hidden-api:writeReleaseLintModelMetadata
> Task :app-process:compileReleaseJavaWithJavac
> Task :app-process:bundleLibCompileToJarRelease
> Task :app-process:bundleLibRuntimeToJarDebug
> Task :app-process:processDebugJavaRes NO-SOURCE
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
> Task :app:processUnstableDebugManifestForPackage
> Task :app:mapPreviewReleaseSourceSetPaths
> Task :app:generatePreviewReleaseLocaleConfig FROM-CACHE
> Task :app:generatePreviewReleaseResources FROM-CACHE
> Task :app:processUnstableDebugResources
> Task :app:packagePreviewReleaseResources
> Task :app:processPreviewReleaseNavigationResources FROM-CACHE
> Task :app:javaPreCompilePreviewRelease FROM-CACHE
> Task :app:preUnstableDebugUnitTestBuild UP-TO-DATE
> Task :app:javaPreCompileUnstableDebugUnitTest FROM-CACHE
> Task :app:extractProguardFiles
> Task :app:extractDeepLinksPreviewRelease FROM-CACHE
> Task :app:parsePreviewReleaseLocalResources
> Task :app:processPreviewReleaseMainManifest
> Task :app:generatePreviewReleaseRFile FROM-CACHE
> Task :hidden-api:lintAnalyzeRelease
> Task :app-process:lintAnalyzeRelease
> Task :app:kspUnstableDebugKotlin
> Task :app:kspPreviewReleaseKotlin
> Task :app:compilePreviewReleaseKotlin
> Task :app:compileUnstableDebugKotlin

> Task :app:compilePreviewReleaseKotlin
w: file:///home/runner/work/InstallerX-Revived/InstallerX-Revived/app/src/main/java/com/rosan/installer/framework/activity/BiometricsAuthenticationActivity.kt:63:47 'val authResultChannel: Channel<Boolean>' is deprecated. Use channelFor(sessionId).
w: file:///home/runner/work/InstallerX-Revived/InstallerX-Revived/app/src/main/java/com/rosan/installer/ui/page/main/tools/QrScannerPage.kt:107:8 Annotation 'androidx.camera.core.ExperimentalGetImage' is not annotated with '@RequiresOptIn'. '@OptIn' has no effect.

> Task :app:compileUnstableDebugKotlin
w: file:///home/runner/work/InstallerX-Revived/InstallerX-Revived/app/src/main/java/com/rosan/installer/framework/activity/BiometricsAuthenticationActivity.kt:63:47 'val authResultChannel: Channel<Boolean>' is deprecated. Use channelFor(sessionId).
w: file:///home/runner/work/InstallerX-Revived/InstallerX-Revived/app/src/main/java/com/rosan/installer/ui/page/main/tools/QrScannerPage.kt:107:8 Annotation 'androidx.camera.core.ExperimentalGetImage' is not annotated with '@RequiresOptIn'. '@OptIn' has no effect.

> Task :app:compilePreviewReleaseJavaWithJavac
> Task :app:compileUnstableDebugJavaWithJavac
> Task :app:processUnstableDebugJavaRes
> Task :app:bundleUnstableDebugClassesToRuntimeJar
> Task :app:bundleUnstableDebugClassesToCompileJar
> Task :app:generatePreviewReleaseLintReportModel
> Task :app:kspUnstableDebugUnitTestKotlin

> Task :app:compileUnstableDebugUnitTestKotlin
w: file:///home/runner/work/InstallerX-Revived/InstallerX-Revived/app/src/test/java/com/rosan/installer/data/share/LocalHttpServerTest.kt:82:49 Unnecessary non-null assertion (!!) on a non-null receiver of type 'String'.

> Task :app:compileUnstableDebugUnitTestJavaWithJavac NO-SOURCE
> Task :app:copyRoomSchemas NO-SOURCE
> Task :app:processUnstableDebugUnitTestJavaRes
> Task :app:lintAnalyzePreviewRelease
> Task :app:testUnstableDebugUnitTest

> Task :app:lintReportPreviewRelease
Wrote HTML report to file:///home/runner/work/InstallerX-Revived/InstallerX-Revived/app/build/reports/lint-results-PreviewRelease.html
Wrote SARIF report to file:///home/runner/work/InstallerX-Revived/InstallerX-Revived/app/build/reports/lint-results-PreviewRelease.sarif
Lint found 1170 errors, 95 warnings and 5 hints. First failure:
/home/runner/work/InstallerX-Revived/InstallerX-Revived/app/src/main/java/com/rosan/installer/ui/page/main/tools/QrScannerPage.kt:109: Error: This declaration is opt-in and its usage should be marked with @androidx.camera.core.ExperimentalGetImage or @OptIn(markerClass = androidx.camera.core.ExperimentalGetImage.class) [UnsafeOptInUsageError from androidx.annotation.experimental]
This issue has been automatically fixed.
    val image = proxy.image ?: return null
                      ~~~~~


> Task :app:lintPreviewRelease FAILED
Lint found 1170 errors, 95 warnings, 5 hints. First failure:

/home/runner/work/InstallerX-Revived/InstallerX-Revived/app/src/main/java/com/rosan/installer/ui/page/main/tools/QrScannerPage.kt:109: Error: This declaration is opt-in and its usage should be marked with @androidx.camera.core.ExperimentalGetImage or @OptIn(markerClass = androidx.camera.core.ExperimentalGetImage.class) [UnsafeOptInUsageError from androidx.annotation.experimental]
    val image = proxy.image ?: return null
                      ~~~~~

   Explanation for issues of type "UnsafeOptInUsageError":
   This API has been flagged as opt-in with error-level severity.

   Any declaration annotated with this marker is considered part of an
   unstable or
   otherwise non-standard API surface and its call sites should accept the
   opt-in
   aspect of it by using the @OptIn annotation, using the marker annotation
   --
   effectively causing further propagation of the opt-in aspect -- or
   configuring
   the UnsafeOptInUsageError check's options for project-wide opt-in.

   To configure project-wide opt-in, specify the opt-in option value in
   lint.xml
   as a comma-delimited list of opted-in annotations:

   <lint>
       <issue id="UnsafeOptInUsageError">
           <option name="opt-in" value="com.foo.ExperimentalBarAnnotation" />
       </issue>
   </lint>

   Vendor: Android Open Source Project
   Identifier: androidx.annotation.experimental
   Feedback: https://issuetracker.google.com/issues/new?component=459778


The full lint text report is located at:
  /home/runner/work/InstallerX-Revived/InstallerX-Revived/app/build/intermediates/lint_intermediate_text_report/PreviewRelease/lintReportPreviewRelease/lint-results-PreviewRelease.txt
gradle/actions: Writing build results to /home/runner/work/_temp/.gradle-actions/build-results/tests-1794069047568.json

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
  
  Lint found 1170 errors, 95 warnings, 5 hints. First failure:
  
  /home/runner/work/InstallerX-Revived/InstallerX-Revived/app/src/main/java/com/rosan/installer/ui/page/main/tools/QrScannerPage.kt:109: Error: This declaration is opt-in and its usage should be marked with @androidx.camera.core.ExperimentalGetImage or @OptIn(markerClass = androidx.camera.core.ExperimentalGetImage.class) [UnsafeOptInUsageError from androidx.annotation.experimental]
      val image = proxy.image ?: return null
                        ~~~~~
  
     Explanation for issues of type "UnsafeOptInUsageError":
     This API has been flagged as opt-in with error-level severity.
  
     Any declaration annotated with this marker is considered part of an
     unstable or
     otherwise non-standard API surface and its call sites should accept the
     opt-in
     aspect of it by using the @OptIn annotation, using the marker annotation
     --
     effectively causing further propagation of the opt-in aspect -- or
     configuring
     the UnsafeOptInUsageError check's options for project-wide opt-in.
  
     To configure project-wide opt-in, specify the opt-in option value in
     lint.xml
     as a comma-delimited list of opted-in annotations:
  
     <lint>
         <issue id="UnsafeOptInUsageError">
             <option name="opt-in" value="com.foo.ExperimentalBarAnnotation" />
         </issue>
     </lint>
  
     Vendor: Android Open Source Project
     Identifier: androidx.annotation.experimental
     Feedback: https://issuetracker.google.com/issues/new?component=459778
  
  
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

BUILD FAILED in 5m 36s
146 actionable tasks: 111 executed, 26 from cache, 9 up-to-date
```

## failing test methods
```
```

## lint categories
```
   1156 [MissingTranslation]
     21 [Untranslatable]
     12 [InlinedApi]
     10 [PluralsCandidate]
     10 [GradleDependency]
      9 [UseKtx]
      7 [UnusedResources]
      5 [NewerVersionAvailable]
      5 [AndroidGradlePluginVersion]
      4 [Typos]
      4 [TypographyDashes]
      3 [TypographyEllipsis]
      1 [SdCardPath]
      1 [InsecureBaseConfiguration]
      1 [EmptySuperCall]
      1 [AppLinkWarning]
```

## lint NewApi findings
```
```
