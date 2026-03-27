package com.example.bitcoinstats

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.bitcoinstats.databinding.ActivityMainBinding
import com.example.bitcoinstats.repository.BlockchainRepository
import kotlin.concurrent.thread
import android.util.Log

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val repository = BlockchainRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 1. Pretraga po broju bloka -> ide direktno na detalje
        binding.btnSearchBlock.setOnClickListener {
            val input = binding.editTextSearch.text.toString()
            if (input.isNotEmpty()) {
                val height = input.toIntOrNull()
                if (height != null) {
                    searchByBlockHeight(height)
                } else {
                    Toast.makeText(this, "Unesite ispravan broj bloka", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // 2. Pretraga po datumu -> otvara listu blokova


        // Otvaranje standardne liste zadnjih blokova
        binding.btnShowRecent.setOnClickListener {
            startActivity(Intent(this, BlockListActivity::class.java))
        }
    }

    private fun searchByBlockHeight(height: Int) {
        thread {
            Log.d("DEBUG_BITCOIN", "Pokrećem dohvat za visinu: $height")
            val hash = repository.getBlockHash(height)
            Log.d("DEBUG_BITCOIN", "Dobiven hash: $hash")

            runOnUiThread {
                if (hash != null) {
                    val intent = Intent(this, BlockDetailActivity::class.java)
                    intent.putExtra("BLOCK_HASH", hash)
                    startActivity(intent)
                } else {
                    Toast.makeText(this, "Blok nije pronađen (provjeri internet)", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}