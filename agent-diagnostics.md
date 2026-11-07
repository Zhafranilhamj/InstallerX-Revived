# Verification diagnostics

Commit: 2c72d3aa9288348fd8d21374f32e61d6566524c3
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
gradle/actions: Writing build results to /home/runner/work/_temp/.gradle-actions/build-results/format-1794085490412.json

[Incubating] Problems report is available at: file:///home/runner/work/InstallerX-Revived/InstallerX-Revived/build/reports/problems/problems-report.html

Deprecated Gradle features were used in this build, making it incompatible with Gradle 10.

You can use '--warning-mode all' to show the individual deprecation warnings and determine if they come from your own scripts or plugins.

For more on this, please refer to https://docs.gradle.org/9.8.1/userguide/command_line_interface.html#sec:command_line_warnings in the Gradle documentation.

BUILD SUCCESSFUL in 58s
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

> Task :app:preBuild UP-TO-DATE
> Task :app:preUnstableDebugBuild UP-TO-DATE
> Task :app-process:preBuild UP-TO-DATE
> Task :app-process:preDebugBuild UP-TO-DATE
> Task :hidden-api:preBuild UP-TO-DATE
> Task :hidden-api:preDebugBuild UP-TO-DATE
> Task :hidden-api:generateDebugResources
> Task :app:generateUnstableDebugBuildConfig
> Task :hidden-api:packageDebugResources
> Task :hidden-api:processDebugNavigationResources
> Task :app-process:extractDebugSupportedLocales
> Task :app-process:processDebugNavigationResources FROM-CACHE
> Task :app-process:generateDebugBuildConfig
> Task :app-process:generateDebugResources FROM-CACHE
> Task :app-process:packageDebugResources
> Task :app-process:compileDebugAidl
> Task :app-process:javaPreCompileDebug
> Task :hidden-api:javaPreCompileDebug FROM-CACHE
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
> Task :app:generateUnstableDebugLocaleConfig
> Task :app:generateUnstableDebugResources
> Task :app:compileUnstableDebugAidl
> Task :hidden-api:compileDebugJavaWithJavac
> Task :hidden-api:bundleLibCompileToJarDebug
> Task :app-process:compileDebugKotlin NO-SOURCE
> Task :app:packageUnstableDebugResources
> Task :app:processUnstableDebugNavigationResources
> Task :app:javaPreCompileUnstableDebug
> Task :app:parseUnstableDebugLocalResources
> Task :app:checkUnstableDebugAarMetadata
> Task :app:compileUnstableDebugNavigationResources
> Task :app:generateUnstableDebugRFile
> Task :app-process:compileDebugJavaWithJavac
> Task :app-process:processDebugJavaRes NO-SOURCE
> Task :app-process:bundleLibCompileToJarDebug
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
e: file:///home/runner/work/InstallerX-Revived/InstallerX-Revived/app/src/main/java/com/rosan/installer/framework/notification/UserActionPrompt.kt:120:77 Unresolved reference 'activityInfo' on receiver of type 'ComponentName'.
e: file:///home/runner/work/InstallerX-Revived/InstallerX-Revived/app/src/main/java/com/rosan/installer/framework/notification/UserActionPrompt.kt:122:24 Operator '!=' cannot be applied to 'BigInteger' and 'Int'.
e: file:///home/runner/work/InstallerX-Revived/InstallerX-Revived/app/src/main/java/com/rosan/installer/framework/notification/UserActionPrompt.kt:123:13 Operator '!=' cannot be applied to 'BigInteger' and 'Int'.

> Task :app:compileUnstableDebugKotlin FAILED
> Task :app:copyRoomSchemas NO-SOURCE
gradle/actions: Writing build results to /home/runner/work/_temp/.gradle-actions/build-results/tests-1794085543939.json

[Incubating] Problems report is available at: file:///home/runner/work/InstallerX-Revived/InstallerX-Revived/build/reports/problems/problems-report.html

FAILURE: Build failed with an exception.

* What went wrong:
Execution failed for task ':app:compileUnstableDebugKotlin' (registered by plugin 'com.android.internal.application').
> A failure occurred while executing org.jetbrains.kotlin.compilerRunner.btapi.BuildToolsApiCompilationWork
   > Kotlin compiler: UNRESOLVED_REFERENCE
       Kotlin compiler error
         Unresolved reference 'activityInfo' on receiver of type 'ComponentName'.
         Location: /home/runner/work/InstallerX-Revived/InstallerX-Revived/app/src/main/java/com/rosan/installer/framework/notification/UserActionPrompt.kt:120

* Try:
> Run with --stacktrace option to get the stack trace.
> Run with --info or --debug option to get more log output.
> Run with --scan to get full insights from a Build Scan (powered by Develocity).
> Get more help at https://help.gradle.org.

Deprecated Gradle features were used in this build, making it incompatible with Gradle 10.

You can use '--warning-mode all' to show the individual deprecation warnings and determine if they come from your own scripts or plugins.

For more on this, please refer to https://docs.gradle.org/9.8.1/userguide/command_line_interface.html#sec:command_line_warnings in the Gradle documentation.

BUILD FAILED in 51s
58 actionable tasks: 45 executed, 4 from cache, 9 up-to-date
```

## failing test methods
```
```

## lint categories
```
```

## lint NewApi findings
```
```
