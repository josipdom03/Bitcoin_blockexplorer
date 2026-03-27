package com.example.bitcoinstats.repository

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.*

class PriceRepository {
    private val client = OkHttpClient()
    private val gson = Gson()

    // Vraća mapu cijena: "usd" -> cijena, "eur" -> cijena
    fun getPricesByDate(date: String): Map<String, Double>? {
        val url = "https://api.coingecko.com/api/v3/coins/bitcoin/history?date=$date"
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0")
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val jsonObject = gson.fromJson(response.body?.string(), JsonObject::class.java)
                    val currentPrice = jsonObject?.getAsJsonObject("market_data")?.getAsJsonObject("current_price")

                    val map = mutableMapOf<String, Double>()
                    currentPrice?.get("usd")?.let { map["usd"] = it.asDouble }
                    currentPrice?.get("eur")?.let { map["eur"] = it.asDouble }
                    map
                } else null
            }
        } catch (e: Exception) {
            Log.e("PriceRepo", "Greška povijest: ${e.message}")
            null
        }
    }

    // Vraća trenutne cijene za obje valute odjednom
    fun getCurrentPrices(): Map<String, Double>? {
        val url = "https://api.coingecko.com/api/v3/simple/price?ids=bitcoin&vs_currencies=usd,eur"
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0")
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val jsonObject = gson.fromJson(response.body?.string(), JsonObject::class.java)
                    val bitcoin = jsonObject?.getAsJsonObject("bitcoin")

                    val map = mutableMapOf<String, Double>()
                    bitcoin?.get("usd")?.let { map["usd"] = it.asDouble }
                    bitcoin?.get("eur")?.let { map["eur"] = it.asDouble }
                    map
                } else null
            }
        } catch (e: Exception) {
            Log.e("PriceRepo", "Greška trenutna: ${e.message}")
            null
        }
    }
}