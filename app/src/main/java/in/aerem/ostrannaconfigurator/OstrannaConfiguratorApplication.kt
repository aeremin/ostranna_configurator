package `in`.aerem.ostrannaconfigurator

import android.app.Application
import com.polidea.rxandroidble3.RxBleClient

class OstrannaConfiguratorApplication : Application() {
    val rxBleClient: RxBleClient by lazy { RxBleClient.create(this) }
}