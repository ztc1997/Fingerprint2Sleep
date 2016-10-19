package com.ztc1997.fingerprint2sleep.extra

object FinishStartFPQAActivityEvent

object ActivityChangedEvent

data class IsScanningChangedEvent(val value: Boolean)

data class PerformGlobalActionEvent(val action: Int)
