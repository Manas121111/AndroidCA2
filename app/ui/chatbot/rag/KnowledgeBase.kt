package com.smarttour360.app.ui.chatbot.rag

import android.content.Context
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import com.smarttour360.app.data.TravelRepository
import com.smarttour360.app.data.local.SmartTourDatabase

@Entity(tableName = "chat_knowledge")
data class ChatKnowledgeEntity(
    @PrimaryKey val id: String,
    val title: String,
    val content: String,
    val tags: String,
    val source: String,
    val embeddingJson: String? = null,
    val updatedAtEpochMs: Long = System.currentTimeMillis()
)

@Dao
interface ChatKnowledgeDao {
    @Query("SELECT COUNT(*) FROM chat_knowledge")
    fun count(): Int

    @Query("SELECT * FROM chat_knowledge")
    fun getAll(): List<ChatKnowledgeEntity>

    @Query(
        """
        SELECT * FROM chat_knowledge
        WHERE lower(title) LIKE '%' || lower(:query) || '%'
           OR lower(content) LIKE '%' || lower(:query) || '%'
           OR lower(tags) LIKE '%' || lower(:query) || '%'
        LIMIT :limit
        """
    )
    fun searchByQuery(query: String, limit: Int): List<ChatKnowledgeEntity>

    @Query("SELECT * FROM chat_knowledge WHERE embeddingJson IS NULL LIMIT :limit")
    fun getMissingEmbeddings(limit: Int): List<ChatKnowledgeEntity>

    @Query("SELECT id FROM chat_knowledge")
    fun getAllIds(): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsertAll(items: List<ChatKnowledgeEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertIfMissing(items: List<ChatKnowledgeEntity>)

    @Query("UPDATE chat_knowledge SET embeddingJson = :embeddingJson, updatedAtEpochMs = :updatedAtEpochMs WHERE id = :id")
    fun updateEmbedding(id: String, embeddingJson: String, updatedAtEpochMs: Long)
}

class KnowledgeBase(private val context: Context) {
    private val dao: ChatKnowledgeDao
        get() = SmartTourDatabase.getInstance(context).chatKnowledgeDao()

    fun seedIfNeeded() {
        val repository = TravelRepository()
        val destinations = repository.getIndianDestinationCatalog().take(30)
        val now = System.currentTimeMillis()
        val destinationRows = destinations.mapIndexed { index, destination ->
            ChatKnowledgeEntity(
                id = "seed-destination-${index + 1}-${destination.id}",
                title = destination.name,
                content = buildString {
                    append("${destination.name} in ${destination.subtitle}. ")
                    append("Safety flag ${destination.flag}. ")
                    append("Eco score ${destination.ecoScore}. ")
                    append("Ethical score ${destination.ethicalScore}. ")
                    append("Carbon estimate ${destination.carbonKg}kg. ")
                    append("Current rating ${"%.1f".format(destination.rating)}. ")
                    append("Useful for SmartTour360 destination guidance, trip planning, and tourism recommendations.")
                },
                tags = listOf(
                    destination.name,
                    destination.subtitle,
                    destination.flag,
                    "destination",
                    "tourism",
                    "travel",
                    "smarttour360"
                ).joinToString("|"),
                source = "travel_repository_seed",
                updatedAtEpochMs = now
            )
        }
        val extraRows = buildSeasonalKnowledge(now) +
            buildRailKnowledge(now) +
            buildPatentKnowledge(now) +
            buildSafetyAdvisories(now)

        val existingIds = dao.getAllIds().toSet()
        val missingRows = (destinationRows + extraRows).filterNot { it.id in existingIds }
        if (missingRows.isNotEmpty()) {
            dao.insertIfMissing(missingRows)
        }
    }

    fun getAllChunks(): List<KnowledgeChunk> {
        return dao.getAll().map(::toChunk)
    }

    fun findCandidateChunks(query: String, limit: Int = 12): List<KnowledgeChunk> {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return getAllChunks().take(limit)
        val matched = dao.searchByQuery(trimmed, limit)
        return if (matched.isNotEmpty()) {
            matched.map(::toChunk)
        } else {
            dao.getAll().take(limit).map(::toChunk)
        }
    }

