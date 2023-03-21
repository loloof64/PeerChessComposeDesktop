// taken from https://gist.github.com/vganin/a9a84653a9f48a2d669910fbd48e32d5
// adapted by loloof64

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import i18n.LocalStrings
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun HorizontalNumberPicker(
    value: Int,
    modifier: Modifier = Modifier,
    range: IntRange? = null,
    textStyle: TextStyle = LocalTextStyle.current,
    onStateChanged: (Int) -> Unit = {},
) {
    var animatedValue by mutableStateOf(value)
    val coroutineScope = rememberCoroutineScope()
    val strings = LocalStrings.current
    val numbersColumnWidth = 36.dp
    val halvedNumbersColumnWidth = numbersColumnWidth / 2
    val halvedNumbersColumnWidthPx = with(LocalDensity.current) { halvedNumbersColumnWidth.toPx() }

    fun animatedStateValue(offset: Float): Int = animatedValue - (offset / halvedNumbersColumnWidthPx).toInt()

    val animatedOffset = remember { Animatable(0f) }.apply {
        if (range != null) {
            val offsetRange = remember(animatedValue, range) {
                val last = -(range.first - animatedValue) * halvedNumbersColumnWidthPx
                val first = -(range.last - animatedValue) * halvedNumbersColumnWidthPx
                first..last
            }
            updateBounds(offsetRange.start, offsetRange.endInclusive)
        }
    }
    val coercedAnimatedOffset = animatedOffset.value % halvedNumbersColumnWidthPx
    val animatedStateValue = animatedStateValue(animatedOffset.value)

    fun updateValueOneStep(upward: Boolean) {
        val newValue = if (upward) value + 1 else value - 1
        if (range != null) {
            if (range.contains(newValue)) {
                onStateChanged(newValue)
            }
        } else {
            onStateChanged(newValue)
        }
    }

    Row(modifier = modifier
        .wrapContentSize()
        .draggable(
            orientation = Orientation.Horizontal,
            state = rememberDraggableState { deltaX ->
                coroutineScope.launch {
                    animatedOffset.snapTo(animatedOffset.value - deltaX)
                }
            },
            onDragStopped = { velocity ->
                coroutineScope.launch {
                    val endValue = animatedOffset.fling(
                        initialVelocity = velocity,
                        animationSpec = exponentialDecay(frictionMultiplier = 20f),
                        adjustTarget = { target ->
                            val coercedTarget = target % halvedNumbersColumnWidthPx
                            val coercedAnchors = listOf(-halvedNumbersColumnWidthPx, 0f, halvedNumbersColumnWidthPx)
                            val coercedPoint = coercedAnchors.minByOrNull { abs(it - coercedTarget) }!!
                            val base = halvedNumbersColumnWidthPx * (target / halvedNumbersColumnWidthPx).toInt()
                            coercedPoint + base
                        }
                    ).endState.value

                    animatedValue = animatedStateValue(endValue)
                    onStateChanged(animatedValue)
                    animatedOffset.snapTo(0f)
                }
            }
        )) {
        val spacing = 4.dp

        val arrowColor = MaterialTheme.colors.onSecondary.copy(alpha = ContentAlpha.disabled)

        IconButton(
            modifier = Modifier.padding(end = spacing),
            onClick = {
                updateValueOneStep(true)
            }) {
            Image(
                painter = painterResource("images/material_vectors/arrow_left.svg"),
                contentDescription = strings.goUp,
                colorFilter = ColorFilter.tint(arrowColor)
            )
        }


        Box(
            modifier = Modifier
                .align(Alignment.CenterVertically)
                .offset { IntOffset(x = 0, y = coercedAnimatedOffset.roundToInt()) }
        ) {
            val baseLabelModifier = Modifier.align(Alignment.Center)
            ProvideTextStyle(textStyle) {
                Label(
                    text = (animatedStateValue - 1).toString(),
                    modifier = baseLabelModifier
                        .offset(y = -halvedNumbersColumnWidth)
                        .alpha(coercedAnimatedOffset / halvedNumbersColumnWidthPx)
                )
                Label(
                    text = animatedStateValue.toString(),
                    modifier = baseLabelModifier
                        .alpha(1 - abs(coercedAnimatedOffset) / halvedNumbersColumnWidthPx)
                )
                Label(
                    text = (animatedStateValue + 1).toString(),
                    modifier = baseLabelModifier
                        .offset(y = halvedNumbersColumnWidth)
                        .alpha(-coercedAnimatedOffset / halvedNumbersColumnWidthPx)
                )
            }
        }

        IconButton(
            modifier = Modifier.padding(end = spacing), onClick = {
                updateValueOneStep(false)
            }) {
            Image(
                painter = painterResource("images/material_vectors/arrow_right.svg"),
                contentDescription = strings.goDown,
                colorFilter = ColorFilter.tint(arrowColor)
            )
        }

    }
}

