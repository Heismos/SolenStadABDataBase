package com.example.solenstadabdatabase;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class User extends AppCompatActivity {

    private static final String SERVER_URL = "http://heiserver.ddns.net/D/server/Heis_nexus/SolenStadAB/data/user.json";

    private static final String TOKEN = "HEISAdmin";  // Токен должен совпадать с серверным
    private static final String PREFS_NAME = "UserPrefs";
    private static final String KEY_REMEMBER = "remember_user";

    private OkHttpClient httpClient = new OkHttpClient();

    private LinearLayout usersContainer;
    private Button addUserButton;
    private CheckBox rememberCheckBox;
    private EditText editText;
    private TextView textView;  // Если нужен для вывода

    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.user);

        usersContainer = findViewById(R.id.users_container);
        addUserButton = findViewById(R.id.my_button);
        rememberCheckBox = findViewById(R.id.my_checkbox);
        editText = findViewById(R.id.editText);
        textView = findViewById(R.id.textView);  // Убедись, что такой TextView есть в layout или создай его

        sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        boolean remember = sharedPreferences.getBoolean(KEY_REMEMBER, false);
        if (remember) {
            Toast.makeText(this, "Пользователь запомнен. Переход к списку проектов...", Toast.LENGTH_LONG).show();
            //startActivity(new Intent(this, ProjectListActivity.class));
            //finish();
            return;
        }

        loadUsersFromServer();

        addUserButton.setOnClickListener(v -> {
            Toast.makeText(User.this, "Теперь должна открыться activity регистрации", Toast.LENGTH_SHORT).show();
        });

        rememberCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            sharedPreferences.edit().putBoolean(KEY_REMEMBER, isChecked).apply();
        });
    }

    private void loadUsersFromServer() {
        Request request = new Request.Builder()
                .url(SERVER_URL)
                .addHeader("Authorization", "Bearer HEISAdmin")  // токен в заголовке
                .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    String msg = "Сервер временно не доступен, синхронизация не возможна.\n" +
                            "Вы можете зарегистрироваться локально, после соединения с сервером получите доступ к данным.";
                    editText.setText(msg);
                    if (textView != null) textView.setText(msg);
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String bodyStr = response.body() != null ? response.body().string() : "";
                runOnUiThread(() -> {
                    if (response.isSuccessful()) {
                        try {
                            // Попытка разобрать json и показать кнопки
                            showUsersFromJson(bodyStr);
                            // Также выводим raw ответ в текстовое поле (если нужно)
                            editText.setText(bodyStr);
                            if (textView != null) textView.setText("");
                        } catch (Exception e) {
                            editText.setText("Ошибка разбора данных");
                            if (textView != null) textView.setText("");
                        }
                    } else {
                        if (response.code() == 403) {
                            editText.setText("Ошибка: Доступ запрещён. Неверный токен.");
                            if (textView != null) textView.setText("");
                        } else {
                            editText.setText("Ошибка ответа сервера: " + response.code());
                            if (textView != null) textView.setText("");
                        }
                    }
                });
            }
        });
    }

    private void showUsersFromJson(String jsonData) {
        usersContainer.removeAllViews();
        try {
            JSONArray usersArray = new JSONArray(jsonData);

            for (int i = 0; i < usersArray.length(); i++) {
                JSONObject userObj = usersArray.getJSONObject(i);
                String userName = userObj.optString("userName", "Без имени");

                Button userButton = new Button(this);
                userButton.setText(userName);
                userButton.setOnClickListener(v -> {
                    Toast.makeText(User.this, "Вы выбрали пользователя: " + userName, Toast.LENGTH_SHORT).show();
                    // Логика для перехода или загрузки проектов
                });

                usersContainer.addView(userButton);
            }

        } catch (JSONException e) {
            e.printStackTrace();
            editText.setText("Ошибка разбора данных");
            if (textView != null) textView.setText("");
        }
    }
}
