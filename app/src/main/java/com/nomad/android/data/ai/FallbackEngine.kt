package com.nomad.android.data.ai

import com.nomad.android.data.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class FallbackEngine : AIEngine {

    private val survivalKeywords = mapOf(
        "cpr" to "To perform CPR: 1) Check responsiveness. 2) Call emergency services. 3) Push hard and fast in the center of the chest at 100-120 compressions per minute. 4) Give rescue breaths if trained. 5) Continue until help arrives.",
        "bleeding" to "For severe bleeding: 1) Apply direct pressure with a clean cloth. 2) Elevate the wound above the heart if possible. 3) Apply a pressure bandage. 4) Do not remove soaked cloths - add more on top. 5) Seek medical help immediately.",
        "water" to "To purify water: 1) Boil for at least 1 minute (3 minutes above 6,500 ft). 2) Use water purification tablets. 3) Filter through a clean cloth, then boil. 4) Solar disinfection: fill clear bottle and leave in direct sunlight for 6 hours.",
        "fire" to "To start a fire: 1) Gather tinder, kindling, and fuel. 2) Create a fire lay (teepee or log cabin). 3) Use matches, lighter, or friction method. 4) Shield from wind. 5) Never leave unattended. 6) Fully extinguish before leaving.",
        "shelter" to "Emergency shelter: 1) Find natural windbreaks. 2) Use branches and leaves for insulation. 3) Build a lean-to or debris hut. 4) Insulate the ground. 5) Keep shelter small to retain body heat. 6) Ensure ventilation if using fire nearby.",
        "navigation" to "Navigation without tools: 1) Sun rises in east, sets in west. 2) North Star (Polaris) indicates north. 3) Moss grows thicker on north side of trees (northern hemisphere). 4) Follow waterways downstream to civilization. 5) Note landmarks.",
        "knot" to "Essential knots: 1) Bowline - secure loop that won't slip. 2) Clove hitch - quick attachment to posts. 3) Square knot - joining two ropes. 4) Taut-line hitch - adjustable tension. 5) Figure-eight - stopper knot.",
        "plant" to "Edible plant identification: 1) Only eat plants you can positively identify. 2) Universal Edibility Test: test one part at a time. 3) Safe bets: dandelion, clover, cattail, pine needles (tea). 4) Avoid: milky sap, umbrella-shaped flowers, almond scent.",
        "sos" to "SOS signals: 1) Three of anything (fires, whistle blasts, flashes). 2) Ground-to-air signals: use contrasting materials, minimum 10ft tall. 3) Mirror signaling: flash toward aircraft. 4) Smoke: white smoke for daylight, bright fires at night.",
        "first aid" to "First aid basics: 1) Stop bleeding first. 2) Treat for shock (lay down, elevate legs, keep warm). 3) Splint fractures with rigid materials. 4) Burns: cool with water, cover with clean dressing. 5) Never give food/water to unconscious person."
    )

    override suspend fun generate(prompt: String, context: List<String>, imagePath: String?): String {
        if (imagePath != null) {
            return buildString {
                appendLine("[FALLBACK MODE - AI model not available]")
                appendLine()
                appendLine("Image analysis requires the AI model. Download a model pack in Settings.")
            }
        }

        val lowercasePrompt = prompt.lowercase()
        val matchedResponses = survivalKeywords.filter { (keyword, _) ->
            lowercasePrompt.contains(keyword)
        }.values.toList()

        return if (matchedResponses.isNotEmpty()) {
            buildString {
                appendLine("[FALLBACK MODE - AI model not available]")
                appendLine()
                appendLine("Using offline knowledge base:")
                appendLine()
                matchedResponses.forEach { response ->
                    appendLine("- $response")
                    appendLine()
                }
                appendLine("Tip: Download an AI model pack in Settings for full AI capabilities.")
            }
        } else {
            buildString {
                appendLine("[FALLBACK MODE - AI model not available]")
                appendLine()
                appendLine("I don't have specific information about that in my offline knowledge base.")
                appendLine("Try asking about: CPR, bleeding, water purification, fire starting, shelter,")
                appendLine("navigation, knots, edible plants, SOS signals, or first aid.")
                appendLine()
                appendLine("Tip: Download an AI model pack in Settings for full AI capabilities.")
            }
        }
    }

    override fun generateStream(prompt: String, context: List<String>, imagePath: String?): Flow<String> = flow {
        val response = generate(prompt, context, imagePath)
        response.chunked(5).forEach { chunk ->
            emit(chunk)
            kotlinx.coroutines.delay(20)
        }
    }

    override suspend fun isAvailable(): Boolean = true

    override fun getModelName(): String = "Fallback (Rule-Based)"

    override fun getDeviceInfo(): DeviceInfo {
        return DeviceInfo(
            totalRamMB = 0,
            availableRamMB = 0,
            hasNPU = false,
            hasGPU = false
        )
    }

    override suspend fun loadModel(): Result<Unit> = Result.success(Unit)

    override fun unloadModel() {}
}
