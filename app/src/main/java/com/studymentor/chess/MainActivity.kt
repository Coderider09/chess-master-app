package com.studymentor.chess

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.card.MaterialCardView

class MainActivity : AppCompatActivity() {

    private lateinit var cardVsBot: MaterialCardView
    private lateinit var cardTwoPlayers: MaterialCardView
    private lateinit var cardBluetooth: MaterialCardView
    private lateinit var cardSettings: MaterialCardView
    private lateinit var difficultyDialog: MaterialCardView
    private lateinit var tvBotDifficulty: TextView
    private lateinit var ivLogo: ImageView
    private lateinit var btnInfo: MaterialCardView

    private lateinit var btnBeginner: MaterialCardView
    private lateinit var btnAmateur: MaterialCardView
    private lateinit var btnExpert: MaterialCardView
    private lateinit var btnMaster: MaterialCardView
    private lateinit var btnGrandmaster: MaterialCardView
    private lateinit var btnCancel: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        setupClickListeners()
        startAnimations()
    }

    private fun initViews() {
        cardVsBot = findViewById(R.id.cardVsBot)
        cardTwoPlayers = findViewById(R.id.cardTwoPlayers)
        cardBluetooth = findViewById(R.id.cardBluetooth)
        cardSettings = findViewById(R.id.cardSettings)
        difficultyDialog = findViewById(R.id.difficultyDialog)
        tvBotDifficulty = findViewById(R.id.tvBotDifficulty)
        ivLogo = findViewById(R.id.ivLogo)
        btnInfo = findViewById(R.id.btnInfo)

        btnBeginner = findViewById(R.id.btnBeginner)
        btnAmateur = findViewById(R.id.btnAmateur)
        btnExpert = findViewById(R.id.btnExpert)
        btnMaster = findViewById(R.id.btnMaster)
        btnGrandmaster = findViewById(R.id.btnGrandmaster)
        btnCancel = findViewById(R.id.btnCancel)
    }

    private fun startAnimations() {
        try {
            val fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in)
            ivLogo.startAnimation(fadeIn)
        } catch (e: Exception) { }
    }

    private fun setupClickListeners() {
        btnInfo.setOnClickListener {
            showInfoDialog()
        }

        cardVsBot.setOnClickListener {
            difficultyDialog.visibility = View.VISIBLE
            try {
                difficultyDialog.startAnimation(
                    AnimationUtils.loadAnimation(this, R.anim.slide_in_up)
                )
            } catch (e: Exception) { }
        }

        cardTwoPlayers.setOnClickListener {
            startGame("twoPlayers", "medium")
        }

        cardBluetooth.setOnClickListener {
            startActivity(Intent(this, BluetoothActivity::class.java))
        }

        cardSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        btnBeginner.setOnClickListener {
            tvBotDifficulty.text = getString(R.string.beginner)
            tvBotDifficulty.setTextColor(resources.getColor(R.color.level_beginner, theme))
            difficultyDialog.visibility = View.GONE
            startGame("bot", "beginner")
        }

        btnAmateur.setOnClickListener {
            tvBotDifficulty.text = getString(R.string.amateur)
            tvBotDifficulty.setTextColor(resources.getColor(R.color.level_amateur, theme))
            difficultyDialog.visibility = View.GONE
            startGame("bot", "amateur")
        }

        btnExpert.setOnClickListener {
            tvBotDifficulty.text = getString(R.string.expert)
            tvBotDifficulty.setTextColor(resources.getColor(R.color.level_expert, theme))
            difficultyDialog.visibility = View.GONE
            startGame("bot", "expert")
        }

        btnMaster.setOnClickListener {
            tvBotDifficulty.text = getString(R.string.master)
            tvBotDifficulty.setTextColor(resources.getColor(R.color.level_master, theme))
            difficultyDialog.visibility = View.GONE
            startGame("bot", "master")
        }

        btnGrandmaster.setOnClickListener {
            tvBotDifficulty.text = getString(R.string.grandmaster)
            tvBotDifficulty.setTextColor(resources.getColor(R.color.level_grandmaster, theme))
            difficultyDialog.visibility = View.GONE
            startGame("bot", "grandmaster")
        }

        btnCancel.setOnClickListener {
            difficultyDialog.visibility = View.GONE
        }
    }

    private fun showInfoDialog() {
        // Принудительно используем темную тему для диалога
        val builder = AlertDialog.Builder(this, R.style.CustomInfoDialog)
        builder.setTitle(getString(R.string.info_title))
        builder.setMessage(getString(R.string.info_content))
        builder.setPositiveButton("OK", null)
        builder.show()
    }

    private fun startGame(mode: String, difficulty: String) {
        val intent = Intent(this, GameActivity::class.java)
        intent.putExtra("game_mode", mode)
        intent.putExtra("difficulty", difficulty)
        startActivity(intent)
    }
}