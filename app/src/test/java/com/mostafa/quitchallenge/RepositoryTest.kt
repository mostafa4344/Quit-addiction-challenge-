package com.mostafa.quitchallenge

import com.mostafa.quitchallenge.data.Category
import com.mostafa.quitchallenge.data.Challenge
import com.mostafa.quitchallenge.data.Repository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class RepositoryTest {
    @get:Rule
    val tmp = TemporaryFolder()

    private fun challenge(id: String) =
        Challenge(id = id, name = "Test $id", category = Category.OTHER, goalHours = 24, createdAt = 0, currentStart = 0)

    @Test
    fun dataSurvivesARestart() {
        val file = File(tmp.root, "data.json")
        Repository(file).update { it.copy(challenges = listOf(challenge("a"))) }
        assertEquals(listOf("a"), Repository(file).data.value.challenges.map { it.id })
    }

    @Test
    fun unreadableDataIsKeptNotOverwritten() {
        val file = File(tmp.root, "data.json")
        file.writeText("{ this is not valid json")
        val repo = Repository(file)
        assertTrue(repo.data.value.challenges.isEmpty())
        repo.update { it.copy(challenges = listOf(challenge("new"))) }

        val kept = tmp.root.listFiles()!!.filter { it.name.startsWith("data.json.unreadable-") }
        assertEquals(1, kept.size)
        assertEquals("{ this is not valid json", kept.single().readText())
    }

    @Test
    fun aFailedSaveDoesNotCrash() {
        // The data "file" is a folder, so every write fails.
        val file = File(tmp.root, "data.json").apply { mkdirs() }
        val repo = Repository(file)
        repo.update { it.copy(challenges = listOf(challenge("a"))) }
        assertEquals(listOf("a"), repo.data.value.challenges.map { it.id })
    }

    @Test
    fun unknownFieldsFromNewerVersionsAreIgnored() {
        val file = File(tmp.root, "data.json")
        file.writeText("""{"challenges":[],"urges":[],"someFutureField":42}""")
        assertTrue(Repository(file).data.value.challenges.isEmpty())
        assertTrue(tmp.root.listFiles()!!.none { it.name.contains("unreadable") })
    }
}
