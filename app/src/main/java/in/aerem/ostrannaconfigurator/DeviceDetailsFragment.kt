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
import androidx.navigation.fragment.navArgs
import com.flask.colorpicker.ColorPickerView
import com.flask.colorpicker.builder.ColorPickerDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.jakewharton.rx3.ReplayingShare
import com.polidea.rxandroidble3.RxBleConnection
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_device_details.*


class DeviceDetailsFragment : Fragment() {
    private val args: DeviceDetailsFragmentArgs by navArgs()
    private val bleClient by lazy { (requireActivity().application as OstrannaConfiguratorApplication).rxBleClient }
    private val connectionDisposable = CompositeDisposable()
    private lateinit var connectionObservable: Observable<RxBleConnection>

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_device_details, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val mac = args.macAddress
        Log.i(TAG, "Connecting to the device with mac address: $mac")
        val device = bleClient.getBleDevice(mac)
        (activity as MainActivity).toolbar?.subtitle  = device?.name
        if (device == null) {
            Snackbar.make(view,
                "Device with the mac address $mac not found",
                Snackbar.LENGTH_LONG).show()
            parentFragmentManager.popBackStack()
            return
        }

        connectionObservable = device.establishConnection(false).compose(ReplayingShare.instance())
        connectionObservable
            .flatMap {
                it.setupNotification(BATTERY_LEVEL_UUID)
            }
            .flatMap { it }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                batteryLevel.text = "Battery level is ${it[0]}"
            }, {
                Log.e(TAG, "Error: $it")
                Snackbar.make(view,
                    "Failure when subscribing to battery level updates: $it",
                    Snackbar.LENGTH_LONG).show()
            })
            .let { connectionDisposable.add(it) }

        connectionObservable
            .flatMapSingle { it.discoverServices() }
            .flatMapSingle { it.getService(OSTRANNA_UUID) }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                Snackbar.make(view, "Connected!", Snackbar.LENGTH_SHORT).show()
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
                Log.e(TAG, "Error: $it")
                Snackbar.make(view,
                    "Failure when discovering BLE services: $it",
                    Snackbar.LENGTH_LONG).show()
                parentFragmentManager.popBackStack()
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
        (activity as MainActivity).toolbar?.subtitle = ""
    }

    private fun beep(volume: Int) {
        connectionObservable
            .flatMapSingle { it.writeCharacteristic(BEEP_UUID, byteArrayOf(volume.toByte())) }
            .subscribe({
                Snackbar.make(requireView(), "Success!", Snackbar.LENGTH_SHORT).show()
            }, {
                Snackbar.make(requireView(), "Fail!", Snackbar.LENGTH_LONG).show()
            })
            .let { connectionDisposable.add(it) }
    }

    private fun blink() {
        connectionObservable
            .flatMapSingle { it.writeCharacteristic(BLINK_UUID, byteArrayOf(0)) }
            .subscribe({
                Snackbar.make(requireView(), "Success!", Snackbar.LENGTH_SHORT).show()
            }, {
                Snackbar.make(requireView(), "Fail!", Snackbar.LENGTH_LONG).show()
            })
            .let { connectionDisposable.add(it) }
    }

    private fun setColor(color: Int) {
        connectionObservable
            .flatMapSingle { it.writeCharacteristic(COLOR_UUID, byteArrayOf(color.red.toByte(), color.green.toByte(), color.blue.toByte())) }
            .subscribe({
                Snackbar.make(requireView(), "Success!", Snackbar.LENGTH_SHORT).show()
            }, {
                Snackbar.make(requireView(), "Fail!", Snackbar.LENGTH_LONG).show()
            })
            .let { connectionDisposable.add(it) }
    }

    companion object {
        private const val TAG = "DeviceDetailsFragment"
    }
}
