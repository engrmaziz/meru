package com.jetbrains.kmpapp.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.round
import kotlin.math.sin
import kotlin.math.sqrt
/**
 * In-process Meru backend. Sessions are looked up. Named roads, relics,
 * expeditions, rituals, contracts, convoys, and the car chronicle are awarded
 * only from a trace, not from a client-sent score.
 */
class AscentBackend(private val files: MeruFiles) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private var snapshot = MeruSnapshot()
    private var restoring = false
    private val users = mutableMapOf<String, UserRecord>()
    private val sessions = mutableMapOf<String, String>()
    private val worlds = mutableMapOf<String, World>()
    private val convoys = mutableMapOf<String, Convoy>()
    private val codes = mutableMapOf<String, String>()
    private val keepers = mutableMapOf<String, Keeper>()
    var activeUserId: String? = null
        private set

    init {
        restore()
    }

    fun openLocalDriver(name: String = "Driver"): AscentHome {
        val existing = activeUserId?.let { users[it] }
        if (existing != null) return home()
        val created = register("driver@meru.app", "ascent1", name)
        return created
    }

    fun register(email: String, password: String, displayName: String): AscentHome {
        val key = email.trim().lowercase()
        if (users.values.any { it.email == key }) error("Email already registered")
        if (!key.contains('@') || password.length < 6) error("Use a real email and 6+ character password")
        val id = rid()
        val salt = rid()
        users[id] = UserRecord(id, key, displayName.trim().ifBlank { "Driver" }, "$salt:${mix(password, salt)}")
        worlds[id] = World()
        signIn(id)
        return home()
    }

    fun login(email: String, password: String): AscentHome {
        val user = users.values.find { it.email == email.trim().lowercase() } ?: error("Invalid credentials")
        val salt = user.passwordHash.substringBefore(':')
        val digest = user.passwordHash.substringAfter(':')
        if (mix(password, salt) != digest) error("Invalid credentials")
        signIn(user.id)
        if (snapshot.profile?.id == user.id) {
            snapshot = snapshot.copy(signedIn = true)
            persist()
        }
        return home()
    }

    fun home(): AscentHome {
        val user = me()
        val world = world()
        val window = windowNow()
        val bond = bondFor(world)
        val next = expeditions().minByOrNull { it.closedChapters }
        val missing = RELICS.firstOrNull { it.id !in world.relics }
        val open = contracts().items.firstOrNull { !it.completed }
        return AscentHome(
            greeting = "${user.displayName}, the car is ready for a real drive.",
            pitch = "Meru turns the roads you already own into a private atlas: named ascents, relics you stand beside, expeditions with an ending, and a chronicle that belongs to the car.",
            window = window,
            bond = bond,
            verifiedKm = round1(world.verifiedKm),
            roadsClaimed = world.claims.size,
            roadsTotal = ROADS.size,
            relicsFound = world.relics.size,
            relicsTotal = RELICS.size,
            nextExpedition = next?.let { ExpeditionChip(it.id, it.name, it.pitch, it.closedChapters, it.chapters.size) },
            nearestRelic = missing?.let { RelicChip(it.id, it.name, it.line, roadName(it.roadId)) },
            openContract = open,
            convoys = world.convoyIds.size,
            latestStory = world.stories.firstOrNull(),
        )
    }

    fun roads(limit: Int = 20, cursor: String? = null): AtlasPage {
        val size = limit.coerceIn(1, 50)
        val start = if (cursor == null) 0 else (ROADS.indexOfFirst { it.id == cursor } + 1).coerceAtLeast(0)
        val slice = ROADS.drop(start).take(size)
        val next = if (start + size < ROADS.size) slice.lastOrNull()?.id else null
        return AtlasPage(slice.map { it.card() }, next)
    }

    fun road(id: String): RoadDetail {
        val road = ROADS.find { it.id == id } ?: error("Road not found")
        val claim = world().claims[id]
        val keeper = keepers[id]
        val uid = requireUser()
        return RoadDetail(
            card = road.card(),
            claimed = claim != null,
            times = claim?.times ?: 0,
            yourQuality = claim?.quality,
            keeper = keeper?.let { KeeperView(it.displayName, it.quality, it.userId == uid) },
            relics = RELICS.filter { it.roadId == id }.map {
                RelicOnRoad(it.id, it.name, it.line, world().relics.containsKey(it.id))
            },
        )
    }

    fun expeditions(): List<ExpeditionView> = EXPEDITIONS.map { view(it) }

    fun expedition(id: String): ExpeditionView =
        EXPEDITIONS.find { it.id == id }?.let { view(it) } ?: error("Expedition not found")

    fun embark(id: String): ExpeditionView {
        if (EXPEDITIONS.none { it.id == id }) error("Expedition not found")
        val world = world()
        if (id !in world.embarked) world.embarked[id] = now()
        remember()
        return expedition(id)
    }

    fun relics(): RelicAlbum {
        val world = world()
        return RelicAlbum(
            found = world.relics.size,
            total = RELICS.size,
            items = RELICS.map {
                RelicItem(it.id, it.name, it.line, roadName(it.roadId), world.relics.containsKey(it.id))
            },
        )
    }

    fun rituals(): List<RitualView> {
        val world = world()
        val today = dayKey(now())
        return RITUALS.map { ritual ->
            val row = world.rituals[ritual.id]
            RitualView(ritual.id, ritual.name, ritual.vow, row?.pledged == true, row?.streak ?: 0, row?.lastDay == today)
        }
    }

    fun pledge(id: String): RitualView {
        val ritual = RITUALS.find { it.id == id } ?: error("Ritual not found")
        val row = world().rituals.getOrPut(id) { RitualState() }
        row.pledged = true
        if (!restoring && ritual.id !in snapshot.pledgedRituals) {
            snapshot = snapshot.copy(pledgedRituals = snapshot.pledgedRituals + ritual.id)
            persist()
        }
        return RitualView(ritual.id, ritual.name, ritual.vow, true, row.streak, row.lastDay == dayKey(now()))
    }

    fun contracts(): ContractBoard {
        val week = weekKey(now())
        val world = world()
        return ContractBoard(
            week = week,
            items = CONTRACTS.map { contract ->
                val row = world.contracts[contract.id]
                val same = row?.week == week
                ContractView(contract.id, contract.name, contract.demand, if (same) round1(row.value) else 0.0, contract.target, same && row.done)
            },
        )
    }

    fun chronicle(): Chronicle {
        val world = world()
        val bond = bondFor(world)
        return Chronicle(
            verifiedKm = round1(world.verifiedKm),
            roadsClaimed = world.claims.size,
            relicsFound = world.relics.size,
            careMarks = world.care.toList(),
            titles = titles(world, bond.level),
            bond = bond,
            latestStory = world.stories.firstOrNull(),
        )
    }

    fun story(): Story = world().stories.firstOrNull()
        ?: Story("none", "No ascent yet", "The car is waiting. The first drive writes the first page.")

    fun myConvoys(): List<ConvoyView> =
        world().convoyIds.mapNotNull { convoys[it] }.map { it.view(requireUser()) }

    fun createConvoy(name: String, oath: String = "We drive to arrive as ourselves."): ConvoyView {
        val clean = name.trim()
        if (clean.length !in 2..40) error("Convoy name must be 2–40 characters")
        val world = world()
        if (world.convoyIds.size >= 3) error("Three convoys is the company you can actually keep")
        val user = me()
        val convoy = Convoy(rid(), convoyCode(), clean, oath.trim().take(140), user.id, mutableListOf(Member(user.id, user.displayName)), 0.0)
        convoys[convoy.id] = convoy
        codes[convoy.code] = convoy.id
        world.convoyIds += convoy.id
        remember()
        return convoy.view(user.id)
    }

    fun joinConvoy(code: String): ConvoyView {
        val id = codes[code.trim().uppercase()] ?: error("Convoy code not found")
        val convoy = convoys.getValue(id)
        val user = me()
        val world = world()
        val already = convoy.members.any { it.userId == user.id }
        if (!already && world.convoyIds.size >= 3) error("Three convoys is the company you can actually keep")
        if (!already && convoy.members.size >= 8) error("This convoy is full")
        if (!already) convoy.members += Member(user.id, user.displayName)
        if (convoy.id !in world.convoyIds) world.convoyIds += convoy.id
        remember()
        return convoy.view(user.id)
    }

    fun noteCare(workshop: String) {
        world().care += CareMark(now(), workshop)
        remember()
    }

    fun hasDriver(): Boolean = snapshot.profile != null && activeUserId != null

    fun signOut() {
        activeUserId = null
        snapshot = snapshot.copy(signedIn = false)
        persist()
    }

    fun profile(): DriverProfile? = snapshot.profile

    fun points(): Int = snapshot.points

    fun drives(): List<SavedDrive> = snapshot.drives

    fun drive(id: String): SavedDrive? = snapshot.drives.find { it.id == id }

    fun openCheckpoint(): SavedDrive? = snapshot.checkpoint

    fun createDriver(
        name: String,
        email: String,
        pin: String,
        city: String,
        province: String,
        country: String,
        carName: String,
        make: String,
        model: String,
        year: Int,
        plate: String,
        referral: String,
    ): DriverProfile {
        if (snapshot.profile != null) error("Free Meru keeps one car")
        if (carName.isBlank() || make.isBlank()) error("The car needs a name and a make")
        register(email, pin, name)
        val user = me()
        val code = referral.trim().uppercase()
        val bonus = if (code == "MERU") 80 else 0
        val profile = DriverProfile(
            id = user.id,
            name = user.displayName,
            email = user.email,
            city = city.trim().ifBlank { "Lahore" },
            province = province.trim().ifBlank { "Punjab" },
            country = country.trim().ifBlank { "Pakistan" },
            carName = carName.trim(),
            make = make.trim(),
            model = model.trim(),
            year = year,
            plate = plate.trim().uppercase(),
            referralCode = referralCode(user.displayName),
            referredBy = code.ifBlank { null },
        )
        snapshot = MeruSnapshot(profile = profile, pinHash = user.passwordHash, points = 100 + bonus)
        persist()
        return profile
    }

    fun checkpoint(drive: SavedDrive) {
        if (restoring) return
        snapshot = snapshot.copy(checkpoint = drive)
        persist()
    }

    fun clearCheckpoint() {
        snapshot = snapshot.copy(checkpoint = null)
        persist()
    }

    fun acceptDrive(drive: SavedDrive): SavedDrive {
        val official = drive.points.size >= 2
        val receipt = if (official) {
            onTrip(
                drive.points.map { TracePoint(it.lat, it.lon, it.alt) },
                drive.brakeCount,
                drive.startedAt,
            )
        } else {
            null
        }
        val score = if (official) scoreOf(drive) else 0
        val saved = drive.copy(
            id = if (drive.id == "open" || drive.id.isBlank()) "drv-${drive.startedAt}" else drive.id,
            endedAt = if (drive.endedAt == 0L) now() else drive.endedAt,
            score = score,
            storyTitle = receipt?.story?.title ?: "Too short to count",
            storyBody = receipt?.story?.body ?: "A drive counts when the trace has at least two points. One point cannot claim a road or a relic.",
        )
        var profile = snapshot.profile
        if (profile != null && drive.city.isNotBlank()) {
            profile = profile.copy(
                city = drive.city,
                province = drive.province.ifBlank { profile.province },
                country = drive.country.ifBlank { profile.country },
            )
        }
        snapshot = snapshot.copy(
            profile = profile,
            drives = listOf(saved) + snapshot.drives,
            points = snapshot.points + score,
            checkpoint = null,
        )
        persist()
        return saved
    }

    fun board(tier: String): List<BoardRow> {
        val me = snapshot.profile ?: return emptyList()
        val you = BoardRow(0, me.name, me.carName, snapshot.points, me.city, true)
        val pool = RIVALS.map {
            BoardRow(0, it.name, it.car, it.points, it.city, false) to it
        }
        val rows = pool.filter { (_, rival) ->
            when (tier) {
                "city" -> rival.city.equals(me.city, true)
                "province" -> rival.province.equals(me.province, true)
                "country" -> rival.country.equals(me.country, true)
                else -> true
            }
        }.map { it.first } + you
        return rows.sortedByDescending { it.points }.mapIndexed { index, row -> row.copy(rank = index + 1) }
    }

    private fun scoreOf(drive: SavedDrive): Int {
        val km = drive.distanceM / 1000.0
        val composure = (8 - drive.brakeCount).coerceAtLeast(0)
        val climb = (drive.elevationGainM / 20.0).toInt()
        val noted = drive.laneChanges.coerceAtMost(6)
        val rush = if (drive.maxSpeedKmh > 130) 20 else 0
        return ((km * 14) + composure * 4 + climb + noted * 2 - rush).toInt().coerceAtLeast(8)
    }

    private fun referralCode(name: String): String {
        val letters = name.filter { it.isLetter() }.uppercase().take(4).padEnd(4, 'X')
        val tail = (1000 + (name.hashCode() and 0xFFF) % 9000).toString()
        return letters + tail
    }

    private fun restore() {
        val text = files.read() ?: return
        val loaded = runCatching { json.decodeFromString(MeruSnapshot.serializer(), text) }.getOrNull() ?: return
        val profile = loaded.profile ?: return
        if (loaded.pinHash.isBlank()) return
        restoring = true
        snapshot = loaded
        users[profile.id] = UserRecord(profile.id, profile.email, profile.name, loaded.pinHash)
        worlds[profile.id] = World()
        signIn(profile.id)
        val world = world()
        world.care += loaded.care
        loaded.embarked.forEach { id -> world.embarked[id] = now() }
        loaded.convoys.forEach { saved ->
            convoys[saved.id] = Convoy(
                saved.id,
                saved.code,
                saved.name,
                saved.oath,
                saved.leaderId,
                saved.members.map { Member(it.userId, it.name) }.toMutableList(),
                0.0,
            )
            codes[saved.code] = saved.id
        }
        world.convoyIds += loaded.memberships.filter { it in convoys }
        loaded.pledgedRituals.forEach { id -> runCatching { pledge(id) } }
        loaded.drives.asReversed().forEach { drive ->
            if (drive.points.size >= 2) {
                runCatching {
                    onTrip(drive.points.map { TracePoint(it.lat, it.lon, it.alt) }, drive.brakeCount, drive.startedAt)
                }
            }
        }
        if (!loaded.signedIn) activeUserId = null
        restoring = false
    }

    private fun remember() {
        if (restoring) return
        val world = runCatching { world() }.getOrNull() ?: return
        snapshot = snapshot.copy(
            care = world.care.toList(),
            embarked = world.embarked.keys.toList(),
            memberships = world.convoyIds.toList(),
            convoys = convoys.values.map { convoy ->
                SavedConvoy(
                    convoy.id,
                    convoy.code,
                    convoy.name,
                    convoy.oath,
                    convoy.leaderId,
                    convoy.members.map { SavedMember(it.userId, it.displayName) },
                )
            },
        )
        persist()
    }

    private fun persist() {
        if (restoring) return
        runCatching { files.write(json.encodeToString(MeruSnapshot.serializer(), snapshot)) }
    }

    /** A real ascent: points along the named road, then the same rules as a synced trip. */
    fun driveRoad(roadId: String): DriveReceipt {
        val road = ROADS.find { it.id == roadId } ?: error("Road not found")
        val relic = RELICS.firstOrNull { it.roadId == roadId }
        val points = mutableListOf(
            TracePoint(road.lat - 0.01, road.lon - 0.01, 400.0),
            TracePoint(road.lat, road.lon, 520.0),
            TracePoint(road.lat + 0.012, road.lon + 0.008, 610.0),
        )
        if (relic != null) points += TracePoint(relic.lat, relic.lon, 640.0)
        return onTrip(points, brakes = 0, startedAt = now())
    }

    fun onTrip(points: List<TracePoint>, brakes: Int, startedAt: Long): DriveReceipt {
        val user = me()
        val world = world()
        val official = points.size >= 2
        val km = if (official) traceKm(points) else 0.0
        val climb = climbMetres(points)
        if (official) world.verifiedKm += km
        val quality = (90 - min(28, brakes * 4) - if (km < 1) 6 else 0).coerceIn(40, 99)
        val claimed = mutableListOf<RoadHit>()
        val found = mutableListOf<RelicHit>()
        if (official) {
            for (road in ROADS) {
                if (points.none { kmBetween(it.lat, it.lon, road.lat, road.lon) <= road.radiusKm }) continue
                val prior = world.claims[road.id]
                val first = prior == null
                world.claims[road.id] = RoadClaim(prior?.at ?: now(), maxOf(prior?.quality ?: 0, quality), (prior?.times ?: 0) + 1)
                val keeper = keepers[road.id]
                val takes = keeper == null || quality > keeper.quality
                if (takes) keepers[road.id] = Keeper(user.id, user.displayName, quality)
                claimed += RoadHit(road.id, road.name, first, takes)
            }
            for (relic in RELICS) {
                if (points.none { kmBetween(it.lat, it.lon, relic.lat, relic.lon) <= relic.radiusKm }) continue
                val first = relic.id !in world.relics
                if (first) world.relics[relic.id] = now()
                found += RelicHit(relic.id, relic.name, first, relic.line)
            }
        }
        val chapters = mutableListOf<String>()
        for (exp in EXPEDITIONS) {
            for (chapter in exp.chapters) {
                val just = (chapter.roadId != null && claimed.any { it.id == chapter.roadId && it.first }) ||
                    (chapter.relicId != null && found.any { it.id == chapter.relicId && it.first })
                if (just) chapters += chapter.title
            }
        }
        val hour = pktHour(startedAt)
        val today = dayKey(startedAt)
        val dow = pktDow(startedAt)
        val ritualsKept = mutableListOf<String>()
        for (ritual in RITUALS) {
            val row = world.rituals[ritual.id] ?: continue
            if (!row.pledged || !official) continue
            if (ritual.id == "friday-release" && dow != 5) continue
            if (ritual.id == "sunday-long" && dow != 0) continue
            if (ritual.id == "midweek-ridge" && dow != 2 && dow != 3) continue
            if (hour < ritual.fromHour || hour >= ritual.toHour) continue
            if (row.lastDay == today) continue
            row.streak = if (row.lastDay == dayKey(startedAt - 86_400_000)) row.streak + 1 else 1
            row.lastDay = today
            ritualsKept += ritual.name
        }
        val week = weekKey(startedAt)
        val inConvoy = world.convoyIds.isNotEmpty()
        if (official && inConvoy && km > 0) {
            world.convoyIds.mapNotNull { convoys[it] }.forEach { it.sharedKm = round1(it.sharedKm + km) }
        }
        val advanced = mutableListOf<String>()
        for (contract in CONTRACTS) {
            val delta = when (contract.kind) {
                "smooth_km" -> if (official && brakes == 0) km else 0.0
                "relic" -> if (official) found.count { it.first }.toDouble() else 0.0
                "road" -> if (!official) 0.0 else if (contract.id == "second-road") {
                    if (claimed.any { it.first } && world.claims.size >= 2) 1.0 else 0.0
                } else {
                    claimed.count { it.first }.toDouble()
                }
                "dawn" -> if (official && hour in 5..7) 1.0 else 0.0
                "convoy_km" -> if (official && inConvoy) km else 0.0
                "elevation" -> if (official) climb else 0.0
                else -> 0.0
            }
            if (delta <= 0) continue
            val row = world.contracts[contract.id]
            val current = if (row == null || row.week != week) ContractState(week, 0.0, false).also { world.contracts[contract.id] = it } else row
            if (current.done) continue
            current.value += delta
            if (current.value >= contract.target) current.done = true
            advanced += contract.name
        }
        val story = writeStory(user.displayName, official, km, claimed, found, chapters, windowNow(startedAt).name)
        world.stories.add(0, story)
        if (world.stories.size > 30) world.stories.removeAt(world.stories.lastIndex)
        return DriveReceipt(round1(km), climb.toInt(), official, claimed, found, chapters, ritualsKept, advanced, story, bondFor(world))
    }

    private fun signIn(id: String) {
        val token = rid()
        sessions[token] = id
        activeUserId = id
    }

    private fun requireUser(): String = activeUserId ?: error("Sign in first")
    private fun me(): UserRecord = users[requireUser()] ?: error("Sign in first")
    private fun world(): World = worlds.getOrPut(requireUser()) { World() }

    private fun view(exp: ExpeditionDef): ExpeditionView {
        val world = world()
        val chapters = exp.chapters.map { chapter ->
            val closed = (chapter.roadId != null && world.claims.containsKey(chapter.roadId)) ||
                (chapter.relicId != null && world.relics.containsKey(chapter.relicId))
            ChapterView(chapter.id, chapter.title, chapter.detail, closed)
        }
        return ExpeditionView(
            exp.id, exp.name, exp.pitch, exp.titleReward,
            world.embarked.containsKey(exp.id),
            chapters.count { it.closed },
            chapters.all { it.closed },
            chapters,
        )
    }
}

