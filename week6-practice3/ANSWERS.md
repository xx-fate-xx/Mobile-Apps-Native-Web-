# Practice 3 - KMP Project Structure

Project modules:
- `shared` - KMP module (android + iosArm64 + iosSimulatorArm64), questions 5, 11, 13
- `androidApp` - small android app that uses `shared`
- `targets` - module with jvm, iosArm64 and js targets, questions 8 and 12

## Section 1

### Q1
KMP is not a bridge and not a VM. Flutter renders everything itself through its own engine (Skia / Impeller) and doesn't use native views. React Native runs JS and talks to the native views across a bridge (JS bridge / JSI). Kotlin/Native compiles the shared code with LLVM into a real native binary (ARM64 framework) for iOS, so Swift calls it directly like a normal library. On Android it's just normal JVM bytecode. So the UI can stay fully native.

### Q2
The problem is that Android and iOS teams build the same logic two times in two languages: validation, networking and business rules are written in Kotlin for Android and again in Swift for iOS. The consequences:
1. duplicated bugs
2. inconsistent behavior between platforms
3. double QA effort for every business logic change

### Q3
Usually 60-80% of the code is shared.

Shared:
- networking / API client (for example Ktor)
- validation and business rules, so they work the same on both platforms

Kept native:
- camera, biometrics, sensors - they need deep integration with the platform hardware
- push notifications - they depend on platform specific delivery services (FCM / APNs)

### Q4
Stable: Android, iOS (arm64 + simulator), Desktop (Windows, macOS, Linux), Server, Web with Kotlin/JS.
Beta: Web with Kotlin/Wasm.

Beta means it is still maturing. It works, but the APIs and tooling can still change and there can be breaking changes, so using it for a production feature is risky.

### Q5
Files are in `shared/src`:
- `commonMain/.../Platform.kt` - `interface Platform` + `expect fun getPlatform(): Platform`
- `androidMain/.../Platform.android.kt` - `actual` that returns "Android (API ..)"
- `iosMain/.../Platform.ios.kt` - `actual` that returns "iOS " + system version (with UIDevice)

```kotlin
// commonMain
interface Platform {
    val name: String
}

expect fun getPlatform(): Platform
```

After building there are no red underlines and the expect has the icon that goes to both actuals.

Screenshot: `screenshots/q5.png`

### Q6
KMP shares the logic (network, data, validation) and the UI stays native (Compose on Android, SwiftUI on iOS). Compose Multiplatform is a UI framework from JetBrains built on top of KMP, so with it you can share the UI too. CMP is stable on Android, iOS and Desktop, and Web (Wasm) is Beta. KMP can be used without CMP (only shared logic), but CMP can't be used without KMP because it uses KMP compiler, targets and source sets.

## Section 2

### Q7
Common code is the code in commonMain, it is compiled for every target of the project. So it can use only things that exist on all targets. `java.io.File` is from the JDK, and the JDK doesn't exist on native (iOS) or JS targets, so the compiler can't resolve it in commonMain. In a normal JVM project there is only JVM target so it works. To use files in common code you need expect/actual or a multiplatform library.

### Q8
In `targets/build.gradle.kts`:
```kotlin
kotlin {
    jvm()
    iosArm64()
}
```
After sync the source sets for the targets are: `commonMain`, `commonTest`, `jvmMain`, `jvmTest`, `iosArm64Main`, `iosArm64Test`.

I also made a task that prints all source sets (`./gradlew :targets:printSourceSets`). It shows also `appleMain`, `iosMain`, `nativeMain` (and Test pairs). These are intermediate source sets from the default hierarchy template, they are created because iosArm64 is an Apple/native target.

### Q9
Source set:
1. has a unique name
2. contains source files (and resources)
3. has the targets it compiles to
4. has its own dependencies and compiler options

commonMain is the root, it compiles to all targets so it can't use platform APIs. androidMain compiles only for Android so it can use Android/JDK APIs, sees commonMain and contains the actual implementations.

### Q10
Merge rule: for a target Kotlin takes every source set that is labeled with this target and compiles them together. For iosArm64 it is commonMain + appleMain/iosMain + iosArm64Main.

Rules:
1. common code is at the top, platform code is at the leaves
2. declaring a target adds a label to the code, so the compiler knows which binaries to make
3. information goes only one way: platform source sets can use commonMain, but commonMain can't see platform code
4. every Main source set has a Test pair that follows the same hierarchy

### Q11
`shared/src/.../Time.kt` + `Time.android.kt` + `Time.ios.kt`

```kotlin
// commonMain
expect fun currentTimestamp(): Long

// androidMain
actual fun currentTimestamp(): Long = System.currentTimeMillis()

// iosMain
actual fun currentTimestamp(): Long = (NSDate().timeIntervalSince1970 * 1000).toLong()
```
On iOS timeIntervalSince1970 is in seconds (Double) so I multiply by 1000. Also needed `import platform.Foundation.timeIntervalSince1970`, without it it was unresolved.

Built with `./gradlew :shared:assembleDebug` and `./gradlew :shared:compileKotlinIosArm64`, no errors.

### Q12
```kotlin
kotlin {
    jvm()
    iosArm64()
    js {
        browser()
    }
}
```
In Kotlin 2.x `js(IR)` is not needed, IR is the only JS compiler now. After re-sync `jsMain` and `jsTest` appeared next to the other source sets (and also `webMain`/`webTest` from the hierarchy template). I didn't connect anything manually, just declaring the target was enough. `targets/src/jsMain/.../Main.js.kt` uses `commonMessage()` from commonMain and it compiles (`./gradlew :targets:compileKotlinJs`).

### Q13
`shared/src/commonMain/.../Greeting.kt` and `shared/src/commonTest/.../GreetingTest.kt`

```kotlin
class GreetingTest {
    @Test
    fun greetReturnsText() {
        assertEquals("Hi from shared code!", Greeting().greet())
    }
}
```
Added `implementation(kotlin("test"))` to commonTest. Ran it with `./gradlew :shared:testDebugUnitTest` (and from the green arrow), test passed.
