package fr.pturpin.slackpublish.block

import com.nhaarman.mockitokotlin2.*
import com.slack.api.model.block.SectionBlock
import com.slack.api.model.block.composition.MarkdownTextObject
import fr.pturpin.slackpublish.SlackMessage
import fr.pturpin.slackpublish.SlackPublishPlugin
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatCode
import org.gradle.api.InvalidUserDataException
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.FileNotFoundException

class ChangelogBlockTest {

    companion object {
        const val CHANGELOG = "CHANGELOG.md"
    }

    @Rule
    @JvmField
    val projectDir = TemporaryFolder()

    @Test
    fun file_GivenChangelogFile_ReturnIt() {
        val project = project()
        val changelogBlock = ChangelogBlock(project)

        changelogBlock.changelogFile.set("./myCHANGELOG.md")
        val file = changelogBlock.file.get()

        assertThat(file).isEqualTo(project.projectDir.resolve("myCHANGELOG.md"))
    }

    @Test
    fun file_GivenNoGivenChangelogPath_FallbackOnDefaultFile() {
        val project = project()
        val changelogBlock = ChangelogBlock(project)

        val file = changelogBlock.file.get()

        assertThat(file).isEqualTo(project.projectDir.resolve(CHANGELOG))
    }

    @Test
    fun file_GivenChangelogInGrandParentAndParentProjectDirectory_FallbackOnTheParentOne() {
        val project = projectWithGrandParent()
        val changelogBlock = ChangelogBlock(project)

        project.parent!!.projectDir.resolve(CHANGELOG).createNewFile()
        project.parent!!.parent!!.projectDir.resolve(CHANGELOG).createNewFile()

        val file = changelogBlock.file.get()
        assertThat(file).isEqualTo(project.parent!!.projectDir.resolve(CHANGELOG))
    }

    @Test
    fun file_GivenChangelogInGrandParentProjectDirectory_FallbackOnIt() {
        val project = projectWithGrandParent()
        val changelogBlock = ChangelogBlock(project)

        project.parent!!.parent!!.projectDir.resolve(CHANGELOG).createNewFile()

        val file = changelogBlock.file.get()
        assertThat(file).isEqualTo(project.parent!!.parent!!.projectDir.resolve(CHANGELOG))
    }

    @Test
    fun version_GivenNoVersion_FallbackOnProjectVersion() {
        val project = project()
        val changelogBlock = ChangelogBlock(project)

        project.version = "42"
        val version = changelogBlock.version.get()

        assertThat(version).isEqualTo("42")
    }

    @Test
    fun changelog_GivenNoChangelogFile_ThrowException() {
        val project = project()
        val changelogBlock = ChangelogBlock(project)

        changelogBlock.versionLinesStartWith("##")

        assertThatCode {
            changelogBlock.changelog.get()
        }.isInstanceOf(FileNotFoundException::class.java)
    }

    @Test
    fun changelog_GivenNoIndicationToReadFile_ThrowException() {
        val project = project()
        val changelogBlock = ChangelogBlock(project)

        project.projectDir.resolve(CHANGELOG).createNewFile()

        assertThatCode {
            changelogBlock.changelog.get()
        }.isInstanceOf(InvalidUserDataException::class.java)
    }

    @Test
    fun changelog_GivenChangelogFileButVersionIsNotFound_ReturnEmpty() {
        val project = project()
        val changelogBlock = ChangelogBlock(project)

        project.projectDir.resolve(CHANGELOG).createNewFile()
        changelogBlock.versionLinesStartWith("#")

        val changelog = changelogBlock.changelog.get()

        assertThat(changelog).isEmpty()
    }

