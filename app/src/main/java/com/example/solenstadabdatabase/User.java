package com.example.solenstadabdatabase;
import android.view.WindowManager;
import android.os.Bundle;
import android.view.View;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class User extends AppCompatActivity {
    private Button myButton;
    private EditText editText;
    private CheckBox myCheckBox;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN |
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        );
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
        );
        setContentView(R.layout.user);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Находим элементы по id
        myButton = findViewById(R.id.my_button);
        editText = findViewById(R.id.editText);
        myCheckBox = findViewById(R.id.my_checkbox);

        myButton.setOnClickListener(view -> {
            String text = editText.getText().toString();
            boolean checked = myCheckBox.isChecked();

            String message = "Текст: " + text + "\n" + (checked ? "Чекбокс отмечен" : "Чекбокс не отмечен");
            Toast.makeText(User.this, message, Toast.LENGTH_SHORT).show();
        });
    }

}