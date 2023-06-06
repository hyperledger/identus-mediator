plugins {
    kotlin("jvm") version "1.8.21"
    idea
    jacoco
    id("net.serenity-bdd.serenity-gradle-plugin") version "3.4.2"
}

group = "io.iog.mediator.tests"
version = "0.1.0-SNAPSHOT"

repositories {
    mavenLocal()
    mavenCentral()
    maven {
        this.url = uri("https://maven.pkg.github.com/input-output-hk/atala-prism-wallet-sdk-kmm")
        credentials {
            this.username = System.getenv("ATALA_GITHUB_ACTOR")
            this.password = System.getenv("ATALA_GITHUB_TOKEN")
        }
    }
    maven {
        url = uri("https://maven.pkg.jetbrains.space/public/p/kotlinx-coroutines/maven")
    }
}

dependencies {
    // Logging
    implementation("org.slf4j:slf4j-log4j12:2.0.5")
    // Beautify async waits
    implementation("org.awaitility:awaitility-kotlin:4.2.0")
    // Test engines and reports
    testImplementation("junit:junit:4.13.2")
    implementation("net.serenity-bdd:serenity-core:3.4.3")
    implementation("net.serenity-bdd:serenity-cucumber:3.4.3")
    implementation("net.serenity-bdd:serenity-screenplay-rest:3.4.3")
    // https://mvnrepository.com/artifact/net.serenity-bdd/serenity-ensure
    testImplementation("net.serenity-bdd:serenity-ensure:3.4.3")
    // Beautify exceptions handling assertions
    testImplementation("org.assertj:assertj-core:3.23.1")
    // Navigate through Json with xpath
    testImplementation("com.jayway.jsonpath:json-path:2.7.0")
    implementation("io.iohk.atala.prism.walletsdk:atala-prism-sdk:0.1.1")
    implementation("org.didcommx:didcomm:0.3.0")
    implementation("org.didcommx:peerdid:0.3.0")
    implementation("io.ktor:ktor-server-netty:2.3.0")
    implementation("io.ktor:ktor-client-apache:2.3.0")

    // https://mvnrepository.com/artifact/com.squareup.okhttp3/okhttp
    implementation("com.squareup.okhttp3:okhttp:4.11.0")

}

buildscript {
    dependencies {
        classpath("net.serenity-bdd:serenity-single-page-report:3.4.3")
        classpath("net.serenity-bdd:serenity-json-summary-report:3.4.3")
    }
}

/**
 * Add HTML one-pager and JSON summary report to be produced
 */
serenity {
    reports = listOf("single-page-html", "json-summary")
}

tasks.test {
    testLogging.showStandardStreams = true
    systemProperty("cucumber.filter.tags", System.getProperty("cucumber.filter.tags"))
}

kotlin {
    jvmToolchain(11)
}
