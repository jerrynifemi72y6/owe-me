package com.jerrynifemi.oweme

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView

/** Every look and preference in one glass column, applying as you touch it. */
class SettingsActivity : Activity() {

    private lateinit var prefs: Settings
    private lateinit var background: LiquidBackgroundView
    private lateinit var content: LinearLayout

    private val ink: Int get() = if (prefs.isLight) 0xFF141026.toInt() else 0xFFFFFFFF.toInt()
    private val muted: Int get() = if (prefs.isLight) 0xFF5B6280.toInt() else 0xFFC7CCE6.toInt()
    private val accent: Int get() = Skin.accent(prefs)
    private val tint: Int get() = if (prefs.isLight) 0xB8FFFFFF.toInt() else 0x1AFFFFFF
    private val edge: Int get() = if (prefs.isLight) 0xDCFFFFFF.toInt() else 0x3DFFFFFF

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        prefs = Settings(this)
        background = findViewById(R.id.settingsBg)
        content = findViewById(R.id.settingsContent)
        background.configure(this, prefs)
        render()
    }

    private fun render() {
        content.removeAllViews()
        content.addView(header())
        content.addView(card("SOUND") { column ->
            column.addView(row("Tap and settle sounds", toggle(prefs.soundOn) {
                prefs.soundOn = !prefs.soundOn
                prefs.touch()
                render()
            }))
        })
        content.addView(card("CURRENCY") { column ->
            column.addView(chipWrap(
                Settings.CURRENCIES.map { it.second },
                selected = Settings.CURRENCIES.indexOfFirst { it.first == prefs.currencyCode }
            ) { index ->
                prefs.currencyCode = Settings.CURRENCIES[index].first
                prefs.currency = Settings.CURRENCIES[index].second
                Money.symbol = prefs.currency
                prefs.touch()
                render()
            })
        })
        content.addView(card("THEME") { column ->
            column.addView(chipWrap(
                listOf("Dark", "Light", "Flex"),
                selected = when (prefs.theme) { "dark" -> 0; "light" -> 1; else -> 2 }
            ) { index ->
                prefs.theme = when (index) { 0 -> "dark"; 1 -> "light"; else -> "flex" }
                prefs.touch()
                refreshBackground()
                render()
            })
            column.addView(note("Flex lets you recolour the glass, add a photo and change the blur."))
        })

        if (prefs.theme == "flex") content.addView(flexCard())

        content.addView(button("Reset look") {
            prefs.resetLook()
            Money.symbol = prefs.currency
            refreshBackground()
            render()
        })
    }

    private fun flexCard(): View = card("FLEX") { column ->
        column.addView(label("Accent"))
        column.addView(swatches())
        column.addView(row("Pulsing background", toggle(prefs.pulse) {
            prefs.pulse = !prefs.pulse
            prefs.touch()
            refreshBackground()
            render()
        }))
        column.addView(slider("Glass blur", prefs.blur, 0, 60) {
            prefs.blur = it
            prefs.touch()
            refreshBackground()
        })
        column.addView(slider("Glass opacity", prefs.glass, 8, 80) {
            prefs.glass = it
            prefs.touch()
        })
        column.addView(row("Backdrop photo", action(if (prefs.backdropUri == null) "Add" else "Remove") {
            if (prefs.backdropUri == null) pickPhoto() else {
                prefs.backdropUri = null
                prefs.touch()
                refreshBackground()
                render()
            }
        }))
    }

    // ------------------------------------------------------------------ widgets

    private fun header(): View {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val back = TextView(this).apply {
            text = "‹"
            textSize = 24f
            gravity = Gravity.CENTER
            setTextColor(ink)
            background = getDrawable(R.drawable.bg_glass_circle)
            isClickable = true
            setOnClickListener { finish() }
        }
        row.addView(back, LinearLayout.LayoutParams(dp(42), dp(42)))
        val title = TextView(this).apply {
            text = "Settings"
            textSize = 26f
            typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
            setTextColor(ink)
        }
        row.addView(title, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { leftMargin = dp(16) })
        return row
    }

    private fun card(title: String, build: (LinearLayout) -> Unit): View {
        val panel = GlassPanel(this).apply {
            tintColor = tint
            borderColor = edge
            living = true
        }
        val column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(16), dp(18), dp(18))
        }
        column.addView(label(title))
        build(column)
        panel.addView(column, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        val wrap = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        wrap.bottomMargin = dp(12)
        panel.layoutParams = wrap
        return panel
    }

    private fun label(text: String) = TextView(this).apply {
        this.text = text
        textSize = 11f
        letterSpacing = 0.2f
        setTextColor(muted)
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        setPadding(0, 0, 0, dp(8))
    }

    private fun note(text: String) = TextView(this).apply {
        this.text = text
        textSize = 12f
        setTextColor(muted)
        setPadding(0, dp(10), 0, 0)
    }

    private fun row(title: String, control: View): View {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(10), 0, dp(4))
        }
        row.addView(TextView(this).apply {
            text = title
            textSize = 15f
            setTextColor(ink)
        }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        row.addView(control)
        return row
    }

    private fun toggle(on: Boolean, tap: () -> Unit) = TextView(this).apply {
        text = if (on) "ON" else "OFF"
        textSize = 12f
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        gravity = Gravity.CENTER
        setTextColor(if (on) Color.WHITE else ink)
        background = getDrawable(if (on) R.drawable.bg_primary else R.drawable.bg_input)
        setPadding(dp(16), dp(6), dp(16), dp(6))
        isClickable = true
        setOnClickListener { tap() }
    }

    private fun action(label: String, tap: () -> Unit) = TextView(this).apply {
        text = label
        textSize = 12f
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        gravity = Gravity.CENTER
        setTextColor(ink)
        background = getDrawable(R.drawable.bg_input)
        setPadding(dp(16), dp(7), dp(16), dp(7))
        isClickable = true
        setOnClickListener { tap() }
    }

    private fun chipWrap(items: List<String>, selected: Int, onPick: (Int) -> Unit): View {
        val wrap = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(6), 0, 0)
        }
        items.chunked(4).forEachIndexed { rowIndex, chunk ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, dp(4), 0, 0)
            }
            chunk.forEachIndexed { i, item ->
                val index = rowIndex * 4 + i
                val chip = TextView(this).apply {
                    text = item
                    textSize = 14f
                    gravity = Gravity.CENTER
                    setTextColor(if (index == selected) Color.WHITE else ink)
                    background = getDrawable(if (index == selected) R.drawable.bg_primary else R.drawable.bg_input)
                    isClickable = true
                    setOnClickListener { onPick(index) }
                }
                row.addView(chip, LinearLayout.LayoutParams(0, dp(42), 1f).apply {
                    rightMargin = dp(6)
                    leftMargin = dp(2)
                })
            }
            wrap.addView(row)
        }
        return wrap
    }

    private fun swatches(): View {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(8), 0, dp(8))
        }
        Settings.ACCENTS.forEachIndexed { index, color ->
            val dot = View(this).apply {
                background = android.graphics.drawable.GradientDrawable().apply {
                    shape = android.graphics.drawable.GradientDrawable.OVAL
                    setColor(color)
                    if (index == prefs.accentIndex) setStroke(dp(3), Color.WHITE)
                }
                isClickable = true
                setOnClickListener {
                    prefs.accentIndex = index
                    prefs.theme = "flex"
                    prefs.touch()
                    refreshBackground()
                    render()
                }
            }
            row.addView(dot, LinearLayout.LayoutParams(dp(44), dp(44)).apply { rightMargin = dp(10) })
        }
        return row
    }

    private fun slider(title: String, value: Int, min: Int, max: Int, change: (Int) -> Unit): View {
        val column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(10), 0, 0)
        }
        val head = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        head.addView(TextView(this).apply {
            text = title
            textSize = 15f
            setTextColor(ink)
        }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        val valueLabel = TextView(this).apply {
            text = value.toString()
            textSize = 13f
            setTextColor(muted)
        }
        head.addView(valueLabel)
        column.addView(head)
        val bar = SeekBar(this).apply {
            this.max = max - min
            progress = (value - min).coerceIn(0, max - min)
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seek: SeekBar?, progress: Int, fromUser: Boolean) {
                    val real = progress + min
                    valueLabel.text = real.toString()
                    if (fromUser) change(real)
                }
                override fun onStartTrackingTouch(seek: SeekBar?) = Unit
                override fun onStopTrackingTouch(seek: SeekBar?) = Unit
            })
        }
        column.addView(bar)
        return column
    }

    private fun button(label: String, tap: () -> Unit) = TextView(this).apply {
        text = label
        textSize = 15f
        gravity = Gravity.CENTER
        setTextColor(ink)
        background = getDrawable(R.drawable.bg_input)
        isClickable = true
        setPadding(0, dp(16), 0, dp(16))
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        setOnClickListener { tap() }
        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            topMargin = dp(6)
        }
    }

    // ------------------------------------------------------------------- photo

    private fun pickPhoto() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "image/*"
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        try {
            startActivityForResult(intent, PICK)
        } catch (e: Exception) {
            // no document picker on this device
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != PICK || resultCode != RESULT_OK) return
        val uri: Uri = data?.data ?: return
        try {
            contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } catch (e: Exception) {
            // permission is not persistable here; the preview still works this session
        }
        prefs.backdropUri = uri.toString()
        prefs.theme = "flex"
        prefs.touch()
        refreshBackground()
        render()
    }

    private fun refreshBackground() {
        background.configure(this, prefs)
        Skin.apply(findViewById(R.id.settingsRoot), prefs)
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private companion object {
        const val PICK = 41
    }
}
