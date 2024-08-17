# LoaderEnv

This is a very simple Java compiler plugin that (optionally) integrates into Gradle build environment to allow
conditional inclusion and exclusion of classes/methods/fields based on the mod loaders that are present while compiling
a project. See [Usage](#Usage) section below for examples.

## Usage

LoaderEnv library adds two annotations:
- [`@NotForLoader`](https://github.com/Caellian/LoaderEnv/blob/trunk/modules/lib/src/main/java/net/tinsvagelj/mc/loaderenv/NotForLoader.java)
- [`@OnlyForLoader`](https://github.com/Caellian/LoaderEnv/blob/trunk/modules/lib/src/main/java/net/tinsvagelj/mc/loaderenv/OnlyForLoader.java)

You can annotate classes and methods with either to hide them if specified mod loader is(n't) present:
```java
@OnlyForLoader(Loader.FORGE)
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

// Cause compiler errors if wrong class is used in Forge:
@NotForLoader(Loader.FORGE)
class FluidStack {
    // ... implementation details ...
}
```

Last important bit of information is that you can specify multiple mod loaders. This allows fine-grained control over
mod functionality at compile time:
```java
@OnlyForLoader({Loader.FORGE, Loader.NEO_FORGE})
public void initClientsideModLogic() {
    // ... forge/neoforge client init ...
}
@NotForLoader({Loader.FORGE, Loader.NEO_FORGE})
public void initClientsideModLogic() {
    // ... non-forge client init ...
}
```

## Important Notes

**IDEs _will_ (by default) show errors even though element conditions are mutually exclusive.**
Code itself will however properly compile with `gradle`/`javac` - this is because Java IDEs have their own error
inspection code and don't use java compiler for error reporting (such as missing types).
While this is a much faster approach, the tradeoff is inaccurate highlighting.

Properly supporting all mainstream IDEs is a lot of work and adds more maintenance burden than I'm willing to put into
this project at the moment.
There's a [minimal implementation](./modules/jetbrains) available for Jetbrains IntelliJ platform (IDEA).
Contributions are welcome.

This plugin runs before any other annotations have been processed, which means it should (hopefully) work as expected
alongside other annotation processors such as [Lombok](https://github.com/projectlombok/lombok).

<details>
<summary><h1>Questions And Answers</h1></summary>
<dl>
<dt>How likely is the plugin to break?</dt>
<dd>This compiler plugin relies on abstractions provided by java compiler to avoid touching too much of compiler internals.
It's more stable than rest of the code you'll write, so don't worry about it.</dd>
<dt>I want another/my mod loader supported.</dt>
<dd>Make a PR. You need to only add an entry to <code>LoaderEnv</code> enum in the <em>loaderenv-lib</em> module.</dd>
<dt>Why add ancient/discontinued mod loaders?</dt>
<dd><ul>
<li>They serve as examples.</li>
<li>It's very cheap to add <code>LoaderEnv</code> entries so there's no reason not to support old stuff.</li>
<li>Archival purposes.</li>
</ul></dd>
<dt>What's the purpose of this?</dt>
<dd>It's supposed to make writing <a href="https://github.com/TheCBProject/CodeChickenLib">base libraries</a> that work
for different mod loaders easier. It should also work for mods, but that's a lot more involved and I believe it's better
to separate implementations into different submodules and then use this processor in the <em>common</em> code instead.</dd>
<dt>Why not just use reflection?</dt>
<dd>While reflection is much simpler, it's too costly when it runs in a game loop. This plugin bridges the gap between
development ergonomics and build system complexity.</dd>
<dt>What's your motivation for writing this?</dt>
<dd>I wanted to try writing a Java compiler plugin that does something similar to C/C++ preprocessor and Rust cfg.</dd>
</dl>
</details>

## License

This project is licensed under ternary [MIT](./LICENSE-MIT)/[Apache-2.0](./LICENSE-APACHE-2.0)/[Zlib](./LICENSE-ZLIB) license.
Adhere to whichever fits your use case the best.
