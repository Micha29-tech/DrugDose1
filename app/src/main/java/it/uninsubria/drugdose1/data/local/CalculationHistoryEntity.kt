package it.uninsubria.drugdose1.data.local
import androidx.room.Entity
import androidx.room.PrimaryKey

/* PERCHÉ abbiamo una Entity separata dalla data class DoseResult?
* Separazione delle responsabilità (K01 - MVVM):
* - DoseResult è un oggetto del DOMINIO (output del calcolo, nessun riferimento a Room)
* - CalculationHistoryEntity è un oggetto del DATA LAYER (sa tutto di Room)
* Tenere queste due cose separate significa che se un giorno cambiamo
* il database (es. aggiungiamo una colonna), non dobbiamo cambiare DoseResult.
*
* COME FUNZIONA @Entity?
* L'annotazione @Entity dice a Room che questa data class rappresenta una tabella.
* Room (tramite KSP) legge questa annotazione e genera automaticamente:
* - Il codice SQL per creare la tabella: CREATE TABLE calculation_history (...)
* - Il codice per inserire dati: INSERT INTO calculation_history VALUES (...)
* - Il codice per leggere dati: SELECT * FROM calculation_history
* Noi non scriviamo mai SQL direttamente.
*
* NOTA SU "alerts": perché è String e non List<String>?
* Room non supporta nativmente il tipo List<String> come colonna.
* Per salvare una lista, abbiamo due opzioni:
* 1. Serializzarla come JSON (nostra scelta): semplice, leggibile
* 2. TypeConverter Room: più elegante ma richiede codice aggiuntivo
* Scegliamo l'opzione 1 per semplicità nel progetto universitario.
* Prima di salvare: Gson().toJson(list) → stringa JSON
* Dopo la lettura: Gson().fromJson(string, listType) → lista
*/

@Entity(tableName = "calculation_history") //nome tabella
data class CalculationHistoryEntity (
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0, //chiave primaria,
    val drugName: String, //nome farmaco
    val indication: String, //indicazione clinica
    val weightKg: Double, //peso del paziente
    //campi nullable ? , Room salva NULL nel db se il valore è null
    val heightCm: Double?, //altezza del paziente
    val ageYears: Int?, //età del paziente
    val bsa: Double?, //BSA del paziente
    val calculatedDose: Double, //dose calcolata
    val unit: String,
    val formula: String,
    val alerts: String, //lista alert serializzata come JSON
    val isOverMaxDose: Boolean, //flag sovraddosaggio
    val source: String, //fonte
    val timestamp: Long //usato per ordinare la cronologia
)