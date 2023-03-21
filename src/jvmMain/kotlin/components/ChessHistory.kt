package components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.platform.Font
import androidx.compose.ui.unit.dp
import com.google.accompanist.flowlayout.FlowCrossAxisAlignment
import com.google.accompanist.flowlayout.FlowRow
import com.google.accompanist.flowlayout.MainAxisAlignment
import i18n.LocalStrings
import logic.toFAN

enum class GameTermination {
    InProgress,
    WhiteWin,
    BlackWin,
    Draw
}

fun GameTermination.toText(): String = when (this) {
    GameTermination.InProgress -> "*"
    GameTermination.WhiteWin -> "1-0"
    GameTermination.BlackWin -> "0-1"
    GameTermination.Draw -> "1/2-1/2"
}

data class MoveCoordinates(
    val startFile: Int,
    val startRank: Int,
    val endFile: Int,
    val endRank: Int,
)

sealed class ChessHistoryItem {
    data class MoveNumberItem(val number: Int, val isWhiteTurn: Boolean) : ChessHistoryItem()
    data class GameTerminationItem(val termination: GameTermination) : ChessHistoryItem()
    data class MoveItem(
        val san: String, val positionFen: String, val isWhiteMove: Boolean,
        val movesCoordinates: MoveCoordinates
    ) : ChessHistoryItem()
}

const val minFontSizePx = 15f
const val maxFontSizePx = 30f

@Composable
fun ChessHistory(
    modifier: Modifier = Modifier,
    selectedNodeIndex: Int?,
    items: List<ChessHistoryItem>,
    onPositionRequest: (String, MoveCoordinates, Int) -> Unit,
    onRequestBackOneMove: () -> Unit,
    onRequestForwardOneMove: () -> Unit,
    onRequestGotoFirstPosition: () -> Unit,
    onRequestGotoLastMove: () -> Unit,
) {
    val scrollState = rememberScrollState()
    val strings = LocalStrings.current
    BoxWithConstraints {
        val fontSize = with(LocalDensity.current) {
            var size = maxWidth * 0.1f
            if (size.toPx() > maxFontSizePx) {
                size = maxFontSizePx.toDp()
            }
            if (size.toPx() < minFontSizePx) {
                size = minFontSizePx.toDp()
            }
            size.toSp()
        }
        val textStyle = TextStyle(
            fontSize = fontSize,
            fontWeight = FontWeight.Normal,
            fontFamily = FontFamily(
                Font(
                    resource = "fonts/FreeSerif.ttf",
                    weight = FontWeight.Normal,
                    style = FontStyle.Normal,
                ),
            )
        )
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(6.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Top,
            ) {
                Button(onClick = onRequestGotoFirstPosition, modifier = Modifier.size(45.dp)) {
                    Image(
                        modifier = Modifier.size(30.dp),
                        painter = painterResource("images/material_vectors/keyboard_double_arrow_left.svg"),
                        contentDescription = strings.goStartHistory,
                        colorFilter = ColorFilter.tint(Color.White)
                    )
                }
                Button(onClick = onRequestBackOneMove, modifier = Modifier.size(45.dp)) {
                    Image(
                        modifier = Modifier.size(30.dp),
                        painter = painterResource("images/material_vectors/arrow_back.svg"),
                        contentDescription = strings.goBackHistory,
                        colorFilter = ColorFilter.tint(Color.White)
                    )
                }
                Button(onClick = onRequestForwardOneMove, modifier = Modifier.size(45.dp)) {
                    Image(
                        modifier = Modifier.size(30.dp),
                        painter = painterResource("images/material_vectors/arrow_forward.svg"),
                        contentDescription = strings.goForwardHistory,
                        colorFilter = ColorFilter.tint(Color.White)
                    )
                }
                Button(onClick = onRequestGotoLastMove, modifier = Modifier.size(45.dp)) {
                    Image(
                        modifier = Modifier.size(30.dp),
                        painter = painterResource("images/material_vectors/keyboard_double_arrow_right.svg"),
                        contentDescription = strings.goEndHistory,
                        colorFilter = ColorFilter.tint(Color.White)
                    )
                }
            }
            FlowRow(
                modifier = modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .background(Color(0x88FFCC00))
                    .padding(6.dp),
                mainAxisAlignment = MainAxisAlignment.Start,
                crossAxisAlignment = FlowCrossAxisAlignment.Start,
                mainAxisSpacing = 10.dp,
            ) {
                items.mapIndexed { index, item ->
                    when (item) {
                        is ChessHistoryItem.MoveNumberItem -> Text(
                            style = textStyle,
                            text = "${item.number}.${if (item.isWhiteTurn) "" else ".."}"
                        )

                        is ChessHistoryItem.GameTerminationItem -> {
                            val text = item.termination.toText()
                            Text(
                                style = textStyle,
                                text = text,
                            )
                        }

                        is ChessHistoryItem.MoveItem -> ClickableText(
                            text = AnnotatedString(text = item.san.toFAN(forBlackTurn = !item.isWhiteMove)),
                            style = if (index != selectedNodeIndex) textStyle else textStyle.copy(
                                background = Color.Blue,
                                color = Color.White
                            ),
                            onClick = {
                                onPositionRequest(item.positionFen, item.movesCoordinates, index)
                            },
                        )
                    }
                }
            }
        }
    }
}