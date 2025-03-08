package com.erbpanel.islamiccalender;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.erbpanel.islamiccalender.Adapters.CaroselAdapter;
import com.erbpanel.islamiccalender.Models.CaroselItem;
import com.erbpanel.islamiccalender.Providers.HijriDateProviders;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 22222;
    private FusedLocationProviderClient fusedLocationClient;
    private Button resetButton;

    private TextView date_hijri, date_gregorian, fajr_value, sunrise_value,
            dhuhr_value, asr_value, maghrib_value, isha_value, location_info;
    private ViewPager2 carouselViewPager;
    private Date currentDate;
    private List<CaroselItem> caroselItemList;
    private CaroselAdapter caroselAdapter;
    private List<HijriDateProviders.DatePair> dates;
    private int todayPosition = -1;
    private String BASE_URL;
    private double latitude;
    private double longitude;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        setContentView(R.layout.activity_main);

        BASE_URL = getString(R.string.base_api_url);

        caroselItemList = new ArrayList<>();

        // Initialize UI elements
        date_hijri = findViewById(R.id.date_hijri);
        date_gregorian = findViewById(R.id.date_gregorian);
        fajr_value = findViewById(R.id.fajr_value);
        sunrise_value = findViewById(R.id.sunrise_value);
        dhuhr_value = findViewById(R.id.dhuhr_value);
        asr_value = findViewById(R.id.asr_value);
        maghrib_value = findViewById(R.id.maghrib_value);
        isha_value = findViewById(R.id.isha_value);
        location_info = findViewById(R.id.location_info);
        resetButton = findViewById(R.id.resetButton);

        carouselViewPager = findViewById(R.id.carouselViewPager);

        carouselViewPager.setOffscreenPageLimit(1);
        RecyclerView recyclerView = (RecyclerView) carouselViewPager.getChildAt(0);
        recyclerView.setPadding(40, 0, 40, 0);
        recyclerView.setClipToPadding(false);
        currentDate = new Date(); // Set current date

        // Initialize fusedLocationClient
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Check permission and fetch location
        checkAndRequestLocationPermission();

        // Setup carousel adapter
        caroselAdapter = new CaroselAdapter(caroselItemList);
        carouselViewPager.setAdapter(caroselAdapter);

        carouselViewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);

                if (dates == null || dates.isEmpty() || position < 0 || position >= dates.size()) {
                    Log.e("ViewPager", "Invalid position or empty dates list.");
                    return;
                }

                HijriDateProviders.DatePair selectedDate = dates.get(position);

                try {
                    // Define the expected format "dd/MM/yyyy"
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

                    // Parse the date using the defined format
                    LocalDate selectedGregorianDate = LocalDate.parse(selectedDate.getGregorianDate(), formatter);

                    // Get today's date for comparison
                    LocalDate today = LocalDate.now();

                    if (!selectedGregorianDate.equals(today)) {
                        fetchPrayerTimes(latitude, longitude, String.valueOf(selectedGregorianDate), false);
                    }

                    // Hide reset button if it's today's date
                    resetButton.setVisibility(selectedDate.isCurrentDate() ? View.GONE : View.VISIBLE);

                    Log.d("ViewPager", "Selected date: " + selectedGregorianDate);

                } catch (DateTimeParseException e) {
                    Log.e("ViewPager", "Date parsing error: " + e.getMessage(), e);
                }
            }
        });
        resetButton.setOnClickListener(v ->setCurrentPage());
    }

    public String getAddressFromLatLng(double latitude, double longitude) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                String city = address.getLocality();
                String country = address.getCountryName();
                return String.format("%s, %s", city != null ? city : "Unknown City",
                        country != null ? country : "Unknown Country");
            } else {
                return "Address not found";
            }
        } catch (IOException e) {
            e.printStackTrace();
            return "Geocoder service not available";
        }
    }

    private void checkAndRequestLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Location Permission Required", Toast.LENGTH_SHORT).show();
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            fetchLocationAndPrayerTimes();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                fetchLocationAndPrayerTimes();
            } else {
                Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @SuppressLint("MissingPermission")
    private void fetchLocationAndPrayerTimes() {
        if (fusedLocationClient == null) {
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        }

        Log.d("TAG", "fetchLocationAndPrayerTimes: ");

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    if (location != null) {
                        latitude = location.getLatitude();
                        longitude = location.getLongitude();
                        Toast.makeText(getApplicationContext(), String.valueOf(LocalDate.now()), Toast.LENGTH_SHORT).show();
                        fetchPrayerTimes(latitude, longitude, String.valueOf(LocalDate.now()), true);
                    } else {
                        Toast.makeText(this, "Location Unavailable", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(this, e -> {
                    Toast.makeText(this, "Failed to get location: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    public void updateCurrentDate(boolean swipe) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
        LocalDate date = currentDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        date = swipe ? date.plusDays(1) : date.minusDays(1);

        currentDate = Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant());
        date_gregorian.setText(date.format(formatter));

        fetchLocationAndPrayerTimes();
    }

    private void fetchPrayerTimes(double latitude, double longitude, String selectedGregorianDate, boolean reset) {
        String url = BASE_URL + "timings/"+selectedGregorianDate+"?latitude=" + latitude + "&longitude=" + longitude;

        Log.d("ATAG", "fetchPrayerTimes: "+selectedGregorianDate);

        RequestQueue queue = Volley.newRequestQueue(this);
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        JSONObject data = response.getJSONObject("data");
                        JSONObject timings = data.getJSONObject("timings");
                        fajr_value.setText(convertTo12HourFormat(timings.getString("Fajr")));
                        dhuhr_value.setText(convertTo12HourFormat(timings.getString("Dhuhr")));
                        asr_value.setText(convertTo12HourFormat(timings.getString("Asr")));
                        maghrib_value.setText(convertTo12HourFormat(timings.getString("Maghrib")));
                        isha_value.setText(convertTo12HourFormat(timings.getString("Isha")));
                        sunrise_value.setText(convertTo12HourFormat(timings.getString("Sunrise")));

                        JSONObject date = data.getJSONObject("date");
                        date_gregorian.setText(date.getString("readable"));

                        JSONObject hijri = date.getJSONObject("hijri");
                        String completeHijriDate = String.format("%s %s %s, %s",
                                hijri.getString("day"), hijri.getJSONObject("month").getString("ar"),
                                hijri.getString("year"), hijri.getJSONObject("weekday").getString("ar"));
                        date_hijri.setText(completeHijriDate);

                        location_info.setText(getAddressFromLatLng(latitude, longitude));
                        dates = HijriDateProviders.getHijriDates();
                        caroselItemList.clear();
                        for (HijriDateProviders.DatePair datePair: dates) {
                            caroselItemList.add(
                                    new CaroselItem(
                                            datePair.getHijriDate(),
                                            convertTo12HourFormat(timings.getString("Fajr")),
                                            convertTo12HourFormat(timings.getString("Isha"))
                                    )
                            );
                        }
                        if (reset) setCurrentPage();
                        caroselAdapter.notifyDataSetChanged();

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }, error -> Toast.makeText(this, "API Error: " + selectedGregorianDate, Toast.LENGTH_SHORT).show());

        queue.add(jsonObjectRequest);
    }

    private void setCurrentPage() {
        int currentPosition = -1;
        for (int i = 0; i < dates.size(); i++) {
            if (dates.get(i).isCurrentDate()) {
                currentPosition = i;
                break;
            }
        }
        if (currentPosition != -1) {
            carouselViewPager.setCurrentItem(currentPosition);
        }
    }

    private boolean isCarouselOnToday() {
        if (todayPosition == -1) {
            return false; // Today's position wasn't found
        }
        int currentPosition = carouselViewPager.getCurrentItem();
        return currentPosition == todayPosition;
    }

    private String convertTo12HourFormat(String time) {
        try {
            DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault());
            DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("hh:mm a", Locale.getDefault());
            LocalTime localTime = LocalTime.parse(time, inputFormatter);
            return localTime.format(outputFormatter);
        } catch (Exception e) {
            e.printStackTrace();
            return time; // Return original time if formatting fails
        }
    }
}
