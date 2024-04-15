package com.tfg.kaifit_pal.chatcontrollers;

import androidx.annotation.NonNull;

import com.tfg.kaifit_pal.BuildConfig;

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

import com.tfg.kaifit_pal.fragments.kaiqassistant.KaiQ;

public class GPTApiCaller {

    public static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private static final Logger LOGGER = Logger.getLogger(GPTApiCaller.class.getName());
    private final OkHttpClient client;
    private final KaiQ kaiQ;

    public GPTApiCaller(KaiQ kaiQ) {
        this.kaiQ = kaiQ;
        client = new OkHttpClient();
    }

    public void callGPTApi(String query) {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("model", "gpt-3.5-turbo");
            jsonObject.put("prompt", query);
            jsonObject.put("max_tokens", 4000);
            jsonObject.put("temperature", 0.5);
        } catch (JSONException e) {
            LOGGER.log(Level.SEVERE, "Error creating JSON object for GPT API request", e);
        }

        RequestBody body = RequestBody.create(jsonObject.toString(), JSON);
        Request request = new Request.Builder()
                .url("https://api.openai.com/v1/chat/completions")
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
                if (response.isSuccessful()) {
                    try {
                        assert response.body() != null;
                        JSONObject jsonObject = new JSONObject(response.body().string());
                        JSONArray jsonArray = jsonObject.getJSONArray("choices");
                        String result = jsonArray.getJSONObject(0).getString("text");
                        kaiQ.addResponseToChat(result.trim());
                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }

                } else {
                    kaiQ.addResponseToChat("Error al conectar con el servidor" + response.message());
                }
            }
        });
    }
}