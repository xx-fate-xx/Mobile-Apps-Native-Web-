# Practice 4 - KMP Project Structure (continuation)

Project modules:
- `shared` - KMP module with jvm, js, linuxX64 (+ iosArm64, iosSimulatorArm64), questions 16, 17 and 21

Useful tasks:
- `./gradlew :shared:printSourceSets` - prints all source sets of the module
- `./gradlew :shared:printJvmCompilations` - prints compilations of the jvm target

## Section 3

### Q14
It's wrong because `dependsOn` is not a Gradle dependency at all. It's a Kotlin-specific relation between two source sets. That's why it's written outside the `dependencies {}` block, and it doesn't add any library or artifact to the build.

When `A.dependsOn(B)` is declared, all four things are true at the same time:
1. A sees B's API, including `internal` declarations
2. A can provide `actual` implementations for B's `expect` declarations
3. B compiles to every target A compiles to, plus its own targets
4. A inherits all of B's regular dependencies

Also these relations are preserved at compile time, the compiler checks them directly. So `dependsOn` is a visibility / actualization relation, not a packaging / build relation. A normal entry in `dependencies {}` just gives you the library API, you can't see its internals, you can't write actuals for its expects and it doesn't change which targets your source set compiles to.

### Q15
`dependsOn` is only for building the source set tree inside your own project, and mostly for source sets that Kotlin didn't create for you (like a custom `jvmAndJsMain`). The default source sets are created and wired with `dependsOn` automatically by the Kotlin Gradle plugin, so you rarely write it yourself.

- `commonTest.dependsOn(commonMain)` - both are default source sets and Kotlin already set them up. Main and Test are two separate trees on purpose (test source sets even align dependency versions separately from main), so connecting them with dependsOn mixes test code into the production tree. Also any manual dependsOn turns off the default hierarchy template (see Q17).
- `dependsOn` on a library's commonMain - this is not possible, dependsOn can't be used for a library or for a source set from another Gradle project. For libraries and other projects you use the `dependencies {}` block with `implementation` / `api`, by coordinates (`"group:artifact:version"`) or by project path (`project(":path")`).

### Q16
`shared/build.gradle.kts`:

```kotlin
kotlin {
    jvm()
    js(IR) {
        browser()
    }
    linuxX64()

    sourceSets {
        val jvmAndJsMain by creating {
        }
        jvmAndJsMain.dependsOn(commonMain.get())
        jvmMain.get().dependsOn(jvmAndJsMain)
        jsMain.get().dependsOn(jvmAndJsMain)
    }
}
```

In Kotlin 2.2 `jvmMain` and `jsMain` inside `sourceSets` are providers, so I had to add `.get()` for them too (same as `commonMain.get()` on the slide), otherwise it doesn't compile.

The shared function is in `shared/src/jvmAndJsMain/kotlin/.../JvmAndJs.kt`:

```kotlin
fun jvmAndJsMessage(name: String): String = "Hi $name, this is shared between JVM and JS"
```

and it is called from `jvmMain/Main.kt` and `jsMain/Main.js.kt`. Sync was ok and `compileKotlinJvm`, `compileKotlinJs`, `compileKotlinLinuxX64` all built successfully.

Screenshot: `screenshots/q16.png`

To check that it really is only for JVM and JS I tried to call `jvmAndJsMessage()` from `linuxX64Main` and got:
```
e: .../linuxX64Main/kotlin/com/practice/kmp4/Linux.kt:4:14 Unresolved reference 'jvmAndJsMessage'.
```
(then I removed it again).

I also printed the source sets before and after (like the "try it yourself" slide says):

Before (no jvmAndJsMain): commonMain, commonTest, jsMain, jsTest, jvmMain, jvmTest, linuxMain, linuxTest, linuxX64Main, linuxX64Test, nativeMain, nativeTest, webMain, webTest

After: commonMain, commonTest, jsMain, jsTest, jvmAndJsMain, jvmMain, jvmTest, linuxX64Main, linuxX64Test

So `linuxMain`, `nativeMain` and `webMain` (and their Test) disappeared, and Gradle shows a warning:
```
w: ⚠️ Default Kotlin Hierarchy Template Not Applied Correctly
Explicit .dependsOn() edges were configured for the following source sets:
[jsMain, jvmAndJsMain, jvmMain]
```

