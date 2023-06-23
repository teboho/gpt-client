package me.teboho.chatwithgpt;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.palette.graphics.Palette;

import android.provider.MediaStore;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.RequestCreator;
import com.squareup.picasso.Target;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;

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
    TextView tvCountdown;
    TextView tvImageInfo;
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

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_image_gen, container, false);
        textInputEditText = view.findViewById(R.id.promptTextInputEditText);
        generateBtn = view.findViewById(R.id.generateBtn);
        imageView = view.findViewById(R.id.imageView);
        dallePB = view.findViewById(R.id.dallePB);
        tvCountdown = view.findViewById(R.id.tvCountdown);
        tvImageInfo = view.findViewById(R.id.tvImageInfo);
        generateBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleImageGen();
            }
        });

        return view;
    }

    public void handleImageGen() {
        String prompt = textInputEditText.getText().toString();
        if (prompt.isEmpty()) {
            textInputEditText.setError("Please enter a prompt");
            return;
        }
        dallePB.setVisibility(View.VISIBLE);

        String jsonBody =
                "{" +
                "\"prompt\": \"" + prompt + "\"," +
                "\"n\": 1," +
                "\"size\": \"1024x1024\"" +
                "}";
        jsonBody = jsonBody.replace("\n", "\\n");
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
                            InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                            imm.hideSoftInputFromWindow(textInputEditText.getWindowToken(), 0);

                            Toast.makeText(getActivity(), "Preparing to show your image", Toast.LENGTH_SHORT).show();
                        }
                    });

                    // Interpreting the response, json object with created, and data array of urls
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

