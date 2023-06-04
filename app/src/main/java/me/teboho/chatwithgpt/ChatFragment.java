package me.teboho.chatwithgpt;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import me.teboho.chatwithgpt.databinding.FragmentChatBinding;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link ChatFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ChatFragment extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    // exposing the API key is not a good practice, but this is just a demo
    String OPENAI_API_KEY = BuildConfig.apikey;
    String model = "gpt-3.5-turbo";
    FragmentChatBinding binding;
    MainViewModel viewModel = new MainViewModel();
    Thread t;

    public ChatFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment ChatFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static ChatFragment newInstance(String param1, String param2) {
        ChatFragment fragment = new ChatFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment

        binding = FragmentChatBinding.inflate(inflater, container, false);

        // handle the buttons
        binding.btnSend.setOnClickListener(this::handleSendButton);
        binding.btnReset.setOnClickListener(this::handleResetButton);

        View view = binding.getRoot();

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // ...
        ChatsAdapter adapter = new ChatsAdapter(viewModel);
        binding.chatsRecyclerView.setAdapter(adapter);

        binding.chatsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        // scroll to bottom of recyclerview
        binding.chatsRecyclerView.scrollToPosition(Objects.requireNonNull(binding.chatsRecyclerView.getAdapter()).getItemCount() - 1);
    }

    public void handleResetButton(View view){
        if (t != null && t.isAlive()) {
            t.interrupt();
            t = null;
        }

        binding.chatInput.clearComposingText();

        binding.progressBar.setVisibility(View.GONE);
        binding.chatInput.clearComposingText();
        viewModel.getChatInput().setValue("");
        viewModel.getChatOutput().setValue("");
        viewModel.getInHistory().getValue().clear();
        viewModel.getOutHistory().getValue().clear();
        binding.chatsRecyclerView.getAdapter().notifyDataSetChanged();
    }

    /**
     * This method is called when the send button is clicked
     * @param view the view that was clicked
     * @author teboho
     */
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
                getActivity().runOnUiThread(() -> binding.chatInput.setError("Please enter something"));
                return;
            } else {
                storeInput(chat);
            }

            chat = complyJSON(chat);

            String json = "{\"model\": \""+ model +"\",\n  \"messages\": [";

            if (viewModel.getInHistory().getValue().size() > 0) {
                for (int i=0; i<viewModel.getInHistory().getValue().size(); i++) {
                    json += "{\"role\": \"user\", \"content\": \"" + complyJSON(viewModel.getInHistory().getValue().get(i)) +"\"}, ";
                    json += "{\"role\": \"assistant\", \"content\": \"" + complyJSON(viewModel.getOutHistory().getValue().get(i)) +"\"}";
                    // add comma if not last element
                    if (i != viewModel.getInHistory().getValue().size() - 1) {
                        json += ",";
                    }
                }
                json += ",{\"role\": \"user\", \"content\": \"" + chat +"\"}";
                json += "]}";
            }
            else if (json.length() > 3900) {
                getActivity().runOnUiThread(() -> {
                    MainActivity.showSnackbar("History data too large\nClearing history...");
                    // fire reset button
                    handleResetButton(view);
                });
            }
            else {
                json = "{\n" +
                        "  \"model\": \""+ model + "\",\n \"messages\": ["
                        +  "{\"role\": \"user\", \"content\": \"" + chat +"\"}]\n}";
            }

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
                if (!response.isSuccessful() || res.contains("error")) {
                    System.out.println("Response code: " + response.code());
                    getActivity().runOnUiThread(() -> {
                        MainActivity.showSnackbar("Error: " + res);
//                        MainActivity.showSnackbar("History data too large\nResetting... Sending request without history, sorry :(");
                        handleResetButton(view);
                    });
                    return;
                } else
                    getActivity().runOnUiThread(() -> binding.chatInput.setError(null));
                getActivity().runOnUiThread(() -> binding.progressBar.setVisibility(View.GONE));

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

                storeOutput(message);
            } catch (IOException e) {
                getActivity().runOnUiThread(() -> binding.chatInput.setError("Something went wrong with the internet request/response"));
                getActivity().runOnUiThread(() -> MainActivity.showSnackbar("Something went wrong with the internet request/response" + e.getMessage()));
                e.printStackTrace();
            }
        }, "chat");
        t.start();
    }

    /**
     * This method stores the input in the view model
     * @param input the input to store in the view model
     */
    private void storeInput(String input) {
        getActivity().runOnUiThread(() -> viewModel.getChatInput().setValue(input));
    }

    /**
     * This method stores the output in the view model
     * @param output the output to store in the view model
     */
    private void storeOutput(String output) {
        getActivity().runOnUiThread(() -> {
            viewModel.getChatOutput().setValue(output);

            // Store the chat in the chat history
            viewModel.getInHistory().getValue().add(viewModel.getChatInput().getValue());
            viewModel.getOutHistory().getValue().add(output);

            viewModel.getLength().setValue(viewModel.getLength().getValue() + 1);
            binding.chatsRecyclerView.getAdapter().notifyItemChanged(viewModel.getLength().getValue() - 1);

            // Scroll to the bottom of the recycler view
            binding.chatsRecyclerView.smoothScrollToPosition(viewModel.getLength().getValue() - 1);
        });
    }

    /**
     *
     * @param str the string to make json compliant
     * @return the json compliant string
     */
    private String complyJSON(String str) {
        // make string json compliant
        String escaped = str.replace("\"", "\\"+"\"");
        escaped = escaped.replace("\n", "\\" + "n");
        escaped = escaped.replace("\r", "\\r");
        escaped = escaped.replace("\t", "\\t");
        escaped = escaped.replace("\b", "\\b");
        escaped = escaped.replace("\f", "\\f");
        escaped = escaped.replace("/", "\\/");

        return escaped;
    }
}