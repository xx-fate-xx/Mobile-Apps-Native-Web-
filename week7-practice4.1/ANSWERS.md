# Practice 4.1 - KMP + KMP project structure

Project modules:
- `shared` - KMP module (android + jvm + iosArm64 + iosSimulatorArm64), tasks 4, 5, 8, 9, 12, 14, 24, 25
- `androidApp` - android app with `LoginViewModel` (task 14, 17)
- `desktopApp` - desktop (JVM) app (task 17)
- `feature:auth`, `feature:profile` - two feature modules (task 19)
- `umbrella` - umbrella module that makes one iOS framework `Shared` (task 19, 20)
- `iosApp/LoginViewModel.swift` - SwiftUI view model (task 14)
- `SharedPackage/Package.swift` - Swift package with `.binaryTarget` (task 25)

## Part A - What is Kotlin Multiplatform

### Task 1
- Flutter renders everything itself through its own engine (Skia / Impeller), it doesn't use native views.
- React Native runs JavaScript and talks to the native views across a bridge.
- In KMP the shared Kotlin code is compiled by Kotlin/Native into a native binary - an Apple framework - and Swift calls it directly like any other library. There is no bridge and no VM at runtime.

### Task 2
- Android - Kotlin/JVM (normal JVM bytecode, like a usual Android app)
- iOS - Kotlin/Native, it uses LLVM to compile to native ARM64 code and makes a framework
- Browser - Kotlin/JS (compiles to JavaScript) or Kotlin/Wasm (compiles to WebAssembly)
- Desktop - Kotlin/JVM (Windows, macOS, Linux)
- Server - Kotlin/JVM

The compiler technology for iOS is Kotlin/Native (based on LLVM).

### Task 3
- Ktor API client - shared (commonMain), networking is the same on all platforms, only the engine is platform specific.
- Biometric login - kept native, it needs deep integration with platform hardware and APIs (BiometricPrompt / Face ID).
- Input validation - shared (commonMain), it's pure logic and must work the same on both platforms.
- Push notifications - kept native, they depend on platform delivery services (FCM / APNs).
- SQLDelight storage - shared (commonMain), SQLDelight is a multiplatform library, only the driver is created per platform.
- Deep-link navigation - kept native, navigation is part of the UI and every platform handles links differently (intent filters / universal links).

### Task 4
Files are in `shared/src/.../uz/ttpu/shared/`.

```kotlin
// androidMain - Platform.android.kt
import android.os.Build

class AndroidPlatform : Platform {
    override val name: String = "Android ${Build.VERSION.SDK_INT}"
}

actual fun getPlatform(): Platform = AndroidPlatform()
```

```kotlin
// iosMain - Platform.ios.kt
import platform.UIKit.UIDevice

class IOSPlatform : Platform {
    override val name: String =
        UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion
}

actual fun getPlatform(): Platform = IOSPlatform()
```

```kotlin
// jvmMain (desktop) - Platform.jvm.kt
class JvmPlatform : Platform {
    override val name: String = "Java ${System.getProperty("java.version")} on ${System.getProperty("os.name")}"
}

actual fun getPlatform(): Platform = JvmPlatform()
```

I checked the desktop one with `./gradlew :desktopApp:run`:
```
Running on: Java 17.0.20.1 on Mac OS X
```

### Task 5
The error is that an `expect` declaration has a body. `expect` only declares the signature, the implementation must be in `actual` in every platform source set. The compiler says:
```
e: shared/src/commonMain/kotlin/uz/ttpu/shared/Time.kt:3:1 Expected declaration cannot have a body.
```

Fixed:
```kotlin
// commonMain
expect fun currentTimeMillis(): Long

// androidMain and jvmMain
actual fun currentTimeMillis(): Long = System.currentTimeMillis()

// iosMain
import platform.Foundation.NSDate
import platform.Foundation.timeIntervalSince1970

actual fun currentTimeMillis(): Long = (NSDate().timeIntervalSince1970 * 1000).toLong()
```

### Task 6
Android and JVM compile fine, but the iOS compilation (iosArm64 and iosSimulatorArm64) fails because there is no `actual` for these targets. I tried it with an `expect fun deviceId(): String` and got:
```
e: shared/src/commonMain/kotlin/uz/ttpu/shared/Platform.kt:9:1 Expected deviceId has no actual declaration in module <commonMain> for Native
```

