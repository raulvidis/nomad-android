package com.nomad.android.ui.emergency

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nomad.android.R
import com.nomad.android.ui.components.TerminalCard
import com.nomad.android.ui.components.TerminalDivider
import com.nomad.android.ui.components.TerminalSectionHeader
import com.nomad.android.ui.components.TerminalText
import com.nomad.android.ui.theme.TertiaryAmber
import com.nomad.android.ui.theme.BackgroundDark
import com.nomad.android.ui.theme.PhosphorGreen
import com.nomad.android.ui.theme.PhosphorGreenDim
import com.nomad.android.ui.theme.SurfaceContainerLow

private data class FirstAidTopic(
    val title: String,
    val steps: List<String>,
)

private val firstAidTopics = listOf(
    FirstAidTopic("CPR", listOf(
        "Check responsiveness — tap shoulders, shout",
        "Call emergency services if possible",
        "Place heel of hand on center of chest",
        "Push hard and fast: 100-120 compressions/min",
        "Depth: at least 2 inches for adults",
        "Give 2 rescue breaths every 30 compressions if trained",
        "Continue until help arrives or person recovers",
    )),
    FirstAidTopic("Severe Bleeding", listOf(
        "Apply direct pressure with clean cloth",
        "Elevate wound above heart if possible",
        "Apply pressure bandage firmly",
        "Do NOT remove soaked cloths — add more on top",
        "Apply tourniquet only as last resort (limb injuries)",
        "Keep victim warm and calm",
        "Seek medical help immediately",
    )),
    FirstAidTopic("Burns", listOf(
        "Cool burn under running water for 20 minutes",
        "Remove jewelry/clothing near burn (not stuck fabric)",
        "Cover with clean, non-stick dressing",
        "Do NOT apply ice, butter, or ointments",
        "Do NOT pop blisters",
        "For chemical burns: flush with water for 20+ minutes",
        "Seek medical help for large or deep burns",
    )),
    FirstAidTopic("Fractures", listOf(
        "Do NOT move the injured limb",
        "Immobilize with splint (rigid material + padding)",
        "Splint should extend beyond joints above and below fracture",
        "Apply cold pack wrapped in cloth (20 min on, 20 min off)",
        "Elevate if possible to reduce swelling",
        "Check circulation below injury (pulse, color, warmth)",
        "Seek medical help — do not attempt to realign bone",
    )),
    FirstAidTopic("Shock", listOf(
        "Lay person flat on their back",
        "Elevate legs 12 inches (unless head/neck/back injury)",
        "Keep warm with blanket or clothing",
        "Do NOT give food or water",
        "Loosen tight clothing",
        "Turn on side if vomiting or bleeding from mouth",
        "Begin CPR if no breathing — seek help immediately",
    )),
    FirstAidTopic("Choking", listOf(
        "Ask: 'Are you choking?' — if they can't speak, act",
        "Give 5 back blows between shoulder blades",
        "Give 5 abdominal thrusts (Heimlich maneuver)",
        "Alternate 5 back blows and 5 thrusts",
        "For infants: face down, 5 back blows, then chest thrusts",
        "If person becomes unconscious: begin CPR",
        "Call emergency services as soon as possible",
    )),
    FirstAidTopic("Hypothermia", listOf(
        "Move to warm, dry shelter",
        "Remove wet clothing",
        "Warm gradually with blankets, body heat, warm drinks",
        "Do NOT rewarm too quickly (no hot water)",
        "Do NOT give alcohol",
        "Warm core first (chest, neck, head, groin)",
        "Seek medical help — hypothermia is life-threatening",
    )),
    FirstAidTopic("Snake Bite", listOf(
        "Stay calm — keep heart rate low",
        "Immobilize bitten limb, keep below heart level",
        "Remove rings, watches, tight clothing",
        "Clean wound gently with soap and water",
        "Cover with clean, dry dressing",
        "Do NOT cut wound, suck venom, or apply tourniquet",
        "Seek medical help — note snake appearance if possible",
    )),
)

private data class ChecklistCategory(
    val name: String,
    val items: List<Pair<String, Boolean>>,
)

private val survivalChecklist = listOf(
    ChecklistCategory("Water", listOf(
        "Water bottles or containers" to false,
        "Water purification tablets" to false,
        "Portable water filter" to false,
        "Metal pot for boiling" to false,
    )),
    ChecklistCategory("Food", listOf(
        "Non-perishable food (3-day supply)" to false,
        "Manual can opener" to false,
        "High-energy snacks (nuts, bars)" to false,
        "Fishing kit or snares" to false,
    )),
    ChecklistCategory("Shelter", listOf(
        "Emergency blanket or tarp" to false,
        "Rope or paracord (50ft minimum)" to false,
        "Duct tape" to false,
        "Knife or multi-tool" to false,
    )),
    ChecklistCategory("First Aid", listOf(
        "Bandages and gauze" to false,
        "Antiseptic wipes" to false,
        "Pain relievers" to false,
        "Personal medications" to false,
    )),
    ChecklistCategory("Navigation", listOf(
        "Physical map of area" to false,
        "Compass" to false,
        "Whistle (signal)" to false,
        "Flashlight with extra batteries" to false,
    )),
    ChecklistCategory("Fire", listOf(
        "Waterproof matches or lighter" to false,
        "Fire starter (ferro rod, tinder)" to false,
        "Candle (fire starter + light)" to false,
    )),
)

