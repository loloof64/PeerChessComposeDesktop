package screens

import VerticalNumberPicker
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.router.stack.push
import components.*
import i18n.LocalStrings
import io.github.wolfraam.chessgame.board.Square
import kotlinx.coroutines.*
import logic.ChessGameManager
import logic.PreferencesManager
import java.awt.KeyboardFocusManager
import java.text.SimpleDateFormat
import java.util.*
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun GamePage(
    navigation: StackNavigation<Screen>,
    clockState: GamePageClockState = rememberGamePageClockState()
) {
    val scaffoldState = rememberScaffoldState()
    val coroutineScope = rememberCoroutineScope()

    val strings = LocalStrings.current
    var purposeStartGameDialogOpen by rememberSaveable { mutableStateOf(false) }
    var purposeStopGameDialogOpen by rememberSaveable { mutableStateOf(false) }
    var confirmExitGameDialogOpen by rememberSaveable { mutableStateOf(false) }
    var boardReversed by rememberSaveable { mutableStateOf(false) }
    var gameInProgress by rememberSaveable { mutableStateOf(ChessGameManager.isGameInProgress()) }
    var boardPieces by rememberSaveable { mutableStateOf(ChessGameManager.getPieces()) }
    var isWhiteTurn by rememberSaveable { mutableStateOf(ChessGameManager.isWhiteTurn()) }
    var lastMoveArrow by rememberSaveable { mutableStateOf(ChessGameManager.getLastMoveArrow()) }
    var pendingPromotion by rememberSaveable { mutableStateOf(ChessGameManager.getPendingPromotion()) }
    var pendingPromotionStartSquare by rememberSaveable { mutableStateOf(ChessGameManager.getPendingPromotionStartSquare()) }
    var pendingPromotionEndSquare by rememberSaveable { mutableStateOf(ChessGameManager.getPendingPromotionEndSquare()) }
    var historyElements by rememberSaveable { mutableStateOf(ChessGameManager.getHistoryElements()) }
    var selectedHistoryNodeIndex by rememberSaveable { mutableStateOf(ChessGameManager.getSelectedHistoryNodeIndex()) }
    var whitePlayerType by rememberSaveable { mutableStateOf(ChessGameManager.getWhitePlayerType()) }
    var blackPlayerType by rememberSaveable { mutableStateOf(ChessGameManager.getBlackPlayerType()) }


    suspend fun stopGameByTimeout(whiteTimeout: Boolean) {
        clockState.clockJob?.cancel()
        clockState.clockJob?.join()
        clockState.clockJob = null
        clockState.clockActive = false

        if (ChessGameManager.checkIfPlayerWinningOnTimeIsMissingMaterialAndUpdatePgnResultTag()) {
            ChessGameManager.stopGame()
            gameInProgress = ChessGameManager.isGameInProgress()
            selectedHistoryNodeIndex = ChessGameManager.getSelectedHistoryNodeIndex()
            whitePlayerType = ChessGameManager.getWhitePlayerType()
            blackPlayerType = ChessGameManager.getBlackPlayerType()

            val message = strings.drawOnTimeByInsufficientMaterial
            coroutineScope.launch {
                scaffoldState.snackbarHostState.showSnackbar(
                    message, actionLabel = strings.close, duration = SnackbarDuration.Long
                )
            }
        } else {
            ChessGameManager.stopGame()
            gameInProgress = ChessGameManager.isGameInProgress()
            selectedHistoryNodeIndex = ChessGameManager.getSelectedHistoryNodeIndex()
            whitePlayerType = ChessGameManager.getWhitePlayerType()
            blackPlayerType = ChessGameManager.getBlackPlayerType()

            val message = if (whiteTimeout) strings.blackWonOnTime else strings.whiteWonOnTime
            coroutineScope.launch {
                scaffoldState.snackbarHostState.showSnackbar(
                    message, actionLabel = strings.close, duration = SnackbarDuration.Long
                )
            }
        }
    }

    fun onCheckmate(whitePlayer: Boolean) {
        clockState.handleClockSelectedChange(false)

        whitePlayerType = ChessGameManager.getWhitePlayerType()
        blackPlayerType = ChessGameManager.getBlackPlayerType()
        coroutineScope.launch {
            scaffoldState.snackbarHostState.showSnackbar(
                if (whitePlayer) strings.whiteWonGame else strings.blackWonGame,
                actionLabel = strings.close,
                duration = SnackbarDuration.Long
            )
        }
    }

    fun onStalemate() {
        clockState.handleClockSelectedChange(false)

        whitePlayerType = ChessGameManager.getWhitePlayerType()
        blackPlayerType = ChessGameManager.getBlackPlayerType()
        coroutineScope.launch {
            scaffoldState.snackbarHostState.showSnackbar(
                strings.drawByStalemate, actionLabel = strings.close, duration = SnackbarDuration.Long
            )
        }
    }

    fun onThreeFoldRepetition() {
        clockState.handleClockSelectedChange(false)

        whitePlayerType = ChessGameManager.getWhitePlayerType()
        blackPlayerType = ChessGameManager.getBlackPlayerType()
        coroutineScope.launch {
            scaffoldState.snackbarHostState.showSnackbar(
                strings.drawByThreeFoldRepetition, actionLabel = strings.close, duration = SnackbarDuration.Long
            )
        }
    }

    fun onInsufficientMaterial() {
        clockState.handleClockSelectedChange(false)

        whitePlayerType = ChessGameManager.getWhitePlayerType()
        blackPlayerType = ChessGameManager.getBlackPlayerType()
        coroutineScope.launch {
            scaffoldState.snackbarHostState.showSnackbar(
                strings.drawByInsufficientMaterial, actionLabel = strings.close, duration = SnackbarDuration.Long
            )
        }
    }

    fun onFiftyMovesRuleDraw() {
        clockState.handleClockSelectedChange(false)

        whitePlayerType = ChessGameManager.getWhitePlayerType()
        blackPlayerType = ChessGameManager.getBlackPlayerType()
        coroutineScope.launch {
            scaffoldState.snackbarHostState.showSnackbar(
                strings.drawByFiftyMovesRule, actionLabel = strings.close, duration = SnackbarDuration.Long
            )
        }
    }

    fun purposeStartNewGame() {
        if (!ChessGameManager.isGameInProgress()) {
            purposeStartGameDialogOpen = true
        }
    }

    fun updateGameStatus() {
        boardPieces = ChessGameManager.getPieces()
        isWhiteTurn = ChessGameManager.isWhiteTurn()
        gameInProgress = ChessGameManager.isGameInProgress()
        whitePlayerType = ChessGameManager.getWhitePlayerType()
        blackPlayerType = ChessGameManager.getBlackPlayerType()
        lastMoveArrow = ChessGameManager.getLastMoveArrow()
        historyElements = ChessGameManager.getHistoryElements()
        selectedHistoryNodeIndex = ChessGameManager.getSelectedHistoryNodeIndex()
    }

    fun startNewGame() {
        navigation.push(Screen.EditPosition {
            updateGameStatus()
            if (clockState.clockSelected) {
                clockState.updateClockValue()
                clockState.startClock { whiteIsLooserSide ->
                    coroutineScope.launch { stopGameByTimeout(whiteIsLooserSide) }
                }
            }
        })
    }

    fun purposeStopGame() {
        if (ChessGameManager.isGameInProgress()) {
            purposeStopGameDialogOpen = true
        }
    }

    fun stopGame(shouldShowSnackBarMessage: Boolean = true) {
        clockState.handleClockSelectedChange(false)

        ChessGameManager.stopGame()
        gameInProgress = ChessGameManager.isGameInProgress()
        selectedHistoryNodeIndex = ChessGameManager.getSelectedHistoryNodeIndex()
        whitePlayerType = ChessGameManager.getWhitePlayerType()
        blackPlayerType = ChessGameManager.getBlackPlayerType()
        if (shouldShowSnackBarMessage) {
            coroutineScope.launch {
                scaffoldState.snackbarHostState.showSnackbar(
                    strings.gameAborted, actionLabel = strings.close, duration = SnackbarDuration.Long
                )
            }
        }
    }

    fun onMovePlayed(isPendingPromotionMove: Boolean) {
        if (!isPendingPromotionMove) {
            clockState.updateClockTimeBasedOnIncrement()
        }
        isWhiteTurn = ChessGameManager.isWhiteTurn()
        boardPieces = ChessGameManager.getPieces()
        pendingPromotion = ChessGameManager.getPendingPromotion()
        pendingPromotionStartSquare = ChessGameManager.getPendingPromotionStartSquare()
        pendingPromotionEndSquare = ChessGameManager.getPendingPromotionEndSquare()
        lastMoveArrow = ChessGameManager.getLastMoveArrow()
        gameInProgress = ChessGameManager.isGameInProgress()
        whitePlayerType = ChessGameManager.getWhitePlayerType()
        blackPlayerType = ChessGameManager.getBlackPlayerType()
        selectedHistoryNodeIndex = ChessGameManager.getSelectedHistoryNodeIndex()
    }

    fun onPromotionCancelled() {
        pendingPromotion = ChessGameManager.getPendingPromotion()
        pendingPromotionStartSquare = ChessGameManager.getPendingPromotionStartSquare()
        pendingPromotionEndSquare = ChessGameManager.getPendingPromotionEndSquare()
    }

    fun purposeSaveGameInPgnFile() {
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
                coroutineScope.launch {
                    scaffoldState.snackbarHostState.showSnackbar(
                        message = strings.pgnExported,
                        actionLabel = strings.close,
                        duration = SnackbarDuration.Long,
                    )
                }
            } catch (ex: Exception) {
                println(ex)
                coroutineScope.launch {
                    scaffoldState.snackbarHostState.showSnackbar(
                        message = strings.failedSavingPgnFile,
                        actionLabel = strings.close,
                        duration = SnackbarDuration.Long,
                    )
                }
            }
        }
    }

    BoxWithConstraints {
        val isLandscape = maxWidth > maxHeight
        Scaffold(scaffoldState = scaffoldState, topBar = {
            TopAppBar(title = { Text(strings.gamePageTitle) }, actions = {
                if (!gameInProgress) {
                    IconButton(content = {
                        Image(
                            painter = painterResource("images/material_vectors/directions_run.svg"),
                            contentDescription = strings.newGame,
                            modifier = Modifier.size(30.dp),
                            colorFilter = ColorFilter.tint(Color.White)
                        )
                    }, onClick = {
                        purposeStartNewGame()
                    })
                }
                IconButton(content = {
                    Image(
                        painter = painterResource("images/material_vectors/swap_vert.svg"),
                        contentDescription = strings.swapBoardOrientation,
                        modifier = Modifier.size(30.dp),
                        colorFilter = ColorFilter.tint(Color.White)
                    )
                }, onClick = {
                    boardReversed = !boardReversed
                })
                if (gameInProgress) {
                    IconButton(::purposeStopGame) {
                        Image(
                            painter = painterResource("images/material_vectors/cancel.svg"),
                            contentDescription = strings.stopGame,
                            modifier = Modifier.size(30.dp),
                            colorFilter = ColorFilter.tint(Color.White)
                        )
                    }
                }

                if (!gameInProgress) {
                    IconButton({
                        purposeSaveGameInPgnFile()
                    }) {
                        Image(
                            painter = painterResource("images/material_vectors/save.svg"),
                            contentDescription = strings.saveGameInPgn,
                            modifier = Modifier.size(30.dp),
                            colorFilter = ColorFilter.tint(Color.White)
                        )
                    }
                }
            })
        }) {

            Column(
                verticalArrangement = Arrangement.Top
            ) {
                Box {
                    if (isLandscape) {
                        val heightRatio =
                            if (gameInProgress) 1.0f else 0.6f
                        Row(
                            modifier = Modifier.fillMaxWidth().fillMaxHeight(heightRatio),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ChessBoardComponent(
                                isWhiteTurn = isWhiteTurn,
                                piecesValues = boardPieces,
                                reversed = boardReversed,
                                whitePlayerType = whitePlayerType,
                                blackPlayerType = blackPlayerType,
                                lastMoveArrow = lastMoveArrow,
                                pendingPromotion = pendingPromotion,
                                pendingPromotionStartSquare = pendingPromotionStartSquare,
                                pendingPromotionEndSquare = pendingPromotionEndSquare,
                                gameInProgress = gameInProgress,
                                onCheckmate = ::onCheckmate,
                                onStalemate = ::onStalemate,
                                onFiftyMovesRuleDraw = ::onFiftyMovesRuleDraw,
                                onThreeFoldRepetition = ::onThreeFoldRepetition,
                                onInsufficientMaterial = ::onInsufficientMaterial,
                                onMovePlayed = ::onMovePlayed,
                                onPromotionCancelled = ::onPromotionCancelled,
                            )

                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Top,
                            ) {
                                if (clockState.clockSelected) {
                                    ClockComponent(
                                        whiteTimeInDeciSeconds = clockState.whiteTimeInDeciSeconds,
                                        blackTimeInDeciSeconds = clockState.blackTimeInDeciSeconds,
                                        whiteTimeActive = clockState.whiteTimeActive,
                                    )
                                }
                                HistoryComponent(
                                    historyElements = historyElements,
                                    selectedHistoryNodeIndex = selectedHistoryNodeIndex,
                                    onPositionSelected = {
                                        isWhiteTurn = ChessGameManager.isWhiteTurn()
                                        boardPieces = ChessGameManager.getPieces()
                                        lastMoveArrow = ChessGameManager.getLastMoveArrow()
                                        selectedHistoryNodeIndex = ChessGameManager.getSelectedHistoryNodeIndex()
                                    },
                                )
                            }
                        }
                    } else {
                        val heightRatio =
                            if (gameInProgress) 1.0f else 0.55f
                        Column(
                            modifier = Modifier.fillMaxWidth().fillMaxHeight(heightRatio),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Column(modifier = Modifier.fillMaxHeight(0.6f)) {
                                ChessBoardComponent(
                                    isWhiteTurn = isWhiteTurn,
                                    piecesValues = boardPieces,
                                    reversed = boardReversed,
                                    whitePlayerType = whitePlayerType,
                                    blackPlayerType = blackPlayerType,
                                    lastMoveArrow = lastMoveArrow,
                                    pendingPromotion = pendingPromotion,
                                    pendingPromotionStartSquare = pendingPromotionStartSquare,
                                    pendingPromotionEndSquare = pendingPromotionEndSquare,
                                    gameInProgress = gameInProgress,
                                    onCheckmate = ::onCheckmate,
                                    onStalemate = ::onStalemate,
                                    onFiftyMovesRuleDraw = ::onFiftyMovesRuleDraw,
                                    onThreeFoldRepetition = ::onThreeFoldRepetition,
                                    onInsufficientMaterial = ::onInsufficientMaterial,
                                    onMovePlayed = ::onMovePlayed,
                                    onPromotionCancelled = ::onPromotionCancelled,
                                )
                            }

                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Top,
                            ) {
                                if (clockState.clockSelected) {
                                    ClockComponent(
                                        whiteTimeInDeciSeconds = clockState.whiteTimeInDeciSeconds,
                                        blackTimeInDeciSeconds = clockState.blackTimeInDeciSeconds,
                                        whiteTimeActive = clockState.whiteTimeActive,
                                    )
                                }
                                HistoryComponent(
                                    historyElements = historyElements,
                                    selectedHistoryNodeIndex = selectedHistoryNodeIndex,
                                    onPositionSelected = {
                                        isWhiteTurn = ChessGameManager.isWhiteTurn()
                                        boardPieces = ChessGameManager.getPieces()
                                        lastMoveArrow = ChessGameManager.getLastMoveArrow()
                                        selectedHistoryNodeIndex = ChessGameManager.getSelectedHistoryNodeIndex()
                                    },
                                )
                            }
                        }
                    }

                }


                if (!gameInProgress) {

                    // base clock options

                    Row(
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(
                            checked = clockState.clockSelected,
                            onCheckedChange = {
                                clockState.handleClockSelectedChange(it)
                                clockState.updateClockValue()
                            },
                        )
                        Text(strings.timedGame)

                        VerticalNumberPicker(value = clockState.hoursFor(clockState.whiteAllocatedTimeInDeciSeconds),
                            range = 0..3,
                            onStateChanged = {
                                clockState.updateAllocatedHours(it)
                                clockState.updateClockValue()
                            })
                        Text("h")

                        VerticalNumberPicker(value = clockState.minutesFor(clockState.whiteAllocatedTimeInDeciSeconds),
                            range = 0..59,
                            onStateChanged = {
                                clockState.updateAllocatedMinutes(it)
                                clockState.updateClockValue()
                            })
                        Text("m")

                        VerticalNumberPicker(value = clockState.secondsFor(clockState.whiteAllocatedTimeInDeciSeconds),
                            range = 0..59,
                            onStateChanged = {
                                clockState.updateAllocatedSeconds(it)
                                clockState.updateClockValue()
                            })
                        Text("s")

                        Text(strings.timeIncrement, modifier = Modifier.padding(start = 10.dp))
                        VerticalNumberPicker(value = clockState.whiteIncrementInSeconds, range = 0..59, onStateChanged = {
                            clockState.whiteIncrementInSeconds = it
                            clockState.updateClockValue()
                        })
                    }

                    // differential clock options

                    if (clockState.clockSelected) {
                        Row(
                            horizontalArrangement = Arrangement.Start,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(
                                checked = clockState.differentialClockActive,
                                onCheckedChange = {
                                    clockState.handleDifferentialClockActiveChange(it)
                                    clockState.updateClockValue()
                                },
                            )
                            Text(strings.differentTimeForBlack)

                            VerticalNumberPicker(value = clockState.hoursFor(clockState.blackAllocatedTimeInDeciSeconds),
                                range = 0..3,
                                onStateChanged = {
                                    clockState.updateAllocatedHours(it, black = true)
                                    clockState.updateClockValue()
                                })
                            Text("h")

                            VerticalNumberPicker(value = clockState.minutesFor(clockState.blackAllocatedTimeInDeciSeconds),
                                range = 0..59,
                                onStateChanged = {
                                    clockState.updateAllocatedMinutes(it, black = true)
                                    clockState.updateClockValue()
                                })
                            Text("m")

                            VerticalNumberPicker(value = clockState.secondsFor(clockState.blackAllocatedTimeInDeciSeconds),
                                range = 0..59,
                                onStateChanged = {
                                    clockState.updateAllocatedSeconds(it, black = true)
                                    clockState.updateClockValue()
                                })
                            Text("s")

                            Text(strings.timeIncrement, modifier = Modifier.padding(start = 10.dp))
                            VerticalNumberPicker(value = clockState.blackIncrementInSeconds, range = 0..59, onStateChanged = {
                                clockState.blackIncrementInSeconds = it
                                clockState.updateClockValue()
                            })
                        }
                    }
                }
            }
        }

        if (purposeStartGameDialogOpen) {
            AlertDialog(onDismissRequest = {
                purposeStartGameDialogOpen = false
            }, title = {
                Text(strings.confirmStartNewGameTitle)
            }, text = {
                Text(strings.confirmStartNewGameMessage)
            }, confirmButton = {
                Button({
                    purposeStartGameDialogOpen = false
                    startNewGame()
                }) {
                    Text(strings.validate)
                }
            }, dismissButton = {
                Button({
                    purposeStartGameDialogOpen = false
                }) {
                    Text(strings.cancel)
                }
            })
        }

        if (purposeStopGameDialogOpen) {
            AlertDialog(onDismissRequest = {
                purposeStopGameDialogOpen = false
            }, title = {
                Text(strings.purposeStopGameTitle)
            }, text = {
                Text(strings.purposeStopGameMessage)
            }, confirmButton = {
                Button({
                    purposeStopGameDialogOpen = false
                    stopGame()
                }) {
                    Text(strings.validate)
                }
            }, dismissButton = {
                Button({
                    purposeStopGameDialogOpen = false
                }) {
                    Text(strings.cancel)
                }
            })
        }

        if (confirmExitGameDialogOpen) {
            AlertDialog(onDismissRequest = {
                confirmExitGameDialogOpen = false
            }, title = {
                Text(strings.confirmExitGameTitle)
            }, text = {
                Text(strings.confirmExitGameMessage)
            }, confirmButton = {
                Button({
                    confirmExitGameDialogOpen = false
                    stopGame(shouldShowSnackBarMessage = false)
                    navigation.pop()
                }) {
                    Text(strings.validate)
                }
            }, dismissButton = {
                Button({
                    confirmExitGameDialogOpen = false
                }) {
                    Text(strings.cancel)
                }
            })
        }
    }
}

