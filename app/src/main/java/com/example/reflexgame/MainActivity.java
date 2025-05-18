package com.example.reflexgame;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresPermission;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.example.reflexgame.models.Result;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import android.graphics.Bitmap;
import android.content.Intent;
import android.provider.MediaStore;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private TextView welcomeText;
    private Button logoutBtn, deleteRecordsBtn, deleteBestRecordBtn;

    private TextView topResults, recentResults, bestRecordText;
    private ImageView profileImage;
    private Button takePhotoBtn;



    private ActivityResultLauncher<Intent> cameraLauncher;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    "game_notify_channel",
                    "Reflex játék értesítések",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.setDescription("Értesítések a játék eseményeiről");
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 200);
            }
        }


        auth = FirebaseAuth.getInstance();
        welcomeText = findViewById(R.id.welcomeText);
        logoutBtn = findViewById(R.id.logoutBtn);
        deleteRecordsBtn = findViewById(R.id.deleteRecordsBtn);
        bestRecordText = findViewById(R.id.bestRecordText);
        Button startGameBtn = findViewById(R.id.startGameBtn);
        deleteBestRecordBtn = findViewById(R.id.deleteBestRecordBtn);
        deleteBestRecordBtn.setOnClickListener(v -> confirmBestRecordDeletion());
        profileImage = findViewById(R.id.profileImage);
        takePhotoBtn = findViewById(R.id.takePhotoBtn);



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

        startGameBtn.setOnClickListener(v -> {
            startActivity(new Intent(this, GameActivity.class));
        });

        deleteRecordsBtn.setOnClickListener(v -> {
            new android.app.AlertDialog.Builder(MainActivity.this)
                    .setTitle("Biztosan törlöd?")
                    .setMessage("Ez az összes eddigi eredményt (kivéve a legjobb idődet) eltávolítja.")
                    .setPositiveButton("Igen", (dialog, which) -> deleteAllResults())
                    .setNegativeButton("Mégse", null)
                    .show();
        });

        takePhotoBtn.setOnClickListener(v -> openCamera());


        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Bundle extras = result.getData().getExtras();
                        Bitmap photo = (Bitmap) extras.get("data");
                        profileImage.setImageBitmap(photo);
                    }
                }
        );


        topResults = findViewById(R.id.topResults);
        recentResults = findViewById(R.id.recentResults);

        loadResults();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadResults();
    }

    private void loadResults() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser != null) {
            String email = currentUser.getEmail();

            db.collection("users").document(currentUser.getUid())
                    .get()
                    .addOnSuccessListener(doc -> {
                        Long best = doc.getLong("bestReactionTime");
                        if (best != null) {
                            bestRecordText.setText("Legjobb idő: " + best + " ms");
                        } else {
                            bestRecordText.setText("Legjobb idő: -");
                        }
                    })
                    .addOnFailureListener(e -> {
                        bestRecordText.setText("Legjobb idő: -");
                    });

            // Top 3
            db.collection("results")
                    .whereEqualTo("userEmail", email)
                    .orderBy("reactionTimeMs", Query.Direction.ASCENDING)
                    .limit(3)
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        StringBuilder topBuilder = new StringBuilder("Top 3 eredmény:\n");
                        if (queryDocumentSnapshots.isEmpty()) {
                            topBuilder.append("Nincs adat.");
                        } else {
                            for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                                Long time = doc.getLong("reactionTimeMs");
                                if (time != null) {
                                    topBuilder.append("- ").append(time).append(" ms\n");
                                }
                            }
                        }
                        topResults.setText(topBuilder.toString());
                    });

            // Legutóbbi 5
            db.collection("results")
                    .whereEqualTo("userEmail", email)
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .limit(5)
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        StringBuilder recentBuilder = new StringBuilder("Legutóbbi 5:\n");
                        if (queryDocumentSnapshots.isEmpty()) {
                            recentBuilder.append("Nincs adat.");
                        } else {
                            for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                                Long time = doc.getLong("reactionTimeMs");
                                if (time != null) {
                                    recentBuilder.append("- ").append(time).append(" ms\n");
                                }
                            }
                        }
                        recentResults.setText(recentBuilder.toString());
                    });
        }
    }


    private void deleteAllResults() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user != null) {
            String email = user.getEmail();

            db.collection("results")
                    .whereEqualTo("userEmail", email)
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        if (queryDocumentSnapshots.isEmpty()) {
                            Toast.makeText(this, "Nincs törlendő eredmény.", Toast.LENGTH_SHORT).show();
                        } else {
                            for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                                db.collection("results").document(doc.getId()).delete();
                            }
                            Toast.makeText(this, "Összes rekord törölve!", Toast.LENGTH_SHORT).show();
                            loadResults(); // újratöltés, hogy kiürüljön a lista
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Hiba történt: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        }
    }


    private void confirmBestRecordDeletion() {
        new android.app.AlertDialog.Builder(MainActivity.this)
                .setTitle("Biztosan törlöd a legjobb időt?")
                .setMessage("Ez nem törli az eredményeket, csak a legjobb idő mezőt.")
                .setPositiveButton("Törlés", (dialog, which) -> deleteBestRecord())
                .setNegativeButton("Mégse", null)
                .show();
    }

    private void deleteBestRecord() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            db.collection("users").document(user.getUid())
                    .update("bestReactionTime", null)
                    .addOnSuccessListener(unused -> {
                        Toast.makeText(this, "Legjobb idő törölve.", Toast.LENGTH_SHORT).show();
                        bestRecordText.setText("Legjobb idő: -");
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Hiba: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void openCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraLauncher.launch(intent);
    }

}
