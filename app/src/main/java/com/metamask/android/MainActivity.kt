package com.metamask.android

import android.content.Intent
import android.os.Bundle
import android.util.Log
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import androidx.lifecycle.LifecycleObserver
import com.metamask.android.databinding.ActivityMainBinding
import com.metamask.android.sdk.CommunicationClient
import com.metamask.android.sdk.KeyExchange
import com.metamask.android.sdk.KeyExchangeMessageType

class MainActivity : AppCompatActivity(), LifecycleObserver {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding

    companion object {
        const val TAG = "MM_ANDROID_SDK"
    }

    private lateinit var communicationClient: CommunicationClient
    private lateinit var button: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        val navController = findNavController(R.id.nav_host_fragment_content_main)
        appBarConfiguration = AppBarConfiguration(navController.graph)
        setupActionBarWithNavController(navController, appBarConfiguration)

        binding.fab.setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                .setAction("Action", null).show()
        }

        button = findViewById<Button>(R.id.bind)
        button.setOnClickListener {
            val message = Bundle().apply {
                val bundle = Bundle().apply {
                    putString(KeyExchange.TYPE, KeyExchangeMessageType.key_exchange_SYN.name)
                }
                putBundle("KEY_EXCHANGE", bundle)
            }
            communicationClient.sendMessage(message)
        }

        Log.d(TAG, "app: onCreate")
        communicationClient = CommunicationClient(applicationContext, lifecycle)
        lifecycle.addObserver(this)
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
        Log.d(TAG, "app: Started SDK activity")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "app: Destroyed SDK activity")
    }
}