package it.uninsubria.drugdose1.ui.druglist
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import it.uninsubria.drugdose1.data.model.Drug
import it.uninsubria.drugdose1.data.repository.DrugRepository

class DrugListViewModel (
    private val repository: DrugRepository
) : ViewModel() {

    private val _drugs = MutableLiveData<List<Drug>>()
    val drugs: LiveData<List<Drug>> = _drugs

    //init:eseguito quando il ViewModel viene creato
    //carichiamo i farmaci immediatamente appena il Fragment si connette ha già i dati disponibili
    init{
        loadAllDrugs()
    }

    //carica tutti i farmaci dal repository
    private fun loadAllDrugs(){
        _drugs.value = repository.getDrugs()
    }

    //filtra i farmaci in base alla query di ricerca
    fun search(query: String){
        _drugs.value=repository.searchDrugs(query)
    }
    class Factory(private val repository: DrugRepository) :
            ViewModelProvider.Factory{
                override fun <T: ViewModel> create(modelClass: Class<T>): T{
                    @Suppress("UNCHECKED_CAST")
                    return DrugListViewModel(repository) as T
        }
            }
}