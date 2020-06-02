import com.slack.api.model.block.Blocks
import com.slack.api.model.block.composition.BlockCompositions.markdownText

plugins {
    id("fr.pturpin.slack-publish") // version <latest>
}

slack {
    messages {
        register("block") {
            webHook.set(project.properties["webHook"] as String)

            payload {
                // Configure meta information:
                channel = "#alerts"
                username = "Good Bot"
                iconEmoji = ":robot:"

                // Or take full control over the payload
            }

            section {
                text = markdownText("This is my *first* section :+1:")
            }

            fields {
                insertDivider.set(false)
                field(":rocket:", "Field 1")
                field(":tada:", "Field 2")
            }

            context {
                markdown("A context line")
                image("http://my.image/path", "alt text")
            }

            block(insertDivider = false) {
                // Do unsupported operations
                Blocks.divider()
            }
        }

        register("git") {
            webHook.set(project.properties["webHook"] as String)

            git {
                format {
                    fields {
                        field(":rocket:", "${currentBranchName()}")
                    }
                }
            }
        }

        register("publication") {
            webHook.set(project.properties["webHook"] as String)

            publication {

            }
        }

        register("changelog") {
            webHook.set(project.properties["webHook"] as String)

            changelog {
                format {

                }
            }
        }
    }
}
