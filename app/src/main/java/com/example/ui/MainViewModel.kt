package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.AvsNodeSpec
import com.example.data.model.ClusterConfig
import com.example.data.model.ClusterMetricSummary
import com.example.data.model.CpuReadyResult
import com.example.data.model.DrsRecommendation
import com.example.data.model.HostWorkload
import com.example.data.model.SavedReport
import com.example.data.model.SummationBreakdown
import com.example.data.model.SummationInputMode
import com.example.data.model.VmProfile
import com.example.data.model.WhatIfScenarioType
import com.example.data.model.WhatIfSimulationResult
import com.example.data.remote.VcenterApiResult
import com.example.data.remote.VcenterApiService
import com.example.domain.CalculatorEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val vmDao = db.vmDao()
    private val clusterDao = db.clusterDao()
    private val reportDao = db.savedReportDao()

    // Theme Mode
    private val _isDarkTheme = MutableStateFlow(true)
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()

    // Current Active Tab (0: Calculator, 1: AVS Cluster, 2: DRS Dashboard, 3: What-If, 4: VM Fleet)
    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    // Interactive CPU RDY % Calculator Inputs
    private val _calcReadyMs = MutableStateFlow(1600.0)
    val calcReadyMs: StateFlow<Double> = _calcReadyMs.asStateFlow()

    private val _calcSamplePeriodSec = MutableStateFlow(20)
    val calcSamplePeriodSec: StateFlow<Int> = _calcSamplePeriodSec.asStateFlow()

    private val _calcVcpuCount = MutableStateFlow(8)
    val calcVcpuCount: StateFlow<Int> = _calcVcpuCount.asStateFlow()

    private val _calcCoStopMs = MutableStateFlow(280.0)
    val calcCoStopMs: StateFlow<Double> = _calcCoStopMs.asStateFlow()

    // Summation Mode & Value
    private val _summationInputMode = MutableStateFlow(SummationInputMode.SUMMATION_MS)
    val summationInputMode: StateFlow<SummationInputMode> = _summationInputMode.asStateFlow()

    private val _summationInputValue = MutableStateFlow(1600.0)
    val summationInputValue: StateFlow<Double> = _summationInputValue.asStateFlow()

    // Live Summation Breakdown State
    val summationBreakdown: StateFlow<SummationBreakdown> = combine(
        _summationInputMode,
        _summationInputValue,
        _calcSamplePeriodSec,
        _calcVcpuCount
    ) { mode, value, sampleSec, vcpus ->
        CalculatorEngine.calculateSummationBreakdown(
            inputMode = mode,
            inputValue = value,
            samplePeriodSec = sampleSec,
            vCpuCount = vcpus
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = CalculatorEngine.calculateSummationBreakdown(SummationInputMode.SUMMATION_MS, 1600.0, 20, 8)
    )

    // Live Data Import Dialog & State
    private val _showImportDialog = MutableStateFlow(false)
    val showImportDialog: StateFlow<Boolean> = _showImportDialog.asStateFlow()

    private val _isImporting = MutableStateFlow(false)
    val isImporting: StateFlow<Boolean> = _isImporting.asStateFlow()

    private val _importStatusMessage = MutableStateFlow<String?>(null)
    val importStatusMessage: StateFlow<String?> = _importStatusMessage.asStateFlow()

    // Computed Live CPU Ready Calculation
    val cpuReadyResult: StateFlow<CpuReadyResult> = combine(
        _calcReadyMs,
        _calcSamplePeriodSec,
        _calcVcpuCount,
        _calcCoStopMs
    ) { readyMs, sampleSec, vcpus, cstpMs ->
        CalculatorEngine.calculateCpuReady(
            readyMs = readyMs,
            samplePeriodSec = sampleSec,
            vCpuCount = vcpus,
            coStopMs = cstpMs
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = CalculatorEngine.calculateCpuReady(1600.0, 20, 8, 280.0)
    )

    // Cluster Configuration from DB
    val clusterConfig: StateFlow<ClusterConfig> = clusterDao.getPrimaryCluster()
        .map { it ?: ClusterConfig() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ClusterConfig()
        )

    // VM Fleet from DB
    val vmList: StateFlow<List<VmProfile>> = vmDao.getAllVms()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Saved Reports from DB
    val savedReports: StateFlow<List<SavedReport>> = reportDao.getAllReports()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // DRS Migration Threshold (1 to 5)
    private val _drsThreshold = MutableStateFlow(3)
    val drsThreshold: StateFlow<Int> = _drsThreshold.asStateFlow()

    // Active What-If Scenario
    private val _whatIfScenario = MutableStateFlow(WhatIfScenarioType.NODE_MAINTENANCE_EVACUATION)
    val whatIfScenario: StateFlow<WhatIfScenarioType> = _whatIfScenario.asStateFlow()

    private val _selectedMaintenanceHostIndex = MutableStateFlow(0)
    val selectedMaintenanceHostIndex: StateFlow<Int> = _selectedMaintenanceHostIndex.asStateFlow()

    // UI Snack/Notification message
    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    init {
        // Initialize default cluster and sample VM fleet if empty
        viewModelScope.launch {
            val existingCluster = clusterDao.getPrimaryClusterDirect()
            if (existingCluster == null) {
                clusterDao.saveClusterConfig(ClusterConfig())
            }
            if (vmDao.getVmCount() == 0) {
                vmDao.insertVms(CalculatorEngine.generateSampleVmFleet())
            }
        }
    }

    fun toggleTheme() {
        _isDarkTheme.update { !it }
    }

    fun setSelectedTab(index: Int) {
        _selectedTab.value = index
    }

    fun updateCalcInputs(
        readyMs: Double? = null,
        samplePeriodSec: Int? = null,
        vCpuCount: Int? = null,
        coStopMs: Double? = null
    ) {
        readyMs?.let {
            _calcReadyMs.value = it
            if (_summationInputMode.value == SummationInputMode.SUMMATION_MS) {
                _summationInputValue.value = it
            }
        }
        samplePeriodSec?.let { _calcSamplePeriodSec.value = it }
        vCpuCount?.let { _calcVcpuCount.value = it }
        coStopMs?.let { _calcCoStopMs.value = it }
    }

    fun setSummationInputMode(mode: SummationInputMode) {
        _summationInputMode.value = mode
        // Synchronize input value with current calculation state
        when (mode) {
            SummationInputMode.SUMMATION_MS -> {
                _summationInputValue.value = _calcReadyMs.value
            }
            SummationInputMode.AVG_PER_VCPU_MS -> {
                val vcpu = kotlin.math.max(1, _calcVcpuCount.value)
                _summationInputValue.value = _calcReadyMs.value / vcpu
            }
            SummationInputMode.PERCENT_RDY_TARGET -> {
                val current = cpuReadyResult.value
                _summationInputValue.value = current.perVcpuReadyPercent
            }
        }
    }

    fun setSummationInputValue(value: Double) {
        val safeVal = kotlin.math.max(0.0, value)
        _summationInputValue.value = safeVal
        val sampleSec = _calcSamplePeriodSec.value
        val vcpus = _calcVcpuCount.value
        val samplePeriodMs = sampleSec * 1000.0

        // Synchronize main readyMs input
        when (_summationInputMode.value) {
            SummationInputMode.SUMMATION_MS -> {
                _calcReadyMs.value = safeVal
            }
            SummationInputMode.AVG_PER_VCPU_MS -> {
                _calcReadyMs.value = safeVal * vcpus
            }
            SummationInputMode.PERCENT_RDY_TARGET -> {
                _calcReadyMs.value = (safeVal / 100.0) * samplePeriodMs * vcpus
            }
        }
    }

    fun openImportDialog() {
        _showImportDialog.value = true
        _importStatusMessage.value = null
    }

    fun closeImportDialog() {
        _showImportDialog.value = false
        _isImporting.value = false
        _importStatusMessage.value = null
    }

    fun importFromPresetEnvironment(presetName: String) {
        viewModelScope.launch {
            _isImporting.value = true
            val presets = CalculatorEngine.getEnvironmentPresets()
            val match = presets.find { it.first.contains(presetName, true) } ?: presets.firstOrNull()
            if (match != null) {
                vmDao.deleteAllVms()
                vmDao.insertVms(match.second)
                // Also update calculator with first VM's stats
                match.second.firstOrNull()?.let { vm ->
                    _calcReadyMs.value = vm.readyTimeMs
                    _calcVcpuCount.value = vm.vCpuCount
                    _calcCoStopMs.value = vm.coStopTimeMs
                    _summationInputValue.value = vm.readyTimeMs
                }
                _userMessage.value = "Ingested live scenario: '${match.first}' (${match.second.size} VMs)"
                closeImportDialog()
            } else {
                _importStatusMessage.value = "Preset environment not found"
            }
            _isImporting.value = false
        }
    }

    fun importFromRawText(text: String) {
        viewModelScope.launch {
            _isImporting.value = true
            try {
                val cluster = clusterConfig.value
                val vms = CalculatorEngine.parseLivePerformanceData(text, cluster.nodeCount)
                if (vms.isNotEmpty()) {
                    vmDao.deleteAllVms()
                    vmDao.insertVms(vms)
                    vms.firstOrNull()?.let { vm ->
                        _calcReadyMs.value = vm.readyTimeMs
                        _calcVcpuCount.value = vm.vCpuCount
                        _calcCoStopMs.value = vm.coStopTimeMs
                        _summationInputValue.value = vm.readyTimeMs
                    }
                    _userMessage.value = "Successfully imported ${vms.size} VMs from telemetry output!"
                    closeImportDialog()
                } else {
                    _importStatusMessage.value = "Could not parse any valid VM lines. Please check format."
                }
            } catch (e: Exception) {
                _importStatusMessage.value = "Error parsing text: ${e.localizedMessage}"
            } finally {
                _isImporting.value = false
            }
        }
    }

    fun importFromVcenterRestApi(
        endpointUrl: String,
        username: String,
        sessionToken: String,
        ignoreSsl: Boolean
    ) {
        viewModelScope.launch {
            _isImporting.value = true
            _importStatusMessage.value = "Connecting to $endpointUrl..."
            val cluster = clusterConfig.value
            val result = VcenterApiService.fetchLiveVcenterVms(
                endpointUrl = endpointUrl,
                username = username,
                sessionToken = sessionToken,
                ignoreSslErrors = ignoreSsl,
                clusterNodeCount = cluster.nodeCount
            )

            when (result) {
                is VcenterApiResult.Success -> {
                    vmDao.deleteAllVms()
                    vmDao.insertVms(result.vms)
                    result.vms.firstOrNull()?.let { vm ->
                        _calcReadyMs.value = vm.readyTimeMs
                        _calcVcpuCount.value = vm.vCpuCount
                        _calcCoStopMs.value = vm.coStopTimeMs
                        _summationInputValue.value = vm.readyTimeMs
                    }
                    _userMessage.value = result.message
                    closeImportDialog()
                }
                is VcenterApiResult.Error -> {
                    _importStatusMessage.value = "${result.errorMessage}\n${result.technicalDetails ?: ""}"
                }
            }
            _isImporting.value = false
        }
    }

    fun clearImportStatusMessage() {
        _importStatusMessage.value = null
    }

    fun updateClusterConfig(config: ClusterConfig) {
        viewModelScope.launch {
            clusterDao.saveClusterConfig(config)
            _userMessage.value = "Cluster configuration updated"
        }
    }

    fun setDrsThreshold(threshold: Int) {
        _drsThreshold.value = threshold.coerceIn(1, 5)
    }

    fun applyDrsMigration(rec: DrsRecommendation) {
        viewModelScope.launch {
            val currentVms = vmList.value
            val targetVm = currentVms.find { it.id == rec.vmId }
            if (targetVm != null) {
                // Update VM's assigned node and lower ready time after vMotion
                val updatedVm = targetVm.copy(
                    assignedNodeIndex = rec.targetHostIndex,
                    readyTimeMs = (targetVm.readyTimeMs * 0.4).coerceAtLeast(200.0),
                    coStopTimeMs = (targetVm.coStopTimeMs * 0.3).coerceAtLeast(20.0)
                )
                vmDao.updateVm(updatedVm)
                _userMessage.value = "Migrated ${targetVm.name} to ${rec.targetHostName} via DRS vMotion!"
            }
        }
    }

    fun setWhatIfScenario(scenario: WhatIfScenarioType) {
        _whatIfScenario.value = scenario
    }

    fun setMaintenanceHostIndex(index: Int) {
        _selectedMaintenanceHostIndex.value = index
    }

    fun addVm(vm: VmProfile) {
        viewModelScope.launch {
            vmDao.insertVm(vm)
            _userMessage.value = "Added VM: ${vm.name}"
        }
    }

    fun updateVm(vm: VmProfile) {
        viewModelScope.launch {
            vmDao.updateVm(vm)
            _userMessage.value = "Updated VM: ${vm.name}"
        }
    }

    fun deleteVm(vm: VmProfile) {
        viewModelScope.launch {
            vmDao.deleteVm(vm)
            _userMessage.value = "Deleted VM: ${vm.name}"
        }
    }

    fun resetSampleFleet() {
        viewModelScope.launch {
            vmDao.deleteAllVms()
            vmDao.insertVms(CalculatorEngine.generateSampleVmFleet())
            _userMessage.value = "Reset to standard enterprise workload fleet"
        }
    }

    fun saveCurrentCalculationReport() {
        viewModelScope.launch {
            val result = cpuReadyResult.value
            val cluster = clusterConfig.value
            val spec = CalculatorEngine.resolveNodeSpec(cluster)
            val vms = vmList.value
            val (summary, _) = CalculatorEngine.calculateClusterSummary(cluster, vms, spec)

            val report = SavedReport(
                title = "Analysis Snapshot - ${result.vCpuCount} vCPU (${String.format("%.2f", result.perVcpuReadyPercent)}% RDY)",
                reportType = "CPU_RDY_CALC",
                cpuReadyPercent = result.perVcpuReadyPercent,
                vCpuCount = result.vCpuCount,
                samplePeriodSec = result.samplePeriodSec,
                clusterNodeCount = cluster.nodeCount,
                nodeSku = cluster.skuCode,
                overcommitRatio = summary.vCpuToPhysicalCoreRatio,
                drsImbalance = summary.clusterImbalanceStdDev,
                summaryText = "Severity: ${result.contentionSeverity.title}. Overcommit: ${String.format("%.2f", summary.vCpuToPhysicalCoreRatio)}:1 on ${cluster.nodeCount}x ${cluster.skuCode} nodes."
            )
            reportDao.insertReport(report)
            _userMessage.value = "Saved calculation snapshot report!"
        }
    }

    fun deleteReport(id: Long) {
        viewModelScope.launch {
            reportDao.deleteReportById(id)
            _userMessage.value = "Report deleted"
        }
    }

    fun clearUserMessage() {
        _userMessage.value = null
    }
}
