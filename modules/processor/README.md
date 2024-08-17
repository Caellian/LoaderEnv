# LoaderEnv Processor

A java compiler plugin that conditionally disables compilation of annotated elements based on plugin arguments or
system properties.

Ideally the build system should provide this information like the [gradle plugin](../gradle) does.

## Running Manually

[LoaderEnv Gradle plugin](../gradle) handles all configuration with just one word in `dependencies` block.
```sh
-Xplugin:LoaderEnvPlugin Mod;Loader;Names
--add-exports=jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED
# a bunch more --add-exports for compiler internals, see processor build.gradle.kts
```
