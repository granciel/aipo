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

-- 20150609
DELETE FROM eip_t_timeline_like WHERE timeline_id NOT IN( SELECT timeline_id FROM eip_t_timeline );
ALTER TABLE eip_t_timeline_like ADD FOREIGN KEY (`timeline_id`) REFERENCES `eip_t_timeline` (`timeline_id`) ON DELETE CASCADE;
-- 20150609

-- 20151021
ALTER TABLE `eip_t_message_room_member` ADD `authority` varchar(1) DEFAULT 'A' AFTER `target_user_id`;
-- 20151021

-- 20151109
UPDATE `eip_t_acl_portlet_feature` SET `acl_type` = 19 WHERE `feature_name` = 'workflow_request_other' AND `feature_alias_name` = 'ワークフロー（他ユーザーの依頼）操作';
UPDATE `eip_t_acl_role` SET `acl_type` = 19 WHERE feature_id IN (SELECT feature_id FROM eip_t_acl_portlet_feature WHERE feature_name = 'workflow_request_other') AND `role_name` = 'ワークフロー（他ユーザーの依頼）管理者';
-- 20151109

-- 20151225
CREATE TABLE `eip_t_schedule_file` (
  `file_id` int(11) NOT NULL AUTO_INCREMENT,
  `owner_id` int(11),
  `schedule_id` int(11),
  `file_name` varchar(128) NOT NULL,
  `file_path` text NOT NULL,
  `file_thumbnail` blob,
  `create_date` date DEFAULT NULL,
  `update_date` datetime DEFAULT NULL,
  FOREIGN KEY (`schedule_id`) REFERENCES `eip_t_schedule` (`schedule_id`) ON DELETE CASCADE,
  PRIMARY KEY (`file_id`),
  KEY `eip_t_file_schedule_id_index` (`schedule_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;

-- 20151225

-- 20160317
ALTER TABLE `eip_t_message_room_member` ADD `desktop_notification` varchar(1) DEFAULT 'A';
ALTER TABLE `eip_t_message_room_member` ADD `mobile_notification` varchar(1) DEFAULT 'A';
-- 20150317
