package app.pleromastisk.data

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

/** En innlogget bruker: instans, OAuth-klient, token og det appen husker om instansen. */
@Serializable
data class SavedAccount(
    /** Lokal nøkkel, uavhengig av instans og bruker-ID. */
    val key: String,
    val instance: String,
    val clientId: String,
    val clientSecret: String,
    val token: String,
    /** Fylles inn når innloggingen er bekreftet (verify_credentials). */
    val userId: String? = null,
    val acct: String = "",
    val displayName: String = "",
    val avatar: String = "",
    val maxChars: Int = SessionStore.DEFAULT_MAX_CHARS,
    val postFormats: List<String> = emptyList(),
    val altLimit: Int = SessionStore.DEFAULT_ALT_LIMIT,
    val features: List<String> = emptyList(),
    val lastSeenNotification: String? = null,
) {
    /** «navn@instans», også for lokale brukere der instansen ikke står i acct. */
    val handle: String
        get() = when {
            acct.isBlank() -> instance.removePrefix("https://")
            "@" in acct -> acct
            else -> acct + "@" + instance.removePrefix("https://")
        }
}

/**
 * Lagrer brukerne lokalt (privat SharedPreferences). Én bruker er aktiv om gangen, og
 * egenskapene [instance], [token], [maxChars] osv. gjelder den aktive brukeren.
 */
