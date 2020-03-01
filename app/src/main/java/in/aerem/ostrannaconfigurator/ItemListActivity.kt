package `in`.aerem.ostrannaconfigurator

import android.content.Intent
import android.os.Bundle
import android.os.ParcelUuid
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.microsoft.appcenter.AppCenter
import com.microsoft.appcenter.analytics.Analytics
import com.microsoft.appcenter.crashes.Crashes
import com.polidea.rxandroidble2.scan.ScanFilter
import com.polidea.rxandroidble2.scan.ScanResult
import com.polidea.rxandroidble2.scan.ScanSettings
import io.reactivex.disposables.Disposable
import kotlinx.android.synthetic.main.activity_item_list.*
import kotlinx.android.synthetic.main.item_list.*
import kotlinx.android.synthetic.main.item_list_content.view.*
import java.util.*


/**
 * An activity representing a list of Pings. This activity
 * has different presentations for handset and tablet-size devices. On
 * handsets, the activity presents a list of items, which when touched,
 * lead to a [ItemDetailActivity] representing
 * item details. On tablets, the activity presents the list of items and
 * item details side-by-side using two vertical panes.
 */
class ItemListActivity : AppCompatActivity() {
    private val TAG = "ItemListActivity"
    /**
     * Whether or not the activity is in two-pane mode, i.e. running on a tablet
     * device.
     */
    private var twoPane: Boolean = false

    private lateinit var scanSubscription: Disposable

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_item_list)

        setSupportActionBar(toolbar)
        toolbar.title = title

        if (item_detail_container != null) {
            // The detail container view will be present only in the
            // large-screen layouts (res/values-w900dp).
            // If this view is present, then the
            // activity should be in two-pane mode.
            twoPane = true
        }

        AppCenter.start(
            application, "1b4861b1-8f51-47b5-9402-d41d5f5e16ae",
            Analytics::class.java, Crashes::class.java
        )

        val adapter = SimpleItemRecyclerViewAdapter(this, twoPane)
        item_list.adapter = adapter

        var scanSettings = ScanSettings.Builder()
            .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        var scanFilter = ScanFilter.Builder()
            .setServiceUuid( ParcelUuid(OSTRANNA_UUID))
            .build()
        scanSubscription = (application as OstrannaConfiguratorApplication).rxBleClient
            .scanBleDevices(scanSettings, scanFilter).subscribe(
            {
                if (it.bleDevice.name != null) adapter.addResult(it)
            },
            { Log.e(TAG, "Error: ${it.message}") }
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        scanSubscription.dispose()
    }

    class SimpleItemRecyclerViewAdapter(private val parentActivity: ItemListActivity,
                                        private val twoPane: Boolean) :
            RecyclerView.Adapter<SimpleItemRecyclerViewAdapter.ViewHolder>() {

        private val onClickListener: View.OnClickListener
        private var items: ArrayList<ScanResult> = arrayListOf()

        init {
            onClickListener = View.OnClickListener { v ->
                val item = v.tag as ScanResult
                if (twoPane) {
                    val fragment = ItemDetailFragment().apply {
                        arguments = Bundle().apply {
                            putString(ItemDetailFragment.ARG_ITEM_ID, item.bleDevice.macAddress)
                        }
                    }
                    parentActivity.supportFragmentManager
                            .beginTransaction()
                            .replace(R.id.item_detail_container, fragment)
                            .commit()
                } else {
                    val intent = Intent(v.context, ItemDetailActivity::class.java).apply {
                        putExtra(ItemDetailFragment.ARG_ITEM_ID, item.bleDevice.macAddress)
                    }
                    v.context.startActivity(intent)
                }
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
            // this.items.sortByDescending { it.bleDevice.name }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_list_content, parent, false)
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
