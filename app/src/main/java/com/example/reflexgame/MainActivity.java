package com.example.reflexgame;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private TextView welcomeText;
    private Button logoutBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        auth = FirebaseAuth.getInstance();
        welcomeText = findViewById(R.id.welcomeText);
        logoutBtn = findViewById(R.id.logoutBtn);

        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            welcomeText.setText("Bejelentkezve: " + user.getEmail());
        } else {
            welcomeText.setText("Nincs bejelentkezett felhasználó.");
        }

        logoutBtn.setOnClickListener(v -> {
            auth.signOut();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
    }
}
