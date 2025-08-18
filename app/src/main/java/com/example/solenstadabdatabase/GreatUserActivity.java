package com.example.solenstadabdatabase;

import android.os.Bundle;
import android.text.InputFilter;
import android.text.TextUtils;
import android.util.TypedValue;
import android.util.Log;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
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
import java.io.IOException;

public class GreatUserActivity extends AppCompatActivity {

    private static final String SERVER_URL =
            "http://heiserver.ddns.net/D/server/Heis_nexus/SolenStadAB/data/user.json";
    private static final String TOKEN = "HEISAdmin";
    private static final String LOCAL_FILE = "user.json";

    private EditText firstNameEdit, userNameEdit, userPassEdit, telEdit, userMailEdit;
    private Button addButton;
    private ScrollView scrollView;
    private LinearLayout cardContainer;

    private OkHttpClient httpClient = new OkHttpClient();
    private JSONArray localUsers = new JSONArray();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_great_user);

        // Инициализация элементов
        scrollView = findViewById(R.id.scrollView);
        cardContainer = findViewById(R.id.cardContainer);
        firstNameEdit = findViewById(R.id.firstNameEdit);
        userNameEdit = findViewById(R.id.userNameEdit);
        userPassEdit = findViewById(R.id.userPassEdit);
        telEdit = findViewById(R.id.telEdit);
        userMailEdit = findViewById(R.id.userMailEdit);
        addButton = findViewById(R.id.addUserButton);

        // Ограничение длины
        userNameEdit.setFilters(new InputFilter[]{new InputFilter.LengthFilter(20)});
        userPassEdit.setFilters(new InputFilter[]{new InputFilter.LengthFilter(20)});
        telEdit.setFilters(new InputFilter[]{new InputFilter.LengthFilter(15)});

        // Загрузка локальных пользователей
        loadLocalUsers();

        // Кнопка "Добавить пользователя"
        addButton.setOnClickListener(v -> addUser());

        // Верхний отступ 30dp при появлении клавиатуры
        final int topPaddingPx = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 30, getResources().getDisplayMetrics());

        scrollView.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            int[] location = new int[2];
            firstNameEdit.getLocationOnScreen(location);
            int fieldBottom = location[1] + firstNameEdit.getHeight();

            int scrollViewHeight = scrollView.getHeight();
            int screenHeight = scrollView.getRootView().getHeight();
            int keyboardHeight = screenHeight - scrollViewHeight;

            if (keyboardHeight > screenHeight * 0.15) { // клавиатура видна
                scrollView.smoothScrollTo(0, fieldBottom - topPaddingPx);
            }
        });

        // Настройка перехода по Enter между полями
        setupFieldNavigation();
    }

    private void setupFieldNavigation() {
        firstNameEdit.setSingleLine(true);
        firstNameEdit.setImeOptions(EditorInfo.IME_ACTION_NEXT);
        firstNameEdit.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_NEXT) {
                userNameEdit.requestFocus();
                return true;
            }
            return false;
        });

        userNameEdit.setSingleLine(true);
        userNameEdit.setImeOptions(EditorInfo.IME_ACTION_NEXT);
        userNameEdit.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_NEXT) {
                userPassEdit.requestFocus();
                return true;
            }
            return false;
        });

        userPassEdit.setSingleLine(true);
        userPassEdit.setImeOptions(EditorInfo.IME_ACTION_NEXT);
        userPassEdit.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_NEXT) {
                telEdit.requestFocus();
                return true;
            }
            return false;
        });

        telEdit.setSingleLine(true);
        telEdit.setImeOptions(EditorInfo.IME_ACTION_NEXT);
        telEdit.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_NEXT) {
                userMailEdit.requestFocus();
                return true;
            }
            return false;
        });

        userMailEdit.setSingleLine(true);
        userMailEdit.setImeOptions(EditorInfo.IME_ACTION_DONE);
        userMailEdit.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                addUser();
                return true;
            }
            return false;
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

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(userMail).matches()) {
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

            localUsers.put(newUser);
            saveToLocalFile();
            syncWithServer(newUser);

            Toast.makeText(this, "Пользователь добавлен", Toast.LENGTH_SHORT).show();

            firstNameEdit.setText("");
            userNameEdit.setText("");
            userPassEdit.setText("");
            telEdit.setText("");
            userMailEdit.setText("");

            setResult(RESULT_OK);
            finish();
        } catch (Exception e) {
            e.printStackTrace();
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
            e.printStackTrace();
        }
    }

    private void saveToLocalFile() {
        try {
            File file = new File(getFilesDir(), LOCAL_FILE);
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(localUsers.toString().getBytes(StandardCharsets.UTF_8));
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String readFile(InputStream is) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] data = new byte[1024];
        int nRead;
        while ((nRead = is.read(data)) != -1) {
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
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                Log.i("GreatUser", "Сервер ответил: " + response.code());
            }
        });
    }
}
