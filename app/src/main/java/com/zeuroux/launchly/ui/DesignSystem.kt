package com.zeuroux.launchly.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.zeuroux.launchly.R

object LaunchlyDesign {
    val Accent = Color(0xFFFF223C)
    val ScreenBackground = Color.Black
    val SurfacePrimary = Color(0xFF1B1518)
    val SurfaceRaised = Color(0xFF332C30)
    val SurfaceDisabled = Color(0xFF4A3037)
    val NavigationSurface = Color(0xFF2D2529)
    val SecondaryButton = Color(0xFF695055)
    val ProgressTrack = Color(0xFF30262A)
    val PromptSurface = Color(0xFF1A1417)
    val Divider = Color(0xFF33272D)
    val TextPrimary = Color.White
    val TextSecondary = Color(0xFFE2D8DC)
    val TextMuted = Color(0xFFD9CDD2)
    val TextTertiary = Color(0xFFB89CA3)
    val Error = Color(0xFFFF4A5F)
    val Overlay = Color(0xD90B0B0D)
    val Scrim = Color(0x8C000000)

    val PanelShape = RoundedCornerShape(20.dp)
    val ControlShape = RoundedCornerShape(16.dp)
    val CardShape = RoundedCornerShape(8.dp)
}

@Composable
fun LaunchlyBackdrop(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(modifier.fillMaxSize().background(LaunchlyDesign.ScreenBackground)) {
        Image(
            painter = painterResource(R.drawable.launcher_background),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        Box(Modifier.fillMaxSize().background(LaunchlyDesign.Overlay))
        content()
    }
}

@Composable
fun LaunchlyBrandMark(modifier: Modifier = Modifier, size: Int = 50) {
    Box(
        modifier = modifier
            .size(size.dp)
            .clip(LaunchlyDesign.CardShape)
            .background(LaunchlyDesign.Accent),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_launcher_foreground),
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size((size * 0.72f).dp)
        )
    }
}

@Composable
fun LaunchlyBrandRow(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        LaunchlyBrandMark()
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.titleLarge,
            color = LaunchlyDesign.TextPrimary
        )
    }
}

@Composable
fun LaunchlyPageTitle(
    @StringRes title: Int,
    modifier: Modifier = Modifier
) {
    Column(modifier.width(IntrinsicSize.Max)) {
        Text(
            text = stringResource(title),
            color = LaunchlyDesign.TextPrimary,
            fontSize = 22.sp,
            lineHeight = 28.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(5.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(3.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(LaunchlyDesign.Accent)
        )
    }
}

@Composable
fun LaunchlySectionTitle(text: String, modifier: Modifier = Modifier) {
    Column(modifier.width(IntrinsicSize.Max)) {
        Text(
            text = text,
            color = LaunchlyDesign.TextPrimary,
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(Modifier.height(4.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(3.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(LaunchlyDesign.Accent)
        )
    }
}

@Composable
fun LaunchlyRaisedCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier,
        color = LaunchlyDesign.SurfaceRaised,
        contentColor = LaunchlyDesign.TextPrimary,
        shape = LaunchlyDesign.CardShape
    ) {
        Column(content = content)
    }
}

@Composable
fun LaunchlyPrimaryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(50.dp),
        enabled = enabled,
        shape = LaunchlyDesign.ControlShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = LaunchlyDesign.Accent,
            contentColor = Color.White,
            disabledContainerColor = LaunchlyDesign.SurfaceDisabled,
            disabledContentColor = LaunchlyDesign.TextTertiary
        ),
        content = content
    )
}

@Composable
fun LaunchlySecondaryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(50.dp),
        enabled = enabled,
        shape = LaunchlyDesign.ControlShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = LaunchlyDesign.SecondaryButton,
            contentColor = Color.White,
            disabledContainerColor = LaunchlyDesign.SurfaceDisabled,
            disabledContentColor = LaunchlyDesign.TextTertiary
        ),
        content = content
    )
}

