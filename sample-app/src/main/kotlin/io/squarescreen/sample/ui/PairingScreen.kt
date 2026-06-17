package io.squarescreen.sample.ui

import android.provider.Settings
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.squarescreen.core.model.PairingStatus
import io.squarescreen.player.SquareScreenPairing
import io.squarescreen.sample.data.DeviceCredentials
import kotlinx.coroutines.launch

/**
 * Pairing screen shown on first launch when no device credentials are stored.
 *
 * Flow:
 *  1. Reads Android ID as the stable OS identifier.
 *  2. Creates a [SquareScreenPairing] session and starts observing [SquareScreenPairing.pairingStatus].
 *  3. SDK calls POST /screen/register — shows a spinner while this completes.
 *  4. On pending: shows an "Awaiting admin approval" state with a manual "Check now" button.
 *  5. On approved: extracts credentials and calls [onPaired] — SDK init happens in MainActivity.
 *  6. On terminal errors: shows an appropriate message and a "Try again" button that restarts the flow.
 *
 * @param onPaired Called with [DeviceCredentials] once the device is approved.
 *   The caller is responsible for persisting credentials and initializing the SDK.
 */
@Composable
fun PairingScreen(onPaired: (DeviceCredentials) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val osIdentifier = remember {
        Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
    }

    // Restart key — incrementing this resets the pairing session for retry flows.
    var sessionKey by remember { mutableStateOf(0) }

    val pairingSession = remember(sessionKey) {
        SquareScreenPairing.create(osIdentifier)
    }

    DisposableEffect(pairingSession) {
        onDispose { pairingSession.cancel() }
    }

    val pairingStatus by pairingSession.pairingStatus.collectAsState(initial = null)

    // Navigate automatically when approved.
    LaunchedEffect(pairingStatus) {
        val status = pairingStatus
        if (status is PairingStatus.Approved) {
            onPaired(DeviceCredentials(status.deviceId, status.deviceToken))
        }
    }

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

            when (val status = pairingStatus) {
                null -> {
                    // Registering — waiting for the register call to complete.
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(32.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Connecting...",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 14.sp
                    )
                }

                PairingStatus.Pending -> {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(32.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Awaiting admin approval",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Ask your workspace admin to approve this device in the dashboard.",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                val result = pairingSession.checkPairStatus()
                                if (result is PairingStatus.Approved) {
                                    onPaired(DeviceCredentials(result.deviceId, result.deviceToken))
                                }
                            }
                        },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Text("Check now", color = Color.White, fontSize = 15.sp)
                    }
                }

                is PairingStatus.Approved -> {
                    // Handled by LaunchedEffect — show a brief confirmation while navigating.
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(32.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Device approved — launching player...",
                        color = Color.White,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                }

                PairingStatus.AlreadyPaired -> {
                    PairingErrorContent(
                        title = "Device already paired",
                        message = "This device is registered but credentials are missing locally. " +
                            "Contact your admin to re-issue credentials.",
                        onRetry = null
                    )
                }

                PairingStatus.DeviceNotFound -> {
                    PairingErrorContent(
                        title = "Device not registered",
                        message = "No device with this identifier was found. " +
                            "Ask your admin to add this device in the SquareScreen dashboard.",
                        onRetry = { sessionKey++ }
                    )
                }

                PairingStatus.Expired -> {
                    PairingErrorContent(
                        title = "Pairing window expired",
                        message = "The 10-minute approval window closed before an admin responded. " +
                            "Tap below to restart.",
                        onRetry = { sessionKey++ }
                    )
                }

                PairingStatus.InvalidToken -> {
                    PairingErrorContent(
                        title = "Invalid pairing token",
                        message = "The pairing session is no longer valid. Tap below to restart.",
                        onRetry = { sessionKey++ }
                    )
                }

                is PairingStatus.Error -> {
                    PairingErrorContent(
                        title = "Something went wrong",
                        message = status.throwable.message ?: "An unexpected error occurred.",
                        onRetry = { sessionKey++ }
                    )
                }
            }
        }
    }
}

@Composable
private fun PairingErrorContent(
    title: String,
    message: String,
    onRetry: (() -> Unit)?
) {
    Text(
        text = title,
        color = Color(0xFFFF3B30),
        fontSize = 18.sp,
        fontWeight = FontWeight.SemiBold,
        textAlign = TextAlign.Center
    )
    Spacer(modifier = Modifier.height(12.dp))
    Text(
        text = message,
        color = Color.White.copy(alpha = 0.6f),
        fontSize = 14.sp,
        textAlign = TextAlign.Center
    )
    if (onRetry != null) {
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onRetry,
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) {
            Text("Try again", color = Color.Black, fontSize = 16.sp, fontWeight = FontWeight.Medium)
        }
    }
}
