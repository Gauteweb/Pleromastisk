package app.pleromastisk.ui

/** Språkene som kan velges for et innlegg (ISO 639-1): Nynorsk, Bokmål og Engelsk. */
val PostLanguages = listOf("nn", "nb", "en")

/**
 * Gjør et språk fra serveren (f.eks. «nb-NO», «no», «EN») om til et av valgene våre, eller null
 * hvis det er et annet språk. «no» (norsk) regnes som bokmål.
 */
fun normalizePostLanguage(code: String?): String? {
    val base = code?.trim()?.lowercase()?.substringBefore('-')?.substringBefore('_') ?: return null
    val mapped = if (base == "no") "nb" else base
    return mapped.takeIf { it in PostLanguages }
}
