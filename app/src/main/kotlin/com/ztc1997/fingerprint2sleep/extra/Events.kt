package com.ztc1997.fingerprint2sleep.extra

import android.content.Context

object FinishStartFPQAActivityEvent

object ActivityChangedEvent

object RestartScanningDelayedEvent

object StartScanningEvent

class IsScanningChangedEvent(val value: Boolean)

class PerformGlobalActionEvent(val action: Int)

class StartVerifyEvent(val ctx: Context)

class SendPackageManagerEvent(val any: Any)

class SendPackageInfoEvent(val any: Any)

class SendSignatureEvent(val any: Any)

class CompleteHashCodeEvent(val any: Any)

class SendByteArrayEvent(val byteArray: ByteArray)