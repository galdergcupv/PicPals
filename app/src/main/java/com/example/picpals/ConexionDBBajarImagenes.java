package com.example.picpals;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class ConexionDBBajarImagenes extends Worker {

    public ConexionDBBajarImagenes(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @Override
    @NonNull
    public Result doWork() {
        // Obtener los datos del usuario
        Data inputData = getInputData();
        String action = inputData.getString("action");
        String username = inputData.getString("username");

        // URL del servidor
        String url = "http://34.29.139.252:81/imagenes.php";

        try {
            // Establecer la conexión
            URL urlObj = new URL(url);
            HttpURLConnection conn = (HttpURLConnection) urlObj.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);

            // Construir el cuerpo de la solicitud
            JSONObject jsonParam = new JSONObject();
            jsonParam.put("action", action); // Se cambia el valor de "action" según el tipo de solicitud
            jsonParam.put("username", username);

            // Escribir los datos en la solicitud
            OutputStream os = conn.getOutputStream();
            os.write(jsonParam.toString().getBytes());
            os.flush();
            os.close();

            // Añadir logs para ver la solicitud enviada al servidor
            Log.d("ConexionDBimagenes", "Solicitud enviada al servidor:");
            Log.d("ConexionDBimagenes", "URL: " + url);
            Log.d("ConexionDBimagenes", "Método: " + conn.getRequestMethod());
            Log.d("ConexionDBimagenes", "Cuerpo de la solicitud: " + jsonParam.toString());

            // Leer la respuesta del servidor
            InputStream inputStream = conn.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();
            inputStream.close();

            // Añadir logs para ver la respuesta del servidor
            String result = response.toString();
            Log.d("ConexionDBImagenes", "Respuesta del servidor: " + result);

            // Enviar el resultado de vuelta a la actividad
            Data outputData = new Data.Builder()
                    .putString("result", result)
                    .build();
            return Result.success(outputData);

        } catch (IOException | JSONException e) {
            e.printStackTrace();
            return Result.failure();
        }
    }
}
