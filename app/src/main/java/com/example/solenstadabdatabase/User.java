package com.example.solenstadabdatabase;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.nio.charset.StandardCharsets;

import okhttp3.*;

public class User extends AppCompatActivity {

    private static final String SERVER_URL = "http://heiserver.ddns.net/D/server/Heis_nexus/SolenStadAB/data/user.json";
    private static final String TOKEN = "HEISAdmin";
    private static final String LOCAL_FILE = "user.json";
    private static final String KEY_REMEMBER = "remember_user";

    private OkHttpClient httpClient = new OkHttpClient();
    private LinearLayout usersContainer;
    private Button addUserButton;
    private CheckBox rememberCheckBox;
    private EditText searchEdit;
    private SharedPreferences sharedPreferences;

    private JSONArray localUsers = new JSONArray();
    private JSONArray serverUsers = new JSONArray();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.user);

        usersContainer = findViewById(R.id.users_container);
        addUserButton = findViewById(R.id.my_button);
        rememberCheckBox = findViewById(R.id.my_checkbox);
        searchEdit = findViewById(R.id.editText);

        // Добавляем отступ сверху у контейнера списка пользователей
        int marginInDp = 10;
        int marginInPx = (int) (marginInDp * getResources().getDisplayMetrics().density + 0.5f);
        usersContainer.setPadding(
                usersContainer.getPaddingLeft(),
                marginInPx,
                usersContainer.getPaddingRight(),
                usersContainer.getPaddingBottom()
        );

        sharedPreferences = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);

        boolean remember = sharedPreferences.getBoolean(KEY_REMEMBER, false);
        if (remember) {
            Toast.makeText(this, "Пользователь запомнен. Переход к списку проектов...", Toast.LENGTH_LONG).show();
            // Здесь можно добавить переход на следующий экран
            return;
        }

        loadUsersFromServer();

        addUserButton.setOnClickListener(v ->
                Toast.makeText(User.this, "Открытие формы добавления пользователя", Toast.LENGTH_SHORT).show()
        );

        rememberCheckBox.setOnCheckedChangeListener((buttonView, isChecked) ->
                sharedPreferences.edit().putBoolean(KEY_REMEMBER, isChecked).apply()
        );

        // Фильтрация при вводе текста
        searchEdit.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterUsers(s.toString());
            }
            @Override public void afterTextChanged(Editable s) { }
        });
    }

    private void loadUsersFromServer() {
        Request request = new Request.Builder()
                .url(SERVER_URL)
                .addHeader("Authorization", "Bearer " + TOKEN)
                .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("User", "Ошибка соединения: " + e.getMessage());
                runOnUiThread(() -> {
                    Toast.makeText(User.this, "Сервер недоступен, загружаем локальные данные", Toast.LENGTH_LONG).show();
                    loadUsersFromLocal();
                    showUsers(localUsers);
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String bodyStr = response.body() != null ? response.body().string() : "";
                if (response.isSuccessful()) {
                    try {
                        serverUsers = new JSONArray(bodyStr);
                        saveToLocalFile(bodyStr);
                        mergeUsers();
                        runOnUiThread(() -> showUsers(localUsers));
                    } catch (JSONException e) {
                        runOnUiThread(() -> Toast.makeText(User.this, "Ошибка парсинга JSON", Toast.LENGTH_SHORT).show());
                    }
                } else {
                    runOnUiThread(() -> {
                        Toast.makeText(User.this, "Ошибка сервера: " + response.code(), Toast.LENGTH_SHORT).show();
                        loadUsersFromLocal();
                        showUsers(localUsers);
                    });
                }
            }
        });
    }

    private void loadUsersFromLocal() {
        try {
            File file = new File(getFilesDir(), LOCAL_FILE);
            if (!file.exists()) return;
            FileInputStream fis = new FileInputStream(file);
            String json = readFile(fis);
            fis.close();
            localUsers = new JSONArray(json);
        } catch (Exception e) {
            Log.e("User", "Ошибка чтения локального файла: " + e.getMessage());
        }
    }

    private void saveToLocalFile(String json) {
        try {
            File file = new File(getFilesDir(), LOCAL_FILE);
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(json.getBytes(StandardCharsets.UTF_8));
            fos.close();
        } catch (IOException e) {
            Log.e("User", "Ошибка сохранения локального файла: " + e.getMessage());
        }
    }

    private String readFile(InputStream is) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] data = new byte[1024];
        int nRead;
        while ((nRead = is.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }
        return buffer.toString(StandardCharsets.UTF_8.name());
    }

    private void mergeUsers() {
        loadUsersFromLocal();
        for (int i = 0; i < serverUsers.length(); i++) {
            JSONObject serverUser = serverUsers.optJSONObject(i);
            boolean exists = false;
            for (int j = 0; j < localUsers.length(); j++) {
                if (localUsers.optJSONObject(j).optString("userName")
                        .equals(serverUser.optString("userName"))) {
                    exists = true;
                    break;
                }
            }
            if (!exists) localUsers.put(serverUser);
        }
        saveToLocalFile(localUsers.toString());
    }

    private void showUsers(JSONArray users) {
        usersContainer.removeAllViews();

        int marginInDp = 10; // отступ в dp
        int marginInPx = (int) (marginInDp * getResources().getDisplayMetrics().density + 0.5f);

        for (int i = 0; i < users.length(); i++) {
            String name = users.optJSONObject(i).optString("userName", "Без имени");

            Button btn = new Button(this);
            btn.setText(name);
            btn.setBackgroundResource(R.drawable.button_selector);
            btn.setTextColor(Color.parseColor("#c0b27b"));

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,  // ширина
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(0, marginInPx, 0, marginInPx); // сверху и снизу отступ
            btn.setLayoutParams(params);

            btn.setOnClickListener(v ->
                    Toast.makeText(this, "Выбран: " + name, Toast.LENGTH_SHORT).show()
            );

            usersContainer.addView(btn);
        }
    }

    private void filterUsers(String query) {
        JSONArray filtered = new JSONArray();
        for (int i = 0; i < localUsers.length(); i++) {
            JSONObject user = localUsers.optJSONObject(i);
            if (user != null && user.optString("userName", "").toLowerCase().contains(query.toLowerCase())) {
                filtered.put(user);
            }
        }
        showUsers(filtered);
    }
}
