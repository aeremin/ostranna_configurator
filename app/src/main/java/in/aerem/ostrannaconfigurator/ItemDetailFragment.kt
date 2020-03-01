package `in`.aerem.ostrannaconfigurator

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar
import com.jakewharton.rx.ReplayingShare
import com.polidea.rxandroidble2.RxBleConnection
import com.polidea.rxandroidble2.RxBleDevice
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import kotlinx.android.synthetic.main.activity_item_detail.*
import kotlinx.android.synthetic.main.item_detail.*
import java.util.*

/**
 * A fragment representing a single Item detail screen.
 * This fragment is either contained in a [ItemListActivity]
 * in two-pane mode (on tablets) or a [ItemDetailActivity]
 * on handsets.
 */
class ItemDetailFragment : Fragment() {
    private val bleClient by lazy { (activity!!.application as OstrannaConfiguratorApplication).rxBleClient }
    private val TAG = "ItemDetailFragment"
    private var device: RxBleDevice? = null
    private val connectionDisposable = CompositeDisposable()
    private lateinit var connectionObservable: Observable<RxBleConnection>

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.item_detail, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val mac = arguments!!.getString(ARG_ITEM_ID)!!
        device = bleClient.getBleDevice(mac)
        activity?.toolbar_layout?.title = device?.name
        if (device == null) {
            Log.e(TAG, "Device with the mac address $mac not found")
            return
        }

        connectionObservable = device!!.establishConnection(false).compose(ReplayingShare.instance())
        connectionObservable
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe{
                beepBlock.visibility = View.VISIBLE
                progressBar.visibility = View.GONE
                Snackbar.make(view, "Connected!", Snackbar.LENGTH_SHORT)
            }
            .let { connectionDisposable.add(it) }

        connectionObservable
            .flatMap {
                it.setupNotification(BATTERY_LEVEL_UUID)
            }
            .flatMap { it }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                batteryLevel.text = "Battery level is ${it[0]}"
            }, {
                Log.e(TAG, "Error: ${it}")
                Snackbar.make(view, "Error: ${it}", Snackbar.LENGTH_LONG)
            })
            .let { connectionDisposable.add(it) }

        buttonBeepQuiet.setOnClickListener { beep(3)  }
        buttonBeepNormal.setOnClickListener { beep(100)  }
        buttonBeepLoud.setOnClickListener { beep(255)  }
    }

    override fun onDestroy() {
        super.onDestroy()
        connectionDisposable.clear()
    }

    private fun beep(volume: Int) {
        connectionObservable
            .flatMapSingle { it.writeCharacteristic(BEEP_UUID, byteArrayOf(volume.toByte())) }
            .subscribe().let { connectionDisposable.add(it) }
    }
    companion object {
        const val ARG_ITEM_ID = "item_id"
        val BATTERY_LEVEL_UUID: UUID = UUID.fromString("00002a19-0000-1000-8000-00805f9b34fb")
        val BEEP_UUID: UUID = UUID.fromString("8ec87062-8865-4eca-82e0-2ea8e45e8221")
        val BLINK_UUID: UUID = UUID.fromString("8ec87063-8865-4eca-82e0-2ea8e45e8221")
    }
}
