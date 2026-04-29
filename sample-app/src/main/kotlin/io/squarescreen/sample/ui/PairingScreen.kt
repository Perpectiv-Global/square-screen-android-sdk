package io.squarescreen.sample.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.squarescreen.sample.data.DeviceCredentials
import kotlinx.coroutines.launch

/**
 * Pairing screen shown on first launch when no device credentials are stored.
 *
 * In production this screen would:
 *   1. Auto-generate a stable device UUID (stored in SharedPreferences, not encrypted —
 *      device IDs are not secret).
 *   2. Call your backend's device registration endpoint with the device UUID and any
 *      other identifiers (serial number, model, etc.).
 *   3. Receive a device token from the backend.
 *   4. Call [onPaired] with the credentials.
 *
 * This sample shows manual entry as a stand-in for that flow, since the
 * registration endpoint spec is still being finalised. Replace the manual entry
 * fields with your actual registration call when the endpoint is ready.
 *
 * @param onPaired Called with the obtained [DeviceCredentials] after successful pairing.
 */
@Composable
fun PairingScreen(onPaired: (DeviceCredentials) -> Unit) {
    val scope = rememberCoroutineScope()

    var deviceId by remember { mutableStateOf("") }
    var deviceToken by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "SquareScreen",
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Pair this device to your workspace",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 16.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(48.dp))

            // -------------------------------------------------------------------
            // TODO: Replace these manual input fields with an automatic registration
            // call once the screen/register endpoint is available.
            //
            // The typical automatic flow:
            //   val deviceUuid = getOrCreateStableDeviceUuid(context)
            //   val response = api.registerDevice(deviceUuid, Build.MODEL, Build.SERIAL)
            //   onPaired(DeviceCredentials(deviceUuid, response.token))
            // -------------------------------------------------------------------

            OutlinedTextField(
                value = deviceId,
                onValueChange = { deviceId = it.trim() },
                label = { Text("Device ID", color = Color.White.copy(alpha = 0.7f)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = deviceToken,
                onValueChange = { deviceToken = it.trim() },
                label = { Text("Device Token", color = Color.White.copy(alpha = 0.7f)) },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth()
            )

            errorMessage?.let { msg ->
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = msg,
                    color = Color(0xFFFF3B30),
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    if (deviceId.isBlank() || deviceToken.isBlank()) {
                        errorMessage = "Device ID and token are required"
                        return@Button
                    }
                    errorMessage = null
                    isLoading = true
                    scope.launch {
                        // TODO: validate credentials against the server here
                        // before calling onPaired — e.g. make a test API call
                        // and only proceed if it succeeds.
                        onPaired(DeviceCredentials(deviceId, deviceToken))
                        isLoading = false
                    }
                },
                enabled = !isLoading,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Pair Device", fontSize = 16.sp)
                }
            }
        }
    }
}
