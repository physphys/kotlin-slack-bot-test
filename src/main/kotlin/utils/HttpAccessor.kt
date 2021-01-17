package utils

import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.result.Result
import com.github.kittinunf.fuel.json.responseJson
import org.json.JSONObject

class HttpAccessor {
    fun getJson(url: String): JSONObject {
        val (_, _, result) = url.httpGet().responseJson()

        return when (result) {
            is Result.Failure -> {
                val ex = result.getException()
                println(ex)
                JSONObject(mapOf("message" to ex.toString()))
            }
            is Result.Success -> {
                println(result.get())
                result.get().obj()
            }
        }
    }
}