data class AscentHome(
    val greeting: String,
    val pitch: String,
    val window: DriveWindow,
    val bond: Bond,
    val verifiedKm: Double,
    val roadsClaimed: Int,
    val roadsTotal: Int,
    val relicsFound: Int,
    val relicsTotal: Int,
    val nextExpedition: ExpeditionChip?,
    val nearestRelic: RelicChip?,
    val openContract: ContractView?,
    val convoys: Int,
    val latestStory: Story?,
)

data class DriveWindow(val name: String, val line: String, val active: Boolean)
data class Bond(val level: Int, val title: String)
data class ExpeditionChip(val id: String, val name: String, val pitch: String, val closedChapters: Int, val chapters: Int)
data class RelicChip(val id: String, val name: String, val line: String, val roadName: String)
data class RoadCard(val id: String, val name: String, val region: String, val legend: String, val difficulty: String, val distanceKm: Int, val hook: String)
data class AtlasPage(val items: List<RoadCard>, val nextCursor: String?)
data class KeeperView(val displayName: String, val quality: Int, val yours: Boolean)
data class RelicOnRoad(val id: String, val name: String, val line: String, val found: Boolean)
data class RoadDetail(val card: RoadCard, val claimed: Boolean, val times: Int, val yourQuality: Int?, val keeper: KeeperView?, val relics: List<RelicOnRoad>)
data class ChapterView(val id: String, val title: String, val detail: String, val closed: Boolean)
data class ExpeditionView(val id: String, val name: String, val pitch: String, val titleReward: String, val embarked: Boolean, val closedChapters: Int, val complete: Boolean, val chapters: List<ChapterView>)
data class RelicItem(val id: String, val name: String, val line: String, val roadName: String, val found: Boolean)
data class RelicAlbum(val found: Int, val total: Int, val items: List<RelicItem>)
data class RitualView(val id: String, val name: String, val vow: String, val pledged: Boolean, val streak: Int, val keptToday: Boolean)
data class ContractView(val id: String, val name: String, val demand: String, val progress: Double, val target: Double, val completed: Boolean)
data class ContractBoard(val week: String, val items: List<ContractView>)
@Serializable
data class CareMark(val at: Long, val workshop: String)
data class Chronicle(val verifiedKm: Double, val roadsClaimed: Int, val relicsFound: Int, val careMarks: List<CareMark>, val titles: List<String>, val bond: Bond, val latestStory: Story?)
data class Story(val id: String, val title: String, val body: String)
data class ConvoyView(val id: String, val code: String, val name: String, val oath: String, val leader: Boolean, val members: Int, val roster: List<String>, val sharedKm: Double)
data class RoadHit(val id: String, val name: String, val first: Boolean, val keeper: Boolean)
data class RelicHit(val id: String, val name: String, val first: Boolean, val line: String)
data class DriveReceipt(val verifiedKm: Double, val elevationGainM: Int, val official: Boolean, val roads: List<RoadHit>, val relics: List<RelicHit>, val chapters: List<String>, val rituals: List<String>, val contracts: List<String>, val story: Story, val bond: Bond)
data class TracePoint(val lat: Double, val lon: Double, val alt: Double? = null)