### Q17
I added `iosArm64()` and `iosSimulatorArm64()` to the same module. After sync there is no `appleMain` and no `iosMain` (also no `nativeMain`). The source sets are:

commonMain, commonTest, iosArm64Main, iosArm64Test, iosSimulatorArm64Main, iosSimulatorArm64Test, jsMain, jsTest, jvmAndJsMain, jvmMain, jvmTest, linuxX64Main, linuxX64Test

And the same "Default Kotlin Hierarchy Template Not Applied" warning is still there. Last week (Practice 3, Q8) with only `jvm()` + `iosArm64()` and no manual dependsOn I had `appleMain`, `iosMain` and `nativeMain` created automatically.

Screenshot: `screenshots/q17.png`

This is the caveat from the lecture: the moment you write any manual `dependsOn`, the project opts out of the default hierarchy template, so Kotlin stops creating the intermediate source sets for all targets, not only for the ones I wired by hand. The iOS targets now only get their leaf source sets that depend directly on commonMain. So if I want shared iOS code I would need to create `iosMain` myself with `by creating` and three dependsOn calls, the same way as `jvmAndJsMain`.

### Q18
Propagation means that a dependency declared once on `commonMain` is automatically passed to every source set that depends on commonMain, directly or transitively. So `androidMain`, `iosMain`, `iosArm64Main` and `iosSimulatorArm64Main` all get `kotlinx-coroutines-core:1.7.3` and I don't need to add it separately to androidMain or iosMain.

If I want it only on iOS, I declare it on `iosMain` instead of commonMain:

```kotlin
iosMain.dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
}
```

Then it propagates only to iosArm64Main and iosSimulatorArm64Main. The source set where you put the dependency is itself the scoping decision.

### Q19
A published KMP library is not one blob, it has its own source sets. kotlinx-coroutines-core has `commonMain` (all targets), `concurrentMain` (all targets except JS) and a JVM-only source set. My dependency "commonMain -> coroutines" is resolved down to real edges source set -> source set.

The rule: a dependency source set is compatible (visible) only if it covers at least all the targets of my source set.

My commonMain compiles to android + iosArm64 + iosSimulatorArm64:
- `Dispatchers.Main` - from coroutines' `commonMain`, it covers all targets -> resolves
- `runBlocking` - from coroutines' `concurrentMain`, it covers everything except JS, and I don't have JS -> resolves
- `dispatcher.asExecutor()` - from the JVM-only source set. It covers android but not iosArm64 / iosSimulatorArm64, so it's not compatible with my commonMain -> red, unresolved

Only `asExecutor()` fails because it's the only one whose source set doesn't cover all of my targets. It would work in `androidMain`.

### Q20
commonMain is compiled several times (once for each target), and every compilation has to agree on one version of a library. navigation-compose 2.7.7 in androidMain needs coroutines 1.8.0+, so Gradle raises coroutines from 1.7.3 to 1.8.0. It is raised in commonMain too and then propagated everywhere, so commonMain and iosMain also use 1.8.0, even though the conflict came only from Android.

The *Test source sets are aligned on their own separate tree. So if I upgrade something in `iosTest`, it only changes the test tree, it never touches the versions in `*Main` source sets (production code).

### Q21
In `shared/build.gradle.kts`:

```kotlin
kotlin {
    jvm {
        val main = compilations.getByName("main")
        println("jvm main compilation: " + main.name)
    }
    ...
}

tasks.register("printJvmCompilations") {
    doLast {
        kotlin.jvm().compilations.forEach { compilation ->
            println(compilation.name + " -> " + compilation.allKotlinSourceSets.map { it.name })
        }
    }
}
```

Output of `./gradlew :shared:printJvmCompilations`:
```
> Configure project :shared
jvm main compilation: main

> Task :shared:printJvmCompilations
main -> [jvmMain, jvmAndJsMain, commonMain]
test -> [jvmTest, commonTest]
```

So the jvm target has a `main` and a `test` compilation registered by default.

A source set is just a group of source files with its targets and dependencies, but a compilation is one actual compiler launch for one target, which takes several source sets together (e.g. jvm `main` = jvmMain + jvmAndJsMain + commonMain).

