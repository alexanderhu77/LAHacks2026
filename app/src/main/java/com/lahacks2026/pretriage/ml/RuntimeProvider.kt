package com.lahacks2026.pretriage.ml

object RuntimeProvider {
    @Volatile
    private var _runtime: MelangeRuntime? = null

    val runtime: MelangeRuntime
        get() = _runtime ?: error("RuntimeProvider not initialized — call init() in Application.onCreate()")

    fun init(runtime: MelangeRuntime) {
        _runtime = runtime
    }

    /** Test-only reset hook. */
    internal fun resetForTest() {
        _runtime = null
    }
}
