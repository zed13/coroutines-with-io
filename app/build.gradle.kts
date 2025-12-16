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

    // Serialization & Logging
    implementation(libs.ktor.serialization.json)
    implementation(libs.kotlinx.serialization.json)
//    implementation(libs.logback.classic)

    implementation("com.squareup.retrofit2:retrofit:3.0.0")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

application {
    mainClass = "org.example.AppKt"
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}
