package io.squarescreen.sample.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.text.font.FontFamily
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
 * On launch it immediately fires the register call to detect the device's state:
 *  - If the device is registered in the dashboard → goes straight to "Awaiting approval".
 *  - If the device is not yet registered (404) → shows the identifier so the admin
 *    can add it, then lets the user retry.
 *  - If already paired (409) → shows an appropriate message.
 *
 * @param onPaired Called with [DeviceCredentials] once the device is approved.
 */
@Composable
fun PairingScreen(onPaired: (DeviceCredentials) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val osIdentifier = remember {
        Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
    }

    // Session is created immediately on launch — auto-checks whether the device is
    // already registered so we skip the pre-pairing screen when it is.
    //
    // We read the state value into a local val so DisposableEffect closes over the
    // instance (not the state). Without this, onDispose reads the *new* session from
    // the state and cancels it before its register call fires.
    val sessionState = remember { mutableStateOf(SquareScreenPairing.create(context, osIdentifier)) }
    val session = sessionState.value

    DisposableEffect(session) {
        onDispose { session.cancel() }
    }

    val status by session.pairingStatus.collectAsState(initial = null)

    LaunchedEffect(status) {
        val s = status
        if (s is PairingStatus.Approved) {
            onPaired(DeviceCredentials(s.deviceId, s.deviceToken))
        }
    }

    val restartSession = { sessionState.value = SquareScreenPairing.create(context, osIdentifier) }

    PairingScaffold {
        when (val s = status) {
            // Register call in-flight — brief check on launch.
            null -> {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(32.dp), strokeWidth = 2.dp)
                Spacer(modifier = Modifier.height(16.dp))
                Text("Checking...", color = Color.White.copy(alpha = 0.6f), fontSize = 14.sp)
            }

            // Device not yet in the dashboard — show identifier so admin can add it,
            // then the user taps "Pair this device" to retry.
            PairingStatus.DeviceNotFound -> {
                NotRegisteredContent(
                    osIdentifier = osIdentifier,
                    onRetry = restartSession
                )
            }

            // Device is registered and we're waiting for admin approval.
            PairingStatus.Pending -> {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(32.dp), strokeWidth = 2.dp)
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Awaiting admin approval",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Ask your workspace admin to approve this device in the dashboard.",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            val result = session.checkPairStatus()
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

            // Brief transition state while MainActivity switches to PlayerScreen.
            is PairingStatus.Approved -> {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(32.dp), strokeWidth = 2.dp)
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Device approved — launching player...",
                    color = Color.White,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
            }

            PairingStatus.AlreadyPaired -> PairingErrorContent(
                title = "Device already paired",
                message = "This device is registered but credentials are missing locally. Contact your admin to re-issue credentials.",
                onRetry = null
            )

            PairingStatus.Expired -> PairingErrorContent(
                title = "Pairing window expired",
                message = "The 10-minute approval window closed before an admin responded. Tap below to restart.",
                onRetry = restartSession
            )

            PairingStatus.InvalidToken -> PairingErrorContent(
                title = "Invalid pairing token",
                message = "The pairing session is no longer valid. Tap below to restart.",
                onRetry = restartSession
            )

            is PairingStatus.Error -> PairingErrorContent(
                title = "Something went wrong",
                message = s.throwable.message ?: "An unexpected error occurred.",
                onRetry = restartSession
            )
        }
    }
}

/** Shown when the server returns 404 — device hasn't been added to the dashboard yet. */
@Composable
private fun NotRegisteredContent(
    osIdentifier: String,
    onRetry: () -> Unit
) {
    val context = LocalContext.current
    var copied by remember { mutableStateOf(false) }
    var isPairing by remember { mutableStateOf(false) }

    Text(
        "Add this device to your dashboard",
        color = Color.White,
        fontSize = 16.sp,
        fontWeight = FontWeight.Medium,
        textAlign = TextAlign.Center
    )

    Spacer(modifier = Modifier.height(8.dp))

    Text(
        "Enter the identifier below in your SquareScreen dashboard, then tap Pair.",
        color = Color.White.copy(alpha = 0.5f),
        fontSize = 13.sp,
        textAlign = TextAlign.Center
    )

    Spacer(modifier = Modifier.height(20.dp))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.2f),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = osIdentifier,
            color = Color.White,
            fontSize = 13.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.weight(1f)
        )
        OutlinedButton(
            onClick = {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("Device identifier", osIdentifier))
                copied = true
            },
            shape = RoundedCornerShape(6.dp),
            modifier = Modifier.padding(start = 12.dp)
        ) {
            Text(
                text = if (copied) "Copied" else "Copy",
                color = if (copied) Color(0xFF34C759) else Color.White,
                fontSize = 13.sp
            )
        }
    }

    Spacer(modifier = Modifier.height(32.dp))

    Button(
        onClick = {
            isPairing = true
            onRetry()
        },
        enabled = !isPairing,
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color.White),
        modifier = Modifier.fillMaxWidth().height(52.dp)
    ) {
        if (isPairing) {
            CircularProgressIndicator(
                color = Color.Black,
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp
            )
        } else {
            Text("Pair this device", color = Color.Black, fontSize = 16.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun PairingScaffold(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("SquareScreen", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Pair this device to your workspace",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 16.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(48.dp))
            content()
        }
    }
}

@Composable
private fun PairingErrorContent(
    title: String,
    message: String,
    onRetry: (() -> Unit)?
) {
    Text(title, color = Color(0xFFFF3B30), fontSize = 18.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
    Spacer(modifier = Modifier.height(12.dp))
    Text(message, color = Color.White.copy(alpha = 0.6f), fontSize = 14.sp, textAlign = TextAlign.Center)
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
