package com.metamask.android

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.TextView
import androidx.lifecycle.LifecycleObserver
import com.metamask.android.databinding.ActivityMainBinding
import io.metamask.androidsdk.*

class MainActivity : AppCompatActivity(), LifecycleObserver {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding

    companion object {
        const val TAG = "MM_ANDROID_SDK"
    }

    private lateinit var connectButton: Button
    private lateinit var connectResultLabel: TextView

    private lateinit var signButton: Button
    private lateinit var signResultLabel: TextView

    private lateinit var sendButton: Button
    private lateinit var sendResultLabel: TextView

    private lateinit var exampleDapp: ExampleDapp

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Logger.log("app: onCreate")

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        val navController = findNavController(R.id.nav_host_fragment_content_main)
        appBarConfiguration = AppBarConfiguration(navController.graph)
        setupActionBarWithNavController(navController, appBarConfiguration)

        connectButton = findViewById(R.id.connectButton)
        connectResultLabel = findViewById(R.id.connectText)
        connectButton.setOnClickListener {
            exampleDapp.connect() { result ->
                connectResultLabel.text = result.toString()
            }
        }

        signButton = findViewById(R.id.signButton)
        signResultLabel = findViewById(R.id.signText)
        signButton.setOnClickListener {
            exampleDapp.signMessage() { result ->
                signResultLabel.text = result.toString()
            }
        }

        sendButton = findViewById(R.id.sendButton)
        sendResultLabel = findViewById(R.id.sendText)
        sendButton.setOnClickListener {
            exampleDapp.sendTransaction() { result ->
                sendResultLabel.text = result.toString()
            }
        }

        lifecycle.addObserver(this)
        exampleDapp = ExampleDapp(this, lifecycle)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration)
                || super.onSupportNavigateUp()
    }

    override fun onStart() {
        super.onStart()
        Logger.log("app: Started SDK activity")
    }

    override fun onDestroy() {
        super.onDestroy()
        Logger.log("app: Destroyed SDK activity")
    }
}