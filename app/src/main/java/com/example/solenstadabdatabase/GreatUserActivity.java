package com.example.solenstadabdatabase;

import android.os.Bundle;
import android.text.InputFilter;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class GreatUserActivity extends AppCompatActivity {

    private static final String SERVER_URL =
            "http://heiserver.ddns.net/D/server/Heis_nexus/SolenStadAB/data/user.json";
    private static final String TOKEN = "HEISAdmin";
    private static final String LOCAL_FILE = "user.json";

    private EditText firstNameEdit, userNameEdit, userPassEdit, telEdit, userMailEdit;
    private Button addButton;
    private ScrollView scrollView;

    private OkHttpClient httpClient = new OkHttpClient();
    private JSONArray localUsers = new JSONArray();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_great_user);

        scrollView = findViewById(R.id.scrollView);
        firstNameEdit = findViewById(R.id.firstNameEdit);
        userNameEdit = findViewById(R.id.userNameEdit);
        userPassEdit = findViewById(R.id.userPassEdit);
        telEdit = findViewById(R.id.telEdit);
        userMailEdit = findViewById(R.id.userMailEdit);
        addButton = findViewById(R.id.addUserButton);

        // Ограничения
        userNameEdit.setFilters(new InputFilter[]{new InputFilter.LengthFilter(20)});
        userPassEdit.setFilters(new InputFilter[]{new InputFilter.LengthFilter(20)});
        telEdit.setFilters(new InputFilter[]{new InputFilter.LengthFilter(15)});

        loadLocalUsers();

        addButton.setOnClickListener(v -> addUser());

        // Автоподъём формы при клавиатуре
        var cardContainer = findViewById(R.id.cardContainer);
        final int initialTopPad = cardContainer.getPaddingTop();

        ViewCompat.setOnApplyWindowInsetsListener(scrollView, (v, insets) -> {
            boolean imeVisible = insets.isVisible(WindowInsetsCompat.Type.ime());
            int top = imeVisible ? 0 : initialTopPad;
            if (cardContainer.getPaddingTop() != top) {
                cardContainer.setPadding(
                        cardContainer.getPaddingLeft(),
                        top,
                        cardContainer.getPaddingRight(),
                        cardContainer.getPaddingBottom()
                );
            }
            return insets;
        });
    }

    private void addUser() {
        String firstName = firstNameEdit.getText().toString().trim();
        String userName = userNameEdit.getText().toString().trim();
        String userPass = userPassEdit.getText().toString().trim();
        String tel = telEdit.getText().toString().trim();
        String userMail = userMailEdit.getText().toString().trim();

        if (TextUtils.isEmpty(firstName) || TextUtils.isEmpty(userName) ||
                TextUtils.isEmpty(userPass) || TextUtils.isEmpty(tel) || TextUtils.isEmpty(userMail)) {
            Toast.makeText(this, "Все поля должны быть заполнены", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(userMail).matches()) {
            Toast.makeText(this, "Неверный email", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!tel.matches("[0-9+]+")) {
            Toast.makeText(this, "Телефон должен содержать только цифры и +", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            JSONObject newUser = new JSONObject();
            newUser.put("firstName", firstName);
            newUser.put("userName", userName);
            newUser.put("userCreated", new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date()));
            newUser.put("userPass", userPass);
            newUser.put("tel", tel);
            newUser.put("userMail", userMail);

            // локально
            localUsers.put(newUser);
            saveToLocalFile();

            // на сервер
            syncWithServer(newUser);

            Toast.makeText(this, "Пользователь добавлен", Toast.LENGTH_SHORT).show();

            // Очистка
            firstNameEdit.setText("");
            userNameEdit.setText("");
            userPassEdit.setText("");
            telEdit.setText("");
            userMailEdit.setText("");

            setResult(RESULT_OK);
            finish();

        } catch (Exception e) {
            Log.e("GreatUser", "Ошибка создания пользователя: " + e.getMessage());
        }
    }

    private void loadLocalUsers() {
        try {
            File file = new File(getFilesDir(), LOCAL_FILE);
            if (!file.exists()) return;
            FileInputStream fis = new FileInputStream(file);
            String json = readFile(fis);
            fis.close();
            localUsers = new JSONArray(json);
        } catch (Exception e) {
            Log.e("GreatUser", "Ошибка чтения локального файла: " + e.getMessage());
        }
    }

    private void saveToLocalFile() {
        try {
            File file = new File(getFilesDir(), LOCAL_FILE);
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(localUsers.toString().getBytes(StandardCharsets.UTF_8));
            fos.close();
        } catch (IOException e) {
            Log.e("GreatUser", "Ошибка сохранения локального файла: " + e.getMessage());
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

    private void syncWithServer(JSONObject newUser) {
        RequestBody body = RequestBody.create(newUser.toString(), MediaType.parse("application/json"));
        Request request = new Request.Builder()
                .url(SERVER_URL)
                .addHeader("Authorization", "Bearer " + TOKEN)
                .post(body)
                .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("GreatUser", "Ошибка отправки на сервер: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) {
                Log.i("GreatUser", "Сервер ответил: " + response.code());
            }
        });
    }
}
