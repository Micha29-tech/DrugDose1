package it.uninsubria.drugdose1.ui.calculator
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import it.uninsubria.drugdose1.data.local.CalculationHistoryEntity
import it.uninsubria.drugdose1.data.model.Drug
import it.uninsubria.drugdose1.data.model.DoseResult
import it.uninsubria.drugdose1.repository.DrugRepository
import it.uninsubria.drugdose1.utils.DoseCalculator
import  com.google.gson.Gson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

//Gestisce lo stato e la logica della schermata Calcolatore
class CalculatorViewModel(
    private val repository: DrugRepository): ViewModel() {
    private val _uiState = MutableStateFlow(CalculatorUIState())
    val uiState: StateFlow<CalculatorUIState> = _uiState.asStateFlow()

    private var lastDoseResult: DoseResult? = null

    /* Imposta il farmaco selezionato dall'utente.
     * Chiamato dal Fragment quando riceve il farmaco dalla navigazione.
     */
    fun setDrug(drug: Drug) {
        _uiState.update { current ->
            current.copy(
                selectedDrug = drug,
                //nuovo farmaco nuovo calcolo
                result = null,
                formulaUsed = null,
                alerts = emptyList(),
                isOverMaxDose = false,
                        sourceRef= null
            )
        }
    }
    fun onEvent(event: CalculatorUiEvent) {
        when (event) {
            // Aggiornamenti di input: modificano solo il campo corrispondente nello stato
            is CalculatorUiEvent.WeightChanged ->
                _uiState.update { it.copy(weightInput = event.value) }
            is CalculatorUiEvent.HeightChanged ->
                _uiState.update { it.copy(heightInput = event.value) }
            is CalculatorUiEvent.AgeChanged ->
                _uiState.update { it.copy(ageInput = event.value) }
            // Azioni che richiedono elaborazione ,delegano a funzioni private
            is CalculatorUiEvent.CalculateClicked -> performCalculation()
            is CalculatorUiEvent.SaveToHistoryClicked -> saveToHistory()
            is CalculatorUiEvent.StartReminderClicked -> { /* gestito nel Fragment */ }
            // Resetta il messaggio di errore dopo che il Fragment l'ha mostrato
            is CalculatorUiEvent.DismissError ->
                _uiState.update { it.copy(errorMessage = null) }
        }
    }
    /**
     * Esegue il calcolo del dosaggio.
     * Legge gli input dallo stato, valida, chiama DoseCalculator, aggiorna lo stato.
     */
    private fun performCalculation() {
        // Legge lo stato corrente (snapshot al momento della chiamata)
        val currentState = _uiState.value
        // Recupera il farmaco selezionato (non dovrebbe mai essere null qui, ma Kotlin lo impone)
        val drug = currentState.selectedDrug ?: return
        // Converte il peso da stringa a Double

        val weight = currentState.weightInput.toDoubleOrNull()
        if (weight == null || weight <= 0) {
            _uiState.update { it.copy(errorMessage = "Inserisci un peso valido (es. 70 oppure 3.5)") }
            return  // early return: non procediamo con dati invalidi
        }
        // Altezza: opzionale, null se il campo è vuoto o non è un numero
        val height = currentState.heightInput.toDoubleOrNull()
        // Età: opzionale, null se il campo è vuoto o non è un numero intero
        val age = currentState.ageInput.toIntOrNull()
        // Mostra stato di caricamento (potremmo mostrare uno spinner)
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        // (niente database, niente rete) è sicuro eseguirlo sul main thread
        val result = DoseCalculator.calculate(drug, weight, height, age)
        // Result.fold: gestisce il caso success e il caso failure
        result.fold(
            onSuccess = { doseResult ->
                // Salva il risultato per il pulsante "Salva in Cronologia"
                lastDoseResult = doseResult
                val resultText = buildString {
                    // Se è stato calcolato il BSA, mostrarlo
                    if (doseResult.bsa != null) {
                        append("BSA stimata: ${"%.2f".format(doseResult.bsa)} m²\n")
                    }
                    append("Dose totale: ${"%.2f".format(doseResult.calculatedDose)} ${doseResult.unit}")
                    // Info somministrazioni se il farmaco si prende più volte al giorno
                    if (drug.administrations > 1) {
                        append("\n(per dose singola — ${drug.administrations} somministrazioni/die)")
                    }
                }
                // Aggiorna lo stato con il risultato, il Fragment ridisegna la UI
                _uiState.update { current ->
                    current.copy(
                        isLoading = false,
                        result = resultText,
                        formulaUsed = doseResult.formula,
                        alerts = doseResult.alerts,
                        isOverMaxDose = doseResult.isOverMaxDose,
                        sourceRef = "Fonte: ${doseResult.source}"
                    )
                }
            },
            onFailure = { error ->
                // Calcolo fallito → mostra il messaggio di errore
                _uiState.update { current ->
                    current.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Errore sconosciuto nel calcolo"
                    )
                }
            }
        )
    }
    /**
     * Salva il risultato del calcolo nella cronologia (Room).
     *
     * viewModelScope.launch: avvia una coroutine nel scope del ViewModel.
     * La coroutine viene eseguita in background (non blocca la UI).
     * Se il ViewModel viene distrutto, la coroutine viene cancellata automaticamente.
     *
     * Dentro la coroutine chiamiamo "suspend fun insert()" del DAO tramite Repository.
     * Questo è il flusso MVVM corretto:
     * Fragment → ViewModel → Repository → DAO → Room → SQLite
     */
    private fun saveToHistory() {
        val result = lastDoseResult ?: return  // nessun calcolo effettuato
        viewModelScope.launch {
            // Dentro una coroutine, possiamo chiamare suspend fun
            repository.insertHistory(
                CalculationHistoryEntity(
                    // id = 0: Room genererà automaticamente un ID univoco
                    drugName = result.drugName,
                    indication = result.indication,
                    weightKg = result.weightKg,
                    heightCm = result.heightCm,
                    ageYears = result.ageYears,
                    bsa = result.bsa,
                    calculatedDose = result.calculatedDose,
                    unit = result.unit,
                    formula = result.formula,
                    // Serializza la lista di alert come stringa JSON per Room
                    alerts = Gson().toJson(result.alerts),
                    isOverMaxDose = result.isOverMaxDose,
                    source = result.source,
                    timestamp = result.timestamp
                )
            )
            // Dopo il salvataggio, nessun aggiornamento di stato necessario:
            // il LiveData nella schermata Cronologia si aggiornerà automaticamente
        }
    }
    class Factory(private val repository: DrugRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            CalculatorViewModel(repository) as T
    }
}
