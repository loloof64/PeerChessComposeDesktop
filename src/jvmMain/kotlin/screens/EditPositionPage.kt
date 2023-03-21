package screens

import HorizontalNumberPicker
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.router.stack.push
import components.*
import i18n.LocalStrings
import kotlinx.coroutines.launch
import logic.*

const val noEnPassant = "-"

fun String.isWellFormedFen(): Boolean {
    val parts = split(" ")
    if (parts.size != 6) return false

    val boardPart = parts[0]
    if (boardPart.split("/").size != 8) return false

    if (parts[1] != "w" && parts[1] != "b") return false
    try {
        parts[4].toInt()
        parts[5].toInt()
    } catch (ex: NumberFormatException) {
        return false
    }

    return true
}

private fun boardPositionFromPiecesValues(piecesValues: List<List<Char>>): String {
    return piecesValues.joinToString(separator = "/") { line ->
        var result = ""
        var groupedHoles = 0

        for (elem in line) {
            if (elem == emptyCell) {
                groupedHoles++
            } else {
                if (groupedHoles > 0) result += groupedHoles.toString()
                groupedHoles = 0
                result += elem
            }
        }
        if (groupedHoles > 0) result += groupedHoles.toString()

        result
    }
}

private fun <T> List<List<T>>.replace(row: Int, col: Int, newValue: T): List<List<T>> {
    return mapIndexed { currRow, line ->
        line.mapIndexed { currCol, elem ->
            if (row == currRow && col == currCol) newValue
            else elem
        }.toList()
    }.toList()
}


