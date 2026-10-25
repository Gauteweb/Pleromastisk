package app.pleromastisk.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull

// Modellene dekker Mastodon-kompatibelt API som Pleroma (og f.eks. Akkoma) implementerer.
// Alle felt har standardverdier slik at avvikende instanser ikke knekker parsingen.

enum class TimelineKind { HOME, LOCAL, FEDERATED }

@Serializable
data class Emoji(
    val shortcode: String,
    val url: String = "",
    @SerialName("static_url") val staticUrl: String = "",
    val category: String? = null,
    @SerialName("visible_in_picker") val visibleInPicker: Boolean = true,
)

@Serializable
data class MastoList(
    val id: String,
    val title: String = "",
)

/** Egendefinert profilfelt («metadata»), f.eks. «Nettside» → lenke. [value] er HTML. */
@Serializable
data class AccountField(
    val name: String = "",
    val value: String = "",
    @SerialName("verified_at") val verifiedAt: String? = null,
)

@Serializable
data class Account(
    val id: String,
    val username: String = "",
    val acct: String = "",
    @SerialName("display_name") val displayName: String = "",
    val avatar: String = "",
    val header: String = "",
    val note: String = "",
    val url: String = "",
    @SerialName("followers_count") val followersCount: Int = 0,
    @SerialName("following_count") val followingCount: Int = 0,
    @SerialName("statuses_count") val statusesCount: Int = 0,
    val emojis: List<Emoji> = emptyList(),
    val bot: Boolean = false,
    val locked: Boolean = false,
    val fields: List<AccountField> = emptyList(),
) {
    val name: String get() = displayName.ifBlank { username }
}

@Serializable
data class MediaAttachment(
    val id: String = "",
    val type: String = "image",
    val url: String = "",
    @SerialName("preview_url") val previewUrl: String? = null,
    val description: String? = null,
    /** Rå `meta`-blokk (størrelse m.m.). Tolkes defensivt – formatet varierer mellom tjenere. */
    val meta: JsonElement? = null,
) {
    /** Bredde/høyde-forhold hvis instansen oppgir det. */
    val aspectRatio: Float?
        get() = runCatching {
            val o = (meta as? JsonObject)?.get("original") as? JsonObject
            val aspect = (o?.get("aspect") as? JsonPrimitive)?.doubleOrNull
            val w = (o?.get("width") as? JsonPrimitive)?.doubleOrNull
            val h = (o?.get("height") as? JsonPrimitive)?.doubleOrNull
            when {
                aspect != null && aspect > 0 -> aspect.toFloat()
                w != null && h != null && w > 0 && h > 0 -> (w / h).toFloat()
                else -> null
            }
        }.getOrNull()
}

@Serializable
data class Mention(
    val id: String = "",
    val username: String = "",
    val acct: String = "",
    val url: String = "",
)

@Serializable
data class PollOption(
    val title: String = "",
    @SerialName("votes_count") val votesCount: Int? = 0,
)

@Serializable
data class Poll(
    val id: String,
    val expired: Boolean = false,
    val multiple: Boolean = false,
    @SerialName("votes_count") val votesCount: Int = 0,
    val options: List<PollOption> = emptyList(),
    val voted: Boolean = false,
    @SerialName("own_votes") val ownVotes: List<Int>? = null,
)

@Serializable
data class Status(
    val id: String,
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("in_reply_to_id") val inReplyToId: String? = null,
    val sensitive: Boolean = false,
    @SerialName("spoiler_text") val spoilerText: String = "",
    val visibility: String = "public",
    val uri: String = "",
    val url: String? = null,
    @SerialName("replies_count") val repliesCount: Int = 0,
    @SerialName("reblogs_count") val reblogsCount: Int = 0,
    @SerialName("favourites_count") val favouritesCount: Int = 0,
    val content: String = "",
    val reblog: Status? = null,
    val account: Account,
    @SerialName("media_attachments") val mediaAttachments: List<MediaAttachment> = emptyList(),
    val mentions: List<Mention> = emptyList(),
    val emojis: List<Emoji> = emptyList(),
    val favourited: Boolean = false,
    val reblogged: Boolean = false,
    val bookmarked: Boolean = false,
    val poll: Poll? = null,
    val card: Card? = null,
    @SerialName("edited_at") val editedAt: String? = null,
    val application: AppInfo? = null,
    val language: String? = null,
)

