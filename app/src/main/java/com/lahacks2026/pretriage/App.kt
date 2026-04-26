package com.lahacks2026.pretriage

import android.app.Application
import android.util.Log
import com.lahacks2026.pretriage.ml.MelangeRuntimeImpl
import com.lahacks2026.pretriage.ml.RuntimeProvider

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        val start = System.currentTimeMillis()
        // Real Qwen-backed runtime. Construction is cheap (model load is deferred
        // until warmUp() is called from Splash). Keep this synchronous — Splash
        // owns the actual download/warmup lifecycle.
        RuntimeProvider.init(MelangeRuntimeImpl(this))
        val elapsed = System.currentTimeMillis() - start
        Log.i("PreTriage", "RuntimeProvider initialized with MelangeRuntimeImpl in ${elapsed}ms")
    }
}
