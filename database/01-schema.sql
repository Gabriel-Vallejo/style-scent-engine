-- Style & Scent Engine: esquema de la base de datos (MySQL 8.0, 3FN)
-- docker-compose monta esta carpeta en /docker-entrypoint-initdb.d, así que
-- MySQL ejecuta los scripts en orden alfabético solo cuando el volumen está vacío.
/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `categorias` (
  `id_categoria` int NOT NULL AUTO_INCREMENT,
  `nombre` varchar(30) NOT NULL,
  PRIMARY KEY (`id_categoria`),
  UNIQUE KEY `nombre` (`nombre`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `colores` (
  `id_color` int NOT NULL AUTO_INCREMENT,
  `nombre` varchar(30) NOT NULL,
  PRIMARY KEY (`id_color`),
  UNIQUE KEY `nombre` (`nombre`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `estados_posesion` (
  `id_estado` int NOT NULL AUTO_INCREMENT,
  `nombre` varchar(30) NOT NULL,
  PRIMARY KEY (`id_estado`),
  UNIQUE KEY `nombre` (`nombre`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `estilos` (
  `id_estilo` int NOT NULL AUTO_INCREMENT,
  `nombre` varchar(30) NOT NULL,
  PRIMARY KEY (`id_estilo`),
  UNIQUE KEY `nombre` (`nombre`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `familias_olfativas` (
  `id_familia` int NOT NULL AUTO_INCREMENT,
  `nombre` varchar(50) NOT NULL,
  PRIMARY KEY (`id_familia`),
  UNIQUE KEY `nombre` (`nombre`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `filtros_exclusion` (
  `id_filtro` int NOT NULL AUTO_INCREMENT,
  `id_nota` int NOT NULL,
  `penalizacion_score` int NOT NULL,
  PRIMARY KEY (`id_filtro`),
  KEY `idx_filtro_nota` (`id_nota`),
  CONSTRAINT `fk_filtro_nota` FOREIGN KEY (`id_nota`) REFERENCES `notas` (`id_nota`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `notas` (
  `id_nota` int NOT NULL AUTO_INCREMENT,
  `nombre` varchar(50) NOT NULL,
  PRIMARY KEY (`id_nota`),
  UNIQUE KEY `nombre` (`nombre`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `perfume_nota` (
  `id_perfume` int NOT NULL,
  `id_nota` int NOT NULL,
  PRIMARY KEY (`id_perfume`,`id_nota`),
  KEY `idx_perfume_nota_nota` (`id_nota`),
  CONSTRAINT `perfume_nota_ibfk_1` FOREIGN KEY (`id_perfume`) REFERENCES `perfumes` (`id_perfume`) ON DELETE CASCADE,
  CONSTRAINT `perfume_nota_ibfk_2` FOREIGN KEY (`id_nota`) REFERENCES `notas` (`id_nota`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `perfumes` (
  `id_perfume` int NOT NULL AUTO_INCREMENT,
  `nombre` varchar(100) NOT NULL,
  `marca` varchar(50) DEFAULT NULL,
  `id_familia` int NOT NULL,
  `id_estado` int NOT NULL,
  PRIMARY KEY (`id_perfume`),
  KEY `idx_perfumes_familia` (`id_familia`),
  KEY `idx_perfumes_estado` (`id_estado`),
  CONSTRAINT `perfumes_ibfk_1` FOREIGN KEY (`id_familia`) REFERENCES `familias_olfativas` (`id_familia`),
  CONSTRAINT `perfumes_ibfk_2` FOREIGN KEY (`id_estado`) REFERENCES `estados_posesion` (`id_estado`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `prenda_estilo` (
  `id_prenda` int NOT NULL,
  `id_estilo` int NOT NULL,
  PRIMARY KEY (`id_prenda`,`id_estilo`),
  KEY `idx_prenda_estilo_estilo` (`id_estilo`),
  CONSTRAINT `prenda_estilo_ibfk_1` FOREIGN KEY (`id_prenda`) REFERENCES `prendas` (`id_prenda`) ON DELETE CASCADE,
  CONSTRAINT `prenda_estilo_ibfk_2` FOREIGN KEY (`id_estilo`) REFERENCES `estilos` (`id_estilo`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `prendas` (
  `id_prenda` int NOT NULL AUTO_INCREMENT,
  `nombre` varchar(100) NOT NULL,
  `id_categoria` int NOT NULL,
  `id_color` int NOT NULL,
  `fecha_registro` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id_prenda`),
  KEY `idx_prendas_color` (`id_color`),
  KEY `idx_prendas_categoria` (`id_categoria`),
  CONSTRAINT `fk_prenda_categoria` FOREIGN KEY (`id_categoria`) REFERENCES `categorias` (`id_categoria`),
  CONSTRAINT `fk_prenda_color` FOREIGN KEY (`id_color`) REFERENCES `colores` (`id_color`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sinergias_color` (
  `id_regla` int NOT NULL AUTO_INCREMENT,
  `id_color` int NOT NULL,
  `id_familia` int NOT NULL,
  `puntos_sumados` int NOT NULL,
  PRIMARY KEY (`id_regla`),
  UNIQUE KEY `uq_sinergia_color` (`id_color`,`id_familia`),
  KEY `idx_sinergia_color` (`id_color`),
  KEY `idx_sinergia_familia` (`id_familia`),
  CONSTRAINT `fk_sinergia_color` FOREIGN KEY (`id_color`) REFERENCES `colores` (`id_color`),
  CONSTRAINT `fk_sinergia_familia` FOREIGN KEY (`id_familia`) REFERENCES `familias_olfativas` (`id_familia`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sinergias_estilo` (
  `id_regla` int NOT NULL AUTO_INCREMENT,
  `id_estilo` int NOT NULL,
  `id_familia` int NOT NULL,
  `puntos_sumados` int NOT NULL,
  PRIMARY KEY (`id_regla`),
  UNIQUE KEY `uq_sinergia_estilo` (`id_estilo`,`id_familia`),
  KEY `id_familia` (`id_familia`),
  CONSTRAINT `sinergias_estilo_ibfk_1` FOREIGN KEY (`id_estilo`) REFERENCES `estilos` (`id_estilo`),
  CONSTRAINT `sinergias_estilo_ibfk_2` FOREIGN KEY (`id_familia`) REFERENCES `familias_olfativas` (`id_familia`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

