package dev.raflos.cocina.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.raflos.cocina.BuildConfig
import dev.raflos.cocina.data.remote.GithubTokenStore
import dev.raflos.cocina.data.remote.NetworkModule
import dev.raflos.cocina.ui.SystemBarsAppearance
import dev.raflos.cocina.ui.theme.MenuColors
import dev.raflos.cocina.ui.theme.SerifFamily
import kotlinx.coroutines.launch

/**
 * Ajustes: el token de GitHub con el que la app commitea los archivos de `data/`. No es un
 * login — no hay usuarios ni sesión, solo esta credencial guardada cifrada en el dispositivo.
 *
 * [onBack] en null = primer arranque (todavía no hay token, no se puede salir de acá).
 */
@Composable
fun SettingsScreen(tokenStore: GithubTokenStore, onSaved: () -> Unit, onBack: (() -> Unit)? = null) {
    var pat by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    val hadToken = remember { tokenStore.hasToken() }
    val scope = rememberCoroutineScope()

    fun submit() {
        val token = pat.trim()
        if (token.isBlank() || loading) return
        loading = true
        error = null
        scope.launch {
            val ok = NetworkModule.validateToken(BuildConfig.GITHUB_OWNER, BuildConfig.GITHUB_REPO, token)
            if (ok) {
                tokenStore.setToken(token)
                onSaved()
            } else {
                error = "El token no sirve para ${BuildConfig.GITHUB_OWNER}/${BuildConfig.GITHUB_REPO}. Revisalo (y tu conexión) e intentá de nuevo."
            }
            loading = false
        }
    }

    SystemBarsAppearance(lightBackground = false)
    if (onBack != null) BackHandler(onBack = onBack)
    Box(
        modifier = Modifier.fillMaxSize().background(MenuColors.bg).windowInsetsPadding(WindowInsets.systemBars).padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .background(MenuColors.tile, RoundedCornerShape(14.dp))
                .border(1.5.dp, MenuColors.border, RoundedCornerShape(14.dp))
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (onBack != null) {
                TextButton(
                    onClick = onBack,
                    modifier = Modifier.align(Alignment.Start),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                ) {
                    Text("← Menú", color = MenuColors.inkSoft, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            Text("◧", color = MenuColors.ink, fontSize = 26.sp)
            Text(
                "Ajustes",
                color = MenuColors.ink,
                fontFamily = SerifFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 24.sp,
            )
            Text(
                "Tus datos viven en el repo ${BuildConfig.GITHUB_OWNER}/${BuildConfig.GITHUB_REPO}. " +
                    "Para poder guardar cambios, la app necesita un token de GitHub.",
                color = MenuColors.inkSoft,
                fontSize = 13.sp,
            )
            Text(
                "En GitHub: Settings → Developer settings → Personal access tokens → " +
                    "Fine-grained tokens → Generate new token. Elegí solo el repositorio " +
                    "${BuildConfig.GITHUB_REPO} y dale el permiso «Contents: Read and write». " +
                    "Copiá el token y pegalo acá abajo.",
                color = MenuColors.inkSoft,
                fontSize = 12.5.sp,
            )
            if (hadToken) {
                Text(
                    "Ya hay un token guardado. Pegá uno nuevo para reemplazarlo.",
                    color = MenuColors.inkSoft,
                    fontSize = 12.5.sp,
                )
            }
            OutlinedTextField(
                value = pat,
                onValueChange = { pat = it },
                label = { Text("Token de GitHub") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
                colors = settingsFieldColors(),
            )
            error?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.5.sp) }
            Button(
                onClick = ::submit,
                enabled = !loading,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MenuColors.ink, contentColor = MenuColors.bg),
            ) {
                if (loading) CircularProgressIndicator(modifier = Modifier.size(16.dp), color = MenuColors.bg, strokeWidth = 2.dp)
                else Text("Guardar", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun settingsFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = MenuColors.ink,
    unfocusedTextColor = MenuColors.ink,
    focusedBorderColor = MenuColors.ink,
    unfocusedBorderColor = MenuColors.border,
    focusedLabelColor = MenuColors.inkSoft,
    unfocusedLabelColor = MenuColors.inkSoft,
    cursorColor = MenuColors.ink,
)
