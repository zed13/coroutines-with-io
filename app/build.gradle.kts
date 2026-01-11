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
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.12.1")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    // Ktor Server
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.jetty)
    implementation(libs.ktor.server.content.negotiation)

    // Ktor Client
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.client.logging)

    // Serialization
    implementation(libs.ktor.serialization.json)
    implementation(libs.kotlinx.serialization.json)

    // SLF4J no-op implementation (silences all framework logs)
    implementation("org.slf4j:slf4j-nop:2.0.9")

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
    mainClass = "test.io.runner.RunnerKt"
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
