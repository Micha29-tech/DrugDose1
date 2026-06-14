package it.uninsubria.drugdose1.domain
import kotlin.math.sqrt

interface DoseFormula {
    fun calculate(weightKg: Double, heightCm: Double?, ageYears: Int?): Double //calcola dose totale in base ai parametri del paziente
    fun describe(): String //mostra all'utente come è calcolata la dose
}
/*
 SINTASSI ": DoseFormula" → questa classe IMPLEMENTA l'interfaccia.
 * Deve obbligatoriamente fornire le implementazioni di calculate() e describe().
 * Se dimentico uno dei metodi, il compilatore me lo dice subito.
 *
 * "private val" nel costruttore: i parametri sono proprietà private.
 * Non servono fuori da questa classe — è un dettaglio implementativo.
 */
class PerKgFormula(
    private val dosePerKg: Double, //dose per ogni kg di peso del paz.
    private val unit: String //unità di misura della dose
) : DoseFormula{
    // "override" = implemento il metodo dichiarato nell'interfaccia
    override fun calculate(weightKg: Double, heightCm: Double?, ageYears: Int?): Double {
        return dosePerKg * weightKg //questa formula dipende solamente dal peso
    }
    override fun describe(): String {
        return "$dosePerKg $unit/kg × peso"  //stringa descrittiva mostrata all'utente nel risultato
    }
    }

class PerBsaFormula(
    private val dosePerM2: Double,//dose per metro quadro di superficie corporea
    private val unit: String //unità di misura della dose
) : DoseFormula {

    override fun calculate(weightKg: Double, heightCm: Double?, ageYears: Int?): Double {
        requireNotNull(heightCm) {
            "L'altezza è obbligatoria per il calcolo BSA (Formula di Mosteller)"
        }
        val bsa = sqrt((heightCm * weightKg) / 3600) //sqrt importata precedentemente
        return dosePerM2 * bsa //questa formula dipende dall'altezza e dal peso
    }

    override fun describe(): String {
        return "$dosePerM2 $unit/m2 × BSA (Mosteller:√(h×p/3600)"
    }
}

class FixedDoseFormula(
    private val dose: Double, //dose totale fissa
    private val unit: String //unità di misura
) : DoseFormula{

    override fun calculate(weightKg: Double, heightCm: Double?, ageYears: Int?): Double {
        return dose //questa formula non dipende dai parametri del paziente
    }
    override fun describe(): String {
        return "Dose fissa: $dose $unit"
    }
}

class WeightRangeFormula(
    private val ranges: List<WeightRangeEntry>
) : DoseFormula{//lista delle fasce di peso
    //classe dati interna visibile solo dentro WeightRangeFormula
    data class WeightRangeEntry(
    val minKg: Double, //peso minimo della fascia (incluso)
    val maxKg: Double, //peso massimo della fascia (escluso)
    val dose: Double, //dose corrispondente a questa fascia di peso
    val unit: String //unità di misura della dose
    )

    override fun calculate(weightKg: Double, heightCm: Double?, ageYears: Int?): Double {

    val range=ranges.find{ entry ->
        weightKg >= entry.minKg && weightKg < entry.maxKg
    }
        requireNotNull(range) {
            "Nessuna fascia di peso disponibile per $weightKg kg"+
                    "Verificare le fasce configurate per questo farmaco"
        }
        return range.dose
}
    override fun describe(): String {
        //joinToString converte la lista in una stringa con separatore
        val fasceSummary = ranges.joinToString(",") { entry ->
            "${entry.minKg} - ${entry.maxKg} kg: ${entry.dose} ${entry.unit}"
        }
        return "Fasce di peso: fasceSummary"
    }
    }



