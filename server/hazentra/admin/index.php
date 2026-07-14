<?php

require_once "../config/database.php";

$search = trim($_GET["search"] ?? "");
$categoryId = trim($_GET["category_id"] ?? "");
$status = trim($_GET["status"] ?? "");

$allowedStatuses = [
    "Active",
    "Resolved",
    "Rejected"
];

/*Retrieve hazard categories*/

$categorySql = "
    SELECT
        category_id,
        category_name
    FROM hazard_categories
    ORDER BY category_id
";

$categoryResult = $conn->query($categorySql);

if (!$categoryResult) {
    die("Unable to retrieve hazard categories.");
}

$categories = [];

while ($category = $categoryResult->fetch_assoc()) {
    $categories[] = $category;
}

/*Build hazard reports query*/

$sql = "
    SELECT
        hr.report_id,
        hr.user_name,
        hr.report_datetime,
        hr.user_agent,
        hr.location_name,
        hr.latitude,
        hr.longitude,
        hr.category_id,
        hr.description,
        hr.report_status,
        hc.category_name
    FROM hazard_reports AS hr
    INNER JOIN hazard_categories AS hc
        ON hr.category_id = hc.category_id
    WHERE 1 = 1
";

$params = [];
$types = "";

if ($search !== "") {
    $sql .= "
        AND (
            hr.user_name LIKE ?
            OR hr.location_name LIKE ?
            OR hr.description LIKE ?
            OR hr.user_agent LIKE ?
        )
    ";

    $searchValue = "%" . $search . "%";

    $params[] = $searchValue;
    $params[] = $searchValue;
    $params[] = $searchValue;
    $params[] = $searchValue;

    $types .= "ssss";
}

if ($categoryId !== "" && ctype_digit($categoryId)) {
    $sql .= " AND hr.category_id = ?";

    $params[] = (int) $categoryId;
    $types .= "i";
}

if (
    $status !== ""
    && in_array($status, $allowedStatuses, true)
) {
    $sql .= " AND hr.report_status = ?";

    $params[] = $status;
    $types .= "s";
}

$sql .= " ORDER BY hr.report_datetime DESC";

$stmt = $conn->prepare($sql);

if (!$stmt) {
    die("Unable to prepare report query.");
}

if (!empty($params)) {
    $stmt->bind_param($types, ...$params);
}

$stmt->execute();

$reportResult = $stmt->get_result();

/*Dashboard summary*/

$summarySql = "
    SELECT
        COUNT(*) AS total_reports,
        COALESCE(SUM(report_status = 'Active'), 0) AS active_reports,
        COALESCE(SUM(report_status = 'Resolved'), 0) AS resolved_reports,
        COALESCE(SUM(report_status = 'Rejected'), 0) AS rejected_reports
    FROM hazard_reports
";

$summaryResult = $conn->query($summarySql);

if (!$summaryResult) {
    die("Unable to retrieve dashboard summary.");
}

$summary = $summaryResult->fetch_assoc();

$activePercent = 0;
$resolvedPercent = 0;
$rejectedPercent = 0;

if ($summary["total_reports"] > 0) {

    $activePercent =
        ($summary["active_reports"] / $summary["total_reports"]) * 100;

    $resolvedPercent =
        ($summary["resolved_reports"] / $summary["total_reports"]) * 100;

    $rejectedPercent =
        ($summary["rejected_reports"] / $summary["total_reports"]) * 100;
}

/* Chart - Reports by Category */

$categoryChartSql = "
    SELECT
        hc.category_name,
        COUNT(hr.report_id) AS total
    FROM hazard_categories hc
    LEFT JOIN hazard_reports hr
        ON hc.category_id = hr.category_id
    GROUP BY hc.category_id, hc.category_name
    ORDER BY hc.category_id
";

$categoryChartResult = $conn->query($categoryChartSql);

$categoryLabels = [];
$categoryTotals = [];

while ($row = $categoryChartResult->fetch_assoc()) {
    $categoryLabels[] = $row["category_name"];
    $categoryTotals[] = (int) $row["total"];
}

/* Trend Over time */

$trendSql = "
SELECT
    DATE(report_datetime) AS report_date,
    COUNT(*) AS total
FROM hazard_reports
GROUP BY DATE(report_datetime)
ORDER BY DATE(report_datetime)
";

$trendResult = $conn->query($trendSql);

$trendLabels = [];
$trendData = [];

while($row = $trendResult->fetch_assoc()){

    $trendLabels[] = date(
        "d M",
        strtotime($row["report_date"])
    );

    $trendData[] = (int)$row["total"];

}

$successMessage = trim($_GET["success"] ?? "");
$errorMessage = trim($_GET["error"] ?? "");

$latestSql = "
SELECT
    user_name,
    location_name,
    report_datetime,
    report_status,
    hc.category_name
FROM hazard_reports hr
INNER JOIN hazard_categories hc
ON hr.category_id = hc.category_id
ORDER BY hr.report_datetime DESC
LIMIT 1
";

