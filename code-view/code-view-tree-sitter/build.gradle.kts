plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.multiplatform.library)
    alias(libs.plugins.kotlinx.serialization)
    `maven-publish`
}

kotlin {
    android {
        namespace = "io.github.dexclub.codeview.treesitter"
        compileSdk = 36
        minSdk = 24
        androidResources.enable = true
    }

    jvm()

    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    sourceSets {
        commonMain {
            dependencies {
                api(project(":code-view-language"))
                implementation(project(":code-view-core"))
                implementation(libs.ktreesitter)
                implementation(libs.kotlinx.serialization.json)
            }
        }
    }
}

group = "io.github.dexclub"
version = libs.versions.code.view.get()

publishing {
    publications.withType<MavenPublication> {
        pom {
            name.set("code-view-tree-sitter")
            description.set("code-view Tree-sitter family bridge")
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
