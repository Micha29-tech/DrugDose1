package it.uninsubria.drugdose1.ui.calculator
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import it.uninsubria.drugdose1.DrugDoseApplication
import it.uninsubria.drugdose1.R
import it.uninsubria.drugdose1.databinding.FragmentCalculatorBinding
import it.uninsubria.drugdose1.service.DoseReminderService
import kotlinx.coroutines.launch
import androidx.navigation.fragment.findNavController
import android.view.inputmethod.InputMethodManager
import android.content.Context

/**
 * Schermata principale dell'app — calcola il dosaggio
 */
class CalculatorFragment : Fragment() {
    private var _binding: FragmentCalculatorBinding? = null
    private val binding get() = _binding!!
    // Safe Args: recupera drugId passato dal DrugDetailFragment
    private val args: CalculatorFragmentArgs by navArgs()
    private val viewModel: CalculatorViewModel by viewModels {
        CalculatorViewModel.Factory(
            (requireActivity().application as DrugDoseApplication).repository
        )
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()  // tipo: richiedi UN permesso
    ) { isGranted ->
        // Callback chiamata dopo che l'utente ha risposto al dialog del permesso
        if (isGranted) {
            // Permesso concesso -> avvia il Service
            launchDoseReminderService()
        } else {
            // Permesso negato → informiamo l'utente
            Toast.makeText(
                requireContext(),
                "Permesso notifiche necessario per il promemoria",
                Toast.LENGTH_LONG
            ).show()
        }
    }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCalculatorBinding.inflate(inflater, container, false)
        return binding.root
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        // Carica il farmaco nel ViewModel tramite l'ID ricevuto dagli argomenti
        val repository = (requireActivity().application as DrugDoseApplication).repository
        repository.getDrugById(args.drugId)?.let { drug ->
            viewModel.setDrug(drug)
        }
        setupTextWatchers()
        setupButtons()
        observeState()
    }

    private fun setupTextWatchers() {
        // Campo peso
        binding.etWeight.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                // Manda l'evento al ViewModel con il testo corrente
                viewModel.onEvent(CalculatorUiEvent.WeightChanged(s?.toString() ?: ""))
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
        // Campo altezza
        binding.etHeight.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                viewModel.onEvent(CalculatorUiEvent.HeightChanged(s?.toString() ?: ""))
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
        // Campo età
        binding.etAge.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                viewModel.onEvent(CalculatorUiEvent.AgeChanged(s?.toString() ?: ""))
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }
    private fun setupButtons() {
        // Bottone CALCOLA
        binding.btnCalculate.setOnClickListener {
            hideKeyboard()
            viewModel.onEvent(CalculatorUiEvent.CalculateClicked)
        }
        // Bottone SALVA IN CRONOLOGIA
        binding.btnSave.setOnClickListener {
            viewModel.onEvent(CalculatorUiEvent.SaveToHistoryClicked)
            Toast.makeText(requireContext(), getString(R.string.toast_saved), Toast.LENGTH_SHORT).show()
        }
        // Bottone ATTIVA PROMEMORIA
        binding.btnReminder.setOnClickListener {
            checkAndStartReminder()
        }
    }

    private fun hideKeyboard() {
        val inputMethodManager = requireContext()
            .getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

        val currentView = requireActivity().currentFocus ?: binding.root

        inputMethodManager.hideSoftInputFromWindow(
            currentView.windowToken,
            0
        )

        currentView.clearFocus()
    }
    /**
     * Controlla il permesso notifiche e avvia il Service.
     * PERCHÉ il Fragment e non il ViewModel gestisce questo?
     * Perché richiedere permessi e avviare Service richiedono il Context.
     * Il ViewModel non deve avere riferimenti al Context (memory leak).
     */
    private fun checkAndStartReminder() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Controlla se il permesso è già stato concesso
            val hasPermission = ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (hasPermission) {
                launchDoseReminderService()
            } else {
                // Richiedi il permesso all'utente
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            // Prima di Android 13, il permesso è già nel Manifest e non serve chiedere
            launchDoseReminderService()
        }
    }
    /**
     * Avvia il DoseReminderService per mostrare notifica eliminabile
     */
    private fun launchDoseReminderService() {
        val state = viewModel.uiState.value
        val drug = state.selectedDrug ?: return
        val doseText = state.result ?: return
        // Crea un Intent esplicito verso il DoseReminderService
        val intent = Intent(requireContext(), DoseReminderService::class.java).apply {
            // putExtra: aggiunge dati extra all'Intent (recuperati dal Service con getStringExtra)
            putExtra(DoseReminderService.EXTRA_DRUG_NAME, drug.name)
            putExtra(DoseReminderService.EXTRA_DOSE, doseText)
            putExtra(DoseReminderService.EXTRA_UNIT, state.selectedDrug.unit)
        }
        // Avvia il Service normale per mostrare una notifica eliminabile
        requireContext().startService(intent)
        Toast.makeText(requireContext(), getString(R.string.toast_reminder_on), Toast.LENGTH_SHORT).show()
    }


    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                renderState(state)
            }
        }
    }
    /**
     * Aggiorna l'intera UI in base allo stato corrente.
     * "render" = disegna: riceve lo stato e aggiorna ogni View di conseguenza.
     * Questo approccio è deterministico: dato uno stato, la UI è sempre uguale.
     */
    private fun renderState(state: CalculatorUIState) {
        // Aggiorna l'header con il farmaco selezionato
        state.selectedDrug?.let { drug ->
            binding.tvCalcDrugName.text = drug.name
            binding.tvCalcIndication.text = drug.indication
        }
        // Mostra/nascondi la card risultato
        if (state.result != null) {
            binding.cardResult.visibility = View.VISIBLE
            // Colora la dose in rosso se supera la dose massima
            binding.tvResult.text = state.result
            binding.tvResult.setTextColor(
                if (state.isOverMaxDose) {
                    requireContext().getColor(R.color.color_error)    // rosso = pericolo
                } else {
                    requireContext().getColor(R.color.color_success)  // verde = ok
                }
            )
            binding.tvFormulaUsed.text = state.formulaUsed
            // Mostra gli alert solo se ce ne sono
            if (state.alerts.isNotEmpty()) {
                binding.layoutAlerts.visibility = View.VISIBLE
                // joinToString con separatore "\n\n" = riga vuota tra alert
                binding.tvAlerts.text = state.alerts.joinToString("\n\n")
            } else {
                binding.layoutAlerts.visibility = View.GONE
            }
            // Fonte bibliografica
            state.sourceRef?.let { binding.tvSourceRef.text = it }
        } else {
            // Nessun risultato → nascondi tutta la card risultato
            binding.cardResult.visibility = View.GONE
        }
        // Mostra errore come Toast
        if (state.errorMessage != null) {
            Toast.makeText(requireContext(), state.errorMessage, Toast.LENGTH_LONG).show()
            // Resetta l'errore nello stato dopo averlo mostrato
            viewModel.onEvent(CalculatorUiEvent.DismissError)
        }
    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}