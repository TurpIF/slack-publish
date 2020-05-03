plugins {
    `kotlin-dsl`
    id("java-gradle-plugin")
    jacoco
    id("de.jansauer.printcoverage") version "2.0.0"
    id("com.gradle.plugin-publish") version "0.11.0"
}

group = "fr.pturpin.slackpublish"
version = "0.1.0"

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    api("com.slack.api:slack-api-model:1.0.6")
    implementation("com.slack.api:slack-api-client:1.0.6")
    implementation("org.eclipse.jgit:org.eclipse.jgit:5.7.0.202003110725-r")

    testImplementation("junit:junit:4.13")
    testImplementation("com.nhaarman.mockitokotlin2:mockito-kotlin:2.2.0")
    testImplementation("org.assertj:assertj-core:3.15.0")
    testImplementation("org.mock-server:mockserver-junit-rule:5.10.0")
}

tasks.check {
    finalizedBy(tasks.jacocoTestReport)
}

tasks.jacocoTestReport {
    reports {
        xml.isEnabled = true
    }

    finalizedBy(tasks.printCoverage)
}

gradlePlugin {
    plugins {
        create("slackpublish") {
            id = "fr.pturpin.slack-publish"
            displayName = "slack-publish"
            description = "Plugin to support Slack WebHook in Gradle build"
            implementationClass = "fr.pturpin.slackpublish.SlackPublishPlugin"
        }
    }
}

pluginBundle {
    website = "https://gitlab.com/TurpIF/slack-publish"
    vcsUrl = "https://gitlab.com/TurpIF/slack-publish.git"
    tags = listOf("slack", "webhook", "ci", "notify")
}
