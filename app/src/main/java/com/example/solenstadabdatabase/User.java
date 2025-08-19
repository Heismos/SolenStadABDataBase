package com.example.solenstadabdatabase;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.*;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.*;
import java.nio.charset.StandardCharsets;
import okhttp3.*;

public class User extends AppCompatActivity {

    private static final String SERVER_URL = "http://heiserver.ddns.net/D/server/Heis_nexus/SolenStadAB/data/user.json";
    private static final String TOKEN = "HEISAdmin";
    private static final String LOCAL_FILE = "user.json";
    private static final String KEY_REMEMBER = "remember_user";
    private static final String KEY_REMEMBER_NAME = "remember_name";

    private OkHttpClient httpClient = new OkHttpClient();

    private LinearLayout usersContainer;
    private Button addUserButton;
    private CheckBox rememberCheckBox;
    private EditText searchEdit;
    private SharedPreferences sharedPreferences;
    private SwipeRefreshLayout swipeRefreshLayout;

    private JSONArray localUsers = new JSONArray();
    private JSONArray serverUsers = new JSONArray();

    private ActivityResultLauncher<Intent> addUserLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.user);

        ConstraintLayout mainLayout = findViewById(R.id.main);
        LinearLayout topBlock = findViewById(R.id.top_block);

        usersContainer = findViewById(R.id.users_container);
        addUserButton = findViewById(R.id.my_button);
        rememberCheckBox = findViewById(R.id.my_checkbox);
        searchEdit = findViewById(R.id.editText);
        swipeRefreshLayout = findViewById(R.id.swipe_refresh);

        sharedPreferences = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);

        // Проверка запомненного пользователя
        boolean remember = sharedPreferences.getBoolean(KEY_REMEMBER, false);
        if (remember) {
            String name = sharedPreferences.getString(KEY_REMEMBER_NAME, "Неизвестный");

            // Переход на ProjectActivity
            Intent intent = new Intent(User.this, ProjectActivity.class);
            startActivity(intent);
            finish(); // закрываем SecurityActivity
        }

        // --- Подъем блока при появлении клавиатуры ---
        mainLayout.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            Rect r = new Rect();
            mainLayout.getWindowVisibleDisplayFrame(r);
            int screenHeight = mainLayout.getRootView().getHeight();
            int keypadHeight = screenHeight - r.bottom;
            boolean keyboardVisible = keypadHeight > screenHeight * 0.15f;
            ConstraintLayout.LayoutParams lp = (ConstraintLayout.LayoutParams) topBlock.getLayoutParams();
            int targetMargin = dp(keyboardVisible ? 30 : 310);
            if (lp.topMargin != targetMargin) {
                lp.topMargin = targetMargin;
                topBlock.setLayoutParams(lp);
                mainLayout.requestLayout();
            }
        });

        loadUsersFromLocal();
        showUsers(localUsers);
        loadUsersFromServer();

        swipeRefreshLayout.setOnRefreshListener(() -> {
            loadUsersFromServer();
            swipeRefreshLayout.setRefreshing(false);
        });

        addUserLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        loadUsersFromLocal();
                        showUsers(localUsers);
                    }
                });

        addUserButton.setOnClickListener(v -> {
            Intent intent = new Intent(User.this, GreatUserActivity.class);
            addUserLauncher.launch(intent);
        });

        rememberCheckBox.setOnCheckedChangeListener((buttonView, isChecked) ->
                sharedPreferences.edit().putBoolean(KEY_REMEMBER, isChecked).apply()
        );

        searchEdit.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { filterUsers(s.toString()); }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private int dp(int value) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(value * density);
    }

    private void loadUsersFromServer() {
        Request request = new Request.Builder()
                .url(SERVER_URL)
                .addHeader("Authorization", "Bearer " + TOKEN)
                .build();
        httpClient.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {
                Log.e("User", "Ошибка соединения: " + e.getMessage());
            }
            @Override public void onResponse(Call call, Response response) throws IOException {
                String bodyStr = response.body() != null ? response.body().string() : "";
                if (response.isSuccessful()) {
                    try {
                        serverUsers = new JSONArray(bodyStr);
                        mergeUsers();
                        runOnUiThread(() -> showUsers(localUsers));
                    } catch (Exception e) {
                        runOnUiThread(() -> Toast.makeText(User.this, "Ошибка парсинга JSON", Toast.LENGTH_SHORT).show());
                    }
                } else {
                    runOnUiThread(() -> Toast.makeText(User.this, "Ошибка сервера: " + response.code(), Toast.LENGTH_SHORT).show());
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
        saveToLocalFile();
    }

    private void saveToLocalFile() {
        try {
            File file = new File(getFilesDir(), LOCAL_FILE);
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(localUsers.toString().getBytes(StandardCharsets.UTF_8));
            fos.close();
        } catch (IOException e) {
            Log.e("User", "Ошибка сохранения локального файла: " + e.getMessage());
        }
    }

    private String readFile(InputStream is) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] data = new byte[1024];
        int nRead;
        while ((nRead = is.read(data, 0, data.length)) != -1) buffer.write(data, 0, nRead);
        return buffer.toString(StandardCharsets.UTF_8.name());
    }

    private void showUsers(JSONArray users) {
        usersContainer.removeAllViews();
        int marginInDp = 10;
        int marginInPx = (int) (marginInDp * getResources().getDisplayMetrics().density + 0.5f);

        for (int i = 0; i < users.length(); i++) {
            JSONObject user = users.optJSONObject(i);
            if (user == null) continue;
            String name = user.optString("userName", "Без имени");

            Button btn = new Button(this);
            btn.setText(name);
            btn.setBackgroundResource(R.drawable.button_selector);
            btn.setTextColor(Color.parseColor("#c0b27b"));

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            params.setMargins(0, marginInPx, 0, marginInPx);
            btn.setLayoutParams(params);

            btn.setOnClickListener(v -> {
                // Переход на SecurityActivity
                Intent intent = new Intent(User.this, SecurityActivity.class);
                intent.putExtra("userName", name);
                intent.putExtra("rememberChecked", rememberCheckBox.isChecked());
                startActivity(intent);
            });

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
