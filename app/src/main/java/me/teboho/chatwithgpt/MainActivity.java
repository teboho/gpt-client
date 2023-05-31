package me.teboho.chatwithgpt;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.graphics.Color;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.View;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.android.material.snackbar.Snackbar;
import com.theokanning.openai.OpenAiResponse;
import com.theokanning.openai.completion.CompletionChoice;
import com.theokanning.openai.completion.CompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.service.OpenAiService;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Objects;
import java.util.StringTokenizer;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import me.teboho.chatwithgpt.databinding.ActivityMainBinding;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {
    // exposing the API key is not a good practice, but this is just a demo
    String OPENAI_API_KEY = "";
    ActivityMainBinding binding;
    Thread t;

    MainViewModel viewModel = new MainViewModel();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // binding.chatOutput.setMovementMethod(new ScrollingMovementMethod()); //make textview scrollable

        ChatsAdapter adapter = new ChatsAdapter(viewModel);
        binding.chatsRecyclerView.setAdapter(adapter);

        binding.chatsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        // scroll to bottom of recyclerview
        binding.chatsRecyclerView.scrollToPosition(Objects.requireNonNull(binding.chatsRecyclerView.getAdapter()).getItemCount() - 1);
    }

    public void handleResetButton(View view){
        if (t != null && t.isAlive()) {
            t.interrupt();
            t = null;
        }

        binding.progressBar.setVisibility(View.GONE);
        binding.chatInput.clearComposingText();
        viewModel.getChatInput().setValue("");
        viewModel.getChatOutput().setValue("");
        viewModel.getInHistory().getValue().clear();
        viewModel.getOutHistory().getValue().clear();
        binding.chatsRecyclerView.getAdapter().notifyDataSetChanged();
    }

    public void handleSendButton(View view) {
        // Show loading indicator
        binding.progressBar.setVisibility(View.VISIBLE);
        t = new Thread(() -> {
            final MediaType JSON
                    = MediaType.get("application/json; charset=utf-8");

            OkHttpClient.Builder builder = new OkHttpClient.Builder();
            builder.connectTimeout(5, TimeUnit.MINUTES) // connect timeout
                    .writeTimeout(5, TimeUnit.MINUTES) // write timeout
                    .readTimeout(5, TimeUnit.MINUTES); // read timeout

            OkHttpClient client = builder.build();

            String chat = binding.chatInput.getText().toString();
            if (chat.isEmpty() || chat == null) {
                runOnUiThread(() -> binding.chatInput.setError("Please enter something"));
                return;
            } else
                storeInput(chat);

            String json = "{\n" +
                    "  \"model\": \"gpt-3.5-turbo-0301\",\n  \"messages\": [";

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
            }
            else if (json.length() > 3900) {
                runOnUiThread(() -> {
                    showSnackbar("History data too large\nSending request without history...");
                    handleResetButton(view);
                    handleSendButton(view);
                });
                storeInput(binding.chatInput.getText().toString());
                json = "{\n" +
                        "  \"model\": \"gpt-3.5-turbo-0301\",\n \"messages\": ["
                        +  "{\"role\": \"user\", \"content\": \"" + chat +"\"}]\n}";
            }
            else {
                json = "{\n" +
                        "  \"model\": \"gpt-3.5-turbo-0301\",\n \"messages\": ["
                        +  "{\"role\": \"user\", \"content\": \"" + chat +"\"}]\n}";
            }

            System.out.println(json);

            String url = "https://api.openai.com/v1/chat/completions";
            json = json.replace("\n", " "); // json cannot contain newlines

            RequestBody body = RequestBody.create(json, JSON);
            Request request = new Request.Builder()
                    .addHeader("Authorization", "Bearer " + OPENAI_API_KEY)
                    .url(url)
                    .post(body)
                    .build();
            try (Response response = client.newCall(request).execute()) {
                String res = response.body().string();
                System.out.println("Response: \n" + res);

                if (!response.isSuccessful() || res.contains("error")) {
                    runOnUiThread(() -> {
                        showSnackbar("History data too large\nResetting... Sending request without history, sorry :(");
                        handleResetButton(view);
                        handleSendButton(view);
                    });
                    return;
                } else runOnUiThread(() -> binding.chatInput.setError(null));
                runOnUiThread(() -> binding.chatInput.setError(null));
                runOnUiThread(() -> binding.progressBar.setVisibility(View.GONE));

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
                runOnUiThread(() -> binding.chatInput.setError("Something went wrong with the internet request/response"));
                e.printStackTrace();
            }
        }, "chat");
        t.start();
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
            binding.chatsRecyclerView.getAdapter().notifyDataSetChanged();

            // Scroll to the bottom of the recycler view
            binding.chatsRecyclerView.smoothScrollToPosition(viewModel.getLength().getValue() - 1);
        });
    }

    public void showSnackbar(String message) {
        Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_SHORT).setTextColor(Color.GREEN).show();
    }
}