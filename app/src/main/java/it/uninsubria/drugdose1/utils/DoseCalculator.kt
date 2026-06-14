package it.uninsubria.drugdose1.utils
import kotlin.math.sqrt
import it.uninsubria.drugdose1.data.model.DoseResult
import it.uninsubria.drugdose1.data.model.Drug
import it.uninsubria.drugdose1.data.model.FormulaType
import it.uninsubria.drugdose1.domain.PerKgFormula
import it.uninsubria.drugdose1.domain.FixedDoseFormula
import it.uninsubria.drugdose1.domain.PerBsaFormula
import it.uninsubria.drugdose1.domain.WeightRangeFormula

//valida input,seleziona formula,calcola dose, controlla limiti, costruisce DoseResult
object DoseCalculator {
    //calcola BSA in base alla formula di Mosteller
    fun calculateBSA(heightCm: Double, weightKg: Double): Double {
        return sqrt((heightCm * weightKg) / 3600)
    }
    //calcola il dosaggio farmacologio per un paziente specifico
    fun calculate(
        drug: Drug,
        weightKg: Double,
        heightCm: Double? = null, // valore di default non obbligatorio da passare
        ageYears: Int? = null,
    ): Result<DoseResult> {

        //VALIDAZIONE INPUT
        //peso must be positivo
        if (weightKg <= 0) {
            //result.failure con motivazione errore
            return Result.failure(IllegalArgumentException("Il peso deve essere maggiore di 0. Valore inserito: $weightKg")
            )
        }

        //verifica peso minimo del farmaco (se specificato)
        drug.minWeight?.let{ minW->
            if (weightKg < minW){
                return Result.failure(IllegalArgumentException(
                    "Peso insufficiente per ${drug.name}.\n" +
                    "Peso paziente: $weightKg kg\n" +
                    "Peso minimo richiesto: $minW kg")
                )
            }
        }

        //verifica età minima (solo se l'utente ha inserito l'età)
        //se ageYears è null non facciamo il controllo
        drug.minAge?.let{ minA->
            if(ageYears != null && ageYears < minA){
                return Result.failure(IllegalArgumentException(
                    "Età insufficiente per ${drug.name}.\n" +
                    "Età paziente: $ageYears anni\n" +
                    "Età minima richiesta: $minA anni")
                )
            }
    }

        //SELEZIONE FORMULA (usa interfaccia DoseFormula)
        //lista alert clinici vuota
        val alerts = drug.alerts.toMutableList()

        //bsa calcolata
        var bsa: Double? = null
        val formula: it.uninsubria.drugdose1.domain.DoseFormula = when (drug.formulaType) { //come lo switch
            FormulaType.PER_KG -> {
                //crea implementazione per dose/kg
                PerKgFormula(drug.dosePerUnit, drug.unit)
            }
            FormulaType.PER_BSA -> {
                if(heightCm == null) {
                    return Result.failure(
                        IllegalArgumentException(
                            "${drug.name} usa una formula basata sulla superficie corporea (BSA).\n" +
                                    "L'altezza del paziente è obbligatoria per questo calcolo."
                        )
                    )
                }
                //calcola e salva bsa
                bsa = calculateBSA(heightCm, weightKg)
                //crea implementazione per dose/BSA
                PerBsaFormula(drug.dosePerUnit, drug.unit)
            }
            FormulaType.FIXED -> {
                //dose fissa, nessuna verifica aggiuntiva necessaria
                FixedDoseFormula(drug.dosePerUnit, drug.unit)
            }
            FormulaType.WEIGHT_RANGES -> {
                //verifica che le classi siano presenti nel JSON
                val ranges = drug.weightRanges
                if(ranges.isNullOrEmpty()) {
                    return Result.failure(
                        IllegalArgumentException(
                            "Dati mancanti: ${drug.name} usa fasce di peso ma non ha fasce configurate"
                        )
                    )
                }

                //converte List<WeightRange> in lista WeightRangeEntry (modello dati -> dominio)
                val entries = ranges.map { range ->
                    WeightRangeFormula.WeightRangeEntry(
                        minKg = range.minKg,
                        maxKg = range.maxKg,
                        dose = range.dose,
                        unit = range.unit
                    )
                }
                //crea implementazione per dose/peso
                WeightRangeFormula(entries)
            }
        }

        //CALCOLO DOSE GREZZA
        //potrebbe lanciare illegalArgumentException (es. peso fuori dalle fasce,altezza null per BSA ...)
        val rawDose: Double = try {
        formula.calculate(weightKg, heightCm, ageYears)
        }catch (e: IllegalArgumentException) {
        //propaghiamo l'eccezione come Result.failure
            return Result.failure(e)
        }

        //CONVERSIONE UNITÀ µg → mg
        val displayDose: Double
        val displayUnit: String
        if (drug.unit.contains("µg")) {
            displayDose = rawDose / 1000.0   // converti µg in mg
            displayUnit = "mg"
        } else {
            // Per mg, g, ecc. non convertiamo
            displayDose = rawDose
            displayUnit = drug.unit
        }

        //CONTROLLO LIMITI (alert clinici)
        //controlla se la dose supera il massimo raccomandato
        val isOverMaxDose = drug.maxDose != null && displayDose > drug.maxDose
        if (isOverMaxDose) {
            //aggiungiamo un alert specifico per il sovradosaggio
            // "%.2f".format() arrotonda a 2 decimali
            alerts.add(
                "ATTENZIONE: la dose calcolata (${"%.2f".format(displayDose)} $displayUnit) " +
                        "supera la dose massima raccomandata (${drug.maxDose} $displayUnit).\n" +
                        "Considerare di usare la dose massima e consultare il medico."
            )
        }
        //controlla se la dose è sotto il minimo raccomandato
        drug.minDose?.let { minD ->
            if (displayDose < minD) {
                alerts.add(
                    "NOTA: la dose calcolata (${"%.2f".format(displayDose)} $displayUnit) " +
                            "è inferiore alla dose minima raccomandata ($minD $displayUnit)."
                )
            }
        }

        //COSTRUZIONE DEL RISULTATO
        //tutto andato bene → costruiamo il DoseResult e lo avvolgiamo in Result.success
        return Result.success(
            DoseResult(
                drugName = drug.name,
                indication = drug.indication,
                weightKg = weightKg,
                heightCm = heightCm,
                ageYears = ageYears,
                bsa = bsa,
                calculatedDose = displayDose,
                unit = displayUnit,
                // describe() dell'implementazione concreta — es "200 µg/kg × peso"
                formula = formula.describe(),
                alerts = alerts,    // alert del farmaco + alert generati dal calcolo
                isOverMaxDose = isOverMaxDose,
                source = drug.source
                //timestamp: usa il valore di default System.currentTimeMillis()
            )
        )
    }
}


