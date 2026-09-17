package com.rskickbox.app

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun RsLiveBackground(c: RsPalette, s: RsStore, scope: BgScope, content: @Composable BoxScope.() -> Unit) {
    val context = LocalContext.current
    val style = runCatching {
        BgStyle.valueOf(s.s("bg_${scope.name}", if (scope == BgScope.LOGIN) "CINEMATIC_RING" else "ARENA_LIGHTS"))
    }.getOrDefault(BgStyle.CINEMATIC_RING)
    val pos = runCatching { BgPos.valueOf(s.s("pos_${scope.name}", "CENTER")) }.getOrDefault(BgPos.CENTER)
    val intensity = s.f("int_${scope.name}", if (scope == BgScope.LOGIN) .85f else .35f)
    val customUri = s.s("custom_uri_${scope.name}", "")
    var customBitmap by remember(customUri) { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(customUri) {
        customBitmap = if (customUri.isBlank()) null else runCatching {
            context.contentResolver.openInputStream(Uri.parse(customUri))?.use { input ->
                BitmapFactory.decodeStream(input)?.asImageBitmap()
            }
        }.getOrNull()
    }

    val tr = rememberInfiniteTransition(label = "rs-bg")
    val m by tr.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(if (scope == BgScope.LOGIN) 7000 else 12000, easing = LinearEasing), RepeatMode.Reverse),
        label = "move"
    )

    Box(
        Modifier.fillMaxSize()
            .background(Brush.verticalGradient(listOf(c.bg, c.panel2, c.bg)))
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        customBitmap?.let { bitmap ->
            val align = when (pos) {
                BgPos.LEFT -> Alignment.CenterStart
                BgPos.RIGHT -> Alignment.CenterEnd
                BgPos.TOP -> Alignment.TopCenter
                BgPos.BOTTOM -> Alignment.BottomCenter
                else -> Alignment.Center
            }
            Image(
                bitmap = bitmap,
                contentDescription = "Custom app background",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                alignment = align,
                alpha = (.35f + intensity * .55f).coerceIn(.35f, .95f)
            )
        }

        Canvas(Modifier.fillMaxSize()) {
            val anchor = when (pos) {
                BgPos.LEFT -> Offset(size.width * .25f, size.height * .55f)
                BgPos.RIGHT -> Offset(size.width * .75f, size.height * .55f)
                BgPos.TOP -> Offset(size.width * .5f, size.height * .28f)
                BgPos.BOTTOM -> Offset(size.width * .5f, size.height * .72f)
                else -> Offset(size.width * .5f, size.height * .52f)
            }
            val base = when (style) {
                BgStyle.RED_CORNER -> Color(0xFFC43A35)
                BgStyle.ARENA_LIGHTS -> Color(0xFFE8DFC9)
                else -> c.gold
            }
            drawCircle(
                base.copy(alpha = .05f + .20f * intensity),
                size.minDimension * (.26f + .07f * m),
                Offset(anchor.x + size.width * .12f * (m - .5f), anchor.y - size.height * .18f)
            )
            if (customBitmap == null && style != BgStyle.MINIMAL_DARK) {
                repeat(3) { i ->
                    drawLine(
                        c.bright.copy(alpha = .05f * intensity),
                        Offset(0f, size.height * (.43f + i * .045f)),
                        Offset(size.width, size.height * (.45f + i * .045f)),
                        4f
                    )
                }
                val left = Offset(anchor.x - size.width * .12f, anchor.y)
                val right = Offset(anchor.x + size.width * .12f, anchor.y)
                fun fighter(o: Offset, flip: Float) {
                    drawCircle(Color.Black.copy(alpha = .88f), size.minDimension * .035f, Offset(o.x, o.y - size.height * .11f))
                    drawLine(Color.Black, Offset(o.x, o.y - size.height * .07f), Offset(o.x, o.y + size.height * .05f), 22f)
                    drawLine(Color.Black, Offset(o.x, o.y - size.height * .03f), Offset(o.x + flip * size.width * .10f, o.y - size.height * .08f), 18f)
                    drawLine(Color.Black, Offset(o.x, o.y + size.height * .04f), Offset(o.x - flip * size.width * .05f, o.y + size.height * .15f), 20f)
                    drawLine(Color.Black, Offset(o.x, o.y + size.height * .04f), Offset(o.x + flip * size.width * .07f, o.y + size.height * .14f), 20f)
                }
                fighter(left, 1f); fighter(right, -1f)
            }
        }
        val scrim = if (scope == BgScope.LOGIN) .42f else if (customBitmap != null) .76f else .72f
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = scrim)))
        content()
    }
}

