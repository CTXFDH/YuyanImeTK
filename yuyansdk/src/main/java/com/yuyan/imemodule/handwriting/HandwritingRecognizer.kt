package com.yuyan.imemodule.handwriting

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.yuyan.imemodule.handwriting.internal.Predictor
import com.yuyan.imemodule.handwriting.internal.RecEngine
import com.yuyan.inputmethod.core.HandWriting
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.jvm.Synchronized

object HandwritingRecognizer {

    private val cpuThreads: Int = resolveCpuThreads()
    private const val POWER_MODE: Int = HandWriting.POWER_MODE_HIGH

    private val worker: ExecutorService by lazy {
        Executors.newSingleThreadExecutor { runnable ->
            val t = Thread(runnable, "hw-rec")
            t.isDaemon = true
            t
        }
    }
    private val mainHandler = Handler(Looper.getMainLooper())
    private var engine: RecEngine? = null

    val isReady: Boolean
        get() = engine?.isReady == true

    private fun resolveCpuThreads(): Int {
        val cores = Runtime.getRuntime().availableProcessors()
        return when {
            cores >= 6 -> 4
            cores >= 4 -> 3
            else -> 2
        }
    }

    fun prepareModel(context: Context): Boolean =
        Predictor.materializeModel(context.applicationContext) != null

    @Synchronized
    fun initialize(context: Context): Boolean {
        val existing = engine
        if (existing != null && existing.isReady) return true

        val target = existing ?: RecEngine(
            context.applicationContext,
            cpuThreads,
            POWER_MODE
        ).also { engine = it }
        return target.load()
    }

    fun initializeAsync(context: Context, callback: ((Boolean) -> Unit)? = null) {
        if (isReady) {
            if (callback != null) {
                mainHandler.post { callback(true) }
            }
            return
        }
        val appContext = context.applicationContext
        worker.execute {
            val ok = initialize(appContext)
            if (callback != null) {
                mainHandler.post { callback(ok) }
            }
        }
    }

    fun recognizeBlocking(strokes: List<Stroke>): List<Candidate> {
        val target = engine ?: return emptyList()
        if (!target.isReady || strokes.isEmpty()) return emptyList()
        return try {
            target.recognize(strokes)
        } catch (_: Exception) {
            emptyList()
        }
    }
}
