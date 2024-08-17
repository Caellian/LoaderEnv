# LoaderEnv IntelliJ Plugin

A plugin for JetBrains IntelliJ IDEA that adds first-class support for LoaderEnv.

## Features

- Disables code inspection for elements disabled at build time
- Hides _most_ incorrect peripheral errors
- Indicates disabled elements with different highlighting

## Stability Notice

This plugin is still in its infancy. There's a lot of edge cases that aren't properly handled yet, and its got a lot
of quirks that need to be ironed out.

While compiler plugin part of LoaderEnv is very straightforward to implement, IDE integration isn't because all inspections
related to duplicate classes, methods and fields, field shadowing, etc. have to be modified.

Think of this module as more of a "minimal implementation" than a stable plugin.
