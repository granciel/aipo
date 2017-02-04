--
-- Aipo is a groupware program developed by TOWN, Inc.
-- Copyright (C) 2004-2015 TOWN, Inc.
-- http://www.aipo.com
--
-- This program is free software: you can redistribute it and/or modify
-- it under the terms of the GNU Affero General Public License as
-- published by the Free Software Foundation, either version 3 of the
-- License, or (at your option) any later version.
--
-- This program is distributed in the hope that it will be useful,
-- but WITHOUT ANY WARRANTY; without even the implied warranty of
-- MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
-- GNU Affero General Public License for more details.
--
-- You should have received a copy of the GNU Affero General Public License
-- along with this program.  If not, see <http://www.gnu.org/licenses/>.
--

-- 20160328
ALTER TABLE `turbine_user` ADD `code` varchar(255) DEFAULT NULL AFTER `migrate_version`;

ALTER TABLE `eip_t_message_room_member` ADD `desktop_notification` varchar(1) DEFAULT 'A' AFTER `authority`;
ALTER TABLE `eip_t_message_room_member` ADD `mobile_notification` varchar(1) DEFAULT 'A' AFTER `desktop_notification`;

ALTER TABLE `eip_t_message_room_member` ADD `history_last_message_id` int(11) NOT NULL DEFAULT 0 AFTER `mobile_notification`;

ALTER TABLE `eip_t_ext_timecard_system` ADD `overtime_type` varchar(8) DEFAULT 'O' AFTER `outgoing_add_flag`;
UPDATE `eip_t_ext_timecard_system` SET `overtime_type` = 'O';

ALTER TABLE `eip_t_ext_timecard_system` ADD `holiday_of_week` varchar(32) DEFAULT 'A' AFTER `overtime_type`;
-- 20160328

-- 20160815
UPDATE `eip_t_acl_portlet_feature` SET `acl_type` = 27 WHERE `feature_name` = 'report_other' AND `feature_alias_name` = '報告書（他ユーザーの報告書）操作';
UPDATE `eip_t_acl_role` SET `acl_type` = 27 WHERE feature_id IN (SELECT feature_id FROM eip_t_acl_portlet_feature WHERE feature_name = 'report_other') AND `role_name` = '報告書（他ユーザーの報告書）管理者';
-- 20160815

-- 20170105
-- timeline
INSERT INTO `eip_t_acl_portlet_feature` VALUES(NULL,'timeline_post','タイムライン（自分の投稿）操作',21);
INSERT INTO `eip_t_acl_portlet_feature` VALUES(NULL,'timeline_post_other','タイムライン（他ユーザーの投稿）操作',17);
INSERT INTO `eip_t_acl_portlet_feature` VALUES(NULL,'timeline_comment','タイムライン（コメント）操作',20);
INSERT INTO `eip_t_acl_role` VALUES(NULL,'タイムライン（自分の投稿）管理者',(SELECT feature_id FROM eip_t_acl_portlet_feature WHERE feature_name = 'timeline_post' limit 1),21,'＊追加、削除は一覧表示の権限を持っていないと使用できません',NULL,NULL);
INSERT INTO `eip_t_acl_role` VALUES(NULL,'タイムライン（他ユーザーの投稿）管理者',(SELECT feature_id FROM eip_t_acl_portlet_feature WHERE feature_name = 'timeline_post_other' limit 1),1,NULL,NULL,NULL);
INSERT INTO `eip_t_acl_role` VALUES(NULL,'タイムライン（コメント）管理者',(SELECT feature_id FROM eip_t_acl_portlet_feature WHERE feature_name = 'timeline_comment' limit 1),20,NULL,NULL,NULL);
-- migration
INSERT INTO eip_t_acl_user_role_map(user_id,role_id) SELECT user_id,(SELECT role_id FROM eip_t_acl_role WHERE role_name = 'タイムライン（自分の投稿）管理者' limit 1) FROM turbine_user WHERE disabled!='T' AND NOT (login_name='admin' or login_name='anon' or login_name='template');
INSERT INTO eip_t_acl_user_role_map(user_id,role_id) SELECT user_id,(SELECT role_id FROM eip_t_acl_role WHERE role_name = 'タイムライン（他ユーザーの投稿）管理者' limit 1) FROM turbine_user WHERE disabled!='T' AND NOT (login_name='admin' or login_name='anon' or login_name='template');
INSERT INTO eip_t_acl_user_role_map(user_id,role_id) SELECT user_id,(SELECT role_id FROM eip_t_acl_role WHERE role_name = 'タイムライン（コメント）管理者' limit 1) FROM turbine_user WHERE disabled!='T' AND NOT (login_name='admin' or login_name='anon' or login_name='template');
-- 20170105
