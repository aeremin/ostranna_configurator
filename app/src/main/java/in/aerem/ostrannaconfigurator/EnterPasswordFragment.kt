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
import com.google.android.material.snackbar.Snackbar
import com.jakewharton.rx3.ReplayingShare
import com.polidea.rxandroidble3.RxBleConnection
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import kotlinx.android.synthetic.main.fragment_enter_password.*

enum class DeviceKind {
    Unsupported,
    OstrannaFirefly,
    ElkBleDom,
}

class EnterPasswordFragment : Fragment() {
    private val args: EnterPasswordFragmentArgs by navArgs()
    private var deviceKind = DeviceKind.Unsupported
    private val bleClient by lazy { (requireActivity().application as OstrannaConfiguratorApplication).rxBleClient }
    private val connectionDisposable = CompositeDisposable()
    private lateinit var connectionObservable: Observable<RxBleConnection>


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_enter_password, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val mac = args.macAddress
        Log.i(TAG, "Connecting to the device with mac address: $mac")
        val device = bleClient.getBleDevice(mac)

        if (device == null) {
            Snackbar.make(view,
                "Device with the mac address $mac not found",
                Snackbar.LENGTH_LONG).show()
            parentFragmentManager.popBackStack()
            return
        }

        connectionObservable = device.establishConnection(false).compose(ReplayingShare.instance())
        connectionObservable
            .flatMapSingle { it.discoverServices() }
            .subscribe {
                for (service in it.bluetoothGattServices) {
                    if (service.uuid == OSTRANNA_UUID) {
                        deviceKind = DeviceKind.OstrannaFirefly
                    } else if (service.uuid == ELK_BLEDOM_SERVICE_UUID) {
                        deviceKind = DeviceKind.ElkBleDom
                    }
                }
            }.let { connectionDisposable.add(it) }

        try_code.setOnClickListener {
            if (code.text.toString() == "549") {
                setColor(Color.GREEN)
            } else {
                setColor(Color.RED)
            }
        }
    }

    private fun setColor(color: Int) {
        connectionObservable
            .flatMapSingle {
                when (deviceKind) {
                    DeviceKind.OstrannaFirefly -> {
                        it.writeCharacteristic(
                            COLOR_UUID,
                            byteArrayOf(color.red.toByte(), color.green.toByte(), color.blue.toByte())
                        )
                    }
                    DeviceKind.ElkBleDom -> {
                        // See https://github.com/FergusInLondon/ELK-BLEDOM/blob/master/PROTCOL.md#data-changing-colour
                        it.writeCharacteristic(
                            ELK_BLEDOM_CHARACTERISTIC_UUID,
                            byteArrayOf(0x7E, 0x00, 0x05, 0x03, color.red.toByte(), color.green.toByte(), color.blue.toByte(), 0x00,
                                0xEF.toByte()
                            )
                        )
                    }
                    DeviceKind.Unsupported -> {
                        throw Exception("Unknown device kind")
                    }
                }
            }
            .subscribe({
                Snackbar.make(requireView(), "Success!", Snackbar.LENGTH_SHORT).show()
            }, {
                Snackbar.make(requireView(), "Fail! ${it.message}", Snackbar.LENGTH_LONG).show()
            })
            .let { connectionDisposable.add(it) }
    }

    override fun onDestroy() {
        super.onDestroy()
        connectionDisposable.clear()
    }

    companion object {
        private const val TAG = "EnterPasswordFragment"
    }
}