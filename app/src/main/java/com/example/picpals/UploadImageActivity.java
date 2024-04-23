package com.example.picpals;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import java.io.ByteArrayOutputStream;

public class UploadImageActivity extends AppCompatActivity {

    private ImageView imageView;
    private EditText editTextImageName;
    private EditText editTextShareWith;

    private Bitmap imageBitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload_image);

        imageView = findViewById(R.id.imageView);
        editTextImageName = findViewById(R.id.editTextName);
        editTextShareWith = findViewById(R.id.editTextShareWith);

        // Recibir la imagen en base64 del Intent
        String imageBase64 = getIntent().getStringExtra("imageBase64");

        // Convertir la imagen en base64 de nuevo a Bitmap
        byte[] decodedString = android.util.Base64.decode(imageBase64, android.util.Base64.DEFAULT);
        imageBitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);

        // Mostrar la imagen en imageView
        imageView.setImageBitmap(imageBitmap);
    }

    public void uploadImage(View view) {
        String imageName = editTextImageName.getText().toString().trim();
        String shareWith = editTextShareWith.getText().toString().trim();

        // Verificar si se ha introducido un nombre de imagen
        if (imageName.isEmpty()) {
            Toast.makeText(this, "Por favor, introduce un nombre para la imagen", Toast.LENGTH_SHORT).show();
            return;
        }

        // Convertir la imagen a base64
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        imageBitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
        byte[] imageByteArray = baos.toByteArray();
        String imageBase64 = android.util.Base64.encodeToString(imageByteArray, android.util.Base64.DEFAULT);



        // Obtener el nombre de usuario de las preferencias
        SharedPreferences sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
        String username = sharedPreferences.getString("username", "");

        // Construir los datos de entrada para la solicitud
        Data inputData = new Data.Builder()
                .putString("action", "save_image")
                .putString("filename", imageName)
                .putString("uploaderName", username)
                .putString("imageBase64", imageBase64)
                .putString("shareWith", shareWith)
                .build();

        // Crear la solicitud de trabajo en segundo plano para guardar la imagen
        OneTimeWorkRequest uploadRequest =
                new OneTimeWorkRequest.Builder(ConexionDBSubirImagenes.class)
                        .setInputData(inputData)
                        .build();

        // Enviar la solicitud de trabajo en segundo plano
        WorkManager.getInstance(this).enqueue(uploadRequest);

        // Observar el resultado del trabajo en segundo plano
        WorkManager.getInstance(this).getWorkInfoByIdLiveData(uploadRequest.getId())
                .observe(this, new Observer<WorkInfo>() {
                    @Override
                    public void onChanged(@Nullable WorkInfo workInfo) {
                        if (workInfo != null && workInfo.getState().isFinished()) {
                            Data outputData = workInfo.getOutputData();
                            String result = outputData.getString("result");

                            // Mostrar el resultado al usuario
                            Toast.makeText(UploadImageActivity.this, result, Toast.LENGTH_SHORT).show();

                            // Si la carga de la imagen es exitosa, cierra la actividad y vuelve a la actividad principal
                            if (result.equals("Imagen guardada con éxito")) {
                                finish();
                                Intent intent = new Intent(UploadImageActivity.this, MainActivity.class);
                                startActivity(intent);
                            }
                        }
                    }
                });
    }

}
