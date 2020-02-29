package `in`.aerem.ostrannaconfigurator

import android.app.Application
import com.polidea.rxandroidble2.RxBleClient

class OstrannaConfiguratorApplication : Application() {
    public val rxBleClient by lazy { RxBleClient.create(this) }
}