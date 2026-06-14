package it.uninsubria.drugdose1.data.model

enum class FormulaType {
    PER_KG,//dose_totale = dose_unitaria × peso_kg
    PER_BSA, //bsa= √(h×p/3600), dose_totale = dose_unitaria × BSA
    FIXED, //dose_totale = dose_unitaria(costante indipendente dal peso
    WEIGHT_RANGES //Cerca la fascia che contiene il peso e restituisce la dose preimpostata
}