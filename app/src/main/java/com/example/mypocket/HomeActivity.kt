package com.example.mypocket  // <- must match your app package

import android.os.Build
import android.os.Bundle
import android.view.WindowInsets
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.example.mypocket.data.AppDatabase
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat


class HomeActivity : AppCompatActivity() {

    private lateinit var bottomNavigation: BottomNavigationView

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleNavigation(intent)
    }

    private fun handleNavigation(intent: Intent?) {
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.fragmentContainer) as androidx.navigation.fragment.NavHostFragment
        val navController = navHostFragment.navController

        when (intent?.getStringExtra("openDestination")) {
            "accountSettingsFragment" -> {
                navController.navigate(R.id.accountSettingsFragment)
            }

            "categorySettingsFragment" -> {
                val bundle = Bundle().apply {
                    putString("CATEGORY_TYPE", intent.getStringExtra("CATEGORY_TYPE"))
                }
                navController.navigate(R.id.categorySettingsFragment, bundle)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        // Check if the Android version is 11 (API level 30) or higher
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Get the WindowInsetsController and ensure the status bar is visible
            val insetsController = window.insetsController
            insetsController?.show(WindowInsets.Type.statusBars())
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    1001
                )
            }
        }


        // Initialize DB + insert default data
        AppDatabase.prepopulateDefaults(this)

        bottomNavigation = findViewById(R.id.bottomNavigation)

        // Get NavController from NavHostFragment
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.fragmentContainer) as androidx.navigation.fragment.NavHostFragment
        val navController = navHostFragment.navController

        handleNavigation(intent)

        // Connect BottomNavigationView to NavController
        // R.id from res/navigation/nav_graph.xml
        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_calendar -> navController.navigate(R.id.calendarFragment)
                R.id.nav_wallet -> navController.navigate(R.id.walletFragment)
                R.id.nav_settings -> navController.navigate(R.id.settingsFragment)
                else -> false
            }
            true
        }
    }
}