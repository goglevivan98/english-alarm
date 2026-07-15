package com.ivan.englishalarm

import android.app.AlarmManager
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var db: WordDbHelper
    private lateinit var prefs: android.content.SharedPreferences
    private lateinit var tvTime: TextView
    private lateinit var listView: ListView

    private var hour = 7
    private var minute = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        db = WordDbHelper(this)
        prefs = getSharedPreferences("prefs", Context.MODE_PRIVATE)
        hour = prefs.getInt("alarm_hour", 7)
        minute = prefs.getInt("alarm_minute", 0)

        tvTime = findViewById(R.id.tvTime)
        listView = findViewById(R.id.listWords)
        updateTimeLabel()

        val switchEnabled = findViewById<Switch>(R.id.switchEnabled)
        switchEnabled.isChecked = prefs.getBoolean("alarm_enabled", false)

        findViewById<Button>(R.id.btnPickTime).setOnClickListener {
            TimePickerDialog(this, { _, h, m ->
                hour = h
                minute = m
                updateTimeLabel()
                prefs.edit().putInt("alarm_hour", hour).putInt("alarm_minute", minute).apply()
                if (switchEnabled.isChecked) {
                    scheduleAlarmIfAllowed()
                }
            }, hour, minute, true).show()
        }

        switchEnabled.setOnCheckedChangeListener { _, checked ->
            prefs.edit().putBoolean("alarm_enabled", checked).apply()
            if (checked) {
                scheduleAlarmIfAllowed()
            } else {
                AlarmScheduler.cancel(this)
                Toast.makeText(this, "Будильник выключен", Toast.LENGTH_SHORT).show()
            }
        }

        findViewById<Button>(R.id.btnAddWord).setOnClickListener { showAddWordDialog() }

        refreshWordList()
    }

    override fun onResume() {
        super.onResume()
        refreshWordList()
    }

    private fun updateTimeLabel() {
        tvTime.text = String.format("%02d:%02d", hour, minute)
    }

    private fun scheduleAlarmIfAllowed() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !AlarmScheduler.canScheduleExact(this)) {
            AlertDialog.Builder(this)
                .setTitle("Нужно разрешение")
                .setMessage("Разреши точные будильники в настройках Android, иначе будильник может опаздывать")
                .setPositiveButton("Открыть настройки") { _, _ ->
                    val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                        data = Uri.parse("package:$packageName")
                    }
                    startActivity(intent)
                }
                .setNegativeButton("Отмена", null)
                .show()
            return
        }
        AlarmScheduler.scheduleNext(this, hour, minute)
        Toast.makeText(this, "Будильник поставлен на ${String.format("%02d:%02d", hour, minute)}", Toast.LENGTH_SHORT).show()
    }

    private fun showAddWordDialog() {
        val container = layoutInflater.inflate(R.layout.dialog_add_word, null)
        val etEn = container.findViewById<EditText>(R.id.etEn)
        val etRu = container.findViewById<EditText>(R.id.etRu)

        AlertDialog.Builder(this)
            .setTitle("Новое слово")
            .setView(container)
            .setPositiveButton("Добавить") { _, _ ->
                val en = etEn.text.toString().trim()
                val ru = etRu.text.toString().trim()
                if (en.isNotEmpty() && ru.isNotEmpty()) {
                    db.addWord(en, ru)
                    refreshWordList()
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun refreshWordList() {
        val words = db.getAllWords()
        val items = words.map { "${it.en} — ${it.ru}  (ошибок: ${it.wrongCount})" }
        listView.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, items)
        listView.setOnItemLongClickListener { _, _, position, _ ->
            AlertDialog.Builder(this)
                .setTitle("Удалить слово?")
                .setMessage(words[position].en)
                .setPositiveButton("Удалить") { _, _ ->
                    db.deleteWord(words[position].id)
                    refreshWordList()
                }
                .setNegativeButton("Отмена", null)
                .show()
            true
        }
    }
}
