package com.example.picpals;

import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.Observer;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.messaging.FirebaseMessaging;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    private SharedPreferences sharedPreferences;
    private Button btnLogin, btnRegister, btnLogout;

    private Button btnTakePhoto, btnChoosePhoto, btnViewUploadedPhotos, btnViewSharedPhotos;
    private TextView tvUsername;

    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int REQUEST_IMAGE_PICK = 2;

    private static final int CAMERA_PERMISSION_REQUEST_CODE = 100;

    private static final int READ_EXTERNAL_STORAGE_PERMISSION_REQUEST_CODE = 200;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);

        btnLogin = findViewById(R.id.btnLogin);
        btnRegister = findViewById(R.id.btnRegister);
        btnLogout = findViewById(R.id.btnLogout);
        tvUsername = findViewById(R.id.tvUsername);

        btnTakePhoto = findViewById(R.id.btnTakePhoto);
        btnChoosePhoto = findViewById(R.id.btnChoosePhoto);
        btnViewUploadedPhotos = findViewById(R.id.btnViewPhotos);
        btnViewSharedPhotos = findViewById(R.id.btnViewSharedPhotos);

        checkSession();

        // Verifica si tienes permiso de cámara
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            // Si no tienes permiso, solicítalo
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST_CODE);
        }
        // Verifica si tienes permiso para acceder a la galería
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            // Si no tienes permiso, solicítalo
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE}, READ_EXTERNAL_STORAGE_PERMISSION_REQUEST_CODE);
        }

        // Si la versión es 13 o superior: verifica si tienes permiso para enviar notificaciones
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)!=
                    PackageManager.PERMISSION_GRANTED) {
                // Si no tienes permiso, solicítalo
                ActivityCompat.requestPermissions(this, new
                        String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 11);
            }
        }

        // Crear canal por defecto si la versión es OREO o superior
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel("canal por defecto", "canal por defecto", NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription("canal por defecto");
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }

        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(new OnCompleteListener<String>() {
                    @Override
                    public void onComplete(@NonNull Task<String> task) {
                        if (!task.isSuccessful()) {

                            return;
                        }
                        String token = task.getResult();
                        SharedPreferences sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE);
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.putString("fcm_token", token);
                        editor.apply();
                        Log.d("MainActivity", "Token firebase: " + token);

                    }
                });
    }




    public void loginClicked(View view) {
        // Aquí iniciamos la actividad de inicio de sesión
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        startActivity(intent);
    }

    public void registerClicked(View view) {
        // Aquí iniciamos la actividad de registro
        Intent intent = new Intent(this, RegisterActivity.class);
        startActivity(intent);
    }

    public void logoutClicked(View view) {
        // Crear un diálogo de confirmación antes de cerrar la sesión
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Confirmación");
        builder.setMessage("¿Estás seguro de que quieres cerrar la sesión?");
        builder.setPositiveButton("Sí", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                // Eliminar el nombre de usuario de SharedPreferences
                String username = sharedPreferences.getString("username", "");
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.remove("username");
                editor.apply();
                removeFCMToken(username);

                // Ocultar el botón de logout y el nombre de usuario
                btnLogout.setVisibility(View.GONE);
                tvUsername.setVisibility(View.GONE);

                // Mostrar los botones de inicio de sesión y registro
                btnLogin.setVisibility(View.VISIBLE);
                btnRegister.setVisibility(View.VISIBLE);
            }
        });
        builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                // No hacer nada si se selecciona "No"
            }
        });
        builder.show();
    }

    private boolean checkSession() {
        String username = sharedPreferences.getString("username", "");
        if (!username.isEmpty()) {
            // Si hay un nombre de usuario en SharedPreferences, lo mostramos en la barra de botones
            tvUsername.setText(username);
            btnLogout.setVisibility(View.VISIBLE);
            tvUsername.setVisibility(View.VISIBLE);
            btnLogin.setVisibility(View.GONE);
            btnRegister.setVisibility(View.GONE);
        }
        return !username.isEmpty();
    }

    // Método de clic para el botón "Sacar Foto"
    public void takePhotoClicked(View view) {
        if (!checkSession()) {
            showLoginRequiredDialog();
        } else {
            // Verifica si tienes permiso de cámara
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED) {
                // Si no tienes permiso, solicítalo
                ActivityCompat.requestPermissions(this,
                        new String[]{android.Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST_CODE);
            }
            else{
                Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                    startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
                }
            }
        }
    }

    // Método de clic para el botón "Elegir Foto Existente"
    public void choosePhotoClicked(View view) {
        if (!checkSession()) {
            showLoginRequiredDialog();
        } else {
            // Verifica si tienes permiso para acceder a la galería
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                // Si no tienes permiso, solicítalo
                ActivityCompat.requestPermissions(this,
                        new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE}, READ_EXTERNAL_STORAGE_PERMISSION_REQUEST_CODE);
            } else {
                Intent pickImageIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                if (pickImageIntent.resolveActivity(getPackageManager()) != null) {
                    startActivityForResult(pickImageIntent, REQUEST_IMAGE_PICK);
                }
            }
        }
    }

    // Método de clic para el botón "Ver Fotos Subidas"
    public void viewPhotosClicked(View view) {
        if (!checkSession()) {
            showLoginRequiredDialog();
        } else {
            Intent intent = new Intent(MainActivity.this, ViewUploadedPhotosActivity.class);
            startActivity(intent);
        }
    }


    // Método de clic para el botón "Ver Fotos Compartidas"
    public void viewSharedPhotosClicked(View view) {
        if (!checkSession()) {
            showLoginRequiredDialog();
        } else {
            Intent intent = new Intent(MainActivity.this, ViewSharedPhotosActivity.class);
            startActivity(intent);
        }
    }


    // Diálogo de inicio de sesión requerido
    private void showLoginRequiredDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Inicio de Sesión Requerido");
        builder.setMessage("Es necesario iniciar sesión para acceder a esta función.");
        builder.setPositiveButton("Iniciar Sesión", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                // Redirige al usuario a la pantalla de inicio de sesión
                Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                startActivity(intent);
            }
        });
        builder.setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                // No hacer nada si se selecciona "Cancelar"
            }
        });
        builder.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        int maxWidth = 50;
        int maxHeight = 25;
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Bundle extras = data.getExtras();
            if (extras != null) {
                Bitmap imageBitmap = (Bitmap) extras.get("data");
                if (imageBitmap != null) {
                    Bitmap resizedBitmap = resizeImage(imageBitmap, maxWidth, maxHeight);

                    // Convertir la imagen a base64
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    resizedBitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
                    byte[] imageByteArray = baos.toByteArray();
                    String imageBase64 = android.util.Base64.encodeToString(imageByteArray, android.util.Base64.DEFAULT);

                    // Iniciar UploadImageActivity y pasar la imagen como base64
                    Intent intent = new Intent(MainActivity.this, UploadImageActivity.class);
                    intent.putExtra("imageBase64", imageBase64);
                    startActivity(intent);
                }
            }
        } else if (requestCode == REQUEST_IMAGE_PICK) {
            Uri selectedImageUri = data.getData();
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), selectedImageUri);

                Bitmap resizedBitmap = resizeImage(bitmap, maxWidth, maxHeight);

                // Convertir la imagen a base64
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                resizedBitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
                byte[] imageByteArray = baos.toByteArray();
                String imageBase64 = android.util.Base64.encodeToString(imageByteArray, android.util.Base64.DEFAULT);

                // Iniciar UploadImageActivity y pasar la imagen como base64
                Intent intent = new Intent(MainActivity.this, UploadImageActivity.class);
                intent.putExtra("imageBase64", imageBase64);
                startActivity(intent);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    // Método para reescalar la imagen
    private Bitmap resizeImage(Bitmap image, int maxWidth, int maxHeight) {
        int width = image.getWidth();
        int height = image.getHeight();

        float ratioBitmap = (float) width / (float) height;
        float ratioMax = (float) maxWidth / (float) maxHeight;

        int finalWidth = maxWidth;
        int finalHeight = maxHeight;
        if (ratioMax > ratioBitmap) {
            finalWidth = (int) ((float) maxHeight * ratioBitmap);
        } else {
            finalHeight = (int) ((float) maxWidth / ratioBitmap);
        }

        return Bitmap.createScaledBitmap(image, finalWidth, finalHeight, true);
    }

    // Método para eliminar el token FCM del usuario en la base de datos
    private void removeFCMToken(String username){
        Data inputData = new Data.Builder()
                .putString("action", "logout")
                .putString("username", username)
                .build();

        OneTimeWorkRequest loginRequest =
                new OneTimeWorkRequest.Builder(ConexionDBUsuarios.class)
                        .setInputData(inputData)
                        .build();

        WorkManager.getInstance(this).enqueue(loginRequest);

        // Observar el resultado del trabajo en segundo plano
        WorkManager.getInstance(this).getWorkInfoByIdLiveData(loginRequest.getId())
                .observe(this, new Observer<WorkInfo>() {
                    @Override
                    public void onChanged(@Nullable WorkInfo workInfo) {
                        if (workInfo != null && workInfo.getState().isFinished()) {
                            Data outputData = workInfo.getOutputData();
                            String result = outputData.getString("result");

                            // Mostrar el resultado del logout
                            Toast.makeText(MainActivity.this, result, Toast.LENGTH_SHORT).show();

                        }
                    }
                });
    }
}

