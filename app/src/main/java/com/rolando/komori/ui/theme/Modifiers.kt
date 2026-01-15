import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.unit.dp

fun Modifier.blurBackground(enabled: Boolean): Modifier {
    return if (enabled) {
        // Blur radius: 16.dp gives a nice frosted glass look
        this.blur(radius = 12.dp)
    } else {
        this
    }
}