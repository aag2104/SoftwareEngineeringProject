-- MySQL dump 10.13  Distrib 8.0.40, for macos14 (arm64)
--
-- Host: cshub.cvqmoymkg5l2.us-east-2.rds.amazonaws.com    Database: old_cshub
-- ------------------------------------------------------
-- Server version	8.0.40

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;
SET @MYSQLDUMP_TEMP_LOG_BIN = @@SESSION.SQL_LOG_BIN;
SET @@SESSION.SQL_LOG_BIN= 0;

--
-- GTID state at the beginning of the backup 
--

SET @@GLOBAL.GTID_PURGED=/*!80000 '+'*/ '';

--
-- Table structure for table `tajob_application`
--

DROP TABLE IF EXISTS `tajob_application`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tajob_application` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tajob_id` bigint DEFAULT NULL,
  `applicant_id` bigint DEFAULT NULL,
  `apply_date` varchar(255) DEFAULT NULL,
  `smu_id` int NOT NULL,
  `ta_semester_types` int NOT NULL,
  `ta_student_types` int NOT NULL,
  `ta_student_admission_types` int NOT NULL,
  `ta_uscitizen` int NOT NULL,
  `ta_native_language` int NOT NULL,
  `ta_english_proficiency_test` int NOT NULL,
  `ta_english_proficiency_test_name` varchar(255) DEFAULT NULL,
  `ta_english_proficiency_test_score` double NOT NULL,
  `ta_english_proficiency_test_date` varchar(255) DEFAULT NULL,
  `gre_v` double NOT NULL,
  `gre_q` double NOT NULL,
  `gre_a` double NOT NULL,
  `gre_date` varchar(255) DEFAULT NULL,
  `undergraduate_gpa` double NOT NULL,
  `undergraduate_school` varchar(255) DEFAULT NULL,
  `graduate_gpa` double NOT NULL,
  `graduate_school` varchar(255) DEFAULT NULL,
  `class_rank_no_gpa` int NOT NULL,
  `score_percentage_no_gpa` double NOT NULL,
  `grade_no_gpa` double NOT NULL,
  `other_info_no_gpa` varchar(255) DEFAULT NULL,
  `enrolled_degree` int NOT NULL,
  `enrolled_phd_degree` int NOT NULL,
  `areas_research_interest1` varchar(255) DEFAULT NULL,
  `areas_research_interest2` varchar(255) DEFAULT NULL,
  `areas_research_interest3` varchar(255) DEFAULT NULL,
  `areas_research_interest4` varchar(255) DEFAULT NULL,
  `ra_smu` int NOT NULL,
  `ra_smutime` varchar(255) DEFAULT NULL,
  `ra_smuadvisor_name` varchar(255) DEFAULT NULL,
  `ra_smuadvisor_email` varchar(255) DEFAULT NULL,
  `ta_smu` int NOT NULL,
  `ta_smutime` varchar(255) DEFAULT NULL,
  `ta_smuadvisor_name` varchar(255) DEFAULT NULL,
  `ta_smuadvisor_email` varchar(255) DEFAULT NULL,
  `programming_language_cpp` int NOT NULL,
  `programming_language_java` int NOT NULL,
  `programming_language_python` int NOT NULL,
  `programming_language_r` int NOT NULL,
  `programming_language_sql` int NOT NULL,
  `programming_language_javascript` int NOT NULL,
  `programming_language_verilog` int NOT NULL,
  `programming_language_assembler` int NOT NULL,
  `programming_language_assembler_type` varchar(255) DEFAULT NULL,
  `computer_systems_type` varchar(255) DEFAULT NULL,
  `ta_courses_preference` varchar(255) DEFAULT NULL,
  `ta_courses_preference_hidden` varchar(255) DEFAULT NULL,
  `ta_courses_not_preference` varchar(255) DEFAULT NULL,
  `ta_courses_not_preference_hidden` varchar(255) DEFAULT NULL,
  `previous_teaching_exp1title` varchar(255) DEFAULT NULL,
  `previous_teaching_exp1where` varchar(255) DEFAULT NULL,
  `previous_teaching_exp1date` varchar(255) DEFAULT NULL,
  `previous_teaching_exp2title` varchar(255) DEFAULT NULL,
  `previous_teaching_exp2where` varchar(255) DEFAULT NULL,
  `previous_teaching_exp2date` varchar(255) DEFAULT NULL,
  `previous_teaching_exp3title` varchar(255) DEFAULT NULL,
  `previous_teaching_exp3where` varchar(255) DEFAULT NULL,
  `previous_teaching_exp3date` varchar(255) DEFAULT NULL,
  `apply_headline` varchar(255) DEFAULT NULL,
  `apply_cover_letter` varchar(255) DEFAULT NULL,
  `tating` double NOT NULL,
  `tating_count` bigint NOT NULL,
  `recommend_rating` double NOT NULL,
  `recommend_rating_count` bigint NOT NULL,
  `homepage` varchar(255) DEFAULT NULL,
  `avatar` varchar(255) DEFAULT NULL,
  `created_time` varchar(255) DEFAULT NULL,
  `is_active` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
SET @@SESSION.SQL_LOG_BIN = @MYSQLDUMP_TEMP_LOG_BIN;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2025-09-05  2:59:38
