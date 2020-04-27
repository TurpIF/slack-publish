plugins {
    `kotlin-dsl`
    id("java-gradle-plugin")
}

group = "fr.pturpin.slackpublish"
version = "1.0-SNAPSHOT"

dependencies {
    implementation(kotlin("stdlib-jdk8"))

    testImplementation("junit:junit:4.13")
    testImplementation("com.nhaarman.mockitokotlin2:mockito-kotlin:2.2.0")
    testImplementation("org.assertj:assertj-core:3.15.0")
}

gradlePlugin {
    plugins {
        create("slackpublish") {
            id = "fr.pturpin.slack-publish"
            implementationClass = "fr.pturpin.slackpublish.SlackPublishPlugin"
        }
    }
}
