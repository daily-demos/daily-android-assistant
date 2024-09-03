package co.daily.bots.assistant

import android.app.Application
import co.daily.bots.assistant.tools.Tools

class RTVIApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        System.loadLibrary("native-lib")
        Preferences.initAppStart(this)
        Tools.init(this)
    }
}