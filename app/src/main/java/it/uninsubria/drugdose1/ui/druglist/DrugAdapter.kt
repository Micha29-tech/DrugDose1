package it.uninsubria.drugdose1.ui.druglist
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.ListAdapter
import it.uninsubria.drugdose1.data.model.Drug
import it.uninsubria.drugdose1.data.model.FormulaType
import it.uninsubria.drugdose1.databinding.ItemDrugBinding

class DrugAdapter(
    // Lambda che viene chiamata quando l'utente clicca su un farmaco
    // (Drug) -> Unit = funzione che riceve un Drug e non restituisce nulla
    private val onDrugClick: (Drug) -> Unit
) : ListAdapter<Drug, DrugAdapter.DrugViewHolder>(DrugDiffCallback()) {
    /**
     * Chiamato quando RecyclerView ha bisogno di una NUOVA View (cella).
     * Non è chiamato per ogni elemento: RecyclerView RICICLA le View
     * che escono dallo schermo e chiama onBindViewHolder per riempirle.
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DrugViewHolder {
        val binding = ItemDrugBinding.inflate(
            LayoutInflater.from(parent.context),  // inflater basato sul context del parent
            parent,                               // il ViewGroup a cui appartiene
            false                                 // non attaccare subito al parent
        )
        return DrugViewHolder(binding)
    }
    /**
     * Chiamato quando RecyclerView deve RIEMPIRE una View con i dati dell'elemento.
     */
    override fun onBindViewHolder(holder: DrugViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class DrugViewHolder(
        private val binding: ItemDrugBinding  // riferimento al binding del layout item_drug.xml
    ) : RecyclerView.ViewHolder(binding.root) {
        // RecyclerView.ViewHolder richiede la View radice del layout
        /**
         * Riempie la cella con i dati del farmaco.
         * Chiamato ogni volta che questa View viene riusata per un elemento diverso.
         */
        fun bind(drug: Drug) {
            // Imposta il nome del farmaco
            binding.tvDrugName.text = drug.name

            // Imposta l'indicazione clinica
            binding.tvIndication.text = drug.indication

            // Mostra il tipo di formula in modo leggibile
            binding.tvFormulaType.text = when (drug.formulaType) {
                FormulaType.PER_KG -> "Dose per kg"
                FormulaType.PER_BSA -> "Dose per BSA (superficie corporea)"
                FormulaType.FIXED -> "Dose fissa"
                FormulaType.WEIGHT_RANGES -> "Fasce di peso"
                // Essendo un when su un enum, il compilatore sa che questi 4 casi
                // sono esaustivi (non servono else).
            }
            // Imposta il click listener sulla radice della card
            // Quando l'utente tocca la card, chiama la lambda passata all'Adapter
            binding.root.setOnClickListener {
                onDrugClick(drug)  // passa il farmaco cliccato al Fragment
            }
        }
    }
    /**
     * ITEMCALLBACK
     * Dice a ListAdapter come determinare se due elementi sono gli stessi.
     * Usato internamente da DiffUtil per calcolare le differenze tra liste.
     */
    class DrugDiffCallback : DiffUtil.ItemCallback<Drug>() {

        override fun areItemsTheSame(oldItem: Drug, newItem: Drug): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Drug, newItem: Drug): Boolean {
            return oldItem == newItem  // usa equals() generato dalla data class
        }
    }
}