@Composable
fun EmergencyScreen() {
    var expandedTopic by remember { mutableStateOf<String?>(null) }
    var checkedItems by rememberSaveable { mutableStateOf(survivalChecklist) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        TerminalText(
            text = "Survival Reference",
            color = TertiaryAmber,
            style = androidx.compose.ui.text.TextStyle(
                fontSize = 20.sp,
                fontFamily = androidx.compose.ui.text.font.FontFamily(
                    androidx.compose.ui.text.font.Font(R.font.space_grotesk_bold, FontWeight.Bold),
                ),
            ),
        )

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                TerminalSectionHeader(text = "First Aid — Conditions A-Z")
            }

            items(firstAidTopics) { topic ->
                val isExpanded = expandedTopic == topic.title
                FirstAidRow(
                    topic = topic,
                    isExpanded = isExpanded,
                    onToggle = { expandedTopic = if (isExpanded) null else topic.title },
                )
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                TerminalSectionHeader(text = "Survival Checklist")
                Spacer(modifier = Modifier.height(8.dp))
            }

            items(checkedItems) { checklist ->
                ChecklistSection(
                    category = checklist.name,
                    items = checklist.items,
                    onToggle = { itemIndex ->
                        checkedItems = checkedItems.map { c ->
                            if (c.name == checklist.name) {
                                c.copy(items = c.items.mapIndexed { i, pair ->
                                    if (i == itemIndex) pair.first to !pair.second else pair
                                })
                            } else c
                        }
                    },
                )
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                TerminalCard {
                    Text(
                        text = "All content available offline — no network required",
                        color = PhosphorGreenDim,
                        fontFamily = androidx.compose.ui.text.font.FontFamily(
                            androidx.compose.ui.text.font.Font(R.font.space_grotesk_regular, FontWeight.Normal),
                        ),
                        fontSize = 11.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun FirstAidRow(
    topic: FirstAidTopic,
    isExpanded: Boolean,
    onToggle: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onToggle,
            )
            .background(SurfaceContainerLow, RoundedCornerShape(0.dp))
            .border(2.dp, TertiaryAmber, RoundedCornerShape(0.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = topic.title.uppercase(),
                color = TertiaryAmber,
                fontFamily = androidx.compose.ui.text.font.FontFamily(
                    androidx.compose.ui.text.font.Font(R.font.space_grotesk_semi_bold, FontWeight.Medium),
                ),
                fontSize = 13.sp,
            )
            Text(
                text = if (isExpanded) "[-]" else "[+]",
                color = PhosphorGreen,
                fontFamily = androidx.compose.ui.text.font.FontFamily(
                    androidx.compose.ui.text.font.Font(R.font.space_grotesk_regular, FontWeight.Normal),
                ),
                fontSize = 12.sp,
            )
        }

        if (isExpanded) {
            Spacer(modifier = Modifier.height(4.dp))
            topic.steps.forEachIndexed { i, step ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "${i + 1}.",
                        color = PhosphorGreenDim,
                        fontFamily = androidx.compose.ui.text.font.FontFamily(
                            androidx.compose.ui.text.font.Font(R.font.space_grotesk_regular, FontWeight.Normal),
                        ),
                        fontSize = 11.sp,
                    )
                    Text(
                        text = step,
                        color = PhosphorGreen,
                        fontFamily = androidx.compose.ui.text.font.FontFamily(
                            androidx.compose.ui.text.font.Font(R.font.space_grotesk_regular, FontWeight.Normal),
                        ),
                        fontSize = 11.sp,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun ChecklistSection(
    category: String,
    items: List<Pair<String, Boolean>>,
    onToggle: (Int) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = category.uppercase(),
            color = PhosphorGreen,
            fontFamily = androidx.compose.ui.text.font.FontFamily(
                androidx.compose.ui.text.font.Font(R.font.space_grotesk_semi_bold, FontWeight.Medium),
            ),
            fontSize = 13.sp,
        )
        items.forEachIndexed { index, (item, checked) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { onToggle(index) },
                    )
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                val checkbox = if (checked) "[X]" else "[ ]"
                val color = if (checked) PhosphorGreen else PhosphorGreenDim
                Text(
                    text = checkbox,
                    color = color,
                    fontFamily = androidx.compose.ui.text.font.FontFamily(
                        androidx.compose.ui.text.font.Font(R.font.space_grotesk_regular, FontWeight.Normal),
                    ),
                    fontSize = 12.sp,
                )
                Text(
                    text = item,
                    color = color,
                    fontFamily = androidx.compose.ui.text.font.FontFamily(
                        androidx.compose.ui.text.font.Font(R.font.space_grotesk_regular, FontWeight.Normal),
                    ),
                    fontSize = 12.sp,
                )
            }
        }
    }
}
