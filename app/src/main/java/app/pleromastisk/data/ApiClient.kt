package app.pleromastisk.data

import android.content.ContentResolver
import android.net.Uri
import android.webkit.MimeTypeMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.UUID
import java.util.concurrent.TimeUnit

class ApiException(val code: Int, message: String) : Exception(message)

/**
 * Tynn klient mot Mastodon-kompatibelt API (Pleroma, Akkoma, Mastodon …).
 * Instansen leses fra [SessionStore], så samme klient fungerer mot alle instanser.
 */
class ApiClient(private val store: SessionStore) {

    private val http = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(40, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
    }

    private val jsonType = "application/json; charset=utf-8".toMediaType()

    // ---------- Infrastruktur ----------

    private fun builder(path: String, base: String? = null, auth: Boolean = true): Request.Builder {
        val root = base ?: store.instance ?: throw IllegalStateException("Ingen instans valgt")
        val b = Request.Builder().url(root + path).header("Accept", "application/json")
        if (auth) store.token?.let { b.header("Authorization", "Bearer $it") }
        return b
    }

    private suspend fun <T> exec(request: Request, parse: (String) -> T): T =
        withContext(Dispatchers.IO) {
            http.newCall(request).execute().use { r ->
                val body = r.body?.string().orEmpty()
                if (!r.isSuccessful) throw ApiException(r.code, errorMessage(body) ?: "HTTP ${r.code}")
                parse(body)
            }
        }

    private fun errorMessage(body: String): String? = runCatching {
        json.parseToJsonElement(body).jsonObject["error"]?.jsonPrimitive?.contentOrNull
    }.getOrNull()

    private suspend inline fun <reified T> get(path: String): T =
        exec(builder(path).get().build()) { json.decodeFromString<T>(it) }

    private suspend inline fun <reified T> post(path: String, body: RequestBody = FormBody.Builder().build()): T =
        exec(builder(path).post(body).build()) { json.decodeFromString<T>(it) }

    private fun withPaging(path: String, maxId: String?, limit: Int = 20): String {
        val sep = if ('?' in path) '&' else '?'
        return path + sep + "limit=$limit" + (maxId?.let { "&max_id=$it" } ?: "")
    }

    // ---------- Innlogging (OAuth) ----------

    fun normalizeInstance(input: String): String {
        var s = input.trim().trimEnd('/')
        if (!s.startsWith("http://") && !s.startsWith("https://")) {
            s = "https://" + s.substringAfterLast('@')
        }
        require(s.length > 8 && '.' in s) { "ugyldig instans" }
        return s
    }

    suspend fun registerApp(base: String): AppRegistration {
        val body = FormBody.Builder()
            .add("client_name", "Feditastic")
            .add("redirect_uris", REDIRECT_URI)
            .add("scopes", SCOPES)
            .add("website", WEBSITE)
            .build()
        return exec(builder("/api/v1/apps", base, auth = false).post(body).build()) {
            json.decodeFromString<AppRegistration>(it)
        }
    }

    fun authorizeUrl(base: String, clientId: String): String =
        Uri.parse("$base/oauth/authorize").buildUpon()
            .appendQueryParameter("client_id", clientId)
            .appendQueryParameter("response_type", "code")
            .appendQueryParameter("redirect_uri", REDIRECT_URI)
            .appendQueryParameter("scope", SCOPES)
            // Be om innloggingsskjema selv om nettleseren er innlogget fra før (ved «Legg til bruker»)
            .appendQueryParameter("force_login", "true")
            .build().toString()

    suspend fun exchangeToken(base: String, clientId: String, clientSecret: String, code: String): String {
        val body = FormBody.Builder()
            .add("client_id", clientId)
            .add("client_secret", clientSecret)
            .add("grant_type", "authorization_code")
            .add("code", code)
            .add("redirect_uri", REDIRECT_URI)
            .add("scope", SCOPES)
            .build()
        return exec(builder("/oauth/token", base, auth = false).post(body).build()) {
            json.decodeFromString<TokenResponse>(it).accessToken
        }
    }

    // ---------- Instans og konto ----------

    suspend fun instanceInfo(): InstanceInfo = get("/api/v1/instance")

