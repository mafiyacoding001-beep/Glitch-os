package com.example.pipeline

import android.util.Log
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class ActivePipelineInfo(
    val jobId: String,
    val title: String,
    val startedAt: Long = System.currentTimeMillis(),
    val isWorkerTask: Boolean = false
)

object PipelineExecutionCoordinator {
    private const val TAG = "PipelineCoordinator"
    private val executionMutex = Mutex()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _activeJobInfo = MutableStateFlow<ActivePipelineInfo?>(null)
    val activeJobInfo: StateFlow<ActivePipelineInfo?> = _activeJobInfo.asStateFlow()

    private var currentCoroutineJob: Job? = null

    fun isJobRunning(): Boolean {
        return _isGenerating.value
    }

    fun getActiveJobId(): String? {
        return _activeJobInfo.value?.jobId
    }

    fun registerJobHandle(job: Job) {
        currentCoroutineJob = job
    }

    suspend fun tryAcquireExecution(
        jobId: String,
        title: String,
        isWorkerTask: Boolean = false
    ): Boolean {
        return executionMutex.withLock {
            if (_isGenerating.value) {
                Log.w(
                    TAG,
                    "Execution request REJECTED for '$title' ($jobId). Another job is already active: ${_activeJobInfo.value?.title} (${_activeJobInfo.value?.jobId})"
                )
                false
            } else {
                Log.i(TAG, "Execution lock ACQUIRED for '$title' ($jobId)")
                _isGenerating.value = true
                _activeJobInfo.value = ActivePipelineInfo(
                    jobId = jobId,
                    title = title,
                    isWorkerTask = isWorkerTask
                )
                true
            }
        }
    }

    suspend fun releaseExecution(jobId: String) {
        executionMutex.withLock {
            if (_activeJobInfo.value?.jobId == jobId || _activeJobInfo.value == null) {
                Log.i(TAG, "Execution lock RELEASED for jobId: $jobId")
                _isGenerating.value = false
                _activeJobInfo.value = null
                currentCoroutineJob = null
            } else {
                Log.w(
                    TAG,
                    "releaseExecution called for $jobId but active job is ${_activeJobInfo.value?.jobId}"
                )
            }
        }
    }

    fun cancelActiveJob(jobId: String) {
        Log.i(TAG, "Cancelling active pipeline execution for jobId: $jobId")
        try {
            currentCoroutineJob?.cancel()
        } catch (e: Exception) {
            Log.e(TAG, "Error cancelling coroutine job", e)
        }
        _isGenerating.value = false
        _activeJobInfo.value = null
        currentCoroutineJob = null
    }
}
