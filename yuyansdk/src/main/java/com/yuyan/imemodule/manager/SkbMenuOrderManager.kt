package com.yuyan.imemodule.manager

import com.yuyan.imemodule.data.menuSkbFunsPreset
import com.yuyan.imemodule.database.DataBaseKT
import com.yuyan.imemodule.database.dao.SkbFunDao
import com.yuyan.imemodule.database.entry.SkbFun
import com.yuyan.imemodule.entity.SkbFunItem
import com.yuyan.imemodule.prefs.behavior.SkbMenuMode
import com.yuyan.imemodule.utils.thread.ThreadPoolUtils

/**
 * 键盘菜单顺序管理
 */
object SkbMenuOrderManager {

    private const val BAR_MENU = 1
    private const val SETTING_MENU = 0

    @Volatile
    private var barMenuCache: MutableList<SkbFunItem>? = null

    fun barMenus(): MutableList<SkbFunItem> =
        barMenuCache?.toMutableList() ?: loadBarMenus().also { barMenuCache = it.toMutableList() }.toMutableList()

    private fun loadBarMenus(): MutableList<SkbFunItem> {
        val items = mutableListOf<SkbFunItem>()
        runCatching {
            DataBaseKT.instance.skbFunDao().getAllBarMenu().forEach { bean ->
                val mode = runCatching { SkbMenuMode.decode(bean.name) }.getOrNull()
                if (mode != null) {
                    menuSkbFunsPreset[mode]?.let(items::add)
                }
            }
        }
        if (items.isEmpty()) {
            items.addAll(menuSkbFunsPreset.values)
        }
        return items
    }

    fun invalidate() {
        barMenuCache = null
    }

    fun isInBarMenu(skbMenuMode: SkbMenuMode): Boolean =
        barMenus().any { it.skbMenuMode == skbMenuMode }

    fun addBarMenu(skbMenuMode: SkbMenuMode): Boolean {
        val item = menuSkbFunsPreset[skbMenuMode] ?: return false
        val current = barMenuCache ?: loadBarMenus().also { barMenuCache = it.toMutableList() }
        if (current.any { it.skbMenuMode == skbMenuMode }) return false
        current.add(item)
        writeBarOrder(current.map { it.skbMenuMode })
        return true
    }

    fun removeBarMenu(skbMenuMode: SkbMenuMode): Boolean {
        val current = barMenuCache ?: loadBarMenus().also { barMenuCache = it.toMutableList() }
        if (!current.removeAll { it.skbMenuMode == skbMenuMode }) return false
        writeBarOrder(current.map { it.skbMenuMode })
        return true
    }

    fun persistBarMenus(menus: List<SkbFunItem>) {
        barMenuCache = menus.toMutableList()
        writeBarOrder(menus.map { it.skbMenuMode })
    }

    fun persistSettingMenus(menus: List<SkbFunItem>) = persistOrder(menus.map { it.skbMenuMode }, SETTING_MENU)

    private fun writeBarOrder(order: List<SkbMenuMode>) {
        write { dao ->
            dao.deleteAllBarMenu()
            order.forEachIndexed { index, mode ->
                dao.insert(SkbFun(name = mode.name, isKeep = BAR_MENU, position = index))
            }
        }
    }

    private fun persistOrder(order: List<SkbMenuMode>, isKeep: Int) {
        write { dao ->
            order.forEachIndexed { index, mode ->
                // 用 REPLACE 语义写入：@Update 在行不存在时会静默失败（例如刚添加还没落盘就拖拽）
                dao.insert(SkbFun(name = mode.name, isKeep = isKeep, position = index))
            }
        }
    }

    private fun write(block: (SkbFunDao) -> Unit) {
        ThreadPoolUtils.executeSingleton {
            runCatching { block(DataBaseKT.instance.skbFunDao()) }
                .onFailure { invalidate() }
        }
    }
}
