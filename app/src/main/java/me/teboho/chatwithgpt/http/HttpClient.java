package me.teboho.chatwithgpt.http;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import me.teboho.chatwithgpt.BuildConfig;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okio.BufferedSink;

public class HttpClient {
    final MediaType JSON = MediaType.get("application/json");
    String OPENAI_API_KEY = BuildConfig.apikey;
    public static HttpClient instance;
    OkHttpClient client;

    private HttpClient() {
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        builder.connectTimeout(5, TimeUnit.MINUTES) // connect timeout
                .writeTimeout(5, TimeUnit.MINUTES) // write timeout
                .readTimeout(5, TimeUnit.MINUTES); // read timeout

        client = builder.build();
    }

    public static HttpClient getInstance() {
        if (instance == null) {
            instance = new HttpClient();
        }
        return instance;
    }

    public String post(String url, String jsonBody) {
        String responseString = "";
        RequestBody body = RequestBody.create(jsonBody.getBytes(), JSON); // entering the body in bytes to supress charset error
        Request request = new Request.Builder()
                .addHeader("Authorization", "Bearer " + OPENAI_API_KEY)
                .url(url)
                .post(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            System.out.println("Response: " + response);
            responseString = response.body().string();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return responseString;
    }
}
