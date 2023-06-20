package me.teboho.chatwithgpt;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.Image;
import android.net.Uri;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.io.IOException;
import java.net.URL;
import java.util.Date;

import me.teboho.chatwithgpt.http.HttpClient;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link ImageGenFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ImageGenFragment extends Fragment {
    public final String name = "Dall-E | Image Generation";

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
    ProgressBar dallePB;
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
        dallePB.setVisibility(View.VISIBLE);

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
                    Log.d("ImageGen", "run: " + response);
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getActivity(), "Preparing to show your image", Toast.LENGTH_SHORT).show();
                        }
                    });
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
                    URL url = new URL(finalImageUrl);
                    Bitmap bmp = null;
                    Bitmap finalBmp = BitmapFactory.decodeStream(url.openConnection().getInputStream());

                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            dallePB.setVisibility(View.GONE);
                            imageView.setVisibility(View.VISIBLE);
                            imageView.setAnimation(android.view.animation.AnimationUtils.loadAnimation(getActivity(), android.R.anim.fade_in));
                            imageView.setImageBitmap(finalBmp);

                            imageView.setOnCreateContextMenuListener(new View.OnCreateContextMenuListener() {
                                @Override
                                public void onCreateContextMenu(android.view.ContextMenu menu, View v, android.view.ContextMenu.ContextMenuInfo menuInfo) {
                                    menu.add(0, 1, 0, "Save Image").setOnMenuItemClickListener(new android.view.MenuItem.OnMenuItemClickListener() {
                                        @Override
                                        public boolean onMenuItemClick(android.view.MenuItem item) {
                                            Bitmap bitmap = finalBmp;
                                            String path = MediaStore.Images.Media.insertImage(getActivity().getContentResolver(), bitmap, prompt.toLowerCase().replace("\s", ""), prompt);
                                            Uri uri = Uri.parse(path);
                                            Toast.makeText(getActivity(), "Image Saved", Toast.LENGTH_SHORT).show();
                                            return false;
                                        }
                                    });
                                }
                            });
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
        dallePB = view.findViewById(R.id.dallePB);
        generateBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleImageGen();
            }
        });

        return view;
    }
}