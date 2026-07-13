pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "dexclub"

include(":cli-app")
include(":app-service")
include(":domain-core")
include(":mcp-app")

includeBuild("dexkit-binding") {
    dependencySubstitution {
        substitute(module("io.github.dexclub:dexkit-binding")).using(project(":"))
    }
}
