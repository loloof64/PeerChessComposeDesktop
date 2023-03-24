/*
    Compose chess experiment
    Copyright (C) 2022  Laurent Bernabe

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>.

 */
package components

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.arkivanov.essenty.parcelable.Parcelize
import i18n.LocalStrings
import i18n.Strings
import logic.defaultPosition
import kotlin.math.*

const val emptyCell = ' '

enum class PendingPromotion {
    None,
    White,
    Black
}

enum class PromotionType {
    Queen,
    Rook,
    Bishop,
    Knight,
}

enum class PlayerType {
    Human,
    Computer,
    None,
}

data class LastMoveArrow(
    val startFile: Int,
    val startRank: Int,
    val endFile: Int,
    val endRank: Int,
)

@Composable
fun ChessBoard(
    piecesValues: List<List<Char>>,
    isWhiteTurn: Boolean,
    reversed: Boolean,
    whitePlayerType: PlayerType,
    blackPlayerType: PlayerType,
    dragAndDropData: ChessBoardDragAndDropData?,
    lastMoveArrow: LastMoveArrow?,
    pendingPromotion: PendingPromotion,
    pendingPromotionStartFile: Int?,
    pendingPromotionStartRank: Int?,
    pendingPromotionEndFile: Int?,
    pendingPromotionEndRank: Int?,
    tryPlayingMove: (ChessBoardDragAndDropData) -> Unit,
    onCancelPromotion: () -> Unit,
    onValidatePromotion: (PromotionType) -> Unit,
    onCellClick: (Int, Int) -> Unit = { _, _ -> },
    onDragAndDropDataUpdate: (ChessBoardDragAndDropData?) -> Unit,
    isEditable: Boolean = false,
) {
    val bgColor = Color(0xFF9999FF)
    BoxWithConstraints {
        val heightBasedAspectRatio = maxHeight > maxWidth
        val minAvailableSide = if (maxWidth < maxHeight) maxWidth else maxHeight
        val cellSize = minAvailableSide * 0.11f

        val cellSizePx = with(LocalDensity.current) {
            cellSize.toPx()
        }

        Box(
            modifier = Modifier.aspectRatio(1f, heightBasedAspectRatio).background(bgColor)
        ) {
            LowerLayer(
                cellSize = cellSize,
                reversed = reversed,
                piecesValues = piecesValues,
                isWhiteTurn = isWhiteTurn,
                dndData = dragAndDropData,
                pendingPromotionStartFile = pendingPromotionStartFile,
                pendingPromotionStartRank = pendingPromotionStartRank,
                pendingPromotionEndFile = pendingPromotionEndFile,
                pendingPromotionEndRank = pendingPromotionEndRank,
            )
            LastMoveArrowLayer(
                cellSize = cellSize,
                reversed = reversed,
                lastMoveArrow = lastMoveArrow,
            )
            DragAndDropLayer(
                outsideDragAndDropData = dragAndDropData,
                cellSizePx = cellSizePx,
                reversed = reversed,
                isActive = pendingPromotion == PendingPromotion.None,
                piecesValues = piecesValues,
                isWhiteTurn = isWhiteTurn,
                whitePlayerType = whitePlayerType,
                blackPlayerType = blackPlayerType,
                tryPlayingMove = tryPlayingMove,
                onDndDataUpdate = { onDragAndDropDataUpdate(it) },
                onCellClick = onCellClick,
                isEditable = isEditable,
            )
            if (pendingPromotion != PendingPromotion.None) {
                PromotionLayer(
                    cellSize = cellSize,
                    forWhitePlayer = pendingPromotion == PendingPromotion.White,
                    onCancelPromotion = onCancelPromotion,
                    onValidatePromotion = onValidatePromotion,
                )
            }
        }
    }
}

