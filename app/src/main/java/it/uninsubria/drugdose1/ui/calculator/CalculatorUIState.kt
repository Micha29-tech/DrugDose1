package it.uninsubria.drugdose1.ui.calculator
import it.uninsubria.drugdose1.data.model.Drug

data class CalculatorUIState(
    //farmaco selezionato dall'utente
    val selectedDrug: Drug?=null,
    val weightInput: String="",
    val heightInput: String="",
    val ageInput: String="",
    val result: String?=null,//risultato
    val formulaUsed: String?=null, //descrizione formula applicata
    val alerts: List<String> = emptyList(), //lista alert clinici da mostrare
    val isOverMaxDose: Boolean = false,
    val isLoading: Boolean = false,//true durante elaborazione calcolo
    val sourceRef: String?=null, //riferimento fonte
    val errorMessage: String?=null //messaggio di errore
)