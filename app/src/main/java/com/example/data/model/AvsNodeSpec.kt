package com.example.data.model

/**
 * Standard Azure VMware Solution (AVS) Node SKU Hardware Specifications
 */
data class AvsNodeSpec(
    val skuCode: String,
    val displayName: String,
    val processorName: String,
    val sockets: Int,
    val coresPerSocket: Int,
    val physicalCores: Int,
    val logicalCores: Int, // with Hyperthreading (HT = 2 threads/core)
    val totalRamGb: Int,
    val rawStorageTb: Double,
    val baseClockGhz: Double,
    val systemReservedCores: Int = 2,
    val systemReservedRamGb: Int = 36,
    val isCustom: Boolean = false,
    val description: String = ""
) {
    val usablePhysicalCores: Int
        get() = (physicalCores - systemReservedCores).coerceAtLeast(1)

    val usableLogicalCores: Int
        get() = ((physicalCores - systemReservedCores) * 2).coerceAtLeast(2)

    val usableRamGb: Int
        get() = (totalRamGb - systemReservedRamGb).coerceAtLeast(1)

    companion object {
        val AV36 = AvsNodeSpec(
            skuCode = "AV36",
            displayName = "AV36 (Intel Xeon Gold 6140)",
            processorName = "Intel Xeon Gold 6140 @ 2.3 GHz",
            sockets = 2,
            coresPerSocket = 18,
            physicalCores = 36,
            logicalCores = 72,
            totalRamGb = 576,
            rawStorageTb = 15.36,
            baseClockGhz = 2.3,
            systemReservedCores = 2,
            systemReservedRamGb = 36,
            description = "Standard AVS SKU. 36 cores, 576 GB RAM, 2x Intel 6140 Skylake."
        )

        val AV36T = AvsNodeSpec(
            skuCode = "AV36t",
            displayName = "AV36t (Intel Xeon Gold 6240)",
            processorName = "Intel Xeon Gold 6240 @ 2.6 GHz",
            sockets = 2,
            coresPerSocket = 18,
            physicalCores = 36,
            logicalCores = 72,
            totalRamGb = 576,
            rawStorageTb = 15.36,
            baseClockGhz = 2.6,
            systemReservedCores = 2,
            systemReservedRamGb = 36,
            description = "Cascade Lake refresh. 36 cores, 576 GB RAM, higher 2.6 GHz base clock."
        )

        val AV52 = AvsNodeSpec(
            skuCode = "AV52",
            displayName = "AV52 (Intel Xeon Platinum 8270)",
            processorName = "Intel Xeon Platinum 8270 @ 2.7 GHz",
            sockets = 2,
            coresPerSocket = 26,
            physicalCores = 52,
            logicalCores = 104,
            totalRamGb = 1536,
            rawStorageTb = 38.4,
            baseClockGhz = 2.7,
            systemReservedCores = 4,
            systemReservedRamGb = 64,
            description = "High Memory & Compute SKU. 52 cores, 1.5 TB RAM, for SAP HANA and large DBs."
        )

        val AV64 = AvsNodeSpec(
            skuCode = "AV64",
            displayName = "AV64 (Intel Xeon Platinum 8470C)",
            processorName = "Intel Xeon Platinum 8470C @ 2.0 GHz",
            sockets = 2,
            coresPerSocket = 32,
            physicalCores = 64,
            logicalCores = 128,
            totalRamGb = 1024,
            rawStorageTb = 30.72,
            baseClockGhz = 2.0,
            systemReservedCores = 4,
            systemReservedRamGb = 48,
            description = "Latest Gen AVS SKU. 64 cores, 1 TB RAM, Sapphire Rapids architecture."
        )

        val ALL_PRESETS = listOf(AV36, AV36T, AV52, AV64)

        fun createCustom(
            name: String,
            sockets: Int,
            coresPerSocket: Int,
            totalRamGb: Int,
            baseClockGhz: Double,
            reservedCores: Int = 2,
            reservedRamGb: Int = 32
        ): AvsNodeSpec {
            val pCores = sockets * coresPerSocket
            return AvsNodeSpec(
                skuCode = "CUSTOM",
                displayName = name.ifBlank { "Custom Host (${pCores} Cores, ${totalRamGb}GB)" },
                processorName = "Custom ESXi Processor @ ${baseClockGhz} GHz",
                sockets = sockets,
                coresPerSocket = coresPerSocket,
                physicalCores = pCores,
                logicalCores = pCores * 2,
                totalRamGb = totalRamGb,
                rawStorageTb = 10.0,
                baseClockGhz = baseClockGhz,
                systemReservedCores = reservedCores,
                systemReservedRamGb = reservedRamGb,
                isCustom = true,
                description = "User-defined custom ESXi host specifications."
            )
        }
    }
}
