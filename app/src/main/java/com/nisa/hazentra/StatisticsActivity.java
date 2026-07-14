package com.nisa.hazentra;

import android.animation.ValueAnimator;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.progressindicator.CircularProgressIndicator;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class StatisticsActivity extends AppCompatActivity {

    private static final String HAZARDS_URL =
            ApiConfig.GET_HAZARDS;

    private TextView totalHazardsText;
    private TextView roadHazardsText;
    private TextView environmentalHazardsText;
    private TextView buildingHazardsText;

    private TextView roadChartCountText;
    private TextView environmentalChartCountText;
    private TextView buildingChartCountText;

    private FrameLayout roadBarTrack;
    private FrameLayout environmentalBarTrack;
    private FrameLayout buildingBarTrack;

    private View roadBarFill;
    private View environmentalBarFill;
    private View buildingBarFill;

    private TextView latestCategoryText;
    private TextView latestLocationText;
    private TextView latestDescriptionText;
    private TextView latestReporterText;
    private TextView latestDateText;

    private TextView emptyStatisticsText;

    private MaterialCardView latestReportCard;
    private MaterialButton retryStatisticsButton;
    private CircularProgressIndicator statisticsLoadingIndicator;

    private RequestQueue requestQueue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_statistics);

        initialiseViews();
        configureToolbar();
        configureRetryButton();

        requestQueue =
                Volley.newRequestQueue(
                        getApplicationContext()
                );

        loadStatistics();
    }

    private void initialiseViews() {

        totalHazardsText =
                findViewById(R.id.totalHazardsText);

        roadHazardsText =
                findViewById(R.id.roadHazardsText);

        environmentalHazardsText =
                findViewById(R.id.environmentalHazardsText);

        buildingHazardsText =
                findViewById(R.id.buildingHazardsText);

        roadChartCountText =
                findViewById(R.id.roadChartCountText);

        environmentalChartCountText =
                findViewById(R.id.environmentalChartCountText);

        buildingChartCountText =
                findViewById(R.id.buildingChartCountText);

        roadBarTrack =
                findViewById(R.id.roadBarTrack);

        environmentalBarTrack =
                findViewById(R.id.environmentalBarTrack);

        buildingBarTrack =
                findViewById(R.id.buildingBarTrack);

        roadBarFill =
                findViewById(R.id.roadBarFill);

        environmentalBarFill =
                findViewById(R.id.environmentalBarFill);

        buildingBarFill =
                findViewById(R.id.buildingBarFill);

        latestCategoryText =
                findViewById(R.id.latestCategoryText);

        latestLocationText =
                findViewById(R.id.latestLocationText);

        latestDescriptionText =
                findViewById(R.id.latestDescriptionText);

        latestReporterText =
                findViewById(R.id.latestReporterText);

        latestDateText =
                findViewById(R.id.latestDateText);

        latestReportCard =
                findViewById(R.id.latestReportCard);

        emptyStatisticsText =
                findViewById(R.id.emptyStatisticsText);

        retryStatisticsButton =
                findViewById(R.id.retryStatisticsButton);

        statisticsLoadingIndicator =
                findViewById(R.id.statisticsLoadingIndicator);
    }

    private void configureToolbar() {

        MaterialToolbar toolbar =
                findViewById(R.id.statisticsToolbar);

        toolbar.setTitle("Statistics");

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

    private void configureRetryButton() {

        retryStatisticsButton.setOnClickListener(
                view -> loadStatistics()
        );
    }

    private void loadStatistics() {

        showLoadingState();

        JsonObjectRequest request =
                new JsonObjectRequest(
                        Request.Method.GET,
                        HAZARDS_URL,
                        null,
                        response -> {

                            try {

                                boolean success =
                                        response.optBoolean(
                                                "success",
                                                false
                                        );

                                if (!success) {

                                    showErrorState(
                                            response.optString(
                                                    "message",
                                                    "Unable to load statistics."
                                            )
                                    );

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

                                    showEmptyState();
                                    return;
                                }

                                displayStatistics(hazards);

                            } catch (JSONException exception) {

                                showErrorState(
                                        "Invalid statistics data received."
                                );
                            }
                        },
                        error -> {

                            String message =
                                    "Unable to connect to the Hazentra server.";

                            if (
                                    error.networkResponse
                                            != null
                            ) {

                                message += " Error code: "
                                        + error.networkResponse.statusCode;
                            }

                            showErrorState(message);
                        }
                ) {

                    @Override
                    public java.util.Map<String, String> getHeaders() {

                        java.util.Map<String, String> headers =
                                new java.util.HashMap<>();

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

    private void displayStatistics(
            @NonNull JSONArray hazards
    ) throws JSONException {

        int roadCount = 0;
        int environmentalCount = 0;
        int buildingCount = 0;

        JSONObject latestHazard = null;
        Date latestDate = null;

        SimpleDateFormat serverDateFormat =
                new SimpleDateFormat(
                        "yyyy-MM-dd HH:mm:ss",
                        Locale.US
                );

        for (
                int index = 0;
                index < hazards.length();
                index++
        ) {

            JSONObject hazard =
                    hazards.getJSONObject(index);

            String categoryName =
                    hazard.optString(
                            "category_name",
                            ""
                    );

            switch (categoryName) {

                case "Road Hazard":
                    roadCount++;
                    break;

                case "Environmental Hazard":
                    environmentalCount++;
                    break;

                case "Building Hazard":
                    buildingCount++;
                    break;
            }

            String reportDate =
                    hazard.optString(
                            "report_datetime",
                            ""
                    );

            try {

                Date parsedDate =
                        serverDateFormat.parse(
                                reportDate
                        );

                if (
                        parsedDate != null
                                && (
                                latestDate == null
                                        || parsedDate.after(latestDate)
                        )
                ) {

                    latestDate = parsedDate;
                    latestHazard = hazard;
                }

            } catch (ParseException ignored) {
                // Skip invalid dates and continue.
            }
        }

        totalHazardsText.setText(
                String.valueOf(hazards.length())
        );

        roadHazardsText.setText(
                String.valueOf(roadCount)
        );

        environmentalHazardsText.setText(
                String.valueOf(environmentalCount)
        );

        buildingHazardsText.setText(
                String.valueOf(buildingCount)
        );

        updateCategoryChart(
                roadCount,
                environmentalCount,
                buildingCount
        );

        statisticsLoadingIndicator.setVisibility(
                View.GONE
        );

        retryStatisticsButton.setVisibility(
                View.GONE
        );

        emptyStatisticsText.setVisibility(
                View.GONE
        );

        if (latestHazard == null) {
            latestHazard =
                    hazards.getJSONObject(0);
        }

        displayLatestHazard(latestHazard);
    }

    private void updateCategoryChart(
            int roadCount,
            int environmentalCount,
            int buildingCount
    ) {

        roadChartCountText.setText(
                String.valueOf(roadCount)
        );

        environmentalChartCountText.setText(
                String.valueOf(environmentalCount)
        );

        buildingChartCountText.setText(
                String.valueOf(buildingCount)
        );

        int maximumCount =
                Math.max(
                        roadCount,
                        Math.max(
                                environmentalCount,
                                buildingCount
                        )
                );

        if (maximumCount <= 0) {

            resetBarWidth(roadBarFill);
            resetBarWidth(environmentalBarFill);
            resetBarWidth(buildingBarFill);

            return;
        }

        animateBar(
                roadBarTrack,
                roadBarFill,
                roadCount,
                maximumCount
        );

        animateBar(
                environmentalBarTrack,
                environmentalBarFill,
                environmentalCount,
                maximumCount
        );

        animateBar(
                buildingBarTrack,
                buildingBarFill,
                buildingCount,
                maximumCount
        );
    }

    private void animateBar(
            @NonNull FrameLayout track,
            @NonNull View fill,
            int value,
            int maximumValue
    ) {

        track.post(
                () -> {

                    int trackWidth =
                            track.getWidth();

                    int targetWidth =
                            maximumValue == 0
                                    ? 0
                                    : Math.round(
                                    trackWidth
                                            * (
                                            value
                                                    / (float) maximumValue
                                    )
                            );

                    ValueAnimator animator =
                            ValueAnimator.ofInt(
                                    0,
                                    targetWidth
                            );

                    animator.setDuration(650);
                    animator.setInterpolator(
                            new DecelerateInterpolator()
                    );

                    animator.addUpdateListener(
                            animation -> {

                                ViewGroup.LayoutParams layoutParams =
                                        fill.getLayoutParams();

                                layoutParams.width =
                                        (int) animation.getAnimatedValue();

                                fill.setLayoutParams(layoutParams);
                            }
                    );

                    animator.start();
                }
        );
    }

    private void resetBarWidth(
            @NonNull View barFill
    ) {

        ViewGroup.LayoutParams layoutParams =
                barFill.getLayoutParams();

        layoutParams.width = 0;
        barFill.setLayoutParams(layoutParams);
    }

    private void displayLatestHazard(
            @NonNull JSONObject latestHazard
    ) {

        String categoryName =
                latestHazard.optString(
                        "category_name",
                        "Hazard"
                );

        latestCategoryText.setText(categoryName);

        setLatestCategoryColor(categoryName);

        latestLocationText.setText(
                "Location: "
                        + latestHazard.optString(
                        "location_name",
                        "Unknown location"
                )
        );

        latestDescriptionText.setText(
                latestHazard.optString(
                        "description",
                        "No description available."
                )
        );

        latestReporterText.setText(
                "Reported by: "
                        + latestHazard.optString(
                        "user_name",
                        "Unknown reporter"
                )
        );

        latestDateText.setText(
                "Reported on: "
                        + formatReportDate(
                        latestHazard.optString(
                                "report_datetime",
                                ""
                        )
                )
        );

        latestReportCard.setVisibility(
                View.VISIBLE
        );
    }

    private void setLatestCategoryColor(
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

        latestCategoryText.setTextColor(
                ContextCompat.getColor(
                        this,
                        colorResource
                )
        );
    }

    private void showLoadingState() {

        statisticsLoadingIndicator.setVisibility(
                View.VISIBLE
        );

        retryStatisticsButton.setVisibility(
                View.GONE
        );

        emptyStatisticsText.setVisibility(
                View.GONE
        );

        latestReportCard.setVisibility(
                View.GONE
        );
    }

    private void showEmptyState() {

        statisticsLoadingIndicator.setVisibility(
                View.GONE
        );

        retryStatisticsButton.setVisibility(
                View.GONE
        );

        latestReportCard.setVisibility(
                View.GONE
        );

        emptyStatisticsText.setVisibility(
                View.VISIBLE
        );

        totalHazardsText.setText("0");
        roadHazardsText.setText("0");
        environmentalHazardsText.setText("0");
        buildingHazardsText.setText("0");

        updateCategoryChart(0, 0, 0);
    }

    private void showErrorState(
            @NonNull String message
    ) {

        statisticsLoadingIndicator.setVisibility(
                View.GONE
        );

        latestReportCard.setVisibility(
                View.GONE
        );

        emptyStatisticsText.setVisibility(
                View.VISIBLE
        );

        retryStatisticsButton.setVisibility(
                View.VISIBLE
        );

        emptyStatisticsText.setText(message);

        Toast.makeText(
                this,
                message,
                Toast.LENGTH_LONG
        ).show();
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
            // Return the original date below.
        }

        return serverDate;
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