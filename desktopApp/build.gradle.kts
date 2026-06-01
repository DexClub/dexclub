import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
}

kotlin {
    jvm()

    sourceSets {
        jvmMain.dependencies {
            implementation(project(":libs:shadcn-ui-compose"))
            implementation(project(":sharedUI"))
            implementation(libs.compose.resources)
            implementation(libs.jewel.decorated.window)
        }
    }
}

compose.desktop {
    application {
        mainClass = "MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Exe, TargetFormat.Deb)
            packageName = "DexClub"
            packageVersion = "1.0.0"

            linux {
                iconFile.set(project.file("appIcons/LinuxIcon.png"))
            }
            windows {
                iconFile.set(project.file("appIcons/WindowsIcon.ico"))
            }
            macOS {
                iconFile.set(project.file("appIcons/MacosIcon.icns"))
                bundleID = "io.github.dexclub.desktop"
            }
        }
    }
}

compose.resources {
    packageOfResClass = "io.github.dexclub.desktop.generated.resources"
    generateResClass = always
}
