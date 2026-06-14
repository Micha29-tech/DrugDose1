package it.uninsubria.drugdose1.data.model

data class DoseResult(
    val drugName: String,
    val indication: String,
    val weightKg : Double,
    val heightCm : Double?, //null se formula non era PER_BSA
    val ageYears: Int?, //null se non inserita dall utetne
    val bsa: Double?, //null se formula non era PER_BSA, BSA calcolata con Mosteller
    val calculatedDose: Double,
    val unit: String,
    val formula: String,
    val alerts: List<String>,
    val isOverMaxDose: Boolean, // flag: true se la dose calcolata supera la dose massima del farmaco
    // utile per mostrare il risultato in rosso e forzare l'attenzione clinica
    val source: String, //fonte bibliografica
    val timestamp: Long = System.currentTimeMillis() //timestamp di calcolo per ordinamento cronologico in Room
)