package net.grafixmafia.spacestate.details

import java.net.URI
import java.net.URLEncoder
import java.net.URLDecoder

/** Provider values stay readable when they cannot safely become an external action. */
object DetailLinks {
    private val encodedControl = Regex("%(?:0[0-9a-f]|1[0-9a-f]|7f)", RegexOption.IGNORE_CASE)
    private fun controls(value: String): Boolean = value.any { it.isISOControl() } || encodedControl.containsMatchIn(value) ||
        runCatching { URLDecoder.decode(value, "UTF-8") }.getOrDefault("").any { it.isISOControl() }

    fun web(value: String?): String? = value?.let { hosted(it, setOf("http", "https")) }
    fun endpoint(value: String?): String? = web(value)

    private fun hosted(value: String, schemes: Set<String>): String? {
        if (controls(value)) return null
        val uri = runCatching { URI(value) }.getOrNull() ?: return null
        if (uri.scheme?.lowercase() !in schemes || uri.isOpaque || uri.rawUserInfo != null) return null
        val host = uri.host?.takeIf { it.isNotEmpty() } ?: return null
        val port = uri.port
        if (port != -1 && port !in 1..65535) return null
        val authority = host + if (port == -1) "" else ":$port"
        if (!authority.equals(uri.rawAuthority, ignoreCase = true)) return null
        return uri.toASCIIString()
    }

    fun contact(kind: String, value: String): String? = when (kind) {
        "email", "issueMail" -> address(value, "mailto")
        "phone" -> phone(value)
        "mailingList" -> web(value) ?: address(value, "mailto")
        "irc" -> hosted(value, setOf("http", "https", "irc", "ircs"))
        "mumble" -> hosted(value, setOf("http", "https", "mumble"))
        "gopher" -> hosted(value, setOf("http", "https", "gopher"))
        "xmpp" -> address(value, "xmpp")
        "sip" -> {
            val scheme = if (value.startsWith("sips:", ignoreCase = true)) "sips" else "sip"
            val raw = value.removeScheme(scheme)
            number(raw)?.let { opaque(scheme, it) } ?: address(value, scheme)
        }
        "matrix" -> web(value) ?: value.takeIf {
            it.isNotEmpty() && it[0] in "#@!" && it.indexOf(':') > 1 && it.substringAfter(':', "").isNotBlank() &&
                !it.any(Char::isWhitespace) && !controls(it)
        }?.let { "https://matrix.to/#/" + URLEncoder.encode(it, "UTF-8").replace("+", "%20") }
        "mastodon" -> web(value) ?: value.removePrefix("@").split('@').takeIf { it.size == 2 }?.let { parts ->
            if (Regex("[A-Za-z0-9_.-]+").matches(parts[0]) && Regex("[A-Za-z0-9.-]+").matches(parts[1]))
                web("https://${parts[1]}/@${parts[0]}") else null
        }
        else -> null
    }

    private fun String.removeScheme(scheme: String) = if (startsWith("$scheme:", ignoreCase = true)) substring(scheme.length + 1) else this
    private fun address(value: String, scheme: String): String? {
        val raw = value.removeScheme(scheme)
        if (controls(raw) || raw.any(Char::isWhitespace) || raw.any { it in ":/?#\\\"" }) return null
        val parts = raw.split('@')
        if (parts.size != 2 || parts.any { it.isEmpty() }) return null
        return opaque(scheme, raw)
    }
    private fun number(value: String): String? {
        val number = value.filterNot { it.isWhitespace() || it in "()-." }
        val digits = number.removePrefix("+")
        return number.takeIf { digits.isNotEmpty() && digits.all { it in '0'..'9' } }
    }
    private fun phone(value: String) = number(value.removeScheme("tel"))?.let { opaque("tel", it) }
    private fun opaque(scheme: String, value: String): String? = runCatching { URI(scheme, value, null).toASCIIString() }.getOrNull()

    fun map(latitude: Double?, longitude: Double?): String? {
        if (latitude == null || longitude == null || !latitude.isFinite() || !longitude.isFinite() ||
            latitude !in -90.0..90.0 || longitude !in -180.0..180.0) return null
        return "geo:$latitude,$longitude?q=$latitude,$longitude"
    }
}
