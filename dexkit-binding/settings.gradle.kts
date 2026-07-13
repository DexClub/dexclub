pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        mavenLocal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenLocal()
        google()
        mavenCentral()
    }
}

rootProject.name = "dexkit-binding"

includeBuild("vendor/DexKit") {
    name = "DexKit"
    dependencySubstitution {
        substitute(module("io.github.dexclub.dexkit:desktop-core")).using(project(":dexkit"))
        substitute(module("io.github.dexclub.dexkit:android-core")).using(project(":dexkit-android"))
    }
}
