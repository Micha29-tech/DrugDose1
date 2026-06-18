package it.uninsubria.drugdose1.ui.history
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import it.uninsubria.drugdose1.DrugDoseApplication
import it.uninsubria.drugdose1.R
import it.uninsubria.drugdose1.databinding.FragmentHistoryBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 Mostra la cronologia dei calcoli salvati con possibilità di eliminazione
 */
class HistoryFragment : Fragment() {

    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HistoryViewModel by viewModels {
        HistoryViewModel.Factory(
            (requireActivity().application as DrugDoseApplication).repository
        )
    }

    private lateinit var adapter: HistoryAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupSwipeToDelete()  // imposta il gesto di swipe per eliminare
        observeViewModel()
        setupButtons()
    }

    private fun setupRecyclerView() {
        adapter = HistoryAdapter { entry ->
            // Click lungo sull'elemento -> elimina con conferma
            viewModel.deleteEntry(entry)
            Toast.makeText(requireContext(), getString(R.string.toast_deleted), Toast.LENGTH_SHORT).show()
        }

        binding.recyclerHistory.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@HistoryFragment.adapter
        }
    }

    /**
     * Configura lo swipe-to-delete sulla RecyclerView.
     */
    private fun setupSwipeToDelete() {
        val swipeCallback = object : ItemTouchHelper.SimpleCallback(
            0,                                                      // nessun drag
            ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT          // swipe in entrambe le direzioni
        ) {
            // onMove: obbligatorio ma non usato (no drag)
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false  // false = non gestiamo il drag

            // onSwiped: chiamato quando l'utente ha completato il gesto di swipe
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                // Recupera l'elemento alla posizione dello swipe
                val position = viewHolder.adapterPosition
                val entry = adapter.currentList[position]

                // Elimina dal database tramite ViewModel
                viewModel.deleteEntry(entry)

                Toast.makeText(requireContext(), getString(R.string.toast_deleted), Toast.LENGTH_SHORT).show()
            }
        }

        // attachToRecyclerView: collega l'helper alla RecyclerView
        ItemTouchHelper(swipeCallback).attachToRecyclerView(binding.recyclerHistory)
    }

    /**
     * Osserva il LiveData della cronologia.
     * Ogni volta che un record viene aggiunto o eliminato da Room,
     * questo observer riceve la lista aggiornata e la passa all'Adapter.
     */
    private fun observeViewModel() {
        viewModel.history.observe(viewLifecycleOwner) { entries ->
            // Aggiorna l'Adapter con la nuova lista (DiffUtil calcola le differenze)
            adapter.submitList(entries)

            // Mostra/nascondi il messaggio "lista vuota"
            binding.tvEmptyHistory.visibility =
                if (entries.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    private fun setupButtons() {
        // Bottone "Cancella tutto" con dialog di conferma
        binding.btnClearHistory.setOnClickListener {
            // MaterialAlertDialogBuilder: crea un dialog Material3
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(getString(R.string.dialog_clear_title))
                .setMessage(getString(R.string.dialog_clear_message))
                .setNegativeButton(getString(R.string.dialog_cancel)) { dialog, _ ->
                    dialog.dismiss()  // chiudi il dialog senza fare nulla
                }
                .setPositiveButton(getString(R.string.dialog_confirm)) { _, _ ->
                    viewModel.clearAll()  // elimina tutto dal database
                }
                .show()  // mostra il dialog
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}