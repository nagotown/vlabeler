package com.sdercolin.vlabeler.ui

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import com.sdercolin.vlabeler.env.Log
import com.sdercolin.vlabeler.model.AppRecord
import com.sdercolin.vlabeler.util.AppRecordFile
import com.sdercolin.vlabeler.util.toJson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@Stable
class AppRecordStore(appRecord: AppRecord, private val scope: CoroutineScope) : MutableState<AppRecord> {

    private val stateFlow = MutableStateFlow(appRecord)

    init {
        collectAndWrite()
    }

    override var value: AppRecord
        get() = stateFlow.value
        set(value) {
            push(value)
        }

    override fun component1(): AppRecord = value

    override fun component2(): (AppRecord) -> Unit = { value = it }

    private fun push(appRecord: AppRecord) {
        scope.launch(Dispatchers.IO) {
            stateFlow.emit(appRecord)
        }
    }

    private fun collectAndWrite() {
        scope.launch(Dispatchers.IO) {
            stateFlow.collectLatest {
                delay(ThrottlePeriodMs)
                AppRecordFile.writeText(toJson(it))
                Log.info("Written appRecord: $it")
            }
        }
    }

    fun update(updater: AppRecord.() -> AppRecord) {
        push(updater(value))
    }

    companion object {
        private const val ThrottlePeriodMs = 500L
    }
}