@Composable
fun LaunchlySettingsButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    primary: Boolean = true,
    enabled: Boolean = true,
    content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier.widthIn(min = 210.dp).height(46.dp),
        enabled = enabled,
        shape = LaunchlyDesign.CardShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (primary) LaunchlyDesign.Accent else LaunchlyDesign.SecondaryButton,
            contentColor = Color.White,
            disabledContainerColor = LaunchlyDesign.SurfaceDisabled,
            disabledContentColor = LaunchlyDesign.TextTertiary
        ),
        content = content
    )
}

@Composable
fun LaunchlyProgress(
    progress: Float?,
    modifier: Modifier = Modifier
) {
    if (progress == null) {
        LinearProgressIndicator(
            modifier = modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(4.dp)),
            color = LaunchlyDesign.Accent,
            trackColor = LaunchlyDesign.ProgressTrack
        )
    } else {
        LinearProgressIndicator(
            progress = { progress.coerceIn(0f, 1f) },
            modifier = modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(4.dp)),
            color = LaunchlyDesign.Accent,
            trackColor = LaunchlyDesign.ProgressTrack
        )
    }
}

@Composable
fun launchlyTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = LaunchlyDesign.NavigationSurface,
    unfocusedContainerColor = LaunchlyDesign.NavigationSurface,
    disabledContainerColor = LaunchlyDesign.SurfaceDisabled,
    focusedBorderColor = LaunchlyDesign.Accent,
    unfocusedBorderColor = Color.Transparent,
    focusedTextColor = LaunchlyDesign.TextPrimary,
    unfocusedTextColor = LaunchlyDesign.TextPrimary,
    cursorColor = LaunchlyDesign.Accent,
    focusedLabelColor = LaunchlyDesign.TextSecondary,
    unfocusedLabelColor = LaunchlyDesign.TextTertiary
)

@Composable
fun LaunchlyPrompt(
    onDismissRequest: () -> Unit,
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize().background(LaunchlyDesign.Scrim)
                .windowInsetsPadding(WindowInsets.safeDrawing),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier.widthIn(max = 420.dp).fillMaxWidth(0.84f).padding(horizontal = 24.dp),
                color = LaunchlyDesign.PromptSurface,
                contentColor = LaunchlyDesign.TextPrimary,
                shape = LaunchlyDesign.PanelShape,
                tonalElevation = 0.dp
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        textAlign = TextAlign.Center
                    )
                    content()
                }
            }
        }
    }
}

@Composable
fun LaunchlyDialog(
    onDismissRequest: () -> Unit,
    title: String,
    message: String,
    confirmText: String,
    onConfirm: () -> Unit,
    dismissText: String? = null,
    destructive: Boolean = false,
    onDismiss: (() -> Unit)? = null
) {
    LaunchlyPrompt(onDismissRequest, title) {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyLarge,
                    color = LaunchlyDesign.TextMuted,
                    textAlign = TextAlign.Center
                )
                Button(
                    onClick = onConfirm,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = LaunchlyDesign.ControlShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (destructive) LaunchlyDesign.Error else LaunchlyDesign.Accent,
                        contentColor = Color.White
                    )
                ) {
                    Text(confirmText)
                }
                if (dismissText != null) {
                    TextButton(
                        onClick = onDismiss ?: onDismissRequest,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        colors = ButtonDefaults.textButtonColors(contentColor = LaunchlyDesign.TextSecondary)
                    ) {
                        Text(dismissText)
                    }
                }
    }
}

@Composable
fun LaunchlyLoadingScreen(modifier: Modifier = Modifier) {
    LaunchlyBackdrop(modifier) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            LaunchlyBrandMark(size = 64)
            Spacer(Modifier.height(22.dp))
            CircularProgressIndicator(
                color = LaunchlyDesign.Accent,
                trackColor = LaunchlyDesign.ProgressTrack,
                strokeWidth = 4.dp
            )
            Spacer(Modifier.height(14.dp))
            Text(
                text = stringResource(R.string.loading),
                color = LaunchlyDesign.TextMuted,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}