@Composable
fun EditPositionPage(
    navigation: StackNavigation<Screen>,
    commitValidateActions: () -> Unit,
) {
    val strings = LocalStrings.current
    val clipboardManager = LocalClipboardManager.current

    val coroutineScope = rememberCoroutineScope()
    val scaffoldState = rememberScaffoldState()

    var boardReversed by rememberSaveable { mutableStateOf(false) }
    var piecesValues by rememberSaveable { mutableStateOf(positionFenToPiecesArray(defaultPosition)) }
    var currentEditingPiece by rememberSaveable { mutableStateOf(emptyCell) }
    var whiteOO by rememberSaveable { mutableStateOf(true) }
    var whiteOOO by rememberSaveable { mutableStateOf(true) }
    var blackOO by rememberSaveable { mutableStateOf(true) }
    var blackOOO by rememberSaveable { mutableStateOf(true) }
    var enPassantFile by rememberSaveable { mutableStateOf<Int?>(null) }
    var drawHalfMovesCount by rememberSaveable { mutableStateOf(0) }
    var moveNumber by rememberSaveable { mutableStateOf(1) }
    var whiteTurn by rememberSaveable { mutableStateOf(true) }
    var currentFen by rememberSaveable { mutableStateOf(defaultPosition) }

    fun getPositionFen(): String {
        val boardPart = boardPositionFromPiecesValues(piecesValues)
        val playerPart = if (whiteTurn) "w" else "b"
        var castleParts = ""
        if (whiteOO) castleParts += "K"
        if (whiteOOO) castleParts += "Q"
        if (blackOO) castleParts += "k"
        if (blackOOO) castleParts += "q"
        if (castleParts.isEmpty()) castleParts = "-"

        val enPassantPart = if (enPassantFile != null) {
            val rank = if (whiteTurn) 6 else 3
            val file = ('a'.code + enPassantFile!!).toChar()
            "$file$rank"
        } else "-"

        return "$boardPart $playerPart $castleParts $enPassantPart $drawHalfMovesCount $moveNumber"
    }

    fun onValidate() {
        try {
            ChessGameManager.setStartPosition(currentFen)
            ChessGameManager.resetGame()
            navigation.pop()
            navigation.pop()
            navigation.push(Screen.Game) {
                commitValidateActions()
            }
        }
        catch (ex: KingNotInTurnIsInCheck) {
            coroutineScope.launch {
                scaffoldState.snackbarHostState.showSnackbar(
                    message = strings.oppositeKingInCheckFen,
                    actionLabel = strings.close,
                    duration = SnackbarDuration.Long,
                )
            }
        }
        catch (ex: WrongFieldsCountException) {
            coroutineScope.launch {
                scaffoldState.snackbarHostState.showSnackbar(
                    message = strings.wrongFieldsCountFen,
                    actionLabel = strings.close,
                    duration = SnackbarDuration.Long,
                )
            }
        }
        catch (ex: WrongKingsCountException) {
            coroutineScope.launch {
                scaffoldState.snackbarHostState.showSnackbar(
                    message = strings.wrongKingsCountFen,
                    actionLabel = strings.close,
                    duration = SnackbarDuration.Long,
                )
            }
        }
    }

    fun onCancel() {
        navigation.pop()
    }

    fun updateFields() {
        val positionParts = currentFen.split(" ")
        piecesValues = positionFenToPiecesArray(currentFen)
        whiteTurn = positionParts[1] == "w"

        val castles = positionParts[2]
        whiteOO = castles.contains("K")
        whiteOOO = castles.contains("Q")
        blackOO = castles.contains("k")
        blackOOO = castles.contains("q")

        val enPassant = positionParts[3]
        enPassantFile = if (enPassant != "-") {
            enPassant[0].code - 'a'.code
        } else null

        drawHalfMovesCount = positionParts[4].toInt()
        moveNumber = positionParts[5].toInt()
    }

    Scaffold(
        scaffoldState = scaffoldState,
        topBar = {
            TopAppBar(
                title = {
                    Text(strings.editPositionPageTitle)
                },
                navigationIcon = {
                    IconButton({ navigation.pop() }) {
                        Icon(Icons.Default.ArrowBack, strings.goBack)
                    }
                },
            )
        }
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        )
        {
            Row(
                modifier = Modifier.fillMaxSize(0.8f).padding(top = 15.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            )
            {
                IconButton(
                    modifier = Modifier.padding(end = 5.dp),
                    content = {
                        Image(
                            painter = painterResource("images/material_vectors/swap_vert.svg"),
                            contentDescription = strings.swapBoardOrientation,
                            modifier = Modifier.size(50.dp)
                                .border(width = 2.dp, shape = CircleShape, color = Color.Blue),
                            colorFilter = ColorFilter.tint(Color.Blue)
                        )
                    }, onClick = {
                        boardReversed = !boardReversed
                    })
                Column(modifier = Modifier.fillMaxWidth(0.4f).fillMaxHeight()) {
                    ChessBoard(
                        isEditable = true,
                        piecesValues = piecesValues,
                        whitePlayerType = PlayerType.None,
                        blackPlayerType = PlayerType.None,
                        isWhiteTurn = whiteTurn,
                        reversed = boardReversed,
                        lastMoveArrow = null,
                        pendingPromotionStartFile = null,
                        pendingPromotionStartRank = null,
                        pendingPromotionEndFile = null,
                        pendingPromotionEndRank = null,
                        tryPlayingMove = { _ -> },
                        pendingPromotion = PendingPromotion.None,
                        onValidatePromotion = { _ -> },
                        onCancelPromotion = { },
                        onCellClick = { file, rank ->
                            piecesValues = piecesValues.replace(7 - rank, file, currentEditingPiece)
                            currentFen = getPositionFen()
                        }
                    )
                }

                PositionEditingControls(
                    modifier = Modifier.padding(start = 5.dp),
                    currentEditingPiece = currentEditingPiece,
                    hasWhiteOO = whiteOO,
                    hasWhiteOOO = whiteOOO,
                    hasBlackOO = blackOO,
                    hasBlackOOO = blackOOO,
                    selectedEnPassantFile = enPassantFile,
                    drawHalfMovesCount = drawHalfMovesCount,
                    moveNumber = moveNumber,
                    isWhiteTurn = whiteTurn,
                    currentFen = currentFen,
                    onEditingPieceChange = { newPiece ->
                        currentEditingPiece = newPiece
                        currentFen = getPositionFen()
                    },
                    onWhiteOOChange = {
                        whiteOO = it
                        currentFen = getPositionFen()
                    },
                    onWhiteOOOChange = {
                        whiteOOO = it
                        currentFen = getPositionFen()
                    },
                    onBlackOOChange = {
                        blackOO = it
                        currentFen = getPositionFen()
                    },
                    onBlackOOOChange = {
                        blackOOO = it
                        currentFen = getPositionFen()
                    },
                    onEnPassantSelection = {
                        enPassantFile = it
                        currentFen = getPositionFen()
                    },
                    onDrawHalfMovesCountChange = {
                        drawHalfMovesCount = it
                        currentFen = getPositionFen()
                    },
                    onMoveNumberChange = {
                        moveNumber = it
                        currentFen = getPositionFen()
                    },
                    onWhiteTurnChange = {
                        whiteTurn = it
                        currentFen = getPositionFen()
                    },
                    onDefaultBoardPosition = {
                        currentFen = defaultPosition
                        updateFields()
                    },
                    onEraseBoard = {
                        currentFen = emptyPosition
                        updateFields()
                    },
                    onCopyPositionFen = {
                        clipboardManager.setText(AnnotatedString(currentFen))
                    },
                    onPastePositionFen = {
                        clipboardManager.getText()?.text?.let {
                            try {
                                val fen = it.trim()
                                if (fen.isWellFormedFen()) {
                                    currentFen = fen
                                    updateFields()
                                }
                            } catch (ex: Exception) {
                                println(ex)
                            }
                        }
                    }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(30.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Button(::onValidate) {
                    Text(strings.validate)
                }
                Button(::onCancel) {
                    Text(strings.cancel)
                }
            }
        }
    }
}

@Composable
fun PositionEditingControls(
    modifier: Modifier = Modifier,
    currentFen: String,
    currentEditingPiece: Char,
    hasWhiteOO: Boolean,
    hasWhiteOOO: Boolean,
    hasBlackOO: Boolean,
    hasBlackOOO: Boolean,
    selectedEnPassantFile: Int?,
    drawHalfMovesCount: Int,
    moveNumber: Int,
    isWhiteTurn: Boolean,
    onEditingPieceChange: (Char) -> Unit,
    onWhiteOOChange: (Boolean) -> Unit,
    onWhiteOOOChange: (Boolean) -> Unit,
    onBlackOOChange: (Boolean) -> Unit,
    onBlackOOOChange: (Boolean) -> Unit,
    onEnPassantSelection: (Int?) -> Unit,
    onDrawHalfMovesCountChange: (Int) -> Unit,
    onMoveNumberChange: (Int) -> Unit,
    onWhiteTurnChange: (Boolean) -> Unit,
    onDefaultBoardPosition: () -> Unit,
    onEraseBoard: () -> Unit,
    onCopyPositionFen: () -> Unit,
    onPastePositionFen: () -> Unit,
) {
    val strings = LocalStrings.current
    var enPassantMenuExpanded by rememberSaveable { mutableStateOf(false) }
    val enPassantValues = listOf(noEnPassant, "a", "b", "c", "d", "e", "f", "g", "h")
    val scrollState = rememberScrollState()
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(strings.playerTurn)
            RadioButton(
                modifier = Modifier.padding(start = 10.dp),
                selected = isWhiteTurn,
                onClick = { onWhiteTurnChange(true) }
            )
            Text(strings.whiteTurn)
            RadioButton(
                modifier = Modifier.padding(start = 10.dp),
                selected = !isWhiteTurn,
                onClick = { onWhiteTurnChange(false) }
            )
            Text(strings.blackTurn)
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            listOf('P', 'N', 'B', 'R', 'Q', 'K').forEach { piece ->
                IconButton({ onEditingPieceChange(piece) }) {
                    Image(
                        painter = painterResource(getVectorForPiece(piece)),
                        contentDescription = getContentDescriptionForPiece(piece, strings)
                    )
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            listOf('p', 'n', 'b', 'r', 'q', 'k').forEach { piece ->
                IconButton({ onEditingPieceChange(piece) }) {
                    Image(
                        painter = painterResource(getVectorForPiece(piece)),
                        contentDescription = getContentDescriptionForPiece(piece, strings)
                    )
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(strings.selectedPiece)
            if (currentEditingPiece != emptyCell) {
                Image(
                    modifier = Modifier.border(width = 1.dp, color = Color.Black, shape = RectangleShape).size(50.dp),
                    painter = painterResource(getVectorForPiece(currentEditingPiece)),
                    contentDescription = getContentDescriptionForPiece(currentEditingPiece, strings)
                )
            } else {
                Row(modifier = Modifier.border(width = 1.dp, color = Color.Black, shape = RectangleShape).size(50.dp)) {
                    Image(
                        modifier = Modifier.border(width = 1.dp, color = Color.Black, shape = RectangleShape).size(50.dp),
                        painter = painterResource("images/material_vectors/delete.svg"),
                        contentDescription = strings.eraseCell,
                        colorFilter = ColorFilter.tint(Color.Red)
                    )
                }
            }
            IconButton({ onEditingPieceChange(emptyCell) }) {
                Image(
                    modifier = Modifier.size(45.dp),
                    painter = painterResource("images/material_vectors/delete.svg"),
                    contentDescription = strings.selectEraseCell,
                    colorFilter = ColorFilter.tint(Color.Red)
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(
                checked = hasWhiteOO,
                onCheckedChange = onWhiteOOChange,
            )
            Text(
                text = strings.whiteOO
            )

            Checkbox(
                checked = hasWhiteOOO,
                onCheckedChange = onWhiteOOOChange,
            )
            Text(
                text = strings.whiteOOO
            )
        }


        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(
                checked = hasBlackOO,
                onCheckedChange = onBlackOOChange,
            )
            Text(
                text = strings.blackOO
            )

            Checkbox(
                checked = hasBlackOOO,
                onCheckedChange = onBlackOOOChange,
            )
            Text(
                text = strings.blackOOO
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(strings.enPassantFile)
            Text(
                text = "[${enPassantValues[if (selectedEnPassantFile == null) 0 else selectedEnPassantFile + 1]}]",
                modifier = Modifier.padding(horizontal = 5.dp)
            )
            Button(onClick = { enPassantMenuExpanded = true }, modifier = Modifier.padding(horizontal = 5.dp)) {
                Text(strings.select)
            }
            DropdownMenu(
                expanded = enPassantMenuExpanded,
                onDismissRequest = { enPassantMenuExpanded = false },
            ) {
                enPassantValues.zip(listOf(null, 0, 1, 2, 3, 4, 5, 6, 7))
                    .forEach { (caption, value) ->
                        DropdownMenuItem({
                            enPassantMenuExpanded = false
                            onEnPassantSelection(value)
                        }) {
                            Text(caption)
                        }
                    }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(strings.drawHalfMovesCount)
            HorizontalNumberPicker(
                modifier = Modifier.padding(horizontal = 5.dp),
                value = drawHalfMovesCount,
                onStateChanged = onDrawHalfMovesCountChange,
                range = 0..50,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(strings.moveNumber)
            HorizontalNumberPicker(
                modifier = Modifier.padding(horizontal = 5.dp),
                value = moveNumber,
                onStateChanged = onMoveNumberChange,
                range = 1..Int.MAX_VALUE,
            )
        }

        Text(currentFen, modifier = Modifier.background(Color.Gray))


        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Button({ onDefaultBoardPosition() }) {
                Text(strings.setDefaultPosition)
            }

            Button({ onEraseBoard() }, modifier = Modifier.padding(start = 10.dp)) {
                Text(strings.eraseBoard)
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Button({ onCopyPositionFen() }) {
                Text(strings.copyFen)
            }

            Button({ onPastePositionFen() }, modifier = Modifier.padding(start = 10.dp)) {
                Text(strings.pasteFen)
            }
        }
    }
}
