package com.jetbrains.kmpapp.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MemoryFiles : MeruFiles {
    var text: String? = null
    override fun read(): String? = text
    override fun write(text: String) {
        this.text = text
    }
}

class AscentRulesTest {
    @Test
    fun signupKeepsOneCarAndPaysTheLaunchCode() {
        val backend = AscentBackend(MemoryFiles())
        val profile = driver(backend, referral = "MERU")
        assertEquals(180, backend.points())
        assertTrue(profile.referralCode.isNotBlank())
        assertFails { driver(backend, email = "b@meru.app") }
        assertFails { backend.login(profile.email, "wrong-pin") }
    }

    @Test
    fun onePointCannotClaimAndARealTraceCan() {
        val files = MemoryFiles()
        val backend = AscentBackend(files)
        driver(backend, referral = "")
        val ignored = backend.acceptDrive(trace(listOf(31.5204 to 74.3587)))
        assertEquals(0, ignored.score)
        assertEquals(0, backend.chronicle().roadsClaimed)
        assertFalse(backend.road("lahore-canal").claimed)

        val counted = backend.acceptDrive(trace(listOf(31.5204 to 74.3587, 31.521 to 74.359)))
        assertTrue(counted.score > 0)
        assertTrue(backend.road("lahore-canal").claimed)
        assertTrue(backend.story().body.isNotBlank())
        val points = backend.points()

        val restored = AscentBackend(files)
        assertEquals(points, restored.points())
        assertTrue(restored.road("lahore-canal").claimed)
        assertEquals(1, restored.road("lahore-canal").times)
        assertEquals(2, restored.drives().size)
    }

    @Test
    fun signOutSurvivesRestartAndLoginLooksUpTheSession() {
        val files = MemoryFiles()
        val backend = AscentBackend(files)
        val profile = driver(backend)
        backend.signOut()
        val closed = AscentBackend(files)
        assertFalse(closed.hasDriver())
        closed.login(profile.email, "ascent1")
        assertTrue(closed.hasDriver())
        val reopened = AscentBackend(files)
        assertTrue(reopened.hasDriver())
    }

    @Test
    fun convoyCareAndEmbarkStayWithTheCar() {
        val files = MemoryFiles()
        val backend = AscentBackend(files)
        driver(backend)
        val convoy = backend.createConvoy("Sunday Company")
        backend.embark("monsoon-covenant")
        backend.noteCare("Canal Workshop")
        assertFails {
            backend.createConvoy("Two")
            backend.createConvoy("Three")
            backend.createConvoy("Four")
        }

        val restored = AscentBackend(files)
        assertEquals(3, restored.myConvoys().size)
        assertTrue(restored.myConvoys().any { it.code == convoy.code })
        assertEquals(1, restored.chronicle().careMarks.size)
        assertTrue(restored.expedition("monsoon-covenant").embarked)
    }

    @Test
    fun convoyLimitsAreEightMembersAndThreeCompanies() {
        val backend = AscentBackend(MemoryFiles())
        driver(backend, email = "lead@meru.app")
        val code = backend.createConvoy("Night Watch").code
        repeat(7) { index ->
            backend.register("mate$index@meru.app", "ascent1", "Mate$index")
            backend.joinConvoy(code)
        }
        backend.register("ninth@meru.app", "ascent1", "Ninth")
        assertFails { backend.joinConvoy(code) }
    }

    @Test
    fun catalogAndBoardsMatchTheDriveWorld() {
        val backend = AscentBackend(MemoryFiles())
        driver(backend, referral = "")
        assertEquals(16, backend.roads(50).items.size)
        assertEquals(6, backend.rituals().size)
        assertEquals(8, backend.contracts().items.size)
        assertTrue(backend.board("city").any { it.you })
        assertTrue(backend.board("city").none { it.name == "Omar Sheikh" })
        assertTrue(backend.board("province").any { it.name == "Hira Khan" })
        assertTrue(backend.board("country").any { it.name == "Omar Sheikh" })
        assertTrue(backend.board("world").any { it.name == "Jonas Berg" })
        assertFalse(backend.board("country").any { it.name == "Jonas Berg" })
    }

    private fun driver(backend: AscentBackend, email: String = "driver@meru.app", referral: String = "MERU") =
        backend.createDriver("Musharraf", email, "ascent1", "Lahore", "Punjab", "Pakistan", "Grey", "Honda", "City", 2019, "LEA1", referral)

    private fun trace(points: List<Pair<Double, Double>>): SavedDrive {
        val samples = points.mapIndexed { index, (lat, lon) ->
            RideSample(1_700_000_000_000 + index * 1000L, lat, lon, 210.0 + index, 36.0)
        }
        return SavedDrive(
            id = "trace-${samples.first().t}",
            startedAt = samples.first().t,
            endedAt = samples.last().t + 1000,
            points = samples,
        )
    }
}
