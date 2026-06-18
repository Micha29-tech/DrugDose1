package it.uninsubria.drugdose1.ui.history
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import it.uninsubria.drugdose1.data.local.CalculationHistoryEntity
import it.uninsubria.drugdose1.databinding.ItemHistoryBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
/**
 * Struttura simile a DrugAdapter ma per CalculationHistoryEntity.
 * DIFFERENZA: qui formattiamo il timestamp come data leggibile.
 *
 * SimpleDateFormat: classe Java per formattare/parsare date.
 * "dd/MM/yyyy HH:mm": formato "18/06/2025 14:30"
 * Locale.ITALY: usa le convenzioni italiane (es. separatore decimale virgola)
 *
 * PERCHÉ creare dateFormat nella classe e non in bind()?
 * SimpleDateFormat è costoso da creare. Crearlo una volta nella classe
 * e riusarlo per ogni bind() è più efficiente.
 */


class HistoryAdapter(
    private val onDeleteClick: (CalculationHistoryEntity) -> Unit
) : ListAdapter<CalculationHistoryEntity, HistoryAdapter.HistoryViewHolder>(HistoryDiffCallback()) {
    // Formato data italiano, creato una volta per tutte le celle
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.ITALY)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val binding = ItemHistoryBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return HistoryViewHolder(binding)
    }
    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    inner class HistoryViewHolder(
        private val binding: ItemHistoryBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(entry: CalculationHistoryEntity) {
            // Nome farmaco
            binding.tvHistoryDrug.text = entry.drugName
            // Dose calcolata con peso del paziente
            // "%.2f": 2 decimali. Es: 14.000 → "14.00"
            binding.tvHistoryDose.text =
                "Dose: ${"%.2f".format(entry.calculatedDose)} ${entry.unit}" +
                        " · Peso: ${entry.weightKg} kg"
            // Formula usata
            binding.tvHistoryFormula.text = entry.formula
            // Data/ora formattata
            // Date(timestamp): crea un oggetto Date dai millisecondi
            // dateFormat.format(date): converte in stringa "dd/MM/yyyy HH:mm"
            binding.tvHistoryDate.text = dateFormat.format(Date(entry.timestamp))
            // Click lungo per eliminare (alternativa al swipe)
            binding.root.setOnLongClickListener {
                onDeleteClick(entry)
                true  // "true" = l'evento è stato gestito (non propagare)
            }
        }
    }
    class HistoryDiffCallback : DiffUtil.ItemCallback<CalculationHistoryEntity>() {
        // Due entry sono lo stesso elemento se hanno lo stesso ID generato da Room
        override fun areItemsTheSame(
            oldItem: CalculationHistoryEntity,
            newItem: CalculationHistoryEntity
        ): Boolean = oldItem.id == newItem.id
        // Il contenuto è uguale se tutti i campi sono uguali (data class equals())
        override fun areContentsTheSame(
            oldItem: CalculationHistoryEntity,
            newItem: CalculationHistoryEntity
        ): Boolean = oldItem == newItem
    }
}