private data class Rival(val name: String, val car: String, val points: Int, val city: String, val province: String, val country: String)

private val RIVALS = listOf(
    Rival("Sana Qureshi", "Canal Grey", 640, "Lahore", "Punjab", "Pakistan"),
    Rival("Bilal Dar", "Ridge Two", 910, "Lahore", "Punjab", "Pakistan"),
    Rival("Hira Khan", "Salt Line", 420, "Murree", "Punjab", "Pakistan"),
    Rival("Omar Sheikh", "Harbour", 780, "Karachi", "Sindh", "Pakistan"),
    Rival("Ayesha Noor", "Margalla", 510, "Islamabad", "Islamabad", "Pakistan"),
    Rival("Leila Haddad", "Blue Hour", 860, "Dubai", "Dubai", "United Arab Emirates"),
    Rival("Jonas Berg", "First Light", 990, "London", "England", "United Kingdom"),
    Rival("Mina Cho", "Night Watch", 730, "Seoul", "Seoul", "South Korea"),
)

private data class UserRecord(val id: String, val email: String, val displayName: String, val passwordHash: String)
private data class RoadClaim(val at: Long, var quality: Int, var times: Int)
private data class RitualState(var pledged: Boolean = false, var lastDay: String? = null, var streak: Int = 0)
private data class ContractState(val week: String, var value: Double, var done: Boolean)
private data class Member(val userId: String, val displayName: String)
private data class Keeper(val userId: String, val displayName: String, val quality: Int)
private class World {
    val claims = mutableMapOf<String, RoadClaim>()
    val relics = mutableMapOf<String, Long>()
    val embarked = mutableMapOf<String, Long>()
    val rituals = mutableMapOf<String, RitualState>()
    val contracts = mutableMapOf<String, ContractState>()
    val convoyIds = mutableListOf<String>()
    val stories = mutableListOf<Story>()
    val care = mutableListOf<CareMark>()
    var verifiedKm = 0.0
}
private class Convoy(val id: String, val code: String, val name: String, val oath: String, val leaderId: String, val members: MutableList<Member>, var sharedKm: Double) {
    fun view(userId: String) = ConvoyView(id, code, name, oath, leaderId == userId, members.size, members.map { it.displayName }, round1(sharedKm))
}

