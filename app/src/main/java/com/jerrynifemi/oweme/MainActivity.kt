package com.jerrynifemi.oweme

import android.app.Activity
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.jerrynifemi.oweme.Motion.countUp
import com.jerrynifemi.oweme.Motion.enter
import com.jerrynifemi.oweme.Motion.shake
import com.jerrynifemi.oweme.Motion.springyPress

class MainActivity : Activity(), SensorEventListener {

    private lateinit var store: Store
    private lateinit var root: FrameLayout
    private lateinit var host: FrameLayout
    private lateinit var sheetLayer: FrameLayout

    private var screen: View? = null
    private var currentPerson: String? = null
    private var tiltPanel: View? = null
    private var sensorManager: SensorManager? = null
    private var snack: View? = null
    private var snackRunner: Runnable? = null

    private val dateFormat = SimpleDateFormat("d MMM · h:mm a", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        store = Store(this)
        root = findViewById(R.id.root)
        host = findViewById(R.id.screenHost)
        sheetLayer = findViewById(R.id.sheetLayer)
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager

        showHome(false)
    }

    override fun onResume() {
        super.onResume()
        sensorManager?.let { sm ->
            sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.let { sm.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) }
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager?.unregisterListener(this)
    }

    // ---------------------------------------------------------------- screens

    private fun showHome(animate: Boolean) {
        currentPerson = null
        val view = layoutInflater.inflate(R.layout.screen_home, host, false)
        bindHome(view)
        swapScreen(view, animate)
    }

    private fun showPerson(name: String, animate: Boolean) {
        currentPerson = name
        val view = layoutInflater.inflate(R.layout.screen_person, host, false)
        bindPerson(view, name)
        swapScreen(view, animate)
    }

    private fun refresh() {
        val person = currentPerson
        if (person == null) showHome(false) else showPerson(person, false)
    }

    private fun swapScreen(next: View, animate: Boolean) {
        val old = screen
        screen = next
        host.addView(next)

        if (!animate) {
            old?.let { host.removeView(it) }
            return
        }

        next.alpha = 0f
        next.translationY = dp(26).toFloat()
        next.scaleX = 0.985f
        next.scaleY = 0.985f
        next.animate()
            .alpha(1f).translationY(0f).scaleX(1f).scaleY(1f)
            .setDuration(440)
            .setInterpolator(Motion.spring(0.78f, 1.7f))
            .start()

        if (old != null) {
            old.animate()
                .alpha(0f)
                .translationY(-dp(16).toFloat())
                .setDuration(170)
                .withEndAction { host.removeView(old) }
                .start()
        }
    }

    // ------------------------------------------------------------------- home

    private fun bindHome(view: View) {
        val people = store.people()
        val owed = people.filter { it.balanceMinor > 0 }.sumOf { it.balanceMinor }
        val iOwe = people.filter { it.balanceMinor < 0 }.sumOf { -it.balanceMinor }

        val tvOwed = view.findViewById<TextView>(R.id.tvOwed)
        val tvIOwe = view.findViewById<TextView>(R.id.tvIOwe)
        tvIOwe.text = Money.full(iOwe)
        tvOwed.text = Money.full(0)
        tvOwed.countUp(0, owed, 900L) { tvOwed.text = Money.full(it) }

        val hero = view.findViewById<GlassPanel>(R.id.heroPanel)
        hero.living = true
        tiltPanel = hero
        hero.enter(40L, 14f)

        val list = view.findViewById<LinearLayout>(R.id.listPeople)
        val empty = view.findViewById<TextView>(R.id.tvEmpty)
        empty.visibility = if (people.isEmpty()) View.VISIBLE else View.GONE

        people.forEachIndexed { index, person ->
            val row = layoutInflater.inflate(R.layout.item_person, list, false)
            row.findViewById<TextView>(R.id.tvInitials).text = person.initials
            row.findViewById<TextView>(R.id.tvName).text = person.name
            row.findViewById<TextView>(R.id.tvSub).text = if (person.entries.size == 1)
                getString(R.string.one_entry)
            else
                getString(R.string.entries_count, person.entries.size)

            val amount = row.findViewById<TextView>(R.id.tvAmount)
            amount.text = Money.full(Math.abs(person.balanceMinor))
            amount.setTextColor(color(if (person.balanceMinor > 0) R.color.teal else R.color.rose))

            row.springyPress()
            row.setOnClickListener {
                Sound.play(this, R.raw.sfx_tap)
                showPerson(person.name, true)
            }
            list.addView(row)
            row.enter(70L + 55L * index, 16f)
        }

        view.findViewById<View>(R.id.fab).apply {
            springyPress(0.92f)
            setOnClickListener {
                Sound.play(this@MainActivity, R.raw.sfx_tap)
                openAddSheet(null)
            }
            enter(120L, 20f)
        }
        view.findViewById<TextView>(R.id.tvHomeTitle).enter(0L, 10f)
    }

    // ----------------------------------------------------------------- person

