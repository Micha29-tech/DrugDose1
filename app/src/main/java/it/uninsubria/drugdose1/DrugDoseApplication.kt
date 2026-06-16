package it.uninsubria.drugdose1
import android.app.Application
import it.uninsubria.drugdose1.data.repository.DrugRepository

class DrugDoseApplication : Application() {
    val repository: DrugRepository by lazy{
        //this = il context dell application vive quanto l app, no memory leak
        DrugRepository(this)
    }

    override fun onCreate(){
        super.onCreate()
        //nessuna inizializzazione aggiuntiva necessaria
        //il repository è lazy, non viene creato qui, ma solo quando serve
    }
}