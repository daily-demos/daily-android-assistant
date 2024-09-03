package co.daily.bots.assistant.utils

import ai.rtvi.client.utils.ThreadRef
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import java.io.File
import kotlin.concurrent.thread

private val JSON_INSTANCE = Json { ignoreUnknownKeys = true }

class DataFile<T>(
    private val serializer: KSerializer<T>,
    defaultValue: T,
    file: File
) {

    companion object {
        private const val TAG = "BackgroundFileWriter"
    }

    private val thread = ThreadRef.forCurrent()

    private val fileTmp = File(file.absolutePath + ".tmp")

    private val onLoadedCallbacks = mutableListOf<() -> Unit>()
    private var loadedContents: T? = null

    private val pendingLock = Object()
    private var pending: String? = null

    init {
        thread {
            val initialContents = if (file.exists()) {
                file.readText()
            } else {
                null
            }

            thread.runOnThread {
                if (loadedContents == null) {
                    loadedContents =
                        initialContents?.let { JSON_INSTANCE.decodeFromString(serializer, it) } ?: defaultValue
                }

                onLoadedCallbacks.forEach { it() }
                onLoadedCallbacks.clear()
            }

            synchronized(pendingLock) {
                while (true) {

                    while (pending == null) {
                        pendingLock.wait()
                    }

                    val toWrite = pending
                    pending = null

                    fileTmp.writeText(toWrite!!)
                    fileTmp.renameTo(file) || throw Exception("Failed to rename file")
                }
            }
        }
    }

    fun write(data: T) {

        thread.assertCurrent()

        loadedContents = data

        synchronized(pendingLock) {
            pending = JSON_INSTANCE.encodeToString(serializer, data)
            pendingLock.notifyAll()
        }
    }

    val isContentsReady
        get() = thread.assertCurrent { loadedContents != null }

    val contents
        get() = thread.assertCurrent { loadedContents }

    fun onLoaded(callback: () -> Unit) {
        thread.assertCurrent()

        if (loadedContents == null) {
            onLoadedCallbacks.add(callback)
        } else {
            callback()
        }
    }
}