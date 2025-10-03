package com.testing.finalprojects.fragments

import android.location.Geocoder
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.core.os.bundleOf
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.testing.finalprojects.R
import com.testing.finalprojects.databinding.FragmentSelectLocationBinding
import java.util.Locale

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [SelectLocationFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class SelectLocationFragment  (     private val showConfirmButton: Boolean = true // للتحكم في ظهور زر التأكيد
) : Fragment(), OnMapReadyCallback {
    /*
    هو Fragment مخصص لعرض خريطة Google Maps عشان المستخدم يحدد موقع (Location).
يعتمد على SupportMapFragment داخل الـ Layout.
يسمح للمستخدم
يشوف Marker على الموقع الافتراضي أو الموقع اللي تم تمريره مسبقًا.
يضغط على الخريطة لتغيير الموقع.
يضغط زر "تأكيد" عشان يرجع الإحداثيات والاسم للـ Fragment اللي استدعاه.

---------------------------------------------------
دمج Google Maps
يعني انا بتستدعي SupportMapFragment من جوا الـ Fragment نفسه
val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
mapFragment.getMapAsync(this)
وبتطبّق OnMapReadyCallback عشان تمسك GoogleMap وتتحكّم فيه (تكبر، تصغر، تحط Markers).

 تبادل الداتا بين الشاشات
بدال ما نستخدم SafeArgs أو Bundles بالطريقة التقليدية، انا عاملها أنضف شوي.
بيستعمل setFragmentResult عشان يرجّع الإحداثيات للشاشة التانية

parentFragmentManager.setFragmentResult(
    "locationRequest",
    bundleOf(
        "lat" to latLng.latitude,
        "lng" to latLng.longitude,
        "locationName" to locationName
    )
)
هيك أي Fragment تاني فاتح Listener ع هالـ Key "locationRequest" رح يستقبل الداتا.

 زر "تأكيد الموقع"

في الكلاس نفسه حاطط باراميتر
class SelectLocationFragment(private val showConfirmButton: Boolean = true)
لو true الزر يبين ويخلي المستخدم يؤكد اللوكيشن.
لو false الزر ما يبين، يعني الخريطة بس للعرض.

 الموقع الافتراضي (القاهرة)
لو المستخدم ما عطاك إحداثيات  الكود بروح على طول بيحط Marker بالقاهرة (الديفولت).
وكمان بيرجع هاللوكيشن للشاشة اللي طلبته.

التعامل مع كليك عالخريطة
أول ما المستخدم يكبس على مكان بالخريطة بيمسح الـ Markers القديمة.
بيضيف Marker جديدة بنفس النقطة.
وبخزن الإحداثيات بمتغير selectedLatLng.

     */

    private var _binding: FragmentSelectLocationBinding? = null
    private val binding get() = _binding!!

    private lateinit var gMap: GoogleMap
    private var selectedLatLng: LatLng? = null
    private var locationName: String = "" // لتخزين اسم الموقع

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSelectLocationBinding.inflate(inflater, container, false)

        val lat = arguments?.getDouble("lat")
        val lng = arguments?.getDouble("lng")
        if (lat != null && lng != null) {
            selectedLatLng = LatLng(lat, lng)
            locationName = " Location"
        }

        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        binding.btnConfirmLocation.visibility = if (showConfirmButton) View.VISIBLE else View.GONE

        binding.btnConfirmLocation.setOnClickListener {
            selectedLatLng?.let { latLng ->
                parentFragmentManager.setFragmentResult(
                    "locationRequest",
                    bundleOf(
                        "lat" to latLng.latitude,
                        "lng" to latLng.longitude,
                        "locationName" to locationName
                    )
                )
                parentFragmentManager.popBackStack()
            }
        }

        binding.fabBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        return binding.root
    }

    override fun onMapReady(map: GoogleMap) {
        gMap = map
        gMap.uiSettings.isZoomControlsEnabled = true

        if (selectedLatLng != null) {
            gMap.addMarker(MarkerOptions().position(selectedLatLng!!).title(locationName))
            gMap.animateCamera(CameraUpdateFactory.newLatLngZoom(selectedLatLng!!, 16f))
        } else {
//            الديفلت لوكيشن في مصر
            val cairo = LatLng(30.0444, 31.2357)
            selectedLatLng = cairo
            locationName = "Default Location"
            gMap.addMarker(MarkerOptions().position(cairo).title(locationName))
            gMap.animateCamera(CameraUpdateFactory.newLatLngZoom(cairo, 10f))

            //  إرسال الافتراضي مباشرة لـ AddProductFragment
            parentFragmentManager.setFragmentResult(
                "locationRequest",
                bundleOf(
                    "lat" to cairo.latitude,
                    "lng" to cairo.longitude,
                    "locationName" to locationName
                )
            )
        }

        gMap.setOnMapClickListener { latLng ->
            gMap.clear()
            gMap.addMarker(MarkerOptions().position(latLng).title("Product location"))
            selectedLatLng = latLng
            locationName = "Location"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}