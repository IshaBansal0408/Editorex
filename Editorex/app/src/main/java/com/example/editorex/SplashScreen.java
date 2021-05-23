package com.example.editorex;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.TaskStackBuilder;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;

import com.airbnb.lottie.LottieAnimationView;

import java.util.Timer;
import java.util.TimerTask;

public class SplashScreen extends AppCompatActivity {

    ImageView logo_view,bg_view;
    LottieAnimationView lottie_view;
    Timer timer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);

        logo_view = findViewById(R.id.logo_img);
        bg_view = findViewById(R.id.bg);
        lottie_view = findViewById(R.id.splash_lottie);
        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                Intent intent = new Intent(SplashScreen.this,ImageLoadScreen.class);
                startActivity(intent);

            }
        },4000);
        bg_view.animate().translationY(-4000).setDuration(1000).setStartDelay(4000);
        logo_view.animate().translationY(4000).setDuration(1000).setStartDelay(4000);
        lottie_view.animate().translationY(1400).setDuration(1000).setStartDelay(4000);
    }
}