    private fun bindPerson(view: View, name: String) {
        val entries = store.all().filter { it.person.trim() == name }.sortedByDescending { it.timestamp }
        val balance = entries.sumOf { it.signedMinor }

        view.findViewById<TextView>(R.id.tvPersonName).text = name

        val hero = view.findViewById<GlassPanel>(R.id.personHero)
        hero.living = true
        tiltPanel = hero
        hero.enter(40L, 14f)

        val balanceView = view.findViewById<TextView>(R.id.tvPersonBalance)
        balanceView.text = Money.full(Math.abs(balance))
        balanceView.setTextColor(color(if (balance >= 0) R.color.teal else R.color.rose))

        val state = when {
            balance > 0 -> getString(R.string.owes_you)
            balance < 0 -> getString(R.string.you_owe_them)
            else -> getString(R.string.all_square)
        }
        val count = if (entries.size == 1) getString(R.string.one_entry) else getString(R.string.entries_count, entries.size)
        view.findViewById<TextView>(R.id.tvPersonCaption).text = "$state · $count"

        val list = view.findViewById<LinearLayout>(R.id.listEntries)
        entries.forEachIndexed { index, entry ->
            val row = layoutInflater.inflate(R.layout.item_entry, list, false)
            val noteView = row.findViewById<TextView>(R.id.tvEntryNote)
            noteView.text = when {
                entry.note.isNotBlank() -> entry.note
                entry.direction > 0 -> getString(R.string.they_owe)
                else -> getString(R.string.i_owe)
            }
            row.findViewById<TextView>(R.id.tvEntryDate).text = dateFormat.format(Date(entry.timestamp))

            val amount = row.findViewById<TextView>(R.id.tvEntryAmount)
            amount.text = Money.full(entry.signedMinor, withSign = true)
            amount.setTextColor(color(if (entry.signedMinor > 0) R.color.teal else R.color.rose))

            row.springyPress()
            row.setOnLongClickListener {
                removeEntry(entry)
                true
            }
            list.addView(row)
            row.enter(70L + 55L * index, 16f)
        }

        view.findViewById<TextView>(R.id.btnBack).apply {
            springyPress(0.9f)
            setOnClickListener {
                Sound.play(this@MainActivity, R.raw.sfx_tap)
                showHome(true)
            }
        }

        view.findViewById<TextView>(R.id.btnSettle).apply {
            springyPress(0.97f)
            setOnClickListener { settle(name) }
        }
    }

    private fun settle(name: String) {
        val affected = store.all().filter { it.person.trim() == name }
        if (affected.isEmpty()) return
        store.remove(affected.map { it.id }.toSet())
        Sound.play(this, R.raw.sfx_settle)
        showHome(true)
        showSnack(getString(R.string.settled_msg, name)) {
            store.save(store.all() + affected)
            refresh()
        }
    }

    private fun removeEntry(entry: Entry) {
        store.remove(setOf(entry.id))
        Sound.play(this, R.raw.sfx_tap)
        refresh()
        showSnack(getString(R.string.deleted_msg)) {
            store.add(entry)
            refresh()
        }
    }

    // ------------------------------------------------------------------ sheet

    private fun openAddSheet(prefill: String?) {
        if (sheetLayer.childCount > 0) return

        val sheet = layoutInflater.inflate(R.layout.sheet_add, sheetLayer, false)
        sheetLayer.addView(sheet)

        val scrim = sheet.findViewById<View>(R.id.scrim)
        val panel = sheet.findViewById<View>(R.id.sheet)
        val etPerson = sheet.findViewById<EditText>(R.id.etPerson)
        val etAmount = sheet.findViewById<EditText>(R.id.etAmount)
        val etNote = sheet.findViewById<EditText>(R.id.etNote)

        if (!prefill.isNullOrBlank()) etPerson.setText(prefill)

        // quick-pick chips for people already in the ledger
        val chips = sheet.findViewById<LinearLayout>(R.id.chipRow)
        store.people().map { it.name }.take(6).forEach { known ->
            val chip = TextView(this)
            chip.text = known
            chip.textSize = 13f
            chip.gravity = Gravity.CENTER
            chip.setTextColor(color(R.color.text_primary))
            chip.setBackgroundResource(R.drawable.bg_input)
            chip.setPadding(dp(14), 0, dp(14), 0)
            val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(34))
            lp.rightMargin = dp(8)
            chip.layoutParams = lp
            chip.springyPress(0.94f)
            chip.setOnClickListener {
                etPerson.setText(known)
                Sound.play(this, R.raw.sfx_tap)
            }
            chips.addView(chip)
        }

        var direction = 1
        val segPill = sheet.findViewById<View>(R.id.segPill)
        val segWrap = sheet.findViewById<View>(R.id.segWrap)
        val segThey = sheet.findViewById<TextView>(R.id.segThey)
        val segI = sheet.findViewById<TextView>(R.id.segI)