private data class RoadDef(val id: String, val name: String, val region: String, val legend: String, val difficulty: String, val distanceKm: Int, val lat: Double, val lon: Double, val radiusKm: Double, val hook: String) {
    fun card() = RoadCard(id, name, region, legend, difficulty, distanceKm, hook)
}
private data class RelicDef(val id: String, val name: String, val roadId: String, val lat: Double, val lon: Double, val radiusKm: Double, val line: String)
private data class ChapterDef(val id: String, val title: String, val detail: String, val roadId: String? = null, val relicId: String? = null)
private data class ExpeditionDef(val id: String, val name: String, val pitch: String, val titleReward: String, val chapters: List<ChapterDef>)
private data class RitualDef(val id: String, val name: String, val vow: String, val fromHour: Int, val toHour: Int)
private data class ContractDef(val id: String, val name: String, val demand: String, val kind: String, val target: Double)

private val ROADS = listOf(
    RoadDef("lahore-canal", "Canal Loop", "Lahore", "The city breathes along the water.", "gentle", 18, 31.5204, 74.3587, 8.0, "Your own city, driven like it is new."),
    RoadDef("ravi-dawn", "Ravi Dawn Line", "Lahore", "Mist on the river before the trucks wake.", "gentle", 22, 31.61, 74.29, 10.0, "Leave before the city does."),
    RoadDef("murree-ascent", "Murree Ascent", "Punjab hills", "Pine, switchback, and the first cold air.", "committed", 58, 33.907, 73.394, 14.0, "The drive that makes a car feel chosen."),
    RoadDef("nathia", "Nathia Ridge", "Galiyat", "A narrower promise above Murree.", "committed", 34, 34.068, 73.391, 8.0, "Earn the view by staying patient in the cloud."),
    RoadDef("margalla", "Margalla Crest", "Islamabad", "The capital seen from the ridge, then left behind.", "gentle", 16, 33.748, 73.055, 8.0, "A weeknight ascent that still counts."),
    RoadDef("swat-climb", "Swat Climb", "Swat", "River on the left, orchards, a road that keeps rising.", "committed", 90, 35.222, 72.425, 16.0, "The valley drive people describe with their hands."),
    RoadDef("kkh-whisper", "KKH Whisper", "Hunza", "The mountain does not care about your schedule.", "serious", 120, 36.316, 74.65, 28.0, "Not a commute. A crossing."),
    RoadDef("fairy-approach", "Fairy Meadows Approach", "Raikot", "The last paved courage before the jeep track.", "serious", 40, 35.386, 74.578, 12.0, "You will tell this one for years."),
    RoadDef("skardu-gate", "Skardu Gate", "Baltistan", "High desert and a sky that feels closer.", "serious", 150, 35.297, 75.633, 22.0, "The car earns a new name up here."),
    RoadDef("fort-munro", "Fort Munro Switchbacks", "Dera Ghazi Khan", "Heat below, pines above.", "committed", 70, 29.929, 69.981, 14.0, "South Punjab’s private ascent."),
    RoadDef("bolan", "Bolan Gate", "Balochistan", "The pass that has carried armies and now carries you.", "serious", 95, 29.45, 67.58, 16.0, "History under the tyres."),
    RoadDef("makran", "Makran Coastal Run", "Balochistan coast", "Sea on one side, empty on the other.", "serious", 180, 25.21, 64.64, 30.0, "Ownership that feels like exploration."),
    RoadDef("seaview-night", "Seaview Night", "Karachi", "Salt air and a city that refuses to sleep.", "gentle", 14, 24.813, 67.03, 8.0, "A night loop with the windows cracked."),
    RoadDef("gt-peshawar", "Grand Trunk Dusk", "Peshawar", "The old road, taken slowly.", "gentle", 24, 34.015, 71.58, 10.0, "An ordinary road, driven with intention."),
    RoadDef("multan-saints", "City of Saints Loop", "Multan", "Domes and dust-gold light.", "gentle", 12, 30.198, 71.468, 7.0, "Short, if you actually look."),
    RoadDef("cholistan-edge", "Cholistan Edge", "Bahawalpur", "The desert starts as a change in the light.", "committed", 80, 29.0, 72.35, 22.0, "Bring water. Leave the playlist off."),
)

