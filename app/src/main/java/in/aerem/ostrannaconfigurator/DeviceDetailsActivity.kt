package `in`.aerem.ostrannaconfigurator

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_item_detail.*

class DeviceDetailsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_item_detail)
        setSupportActionBar(detail_toolbar)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        if (savedInstanceState == null) {
            // Create the detail fragment and add it to the activity
            // using a fragment transaction.
            val fragment = DeviceDetailsFragment().apply {
                arguments = Bundle().apply {
                    putString(DeviceDetailsFragment.ARG_MAC_ADDRESS,
                            intent.getStringExtra(DeviceDetailsFragment.ARG_MAC_ADDRESS))
                }
            }

            supportFragmentManager.beginTransaction()
                    .add(R.id.item_detail_container, fragment)
                    .commit()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem) =
            when (item.itemId) {
                android.R.id.home -> {
                    navigateUpTo(Intent(this, DevicesListActivity::class.java))
                    true
                }
                else -> super.onOptionsItemSelected(item)
            }
}