$latestResult = $conn->query($latestSql);

$latestReport = $latestResult->fetch_assoc();
?>
<!DOCTYPE html>
<html lang="en">

<head>
    <meta charset="UTF-8">

    <meta
        name="viewport"
        content="width=device-width, initial-scale=1.0"
    >

    <meta
        name="description"
        content="Hazentra hazard monitoring and management dashboard."
    >

    <title>Hazentra Admin Dashboard</title>

    <link
        href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css"
        rel="stylesheet"
    >

    <link
        href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.min.css"
        rel="stylesheet"
    >

    <link
        href="../assets/css/admin.css"
        rel="stylesheet"
    >
</head>

<body>

<div
    class="sidebar-overlay"
    id="sidebarOverlay"
></div>

<div class="dashboard-layout">

    <!-- SIDEBAR -->
    <aside
        class="sidebar"
        id="sidebar"
    >

        <div class="sidebar-brand">

            <a
                href="index.php"
                class="brand-link"
                aria-label="Hazentra dashboard"
            >
                <div class="brand-image-frame">

                    <img
                        src="../assets/images/hazentra_logo.png"
                        alt="Hazentra Hazard Management System"
                        class="sidebar-logo"
                    >

                </div>
            </a>

            <button
                type="button"
                class="sidebar-close"
                id="sidebarClose"
                aria-label="Close sidebar"
            >
                <i class="bi bi-x-lg"></i>
            </button>

        </div>

        <nav class="sidebar-navigation">

            <span class="navigation-label">
                Overview
            </span>

            <a
                href="index.php"
                class="navigation-item active"
            >
                <i class="bi bi-grid-1x2-fill"></i>
                <span>Dashboard</span>
            </a>

            <span class="navigation-label">
                Management
            </span>

            <a
                href="#reports"
                class="navigation-item"
            >
                <i class="bi bi-clipboard2-data"></i>
                <span>Hazard Reports</span>
            </a>

            <a
                href="#filters"
                class="navigation-item"
            >
                <i class="bi bi-funnel"></i>
                <span>Report Filters</span>
            </a>

            <a
                href="#reports"
                class="navigation-item"
            >
                <i class="bi bi-shield-check"></i>
                <span>Status Management</span>
            </a>

            <span class="navigation-label">
                System
            </span>

            <a
                href="../api/get_hazards.php"
                target="_blank"
                rel="noopener noreferrer"
                class="navigation-item"
            >
                <i class="bi bi-braces"></i>
                <span>API Access</span>
            </a>

        </nav>

        <div class="sidebar-footer">

            <div class="sidebar-user">

                <div class="sidebar-avatar">
                    A
                </div>

                <div class="sidebar-user-details">
                    <strong>Administrator</strong>
                    <span>Hazentra Control Centre</span>
                </div>

            </div>

            <div class="system-status">
                <span class="online-dot"></span>
                System online
            </div>

        </div>

    </aside>

    <!-- MAIN DASHBOARD -->
    <div class="dashboard-main">

        <header class="topbar">

            <button
                type="button"
                class="sidebar-toggle"
                id="sidebarToggle"
                aria-label="Toggle sidebar"
                aria-expanded="true"
            >
                <i
                    class="bi bi-layout-sidebar"
                    id="sidebarToggleIcon"
                ></i>
            </button>

            <div class="topbar-actions">

                <a
                    href="../api/get_hazards.php"
                    target="_blank"
                    rel="noopener noreferrer"
                    class="api-status-button"
                >
                    <span class="online-dot"></span>
                    API Status
                </a>

                <div class="topbar-divider"></div>

                <div class="topbar-profile">

                    <div class="topbar-avatar">
                        A
                    </div>

                    <div class="topbar-profile-text">
                        <strong>Administrator</strong>
                        <span>Hazentra Control Centre</span>
                    </div>

                </div>

            </div>

        </header>

        <main class="dashboard-content">

            <!-- HEADER -->
            <section class="dashboard-header">

                <div class="dashboard-heading">

                    <div class="section-label">
                        <span class="live-indicator"></span>
                        Live monitoring dashboard
                    </div>

                    <h1>Hazard overview</h1>

                    <p>
                        Monitor, review and manage crowdsourced road,
                        environmental and building hazard reports submitted
                        through the Hazentra Android application.
                    </p>

                </div>

                <div class="dashboard-header-actions">

                    <a
                        href="../api/get_hazards.php"
                        target="_blank"
                        rel="noopener noreferrer"
                        class="secondary-action"
                    >
                        <i class="bi bi-box-arrow-up-right"></i>
                        View API
                    </a>

                    <button
                        type="button"
                        class="primary-action"
                        onclick="window.location.reload();"
                    >
                        <i class="bi bi-arrow-clockwise"></i>
                        Refresh
                    </button>

                </div>

            </section>

            <!-- ALERTS -->
            <?php if ($successMessage !== ""): ?>

                <div
                    id="successAlert"
                    class="alert alert-success custom-alert"
                    role="alert"
                >
                    <i class="bi bi-check-circle-fill"></i>

                    <span>
                        <?= htmlspecialchars($successMessage); ?>
                    </span>
                </div>

            <?php endif; ?>

            <?php if ($errorMessage !== ""): ?>

                <div
                    id="errorAlert"
                    class="alert alert-danger custom-alert"
                    role="alert"
                >
                    <i class="bi bi-exclamation-circle-fill"></i>

                    <span>
                        <?= htmlspecialchars($errorMessage); ?>
                    </span>
                </div>

            <?php endif; ?>

            <!-- SUMMARY CARDS -->
            <section class="summary-grid">

                <article class="summary-card total-card">

                    <div class="summary-card-header">

                        <div class="summary-card-icon">
                            <i class="bi bi-file-earmark-text"></i>
                        </div>

                        <span>Total reports</span>

                    </div>

                    <strong>
                        <?= (int) $summary["total_reports"]; ?>
                    </strong>

                    <p>All submitted records</p>

                </article>

                <article class="summary-card active-card">

                    <div class="summary-card-header">

                        <div class="summary-card-icon">
                            <i class="bi bi-exclamation-triangle"></i>
                        </div>

                        <span>Active hazards</span>

                    </div>

                    <strong>
                        <?= (int) $summary["active_reports"]; ?>
                    </strong>

                    <p>Require attention</p>

                </article>

                <article class="summary-card resolved-card">

                    <div class="summary-card-header">

                        <div class="summary-card-icon">
                            <i class="bi bi-shield-check"></i>
                        </div>

                        <span>Resolved reports</span>

                    </div>

                    <strong>
                        <?= (int) $summary["resolved_reports"]; ?>
                    </strong>

                    <p>Successfully handled</p>

                </article>

                <article class="summary-card rejected-card">

                    <div class="summary-card-header">

                        <div class="summary-card-icon">
                            <i class="bi bi-shield-x"></i>
                        </div>

                        <span>Rejected reports</span>

                    </div>

                    <strong>
                        <?= (int) $summary["rejected_reports"]; ?>
                    </strong>

                    <p>Invalid submissions</p>

                </article>

            </section>

            <!-- DASHBOARD ANALYTICS -->
            <section class="analytics-grid">

                <!-- Category Chart -->
                <div class="analytics-card">

                    <div class="analytics-header">

                        <div>
                            <span class="panel-eyebrow">
                                Analytics
                            </span>

                            <h3>Hazard Reports by Category</h3>

                            <p class="analytics-description">
                                Distribution of hazard reports submitted for each hazard category.
                            </p>
                        </div>

                    </div>

                    <div class="chart-container">
                        <canvas id="categoryChart"></canvas>
                    </div>

                </div>

                <!-- Reports Over Time -->
                <div class="analytics-card">

                    <div class="analytics-header">

                        <div>

                            <span class="panel-eyebrow">
                                Analytics
                            </span>

                            <h3>Hazard Report Trend</h3>

                            <p class="analytics-description">
                                Daily trend of hazard reports submitted through the Hazentra mobile application.
                            </p>

                        </div>

                    </div>

                    <div class="chart-container">
                        <canvas id="trendChart"></canvas>
                    </div>

                </div>

            </section>

            <!-- FILTERS -->
            <section
                class="filter-panel"
                id="filters"
            >

                <div class="panel-heading">

                    <div>

                        <span class="panel-eyebrow">
                            Search and classification
                        </span>

                        <h2>Filter hazard reports</h2>

                    </div>

                    <span class="result-count">
                        <?= (int) $reportResult->num_rows; ?>
                        result(s)
                    </span>

                </div>

                <form
                    method="GET"
                    action="index.php"
                    class="filter-form"
                >

                    <div class="filter-field filter-search">

                        <label for="search">
                            Search reports
                        </label>

                        <div class="input-icon-wrapper">

                            <i class="bi bi-search"></i>

                            <input
                                type="text"
                                id="search"
                                name="search"
                                class="form-control"
                                placeholder="Reporter, location, description or device..."
                                value="<?= htmlspecialchars($search); ?>"
                            >

                        </div>

                    </div>

                    <div class="filter-field">

                        <label for="category_id">
                            Category
                        </label>

                        <select
                            id="category_id"
                            name="category_id"
                            class="form-select"
                        >

                            <option value="">
                                All categories
                            </option>

                            <?php foreach ($categories as $category): ?>
                                <option
                                    value="<?= (int) $category["category_id"]; ?>"
                                    <?=
                                        (string) $categoryId
                                        === (string) $category["category_id"]
                                        ? "selected"
                                        : "";
                                    ?>
                                >
                                    <?= htmlspecialchars(
                                        $category["category_name"]
                                    ); ?>
                                </option>

                            <?php endforeach; ?>

                        </select>

                    </div>

                    <div class="filter-field">

                        <label for="status">
                            Status
                        </label>

                        <select
                            id="status"
                            name="status"
                            class="form-select"
                        >

                            <option value="">
                                All statuses
                            </option>

                            <?php foreach (
                                $allowedStatuses as $allowedStatus
                            ): ?>

                                <option
                                    value="<?= htmlspecialchars(
                                        $allowedStatus
                                    ); ?>"
                                    <?=
                                        $status === $allowedStatus
                                        ? "selected"
                                        : "";
                                    ?>
                                >
                                    <?= htmlspecialchars($allowedStatus); ?>
                                </option>

                            <?php endforeach; ?>

                        </select>

                    </div>

                    <button
                        type="submit"
                        class="filter-button"
                    >
                        <i class="bi bi-funnel"></i>
                        Apply
                    </button>

                    <a
                        href="index.php"
                        class="clear-filter-button"
                    >
                        <i class="bi bi-arrow-counterclockwise"></i>
                        Reset
                    </a>

                </form>

            </section>

            <!-- REPORT TABLE -->
            <section
                class="report-panel"
                id="reports"
            >

                <div class="report-panel-header">

                    <div>

                        <span class="panel-eyebrow">
                            Submitted Reports
                        </span>

                        <h2>Hazard Report Records</h2>

                        <p>
                            View the essential report information below.
                            Open a report to review or update its complete details.
                        </p>

                    </div>

                    <div class="report-header-actions">

                        <span class="result-count">
                            <?= (int)$reportResult->num_rows; ?> report(s)
                        </span>

                        <button
                            type="button"
                            class="edit-report-button"
                            data-bs-toggle="modal"
                            data-bs-target="#addReportModal"
                        >
                            <i class="bi bi-plus-circle"></i>
                            New Report
                        </button>

                    </div>

                </div>

                <div class="table-responsive">

                    <table class="table hazard-table compact-table">

                        <thead>
                            <tr>
                                <th>No.</th>
                                <th>ID</th>
                                <th>Reporter</th>
                                <th>Date &amp; Time</th>
                                <th>Category</th>
                                <th>Location</th>
                                <th>Status</th>
                                <th class="action-heading">
                                    Action
                                </th>
                            </tr>
                        </thead>

                        <tbody>

                            <?php if ($reportResult->num_rows > 0): ?>

                                <?php $no = 1; ?>
                                
                                <?php while (
                                    $report = $reportResult->fetch_assoc()
                                ): ?>

                                    <?php

                                    $formattedDate = date(
                                        "d M Y, h:i A",
                                        strtotime(
                                            $report["report_datetime"]
                                        )
                                    );

                                    $statusClass = strtolower(
                                        $report["report_status"]
                                    );

                                    $categoryClass = match (
                                        $report["category_name"]
                                    ) {
                                        "Road Hazard"
                                            => "category-road",

                                        "Environmental Hazard"
                                            => "category-environment",

                                        "Building Hazard"
                                            => "category-building",

                                        default
                                            => "category-default"
                                    };

                                    $reporterInitial = strtoupper(
                                        substr(
                                            $report["user_name"],
                                            0,
                                            1
                                        )
                                    );

                                    ?>

                                    <tr>

                                        <td class="row-number">
                                            <?= $no++; ?>
                                        </td>

                                        <td class="report-id">
                                            <?= (int) $report["report_id"]; ?>
                                        </td>

                                        <td>

                                            <div class="reporter-cell">

                                                <div
                                                    class="reporter-avatar"
                                                    aria-hidden="true"
                                                >
                                                    <?= htmlspecialchars(
                                                        $reporterInitial
                                                    ); ?>
                                                </div>

                                                <strong>
                                                    <?= htmlspecialchars(
                                                        $report["user_name"]
                                                    ); ?>
                                                </strong>

                                            </div>

                                        </td>

                                        <td class="date-cell">
                                            <?= htmlspecialchars(
                                                $formattedDate
                                            ); ?>
                                        </td>

                                        <td>

                                            <span
                                                class="
                                                    category-badge
                                                    <?= $categoryClass; ?>
                                                "
                                            >
                                                <?= htmlspecialchars(
                                                    $report["category_name"]
                                                ); ?>
                                            </span>

                                        </td>

                                        <td>

                                            <div class="location-summary">

                                                <i
                                                    class="bi bi-geo-alt"
                                                    aria-hidden="true"
                                                ></i>

                                                <span>
                                                    <?= htmlspecialchars(
                                                        $report[
                                                            "location_name"
                                                        ]
                                                    ); ?>
                                                </span>

                                            </div>

                                        </td>

                                        <td>

                                            <span
                                                class="
                                                    status-badge
                                                    status-<?= $statusClass; ?>
                                                "
                                            >
                                                <span
                                                    class="status-dot"
                                                    aria-hidden="true"
                                                ></span>

                                                <?= htmlspecialchars(
                                                    $report["report_status"]
                                                ); ?>
                                            </span>

                                        </td>

                                        <td class="action-cell">

                                            <button
                                                type="button"
                                                class="edit-report-button"
                                                data-bs-toggle="modal"
                                                data-bs-target="#editReportModal"

                                                data-report-id="<?= (int) $report["report_id"]; ?>"

                                                data-user-name="<?= htmlspecialchars(
                                                    $report["user_name"],
                                                    ENT_QUOTES
                                                ); ?>"

                                                data-report-datetime="<?= htmlspecialchars(
                                                    $report["report_datetime"],
                                                    ENT_QUOTES
                                                ); ?>"

                                                data-user-agent="<?= htmlspecialchars(
                                                    $report["user_agent"],
                                                    ENT_QUOTES
                                                ); ?>"

                                                data-location-name="<?= htmlspecialchars(
                                                    $report["location_name"],
                                                    ENT_QUOTES
                                                ); ?>"

                                                data-latitude="<?= htmlspecialchars(
                                                    $report["latitude"],
                                                    ENT_QUOTES
                                                ); ?>"

                                                data-longitude="<?= htmlspecialchars(
                                                    $report["longitude"],
                                                    ENT_QUOTES
                                                ); ?>"

                                                data-category-id="<?= (int) $report["category_id"]; ?>"

                                                data-description="<?= htmlspecialchars(
                                                    $report["description"],
                                                    ENT_QUOTES
                                                ); ?>"

                                                data-report-status="<?= htmlspecialchars(
                                                    $report["report_status"],
                                                    ENT_QUOTES
                                                ); ?>"
                                            >
                                                <i class="bi bi-pencil-square"></i>
                                                Manage
                                            </button>

                                        </td>

                                    </tr>

                                <?php endwhile; ?>

                            <?php else: ?>

                                <tr>

                                    <td
                                        colspan="7"
                                        class="empty-state"
                                    >
                                        <i
                                            class="bi bi-inbox"
                                            aria-hidden="true"
                                        ></i>

                                        <strong>
                                            No reports found
                                        </strong>

                                        <span>
                                            No hazard reports match the
                                            selected filters.
                                        </span>
                                    </td>

                                </tr>

                            <?php endif; ?>

                        </tbody>

                    </table>

                </div>

            </section>

        </main>

        <div
        class="modal fade"
        id="editReportModal"
        tabindex="-1"
        aria-labelledby="editReportModalLabel"
        aria-hidden="true"
    >
        <div
            class="
                modal-dialog
                modal-xl
                modal-dialog-centered
                modal-dialog-scrollable
            "
        >
            <div class="modal-content hazentra-modal">

                <div class="modal-header">

                    <div>
                        <span class="modal-eyebrow">
                            Hazard management
                        </span>

                        <h2
                            class="modal-title"
                            id="editReportModalLabel"
                        >
                            Review and edit report
                        </h2>

                        <p>
                            View the complete submission and update the
                            administrative information.
                        </p>
                    </div>

                </div>

                <form
                    method="POST"
                    action="update_report.php"
                    id="editReportForm"
                    class="modal-edit-form"
                >

                    <div class="modal-body">

                        <input
                            type="hidden"
                            name="report_id"
                            id="modalReportId"
                        >

                        <section class="modal-form-section">

                            <div class="modal-section-heading">

                                <div class="modal-section-icon">
                                    <i class="bi bi-person"></i>
                                </div>

                                <div>
                                    <h3>Reporter information</h3>

                                    <p>
                                        User and device details received from
                                        the Android application.
                                    </p>
                                </div>

                            </div>

                            <div class="modal-form-grid">

                                <div class="modal-field">

                                    <label for="modalUserName">
                                        Reporter name
                                    </label>

                                    <input
                                        type="text"
                                        id="modalUserName"
                                        name="user_name"
                                        class="form-control"
                                        required
                                    >

                                </div>

                                <div class="modal-field">

                                    <label for="modalReportDatetime">
                                        Report date and time
                                    </label>

                                    <input
                                        type="datetime-local"
                                        id="modalReportDatetime"
                                        name="report_datetime"
                                        class="form-control"
                                        required
                                    >

                                </div>

                                <div class="modal-field modal-field-full">

                                    <label for="modalUserAgent">
                                        Device information
                                    </label>

                                    <input
                                        type="text"
                                        id="modalUserAgent"
                                        name="user_agent"
                                        class="form-control"
                                        required
                                    >

                                </div>

                            </div>

                        </section>

                        <section class="modal-form-section">

                            <div class="modal-section-heading">

                                <div class="modal-section-icon">
                                    <i class="bi bi-geo-alt"></i>
                                </div>

                                <div>
                                    <h3>Hazard and location</h3>

                                    <p>
                                        Location, category, coordinates and
                                        hazard description.
                                    </p>
                                </div>

                            </div>

                            <div class="modal-form-grid">

                                <div class="modal-field modal-field-full">

                                    <label for="modalLocationName">
                                        Location
                                    </label>

                                    <input
                                        type="text"
                                        id="modalLocationName"
                                        name="location_name"
                                        class="form-control"
                                        required
                                    >

                                </div>

                                <div class="modal-field">

                                    <label for="modalLatitude">
                                        Latitude
                                    </label>

                                    <input
                                        type="number"
                                        step="0.00000001"
                                        id="modalLatitude"
                                        name="latitude"
                                        class="form-control"
                                        required
                                    >

                                </div>

                                <div class="modal-field">

                                    <label for="modalLongitude">
                                        Longitude
                                    </label>

                                    <input
                                        type="number"
                                        step="0.00000001"
                                        id="modalLongitude"
                                        name="longitude"
                                        class="form-control"
                                        required
                                    >

                                </div>

                                <div class="modal-field">

                                    <label for="modalCategoryId">
                                        Hazard category
                                    </label>

                                    <select
                                        id="modalCategoryId"
                                        name="category_id"
                                        class="form-select"
                                        required
                                    >

                                        <?php foreach ($categories as $category): ?>

                                            <option
                                                value="<?= (int) $category[
                                                    "category_id"
                                                ]; ?>"
                                            >
                                                <?= htmlspecialchars(
                                                    $category["category_name"]
                                                ); ?>
                                            </option>

                                        <?php endforeach; ?>

                                    </select>

                                </div>

                                <div class="modal-field">

                                    <label for="modalReportStatus">
                                        Report status
                                    </label>

                                    <select
                                        id="modalReportStatus"
                                        name="report_status"
                                        class="form-select"
                                        required
                                    >

                                        <?php foreach (
                                            $allowedStatuses as $allowedStatus
                                        ): ?>

                                            <option
                                                value="<?= htmlspecialchars(
                                                    $allowedStatus
                                                ); ?>"
                                            >
                                                <?= htmlspecialchars(
                                                    $allowedStatus
                                                ); ?>
                                            </option>

                                        <?php endforeach; ?>

                                    </select>

                                </div>

                                <div class="modal-field modal-field-full">

                                    <label for="modalDescription">
                                        Hazard description
                                    </label>

                                    <textarea
                                        id="modalDescription"
                                        name="description"
                                        class="form-control modal-description"
                                        required
                                    ></textarea>

                                </div>

                            </div>

                            <a
                                href="#"
                                target="_blank"
                                rel="noopener noreferrer"
                                class="modal-map-link"
                                id="modalMapLink"
                            >
                                <i class="bi bi-map"></i>
                                Open coordinates in Google Maps
                            </a>

                        </section>

                    </div>

                    <div class="modal-footer">

                        <button
                            type="button"
                            class="modal-cancel-button"
                            data-bs-dismiss="modal"
                        >
                            Cancel
                        </button>

                        <button
                            type="submit"
                            class="modal-save-button"
                        >
                            <i class="bi bi-check2-circle"></i>
                            Save changes
                        </button>

                    </div>

                </form>

            </div>
        </div>
    </div>

        <footer class="dashboard-footer">
            <p>
                © <?= date("Y"); ?> Hazentra Development Team.
                All rights reserved.
            </p>
        </footer>

    </div>

