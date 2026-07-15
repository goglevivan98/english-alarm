package com.ivan.englishalarm

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import kotlin.random.Random

data class Word(val id: Long, val en: String, val ru: String, val wrongCount: Int)

class WordDbHelper(context: Context) :
    SQLiteOpenHelper(context, "words.db", null, 1) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE words (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "en TEXT NOT NULL, " +
                "ru TEXT NOT NULL, " +
                "wrong_count INTEGER NOT NULL DEFAULT 0)"
        )
        seedDefaults(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS words")
        onCreate(db)
    }

    private fun seedDefaults(db: SQLiteDatabase) {
        val defaults = listOf(
            "regression testing" to "регрессионное тестирование",
            "test coverage" to "покрытие тестами",
            "flaky test" to "нестабильный тест",
            "assertion" to "проверка утверждения",
            "mock" to "имитация объекта",
            "root cause" to "первопричина",
            "edge case" to "граничный случай",
            "pipeline" to "конвейер сборки",
            "rollback" to "откат изменений",
            "deployment" to "развёртывание",
            "bottleneck" to "узкое место",
            "trade-off" to "компромисс",
            "inheritance" to "наследование",
            "encapsulation" to "инкапсуляция",
            "concurrency" to "параллелизм"
        )
        defaults.forEach { (en, ru) ->
            val cv = ContentValues().apply {
                put("en", en)
                put("ru", ru)
            }
            db.insert("words", null, cv)
        }
    }

    fun getAllWords(): List<Word> {
        val list = mutableListOf<Word>()
        val c = readableDatabase.rawQuery(
            "SELECT id, en, ru, wrong_count FROM words ORDER BY id DESC", null
        )
        while (c.moveToNext()) {
            list.add(
                Word(
                    c.getLong(0), c.getString(1), c.getString(2), c.getInt(3)
                )
            )
        }
        c.close()
        return list
    }

    fun addWord(en: String, ru: String) {
        val cv = ContentValues().apply {
            put("en", en)
            put("ru", ru)
        }
        writableDatabase.insert("words", null, cv)
    }

    fun deleteWord(id: Long) {
        writableDatabase.delete("words", "id=?", arrayOf(id.toString()))
    }

    fun incrementWrong(id: Long) {
        writableDatabase.execSQL(
            "UPDATE words SET wrong_count = wrong_count + 1 WHERE id=?",
            arrayOf(id)
        )
    }

    /** Слова с большим числом ошибок выпадают чаще (простая взвешенная репетиция). */
    fun getRandomWeightedWord(): Word? {
        val words = getAllWords()
        if (words.isEmpty()) return null
        val expanded = mutableListOf<Word>()
        words.forEach { w ->
            val weight = (w.wrongCount.coerceAtMost(5)) + 1
            repeat(weight) { expanded.add(w) }
        }
        return expanded[Random.nextInt(expanded.size)]
    }
}
