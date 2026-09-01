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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import io.squarescreen.sample.R
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.squarescreen.core.logging.SquareScreenDebugLogger
import io.squarescreen.core.model.PairingStatus
import io.squarescreen.player.SquareScreenPairing
import io.squarescreen.sample.data.DeviceCredentials
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

private enum class PairingMode { REGISTER, ACTIVATE }

@Composable
fun PairingScreen(onPaired: (DeviceCredentials) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val androidId = remember {
        Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
    }

    var mode by remember { mutableStateOf(PairingMode.REGISTER) }

    // Register path — session created only when user presses "Pair this device".
    val registerSessionState = remember { mutableStateOf<SquareScreenPairing?>(null) }
    val registerSession = registerSessionState.value

    DisposableEffect(registerSession) {
        onDispose { registerSession?.cancel() }
    }

    val registerStatus by (registerSession?.pairingStatus ?: flowOf(null))
        .collectAsState(initial = null)

    LaunchedEffect(registerStatus) {
        if (registerStatus is PairingStatus.Approved) {
            val s = registerStatus as PairingStatus.Approved
            onPaired(DeviceCredentials(s.deviceId, s.deviceToken))
        }
    }

    val startRegisterSession = {
        registerSessionState.value = SquareScreenPairing.create(context = context, osIdentifier = androidId, logger = SquareScreenDebugLogger())
    }

    val restartRegisterSession = {
        registerSessionState.value = SquareScreenPairing.create(context = context, osIdentifier = androidId, logger = SquareScreenDebugLogger())
    }

    // Activate path — session created when user submits a device ID.
    val activateSessionState = remember { mutableStateOf<SquareScreenPairing?>(null) }
    val activateSession = activateSessionState.value

    DisposableEffect(activateSession) {
        onDispose { activateSession?.cancel() }
    }

    val activateStatus by (activateSession?.pairingStatus ?: flowOf(null))
        .collectAsState(initial = null)

    LaunchedEffect(activateStatus) {
        if (activateStatus is PairingStatus.Approved) {
            val s = activateStatus as PairingStatus.Approved
            onPaired(DeviceCredentials(s.deviceId, s.deviceToken))
        }
    }

    PairingScaffold {
        // Mode switcher
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            PairingMode.entries.forEach { m ->
                val selected = mode == m
                Button(
                    onClick = { mode = m },
                    shape = RoundedCornerShape(6.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selected) Color.White else Color.Transparent,
                        contentColor = if (selected) Color.Black else Color.White.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.weight(1f).height(38.dp)
                ) {
                    Text(
                        text = if (m == PairingMode.REGISTER) "Register" else "Activate",
                        fontSize = 14.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        when (mode) {
            PairingMode.REGISTER -> RegisterContent(
                status = registerStatus,
                sessionStarted = registerSession != null,
                osIdentifier = androidId,
                onStart = startRegisterSession,
                onRetry = restartRegisterSession,
                onCheckNow = {
                    scope.launch {
                        val result = registerSession?.checkPairStatus() ?: return@launch
                        if (result is PairingStatus.Approved) {
                            onPaired(DeviceCredentials(result.deviceId, result.deviceToken))
                        }
                    }
                }
            )

            PairingMode.ACTIVATE -> ActivateContent(
                status = activateStatus,
                onActivate = { deviceId ->
                    activateSessionState.value = SquareScreenPairing.createWithActivation(
                        context = context,
                        deviceId = deviceId,
                        deviceToken = androidId,
                        logger = SquareScreenDebugLogger()
                    )
                },
                onRetry = { activateSessionState.value = null },
                onCheckNow = {
                    scope.launch {
                        val result = activateSession?.checkPairStatus() ?: return@launch
                        if (result is PairingStatus.Approved) {
                            onPaired(DeviceCredentials(result.deviceId, result.deviceToken))
                        }
                    }
                }
            )
        }
    }
}

@Composable
private fun RegisterContent(
    status: PairingStatus?,
    sessionStarted: Boolean,
    osIdentifier: String,
    onStart: () -> Unit,
    onRetry: () -> Unit,
    onCheckNow: () -> Unit
) {
    // Not yet started — show identifier and let the user kick off pairing.
    if (!sessionStarted) {
        RegisterLandingContent(osIdentifier = osIdentifier, onStart = onStart)
        return
    }

    when (val s = status) {
        null -> {
            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(32.dp), strokeWidth = 2.dp)
            Spacer(modifier = Modifier.height(16.dp))
            Text("Connecting...", color = Color.White.copy(alpha = 0.6f), fontSize = 14.sp)
        }

        PairingStatus.Pending -> PendingContent(onCheckNow = onCheckNow)

        is PairingStatus.Approved -> {
            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(32.dp), strokeWidth = 2.dp)
            Spacer(modifier = Modifier.height(16.dp))
            Text("Device approved — launching player...", color = Color.White, fontSize = 14.sp, textAlign = TextAlign.Center)
        }

        PairingStatus.AlreadyPaired -> {
            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(32.dp), strokeWidth = 2.dp)
            Spacer(modifier = Modifier.height(16.dp))
            Text("Re-pairing device...", color = Color.White, fontSize = 14.sp, textAlign = TextAlign.Center)
        }

        PairingStatus.DeviceNotFound -> PairingErrorContent(
            title = "Device not found",
            message = "This device identifier isn't registered in the dashboard. Add it there first, then try again.",
            onRetry = onRetry
        )

        PairingStatus.Expired -> PairingErrorContent(
            title = "Pairing window expired",
            message = "The approval window closed before an admin responded. Tap below to restart.",
            onRetry = onRetry
        )

        PairingStatus.InvalidToken -> PairingErrorContent(
            title = "Invalid pairing token",
            message = "The pairing session is no longer valid. Tap below to restart.",
            onRetry = onRetry
        )

        is PairingStatus.Error -> PairingErrorContent(
            title = "Something went wrong",
            message = s.throwable.message ?: "An unexpected error occurred.",
            onRetry = onRetry
        )

        PairingStatus.IdentifierMismatch -> PairingErrorContent(
            title = "Something went wrong",
            message = "An unexpected error occurred.",
            onRetry = onRetry
        )
    }
}

