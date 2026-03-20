plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.multiplatform.library)
    `maven-publish`
}

kotlin {
    explicitApi()

    android {
        namespace = "io.github.dexclub.codeview.treesitter.java"
        compileSdk = 36
        minSdk = 24
    }

    jvm()

    sourceSets {
        commonMain {
            dependencies {
                api(project(":code-view-language"))
                api(project(":code-view-tree-sitter"))
                implementation(libs.ktreesitter)
                implementation(project(":code-view-tree-sitter-java:ktreesitter-java"))
            }
        }
    }
}

group = "io.github.dexclub"
version = libs.versions.code.view.get()

publishing {
    publications.withType<MavenPublication> {
        pom {
            name.set("code-view-tree-sitter-java")
            description.set("code-view Java language pack (Tree-sitter)")
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
