package it.uninsubria.drugdose1.ui.history
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import it.uninsubria.drugdose1.data.local.CalculationHistoryEntity
import it.uninsubria.drugdose1.repository.DrugRepository
import kotlinx.coroutines.launch

class HistoryViewModel(
    private val repository: DrugRepository
    ) : ViewModel(){
    val history: LiveData<List<CalculationHistoryEntity>> = repository.getAllHistory()
    fun deleteEntry(entry: CalculationHistoryEntity){
        viewModelScope.launch {
            repository.deleteHistory(entry)
            //room aggiorna automaticamente il live data
        }
    }
    //Elimina tutta la cronologia
    fun clearAll(){
        viewModelScope.launch {
            repository.clearHistory()
        }
    }

    class Factory(private val repository: DrugRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            HistoryViewModel(repository) as T
    }
    }
