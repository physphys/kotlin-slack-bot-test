package views

import kotlinx.serialization.Serializable

@Serializable
data class SearchAddressesByZipcodeResponse(val message: String?, val status: Int)