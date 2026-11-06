package com.jerrynifemi.oweme

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/** Dead simple local store: one JSON array in SharedPreferences. No permissions, no cloud. */
class Store(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences("owe_me", Context.MODE_PRIVATE)

    fun all(): List<Entry> {
        val raw = prefs.getString(KEY, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).mapNotNull { i ->
                val o = arr.optJSONObject(i) ?: return@mapNotNull null
                Entry(
                    id = o.optString("id"),
                    person = o.optString("person"),
                    amountMinor = o.optLong("amount"),
                    direction = o.optInt("dir", 1),
                    note = o.optString("note"),
                    timestamp = o.optLong("ts"),
                    paidMinor = o.optLong("paid")
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun save(list: List<Entry>) {
        val arr = JSONArray()
        list.forEach { e ->
            arr.put(
                JSONObject()
                    .put("id", e.id)
                    .put("person", e.person)
                    .put("amount", e.amountMinor)
                    .put("paid", e.paidMinor)
                    .put("dir", e.direction)
                    .put("note", e.note)
                    .put("ts", e.timestamp)
            )
        }
        prefs.edit().putString(KEY, arr.toString()).apply()
    }

    fun add(entry: Entry) = save(all() + entry)

    fun remove(ids: Set<String>) = save(all().filterNot { it.id in ids })

    /** Every person with a non-zero balance, biggest absolute debt first. */
    fun people(): List<PersonBalance> =
        all()
            .groupBy { it.person.trim() }
            .map { (name, entries) -> PersonBalance(name, entries.sumOf { it.signedMinor }, entries) }
            .filter { it.balanceMinor != 0L }
            .sortedByDescending { Math.abs(it.balanceMinor) }

    /** Newest first, for the activity feed. */
    fun recent(limit: Int): List<Entry> =
        all().sortedByDescending { it.timestamp }.take(limit)

    /** Records a payment against one entry, never more than what is still owed. */
    fun pay(entryId: String, paymentMinor: Long) {
        val list = all()
        val updated = list.map { e ->
            if (e.id != entryId) e
            else e.copy(paidMinor = (e.paidMinor + paymentMinor).coerceAtMost(e.amountMinor))
        }
        save(updated)
    }

    fun nextId(): String = UUID.randomUUID().toString()

    companion object {
        private const val KEY = "entries_json"
    }
}
