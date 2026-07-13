plugins {
    alias(libs.plugins.android.multiplatform.library)
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
}

group = "io.github.dexclub"
version = "0.0.0-local"

val cleanVendoredDexKitAndroidCxxCache = tasks.register<Delete>("cleanVendoredDexKitAndroidCxxCache") {
    group = "build"
    description = "Clear vendored DexKit Android CMake/.cxx caches so clean does not hit stale absolute paths after moving the repo"
    delete(
        layout.projectDirectory.dir("vendor/DexKit/dexkit-android/.cxx"),
        layout.projectDirectory.dir("vendor/DexKit/dexkit-android/build/intermediates/cxx"),
    )
}

tasks.named("clean") {
    dependsOn(cleanVendoredDexKitAndroidCxxCache)
}

tasks.register("prepareDexKitDesktopNative") {
    group = "build"
    description = "Build the vendored DexKit desktop native library for upstream tests and packaging"
    dependsOn(gradle.includedBuild("DexKit").task(":dexkit:copyLibrary"))
}

tasks.register("assembleDexKitAndroidRelease") {
    group = "build"
    description = "Build the vendored DexKit Android release AAR for the native maintenance flow"
    dependsOn(gradle.includedBuild("DexKit").task(":dexkit-android:assembleRelease"))
}

kotlin {
    android {
        namespace = "io.github.dexclub.dexkit"
        compileSdk = 36
        minSdk = 24
        withHostTest {}
    }

    jvm()

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(libs.kotlinx.serialization.json)
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.kotlinx.serialization.json)
            }
        }

        val androidMain by getting {
            // JVM and Android share the exact same ResultMappers source to avoid duplicate maintenance.
            kotlin.srcDir("src/jvmAndroidShared/kotlin")
            dependencies {
                // noinspection UseTomlInstead
                implementation("io.github.dexclub.dexkit:android-core:0.0.0-local")
            }
        }

        val jvmMain by getting {
            kotlin.srcDir("src/jvmAndroidShared/kotlin")
            dependencies {
                // noinspection UseTomlInstead
                implementation("io.github.dexclub.dexkit:desktop-core:0.0.0-local")
            }
        }
    }

    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }
}
