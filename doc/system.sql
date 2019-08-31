/*
SQLyog Ultimate v12.3.1 (64 bit)
MySQL - 5.5.49 : Database - qingcheng_system
*********************************************************************
*/

/*!40101 SET NAMES utf8 */;

/*!40101 SET SQL_MODE=''*/;

/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;
CREATE DATABASE /*!32312 IF NOT EXISTS*/`qingcheng_system` /*!40100 DEFAULT CHARACTER SET utf8 */;

USE `qingcheng_system`;

/*Table structure for table `tb_admin` */

DROP TABLE IF EXISTS `tb_admin`;

CREATE TABLE `tb_admin` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `login_name` varchar(20) DEFAULT NULL COMMENT '用户名',
  `password` varchar(60) DEFAULT NULL COMMENT '密码',
  `status` char(1) DEFAULT NULL COMMENT '状态',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8;

/*Data for the table `tb_admin` */

insert  into `tb_admin`(`id`,`login_name`,`password`,`status`) values 
(1,'admin','$2a$10$61ogZY7EXsMDWeVGQpDq3OBF1.phaUu7.xrwLyWFTOu8woE08zMIW','1'),
(2,'test','$2a$10$61ogZY7EXsMDWeVGQpDq3OBF1.phaUu7.xrwLyWFTOu8woE08zMIW','1');

/*Table structure for table `tb_admin_role` */

DROP TABLE IF EXISTS `tb_admin_role`;

