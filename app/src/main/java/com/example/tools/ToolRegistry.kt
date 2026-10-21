package com.example.tools

import com.example.data.model.ToolDefinition

object ToolRegistry {
    val tools = listOf(
        ToolDefinition(
            toolId = "app_explorer",
            displayName = "Installed App Explorer",
            description = "Discover installed applications subject to Android package visibility rules, view versions and installer info.",
            requiredPermissions = emptyList(),
            requiresShizuku = false,
            dataSources = listOf("Android PackageManager"),
            isReadOnly = true,
            iconName = "apps"
        ),
        ToolDefinition(
            toolId = "permission_inspector",
            displayName = "Permission Inspector",
            description = "Inspect requested and granted permissions across apps, view permission groups and explanations.",
            requiredPermissions = emptyList(),
            requiresShizuku = false,
            dataSources = listOf("PackageManager.getPermissionInfo"),
            isReadOnly = true,
            iconName = "security"
        ),
        ToolDefinition(
            toolId = "storage_explorer",
            displayName = "Storage Explorer",
            description = "Browse shared storage and user-authorized directories with file metadata and size inspection.",
            requiredPermissions = listOf("android.permission.READ_EXTERNAL_STORAGE"),
            requiresShizuku = false,
            dataSources = listOf("Android Storage Access / Files"),
            isReadOnly = true,
            iconName = "folder"
        ),
        ToolDefinition(
            toolId = "transparency_dashboard",
            displayName = "App Data Transparency",
            description = "Categorize app data accessibility into public, system metadata, shared files, and sandbox restrictions.",
            requiredPermissions = emptyList(),
            requiresShizuku = false,
            dataSources = listOf("Sandbox Security Audit"),
            isReadOnly = true,
            iconName = "visibility"
        ),
        ToolDefinition(
            toolId = "shizuku_diagnostics",
            displayName = "Shizuku Diagnostics",
            description = "Check Shizuku service status, binder availability, authorization state, and execute read-only shell commands.",
            requiredPermissions = emptyList(),
            requiresShizuku = true,
            dataSources = listOf("Shizuku Binder API"),
            isReadOnly = true,
            iconName = "bolt"
        ),
        ToolDefinition(
            toolId = "educational_demo",
            displayName = "Educational Prank Mode",
            description = "Toggle harmless demonstration mode with simulated scan data and interactive permission walkthroughs.",
            requiredPermissions = emptyList(),
            requiresShizuku = false,
            dataSources = listOf("Demo Dataset"),
            isReadOnly = true,
            iconName = "school"
        ),
        ToolDefinition(
            toolId = "export_reports",
            displayName = "Export & Reports",
            description = "Export scan results as JSON and CSV reports with custom field selection and local storage control.",
            requiredPermissions = emptyList(),
            requiresShizuku = false,
            dataSources = listOf("Local Report Storage"),
            isReadOnly = true,
            iconName = "download"
        )
    )

    fun getTool(toolId: String): ToolDefinition? {
        return tools.find { it.toolId == toolId }
    }
}
