package it.uninsubria.drugdose1.data.repository
import android.content.Context
import androidx.lifecycle.LiveData
import it.uninsubria.drugdose1.data.local.AppDatabase
import it.uninsubria.drugdose1.data.local.CalculationHistoryEntity
import it.uninsubria.drugdose1.data.model.Drug
import it.uninsubria.drugdose1.utils.JsonLoader

class DrugRepository(private val context: Context){
    //istanza de database, creata la prima volta che serve (lazy)
    private val database by lazy { AppDatabase.getDatabase(context) }

    //DAO creato dopo il database
    private val dao by lazy { database.calculationHistoryDao() }

    //OPERAZIONI SUI FARMACI DAL FILE JSON

    fun getDrugs(): List<Drug> =
        JsonLoader.loadDrugs(context)

    fun searchDrugs(query: String): List<Drug>{
    if (query.isBlank()) return getDrugs() //query vuota restituiamo tutti i farmaci
        val lower= query.lowercase().trim() //normalizza la query
        return getDrugs().filter{ drug -> //tieni i drug che soddisfano la condizione
            drug.name.lowercase().contains(lower) //cerca nel nome
            drug.indication.lowercase().contains(lower) //cerca nell'indicazione clinica
        }
    }
    fun getDrugById(id: String): Drug?{
        //find cerca il primo elemento che soddisfa la condizione
        return getDrugs().find { drug -> drug.id == id }
    }

    //OPERAZIONI SULLA CRONOLOGIA (Room DB)
    fun getAllHistory():
            LiveData<List<CalculationHistoryEntity>> = dao.getAllHistory()
    //salva calcolo nella cronologia, suspend deve essere chiamata da una coroutine
    suspend fun  insertHistory(entry: CalculationHistoryEntity) = dao.insert(entry)
    //elimina calcolo specifico dalla cronologia
    suspend fun deleteHistory(entry: CalculationHistoryEntity) = dao.delete(entry)
   //elimina tutta la cronologia
    suspend fun clearHistory() = dao.deleteAll()
}