Two ways to fix:
1. Add the `actual` in `iosMain` - it is shared by iosArm64 and iosSimulatorArm64, so one actual is enough for both.
2. Don't use expect/actual at all - if the code can be written with common Kotlin only, make it a normal function in commonMain (or use a multiplatform library), then no actual is needed.

### Task 7
For iosArm64 the compiler merges: `commonMain` + `nativeMain` + `appleMain` + `iosMain` + `iosArm64Main`.

`appleMain` is an intermediate source set that is labeled only with the Apple targets (iOS, macOS, ...). iosMain depends on appleMain, so it's in the iOS compilation. androidMain depends only on commonMain, and appleMain is not compiled for Android at all, so Android code can't see it. Visibility only goes up the tree (platform -> common), never sideways.

### Task 8
```kotlin
// iosMain/IosVersion.kt
import platform.UIKit.UIDevice

fun iosVersion(): String = UIDevice.currentDevice.systemVersion
```

It can't be in commonMain because `platform.UIKit` exists only on iOS. commonMain is compiled for all targets (also Android and JVM), so it can use only APIs that exist on every target. If I put it in commonMain the import is unresolved.

### Task 9
```kotlin
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation("io.ktor:ktor-client-core:3.3.0")
        }
        androidMain.dependencies {
            implementation("io.ktor:ktor-client-okhttp:3.3.0")
        }
        iosMain.dependencies {
            implementation("io.ktor:ktor-client-darwin:3.3.0")
        }
    }
}
```

In commonMain I can just write `HttpClient()` (see `ApiClient.kt`), Ktor picks the engine from the platform dependency.

### Task 10
Kotlin Multiplatform is the technology for sharing code (logic) between platforms. Compose Multiplatform is a UI framework from JetBrains built on top of KMP, it lets you share the UI too.

Module layout for shared logic + native UI:
- `shared` - KMP module with commonMain (models, networking, validation, business rules), androidMain and iosMain
- `androidApp` - Android app with Jetpack Compose UI, depends on `shared` like a normal module
- `iosApp` - Xcode project with SwiftUI, uses the iOS framework generated from `shared`

KMP can be used without Compose Multiplatform (exactly this layout). Compose Multiplatform can't be used without KMP, because it is built on KMP and its code lives in KMP source sets.

### Task 11
- iOS - Stable. Yes, I would ship it, it's stable and used in production.
- Kotlin/Wasm - Beta. Not for an important production feature yet, APIs and tooling can still change.
- expect/actual classes - Beta. expect/actual functions and properties are stable, but for classes I would use an interface + expect fun instead (like Platform in task 4), so I don't depend on a Beta feature.
- Compose Multiplatform for web - Beta. I would not ship a main production app on it today, maybe only an internal tool or experiment.

### Task 12
```kotlin
// commonMain
fun isValidEmail(text: String): Boolean {
    val at = text.indexOf('@')
    if (at <= 0 || at != text.lastIndexOf('@')) return false
    val domain = text.substring(at + 1)
    return domain.contains('.') && !domain.startsWith('.') && !domain.endsWith('.')
}
```

```kotlin
// commonTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EmailValidationTest {
    @Test
    fun validEmail() {
        assertTrue(isValidEmail("student@ttpu.uz"))
    }

    @Test
    fun invalidEmail() {
        assertFalse(isValidEmail("student.ttpu.uz"))
    }
}
```

Gradle tasks:
- JVM: `./gradlew :shared:jvmTest`
- iOS simulator: `./gradlew :shared:iosSimulatorArm64Test`

(and `:shared:testDebugUnitTest` for Android unit tests, `allTests` runs everything). All of them passed.

Screenshot: `screenshots/task12.png`

### Task 13
1. Swift interop - Kotlin is exported to Swift through an Objective-C header, so some Kotlin features don't look nice in Swift (suspend functions, sealed classes, generics, default arguments). Mitigation: keep the public API of the shared module simple (plain classes and functions), and use a tool like SKIE or the new Swift export when it is stable.
2. Binary size - the framework contains the Kotlin runtime and all dependencies, so the iOS app gets bigger. Mitigation: use one umbrella framework (no duplicated copies), build a static framework in release, and don't add heavy libraries to commonMain if only one platform needs them.
3. Team skills - iOS developers have to learn Kotlin, Gradle and Android Studio. Mitigation: start with a small shared module (for example only validation and models), and let Android developers own Gradle at the start, while iOS developers review the shared API.

