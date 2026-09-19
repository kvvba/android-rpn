package com.jakub.rpncalculator.helpers

import android.content.Context
import org.json.JSONObject
import java.math.BigDecimal
import java.net.HttpURLConnection
import java.net.URL

/**
 * Holds USD-based exchange rates in memory, persisted to disk after a manual refresh so the
 * currency converter keeps working offline with the last fetched rates. Never fetches on its
 * own; [refresh] must be triggered explicitly (e.g. a button tap).
 */
object CurrencyRatesStore {
    private const val PREFS_NAME = "currency_rates"
    private const val KEY_RATES = "rates_json"
    private const val KEY_UPDATED_AT = "updated_at"
    private const val API_URL = "https://open.er-api.com/v6/latest/USD"

    // Approximate snapshot, used only until the user fetches live rates for the first time.
    private val fallbackRates: Map<String, BigDecimal> = mapOf(
        "USD" to "1", "EUR" to "0.92", "GBP" to "0.79", "JPY" to "149.5", "CHF" to "0.88",
        "CAD" to "1.36", "AUD" to "1.53", "CNY" to "7.18", "PLN" to "3.95", "HUF" to "356",
        "SEK" to "10.4", "NOK" to "10.6", "DKK" to "6.87", "CZK" to "22.7", "INR" to "83.3",
        "BRL" to "5.4", "MXN" to "17.0", "ZAR" to "18.6", "NZD" to "1.66", "SGD" to "1.34",
        "KRW" to "1330", "TRY" to "32.5", "HKD" to "7.82", "ILS" to "3.7", "AED" to "3.67"
    ).mapValues { BigDecimal(it.value) }

    private var appContext: Context? = null
    private var loadedFromDisk = false
    private var rates: Map<String, BigDecimal> = fallbackRates
    private var updatedAtMillis: Long = 0L

    /** Cheap to call repeatedly; only reads from disk once per process. */
    fun ensureLoaded(context: Context) {
        if (appContext == null) {
            appContext = context.applicationContext
        }
        if (loadedFromDisk) {
            return
        }
        loadedFromDisk = true

        val prefs = appContext!!.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        updatedAtMillis = prefs.getLong(KEY_UPDATED_AT, 0L)
        val json = prefs.getString(KEY_RATES, null) ?: return
        val parsed = parseRates(json)
        if (parsed != null) {
            rates = parsed
        }
    }

    fun rateFor(code: String): BigDecimal = rates[code] ?: BigDecimal.ONE

    fun lastUpdatedMillis(): Long = updatedAtMillis

    fun hasLiveRates(): Boolean = updatedAtMillis > 0L

    /** Fetches fresh rates on a background thread; [onResult] runs on the calling (main) thread. */
    fun refresh(onResult: (success: Boolean) -> Unit) {
        val context = appContext
        val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())
        if (context == null) {
            onResult(false)
            return
        }

        Thread {
            val success = fetchAndStore(context)
            mainHandler.post { onResult(success) }
        }.start()
    }

    private fun fetchAndStore(context: Context): Boolean {
        return try {
            val connection = URL(API_URL).openConnection() as HttpURLConnection
            connection.connectTimeout = 10000
            connection.readTimeout = 10000
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            val ratesObj = JSONObject(body).getJSONObject("rates")
            val map = mutableMapOf<String, BigDecimal>()
            ratesObj.keys().forEach { code -> map[code] = BigDecimal(ratesObj.getString(code)) }
            if (map.isEmpty()) {
                return false
            }

            rates = map
            updatedAtMillis = System.currentTimeMillis()

            val ratesJson = JSONObject(map.mapValues { it.value.toPlainString() }).toString()
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
                .putString(KEY_RATES, ratesJson)
                .putLong(KEY_UPDATED_AT, updatedAtMillis)
                .apply()
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun parseRates(json: String): Map<String, BigDecimal>? {
        return try {
            val obj = JSONObject(json)
            val map = mutableMapOf<String, BigDecimal>()
            obj.keys().forEach { code -> map[code] = BigDecimal(obj.getString(code)) }
            map.ifEmpty { null }
        } catch (_: Exception) {
            null
        }
    }
}
