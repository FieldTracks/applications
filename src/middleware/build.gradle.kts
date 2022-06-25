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
    implementation("org.eclipse.paho:org.eclipse.paho.client.mqttv3:1.2.5")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.13.3")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.13.3")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.13.3")
    implementation("org.apache.logging.log4j:log4j-api:2.17.2")
    implementation("org.apache.logging.log4j:log4j-core:2.17.2")
    implementation("org.apache.logging.log4j:log4j-slf4j-impl:2.17.2")
    implementation("com.github.ajalt.clikt:clikt:3.5.0")
    implementation("io.vertx:vertx-web:4.3.1")
    implementation("io.vertx:vertx-auth-jwt:4.3.1")
//    implementation("org.jboss.resteasy:resteasy-vertx:6.0.1.Final")
//    implementation("com.fasterxml.jackson.jaxrs:jackson-jaxrs-json-provider:2.13.3")
 //   implementation("org.jboss.resteasy:resteasy-jackson2-provider:6.1.0.Beta2")
//    implementation("org.jboss.resteasy:resteasy-jaxb-provider:6.0.1.Final")
//    implementation("org.jboss.spec.javax.xml.bind:jboss-jaxb-api_2.3_spec:2.0.1.Final")

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
