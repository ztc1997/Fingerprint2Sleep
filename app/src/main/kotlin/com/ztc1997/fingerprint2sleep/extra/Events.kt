package com.ztc1997.fingerprint2sleep.extra

import android.view.accessibility.AccessibilityEvent

object FinishStartFPQAActivityEvent

object RestartScanningDelayedEvent

object StartScanningEvent

class ActivityChangedEvent(val event: AccessibilityEvent)

class IsScanningChangedEvent(val value: Boolean)

class PerformGlobalActionEvent(val action: Int)
