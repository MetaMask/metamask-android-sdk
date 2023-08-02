package com.metamask.android

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.activity.viewModels
import com.metamask.android.databinding.ActivityMainBinding
import io.metamask.androidsdk.*

class MainActivity : AppCompatActivity(), RootLayoutProvider {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding

    private lateinit var connectButton: Button
    private lateinit var connectResultLabel: TextView

    private lateinit var clearSessionButton: Button
    private lateinit var sessionLabel: TextView

    private lateinit var signButton: Button
    private lateinit var signResultLabel: TextView

    private lateinit var sendButton: Button
    private lateinit var sendResultLabel: TextView

    // Obtain EthereumViewModel using viewModels() delegate
    private val ethereumViewModel: EthereumViewModel by viewModels()

    private lateinit var exampleDapp: ExampleDapp

    override fun getRootLayout(): View {
        return findViewById(R.id.toolbar)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Logger.log("app: onCreate")

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        val navController = findNavController(R.id.nav_host_fragment_content_main)
        appBarConfiguration = AppBarConfiguration(navController.graph)
        setupActionBarWithNavController(navController, appBarConfiguration)

        exampleDapp = ExampleDapp(ethereumViewModel, this)

        connectButton = findViewById(R.id.connectButton)
        connectResultLabel = findViewById(R.id.connectText)

        ethereumViewModel.activeAddress.observe(this) { account ->
            connectResultLabel.text = account
//            if (account.isNotEmpty()) {
//                val intent = Intent(this, DappActionsActvity::class.java)
//                startActivity(intent)
//            }
        }

        connectButton.setOnClickListener {
            exampleDapp.connect() { result ->
                connectResultLabel.text = result.toString()

                if (result !is RequestError) {
                    "SessionId: ${ethereumViewModel.getSessionId()}"
                        .also { sessionLabel.text = it }
                }
            }
        }

        clearSessionButton = findViewById(R.id.clearButton)
        sessionLabel = findViewById(R.id.sessionText)
        "SessionId: ${ethereumViewModel.getSessionId()}"
            .also { sessionLabel.text = it }

        clearSessionButton.setOnClickListener {
            ethereumViewModel.clearSession()
            "SessionId: ${ethereumViewModel.getSessionId()}"
                .also { sessionLabel.text = it }
            signResultLabel.text = ""
            connectResultLabel.text = ""
            sendResultLabel.text = ""
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