    @Test
    fun changelog_GivenChangelogFileAndVersionIsPresent_ExtractSectionForThisVersion() {
        val project = project()
        val changelogBlock = ChangelogBlock(project)

        project.projectDir.resolve(CHANGELOG).apply {
            createNewFile()
            writeText("""
                ## Version 2.0.2
                 - Fix 1
                ## Version 2.0.1
                  - Feature 1
                    - Feature 1.a
                    - Feature 1.b
                  - Feature 2
                  - ...
                  
                ## Version 2.0.0
                  - ...
            """.trimIndent())
        }

        changelogBlock.versionLinesStartWith("## Version")
        project.version = "2.0.1"

        val changelog = changelogBlock.changelog.get()

        assertThat(changelog).isEqualTo("""
            - Feature 1
              - Feature 1.a
              - Feature 1.b
            - Feature 2
            - ...
        """.trimIndent())
    }

    @Test
    fun changelog_GivenAllInformationAndVersionIsPresent_ExtractSectionForThisVersion2() {
        val project = project()
        val changelogBlock = ChangelogBlock(project)

        project.version = "2.0.2"
        changelogBlock.version.set("2.0.1")
        changelogBlock.changelogFile.set("myCHANGELOG.md")
        changelogBlock.versionLinesStartWith("## Version")

        project.projectDir.resolve("myCHANGELOG.md").apply {
            createNewFile()
            writeText("""
                ## Version 2.0.2
                 - Fix 1
                ## Version 2.0.1
                  - Feature 1
                    - Feature 1.a
                    - Feature 1.b
                  - Feature 2
                  - ...
                  
                ## Version 2.0.0
                  - ...
            """.trimIndent())
        }

        val changelog = changelogBlock.changelog.get()

        assertThat(changelog).isEqualTo("""
            - Feature 1
              - Feature 1.a
              - Feature 1.b
            - Feature 2
            - ...
        """.trimIndent())
    }

    @Test
    fun defaultFormat_GivenChangelogInError_ThrowTheError() {
        val project = project()
        val changelogBlock = ChangelogBlock(project)

        assertThatCode {
            changelogBlock.format(mock<SlackMessage>())
        }.isNotNull()
    }

    @Test
    fun defaultFormat_GivenChangelogWithVersionPresent_FormatTheChangelog() {
        val project = project()
        val changelogBlock = ChangelogBlock(project)

        project.projectDir.resolve(CHANGELOG).apply {
            createNewFile()
            writeText("""
                # 1.0
                - Fix 1
            """.trimIndent())
        }

        changelogBlock.versionLinesStartWith("#")
        changelogBlock.version.set("1.0")

        val message = mock<SlackMessage>()
        changelogBlock.format(message)

        verify(message).section(eq(true), check {
            val section = mock<SectionBlock>()
            it(section)

            verify(section).text = check {
                assertThat(it).isInstanceOf(MarkdownTextObject::class.java)
                assertThat((it as MarkdownTextObject).text).isEqualTo("*Changelog*\n- Fix 1")
            }
        })
    }

    @Test
    fun defaultFormat_GivenChangelogWithoutVersionPresent_DoesNothing() {
        val project = project()
        val changelogBlock = ChangelogBlock(project)

        project.projectDir.resolve(CHANGELOG).apply {
            createNewFile()
            writeText("""
                # 1.0
                - Fix 1
            """.trimIndent())
        }

        changelogBlock.versionLinesStartWith("#")
        changelogBlock.version.set("1.1")

        val message = mock<SlackMessage>()
        changelogBlock.format(message)

        verifyZeroInteractions(message)
    }

    private fun project(root: File = projectDir.root): Project {
        val project = ProjectBuilder.builder()
            .withProjectDir(root)
            .build()

        project.pluginManager.apply(SlackPublishPlugin::class.java)
        return project
    }

    private fun projectWithGrandParent(): Project {
        val grandParentProject = ProjectBuilder.builder()
            .withProjectDir(projectDir.root)
            .build()

        val parentProject = ProjectBuilder.builder()
            .withProjectDir(projectDir.newFolder("child"))
            .withParent(grandParentProject)
            .build()

        val project = ProjectBuilder.builder()
            .withProjectDir(projectDir.newFolder("child", "grandchild"))
            .withParent(parentProject)
            .build()

        project.pluginManager.apply(SlackPublishPlugin::class.java)
        return project
    }

}