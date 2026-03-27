package com.example.bitcoinstats.model

data class BlockStat(
    val height: Int,
    val hash: String,
    val total_out: Double,    // Ukupna vrijednost svih izlaza
    val total_fee: Double,    // Ukupna naknada u bloku
    val tx_count: Int,        // Broj transakcija
    val avg_fee: Double,
    val miner_reward: Double  // Subsidy + Fees
)