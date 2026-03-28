import org.gradle.jvm.tasks.Jar
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.shadow)
    application
}

kotlin {
    jvmToolchain(21)
}

application {
    mainClass = "io.github.dexclub.dexengine.cli.MainKt"
}

dependencies {
    implementation(project(":dex-engine"))
    implementation(libs.filekit.core)
    implementation(libs.kotlinx.coroutines.core)
}

tasks.named<Jar>("jar") {
    manifest {
        attributes["Main-Class"] = application.mainClass.get()
    }
}

tasks.named<ShadowJar>("shadowJar") {
    group = "build"
    description = "打包 dex-engine CLI 可执行 fat jar"
    archiveBaseName.set("dex-engine-cli")
    archiveClassifier.set("all")
    manifest {
        attributes["Main-Class"] = application.mainClass.get()
    }
    mergeServiceFiles()
}

tasks.register("fatJar") {
    group = "build"
    description = "打包 dex-engine CLI 可执行 fat jar"
    dependsOn(tasks.named("shadowJar"))
}
