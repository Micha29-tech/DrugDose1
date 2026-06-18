package it.uninsubria.drugdose1.ui.druglist

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import it.uninsubria.drugdose1.DrugDoseApplication
import it.uninsubria.drugdose1.databinding.FragmentDrugListBinding
class DrugListFragment : Fragment() {
    // PATTERN BINDING SICURO PER FRAGMENT:
    private var _binding: FragmentDrugListBinding? = null   // nullable: null fuori dal ciclo
    private val binding get() = _binding!!                   // non-nullable: usata solo quando sicuro
    // ViewModel recuperato (o creato) con il Factory
    private val viewModel: DrugListViewModel by viewModels {
        DrugListViewModel.Factory(
            // Recupera il Repository dall'Application class
            (requireActivity().application as DrugDoseApplication).repository
        )
    }

    private lateinit var adapter: DrugAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDrugListBinding.inflate(inflater, container, false)
        return binding.root  // restituisce la View radice
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupSearchView()
        observeViewModel()
    }
    /**
     * Configura la RecyclerView con il LinearLayoutManager e l'Adapter.
     * LinearLayoutManager: dispone gli elementi verticalmente (lista normale)
     */
    private fun setupRecyclerView() {
        adapter = DrugAdapter { drug ->
            // Lambda eseguita al click su un farmaco
            // DrugListFragmentDirections: classe generata da Safe Args

            val action = DrugListFragmentDirections.actionDrugListToDetail(drug.id)

            findNavController().navigate(action)
        }
        // Configura la RecyclerView
        binding.recyclerDrugs.apply {

            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@DrugListFragment.adapter  // "this@DrugListFragment" per disambiguare
        }
    }

    private fun setupSearchView() {
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {

            override fun onQueryTextChange(newText: String?): Boolean {
                viewModel.search(newText ?: "")  // Elvis operator: se null usa stringa vuota
                return true
            }
            /**
             * Chiamato quando l'utente preme Invio sulla tastiera.
             * Potremmo fare qualcosa di specifico, ma la ricerca è già in tempo reale.
             */
            override fun onQueryTextSubmit(query: String?): Boolean = true
        })
    }

    private fun observeViewModel() {
        viewModel.drugs.observe(viewLifecycleOwner) { drugs ->
            adapter.submitList(drugs)
        }
    }
    /**
     * azzera il binding per evitare memory leak.
     * Chiamato quando la View del Fragment viene distrutta.
     */
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null  // rimuovi il riferimento alla View
    }
}