CREATE TABLE `tb_admin_role` (
  `admin_id` int(11) NOT NULL COMMENT '管理员ID',
  `role_id` int(11) NOT NULL COMMENT '角色ID',
  PRIMARY KEY (`admin_id`,`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

/*Data for the table `tb_admin_role` */

insert  into `tb_admin_role`(`admin_id`,`role_id`) values 
(1,3),
(2,2);

/*Table structure for table `tb_login_log` */

DROP TABLE IF EXISTS `tb_login_log`;

CREATE TABLE `tb_login_log` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `login_name` varchar(20) DEFAULT NULL,
  `ip` varchar(33) DEFAULT NULL,
  `browser_name` varchar(33) DEFAULT NULL,
  `location` varchar(33) DEFAULT NULL COMMENT '地区',
  `login_time` datetime NOT NULL COMMENT '登录时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=48 DEFAULT CHARSET=utf8;

/*Data for the table `tb_login_log` */

insert  into `tb_login_log`(`id`,`login_name`,`ip`,`browser_name`,`location`,`login_time`) values 
(42,'admin','0:0:0:0:0:0:0:1','webkit',NULL,'2019-02-22 18:29:10'),
(43,'admin','0:0:0:0:0:0:0:1','webkit',NULL,'2019-02-22 18:30:22'),
(44,'admin','0:0:0:0:0:0:0:1','webkit',NULL,'2019-02-22 18:30:31'),
(45,'admin','0:0:0:0:0:0:0:1','webkit','本地','2019-02-22 18:45:45'),
(46,'admin','0:0:0:0:0:0:0:1','webkit','本地','2019-02-28 17:51:23'),
(47,'admin','0:0:0:0:0:0:0:1','webkit','本地','2019-02-28 17:51:23');

/*Table structure for table `tb_menu` */

DROP TABLE IF EXISTS `tb_menu`;

CREATE TABLE `tb_menu` (
  `id` varchar(10) NOT NULL COMMENT '菜单ID',
  `name` varchar(50) DEFAULT NULL COMMENT '菜单名称',
  `icon` varchar(50) DEFAULT NULL COMMENT '图标',
  `url` varchar(50) DEFAULT NULL COMMENT 'URL',
  `parent_id` varchar(10) DEFAULT NULL COMMENT '上级菜单ID',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

/*Data for the table `tb_menu` */

insert  into `tb_menu`(`id`,`name`,`icon`,`url`,`parent_id`) values 
('1','首页','iconHome',NULL,'0'),
('2','商品','iconCommodity',NULL,'0'),
('2-1','商品管理','iconSp',NULL,'2'),
('2-1-1','商品列表',NULL,'goods/goods.html','2-1'),
('2-1-2','添加商品',NULL,'goods/goods_edit.html','2-1'),
('2-1-3','商品审核',NULL,'goods/goods_audit.html','2-1'),
('2-2','商品配置','iconSeting',NULL,'2'),
('2-2-1','商品分类',NULL,'goods/category.html','2-2'),
('2-2-2','规格参数',NULL,'goods/template.html','2-2'),
('2-2-3','品牌管理',NULL,'goods/brand.html','2-2'),
('2-2-4','图片库管理',NULL,'goods/album.html','2-2'),
('3','订单','iconOrder',NULL,'0'),
('3-1','订单管理',NULL,'','3'),
('3-1-1','订单列表',NULL,'order/order.html','3-1'),
('3-1-2','确认收货',NULL,'order/order.html','3-1'),
('3-1-3','到货提醒',NULL,'order/order_message.html','3-1'),
('3-1-4','订单设置',NULL,'order/order_config.html','3-1'),
('3-2-1','退货申请处理',NULL,'order/order.html','3-2'),
('3-2-2','退款申请处理',NULL,NULL,'3-2'),
('3-2-3','退款原因设置',NULL,NULL,'3-2'),
('4','库存','iconStock',NULL,'0'),
('4-1','库存记录',NULL,NULL,'4'),
('4-1-1','商品入库',NULL,NULL,'4-1'),
('4-1-2','商品出库',NULL,NULL,'4-1'),
('5','用户','iconUser',NULL,'0'),
('5-1','用户管理',NULL,NULL,'5'),
('5-1-1','用户列表',NULL,NULL,'5-1'),
('5-1-2','用户等级设置',NULL,NULL,'5-1'),
('5-2','成长值',NULL,NULL,'5'),
('5-2-1','成长值查询',NULL,NULL,'5-2'),
('5-2-2','任务奖励设置',NULL,NULL,'5-2'),
('6','运营','iconOperate',NULL,'0'),
('6-1','秒杀专区',NULL,NULL,'6'),
('6-1-1','秒杀活动列表',NULL,NULL,'6-1'),
('6-1-2','时间段列表',NULL,NULL,'6-1'),
('6-2','满减专区',NULL,NULL,'6'),
('6-2-1','满减活动列表',NULL,NULL,'6-2'),
('6-2-2','添加满减活动',NULL,NULL,'6-2'),
('6-3','优惠券管理',NULL,NULL,'6'),
('6-3-1','优惠券列表',NULL,NULL,'6-3'),
('6-3-2','添加优惠券',NULL,NULL,'6-3'),
('6-3-3','优惠券查询',NULL,NULL,'6-3'),
('6-4','活动管理',NULL,NULL,'6'),
('6-4-1','活动列表',NULL,NULL,'6-4'),
('6-4-2','添加活动',NULL,NULL,'6-4'),
('6-5','广告管理',NULL,NULL,'6'),
('6-5-1','广告列表',NULL,NULL,'6-5'),
('6-5-2','添加广告',NULL,NULL,'6-5'),
('7','统计','iconContent',NULL,'0'),
('7-1','统计分析',NULL,NULL,'7'),
('7-1-1','交易统计',NULL,NULL,'7-1'),
('7-1-2','流量统计',NULL,NULL,'7-1'),
('7-1-3','商品统计',NULL,NULL,'7-1'),
('7-1-4','会员统计',NULL,NULL,'7-1'),
('7-1-5','搜索统计',NULL,NULL,'7-1'),
('8','财务','iconStatistics',NULL,'0'),
('8-1','财务报表',NULL,NULL,'8'),
('8-1-1','综合统计',NULL,NULL,'8-1'),
('8-1-2','销售统计',NULL,NULL,'8-1'),
('8-2','对账管理',NULL,NULL,'8'),
('8-2-1','对账列表',NULL,NULL,'8-2'),
('9','设置','iconSeting',NULL,'0'),
('9-1','平台设置',NULL,NULL,'9'),
('9-1-1','平台信息',NULL,NULL,'9-1'),
('9-1-2','基本设置',NULL,NULL,'9-1'),
('9-1-3','消息提醒',NULL,NULL,'9-1'),
('9-1-4','验证码设置',NULL,NULL,'9-1'),
('9-2','支付配送',NULL,NULL,'9'),
('9-2-1','运费模板',NULL,NULL,'9-2'),
('9-2-2','物流公司',NULL,NULL,'9-2'),
('9-2-3','支付设置',NULL,NULL,'9-2'),
('9-2-4','区域管理',NULL,NULL,'9-2'),
('9-3','权限管理',NULL,NULL,'9'),
('9-3-1','角色管理',NULL,NULL,'9-3'),
('9-3-2','成员管理',NULL,NULL,'9-3'),
('9-3-3','操作日志',NULL,NULL,'9-3');

/*Table structure for table `tb_resource` */

DROP TABLE IF EXISTS `tb_resource`;

CREATE TABLE `tb_resource` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `res_key` varchar(50) DEFAULT NULL,
  `res_name` varchar(50) DEFAULT NULL,
  `parent_id` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=15 DEFAULT CHARSET=utf8;

/*Data for the table `tb_resource` */

insert  into `tb_resource`(`id`,`res_key`,`res_name`,`parent_id`) values 
(1,'goods','商品',0),
(2,'orders','订单',0),
(3,'stock','库存',0),
(4,'user','用户',0),
(5,'business','运营',0),
(6,'report','统计',0),
(7,'config','设置',0),
(8,'goods_add','商品添加',1),
(9,'goods_edit','商品修改',1),
(10,'goods_delete','商品删除',1),
(11,'goods_examine','商品审核',1),
(12,'goods_restore','商品恢复',1),
(13,'goods_real_delete','商品物理删除',1),
(14,'brand','品牌管理',1);

/*Table structure for table `tb_resource_menu` */

DROP TABLE IF EXISTS `tb_resource_menu`;

CREATE TABLE `tb_resource_menu` (
  `resource_id` int(11) NOT NULL,
  `menu_id` varchar(10) NOT NULL,
  PRIMARY KEY (`resource_id`,`menu_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

/*Data for the table `tb_resource_menu` */

insert  into `tb_resource_menu`(`resource_id`,`menu_id`) values 
(8,'2-1-2'),
(9,'2-1-1'),
(10,'2-1-1');

/*Table structure for table `tb_role` */

DROP TABLE IF EXISTS `tb_role`;

CREATE TABLE `tb_role` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `name` varchar(20) DEFAULT NULL COMMENT '角色名称',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8;

/*Data for the table `tb_role` */

insert  into `tb_role`(`id`,`name`) values 
(1,'超级管理员'),
(2,'客服'),
(3,'商品管理员');

/*Table structure for table `tb_role_resource` */

DROP TABLE IF EXISTS `tb_role_resource`;

CREATE TABLE `tb_role_resource` (
  `role_id` int(11) NOT NULL,
  `resource_id` int(11) NOT NULL,
  PRIMARY KEY (`role_id`,`resource_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

/*Data for the table `tb_role_resource` */

insert  into `tb_role_resource`(`role_id`,`resource_id`) values 
(2,8),
(3,8),
(3,9),
(3,10),
(3,14);

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;
