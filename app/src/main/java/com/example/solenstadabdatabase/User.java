package com.example.solenstadabdatabase; // Пакет приложения: определяет логическое пространство имён, где находится этот класс

import android.content.Context; // Импорт контекста — нужен для доступа к ресурсам, файлам, SharedPreferences
import android.content.Intent; // Импорт Intent — для запуска других Activity
import android.content.SharedPreferences; // Импорт SharedPreferences — для локального хранения простых настроек (ключ-значение)
import android.graphics.Color; // Импорт Color — для установки цветов элементов UI
import android.graphics.Rect; // Импорт Rect — для вычисления видимой области экрана (детекция клавиатуры)
import android.os.Bundle; // Импорт Bundle — контейнер для передачи состояния между Activity
import android.text.Editable; // Импорт Editable — интерфейс редактируемого текста для TextWatcher
import android.text.TextWatcher; // Импорт TextWatcher — слушатель изменений текста в EditText
import android.util.Log; // Импорт Log — для логирования ошибок и отладочной информации
import android.widget.*; // Импорт виджетов Android (Button, CheckBox, EditText, LinearLayout, Toast и т.д.)
import androidx.activity.result.ActivityResultLauncher; // Импорт API для получения результата от запущенной Activity (современный способ)
import androidx.activity.result.contract.ActivityResultContracts; // Контракты результатов Activity (StartActivityForResult и др.)
import androidx.appcompat.app.AppCompatActivity; // Базовая Activity с поддержкой Material/Compat
import androidx.constraintlayout.widget.ConstraintLayout; // Импорт ConstraintLayout — корневой контейнер разметки
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout; // Импорт SwipeRefreshLayout — пул‑ту‑рефреш контейнер

import org.json.JSONArray; // Импорт JSONArray — для работы с JSON массивами пользователей
import org.json.JSONObject; // Импорт JSONObject — для работы с JSON объектами

import java.io.*; // Импорт ввода/вывода — File, FileInputStream, FileOutputStream, IOException и пр.
import java.nio.charset.StandardCharsets; // Импорт кодировки UTF‑8 для корректной записи/чтения файлов

import okhttp3.*; // Импорт OkHttp — HTTP клиент для сетевых запросов

public class User extends AppCompatActivity { // Объявление класса Activity "User", расширяющего AppCompatActivity

    private static final String SERVER_URL = "http://heiserver.ddns.net/D/server/Heis_nexus/SolenStadAB/data/user.json"; // Адрес JSON на сервере
    private static final String TOKEN = "HEISAdmin"; // Токен авторизации (в заголовке Authorization: Bearer ...)
    private static final String LOCAL_FILE = "user.json"; // Имя локального файла для кеша/слияния пользователей
    private static final String KEY_REMEMBER = "remember_user"; // Ключ для SharedPreferences — флаг "Запомнить меня"

    private OkHttpClient httpClient = new OkHttpClient(); // Экземпляр HTTP‑клиента OkHttp для выполнения сетевых запросов
    private LinearLayout usersContainer; // Контейнер, куда будут динамически добавляться кнопки пользователей
    private Button addUserButton; // Кнопка "Добавить пользователя"
    private CheckBox rememberCheckBox; // Чекбокс "Запомнить меня"
    private EditText searchEdit; // Поле поиска по пользователям
    private SharedPreferences sharedPreferences; // Хранилище простых настроек приложения
    private SwipeRefreshLayout swipeRefreshLayout; // Контейнер для жеста "потяни, чтобы обновить"

    private JSONArray localUsers = new JSONArray(); // JSON‑массив локальных пользователей (кеш/объединённый список)
    private JSONArray serverUsers = new JSONArray(); // JSON‑массив пользователей, загруженных с сервера

    private ActivityResultLauncher<Intent> addUserLauncher; // Лаунчер для запуска Activity создания пользователя и получения результата

