package com.nisa.hazentra;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class ReportHazardActivity extends AppCompatActivity {

    private static final String ADD_HAZARD_URL =
            ApiConfig.ADD_HAZARD;

    private AutoCompleteTextView categoryDropdown;

    private TextInputEditText reporterNameInput;
    private TextInputEditText locationNameInput;
    private TextInputEditText latitudeInput;
    private TextInputEditText longitudeInput;
    private TextInputEditText descriptionInput;

    private MaterialButton useCurrentLocationButton;
    private MaterialButton submitHazardButton;

    private FusedLocationProviderClient fusedLocationClient;
    private RequestQueue requestQueue;

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
                            retrieveCurrentLocation();
                        } else {
                            Toast.makeText(
                                    this,
                                    "Location permission is required "
                                            + "to use your current GPS location.",
                                    Toast.LENGTH_LONG
                            ).show();
                        }
                    }
            );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(
                R.layout.activity_report_hazard
        );

        initialiseViews();
        configureToolbar();
        configureCategoryDropdown();
        configureLocationButton();
        configureSubmitButton();

        fusedLocationClient =
                LocationServices
                        .getFusedLocationProviderClient(this);

        requestQueue =
                Volley.newRequestQueue(
                        getApplicationContext()
                );
    }

    private void initialiseViews() {

        categoryDropdown =
                findViewById(
                        R.id.categoryDropdown
                );

        reporterNameInput =
                findViewById(
                        R.id.reporterNameInput
                );

        locationNameInput =
                findViewById(
                        R.id.locationNameInput
                );

        latitudeInput =
                findViewById(
                        R.id.latitudeInput
                );

        longitudeInput =
                findViewById(
                        R.id.longitudeInput
                );

        descriptionInput =
                findViewById(
                        R.id.descriptionInput
                );

        useCurrentLocationButton =
                findViewById(
                        R.id.useCurrentLocationButton
                );

        submitHazardButton =
                findViewById(
                        R.id.submitHazardButton
                );
    }

    private void configureToolbar() {

        MaterialToolbar toolbar =
                findViewById(R.id.reportToolbar);

        toolbar.setTitle("Report Hazard");

        toolbar.setTitleTextColor(
                ContextCompat.getColor(
                        this,
                        R.color.hazentra_on_navy
                )
        );

        toolbar.setTitleCentered(true);

        toolbar.setNavigationOnClickListener(
                view -> finish()
        );
    }

    private void configureCategoryDropdown() {

        String[] categories = {
                "Road Hazard",
                "Environmental Hazard",
                "Building Hazard"
        };

        ArrayAdapter<String> categoryAdapter =
                new ArrayAdapter<>(
                        this,
                        android.R.layout
                                .simple_dropdown_item_1line,
                        categories
                );

        categoryDropdown.setAdapter(
                categoryAdapter
        );

        categoryDropdown.setOnClickListener(
                view -> categoryDropdown.showDropDown()
        );

        categoryDropdown.setOnFocusChangeListener(
                (view, hasFocus) -> {

                    if (hasFocus) {
                        categoryDropdown.showDropDown();
                    }
                }
        );
    }

    private void configureLocationButton() {

        useCurrentLocationButton
                .setOnClickListener(
                        view -> checkLocationPermission()
                );
    }

    private void configureSubmitButton() {

        submitHazardButton.setOnClickListener(
                view -> {

                    if (validateForm()) {
                        submitHazardReport();
                    }
                }
        );
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
            retrieveCurrentLocation();
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

    private void retrieveCurrentLocation() {

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

        useCurrentLocationButton.setEnabled(false);
        useCurrentLocationButton.setText(
                "Detecting location..."
        );

        fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                null
        ).addOnSuccessListener(
                this,
                location -> {

                    restoreLocationButton();

                    if (location != null) {
                        displayCoordinates(location);
                    } else {
                        Toast.makeText(
                                this,
                                "Unable to detect the current location. "
                                        + "Make sure GPS is enabled.",
                                Toast.LENGTH_LONG
                        ).show();
                    }
                }
        ).addOnFailureListener(
                exception -> {

                    restoreLocationButton();

                    Toast.makeText(
                            this,
                            "Failed to retrieve the current location.",
                            Toast.LENGTH_LONG
                    ).show();
                }
        );
    }

    private void displayCoordinates(
            @NonNull Location location
    ) {

        String latitude = String.format(
                Locale.US,
                "%.8f",
                location.getLatitude()
        );

        String longitude = String.format(
                Locale.US,
                "%.8f",
                location.getLongitude()
        );

        latitudeInput.setText(latitude);
        longitudeInput.setText(longitude);

        latitudeInput.setError(null);
        longitudeInput.setError(null);

        Toast.makeText(
                this,
                "Current GPS coordinates detected.",
                Toast.LENGTH_SHORT
        ).show();
    }

    private void restoreLocationButton() {

        useCurrentLocationButton.setEnabled(true);

        useCurrentLocationButton.setText(
                "Use My Current Location"
        );
    }

    private boolean validateForm() {

        String reporterName =
                getText(reporterNameInput);

        String category =
                categoryDropdown
                        .getText()
                        .toString()
                        .trim();

        String locationName =
                getText(locationNameInput);

        String latitude =
                getText(latitudeInput);

        String longitude =
                getText(longitudeInput);

        String description =
                getText(descriptionInput);

        clearFieldErrors();

        if (reporterName.isEmpty()) {

            reporterNameInput.setError(
                    "Reporter name is required."
            );

            reporterNameInput.requestFocus();

            return false;
        }

        if (category.isEmpty()) {

            categoryDropdown.setError(
                    "Please select a hazard category."
            );

            categoryDropdown.requestFocus();

            return false;
        }

        if (!isValidCategory(category)) {

            categoryDropdown.setError(
                    "Please select a valid category."
            );

            categoryDropdown.requestFocus();

            return false;
        }

        if (locationName.isEmpty()) {

            locationNameInput.setError(
                    "Location name is required."
            );

            locationNameInput.requestFocus();

            return false;
        }

        if (latitude.isEmpty()) {

            latitudeInput.setError(
                    "Latitude is required."
            );

            latitudeInput.requestFocus();

            return false;
        }

        if (longitude.isEmpty()) {

            longitudeInput.setError(
                    "Longitude is required."
            );

            longitudeInput.requestFocus();

            return false;
        }

        if (!isLatitudeValid(latitude)) {

            latitudeInput.setError(
                    "Latitude must be between -90 and 90."
            );

            latitudeInput.requestFocus();

            return false;
        }

        if (!isLongitudeValid(longitude)) {

            longitudeInput.setError(
                    "Longitude must be between -180 and 180."
            );

            longitudeInput.requestFocus();

            return false;
        }

        if (description.isEmpty()) {

            descriptionInput.setError(
                    "Hazard description is required."
            );

            descriptionInput.requestFocus();

            return false;
        }

        return true;
    }

    private void clearFieldErrors() {

        reporterNameInput.setError(null);
        categoryDropdown.setError(null);
        locationNameInput.setError(null);
        latitudeInput.setError(null);
        longitudeInput.setError(null);
        descriptionInput.setError(null);
    }

    private boolean isValidCategory(
            String category
    ) {

        return category.equals("Road Hazard")
                || category.equals("Environmental Hazard")
                || category.equals("Building Hazard");
    }

    private boolean isLatitudeValid(
            String latitudeValue
    ) {

        try {

            double latitude =
                    Double.parseDouble(latitudeValue);

            return latitude >= -90
                    && latitude <= 90;

        } catch (NumberFormatException exception) {
            return false;
        }
    }

    private boolean isLongitudeValid(
            String longitudeValue
    ) {

        try {

            double longitude =
                    Double.parseDouble(longitudeValue);

            return longitude >= -180
                    && longitude <= 180;

        } catch (NumberFormatException exception) {
            return false;
        }
    }

    private void submitHazardReport() {

        String reporterName =
                getText(reporterNameInput);

        String selectedCategory =
                categoryDropdown
                        .getText()
                        .toString()
                        .trim();

        String locationName =
                getText(locationNameInput);

        String latitude =
                getText(latitudeInput);

        String longitude =
                getText(longitudeInput);

        String description =
                getText(descriptionInput);

        int categoryId =
                getCategoryId(
                        selectedCategory
                );

        if (categoryId == 0) {

            Toast.makeText(
                    this,
                    "Invalid hazard category.",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        String manufacturer =
                capitaliseWords(
                        Build.MANUFACTURER
                );

        String userAgent =
                manufacturer
                        + " "
                        + Build.MODEL
                        + " | Android "
                        + Build.VERSION.RELEASE;

        setSubmittingState(true);

        StringRequest request =
                new StringRequest(
                        Request.Method.POST,
                        ADD_HAZARD_URL,
                        response -> {

                            try {

                                JSONObject jsonResponse =
                                        new JSONObject(
                                                response
                                        );

                                boolean success =
                                        jsonResponse.optBoolean(
                                                "success",
                                                false
                                        );

                                String message =
                                        jsonResponse.optString(
                                                "message",
                                                "Unable to submit the report."
                                        );

                                if (success) {

                                    Toast.makeText(
                                            this,
                                            message,
                                            Toast.LENGTH_LONG
                                    ).show();

                                    setResult(RESULT_OK);

                                    finish();

                                } else {

                                    setSubmittingState(false);

                                    Toast.makeText(
                                            this,
                                            message,
                                            Toast.LENGTH_LONG
                                    ).show();
                                }

                            } catch (
                                    JSONException exception
                            ) {

                                setSubmittingState(false);

                                Toast.makeText(
                                        this,
                                        "Invalid response received "
                                                + "from the server.",
                                        Toast.LENGTH_LONG
                                ).show();
                            }
                        },
                        error -> {

                            setSubmittingState(false);

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
                    protected Map<String, String>
                    getParams() {

                        Map<String, String> params =
                                new HashMap<>();

                        params.put(
                                "user_name",
                                reporterName
                        );

                        params.put(
                                "user_agent",
                                userAgent
                        );

                        params.put(
                                "location_name",
                                locationName
                        );

                        params.put(
                                "latitude",
                                latitude
                        );

                        params.put(
                                "longitude",
                                longitude
                        );

                        params.put(
                                "category_id",
                                String.valueOf(
                                        categoryId
                                )
                        );

                        params.put(
                                "description",
                                description
                        );

                        return params;
                    }

                    @Override
                    public Map<String, String>
                    getHeaders() {

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

        request.setTag(
                ADD_HAZARD_URL
        );

        requestQueue.add(
                request
        );
    }

    private int getCategoryId(
            String categoryName
    ) {

        switch (categoryName) {

            case "Road Hazard":
                return 1;

            case "Environmental Hazard":
                return 2;

            case "Building Hazard":
                return 3;

            default:
                return 0;
        }
    }

    private void setSubmittingState(
            boolean isSubmitting
    ) {

        submitHazardButton.setEnabled(
                !isSubmitting
        );

        useCurrentLocationButton.setEnabled(
                !isSubmitting
        );

        reporterNameInput.setEnabled(
                !isSubmitting
        );

        categoryDropdown.setEnabled(
                !isSubmitting
        );

        locationNameInput.setEnabled(
                !isSubmitting
        );

        latitudeInput.setEnabled(
                !isSubmitting
        );

        longitudeInput.setEnabled(
                !isSubmitting
        );

        descriptionInput.setEnabled(
                !isSubmitting
        );

        if (isSubmitting) {

            submitHazardButton.setText(
                    "Submitting report..."
            );

        } else {

            submitHazardButton.setText(
                    "Submit Hazard Report"
            );
        }
    }

    private String capitaliseWords(
            String value
    ) {

        if (
                value == null
                        || value.trim().isEmpty()
        ) {
            return "Android";
        }

        String trimmedValue =
                value.trim();

        return trimmedValue
                .substring(0, 1)
                .toUpperCase(Locale.US)
                + trimmedValue.substring(1);
    }

    private String getText(
            TextInputEditText input
    ) {

        if (input.getText() == null) {
            return "";
        }

        return input
                .getText()
                .toString()
                .trim();
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (requestQueue != null) {
            requestQueue.cancelAll(
                    ADD_HAZARD_URL
            );
        }
    }
}