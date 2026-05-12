package com.nomad.android.data.content

/**
 * Searchable index of bundled offline survival knowledge.
 * Extracted from KiwixManager for testability — no Android Context required.
 */
object BundledContentSearch {

    data class KnowledgeEntry(val title: String, val content: String)

    val entries: List<KnowledgeEntry> = listOf(
        KnowledgeEntry("CPR Basics", "To perform CPR: Check responsiveness, call emergency services, push hard and fast in the center of the chest at 100-120 compressions per minute, give rescue breaths if trained."),
        KnowledgeEntry("Water Purification", "Boil water for at least 1 minute (3 minutes above 6,500 ft). Use purification tablets or chlorine dioxide drops. Solar disinfection: clear bottle in direct sunlight for 6 hours."),
        KnowledgeEntry("Fire Starting", "Gather tinder, kindling, and fuel. Create a fire lay. Use matches, lighter, or friction method. Shield from wind. Never leave unattended."),
        KnowledgeEntry("Shelter Building", "Find natural windbreaks. Use branches and leaves for insulation. Build a lean-to or debris hut. Insulate the ground. Keep shelter small to retain body heat."),
        KnowledgeEntry("Navigation", "Sun rises in east, sets in west. North Star (Polaris) indicates north. Follow waterways downstream to civilization. Note landmarks."),
        KnowledgeEntry("Edible Plants", "Only eat plants you can positively identify. Safe bets: dandelion, clover, cattail, pine needles (tea). Avoid: milky sap, umbrella-shaped flowers, almond scent."),
        KnowledgeEntry("SOS Signals", "Three of anything (fires, whistle blasts, flashes). Ground-to-air signals: use contrasting materials, minimum 10ft tall. Mirror signaling: flash toward aircraft."),
        KnowledgeEntry("First Aid", "Stop bleeding first. Treat for shock (lay down, elevate legs, keep warm). Splint fractures with rigid materials. Burns: cool with water, cover with clean dressing."),
        KnowledgeEntry("Knots", "Essential knots: Bowline (secure loop), Clove hitch (quick attachment), Square knot (joining ropes), Taut-line hitch (adjustable tension), Figure-eight (stopper knot)."),
    )

    /**
     * Search bundled knowledge by title and content (case-insensitive substring match).
     * Returns results formatted as [ZimSearchResult] with snippet preview.
     * When no matches are found, returns a single "no matches" result.
     */
    fun search(query: String): List<ZimSearchResult> {
        val lowercaseQuery = query.lowercase()
        return entries
            .filter { it.title.lowercase().contains(lowercaseQuery) || it.content.lowercase().contains(lowercaseQuery) }
            .map { ZimSearchResult("A/${it.title}", it.title, it.content.take(100) + "...") }
            .ifEmpty { listOf(ZimSearchResult("A/Search", "Search: $query", "No matches in bundled content. Try downloading knowledge packs in Settings.")) }
    }
}
