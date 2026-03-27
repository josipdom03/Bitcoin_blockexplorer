package com.example.bitcoinstats.model

data class Block(
    val hash: String,
    val height: Int,
    val version: Int,
    val time: Long,           // Timestamp u sekundama
    val nonce: Long,
    val bits: String,
    val difficulty: Double,
    val chainwork: String,
    val previousblockhash: String?,
    val nextblockhash: String?,
    val tx: List<String>      // Lista ID-ova transakcija u ovom bloku
)