private val RELICS = listOf(
    RelicDef("pine-gate", "Pine Gate", "murree-ascent", 33.91, 73.39, 4.0, "The first pines that smell like a different country."),
    RelicDef("mall-overlook", "Mall Overlook", "murree-ascent", 33.907, 73.394, 2.0, "The ridge where the plains become a map."),
    RelicDef("canal-mile-7", "Seventh Mile Lamp", "lahore-canal", 31.52, 74.36, 3.0, "A lamp that has watched a thousand quiet nights."),
    RelicDef("ravi-mist", "Mist Bank", "ravi-dawn", 31.612, 74.292, 4.0, "Where the river hides the far bank."),
    RelicDef("cloud-seat", "Cloud Seat", "nathia", 34.07, 73.39, 3.0, "A bend that disappears into weather."),
    RelicDef("margalla-stone", "Ridge Stone", "margalla", 33.75, 73.06, 3.0, "A lookout the city cannot follow you into."),
    RelicDef("swat-orchard", "Orchard Turn", "swat-climb", 35.22, 72.43, 5.0, "The river still argues."),
    RelicDef("hunza-shelf", "Hunza Shelf", "kkh-whisper", 36.32, 74.65, 8.0, "A shelf of road with the valley straight down."),
    RelicDef("rakaposhi-glance", "Rakaposhi Glance", "kkh-whisper", 36.25, 74.6, 10.0, "The mountain arrives before you are ready."),
    RelicDef("raikot-last-pave", "Last Pavement", "fairy-approach", 35.39, 74.58, 5.0, "Where the tarmac gives up with dignity."),
    RelicDef("skardu-braid", "River Braid", "skardu-gate", 35.3, 75.63, 8.0, "The Indus splitting into silver threads."),
    RelicDef("munro-pine", "Upper Pine", "fort-munro", 29.93, 69.98, 4.0, "Cool shade after the plains."),
    RelicDef("bolan-cut", "The Cut", "bolan", 29.45, 67.58, 6.0, "Rock walls close, then open."),
    RelicDef("ormara-cliff", "Ormara Cliff", "makran", 25.21, 64.64, 8.0, "The Arabian Sea, sudden and enormous."),
    RelicDef("seaview-salt", "Salt Mile", "seaview-night", 24.81, 67.03, 3.0, "Night wind that tastes like metal and tide."),
    RelicDef("gt-gate", "Old Gate Hour", "gt-peshawar", 34.015, 71.58, 4.0, "Dusk on a road older than the idea of the car."),
    RelicDef("multan-gold", "Gold Dome Light", "multan-saints", 30.2, 71.47, 3.0, "Late light on brick and blue tile."),
    RelicDef("cholistan-silence", "First Silence", "cholistan-edge", 29.0, 72.35, 8.0, "The moment the radio feels rude."),
)

