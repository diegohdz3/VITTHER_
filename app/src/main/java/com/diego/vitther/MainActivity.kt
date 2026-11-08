package com.diego.vitther

import android.content.Intent
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.toArgb
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.diego.vitther.data.notifications.EXTRA_PIEZA
import com.diego.vitther.navigation.VittherNavHost
import com.diego.vitther.ui.theme.BackgroundPrimary
import com.diego.vitther.ui.theme.VITTHERTheme
import kotlinx.coroutines.flow.MutableStateFlow

class MainActivity : ComponentActivity() {

    /** Almacena la pieza que pidió abrir una notificación. */
    private val piezaPendiente = MutableStateFlow<Long?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        splash.setOnExitAnimationListener { provider ->
            // Nunca se retiene el splash esperando consultas o una futura API.
            if (savedInstanceState != null || !ValueAnimator.areAnimatorsEnabled()) {
                provider.remove()
            } else {
                provider.iconView.animate().scaleX(1.12f).scaleY(1.12f).setDuration(500).start()
                provider.view.animate().alpha(0f).setDuration(500)
                    .setListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) { provider.remove() }
                        override fun onAnimationCancel(animation: Animator) { provider.remove() }
                    }).start()
            }
        }
        // Edge-to-edge con barras oscuras: sin window.statusBarColor (deprecado).
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(BackgroundPrimary.toArgb()),
            navigationBarStyle = SystemBarStyle.dark(BackgroundPrimary.toArgb())
        )
        piezaPendiente.value = intent.getLongExtra(EXTRA_PIEZA, 0L).takeIf { it > 0 }
        setContent {
            VITTHERTheme {
                val pieza by piezaPendiente.collectAsStateWithLifecycle()
                VittherNavHost(
                    piezaPendiente = pieza,
                    onPiezaPendienteConsumida = {
                        piezaPendiente.value = null
                        intent.removeExtra(EXTRA_PIEZA)
                    }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        piezaPendiente.value = intent.getLongExtra(EXTRA_PIEZA, 0L).takeIf { it > 0 }
    }
}
