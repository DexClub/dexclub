import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import javax.inject.Inject

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.multiplatform.library)
    `maven-publish`
}

abstract class SyncTreeSitterQueryAssetsTask : DefaultTask() {
    @get:Inject
    abstract val fileSystemOperations: FileSystemOperations

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val sourceDir: DirectoryProperty

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun syncAssets() {
        fileSystemOperations.sync {
            from(sourceDir)
            include("io/github/dexclub/codeview/treesitter/**")
            into(outputDir)
        }
    }
}

kotlin {
    android {
        namespace = "io.github.dexclub.codeview.treesitter.kotlin"
        compileSdk = 36
        minSdk = 24
        androidResources.enable = true
    }

    jvm()

    sourceSets {
        commonMain {
            dependencies {
                api(project(":code-view-language"))
                implementation(project(":code-view-tree-sitter"))
                implementation(project(":code-view-tree-sitter-kotlin:ktreesitter-kotlin"))
            }
        }
    }
}

group = "io.github.dexclub"
version = libs.versions.code.view.get()

publishing {
    publications.withType<MavenPublication> {
        pom {
            name.set("code-view-tree-sitter-kotlin")
            description.set("code-view Kotlin language pack (Tree-sitter)")
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

androidComponents {
    onVariants { variant ->
        if (variant.name != "androidMain") {
            return@onVariants
        }

        val syncQueryAssets = tasks.register<SyncTreeSitterQueryAssetsTask>("syncAndroidMainTreeSitterQueryAssets") {
            sourceDir.set(layout.projectDirectory.dir("src/commonMain/resources"))
        }

        variant.sources.assets?.addGeneratedSourceDirectory(
            syncQueryAssets,
            SyncTreeSitterQueryAssetsTask::outputDir,
        )
    }
}
