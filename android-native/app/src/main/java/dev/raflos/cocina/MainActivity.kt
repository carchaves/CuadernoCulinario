package dev.raflos.cocina

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import dev.raflos.cocina.data.SyncRepository
import dev.raflos.cocina.data.local.CocinaDatabase
import dev.raflos.cocina.data.remote.NetworkModule
import dev.raflos.cocina.data.remote.TokenStore
import dev.raflos.cocina.ui.AppViewModel
import dev.raflos.cocina.ui.despensa.DespensaScreen
import dev.raflos.cocina.ui.lista.ListaDeCompraScreen
import dev.raflos.cocina.ui.login.LoginScreen
import dev.raflos.cocina.ui.menu.MenuDestination
import dev.raflos.cocina.ui.menu.MenuScreen
import dev.raflos.cocina.ui.recetas.RecetasScreen

private enum class Screen { MENU, DESPENSA, RECETAS, COMPRA }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    CocinaRoot()
                }
            }
        }
    }
}

@Composable
private fun CocinaRoot() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val tokenStore = remember { TokenStore(context) }
    var authed by remember { mutableStateOf(tokenStore.hasSession()) }

    if (!authed) {
        LoginScreen(tokenStore = tokenStore, onSuccess = { authed = true })
        return
    }

    val network = remember(authed) { NetworkModule(tokenStore) }
    val repo = remember(authed) { SyncRepository(CocinaDatabase.get(context).appStateDao(), network.cocinaApi, network) }
    val vm: AppViewModel = viewModel(factory = AppViewModel.Factory(repo), key = "app-$authed")

    // Sync al abrir y cada vez que la app vuelve a primer plano.
    val lifecycleOwner = LocalLifecycleOwner.current
    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) vm.syncNow()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val state by vm.state.collectAsState()
    var screen by remember { mutableStateOf(Screen.MENU) }

    val currentState = state
    if (currentState == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }

    when (screen) {
        Screen.MENU -> MenuScreen(onGo = { dest ->
            screen = when (dest) {
                MenuDestination.DESPENSA -> Screen.DESPENSA
                MenuDestination.RECETAS -> Screen.RECETAS
                MenuDestination.COMPRA -> Screen.COMPRA
            }
        })
        Screen.DESPENSA -> DespensaScreen(currentState, vm, onBack = { screen = Screen.MENU })
        Screen.RECETAS -> RecetasScreen(currentState, vm, onBack = { screen = Screen.MENU })
        Screen.COMPRA -> ListaDeCompraScreen(currentState, vm, onBack = { screen = Screen.MENU })
    }
}
