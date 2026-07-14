<?php

error_reporting(E_ALL);
ini_set("display_errors", "1");

header("Content-Type: application/json; charset=UTF-8");
header("Access-Control-Allow-Origin: *");
header("Access-Control-Allow-Methods: POST");
header("Access-Control-Allow-Headers: Content-Type");

require_once "../config/database.php";

function sendJson(
    int $statusCode,
    bool $success,
    string $message,
    array $extra = []
): void {
    http_response_code($statusCode);

    echo json_encode(
        array_merge(
            [
                "success" => $success,
                "message" => $message
            ],
            $extra
        ),
        JSON_UNESCAPED_UNICODE
    );

    exit;
}

if ($_SERVER["REQUEST_METHOD"] !== "POST") {
    sendJson(
        405,
        false,
        "Only POST requests are allowed."
    );
}

$userName = trim($_POST["user_name"] ?? "");
$userAgent = trim($_POST["user_agent"] ?? "");
$locationName = trim($_POST["location_name"] ?? "");
$latitude = trim($_POST["latitude"] ?? "");
$longitude = trim($_POST["longitude"] ?? "");
$categoryId = trim($_POST["category_id"] ?? "");
$description = trim($_POST["description"] ?? "");

if (
    $userName === ""
    || $userAgent === ""
    || $locationName === ""
    || $latitude === ""
    || $longitude === ""
    || $categoryId === ""
    || $description === ""
) {
    sendJson(
        400,
        false,
        "All fields are required."
    );
}

if (
    !is_numeric($latitude)
    || !is_numeric($longitude)
) {
    sendJson(
        400,
        false,
        "Latitude and longitude must be numeric."
    );
}

if (!ctype_digit($categoryId)) {
    sendJson(
        400,
        false,
        "Invalid category ID."
    );
}

$latitude = (float) $latitude;
$longitude = (float) $longitude;
$categoryId = (int) $categoryId;

if ($latitude < -90 || $latitude > 90) {
    sendJson(
        400,
        false,
        "Latitude must be between -90 and 90."
    );
}

if ($longitude < -180 || $longitude > 180) {
    sendJson(
        400,
        false,
        "Longitude must be between -180 and 180."
    );
}

/*
|--------------------------------------------------------------------------
| Validate category
|--------------------------------------------------------------------------
*/

$categoryCheck = $conn->prepare(
    "SELECT category_id
     FROM hazard_categories
     WHERE category_id = ?"
);

if (!$categoryCheck) {
    sendJson(
        500,
        false,
        "Unable to prepare category validation: "
        . $conn->error
    );
}

$categoryCheck->bind_param("i", $categoryId);

if (!$categoryCheck->execute()) {
    sendJson(
        500,
        false,
        "Unable to validate category: "
        . $categoryCheck->error
    );
}

$categoryCheck->store_result();

if ($categoryCheck->num_rows === 0) {
    $categoryCheck->close();

    sendJson(
        400,
        false,
        "The selected category does not exist."
    );
}

$categoryCheck->close();

/*
|--------------------------------------------------------------------------
| Insert hazard report
|--------------------------------------------------------------------------
*/

$sql = "
    INSERT INTO hazard_reports (
        user_name,
        user_agent,
        location_name,
        latitude,
        longitude,
        category_id,
        description,
        report_status
    )
    VALUES (?, ?, ?, ?, ?, ?, ?, 'Active')
";

$stmt = $conn->prepare($sql);

if (!$stmt) {
    sendJson(
        500,
        false,
        "Unable to prepare report submission: "
        . $conn->error
    );
}

$stmt->bind_param(
    "sssddis",
    $userName,
    $userAgent,
    $locationName,
    $latitude,
    $longitude,
    $categoryId,
    $description
);

if (!$stmt->execute()) {
    sendJson(
        500,
        false,
        "Unable to save the hazard report: "
        . $stmt->error
    );
}

$newReportId = $stmt->insert_id;

$stmt->close();
$conn->close();

sendJson(
    201,
    true,
    "Hazard report submitted successfully.",
    [
        "report_id" => $newReportId
    ]
);