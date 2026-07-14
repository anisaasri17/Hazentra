package com.nisa.hazentra;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class HazardReportAdapter
        extends RecyclerView.Adapter<HazardReportAdapter.HazardViewHolder> {

    private final List<JSONObject> hazardReports =
            new ArrayList<>();

    public void replaceData(
            @NonNull List<JSONObject> reports
    ) {

        hazardReports.clear();
        hazardReports.addAll(reports);

        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public HazardViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {

        View itemView =
                LayoutInflater
                        .from(parent.getContext())
                        .inflate(
                                R.layout.item_hazard_report,
                                parent,
                                false
                        );

        return new HazardViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(
            @NonNull HazardViewHolder holder,
            int position
    ) {

        JSONObject hazard =
                hazardReports.get(position);

        String categoryName =
                hazard.optString(
                        "category_name",
                        "Hazard"
                );

        holder.categoryText.setText(categoryName);
        holder.setCategoryColor(categoryName);

        holder.statusText.setText(
                hazard.optString(
                        "report_status",
                        "Active"
                )
        );

        holder.locationText.setText(
                "Location: "
                        + hazard.optString(
                        "location_name",
                        "Unknown location"
                )
        );

        holder.descriptionText.setText(
                hazard.optString(
                        "description",
                        "No description available."
                )
        );

        holder.reporterText.setText(
                "Reported by: "
                        + hazard.optString(
                        "user_name",
                        "Unknown reporter"
                )
        );

        holder.dateText.setText(
                "Reported on: "
                        + HazardListActivity.formatReportDate(
                        hazard.optString(
                                "report_datetime",
                                ""
                        )
                )
        );

        double latitude =
                hazard.optDouble(
                        "latitude",
                        0.0
                );

        double longitude =
                hazard.optDouble(
                        "longitude",
                        0.0
                );

        holder.coordinatesText.setText(
                String.format(
                        java.util.Locale.US,
                        "Coordinates: %.6f, %.6f",
                        latitude,
                        longitude
                )
        );

        holder.deviceText.setText(
                "Device: "
                        + hazard.optString(
                        "user_agent",
                        "Unknown device"
                )
        );
    }

    @Override
    public int getItemCount() {
        return hazardReports.size();
    }

    static class HazardViewHolder
            extends RecyclerView.ViewHolder {

        private final TextView categoryText;
        private final TextView statusText;
        private final TextView locationText;
        private final TextView descriptionText;
        private final TextView reporterText;
        private final TextView dateText;
        private final TextView coordinatesText;
        private final TextView deviceText;

        HazardViewHolder(
                @NonNull View itemView
        ) {
            super(itemView);

            categoryText =
                    itemView.findViewById(
                            R.id.itemCategoryText
                    );

            statusText =
                    itemView.findViewById(
                            R.id.itemStatusText
                    );

            locationText =
                    itemView.findViewById(
                            R.id.itemLocationText
                    );

            descriptionText =
                    itemView.findViewById(
                            R.id.itemDescriptionText
                    );

            reporterText =
                    itemView.findViewById(
                            R.id.itemReporterText
                    );

            dateText =
                    itemView.findViewById(
                            R.id.itemDateText
                    );

            coordinatesText =
                    itemView.findViewById(
                            R.id.itemCoordinatesText
                    );

            deviceText =
                    itemView.findViewById(
                            R.id.itemDeviceText
                    );
        }

        void setCategoryColor(
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
                            itemView.getContext(),
                            colorResource
                    )
            );
        }
    }
}