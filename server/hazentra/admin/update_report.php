<?php

require_once "../config/database.php";

function redirectWithError(string $message): never
{
    header(
        "Location: index.php?error=" . urlencode($message)
    );

    exit;
}

if ($_SERVER["REQUEST_METHOD"] !== "POST") {
    redirectWithError("Invalid request method.");
}

$reportId = $_POST["report_id"] ?? "";
$userName = trim($_POST["user_name"] ?? "");
$reportDatetime = trim($_POST["report_datetime"] ?? "");
$userAgent = trim($_POST["user_agent"] ?? "");
$locationName = trim($_POST["location_name"] ?? "");
$latitude = trim($_POST["latitude"] ?? "");
$longitude = trim($_POST["longitude"] ?? "");
$categoryId = $_POST["category_id"] ?? "";
$description = trim($_POST["description"] ?? "");
$reportStatus = trim($_POST["report_status"] ?? "");

$allowedStatuses = [
    "Active",
    "Resolved",
    "Rejected"
];

if (
    !ctype_digit((string) $reportId)
    || !ctype_digit((string) $categoryId)
) {
    redirectWithError("Invalid report or category ID.");
}

if (
    $userName === ""
    || $reportDatetime === ""
    || $userAgent === ""
    || $locationName === ""
    || $description === ""
) {
    redirectWithError("All report fields are required.");
}

if (
    !is_numeric($latitude)
    || !is_numeric($longitude)
) {
    redirectWithError("Invalid GPS coordinates.");
}

if (!in_array($reportStatus, $allowedStatuses, true)) {
    redirectWithError("Invalid report status.");
}

$reportId = (int) $reportId;
$categoryId = (int) $categoryId;
$latitude = (float) $latitude;
$longitude = (float) $longitude;

$reportTimestamp = strtotime($reportDatetime);

if ($reportTimestamp === false) {
    redirectWithError("Invalid report date and time.");
}

$reportDatetime = date(
    "Y-m-d H:i:s",
    $reportTimestamp
);

if ($latitude < -90 || $latitude > 90) {
    redirectWithError("Latitude must be between -90 and 90.");
}

if ($longitude < -180 || $longitude > 180) {
    redirectWithError("Longitude must be between -180 and 180.");
}

$categoryCheck = $conn->prepare(
    "SELECT category_id
     FROM hazard_categories
     WHERE category_id = ?"
);

if (!$categoryCheck) {
    redirectWithError("Unable to validate the category.");
}

$categoryCheck->bind_param("i", $categoryId);
$categoryCheck->execute();

$categoryResult = $categoryCheck->get_result();

if ($categoryResult->num_rows === 0) {
    $categoryCheck->close();

    redirectWithError(
        "The selected hazard category does not exist."
    );
}

$categoryCheck->close();

$sql = "
    UPDATE hazard_reports
    SET
        user_name = ?,
        report_datetime = ?,
        user_agent = ?,
        location_name = ?,
        latitude = ?,
        longitude = ?,
        category_id = ?,
        description = ?,
        report_status = ?
    WHERE report_id = ?
";

$stmt = $conn->prepare($sql);

if (!$stmt) {
    redirectWithError(
        "Unable to prepare the report update."
    );
}

$stmt->bind_param(
    "ssssddissi",
    $userName,
    $reportDatetime,
    $userAgent,
    $locationName,
    $latitude,
    $longitude,
    $categoryId,
    $description,
    $reportStatus,
    $reportId
);

if (!$stmt->execute()) {
    $stmt->close();
    $conn->close();

    redirectWithError(
        "Unable to update the hazard report."
    );
}

$stmt->close();
$conn->close();

header(
    "Location: index.php?success="
    . urlencode("Hazard report updated successfully.")
);

exit;