@Composable
fun RsBackgroundStudio(c: RsPalette, s: RsStore) {
    val context = LocalContext.current
    var scope by remember { mutableStateOf(BgScope.LOGIN) }
    var revision by remember { mutableIntStateOf(0) }
    var customUri by remember(scope, revision) { mutableStateOf(s.s("custom_uri_${scope.name}", "")) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            s.ps("custom_uri_${scope.name}", uri.toString())
            customUri = uri.toString()
            revision++
        }
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Background Studio", color = c.bright, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
        Text("Control every app background, placement and intensity, or upload your own image from this device or connected cloud library.", color = c.muted)

        RsPanel(c) {
            Text("PAGE GROUP", color = c.bright, fontWeight = FontWeight.Bold)
            BgScope.entries.forEach { x -> FilterChip(selected = scope == x, onClick = { scope = x }, label = { Text(x.name.replace('_', ' '), fontSize = 10.sp) }) }
        }

        RsPanel(c) {
            Text("CUSTOM BACKGROUND", color = c.bright, fontWeight = FontWeight.Bold)
            Text("Choose an image from phone, tablet, Chromebook/PC file provider, Google Drive or another connected document library.", color = c.muted, fontSize = 11.sp)
            Button(onClick = { picker.launch(arrayOf("image/*")) }, modifier = Modifier.fillMaxWidth()) { Text("＋ Upload from device / library") }
            if (customUri.isNotBlank()) {
                Text("Custom background active for ${scope.name.replace('_',' ')}", color = c.text, fontSize = 11.sp)
                OutlinedButton(onClick = { s.ps("custom_uri_${scope.name}", ""); customUri = ""; revision++ }, modifier = Modifier.fillMaxWidth()) { Text("Remove custom background") }
            }
            Text("Tip: use high-resolution portrait or landscape artwork. Position and intensity controls below also apply to uploaded images.", color = c.muted, fontSize = 10.sp)
        }

        RsPanel(c) {
            Text("BUILT-IN LIVE STYLE", color = c.bright, fontWeight = FontWeight.Bold)
            BgStyle.entries.forEach { x ->
                val active = runCatching { BgStyle.valueOf(s.s("bg_${scope.name}", "CINEMATIC_RING")) }.getOrDefault(BgStyle.CINEMATIC_RING) == x
                FilterChip(selected = active, onClick = { s.ps("bg_${scope.name}", x.name); revision++ }, label = { Text(x.name.replace('_', ' ')) })
            }
        }

        RsPanel(c) {
            Text("POSITION", color = c.bright, fontWeight = FontWeight.Bold)
            BgPos.entries.forEach { x ->
                val active = runCatching { BgPos.valueOf(s.s("pos_${scope.name}", "CENTER")) }.getOrDefault(BgPos.CENTER) == x
                FilterChip(selected = active, onClick = { s.ps("pos_${scope.name}", x.name); revision++ }, label = { Text(x.name) })
            }
        }

        RsPanel(c) {
            var v by remember(scope) { mutableFloatStateOf(s.f("int_${scope.name}", if (scope == BgScope.LOGIN) .85f else .35f)) }
            Text("VISUAL INTENSITY ${(v * 100).toInt()}%", color = c.bright, fontWeight = FontWeight.Bold)
            Slider(v, { value -> v = value; s.pf("int_${scope.name}", value) }, valueRange = .05f..1f)
            Text("For training pages, 25–45% keeps timers and technique text easy to read.", color = c.muted, fontSize = 11.sp)
        }
        Spacer(Modifier.height(20.dp))
    }
}
