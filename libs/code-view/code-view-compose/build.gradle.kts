plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.multiplatform.library)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    `maven-publish`
}

kotlin {
    explicitApi()

    android {
        namespace = "io.github.dexclub.codeview.compose"
        compileSdk = 36
        minSdk = 24
    }

    jvm()

    sourceSets {
        commonMain {
            dependencies {
                api(project(":code-view-core"))
                api(project(":code-view-runtime"))
                implementation(libs.compose.runtime)
                implementation(libs.compose.foundation)
                implementation(libs.compose.ui)
                implementation(libs.kotlinx.coroutines.core)
            }
        }

        commonTest {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}

group = "io.github.dexclub"
version = libs.versions.code.view.get()

publishing {
    publications.withType<MavenPublication> {
        pom {
            name.set("code-view-compose")
            description.set("code-view Compose UI components")
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
