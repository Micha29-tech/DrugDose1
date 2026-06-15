package it.uninsubria.drugdose1.data.local
import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/*
 * Dichiariamo solo la struttura (quali Entity ci sono, quali DAO esponiamo).
 * KSP genera AppDatabase_Impl che fa il lavoro vero.
 * Noi istanziamo AppDatabase, ma a runtime è in realtà AppDatabase_Impl.
 *
 * @Database(entities = [...]): elenca tutte le tabelle del database.
 * Aggiungere una nuova Entity qui → Room crea la tabella corrispondente.
 *
 * version = 1: versione dello schema del database.
 * IMPORTANTE: se modifichiamo la struttura di una Entity (aggiungiamo colonna,
 * cambiamo tipo), dobbiamo incrementare la versione E definire una Migration.
 * Senza Migration, Room distrugge e ricrea il database (perdendo i dati).
 */

@Database(entities = [CalculationHistoryEntity::class], version = 1,
        exportSchema=false) // non esporta lo schema in un file JSON.
abstract class AppDatabase : RoomDatabase() {
    //metodo astratto che espone il DAO, Room genera implementazione
    abstract fun calculationHistoryDao(): CalculationHistoryDao
    companion object {

    /* @Volatile: garantisce che il valore di INSTANCE sia sempre aggiornato
     * e visibile a tutti i thread (non viene cachato nella CPU locale del thread).
     * Senza @Volatile, due thread potrebbero vedere versioni diverse di INSTANCE.
     */
        @Volatile
        private var INSTANCE: AppDatabase? = null
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                //crea db solo se esiste ancora
                val istance= Room.databaseBuilder(
                    context.applicationContext, //no memory leak
                    AppDatabase::class.java, //classe database astratta
                    "drugdose_database"
                ).build()
                INSTANCE = istance //salva istanza per i prossimi utilizzi
                istance //restituisce istanza appena creata
            }
        }
    }
            }
