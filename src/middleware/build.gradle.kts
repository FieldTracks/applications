import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.6.21"
    application
    distribution
}

group = "org.fieldtracks"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()

}

dependencies {
    testImplementation(kotlin("test"))
    testImplementation("org.testcontainers:testcontainers:1.17.6")
    implementation("org.eclipse.paho:org.eclipse.paho.client.mqttv3:1.2.5")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.14.1")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.14.1")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.14.1")
    implementation("org.apache.logging.log4j:log4j-api:2.19.0")
    implementation("org.apache.logging.log4j:log4j-core:2.19.0")
    implementation("org.apache.logging.log4j:log4j-slf4j-impl:2.19.0")
    implementation("com.github.ajalt.clikt:clikt:3.5.0")
    implementation("io.vertx:vertx-web:4.3.7")
    implementation("io.vertx:vertx-auth-jwt:4.3.7")
    implementation("io.vertx:vertx-http-proxy:4.3.7")
    implementation("org.jboss.resteasy:resteasy-vertx:6.2.2.Final")
    implementation("org.jboss.resteasy:resteasy-jackson2-provider:6.2.2.Final")


}
tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "11"
}

application {
    mainClass.set("org.fieldtracks.middleware.MainKt")
    applicationDefaultJvmArgs = listOf("--add-opens=java.base/sun.nio.ch=ALL-UNNAMED") // https://github.com/eclipse/paho.mqtt.java/issues/507#issuecomment-814681499
}
