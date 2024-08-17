plugins {
    `java-library`
    signing
}

version = "0.0.1"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

packageInfo {
    name = "LoaderEnv"
    description = "Annotation library that allows specifying supported mod loaders for class elements"
}