class SessionStore(context: Context) {
    private val sp = context.getSharedPreferences("pleromastisk", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

    private var cache: List<SavedAccount>? = null

    init {
        migrateSingleAccount()
    }

    // ---------- Brukere ----------

    @get:Synchronized
    val accounts: List<SavedAccount>
        get() = cache ?: runCatching {
            json.decodeFromString<List<SavedAccount>>(sp.getString("accounts", null) ?: "[]")
        }.getOrDefault(emptyList()).also { cache = it }

    @Synchronized
    private fun saveAccounts(list: List<SavedAccount>) {
        cache = list
        sp.edit().putString("accounts", json.encodeToString(list)).apply()
    }

    var activeKey: String?
        get() = sp.getString("active_account", null)?.takeIf { k -> accounts.any { it.key == k } }
        set(v) = sp.edit().putString("active_account", v).apply()

    val active: SavedAccount? get() = activeKey?.let { k -> accounts.firstOrNull { it.key == k } }

    @Synchronized
    fun updateAccount(key: String, change: (SavedAccount) -> SavedAccount) {
        saveAccounts(accounts.map { if (it.key == key) change(it) else it })
    }

    private fun updateActive(change: (SavedAccount) -> SavedAccount) {
        activeKey?.let { updateAccount(it, change) }
    }

    /** Legger til en ny bruker og gjør den aktiv. */
    @Synchronized
    fun addAccount(instance: String, clientId: String, clientSecret: String, token: String): SavedAccount {
        val account = SavedAccount(UUID.randomUUID().toString(), instance, clientId, clientSecret, token)
        saveAccounts(accounts + account)
        activeKey = account.key
        return account
    }

    /**
     * Fjerner andre lagrede innlogginger for samme bruker på samme instans som [key]
     * (når man logger inn på nytt med en bruker man allerede har).
     */
    @Synchronized
    fun removeDuplicatesOf(key: String) {
        val me = accounts.firstOrNull { it.key == key } ?: return
        if (me.userId == null) return
        saveAccounts(accounts.filter { it.key == key || it.instance != me.instance || it.userId != me.userId })
    }

    /** Fjerner brukeren. Var den aktiv, blir den første gjenværende brukeren aktiv. */
    @Synchronized
    fun removeAccount(key: String) {
        val rest = accounts.filter { it.key != key }
        saveAccounts(rest)
        if (sp.getString("active_account", null) == key) activeKey = rest.firstOrNull()?.key
    }

    // ---------- Den aktive brukeren ----------

    val instance: String? get() = active?.instance
    val token: String? get() = active?.token

    /** Siste kjente tegngrense. Faller tilbake til 5000 hvis instansen ikke oppgir noen. */
    var maxChars: Int
        get() = active?.maxChars ?: DEFAULT_MAX_CHARS
        set(v) = updateActive { it.copy(maxChars = v) }

    var postFormats: List<String>
        get() = active?.postFormats ?: emptyList()
        set(v) = updateActive { it.copy(postFormats = v) }

    /** Maks lengde på alt-tekst (Pleroma: description_limit). */
    var altLimit: Int
        get() = active?.altLimit ?: DEFAULT_ALT_LIMIT
        set(v) = updateActive { it.copy(altLimit = v) }

    /** Funksjoner instansen annonserer (f.eks. «editing»). */
    var features: List<String>
        get() = active?.features ?: emptyList()
        set(v) = updateActive { it.copy(features = v) }

    // ---------- Innlogging som pågår ----------

    /** Instans og OAuth-klient for en innlogging som venter på svar fra nettleseren. */
    fun setPendingLogin(instance: String, clientId: String, clientSecret: String) {
        sp.edit()
            .putString("pending_instance", instance)
            .putString("pending_client_id", clientId)
            .putString("pending_client_secret", clientSecret)
            .apply()
    }

    /** (instans, client_id, client_secret) for innloggingen som pågår, eller null. */
    fun pendingLogin(): Triple<String, String, String>? {
        val i = sp.getString("pending_instance", null) ?: return null
        val id = sp.getString("pending_client_id", null) ?: return null
        val secret = sp.getString("pending_client_secret", null) ?: return null
        return Triple(i, id, secret)
    }

    fun clearPendingLogin() {
        sp.edit().remove("pending_instance").remove("pending_client_id").remove("pending_client_secret").apply()
    }

    // ---------- Felles for alle brukere ----------

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

    /** Sist valgte språk for nye innlegg (nb, nn eller en). */
    var language: String
        get() = sp.getString("language", "nb") ?: "nb"
        set(v) = sp.edit().putString("language", v).apply()

    /** Bunnmenyen uten tekst under ikonene (slås av/på ved å holde inne en menyknapp). */
    var compactNav: Boolean
        get() = sp.getBoolean("compact_nav", false)
        set(v) = sp.edit().putBoolean("compact_nav", v).apply()

    /** Før flere brukere: én bruker lå i egne nøkler. Flyttes inn i brukerlisten første gang. */
    private fun migrateSingleAccount() {
        if (sp.contains("accounts")) return
        val instance = sp.getString("instance", null)
        val clientId = sp.getString("client_id", null)
        val clientSecret = sp.getString("client_secret", null)
        val token = sp.getString("token", null)
        val list = if (instance != null && clientId != null && clientSecret != null && token != null) {
            listOf(
                SavedAccount(
                    key = UUID.randomUUID().toString(),
                    instance = instance, clientId = clientId, clientSecret = clientSecret, token = token,
                    maxChars = sp.getInt("max_chars", DEFAULT_MAX_CHARS),
                    postFormats = sp.getString("post_formats", "").orEmpty().split(",").filter { it.isNotBlank() },
                    altLimit = sp.getInt("alt_limit", DEFAULT_ALT_LIMIT),
                    features = sp.getString("features", "").orEmpty().split(",").filter { it.isNotBlank() },
                    lastSeenNotification = sp.getString("last_seen_notification", null),
                ),
            )
        } else {
            emptyList()
        }
        saveAccounts(list)
        sp.edit()
            .putString("active_account", list.firstOrNull()?.key)
            .remove("instance").remove("client_id").remove("client_secret").remove("token")
            .remove("max_chars").remove("post_formats").remove("alt_limit").remove("features")
            .remove("last_seen_notification")
            .apply()
    }

    companion object {
        const val DEFAULT_MAX_CHARS = 5000
        const val DEFAULT_ALT_LIMIT = 1500
    }
}
