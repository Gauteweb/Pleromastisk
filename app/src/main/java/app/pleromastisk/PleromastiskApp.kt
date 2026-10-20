package app.pleromastisk

import android.app.Application
import android.content.Context
import android.widget.Toast
import androidx.annotation.StringRes
import app.pleromastisk.data.ApiClient
import app.pleromastisk.data.ApiException
import app.pleromastisk.data.Emoji
import app.pleromastisk.data.SessionStore
import kotlinx.coroutines.flow.MutableSharedFlow
import java.io.IOException
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

class PleromastiskApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Graph.init(this)
    }
}

/** Enkel «service locator» – holder på det som deles mellom skjermene. */
object Graph {
    lateinit var appContext: Context
        private set
    lateinit var store: SessionStore
        private set
    lateinit var api: ApiClient
        private set

    /** Utløses når brukeren har publisert noe, slik at tidslinjene kan oppdateres. */
    val posted = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    fun init(app: Application) {
        appContext = app.applicationContext
        store = SessionStore(app)
        api = ApiClient(store)
    }

    private var emojiCache: Pair<String?, List<Emoji>>? = null

    /** Instansens egendefinerte emojier, hentet én gang per instans. */
    fun cachedEmojis(): List<Emoji>? = emojiCache?.takeIf { it.first == store.instance }?.second

    suspend fun loadEmojis(): List<Emoji> {
        val list = api.customEmojis()
            .filter { it.visibleInPicker }
            .sortedWith(compareBy({ it.category.orEmpty() }, { it.shortcode }))
        emojiCache = store.instance to list
        return list
    }

    fun str(@StringRes id: Int, vararg args: Any): String = appContext.getString(id, *args)

    fun toast(message: String) = Toast.makeText(appContext, message, Toast.LENGTH_LONG).show()
}

/** Gjør unntak om til norske meldinger brukeren kan forstå. */
fun Throwable.userMessage(): String = when {
    this is ApiException && code == 401 -> Graph.str(R.string.error_session_expired)
    this is ApiException -> Graph.str(R.string.error_generic, message ?: "HTTP $code")
    this is IOException -> Graph.str(R.string.error_network)
    else -> Graph.str(R.string.error_generic, message ?: javaClass.simpleName)
}

/** «nå», «5 min», «3 t», «2 d», ellers dato. */
fun relativeTime(iso: String): String {
    val instant = runCatching { OffsetDateTime.parse(iso).toInstant() }
        .recoverCatching { Instant.parse(iso) }
        .getOrNull() ?: return ""
    val secs = (Instant.now().epochSecond - instant.epochSecond).coerceAtLeast(0)
    return when {
        secs < 60 -> Graph.str(R.string.time_now)
        secs < 3600 -> Graph.str(R.string.time_min, secs / 60)
        secs < 86400 -> Graph.str(R.string.time_hour, secs / 3600)
        secs < 7 * 86400 -> Graph.str(R.string.time_day, secs / 86400)
        else -> DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
            .withZone(ZoneId.systemDefault()).format(instant)
    }
}
