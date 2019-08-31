/*
SQLyog Ultimate v12.3.1 (64 bit)
MySQL - 5.5.49 : Database - qingcheng_business
*********************************************************************
*/

/*!40101 SET NAMES utf8 */;

/*!40101 SET SQL_MODE=''*/;

/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;
CREATE DATABASE /*!32312 IF NOT EXISTS*/`qingcheng_business` /*!40100 DEFAULT CHARACTER SET utf8 */;

USE `qingcheng_business`;

/*Table structure for table `tb_activity` */

DROP TABLE IF EXISTS `tb_activity`;

CREATE TABLE `tb_activity` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `title` varchar(200) DEFAULT NULL COMMENT '活动标题',
  `start_time` datetime DEFAULT NULL COMMENT '开始时间',
  `end_time` datetime DEFAULT NULL COMMENT '结束时间',
  `status` char(1) DEFAULT NULL COMMENT '状态',
  `content` text COMMENT '活动内容',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

/*Data for the table `tb_activity` */

/*Table structure for table `tb_ad` */

DROP TABLE IF EXISTS `tb_ad`;

CREATE TABLE `tb_ad` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `name` varchar(50) DEFAULT NULL COMMENT '广告名称',
  `position` varchar(50) DEFAULT NULL COMMENT '广告位置',
  `start_time` datetime DEFAULT NULL COMMENT '开始时间',
  `end_time` datetime DEFAULT NULL COMMENT '到期时间',
  `status` char(1) DEFAULT NULL COMMENT '状态',
  `image` varchar(100) DEFAULT NULL COMMENT '图片地址',
  `url` varchar(100) DEFAULT NULL COMMENT 'URL',
  `remarks` varchar(1000) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8;

/*Data for the table `tb_ad` */

insert  into `tb_ad`(`id`,`name`,`position`,`start_time`,`end_time`,`status`,`image`,`url`,`remarks`) values 
(1,'轮播图1','web_index_lb','2019-02-12 15:28:51','2019-02-16 15:28:56','1','img/banner1.jpg','img/banner1.jpg',NULL),
(2,'轮播图2','web_index_lb','2019-02-14 15:29:42','2019-02-16 15:29:45','1','img/banner2.jpg','img/banner2.jpg',NULL),
(3,'轮播图3','web_index_lb','2019-02-06 15:29:59','2019-02-23 15:30:02','1','img/banner3.jpg','img/banner3.jpg',NULL);

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;
