package it.uninsubria.drugdose1.utils

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import it.uninsubria.drugdose1.data.model.Drug

//parsing di file JSON, carica e deserializza il file assets/drug.json in List<Drug>
/*
Utilizziamo object perchè JsonLoader non ha stato: non tiene variabili che cambiano nel tempo.
Riceve un context,legge un file,restituisce dati. Non ha senso creare più istanze (Singleton)
 */

object JsonLoader {
    fun loadDrugs(context: Context): List<Drug> {
        return try{
            //apri il file
            val inputStream = context.assets.open("drug.json")
            //leggi il contenuto
            val jsonString=inputStream.bufferedReader().use { reader ->
                reader.readText() //legge l intero file come stringa
            }
            val listType = object : TypeToken<List<Drug>>() {}.type
            val drugs: List<Drug> = Gson().fromJson(jsonString, listType)
            drugs?: emptyList()
        }catch (e: Exception){
            emptyList()
        }
    }
}