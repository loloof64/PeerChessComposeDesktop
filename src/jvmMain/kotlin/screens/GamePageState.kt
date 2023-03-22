package screens

import androidx.compose.runtime.*
import kotlinx.coroutines.*


@Composable
fun rememberGamePageClockState(
    coroutineScope: CoroutineScope = rememberCoroutineScope(),
) : GamePageClockState {
 return remember(coroutineScope) {
     GamePageClockState(coroutineScope)
 }
}
@Stable
class GamePageClockState(
    private val coroutineScope: CoroutineScope,
) {
    var whiteTimeInDeciSeconds by mutableStateOf(0)
    var blackTimeInDeciSeconds by mutableStateOf(0)
    var whiteIncrementInSeconds by mutableStateOf(0)
    var blackIncrementInSeconds by mutableStateOf(0)
    var whiteTimeActive by mutableStateOf(true)
    var clockSelected by mutableStateOf(false)
    var clockActive by mutableStateOf(false)
    var whiteAllocatedTimeInDeciSeconds by mutableStateOf(600)
    var blackAllocatedTimeInDeciSeconds by mutableStateOf(0)
    var differentialClockActive by mutableStateOf(false)
    var clockJob by mutableStateOf<Job?>(null)

    /**
     * stoppingGameCallback : what to do whenever a stop game by timeout request is emitted.
     * The parameter is a boolean indicating if white has lost, or if it is black that lost the game.
     */
    fun startClock(stoppingGameCallback: (Boolean) -> Unit) {
        clockJob = coroutineScope.launch {
            while (isActive) {
                delay(100)
                if (whiteTimeActive) {
                    whiteTimeInDeciSeconds--
                    if (whiteTimeInDeciSeconds <= 0) {
                        stoppingGameCallback(true)
                    }
                } else {
                    blackTimeInDeciSeconds--
                    if (blackTimeInDeciSeconds <= 0) {
                        stoppingGameCallback(false)
                    }
                }
            }
        }
    }

    fun updateClockValue() {
        whiteTimeInDeciSeconds = whiteAllocatedTimeInDeciSeconds + 10 * whiteIncrementInSeconds
        blackTimeInDeciSeconds =
            if (differentialClockActive) blackAllocatedTimeInDeciSeconds + 10 * blackIncrementInSeconds else whiteTimeInDeciSeconds
        blackIncrementInSeconds = if (differentialClockActive) blackIncrementInSeconds else whiteIncrementInSeconds
    }

    fun handleDifferentialClockActiveChange(newState: Boolean) {
        differentialClockActive = newState
    }

    fun handleClockSelectedChange(newState: Boolean) {
        clockSelected = newState
    }

    fun updateClockTimeBasedOnIncrement() {
        whiteTimeActive = !whiteTimeActive
        if (whiteTimeActive) {
            blackTimeInDeciSeconds += blackIncrementInSeconds * 10
        } else {
            whiteTimeInDeciSeconds += whiteIncrementInSeconds * 10
        }
    }

    fun updateAllocatedHours(newHoursCount: Int, black: Boolean = false) {
        val currentAllocatedMinutes =
            minutesFor(if (black) blackAllocatedTimeInDeciSeconds else whiteAllocatedTimeInDeciSeconds)
        val currentAllocatedSeconds =
            secondsFor(if (black) blackAllocatedTimeInDeciSeconds else whiteAllocatedTimeInDeciSeconds)
        val newValue = newHoursCount * 3600_0 + currentAllocatedMinutes * 60_0 + currentAllocatedSeconds * 10
        if (black) blackAllocatedTimeInDeciSeconds = newValue
        else whiteAllocatedTimeInDeciSeconds = newValue
    }

    fun updateAllocatedMinutes(newMinutesCount: Int, black: Boolean = false) {
        val currentAllocatedHours =
            hoursFor(if (black) blackAllocatedTimeInDeciSeconds else whiteAllocatedTimeInDeciSeconds)
        val currentAllocatedSeconds =
            secondsFor(if (black) blackAllocatedTimeInDeciSeconds else whiteAllocatedTimeInDeciSeconds)
        val newValue = currentAllocatedHours * 3600_0 + newMinutesCount * 60_0 + currentAllocatedSeconds * 10
        if (black) blackAllocatedTimeInDeciSeconds = newValue
        else whiteAllocatedTimeInDeciSeconds = newValue
    }

    fun updateAllocatedSeconds(newSecondsCount: Int, black: Boolean = false) {
        val currentAllocatedHours =
            hoursFor(if (black) blackAllocatedTimeInDeciSeconds else whiteAllocatedTimeInDeciSeconds)
        val currentAllocatedMinutes =
            minutesFor(if (black) blackAllocatedTimeInDeciSeconds else whiteAllocatedTimeInDeciSeconds)
        val newValue = currentAllocatedHours * 3600_0 + currentAllocatedMinutes * 60_0 + newSecondsCount * 10
        if (black) blackAllocatedTimeInDeciSeconds = newValue
        else whiteAllocatedTimeInDeciSeconds = newValue
    }

    fun hoursFor(timeInDeciSeconds: Int): Int {
        val timeSeconds = timeInDeciSeconds / 10
        return timeSeconds / 3600
    }

    fun minutesFor(timeInDeciSeconds: Int): Int {
        val timeSeconds = timeInDeciSeconds / 10
        var result = timeSeconds % 3600
        result /= 60
        return result
    }

    fun secondsFor(timeInDeciSeconds: Int): Int {
        val timeSeconds = timeInDeciSeconds / 10
        var result = timeSeconds % 3600
        result %= 60
        return result
    }
}