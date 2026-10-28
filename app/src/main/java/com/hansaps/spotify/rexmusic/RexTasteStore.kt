package com.hansaps.spotify.rexmusic

import android.content.Context
import kotlin.math.max
import kotlin.random.Random
import org.json.JSONObject

/**
 * Belajar selera pengguna: skor per artis.
 * - Lagu didengar >= 15 detik  -> skor artis naik.
 * - Di-skip sebelum 10 detik   -> skor artis turun sedikit.
 * - Setiap pencatatan, semua skor meluruh sedikit supaya selera terbaru lebih berpengaruh.
 * Yang disimpan hanya nama artis + skor (sangat ringan). Juga menyimpan pilihan autoplay.
 */
class RexTasteStore(context: Context) {

    class Entry(val name: String, var score: Double)

    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val scores = LinkedHashMap<String, Entry>()

    init {
        load()
    }

    var autoplay: Boolean
        get() = prefs.getBoolean(KEY_AUTOPLAY, true)
        set(value) {
            prefs.edit().putBoolean(KEY_AUTOPLAY, value).apply()
        }

    fun recordPlay(artist: String, weight: Double = 1.0) {
        val name = primaryArtist(artist)
        if (name.isBlank() || name.equals("Unknown", ignoreCase = true)) return
        scores.values.forEach { it.score *= DECAY }
        val entry = scores.getOrPut(name.lowercase()) { Entry(name, 0.0) }
        entry.score += weight
        prune()
        save()
    }

    fun recordSkip(artist: String) {
        val entry = scores[primaryArtist(artist).lowercase()] ?: return
        entry.score = max(0.0, entry.score - SKIP_PENALTY)
        prune()
        save()
    }

    /** Artis dengan skor cukup tinggi, terurut dari terbesar. */
    fun top(limit: Int): List<Entry> = scores.values
        .filter { it.score >= MIN_SCORE }
        .sortedByDescending { it.score }
        .take(limit)

    fun favorites(limit: Int): List<String> = top(limit).map { it.name }

    /** Pilih satu artis favorit secara acak, peluang sebanding dengan skor. */
    fun pickFavorite(): String? {
        val pool = top(5)
        if (pool.isEmpty()) return null
        var r = Random.nextDouble() * pool.sumOf { it.score }
        for (entry in pool) {
            r -= entry.score
            if (r <= 0.0) return entry.name
        }
        return pool.last().name
    }

    fun reset() {
        scores.clear()
        save()
    }

    private fun prune() {
        scores.entries.removeAll { it.value.score < 0.1 }
        if (scores.size > MAX_ARTISTS) {
            val keep = scores.entries
                .sortedByDescending { it.value.score }
                .take(MAX_ARTISTS)
                .map { it.key }
                .toSet()
            scores.keys.retainAll(keep)
        }
    }

    private fun load() {
        runCatching {
            val obj = JSONObject(prefs.getString(KEY_SCORES, "{}") ?: "{}")
            obj.keys().forEach { key ->
                val o = obj.optJSONObject(key) ?: return@forEach
                val name = o.optString("n")
                val score = o.optDouble("s", 0.0)
                if (name.isNotBlank() && score > 0.0) scores[key] = Entry(name, score)
            }
        }
    }

    private fun save() {
        runCatching {
            val obj = JSONObject()
            scores.forEach { (key, e) ->
                obj.put(key, JSONObject().put("n", e.name).put("s", e.score))
            }
            prefs.edit().putString(KEY_SCORES, obj.toString()).apply()
        }
    }

    private companion object {
        const val PREFS = "rex_music_taste"
        const val KEY_SCORES = "scores_v1"
        const val KEY_AUTOPLAY = "autoplay"
        const val DECAY = 0.97
        const val MIN_SCORE = 1.5
        const val SKIP_PENALTY = 0.4
        const val MAX_ARTISTS = 40
    }
}

private val ARTIST_SPLIT = Regex(
    "\\s*(?:,|&|;|\\bfeat\\.?|\\bft\\.?|\\bfeaturing\\b|\\bx\\b)\\s*",
    RegexOption.IGNORE_CASE
)

/** Artis utama dari string seperti "Tulus, Raisa" atau "A feat. B". */
fun primaryArtist(artist: String): String =
    artist.split(ARTIST_SPLIT).firstOrNull { it.isNotBlank() }?.trim().orEmpty()

/** Kunci lagu untuk mendeteksi lagu yang sama (judul + artis utama, huruf kecil). */
fun songKey(track: RexTrack): String =
    "${track.title.trim().lowercase()}|${primaryArtist(track.artist).lowercase()}"