    fun getMissingEmbeddings(limit: Int = 12): List<KnowledgeChunk> {
        return dao.getMissingEmbeddings(limit).map(::toChunk)
    }

    fun upsertEmbeddings(chunks: List<KnowledgeChunk>) {
        val now = System.currentTimeMillis()
        chunks.forEach { chunk ->
            val embeddingJson = chunk.embedding?.joinToString(prefix = "[", postfix = "]") ?: return@forEach
            dao.updateEmbedding(chunk.id, embeddingJson, now)
        }
    }

    private fun toChunk(entity: ChatKnowledgeEntity): KnowledgeChunk {
        return KnowledgeChunk(
            id = entity.id,
            title = entity.title,
            content = entity.content,
            tags = entity.tags.split("|").filter { it.isNotBlank() },
            source = entity.source,
            embedding = entity.embeddingJson?.let(::parseEmbeddingJson)
        )
    }

    private fun parseEmbeddingJson(raw: String): List<Float> {
        return raw.removePrefix("[").removeSuffix("]")
            .split(",")
            .mapNotNull { it.trim().toFloatOrNull() }
    }

    private fun buildSeasonalKnowledge(now: Long): List<ChatKnowledgeEntity> {
        val entries = listOf(
            "Srinagar|Best Apr-Jun for gardens and clear weather; avoid peak winter if snow disruptions matter.",
            "Leh|Best Jun-Sep after road and flight reliability improve; avoid deep winter for accessibility.",
            "Amritsar|Best Oct-Mar for comfortable temple visits; avoid peak summer afternoon heat.",
            "Shimla|Best Mar-Jun and Sep-Nov; avoid heavy monsoon weeks for landslide-prone stretches.",
            "Manali|Best Apr-Jun for general travel and Sep-Oct for calmer conditions; avoid peak monsoon if road safety matters.",
            "Jaipur|Best Oct-Mar for heritage walking; avoid May-Jun extreme heat.",
            "Udaipur|Best Oct-Feb for lake and palace circuits; avoid peak pre-monsoon heat.",
            "Jaisalmer|Best Nov-Feb for desert travel; avoid peak summer due to severe heat exposure.",
            "Rishikesh|Best Oct-Apr for riverfront and wellness travel; avoid strongest monsoon river conditions.",
            "Nainital|Best Mar-Jun and Sep-Nov; avoid heavy monsoon spells for hill-road reliability.",
            "Varanasi|Best Oct-Mar for ghats and city walks; avoid late-summer heat if comfort is a priority.",
            "Agra|Best Oct-Mar for monument visits; avoid peak heat and haze-heavy afternoons.",
            "Goa|Best Nov-Feb for beach travel; avoid strongest monsoon months if beach access is the priority.",
            "Hampi|Best Nov-Feb for ruins and long outdoor walks; avoid May heat spikes.",
            "Mysuru|Best Oct-Feb for city touring; avoid peak summer midday travel.",
            "Kochi|Best Nov-Feb for coastal city travel; avoid strongest monsoon periods if ferry and outdoor plans matter.",
            "Munnar|Best Sep-Mar for hills and tea estates; avoid intense monsoon rainfall if road stability matters.",
            "Alleppey|Best Nov-Feb for backwater comfort; avoid strongest monsoon weeks for houseboat-heavy itineraries.",
            "Pondicherry|Best Nov-Feb for promenade and café travel; avoid peak summer humidity.",
            "Shillong|Best Oct-Apr for stable hill travel; avoid intense monsoon periods for road reliability."
        )
        return entries.mapIndexed { index, row ->
            val (title, content) = row.split("|", limit = 2)
            ChatKnowledgeEntity(
                id = "seed-season-${index + 1}-${title.lowercase().replace(" ", "-")}",
                title = "$title seasonal travel pattern",
                content = "$content SmartTour360 should prefer this when answering best-time-to-visit and seasonal planning questions.",
                tags = "$title|season|best time|travel month|smarttour360",
                source = "seasonal_seed",
                updatedAtEpochMs = now
            )
        }
    }