### Task 14
```kotlin
// commonMain/LoginValidator.kt
class LoginValidator {

    fun validateEmail(email: String): String? {
        if (email.isBlank()) return "Email is empty"
        if (!isValidEmail(email)) return "Email is not valid"
        return null
    }

    fun validatePassword(password: String): String? {
        if (password.length < 8) return "Password must be at least 8 characters"
        if (password.none { it.isDigit() }) return "Password must contain a digit"
        if (password.none { it.isUpperCase() }) return "Password must contain an uppercase letter"
        return null
    }

    // returns null if everything is ok, otherwise the first error
    fun validate(email: String, password: String): String? =
        validateEmail(email) ?: validatePassword(password)
}
```

Android ViewModel (`androidApp/.../LoginViewModel.kt`):
```kotlin
class LoginViewModel : ViewModel() {
    private val validator = LoginValidator()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun onLoginClick(email: String, password: String) {
        _error.value = validator.validate(email, password)
    }
}
```

SwiftUI view model (`iosApp/LoginViewModel.swift`), it uses the class from the `Shared` framework:
```swift
import SwiftUI
import Shared

class LoginViewModel: ObservableObject {
    private let validator = LoginValidator()

    @Published var email = ""
    @Published var password = ""
    @Published var error: String? = nil

    func onLoginClick() {
        error = validator.validate(email: email, password: password)
    }
}
```

`String?` from Kotlin becomes `String?` in Swift, and the parameter names become labels (`email:`, `password:`). I checked that the Swift file compiles against the generated framework (`swiftc -typecheck` with the simulator framework), no errors.

### Task 15
Order: 1) data models, 2) networking, 3) local storage, 4) business rules, 5) UI.

I would start with data models because they are pure Kotlin data classes with no platform dependencies, everything else (networking, storage, rules) uses them, so they must be shared first. It's also the safest step, it's easy to test and doesn't change app behavior. UI is last and it can also stay native (Jetpack Compose on Android, SwiftUI on iOS).

## Part B - KMP Project Configuration

### Task 16
- Single shared module: advantage - simple design, less cognitive load, you don't decide where each functionality goes, good starting point. Drawback - compilation time grows with the module and you can't depend on only part of the shared code.
- Several shared modules: advantage - separation of concerns and better scalability. Drawback - more complicated setup (umbrella framework configuration) and more involved dependency management.

Signal to split: the shared module has grown and builds are slow, and/or the app needs separate features (some app or module wants only part of the shared code).

### Task 17
```kotlin
// settings.gradle.kts
include(":shared")
include(":androidApp")
include(":desktopApp")
```

```kotlin
// androidApp/build.gradle.kts and desktopApp/build.gradle.kts
dependencies {
    implementation(project(":shared"))
}
```

Both apps build, and `:desktopApp:run` prints the platform from shared.

### Task 18
Each KMP module compiled into its own framework bundles all its dependencies inside it. So if feature1 and feature2 both use the same library and are exposed as two frameworks, the app contains two copies of this library (and of the Kotlin runtime), and they are incompatible with each other.

The failure with shared state: if the library has some state, like a singleton with the logged in user or a cache, feature1 writes into its own copy and feature2 reads from another copy. State passed through "the same" dependency is not connected, so feature2 doesn't see the user. On Android it works because there is only one copy, so the bug shows up only on iOS.

### Task 19
`umbrella/build.gradle.kts`:
```kotlin
plugins {
    kotlin("multiplatform")
}

kotlin {
    listOf(iosArm64(), iosSimulatorArm64()).forEach {
        it.binaries.framework {
            baseName = "Shared"
            isStatic = true
            export(project(":feature:auth"))
            export(project(":feature:profile"))
        }
    }

    sourceSets.commonMain.dependencies {
        api(project(":feature:auth"))
        api(project(":feature:profile"))
    }
}
```

After `./gradlew :umbrella:linkDebugFrameworkIosSimulatorArm64` the `Shared.h` header has `AuthRepository`, `User`, `Profile` and `ProfileRepository`, so iOS sees both features from one framework.

### Task 20
There are two problems:
1. The dependency is `implementation`, so the module is hidden from consumers of the umbrella.
2. There is no `export(...)` in the framework block, so the types from `:feature:auth` are not put into the Objective-C header.

