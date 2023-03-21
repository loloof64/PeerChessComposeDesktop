package logic

import io.github.wolfraam.chessgame.ChessGame
import io.github.wolfraam.chessgame.board.Board
import io.github.wolfraam.chessgame.board.Piece
import io.github.wolfraam.chessgame.move.KingState
import io.github.wolfraam.chessgame.move.MoveHelper

fun ChessGame.isOppositeKingAttacked(): Boolean {
    val board = Board.fromFen(fen)
    val moveHelper = MoveHelper(board)
    return moveHelper.getKingState(board.sideToMove.flip(), false) == KingState.CHECK
}

fun String.testIfIsLegalChessFen() {
    val parts = this.split(" ")
    if (parts.size != 6) throw WrongFieldsCountException(parts.size)

    val gameLogic = ChessGame(this)
    if (gameLogic.isOppositeKingAttacked()) throw KingNotInTurnIsInCheck()

    val whiteKingsCount = gameLogic.getSquares(Piece.WHITE_KING).size
    val blackKingsCount = gameLogic.getSquares(Piece.BLACK_KING).size

    if (whiteKingsCount != 1 || blackKingsCount != 1) throw WrongKingsCountException()
}

data class WrongFieldsCountException(val count: Int): Exception()
class KingNotInTurnIsInCheck: Exception()

class WrongKingsCountException: Exception()