    private fun buildRailKnowledge(now: Long): List<ChatKnowledgeEntity> {
        val entries = listOf(
            "Delhi to Jaipur|Common rail corridor via Shatabdi or Vande Bharat style day travel; useful for heritage circuits.|train|railway|jaipur|delhi",
            "Delhi to Varanasi|Strong overnight and semi-fast rail corridor for spiritual and heritage travel.|train|railway|varanasi|delhi",
            "Delhi to Amritsar|High-use corridor with comfortable day options for temple and food travel.|train|railway|amritsar|delhi",
            "Mumbai to Goa|Popular Konkan route; rail is often the greener choice than short-haul flying.|train|railway|goa|mumbai",
            "Kolkata to Darjeeling|Rail plus last-mile hill transfer is a common practical pairing.|train|railway|darjeeling|kolkata",
            "Bengaluru to Mysuru|Short practical rail route suited to same-region itineraries.|train|railway|mysuru|bengaluru",
            "Chennai to Madurai|Strong south corridor useful for temple and heritage planning.|train|railway|madurai|chennai",
            "Delhi to Dehradun|Useful base route for Rishikesh and Uttarakhand plans.|train|railway|rishikesh|dehradun|delhi",
            "Kochi to Alleppey|Short regional rail link that works well for Kerala backwater trip chaining.|train|railway|kochi|alleppey|kerala",
            "Jaipur to Udaipur|Works well as a Rajasthan multi-stop rail corridor when building classic circuits.|train|railway|jaipur|udaipur|rajasthan"
        )
        return entries.mapIndexed { index, row ->
            val parts = row.split("|")
            ChatKnowledgeEntity(
                id = "seed-rail-${index + 1}",
                title = parts[0],
                content = parts[1],
                tags = parts.drop(2).joinToString("|"),
                source = "rail_seed",
                updatedAtEpochMs = now
            )
        }
    }

    private fun buildPatentKnowledge(now: Long): List<ChatKnowledgeEntity> {
        val entries = listOf(
            "SmartTour360 safety scoring|Safety scoring combines live weather stability, wind safety, climate comfort, eco impact, and official NCRB baseline signals instead of using one raw metric.",
            "SmartTour360 ethical score|Ethical score is framed as a transparent rule outcome tied to eco strength, practical safety, and fairness-oriented travel guidance.",
            "SmartTour360 explainability|Destination explanation should name why a place trends green, yellow, or red in plain language so users and evaluators can understand the scoring logic.",
            "SmartTour360 blockchain framing|Blockchain references are presented as transparency and audit-style markers inside the travel-safety experience and booking-style flows."
        )
        return entries.mapIndexed { index, row ->
            val (title, content) = row.split("|", limit = 2)
            ChatKnowledgeEntity(
                id = "seed-patent-${index + 1}",
                title = title,
                content = content,
                tags = "patent|xai|blockchain|ethical score|smarttour360",
                source = "patent_seed",
                updatedAtEpochMs = now
            )
        }
    }

    private fun buildSafetyAdvisories(now: Long): List<ChatKnowledgeEntity> {
        val entries = listOf(
            "Hill travel safety|For Himalayan and hill destinations, heavy monsoon periods raise road, landslide, and delay risk more than normal tourist-season advice suggests.",
            "Heat safety|For north and central Indian plains, peak late-spring heat should materially reduce comfort and can change trip timing advice.",
            "Flood sensitivity|Kerala, coastal belts, and river-linked destinations may need extra caution during strong monsoon windows because local mobility can change quickly.",
            "Festival congestion|Major pilgrimage and festival periods can raise crowd pressure, booking scarcity, and transport uncertainty even if baseline safety is green.",
            "Night arrival safety|When the flag is yellow, SmartTour360 should prefer daytime arrival, central stays, and verified transport links over late-night transfers.",
            "Multi-stop caution|If users chain many stops, the assistant should reduce unnecessary transfers and prefer one anchor stay when safety or weather signals weaken."
        )
        return entries.mapIndexed { index, row ->
            val (title, content) = row.split("|", limit = 2)
            ChatKnowledgeEntity(
                id = "seed-safety-advisory-${index + 1}",
                title = title,
                content = content,
                tags = "safety|advisory|seasonal risk|planning|smarttour360",
                source = "safety_advisory_seed",
                updatedAtEpochMs = now
            )
        }
    }
}