    @Override // Переопределение метода жизненного цикла Activity
    protected void onCreate(Bundle savedInstanceState) { // onCreate — точка входа при создании Activity
        super.onCreate(savedInstanceState); // Вызов базовой реализации — обязателен
        setContentView(R.layout.user); // Привязка к XML‑разметке res/layout/user.xml

        ConstraintLayout mainLayout = findViewById(R.id.main); // Получаем корневой ConstraintLayout по id
        LinearLayout topBlock = findViewById(R.id.top_block); // Находим верхний блок (EditText + CheckBox) по id

        // --- Подъем блока при появлении клавиатуры: меняем topMargin с 310dp на 30dp ---
        mainLayout.getViewTreeObserver().addOnGlobalLayoutListener(() -> { // Подписываемся на глобальные изменения разметки (для детекции клавиатуры)
            Rect r = new Rect(); // Прямоугольник для хранения видимой области окна
            mainLayout.getWindowVisibleDisplayFrame(r); // Заполняем r текущей видимой областью (без панели клавиатуры)
            int screenHeight = mainLayout.getRootView().getHeight(); // Общая высота корневого окна (включая всю область)
            int keypadHeight = screenHeight - r.bottom; // Высота "клавиатуры" = невидимая часть снизу

            boolean keyboardVisible = keypadHeight > screenHeight * 0.15f; // Эвристика: если перекрыто >15% высоты, значит клавиатура показана

            ConstraintLayout.LayoutParams lp = (ConstraintLayout.LayoutParams) topBlock.getLayoutParams(); // Получаем LayoutParams верхнего блока
            int targetMargin = dp(keyboardVisible ? 30 : 310); // Целевой верхний отступ: 30dp при показанной клавиатуре, 310dp — иначе
            if (lp.topMargin != targetMargin) { // Проверяем, чтобы не переразмечать без надобности
                lp.topMargin = targetMargin; // Применяем новый верхний отступ
                topBlock.setLayoutParams(lp); // Назначаем обновлённые параметры блоку
                mainLayout.requestLayout(); // Просим систему пересчитать и перерисовать разметку
            }
        }); // Конец слушателя глобальной разметки
        // -------------------------------------------------------------------------------

        usersContainer = findViewById(R.id.users_container); // Контейнер для списка (кнопок) пользователей внутри ScrollView
        addUserButton = findViewById(R.id.my_button); // Кнопка "Добавить пользователя"
        rememberCheckBox = findViewById(R.id.my_checkbox); // Чекбокс "Запомнить меня"
        searchEdit = findViewById(R.id.editText); // Поле ввода для поиска пользователя
        swipeRefreshLayout = findViewById(R.id.swipe_refresh); // Контейнер pull‑to‑refresh

        // SharedPreferences
        sharedPreferences = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE); // Получаем объект настроек с именем "UserPrefs" в приватном режиме
        boolean remember = sharedPreferences.getBoolean(KEY_REMEMBER, false); // Читаем флаг: был ли установлен "Запомнить меня"
        if (remember) { // Если флаг установлен
            Toast.makeText(this, "Пользователь запомнен. Переход к списку проектов...", Toast.LENGTH_LONG).show(); // Показываем информационный Toast
            return; // Ранний выход из onCreate: в текущей логике Activity больше не инициализируем (предположительно дальше пойдёт навигация)
        }

        loadUsersFromLocal(); // Загружаем локальный JSON (если есть) в localUsers
        showUsers(localUsers); // Отрисовываем список пользователей из локальных данных (до прихода сервера)
        loadUsersFromServer(); // Асинхронно запрашиваем список пользователей с сервера и сольём с локальными

        swipeRefreshLayout.setOnRefreshListener(() -> { // Устанавливаем обработчик жеста обновления (pull‑to‑refresh)
            loadUsersFromServer(); // При обновлении — снова тянем данные с сервера
            swipeRefreshLayout.setRefreshing(false); // Сразу выключаем индикатор (UI), загрузка продолжается в фоне
        });

        addUserLauncher = registerForActivityResult( // Регистрируем лаунчер для запуска Activity и обработки результата
                new ActivityResultContracts.StartActivityForResult(), // Контракт: запуск Activity с ожиданием результата
                result -> { // Колбэк, который вызовется после закрытия запущенной Activity
                    if (result.getResultCode() == RESULT_OK) { // Если результат успешный
                        loadUsersFromLocal(); // Перечитываем локальный список (в нём мог появиться новый пользователь)
                        showUsers(localUsers); // Обновляем отображение
                    }
                }); // Завершение регистрации лаунчера

        addUserButton.setOnClickListener(v -> { // Обработчик нажатия на кнопку "Добавить пользователя"
            Intent intent = new Intent(User.this, GreatUserActivity.class); // Создаём Intent для запуска экрана создания пользователя
            addUserLauncher.launch(intent); // Запускаем Activity через лаунчер, чтобы получить результат
        });

        rememberCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> // Слушатель изменения состояния чекбокса
                sharedPreferences.edit().putBoolean(KEY_REMEMBER, isChecked).apply()); // Сохраняем флаг в SharedPreferences немедленно

        searchEdit.addTextChangedListener(new TextWatcher() { // Добавляем слушатель на изменения текста в поле поиска
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {} // Хук до изменения — тут не используется
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { filterUsers(s.toString()); } // При каждом изменении фильтруем список
            @Override public void afterTextChanged(Editable s) {} // Хук после изменения — не используется
        }); // Конец добавления TextWatcher
    } // Конец onCreate

    // dp -> px
    private int dp(int value) { // Утилитный метод: переводит значения в dp в пиксели с учётом плотности экрана
        float density = getResources().getDisplayMetrics().density; // Получаем коэффициент плотности (dpi/160)
        return Math.round(value * density); // Возвращаем округлённое значение пикселей
    } // Конец метода dp

    private void loadUsersFromServer() { // Метод: загрузка массива пользователей с сервера
        Request request = new Request.Builder() // Строим HTTP‑запрос через OkHttp Request.Builder
                .url(SERVER_URL) // Указываем URL ресурса с JSON
                .addHeader("Authorization", "Bearer " + TOKEN) // Добавляем заголовок авторизации (Bearer токен)
                .build(); // Строим неизменяемый объект запроса

        httpClient.newCall(request).enqueue(new Callback() { // Делаем асинхронный HTTP‑вызов; ответ придёт в этот Callback
            @Override
            public void onFailure(Call call, IOException e) { Log.e("User", "Ошибка соединения: " + e.getMessage()); } // Логируем сетевую ошибку
            @Override
            public void onResponse(Call call, Response response) throws IOException { // Обработка успешного (или неуспешного) HTTP‑ответа
                String bodyStr = response.body() != null ? response.body().string() : ""; // Читаем тело ответа как строку (или пустую строку)
                if (response.isSuccessful()) { // Если код ответа в диапазоне 200..299
                    try {
                        serverUsers = new JSONArray(bodyStr); // Парсим тело ответа в JSONArray — список пользователей с сервера
                        mergeUsers(); // Сливаем серверный список с локальным (без дубликатов по userName)
                        runOnUiThread(() -> showUsers(localUsers)); // Обновляем UI в главном потоке, показывая объединённый список
                    } catch (Exception e) { // Перехватываем любые ошибки парсинга JSON
                        runOnUiThread(() -> Toast.makeText(User.this, "Ошибка парсинга JSON", Toast.LENGTH_SHORT).show()); // Сообщаем пользователю о проблеме
                    }
                } else { // Если сервер вернул неуспешный статус (например, 404/500)
                    runOnUiThread(() -> Toast.makeText(User.this, "Ошибка сервера: " + response.code(), Toast.LENGTH_SHORT).show()); // Показываем код ошибки
                }
            }
        }); // Конец enqueue
    } // Конец loadUsersFromServer

    private void loadUsersFromLocal() { // Метод: загрузка локального файла пользователей в localUsers
        try {
            File file = new File(getFilesDir(), LOCAL_FILE); // Формируем путь к файлу внутри внутреннего хранилища приложения
            if (!file.exists()) return; // Если файла нет — просто выходим (оставляем localUsers как пустой JSONArray)
            FileInputStream fis = new FileInputStream(file); // Открываем поток чтения из файла
            String json = readFile(fis); // Читаем весь файл в строку с помощью вспомогательного метода
            fis.close(); // Закрываем поток чтения
            localUsers = new JSONArray(json); // Парсим строку как JSONArray и сохраняем в localUsers
        } catch (Exception e) { Log.e("User", "Ошибка чтения локального файла: " + e.getMessage()); } // Логируем ошибки чтения/парсинга
    } // Конец loadUsersFromLocal

    private void mergeUsers() { // Метод: объединение serverUsers в localUsers без дублирования по полю userName
        loadUsersFromLocal(); // На всякий случай перечитываем локальные данные (актуализируем перед слиянием)
        for (int i = 0; i < serverUsers.length(); i++) { // Идём по всем пользователям, полученным с сервера
            JSONObject serverUser = serverUsers.optJSONObject(i); // Берём i‑й объект пользователя (может вернуть null, поэтому opt)
            boolean exists = false; // Флаг: найден ли пользователь с таким же userName в localUsers
            for (int j = 0; j < localUsers.length(); j++) { // Перебираем локальных пользователей
                if (localUsers.optJSONObject(j).optString("userName") // Берём userName локального пользователя (пустая строка, если нет)
                        .equals(serverUser.optString("userName"))) { // Сравниваем с userName серверного пользователя
                    exists = true; break; // Найден дубликат — помечаем и выходим из внутреннего цикла
                }
            }
            if (!exists) localUsers.put(serverUser); // Если такого имени не было — добавляем серверного пользователя в локальный массив
        }
        saveToLocalFile(); // Сохраняем объединённый список обратно в локальный файл
    } // Конец mergeUsers

    private void saveToLocalFile() { // Метод: сохранение текущего localUsers в файл JSON
        try {
            File file = new File(getFilesDir(), LOCAL_FILE); // Путь к локальному файлу во внутреннем хранилище
            FileOutputStream fos = new FileOutputStream(file); // Открываем поток записи
            fos.write(localUsers.toString().getBytes(StandardCharsets.UTF_8)); // Пишем строковое представление JSONArray в UTF‑8
            fos.close(); // Закрываем поток записи
        } catch (IOException e) { Log.e("User", "Ошибка сохранения локального файла: " + e.getMessage()); } // Логируем сбои записи
    } // Конец saveToLocalFile

    private String readFile(InputStream is) throws IOException { // Вспомогательный метод: читает весь InputStream в строку UTF‑8
        ByteArrayOutputStream buffer = new ByteArrayOutputStream(); // Буфер, куда накапливаем содержимое
        byte[] data = new byte[1024]; int nRead; // Временный буфер на 1024 байта и счётчик прочитанного
        while ((nRead = is.read(data, 0, data.length)) != -1) buffer.write(data, 0, nRead); // Читаем по кускам до конца потока
        return buffer.toString(StandardCharsets.UTF_8.name()); // Преобразуем накопленные байты в строку UTF‑8 и возвращаем
    } // Конец readFile

    private void showUsers(JSONArray users) { // Метод: рисует список пользователей как вертикальные кнопки в контейнере
        usersContainer.removeAllViews(); // Очищаем контейнер от старых представлений
        int marginInDp = 10; // Отступ между кнопками в dp
        int marginInPx = (int) (marginInDp * getResources().getDisplayMetrics().density + 0.5f); // Конвертируем dp в px (ручной способ с добавлением 0.5 для округления)

        for (int i = 0; i < users.length(); i++) { // Итерируемся по массиву пользователей
            String name = users.optJSONObject(i).optString("userName", "Без имени"); // Достаём имя пользователя (или "Без имени", если поля нет)
            Button btn = new Button(this); // Создаём кнопку программно
            btn.setText(name); // Ставим текст кнопки = имя пользователя
            btn.setBackgroundResource(R.drawable.button_selector); // Применяем селектор/фон из ресурсов (анимации/состояния)
            btn.setTextColor(Color.parseColor("#c0b27b")); // Устанавливаем цвет текста в соответствии с дизайном

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams( // Создаём параметры разметки для кнопки внутри LinearLayout
                    LinearLayout.LayoutParams.MATCH_PARENT, // Ширина: на всю ширину контейнера
                    LinearLayout.LayoutParams.WRAP_CONTENT // Высота: по содержимому
            ); // Конец создания LayoutParams
            params.setMargins(0, marginInPx, 0, marginInPx); // Устанавливаем верхний и нижний отступы между кнопками
            btn.setLayoutParams(params); // Применяем параметры к кнопке

            btn.setOnClickListener(v -> // Обработчик клика по кнопке пользователя
                    Toast.makeText(this, "Выбран: " + name, Toast.LENGTH_SHORT).show()); // Показываем Toast с именем выбранного пользователя

            usersContainer.addView(btn); // Добавляем кнопку в контейнер
        }
    } // Конец showUsers

    private void filterUsers(String query) { // Метод: фильтрует localUsers по подстроке в userName и отображает результат
        JSONArray filtered = new JSONArray(); // Создаём новый массив для отфильтрованных пользователей
        for (int i = 0; i < localUsers.length(); i++) { // Идём по всем локальным пользователям
            JSONObject user = localUsers.optJSONObject(i); // Получаем i‑го пользователя как JSONObject (может быть null)
            if (user != null && user.optString("userName", "").toLowerCase().contains(query.toLowerCase())) { // Если имя содержит запрос (без учёта регистра)
                filtered.put(user); // Добавляем пользователя в результирующий массив
            }
        }
        showUsers(filtered); // Показываем отфильтрованный список в UI
    } // Конец filterUsers
} // Конец класса User
