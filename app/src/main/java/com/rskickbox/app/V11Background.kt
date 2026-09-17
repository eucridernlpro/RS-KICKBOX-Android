package com.rskickbox.app

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun RsLiveBackground(c: RsPalette, s: RsStore, scope: BgScope, content: @Composable BoxScope.() -> Unit) {
    val style = runCatching {
        BgStyle.valueOf(s.s("bg_${scope.name}", if (scope == BgScope.LOGIN) "CINEMATIC_RING" else "ARENA_LIGHTS"))
    }.getOrDefault(BgStyle.CINEMATIC_RING)
    val pos = runCatching { BgPos.valueOf(s.s("pos_${scope.name}", "CENTER")) }.getOrDefault(BgPos.CENTER)
    val intensity = s.f("int_${scope.name}", if (scope == BgScope.LOGIN) .85f else .35f)
    val tr = rememberInfiniteTransition(label = "rs-bg")
    val m by tr.animateFloat(
        0f,
        1f,
        infiniteRepeatable(tween(if (scope == BgScope.LOGIN) 7000 else 12000, easing = LinearEasing), RepeatMode.Reverse),
        label = "move"
    )

    Box(
        Modifier.fillMaxSize()
            .background(Brush.verticalGradient(listOf(c.bg, c.panel2, c.bg)))
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
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
            if (style != BgStyle.MINIMAL_DARK) {
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
                fighter(left, 1f)
                fighter(right, -1f)
            }
        }
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = if (scope == BgScope.LOGIN) .42f else .72f)))
        content()
    }
}

@Composable
fun RsBackgroundStudio(c: RsPalette, s: RsStore) {
    var scope by remember { mutableStateOf(BgScope.LOGIN) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Background Studio", color = c.bright, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
        Text("Control the live background used on each app area, including its placement and intensity.", color = c.muted)
        RsPanel(c) {
            Text("PAGE GROUP", color = c.bright, fontWeight = FontWeight.Bold)
            BgScope.entries.forEach { x -> FilterChip(selected = scope == x, onClick = { scope = x }, label = { Text(x.name.replace('_', ' '), fontSize = 10.sp) }) }
        }
        RsPanel(c) {
            Text("BACKGROUND STYLE", color = c.bright, fontWeight = FontWeight.Bold)
            BgStyle.entries.forEach { x ->
                val active = runCatching { BgStyle.valueOf(s.s("bg_${scope.name}", "CINEMATIC_RING")) }.getOrDefault(BgStyle.CINEMATIC_RING) == x
                FilterChip(selected = active, onClick = { s.ps("bg_${scope.name}", x.name) }, label = { Text(x.name.replace('_', ' ')) })
            }
        }
        RsPanel(c) {
            Text("POSITION", color = c.bright, fontWeight = FontWeight.Bold)
            BgPos.entries.forEach { x ->
                val active = runCatching { BgPos.valueOf(s.s("pos_${scope.name}", "CENTER")) }.getOrDefault(BgPos.CENTER) == x
                FilterChip(selected = active, onClick = { s.ps("pos_${scope.name}", x.name) }, label = { Text(x.name) })
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
