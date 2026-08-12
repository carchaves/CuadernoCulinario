package dev.raflos.cocina.ui.login

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
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
import dev.raflos.cocina.data.remote.LoginRequest
import dev.raflos.cocina.data.remote.NetworkModule
import dev.raflos.cocina.data.remote.TokenStore
import dev.raflos.cocina.ui.SystemBarsAppearance
import dev.raflos.cocina.ui.theme.MenuColors
import dev.raflos.cocina.ui.theme.SerifFamily
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(tokenStore: TokenStore, onSuccess: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val network = remember { NetworkModule(tokenStore) }

    fun submit() {
        if (email.isBlank() || password.isBlank() || loading) return
        loading = true
        error = null
        scope.launch {
            try {
                val result = network.authApi.login(LoginRequest(email.trim(), password))
                tokenStore.set(result.accessToken, result.refreshToken)
                onSuccess()
            } catch (e: Exception) {
                error = "Email o contraseña incorrectos."
            } finally {
                loading = false
            }
        }
    }

    SystemBarsAppearance(lightBackground = false)
    Box(
        modifier = Modifier.fillMaxSize().background(MenuColors.bg).windowInsetsPadding(WindowInsets.systemBars).padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MenuColors.tile, RoundedCornerShape(14.dp))
                .border(1.5.dp, MenuColors.border, RoundedCornerShape(14.dp))
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("◧", color = MenuColors.ink, fontSize = 26.sp)
            Text(
                "Cocina App",
                color = MenuColors.ink,
                fontFamily = SerifFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 24.sp,
            )
            Text(
                "Iniciá sesión para ver tu despensa, recetas y lista de compra.",
                color = MenuColors.inkSoft,
                fontSize = 13.sp,
            )
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth(),
                colors = loginFieldColors(),
            )
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Contraseña") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
                colors = loginFieldColors(),
            )
            error?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.5.sp) }
            Button(
                onClick = ::submit,
                enabled = !loading,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MenuColors.ink, contentColor = MenuColors.bg),
            ) {
                if (loading) CircularProgressIndicator(modifier = Modifier.size(16.dp), color = MenuColors.bg, strokeWidth = 2.dp)
                else Text("Entrar", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun loginFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = MenuColors.ink,
    unfocusedTextColor = MenuColors.ink,
    focusedBorderColor = MenuColors.ink,
    unfocusedBorderColor = MenuColors.border,
    focusedLabelColor = MenuColors.inkSoft,
    unfocusedLabelColor = MenuColors.inkSoft,
    cursorColor = MenuColors.ink,
)
