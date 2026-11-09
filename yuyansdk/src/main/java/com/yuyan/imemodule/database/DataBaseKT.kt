package com.yuyan.imemodule.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.yuyan.imemodule.application.Launcher
import com.yuyan.imemodule.database.dao.ClipboardDao
import com.yuyan.imemodule.database.dao.PhraseDao
import com.yuyan.imemodule.database.dao.SideSymbolDao
import com.yuyan.imemodule.database.dao.SkbFunDao
import com.yuyan.imemodule.database.dao.UsedSymbolDao
import com.yuyan.imemodule.database.entry.Clipboard
import com.yuyan.imemodule.database.entry.Phrase
import com.yuyan.imemodule.database.entry.SideSymbol
import com.yuyan.imemodule.database.entry.SkbFun
import com.yuyan.imemodule.database.entry.UsedSymbol
import com.yuyan.imemodule.prefs.behavior.SkbMenuMode
import com.yuyan.imemodule.utils.thread.ThreadPoolUtils

//@Database(entities = [SideSymbol::class, Clipboard::class, UsedSymbol::class], version = 1, exportSchema = false)
@Database(entities = [SideSymbol::class, Clipboard::class, UsedSymbol::class, Phrase::class, SkbFun::class], version = 4, exportSchema = false)
abstract class DataBaseKT : RoomDatabase() {
    abstract fun sideSymbolDao(): SideSymbolDao
    abstract fun clipboardDao(): ClipboardDao
    abstract fun usedSymbolDao(): UsedSymbolDao
    abstract fun phraseDao(): PhraseDao
    abstract fun skbFunDao(): SkbFunDao
    companion object {
        @Volatile private var cachedSideSymbolsPinyin: List<SideSymbol>? = null
        @Volatile private var cachedSideSymbolsNumber: List<SideSymbol>? = null

        fun getSideSymbolsPinyin(): List<SideSymbol> {
            return cachedSideSymbolsPinyin ?: instance.sideSymbolDao().getAllSideSymbolPinyin().also { cachedSideSymbolsPinyin = it }
        }

        fun getSideSymbolsNumber(): List<SideSymbol> {
            return cachedSideSymbolsNumber ?: instance.sideSymbolDao().getAllSideSymbolNumber().also { cachedSideSymbolsNumber = it }
        }

        fun invalidateSideSymbolsCache() {
            cachedSideSymbolsPinyin = null
            cachedSideSymbolsNumber = null
        }

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS phrase (content TEXT PRIMARY KEY NOT NULL, isKeep INTEGER NOT NULL, t9 TEXT NOT NULL, qwerty TEXT NOT NULL, lx17 TEXT NOT NULL, time INTEGER NOT NULL)")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS skbfun (name TEXT KEY NOT NULL, isKeep INTEGER NOT NULL, position INTEGER NOT NULL, PRIMARY KEY (name, isKeep))")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("INSERT INTO skbfun (name, isKeep, position) VALUES ('TextEdit', 0, 15)")
                db.execSQL("INSERT INTO skbfun (name, isKeep, position) VALUES ('TextEdit', 1, 0)")
            }
        }

        val instance = Room.databaseBuilder(Launcher.instance.context, DataBaseKT::class.java, "ime_db")
            .allowMainThreadQueries()
            .addMigrations(MIGRATION_1_2)
            .addMigrations(MIGRATION_2_3)
            .addMigrations(MIGRATION_3_4)
            .addCallback(object :Callback(){
                override fun onCreate(db: SupportSQLiteDatabase) {
                    super.onCreate(db)
                    ThreadPoolUtils.executeSingleton {
                        initDb()
                    }
                }

                override fun onOpen(db: SupportSQLiteDatabase) {
                    super.onOpen(db)
                    ThreadPoolUtils.executeSingleton {
                        initPhrasesDb()
                    }
                }
            })
            .build()
        private fun initDb() {  //初始化数据库数据
            val symbolPinyin = listOf("，", "。", "？", "！", "～", "：", "；", ".").map {  symbolKey->
                SideSymbol(symbolKey, symbolKey)
            }
            instance.sideSymbolDao().insertAll(symbolPinyin)
            val symbolNumber = listOf("%", "/", "-", "+", "*", "#", "@").map {  symbolKey->
                SideSymbol(symbolKey, symbolKey, "number")
            }
            instance.sideSymbolDao().insertAll(symbolNumber)
        }

        private fun initPhrasesDb() {  // 初始化常用语数据
            val presetContents = listOf(
                "我的电话是__，常联系。",
                "抱歉，我现在不方便接电话，稍后联系。",
                "我正在开会，有急事请发短信。",
                "我很快就到，请稍微等一会儿。",
                "麻烦放驿站，谢谢。"
            )
            for (preset in presetContents) {
                instance.phraseDao().deleteByContent(preset)
            }
            if(instance.skbFunDao().getAllMenu().isEmpty()) {
                val skbFuns = listOf(
                    SkbFun(name = SkbMenuMode.SwitchKeyboard.name, isKeep = 1, position = 0),
                    SkbFun(name = SkbMenuMode.Emojicon.name, isKeep = 1, position = 1),
                    SkbFun(name = SkbMenuMode.ClipBoard.name, isKeep = 1, position = 2),
                    SkbFun(name = SkbMenuMode.Phrases.name, isKeep = 0, position = 0),          // 1. 常用语
                    SkbFun(name = SkbMenuMode.Settings.name, isKeep = 0, position = 1),         // 2. 设置
                    SkbFun(name = SkbMenuMode.KeyboardHeight.name, isKeep = 0, position = 2),   // 3. 键盘调节
                    SkbFun(name = SkbMenuMode.Custom.name, isKeep = 0, position = 3),           // 4. 主题皮肤
                    SkbFun(name = SkbMenuMode.OneHanded.name, isKeep = 0, position = 4),         // 5. 单手模式
                    SkbFun(name = SkbMenuMode.Feedback.name, isKeep = 0, position = 5),          // 6. 按键效果
                    SkbFun(name = SkbMenuMode.Mnemonic.name, isKeep = 0, position = 6),          // 7. 双拼提示
                    SkbFun(name = SkbMenuMode.FloatKeyboard.name, isKeep = 0, position = 7),    // 8. 悬浮键盘
                    SkbFun(name = SkbMenuMode.NumberRow.name, isKeep = 0, position = 8),        // 9. 数字行
                    SkbFun(name = SkbMenuMode.JianFan.name, isKeep = 0, position = 9),          // 10. 繁体输入
                    SkbFun(name = SkbMenuMode.Emojicon.name, isKeep = 0, position = 10),
                    SkbFun(name = SkbMenuMode.SwitchKeyboard.name, isKeep = 0, position = 11),
                    SkbFun(name = SkbMenuMode.ClipBoard.name, isKeep = 0, position = 12),
                    SkbFun(name = SkbMenuMode.DarkTheme.name, isKeep = 0, position = 13),
                    SkbFun(name = SkbMenuMode.FlowerTypeface.name, isKeep = 0, position = 14),
                    SkbFun(name = SkbMenuMode.TextEdit.name, isKeep = 0, position = 15),
                )
                instance.skbFunDao().insertAll(skbFuns)
            } else {
                val currentBarMenu = instance.skbFunDao().getALlBarMenu()
                val oldDefaultNames = listOf(SkbMenuMode.ClipBoard.name, SkbMenuMode.Emojicon.name, SkbMenuMode.SwitchKeyboard.name)
                val currentNames = currentBarMenu.map { it.name }
                if (currentNames == oldDefaultNames || currentNames.isEmpty()) {
                    instance.skbFunDao().deleteAllBarMenu()
                    instance.skbFunDao().insert(SkbFun(name = SkbMenuMode.SwitchKeyboard.name, isKeep = 1, position = 0))
                    instance.skbFunDao().insert(SkbFun(name = SkbMenuMode.Emojicon.name, isKeep = 1, position = 1))
                    instance.skbFunDao().insert(SkbFun(name = SkbMenuMode.ClipBoard.name, isKeep = 1, position = 2))
                }
            }
        }
    }
}
