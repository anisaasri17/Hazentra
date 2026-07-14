-- phpMyAdmin SQL Dump
-- version 5.2.1
-- https://www.phpmyadmin.net/
--
-- Host: 127.0.0.1
-- Generation Time: Jul 14, 2026 at 11:31 AM
-- Server version: 10.4.32-MariaDB
-- PHP Version: 8.2.12

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

--
-- Database: `hazentra_db`
--

-- --------------------------------------------------------

--
-- Table structure for table `hazard_categories`
--

CREATE TABLE `hazard_categories` (
  `category_id` int(10) UNSIGNED NOT NULL,
  `category_name` varchar(100) NOT NULL,
  `category_description` varchar(255) NOT NULL,
  `marker_color` varchar(30) NOT NULL,
  `marker_icon` varchar(100) NOT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `hazard_categories`
--

INSERT INTO `hazard_categories` (`category_id`, `category_name`, `category_description`, `marker_color`, `marker_icon`, `created_at`) VALUES
(1, 'Road Hazard', 'Road-related risks such as potholes, damaged roads, and blocked roads.', 'Red', 'road_hazard_marker.png', '2026-07-11 15:07:56'),
(2, 'Environmental Hazard', 'Environmental risks such as floods, fallen trees, and muddy or slippery areas.', 'Green', 'environmental_hazard_marker.png', '2026-07-11 15:07:56'),
(3, 'Building Hazard', 'Building-related risks such as damaged structures, broken stairs, and falling materials.', 'Orange', 'building_hazard_marker.png', '2026-07-11 15:07:56');

-- --------------------------------------------------------

--
-- Table structure for table `hazard_reports`
--

CREATE TABLE `hazard_reports` (
  `report_id` int(10) UNSIGNED NOT NULL,
  `user_name` varchar(100) NOT NULL,
  `report_datetime` datetime NOT NULL DEFAULT current_timestamp(),
  `user_agent` varchar(255) NOT NULL,
  `location_name` varchar(255) NOT NULL,
  `latitude` decimal(10,8) NOT NULL,
  `longitude` decimal(11,8) NOT NULL,
  `category_id` int(10) UNSIGNED NOT NULL,
  `description` text NOT NULL,
  `report_status` enum('Active','Resolved','Rejected') NOT NULL DEFAULT 'Active',
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `updated_at` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `hazard_reports`
--

INSERT INTO `hazard_reports` (`report_id`, `user_name`, `report_datetime`, `user_agent`, `location_name`, `latitude`, `longitude`, `category_id`, `description`, `report_status`, `created_at`, `updated_at`) VALUES
(1, 'Nur Aina', '2026-07-11 23:08:39', 'Samsung Galaxy A55 | Android 15', 'UiTM Perlis Arau Campus Area', 6.44680000, 100.27700000, 1, 'A large pothole is present near the roadside and may be dangerous to motorcycles.', 'Resolved', '2026-07-11 15:08:39', '2026-07-11 15:24:36'),
(2, 'Amir Hakimi', '2026-07-11 23:08:39', 'OPPO Reno 12 | Android 14', 'Arau Railway Station Area', 6.42970000, 100.26950000, 1, 'The road surface near the entrance is cracked and uneven.', 'Active', '2026-07-11 15:08:39', '2026-07-11 15:08:39'),
(3, 'Siti Hajar', '2026-07-11 23:08:39', 'Xiaomi 14T | Android 15', 'Arau Town Area', 6.43150000, 100.27300000, 2, 'Water has collected along the pedestrian pathway after heavy rain.', 'Active', '2026-07-11 15:08:39', '2026-07-11 15:08:39'),
(4, 'Muhammad Faris', '2026-07-11 23:08:39', 'Google Pixel 8 | Android 15', 'Pauh Putra Area, Perlis', 6.46150000, 100.35000000, 2, 'A fallen tree branch is partially blocking the pedestrian walkway.', 'Active', '2026-07-11 15:08:39', '2026-07-11 15:08:39'),
(5, 'Nur Syafiqah', '2026-07-11 23:08:39', 'Vivo V40 | Android 14', 'Arau Public Facility Area', 6.43400000, 100.27600000, 3, 'Several loose ceiling panels were observed near the public waiting area.', 'Active', '2026-07-11 15:08:39', '2026-07-11 15:08:39'),
(6, 'Aiman Zikri', '2026-07-11 23:09:09', 'Samsung Galaxy S24 | Android 15', 'Sungai Petani Town Area', 5.64700000, 100.48700000, 1, 'A deep pothole is located on the left side of the road.', 'Active', '2026-07-11 15:09:09', '2026-07-11 15:09:09'),
(7, 'Nur Izzah', '2026-07-11 23:09:09', 'OPPO Find X8 | Android 15', 'Bandar Amanjaya Area, Sungai Petani', 5.69000000, 100.50700000, 1, 'A damaged road section is causing vehicles to move into the opposite lane.', 'Active', '2026-07-11 15:09:09', '2026-07-11 15:09:09'),
(8, 'Daniel Tan', '2026-07-11 23:09:09', 'Google Pixel 9 | Android 15', 'Taman Ria Area, Sungai Petani', 5.66500000, 100.49300000, 2, 'The pathway is muddy and slippery following continuous rainfall.', 'Active', '2026-07-11 15:09:09', '2026-07-11 15:09:09'),
(9, 'Siti Aisyah', '2026-07-11 23:09:09', 'Xiaomi Redmi Note 14 | Android 15', 'Sungai Petani Residential Area', 5.65300000, 100.50000000, 2, 'A section of the road is flooded and may be unsafe for smaller vehicles.', 'Active', '2026-07-11 15:09:09', '2026-07-11 15:09:09'),
(10, 'Hakim Roslan', '2026-07-11 23:09:00', 'Vivo V40 Pro | Android 14', 'Sungai Petani Commercial Area', 5.64300000, 100.49000000, 3, 'Broken floor tiles near the building entrance may cause visitors to trip.', 'Resolved', '2026-07-11 15:09:09', '2026-07-12 17:10:33'),
(11, 'Test User', '2026-07-13 01:43:00', 'Android Emulator | Android 15', 'UiTM Perlis Test Area', 6.44680000, 100.27700000, 1, 'Test pothole report submitted through Postman.', 'Resolved', '2026-07-12 17:43:57', '2026-07-12 18:18:38'),
(12, 'Wanis Md Nor', '2026-07-13 10:06:20', 'Realme RMX3521 | Android 14', 'UiTM ARAU', 6.44939120, 100.28387760, 2, 'dust', 'Active', '2026-07-13 02:06:20', '2026-07-13 02:06:20'),
(13, 'Hamid bin Safawi', '2026-07-13 10:37:00', 'Realme RMX3521 | Android 14', 'Cmart', 6.44939570, 100.28389140, 1, 'Potholes', 'Resolved', '2026-07-13 02:37:07', '2026-07-13 02:40:38'),
(14, 'Cik Siti', '2026-07-13 10:42:16', 'Realme RMX3521 | Android 14', 'Emart ARAU', 6.44935130, 100.28386310, 2, 'flooded', 'Active', '2026-07-13 02:42:16', '2026-07-13 02:42:16'),
(15, 'Maimunah Jaafar', '2026-07-13 11:24:00', 'Realme RMX3521 | Android 14', 'Kesinai', 6.44939440, 100.28387350, 1, 'Damage Road', 'Resolved', '2026-07-13 03:24:49', '2026-07-13 04:02:48'),
(16, 'Manap', '2026-07-13 12:30:29', 'Realme RMX3521 | Android 14', 'Arau', 6.54940120, 100.38387720, 3, 'Wall damage', 'Active', '2026-07-13 04:30:29', '2026-07-13 04:30:29'),
(17, 'Aminah Halim', '2026-07-13 23:34:46', 'Realme RMX3521 | Android 14', 'Dahlia 1', 6.44930060, 100.28356150, 3, 'Crack', 'Active', '2026-07-13 15:34:46', '2026-07-13 15:34:46'),
(18, 'Alia', '2026-07-14 13:57:41', 'Realme RMX3521 | Android 14', 'Sungai Petani', 5.71539340, 100.52976330, 2, 'Fluid', 'Active', '2026-07-14 05:57:41', '2026-07-14 05:57:41'),
(19, 'Amani', '2026-07-14 14:04:36', 'Realme RMX3521 | Android 14', 'Amanjay', 5.71533000, 100.52978840, 3, 'Crack', 'Active', '2026-07-14 06:04:36', '2026-07-14 06:04:36'),
(20, 'Hafzan', '2026-07-14 14:22:43', 'Realme RMX3521 | Android 14', 'Bandar Baru', 5.71546830, 100.52966140, 2, 'Flood', 'Active', '2026-07-14 06:22:43', '2026-07-14 06:22:43');

--
-- Indexes for dumped tables
--

--
-- Indexes for table `hazard_categories`
--
ALTER TABLE `hazard_categories`
  ADD PRIMARY KEY (`category_id`),
  ADD UNIQUE KEY `category_name` (`category_name`);

--
-- Indexes for table `hazard_reports`
--
ALTER TABLE `hazard_reports`
  ADD PRIMARY KEY (`report_id`),
  ADD KEY `idx_category_id` (`category_id`),
  ADD KEY `idx_report_datetime` (`report_datetime`),
  ADD KEY `idx_report_status` (`report_status`),
  ADD KEY `idx_coordinates` (`latitude`,`longitude`);

--
-- AUTO_INCREMENT for dumped tables
--

--
-- AUTO_INCREMENT for table `hazard_categories`
--
ALTER TABLE `hazard_categories`
  MODIFY `category_id` int(10) UNSIGNED NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=4;

--
-- AUTO_INCREMENT for table `hazard_reports`
--
ALTER TABLE `hazard_reports`
  MODIFY `report_id` int(10) UNSIGNED NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=21;

--
-- Constraints for dumped tables
--

--
-- Constraints for table `hazard_reports`
--
ALTER TABLE `hazard_reports`
  ADD CONSTRAINT `fk_hazard_category` FOREIGN KEY (`category_id`) REFERENCES `hazard_categories` (`category_id`) ON UPDATE CASCADE;
COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
