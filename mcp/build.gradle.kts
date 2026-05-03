plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    application
}

kotlin {
    jvmToolchain(21)
}

application {
    mainClass = "io.github.dexclub.mcp.MainKt"
}

dependencies {
    implementation(project(":core"))
    implementation(platform(libs.ktor.bom))
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.mcp.kotlin.sdk.server)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.server.double.receive)
    implementation(libs.ktor.serialization.kotlinx.json)
    runtimeOnly(libs.slf4j.simple)

    testImplementation(kotlin("test"))
    testImplementation(project(":dexkit"))
    testImplementation(libs.ktor.client.content.negotiation)
    testImplementation(libs.ktor.client.cio)
    testImplementation(libs.ktor.serialization.kotlinx.json)
}

tasks.test {
    useJUnitPlatform()
}
