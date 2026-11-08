package com.yuyan.imemodule.ui.keyboard

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
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
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yuyan.imemodule.R
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
    accentKeyBackgroundColor: Color = Color.Unspecified,
    keyPressHighlightColor: Color = Color.Unspecified,
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

    val normalAccentBgColor = if (accentKeyBackgroundColor != Color.Unspecified) accentKeyBackgroundColor else Color(0xFF6366F1)
    val activePressedBgColor = if (keyPressHighlightColor != Color.Unspecified && keyPressHighlightColor != Color.Transparent) {
        keyPressHighlightColor
    } else {
        Color(
            red = (normalAccentBgColor.red * 0.6f),
            green = (normalAccentBgColor.green * 0.6f),
            blue = (normalAccentBgColor.blue * 0.6f),
            alpha = normalAccentBgColor.alpha
        )
    }

    var accumulativeDrag by remember { mutableFloatStateOf(0f) }
    val dragThresholdPx = with(LocalDensity.current) { 16.dp.toPx() }

    val coroutineScope = rememberCoroutineScope()
    var isDeletePressed by remember { mutableStateOf(false) }
    var isVoicePressed by remember { mutableStateOf(false) }

    LaunchedEffect(isVoicePressed) {
        if (isVoicePressed) {
            onStartVoice()
        } else {
            onStopVoice()
        }
    }

    val isRecordingActive = isVoicePressed || 
                            bottomActive || 
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
        // 第一行（上）：上矩形框（1号框：非输入空白，输入展示彩色波形） + 右上角 Backspace 删除键
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 上矩形框（1号框）：非输入状态下空白显示，输入状态下展示动态彩色波形图
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
                if (isRecordingActive) {
                    AudioSpectrumAnimation(
                        modifier = Modifier.fillMaxSize(),
                        isActive = true,
                        amplitude = amplitude,
                        spectrum = spectrum
                    )
                }
            }

            // 右上：退格删除按键（对齐中文9键：长按连续删除、上滑清空）
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(if (isDeletePressed || leftActive) activePressedBgColor else circleButtonBgColor)
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
                Icon(
                    painter = painterResource(id = R.drawable.ic_backspace),
                    contentDescription = "删除",
                    tint = if (isDeletePressed || leftActive) Color.White else buttonIconColor,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        // 第二行（下）：下矩形框（2号框：按住说话按键） + 右下角 Send 消息发送键
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 下矩形框（2号框）：核心语音长按按键
            val voiceBtnBg = if (isRecordingActive) activePressedBgColor else normalAccentBgColor
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(32.dp))
                    .background(voiceBtnBg)
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            awaitFirstDown(requireUnconsumed = false)
                            isVoicePressed = true
                            do {
                                val event = awaitPointerEvent()
                                val up = event.changes.all { !it.pressed }
                                if (up) break
                            } while (true)
                            isVoicePressed = false
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

            // 右下：消息发送按键（精美矢量向上箭头图标）
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(if (rightActive) activePressedBgColor else circleButtonBgColor)
                    .clickable { onSendClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_send_arrow),
                    contentDescription = "发送",
                    tint = if (rightActive) Color.White else buttonIconColor,
                    modifier = Modifier.size(24.dp)
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
    barWidthFactor: Float = 1.9f,
    barCount: Int = 11,
    spacingRatio: Float = 0.5f,
    heightScale: Float = 0.85f,
) {
    if (!isActive) {
        Canvas(modifier = modifier) {}
        return
    }

    val infiniteTransition = rememberInfiniteTransition(label = "wavePhase")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.2831855f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    val colors = remember {
        listOf(
            Color(0xFFFF7070),
            Color(0xFFFFA07A),
            Color(0xFFFFCF7D),
            Color(0xFFFFF0A5),
            Color(0xFF7BE4B7),
            Color(0xFF7BE4D8),
            Color(0xFF7BC3FF),
            Color(0xFF9D7BFF),
            Color(0xFFFF7BE8),
            Color(0xFFFF8A8A),
            Color(0xFFFFB8A8),
        )
    }

    val ampClamped = amplitude.coerceIn(0f, 1f)

    Canvas(modifier = modifier) {
        val barWidth = size.width / (barCount * barWidthFactor)
        val spacing = barWidth * spacingRatio
        val maxHeight = size.height

        repeat(barCount) { index ->
            val sineWave = 0.35f + 0.35f * sin(phase + index * 0.6f)
            val specVal = if (spectrum.isNotEmpty()) spectrum[index % spectrum.size] else 0f
            val audioScale = maxOf(specVal * 1.5f, ampClamped * 4.0f)
            val finalScale = (sineWave + audioScale).coerceIn(0.25f, 0.95f)

            val barHeight = maxHeight * finalScale * heightScale
            val totalWidth = barCount * barWidth + (barCount - 1) * spacing
            val startX = (size.width - totalWidth) / 2f
            val x = startX + index * (barWidth + spacing)
            val y = (maxHeight - barHeight) / 2f

            drawRoundRect(
                color = colors[index % colors.size],
                topLeft = Offset(x, y),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
            )
        }
    }
}
