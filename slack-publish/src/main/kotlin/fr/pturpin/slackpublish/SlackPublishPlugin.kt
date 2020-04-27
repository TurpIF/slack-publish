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
 *     dummy {
 *     }
 * }
 * ```
 *
 * Tasks for sending messages to Slack are automatically added to the build. For the previous example this means that
 * `sendDummyToSlack` task is automatically created. New tasks can be added by leveraging the [SlackTask] task type.
 */
class SlackPublishPlugin: Plugin<Project> {
    override fun apply(target: Project) {
        val extension = target.extensions.create("slack", SlackPublishExtension::class.java, target)

        extension.messages.all {
            val taskName = "send${name.capitalize()}MessageToSlack"

            target.tasks.register(taskName, SlackTask::class.java) {
                message.set(this@all)
                group = "slack"
                description = "Send the message `${this@all.name}` to Slack"
            }
        }
    }
}