private val EXPEDITIONS = listOf(
    ExpeditionDef("monsoon-covenant", "Monsoon Covenant", "One city, then the hills, then the cloud.", "Covenant Driver", listOf(
        ChapterDef("mc-1", "The Canal at Dusk", "Take the Canal Loop as if you have never lived here.", roadId = "lahore-canal"),
        ChapterDef("mc-2", "Leave the Plain", "Climb the Murree Ascent.", roadId = "murree-ascent"),
        ChapterDef("mc-3", "Sit in the Cloud", "Find the Cloud Seat.", relicId = "cloud-seat"),
    )),
    ExpeditionDef("northern-silence", "Northern Silence", "The trip people mean when they say they need to get out.", "Silence Keeper", listOf(
        ChapterDef("ns-1", "The Whisper", "Put a wheel on the KKH.", roadId = "kkh-whisper"),
        ChapterDef("ns-2", "The Glance", "Be present for Rakaposhi.", relicId = "rakaposhi-glance"),
        ChapterDef("ns-3", "Last Pavement", "Reach the end of the paved approach.", relicId = "raikot-last-pave"),
    )),
    ExpeditionDef("salt-and-sea", "Salt and Sea", "A coastal oath. Fuel planned like a ritual.", "Coast Runner", listOf(
        ChapterDef("ss-1", "Night Salt", "Run Seaview after dark.", roadId = "seaview-night"),
        ChapterDef("ss-2", "The Cliff", "Stand the car at Ormara.", relicId = "ormara-cliff"),
        ChapterDef("ss-3", "The Long Water", "Claim the Makran run.", roadId = "makran"),
    )),
    ExpeditionDef("city-ghost", "City Ghost", "An early alarm and an empty road.", "City Ghost", listOf(
        ChapterDef("cg-1", "Before the Trucks", "Drive the Ravi at dawn.", roadId = "ravi-dawn"),
        ChapterDef("cg-2", "Seventh Lamp", "Find the canal lamp.", relicId = "canal-mile-7"),
        ChapterDef("cg-3", "Ridge After Work", "Take the Margalla Crest.", roadId = "margalla"),
    )),
    ExpeditionDef("keepers-errand", "Keeper's Errand", "A pass, a hill station, a desert edge.", "Errand Keeper", listOf(
        ChapterDef("ke-1", "The Cut", "Enter the Bolan.", relicId = "bolan-cut"),
        ChapterDef("ke-2", "Upper Pine", "Reach Fort Munro’s shade.", relicId = "munro-pine"),
        ChapterDef("ke-3", "First Silence", "Find where Cholistan goes quiet.", relicId = "cholistan-silence"),
    )),
    ExpeditionDef("old-roads", "Old Roads", "Saints, gates, and a trunk road.", "Old Road Hand", listOf(
        ChapterDef("or-1", "Gold Light", "Loop Multan while the domes are lit.", relicId = "multan-gold"),
        ChapterDef("or-2", "Gate Hour", "Take the Grand Trunk at dusk.", roadId = "gt-peshawar"),
        ChapterDef("or-3", "Orchard Turn", "Climb far enough into Swat.", relicId = "swat-orchard"),
    )),
)

