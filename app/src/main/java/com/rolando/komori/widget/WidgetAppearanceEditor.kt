package com.rolando.komori.widget

import android.graphics.Color as AndroidColor
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import com.rolando.komori.R
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Bold
import com.adamglin.phosphoricons.bold.PencilSimple
import com.rolando.komori.ui.theme.AppFont
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetAppearanceEditor(initial: WidgetStyle, onCancel: () -> Unit, onSave: suspend (WidgetStyle) -> Unit) {
    val initialHsv = remember(initial.color) { FloatArray(3).also { AndroidColor.colorToHSV(initial.color, it) } }
    var hue by rememberSaveable { mutableFloatStateOf(initialHsv[0]) }
    var saturation by rememberSaveable { mutableFloatStateOf(initialHsv[1]) }
    var brightness by rememberSaveable { mutableFloatStateOf(initialHsv[2]) }
    var opacity by rememberSaveable { mutableFloatStateOf(initial.opacity) }
    val color = AndroidColor.HSVToColor(floatArrayOf(hue, saturation, brightness))
    var hex by rememberSaveable { mutableStateOf(String.format(Locale.ROOT, "%06X", initial.color and 0xFFFFFF)) }
    var saving by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val validHex = hex.matches(Regex("[0-9A-Fa-f]{6}"))
    fun setColor(value: Int) {
        val hsv = FloatArray(3).also { AndroidColor.colorToHSV(value, it) }
        hue = hsv[0]; saturation = hsv[1]; brightness = hsv[2]
        hex = String.format(Locale.ROOT, "%06X", value and 0xFFFFFF)
    }
    fun syncHex() { hex = String.format(Locale.ROOT, "%06X", AndroidColor.HSVToColor(floatArrayOf(hue, saturation, brightness)) and 0xFFFFFF) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Widget appearance", fontFamily = AppFont, fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onCancel, enabled = !saving) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Cancel")
                } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        bottomBar = {
            Surface(color = MaterialTheme.colorScheme.background) {
                Row(Modifier.navigationBarsPadding().imePadding().padding(horizontal = 24.dp, vertical = 12.dp)
                    .widthIn(max = 560.dp).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = onCancel, enabled = !saving, modifier = Modifier.weight(1f), shape = RoundedCornerShape(16.dp)) { Text("Cancel", fontFamily = AppFont) }
                    Button(onClick = {
                        scope.launch {
                            saving = true
                            try { onSave(WidgetStyle(color, opacity)) }
                            catch (e: CancellationException) { throw e }
                            catch (e: Exception) { Toast.makeText(context, "Could not save: ${e.message}", Toast.LENGTH_LONG).show() }
                            finally { saving = false }
                        }
                    }, enabled = validHex && !saving, modifier = Modifier.weight(1f), shape = RoundedCornerShape(16.dp)) {
                        Text(if (saving) "Saving…" else "Save widget", fontFamily = AppFont, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            Column(Modifier.widthIn(max = 560.dp).fillMaxWidth().verticalScroll(rememberScrollState())
                .padding(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                EditorLabel("LIVE PREVIEW")
                WidgetPreview(WidgetStyle(color, opacity))
                Text("Only this widget changes. Your app theme stays the same.",
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = AppFont), color = MaterialTheme.colorScheme.onSurfaceVariant)
                EditorCard {
                    Text("Background color", fontFamily = AppFont, fontWeight = FontWeight.Bold)
                    Text("Pick any shade, or enter an exact color below.", style = MaterialTheme.typography.bodySmall.copy(fontFamily = AppFont), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    ColorField(hue, saturation, brightness) { s, v -> saturation = s; brightness = v; syncHex() }
                    EditorSlider("Hue", hue, 0f..360f, "${hue.roundToInt()}°") { hue = it; syncHex() }
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        listOf(0xFFEDF2E9, 0xFF1D211D, 0xFFFFFFFF, 0xFF000000, 0xFFD5C3AE, 0xFFB6D5DD, 0xFFE6BDC9, 0xFFCABFE0).forEach { swatch ->
                            Box(Modifier.size(36.dp).clip(CircleShape).background(Color(swatch))
                                .border(if (color == swatch.toInt()) 3.dp else 1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                                .semantics { contentDescription = "Color #${String.format(Locale.ROOT, "%06X", swatch and 0xFFFFFF)}" }
                                .clickable { setColor(swatch.toInt()) })
                        }
                    }
                    OutlinedTextField(value = hex, onValueChange = { raw ->
                        val value = raw.removePrefix("#").uppercase(Locale.ROOT)
                        if (value.length <= 6 && value.all { it in '0'..'9' || it in 'A'..'F' }) {
                            hex = value
                            if (value.length == 6) setColor(AndroidColor.parseColor("#$value"))
                        }
                    }, label = { Text("Hex color") }, prefix = { Text("#") }, singleLine = true,
                        isError = !validHex, supportingText = { if (!validHex) Text("Enter all 6 color digits") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp))
                    EditorLabel("RGB COLOR EDITOR")
                    EditorSlider("Red", AndroidColor.red(color).toFloat(), 0f..255f, AndroidColor.red(color).toString()) {
                        setColor(AndroidColor.rgb(it.roundToInt(), AndroidColor.green(color), AndroidColor.blue(color)))
                    }
                    EditorSlider("Green", AndroidColor.green(color).toFloat(), 0f..255f, AndroidColor.green(color).toString()) {
                        setColor(AndroidColor.rgb(AndroidColor.red(color), it.roundToInt(), AndroidColor.blue(color)))
                    }
                    EditorSlider("Blue", AndroidColor.blue(color).toFloat(), 0f..255f, AndroidColor.blue(color).toString()) {
                        setColor(AndroidColor.rgb(AndroidColor.red(color), AndroidColor.green(color), it.roundToInt()))
                    }
                }
                EditorCard {
                    Text("Background opacity", fontFamily = AppFont, fontWeight = FontWeight.Bold)
                    EditorSlider("Opacity", opacity, 0f..1f, "${(opacity * 100).roundToInt()}%") { opacity = it }
                    Text("0% is transparent; 100% is solid. Text and checkboxes stay visible. At low opacity, choose a color that suits your wallpaper.",
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = AppFont), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                TextButton(onClick = { setColor(WidgetStyle().color); opacity = 1f }, enabled = !saving) {
                    Text("Reset to default", fontFamily = AppFont)
                }
            }
        }
    }
}

@Composable
private fun EditorLabel(text: String) {
    Text(text, style = MaterialTheme.typography.labelSmall.copy(fontFamily = AppFont, fontWeight = FontWeight.Bold, letterSpacing = 1.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun EditorCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
    }
}

@Composable
private fun EditorSlider(label: String, value: Float, range: ClosedFloatingPointRange<Float>, display: String, onChange: (Float) -> Unit) {
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, fontFamily = AppFont, style = MaterialTheme.typography.bodyMedium)
            Text(display, fontFamily = AppFont, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Slider(value = value, onValueChange = onChange, valueRange = range,
            modifier = Modifier.fillMaxWidth().semantics { contentDescription = label })
    }
}

@Composable
private fun ColorField(hue: Float, saturation: Float, brightness: Float, onSelected: (Float, Float) -> Unit) {
    val callback by rememberUpdatedState(onSelected)
    Canvas(Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(16.dp))
        .semantics { contentDescription = "Saturation and brightness color field" }
        .pointerInput(Unit) {
            detectTapGestures { p -> callback((p.x / size.width).coerceIn(0f, 1f), (1 - p.y / size.height).coerceIn(0f, 1f)) }
        }.pointerInput(Unit) {
            fun pick(p: Offset) = callback((p.x / size.width).coerceIn(0f, 1f), (1 - p.y / size.height).coerceIn(0f, 1f))
            detectDragGestures(onDragStart = { pick(it) }) { change, _ -> change.consume(); pick(change.position) }
        }) {
        drawRect(Brush.horizontalGradient(listOf(Color.White, Color(AndroidColor.HSVToColor(floatArrayOf(hue, 1f, 1f))))))
        drawRect(Brush.verticalGradient(listOf(Color.Transparent, Color.Black)))
        val marker = Offset(saturation * size.width, (1 - brightness) * size.height)
        drawCircle(Color.Black.copy(alpha = 0.5f), 9.dp.toPx(), marker, style = Stroke(4.dp.toPx()))
        drawCircle(Color.White, 9.dp.toPx(), marker, style = Stroke(2.dp.toPx()))
    }
}

@Composable
private fun WidgetPreview(style: WidgetStyle) {
    val text = Color(style.foreground)
    val radius = dimensionResource(R.dimen.widget_corner_radius)
    val cardRadius = dimensionResource(R.dimen.widget_card_corner_radius)
    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(radius))) {
        Canvas(Modifier.matchParentSize()) {
            val cell = 14.dp.toPx()
            for (x in 0..(size.width / cell).toInt()) for (y in 0..(size.height / cell).toInt()) {
                drawRect(if ((x + y) % 2 == 0) Color(0xFFB8BDB7) else Color(0xFFDDE1D9), Offset(x * cell, y * cell), Size(cell, cell))
            }
        }
        Column(Modifier.fillMaxWidth().background(Color(style.color).copy(alpha = style.opacity)).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("My Focus", fontFamily = AppFont, fontWeight = FontWeight.Bold, fontSize = 22.sp, color = text)
                Icon(PhosphorIcons.Bold.PencilSimple, "Edit widget preview", tint = text, modifier = Modifier.size(20.dp))
            }
            Text("Your focus, your colors", fontFamily = AppFont, fontSize = 12.sp, color = text)
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(cardRadius)).background(Color(style.surface).copy(alpha = style.opacity)).padding(14.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.size(20.dp).border(1.5.dp, text, CircleShape))
                Column {
                    Text("Make space for what matters", fontFamily = AppFont, fontWeight = FontWeight.Medium, fontSize = 14.sp, color = text)
                    Text("Medium priority", fontFamily = AppFont, fontSize = 11.sp, color = text)
                }
            }
        }
    }
}
