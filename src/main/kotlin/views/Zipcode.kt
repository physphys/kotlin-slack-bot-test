package views

import kotlinx.serialization.Serializable

@Serializable
data class SearchAddressesByZipcodeResponse(val message: String?, val status: Int, val results: List<Address>)

@Serializable
data class Address(val address1: String, val address2: String, val address3: String)