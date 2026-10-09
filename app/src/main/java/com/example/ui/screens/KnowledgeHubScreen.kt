package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ElectricGold
import com.example.ui.theme.HologramPurple
import com.example.ui.theme.MatrixGreen
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.SpaceBlack
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceCardBorder
import com.example.ui.theme.SurfaceCardElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.HudTab
import com.example.viewmodel.MayaViewModel

@Composable
fun KnowledgeHubScreen(
    viewModel: MayaViewModel,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SpaceBlack)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Screen Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(HologramPurple.copy(alpha = 0.2f))
                    .border(1.dp, HologramPurple, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Psychology,
                    contentDescription = null,
                    tint = HologramPurple,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "HIGH KNOWLEDGE & WEB HUB",
                    color = TextPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "JARVIS Cognitive Retrieval Engine",
                    color = TextMuted,
                    fontSize = 12.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Web Search Box
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("knowledge_search_card"),
            colors = CardDefaults.cardColors(containerColor = SurfaceCard),
            border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.4f)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "SEARCH THE WEB WITH MAYA",
                    color = NeonCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            text = "Search anything (e.g. ISRO Gaganyaan mission, Quantum Physics)...",
                            color = TextMuted,
                            fontSize = 12.5.sp
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("web_search_input_field"),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = SurfaceCardBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = {
                        if (searchQuery.isNotBlank()) {
                            viewModel.openWebSearch(searchQuery)
                        }
                    }),
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = { viewModel.openWebSearch(searchQuery) },
                                modifier = Modifier.testTag("direct_web_search_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.OpenInBrowser,
                                    contentDescription = "Search in Browser",
                                    tint = NeonCyan
                                )
                            }
                        }
                    }
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            if (searchQuery.isNotBlank()) {
                                viewModel.openWebSearch(searchQuery)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SurfaceCardElevated,
                            contentColor = TextPrimary
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceCardBorder),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("launch_web_btn")
                    ) {
                        Icon(imageVector = Icons.Default.Language, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Search Web", fontSize = 12.sp)
                    }

                    Button(
                        onClick = {
                            if (searchQuery.isNotBlank()) {
                                viewModel.processUserCommand("Maya, search web for $searchQuery aur mujhe vistar se batao")
                                viewModel.setTab(HudTab.CORE)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NeonCyan,
                            contentColor = SpaceBlack
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("ask_maya_ai_btn")
                    ) {
                        Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Ask Maya AI", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // High Knowledge Exploration Cards
        Text(
            text = "DEEP INTELLIGENCE TOPICS",
            color = TextSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
        Spacer(modifier = Modifier.height(8.dp))

        val knowledgeTopics = listOf(
            KnowledgeTopic(
                title = "Space & Astronomy",
                subtitle = "ISRO, Chandrayaan, Gaganyaan aur Black holes",
                icon = Icons.Default.RocketLaunch,
                color = ElectricGold,
                prompt = "Maya, ISRO ke latest space missions aur Gaganyaan ke baare mein Hindi mein detail batao"
            ),
            KnowledgeTopic(
                title = "Quantum Computing & Physics",
                subtitle = "Qubits, Superposition aur Quantum Supremacy",
                icon = Icons.Default.AutoAwesome,
                color = NeonCyan,
                prompt = "Maya, Quantum Computing kaise kaam karta hai simple Hindi mein samjhao"
            ),
            KnowledgeTopic(
                title = "AI & Machine Learning",
                subtitle = "Neural networks, Gemini models aur Transformers",
                icon = Icons.Default.Psychology,
                color = HologramPurple,
                prompt = "Maya, Large Language Models kaise trained hote hain aur unka architecture kya hai?"
            ),
            KnowledgeTopic(
                title = "Programming & Android Core",
                subtitle = "Kotlin, Coroutines, Jetpack Compose aur Linux kernel",
                icon = Icons.Default.Code,
                color = MatrixGreen,
                prompt = "Maya, Kotlin Coroutines aur Flow ke best practices kya hain Android app development mein?"
            )
        )

        knowledgeTopics.forEach { topic ->
            KnowledgeTopicCard(
                topic = topic,
                onClick = {
                    viewModel.processUserCommand(topic.prompt)
                    viewModel.setTab(HudTab.CORE)
                }
            )
            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

data class KnowledgeTopic(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val color: Color,
    val prompt: String
)

@Composable
fun KnowledgeTopicCard(
    topic: KnowledgeTopic,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("topic_card_${topic.title.replace(" ", "_")}"),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceCardBorder),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(topic.color.copy(alpha = 0.15f))
                    .border(1.dp, topic.color.copy(alpha = 0.4f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = topic.icon,
                    contentDescription = topic.title,
                    tint = topic.color,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = topic.title,
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = topic.subtitle,
                    color = TextMuted,
                    fontSize = 11.5.sp
                )
            }
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = "Ask",
                tint = topic.color,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
