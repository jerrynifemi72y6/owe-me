package com.jerrynifemi.oweme

import android.app.Activity
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.content.Intent
import android.graphics.RenderEffect
import android.graphics.Shader
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.Calendar
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
    private lateinit var prefs: Settings
    private lateinit var background: LiquidBackgroundView
    private var lastRevision = 0

    private val dateFormat = SimpleDateFormat("d MMM · h:mm a", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        store = Store(this)
        root = findViewById(R.id.root)
        host = findViewById(R.id.screenHost)
        background = findViewById(R.id.liquid)
        prefs = Settings(this)
        Money.symbol = prefs.currency
        background.configure(this, prefs)
        sheetLayer = findViewById(R.id.sheetLayer)
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager

        showHome(false)
        lastRevision = prefs.revision
    }

    override fun onResume() {
        super.onResume()
        val saved = prefs.revision
        if (saved != lastRevision) {
            lastRevision = saved
            Money.symbol = prefs.currency
            background.configure(this, prefs)
            Skin.apply(root, prefs)
            refresh()
        }
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
        Skin.apply(next, prefs)

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
        val net = owed - iOwe

        val tvOwed = view.findViewById<TextView>(R.id.tvOwed)
        val tvIOwe = view.findViewById<TextView>(R.id.tvIOwe)
        tvIOwe.text = Money.full(iOwe)
        tvOwed.text = Money.full(0)
        tvOwed.countUp(0, owed, 900L) { tvOwed.text = Money.full(it) }

        val tvNet = view.findViewById<TextView>(R.id.tvNet)
        tvNet.text = Money.full(net, withSign = true)
        tvNet.setTextColor(color(if (net >= 0) R.color.teal else R.color.rose))
        view.findViewById<TextView>(R.id.tvNetCaption).text = when {
            net > 0 -> getString(R.string.net_ahead)
            net < 0 -> getString(R.string.net_behind)
            else -> getString(R.string.net_even)
        }

        val hero = view.findViewById<GlassPanel>(R.id.heroPanel)
        hero.living = true
        tiltPanel = hero
        hero.enter(40L, 14f)

        val search = view.findViewById<EditText>(R.id.etSearch)
        renderPeople(view, people)
        search.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(editable: Editable?) {
                renderPeople(view, people, editable?.toString().orEmpty())
            }
            override fun beforeTextChanged(x: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(x: CharSequence?, a: Int, b: Int, c: Int) {}
        })

        bindWeek(view)
        bindActivity(view)

        view.findViewById<View>(R.id.btnSettings).apply {
            springyPress(0.9f)
            setOnClickListener {
                Sound.play(this, R.raw.sfx_tap)
                startActivity(Intent(this, SettingsActivity::class.java))
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            }
        }

        view.findViewById<View>(R.id.fabAdd).apply {
            springyPress(0.95f)
            setOnClickListener {
                Sound.play(this@MainActivity, R.raw.sfx_tap)
                openAddSheet(null)
            }
            enter(120L, 20f)
        }
        view.findViewById<TextView>(R.id.tvHomeTitle).enter(0L, 10f)
    }

    private fun renderPeople(screen: View, people: List<PersonBalance>, query: String = "") {
        val list = screen.findViewById<LinearLayout>(R.id.listPeople)
        val empty = screen.findViewById<TextView>(R.id.tvEmpty)
        list.removeAllViews()

        val filtered = if (query.isBlank()) people
        else people.filter { it.name.contains(query, ignoreCase = true) }

        empty.text = if (query.isNotBlank()) getString(R.string.no_match)
        else getString(R.string.empty_people)
        empty.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE

        filtered.forEachIndexed { index, person ->
            val row = layoutInflater.inflate(R.layout.item_person, list, false)
            row.findViewById<AvatarView>(R.id.avatar).apply {
                initials = person.initials
                isGroup = person.isGroup
                accent = Accents.of(person.name)
            }
            row.findViewById<TextView>(R.id.tvName).text = person.name

            val count = if (person.entries.size == 1) getString(R.string.one_entry)
            else getString(R.string.entries_count, person.entries.size)
            row.findViewById<TextView>(R.id.tvSub).text =
                if (person.isGroup) getString(R.string.group_tag) + "  ·  " + count else count

            val amount = row.findViewById<TextView>(R.id.tvAmount)
            amount.text = Money.full(Math.abs(person.balanceMinor))
            amount.setTextColor(color(if (person.balanceMinor > 0) R.color.teal else R.color.rose))

            row.springyPress()
            row.setOnClickListener {
                Sound.play(this, R.raw.sfx_tap)
                showPerson(person.name, true)
            }
            list.addView(row)
            if (query.isBlank()) row.enter(70L + 55L * index, 16f)
        }
    }

    /** Seven days of movement, so the summary card is not the only thing on the page. */
    private fun bindWeek(view: View) {
        val chart = view.findViewById<WeekChartView>(R.id.weekChart)
        val dayMs = 24L * 60L * 60L * 1000L
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val todayStart = cal.timeInMillis
        val entries = store.all()
        val sums = FloatArray(7)
        val labels = Array(7) { "" }
        val dayFormat = SimpleDateFormat("EEEEE", Locale.getDefault())
        var weekTotal = 0L

        for (i in 0 until 7) {
            val start = todayStart - (6 - i) * dayMs
            var sum = 0L
            for (entry in entries) {
                if (entry.timestamp >= start && entry.timestamp < start + dayMs) {
                    sum += Math.abs(entry.signedMinor)
                }
            }
            sums[i] = sum.toFloat()
            weekTotal += sum
            labels[i] = dayFormat.format(Date(start))
        }

        chart.setData(sums, labels)
        view.findViewById<TextView>(R.id.tvWeekTotal).text =
            getString(R.string.week_total, Money.full(weekTotal))
    }

    private fun bindActivity(view: View) {
        val list = view.findViewById<LinearLayout>(R.id.listActivity)
        val none = view.findViewById<TextView>(R.id.tvNoActivity)
        val recent = store.recent(5)
        none.visibility = if (recent.isEmpty()) View.VISIBLE else View.GONE
        recent.forEachIndexed { index, entry ->
            val row = layoutInflater.inflate(R.layout.item_entry, list, false)
            row.findViewById<TextView>(R.id.tvEntryNote).text =
                if (entry.direction > 0) getString(R.string.borrowed, entry.person, Money.full(entry.amountMinor))
                else getString(R.string.you_borrowed, Money.full(entry.amountMinor), entry.person)
            row.findViewById<TextView>(R.id.tvEntryDate).text =
                TimeAgo.format(entry.timestamp)
            val amount = row.findViewById<TextView>(R.id.tvEntryAmount)
            amount.text = Money.full(entry.signedMinor, withSign = true)
            amount.setTextColor(color(if (entry.signedMinor > 0) R.color.teal else R.color.rose))
            row.springyPress()
            row.setOnClickListener { showPerson(entry.person, true) }
            list.addView(row)
            row.enter(60L + 45L * index, 12f)
        }
    }

    // ----------------------------------------------------------------- person

    private fun bindPerson(view: View, name: String) {
        val entries = store.all().filter { it.person.trim() == name }.sortedByDescending { it.timestamp }
        val balance = entries.sumOf { it.signedMinor }

        view.findViewById<TextView>(R.id.tvPersonName).text = name
        val summary = PersonBalance(name, balance, entries)
        view.findViewById<AvatarView>(R.id.avatarPerson).apply {
            initials = summary.initials
            isGroup = summary.isGroup
            accent = Accents.of(name)
        }

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

            val when_ = StringBuilder(TimeAgo.format(entry.timestamp))
            if (entry.paidMinor > 0) {
                when_.append("  ·  ").append(getString(R.string.part_paid)).append(" ")
                    .append(Money.full(entry.paidMinor))
            }
            row.findViewById<TextView>(R.id.tvEntryDate).text = when_.toString()

            val amount = row.findViewById<TextView>(R.id.tvEntryAmount)
            amount.text = Money.full(entry.signedMinor, withSign = true)
            amount.setTextColor(color(if (entry.signedMinor > 0) R.color.teal else R.color.rose))

            val payPill = row.findViewById<TextView>(R.id.btnEntryPay)
            if (entry.remainingMinor > 0) {
                payPill.visibility = View.VISIBLE
                payPill.springyPress(0.92f)
                payPill.setOnClickListener { openPaySheet(entry) }
            } else {
                payPill.visibility = View.GONE
            }

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
            setOnClickListener { openSettleSheet(name) }
        }

        view.findViewById<TextView>(R.id.btnRemind).apply {
            springyPress(0.95f)
            setOnClickListener { remind(name) }
        }
    }

    private fun settle(name: String, method: String) {
        val affected = store.all().filter { it.person.trim() == name }
        if (affected.isEmpty()) return
        store.remove(affected.map { it.id }.toSet())
        Sound.play(this, R.raw.sfx_settle)
        showHome(true)
        showSnack(getString(R.string.settled_via, name, method)) {
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

    // ------------------------------------------------------------------ sheets

    /** Real backdrop blur on Android 12+, plus a 60% scrim on everything below. */
    private fun setBackdrop(active: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            host.setRenderEffect(
                if (active) RenderEffect.createBlurEffect(24f, 24f, Shader.TileMode.CLAMP) else null
            )
        }
    }

    private fun revealSheet(scrim: View, panel: View) {
        (panel as? GlassPanel)?.tintColor = android.graphics.Color.parseColor("#F5181D30")
        setBackdrop(true)
        panel.translationY = dp(700).toFloat()
        panel.post {
            panel.animate().translationY(0f)
                .setDuration(560).setInterpolator(Motion.spring(0.74f, 1.5f)).start()
        }
        scrim.alpha = 0f
        scrim.animate().alpha(0.6f).setDuration(240).start()
    }

    private fun dismissSheet(scrim: View, panel: View, after: (() -> Unit)? = null) {
        panel.animate()
            .translationY((panel.height.takeIf { it > 0 } ?: dp(600)).toFloat() + dp(80))
            .setDuration(280)
            .setInterpolator(Motion.glide())
            .withEndAction {
                sheetLayer.removeAllViews()
                setBackdrop(false)
                after?.invoke()
            }
            .start()
        scrim.animate().alpha(0f).setDuration(200).start()
    }

    // ------------------------------------------------------------- part payment

    private fun openPaySheet(entry: Entry) {
        if (sheetLayer.childCount > 0) return
        val sheet = layoutInflater.inflate(R.layout.sheet_pay, sheetLayer, false)
        sheetLayer.addView(sheet)

        val scrim = sheet.findViewById<View>(R.id.payScrim)
        val panel = sheet.findViewById<View>(R.id.paySheet)
        val etPay = sheet.findViewById<EditText>(R.id.etPayAmount)

        sheet.findViewById<TextView>(R.id.tvPayRemaining).text = Money.full(entry.remainingMinor)
        sheet.findViewById<TextView>(R.id.tvPayCaption).text =
            getString(R.string.pay_remaining_of, Money.full(entry.amountMinor))

        fun close(after: (() -> Unit)? = null) {
            hideKeyboard(etPay)
            dismissSheet(scrim, panel, after)
        }

        scrim.setOnClickListener { close() }
        sheet.findViewById<TextView>(R.id.btnPayCancel).setOnClickListener { close() }

        sheet.findViewById<TextView>(R.id.chipHalf).setOnClickListener {
            val half = entry.remainingMinor / 2
            etPay.setText(String.format(Locale.US, "%.2f", half / 100.0))
            etPay.setSelection(etPay.text.length)
        }
        sheet.findViewById<TextView>(R.id.chipFull).setOnClickListener {
            etPay.setText(String.format(Locale.US, "%.2f", entry.remainingMinor / 100.0))
            etPay.setSelection(etPay.text.length)
        }

        sheet.findViewById<TextView>(R.id.btnPay).apply {
            springyPress(0.97f)
            setOnClickListener {
                val amount = Money.parse(etPay.text.toString())
                if (amount == null) {
                    etPay.shake()
                    return@setOnClickListener
                }
                store.pay(entry.id, amount)
                Sound.play(this@MainActivity, R.raw.sfx_settle)
                close { refresh() }
            }
        }

        revealSheet(scrim, panel)
    }

    // ---------------------------------------------------------------- settle

    private fun openSettleSheet(name: String) {
        if (sheetLayer.childCount > 0) return
        val sheet = layoutInflater.inflate(R.layout.sheet_settle, sheetLayer, false)
        sheetLayer.addView(sheet)

        val scrim = sheet.findViewById<View>(R.id.settleScrim)
        val panel = sheet.findViewById<View>(R.id.settleSheet)

        fun close(after: (() -> Unit)? = null) = dismissSheet(scrim, panel, after)

        scrim.setOnClickListener { close() }
        sheet.findViewById<TextView>(R.id.btnSettleCancel).setOnClickListener { close() }

        listOf(
            R.id.methodCash to getString(R.string.method_cash),
            R.id.methodTransfer to getString(R.string.method_transfer),
            R.id.methodOpay to getString(R.string.method_opay)
        ).forEach { (id, label) ->
            sheet.findViewById<TextView>(id).apply {
                springyPress(0.97f)
                setOnClickListener { close { settle(name, label) } }
            }
        }

        revealSheet(scrim, panel)
    }

    // ----------------------------------------------------------------- remind

    private fun remind(name: String) {
        val balance = store.all().filter { it.person.trim() == name }.sumOf { it.signedMinor }
        val amount = Money.full(Math.abs(balance))
        val message = if (balance > 0) "Yo $name, you still have my $amount 😅"
        else "Hey $name, I still owe you $amount 🙏"
        Sound.play(this, R.raw.sfx_tap)
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/?text=" + Uri.encode(message))))
        } catch (e: Exception) {
            val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, message)
            startActivity(Intent.createChooser(send, getString(R.string.remind)))
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

        // quick amounts, because most entries are 500 / 1k / 2k / 5k
        listOf(
            R.id.chip500 to "500",
            R.id.chip1k to "1000",
            R.id.chip2k to "2000",
            R.id.chip5k to "5000"
        ).forEach { (id, value) ->
            sheet.findViewById<TextView>(id).apply {
                springyPress(0.94f)
                setOnClickListener {
                    etAmount.setText(value)
                    etAmount.setSelection(value.length)
                    Sound.play(this@MainActivity, R.raw.sfx_tap)
                }
            }
        }

        // the big number reacts to every keystroke
        etAmount.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(editable: Editable?) {
                etAmount.animate().scaleX(1.05f).scaleY(1.05f).setDuration(80).withEndAction {
                    etAmount.animate().scaleX(1f).scaleY(1f).setDuration(160).start()
                }.start()
            }
            override fun beforeTextChanged(x: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(x: CharSequence?, a: Int, b: Int, c: Int) {}
        })

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
            dismissSheet(scrim, panel, after)
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

        revealSheet(scrim, panel)
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
            setBackdrop(false)
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
