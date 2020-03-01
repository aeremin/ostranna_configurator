package `in`.aerem.ostrannaconfigurator

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.graphics.blue
import androidx.core.graphics.green
import androidx.core.graphics.red
import androidx.fragment.app.Fragment
import com.flask.colorpicker.ColorPickerView
import com.flask.colorpicker.builder.ColorPickerDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.jakewharton.rx.ReplayingShare
import com.polidea.rxandroidble2.RxBleConnection
import com.polidea.rxandroidble2.RxBleDevice
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import kotlinx.android.synthetic.main.activity_item_detail.*
import kotlinx.android.synthetic.main.item_detail.*


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

        connectionObservable
            .flatMapSingle { it.discoverServices() }
            .flatMapSingle { it.getService(OSTRANNA_UUID) }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                Snackbar.make(view, "Connected!", Snackbar.LENGTH_SHORT)
                connectedBlock.visibility = View.VISIBLE
                progressBar.visibility = View.GONE
                for (ch in it.characteristics) {
                    if (ch.uuid == COLOR_UUID) buttonColor.isEnabled = true
                    if (ch.uuid == BEEP_UUID) {
                        buttonBeepQuiet.isEnabled = true
                        buttonBeepNormal.isEnabled = true
                        buttonBeepLoud.isEnabled = true
                    }
                    if (ch.uuid == BLINK_UUID) buttonBlink.isEnabled = true
                }
            }, {
                Log.e(TAG, "Error: ${it}")
                Snackbar.make(view, "Error: ${it}", Snackbar.LENGTH_LONG)
            }).let { connectionDisposable.add(it) }

        buttonBeepQuiet.setOnClickListener { beep(3)  }
        buttonBeepNormal.setOnClickListener { beep(100)  }
        buttonBeepLoud.setOnClickListener { beep(255)  }
        buttonBlink.setOnClickListener { blink() }

        buttonColor.setOnClickListener {
            ColorPickerDialogBuilder
                .with(context)
                .setTitle("Choose color")
                .initialColor(Color.parseColor("#00ff00"))
                .lightnessSliderOnly()
                .wheelType(ColorPickerView.WHEEL_TYPE.FLOWER)
                .density(12)
                .setPositiveButton(
                    "OK"
                ) { _, selectedColor, _ -> setColor(0xFFFFFF and selectedColor) }
                .setNegativeButton(
                    "Cancel"
                ) { _, _ -> }
                .build()
                .show()
        }
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

    private fun blink() {
        connectionObservable
            .flatMapSingle { it.writeCharacteristic(BLINK_UUID, byteArrayOf(0)) }
            .subscribe().let { connectionDisposable.add(it) }
    }

    private fun setColor(color: Int) {
        connectionObservable
            .flatMapSingle { it.writeCharacteristic(COLOR_UUID, byteArrayOf(color.red.toByte(), color.green.toByte(), color.blue.toByte())) }
            .subscribe().let { connectionDisposable.add(it) }
    }

    companion object {
        const val ARG_ITEM_ID = "item_id"
    }
}
