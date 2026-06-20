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

        /*
         *  VALIDAZIONE GENERALE INPUT PAZIENTE
         *
         *
         * Questi controlli servono a bloccare valori oggettivamente non realistici.
         * Non devono sostituire il giudizio medico: servono solo a evitare input
         * impossibili o chiaramente sbagliati.
         */

        if (weightKg <= 0) {
            return Result.failure(
                IllegalArgumentException(
                    "Il peso deve essere maggiore di 0 kg.\n" +
                            "Valore inserito: $weightKg kg"
                )
            )
        }

        /*
         * Peso realisticamente accettabile.
         *
         * 1 kg: limite minimo molto basso, utile per neonati/pazienti pediatrici.
         * 350 kg: limite massimo molto ampio, per non bloccare casi rari ma possibili.
         */
        if (weightKg < 1 || weightKg > 350) {
            return Result.failure(
                IllegalArgumentException(
                    "Peso non realistico.\n" +
                            "Inserisci un valore compreso tra 1 kg e 350 kg.\n" +
                            "Valore inserito: $weightKg kg"
                )
            )
        }

        ageYears?.let { age ->
            /*
             * Età realisticamente accettabile.
             *
             * 0 anni: neonato.
             * 120 anni: limite molto alto ma ancora teoricamente possibile.
             */
            if (age < 0 || age > 120) {
                return Result.failure(
                    IllegalArgumentException(
                        "Età non realistica.\n" +
                                "Inserisci un valore compreso tra 0 e 120 anni.\n" +
                                "Valore inserito: $age anni"
                    )
                )
            }
        }

        /*
         * Validazione altezza.
         *
         * La controlliamo ogni volta che viene inserita.
         * Anche se il farmaco non usa la BSA, un valore assurdo non deve essere accettato.
         */
        heightCm?.let { height ->

            if (height <= 0) {
                return Result.failure(
                    IllegalArgumentException(
                        "L'altezza deve essere maggiore di 0 cm.\n" +
                                "Valore inserito: $height cm"
                    )
                )
            }

            /*
             * Altezza realisticamente accettabile.
             *
             * 40 cm: limite minimo molto basso, compatibile con neonati molto piccoli.
             * 250 cm: limite massimo molto ampio, oltre il quale il dato è quasi certamente errato.
             */
            if (height < 40 || height > 250) {
                return Result.failure(
                    IllegalArgumentException(
                        "Altezza non realistica.\n" +
                                "Inserisci un valore compreso tra 40 cm e 250 cm.\n" +
                                "Valore inserito: $height cm"
                    )
                )
            }

            /*
             * Controllo combinato peso/altezza.
             *
             * Questo blocca combinazioni oggettivamente assurde:
             * - 38 kg e 10 cm
             * - 12 kg e 160 cm
             * - 300 kg e 60 cm
             *
             * Usiamo un intervallo molto ampio di BMI per non bloccare casi clinici
             * rari ma possibili.
             */
            val heightM = height / 100.0
            val bmi = weightKg / (heightM * heightM)

            if (bmi < 8 || bmi > 100) {
                return Result.failure(
                    IllegalArgumentException(
                        "Combinazione peso/altezza non realistica.\n" +
                                "Peso inserito: $weightKg kg\n" +
                                "Altezza inserita: $height cm"
                    )
                )
            }
        }

                //SELEZIONE FORMULA (usa interfaccia DoseFormula)
                //lista alert clinici vuota
                val alerts = drug.alerts.toMutableList()

                //bsa calcolata
                var bsa: Double? = null
                val formula: it.uninsubria.drugdose1.domain.DoseFormula =
                    when (drug.formulaType) { //come lo switch
                        FormulaType.PER_KG -> {
                            //crea implementazione per dose/kg
                            PerKgFormula(drug.dosePerUnit, drug.unit)
                        }

                        FormulaType.PER_BSA -> {
                            if (heightCm == null) {
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
                            if (ranges.isNullOrEmpty()) {
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
                } catch (e: IllegalArgumentException) {
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



