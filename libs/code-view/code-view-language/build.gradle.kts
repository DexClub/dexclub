plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.multiplatform.library)
    `maven-publish`
}

kotlin {
    explicitApi()

    android {
        namespace = "io.github.dexclub.codeview.language"
        compileSdk = 36
        minSdk = 24
    }

    jvm()

    sourceSets {
        commonMain {
            dependencies {
                api(project(":code-view-core"))
            }
        }
    }
}

group = "io.github.dexclub"
version = libs.versions.code.view.get()

publishing {
    publications.withType<MavenPublication> {
        pom {
            name.set("code-view-language-api")
            description.set("code-view language extension SPI")
            url.set("https://github.com/dexclub/code-view")
            licenses {
                license {
                    name.set("Apache-2.0")
                    url.set("https://www.apache.org/licenses/LICENSE-2.0")
                }
            }
        }
    }
}

val forbiddenDependencyPrefixes = listOf(
    "org.jetbrains.compose",
    "io.github.tree-sitter",
)

tasks.register("checkForbiddenDependencies") {
    group = "verification"
    description = "Ensures this module does not depend on Compose or Tree-sitter"
    doLast {
        val violations = mutableListOf<String>()
        configurations.filter { it.isCanBeResolved }.forEach { config ->
            runCatching {
                config.resolvedConfiguration.resolvedArtifacts.forEach { artifact ->
                    val notation = "${artifact.moduleVersion.id.group}:${artifact.moduleVersion.id.name}"
                    if (forbiddenDependencyPrefixes.any { notation.startsWith(it) }) {
                        violations += "[${config.name}] $notation"
                    }
                }
            }
        }
        if (violations.isNotEmpty()) {
            throw GradleException(
                "Forbidden dependencies in ${project.name}:\n" +
                violations.joinToString("\n") { "  - $it" }
            )
        }
    }
}

tasks.named("check") { dependsOn("checkForbiddenDependencies") }
