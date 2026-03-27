package com.example.bitcoinstats.model

data class Price(
    val usd: Double,          // Trenutna cijena u dolarima
    val eur: Double,          // Trenutna cijena u eurima
    val lastUpdated: Long,    // Vrijeme zadnjeg osvježavanja
    val dailyChange: Double   // Postotak promjene u zadnjih 24h
)