</div>

<script
    src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"
></script>

<script>
    const body = document.body;
    const sidebar = document.getElementById("sidebar");
    const sidebarToggle = document.getElementById("sidebarToggle");
    const sidebarToggleIcon = document.getElementById(
        "sidebarToggleIcon"
    );
    const sidebarClose = document.getElementById("sidebarClose");
    const sidebarOverlay = document.getElementById(
        "sidebarOverlay"
    );

    const mobileBreakpoint = 950;

    function isMobile() {
        return window.innerWidth <= mobileBreakpoint;
    }

    function openMobileSidebar() {
        sidebar.classList.add("sidebar-open");
        sidebarOverlay.classList.add(
            "sidebar-overlay-visible"
        );
        body.classList.add("no-scroll");

        sidebarToggle.setAttribute(
            "aria-expanded",
            "true"
        );
    }

    function closeMobileSidebar() {
        sidebar.classList.remove("sidebar-open");
        sidebarOverlay.classList.remove(
            "sidebar-overlay-visible"
        );
        body.classList.remove("no-scroll");

        sidebarToggle.setAttribute(
            "aria-expanded",
            "false"
        );
    }

    function toggleDesktopSidebar() {
        body.classList.toggle("sidebar-collapsed");

        const isCollapsed = body.classList.contains(
            "sidebar-collapsed"
        );

        sidebarToggle.setAttribute(
            "aria-expanded",
            isCollapsed ? "false" : "true"
        );

        sidebarToggleIcon.className = isCollapsed
            ? "bi bi-layout-sidebar-inset"
            : "bi bi-layout-sidebar";
    }

    sidebarToggle.addEventListener(
        "click",
        function () {
            if (isMobile()) {
                if (
                    sidebar.classList.contains(
                        "sidebar-open"
                    )
                ) {
                    closeMobileSidebar();
                } else {
                    openMobileSidebar();
                }

                return;
            }

            toggleDesktopSidebar();
        }
    );

    sidebarClose.addEventListener(
        "click",
        closeMobileSidebar
    );

    sidebarOverlay.addEventListener(
        "click",
        closeMobileSidebar
    );

    document
        .querySelectorAll(".navigation-item")
        .forEach(function (item) {
            item.addEventListener(
                "click",
                function () {
                    if (isMobile()) {
                        closeMobileSidebar();
                    }
                }
            );
        });

    window.addEventListener(
        "resize",
        function () {
            if (!isMobile()) {
                closeMobileSidebar();
            }
        }
    );

    const editReportModal = document.getElementById(
        "editReportModal"
    );

    if (editReportModal) {
        editReportModal.addEventListener(
            "show.bs.modal",
            function (event) {
                const button = event.relatedTarget;

                if (!button) {
                    return;
                }

                const reportId = button.dataset.reportId || "";
                const userName = button.dataset.userName || "";
                const reportDatetime =
                    button.dataset.reportDatetime || "";
                const userAgent = button.dataset.userAgent || "";
                const locationName =
                    button.dataset.locationName || "";
                const latitude = button.dataset.latitude || "";
                const longitude = button.dataset.longitude || "";
                const categoryId =
                    button.dataset.categoryId || "";
                const description =
                    button.dataset.description || "";
                const reportStatus =
                    button.dataset.reportStatus || "";

                document.getElementById(
                    "modalReportId"
                ).value = reportId;

                document.getElementById(
                    "modalUserName"
                ).value = userName;

                const formattedReportDatetime = reportDatetime
                    ? reportDatetime.replace(" ", "T").slice(0, 16)
                    : "";

                document.getElementById(
                    "modalReportDatetime"
                ).value = formattedReportDatetime;

                document.getElementById(
                    "modalUserAgent"
                ).value = userAgent;

                document.getElementById(
                    "modalLocationName"
                ).value = locationName;

                document.getElementById(
                    "modalLatitude"
                ).value = latitude;

                document.getElementById(
                    "modalLongitude"
                ).value = longitude;

                document.getElementById(
                    "modalCategoryId"
                ).value = categoryId;

                document.getElementById(
                    "modalDescription"
                ).value = description;

                document.getElementById(
                    "modalReportStatus"
                ).value = reportStatus;

                const mapLink = document.getElementById(
                    "modalMapLink"
                );

                mapLink.href =
                    "https://www.google.com/maps?q="
                    + encodeURIComponent(
                        latitude + "," + longitude
                    );
            }
        );
    }
