package me.teboho.chatwithgpt;

import android.media.Image;
import android.net.Uri;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.Date;

import me.teboho.chatwithgpt.http.HttpClient;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link ImageGenFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ImageGenFragment extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    TextInputEditText textInputEditText;
    MaterialButton generateBtn;
    ImageView imageView;
    HttpClient httpClient = HttpClient.getInstance();
    String url = "https://api.openai.com/v1/images/generations";
    Thread thread;

    public ImageGenFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment ImageGenFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static ImageGenFragment newInstance(String param1, String param2) {
        ImageGenFragment fragment = new ImageGenFragment();
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

    public void handleImageGen() {
        String prompt = textInputEditText.getText().toString();
        if (prompt.isEmpty()) {
            textInputEditText.setError("Please enter a prompt");
            return;
        }

        String jsonBody = "{" +
                "\"prompt\": \"" + prompt + "\"," +
                "\"n\": 1," +
                "\"size\": \"1024x1024\"" +
                "}";
        //jsonBody = ChatFragment.complyJSON(jsonBody);
        Log.d("", "handleImageGen: " + jsonBody);

        String finalJsonBody = jsonBody;
        thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String response = httpClient.post(url, finalJsonBody);
                    Log.d("", "run: " + response);

                    // Interpretting the response, json object with created, and data array of urls
                    ObjectMapper objectMapper = new ObjectMapper();
                    JsonNode jsonNode = null;
                    String imageUrl = "";
                    jsonNode = objectMapper.readTree(response);
                    String created = jsonNode.get("created").asText();
                    JsonNode data = jsonNode.get("data");
                    for (JsonNode node : data) {
                        imageUrl = node.get("url").asText();
                    }
                    Log.d("", "run: " + imageUrl);

                    // Displaying the image
                    String finalImageUrl = imageUrl;
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            imageView.setImageDrawable(null);
                            imageView.setImageURI(null);
                            imageView.setImageURI(Uri.parse(finalImageUrl));

                            textInputEditText.setText(finalImageUrl);
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        thread.start();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_image_gen, container, false);
        textInputEditText = view.findViewById(R.id.promptTextInputEditText);
        generateBtn = view.findViewById(R.id.generateBtn);
        imageView = view.findViewById(R.id.imageView);
        generateBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleImageGen();
            }
        });
        return view;
    }
}