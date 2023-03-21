package screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.arkivanov.decompose.ExperimentalDecomposeApi
import com.arkivanov.decompose.extensions.compose.jetbrains.stack.animation.fade
import com.arkivanov.decompose.extensions.compose.jetbrains.stack.animation.plus
import com.arkivanov.decompose.extensions.compose.jetbrains.stack.animation.scale
import com.arkivanov.decompose.extensions.compose.jetbrains.stack.animation.stackAnimation
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.essenty.parcelable.Parcelable
import com.arkivanov.essenty.parcelable.Parcelize
import logic.*
import navigator.ChildStack

@OptIn(ExperimentalDecomposeApi::class)
@Composable
fun MainContent() {
    val navigation = remember { StackNavigation<Screen>() }

    ChildStack(
        source = navigation,
        initialStack = { listOf(Screen.Home) },
        animation = stackAnimation(fade() + scale()),
    ) { screen ->
        when (screen) {
            is Screen.Home -> HomePage(
                navigation = navigation,
            )

            is Screen.Game -> GamePage(
                navigation = navigation,
            )

            is Screen.EditPosition -> EditPositionPage(
                navigation = navigation,
            )
        }
    }
}

sealed class Screen : Parcelable {

    @Parcelize
    object Home: Screen()
    @Parcelize
    object Game : Screen()

    @Parcelize
    object EditPosition : Screen()
}