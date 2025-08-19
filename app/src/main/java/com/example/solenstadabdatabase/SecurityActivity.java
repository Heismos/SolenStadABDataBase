package com.example.solenstadabdatabase;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.*;
import java.nio.charset.StandardCharsets;

public class SecurityActivity extends AppCompatActivity {

    private static final String LOCAL_FILE = "user.json";
    private static final String PREFS_NAME = "UserPrefs";
    private static final String KEY_ATTEMPTS = "attempts";
    private static final String KEY_LOCK_TIME = "lock_time";
    private static final int MAX_ATTEMPTS = 10;
    private static final long LOCK_DURATION = 10000; // 10 секунд тест

    private EditText passwordEdit;
    private Button enterButton;
    private TextView infoText;
    private ConstraintLayout mainLayout;

    private String userName;
    private boolean rememberChecked;
    private SharedPreferences prefs;
    private int attempts;
    private long lockTime;

    private JSONArray usersArray;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_security);

        mainLayout = findViewById(R.id.security_main);
        passwordEdit = findViewById(R.id.password_edit);
        enterButton = findViewById(R.id.enter_button);
        infoText = findViewById(R.id.info_text);

        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        userName = getIntent().getStringExtra("userName");
        rememberChecked = getIntent().getBooleanExtra("rememberChecked", false);

        attempts = prefs.getInt(KEY_ATTEMPTS + userName, 0);
        lockTime = prefs.getLong(KEY_LOCK_TIME + userName, 0);

        loadUsers();

        checkLock();

        enterButton.setOnClickListener(v -> {
            if (System.currentTimeMillis() < lockTime) {
                long remain = (lockTime - System.currentTimeMillis()) / 1000;
                infoText.setText("Попытки заблокированы. Попробуйте через " + remain + " сек");
                return;
            }

            String inputPass = passwordEdit.getText().toString();
            if (checkPassword(userName, inputPass)) {
                // Сохраняем состояние "запомнить пользователя", если нужно
                if (rememberChecked) {
                    prefs.edit().putString("remember_name", userName)
                            .putBoolean("remember_user", true).apply();
                }

                // Сброс попыток
                prefs.edit().putInt(KEY_ATTEMPTS + userName, 0).apply();

                // Переход на ProjectActivity
                Intent intent = new Intent(SecurityActivity.this, ProjectActivity.class);
                startActivity(intent);
                finish(); // закрываем SecurityActivity
            } else {
                attempts++;
                if (attempts >= MAX_ATTEMPTS) {
                    lockTime = System.currentTimeMillis() + LOCK_DURATION;
                    prefs.edit().putLong(KEY_LOCK_TIME + userName, lockTime).apply();
                    infoText.setText("Максимум попыток достигнут. Заблокировано на 10 секунд.");
                    enterButton.setEnabled(false);
                    new Handler().postDelayed(() -> {
                        attempts = 0;
                        prefs.edit().putInt(KEY_ATTEMPTS + userName, attempts).apply();
                        infoText.setText("");
                        enterButton.setEnabled(true);
                    }, LOCK_DURATION);
                } else {
                    infoText.setText("Неправильный пароль. Осталось попыток: " + (MAX_ATTEMPTS - attempts));
                }
                prefs.edit().putInt(KEY_ATTEMPTS + userName, attempts).apply();
            }
        });
    }

    private void checkLock() {
        long now = System.currentTimeMillis();
        if (now < lockTime) {
            long remain = (lockTime - now) / 1000;
            infoText.setText("Попытки заблокированы. Попробуйте через " + remain + " сек");
            enterButton.setEnabled(false);
            new Handler().postDelayed(() -> {
                enterButton.setEnabled(true);
                infoText.setText("");
                attempts = 0;
                prefs.edit().putInt(KEY_ATTEMPTS + userName, attempts).apply();
            }, lockTime - now);
        }
    }

    private void loadUsers() {
        try {
            File file = new File(getFilesDir(), LOCAL_FILE);
            if (!file.exists()) return;
            FileInputStream fis = new FileInputStream(file);
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] data = new byte[1024];
            int nRead;
            while ((nRead = fis.read(data, 0, data.length)) != -1) buffer.write(data, 0, nRead);
            fis.close();
            usersArray = new JSONArray(buffer.toString(StandardCharsets.UTF_8.name()));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean checkPassword(String username, String pass) {
        if (usersArray == null) return false;
        for (int i = 0; i < usersArray.length(); i++) {
            JSONObject user = usersArray.optJSONObject(i);
            if (user != null && user.optString("userName").equals(username) &&
                    user.optString("userPass").equals(pass)) {
                return true;
            }
        }
        return false;
    }
}
