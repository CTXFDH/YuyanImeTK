package com.yuyan.imemodule.ui.keyboard

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yuyan.imemodule.speech.RecognitionState
import kotlinx.coroutines.delay

@Composable
fun VoiceKeyboardLayout(
    modifier: Modifier = Modifier,
    isDarkTheme: Boolean = false,
    isVoicePressed: Boolean = false,
    recognitionState: RecognitionState = RecognitionState.IDLE,
    amplitude: Float = 0f,
    spectrum: FloatArray = FloatArray(16),
    onDeleteClick: () -> Unit = {},
    onSendClick: () -> Unit = {},
    onStartVoice: () -> Unit = {},
    onStopVoice: () -> Unit = {},
    onCursorMove: (Int) -> Unit = {}
) {
    val boxBgColor = if (isDarkTheme) Color(0xFF252836) else Color(0xFFE2E8F0)
    val circleButtonBgColor = if (isDarkTheme) Color(0xFF33374B) else Color(0xFFE2E8F0)
    val buttonIconColor = if (isDarkTheme) Color.White else Color(0xFF1E293B)

    val primaryAccent = Color(0xFF07C160)
    val primaryAccentPressed = Color(0xFF06AD56)

    var accumulativeDrag by remember { mutableFloatStateOf(0f) }
    val dragThresholdPx = with(LocalDensity.current) { 16.dp.toPx() }

    var isDeletePressed by remember { mutableStateOf(false) }

    LaunchedEffect(isDeletePressed) {
        if (isDeletePressed) {
            onDeleteClick()
            delay(400)
            while (isDeletePressed) {
                onDeleteClick()
                delay(50)
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(if (isDarkTheme) Color(0xFF181A20) else Color(0xFFF1F5F9))
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.SpaceEvenly,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(32.dp))
                    .background(boxBgColor)
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures(
                            onDragStart = { accumulativeDrag = 0f },
                            onDragEnd = { accumulativeDrag = 0f },
                            onDragCancel = { accumulativeDrag = 0f },
                            onHorizontalDrag = { change, dragAmount ->
                                change.consume()
                                accumulativeDrag += dragAmount
                                if (accumulativeDrag >= dragThresholdPx) {
                                    onCursorMove(1)
                                    accumulativeDrag = 0f
                                } else if (accumulativeDrag <= -dragThresholdPx) {
                                    onCursorMove(-1)
                                    accumulativeDrag = 0f
                                }
                            }
                        )
                    }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                AudioSpectrumAnimation(
                    modifier = Modifier.fillMaxSize(),
                    isActive = recognitionState == RecognitionState.LISTENING || 
                               recognitionState == RecognitionState.PROCESSING,
                    amplitude = amplitude,
                    spectrum = spectrum
                )
            }

            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(if (isDeletePressed) primaryAccent else circleButtonBgColor)
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            awaitFirstDown(requireUnconsumed = false)
                            isDeletePressed = true
                            do {
                                val event = awaitPointerEvent()
                                val up = event.changes.all { !it.pressed }
                                if (up) break
                            } while (true)
                            isDeletePressed = false
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "⌫",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDeletePressed) Color.White else buttonIconColor
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val voiceBtnBgColor = if (isVoicePressed) primaryAccent else circleButtonBgColor
            val voiceBtnTextColor = if (isVoicePressed) Color.White else buttonIconColor

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(32.dp))
                    .background(voiceBtnBgColor)
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            awaitFirstDown(requireUnconsumed = false)
                            onStartVoice()
                            do {
                                val event = awaitPointerEvent()
                                val up = event.changes.all { !it.pressed }
                                if (up) break
                            } while (true)
                            onStopVoice()
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isVoicePressed) "松 开 结 束" else "按 住 说 话",
                    color = voiceBtnTextColor,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }

            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(circleButtonBgColor)
                    .clickable { onSendClick() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "↑",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = buttonIconColor
                )
            }
        }
    }
}

@Composable
fun AudioSpectrumAnimation(
    modifier: Modifier = Modifier,
    isActive: Boolean = true,
    amplitude: Float = 0f,
    spectrum: FloatArray = FloatArray(16),
    barWidthFactor: Float = 1.8f,
    barCount: Int = 11,
    spacingRatio: Float = 0.5f,
    heightScale: Float = 0.88f,
) {
    if (!isActive) {
        Canvas(modifier = modifier) {}
        return
    }

    val amplitudeValue = amplitude.coerceIn(0f, 1f)

    val barTargets = remember(spectrum, amplitudeValue, barCount) {
        FloatArray(barCount) { index ->
            if (spectrum.isNotEmpty()) {
                spectrum[index % spectrum.size]
            } else {
                amplitudeValue
            }
        }
    }
    val animatedValues = barTargets.mapIndexed { index, target ->
        animateFloatAsState(
            targetValue = target,
            animationSpec = tween(durationMillis = 120),
            label = "spectrumBar$index"
        )
    }

    val colors = listOf(
        Color(0xFFFF6B6B),
        Color(0xFFFF8E72),
        Color(0xFFFF9F43),
        Color(0xFFFFB86C),
        Color(0xFFFFEAA7),
        Color(0xFF55EFC4),
        Color(0xFF74D7AE),
        Color(0xFF54A0FF),
        Color(0xFF5F27CD),
        Color(0xFFFF6B6B),
        Color(0xFFFF8E72),
    )

    Canvas(modifier = modifier) {
        val barWidth = size.width / (barCount * barWidthFactor)
        val spacing = barWidth * spacingRatio
        val maxHeight = size.height

        repeat(barCount) { index ->
            val barHeight = maxHeight * animatedValues[index].value.coerceIn(0.15f, 0.98f) * heightScale
            val totalWidth = barCount * barWidth + (barCount - 1) * spacing
            val startX = (size.width - totalWidth) / 2f
            val x = startX + index * (barWidth + spacing)
            val y = (maxHeight - barHeight) / 2f
            val barAlpha = 0.65f + amplitudeValue * 0.35f

            drawRoundRect(
                color = colors[index % colors.size].copy(alpha = barAlpha),
                topLeft = Offset(x, y),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
            )
        }
    }
}