@Composable
private fun ChessBoardComponent(
    gameInProgress: Boolean,
    isWhiteTurn: Boolean,
    reversed: Boolean,
    piecesValues: List<List<Char>>,
    whitePlayerType: PlayerType,
    blackPlayerType: PlayerType,
    lastMoveArrow: LastMoveArrow?,
    pendingPromotion: PendingPromotion,
    pendingPromotionStartSquare: Square?,
    pendingPromotionEndSquare: Square?,
    onCheckmate: (Boolean) -> Unit,
    onStalemate: () -> Unit,
    onThreeFoldRepetition: () -> Unit,
    onInsufficientMaterial: () -> Unit,
    onFiftyMovesRuleDraw: () -> Unit,
    onMovePlayed: (Boolean) -> Unit,
    onPromotionCancelled: () -> Unit,
) {
    ChessBoard(isWhiteTurn = isWhiteTurn,
        piecesValues = piecesValues,
        reversed = reversed,
        whitePlayerType = whitePlayerType,
        blackPlayerType = blackPlayerType,
        lastMoveArrow = lastMoveArrow,
        pendingPromotion = pendingPromotion,
        pendingPromotionStartFile = pendingPromotionStartSquare?.x,
        pendingPromotionStartRank = pendingPromotionStartSquare?.y,
        pendingPromotionEndFile = pendingPromotionEndSquare?.x,
        pendingPromotionEndRank = pendingPromotionEndSquare?.y,
        tryPlayingMove = { dragAndDropData ->
            if (!gameInProgress) return@ChessBoard
            val moveResultInfo = ChessGameManager.playMove(
                startFile = dragAndDropData.startFile,
                startRank = dragAndDropData.startRank,
                endFile = dragAndDropData.endFile,
                endRank = dragAndDropData.endRank,
                onCheckmate = onCheckmate,
                onStalemate = onStalemate,
                onThreeFoldsRepetition = onThreeFoldRepetition,
                onInsufficientMaterial = onInsufficientMaterial,
                onFiftyMovesRuleDraw = onFiftyMovesRuleDraw,
            )
            if (moveResultInfo.isLegal) onMovePlayed(moveResultInfo.isPendingPromotion)
        },
        onCancelPromotion = {
            if (!gameInProgress) return@ChessBoard
            ChessGameManager.cancelPromotion()
            onPromotionCancelled()
        },
        onValidatePromotion = {
            if (!gameInProgress) return@ChessBoard
            ChessGameManager.commitPromotion(
                pieceType = it,
                onCheckmate = onCheckmate,
                onStalemate = onStalemate,
                onThreeFoldsRepetition = onThreeFoldRepetition,
                onInsufficientMaterial = onInsufficientMaterial,
                onFiftyMovesRuleDraw = onFiftyMovesRuleDraw,
            )
            onMovePlayed(false)
        })
}

