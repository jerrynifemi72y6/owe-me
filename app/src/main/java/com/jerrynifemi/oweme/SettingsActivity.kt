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

class SettingsActivity : Activity() {

    private lateinit var settings: Settings
    private lateinit var content: LinearLayout
    private var skin: Skin? = null

    private val accent: Int get() = skin?.accent ?: Color.WHITE
    private val ink: Int get() = skin?.ink ?: Color.WHITE
    private val muted: Int get() = skin?.muted ?: Color.GRAY

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        settings = Settings(this)
        content = findViewById(R.id.settingsContent)
        Skin.of(settings).applyTo(findViewById(R.id.settingsRoot))
        findViewById<LiquidBackgroundView>(R.id.settingsBg).configure(this, settings)
        render()
    }

    private fun render() {
        skin = Skin.of(settings)
        content.removeAllViews()
        val s = skin ?: return

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        header.addView(
            TextView(this).apply {
                text = "‹"
                textSize = 24f
                gravity = Gravity.CENTER
                setTextColor(s.ink)
                background = getDrawable(R.drawable.bg_glass_circle)
                isClickable = true
                setOnClickListener { finish() }
            },
            LinearLayout.LayoutParams(dp(42), dp(42))
        )
        header.addView(
            TextView(this).apply {
                text = "Settings"
                textSize = 25f
                setTextColor(s.ink)
                typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
            },
            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { leftMargin = dp(14) }
        )
        content.addView(header)
        content.addView(block(
            "SOUND",
            toggle(settings.soundOn) {
                settings.soundOn = !settings.soundOn
                settings.touch()
                render()
            }
        ))

        content.addView(block(
            "CURRENCY",
            chips(
                Settings.CURRENCIES.map { it.second },
                Settings.CURRENCIES.indexOfFirst { it.first == settings.currencyCode }
            ) { index ->
                settings.currencyCode = Settings.CURRENCIES[index].first
                Money.symbol = settings.currencySymbol
                settings.touch()
                render()
            }
        ))

        content.addView(block(
            "STYLE · glass, solid or flex",
            chips(listOf("Glass", "Solid", "Flex"), listOf("glass", "solid", "flex").indexOf(settings.style)) { index ->
                settings.style = listOf("glass", "solid", "flex")[index]
                settings.touch()
                applyLive()
            }
        ))

        content.addView(block(
            "TONE",
            chips(listOf("Dark", "Light"), if (settings.isLight) 1 else 0) { index ->
                settings.tone = if (index == 1) "light" else "dark"
                settings.touch()
                applyLive()
            }
        ))

        content.addView(block("ACCENT", swatches()))

        if (settings.style == "flex") {
            val column = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
            column.addView(row("Pulsing field", toggle(settings.pulse) {
                settings.pulse = !settings.pulse
                settings.touch()
                render()
            }))
            column.addView(slider("Glass blur", settings.blur, 60) {
                settings.blur = it
                settings.touch()
                applyLive()
            })
            column.addView(slider("Glass opacity", settings.glassOpacity, 100) {
                settings.glassOpacity = it
                settings.touch()
                applyLive()
            })
            column.addView(row("Photo backdrop", pill(if (settings.backdrop == null) "Choose" else "Change") {
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "image/*"
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                startActivityForResult(intent, 41)
            }))
            if (settings.backdrop != null) {
                column.addView(row("Remove photo", pill("Remove") {
                    settings.backdrop = null
                    settings.touch()
                    applyLive()
                }))
            }
            content.addView(block("FLEX", column))
        }

        content.addView(pill("Reset look", top = 18) {
            settings.resetLook()
            render()
        })
    }

    /** Repaint this screen, and mark the main screen dirty so it rebuilds. */
    private fun applyLive() {
        skin = Skin.of(settings)
        Skin.of(settings).applyTo(findViewById(R.id.settingsRoot))
        findViewById<LiquidBackgroundView>(R.id.settingsBg).configure(this, settings)
        render()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != 41 || resultCode != RESULT_OK) return
        val uri: Uri = data?.data ?: return
        try {
            contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } catch (ignored: SecurityException) {
        }
        settings.backdrop = uri.toString()
        settings.touch()
        applyLive()
    }

    // ------------------------------------------------------------------ pieces

    private fun block(title: String, child: View): View {
        val column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(16), dp(14), dp(16))
            addView(TextView(this@SettingsActivity).apply {
                text = title
                textSize = 10f
                letterSpacing = 0.16f
                setTextColor(muted)
                typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            })
            addView(child)
        }
        val panel = GlassPanel(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(14) }
            Skin.of(settings).let {
                tintColor = it.cardFill
                borderColor = it.cardBorder
                frosted = it.frosted
            }
            addView(column)
        }
        return panel
    }

    private fun row(label: String, tail: View): View {
        val line = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(10), 0, dp(10))
        }
        line.addView(TextView(this).apply {
            text = label
            textSize = 14f
            setTextColor(ink)
        }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        line.addView(tail)
        return line
    }

    private fun chips(items: List<String>, selected: Int, onPick: (Int) -> Unit): View {
        val strip = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(10), 0, 0)
        }
        items.forEachIndexed { index, label ->
            val picked = index == selected
            val chip = TextView(this).apply {
                text = label
                textSize = 13f
                gravity = Gravity.CENTER
                setPadding(dp(12), dp(9), dp(12), dp(9))
                setTextColor(if (picked) Color.WHITE else muted)
                background = if (picked) chipActive() else chipIdle()
                isClickable = true
                setOnClickListener { onPick(index) }
            }
            strip.addView(chip, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                rightMargin = if (index < items.size - 1) dp(8) else 0
            })
        }
        return strip
    }

    private fun chipActive() = android.graphics.drawable.GradientDrawable().apply {
        cornerRadius = dp(13).toFloat()
        setColor(accent)
    }

    private fun chipIdle() = android.graphics.drawable.GradientDrawable().apply {
        cornerRadius = dp(13).toFloat()
        setColor(if (skin?.isLightBg == true) 0x14000000 else 0x22FFFFFF)
    }

    private fun toggle(on: Boolean, tap: () -> Unit): TextView {
        val view = TextView(this).apply {
            text = if (on) "ON" else "OFF"
            textSize = 12f
            gravity = Gravity.CENTER
            letterSpacing = 0.08f
            setPadding(dp(16), dp(8), dp(16), dp(8))
            setTextColor(if (on) Color.WHITE else muted)
            background = if (on) chipActive() else chipIdle()
            isClickable = true
            setOnClickListener { tap() }
        }
        return view
    }

    private fun pill(label: String, top: Int = 0, tap: () -> Unit): TextView =
        TextView(this).apply {
            text = label
            textSize = 13f
            gravity = Gravity.CENTER
            setPadding(dp(16), dp(10), dp(16), dp(10))
            setTextColor(ink)
            background = chipIdle()
            isClickable = true
            setOnClickListener { tap() }
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(top) }
        }

    private fun swatches(): View {
        val strip = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(12), 0, 0)
        }
        Settings.ACCENTS.forEachIndexed { index, (color, _) ->
            val dot = View(this).apply {
                background = android.graphics.drawable.GradientDrawable().apply {
                    shape = android.graphics.drawable.GradientDrawable.OVAL
                    setColor(color)
                    setStroke(
                        if (index == settings.accentIndex) dp(3) else dp(1),
                        if (index == settings.accentIndex) Color.WHITE else 0x44FFFFFF
                    )
                }
                isClickable = true
                setOnClickListener {
                    settings.accentIndex = index
                    settings.touch()
                    applyLive()
                }
            }
            strip.addView(dot, LinearLayout.LayoutParams(0, dp(36), 1f).apply {
                rightMargin = if (index < Settings.ACCENTS.size - 1) dp(10) else 0
            })
        }
        return strip
    }

    private fun slider(label: String, value: Int, max: Int, onChange: (Int) -> Unit): View {
        val column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(12), 0, 0)
        }
        column.addView(TextView(this).apply {
            text = "$label  ·  $value"
            textSize = 13f
            setTextColor(ink)
        })
        column.addView(SeekBar(this).apply {
            this.max = max
            progress = value
            setPadding(0, dp(6), 0, 0)
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(bar: SeekBar?, p: Int, fromUser: Boolean) {
                    if (fromUser) onChange(p)
                }
                override fun onStartTrackingTouch(bar: SeekBar?) = Unit
                override fun onStopTrackingTouch(bar: SeekBar?) = Unit
            })
        })
        return column
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
