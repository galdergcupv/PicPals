package com.example.picpals;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

public class RegisterActivity extends AppCompatActivity {

    private EditText editTextNewUsername;
    private EditText editTextNewPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        editTextNewUsername = findViewById(R.id.editTextNewUsername);
        editTextNewPassword = findViewById(R.id.editTextNewPassword);
    }

    public void register(View view) {
        String username = editTextNewUsername.getText().toString().trim();
        String password = editTextNewPassword.getText().toString().trim();

        Data inputData = new Data.Builder()
                .putString("action", "register")
                .putString("username", username)
                .putString("password", password)
                .build();

        OneTimeWorkRequest registerRequest =
                new OneTimeWorkRequest.Builder(ConexionDBUsuarios.class)
                        .setInputData(inputData)
                        .build();

        WorkManager.getInstance(this).enqueue(registerRequest);

        // Observar el resultado del trabajo en segundo plano
        WorkManager.getInstance(this).getWorkInfoByIdLiveData(registerRequest.getId())
                .observe(this, new Observer<WorkInfo>() {
                    @Override
                    public void onChanged(@Nullable WorkInfo workInfo) {
                        if (workInfo != null && workInfo.getState().isFinished()) {
                            Data outputData = workInfo.getOutputData();
                            String result = outputData.getString("result");

                            // Mostrar el resultado del registro al usuario
                            Toast.makeText(RegisterActivity.this, result, Toast.LENGTH_SHORT).show();

                            // Si el registro es exitoso, cerrar la actividad y volver a la actividad principal
                            if (result.equals("Registro exitoso")) {
                                finish(); // Cerrar la actividad actual
                                Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
                                startActivity(intent); // Ir a la actividad principal
                            }
                        }
                    }
                });
    }
}
