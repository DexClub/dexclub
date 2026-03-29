rootProject.name = "DexClub"

pluginManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

include(":sharedUI")
include(":androidApp")
include(":desktopApp")

include(":libs:shadcn-ui-compose")

includeBuild("libs/code-view") {
    dependencySubstitution {
        substitute(module("io.github.dexclub:code-view-bom")).using(project(":code-view-bom"))
        substitute(module("io.github.dexclub:code-view-compose")).using(project(":code-view-compose"))
        substitute(module("io.github.dexclub:code-view-core")).using(project(":code-view-core"))
        substitute(module("io.github.dexclub:code-view-language")).using(project(":code-view-language"))
        substitute(module("io.github.dexclub:code-view-runtime")).using(project(":code-view-runtime"))
        substitute(module("io.github.dexclub:code-view-tree-sitter")).using(project(":code-view-tree-sitter"))
        substitute(module("io.github.dexclub:code-view-tree-sitter-java")).using(project(":code-view-tree-sitter-java"))
        substitute(module("io.github.dexclub:code-view-tree-sitter-kotlin")).using(project(":code-view-tree-sitter-kotlin"))
        substitute(module("io.github.dexclub:code-view-tree-sitter-smali")).using(project(":code-view-tree-sitter-smali"))
    }
}

include(":libs:dex-engine")
include(":libs:dex-engine:cli")

includeBuild("libs/dex-engine/vendor/DexKit") {
    dependencySubstitution {
        substitute(module("io.github.dexclub.dexkit:desktop-core")).using(project(":dexkit"))
        substitute(module("io.github.dexclub.dexkit:android-core")).using(project(":dexkit-android"))
    }
}

