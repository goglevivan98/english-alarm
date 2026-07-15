package com.ivan.englishalarm

import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.WindowManager
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class QuizActivity : AppCompatActivity() {

    private lateinit var db: WordDbHelper
    private var currentWord: Word? = null
    private var ringtone: android.media.Ringtone? = null
    private var vibrator: Vibrator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Показать поверх экрана блокировки и включить экран.
        setShowWhenLocked(true)
        setTurnScreenOn(true)
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
        )

        setContentView(R.layout.activity_quiz)
        db = WordDbHelper(this)

        startAlarmSound()
        loadNextWord()

        findViewById<Button>(R.id.btnSubmit).setOnClickListener { checkAnswer() }
    }

    private fun startAlarmSound() {
        val uri = RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_ALARM)
        ringtone = RingtoneManager.getRingtone(this, uri)
        ringtone?.audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        ringtone?.play()

        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        val pattern = longArrayOf(0, 500, 500)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(pattern, 0)
        }
    }

    private fun stopAlarmSound() {
        ringtone?.stop()
        vibrator?.cancel()
    }

    private fun loadNextWord() {
        val word = db.getRandomWeightedWord()
        currentWord = word
        findViewById<TextView>(R.id.tvWord).text = word?.en ?: "Нет слов — добавь их в приложении"
        findViewById<EditText>(R.id.etAnswer).setText("")
    }

    private fun checkAnswer() {
        val word = currentWord ?: return
        val input = findViewById<EditText>(R.id.etAnswer).text.toString().trim()
        val correct = input.equals(word.ru.trim(), ignoreCase = true)

        if (correct) {
            stopAlarmSound()
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.cancel(1)
            Toast.makeText(this, "Верно! Будильник выключен", Toast.LENGTH_SHORT).show()
            finish()
        } else {
            db.incrementWrong(word.id)
            val shake = AnimationUtils.loadAnimation(this, R.anim.shake)
            findViewById<TextView>(R.id.tvWord).startAnimation(shake)
            Toast.makeText(this, "Неверно, попробуй ещё раз", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopAlarmSound()
    }
}