    /**
     * Innholdet Pleroma-FE viser på /about: instansens egen panel-tekst og tjenestevilkårene.
     * Begge er HTML; en side som mangler (404 o.l.) hoppes over, mens nettverksfeil kastes videre.
     */
    suspend fun instanceAboutHtml(): List<String> =
        listOf("/instance/panel.html", "/static/terms-of-service.html").mapNotNull { path ->
            try {
                exec(builder(path, auth = false).header("Accept", "text/html").get().build()) { it }
            } catch (e: ApiException) {
                null
            }?.let(::htmlBody)?.takeIf { it.isNotBlank() }
        }

    /**
     * Innholdet Mastodon viser på /about (der siden lages med JavaScript): den lange beskrivelsen og
     * serverreglene. Endepunkter instansen ikke har (404 o.l.) gir tomt svar.
     */
    suspend fun extendedDescription(): String =
        try { get<ExtendedDescription>("/api/v1/instance/extended_description").content } catch (e: ApiException) { "" }

    suspend fun instanceRules(): List<InstanceRule> =
        try { get("/api/v1/instance/rules") } catch (e: ApiException) { emptyList() }

    /** Plukker ut innholdet i `<body>` når siden er et helt HTML-dokument. */
    private fun htmlBody(html: String): String =
        Regex("(?is)<body[^>]*>(.*)</body>").find(html)?.groupValues?.get(1)?.trim() ?: html.trim()
    suspend fun verifyCredentials(): Account = get("/api/v1/accounts/verify_credentials")
    suspend fun account(id: String): Account = get("/api/v1/accounts/$id")
    suspend fun relationship(id: String): Relationship? =
        get<List<Relationship>>("/api/v1/accounts/relationships?id[]=$id").firstOrNull()

    suspend fun follow(id: String, on: Boolean): Relationship =
        post("/api/v1/accounts/$id/${if (on) "follow" else "unfollow"}")

    // ---------- Tidslinjer ----------

    /** [onlyMedia] gjelder bare offentlige tidslinjer (hjemmetidslinjen støtter det ikke). */
    suspend fun timeline(kind: TimelineKind, maxId: String?, onlyMedia: Boolean = false): List<Status> {
        val media = if (onlyMedia) "only_media=true" else ""
        val path = when (kind) {
            TimelineKind.HOME -> "/api/v1/timelines/home"
            TimelineKind.LOCAL -> "/api/v1/timelines/public?local=true&$media"
            TimelineKind.FEDERATED -> "/api/v1/timelines/public?$media"
        }.trimEnd('?', '&')
        return get(withPaging(path, maxId))
    }

    suspend fun listTimeline(listId: String, maxId: String?): List<Status> =
        get(withPaging("/api/v1/timelines/list/$listId", maxId))

    suspend fun tagTimeline(tag: String, maxId: String?): List<Status> =
        get(withPaging("/api/v1/timelines/tag/${Uri.encode(tag)}", maxId))

    /** [type] er «accounts» eller «hashtags». [resolve] ber tjeneren slå opp eksterne brukere (@navn@domene). */
    suspend fun search(q: String, type: String, resolve: Boolean): SearchResponse =
        get("/api/v2/search?q=${Uri.encode(q)}&type=$type&limit=20" + if (resolve) "&resolve=true" else "")

    suspend fun lists(): List<MastoList> = get("/api/v1/lists")

    /** Listene (av mine) som en konto ligger i. */
    suspend fun accountLists(accountId: String): List<MastoList> = get("/api/v1/accounts/$accountId/lists")

    suspend fun createList(title: String): MastoList =
        post("/api/v1/lists", FormBody.Builder().add("title", title).build())

    suspend fun setInList(listId: String, accountId: String, add: Boolean) {
        val body = FormBody.Builder().add("account_ids[]", accountId).build()
        val req = builder("/api/v1/lists/$listId/accounts")
        exec((if (add) req.post(body) else req.delete(body)).build()) { }
    }

    suspend fun customEmojis(): List<Emoji> = get("/api/v1/custom_emojis")

    suspend fun accountStatuses(id: String, maxId: String?): List<Status> =
        get(withPaging("/api/v1/accounts/$id/statuses", maxId))

    /**
     * [onlyMentions] filtrerer på serveren. Pleroma bruker `include_types[]`, Mastodon `types[]` –
     * begge sendes, og tjenere ignorerer parameteren de ikke kjenner. Svar til deg kommer som «mention».
     */
    suspend fun notifications(maxId: String?, onlyMentions: Boolean = false): List<Notification> {
        val path = if (onlyMentions) {
            "/api/v1/notifications?include_types[]=mention&types[]=mention"
        } else "/api/v1/notifications"
        return get(withPaging(path, maxId))
    }

