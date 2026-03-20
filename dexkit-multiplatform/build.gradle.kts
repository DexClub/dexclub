import org.gradle.language.jvm.tasks.ProcessResources

plugins {
    alias(libs.plugins.android.multiplatform.library)
    alias(libs.plugins.kotlin.multiplatform)
}

kotlin {
    android {
        namespace = "io.github.dexclub.dexkit"
        compileSdk = 36
        minSdk = 24
    }

    jvm()

    sourceSets {
        androidMain.dependencies {
            implementation("io.github.dexclub.dexkit:android-core:1.0.0")
        }

        jvmMain.dependencies {
            implementation("io.github.dexclub.dexkit:desktop-core:1.0.0")
        }
    }

    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }
}

tasks.named<ProcessResources>("jvmProcessResources") {
    dependsOn(gradle.includedBuild("DexKit").task(":dexkit:copyLibrary"))
    from(rootProject.layout.projectDirectory.dir("dexkit-multiplatform/DexKit/dexkit/build/library")) {
        include("**/*.so", "**/*.dll", "**/*.dylib")
        into("natives")
    }
}
