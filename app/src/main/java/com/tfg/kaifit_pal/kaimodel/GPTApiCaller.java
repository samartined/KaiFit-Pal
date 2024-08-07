package com.tfg.kaifit_pal.kaimodel;

import androidx.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import com.tfg.kaifit_pal.views.fragments.kaiq.KaiQ;
//TODO: repair this import
import com.tfg.kaifit_pal.BuildConfig;

public class GPTApiCaller {
    private final ChatHistoryManager chatHistoryManager;
    public static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private static final Logger LOGGER = Logger.getLogger(GPTApiCaller.class.getName());
    private final OkHttpClient client;
    private final KaiQ kaiQ;

    public GPTApiCaller(KaiQ kaiQ) {
        this.kaiQ = kaiQ;
        this.chatHistoryManager = new ChatHistoryManager();
        client = new OkHttpClient();
    }

    public void gptApiRequest(String query) {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("model", "gpt-3.5-turbo");
            if (chatHistoryManager.getChatHistory().isEmpty()) {
                chatHistoryManager.addAssistantMessageToHistory("Eres Kai-Q, debes actuar como un nutricionista deportivo real, " +
                        "inteligente y conciso con sus respuestas. " +
                        "Eres un asistente que complementa al uso de una calculadora nutricional. " +
                        "Das la información concreta y breve que te pide el usuario, y solo amplias tu explicación si requiere un conocimiento muy técnico.");
            }
            chatHistoryManager.addUserMessageToHistory(query);
            jsonObject.put("messages", new JSONArray(chatHistoryManager.getChatHistory()));
            jsonObject.put("max_tokens", 800);
            jsonObject.put("temperature", 0.2);
        } catch (JSONException e) {
            LOGGER.log(Level.SEVERE, "Error creating JSON object for GPT API request", e);
        }


        RequestBody body = RequestBody.create(jsonObject.toString(), JSON);
        Request request = new Request.Builder()
                .url("https://api.openai.com/v1/chat/completions")
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", "Bearer " + BuildConfig.AKy_URL)
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {

            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                kaiQ.addResponseToChat("Error al conectar con el servidor" + e.getMessage());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                assert response.body() != null;
                String responseString = response.body().string();
                if (response.isSuccessful()) {
                    try {
                        JSONObject responseJson = new JSONObject(responseString);
                        JSONArray choices = responseJson.getJSONArray("choices");
                        JSONObject choice = choices.getJSONObject(0);
                        JSONObject message = choice.getJSONObject("message");
                        String responseMessage = message.getString("content");
                        kaiQ.addResponseToChat(responseMessage);
                        chatHistoryManager.addAssistantMessageToHistory(responseMessage);
                    } catch (JSONException e) {
                        LOGGER.log(Level.SEVERE, "Error parsing JSON response from GPT API", e);
                    }
                } else {
                    kaiQ.addResponseToChat("Error al conectar con el servidor" + responseString);
                }
            }
        });
    }
}