@Composable
private fun LastMoveArrowLayer(
    cellSize: Dp,
    reversed: Boolean,
    lastMoveArrow: LastMoveArrow?,
) {
    val cellsSizePx = with(LocalDensity.current) {
        cellSize.toPx()
    }

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .alpha(0.8f)
    ) {
        if (lastMoveArrow != null) {
            val arrowWidth = cellsSizePx * 0.1f
            val brush = Brush.linearGradient(colors = listOf(Color.Blue, Color.Magenta, Color.Red))
            val start = Offset(
                cellsSizePx * (if (reversed) 8.0f - lastMoveArrow.startFile else 1.0f + lastMoveArrow.startFile),
                cellsSizePx * (if (reversed) 1.0f + lastMoveArrow.startRank else 8.0f - lastMoveArrow.startRank)
            )
            val end = Offset(
                cellsSizePx * (if (reversed) 8.0f - lastMoveArrow.endFile else 1.0f + lastMoveArrow.endFile),
                cellsSizePx * (if (reversed) 1.0f + lastMoveArrow.endRank else 8.0f - lastMoveArrow.endRank)
            )

            val deltaX = end.x - start.x
            val deltaY = end.y - start.y

            val angle = atan2(deltaY, deltaX)
            val headLen = cellsSizePx * 0.5f
            val arrowPointLeft = Offset(
                end.x - headLen * cos(angle - Math.PI / 6).toFloat(),
                end.y - headLen * sin(angle - Math.PI / 6).toFloat()
            )
            val arrowPointRight = Offset(
                end.x - headLen * cos(angle + Math.PI / 6).toFloat(),
                end.y - headLen * sin(angle + Math.PI / 6).toFloat()
            )

            drawLine(brush = brush, start = start, end = end, strokeWidth = arrowWidth)
            drawLine(brush = brush, start = end, end = arrowPointLeft, strokeWidth = arrowWidth)
            drawLine(brush = brush, start = end, end = arrowPointRight, strokeWidth = arrowWidth)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PromotionLayer(
    cellSize: Dp,
    forWhitePlayer: Boolean,
    onCancelPromotion: () -> Unit,
    onValidatePromotion: (PromotionType) -> Unit,
) {
    val strings = LocalStrings.current
    Box(modifier = Modifier
        .fillMaxSize()
        .background(
            Color(0x88000000)
        )
        .onClick { onCancelPromotion() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth(0.885f)
                .fillMaxHeight(0.225f)
                .offset(x = cellSize * 0.5f, y = cellSize * 3.5f)
        ) {
            Image(
                painter = painterResource(getVectorForPiece(if (forWhitePlayer) 'Q' else 'q')),
                contentDescription = strings.queenPromotion,
                modifier = Modifier.size(cellSize).offset(x = cellSize * 0.5f, y = cellSize * 0.5f)
                    .onClick { onValidatePromotion(PromotionType.Queen) },
            )
            Image(
                painter = painterResource(getVectorForPiece(if (forWhitePlayer) 'R' else 'r')),
                contentDescription = strings.rookPromotion,
                modifier = Modifier.size(cellSize).offset(x = cellSize * 1.5f, y = cellSize * 0.5f)
                    .onClick { onValidatePromotion(PromotionType.Rook) },
            )
            Image(
                painter = painterResource(getVectorForPiece(if (forWhitePlayer) 'B' else 'b')),
                contentDescription = strings.bishopPromotion,
                modifier = Modifier.size(cellSize).offset(x = cellSize * 2.5f, y = cellSize * 0.5f)
                    .onClick { onValidatePromotion(PromotionType.Bishop) },
            )
            Image(
                painter = painterResource(getVectorForPiece(if (forWhitePlayer) 'N' else 'n')),
                contentDescription = strings.knightPromotion,
                modifier = Modifier.size(cellSize).offset(x = cellSize * 3.5f, y = cellSize * 0.5f)
                    .onClick { onValidatePromotion(PromotionType.Knight) },
            )
        }
    }
}

@Composable
private fun DragAndDropLayer(
    outsideDragAndDropData: ChessBoardDragAndDropData?,
    cellSizePx: Float,
    reversed: Boolean,
    isActive: Boolean,
    piecesValues: List<List<Char>>,
    isWhiteTurn: Boolean,
    whitePlayerType: PlayerType,
    blackPlayerType: PlayerType,
    onDndDataUpdate: (ChessBoardDragAndDropData?) -> Unit,
    tryPlayingMove: (ChessBoardDragAndDropData) -> Unit,
    onCellClick: (Int, Int) -> Unit,
    isEditable: Boolean,
) {
    val strings = LocalStrings.current
    var dndData by rememberSaveable { mutableStateOf<ChessBoardDragAndDropData?>(null) }
    Column(
        modifier = Modifier.fillMaxSize()
            .pointerInput(reversed, piecesValues, isWhiteTurn, whitePlayerType, blackPlayerType, cellSizePx) {
                if (isEditable) {
                    detectTapGestures(
                        onTap = { offset ->
                            val col = ((offset.x - cellSizePx * 0.5) / cellSizePx).toInt()
                            val row = ((offset.y - cellSizePx * 0.5) / cellSizePx).toInt()
                            val file = if (reversed) 7 - col else col
                            val rank = if (reversed) row else 7 - row
                            onCellClick(file, rank)
                        }
                    )
                } else {
                    detectDragGestures(
                        onDragStart = { offset: Offset ->
                            if (!isActive) return@detectDragGestures

                            val col = ((offset.x - cellSizePx * 0.5) / cellSizePx).toInt()
                            val row = ((offset.y - cellSizePx * 0.5) / cellSizePx).toInt()
                            val file = if (reversed) 7 - col else col
                            val rank = if (reversed) row else 7 - row

                            if (file < 0 || file > 7) return@detectDragGestures
                            if (rank < 0 || rank > 7) return@detectDragGestures


                            val piece = piecesValues[7 - rank][file]
                            if (piece == emptyCell) return@detectDragGestures

                            val isComputerTurn = (isWhiteTurn && whitePlayerType == PlayerType.Computer)
                                    || (!isWhiteTurn && blackPlayerType == PlayerType.Computer)
                            if (isComputerTurn) return@detectDragGestures

                            val isNobodyTurn = (isWhiteTurn && whitePlayerType == PlayerType.None)
                                    || (!isWhiteTurn && blackPlayerType == PlayerType.None)
                            if (isNobodyTurn) return@detectDragGestures

                            val isOurPiece = piece.isUpperCase() == isWhiteTurn
                            if (!isOurPiece) return@detectDragGestures

                            dndData =
                                ChessBoardDragAndDropData(
                                    startFile = file,
                                    startRank = rank,
                                    endFile = file,
                                    endRank = rank,
                                    carriedPiece = piece,
                                    startLocation = offset,
                                    currentLocation = offset,
                                )
                            onDndDataUpdate(dndData)

                        },
                        onDrag = { _, dragAmount ->
                            if (!isActive) return@detectDragGestures
                            if (dndData == null) return@detectDragGestures

                            val currentLocation = dndData!!.currentLocation + dragAmount

                            val col = ((currentLocation.x - cellSizePx * 0.5) / cellSizePx).toInt()
                            val row = ((currentLocation.y - cellSizePx * 0.5) / cellSizePx).toInt()
                            val file = if (reversed) 7 - col else col
                            val rank = if (reversed) row else 7 - row

                            dndData =
                                dndData!!.copy(
                                    endRank = rank,
                                    endFile = file,
                                    currentLocation = currentLocation,
                                )
                            onDndDataUpdate(dndData)

                        },
                        onDragEnd = {
                            if (!isActive) return@detectDragGestures
                            if (dndData == null) return@detectDragGestures

                            val startAndEndCellsAreDifferent =
                                (dndData!!.startFile != dndData!!.endFile) || (dndData!!.startRank != dndData!!.endRank)
                            if (startAndEndCellsAreDifferent) {
                                val inBounds =
                                    (0..7).contains(dndData!!.startFile) && (0..7).contains(dndData!!.startRank)
                                            && (0..7).contains(dndData!!.endFile) && (0..7).contains(dndData!!.endRank)
                                if (inBounds) {
                                    tryPlayingMove(dndData!!)
                                }
                            }
                            dndData = null
                            onDndDataUpdate(null)
                        },
                        onDragCancel = {
                            if (!isActive) return@detectDragGestures
                            if (dndData == null) return@detectDragGestures
                            dndData = null
                            onDndDataUpdate(null)
                        }
                    )
                }
            }) {
        if (dndData != null && outsideDragAndDropData != null) {
            val xDp = with(LocalDensity.current) {
                dndData!!.currentLocation.x.toDp()
            }
            val yDp = with(LocalDensity.current) {
                dndData!!.currentLocation.y.toDp()
            }
            Image(
                painter = painterResource(getVectorForPiece(dndData!!.carriedPiece)),
                contentDescription = strings.draggedPiece,
                modifier = Modifier.fillMaxSize(0.111f).offset(xDp, yDp),
            )
        }
    }
}

@Composable
private fun LowerLayer(
    cellSize: Dp,
    reversed: Boolean,
    piecesValues: List<List<Char>>,
    isWhiteTurn: Boolean,
    dndData: ChessBoardDragAndDropData?,
    pendingPromotionStartFile: Int?,
    pendingPromotionStartRank: Int?,
    pendingPromotionEndFile: Int?,
    pendingPromotionEndRank: Int?,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        ChessBoardHorizontalLabels(cellSize = cellSize, whiteTurn = null, reversed = reversed)
        (0..7).forEach { rowIndex ->
            val rank = if (reversed) rowIndex + 1 else 8 - rowIndex
            val rankLabel = "${Char('0'.code + rank)}"
            val firstIsWhite = rowIndex % 2 == 0
            val rowPiecesValues = piecesValues[if (reversed) 7 - rowIndex else rowIndex]
            ChessBoardCellsLine(
                cellSize = cellSize, firstCellWhite = firstIsWhite,
                rankLabel = rankLabel, rowPiecesValues = rowPiecesValues,
                reversed = reversed, dndData = dndData,
                rowIndex = rowIndex,
                pendingPromotionStartFile = pendingPromotionStartFile,
                pendingPromotionStartRank = pendingPromotionStartRank,
                pendingPromotionEndFile = pendingPromotionEndFile,
                pendingPromotionEndRank = pendingPromotionEndRank,
            )
        }
        ChessBoardHorizontalLabels(cellSize = cellSize, whiteTurn = isWhiteTurn, reversed = reversed)
    }
}

@Composable
private fun ChessBoardCellsLine(
    modifier: Modifier = Modifier,
    cellSize: Dp,
    firstCellWhite: Boolean,
    rankLabel: String,
    rowPiecesValues: List<Char>,
    reversed: Boolean,
    rowIndex: Int,
    dndData: ChessBoardDragAndDropData?,
    pendingPromotionStartFile: Int?,
    pendingPromotionStartRank: Int?,
    pendingPromotionEndFile: Int?,
    pendingPromotionEndRank: Int?,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ChessBoardVerticalLabel(text = rankLabel, cellSize = cellSize)
        (0..7).forEach { colIndex ->
            val file = if (reversed) 7 - colIndex else colIndex
            val rank = if (reversed) rowIndex else 7 - rowIndex

            val isDraggedPieceOrigin = if (dndData != null) {
                file == dndData.startFile && rank == dndData.startRank
            } else false

            val isIntoDragAndDropCrossLines = if (dndData != null) {
                file == dndData.endFile || rank == dndData.endRank
            } else false

            val isDragAndDropStartCell = if (dndData != null) {
                file == dndData.startFile && rank == dndData.startRank
            } else false

            val isDragAndDropEndCell = if (dndData != null) {
                file == dndData.endFile && rank == dndData.endRank
            } else false

            val isPendingPromotionStartCell =
                file == pendingPromotionStartFile && rank == pendingPromotionStartRank


            val isPendingPromotionEndCell =
                file == pendingPromotionEndFile && rank == pendingPromotionEndRank


            ChessBoardCell(
                isWhite = if ((colIndex % 2) == 0) firstCellWhite else !firstCellWhite,
                size = cellSize,
                pieceValue = rowPiecesValues[if (reversed) 7 - colIndex else colIndex],
                isDraggedPieceOrigin = isDraggedPieceOrigin,
                isIntoDragAndDropCrossLines = isIntoDragAndDropCrossLines,
                isDragAndDropStartCell = isDragAndDropStartCell,
                isDragAndDropEndCell = isDragAndDropEndCell,
                isPendingPromotionStartCell = isPendingPromotionStartCell,
                isPendingPromotionEndCell = isPendingPromotionEndCell,
            )
        }
        ChessBoardVerticalLabel(text = rankLabel, cellSize = cellSize)
    }
}

