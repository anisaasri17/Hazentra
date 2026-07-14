<?php

require_once "../config/database.php";

$userName = $_POST["user_name"];
$categoryId = $_POST["category_id"];
$location = $_POST["location_name"];
$latitude = $_POST["latitude"];
$longitude = $_POST["longitude"];
$description = $_POST["description"];
$status = $_POST["report_status"];
$userAgent = $_POST["user_agent"];

$sql = "
    INSERT INTO hazard_reports
    (
        user_name,
        category_id,
        location_name,
        latitude,
        longitude,
        description,
        report_status,
        user_agent,
        report_datetime
    )
    VALUES
    (
        ?,
        ?,
        ?,
        ?,
        ?,
        ?,
        ?,
        ?,
        NOW()
    )
";

$stmt = $conn->prepare($sql);

$stmt->bind_param(
    "sisddsss",
    $userName,
    $categoryId,
    $location,
    $latitude,
    $longitude,
    $description,
    $status,
    $userAgent
);

$stmt->execute();

$stmt->close();

$conn->close();

header(
    "Location: index.php?success="
    . urlencode("Hazard report added successfully.")
);

exit;