package com.example.bitcoinstats

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.bitcoinstats.R
import com.example.bitcoinstats.model.Block
import java.text.SimpleDateFormat
import java.util.*

class BlockListAdapter(private val onClick: (Block) -> Unit) :
    ListAdapter<Block, BlockListAdapter.BlockViewHolder>(BlockDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BlockViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_block, parent, false)
        return BlockViewHolder(view, onClick)
    }

    override fun onBindViewHolder(holder: BlockViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class BlockViewHolder(view: View, val onClick: (Block) -> Unit) : RecyclerView.ViewHolder(view) {
        private val heightText: TextView = view.findViewById(R.id.textViewBlockHeight)
        private val hashText: TextView = view.findViewById(R.id.textViewBlockHashShort)
        private val timeText: TextView = view.findViewById(R.id.textViewBlockTime)
        private val txCountText: TextView = view.findViewById(R.id.textViewTxCount)

        fun bind(block: Block) {
            heightText.text = "#${block.height}"
            hashText.text = block.hash.take(30) + "..."

            // Formatiranje vremena
            val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            timeText.text = sdf.format(Date(block.time * 1000L))

            txCountText.text = "${block.tx.size} transakcije"

            itemView.setOnClickListener { onClick(block) }
        }
    }

    class BlockDiffCallback : DiffUtil.ItemCallback<Block>() {
        override fun areItemsTheSame(oldItem: Block, newItem: Block) = oldItem.hash == newItem.hash
        override fun areContentsTheSame(oldItem: Block, newItem: Block) = oldItem == newItem
    }
}