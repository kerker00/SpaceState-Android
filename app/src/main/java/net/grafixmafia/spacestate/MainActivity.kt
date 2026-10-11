package net.grafixmafia.spacestate

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import android.util.Log
import net.grafixmafia.spacestate.ui.theme.SpaceStateTheme
import net.grafixmafia.spacestatekit.SpaceStateClient

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SpaceStateTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    SmokeTest(Modifier.padding(innerPadding))
                }
            }
        }
    }
}

/** Temporary: proves SpaceStateKit runs inside the app. Replaced by the status screen. */
@Composable
fun SmokeTest(modifier: Modifier = Modifier) {
    var text by remember { mutableStateOf("Loading…") }
    LaunchedEffect(Unit) {
        val client = SpaceStateClient(
            spacePushUrl = "https://push.grafixmafia.net",
            userAgent = "SpaceState/0 (Android ${Build.VERSION.RELEASE})",
            activePeriodsFile = null,
        )
        val endpoint = SpaceStateClient.mainframeEndpoint
        text = runCatching {
            val space = client.space(endpoint)
            val rooms = client.mainframeRooms()
            val directory = client.directory()
            "${space.name}: ${space.status} since ${space.lastChange}\n" +
                rooms.joinToString("\n") { "${it.name}: ${it.state}" } +
                "\n${directory.size} spaces in the directory"
        }.getOrElse { "Failed: $it" }
        Log.i("SpaceStateSmoke", text)
    }
    Text(text, modifier)
}
