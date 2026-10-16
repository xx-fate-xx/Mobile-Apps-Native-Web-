// swift-tools-version:5.9
import PackageDescription

// Task 25 - Swift package that gets the XCFramework from a URL
let package = Package(
    name: "Shared",
    platforms: [.iOS(.v15)],
    products: [
        .library(name: "Shared", targets: ["Shared"])
    ],
    targets: [
        .binaryTarget(
            name: "Shared",
            url: "https://github.com/xx-fate-xx/Mobile-Apps-Native-Web-/releases/download/1.0.0/Shared.xcframework.zip",
            checksum: "b26f3455c1d12e888ac9dc7647119f0303e3ee3efebe95698edbb96187a4b16f"
        )
    ]
)