Fix - use `api` and export the module:
```kotlin
kotlin {
    listOf(iosArm64(), iosSimulatorArm64()).forEach {
        it.binaries.framework {
            baseName = "Shared"
            export(project(":feature:auth"))
        }
    }
    sourceSets.commonMain.dependencies {
        api(project(":feature:auth"))
    }
}
```

(export only works with `api` dependencies.) I checked it: with the broken version `Shared.h` has only the Kotlin base types (`KotlinBase`, `KotlinInt`, ...) and `UmbrellaKt`, and after the fix `AuthRepository` and `User` are there.

Screenshot: `screenshots/task20.png`

### Task 21
When Kotlin/Native builds a framework, it compiles the module together with all its dependencies into one binary, and each binary is trimmed to exactly what this one build needs. There is no way to share a Kotlin dependency between two separately built binaries, each framework has its own copy, with its own Kotlin runtime and its own code structure, and the types from one copy are not the same types as in the other copy. So the only way to be sure there is one copy is to compile everything into one framework - the umbrella framework, where every dependency exists exactly once.

### Task 22
Monorepo:
- Advantage: easy to set up with project wizards, iOS developers can work with shared code without looking across repositories.
- Drawback: iOS developers must set up unfamiliar tools (Gradle, Android Studio / IntelliJ), and it often doesn't fit apps that already live in separate repositories.
- Versioning: the shared module is not versioned separately, everything moves together in one repository and is used from source.

Two repositories (Android + shared | iOS):
- Advantage: Android and iOS can be versioned separately, the Xcode project stays in its own repository.
- Drawback: the shared code is tied to the Android side, and iOS has to get the framework from another repository.
- Versioning: the shared module's version moves in lockstep with the Android app (same repository), only the iOS framework is given to the iOS repository.

Three repositories (Android | iOS | shared):
- Advantage: all three parts are versioned independently, both teams use the same versioned artifact, so there is version parity and the shared code is not "owned" by the Android team.
- Drawback: the most publishing overhead.
- Versioning: the shared modules have their own version and must be published for Android/JVM and for iOS, both apps depend on a published version.

### Task 23
Two options:
1. Publish each feature module separately.
2. Publish only the umbrella module.

Trade-off: publishing each feature separately is more flexible - Android can depend only on the features it needs, and every feature can have its own version, but there are more artifacts to version and publish and to keep compatible. Publishing only the umbrella is simpler (one artifact, one version, the same thing iOS gets), but Android has to take all the shared code at once and can't pick only some features.

### Task 24
In `shared/build.gradle.kts`:
```kotlin
plugins {
    kotlin("multiplatform")
    id("com.android.library")
    id("maven-publish")
}

group = "uz.ttpu.shared"
version = "1.0.0"

kotlin {
    androidTarget {
        // without this only the jvm/ios parts are published, not android
        publishLibraryVariants("release")
    }
    ...
}

publishing {
    repositories {
        mavenLocal()
    }
}
```

Publish with `./gradlew :shared:publishToMavenLocal`. It creates `uz.ttpu.shared:shared:1.0.0` plus `shared-android`, `shared-jvm`, `shared-iosarm64`, `shared-iossimulatorarm64`.

The Android repository adds `mavenLocal()` to its repositories and uses:
```kotlin
dependencies {
    implementation("uz.ttpu.shared:shared:1.0.0")
}
```

Gradle picks the android variant automatically from the Gradle metadata. I tested it with a separate small Android project and it resolves `uz.ttpu.shared:shared-android:1.0.0` and compiles code that uses `LoginValidator`.

### Task 25
In `shared/build.gradle.kts`:
```kotlin
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFramework

kotlin {
    val xcf = XCFramework("Shared")
    listOf(iosArm64(), iosSimulatorArm64()).forEach {
        it.binaries.framework {
            baseName = "Shared"
            xcf.add(this)
        }
    }
}
```

`./gradlew :shared:assembleSharedXCFramework` makes `shared/build/XCFrameworks/release/Shared.xcframework` with `ios-arm64` and `ios-arm64-simulator` inside. Then I zipped it and computed the checksum:
```
zip -r Shared.xcframework.zip Shared.xcframework
swift package compute-checksum Shared.xcframework.zip
b26f3455c1d12e888ac9dc7647119f0303e3ee3efebe95698edbb96187a4b16f
```