</script>

<script src="https://cdn.jsdelivr.net/npm/chart.js"></script>

<script>

const categoryLabels = <?= json_encode($categoryLabels); ?>;
const categoryValues = <?= json_encode($categoryTotals); ?>;

const trendLabels = <?= json_encode($trendLabels); ?>;
const trendValues = <?= json_encode($trendData); ?>;

new Chart(
    document.getElementById("categoryChart"),
    {
        type: "bar",

        data: {
            labels: categoryLabels,

            datasets: [{
                label: "Total Reports",
                data: categoryValues,

                backgroundColor: [
                    "#0B4F8A",
                    "#F97316",
                    "#22C55E"
                ],

                borderRadius: 10,
                borderSkipped: false
            }]
        },

        options: {

            responsive: true,

            maintainAspectRatio: false,

            plugins: {

                legend: {
                    display: false
                }

            },

            scales: {

                y: {

                    beginAtZero: true,

                    ticks: {
                        precision: 0
                    }

                }

            }

        }

    }
);

new Chart(
    document.getElementById("trendChart"),
    {
        type: "line",

        data: {
            labels: trendLabels,

            datasets: [{
                label: "Reports",
                data: trendValues,

                borderColor: "#0B4F8A",

                backgroundColor: "rgba(11,79,138,0.12)",

                fill: true,

                tension: 0.4,

                pointRadius: 4,

                pointHoverRadius: 6,

                pointBackgroundColor: "#0B4F8A",

                borderWidth: 3
            }]
        },

        options: {

            responsive: true,

            maintainAspectRatio: false,

            plugins: {

                legend: {
                    display: false
                }

            },

            scales: {

                x: {

                    grid: {
                        display: false
                    }

                },

                y: {

                    beginAtZero: true,

                    ticks: {
                        precision: 0
                    }

                }

            }

        }

    }
);
</script>

