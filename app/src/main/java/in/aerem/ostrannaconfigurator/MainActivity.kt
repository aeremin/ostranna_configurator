package `in`.aerem.ostrannaconfigurator

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.microsoft.appcenter.AppCenter
import com.microsoft.appcenter.analytics.Analytics
import com.microsoft.appcenter.crashes.Crashes
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        AppCenter.start(
            application, "1b4861b1-8f51-47b5-9402-d41d5f5e16ae",
            Analytics::class.java, Crashes::class.java
        )

        setSupportActionBar(toolbar)
        toolbar.title = title
    }
}
