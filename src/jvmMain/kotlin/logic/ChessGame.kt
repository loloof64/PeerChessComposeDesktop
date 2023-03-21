package logic

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import components.*
import io.github.wolfraam.chessgame.ChessGame
import io.github.wolfraam.chessgame.board.Piece
import io.github.wolfraam.chessgame.board.PieceType
import io.github.wolfraam.chessgame.board.Side
import io.github.wolfraam.chessgame.board.Square
import io.github.wolfraam.chessgame.move.IllegalMoveException
import io.github.wolfraam.chessgame.move.Move
import io.github.wolfraam.chessgame.notation.NotationType
import io.github.wolfraam.chessgame.pgn.PGNExporter
import io.github.wolfraam.chessgame.pgn.PgnTag
import io.github.wolfraam.chessgame.result.ChessGameResult
import io.github.wolfraam.chessgame.result.ChessGameResultType
import io.github.wolfraam.chessgame.result.DrawType
import java.io.File
import java.io.FileOutputStream

const val defaultPosition = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"
const val emptyPosition = "4k3/8/8/8/8/8/8/4K3 w - - 0 1"

data class MoveResultInfo(val isLegal: Boolean, val isPendingPromotion: Boolean)

fun String.toLastMoveArrow(): LastMoveArrow {
    if (length < 4) throw IllegalMoveException("Not a uci chess move string : $this.")

    val startFile = this[0].code - 'a'.code
    val startRank = this[1].code - '1'.code

    val endFile = this[2].code - 'a'.code
    val endRank = this[3].code - '1'.code

    return LastMoveArrow(
        startFile = startFile,
        startRank = startRank,
        endFile = endFile,
        endRank = endRank
    )
}

fun LastMoveArrow.toMoveCoordinates(): MoveCoordinates {
    return MoveCoordinates(
        startFile = startFile,
        startRank = startRank,
        endFile = endFile,
        endRank = endRank
    )
}

object ChessGameManager {
    private var _gameInProgress by mutableStateOf(false)
    private var _gameLogic by mutableStateOf(ChessGame(emptyPosition))
    private var _pendingPromotion by mutableStateOf(PendingPromotion.None)
    private var _pendingPromotionStartSquare by mutableStateOf<Square?>(null)
    private var _pendingPromotionEndSquare by mutableStateOf<Square?>(null)
    private var _lastMoveArrow by mutableStateOf<LastMoveArrow?>(null)
    private var _historyElements by mutableStateOf<MutableList<ChessHistoryItem>>(mutableListOf())
    private var _isFirstHistoryNode by mutableStateOf(false)
    private var _positionFenBeforeLastMove by mutableStateOf<String?>(null)
    private var _selectedNodeIndex by mutableStateOf<Int?>(null)
    private var _startPosition by mutableStateOf(defaultPosition)
    private var _whitePlayerType by mutableStateOf(PlayerType.Human)
    private var _blackPlayerType by mutableStateOf(PlayerType.Human)
    private var _savedGameLogic by mutableStateOf<ChessGame?>(null)

    fun getCurrentPosition(): String = _gameLogic.fen

    fun exportAsPgn(outputFile: File) {
        val exporter = PGNExporter(FileOutputStream(outputFile))
        _savedGameLogic?.setPgnTag(PgnTag.EVENT, "")
        _savedGameLogic?.setPgnTag(PgnTag.SITE, "")
        _savedGameLogic?.setPgnTag(PgnTag.DATE, "")
        _savedGameLogic?.setPgnTag(PgnTag.ROUND, "")
        _savedGameLogic?.setPgnTag(PgnTag.WHITE, "")
        _savedGameLogic?.setPgnTag(PgnTag.BLACK, "")
        if (_startPosition != defaultPosition) {
            _savedGameLogic?.setPgnTag(PgnTag.FEN, _startPosition)
            _savedGameLogic?.setPgnTag(PgnTag.SET_UP, "1")
        }
        exporter.write(_savedGameLogic)
    }

    fun getPieces(): List<List<Char>> {
        val positionFen = _gameLogic.fen
        val lineParts = positionFen.split(" ")[0].split('/')

        return lineParts.map { line ->
            line.flatMap { value ->
                if (value.isDigit()) {
                    List(value.digitToInt()) { emptyCell }
                } else {
                    listOf(value)
                }
            }
        }
    }

