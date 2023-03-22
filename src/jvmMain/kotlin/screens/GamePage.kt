package screens

import VerticalNumberPicker
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
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
import java.text.SimpleDateFormat
import java.util.*

suspend fun stopGameByTimeout(
    whiteTimeout: Boolean,
    clockState: GamePageClockState,
    gameLogicState: GamePageLogicState
) {
    clockState.clockJob?.cancel()
    clockState.clockJob?.join()
    clockState.clockJob = null
    clockState.clockActive = false

    gameLogicState.stopGameByTimeout(whiteTimeout)
}

fun stopGame(
    shouldShowSnackBarMessage: Boolean = true,
    clockState: GamePageClockState,
    gameLogicState: GamePageLogicState,
    coroutineScope: CoroutineScope,
) {
    coroutineScope.launch {
        clockState.stopClock()
    }
    gameLogicState.stopGame(shouldShowSnackBarMessage)
}

fun onCheckmate(
    whitePlayer: Boolean,
    clockState: GamePageClockState,
    gameLogicState: GamePageLogicState,
    coroutineScope: CoroutineScope
) {
    coroutineScope.launch {
        clockState.stopClock()
    }
    gameLogicState.onCheckmate(whitePlayer)
}

fun onStalemate(clockState: GamePageClockState, gameLogicState: GamePageLogicState, coroutineScope: CoroutineScope) {
    coroutineScope.launch {
        clockState.stopClock()
    }
    gameLogicState.onStalemate()
}

fun onThreeFoldRepetition(
    clockState: GamePageClockState,
    gameLogicState: GamePageLogicState,
    coroutineScope: CoroutineScope
) {
    coroutineScope.launch {
        clockState.stopClock()
    }
    gameLogicState.onThreeFoldRepetition()
}

fun onInsufficientMaterial(
    clockState: GamePageClockState,
    gameLogicState: GamePageLogicState,
    coroutineScope: CoroutineScope
) {
    coroutineScope.launch {
        clockState.stopClock()
    }
    gameLogicState.onInsufficientMaterial()
}

fun onFiftyMovesRuleDraw(
    clockState: GamePageClockState,
    gameLogicState: GamePageLogicState,
    coroutineScope: CoroutineScope
) {
    coroutineScope.launch {
        clockState.stopClock()
    }
    gameLogicState.onFiftyMovesRuleDraw()
}

fun startNewGame(
    clockState: GamePageClockState,
    gameLogicState: GamePageLogicState,
    navigation: StackNavigation<Screen>, coroutineScope: CoroutineScope,
) {
    navigation.push(Screen.EditPosition {
        gameLogicState.updateGameStatus()
        if (clockState.clockSelected) {
            clockState.updateClockValue()
            clockState.startClock { whiteIsLooserSide ->
                coroutineScope.launch { stopGameByTimeout(whiteIsLooserSide, clockState, gameLogicState) }
            }
        }
    })
}

