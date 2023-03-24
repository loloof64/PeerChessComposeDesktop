package screens

import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import i18n.LocalStrings
import i18n.Strings
import kotlinx.coroutines.*
import logic.ChessGameManager
import logic.PreferencesManager
import java.awt.KeyboardFocusManager
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter


@Composable
fun rememberSaveableGamePageClockState(
    coroutineScope: CoroutineScope = rememberCoroutineScope(),
): GamePageClockState {
    return rememberSaveable(coroutineScope) {
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
     * startClock.
     * stoppingGameCallback : what to do whenever a stop game by timeout request is emitted.
     * The parameter is a boolean indicating if white has lost, or if it is black that lost the game.
     */
    fun startClock(whiteStarts: Boolean, stoppingGameCallback: (Boolean) -> Unit) {
        whiteTimeActive = whiteStarts
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

    suspend fun stopClock() {
        clockJob?.cancel()
        clockJob?.join()
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

@Composable
fun rememberSaveableGamePageLogicState(
    coroutineScope: CoroutineScope = rememberCoroutineScope(),
): GamePageLogicState {
    val strings = LocalStrings.current
    return rememberSaveable(coroutineScope) {
        GamePageLogicState(coroutineScope, strings)
    }
}

@Stable
class GamePageLogicState(
    private val coroutineScope: CoroutineScope,
    private val strings: Strings,
) {
    var purposeStartGameDialogOpen by mutableStateOf(false)
    var purposeStopGameDialogOpen by mutableStateOf(false)
    var confirmExitGameDialogOpen by mutableStateOf(false)
    var boardReversed by mutableStateOf(false)
    var gameInProgress by mutableStateOf(ChessGameManager.isGameInProgress())
    var boardPieces by mutableStateOf(ChessGameManager.getPieces())
    var isWhiteTurn by mutableStateOf(ChessGameManager.isWhiteTurn())
    var lastMoveArrow by mutableStateOf(ChessGameManager.getLastMoveArrow())
    var pendingPromotion by mutableStateOf(ChessGameManager.getPendingPromotion())
    var pendingPromotionStartSquare by mutableStateOf(ChessGameManager.getPendingPromotionStartSquare())
    var pendingPromotionEndSquare by mutableStateOf(ChessGameManager.getPendingPromotionEndSquare())
    var historyElements by mutableStateOf(ChessGameManager.getHistoryElements())
    var selectedHistoryNodeIndex by mutableStateOf(ChessGameManager.getSelectedHistoryNodeIndex())
    var whitePlayerType by mutableStateOf(ChessGameManager.getWhitePlayerType())
    var blackPlayerType by mutableStateOf(ChessGameManager.getBlackPlayerType())

    fun stopGameByTimeout(whiteTimeout: Boolean, notifyUser: (String) -> Unit) {
        if (ChessGameManager.checkIfPlayerWinningOnTimeIsMissingMaterialAndUpdatePgnResultTag()) {
            ChessGameManager.stopGame()
            gameInProgress = ChessGameManager.isGameInProgress()
            selectedHistoryNodeIndex = ChessGameManager.getSelectedHistoryNodeIndex()
            whitePlayerType = ChessGameManager.getWhitePlayerType()
            blackPlayerType = ChessGameManager.getBlackPlayerType()

            val message = strings.drawOnTimeByInsufficientMaterial
            notifyUser(message)
        } else {
            ChessGameManager.stopGame()
            gameInProgress = ChessGameManager.isGameInProgress()
            selectedHistoryNodeIndex = ChessGameManager.getSelectedHistoryNodeIndex()
            whitePlayerType = ChessGameManager.getWhitePlayerType()
            blackPlayerType = ChessGameManager.getBlackPlayerType()

            val message = if (whiteTimeout) strings.blackWonOnTime else strings.whiteWonOnTime
            notifyUser(message)
        }
    }

    fun onCheckmate(whitePlayer: Boolean, notifyUser: (String) -> Unit) {
        whitePlayerType = ChessGameManager.getWhitePlayerType()
        blackPlayerType = ChessGameManager.getBlackPlayerType()
        coroutineScope.launch {
            val message = if (whitePlayer) strings.whiteWonGame else strings.blackWonGame
            notifyUser(message)
        }
    }

    fun onStalemate(notifyUser: (String) -> Unit) {
        whitePlayerType = ChessGameManager.getWhitePlayerType()
        blackPlayerType = ChessGameManager.getBlackPlayerType()
        notifyUser(
            strings.drawByStalemate
        )
    }

    fun onThreeFoldRepetition(notifyUser: (String) -> Unit) {
        whitePlayerType = ChessGameManager.getWhitePlayerType()
        blackPlayerType = ChessGameManager.getBlackPlayerType()
        notifyUser(
            strings.drawByThreeFoldRepetition
        )
    }

    fun onInsufficientMaterial(notifyUser: (String) -> Unit) {
        whitePlayerType = ChessGameManager.getWhitePlayerType()
        blackPlayerType = ChessGameManager.getBlackPlayerType()
        notifyUser(
            strings.drawByInsufficientMaterial
        )
    }

    fun onFiftyMovesRuleDraw(notifyUser: (String) -> Unit) {
        whitePlayerType = ChessGameManager.getWhitePlayerType()
        blackPlayerType = ChessGameManager.getBlackPlayerType()
        notifyUser(
            strings.drawByFiftyMovesRule
        )
    }

    fun purposeStartNewGame() {
        if (!ChessGameManager.isGameInProgress()) {
            purposeStartGameDialogOpen = true
        }
    }

    fun updateGameStatus() {
        isWhiteTurn = ChessGameManager.isWhiteTurn()
        boardPieces = ChessGameManager.getPieces()
        pendingPromotion = ChessGameManager.getPendingPromotion()
        pendingPromotionStartSquare = ChessGameManager.getPendingPromotionStartSquare()
        pendingPromotionEndSquare = ChessGameManager.getPendingPromotionEndSquare()
        lastMoveArrow = ChessGameManager.getLastMoveArrow()
        gameInProgress = ChessGameManager.isGameInProgress()
        whitePlayerType = ChessGameManager.getWhitePlayerType()
        blackPlayerType = ChessGameManager.getBlackPlayerType()
        historyElements = ChessGameManager.getHistoryElements()
        selectedHistoryNodeIndex = ChessGameManager.getSelectedHistoryNodeIndex()
    }

    fun purposeStopGame() {
        if (ChessGameManager.isGameInProgress()) {
            purposeStopGameDialogOpen = true
        }
    }

    fun stopGame(shouldShowSnackBarMessage: Boolean, notifyUser: (String) -> Unit) {
        ChessGameManager.stopGame()
        gameInProgress = ChessGameManager.isGameInProgress()
        selectedHistoryNodeIndex = ChessGameManager.getSelectedHistoryNodeIndex()
        whitePlayerType = ChessGameManager.getWhitePlayerType()
        blackPlayerType = ChessGameManager.getBlackPlayerType()
        if (shouldShowSnackBarMessage) {
            notifyUser(
                strings.gameAborted
            )
        }
    }

    fun onPromotionCancelled() {
        pendingPromotion = ChessGameManager.getPendingPromotion()
        pendingPromotionStartSquare = ChessGameManager.getPendingPromotionStartSquare()
        pendingPromotionEndSquare = ChessGameManager.getPendingPromotionEndSquare()
    }

    fun purposeSaveGameInPgnFile(notifyUser: (String) -> Unit) {
        if (gameInProgress) return
        val folder = PreferencesManager.loadSavePgnFolder()
        val fileChooser = if (folder.isNotEmpty()) JFileChooser(folder) else JFileChooser()
        fileChooser.dialogTitle = strings.selectSavePgnPathDialogTitle
        fileChooser.approveButtonText = strings.validate

        val pgnFilter = FileNameExtensionFilter(strings.pgnFileType, "pgn")
        val currentWindow = KeyboardFocusManager.getCurrentKeyboardFocusManager().activeWindow
        fileChooser.addChoosableFileFilter(pgnFilter)
        fileChooser.isAcceptAllFileFilterUsed = true
        val actionResult = fileChooser.showSaveDialog(currentWindow)
        if (actionResult == JFileChooser.APPROVE_OPTION) {
            PreferencesManager.saveSavePgnFolder(fileChooser.currentDirectory.absolutePath)
            val selectedFile = fileChooser.selectedFile
            try {
                ChessGameManager.exportAsPgn(selectedFile)
                notifyUser(
                    strings.pgnExported
                )
            } catch (ex: Exception) {
                println(ex)
                notifyUser(
                    strings.failedSavingPgnFile
                )
            }
        }
    }
}