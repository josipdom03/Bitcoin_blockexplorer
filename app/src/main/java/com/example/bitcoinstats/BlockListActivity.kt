package com.example.bitcoinstats

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.bitcoinstats.BlockListAdapter
import com.example.bitcoinstats.databinding.ActivityBlockListBinding
import com.example.bitcoinstats.model.Block
import com.example.bitcoinstats.repository.BlockchainRepository
import com.google.gson.Gson
import kotlin.concurrent.thread

class BlockListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBlockListBinding
    private val repository = BlockchainRepository()
    private val gson = Gson()
    private lateinit var adapter: BlockListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBlockListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        loadBlocks()
    }

    private fun setupRecyclerView() {
        adapter = BlockListAdapter { block ->
            val intent = Intent(this, BlockDetailActivity::class.java)
            intent.putExtra("BLOCK_HASH", block.hash)
            startActivity(intent)
        }
        binding.recyclerViewBlocks.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewBlocks.adapter = adapter
    }

    private fun loadBlocks() {
        // Pokaži ProgressBar ako ga imaš u layoutu
        thread {
            try {
                val currentHeight = repository.getBlockCount()
                val blockList = mutableListOf<Block>()

                // Dohvaćamo zadnjih 30 blokova
                // i > 0 osigurava da ne idemo u negativnu visinu
                for (i in 0 until 30) {
                    val targetHeight = currentHeight - i
                    if (targetHeight < 0) break

                    val hash = repository.getBlockHash(targetHeight)
                    hash?.let { blockHash ->
                        val detailsJson = repository.getBlockDetails(blockHash)
                        detailsJson?.let { json ->
                            val blockObj = gson.fromJson(json, Block::class.java)
                            blockList.add(blockObj)
                        }
                    }
                }

                runOnUiThread {
                    adapter.submitList(blockList)
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this, "Greška pri dohvatu blokova", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}