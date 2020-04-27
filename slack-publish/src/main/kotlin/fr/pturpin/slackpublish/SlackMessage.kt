package fr.pturpin.slackpublish

import com.slack.api.webhook.Payload
import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider

class SlackMessage(val name: String, project: Project) {

    val webHook: Property<String> = project.objects.property(String::class.java)
    internal val payload: Provider<Payload> = project.provider { Payload.builder().build() }

}