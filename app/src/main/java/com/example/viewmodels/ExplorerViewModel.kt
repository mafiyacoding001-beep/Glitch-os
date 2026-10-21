package com.example.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AppInfoModel
import com.example.data.model.PermissionInfoModel
import com.example.data.model.ScanReportModel
import com.example.data.model.StorageItemModel
import com.example.data.model.TransparencyStatus
import com.example.data.repository.AppDataRepository
import com.example.shizuku.ShizukuManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class ExplorerViewModel(application: Application) : AndroidViewModel(application) {

    private val _isDemoMode = MutableStateFlow(false)
    val isDemoMode: StateFlow<Boolean> = _isDemoMode.asStateFlow()

    private val _apps = MutableStateFlow<List<AppInfoModel>>(emptyList())
    val apps: StateFlow<List<AppInfoModel>> = _apps.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _permissions = MutableStateFlow<List<PermissionInfoModel>>(emptyList())
    val permissions: StateFlow<List<PermissionInfoModel>> = _permissions.asStateFlow()

    private val _storageItems = MutableStateFlow<List<StorageItemModel>>(emptyList())
    val storageItems: StateFlow<List<StorageItemModel>> = _storageItems.asStateFlow()

    private val _currentPath = MutableStateFlow("")
    val currentPath: StateFlow<String> = _currentPath.asStateFlow()

    private val _shizukuInstalled = MutableStateFlow(false)
    val shizukuInstalled: StateFlow<Boolean> = _shizukuInstalled.asStateFlow()

    private val _shizukuRunning = MutableStateFlow(false)
    val shizukuRunning: StateFlow<Boolean> = _shizukuRunning.asStateFlow()

    private val _shizukuPermission = MutableStateFlow(false)
    val shizukuPermission: StateFlow<Boolean> = _shizukuPermission.asStateFlow()

    private val _shizukuVersion = MutableStateFlow(-1)
    val shizukuVersion: StateFlow<Int> = _shizukuVersion.asStateFlow()

    private val _shellOutput = MutableStateFlow("Ready for Shizuku command execution.")
    val shellOutput: StateFlow<String> = _shellOutput.asStateFlow()

    private val _reports = MutableStateFlow<List<ScanReportModel>>(emptyList())
    val reports: StateFlow<List<ScanReportModel>> = _reports.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        refreshData()
    }

    fun setDemoMode(enabled: Boolean) {
        _isDemoMode.value = enabled
        refreshData()
    }

    fun refreshData() {
        val context = getApplication<Application>()
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _apps.value = AppDataRepository.getInstalledApps(context, _isDemoMode.value)
                _permissions.value = AppDataRepository.getPermissionsSummary(context, _isDemoMode.value)
                _storageItems.value = AppDataRepository.getStorageItems(context, _currentPath.value)

                // Check Shizuku status
                val installed = ShizukuManager.isShizukuInstalled(context)
                val running = ShizukuManager.isShizukuRunning()
                val perm = ShizukuManager.hasShizukuPermission()
                val ver = ShizukuManager.getVersion()

                _shizukuInstalled.value = installed
                _shizukuRunning.value = running
                _shizukuPermission.value = perm
                _shizukuVersion.value = ver
            } catch (e: Exception) {
                // Handle error gracefully
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun loadStoragePath(path: String) {
        _currentPath.value = path
        val context = getApplication<Application>()
        viewModelScope.launch {
            _storageItems.value = AppDataRepository.getStorageItems(context, path)
        }
    }

    fun requestShizukuPermission(requestCode: Int) {
        ShizukuManager.requestPermission(requestCode)
        refreshData()
    }

    fun executeShizukuCommand(command: String) {
        viewModelScope.launch {
            _shellOutput.value = "Executing: $command..."
            val result = ShizukuManager.runShizukuCommand(command)
            result.fold(
                onSuccess = { output ->
                    _shellOutput.value = output.ifEmpty { "Command executed successfully with empty output." }
                },
                onFailure = { err ->
                    _shellOutput.value = "Error: ${err.localizedMessage ?: "Unknown error"}\nNote: Ensure Shizuku is running and authorization is granted."
                }
            )
        }
    }

    fun generateReport(packageName: String?, appName: String?, status: TransparencyStatus, dataSource: String, itemsSummary: String) {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val report = ScanReportModel(
            scanId = UUID.randomUUID().toString().substring(0, 8),
            timestamp = dateFormat.format(Date()),
            packageName = packageName,
            appName = appName,
            status = status,
            dataSource = dataSource,
            itemsSummary = itemsSummary,
            limitations = emptyList(),
            consentRequired = false,
            consentGranted = true,
            errors = emptyList()
        )
        _reports.value = listOf(report) + _reports.value
    }

    fun deleteReport(scanId: String) {
        _reports.value = _reports.value.filter { it.scanId != scanId }
    }
}