    fun setWhitePlayerType(playerType: PlayerType) {
        _whitePlayerType = playerType
    }

    fun setBlackPlayerType(playerType: PlayerType) {
        _blackPlayerType = playerType
    }

    fun getWhitePlayerType(): PlayerType = _whitePlayerType

    fun getBlackPlayerType(): PlayerType = _blackPlayerType

    fun getHistoryElements(): List<ChessHistoryItem> = _historyElements

    fun getLastMoveArrow(): LastMoveArrow? = _lastMoveArrow

    fun isGameInProgress(): Boolean = _gameInProgress

    fun getPendingPromotion(): PendingPromotion = _pendingPromotion

    fun getPendingPromotionStartSquare(): Square? = _pendingPromotionStartSquare

    fun getPendingPromotionEndSquare(): Square? = _pendingPromotionEndSquare

    fun getSelectedHistoryNodeIndex(): Int? = _selectedNodeIndex

    fun isWhiteTurn(): Boolean {
        val positionFen = _gameLogic.fen
        return positionFen.split(" ")[1] == "w"
    }

    fun setStartPosition(startPosition: String) {
        startPosition.testIfIsLegalChessFen()
        _startPosition = startPosition
    }

    fun stopGame() {
        _gameInProgress = false
        _whitePlayerType = PlayerType.None
        _blackPlayerType = PlayerType.None
        if (_gameLogic.getPgnTagValue(PgnTag.RESULT) == null) {
            _gameLogic.setPgnTag(PgnTag.RESULT, "*")
        }
        _savedGameLogic = _gameLogic.clone()
        selectLastHistoryMoveNodeIfAny()
    }

    fun resetGame() {
        _gameLogic = ChessGame(_startPosition)
        _savedGameLogic = null
        val isWhiteTurn = _gameLogic.sideToMove == Side.WHITE
        val moveNumber = _gameLogic.fullMoveCount
        _whitePlayerType = PlayerType.Human
        _blackPlayerType = PlayerType.Human
        _historyElements = mutableListOf()
        _historyElements.add(ChessHistoryItem.MoveNumberItem(moveNumber, isWhiteTurn))
        _pendingPromotion = PendingPromotion.None
        _pendingPromotionStartSquare = null
        _pendingPromotionEndSquare = null
        _lastMoveArrow = null
        _isFirstHistoryNode = true
        _positionFenBeforeLastMove = null
        _selectedNodeIndex = null
        _gameInProgress = true
    }

    fun playMove(
        startFile: Int, startRank: Int,
        endFile: Int, endRank: Int,
        onCheckmate: (Boolean) -> Unit,
        onStalemate: () -> Unit,
        onThreeFoldsRepetition: () -> Unit,
        onInsufficientMaterial: () -> Unit,
        onFiftyMovesRuleDraw: () -> Unit
    ): MoveResultInfo {
        val startSquare = Square.values()[8 * startFile + startRank]
        val endSquare = Square.values()[8 * endFile + endRank]
        val move = Move(startSquare, endSquare)

        if (_gameLogic.isLegalMove(move)) {
            _positionFenBeforeLastMove = _gameLogic.fen
            _gameLogic.playMove(move)
            addMoveToHistory(
                MoveCoordinates(
                    startFile = startFile,
                    startRank = startRank,
                    endFile = endFile,
                    endRank = endRank
                )
            )
            _lastMoveArrow = LastMoveArrow(
                startFile = startFile,
                startRank = startRank,
                endFile = endFile,
                endRank = endRank
            )
            handleGameEndingStatus(
                onCheckmate = onCheckmate,
                onStalemate = onStalemate,
                onThreeFoldsRepetition = onThreeFoldsRepetition,
                onInsufficientMaterial = onInsufficientMaterial,
                onFiftyMovesRuleDraw = onFiftyMovesRuleDraw,
            )
            return MoveResultInfo(isLegal = true, isPendingPromotion = false)
        } else {
            val isLegalPromotionMove = _gameLogic.isLegalMove(Move(startSquare, endSquare, PieceType.QUEEN))

            if (isLegalPromotionMove) {
                _pendingPromotion = if (isWhiteTurn()) PendingPromotion.White else PendingPromotion.Black
                _pendingPromotionStartSquare = startSquare
                _pendingPromotionEndSquare = endSquare
                return MoveResultInfo(isLegal = true, isPendingPromotion = true)
            }

            return MoveResultInfo(isLegal = false, isPendingPromotion = false)
        }
    }

