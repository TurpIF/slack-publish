package fr.pturpin.slackpublish

import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project

open class SlackPublishExtension(project: Project) {

    val messages: SlackMessageContainer = project.container(
        SlackMessage::class.java) {
        SlackMessage(it, project)
    }

    fun messages(configure: SlackMessageContainer.() -> Unit) {
        configure(messages)
    }

}

internal val Project.slack: SlackPublishExtension
    get() = extensions.getByType(
        SlackPublishExtension::class.java)

internal typealias SlackMessageContainer = NamedDomainObjectContainer<SlackMessage>