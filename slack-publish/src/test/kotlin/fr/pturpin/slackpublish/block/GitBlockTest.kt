package fr.pturpin.slackpublish.block

import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.spy
import com.nhaarman.mockitokotlin2.whenever
import com.slack.api.model.block.DividerBlock
import com.slack.api.model.block.SectionBlock
import com.slack.api.webhook.Payload
import fr.pturpin.slackpublish.SlackMessage
import fr.pturpin.slackpublish.SlackPublishPlugin
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatCode
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.errors.RepositoryNotFoundException
import org.gradle.api.InvalidUserDataException
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class GitBlockTest {

    @Rule
    @JvmField
    val projectDir = TemporaryFolder()

    @Test
    fun root_GivenProjectWithoutParentGitRepositoryInProjectDir_UseGitRepositoryInProjectDir() {
        val project = project()

        gitInit().commit().setMessage("commit").call()

        val gitBlock = GitBlock(project)
        val commitMessage = gitBlock.lastCommitShortMessage()

        assertThat(commitMessage).isEqualTo("commit")
    }

    @Test
    fun root_GivenProjectWithGrandParentAndGitRepositoryInProjectDir_UseGitRepositoryInProjectDir() {
        val project = projectWithGrandParent()

        gitInit(project.projectDir).commit().setMessage("commit").call()

        val gitBlock = GitBlock(project)
        val commitMessage = gitBlock.lastCommitShortMessage()

        assertThat(commitMessage).isEqualTo("commit")
    }

    @Test
    fun root_GivenProjectWithGrandParentAndGitRepositoryInBothProjectDirAndGrandParentDir_UseGitRepositoryInProjectDir() {
        val project = projectWithGrandParent()

        gitInit(projectDir.root).commit().setMessage("commit1").call()
        gitInit(project.projectDir).commit().setMessage("commit2").call()

        val gitBlock = GitBlock(project)
        val commitMessage = gitBlock.lastCommitShortMessage()

        assertThat(commitMessage).isEqualTo("commit2")
    }

    @Test
    fun root_GivenProjectWithGrandParentAndGitRepositoryGrandParentDir_UseGitRepositoryInGrandParent() {
        val project = projectWithGrandParent()

        gitInit(projectDir.root).commit().setMessage("commit").call()

        val gitBlock = GitBlock(project)
        val commitMessage = gitBlock.lastCommitShortMessage()

        assertThat(commitMessage).isEqualTo("commit")
    }

    @Test
    fun root_GivenProjectAndGitRepositoryAboveParentDir_UseGitRepositoryAbove() {
        val project = project(projectDir.newFolder("project"))

        gitInit(projectDir.root).commit().setMessage("commit").call()

        val gitBlock = GitBlock(project)
        val commitMessage = gitBlock.lastCommitShortMessage()

        assertThat(commitMessage).isEqualTo("commit")
    }

    @Test
    fun root_GivenProjectWithGitRepositoryAndGivenOtherDirAsGitRoot_UseGivenOne() {
        val project = project(projectDir.newFolder("project"))

        gitInit(projectDir.root.resolve("project")).commit().setMessage("commit1").call()
        gitInit(projectDir.root.resolve("git")).commit().setMessage("commit2").call()

        val gitBlock = GitBlock(project)
        gitBlock.root.set("../git")
        val commitMessage = gitBlock.lastCommitShortMessage()

        assertThat(commitMessage).isEqualTo("commit2")
    }

    @Test
    fun currentBranchName_GivenNoRepository_ThrowException() {
        val project = project()

        val gitBlock = GitBlock(project)

        assertThatCode {
            gitBlock.currentBranchName()
        }.isInstanceOf(InvalidUserDataException::class.java)
            .hasCauseInstanceOf(RepositoryNotFoundException::class.java)
    }

    @Test
    fun currentBranchName_GivenEmptyRepository_ReturnMaster() {
        val project = project()

        gitInit()

        val gitBlock = GitBlock(project)
        val branchName = gitBlock.currentBranchName()

        assertThat(branchName).isEqualTo("master")
    }

    @Test
    fun currentBranchName_GivenRepositoryOnBranch_ReturnBranchName() {
        val project = project()

        val git = gitInit()
        git.commit().setMessage("init").call()
        git.checkout().setCreateBranch(true).setName("myBranch").call()
        git.checkout().setCreateBranch(true).setName("myBranch2").call()

        val gitBlock = GitBlock(project)
        val branchName = gitBlock.currentBranchName()

        assertThat(branchName).isEqualTo("myBranch2")
    }

    @Test
    fun currentBranchName_GivenRepositoryOnDetachedHead_ReturnSha1() {
        val project = project()

        val git = gitInit()
        val revCommit = git.commit().setMessage("init").call()
        git.checkout().setName(revCommit.name).call()

        val gitBlock = GitBlock(project)
        val branchName = gitBlock.currentBranchName()

        assertThat(branchName).isEqualTo(revCommit.name)
    }

    @Test
    fun lastCommitDescription_GivenEmptyRepository_ReturnMaster() {
        val project = project()

        gitInit()

        val gitBlock = GitBlock(project)
        val branchName = gitBlock.lastCommitDescribe()

        assertThat(branchName).isEqualTo("refs/heads/master")
    }

    @Test
    fun lastCommitDescription_GivenRepositoryOnBranch_ReturnBranchName() {
        val project = project()

        val git = gitInit()
        git.commit().setMessage("init").call()
        git.checkout().setCreateBranch(true).setName("myBranch").call()
        git.commit().setMessage("init2").call()
        git.checkout().setCreateBranch(true).setName("myBranch2").call()

        val gitBlock = GitBlock(project)
        val branchName = gitBlock.lastCommitDescribe()

        assertThat(branchName).isEqualTo("refs/heads/myBranch")
    }

    @Test
    fun lastCommitDescription_GivenRepositoryOnDetachedHead_ReturnBranchName() {
        val project = project()

        val git = gitInit()
        val revCommit = git.commit().setMessage("init").call()
        git.checkout().setName(revCommit.name).call()

        val gitBlock = GitBlock(project)
        val branchName = gitBlock.lastCommitDescribe()

        assertThat(branchName).isEqualTo("refs/heads/master")
    }

    @Test
    fun lastCommitSha1_GivenEmptyRepository_ReturnNull() {
        val project = project()

        gitInit()

        val gitBlock = GitBlock(project)
        val sha1 = gitBlock.lastCommitSha1()

        assertThat(sha1).isNull()
    }

    @Test
    fun lastCommitSha1_GivenNotRepository_ReturnSha1() {
        val project = project()

        val git = gitInit()
        val revCommit = git.commit().setMessage("init").call()

        val gitBlock = GitBlock(project)
        val sha1 = gitBlock.lastCommitSha1()

        assertThat(sha1).isEqualTo(revCommit.name)
    }

    @Test
    fun lastCommitAuthorEmail_GivenEmptyRepository_ReturnNull() {
        val project = project()

        gitInit()

        val gitBlock = GitBlock(project)
        val email = gitBlock.lastCommitAuthorEmail()

        assertThat(email).isNull()
    }

    @Test
    fun lastCommitAuthorEmail_GivenNotEmptyRepository_ReturnLastEmail() {
        val project = project()

        val git = gitInit()
        git.commit().setMessage("init").setAuthor("name", "author@email.com").call()

        val gitBlock = GitBlock(project)
        val email = gitBlock.lastCommitAuthorEmail()

        assertThat(email).isEqualTo("author@email.com")
    }

    @Test
    fun lastCommitShortMessage_GivenEmptyRepository_ReturnNull() {
        val project = project()

        gitInit()

        val gitBlock = GitBlock(project)
        val message = gitBlock.lastCommitShortMessage()

        assertThat(message).isNull()
    }

    @Test
    fun lastCommitShortMessage_GivenNotEmptyRepository_ReturnMessage() {
        val project = project()

        val git = gitInit()
        git.commit().setMessage("the first line\n\nthe description lines...").call()

        val gitBlock = GitBlock(project)
        val message = gitBlock.lastCommitShortMessage()

        assertThat(message).isEqualTo("the first line")
    }

    @Test
    fun format_GivenANewOne_UseIt() {
        val project = project()

        val message = SlackMessage("name", project)
        val gitBlock = GitBlock(project)

        gitBlock.format {
            section {
                blockId = "42"
            }
        }

        gitBlock.format(message)
        val payload = message.payload.get()

        assertThat(payload).isEqualTo(Payload.builder()
            .blocks(listOf(SectionBlock.builder().blockId("42").build()))
            .build())
    }

    @Test
    fun defaultFormat_GivenNoFormat_DefaultOneIsSet() {
        val project = project()

        val message = SlackMessage("name", project)

        val gitBlock = spy(GitBlock(project)) {
            doReturn("myBranch").whenever(mock).currentBranchName()
            doReturn("author@email.com").whenever(mock).lastCommitAuthorEmail()
            doReturn("commit message").whenever(mock).lastCommitShortMessage()
            doReturn("92cfceb39d57d914ed8b14d0e37643de0797ae56").whenever(mock).lastCommitSha1()
        }

        // Add a section before to show that divider is added
        message.section {
            blockId = "42"
        }

        gitBlock.format(message)
        val payload = message.payload.get()

        // Equality on Markdown object is broken, so comparison is fastidious
        val blocks = payload.blocks
        payload.blocks = null
        assertThat(payload).isEqualTo(Payload.builder().build())
        assertThat(blocks).hasSize(3)
        assertThat(blocks[0]).isEqualTo(SectionBlock.builder().blockId("42").build())
        assertThat(blocks[1]).isEqualTo(DividerBlock())
        assertThat(blocks[2]).isInstanceOf(SectionBlock::class.java)

        val block = blocks[2] as SectionBlock
        val fields = block.fields
        block.fields = null

        assertThat(block).isEqualTo(SectionBlock.builder().build())
        assertThat(fields).hasSize(4)
        fields[0].assertIsMarkdown("*Git Branch*\nmyBranch")
        fields[1].assertIsMarkdown("*Git Author*\n<mailto:author@email.com|author@email.com>")
        fields[2].assertIsMarkdown("*Git Commit*\ncommit message")
        fields[3].assertIsMarkdown("*Git SHA-1*\n`92cfceb39d57d914ed8b14d0e37643de0797ae56`")
    }

    private fun gitInit(root: File = projectDir.root): Git {
        return Git.init().setDirectory(root).call()
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