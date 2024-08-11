# ModEnv

This is a very simple Java compiler plugin that (optionally) integrates into Gradle build environment to allow
conditional inclusion and exclusion of classes/fields/methods based on the mod loader that's used while compiling
a project. See [Usage](#Usage) section for examples.

Basically it mimics Rust conditional compilation for mod loaders (`#[cfg(mod_loader = "fabric")]`).

# Configuration

Until I publish the plugin, you can consume it either as a jar or a submodule (build from source).

## 1.B. Add remote buildscript dependency

Not yet published.
<sub>If you're a credible source and want to offer hosting, lmk.</sub>

## 1.B. Add precompiled jar

- Download a [release](https://github.com/Caellian/ModEnv/releases), or build the plugin on your own.
- Move the downloaded/built jar to desired location (e.g. `<project_root>/libs/modenv-gradle-1.0.0.jar`)
- Add the jar to buildscript classpath:
```groovy
buildscript{
    repositories {
        // ...
    }
    
    dependencies {
        classpath files('libs/modenv-gradle-1.0.0.jar')
    }
}
```

## 1.C. Build from source

- Add a git submodule to root of your project, or simply unarchive the repository.
- Include it in the build:
```groovy
// ... end of settings.gradle ...
includeBuild("modenv")
```

# 2. Apply the plugin

```groovy
// ... top of build.gradle ...
plugins {
    // ... other plugins ...
    id 'net.tinsvagelj.mc.modenv'
}
```

## 3. Add dependencies

Plugin bundles required processor, library and runtime checker in order to simplify usage.

You can enable annotations and compiler plugin via:
```groovy
dependencies {
    implementation modEnv.lib
    annotationProcessor modEnv.lib
    annotationProcessor modEnv.plugin
}
```

The gradle plugin will automatically run the checker to determine which mod loader(s) is present in classpath and add
appropriate compiler arguments:
```sh
-Xplugin:ModEnvPlugin Mod;Loader;Names
--add-exports=jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED
# a bunch more --add-exports for compiler internals, see processor build.gradle.kts
```

# Usage

ModEnv library adds two annotations:
- [`@OnlyForLoader`](https://github.com/Caellian/ModEnv/blob/trunk/modenv-lib/src/main/java/net/tinsvagelj/mc/modenv/OnlyForLoader.java)
- [`@NotForLoader`](https://github.com/Caellian/ModEnv/blob/trunk/modenv-lib/src/main/java/net/tinsvagelj/mc/modenv/NotForLoader.java)

You can annotate classes and methods with either to hide them if specified mod loader is(n't) present:
```java
@OnlyForLoader({ModEnv.FORGE})
public void useFluid(net.minecraftforge.fluids.FluidStack fluid) {
    useFluid(fluid.getFluid(), fluid.getAmount());
}
public void useFluid(Fluid fluid, float buckets) {
    // ... implementation details ...
}
```

If the code is compiled without Forge in classpath (e.g. for Fabric), the first method definition will not be present.

This means you can also add your own polyfill to bridge functionality you need to rely on (if not already available).
```java
package must.be.in.your.own.pkg;

// ... imports ...

@NotForLoader({ModEnv.FORGE})
class FluidStack {
    // ... implementation details ...
}
```

Last important bit of information is that you can specify multiple mod loaders. This allows fine-grained control over
mod functionality at compile time:
```java
@OnlyForLoader({ModEnv.FORGE, ModEnv.NEOFORGE})
public void initClientsideModLogic() {
    // ... forge/neoforge client init ...
}
@NotForLoader({ModEnv.FORGE, ModEnv.NEOFORGE})
public void initClientsideModLogic() {
    // ... non-forge client init ...
}
```

# Important details

Your IDE will likely show you errors, but compilation will work. This is because IDEs do their own thing and don't use
java compiler for error reporting (such as missing types) - this is much faster than your IDE having to run the compiler
after every typed character but the tradeoff is incorrect highlighting.

Properly supporting all mainstream IDEs is a lot of work and adds more maintenance burden than I'm willing to put into
this project at this time. Contributions to this front are welcome.

This plugin runs before any other annotations have been processed, which means it should work as expected alongside
other annotation processors such as [Lombok](https://github.com/projectlombok/lombok).

# FAQ

<details>
<summary>Questions & Answers</summary>
<dl>
<dt>How likely is the plugin to break?</dt>
<dd>This compiler plugin relies on abstractions provided by java compiler to avoid touching too much of compiler internals. It's more stable than rest of the code you'll write, so don't worry about it.</dd>
<dt>I want another/my mod loader supported.</dt>
<dd>Make a PR. You need to only add an entry to <code>ModEnv</code> enum in the <em>modenv-lib</em> module.</dd>
<dt>Why add ancient/discontinued mod loaders?</dt>
<dd><ul>
<li>They serve as examples.</li>
<li>It's very cheap to add <code>ModEnv</code> entries so there's no reason not to support old stuff.</li>
<li>Archival purposes.</li>
</ul></dd>
<dt>What's the purpose of this?</dt>
<dd>It's supposed to make writing <a href="https://github.com/TheCBProject/CodeChickenLib">base libraries</a> that work for different mod loaders easier. It should also work for mods, but that's a lot more involved and I believe it's better to separate implementations into different submodules and then use this processor in the <em>common</em> code.</dd>
<dt>Why not just use reflection?</dt>
<dd>While reflection is much simpler, it's too costly when it runs in a game loop. This plugin bridges the gap between development ergonomics and build system complexity.</dd>
<dt>What's your motivation for writing this?</dt>
<dd>I wanted to write a compiler plugin for a GC language after a decade of metaprogramming in compiled ones.</dd>
</dl>
</details>

# License

This project is licensed under ternary [MIT](./LICENSE-MIT)/[Apache-2.0](./LICENSE-APACHE-2.0)/[Zlib](./LICENSE-ZLIB) license. Adhere to whichever fits your use case the best.
