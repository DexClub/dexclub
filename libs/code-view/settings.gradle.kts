rootProject.name = "code-view"

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

include(":code-view-bom")
include(":code-view-compose")
include(":code-view-core")
include(":code-view-language")
include(":code-view-runtime")

include(":code-view-tree-sitter")

include(":code-view-tree-sitter-java")
include(":code-view-tree-sitter-java:ktreesitter-java")

include(":code-view-tree-sitter-kotlin")
include(":code-view-tree-sitter-kotlin:ktreesitter-kotlin")

include(":code-view-tree-sitter-smali")
include(":code-view-tree-sitter-smali:ktreesitter-smali")