@Serializable
data class StatusSource(
    val id: String = "",
    val text: String = "",
    @SerialName("spoiler_text") val spoilerText: String = "",
    @SerialName("content_type") val contentType: String? = null,
)

/** Forhåndsvisning av en lenke (Open Graph), laget av instansen. */
@Serializable
data class Card(
    val url: String = "",
    val title: String = "",
    val description: String = "",
    val type: String = "link",
    val image: String? = null,
    @SerialName("provider_name") val providerName: String? = null,
)

/** Appen innlegget ble postet fra (finnes typisk bare for lokale innlegg). */
@Serializable
data class AppInfo(
    val name: String = "",
    val website: String? = null,
)

/** Svar fra /api/v2/search. Emneknagger tolkes defensivt (objekter eller rene tekststrenger). */
@Serializable
data class SearchResponse(
    val accounts: List<Account> = emptyList(),
    val hashtags: List<JsonElement> = emptyList(),
) {
    val tagNames: List<String>
        get() = hashtags.mapNotNull { e ->
            when (e) {
                is JsonObject -> (e["name"] as? JsonPrimitive)?.contentOrNull
                is JsonPrimitive -> e.contentOrNull
                else -> null
            }
        }.filter { it.isNotBlank() }
}

@Serializable
data class StatusContext(
    val ancestors: List<Status> = emptyList(),
    val descendants: List<Status> = emptyList(),
)

@Serializable
data class Notification(
    val id: String,
    val type: String = "",
    @SerialName("created_at") val createdAt: String = "",
    val account: Account,
    val status: Status? = null,
    val emoji: String? = null,
)

@Serializable
data class Relationship(
    val id: String = "",
    val following: Boolean = false,
    @SerialName("followed_by") val followedBy: Boolean = false,
    val requested: Boolean = false,
)

@Serializable
data class MediaUploaded(val id: String)

@Serializable
data class AppRegistration(
    @SerialName("client_id") val clientId: String,
    @SerialName("client_secret") val clientSecret: String,
)

@Serializable
data class TokenResponse(@SerialName("access_token") val accessToken: String)

// /api/v1/instance – både Mastodon-formatet og Pleromas utvidelser
@Serializable
data class InstanceInfo(
    val title: String = "",
    /** Pleroma oppgir f.eks. «2.7.2 (compatible; Pleroma 2.6.3)». */
    val version: String = "",
    @SerialName("max_toot_chars") val maxTootChars: Int? = null,
    @SerialName("description_limit") val descriptionLimit: Int? = null,
    val configuration: InstanceConfig? = null,
    val pleroma: PleromaInfo? = null,
) {
    /** Pleroma (eller en forgrening som Akkoma), ikke Mastodon e.l. */
    val isPleroma: Boolean get() = pleroma != null || "pleroma" in version.lowercase()
}

@Serializable
data class InstanceConfig(val statuses: StatusesConfig? = null)

@Serializable
data class StatusesConfig(
    @SerialName("max_characters") val maxCharacters: Int? = null,
    /** Tekstformater for innlegg (GoToSocial, glitch-soc), f.eks. text/plain og text/markdown. */
    @SerialName("supported_mime_types") val supportedMimeTypes: List<String> = emptyList(),
)

@Serializable
data class PleromaInfo(val metadata: PleromaMetadata? = null)

@Serializable
data class PleromaMetadata(@SerialName("post_formats") val postFormats: List<String> = emptyList(),
    val features: List<String> = emptyList(),
)
