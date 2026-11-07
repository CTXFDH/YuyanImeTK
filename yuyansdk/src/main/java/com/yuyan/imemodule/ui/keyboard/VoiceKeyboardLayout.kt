package com.yuyan.imemodule.ui.keyboard

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.sin

@Composable
fun VoiceKeyboardLayout(
    keyBackgroundColor: Color,
    keyTextColor: Color,
    specialKeyBackgroundColor: Color,
    keyboardBackgroundColor: Color,
    modifier: Modifier = Modifier,
    isDarkTheme: Boolean = false,
    themeId: String = "ocean_blue",
    bottomActive: Boolean = false,
    leftActive: Boolean = false,
    rightActive: Boolean = false,
    recognitionState: RecognitionState = RecognitionState.IDLE,
    recognizedText: String = "",
    amplitude: Float = 0f,
    spectrum: FloatArray = FloatArray(16),
    onDeleteClick: () -> Unit = {},
    onClearAll: () -> Unit = {},
    onSendClick: () -> Unit = {},
    onStartVoice: () -> Unit = {},
    onStopVoice: () -> Unit = {},
    onCursorMove: (Int) -> Unit = {}
) {
    val boxBgColor = if (isDarkTheme) Color(0xFF252836) else Color(0xFFE2E8F0)
    val circleButtonBgColor = specialKeyBackgroundColor
    val buttonIconColor = keyTextColor

    val primaryAccent = Color(0xFF6366F1)
    val primaryAccentPressed = Color(0xFF4F46E5)
    val voicePressedColor = Color(0xFFE53935) // 按住说话时的高亮录音红

    var accumulativeDrag by remember { mutableFloatStateOf(0f) }
    val dragThresholdPx = with(LocalDensity.current) { 16.dp.toPx() }

    val coroutineScope = rememberCoroutineScope()
    var isDeletePressed by remember { mutableStateOf(false) }

    val isRecordingActive = bottomActive || 
                            recognitionState == RecognitionState.LISTENING || 
                            recognitionState == RecognitionState.PROCESSING

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(keyboardBackgroundColor)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.SpaceEvenly,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 第一行（上）：上矩形框（支持左右滑动移动光标 + 非输入状态下显示提示文字，录音/输入状态下展示彩色波形） + 右上角 Backspace 删除键
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 上矩形框：支持水平滑动滑动控光标
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
                                    onCursorMove(1) // 手指向右划 -> 光标向右移
                                    accumulativeDrag = 0f
                                } else if (accumulativeDrag <= -dragThresholdPx) {
                                    onCursorMove(-1) // 手指向左划 -> 光标向左移
                                    accumulativeDrag = 0f
                                }
                            }
                        )
                    }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isRecordingActive) {
                    AudioSpectrumAnimation(
                        modifier = Modifier.fillMaxSize(),
                        isActive = true,
                        amplitude = amplitude,
                        spectrum = spectrum
                    )
                } else {
                    Text(
                        text = "左右滑动手指移动光标",
                        color = buttonIconColor.copy(alpha = 0.4f),
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // 右上：退格删除按键（支持短按、长按连续删除、上滑清空）
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(if (isDeletePressed || leftActive) primaryAccentPressed else circleButtonBgColor)
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            isDeletePressed = true
                            val startY = down.position.y
                            var cleared = false

                            val deleteJob: Job = coroutineScope.launch {
                                onDeleteClick()
                                delay(400)
                                while (true) {
                                    onDeleteClick()
                                    delay(50)
                                }
                            }

                            do {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull()
                                if (change != null) {
                                    val currentY = change.position.y
                                    if (startY - currentY > 40f && !cleared) {
                                        cleared = true
                                        deleteJob.cancel()
                                        isDeletePressed = false
                                        onClearAll()
                                        change.consume()
                                        break
                                    }
                                }
                                val up = event.changes.all { !it.pressed }
                                if (up) break
                            } while (true)

                            deleteJob.cancel()
                            isDeletePressed = false
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "⌫",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDeletePressed || leftActive) Color.White else buttonIconColor
                )
            }
        }

        // 第二行（下）：下矩形框（长按说话按键） + 右下角 Send 消息发送键
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 下矩形框：核心语音长按按键
            val voiceBtnBg = if (isRecordingActive) voicePressedColor else primaryAccent
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(32.dp))
                    .background(voiceBtnBg)
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
                    text = if (isRecordingActive) "松开结束" else "按 住 说 话",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }

            // 右下：消息发送按键
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(if (rightActive) primaryAccentPressed else circleButtonBgColor)
                    .clickable { onSendClick() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "↑",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (rightActive) Color.White else buttonIconColor
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

    val infiniteTransition = rememberInfiniteTransition(label = "wave")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.2831855f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    val amplifiedAmp = (amplitude * 5f).coerceIn(0f, 1f)

    val barTargets = remember(spectrum, amplifiedAmp, barCount, phase) {
        FloatArray(barCount) { index ->
            val sineWave = (0.5f + 0.5f * sin(phase + index * 0.7f))
            if (spectrum.isNotEmpty() && spectrum.any { it > 0.01f }) {
                val rawSpec = spectrum[index % spectrum.size]
                maxOf(rawSpec * 1.5f, amplifiedAmp * 0.8f, sineWave * 0.25f)
            } else if (amplifiedAmp > 0.01f) {
                maxOf(amplifiedAmp, sineWave * 0.5f)
            } else {
                0.2f + 0.15f * sineWave
            }
        }
    }

    val animatedValues = barTargets.mapIndexed { index, target ->
        animateFloatAsState(
            targetValue = target,
            animationSpec = tween(durationMillis = 50),
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
            val scale = animatedValues[index].value
            val barHeight = maxHeight * scale.coerceIn(0.1f, 0.98f) * heightScale
            val totalWidth = barCount * barWidth + (barCount - 1) * spacing
            val startX = (size.width - totalWidth) / 2f
            val x = startX + index * (barWidth + spacing)
            val y = (maxHeight - barHeight) / 2f
            val barAlpha = 0.95f

            drawRoundRect(
                color = colors[index % colors.size].copy(alpha = barAlpha),
                topLeft = Offset(x, y),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
            )
        }
    }
}
