package com.dailysystems.app.widget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import android.widget.FrameLayout
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.ExperimentalGlanceRemoteViewsApi
import androidx.glance.appwidget.compose
import kotlinx.coroutines.flow.first
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.dailysystems.app.DailySystemsApp
import com.dailysystems.app.testContainer
import java.io.File
import java.time.LocalDate
import kotlinx.coroutines.runBlocking
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Opt-in: renders the widget's RemoteViews to PNG (see ScreenshotTest for how to enable).
 * Robolectric can't populate the scrolling list (RemoteCollectionItems), so only the header and
 * progress bar show here; the list content is covered by RemindersAndWidgetTest.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w393dp-h852dp-xxhdpi")
class WidgetRenderTest {
    private val dir = System.getProperty("screenshotDir").orEmpty()

    @OptIn(ExperimentalGlanceRemoteViewsApi::class)
    private fun render(name: String): Unit = runBlocking {
        assumeTrue(dir.isNotEmpty())
        val app = ApplicationProvider.getApplicationContext<DailySystemsApp>()
        val c = testContainer(LocalDate.now())
        app.replaceContainer(c)
        c.starterExamples.load(LocalDate.now())
        val tasks = c.tasks.tasks.first()
        c.tasks.toggleDone(tasks.first { it.title == "Pay electricity bill" }.id, LocalDate.now())

        val rv = TodayWidget().compose(app, size = DpSize(320.dp, 300.dp))
        val parent = FrameLayout(app)
        val view = rv.apply(app, parent)
        val d = app.resources.displayMetrics.density
        val w = (320 * d).toInt(); val h = (300 * d).toInt()
        view.measure(View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(h, View.MeasureSpec.EXACTLY))
        view.layout(0, 0, w, h)
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        Canvas(bmp).apply { drawColor(0xFF6E7F99.toInt()) }.also { view.draw(it) }
        File(dir).mkdirs()
        File(dir, "$name.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        Unit
    }

    @Test fun light() = render("09-widget")

    @Test @Config(qualifiers = "+night") fun dark() = render("09-widget-dark")
}
