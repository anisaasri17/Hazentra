<?php

$host = "localhost";
$username = "root";
$password = "";
$database = "hazentra_db";

$conn = new mysqli(
    $host,
    $username,
    $password,
    $database
);

if ($conn->connect_error) {

    http_response_code(500);

    echo json_encode([
        "success" => false,
        "message" => "Database connection failed."
    ]);

    exit;
}

$conn->set_charset("utf8mb4");