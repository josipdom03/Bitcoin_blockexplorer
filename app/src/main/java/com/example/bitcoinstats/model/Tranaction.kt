package com.example.bitcoinstats.model

data class Transaction(
    val txid: String,
    val version: Int,
    val vin: List<Vin>,
    val vout: List<Vout>
)

data class Vin(
    val txid: String?,
    val vout: Int?,
    val coinbase: String?
)

data class Vout(
    val value: Double,
    val n: Int,
    val scriptPubKey: ScriptPubKey
)

data class ScriptPubKey(
    val address: String?,
    val hex: String
)