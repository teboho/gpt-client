package me.teboho.chatwithgpt;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.theokanning.openai.OpenAiResponse;
import com.theokanning.openai.completion.CompletionChoice;
import com.theokanning.openai.completion.CompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.service.OpenAiService;

import java.io.IOException;
import java.util.StringTokenizer;

import me.teboho.chatwithgpt.databinding.ActivityMainBinding;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {
    String OPENAI_API_KEY = "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";
    ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.chatOutput.setMovementMethod(new ScrollingMovementMethod());

        binding.btnSend.setOnClickListener(l -> {
            binding.chatOutput.setText("Hello");
            new Thread(() -> {
                final MediaType JSON
                        = MediaType.get("application/json; charset=utf-8");

                OkHttpClient client = new OkHttpClient();

                String chat = binding.chatInput.getText().toString();
                if (chat.isEmpty() || chat == null) {
                    chat = "Hello";
                }
                String json = "{\n" +
                        "  \"model\": \"gpt-3.5-turbo\",\n" +
                        "  \"messages\": [{\"role\": \"user\", \"content\": \"" +chat+"\"}]\n" +
                        "}\n";

                String url = "https://api.openai.com/v1/chat/completions";
                RequestBody body = RequestBody.create(json, JSON);
                Request request = new Request.Builder()
                        .addHeader("Authorization", "Bearer " + OPENAI_API_KEY)
                        .url(url)
                        .post(body)
                        .build();
                try (Response response = client.newCall(request).execute()) {
                    String res = response.body().string();
                    System.out.println(res);

                    // Break down the response json
                    ObjectMapper mapper = new ObjectMapper();
                    JsonNode rootNode = mapper.readTree(res);

                    // Get the choices
                    String id = rootNode.get("id").asText();
                    String object = rootNode.get("object").asText();
                    String created = rootNode.get("created").asText();
                    String model = rootNode.get("model").asText();
                    String choices = rootNode.get("choices").asText();
                    System.out.println(choices);
                    JsonNode choicesNode = rootNode.get("choices");
                    int index = choicesNode.get(0).get("index").asInt();
                    String message = choicesNode.get(0).get("message").get("content").asText();
                    String finishReason = choicesNode.get(0).get("finish_reason").asText();
                    showResponse(message);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }, "chat").start();
        });
    }

    private void showResponse(String response) {
        System.out.println(response);
        runOnUiThread(() -> binding.chatOutput.setText(response + "\n"));
    }
}