        fun applyDirection(dir: Int, animate: Boolean) {
            direction = dir
            val half = ((segWrap.width - dp(10)) / 2f).coerceAtLeast(0f)
            if (animate) {
                segPill.animate().translationX(if (dir > 0) 0f else half)
                    .setDuration(380).setInterpolator(Motion.spring(0.72f, 1.9f)).start()
            } else {
                segPill.translationX = if (dir > 0) 0f else half
            }
            segThey.setTextColor(color(if (dir > 0) R.color.text_primary else R.color.text_muted))
            segI.setTextColor(color(if (dir > 0) R.color.text_muted else R.color.text_primary))
        }

        segWrap.post {
            val half = ((segWrap.width - dp(10)) / 2).coerceAtLeast(1)
            segPill.layoutParams = FrameLayout.LayoutParams(half, ViewGroup.LayoutParams.MATCH_PARENT).apply {
                leftMargin = dp(5)
                topMargin = dp(5)
                bottomMargin = dp(5)
            }
            applyDirection(direction, false)
        }

        segThey.setOnClickListener {
            applyDirection(1, true)
            Sound.play(this, R.raw.sfx_tap)
        }
        segI.setOnClickListener {
            applyDirection(-1, true)
            Sound.play(this, R.raw.sfx_tap)
        }

        fun close(after: (() -> Unit)? = null) {
            hideKeyboard(etPerson)
            panel.animate()
                .translationY((panel.height.takeIf { it > 0 } ?: dp(600)).toFloat() + dp(60))
                .setDuration(280)
                .setInterpolator(Motion.glide())
                .withEndAction {
                    sheetLayer.removeAllViews()
                    after?.invoke()
                }
                .start()
            scrim.animate().alpha(0f).setDuration(200).start()
        }

        scrim.setOnClickListener { close() }
        sheet.findViewById<TextView>(R.id.btnCancel).setOnClickListener { close() }

        sheet.findViewById<TextView>(R.id.btnSave).apply {
            springyPress(0.97f)
            setOnClickListener {
                val person = etPerson.text.toString().trim()
                val amount = Money.parse(etAmount.text.toString())
                if (person.isEmpty()) {
                    etPerson.shake()
                    return@setOnClickListener
                }
                if (amount == null) {
                    etAmount.shake()
                    return@setOnClickListener
                }
                store.add(
                    Entry(
                        id = store.nextId(),
                        person = person,
                        amountMinor = amount,
                        direction = direction,
                        note = etNote.text.toString().trim(),
                        timestamp = System.currentTimeMillis()
                    )
                )
                Sound.play(this@MainActivity, R.raw.sfx_add)
                close { refresh() }
            }
        }

        panel.translationY = dp(680).toFloat()
        panel.post {
            panel.animate().translationY(0f)
                .setDuration(560).setInterpolator(Motion.spring(0.74f, 1.5f)).start()
        }
        scrim.alpha = 0f
        scrim.animate().alpha(1f).setDuration(260).start()
        etPerson.post { etPerson.requestFocus() }
    }

    // ---------------------------------------------------------------- snackbar

    private fun showSnack(message: String, action: (() -> Unit)? = null) {
        snack?.let { root.removeView(it) }
        snackRunner?.let { root.removeCallbacks(it) }

        val view = layoutInflater.inflate(R.layout.snack, root, false)
        view.findViewById<TextView>(R.id.snackText).text = message
        val actionView = view.findViewById<TextView>(R.id.snackAction)
        actionView.visibility = if (action != null) View.VISIBLE else View.GONE

        val lp = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        lp.gravity = Gravity.BOTTOM
        lp.leftMargin = dp(16)
        lp.rightMargin = dp(16)
        lp.bottomMargin = dp(24)
        root.addView(view, lp)

        view.translationY = dp(150).toFloat()
        view.animate().translationY(0f).setDuration(500)
            .setInterpolator(Motion.spring(0.7f, 1.7f)).start()

        actionView.setOnClickListener {
            action?.invoke()
            dismissSnack()
        }

        snack = view
        val runner = Runnable { dismissSnack() }
        snackRunner = runner
        root.postDelayed(runner, 4200)
    }

    private fun dismissSnack() {
        val view = snack ?: return
        snack = null
        snackRunner?.let { root.removeCallbacks(it) }
        snackRunner = null
        view.animate().translationY(dp(170).toFloat()).alpha(0f).setDuration(240)
            .withEndAction { root.removeView(view) }.start()
    }

    // ------------------------------------------------------------------ misc

    override fun onBackPressed() {
        if (sheetLayer.childCount > 0) {
            sheetLayer.removeAllViews()
            return
        }
        val person = currentPerson
        if (person != null) {
            showHome(true)
            return
        }
        super.onBackPressed()
    }

    override fun onSensorChanged(event: SensorEvent) {
        val panel = tiltPanel ?: return
        val x = (-event.values[0] / 9.81f).coerceIn(-1f, 1f)
        val y = (event.values[1] / 9.81f).coerceIn(-1f, 1f)
        panel.cameraDistance = 16000f
        panel.rotationY = x * 4.5f
        panel.rotationX = y * 3.5f
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    private fun hideKeyboard(view: View) {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    private fun color(resId: Int): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) getColor(resId)
        else @Suppress("DEPRECATION") resources.getColor(resId)

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