    fun cancelPromotion() {
        _pendingPromotion = PendingPromotion.None
        _pendingPromotionStartSquare = null
        _pendingPromotionEndSquare = null
    }

    fun commitPromotion(
        pieceType: PromotionType,
        onCheckmate: (Boolean) -> Unit,
        onStalemate: () -> Unit,
        onThreeFoldsRepetition: () -> Unit,
        onInsufficientMaterial: () -> Unit,
        onFiftyMovesRuleDraw: () -> Unit
    ) {
        if (_pendingPromotion == PendingPromotion.None) return
        val promotionPiece = when (pieceType) {
            PromotionType.Queen -> PieceType.QUEEN
            PromotionType.Rook -> PieceType.ROOK
            PromotionType.Bishop -> PieceType.BISHOP
            PromotionType.Knight -> PieceType.KNIGHT
        }
        val move = Move(_pendingPromotionStartSquare, _pendingPromotionEndSquare, promotionPiece)
        if (_gameLogic.isLegalMove(move)) {
            _positionFenBeforeLastMove = _gameLogic.fen
            _gameLogic.playMove(move)
            addMoveToHistory(
                MoveCoordinates(
                    startFile = _pendingPromotionStartSquare!!.x,
                    startRank = _pendingPromotionStartSquare!!.y,
                    endFile = _pendingPromotionEndSquare!!.x,
                    endRank = _pendingPromotionEndSquare!!.y
                )
            )
            _pendingPromotion = PendingPromotion.None
            _pendingPromotionStartSquare = null
            _pendingPromotionEndSquare = null

            _lastMoveArrow = LastMoveArrow(
                startFile = move.from.x,
                startRank = move.from.y,
                endFile = move.to.x,
                endRank = move.to.y
            )


            handleGameEndingStatus(
                onCheckmate = onCheckmate,
                onStalemate = onStalemate,
                onThreeFoldsRepetition = onThreeFoldsRepetition,
                onInsufficientMaterial = onInsufficientMaterial,
                onFiftyMovesRuleDraw = onFiftyMovesRuleDraw,
            )
        }
    }

    fun requestPosition(positionFen: String, moveCoordinates: MoveCoordinates, nodeToSelectIndex: Int): Boolean {
        if (_gameInProgress) return false
        _gameLogic = ChessGame(positionFen)
        _lastMoveArrow = LastMoveArrow(
            startFile = moveCoordinates.startFile,
            startRank = moveCoordinates.startRank,
            endFile = moveCoordinates.endFile,
            endRank = moveCoordinates.endRank
        )
        _selectedNodeIndex = nodeToSelectIndex
        return true
    }

    fun requestGotoPreviousHistoryNode(): Boolean {
        if (_gameInProgress) return false
        if (_selectedNodeIndex == null) return false
        return if (requestBackOneMove()) {
            val currentHistoryNode = _historyElements[_selectedNodeIndex!!] as ChessHistoryItem.MoveItem
            _gameLogic = ChessGame(currentHistoryNode.positionFen)
            _lastMoveArrow = LastMoveArrow(
                startFile = currentHistoryNode.movesCoordinates.startFile,
                startRank = currentHistoryNode.movesCoordinates.startRank,
                endFile = currentHistoryNode.movesCoordinates.endFile,
                endRank = currentHistoryNode.movesCoordinates.endRank,
            )
            true
        } else {
            _gameLogic = ChessGame(_startPosition)
            _selectedNodeIndex = null
            _lastMoveArrow = null
            true
        }
    }

    fun requestGotoNextHistoryNode(): Boolean {
        if (_gameInProgress) return false
        return requestForwardOneMove()
    }

    fun requestGotoFirstPosition(): Boolean {
        if (_gameInProgress) return false
        while (requestGotoPreviousHistoryNode()){}
        return true
    }

    fun requestGotoLastHistoryNode(): Boolean {
        if (_gameInProgress) return false
        while (requestGotoNextHistoryNode()){}
        return true
    }

