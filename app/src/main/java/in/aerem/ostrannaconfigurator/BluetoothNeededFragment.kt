package `in`.aerem.ostrannaconfigurator

import android.Manifest
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar

class BluetoothNeededFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_bluetooth_needed, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requestBluetoothPermissions()
    }

    private fun requestBluetoothPermissions() {
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            permissions.entries.forEach {
                Log.d("MainActivity", "${it.key} = ${it.value}")
            }
            if (permissions[Manifest.permission.BLUETOOTH_SCAN] == true &&
                permissions[Manifest.permission.BLUETOOTH_CONNECT] == true) {
                findNavController().navigate(BluetoothNeededFragmentDirections.actionFragmentBluetoothNeededToDeviceListFragment())
            } else {
                Snackbar.make(requireView(), "Without bluetooth nothing will ever work, please restart the app and provide a permission!", Snackbar.LENGTH_LONG).show()
            }
        }.launch(arrayOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT))
    }
}