    suspend fun status(id: String): Status = get("/api/v1/statuses/$id")
    suspend fun statusContext(id: String): StatusContext = get("/api/v1/statuses/$id/context")

    // ---------- Handlinger på innlegg ----------

    suspend fun favourite(id: String, on: Boolean): Status =
        post("/api/v1/statuses/$id/${if (on) "favourite" else "unfavourite"}")

    suspend fun reblog(id: String, on: Boolean): Status =
        post("/api/v1/statuses/$id/${if (on) "reblog" else "unreblog"}")

    suspend fun bookmark(id: String, on: Boolean): Status =
        post("/api/v1/statuses/$id/${if (on) "bookmark" else "unbookmark"}")

    suspend fun deleteStatus(id: String) {
        exec(builder("/api/v1/statuses/$id").delete().build()) { }
    }

    suspend fun vote(pollId: String, choices: List<Int>): Poll {
        val body = buildJsonObject {
            put("choices", JsonArray(choices.map { JsonPrimitive(it) }))
        }.toString().toRequestBody(jsonType)
        return post("/api/v1/polls/$pollId/votes", body)
    }

    suspend fun postStatus(
        text: String,
        visibility: String,
        spoiler: String?,
        replyTo: String?,
        mediaIds: List<String>,
        contentType: String?,
        language: String? = null,
    ): Status {
        val body = buildJsonObject {
            put("status", text)
            put("visibility", visibility)
            if (language != null) put("language", language)
            if (!spoiler.isNullOrBlank()) put("spoiler_text", spoiler)
            if (replyTo != null) put("in_reply_to_id", replyTo)
            if (mediaIds.isNotEmpty()) put("media_ids", JsonArray(mediaIds.map { JsonPrimitive(it) }))
            if (contentType != null) put("content_type", contentType)
        }.toString().toRequestBody(jsonType)
        val req = builder("/api/v1/statuses")
            .header("Idempotency-Key", UUID.randomUUID().toString())
            .post(body).build()
        return exec(req) { json.decodeFromString<Status>(it) }.also { store.rememberPosted(it.id) }
    }

    /** Pleroma: gir også mulighet til å endre alt-tekst etter at bildet er festet til et innlegg. */
    suspend fun updateMedia(id: String, description: String) {
        val body = buildJsonObject { put("description", description) }.toString().toRequestBody(jsonType)
        exec(builder("/api/v1/media/$id").put(body).build()) { }
    }

    suspend fun statusSource(id: String): StatusSource = get("/api/v1/statuses/$id/source")

    suspend fun editStatus(
        id: String,
        text: String,
        spoiler: String?,
        mediaIds: List<String>,
        contentType: String?,
        language: String? = null,
    ): Status {
        val body = buildJsonObject {
            put("status", text)
            if (language != null) put("language", language)
            put("spoiler_text", spoiler.orEmpty())
            put("media_ids", JsonArray(mediaIds.map { JsonPrimitive(it) }))
            if (contentType != null) put("content_type", contentType)
        }.toString().toRequestBody(jsonType)
        return exec(builder("/api/v1/statuses/$id").put(body).build()) { json.decodeFromString<Status>(it) }
    }

    suspend fun uploadMedia(resolver: ContentResolver, uri: Uri): MediaUploaded {
        val (bytes, mime) = withContext(Dispatchers.IO) {
            val type = resolver.getType(uri) ?: "application/octet-stream"
            val data = resolver.openInputStream(uri)?.use { it.readBytes() }
                ?: throw IOException("Kunne ikke lese filen")
            data to type
        }
        val ext = MimeTypeMap.getSingleton().getExtensionFromMimeType(mime) ?: "bin"
        val body = MultipartBody.Builder().setType(MultipartBody.FORM)
            .addFormDataPart("file", "opplasting.$ext", bytes.toRequestBody(mime.toMediaType()))
            .build()
        return exec(builder("/api/v1/media").post(body).build()) {
            json.decodeFromString<MediaUploaded>(it)
        }
    }

    companion object {
        const val REDIRECT_URI = "pleromastisk://oauth"
        const val WEBSITE = "https://github.com/Gauteweb/Feditastic"
        const val SCOPES = "read write follow push"
    }
}
