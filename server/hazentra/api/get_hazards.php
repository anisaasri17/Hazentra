<?php

header("Content-Type: application/json; charset=UTF-8");

header("Access-Control-Allow-Origin: *");
header("Access-Control-Allow-Methods: GET");
header("Access-Control-Allow-Headers: Content-Type");

require_once "../config/database.php";

$sql = "
    SELECT
        hr.report_id,
        hr.user_name,
        hr.report_datetime,
        hr.user_agent,
        hr.location_name,
        hr.latitude,
        hr.longitude,
        hr.description,
        hr.report_status,
        hc.category_id,
        hc.category_name,
        hc.marker_color,
        hc.marker_icon
    FROM hazard_reports AS hr
    INNER JOIN hazard_categories AS hc
        ON hr.category_id = hc.category_id
    WHERE hr.report_status = 'Active'
    ORDER BY hr.report_datetime DESC
";

$result = $conn->query($sql);

if (!$result) {
    http_response_code(500);

    echo json_encode([
        "success" => false,
        "message" => "Unable to retrieve hazard reports."
    ]);

    $conn->close();
    exit;
}

$hazards = [];

while ($row = $result->fetch_assoc()) {
    $hazards[] = [
        "report_id" => (int) $row["report_id"],
        "user_name" => $row["user_name"],
        "report_datetime" => $row["report_datetime"],
        "user_agent" => $row["user_agent"],
        "location_name" => $row["location_name"],
        "latitude" => (float) $row["latitude"],
        "longitude" => (float) $row["longitude"],
        "category_id" => (int) $row["category_id"],
        "category_name" => $row["category_name"],
        "marker_color" => $row["marker_color"],
        "marker_icon" => $row["marker_icon"],
        "description" => $row["description"],
        "report_status" => $row["report_status"]
    ];
}

echo json_encode([
    "success" => true,
    "count" => count($hazards),
    "hazards" => $hazards
], JSON_PRETTY_PRINT | JSON_UNESCAPED_UNICODE);

$conn->close();