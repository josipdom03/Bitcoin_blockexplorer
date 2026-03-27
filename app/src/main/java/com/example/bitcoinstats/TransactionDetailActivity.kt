package com.example.bitcoinstats

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.bitcoinstats.databinding.ActivityTransactionDetailBinding
import com.example.bitcoinstats.repository.BlockchainRepository
import com.example.bitcoinstats.repository.PriceRepository
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.util.*
import kotlin.concurrent.thread

class TransactionDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTransactionDetailBinding
    private val blockRepo = BlockchainRepository()
    private val priceRepo = PriceRepository()
    private val gson = Gson()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTransactionDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Detalji Transakcije"

        val txId = intent.getStringExtra("TX_ID") ?: return
        loadTransactionData(txId)
    }

    private fun loadTransactionData(txId: String) {
        binding.textViewTxIdDetail.text = txId

        thread {
            try {
                val rawJson = blockRepo.getRawTransaction(txId) ?: return@thread
                val txObj = gson.fromJson(rawJson, JsonObject::class.java)

                var blockHeight = -1
                var unixTime = 0L

                if (txObj.has("blockheight")) blockHeight = txObj.get("blockheight").asInt
                if (txObj.has("time")) unixTime = txObj.get("time").asLong

                if (txObj.has("status")) {
                    val status = txObj.getAsJsonObject("status")
                    if (status.has("confirmed") && status.get("confirmed").asBoolean) {
                        blockHeight = if (status.has("block_height")) status.get("block_height").asInt else blockHeight
                        unixTime = if (status.has("block_time")) status.get("block_time").asLong else unixTime
                    }
                }

                val dateForAPI = if (unixTime != 0L) {
                    val date = Date(unixTime * 1000L)
                    val sdf = SimpleDateFormat("dd-MM-yyyy", Locale.US)
                    sdf.format(date)
                } else null

                val vinArray = txObj.getAsJsonArray("vin") ?: JsonArray()
                val voutArray = txObj.getAsJsonArray("vout") ?: JsonArray()

                var totalValueBtc = BigDecimal.ZERO
                voutArray.forEach { element ->
                    val obj = element.asJsonObject
                    if (obj.has("value")) {
                        totalValueBtc = totalValueBtc.add(obj.get("value").asBigDecimal)
                    }
                }

                // DOHVAT CIJENA (USD i EUR)
                val currentPrices = priceRepo.getCurrentPrices()
                Thread.sleep(600) // Rate limit protection
                val pricesThen = if (dateForAPI != null) priceRepo.getPricesByDate(dateForAPI) else null

                runOnUiThread {
                    binding.textViewTxTime.text = if (unixTime != 0L) formatUnixTime(unixTime) else "Nepotvrđeno"
                    binding.textViewTotalValue.text = "${totalValueBtc.setScale(8, RoundingMode.HALF_UP).toPlainString()} BTC"

                    // PRIKAZ TRENUTNIH CIJENA I VRIJEDNOSTI
                    if (currentPrices != null) {
                        val pNowUsd = currentPrices["usd"] ?: 0.0
                        val pNowEur = currentPrices["eur"] ?: 0.0
                        val totalNowUsd = totalValueBtc.multiply(BigDecimal.valueOf(pNowUsd))
                        val totalNowEur = totalValueBtc.multiply(BigDecimal.valueOf(pNowEur))

                        binding.textViewPriceNow.text = String.format(Locale.US, "$%,.2f / €%,.2f", pNowUsd, pNowEur)
                        binding.textViewValueNowTotal.text = String.format(Locale.US, "$%,.2f / €%,.2f", totalNowUsd, totalNowEur)
                    }

                    // PRIKAZ POVIJESNIH CIJENA I VRIJEDNOSTI
                    if (pricesThen != null) {
                        val pThenUsd = pricesThen["usd"] ?: 0.0
                        val pThenEur = pricesThen["eur"] ?: 0.0
                        val totalThenUsd = totalValueBtc.multiply(BigDecimal.valueOf(pThenUsd))
                        val totalThenEur = totalValueBtc.multiply(BigDecimal.valueOf(pThenEur))

                        binding.textViewPriceThen.text = String.format(Locale.US, "$%,.2f / €%,.2f", pThenUsd, pThenEur)
                        binding.textViewValueThenTotal.text = String.format(Locale.US, "$%,.2f / €%,.2f", totalThenUsd, totalThenEur)
                    } else {
                        binding.textViewPriceThen.text = "N/A"
                        binding.textViewValueThenTotal.text = "N/A"
                    }

                    displayFullDetails(vinArray, voutArray)
                }

                val fee = blockRepo.calculateTransactionFee(txId)
                runOnUiThread {
                    binding.textViewTxFee.text = "${fee.setScale(8, RoundingMode.HALF_UP).toPlainString()} BTC"
                }

            } catch (e: Exception) {
                Log.e("TX_DETAIL", "Kritična greška: ${e.message}")
            }
        }
    }

    private fun displayFullDetails(vin: JsonArray, vout: JsonArray) {
        val container = binding.layoutInputsOutputs
        container.removeAllViews()

        container.addView(createSectionTitle("Ulazi (Inputs)"))
        if (vin.size() > 0 && vin[0].asJsonObject.has("coinbase")) {
            container.addView(createDataRow("Nagrada rudaru (Coinbase)", "#6C757D"))
        } else {
            vin.forEach { it ->
                val obj = it.asJsonObject
                val prevTxId = if (obj.has("txid")) obj.get("txid").asString else "Nepoznato"
                container.addView(createDataRow("Iz TX: $prevTxId", "#ADB5BD", if (prevTxId != "Nepoznato") prevTxId else null))
            }
        }

        container.addView(createSectionTitle("Izlazi (Outputs)"))
        vout.forEach { it ->
            val obj = it.asJsonObject
            val btc = if (obj.has("value")) obj.get("value").asBigDecimal else BigDecimal.ZERO
            var address = "Nepoznata adresa"
            if (obj.has("scriptPubKey")) {
                val spk = obj.getAsJsonObject("scriptPubKey")
                when {
                    spk.has("address") -> address = spk.get("address").asString
                    spk.has("addresses") -> {
                        val addrs = spk.getAsJsonArray("addresses")
                        if (addrs.size() > 0) address = addrs[0].asString
                    }
                }
            }
            container.addView(createDataRow("${btc.toPlainString()} BTC -> $address", "#40C057"))
        }
    }

    private fun formatUnixTime(unixTime: Long): String {
        val date = Date(unixTime * 1000L)
        val sdf = SimpleDateFormat("dd.MM.yyyy. HH:mm", Locale.getDefault())
        return sdf.format(date)
    }

    private fun createSectionTitle(title: String): TextView {
        return TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            text = title
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            setPadding(16, 48, 16, 12)
            setTextColor(Color.parseColor("#1A1A1A"))
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
    }

    private fun createDataRow(content: String, colorHex: String, navigationTxId: String? = null): TextView {
        return TextView(this).apply {
            val params = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            params.setMargins(0, 4, 0, 4)
            layoutParams = params
            text = content
            setTextColor(Color.parseColor(colorHex))
            setPadding(32, 32, 32, 32)
            textSize = 12f
            setBackgroundResource(android.R.drawable.editbox_dropdown_light_frame)
            setTextIsSelectable(true)
            if (!navigationTxId.isNullOrEmpty()) {
                setCompoundDrawablesWithIntrinsicBounds(0, 0, android.R.drawable.ic_menu_directions, 0)
                setOnClickListener {
                    val nextIntent = Intent(context, TransactionDetailActivity::class.java)
                    nextIntent.putExtra("TX_ID", navigationTxId)
                    startActivity(nextIntent)
                }
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}