    fun checkIfPlayerWinningOnTimeIsMissingMaterialAndUpdatePgnResultTag(): Boolean {
        val supposedWinnerSide = if (_gameLogic.sideToMove == Side.BLACK) Side.WHITE else Side.BLACK
        if (supposedWinnerSide == Side.WHITE) {
            val queensCount = _gameLogic.getSquares(Piece.WHITE_QUEEN).size
            val rooksCount = _gameLogic.getSquares(Piece.WHITE_ROOK).size
            val pawnsCount = _gameLogic.getSquares(Piece.WHITE_PAWN).size

            if (queensCount > 0 || rooksCount > 0 || pawnsCount > 0) {
                _gameLogic.setPgnTag(PgnTag.RESULT, "1-0")
                return false
            }

            val bishopsCount = _gameLogic.getSquares(Piece.WHITE_BISHOP).size
            val knightsCount = _gameLogic.getSquares(Piece.WHITE_KNIGHT).size

            val isDraw = (bishopsCount == 0 && knightsCount == 1) || (knightsCount == 0 && bishopsCount == 1)
            if (isDraw) _gameLogic.setPgnTag(PgnTag.RESULT, "1/2-1/2")
            else _gameLogic.setPgnTag(PgnTag.RESULT, "1-0")
            return isDraw
        } else {
            val queensCount = _gameLogic.getSquares(Piece.BLACK_QUEEN).size
            val rooksCount = _gameLogic.getSquares(Piece.BLACK_ROOK).size
            val pawnsCount = _gameLogic.getSquares(Piece.BLACK_PAWN).size

            if (queensCount > 0 || rooksCount > 0 || pawnsCount > 0) {
                _gameLogic.setPgnTag(PgnTag.RESULT, "0-1")
                return false
            }

            val bishopsCount = _gameLogic.getSquares(Piece.BLACK_BISHOP).size
            val knightsCount = _gameLogic.getSquares(Piece.BLACK_KNIGHT).size

            val isDraw = (bishopsCount == 0 && knightsCount <= 1) || (knightsCount == 0 && bishopsCount <= 1)
            if (isDraw) _gameLogic.setPgnTag(PgnTag.RESULT, "1/2-1/2")
            else _gameLogic.setPgnTag(PgnTag.RESULT, "0-1")
            return isDraw
        }
    }

    private fun handleGameEndingStatus(
        onCheckmate: (Boolean) -> Unit,
        onStalemate: () -> Unit,
        onThreeFoldsRepetition: () -> Unit,
        onInsufficientMaterial: () -> Unit,
        onFiftyMovesRuleDraw: () -> Unit
    ) {
        val gameResult: ChessGameResult? = _gameLogic.gameResult
        when (gameResult?.chessGameResultType) {
            ChessGameResultType.WHITE_WINS -> {
                _gameInProgress = false
                _whitePlayerType = PlayerType.None
                _blackPlayerType = PlayerType.None
                _gameLogic.setPgnTag(PgnTag.RESULT, "1-0")
                _savedGameLogic = _gameLogic.clone()
                selectLastHistoryMoveNodeIfAny()
                onCheckmate(true)
            }

            ChessGameResultType.BLACK_WINS -> {
                _gameInProgress = false
                _whitePlayerType = PlayerType.None
                _blackPlayerType = PlayerType.None
                _gameLogic.setPgnTag(PgnTag.RESULT, "0-1")
                _savedGameLogic = _gameLogic.clone()
                selectLastHistoryMoveNodeIfAny()
                onCheckmate(false)
            }

            ChessGameResultType.DRAW -> {
                when (gameResult.drawType) {
                    DrawType.STALE_MATE -> {
                        _gameInProgress = false
                        _whitePlayerType = PlayerType.None
                        _blackPlayerType = PlayerType.None
                        _gameLogic.setPgnTag(PgnTag.RESULT, "1/2-1/2")
                        _savedGameLogic = _gameLogic.clone()
                        selectLastHistoryMoveNodeIfAny()
                        onStalemate()
                    }

                    DrawType.THREEFOLD_REPETITION -> {
                        _gameInProgress = false
                        _whitePlayerType = PlayerType.None
                        _blackPlayerType = PlayerType.None
                        _gameLogic.setPgnTag(PgnTag.RESULT, "1/2-1/2")
                        _savedGameLogic = _gameLogic.clone()
                        selectLastHistoryMoveNodeIfAny()
                        onThreeFoldsRepetition()
                    }

                    DrawType.INSUFFICIENT_MATERIAL -> {
                        _gameInProgress = false
                        _whitePlayerType = PlayerType.None
                        _blackPlayerType = PlayerType.None
                        _gameLogic.setPgnTag(PgnTag.RESULT, "1/2-1/2")
                        _savedGameLogic = _gameLogic.clone()
                        selectLastHistoryMoveNodeIfAny()
                        onInsufficientMaterial()
                    }

                    DrawType.FIFTY_MOVE_RULE -> {
                        _gameInProgress = false
                        _whitePlayerType = PlayerType.None
                        _blackPlayerType = PlayerType.None
                        _gameLogic.setPgnTag(PgnTag.RESULT, "1/2-1/2")
                        _savedGameLogic = _gameLogic.clone()
                        selectLastHistoryMoveNodeIfAny()
                        onFiftyMovesRuleDraw()
                    }

                    else -> throw RuntimeException("Not in a draw state.")
                }
            }

            else -> {}
        }
    }