<!-- ADD REPORT MODAL -->

<div
    class="modal fade"
    id="addReportModal"
    tabindex="-1"
    aria-labelledby="addReportModalLabel"
    aria-hidden="true"
>

    <div
        class="
            modal-dialog
            modal-xl
            modal-dialog-centered
            modal-dialog-scrollable
        "
    >

        <div class="modal-content hazentra-modal">

            <div class="modal-header">

                <div>

                    <span class="modal-eyebrow">
                        Hazard Management
                    </span>

                    <h2
                        class="modal-title"
                        id="addReportModalLabel"
                    >
                        Create New Hazard Report
                    </h2>

                    <p>
                        Enter the report information below before creating a
                        new hazard report.
                    </p>

                </div>

            </div>

            <form
                action="add_report.php"
                method="POST"
                class="modal-edit-form"
            >

                <div class="modal-body">

                    <!-- REPORTER INFORMATION -->

                    <section class="modal-form-section">

                        <div class="modal-section-heading">

                            <div class="modal-section-icon">

                                <i class="bi bi-person"></i>

                            </div>

                            <div>

                                <h3>Reporter Information</h3>

                                <p>
                                    Enter the reporter details for this hazard
                                    report.
                                </p>

                            </div>

                        </div>

                        <div class="modal-form-grid">

                            <div class="modal-field">

                                <label>
                                    Reporter Name
                                </label>

                                <input
                                    type="text"
                                    name="user_name"
                                    class="form-control"
                                    required
                                >

                            </div>

                            <div class="modal-field">

                                <label>
                                    Device Information
                                </label>

                                <input
                                    type="text"
                                    name="user_agent"
                                    class="form-control"
                                    value="Admin Dashboard"
                                    readonly
                                >

                            </div>

                        </div>

                    </section>

                    <!-- HAZARD INFORMATION -->

                    <section class="modal-form-section">

                        <div class="modal-section-heading">

                            <div class="modal-section-icon">

                                <i class="bi bi-geo-alt"></i>

                            </div>

                            <div>

                                <h3>Hazard and Location</h3>

                                <p>
                                    Enter the hazard category, location,
                                    coordinates and description.
                                </p>

                            </div>

                        </div>

                        <div class="modal-form-grid">

                            <div class="modal-field modal-field-full">

                                <label>
                                    Location
                                </label>

                                <input
                                    type="text"
                                    name="location_name"
                                    class="form-control"
                                    required
                                >

                            </div>

                            <div class="modal-field">

                                <label>
                                    Latitude
                                </label>

                                <input
                                    type="number"
                                    step="0.00000001"
                                    name="latitude"
                                    class="form-control"
                                    required
                                >

                            </div>

                            <div class="modal-field">

                                <label>
                                    Longitude
                                </label>

                                <input
                                    type="number"
                                    step="0.00000001"
                                    name="longitude"
                                    class="form-control"
                                    required
                                >

                            </div>

                            <div class="modal-field">

                                <label>
                                    Hazard Category
                                </label>

                                <select
                                    name="category_id"
                                    class="form-select"
                                    required
                                >

                                    <?php

                                    $categoryResult->data_seek(0);

                                    while (
                                        $cat = $categoryResult->fetch_assoc()
                                    ):

                                    ?>

                                        <option
                                            value="<?= $cat["category_id"]; ?>"
                                        >
                                            <?= htmlspecialchars(
                                                $cat["category_name"]
                                            ); ?>
                                        </option>

                                    <?php endwhile; ?>

                                </select>

                            </div>

                            <div class="modal-field">

                                <label>
                                    Report Status
                                </label>

                                <select
                                    name="report_status"
                                    class="form-select"
                                >

                                    <option value="Active">
                                        Active
                                    </option>

                                    <option value="Resolved">
                                        Resolved
                                    </option>

                                    <option value="Rejected">
                                        Rejected
                                    </option>

                                </select>

                            </div>

                            <div class="modal-field modal-field-full">

                                <label>
                                    Hazard Description
                                </label>

                                <textarea
                                    name="description"
                                    class="form-control modal-description"
                                    required
                                ></textarea>

                            </div>

                        </div>

                    </section>

                </div>

                <div class="modal-footer">

                    <button
                        type="button"
                        class="modal-cancel-button"
                        data-bs-dismiss="modal"
                    >
                        Cancel
                    </button>

                    <button
                        type="submit"
                        class="modal-save-button"
                    >

                        <i class="bi bi-plus-circle"></i>

                        Create Report

                    </button>

                </div>

            </form>

        </div>

    </div>

</div>

<script>

document.addEventListener("DOMContentLoaded", function () {

    const successAlert = document.getElementById("successAlert");
    const errorAlert = document.getElementById("errorAlert");

    if (successAlert || errorAlert) {

        setTimeout(function () {

            if (successAlert) {

                successAlert.style.opacity = "0";

                setTimeout(() => successAlert.remove(), 500);

            }

            if (errorAlert) {

                errorAlert.style.opacity = "0";

                setTimeout(() => errorAlert.remove(), 500);

            }

            // Remove success/error from URL
            const url = new URL(window.location);

            url.searchParams.delete("success");
            url.searchParams.delete("error");

            window.history.replaceState(
                {},
                document.title,
                url.pathname + url.search
            );

        }, 3000);

    }

});

</script>
</script>
</body>
</html>

<?php

$stmt->close();
$conn->close();

?>