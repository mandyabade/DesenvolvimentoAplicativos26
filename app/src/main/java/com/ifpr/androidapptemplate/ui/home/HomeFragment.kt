package com.ifpr.androidapptemplate.ui.home

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.location.Geocoder
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.os.Looper
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.google.android.gms.location.*
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.database.*
import com.ifpr.androidapptemplate.R
import com.ifpr.androidapptemplate.baseclasses.Item
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class HomeFragment : Fragment() {

    private lateinit var currentAddressTextView: TextView
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var locationRequest: LocationRequest
    private lateinit var mapsButton: Button

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val view = inflater.inflate(R.layout.fragment_home, container, false)

        // PEGA O BOTÃO DO XML
        mapsButton = view.findViewById(R.id.mapsButton)

        inicializaGerenciamentoLocalizacao(view)

        val containerItems = view.findViewById<LinearLayout>(R.id.itemContainer)
        carregarItensMarketplace(containerItems)

        return view
    }

    private fun inicializaGerenciamentoLocalizacao(view: View) {

        currentAddressTextView = view.findViewById(R.id.currentAddressTextView)

        fusedLocationClient =
            LocationServices.getFusedLocationProviderClient(requireActivity())

        if (
            ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
            &&
            ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {

            requestLocationPermission()

        } else {

            getCurrentLocation()
        }
    }

    private fun requestLocationPermission() {

        requestPermissions(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ),
            LOCATION_PERMISSION_REQUEST_CODE
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {

        super.onRequestPermissionsResult(
            requestCode,
            permissions,
            grantResults
        )

        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {

            if (
                grantResults.isNotEmpty()
                &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED
            ) {

                getCurrentLocation()

            } else {

                Snackbar.make(
                    requireView(),
                    "Permissão negada.",
                    Snackbar.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun getCurrentLocation() {

        if (
            ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
            &&
            ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        locationCallback = object : LocationCallback() {

            override fun onLocationResult(locationResult: LocationResult) {

                locationResult.lastLocation?.let { location ->

                    displayAddress(location)

                    mapsButton.setOnClickListener {

                        abrirNoGoogleMaps(
                            location.latitude,
                            location.longitude
                        )
                    }
                }
            }
        }

        locationRequest = LocationRequest.create().apply {

            interval = 30000
            fastestInterval = 30000
            priority = Priority.PRIORITY_HIGH_ACCURACY
        }

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
    }

    private fun displayAddress(location: Location) {

        CoroutineScope(Dispatchers.IO).launch {

            try {

                val geocoder = Geocoder(
                    requireContext(),
                    Locale.getDefault()
                )

                val addresses = geocoder.getFromLocation(
                    location.latitude,
                    location.longitude,
                    1
                )

                val address =
                    addresses?.firstOrNull()?.getAddressLine(0)
                        ?: "Endereço não encontrado"

                withContext(Dispatchers.Main) {

                    currentAddressTextView.text = address
                }

            } catch (e: Exception) {

                withContext(Dispatchers.Main) {

                    currentAddressTextView.text =
                        "Erro: ${e.message}"
                }
            }
        }
    }

    private fun abrirNoGoogleMaps(
        latitude: Double,
        longitude: Double
    ) {

        val uri =
            Uri.parse("geo:$latitude,$longitude?q=$latitude,$longitude")

        val intent = Intent(Intent.ACTION_VIEW, uri)

        intent.setPackage("com.google.android.apps.maps")

        try {

            startActivity(intent)

        } catch (e: Exception) {

            Toast.makeText(
                requireContext(),
                "Google Maps não encontrado",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun carregarItensMarketplace(container: LinearLayout) {

        val databaseRef =
            FirebaseDatabase.getInstance()
                .getReference("objetivos")

        databaseRef.addListenerForSingleValueEvent(
            object : ValueEventListener {

                override fun onDataChange(snapshot: DataSnapshot) {

                    container.removeAllViews()

                    for (userSnapshot in snapshot.children) {

                        for (itemSnapshot in userSnapshot.children) {

                            val item =
                                itemSnapshot.getValue(Item::class.java)
                                    ?: continue

                            val itemView =
                                LayoutInflater.from(container.context)
                                    .inflate(
                                        R.layout.item_template,
                                        container,
                                        false
                                    )

                            val imageView =
                                itemView.findViewById<ImageView>(R.id.item_image)

                            val nomeView =
                                itemView.findViewById<TextView>(R.id.item_nome)

                            val enderecoView =
                                itemView.findViewById<TextView>(R.id.item_endereco)

                            val objetivoView =
                                itemView.findViewById<TextView>(R.id.item_objetivo)

                            val tempoView =
                                itemView.findViewById<TextView>(R.id.item_tempo)

                            val frequenciaView =
                                itemView.findViewById<TextView>(R.id.item_frequencia)

                            nomeView.text =
                                "Nome: ${item.nome ?: "Não informado"}"

                            enderecoView.text =
                                "Endereço: ${item.endereco ?: "Não informado"}"

                            objetivoView.text =
                                "Objetivo: ${item.objetivo ?: "Não informado"}"

                            tempoView.text =
                                "Tempo: ${item.tempo ?: "Não informado"}"

                            frequenciaView.text =
                                "Frequência: ${item.frequencia ?: "Não informado"}"

                            if (!item.imageUrl.isNullOrEmpty()) {

                                Glide.with(container.context)
                                    .load(item.imageUrl)
                                    .into(imageView)

                            } else if (!item.base64Image.isNullOrEmpty()) {

                                try {

                                    val bytes = Base64.decode(
                                        item.base64Image,
                                        Base64.DEFAULT
                                    )

                                    val bitmap =
                                        BitmapFactory.decodeByteArray(
                                            bytes,
                                            0,
                                            bytes.size
                                        )

                                    imageView.setImageBitmap(bitmap)

                                } catch (_: Exception) {
                                }
                            }

                            container.addView(itemView)
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {

                    Toast.makeText(
                        container.context,
                        "Erro ao carregar dados",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        )
    }

    override fun onDestroyView() {

        super.onDestroyView()

        fusedLocationClient.removeLocationUpdates(locationCallback)
    }
}