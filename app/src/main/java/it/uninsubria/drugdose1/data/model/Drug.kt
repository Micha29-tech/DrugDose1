package it.uninsubria.drugdose1.data.model
import com.google.gson.annotations.SerializedName

data class Drug (
    @SerializedName("id") val id : String,
    @SerializedName("name") val name : String,
    @SerializedName("indication") val indication : String,
    @SerializedName("formulaType") val formulaType : FormulaType,
    @SerializedName("dosePerUnit") val dosePerUnit : Double,
    @SerializedName("unit") val unit : String,
    @SerializedName("minDose") val minDose : Double? = null,
    @SerializedName("maxDose") val maxDose : Double? = null,
    @SerializedName("minWeight") val minWeight : Double? = null,
    @SerializedName("minAge") val minAge : Int? = null,
    @SerializedName("administrations") val administrations : Int = 1,
    @SerializedName("alerts") val alerts : List<String> =emptyList(),
    @SerializedName("source") val source : String,
    @SerializedName("weightRanges") val weightRanges : List<WeightRange>? = null
)


