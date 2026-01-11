plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    application
}

repositories {
    mavenCentral()
}

dependencies {
    // Testing
    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation(libs.junit.jupiter.engine)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    // Ktor Server
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.jetty)
    implementation(libs.ktor.server.content.negotiation)
//    implementation(libs.ktor.server.call.logging)

    // Ktor Client
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.client.logging)

    // Serialization & Logging
    implementation(libs.ktor.serialization.json)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.logback.classic)

    // Retrofit
    implementation("com.squareup.retrofit2:retrofit:3.0.0")
    implementation("com.squareup.retrofit2:converter-kotlinx-serialization:3.0.0")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

application {
    mainClass = "test.io.server.AppKt"
}

tasks.register<JavaExec>("runClient") {
    group = "application"
    description = "Run the client test suite"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("test.io.client.Client")
    standardInput = System.`in`
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}
