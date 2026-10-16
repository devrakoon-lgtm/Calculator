package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CoralOperator
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.CyanAccentGlow
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceElevated
import com.example.ui.theme.SurfaceSlate
import com.example.ui.viewmodel.AppTab
import com.example.ui.viewmodel.CalculatorUiState
import com.example.ui.viewmodel.CalculatorViewModel

@Composable
fun CalculatorScreen(
    state: CalculatorUiState,
    viewModel: CalculatorViewModel,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Display Section
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .testTag("calculator_display_card"),
            colors = CardDefaults.cardColors(
                containerColor = SurfaceSlate
            ),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top row inside display: quick mode badge + action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilledTonalButton(
                        onClick = { viewModel.selectTab(AppTab.SCANNER) },
                        modifier = Modifier.testTag("quick_scan_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "Scan Problem",
                            modifier = Modifier.size(16.dp),
                            tint = CyanAccent
                        )
                        Spacer(modifier = Modifier.size(6.dp))
                        Text(
                            text = "Scan Math",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = CyanAccent
                        )
                    }

                    IconButton(
                        onClick = { viewModel.selectTab(AppTab.HISTORY) },
                        modifier = Modifier.testTag("quick_history_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "Calculation History",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Middle & Bottom: Expression + Live Preview + Display Number
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.End
                ) {
                    // Full expression
                    Text(
                        text = state.currentExpression.ifEmpty { " " },
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = 20.sp,
                            fontFamily = FontFamily.Monospace
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        textAlign = TextAlign.End,
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(scrollState)
                            .testTag("calculator_expression_text")
                    )

                    // Live calculation preview
                    if (!state.liveResultPreview.isNullOrBlank() && !state.isEvaluated) {
                        Text(
                            text = "= ${state.liveResultPreview}",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontSize = 24.sp,
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = CyanAccentGlow.copy(alpha = 0.85f),
                            textAlign = TextAlign.End,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .testTag("calculator_live_preview")
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Main display value
                    Text(
                        text = state.displayValue,
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontSize = if (state.displayValue.length > 8) 36.sp else 48.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        ),
                        color = Color.White,
                        textAlign = TextAlign.End,
                        maxLines = 1,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("calculator_main_display")
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Keypad Section
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1.8f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Row 1: Clear, Toggle Sign, Quick Scan, Operator -
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                KeypadButton(
                    text = "C",
                    modifier = Modifier.weight(1f),
                    color = SurfaceElevated,
                    textColor = CyanAccent,
                    tag = "key_clear"
                ) {
                    viewModel.onClear()
                }
                KeypadButton(
                    text = "±",
                    modifier = Modifier.weight(1f),
                    color = SurfaceElevated,
                    textColor = Color.White,
                    tag = "key_toggle_sign"
                ) {
                    viewModel.onToggleSign()
                }
                KeypadIconButton(
                    icon = Icons.AutoMirrored.Filled.Backspace,
                    contentDescription = "Backspace",
                    modifier = Modifier.weight(1f),
                    color = SurfaceElevated,
                    iconColor = Color.White,
                    tag = "key_backspace"
                ) {
                    viewModel.onBackspace()
                }
                KeypadButton(
                    text = "−",
                    modifier = Modifier.weight(1f),
                    color = CoralOperator,
                    textColor = Color.White,
                    tag = "key_operator_minus"
                ) {
                    viewModel.onOperator('-')
                }
            }

            // Row 2: 7, 8, 9, Operator +
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                KeypadButton("7", Modifier.weight(1f), tag = "key_7") { viewModel.onDigit("7") }
                KeypadButton("8", Modifier.weight(1f), tag = "key_8") { viewModel.onDigit("8") }
                KeypadButton("9", Modifier.weight(1f), tag = "key_9") { viewModel.onDigit("9") }
                KeypadButton(
                    text = "+",
                    modifier = Modifier.weight(1f),
                    color = CoralOperator,
                    textColor = Color.White,
                    tag = "key_operator_plus"
                ) {
                    viewModel.onOperator('+')
                }
            }

            // Row 3: 4, 5, 6, Subtraction quick helper
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                KeypadButton("4", Modifier.weight(1f), tag = "key_4") { viewModel.onDigit("4") }
                KeypadButton("5", Modifier.weight(1f), tag = "key_5") { viewModel.onDigit("5") }
                KeypadButton("6", Modifier.weight(1f), tag = "key_6") { viewModel.onDigit("6") }
                KeypadButton(
                    text = "+/−",
                    modifier = Modifier.weight(1f),
                    color = SurfaceElevated,
                    textColor = CyanAccent,
                    tag = "key_step_helper"
                ) {
                    viewModel.onToggleSign()
                }
            }

            // Row 4: 1, 2, 3, Equals =
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                KeypadButton("1", Modifier.weight(1f), tag = "key_1") { viewModel.onDigit("1") }
                KeypadButton("2", Modifier.weight(1f), tag = "key_2") { viewModel.onDigit("2") }
                KeypadButton("3", Modifier.weight(1f), tag = "key_3") { viewModel.onDigit("3") }
                KeypadButton(
                    text = "=",
                    modifier = Modifier.weight(1f),
                    color = CyanAccent,
                    textColor = Color(0xFF0F172A),
                    tag = "key_equals"
                ) {
                    viewModel.onEquals()
                }
            }

            // Row 5: 0 (double wide), ., Scan Action
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                KeypadButton("0", Modifier.weight(2.1f), tag = "key_0") { viewModel.onDigit("0") }
                KeypadButton(".", Modifier.weight(1f), tag = "key_decimal") { viewModel.onDecimal() }
                KeypadIconButton(
                    icon = Icons.Default.CameraAlt,
                    contentDescription = "Scan Image",
                    modifier = Modifier.weight(1f),
                    color = SurfaceCard,
                    iconColor = CyanAccent,
                    tag = "key_scan_icon"
                ) {
                    viewModel.selectTab(AppTab.SCANNER)
                }
            }
        }
    }
}

@Composable
fun KeypadButton(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = SurfaceCard,
    textColor: Color = Color.White,
    tag: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .aspectRatio(1.25f)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .testTag(tag),
        color = color,
        shape = RoundedCornerShape(16.dp),
        shadowElevation = 2.dp
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = 26.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                color = textColor
            )
        }
    }
}

@Composable
fun KeypadIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    modifier: Modifier = Modifier,
    color: Color = SurfaceCard,
    iconColor: Color = Color.White,
    tag: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .aspectRatio(1.25f)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .testTag(tag),
        color = color,
        shape = RoundedCornerShape(16.dp),
        shadowElevation = 2.dp
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = iconColor,
                modifier = Modifier.size(26.dp)
            )
        }
    }
}