`SharedPackage/Package.swift`:
```swift
.binaryTarget(
    name: "Shared",
    url: "https://github.com/xx-fate-xx/Mobile-Apps-Native-Web-/releases/download/1.0.0/Shared.xcframework.zip",
    checksum: "b26f3455c1d12e888ac9dc7647119f0303e3ee3efebe95698edbb96187a4b16f"
)
```
(the URL is where the zip would be uploaded as a release asset, the checksum is from my build.)

The checksum is required because SwiftPM downloads a binary from a remote URL, and it must check that the file is exactly the one the package author published - not changed, broken or replaced on the server. If the zip doesn't match the checksum, SwiftPM refuses to use it. That's also why every new version of the framework needs a new checksum.

### Task 26
| | Local (source) | Remote (artifact) |
|---|---|---|
| iOS developer onboarding | hard - everyone needs a full dev environment, iOS developers have to learn Gradle and Android Studio | easy - iOS developers never touch Kotlin, Gradle or Android Studio |
| Iteration speed | fast - the framework is built fresh on every iOS build, no waiting for artifacts | slow - every change means publish, version, then rebuild |
| Versioning overhead | none - no version number, no publish step | high - every change needs a new version and publishing |
| Who can edit shared code | both Android and iOS developers, it's a shared responsibility | mostly Android/KMP developers, iOS contributions taper off over time (debugging from iOS is hard) |

### Task 27
1. Default setup - the iOS app uses the published, versioned framework (remote), for example through Swift Package Manager.
2. Switch to local - when the iOS developer needs to change shared Kotlin code, they point the iOS build to a local dependency on the framework instead (direct framework integration, or with CocoaPods change an environment variable).
3. Verify the change - they edit Kotlin code, and the change is visible on the next iOS build, so they can debug it immediately like in pure local distribution.
4. Publish - when the feature is ready, they publish a new version of the shared module first.
5. Switch back - then update the iOS app to depend on the new remote version and remove the local dependency.

Correct order: publish the shared module first, then update the app to the new version. If you update the app first it points to a version that doesn't exist yet.

### Task 28
```
logging-lib (repo, own version) ------+----------------> app-a-umbrella (repo) ---> App A (Android + iOS)
                                       |                       ^
networking-lib (repo, own version) ---+-----------------------+
                                       |
                                       +----------------> app-b-umbrella (repo) ---> App B (Android + iOS)
                                                               ^
analytics-lib (repo, own version) -----------------------------+
```

App A umbrella = logging + networking, App B umbrella = networking + analytics.

Each app gets its own umbrella repository because the two apps use different subsets of the libraries. One umbrella for both would force App A to include analytics and App B to include logging, which they don't need. And each iOS app still needs exactly one framework (umbrella), so that networking is not duplicated next to logging/analytics inside one app.

### Task 29
- Solo developer with a small app: single shared module, monorepo, local distribution. One person does everything, so no need for publishing or versioning, and it's the simplest setup.
- Four engineers releasing Android and iOS together: monorepo with local distribution, single module at the start and feature modules + umbrella when it grows. They release together, so they don't need separate versions, and everyone can edit the shared code with a fast loop.
- Separate Android and iOS teams with independent release cycles: three repositories (Android | iOS | shared), several modules with an umbrella, remote distribution (with the hybrid workflow for people changing shared code). Each team needs its own release cycle and version of the shared code, and iOS developers don't need to set up Gradle.

### Task 30
1. Look at the code in `:shared` and find feature boundaries (for example auth, profile, payments) and a `core` part with things everyone uses (models, networking, utils).
2. Create the new modules (`:core`, `:feature:auth`, `:feature:profile`, ...) as KMP modules with the same targets and include them in `settings.gradle.kts`.
3. Move the code feature by feature into the new modules, without renaming classes and packages. Features depend on `:core`.
4. Turn `:shared` into the umbrella: it keeps almost no code, depends with `api(project(...))` on all feature modules and `export(...)`s them in the framework block.
5. Keep the framework `baseName = "Shared"` (and the same static/dynamic setting), so the iOS app still does `import Shared` and nothing changes in the Xcode project.
6. Build the framework and compare the new `Shared.h` with the old one - the same classes should be there with the same names. Run all tests (`allTests`).
7. The Android app can now depend directly on the feature modules it needs, and builds get faster because only changed modules are recompiled.

The iOS app always gets one framework with the same name and the same API, only the inside is split into modules.
