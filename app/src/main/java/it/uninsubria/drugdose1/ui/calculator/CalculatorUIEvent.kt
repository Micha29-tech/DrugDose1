package it.uninsubria.drugdose1.ui.calculator

sealed class CalculatorUiEvent {
    //L'utente ha modificato il campo peso
    data class WeightChanged(val value: String) : CalculatorUiEvent()
    //L'utente ha modificato il campo altezza
    data class HeightChanged(val value: String) : CalculatorUiEvent()
    //L'utente ha modificato il campo età
    data class AgeChanged(val value: String) : CalculatorUiEvent()
    //L'utente ha premuto "Calcola"
    object CalculateClicked : CalculatorUiEvent()
    //L'utente ha premuto "Salva in Cronologia"
    object SaveToHistoryClicked : CalculatorUiEvent()
    //L'utente ha premuto "Attiva Promemoria"
    //l'avvio del Service viene gestito nel Fragment (ha bisogno del Context)
    //Il ViewModel registra solo che l'evento è avvenuto
    object StartReminderClicked : CalculatorUiEvent()
    //Il Toast con l'errore è stato mostrato → resetta errorMessage nello stato
    object DismissError : CalculatorUiEvent()
}