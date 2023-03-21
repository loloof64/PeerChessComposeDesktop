package screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Button
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.push
import i18n.LocalStrings

@Composable
fun HomePage(
    navigation: StackNavigation<Screen>,
) {
    val strings = LocalStrings.current

    fun gotoGamePage() {
        navigation.push(Screen.Game)
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(strings.homePageTitle) })
        },
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center) {
            Button(onClick = {gotoGamePage()}) {
                Text(strings.gotoGamePage)
            }
        }
    }
}