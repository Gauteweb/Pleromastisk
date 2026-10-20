package app.pleromastisk.data

import android.content.Context

/** Lagrer instans, OAuth-klient og token lokalt (privat SharedPreferences). */
class SessionStore(context: Context) {
    private val sp = context.getSharedPreferences("pleromastisk", Context.MODE_PRIVATE)

    var instance: String?
        get() = sp.getString("instance", null)
        set(v) = sp.edit().putString("instance", v).apply()

    var clientId: String?
        get() = sp.getString("client_id", null)
        set(v) = sp.edit().putString("client_id", v).apply()

    var clientSecret: String?
        get() = sp.getString("client_secret", null)
        set(v) = sp.edit().putString("client_secret", v).apply()

    var token: String?
        get() = sp.getString("token", null)
        set(v) = sp.edit().putString("token", v).apply()

    /** Siste kjente tegngrense. Faller tilbake til 5000 hvis instansen ikke oppgir noen. */
    var maxChars: Int
        get() = sp.getInt("max_chars", DEFAULT_MAX_CHARS)
        set(v) = sp.edit().putInt("max_chars", v).apply()

    var postFormats: List<String>
        get() = sp.getString("post_formats", "").orEmpty().split(",").filter { it.isNotBlank() }
        set(v) = sp.edit().putString("post_formats", v.joinToString(",")).apply()

    /** Maks lengde på alt-tekst (Pleroma: description_limit). */
    var altLimit: Int
        get() = sp.getInt("alt_limit", DEFAULT_ALT_LIMIT)
        set(v) = sp.edit().putInt("alt_limit", v).apply()

    /** Funksjoner instansen annonserer (f.eks. «editing»). */
    var features: List<String>
        get() = sp.getString("features", "").orEmpty().split(",").filter { it.isNotBlank() }
        set(v) = sp.edit().putString("features", v.joinToString(",")).apply()

    private val postedHere: LinkedHashSet<String> by lazy {
        LinkedHashSet(sp.getString("posted_ids", "").orEmpty().split(",").filter { it.isNotBlank() })
    }

    /** Husker at dette innlegget ble postet fra Pleromastisk (vises som «via Pleromastisk»). */
    @Synchronized
    fun rememberPosted(id: String) {
        postedHere.remove(id)
        postedHere.add(id)
        while (postedHere.size > 300) postedHere.remove(postedHere.first())
        sp.edit().putString("posted_ids", postedHere.joinToString(",")).apply()
    }

    @Synchronized
    fun wasPostedHere(id: String): Boolean = id in postedHere

    fun clear() = sp.edit().clear().apply()

    companion object {
        const val DEFAULT_MAX_CHARS = 5000
        const val DEFAULT_ALT_LIMIT = 1500
    }
}
