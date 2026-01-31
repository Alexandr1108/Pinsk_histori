package com.example.pinsk

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val splashRoot = findViewById<View>(R.id.splash_root)

        splashRoot.setOnClickListener {
            val intent = Intent(this, StartActivity::class.java)
            startActivity(intent)
            finish()

            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)

            finish()
        }
    }
}