//                    renderImageStream(imageUrl);
                    renderWithPicasso(imageUrl);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        thread.start();
    }

    private void renderWithPicasso(String imageUrl) {
        getActivity().runOnUiThread(() -> {
            dallePB.setVisibility(View.GONE);
            imageView.setVisibility(View.VISIBLE);
            tvImageInfo.setVisibility(View.VISIBLE);

            Target tag = new Target() {
                @Override
                public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                    tvImageInfo.setText("Image loaded");
                    imageView.setImageBitmap(bitmap);
                }

                @Override
                public void onBitmapFailed(Exception e, Drawable errorDrawable) {
                    tvImageInfo.setText("Image failed to load");
                }

                @Override
                public void onPrepareLoad(Drawable placeHolderDrawable) {
                    tvImageInfo.setText("Image loading...");
                }
            };

            imageView.setTag(tag);

            Picasso.get().load(imageUrl).into(tag);

            imageView.setOnCreateContextMenuListener(
                    (menu, v, menuInfo) ->
                            menu.add(0, v.getId(), 0, "Save Image")
                                .setOnMenuItemClickListener(item -> {
                                    MediaStore.Images.Media.insertImage(getActivity().getContentResolver(), ((BitmapDrawable)imageView.getDrawable()).getBitmap(), new Date().toString().replace("\s", "-"), "Dall-E Image");
                                    Toast.makeText(getActivity(), "Saved Image", Toast.LENGTH_SHORT).show();
                                    return false;
                                })
            );
        });
    }

    private void renderImage(String imageUrl) {
        thread = new Thread(() -> {
            // Downloading the image from the url
            byte[] imageBytes = httpClient.get(imageUrl);
            // still slo
            Bitmap finalBmp = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);;//BitmapFactory.decodeStream(url.openConnection().getInputStream());

            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    dallePB.setVisibility(View.GONE);
                    imageView.setVisibility(View.VISIBLE);
                    imageView.setAnimation(android.view.animation.AnimationUtils.loadAnimation(getActivity(), R.anim.image_fade_in));
                    imageView.setImageBitmap(finalBmp);

                    imageView.setOnCreateContextMenuListener(new View.OnCreateContextMenuListener() {
                        @Override
                        public void onCreateContextMenu(android.view.ContextMenu menu, View v, android.view.ContextMenu.ContextMenuInfo menuInfo) {
                            menu.add(0, 1, 0, "Save Image").setOnMenuItemClickListener(new android.view.MenuItem.OnMenuItemClickListener() {
                                @Override
                                public boolean onMenuItemClick(android.view.MenuItem item) {
                                    Bitmap bitmap = finalBmp;
                                    String path = MediaStore.Images.Media.insertImage(getActivity().getContentResolver(), bitmap, new Date().toString().replace("\s", "-"), textInputEditText.getText().toString());
                                    Uri uri = Uri.parse(path);
                                    Toast.makeText(getActivity(), "Image Saved", Toast.LENGTH_SHORT).show();
                                    return false;
                                }
                            });
                        }
                    });
                }
            });
        }, "Rendering image");
        thread.start();
    }

    private void renderImageStream(String imageUrl) {
        getActivity().runOnUiThread(() -> {
            tvCountdown.setVisibility(View.VISIBLE);
            imageView.setAnimation(android.view.animation.AnimationUtils.loadAnimation(getActivity(), R.anim.fade_out));
            // imageView.setVisibility(View.INVISIBLE);
        });
        AtomicInteger count = new AtomicInteger(); // a thread safe sentinel because we modify it in a completely different thread
        Thread t = new Thread(() -> {
            // getting the byte stream from the http client
            InputStream imageByteStream = httpClient.getStream(imageUrl);

            // this timer task will be used to increment the counter as BitMapFactory takes its time to decode the byte stream
            TimerTask task = new TimerTask() {
                @Override
                public void run() {
                    getActivity().runOnUiThread(() -> tvCountdown.setText(String.format("%d", count.getAndIncrement())));
                }
            };
            Timer timer = new Timer("Download Timer");
            long delay = 1000L;
            timer.schedule(task, 0, delay);

            Bitmap finalBmp = BitmapFactory.decodeStream(new BufferedInputStream(imageByteStream));

            getActivity().runOnUiThread(() -> tvCountdown.setVisibility(View.GONE));
            timer.cancel();
            // decoding the stream is done
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    dallePB.setVisibility(View.GONE);
                    imageView.setVisibility(View.VISIBLE);
                    imageView.setAnimation(AnimationUtils.loadAnimation(getActivity(), android.R.anim.slide_in_left));
                    imageView.setImageBitmap(finalBmp);
                    tvImageInfo.setText(String.format("Took %d seconds", count.get()));
                    tvImageInfo.setVisibility(View.VISIBLE);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        try {
                            Palette palette = Palette.from(finalBmp).generate();
                            Palette.Swatch swatch = palette.getDominantSwatch();
                            if (swatch != null) {
                                int color = swatch.getRgb();
                                int textColor = swatch.getTitleTextColor();
                                tvImageInfo.setTextColor(textColor);
                                tvImageInfo.setBackgroundColor(color);
                                // change the background color of the image view
                                imageView.setBackgroundColor(color);
                                imageView.setPadding(2, 2, 2, 2);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    imageView.setOnCreateContextMenuListener(new View.OnCreateContextMenuListener() {
                        @Override
                        public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
                            menu.add(0, 1, 0, "Save Image").setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                                @Override
                                public boolean onMenuItemClick(MenuItem item) {
                                    Bitmap bitmap = finalBmp;
                                    String path = MediaStore.Images.Media.insertImage(getActivity().getContentResolver(), bitmap, new Date().toString().replace("\s", "-"), textInputEditText.getText().toString());
                                    Uri uri = Uri.parse(path);
                                    Toast.makeText(getActivity(), "Image Saved", Toast.LENGTH_SHORT).show();
                                    return false;
                                }
                            });
                        }
                    });
                }
            });

            try {
                if (imageByteStream.available() < 0) {
                    imageByteStream.close();
                }
            } catch (IOException e) {
                Toast.makeText(getActivity(), "Could not close the stream", Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }
        }, "Rendering image");
        t.start();
    }

}