@Composable
fun HistoryComponent(
    historyElements: List<ChessHistoryItem>,
    selectedHistoryNodeIndex: Int?,
    onPositionSelected: () -> Unit,
) {
    ChessHistory(items = historyElements,
        selectedNodeIndex = selectedHistoryNodeIndex,
        onPositionRequest = { positionFen, moveCoordinates, nodeToSelectIndex ->
            val success = ChessGameManager.requestPosition(
                positionFen = positionFen, moveCoordinates = moveCoordinates, nodeToSelectIndex
            )
            if (success) {
                onPositionSelected()
            }
        },
        onRequestBackOneMove = {
            val success = ChessGameManager.requestGotoPreviousHistoryNode()
            if (success) {
                onPositionSelected()
            }
        },
        onRequestForwardOneMove = {
            val success = ChessGameManager.requestGotoNextHistoryNode()
            if (success) {
                onPositionSelected()
            }
        },
        onRequestGotoFirstPosition = {
            val success = ChessGameManager.requestGotoFirstPosition()
            if (success) {
                onPositionSelected()
            }
        },
        onRequestGotoLastMove = {
            val success = ChessGameManager.requestGotoLastHistoryNode()
            if (success) {
                onPositionSelected()
            }
        })
}

private fun getTimeText(timeInDeciSeconds: Int): String {
    val pattern = if (timeInDeciSeconds >= 36000) "HH:mm:ss"
    else if (timeInDeciSeconds >= 600) "mm:ss"
    else "ss.S"
    val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
    val simpleDateFormat = SimpleDateFormat(pattern, Locale.US)
    simpleDateFormat.timeZone = cal.timeZone
    cal.timeInMillis = (timeInDeciSeconds * 100).toLong()
    return simpleDateFormat.format(cal.time)
}

