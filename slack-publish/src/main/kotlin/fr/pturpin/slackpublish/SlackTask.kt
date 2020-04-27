package fr.pturpin.slackpublish

import com.slack.api.Slack
import org.gradle.api.DefaultTask
import org.gradle.api.InvalidUserDataException
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction

open class SlackTask: DefaultTask() {

    @Input
    val messages: ListProperty<SlackMessage> = project.objects.listProperty(SlackMessage::class.java)

    private val slack: Slack by lazy { Slack.getInstance() }

    fun message(name: String, configure: SlackMessage.() -> Unit) {
        messages.add(project.provider {
            val message = SlackMessage(name, project)
            configure(message)
            message
        })
    }

    @TaskAction
    fun sendMessages() {
        val slackMessages = messages.get()
        if (slackMessages.isEmpty()) {
            throw InvalidUserDataException("No slack message declared in $this")
        }

        slackMessages.forEach {
            slack.send(it.webHook.get(), it.payload.get())
        }
    }

}