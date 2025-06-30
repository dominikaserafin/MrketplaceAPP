package com.example.marketplace

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.app.Activity

/**
 * Main launcher activity that shows splash screen and redirects to login
 */
class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }, 1000)
    }
}