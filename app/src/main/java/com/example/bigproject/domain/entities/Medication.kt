package com.example.bigproject.domain.entities

data class Medication(
    val id: String,
    val name: String,
    val activeSubstance: String,
    val dosage: String,
    val form: String,
    val imprintCode: String,
    val color: String,
    val shape: String,
    val commonSideEffects: List<String>,
    val warnings: List<String>
)
