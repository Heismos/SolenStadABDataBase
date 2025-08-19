package com.example.solenstadabdatabase;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;

public class ProjectActivity extends AppCompatActivity {

    private LinearLayout projectsContainer;
    private Spinner sortSpinner;
    private JSONArray projects;

    private DrawerLayout drawerLayout;
    private ListView drawerList;
    private ActionBarDrawerToggle drawerToggle;
    private String[] drawerItems = {"Создать materials.json", "Создать projects.json"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_project);

        projectsContainer = findViewById(R.id.projects_container);
        sortSpinner = findViewById(R.id.spinner_sort);

        // Drawer
        drawerLayout = findViewById(R.id.drawer_layout);
        drawerList = findViewById(R.id.left_drawer);
        drawerList.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, drawerItems));

        drawerList.setOnItemClickListener((parent, view, position, id) -> {
            switch (position) {
                case 0:
                    createMaterialsJson();
                    break;
                case 1:
                    createProjectsJson();
                    break;
            }
            drawerLayout.closeDrawers();
        });

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        drawerToggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar,
                R.string.drawer_open, R.string.drawer_close);
        drawerLayout.addDrawerListener(drawerToggle);
        drawerToggle.syncState();

        // Настройка сортировки
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item,
                new String[]{"По дате", "По имени", "По времени"});
        sortSpinner.setAdapter(adapter);

        // Загружаем и показываем проекты
        loadProjects();
        showProjects();
    }

    private void loadProjects() {
        projects = loadJSON("projects.json");
    }

    private JSONArray loadJSON(String fileName) {
        try {
            File file = new File(getFilesDir(), fileName);
            if (!file.exists()) return new JSONArray();
            byte[] buffer = new byte[(int) file.length()];
            FileInputStream fis = new FileInputStream(file);
            fis.read(buffer);
            fis.close();
            return new JSONArray(new String(buffer, StandardCharsets.UTF_8));
        } catch (Exception e) {
            e.printStackTrace();
            return new JSONArray();
        }
    }

    private void showProjects() {
        projectsContainer.removeAllViews();
        for (int i = 0; i < projects.length(); i++) {
            JSONObject proj = projects.optJSONObject(i);
            if (proj == null) continue;

            String name = proj.optString("projectName", "Без названия");
            String date = proj.optString("date", "-");
            String comment = proj.optString("comment", "-");

            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setPadding(20, 20, 20, 20);
            card.setBackgroundResource(R.drawable.edit_gradient_border);

            TextView tv = new TextView(this);
            tv.setText("Проект: " + name + "\nДата: " + date + "\nКомментарий: " + comment);
            tv.setTextColor(0xFFC0B27B);
            tv.setTextSize(16);
            card.addView(tv);

            LinearLayout btns = new LinearLayout(this);
            btns.setOrientation(LinearLayout.HORIZONTAL);

            Button addMatBtn = new Button(this);
            addMatBtn.setText("Добавить материал");
            addMatBtn.setOnClickListener(v ->
                    Toast.makeText(this, "Добавление материала пока не реализовано", Toast.LENGTH_SHORT).show());
            btns.addView(addMatBtn);

            Button delBtn = new Button(this);
            delBtn.setText("Удалить проект");
            delBtn.setOnClickListener(v -> {
                projects.remove(i);
                saveJSON("projects.json", projects);
                showProjects();
            });
            btns.addView(delBtn);

            card.addView(btns);

            projectsContainer.addView(card);
        }
    }

    private void saveJSON(String fileName, JSONArray array) {
        try {
            File file = new File(getFilesDir(), fileName);
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(array.toString(2).getBytes(StandardCharsets.UTF_8));
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Ошибка сохранения " + fileName, Toast.LENGTH_SHORT).show();
        }
    }

    private void createMaterialsJson() {
        try {
            JSONArray materials = new JSONArray();
            materials.put(new JSONObject().put("name", "Розетки").put("unit", "шт"));
            materials.put(new JSONObject().put("name", "Провод").put("unit", "м"));
            materials.put(new JSONObject().put("name", "Кабель").put("unit", "м"));
            materials.put(new JSONObject().put("name", "Автомат").put("unit", "шт"));
            materials.put(new JSONObject().put("name", "Светильник").put("unit", "шт"));
            materials.put(new JSONObject().put("name", "Щит").put("unit", "шт"));

            saveJSON("materials.json", materials);
            Toast.makeText(this, "materials.json создан", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void createProjectsJson() {
        try {
            JSONArray projectsArray = new JSONArray();

            JSONObject p1 = new JSONObject();
            p1.put("projectName", "Стафансторп");
            p1.put("date", "12.09.2025");
            p1.put("comment", "Штробили стены, делали подразетники");
            p1.put("users", new JSONArray("[{\"name\":\"Oleksii\",\"start\":\"07:00\",\"end\":\"13:00\",\"hours\":6},{\"name\":\"Mikolay\",\"start\":\"07:00\",\"end\":\"14:00\",\"hours\":7}]"));
            p1.put("materials", new JSONArray("[{\"name\":\"Розетки\",\"quantity\":26,\"unit\":\"шт\"},{\"name\":\"Провод\",\"quantity\":12,\"unit\":\"м\"}]"));
            projectsArray.put(p1);

            JSONObject p2 = new JSONObject();
            p2.put("projectName", "Лунд");
            p2.put("date", "13.09.2025");
            p2.put("comment", "Протяжка кабеля");
            p2.put("users", new JSONArray("[{\"name\":\"Oleksii\",\"start\":\"07:00\",\"end\":\"09:30\",\"hours\":2.5}]"));
            p2.put("materials", new JSONArray("[{\"name\":\"Кабель\",\"quantity\":50,\"unit\":\"м\"}]"));
            projectsArray.put(p2);

            // Добавь остальные проекты по аналогии...

            saveJSON("projects.json", projectsArray);
            loadProjects();
            showProjects();
            Toast.makeText(this, "projects.json создан", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