@Composable
private fun ChessBoardVerticalLabel(
    modifier: Modifier = Modifier,
    text: String,
    cellSize: Dp
) {
    val fontSize = with(LocalDensity.current) {
        (cellSize * 0.3f).toSp()
    }
    Column(
        modifier = modifier.width(cellSize / 2).height(cellSize / 2).background(Color.Transparent),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text, fontWeight = FontWeight.Bold, color = Color.Yellow, fontSize = fontSize)
    }
}

@Composable
private fun ChessBoardHorizontalLabels(
    modifier: Modifier = Modifier, cellSize: Dp, whiteTurn: Boolean?, reversed: Boolean
) {
    val fontSize = with(LocalDensity.current) {
        (cellSize * 0.3f).toSp()
    }
    Row(
        modifier = modifier.fillMaxWidth().height(cellSize / 2)
    ) {
        Row(
            modifier = Modifier.width(cellSize / 2).height(cellSize / 2)
        ) {
            Text(
                text = "", fontWeight = FontWeight.Bold, color = Color.Transparent, fontSize = fontSize
            )
        }
        (0..7).forEach {
            val col = if (reversed) 7 - it else it
            val colLabel = "${Char('A'.code + col)}"
            Row(
                modifier = Modifier.width(cellSize).height(cellSize / 2),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = colLabel,
                    fontWeight = FontWeight.Bold,
                    color = Color.Yellow,
                    fontSize = fontSize,
                    textAlign = TextAlign.Center,
                )
            }
        }
        if (whiteTurn == null) {
            Row(
                modifier = Modifier.width(cellSize / 2).height(cellSize / 2)
            ) {
                Text(
                    text = "", fontWeight = FontWeight.Bold, color = Color.Transparent, fontSize = fontSize
                )
            }
        } else {
            val color = if (whiteTurn) Color.White else Color.Black
            Column(
                modifier = Modifier.width(cellSize / 2).height(cellSize / 2).clip(CircleShape).background(color)
            ) {

            }
        }
    }
}

