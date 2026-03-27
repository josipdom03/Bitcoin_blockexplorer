package com.example.bitcoinstats.model

data class Miner(
    val name: String,         // Ime mining poola (npr. AntPool)
    val address: String,      // Bitcoin adresa rudara
    val poolUrl: String?,
    val blocksFound: Int      // Broj blokova koje je ovaj rudar pronašao
)