fun onMovePlayed(isPendingPromotionMove: Boolean, clockState: GamePageClockState, gameLogicState: GamePageLogicState) {
    if (!isPendingPromotionMove) {
        clockState.updateClockTimeBasedOnIncrement()
    }
    gameLogicState.updateGameStatus()
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun GamePage(
    navigation: StackNavigation<Screen>,
    coroutineScope: CoroutineScope = rememberCoroutineScope(),
) {
    val scaffoldState = rememberScaffoldState()
    val strings = LocalStrings.current
    val clockState = rememberSaveableGamePageClockState(coroutineScope = coroutineScope)
    val gameLogicState = rememberSaveableGamePageLogicState(coroutineScope = coroutineScope)

    BoxWithConstraints {
        val isLandscape = maxWidth > maxHeight
        Scaffold(scaffoldState = scaffoldState, topBar = {
            TopAppBar(title = { Text(strings.gamePageTitle) }, actions = {
                if (!gameLogicState.gameInProgress) {
                    IconButton(content = {
                        Image(
                            painter = painterResource("images/material_vectors/directions_run.svg"),
                            contentDescription = strings.newGame,
                            modifier = Modifier.size(30.dp),
                            colorFilter = ColorFilter.tint(Color.White)
                        )
                    }, onClick = {
                        gameLogicState.purposeStartNewGame()
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
                    gameLogicState.boardReversed = !gameLogicState.boardReversed
                })
                if (gameLogicState.gameInProgress) {
                    IconButton({ gameLogicState.purposeStopGame() }) {
                        Image(
                            painter = painterResource("images/material_vectors/cancel.svg"),
                            contentDescription = strings.stopGame,
                            modifier = Modifier.size(30.dp),
                            colorFilter = ColorFilter.tint(Color.White)
                        )
                    }
                }

                if (!gameLogicState.gameInProgress) {
                    IconButton({
                        gameLogicState.purposeSaveGameInPgnFile()
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
                            if (gameLogicState.gameInProgress) 1.0f else 0.6f
                        Row(
                            modifier = Modifier.fillMaxWidth().fillMaxHeight(heightRatio),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ChessBoardComponent(
                                isWhiteTurn = gameLogicState.isWhiteTurn,
                                piecesValues = gameLogicState.boardPieces,
                                reversed = gameLogicState.boardReversed,
                                whitePlayerType = gameLogicState.whitePlayerType,
                                blackPlayerType = gameLogicState.blackPlayerType,
                                lastMoveArrow = gameLogicState.lastMoveArrow,
                                pendingPromotion = gameLogicState.pendingPromotion,
                                pendingPromotionStartSquare = gameLogicState.pendingPromotionStartSquare,
                                pendingPromotionEndSquare = gameLogicState.pendingPromotionEndSquare,
                                gameInProgress = gameLogicState.gameInProgress,
                                onCheckmate = { onCheckmate(it, clockState, gameLogicState, coroutineScope) },
                                onStalemate = { onStalemate(clockState, gameLogicState, coroutineScope) },
                                onFiftyMovesRuleDraw = {
                                    onFiftyMovesRuleDraw(
                                        clockState,
                                        gameLogicState,
                                        coroutineScope
                                    )
                                },
                                onThreeFoldRepetition = {
                                    onThreeFoldRepetition(
                                        clockState,
                                        gameLogicState,
                                        coroutineScope
                                    )
                                },
                                onInsufficientMaterial = {
                                    onInsufficientMaterial(
                                        clockState,
                                        gameLogicState,
                                        coroutineScope
                                    )
                                },
                                onMovePlayed = { onMovePlayed(it, clockState, gameLogicState) },
                                onPromotionCancelled = { gameLogicState.onPromotionCancelled() },
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
                                    historyElements = gameLogicState.historyElements,
                                    selectedHistoryNodeIndex = gameLogicState.selectedHistoryNodeIndex,
                                    onPositionSelected = {
                                        gameLogicState.updateGameStatus()
                                    },
                                )
                            }
                        }
                    } else {
                        val heightRatio =
                            if (gameLogicState.gameInProgress) 1.0f else 0.55f
                        Column(
                            modifier = Modifier.fillMaxWidth().fillMaxHeight(heightRatio),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Column(modifier = Modifier.fillMaxHeight(0.6f)) {
                                ChessBoardComponent(
                                    isWhiteTurn = gameLogicState.isWhiteTurn,
                                    piecesValues = gameLogicState.boardPieces,
                                    reversed = gameLogicState.boardReversed,
                                    whitePlayerType = gameLogicState.whitePlayerType,
                                    blackPlayerType = gameLogicState.blackPlayerType,
                                    lastMoveArrow = gameLogicState.lastMoveArrow,
                                    pendingPromotion = gameLogicState.pendingPromotion,
                                    pendingPromotionStartSquare = gameLogicState.pendingPromotionStartSquare,
                                    pendingPromotionEndSquare = gameLogicState.pendingPromotionEndSquare,
                                    gameInProgress = gameLogicState.gameInProgress,
                                    onCheckmate = { onCheckmate(it, clockState, gameLogicState, coroutineScope) },
                                    onStalemate = { onStalemate(clockState, gameLogicState, coroutineScope) },
                                    onFiftyMovesRuleDraw = {
                                        onFiftyMovesRuleDraw(
                                            clockState,
                                            gameLogicState,
                                            coroutineScope
                                        )
                                    },
                                    onThreeFoldRepetition = {
                                        onThreeFoldRepetition(
                                            clockState,
                                            gameLogicState,
                                            coroutineScope
                                        )
                                    },
                                    onInsufficientMaterial = {
                                        onInsufficientMaterial(
                                            clockState,
                                            gameLogicState,
                                            coroutineScope
                                        )
                                    },
                                    onMovePlayed = { onMovePlayed(it, clockState, gameLogicState) },
                                    onPromotionCancelled = { gameLogicState.onPromotionCancelled() },
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
                                    historyElements = gameLogicState.historyElements,
                                    selectedHistoryNodeIndex = gameLogicState.selectedHistoryNodeIndex,
                                    onPositionSelected = {
                                        gameLogicState.updateGameStatus()
                                    },
                                )
                            }
                        }
                    }

                }


                if (!gameLogicState.gameInProgress) {

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
                        VerticalNumberPicker(
                            value = clockState.whiteIncrementInSeconds,
                            range = 0..59,
                            onStateChanged = {
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
                            VerticalNumberPicker(
                                value = clockState.blackIncrementInSeconds,
                                range = 0..59,
                                onStateChanged = {
                                    clockState.blackIncrementInSeconds = it
                                    clockState.updateClockValue()
                                })
                        }
                    }
                }
            }
        }

        if (gameLogicState.purposeStartGameDialogOpen) {
            AlertDialog(onDismissRequest = {
                gameLogicState.purposeStartGameDialogOpen = false
            }, title = {
                Text(strings.confirmStartNewGameTitle)
            }, text = {
                Text(strings.confirmStartNewGameMessage)
            }, confirmButton = {
                Button({
                    gameLogicState.purposeStartGameDialogOpen = false
                    startNewGame(clockState, gameLogicState, navigation, coroutineScope)
                }) {
                    Text(strings.validate)
                }
            }, dismissButton = {
                Button({
                    gameLogicState.purposeStartGameDialogOpen = false
                }) {
                    Text(strings.cancel)
                }
            })
        }

        if (gameLogicState.purposeStopGameDialogOpen) {
            AlertDialog(onDismissRequest = {
                gameLogicState.purposeStopGameDialogOpen = false
            }, title = {
                Text(strings.purposeStopGameTitle)
            }, text = {
                Text(strings.purposeStopGameMessage)
            }, confirmButton = {
                Button({
                    gameLogicState.purposeStopGameDialogOpen = false
                    stopGame(shouldShowSnackBarMessage = true, clockState, gameLogicState, coroutineScope)
                }) {
                    Text(strings.validate)
                }
            }, dismissButton = {
                Button({
                    gameLogicState.purposeStopGameDialogOpen = false
                }) {
                    Text(strings.cancel)
                }
            })
        }

        if (gameLogicState.confirmExitGameDialogOpen) {
            AlertDialog(onDismissRequest = {
                gameLogicState.confirmExitGameDialogOpen = false
            }, title = {
                Text(strings.confirmExitGameTitle)
            }, text = {
                Text(strings.confirmExitGameMessage)
            }, confirmButton = {
                Button({
                    gameLogicState.confirmExitGameDialogOpen = false
                    gameLogicState.stopGame(shouldShowSnackBarMessage = false)
                    navigation.pop()
                }) {
                    Text(strings.validate)
                }
            }, dismissButton = {
                Button({
                    gameLogicState.confirmExitGameDialogOpen = false
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