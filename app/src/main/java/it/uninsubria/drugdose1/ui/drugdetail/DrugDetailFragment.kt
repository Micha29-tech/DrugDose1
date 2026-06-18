package it.uninsubria.drugdose1.ui.drugdetail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import it.uninsubria.drugdose1.DrugDoseApplication
import it.uninsubria.drugdose1.data.model.FormulaType
import it.uninsubria.drugdose1.databinding.FragmentDrugDetailBinding
/**
 Mostra i dettagli di un farmaco e naviga al calcolatore
 */
class DrugDetailFragment : Fragment() {
    private var _binding: FragmentDrugDetailBinding? = null
    private val binding get() = _binding!!
    // Safe Args: delegate che recupera automaticamente gli argomenti dal Bundle
    // Il tipo DrugDetailFragmentArgs è generato da Safe Args in base al nav_graph
    private val args: DrugDetailFragmentArgs by navArgs()
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDrugDetailBinding.inflate(inflater, container, false)
        return binding.root
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Recupera il Repository dall'Application
        val repository = (requireActivity().application as DrugDoseApplication).repository

        // Cerca il farmaco per ID ,può restituire null se l'ID non esiste
        val drug = repository.getDrugById(args.drugId)

        // Se il farmaco non esiste (es. JSON malformato), torniamo indietro
        if (drug == null) {
            findNavController().popBackStack()
            return
        }
        // Popola l'UI con i dati del farmaco
        binding.apply {
            // "apply" esegue il blocco con "this" = binding (evita di ripetere "binding." ogni volta)
            tvDetailName.text = drug.name
            tvDetailIndication.text = drug.indication

            // Costruisce la descrizione della formula in base al tipo
            tvDetailFormula.text = when (drug.formulaType) {
                FormulaType.PER_KG ->
                    "Formula: ${drug.dosePerUnit} ${drug.unit}/kg\n" +
                            "Calcolo: ${drug.dosePerUnit} ${drug.unit} × peso (kg)" +
                            // "?.let": aggiunge la riga dose max SOLO se esiste (null safety)
                            (drug.maxDose?.let { "\nDose massima: $it mg" } ?: "") +
                            (drug.minWeight?.let { "\nPeso minimo: $it kg" } ?: "")
                FormulaType.PER_BSA ->
                    "Formula: ${drug.dosePerUnit} ${drug.unit}/m²\n" +
                            "BSA = √(altezza_cm × peso_kg / 3600)  [Formula di Mosteller]\n" +
                            "Calcolo: ${drug.dosePerUnit} ${drug.unit} × BSA" +
                            (drug.maxDose?.let { "\nDose massima: $it ${drug.unit}" } ?: "")
                FormulaType.FIXED ->
                    "Dose fissa: ${drug.dosePerUnit} ${drug.unit}" +
                            (drug.minAge?.let { "\nEtà minima: $it anni" } ?: "") +
                            (drug.maxDose?.let { "\nDose massima: $it ${drug.unit}" } ?: "")
                FormulaType.WEIGHT_RANGES -> {
                    // Costruisce la tabella delle fasce di peso
                    val fasceText = drug.weightRanges?.joinToString("\n") { range ->
                        "  ${range.minKg} – ${range.maxKg} kg  →  ${range.dose} ${range.unit}"
                    } ?: "N/D"
                    "Fasce di peso:\n$fasceText"
                }
            }
            tvDetailSource.text = drug.source
            // Click sul bottone -> naviga al Calcolatore passando l'ID del farmaco
            btnGoToCalculator.setOnClickListener {
                val action = DrugDetailFragmentDirections.actionDetailToCalculator(drug.id)
                findNavController().navigate(action)
            }
        }
    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}