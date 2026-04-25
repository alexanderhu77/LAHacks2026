package com.lahacks2026.pretriage

import android.app.Application
import android.util.Log
import com.lahacks2026.pretriage.ml.FakeMelangeRuntime
import com.lahacks2026.pretriage.ml.RuntimeProvider

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        val start = System.currentTimeMillis()
        RuntimeProvider.init(FakeMelangeRuntime())
        val elapsed = System.currentTimeMillis() - start
        Log.i("PreTriage", "RuntimeProvider initialized with FakeMelangeRuntime in ${elapsed}ms")
    }
}