    private fun requestForwardOneMove(): Boolean {
        if (_gameInProgress) return false
        if (_selectedNodeIndex != null && _selectedNodeIndex!! >= _historyElements.size - 1) return false
        var newSelectedNodeIndex = if (_selectedNodeIndex == null) {
            0
        } else (_selectedNodeIndex!! + 1)
        while ((newSelectedNodeIndex < _historyElements.size - 1) && (_historyElements[newSelectedNodeIndex] !is ChessHistoryItem.MoveItem)) {
            newSelectedNodeIndex++
        }
        if (_historyElements[newSelectedNodeIndex] !is ChessHistoryItem.MoveItem) return false
        _selectedNodeIndex = newSelectedNodeIndex
        val currentHistoryNode = _historyElements[newSelectedNodeIndex] as ChessHistoryItem.MoveItem
        _gameLogic = ChessGame(currentHistoryNode.positionFen)
        _lastMoveArrow = LastMoveArrow(
            startFile = currentHistoryNode.movesCoordinates.startFile,
            startRank = currentHistoryNode.movesCoordinates.startRank,
            endFile = currentHistoryNode.movesCoordinates.endFile,
            endRank = currentHistoryNode.movesCoordinates.endRank,
        )
        return true
    }

    private fun requestBackOneMove(): Boolean {
        if (_gameInProgress) return false
        if (_selectedNodeIndex == null) return false
        if (_selectedNodeIndex!! <= 0) return false
        var newSelectedNodeIndex = _selectedNodeIndex!! - 1
        while ((newSelectedNodeIndex >= 0) && (_historyElements[newSelectedNodeIndex] !is ChessHistoryItem.MoveItem)) {
            newSelectedNodeIndex--
        }
        return if (newSelectedNodeIndex >= 0) {
            _selectedNodeIndex = newSelectedNodeIndex
            true
        } else {
            false
        }
    }

    private fun addMoveToHistory(moveCoordinates: MoveCoordinates) {
        val lastMove: Move? = _gameLogic.lastMove
        val isWhiteTurnBeforeMove = _gameLogic.sideToMove == Side.BLACK
        val needingToAddMoveNumber = isWhiteTurnBeforeMove && !_isFirstHistoryNode

        if (needingToAddMoveNumber) {
            _historyElements.add(
                ChessHistoryItem.MoveNumberItem(
                    number = _gameLogic.fullMoveCount,
                    isWhiteTurn = true,
                )
            )
        }

        val gameLogicBeforeMove = ChessGame(_positionFenBeforeLastMove)
        val moveSan = gameLogicBeforeMove.getNotation(NotationType.SAN, lastMove)
        _historyElements.add(
            ChessHistoryItem.MoveItem(
                san = moveSan, positionFen = _gameLogic.fen, isWhiteMove = isWhiteTurnBeforeMove,
                movesCoordinates = moveCoordinates,
            )
        )
        _isFirstHistoryNode = false
    }


    private fun selectLastHistoryMoveNodeIfAny() {
        var lastHistoryMoveNodeIndex = _historyElements.size - 1
        while ((lastHistoryMoveNodeIndex >= 0) && (_historyElements[lastHistoryMoveNodeIndex] !is ChessHistoryItem.MoveItem)) {
            lastHistoryMoveNodeIndex--
        }
        _selectedNodeIndex = if (lastHistoryMoveNodeIndex >= 0) lastHistoryMoveNodeIndex else null
    }
}