package app.furibudget.data

import app.furibudget.core.Currency
import app.furibudget.core.Rates
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * The only network use in the app, and only when the user switches "Update exchange rates online" on.
 * One GET to a public no-key endpoint; the request carries just the main currency's code.
 */
object RateFetcher {
    @Serializable
    private data class Reply(val result: String = "", val rates: Map<String, Double> = emptyMap())

    private val json = Json { ignoreUnknownKeys = true }

    /** Turns the endpoint's reply into rates for the currencies the app knows. Empty when the reply is not a success. */
    fun parse(body: String, base: String): Map<String, Long> {
        val reply = runCatching { json.decodeFromString<Reply>(body) }.getOrNull() ?: return emptyMap()
        if (reply.result != "success") return emptyMap()
        val out = HashMap<String, Long>()
        for (c in Currency.all) {
            if (c.code == base) continue
            val perBase = reply.rates[c.code] ?: continue
            Rates.fromPerBase(perBase)?.let { out[c.code] = it }
        }
        return out
    }

    /** Fetches the rates for [base]. Null on any failure (offline, timeout, bad reply): the caller keeps what it cached. */
    suspend fun fetch(base: String): Map<String, Long>? = withContext(Dispatchers.IO) {
        runCatching {
            val c = URL("https://open.er-api.com/v6/latest/$base").openConnection() as HttpURLConnection
            try {
                c.connectTimeout = 10_000; c.readTimeout = 10_000; c.requestMethod = "GET"
                c.setRequestProperty("Accept", "application/json")
                if (c.responseCode != 200) return@runCatching null
                val body = c.inputStream.bufferedReader().use { it.readText().take(500_000) }
                parse(body, base).takeIf { it.isNotEmpty() }
            } finally { c.disconnect() }
        }.getOrNull()
    }
}
