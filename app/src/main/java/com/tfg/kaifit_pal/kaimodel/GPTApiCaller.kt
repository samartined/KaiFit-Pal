package com.tfg.kaifit_pal.kaimodel

import com.tfg.kaifit_pal.BuildConfig
import com.tfg.kaifit_pal.views.fragments.kaiq.KaiQ
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Request.Builder.addHeader
import okhttp3.Request.Builder.build
import okhttp3.Request.Builder.post
import okhttp3.Request.Builder.url
import okhttp3.RequestBody
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.util.logging.Level
import java.util.logging.Logger

//TODO: repair this import
class GPTApiCaller(private val kaiQ: KaiQ) {
    private val chatHistoryManager = ChatHistoryManager()
    private val client = OkHttpClient()

    fun gptApiRequest(query: String) {
        val jsonObject = JSONObject()
        try {
            jsonObject.put("model", "gpt-3.5-turbo")
            if (chatHistoryManager.chatHistory.isEmpty()) {
                chatHistoryManager.addAssistantMessageToHistory(
                    "Eres Kai-Q, debes actuar como un nutricionista deportivo real, " +
                            "inteligente y conciso con sus respuestas. " +
                            "Eres un asistente que complementa al uso de una calculadora nutricional. " +
                            "Das la información concreta y breve que te pide el usuario, y solo amplias tu explicación si requiere un conocimiento muy técnico."
                )
            }
            chatHistoryManager.addUserMessageToHistory(query)
            jsonObject.put("messages", JSONArray(chatHistoryManager.chatHistory))
            jsonObject.put("max_tokens", 800)
            jsonObject.put("temperature", 0.2)
        } catch (e: JSONException) {
            LOGGER.log(Level.SEVERE, "Error creating JSON object for GPT API request", e)
        }


        val body: RequestBody = RequestBody.create(jsonObject.toString(), JSON)
        val request: Request = Builder()
            .url("https://api.openai.com/v1/chat/completions")
            .addHeader("Content-Type", "application/json")
            .addHeader("Authorization", "Bearer " + BuildConfig.AKy_URL)
            .post(body)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                kaiQ.addResponseToChat("Error al conectar con el servidor" + e.message)
            }

            @Throws(IOException::class)
            override fun onResponse(call: Call, response: Response) {
                checkNotNull(response.body())
                val responseString = response.body()!!.string()
                if (response.isSuccessful) {
                    try {
                        val responseJson = JSONObject(responseString)
                        val choices = responseJson.getJSONArray("choices")
                        val choice = choices.getJSONObject(0)
                        val message = choice.getJSONObject("message")
                        val responseMessage = message.getString("content")
                        kaiQ.addResponseToChat(responseMessage)
                        chatHistoryManager.addAssistantMessageToHistory(responseMessage)
                    } catch (e: JSONException) {
                        LOGGER.log(Level.SEVERE, "Error parsing JSON response from GPT API", e)
                    }
                } else {
                    kaiQ.addResponseToChat("Error al conectar con el servidor$responseString")
                }
            }
        })
    }

    companion object {
        val JSON: MediaType = get.get("application/json; charset=utf-8")
        private val LOGGER: Logger = Logger.getLogger(
            GPTApiCaller::class.java.name
        )
    }
}