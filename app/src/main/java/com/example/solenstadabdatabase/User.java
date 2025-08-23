package com.example.solenstadabdatabase;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;

public class User extends AppCompatActivity {

    private TextView statusText;
    private final String FILE_NAME = "settings.json";
    private final String SERVER_URL = "http://your-server.com/settings.json"; // замени на свой сервер

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.user);
        statusText = findViewById(R.id.status_text);

        checkServerOrLocal();
    }

    private void checkServerOrLocal() {
        new Thread(() -> {
            try {
                // 1. Пробуем достучаться до сервера
                URL url = new URL(SERVER_URL);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);
                conn.connect();

                if (conn.getResponseCode() == 200) {
                    Scanner scanner = new Scanner(conn.getInputStream()).useDelimiter("\\A");
                    String response = scanner.hasNext() ? scanner.next() : "";

                    // сохраняем в локальный файл
                    saveToLocalFile(response);

                    updateUI("Данные успешно получены с сервера и сохранены локально.");
                    return;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            // 2. Если сервер недоступен → проверяем локальный файл
            File file = new File(getFilesDir(), FILE_NAME);
            if (file.exists()) {
                String localData = readFromLocalFile();
                updateUI("Нет связи с сервером, загружены локальные данные:\n" + localData);
            } else {
                // 3. Если файла нет → создаём новый с дефолтом
                createDefaultFile();
                updateUI("Нет связи с сервером, создан локальный файл с дефолтными настройками");
            }
        }).start();
    }

    private void saveToLocalFile(String data) {
        try (FileOutputStream fos = openFileOutput(FILE_NAME, MODE_PRIVATE)) {
            fos.write(data.getBytes());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String readFromLocalFile() {
        try (FileInputStream fis = openFileInput(FILE_NAME)) {
            Scanner scanner = new Scanner(fis).useDelimiter("\\A");
            return scanner.hasNext() ? scanner.next() : "";
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    private void createDefaultFile() {
        try {
            JSONObject json = new JSONObject();
            json.put("pin", "1234");
            json.put("first_launch", true);
            json.put("max_attempts", 10);
            json.put("lockout_time", 30000);
            json.put("lockout_start", 0);

            saveToLocalFile(json.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateUI(String message) {
        new Handler(Looper.getMainLooper()).post(() -> {
            statusText.setText(message);
            Toast.makeText(User.this, message, Toast.LENGTH_LONG).show();
        });
    }
}
