package it.uninsubria.drugdose1
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import it.uninsubria.drugdose1.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    // "lateinit var": la variabile sarà inizializzata prima del primo uso
    // Non può essere val perché viene assegnata in onCreate() (dopo la dichiarazione)
    // Non ha bisogno di essere nullable perché la inizializziamo sempre in onCreate()
    private lateinit var binding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)  // OBBLIGATORIO: chiama il metodo del padre
        // Crea il binding dal layout activity_main.xml
        // ActivityMainBinding.inflate(layoutInflater):
        // - layoutInflater: strumento per creare View da XML
        // - restituisce ActivityMainBinding con tutte le View del layout
        binding = ActivityMainBinding.inflate(layoutInflater)
        // setContentView: imposta la View radice dell'Activity
        // binding.root: la ConstraintLayout del layout activity_main.xml
        setContentView(binding.root)
        setupNavigation()
    }
    /**
     * navHostFragment.navController:
     * Il NavController gestisce la navigazione: tiene traccia del back stack,
     * esegue le transizioni, ecc.
     *
     * Navigation UI:
     * Metodo di estensione che collega la BottomNavigationView al NavController.
     * Effetti automatici:
     * - Click su "Farmaci" -> naviga a drugListFragment
     * - Click su "Cronologia" -> naviga a historyFragment
     * - Cambio di destinazione -> evidenzia l'icona corrispondente
     * - Gestisce il comportamento del back stack (pop sul back press)
     * Gli ID degli item nel menu devono corrispondere agli ID nel nav_graph.
     */
    private fun setupNavigation() {
        val navHostFragment = supportFragmentManager
            .findFragmentById(binding.navHostFragment.id) as NavHostFragment
        val navController = navHostFragment.navController
        // Collega la BottomNavigationView al NavController
        binding.bottomNavigation.setupWithNavController(navController)
    }
}