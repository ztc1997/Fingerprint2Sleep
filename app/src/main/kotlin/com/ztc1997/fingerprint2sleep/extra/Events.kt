package com.ztc1997.fingerprint2sleep.extra

import android.view.accessibility.AccessibilityEvent

object FinishStartFPQAActivityEvent

object RestartScanningDelayedEvent

object RestartScanningEvent

object StartScanningEvent

data class ActivityChangedEvent(val event: AccessibilityEvent)

data class IsScanningChangedEvent(val value: Boolean)

data class PerformGlobalActionEvent(val action: Int)
