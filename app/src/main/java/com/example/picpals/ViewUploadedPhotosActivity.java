package com.example.picpals;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class ViewUploadedPhotosActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private UploadedPhotosAdapter adapter;
    private List<String> base64Images;
    private List<String> imageNames;
    private List<String> uploaderNames;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_uploaded_photos);

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Fetch images from the server
        fetchImagesFromServer();
    }

    private void fetchImagesFromServer() {

        SharedPreferences sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
        String username = sharedPreferences.getString("username", "");

        // Create a Data object with action and username
        Data inputData = new Data.Builder()
                .putString("action", "get_user_images")
                .putString("username", username) // You need to implement this method to get the username from SharedPreferences
                .build();

        // Create a OneTimeWorkRequest for ConexionDBBajarImagenes
        OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(ConexionDBBajarImagenes.class)
                .setInputData(inputData)
                .build();

        // Enqueue the work request
        WorkManager.getInstance(this).enqueue(workRequest);

        // Observe the result
        WorkManager.getInstance(this).getWorkInfoByIdLiveData(workRequest.getId())
                .observe(this, workInfo -> {
                    if (workInfo != null && workInfo.getState().isFinished()) {
                        // Get the result from the worker
                        Data outputData = workInfo.getOutputData();
                        String result = outputData.getString("result");

                        // Process the result and update the adapter
                        processResultAndUpdateAdapter(result);
                    }
                });
    }

    private void processResultAndUpdateAdapter(String result) {
        if (result != null) {
            if (result.equals("\"No se encontraron imagenes para este usuario\"")) {
                Toast.makeText(this, "No se encontraron imagenes para este usuario", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
            try {
                JSONArray jsonArray = new JSONArray(result);

                base64Images = new ArrayList<>();
                imageNames = new ArrayList<>();
                uploaderNames = new ArrayList<>();
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject jsonObject = jsonArray.getJSONObject(i);

                    String imageData = jsonObject.getString("imagen");
                    base64Images.add(imageData);

                    String imageName = jsonObject.getString("filename");
                    imageNames.add(imageName);

                    String uploaderName = jsonObject.getString("uploader_name");
                    uploaderNames.add(uploaderName);
                }

                adapter = new UploadedPhotosAdapter(this, base64Images, imageNames, uploaderNames);
                recyclerView.setAdapter(adapter);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else {
            Log.e("ViewUploadedPhotos", "Result is null");
        }
    }
}

