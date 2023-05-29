package me.teboho.chatwithgpt;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.View;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.theokanning.openai.OpenAiResponse;
import com.theokanning.openai.completion.CompletionChoice;
import com.theokanning.openai.completion.CompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.service.OpenAiService;

import java.io.IOException;
import java.util.Objects;
import java.util.StringTokenizer;
import java.util.logging.Logger;

import me.teboho.chatwithgpt.databinding.ActivityMainBinding;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {
    String OPENAI_API_KEY = "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";
    ActivityMainBinding binding;

    MainViewModel viewModel = new MainViewModel();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.chatOutput.setMovementMethod(new ScrollingMovementMethod());

        viewModel.getChatOutput().observe(this, chatOutput -> {
            binding.chatOutput.setText(chatOutput);
        });
    }

    public void handleResetButton(View view){
        binding.chatInput.clearComposingText();
        viewModel.getChatInput().setValue("");
        viewModel.getChatOutput().setValue("");
        viewModel.getInHistory().getValue().clear();
        viewModel.getOutHistory().getValue().clear();
    }

    public void handleSendButton(View view) {
        new Thread(() -> {
            final MediaType JSON
                    = MediaType.get("application/json; charset=utf-8");

            OkHttpClient client = new OkHttpClient();

            String chat = binding.chatInput.getText().toString();
            if (chat.isEmpty() || chat == null) {
                runOnUiThread(() -> binding.chatInput.setError("Please enter something"));
                return;
            } else
                storeInput(chat);

            String json = "{\n" +
                    "  \"model\": \"gpt-3.5-turbo\",\n  \"messages\": [";

            if (viewModel.getInHistory().getValue().size() > 0) {
                for (int i=0; i<viewModel.getInHistory().getValue().size(); i++) {
                    json += "{\"role\": \"user\", \"content\": \"" + viewModel.getInHistory().getValue().get(i) +"\"}, ";
                    json += "{\"role\": \"assistant\", \"content\": \"" + viewModel.getOutHistory().getValue().get(i) +"\"}";
                    if (i != viewModel.getInHistory().getValue().size() - 1) {
                        json += ",";
                    }
                }
                json += ",{\"role\": \"user\", \"content\": \"" + chat +"\"}";
                json += "]\n}";
            } else {
                json = "{\n" +
                        "  \"model\": \"gpt-3.5-turbo\",\n \"messages\": ["
                        +  "{\"role\": \"user\", \"content\": \"" + chat +"\"}]\n}";
            }

            if (json.length() > 2800)
                json = "{\n"
                        +"  \"model\": \"gpt-3.5-turbo\",\n \"messages\": ["
                        +  "{\"role\": \"user\", \"content\": \"" + chat +"\"}]\n}";
            System.out.println(json);

            String url = "https://api.openai.com/v1/chat/completions";
            RequestBody body = RequestBody.create(json, JSON);
            Request request = new Request.Builder()
                    .addHeader("Authorization", "Bearer " + OPENAI_API_KEY)
                    .url(url)
                    .post(body)
                    .build();
            try (Response response = client.newCall(request).execute()) {
                String res = response.body().string();
                System.out.println("Response: \n" + res);

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

                //showResponse(message);
                storeOutput(message);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }, "chat").start();
    }

    private void showResponse(String response) {
        System.out.println(response);
        runOnUiThread(() -> viewModel.getChatOutput().setValue(response + "\n"));
    }

    private void storeInput(String input) {
        runOnUiThread(() -> viewModel.getChatInput().setValue(input));
    }

    private void storeOutput(String output) {
        runOnUiThread(() -> {
            viewModel.getChatOutput().setValue(output);

            // Store the chat in the chat history
            viewModel.getInHistory().getValue().add(binding.chatInput.getText().toString());
            viewModel.getOutHistory().getValue().add(output);

            viewModel.getLength().setValue(viewModel.getLength().getValue() + 1);
        });
    }
}