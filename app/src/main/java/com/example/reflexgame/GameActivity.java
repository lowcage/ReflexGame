package com.example.reflexgame;

import android.Manifest;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresPermission;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.example.reflexgame.models.Result;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;

public class GameActivity extends AppCompatActivity {

    private ConstraintLayout gameLayout;
    private Button startBtn;
    private Button backBtn;
    private TextView resultText;

    private boolean waiting = false;
    private boolean canTap = false;
    private long startTime = 0;
    private long lastReactionTime = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        gameLayout = findViewById(R.id.gameLayout);
        startBtn = findViewById(R.id.startButton);
        resultText = findViewById(R.id.resultText);
        startBtn.setOnClickListener(v -> startGame());
        backBtn = findViewById(R.id.backToProfileBtn);


        gameLayout.setOnClickListener(v -> {
            if (waiting) {
                resultText.setText("Túl korán!");
                resetGame();
            } else if (canTap) {
                long reactionTime = System.currentTimeMillis() - startTime;
                resultText.setText("Reakcióidő: " + reactionTime + " ms");
                lastReactionTime = reactionTime;
                saveResult(reactionTime);
                resetGame();
            }
        });

        backBtn.setOnClickListener(v -> {
            finish(); // visszalépés a MainActivity-re
        });
    }

    private void startGame() {
        // Alaphelyzet
        startBtn.setVisibility(View.GONE);
        backBtn.setVisibility(View.GONE);
        resultText.setText("Várj...");
        gameLayout.setBackgroundColor(getResources().getColor(android.R.color.holo_red_dark));

        waiting = true;
        canTap = false;

        int delay = 2000 + (int)(Math.random() * 3000);

        new Handler().postDelayed(() -> {
            waiting = false;
            canTap = true;
            startTime = System.currentTimeMillis();
            resultText.setText("Most!");
            gameLayout.setBackgroundColor(getResources().getColor(android.R.color.holo_green_dark));
        }, delay);
    }

    private void resetGame() {
        waiting = false;
        canTap = false;
        gameLayout.setBackgroundColor(getResources().getColor(android.R.color.white));

        new Handler().postDelayed(() -> {
            startBtn.setVisibility(View.VISIBLE);
            backBtn.setVisibility(View.VISIBLE);
            startBtn.setText("Újra");
            if (lastReactionTime > 0) {
                resultText.setText("Előző idő: " + lastReactionTime + " ms");
            } else {
                resultText.setText("Kezdés");
            }

        }, 1000);
    }

    private void saveResult(long reactionTimeMs) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user != null) {
            Result result = new Result(user.getEmail(), reactionTimeMs, Timestamp.now());

            db.collection("results")
                    .add(result)
                    .addOnSuccessListener(documentReference -> {
                        Toast.makeText(this, "Eredmény elmentve!", Toast.LENGTH_SHORT).show();
                        checkAndUpdateBestRecord(reactionTimeMs, user.getUid());
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Hiba: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        }
    }

    @RequiresPermission(Manifest.permission.SCHEDULE_EXACT_ALARM)
    private void scheduleReminderNotification() {
        Intent intent = new Intent(this, ReminderReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        if (alarmManager != null) {
            long triggerTime = System.currentTimeMillis() + 5000; // 5 másodperc
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
        }
    }


    private void checkAndUpdateBestRecord(long reactionTimeMs, String uid) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("users").document(uid)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    Long currentBest = documentSnapshot.getLong("bestReactionTime");
                    if (currentBest == null || reactionTimeMs < currentBest) {
                        Toast.makeText(this, "🥇 Új rekord!", Toast.LENGTH_LONG).show(); // ← ide előre
                        db.collection("users").document(uid)
                                .update("bestReactionTime", reactionTimeMs)
                                .addOnFailureListener(e -> {
                                    db.collection("users").document(uid)
                                            .set(new HashMap<String, Object>() {{
                                                put("bestReactionTime", reactionTimeMs);
                                            }});
                                });
                    }

                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Nem sikerült lekérni a rekordot: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }


}
