package fr.pturpin.slackpublish

import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Plugin to add Slack support to Gradle builds.
 *
 * Exposes an `slack` extension that allows the user to configure several messages that can be sent to Slack.
 *
 * ```groovy
 * slack {
 * }
 * ```
 */
class SlackPublishPlugin: Plugin<Project> {
    override fun apply(target: Project) {
        target.extensions.create("slack", SlackPublishExtension::class.java)
    }
}