package com.example.bitcoinstats

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.bitcoinstats.databinding.ActivityBlockDetailBinding
import com.example.bitcoinstats.model.Transaction
import com.example.bitcoinstats.repository.BlockchainRepository
import com.example.bitcoinstats.repository.PriceRepository
import com.google.gson.Gson
import com.google.gson.JsonObject
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.util.*
import kotlin.concurrent.thread

class BlockDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBlockDetailBinding
    private val blockRepo = BlockchainRepository()
    private val priceRepo = PriceRepository()
    private val gson = Gson()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBlockDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val hash = intent.getStringExtra("BLOCK_HASH") ?: return
        loadDetailData(hash)
    }

    private fun loadDetailData(hash: String) {
        runOnUiThread {
            binding.progressBar.visibility = View.VISIBLE
            binding.textViewDetailHash.text = hash
            binding.cardNext.visibility = View.INVISIBLE
            binding.textViewPrevHeight.text = "---"
        }

        thread {
            try {
                val blockJson = blockRepo.getBlockDetails(hash)

                if (blockJson != null && blockJson != "null") {
                    val blockObj: JsonObject = gson.fromJson(blockJson, JsonObject::class.java)

                    val prevHash = blockObj.get("previousblockhash")?.asString
                    val nextHash = blockObj.get("nextblockhash")?.asString
                    val heightInt = blockObj.get("height")?.asInt ?: 0
                    val height = heightInt.toString()

                    val nonce = blockObj.get("nonce")?.asLong?.toString() ?: "N/A"
                    val difficulty = blockObj.get("difficulty")?.asDouble ?: 0.0
                    val size = blockObj.get("size")?.asInt?.toString() ?: "N/A"
                    val merkleRoot = blockObj.get("merkleroot")?.asString ?: "N/A"
                    val timeStamp = blockObj.get("time")?.asLong ?: 0L
                    val dateStr = if (timeStamp > 0) {
                        SimpleDateFormat("dd.MM.yyyy. HH:mm", Locale.getDefault()).format(Date(timeStamp * 1000L))
                    } else "N/A"

                    val txArray = blockObj.getAsJsonArray("tx")
                    val totalTxCount = txArray?.size() ?: 0
                    var blockTotalBtc = BigDecimal.ZERO
                    val fullTransactions = mutableListOf<Transaction>()
                    var totalFeeBtc = BigDecimal.ZERO
                    var rewardBtc = "N/A"

                    if (txArray != null) {
                        for (i in 0 until txArray.size()) {
                            val txId = txArray.get(i).asString
                            val rawTxJson = blockRepo.getRawTransaction(txId)
                            if (rawTxJson != null) {
                                val txModel = gson.fromJson(rawTxJson, Transaction::class.java)
                                fullTransactions.add(txModel)

                                val txSum = txModel.vout.sumOf { it.value }
                                blockTotalBtc = blockTotalBtc.add(BigDecimal.valueOf(txSum))

                                if (i == 0) {
                                    val totalOut = txModel.vout.sumOf { it.value }
                                    rewardBtc = "%.2f BTC".format(totalOut)
                                } else {
                                    try {
                                        val fee = blockRepo.calculateTransactionFee(txId)
                                        totalFeeBtc = totalFeeBtc.add(fee)
                                    } catch (e: Exception) {}
                                }
                            }
                        }
                    }

                    val totalFeeSat = totalFeeBtc.multiply(BigDecimal(100_000_000)).setScale(0, RoundingMode.HALF_UP)


                    val prices = priceRepo.getCurrentPrices()
                    val pUsd = prices?.get("usd") ?: 0.0
                    val pEur = prices?.get("eur") ?: 0.0

                    runOnUiThread {
                        binding.textViewNavHeight.text = "#$height"

                        if (heightInt > 0) {
                            binding.textViewPrevHeight.text = (heightInt - 1).toString()
                            prevHash?.let { pHash ->
                                binding.cardPrevious.setOnClickListener { refreshWithNewBlock(pHash) }
                            }
                        }

                        if (nextHash != null) {
                            binding.textViewNextHeight.text = (heightInt + 1).toString()
                            binding.cardNext.visibility = View.VISIBLE
                            binding.cardNext.setOnClickListener { refreshWithNewBlock(nextHash) }
                        }

                        binding.textViewHeight.text = height
                        binding.textViewNonce.text = nonce
                        binding.textViewDifficulty.text = "%.0f".format(difficulty)
                        binding.textViewSize.text = "$size bytes"
                        binding.textViewMerkleRoot.text = merkleRoot
                        binding.textViewTime.text = dateStr
                        binding.textViewTxCount.text = totalTxCount.toString()
                        binding.textViewReward.text = rewardBtc
                        binding.textViewBlockTotal.text = "%.2f BTC".format(blockTotalBtc)

                        binding.textViewBtcPrice.text = String.format(Locale.US, "$%,.2f / €%,.2f", pUsd, pEur)

                        binding.textViewFee.text = "${totalFeeSat.toPlainString()} sat"

                        binding.recyclerViewTransactions.apply {
                            layoutManager = LinearLayoutManager(this@BlockDetailActivity)
                            adapter = TransactionAdapter(fullTransactions)
                            isNestedScrollingEnabled = false
                        }
                        binding.progressBar.visibility = View.GONE
                    }
                }
            } catch (e: Exception) {
                Log.e("BlockDetail", "Greška: ${e.message}")
                showError("Greška pri učitavanju.")
            }
        }
    }

    private fun refreshWithNewBlock(newHash: String) {
        val intent = Intent(this, BlockDetailActivity::class.java)
        intent.putExtra("BLOCK_HASH", newHash)
        startActivity(intent)
        finish()
    }

    private fun showError(message: String) {
        runOnUiThread {
            binding.progressBar.visibility = View.GONE
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        }
    }
}