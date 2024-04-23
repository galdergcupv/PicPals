package com.example.picpals;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.messaging.FirebaseMessaging;

public class LoginActivity extends AppCompatActivity {

    private EditText editTextUsername;
    private EditText editTextPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        editTextUsername = findViewById(R.id.editTextUsername);
        editTextPassword = findViewById(R.id.editTextPassword);
    }

    public void login(View view) {
        String username = editTextUsername.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();
        SharedPreferences sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE);
        String fcm_token = sharedPreferences.getString("fcm_token", null);


        Data inputData = new Data.Builder()
                .putString("action", "login")
                .putString("username", username)
                .putString("password", password)
                .putString("fcm_token", fcm_token)
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

                            // Mostrar el resultado del inicio de sesión al usuario
                            Toast.makeText(LoginActivity.this, result, Toast.LENGTH_SHORT).show();

                            // Si el inicio de sesión es exitoso, guardar el nombre de usuario en SharedPreferences
                            if (result.equals("Inicio de sesión exitoso")) {
                                SharedPreferences.Editor editor = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE).edit();
                                editor.putString("username", username);
                                editor.apply();

                                // Cerrar la actividad actual y volver a la actividad principal
                                finish();
                                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                                startActivity(intent);
                            }
                        }
                    }
                });
    }

}
