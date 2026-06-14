package it.uninsubria.drugdose1.data.model

import com.google.gson.annotations.SerializedName

data class WeightRange (
    @SerializedName("minKg") val minKg : Double, //Peso minimo della fascia
    @SerializedName("maxKg") val maxKg : Double, //Peso massimo della fascia
    @SerializedName("dose") val dose : Double, //Dose corrispondente a questa fascia di peso
    @SerializedName("unit") val unit : String //Unità di misura della dose
)
