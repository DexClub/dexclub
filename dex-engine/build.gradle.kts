import org.gradle.language.jvm.tasks.ProcessResources

plugins {
    alias(libs.plugins.android.multiplatform.library)
    alias(libs.plugins.kotlin.multiplatform)
}

kotlin {
    android {
        namespace = "io.github.dexclub.dexengine"
        compileSdk = 36
        minSdk = 24
    }

    jvm()

    sourceSets {
        commonMain.dependencies {
            api(libs.filekit.core)
            api(libs.smali.dexlib2)
            implementation(libs.smali.baksmali)
            implementation(libs.jadx.core)
            implementation(libs.jadx.dex.input)
            implementation(libs.jadx.kotlin.metadata)
        }

        androidMain.dependencies {
            implementation("io.github.dexclub.dexkit:android-core:1.0.0")
        }

        jvmMain.dependencies {
            implementation("io.github.dexclub.dexkit:desktop-core:1.0.0")
            implementation(libs.logback.classic)
        }
    }

    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }
}

tasks.named<ProcessResources>("jvmProcessResources") {
    dependsOn(gradle.includedBuild("DexKit").task(":dexkit:copyLibrary"))
    from(rootProject.layout.projectDirectory.dir("dex-engine/vendor/DexKit/dexkit/build/library")) {
        include("**/*.so", "**/*.dll", "**/*.dylib")
        into("natives")
    }
}