@Composable
fun VerticalNumberPicker(
    value: Int,
    modifier: Modifier = Modifier,
    range: IntRange? = null,
    textStyle: TextStyle = LocalTextStyle.current,
    onStateChanged: (Int) -> Unit = {},
) {
    var animatedValue by mutableStateOf(value)
    val coroutineScope = rememberCoroutineScope()
    val strings = LocalStrings.current
    val numbersColumnWidth = 36.dp
    val halvedNumbersColumnWidth = numbersColumnWidth / 2
    val halvedNumbersColumnWidthPx = with(LocalDensity.current) { halvedNumbersColumnWidth.toPx() }

    fun animatedStateValue(offset: Float): Int = animatedValue - (offset / halvedNumbersColumnWidthPx).toInt()

    val animatedOffset = remember { Animatable(0f) }.apply {
        if (range != null) {
            val offsetRange = remember(animatedValue, range) {
                val last = -(range.first - animatedValue) * halvedNumbersColumnWidthPx
                val first = -(range.last - animatedValue) * halvedNumbersColumnWidthPx
                first..last
            }
            updateBounds(offsetRange.start, offsetRange.endInclusive)
        }
    }
    val coercedAnimatedOffset = animatedOffset.value % halvedNumbersColumnWidthPx
    val animatedStateValue = animatedStateValue(animatedOffset.value)

    fun updateValueOneStep(upward: Boolean) {
        val newValue = if (upward) value + 1 else value - 1
        if (range != null) {
            if (range.contains(newValue)) {
                onStateChanged(newValue)
            }
        } else {
            onStateChanged(newValue)
        }
    }

    Column(modifier = modifier
        .wrapContentSize()
        .draggable(
            orientation = Orientation.Vertical,
            state = rememberDraggableState { deltaY ->
                coroutineScope.launch {
                    animatedOffset.snapTo(animatedOffset.value + deltaY)
                }
            },
            onDragStopped = { velocity ->
                coroutineScope.launch {
                    val endValue = animatedOffset.fling(
                        initialVelocity = velocity,
                        animationSpec = exponentialDecay(frictionMultiplier = 20f),
                        adjustTarget = { target ->
                            val coercedTarget = target % halvedNumbersColumnWidthPx
                            val coercedAnchors = listOf(-halvedNumbersColumnWidthPx, 0f, halvedNumbersColumnWidthPx)
                            val coercedPoint = coercedAnchors.minByOrNull { abs(it - coercedTarget) }!!
                            val base = halvedNumbersColumnWidthPx * (target / halvedNumbersColumnWidthPx).toInt()
                            coercedPoint + base
                        }
                    ).endState.value

                    animatedValue = animatedStateValue(endValue)
                    onStateChanged(animatedValue)
                    animatedOffset.snapTo(0f)
                }
            }
        )) {
        val spacing = 2.dp

        val arrowColor = MaterialTheme.colors.onSecondary.copy(alpha = ContentAlpha.disabled)

        IconButton(
            modifier = Modifier.padding(end = spacing),
            onClick = {
                updateValueOneStep(true)
            }) {
            Image(
                modifier = Modifier.size(20.dp),
                painter = painterResource("images/material_vectors/arrow_upward.svg"),
                contentDescription = strings.goUp,
                colorFilter = ColorFilter.tint(arrowColor)
            )
        }


        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .offset { IntOffset(x = 0, y = coercedAnimatedOffset.roundToInt()) }
        ) {
            val baseLabelModifier = Modifier.align(Alignment.Center)
            ProvideTextStyle(textStyle) {
                Label(
                    text = (animatedStateValue - 1).toString(),
                    modifier = baseLabelModifier
                        .offset(y = -halvedNumbersColumnWidth)
                        .alpha(coercedAnimatedOffset / halvedNumbersColumnWidthPx)
                )
                Label(
                    text = animatedStateValue.toString(),
                    modifier = baseLabelModifier
                        .alpha(1 - abs(coercedAnimatedOffset) / halvedNumbersColumnWidthPx)
                )
                Label(
                    text = (animatedStateValue + 1).toString(),
                    modifier = baseLabelModifier
                        .offset(y = halvedNumbersColumnWidth)
                        .alpha(-coercedAnimatedOffset / halvedNumbersColumnWidthPx)
                )
            }
        }

        IconButton(
            modifier = Modifier.padding(end = spacing), onClick = {
                updateValueOneStep(false)
            }) {
            Image(
                modifier = Modifier.size(20.dp),
                painter = painterResource("images/material_vectors/arrow_downward.svg"),
                contentDescription = strings.goDown,
                colorFilter = ColorFilter.tint(arrowColor)
            )
        }

    }
}

@Composable
private fun Label(text: String, modifier: Modifier) {
    Text(
        text = text,
        modifier = modifier.pointerInput(Unit) {
            detectTapGestures(onLongPress = {
                // FIXME: Empty to disable text selection
            })
        }
    )
}

private suspend fun Animatable<Float, AnimationVector1D>.fling(
    initialVelocity: Float,
    animationSpec: DecayAnimationSpec<Float>,
    adjustTarget: ((Float) -> Float)?,
    block: (Animatable<Float, AnimationVector1D>.() -> Unit)? = null,
): AnimationResult<Float, AnimationVector1D> {
    val targetValue = animationSpec.calculateTargetValue(value, initialVelocity)
    val adjustedTarget = adjustTarget?.invoke(targetValue)

    return if (adjustedTarget != null) {
        animateTo(
            targetValue = adjustedTarget,
            initialVelocity = initialVelocity,
            block = block
        )
    } else {
        animateDecay(
            initialVelocity = initialVelocity,
            animationSpec = animationSpec,
            block = block,
        )
    }
}