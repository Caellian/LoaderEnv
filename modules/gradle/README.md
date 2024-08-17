# LoaderEnv Gradle Plugin

A Gradle plugin to make adding LoaderEnv to projects a breeze.

## Configuration

### Add LoaderEnv to Buildscript

<details open>
<summary><h4>Download from Maven</h4></summary>

```groovy
// file: settings.gradle
buildscript{
    repositories {
        // ...
    }
    
    dependencies {
        classpath files('libs/loaderenv-gradle.jar')
    }
}
```

</details>
<details>
<summary><h4>Use Precompiled JAR</h4></summary>

1. Download a [release](https://github.com/Caellian/LoaderEnv/releases), or build the plugin on your own.
2. Move the downloaded/built jar to desired location (e.g. `<project_root>/libs/loaderenv-gradle.jar`)
3. Add the jar to buildscript classpath:

```groovy
// file: settings.gradle
buildscript{
    repositories {
        // ...
    }
    
    dependencies {
        classpath files('libs/loaderenv-gradle.jar')
    }
}
```

</details>
<details>
<summary><h4>Build from Source</h4></summary>

1. Add a git submodule to root of your project, or simply unarchive the repository.
2. Include it in the build:

```groovy
// file: settings.gradle
includeBuild("loaderenv")
// EOF
```

</details>

### Add LoaderEnv to Project

You can enable annotations and compiler plugin for the project by adding it to corresponding project dependencies:
```groovy
// file: build.gradle
dependencies {
    loaderenv
}
```

<details>
<summary><h4>Running Without the Processor</h4></summary>

In most cases you want to run the processor. The only reason why you'd want to disable it is if you have a multi-target
build system and include some common module as sources within each target before compilation.

In any case, LoaderEnv allows you to do so:

```groovy
// file: build.gradle
dependencies {
    loaderenv(processor = false)
    // or in less readable form:
    loaderenv(false)
}
```

</details>

