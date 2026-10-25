package app.pleromastisk

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import app.pleromastisk.ui.AppTheme
import app.pleromastisk.ui.PT
import app.pleromastisk.ui.PleromastiskRoot
import app.pleromastisk.ui.PleromastiskTheme
import app.pleromastisk.vm.AuthViewModel

class MainActivity : ComponentActivity() {
    private val auth: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        PT.theme = AppTheme.fromId(Graph.store.theme)
        if (savedInstanceState == null) handle(intent)

        setContent {
            // Lyst tema: mørke ikoner i status- og navigasjonslinjen
            val light = PT.theme.colors.light
            LaunchedEffect(light) {
                val style = if (light) {
                    SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                } else {
                    SystemBarStyle.dark(Color.TRANSPARENT)
                }
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
            }
            PleromastiskTheme {
                Box(Modifier.fillMaxSize().background(PT.Bg)) {
                    // Bakgrunnsbildet. Forskjøvet mot høyre slik at fjelltoppen vises i stående format.
                    Image(
                        painter = painterResource(R.drawable.bakgrunn),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        alignment = BiasAlignment(horizontalBias = 0.5f, verticalBias = 0f),
                        modifier = Modifier.fillMaxSize(),
                    )
                    // Slør i temaets farge, så teksten alltid er lesbar
                    Box(Modifier.fillMaxSize().background(PT.Overlay))
                    PleromastiskRoot(auth)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handle(intent)
    }

    private fun handle(intent: Intent?) {
        intent?.data?.let { auth.handleRedirect(it) }
    }
}
