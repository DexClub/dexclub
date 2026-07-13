import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.testing.Test

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    `java-library`
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    api(project(":domain-core"))
    implementation(libs.kotlinx.serialization.json)

    testImplementation(kotlin("test"))
}

val appServiceTestSourceSet = the<SourceSetContainer>()["test"]

fun registerAppServiceTestTask(
    name: String,
    description: String,
    vararg classNames: String,
) = tasks.register<Test>(name) {
    group = "verification"
    this.description = description
    testClassesDirs = appServiceTestSourceSet.output.classesDirs
    classpath = appServiceTestSourceSet.runtimeClasspath
    useJUnitPlatform()
    filter {
        classNames.forEach(::includeTestsMatching)
    }
}

tasks.test {
    useJUnitPlatform()
}

tasks.register("testFast") {
    group = "verification"
    description = "Run the fast app-service verification suite"
    dependsOn(tasks.test)
}

val testWorkspace by registerAppServiceTestTask(
    name = "testWorkspace",
    description = "Run app-service application wiring and workspace tests",
    "io.github.dexclub.core.app.AppUseCasesTest",
    "io.github.dexclub.core.app.AppRuntimeTest",
)

val testSession by registerAppServiceTestTask(
    name = "testSession",
    description = "Run app-service session runtime tests",
    "io.github.dexclub.core.app.session.TargetSessionRuntimeTest",
)

tasks.register("testStructured") {
    group = "verification"
    description = "Run all split app-service test tasks by responsibility"
    dependsOn(testWorkspace, testSession)
}