@Composable
fun ClockComponent(
    modifier: Modifier = Modifier,
    whiteTimeInDeciSeconds: Int,
    blackTimeInDeciSeconds: Int,
    whiteTimeActive: Boolean,
) {
    val whiteText = getTimeText(whiteTimeInDeciSeconds)
    val blackText = getTimeText(blackTimeInDeciSeconds)

    var whiteZoneFgColor = Color.Black
    var blackZoneFgColor = Color.White

    var whiteZoneBgColor = Color.White
    var blackZoneBgColor = Color.Black

    if (whiteTimeActive) {
        if (whiteTimeInDeciSeconds < 600) {
            whiteZoneBgColor = Color.Red
            whiteZoneFgColor = Color.White
        } else {
            whiteZoneBgColor = Color.Blue
            whiteZoneFgColor = Color.Yellow
        }
    } else {
        if (blackTimeInDeciSeconds < 600) {
            blackZoneBgColor = Color.Red
            blackZoneFgColor = Color.White
        } else {
            blackZoneBgColor = Color.Blue
            blackZoneFgColor = Color.Yellow
        }
    }

    Row(
        modifier = modifier.fillMaxWidth().fillMaxHeight(0.1f).border(width = 1.dp, color = Color.Black)
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(0.5f).fillMaxHeight().background(whiteZoneBgColor),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = whiteText,
                color = whiteZoneFgColor,
                textAlign = TextAlign.Center,
            )
        }
        Box(
            modifier = Modifier.fillMaxWidth().fillMaxHeight().background(blackZoneBgColor),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = blackText,
                color = blackZoneFgColor,
                textAlign = TextAlign.Center,
            )
        }
    }
}