package com.nisa.hazentra;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HazardListActivity extends AppCompatActivity {

    private static final String HAZARDS_URL =
            ApiConfig.GET_HAZARDS;

    private RecyclerView hazardReportsRecyclerView;
    private TextView reportCountText;
    private TextView emptyStateTitle;
    private TextView emptyStateMessage;

    private View emptyStateContainer;

    private MaterialButton retryHazardListButton;
    private CircularProgressIndicator hazardListLoadingIndicator;

    private HazardReportAdapter hazardReportAdapter;
    private RequestQueue requestQueue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_hazard_list);

        initialiseViews();
        configureToolbar();
        configureRecyclerView();
        configureRetryButton();

        requestQueue =
                Volley.newRequestQueue(
                        getApplicationContext()
                );

        loadHazardReports();
    }

    private void initialiseViews() {

        hazardReportsRecyclerView =
                findViewById(
                        R.id.hazardReportsRecyclerView
                );

        reportCountText =
                findViewById(
                        R.id.reportCountText
                );

        emptyStateContainer =
                findViewById(
                        R.id.emptyStateContainer
                );

        emptyStateTitle =
                findViewById(
                        R.id.emptyStateTitle
                );

        emptyStateMessage =
                findViewById(
                        R.id.emptyStateMessage
                );

        retryHazardListButton =
                findViewById(
                        R.id.retryHazardListButton
                );

        hazardListLoadingIndicator =
                findViewById(
                        R.id.hazardListLoadingIndicator
                );
    }

    private void configureToolbar() {

        MaterialToolbar toolbar =
                findViewById(R.id.hazardListToolbar);

        toolbar.setTitle("Hazard Reports");

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

    private void configureRecyclerView() {

        hazardReportAdapter =
                new HazardReportAdapter();

        hazardReportsRecyclerView.setLayoutManager(
                new LinearLayoutManager(this)
        );

        hazardReportsRecyclerView.setHasFixedSize(
                false
        );

        hazardReportsRecyclerView.setAdapter(
                hazardReportAdapter
        );
    }

    private void configureRetryButton() {

        retryHazardListButton.setOnClickListener(
                view -> loadHazardReports()
        );
    }

    private void loadHazardReports() {

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
                                                    "Unable to load hazard reports."
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

                                List<JSONObject> hazardList =
                                        new ArrayList<>();

                                for (
                                        int index = 0;
                                        index < hazards.length();
                                        index++
                                ) {

                                    hazardList.add(
                                            hazards.getJSONObject(index)
                                    );
                                }

                                showReportList(hazardList);

                            } catch (JSONException exception) {

                                showErrorState(
                                        "Invalid hazard report data received."
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

    private void showLoadingState() {

        hazardListLoadingIndicator.setVisibility(
                View.VISIBLE
        );

        hazardReportsRecyclerView.setVisibility(
                View.GONE
        );

        emptyStateContainer.setVisibility(
                View.GONE
        );

        retryHazardListButton.setVisibility(
                View.GONE
        );

        reportCountText.setText(
                "Loading reports..."
        );
    }

    private void showReportList(
            @NonNull List<JSONObject> reports
    ) {

        hazardListLoadingIndicator.setVisibility(
                View.GONE
        );

        emptyStateContainer.setVisibility(
                View.GONE
        );

        retryHazardListButton.setVisibility(
                View.GONE
        );

        hazardReportsRecyclerView.setVisibility(
                View.VISIBLE
        );

        hazardReportAdapter.replaceData(reports);

        reportCountText.setText(
                reports.size()
                        + (
                        reports.size() == 1
                                ? " active report"
                                : " active reports"
                )
        );
    }

    private void showEmptyState() {

        hazardListLoadingIndicator.setVisibility(
                View.GONE
        );

        hazardReportsRecyclerView.setVisibility(
                View.GONE
        );

        retryHazardListButton.setVisibility(
                View.GONE
        );

        emptyStateContainer.setVisibility(
                View.VISIBLE
        );

        emptyStateTitle.setText(
                "No active hazard reports"
        );

        emptyStateMessage.setText(
                "There are currently no active reports available."
        );

        reportCountText.setText(
                "0 active reports"
        );
    }

    private void showErrorState(
            @NonNull String message
    ) {

        hazardListLoadingIndicator.setVisibility(
                View.GONE
        );

        hazardReportsRecyclerView.setVisibility(
                View.GONE
        );

        emptyStateContainer.setVisibility(
                View.VISIBLE
        );

        retryHazardListButton.setVisibility(
                View.VISIBLE
        );

        emptyStateTitle.setText(
                "Unable to load reports"
        );

        emptyStateMessage.setText(message);

        reportCountText.setText(
                "Reports unavailable"
        );

        Toast.makeText(
                this,
                message,
                Toast.LENGTH_LONG
        ).show();
    }

    public static String formatReportDate(
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