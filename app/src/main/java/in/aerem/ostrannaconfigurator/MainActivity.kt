package `in`.aerem.ostrannaconfigurator

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import com.microsoft.appcenter.AppCenter
import com.microsoft.appcenter.analytics.Analytics
import com.microsoft.appcenter.crashes.Crashes
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setUpAppCenterIntegration()
        setUpToolbarAndNavigationIntegration()
    }

    private fun setUpAppCenterIntegration() {
        // See instruction at https://appcenter.ms/users/a.eremin.msu/apps/Ostranna-Configurator.
        AppCenter.start(
            application, "1b4861b1-8f51-47b5-9402-d41d5f5e16ae",
            Analytics::class.java, Crashes::class.java
        )
    }

    private fun setUpToolbarAndNavigationIntegration() {
        // See https://issuetracker.google.com/issues/142847973 - this is not a recommended way
        // to get navController, but nothing else works if it's added to the layout via
        // androidx.fragment.app.FragmentContainerView.
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        val appBarConfiguration = AppBarConfiguration(navController.graph)
        toolbar.setupWithNavController(navController, appBarConfiguration)
    }
}
