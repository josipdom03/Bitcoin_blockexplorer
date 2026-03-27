package com.example.bitcoinstats

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.bitcoinstats.model.Transaction
import java.util.Locale

class TransactionAdapter(private val transactions: List<Transaction>) :
    RecyclerView.Adapter<TransactionAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val indexText: TextView = view.findViewById(R.id.textViewIndex)
        val txIdText: TextView = view.findViewById(R.id.textViewTxId)
        val txValueText: TextView = view.findViewById(R.id.textViewValue)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_transaction, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val tx = transactions[position]
        // Izračun ukupne vrijednosti izlaza (vout)
        val totalOutput = tx.vout.sumOf { it.value }

        holder.indexText.text = "#${position + 1}"
        holder.txIdText.text = "ID: ${tx.txid}"
        holder.txValueText.text = String.format(Locale.US, "%.8f BTC", totalOutput)

        // KLIK LOGIKA: Otvaranje detalja transakcije
        holder.itemView.setOnClickListener {
            val context = holder.itemView.context
            val intent = Intent(context, TransactionDetailActivity::class.java).apply {
                putExtra("TX_ID", tx.txid)
            }
            context.startActivity(intent)
        }
    }

    override fun getItemCount() = transactions.size
}