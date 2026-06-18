package it.uninsubria.drugdose1.service
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import it.uninsubria.drugdose1.R

/**
 *
 * PERCHÉ UN FOREGROUND SERVICE e non un Background Service?
 * i Background Service sono fortemente limitati:
 * - Il sistema può killarli dopo pochi secondi se l'app non è in primo piano
 * - Non possono fare molto senza interazione utente
 *
 * Un Foreground Service invece:
 * - Mostra UNA NOTIFICA PERSISTENTE all'utente (è sempre visibile)
 * - È "dichiarato" all'utente ->il sistema non lo killa arbitrariamente
 * - Può continuare a funzionare anche quando l'app è in background
 *
 * PERCHÉ ha senso clinicamente?
 * Un promemoria di dosaggio farmacologico DEVE essere visibile.
 * Il personale sanitario non può perdere un promemoria perché l'app
 * è finita in background. La notifica persistente garantisce visibilità.
 */
class DoseReminderService : Service() {

    companion object {
        // ID del canale di notifica (deve essere univoco nell'app)
        const val CHANNEL_ID = "dose_reminder_channel"

        // ID della notifica (deve essere > 0, univoco tra le notifiche attive)
        const val NOTIFICATION_ID = 42

        // Chiavi per gli extra dell'Intent (costanti per evitare errori di battitura)
        const val EXTRA_DRUG_NAME = "extra_drug_name"
        const val EXTRA_DOSE = "extra_dose"
        const val EXTRA_UNIT = "extra_unit"
    }

    /**
     * onCreate: chiamato una sola volta quando il Service viene creato.
     * Qui creiamo il canale di notifica.
     */
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Recupera i dati dall'Intent (con valori di default se null)
        // PERCHÉ usare costanti (EXTRA_DRUG_NAME)? Evita errori di battitura.
        // Il Fragment usa DoseReminderService.EXTRA_DRUG_NAME → stessa stringa garantita.
        val drugName = intent?.getStringExtra(EXTRA_DRUG_NAME) ?: "Farmaco"
        val dose = intent?.getStringExtra(EXTRA_DOSE) ?: ""

        // Crea la notifica persistente
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            // Titolo della notifica
            .setContentTitle("Promemoria: $drugName")
            // Testo della notifca
            .setContentText(dose)
            // Icona nella status bar (deve essere un drawable vettoriale o bitmap)
            .setSmallIcon(R.drawable.ic_medication)
            // Priorità: HIGH = notifica con suono e in cima alla lista
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            // setOngoing(true): l'utente NON può swipare via questa notifica
            // È necessario per una notifica di promemoria medico importante
            .setOngoing(true)
            .build()

        // startForeground: registra il Service come Foreground e mostra la notifica
        // NOTIFICATION_ID: identificatore della notifica (>0)
        startForeground(NOTIFICATION_ID, notification)

        // START_STICKY: se il sistema killa il Service per mancanza di memoria,
        // lo riavvia automaticamente (con intent null)
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        // STOP_FOREGROUND_REMOVE = rimuovi la notifica quando il Service si ferma
        stopForeground(STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }

    /**
     * Crea il canale di notifica (obbligatorio da API 26).
     * Idempotente: chiamarlo più volte con lo stesso CHANNEL_ID non crea duplicati.
     *
     * IMPORTANCE_HIGH: notifica con suono, appare in cima all'area notifiche.
     * Appropriate per un promemoria medico importante.
     */
    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,                                         // ID univoco del canale
            getString(R.string.notification_channel_name),     // nome visibile nelle impostazioni
            NotificationManager.IMPORTANCE_HIGH                 // importanza = tipo di notifica
        ).apply {
            description = getString(R.string.notification_channel_desc)
        }

        // getSystemService: recupera un servizio di sistema Android
        // NotificationManager: gestisce le notifiche dell'app
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }
}