private val RITUALS = listOf(
    RitualDef("dawn-ascent", "Dawn Ascent", "Start while the city is still blue.", 5, 8),
    RitualDef("friday-release", "Friday Release", "Leave work in the car. Friday, after noon.", 13, 20),
    RitualDef("sunday-long", "Sunday Long", "One unhurried Sunday.", 7, 12),
    RitualDef("night-watch", "Night Watch", "A night loop with no errand hiding in it.", 21, 24),
    RitualDef("midweek-ridge", "Midweek Ridge", "A Tuesday or Wednesday taken seriously.", 16, 20),
    RitualDef("first-light-return", "Return by First Light", "Be finished before the day claims you.", 4, 6),
)

private val CONTRACTS = listOf(
    ContractDef("smooth-15", "Unbroken Fifteen", "Fifteen verified kilometres with no hard brake.", "smooth_km", 15.0),
    ContractDef("one-relic", "One New Relic", "Uncover a relic you have never stood beside.", "relic", 1.0),
    ContractDef("claim-road", "Put Your Name on a Road", "Claim a named road for the first time.", "road", 1.0),
    ContractDef("dawn-contract", "Blue Hour Contract", "Start a drive in the dawn window.", "dawn", 1.0),
    ContractDef("company-10", "Ten with Company", "Ten kilometres while you belong to a convoy.", "convoy_km", 10.0),
    ContractDef("climb-300", "Three Hundred Metres", "A drive that climbs at least 300 metres.", "elevation", 300.0),
    ContractDef("two-relics", "Pair of Relics", "Find two relics in the same week.", "relic", 2.0),
    ContractDef("second-road", "A Second Road", "Claim any named road this week.", "road", 1.0),
)