@Composable
private fun ChessBoardCell(
    modifier: Modifier = Modifier,
    isWhite: Boolean,
    size: Dp,
    pieceValue: Char,
    isDraggedPieceOrigin: Boolean,
    isDragAndDropStartCell: Boolean,
    isDragAndDropEndCell: Boolean,
    isIntoDragAndDropCrossLines: Boolean,
    isPendingPromotionStartCell: Boolean,
    isPendingPromotionEndCell: Boolean,
) {
    val whiteCellColor = 0xFFFFDEAD
    val blackCellColor = 0xFFCD853F
    val dragDropCrossLineCellColor = 0xFFE84FF5
    val dragDropStartCellColor = 0xFFDC1818
    val dragDropEndCellColor = 0xFF41D94D

    val strings = LocalStrings.current
    var bgColor = if (isWhite) Color(whiteCellColor) else Color(blackCellColor)
    if (isIntoDragAndDropCrossLines) bgColor = Color(dragDropCrossLineCellColor)
    if (isDragAndDropStartCell) bgColor = Color(dragDropStartCellColor)
    if (isDragAndDropEndCell) bgColor = Color(dragDropEndCellColor)
    if (isPendingPromotionStartCell) bgColor = Color(dragDropStartCellColor)
    if (isPendingPromotionEndCell) bgColor = Color(dragDropEndCellColor)

    Surface(modifier = modifier.size(size)) {
        Column(modifier = Modifier.background(bgColor)) {
            val noPiece = pieceValue == emptyCell
            if (!noPiece && !isDraggedPieceOrigin) {
                Image(
                    painter = painterResource(getVectorForPiece(pieceValue)),
                    contentDescription = getContentDescriptionForPiece(pieceValue, strings),
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

fun getVectorForPiece(pieceValue: Char): String {
    val name = when (pieceValue) {
        'P' -> "Chess_plt45.svg"
        'N' -> "Chess_nlt45.svg"
        'B' -> "Chess_blt45.svg"
        'R' -> "Chess_rlt45.svg"
        'Q' -> "Chess_qlt45.svg"
        'K' -> "Chess_klt45.svg"

        'p' -> "Chess_pdt45.svg"
        'n' -> "Chess_ndt45.svg"
        'b' -> "Chess_bdt45.svg"
        'r' -> "Chess_rdt45.svg"
        'q' -> "Chess_qdt45.svg"
        'k' -> "Chess_kdt45.svg"
        else -> throw IllegalArgumentException("Not recognized piece $pieceValue")
    }
    return "images/chess_vectors/$name"
}

fun getContentDescriptionForPiece(pieceValue: Char, strings: Strings): String {
    return when (pieceValue) {
        'P' -> strings.whitePawn
        'N' -> strings.whiteKnight
        'B' -> strings.whiteBishop
        'R' -> strings.whiteRook
        'Q' -> strings.whiteQueen
        'K' -> strings.whiteKing

        'p' -> strings.blackPawn
        'n' -> strings.blackKnight
        'b' -> strings.blackBishop
        'r' -> strings.blackRook
        'q' -> strings.blackQueen
        'k' -> strings.blackKing
        else -> strings.emptyCell
    }
}

@Parcelize
data class ChessBoardDragAndDropData(
    val startFile: Int,
    val startRank: Int,
    var endFile: Int,
    var endRank: Int,
    val startLocation: Offset,
    var currentLocation: Offset,
    val carriedPiece: Char,
)

@Preview
@Composable
fun ChessBoardLowerLayerPreview() {
    val piecesValues = defaultPosition.split(" ")[0].split("/").map { line ->
        line.flatMap { value ->
            if (value.isDigit()) {
                List(value.digitToInt()) { emptyCell }
            } else {
                listOf(value)
            }
        }
    }
    LowerLayer(
        cellSize = 300.dp,
        reversed = false,
        piecesValues = piecesValues,
        isWhiteTurn = true,
        dndData = null,
        pendingPromotionStartFile = null,
        pendingPromotionStartRank = null,
        pendingPromotionEndFile = null,
        pendingPromotionEndRank = null
    )
}