@Composable
private fun RegisterLandingContent(osIdentifier: String, onStart: () -> Unit) {
    val context = LocalContext.current
    var copied by remember { mutableStateOf(false) }

    Text(
        "Pair this device",
        color = Color.White,
        fontSize = 16.sp,
        fontWeight = FontWeight.Medium,
        textAlign = TextAlign.Center
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        "Your device identifier is shown below. Add it to your SquareScreen dashboard, then tap Pair.",
        color = Color.White.copy(alpha = 0.5f),
        fontSize = 13.sp,
        textAlign = TextAlign.Center
    )
    Spacer(modifier = Modifier.height(20.dp))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            osIdentifier,
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
        onClick = onStart,
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color.White),
        modifier = Modifier.fillMaxWidth().height(52.dp)
    ) {
        Text("Pair this device", color = Color.Black, fontSize = 16.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun ActivateContent(
    status: PairingStatus?,
    onActivate: (deviceId: String) -> Unit,
    onRetry: () -> Unit,
    onCheckNow: () -> Unit
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    var deviceId by remember { mutableStateOf("") }

    if (status == null) {
        Text(
            "Enter your device ID",
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Your device ID is generated in the SquareScreen admin dashboard.",
            color = Color.White.copy(alpha = 0.5f),
            fontSize = 13.sp,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(20.dp))

        OutlinedTextField(
            value = deviceId,
            onValueChange = { deviceId = it.uppercase().take(8) },
            placeholder = { Text("e.g. AB12CD34", color = Color.White.copy(alpha = 0.3f), fontFamily = FontFamily.Monospace) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = {
                keyboardController?.hide()
                if (deviceId.length == 8) onActivate(deviceId)
            }),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Color.White.copy(alpha = 0.6f),
                unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                cursorColor = Color.White
            ),
            textStyle = androidx.compose.ui.text.TextStyle(
                fontFamily = FontFamily.Monospace,
                fontSize = 18.sp,
                letterSpacing = 4.sp
            ),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                keyboardController?.hide()
                onActivate(deviceId)
            },
            enabled = deviceId.length == 8,
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.White,
                disabledContainerColor = Color.White.copy(alpha = 0.2f)
            ),
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) {
            Text(
                "Activate",
                color = if (deviceId.length == 8) Color.Black else Color.White.copy(alpha = 0.4f),
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
        }
        return
    }

    when (val s = status) {
        PairingStatus.Pending -> PendingContent(onCheckNow = onCheckNow)

        is PairingStatus.Approved -> {
            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(32.dp), strokeWidth = 2.dp)
            Spacer(modifier = Modifier.height(16.dp))
            Text("Device approved — launching player...", color = Color.White, fontSize = 14.sp, textAlign = TextAlign.Center)
        }

        PairingStatus.AlreadyPaired -> {
            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(32.dp), strokeWidth = 2.dp)
            Spacer(modifier = Modifier.height(16.dp))
            Text("Re-pairing device...", color = Color.White, fontSize = 14.sp, textAlign = TextAlign.Center)
        }

        PairingStatus.DeviceNotFound -> PairingErrorContent(
            title = "Device ID not found",
            message = "No device with that ID exists in the dashboard. Check the ID and try again.",
            onRetry = onRetry
        )

        PairingStatus.IdentifierMismatch -> PairingErrorContent(
            title = "Identifier mismatch",
            message = "This device ID is bound to a different token. Contact your admin to re-issue the device ID.",
            onRetry = onRetry
        )

        PairingStatus.Expired -> PairingErrorContent(
            title = "Pairing window expired",
            message = "The approval window closed. Tap below to try again.",
            onRetry = onRetry
        )

        PairingStatus.InvalidToken -> PairingErrorContent(
            title = "Invalid pairing token",
            message = "The pairing session is no longer valid. Tap below to restart.",
            onRetry = onRetry
        )

        is PairingStatus.Error -> PairingErrorContent(
            title = "Something went wrong",
            message = s.throwable.message ?: "An unexpected error occurred.",
            onRetry = onRetry
        )

        null -> {}
    }
}

@Composable
private fun PendingContent(onCheckNow: () -> Unit) {
    val scope = rememberCoroutineScope()
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
        onClick = { scope.launch { onCheckNow() } },
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth().height(48.dp)
    ) {
        Text("Check now", color = Color.White, fontSize = 15.sp)
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
            androidx.compose.foundation.Image(
                painter = painterResource(R.drawable.ic_squarescreen_logo),
                contentDescription = "SquareScreen",
                modifier = Modifier.height(36.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
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
private fun PairingErrorContent(title: String, message: String, onRetry: (() -> Unit)?) {
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