## Section 4

### Q22
Single shared module:
- Pros: simple design, only one module, so less cognitive load - you don't decide where each functionality goes. Good starting point.
- Cons: compilation time increases as the module grows. No separate features, and you can't depend on only part of the shared code.

Several shared modules:
- Pros: separation of concerns for shared code, better scalability when the app grows.
- Cons: more complicated setup, including the umbrella framework configuration, and more involved dependency management between modules.

For a small team starting their first KMP project I would start with one shared module. They usually move to several modules when the shared module grows - when the build gets slow and they need separate features or want to depend on only part of the shared code. At that point they split it into feature modules and add an umbrella module for iOS.

### Q23
Each KMP module compiled to its own framework bundles all of its dependencies inside it. If feature1 and feature2 both use the same dependency (for example coroutines or a common `core` module) and each one becomes a separate framework, this dependency is compiled into the iOS app two times, as two incompatible copies. Kotlin can't share a common dependency between separate builds, because each binary is trimmed to exactly what that one build needs.

The failure: state passed through "the same" dependency from two different modules is not actually connected. For example, if `core` has a singleton with the logged in user and feature1 saves the user there, feature2 will read its own copy of `core` and won't see it. On Android it works fine, so these bugs show up only on iOS.

An umbrella module is one extra module that depends on all the feature modules, and only this module is compiled into the iOS framework. The iOS app depends only on the umbrella framework, and the Android app can still depend on any/all modules directly. Since everything is in one compiled artifact, every dependency exists exactly once with one consistent code structure, so the problem is gone.

### Q24
Monorepo (everything in one repository):
- Advantage: easy to set up with project wizards, and iOS developers can work with shared code without searching across repositories.
- Disadvantage: iOS developers need to set up unfamiliar tools (Gradle, Android Studio / IntelliJ). Also it often doesn't fit apps that already live in separate repositories.

Two repositories (Android + shared | iOS):
- Advantage: Android and iOS can be versioned separately, and the Xcode project stays on its own repository.
- Disadvantage: the shared module's version moves in lockstep with the Android app, so the shared code is basically tied to (and "owned" by) the Android side, and iOS has to get the framework from another repository.

Three repositories (Android | iOS | shared):
- Advantage: each part is versioned independently, and both teams consume the same versioned artifact, so there is version parity and the shared code doesn't feel owned by the Android team.
- Disadvantage: more publishing overhead - the shared modules have to be published for Android/JVM too, not only for iOS.

Versioning consequence: in the two-repository setup the shared module is in the same repository as the Android app, so Android uses it directly from source and only iOS needs a framework. In the three-repository setup the shared code lives separately, so the shared modules must be published for Android/JVM as well as iOS - either every feature module separately, or only the umbrella module - and the Android app depends on a published version like the iOS app does.

### Q25
I would use the "many repositories" configuration:

- 3 repositories for the shared libraries: logging, networking and analytics. Each one has its own repo and its own version, and is published independently for Android/JVM and for iOS.
- Each app gets its own umbrella repository (so up to 5 umbrellas) that pulls in only the libraries this app needs. For example an app without analytics just doesn't include it in its umbrella. Android apps can depend on the published libraries they need, and the iOS app uses its umbrella framework. The umbrella is needed because networking and analytics will probably both use logging, and without one framework logging would be duplicated in the iOS app (Q23).
- Distribution: for 200 engineers pure local distribution is not a good idea, because everyone (iOS developers too) would need a full Kotlin/Gradle dev environment and it gets harder to manage when the team grows. Remote distribution is better as the default: iOS developers who don't work on shared code never touch Kotlin, Gradle or Android Studio. But remote has a slow loop (publish, version, rebuild) and debugging from iOS is hard, so I would use the hybrid workflow: by default apps use the published versions, and developers who are actively changing a shared library switch to a local dependency, iterate, then publish the library and switch the app back to the new remote version. For iOS this can be Swift Package Manager for remote and direct framework integration for local (or CocoaPods with an environment variable).

So on the four axes: several modules + umbrella per app, many repositories, remote distribution, and yes, the hybrid workflow.
