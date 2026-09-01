package expo.modules.ehcnfc.readingresult

import com.google.gson.Gson

data class PersonalData(
    val title: String?,
    val firstname: String?,
    val lastname: String?,
    val nameAddition: String?,
    val birthday: String?,
    val gender: String?,
    val street: String?,
    val housenumber: String?,
    val zipCode: String?,
    val city: String?,
    val countryCode: String?,
    val insuranceId: String?,
) {
    companion object {
        val EMPTY_PD = PersonalData(
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            ""
        )
        fun fromMap(map: Map<String, String>?): PersonalData {
            if(map == null){
                return EMPTY_PD
            }
            return PersonalData(
                title = map["Titel"],
                firstname = map["Vorname"],
                lastname = map["Nachname"],
                nameAddition = map["Namenszusatz"],
                birthday = map["Geburtsdatum"],
                gender = map["Geschlecht"],
                street = map["Strasse"],
                housenumber = map["Hausnummer"],
                zipCode = map["Postleitzahl"],
                city = map["Ort"],
                countryCode = map["Wohnsitzlaendercode"],
                insuranceId = map["Versicherten_ID"],
            )
        }
    }
    fun isEmpty(): Boolean{
        if(this@PersonalData.firstname == "") {
            return true
        }
        return false
    }
    fun toJson(): String{
        val gson = Gson()
        return gson.toJson(this@PersonalData)
    }
}