private fun roadName(id: String) = ROADS.find { it.id == id }?.name ?: id

private fun bondFor(world: World): Bond {
    val score = world.verifiedKm + world.relics.size * 8 + world.care.size * 12 + world.claims.size * 10
    val level = 1 + (score / 40).toInt()
    val title = when {
        level >= 12 -> "Legend in the logbook"
        level >= 7 -> "Road companion"
        level >= 3 -> "Known by the car"
        else -> "Just introduced"
    }
    return Bond(level, title)
}

private fun titles(world: World, level: Int): List<String> {
    val titles = mutableListOf<String>()
    if (world.relics.isNotEmpty()) titles += "Finder"
    if (world.relics.size >= 6) titles += "Cartographer"
    if (world.claims.isNotEmpty()) titles += "Roadclaimed"
    if (world.claims.size >= 4) titles += "Keeper of several roads"
    if (world.verifiedKm >= 100) titles += "Hundred verified"
    if (world.care.isNotEmpty()) titles += "Well kept"
    if (world.convoyIds.isNotEmpty()) titles += "Of a company"
    if (level >= 5) titles += "Bonded"
    for (exp in EXPEDITIONS) {
        val closed = exp.chapters.all { chapter ->
            (chapter.roadId != null && world.claims.containsKey(chapter.roadId)) ||
                (chapter.relicId != null && world.relics.containsKey(chapter.relicId))
        }
        if (closed && world.embarked.containsKey(exp.id)) titles += exp.titleReward
    }
    return titles
}

private fun writeStory(name: String, official: Boolean, km: Double, roads: List<RoadHit>, relics: List<RelicHit>, chapters: List<String>, window: String): Story {
    if (!official) return Story(rid(), "An unverified trace", "$name sent a drive Meru could not stand behind. Two real points are the price of a page.")
    val road = roads.firstOrNull()
    val relic = relics.firstOrNull { it.first } ?: relics.firstOrNull()
    val title = when {
        road?.keeper == true -> "${road.name} remembers you"
        road?.first == true -> "First time on ${road.name}"
        relic?.first == true -> relic.name
        else -> "$window drive"
    }
    val bits = mutableListOf("$name put ${round1(km)} verified kilometres into the log during ${window.lowercase()}.")
    if (road != null) bits += if (road.first) "${road.name} is now part of the car, not a rumour." else "The car returned to ${road.name}."
    if (road?.keeper == true) bits += "The road’s best careful drive is now yours."
    if (relic?.first == true) bits += "${relic.name}: ${relic.line}"
    if (chapters.isNotEmpty()) bits += "A chapter closed — ${chapters.first()}."
    if (road == null && relic == null) bits += "No named road this time. The kilometres still belong to the car."
    return Story(rid(), title, bits.joinToString(" "))
}

private fun windowNow(ms: Long = now()): DriveWindow {
    val hour = pktHour(ms)
    return when {
        hour in 5..7 -> DriveWindow("Blue hour", "The city has not started. A drive now is almost private.", true)
        hour in 16..17 -> DriveWindow("Golden descent", "The light is doing half the work. Point the car at a ridge.", true)
        hour >= 21 || hour < 4 -> DriveWindow("Night watch", "Night is a different map of the same streets.", true)
        else -> DriveWindow("Open road", "Any hour can be an ascent if the drive has a point.", false)
    }
}

private fun kmBetween(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val r = 6371.0
    val dLat = (lat2 - lat1) * PI / 180
    val dLon = (lon2 - lon1) * PI / 180
    val a = sin(dLat / 2) * sin(dLat / 2) + cos(lat1 * PI / 180) * cos(lat2 * PI / 180) * sin(dLon / 2) * sin(dLon / 2)
    return 2 * r * asin(min(1.0, sqrt(a)))
}

private fun traceKm(points: List<TracePoint>): Double {
    var km = 0.0
    for (i in 1 until points.size) {
        val step = kmBetween(points[i - 1].lat, points[i - 1].lon, points[i].lat, points[i].lon)
        if (step <= 2) km += step
    }
    return km
}

private fun climbMetres(points: List<TracePoint>): Double {
    var gain = 0.0
    for (i in 1 until points.size) {
        val prev = points[i - 1].alt ?: continue
        val next = points[i].alt ?: continue
        val delta = next - prev
        if (delta in 0.0..80.0) gain += delta
    }
    return gain
}

private fun pktHour(ms: Long) = (((ms + 5 * 3_600_000) / 3_600_000) % 24).toInt()
private fun dayKey(ms: Long): String {
    val days = (ms + 5 * 3_600_000) / 86_400_000
    return days.toString()
}
private fun weekKey(ms: Long) = ((ms + 5 * 3_600_000) / (7 * 86_400_000)).toString()
private fun pktDow(ms: Long): Int {
    val days = ((ms + 5 * 3_600_000) / 86_400_000).toInt()
    return ((days + 4) % 7 + 7) % 7
}
private fun now(): Long = kotlin.time.Clock.System.now().toEpochMilliseconds()
private fun round1(n: Double) = round(n * 10) / 10.0
private fun rid(): String = List(12) { "abcdefghjkmnpqrstuvwxyz23456789".random() }.joinToString("")
private fun convoyCode(): String = List(6) { "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".random() }.joinToString("")
private fun mix(password: String, salt: String): String {
    var h = 17
    (salt + ":" + password).forEach { h = h * 31 + it.code }
    return h.toString()
}
