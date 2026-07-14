package com.nisa.hazentra;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.location.Location;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.core.splashscreen.SplashScreen;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.progressindicator.CircularProgressIndicator;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import com.google.android.gms.maps.model.BitmapDescriptor;

import android.os.SystemClock;

public class MainActivity extends AppCompatActivity
        implements OnMapReadyCallback {

    private static final float DEFAULT_ZOOM = 16f;

    private static final String PREFERENCES_NAME =
            "hazentra_preferences";

    private static final String KEY_NIGHT_MODE =
            "night_mode";

    private static final String HAZARDS_URL =
            ApiConfig.GET_HAZARDS;

    private GoogleMap googleMap;

    private FusedLocationProviderClient fusedLocationClient;
    private RequestQueue requestQueue;
    private CircularProgressIndicator loadingIndicator;

    private boolean currentLocationFound = false;

    private final ActivityResultLauncher<String[]>
            locationPermissionLauncher =
            registerForActivityResult(
                    new ActivityResultContracts
                            .RequestMultiplePermissions(),
                    result -> {

                        boolean fineLocationGranted =
                                Boolean.TRUE.equals(
                                        result.get(
                                                Manifest.permission
                                                        .ACCESS_FINE_LOCATION
                                        )
                                );

                        boolean coarseLocationGranted =
                                Boolean.TRUE.equals(
                                        result.get(
                                                Manifest.permission
                                                        .ACCESS_COARSE_LOCATION
                                        )
                                );

                        if (
                                fineLocationGranted
                                        || coarseLocationGranted
                        ) {
                            enableCurrentLocation();

                        } else {

                            Toast.makeText(
                                    this,
                                    "Location permission is required "
                                            + "to show your current location.",
                                    Toast.LENGTH_LONG
                            ).show();
                        }
                    }
            );

    private final ActivityResultLauncher<Intent>
            reportHazardLauncher =
            registerForActivityResult(
                    new ActivityResultContracts
                            .StartActivityForResult(),
                    result -> {

                        if (
                                result.getResultCode() == RESULT_OK
                                        && googleMap != null
                        ) {
                            reloadHazardMarkers();
                        }
                    }
            );

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        applySavedTheme();

        long splashStartTime =
                SystemClock.elapsedRealtime();

        SplashScreen splashScreen =
                SplashScreen.installSplashScreen(this);

        splashScreen.setKeepOnScreenCondition(
                () -> SystemClock.elapsedRealtime()
                        - splashStartTime < 1100
        );

        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);

        setContentView(R.layout.activity_main);

        loadingIndicator =
                findViewById(R.id.loadingIndicator);

        configureWindowInsets();
        configureTopAppBar();
        configureReportHazardButton();
        configureRefreshButton();

        fusedLocationClient =
                LocationServices
                        .getFusedLocationProviderClient(this);

        requestQueue =
                Volley.newRequestQueue(
                        getApplicationContext()
                );

        initialiseMap();
    }

    private void configureWindowInsets() {

        ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(R.id.main),
                (view, insets) -> {

                    Insets systemBars =
                            insets.getInsets(
                                    WindowInsetsCompat.Type
                                            .systemBars()
                            );

                    view.setPadding(
                            systemBars.left,
                            systemBars.top,
                            systemBars.right,
                            systemBars.bottom
                    );

                    return insets;
                }
        );
    }

    private void configureTopAppBar() {

        MaterialToolbar topAppBar =
                findViewById(R.id.topAppBar);

        topAppBar.setTitle("Hazentra Map");

        topAppBar.setTitleTextColor(
                ContextCompat.getColor(
                        this,
                        R.color.hazentra_on_navy
                )
        );

        topAppBar.setTitleCentered(true);

        topAppBar.setOverflowIcon(
                ContextCompat.getDrawable(
                        this,
                        R.drawable.ic_more_vert
                )
        );

        updateThemeMenuIcon(topAppBar);

        topAppBar.setOnMenuItemClickListener(
                menuItem -> {

                    if (menuItem.getItemId() == R.id.menuTheme) {

                        toggleTheme();
                        return true;
                    }

                    if (menuItem.getItemId() == R.id.menuStatistics) {

                        startActivity(
                                new Intent(
                                        MainActivity.this,
                                        StatisticsActivity.class
                                )
                        );

                        return true;
                    }

                    if (menuItem.getItemId() == R.id.menuHazardReports) {

                        startActivity(
                                new Intent(
                                        MainActivity.this,
                                        HazardListActivity.class
                                )
                        );

                        return true;
                    }

                    if (menuItem.getItemId() == R.id.menuAbout) {

                        startActivity(
                                new Intent(
                                        MainActivity.this,
                                        AboutActivity.class
                                )
                        );

                        return true;
                    }

                    return false;
                }
        );
    }

    private void applySavedTheme() {

        SharedPreferences preferences =
                getSharedPreferences(
                        PREFERENCES_NAME,
                        MODE_PRIVATE
                );

        int savedMode =
                preferences.getInt(
                        KEY_NIGHT_MODE,
                        AppCompatDelegate.MODE_NIGHT_NO
                );

        if (
                AppCompatDelegate.getDefaultNightMode()
                        != savedMode
        ) {

            AppCompatDelegate.setDefaultNightMode(
                    savedMode
            );
        }
    }

    private void toggleTheme() {

        boolean darkModeEnabled =
                isDarkModeEnabled();

        int newMode =
                darkModeEnabled
                        ? AppCompatDelegate.MODE_NIGHT_NO
                        : AppCompatDelegate.MODE_NIGHT_YES;

        getSharedPreferences(
                PREFERENCES_NAME,
                MODE_PRIVATE
        )
                .edit()
                .putInt(
                        KEY_NIGHT_MODE,
                        newMode
                )
                .apply();

        AppCompatDelegate.setDefaultNightMode(
                newMode
        );
    }

    private boolean isDarkModeEnabled() {

        int currentNightMode =
                getResources()
                        .getConfiguration()
                        .uiMode
                        & Configuration.UI_MODE_NIGHT_MASK;

        return currentNightMode
                == Configuration.UI_MODE_NIGHT_YES;
    }

    private void updateThemeMenuIcon(
            @NonNull MaterialToolbar topAppBar
    ) {

        MenuItem themeMenuItem =
                topAppBar.getMenu()
                        .findItem(R.id.menuTheme);

        if (themeMenuItem == null) {
            return;
        }

        int iconResource =
                isDarkModeEnabled()
                        ? R.drawable.ic_light_mode
                        : R.drawable.ic_dark_mode;

        themeMenuItem.setIcon(iconResource);

        if (themeMenuItem.getIcon() != null) {

            DrawableCompat.setTint(
                    themeMenuItem.getIcon(),
                    ContextCompat.getColor(
                            this,
                            R.color.hazentra_on_navy
                    )
            );
        }

        themeMenuItem.setTitle(
                isDarkModeEnabled()
                        ? "Switch to light mode"
                        : "Switch to dark mode"
        );
    }

    private void configureReportHazardButton() {

        findViewById(R.id.reportHazardButton)
                .setOnClickListener(view -> {

                    Intent intent =
                            new Intent(
                                    MainActivity.this,
                                    ReportHazardActivity.class
                            );

                    reportHazardLauncher.launch(intent);
                });
    }

    private void configureRefreshButton() {

        findViewById(R.id.refreshHazardsButton)
                .setOnClickListener(
                        view -> {

                            if (
                                    loadingIndicator != null
                                            && loadingIndicator.getVisibility()
                                            == View.VISIBLE
                            ) {
                                return;
                            }

                            reloadHazardMarkers();
                        }
                );
    }

    private void initialiseMap() {

        SupportMapFragment mapFragment =
                (SupportMapFragment)
                        getSupportFragmentManager()
                                .findFragmentById(
                                        R.id.mapFragment
                                );

        if (mapFragment != null) {

            mapFragment.getMapAsync(this);

        } else {

            Toast.makeText(
                    this,
                    "Unable to load the Google Map.",
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    @Override
    public void onMapReady(
            @NonNull GoogleMap map
    ) {

        googleMap = map;

        /*
         * Local JSON map styling works with the normal map type.
         */
        googleMap.setMapType(
                GoogleMap.MAP_TYPE_NORMAL
        );

        applyMapStyle();

        googleMap.getUiSettings()
                .setZoomControlsEnabled(true);

        googleMap.getUiSettings()
                .setCompassEnabled(true);

        googleMap.getUiSettings()
                .setMyLocationButtonEnabled(true);

        googleMap.setInfoWindowAdapter(
                new HazardInfoWindowAdapter()
        );

        checkLocationPermission();
        loadHazardsFromServer();
    }

    private void checkLocationPermission() {

        boolean fineLocationGranted =
                ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission
                                .ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED;

        boolean coarseLocationGranted =
                ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission
                                .ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED;

        if (
                fineLocationGranted
                        || coarseLocationGranted
        ) {

            enableCurrentLocation();

        } else {

            locationPermissionLauncher.launch(
                    new String[]{
                            Manifest.permission
                                    .ACCESS_FINE_LOCATION,

                            Manifest.permission
                                    .ACCESS_COARSE_LOCATION
                    }
            );
        }
    }

    private void enableCurrentLocation() {

        boolean fineLocationGranted =
                ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission
                                .ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED;

        boolean coarseLocationGranted =
                ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission
                                .ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED;

        if (
                !fineLocationGranted
                        && !coarseLocationGranted
        ) {
            return;
        }

        if (googleMap == null) {
            return;
        }

        googleMap.setMyLocationEnabled(true);

        fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                null
        ).addOnSuccessListener(
                this,
                location -> {

                    if (location != null) {

                        currentLocationFound = true;

                        moveCameraToLocation(location);

                    } else {

                        Toast.makeText(
                                this,
                                "Unable to detect current location. "
                                        + "Please enable location services.",
                                Toast.LENGTH_LONG
                        ).show();
                    }
                }
        ).addOnFailureListener(
                exception -> Toast.makeText(
                        this,
                        "Failed to retrieve current location.",
                        Toast.LENGTH_LONG
                ).show()
        );
    }

    private void moveCameraToLocation(
            @NonNull Location location
    ) {

        if (googleMap == null) {
            return;
        }

        LatLng currentLatLng =
                new LatLng(
                        location.getLatitude(),
                        location.getLongitude()
                );

        googleMap.animateCamera(
                CameraUpdateFactory.newLatLngZoom(
                        currentLatLng,
                        DEFAULT_ZOOM
                )
        );
    }

    private void loadHazardsFromServer() {

        if (
                googleMap == null
                        || requestQueue == null
        ) {
            return;
        }

        showLoadingIndicator();

        JsonObjectRequest request =
                new JsonObjectRequest(
                        Request.Method.GET,
                        HAZARDS_URL,
                        null,
                        response -> {

                            hideLoadingIndicator();

                            try {

                                boolean success =
                                        response.optBoolean(
                                                "success",
                                                false
                                        );

                                if (!success) {

                                    Toast.makeText(
                                            this,
                                            "Unable to load hazard reports.",
                                            Toast.LENGTH_LONG
                                    ).show();

                                    return;
                                }

                                JSONArray hazards =
                                        response.optJSONArray(
                                                "hazards"
                                        );

                                if (
                                        hazards == null
                                                || hazards.length() == 0
                                ) {

                                    Toast.makeText(
                                            this,
                                            "No active hazard reports "
                                                    + "were found.",
                                            Toast.LENGTH_LONG
                                    ).show();

                                    return;
                                }

                                LatLngBounds.Builder boundsBuilder =
                                        new LatLngBounds.Builder();

                                boolean hasValidHazard = false;
                                int loadedHazardCount = 0;

                                for (
                                        int i = 0;
                                        i < hazards.length();
                                        i++
                                ) {

                                    JSONObject hazard =
                                            hazards.getJSONObject(i);

                                    String categoryName =
                                            hazard.optString(
                                                    "category_name",
                                                    "Hazard"
                                            );

                                    double latitude =
                                            hazard.getDouble(
                                                    "latitude"
                                            );

                                    double longitude =
                                            hazard.getDouble(
                                                    "longitude"
                                            );

                                    if (
                                            latitude < -90
                                                    || latitude > 90
                                                    || longitude < -180
                                                    || longitude > 180
                                    ) {
                                        continue;
                                    }

                                    LatLng hazardPosition =
                                            new LatLng(
                                                    latitude,
                                                    longitude
                                            );

                                    boundsBuilder.include(
                                            hazardPosition
                                    );

                                    hasValidHazard = true;
                                    loadedHazardCount++;

                                    BitmapDescriptor markerIcon =
                                            getHazardMarkerIcon(
                                                    categoryName
                                            );

                                    Marker marker =
                                            googleMap.addMarker(
                                                    new MarkerOptions()
                                                            .position(
                                                                    hazardPosition
                                                            )
                                                            .title(
                                                                    categoryName
                                                            )
                                                            .icon(
                                                                    markerIcon
                                                            )
                                                            .anchor(
                                                                    0.5f,
                                                                    0.5f
                                                            )
                                            );

                                    if (marker != null) {
                                        marker.setTag(hazard);
                                    }
                                }

                                /*
                                 * Show all markers only when the user's
                                 * current location cannot be retrieved.
                                 */

                                if (
                                        hasValidHazard
                                                && !currentLocationFound
                                ) {

                                    googleMap.setOnMapLoadedCallback(
                                            () -> {

                                                try {

                                                    googleMap.animateCamera(
                                                            CameraUpdateFactory
                                                                    .newLatLngBounds(
                                                                            boundsBuilder
                                                                                    .build(),
                                                                            120
                                                                    )
                                                    );

                                                } catch (
                                                        Exception exception
                                                ) {

                                                    Toast.makeText(
                                                            this,
                                                            "Hazards were loaded, "
                                                                    + "but the map view "
                                                                    + "could not be adjusted.",
                                                            Toast.LENGTH_SHORT
                                                    ).show();
                                                }
                                            }
                                    );
                                }

                                Toast.makeText(
                                        this,
                                        loadedHazardCount
                                                + " hazard report(s) "
                                                + "loaded.",
                                        Toast.LENGTH_SHORT
                                ).show();

                            } catch (
                                    JSONException exception
                            ) {

                                Toast.makeText(
                                        this,
                                        "Invalid data received "
                                                + "from the server.",
                                        Toast.LENGTH_LONG
                                ).show();
                            }
                        },
                        error -> {

                            hideLoadingIndicator();

                            String message =
                                    "Unable to connect to "
                                            + "the Hazentra server.";

                            if (
                                    error.networkResponse
                                            != null
                            ) {
                                message += " Error code: "
                                        + error.networkResponse
                                        .statusCode;
                            }

                            Toast.makeText(
                                    this,
                                    message,
                                    Toast.LENGTH_LONG
                            ).show();
                        }
                ) {

                    @Override
                    public Map<String, String> getHeaders() {

                        Map<String, String> headers =
                                new HashMap<>();

                        headers.put(
                                "ngrok-skip-browser-warning",
                                "true"
                        );

                        headers.put(
                                "Accept",
                                "application/json"
                        );

                        return headers;
                    }
                };

        request.setTag(HAZARDS_URL);
        requestQueue.add(request);
    }

    private void reloadHazardMarkers() {

        if (googleMap == null) {
            return;
        }

        googleMap.clear();
        loadHazardsFromServer();
    }

    private void showLoadingIndicator() {

        if (loadingIndicator != null) {
            loadingIndicator.setVisibility(View.VISIBLE);
        }
    }

    private void hideLoadingIndicator() {

        if (loadingIndicator != null) {
            loadingIndicator.setVisibility(View.GONE);
        }
    }
    private BitmapDescriptor getHazardMarkerIcon(
            @NonNull String categoryName
    ) {

        switch (categoryName) {

            case "Road Hazard":
                return bitmapDescriptorFromVector(
                        R.drawable.marker_road_hazard
                );

            case "Environmental Hazard":
                return bitmapDescriptorFromVector(
                        R.drawable.marker_environmental_hazard
                );

            case "Building Hazard":
                return bitmapDescriptorFromVector(
                        R.drawable.marker_building_hazard
                );

            default:
                return BitmapDescriptorFactory.defaultMarker(
                        BitmapDescriptorFactory.HUE_AZURE
                );
        }
    }
    private BitmapDescriptor bitmapDescriptorFromVector(
            int drawableResource
    ) {

        Drawable vectorDrawable =
                ContextCompat.getDrawable(
                        this,
                        drawableResource
                );

        if (vectorDrawable == null) {

            return BitmapDescriptorFactory
                    .defaultMarker(
                            BitmapDescriptorFactory.HUE_AZURE
                    );
        }

        int width = 90;
        int height = 90;

        vectorDrawable.setBounds(
                0,
                0,
                width,
                height
        );

        Bitmap bitmap =
                Bitmap.createBitmap(
                        width,
                        height,
                        Bitmap.Config.ARGB_8888
                );

        Canvas canvas =
                new Canvas(bitmap);

        vectorDrawable.draw(canvas);

        return BitmapDescriptorFactory
                .fromBitmap(bitmap);
    }

    private class HazardInfoWindowAdapter
            implements GoogleMap.InfoWindowAdapter {

        private final View infoWindowView;

        HazardInfoWindowAdapter() {

            infoWindowView =
                    LayoutInflater
                            .from(MainActivity.this)
                            .inflate(
                                    R.layout.info_window_hazard,
                                    null
                            );
        }

        @Override
        public View getInfoWindow(
                @NonNull Marker marker
        ) {

            return renderInfoWindow(marker);
        }

        @Override
        public View getInfoContents(
                @NonNull Marker marker
        ) {

            return null;
        }

        private View renderInfoWindow(
                @NonNull Marker marker
        ) {

            Object markerTag =
                    marker.getTag();

            if (!(markerTag instanceof JSONObject)) {
                return null;
            }

            JSONObject hazard =
                    (JSONObject) markerTag;

            TextView categoryText =
                    infoWindowView.findViewById(
                            R.id.infoCategory
                    );

            TextView locationText =
                    infoWindowView.findViewById(
                            R.id.infoLocation
                    );

            TextView descriptionText =
                    infoWindowView.findViewById(
                            R.id.infoDescription
                    );

            TextView reporterText =
                    infoWindowView.findViewById(
                            R.id.infoReporter
                    );

            TextView dateTimeText =
                    infoWindowView.findViewById(
                            R.id.infoDateTime
                    );

            TextView deviceText =
                    infoWindowView.findViewById(
                            R.id.infoDevice
                    );

            String categoryName =
                    hazard.optString(
                            "category_name",
                            "Hazard"
                    );

            categoryText.setText(categoryName);
            setCategoryTitleColor(
                    categoryText,
                    categoryName
            );

            locationText.setText(
                    "Location: "
                            + hazard.optString(
                            "location_name",
                            "Unknown location"
                    )
            );

            descriptionText.setText(
                    hazard.optString(
                            "description",
                            "No description available."
                    )
            );

            reporterText.setText(
                    "Reported by: "
                            + hazard.optString(
                            "user_name",
                            "Unknown reporter"
                    )
            );

            dateTimeText.setText(
                    "Reported on: "
                            + formatReportDate(
                            hazard.optString(
                                    "report_datetime",
                                    ""
                            )
                    )
            );

            deviceText.setText(
                    "Device: "
                            + hazard.optString(
                            "user_agent",
                            "Unknown device"
                    )
            );

            return infoWindowView;
        }
    }
    private void setCategoryTitleColor(
            @NonNull TextView categoryText,
            @NonNull String categoryName
    ) {

        int colorResource;

        switch (categoryName) {

            case "Road Hazard":
                colorResource =
                        R.color.hazentra_red;
                break;

            case "Environmental Hazard":
                colorResource =
                        R.color.hazentra_warning_green;
                break;

            case "Building Hazard":
                colorResource =
                        R.color.hazentra_warning_orange;
                break;

            default:
                colorResource =
                        R.color.hazentra_orange;
                break;
        }

        categoryText.setTextColor(
                ContextCompat.getColor(
                        MainActivity.this,
                        colorResource
                )
        );
    }

    private String formatReportDate(
            String serverDate
    ) {

        if (
                serverDate == null
                        || serverDate.trim().isEmpty()
        ) {
            return "Unknown date";
        }

        SimpleDateFormat inputFormat =
                new SimpleDateFormat(
                        "yyyy-MM-dd HH:mm:ss",
                        Locale.US
                );

        SimpleDateFormat outputFormat =
                new SimpleDateFormat(
                        "dd MMM yyyy, hh:mm a",
                        Locale.US
                );

        try {

            Date parsedDate =
                    inputFormat.parse(serverDate);

            if (parsedDate != null) {
                return outputFormat.format(
                        parsedDate
                );
            }

        } catch (ParseException ignored) {
            // Return the original server date below.
        }

        return serverDate;
    }

    private void applyMapStyle() {

        if (googleMap == null) {
            return;
        }

        /*
         * Keep the original Google Maps appearance in Light Mode.
         */
        if (!isDarkModeEnabled()) {

            googleMap.setMapStyle(null);
            return;
        }

        /*
         * Apply Hazentra's local JSON map style in Dark Mode.
         */
        try {

            MapStyleOptions darkMapStyle =
                    MapStyleOptions.loadRawResourceStyle(
                            this,
                            R.raw.map_style_dark
                    );

            boolean styleApplied =
                    googleMap.setMapStyle(
                            darkMapStyle
                    );

            if (!styleApplied) {

                Toast.makeText(
                        this,
                        "Dark map JSON could not be applied.",
                        Toast.LENGTH_LONG
                ).show();
            }

        } catch (Exception exception) {

            Toast.makeText(
                    this,
                    "Unable to load the dark map style.",
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (requestQueue != null) {
            requestQueue.cancelAll(
                    HAZARDS_URL
            );
        }
    }
}