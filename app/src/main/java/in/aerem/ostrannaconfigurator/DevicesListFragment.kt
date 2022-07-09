package `in`.aerem.ostrannaconfigurator

import android.content.pm.PackageManager
import android.os.Bundle
import android.os.ParcelUuid
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.polidea.rxandroidble3.scan.ScanFilter
import com.polidea.rxandroidble3.scan.ScanResult
import com.polidea.rxandroidble3.scan.ScanSettings
import io.reactivex.rxjava3.disposables.Disposable
import kotlinx.android.synthetic.main.device_list_item.view.*
import kotlinx.android.synthetic.main.fragment_device_list.*
import java.util.*


class DevicesListFragment : Fragment() {
    companion object {
        const val PERMISSIONS_REQUEST_LOCATION = 1
        const val TAG = "DevicesListFragment"
    }

    private lateinit var scanSubscription: Disposable

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_device_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        startScan()
    }

    override fun onDestroy() {
        super.onDestroy()
        scanSubscription.dispose()
    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            PERMISSIONS_REQUEST_LOCATION -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    startScan()
                }
            }
        }
    }

    private fun startScan() {
        Log.i(TAG, "Starting bluetooth scan")
        val adapter = SimpleItemRecyclerViewAdapter(this)
        item_list.adapter = adapter
        val scanSettings = ScanSettings.Builder()
            .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        val scanFilter = ScanFilter.Builder()
            .setServiceUuid( ParcelUuid(OSTRANNA_UUID))
            .build()
        scanSubscription = (requireActivity().application as OstrannaConfiguratorApplication).rxBleClient
            .scanBleDevices(scanSettings, scanFilter).subscribe(
                {
                    if (it.bleDevice.name != null) adapter.addResult(it)
                },
                { Log.e(TAG, "Error: ${it.message}") }
            )
    }

    class SimpleItemRecyclerViewAdapter(private val parentFragment: DevicesListFragment) :
            RecyclerView.Adapter<SimpleItemRecyclerViewAdapter.ViewHolder>() {

        private val onClickListener: View.OnClickListener
        private var items: ArrayList<ScanResult> = arrayListOf()

        init {
            onClickListener = View.OnClickListener { v ->
                val item = v.tag as ScanResult
                connectTo(item.bleDevice.macAddress)
            }
        }

        fun addResult(r: ScanResult) {
            val existing = this.items.indexOfFirst { it.bleDevice.macAddress == r.bleDevice.macAddress }
            if (existing < 0) {
                this.items.add(r)
                notifyItemInserted(this.items.size - 1)
            } else {
                this.items[existing] = r
                notifyItemChanged(existing)
            }

            if (r.rssi > -42) {
                connectTo(r.bleDevice.macAddress)
            }
        }

        private fun connectTo(address: String) {
            val action = DevicesListFragmentDirections.actionDeviceListFragmentToDeviceDetailsFragment(address)
            parentFragment.findNavController().navigate(action)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.device_list_item, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            holder.mainTextView.text = item.bleDevice.name
            holder.subTextView.text = item.bleDevice.macAddress
            holder.rssiView.text = item.rssi.toString()

            with(holder.itemView) {
                tag = item
                setOnClickListener(onClickListener)
            }
        }

        override fun getItemCount() = items.size

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val mainTextView: TextView = view.mainText
            val subTextView: TextView = view.subText
            val rssiView: TextView = view.rssi
        }
    }
}
