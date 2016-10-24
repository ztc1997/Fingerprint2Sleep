package com.ztc1997.fingerprint2sleep.extra

import android.content.Context

object FinishStartFPQAActivityEvent

object ActivityChangedEvent

data class IsScanningChangedEvent(val value: Boolean)

data class PerformGlobalActionEvent(val action: Int)

data class StartVerifyEvent(val ctx: Context)

data class SendPackageManagerEvent(val any: Any)

data class SendPackageInfoEvent(val any: Any)

data class SendSignatureEvent(val any: Any)

data class CompleteHashCodeEvent(val any: Any)

data class SendByteArrayEvent(val byteArray: ByteArray)