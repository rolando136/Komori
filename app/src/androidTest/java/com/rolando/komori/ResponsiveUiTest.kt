package com.rolando.komori

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.unit.dp
import androidx.test.filters.SdkSuppress
import com.rolando.komori.ui.screens.ScheduleScreen
import com.rolando.komori.ui.screens.ThemeSelector
import com.rolando.komori.ui.theme.AppThemeOption
import com.rolando.komori.widget.WidgetAppearanceEditor
import com.rolando.komori.widget.WidgetStyle
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class ResponsiveUiTest {
    @get:Rule val compose = createComposeRule()

    @Test fun themeColorsCanScrollOnNarrowScreens() {
        compose.setContent {
            MaterialTheme {
                Box(Modifier.width(240.dp)) {
                    ThemeSelector(AppThemeOption.Komori, {})
                }
            }
        }
        compose.onNodeWithContentDescription("Paper").performScrollTo().assertIsDisplayed()
        compose.onNodeWithContentDescription("Komori").performScrollTo().assertIsDisplayed()
    }

    @Test fun widgetEditorSavesExactColorAndOpacity() {
        var saved: WidgetStyle? = null
        compose.setContent { MaterialTheme {
            WidgetAppearanceEditor(WidgetStyle(), onCancel = {}, onSave = { saved = it })
        } }
        compose.onNodeWithText("Hex color").performScrollTo().performTextReplacement("315975")
        compose.onNodeWithContentDescription("Opacity").performScrollTo()
            .performSemanticsAction(SemanticsActions.SetProgress) { it(0.35f) }
        compose.onNodeWithText("Save widget").performClick()
        compose.waitForIdle()
        compose.runOnIdle {
            assertEquals(android.graphics.Color.parseColor("#315975"), saved?.color)
            assertEquals(0.35f, saved!!.opacity, 0.001f)
        }
    }

    @Test fun widgetEditorCancelDoesNotSaveDraft() {
        var saved = false
        var cancelled = false
        compose.setContent { MaterialTheme {
            WidgetAppearanceEditor(WidgetStyle(), onCancel = { cancelled = true }, onSave = { saved = true })
        } }
        compose.onNodeWithText("Cancel").performClick()
        compose.runOnIdle { assertTrue(cancelled); assertFalse(saved) }
    }

    @SdkSuppress(minSdkVersion = 26)
    @Test fun visibleWeekChangesMonthAfterSwiping() {
        val visibleDate = mutableStateOf(LocalDate.now())
        val format = DateTimeFormatter.ofPattern("MMMM yyyy")
        val monday = LocalDate.now().let { it.minusDays((it.dayOfWeek.value - 1).toLong()) }
        compose.setContent {
            MaterialTheme {
                Column {
                    Text(visibleDate.value.format(format))
                    ScheduleScreen(
                        events = emptyList(), jumpToDate = null,
                        onVisibleDateChanged = { visibleDate.value = it },
                        onEventClick = {}, onAddEvent = {}, onFabVisibilityChange = {}
                    )
                }
            }
        }
        compose.waitForIdle()
        repeat(5) {
            compose.onRoot().performTouchInput { swipeLeft() }
            compose.waitForIdle()
        }
        compose.onNodeWithText(monday.plusWeeks(5).format(format)).assertIsDisplayed()
        compose.runOnIdle { check(visibleDate.value == monday.plusWeeks(5)) }
    }
}
