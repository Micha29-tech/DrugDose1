package it.uninsubria.drugdose1.data.local
import androidx.lifecycle.LiveData
import androidx.room.*

/*
 * Interfaccia che definisce COSA vogliamo fare col database,
 * senza specificare COME (Room lo implementa automaticamente tramite KSP).
 */

@Dao
interface CalculationHistoryDao {
    //legge tutti i calcoli salvati, ordinati dal più recente.
    @Query("SELECT * FROM calculation_history ORDER BY timestamp DESC")
    fun getAllHistory(): LiveData<List<CalculationHistoryEntity>>

    //inserire un nuovo calcolo nella cronologia
    @Insert(onConflict = OnConflictStrategy.REPLACE) //se esiste già un record con lo stesso ID, lo sovrascrive
    suspend fun insert(calculation: CalculationHistoryEntity) //suspend: obbligatorio per le operazioni di scrittura Room

    //elimina un calcolo specifico nella cronologia
    @Delete
    suspend fun delete(entry: CalculationHistoryEntity)

    //elimina tutti i calcoli della cronologia
    @Query("DELETE FROM calculation_history")
    suspend fun deleteAll()
}
