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

-----------------------------------------------------------------------------
-- AIPO_LICENSE
-----------------------------------------------------------------------------

CREATE TABLE AIPO_LICENSE
(
   LICENSE_ID INTEGER NOT NULL,
   LICENSE VARCHAR (99) NOT NULL,
   LIMIT_USERS INTEGER,
   PRIMARY KEY (LICENSE_ID)
);

-----------------------------------------------------------------------------
-- TURBINE_PERMISSION
-----------------------------------------------------------------------------

CREATE TABLE TURBINE_PERMISSION
(
    PERMISSION_ID INTEGER NOT NULL,
    PERMISSION_NAME VARCHAR (99) NOT NULL,
    OBJECTDATA bytea,
    PRIMARY KEY(PERMISSION_ID),
    UNIQUE (PERMISSION_NAME)
);

-----------------------------------------------------------------------------
-- TURBINE_ROLE
-----------------------------------------------------------------------------

CREATE TABLE TURBINE_ROLE
(
    ROLE_ID INTEGER NOT NULL,
    ROLE_NAME VARCHAR (99) NOT NULL,
    OBJECTDATA bytea,
    PRIMARY KEY(ROLE_ID),
    UNIQUE (ROLE_NAME)
);

-----------------------------------------------------------------------------
-- TURBINE_GROUP
-----------------------------------------------------------------------------

CREATE TABLE TURBINE_GROUP
(
    GROUP_ID INTEGER NOT NULL,
    GROUP_NAME VARCHAR (99) NOT NULL,
    OBJECTDATA bytea,
    OWNER_ID INTEGER,
    GROUP_ALIAS_NAME VARCHAR (99),
    PUBLIC_FLAG CHAR,
    PRIMARY KEY(GROUP_ID),
    UNIQUE (GROUP_NAME),
    UNIQUE (OWNER_ID, GROUP_ALIAS_NAME)
);

-----------------------------------------------------------------------------
-- TURBINE_ROLE_PERMISSION
-----------------------------------------------------------------------------

CREATE TABLE TURBINE_ROLE_PERMISSION
(
    ROLE_ID INTEGER NOT NULL,
    PERMISSION_ID INTEGER NOT NULL,
    PRIMARY KEY(ROLE_ID,PERMISSION_ID),
    FOREIGN KEY (ROLE_ID) REFERENCES TURBINE_ROLE (ROLE_ID),
    FOREIGN KEY (PERMISSION_ID) REFERENCES TURBINE_PERMISSION (PERMISSION_ID)
);

CREATE UNIQUE INDEX ROLE_PERMISSION_INDEX
  ON TURBINE_ROLE_PERMISSION(ROLE_ID,PERMISSION_ID);

-----------------------------------------------------------------------------
-- TURBINE_USER
-----------------------------------------------------------------------------

CREATE TABLE TURBINE_USER
(
    USER_ID INTEGER NOT NULL,
    LOGIN_NAME VARCHAR (32) NOT NULL,
    PASSWORD_VALUE VARCHAR (200) NOT NULL,
    FIRST_NAME VARCHAR (99) NOT NULL,
    LAST_NAME VARCHAR (99) NOT NULL,
    EMAIL VARCHAR (99),
    CONFIRM_VALUE VARCHAR (99),
    MODIFIED TIMESTAMP,
    CREATED TIMESTAMP,
    LAST_LOGIN TIMESTAMP,
    DISABLED CHAR,
    OBJECTDATA bytea,
    PASSWORD_CHANGED TIMESTAMP,
    COMPANY_ID INTEGER,
    POSITION_ID INTEGER,
    IN_TELEPHONE VARCHAR (15),
    OUT_TELEPHONE VARCHAR (15),
    CELLULAR_PHONE VARCHAR (15),
    CELLULAR_MAIL VARCHAR (99),
    CELLULAR_UID VARCHAR (99),
    FIRST_NAME_KANA VARCHAR (99),
    LAST_NAME_KANA VARCHAR(99),
    PHOTO bytea,
    HAS_PHOTO VARCHAR (1) DEFAULT 'F',
    PHOTO_MODIFIED TIMESTAMP,
    PHOTO_SMARTPHONE bytea,
    HAS_PHOTO_SMARTPHONE VARCHAR (1) DEFAULT 'F',
    PHOTO_MODIFIED_SMARTPHONE TIMESTAMP,
    TUTORIAL_FORBID VARCHAR (64) DEFAULT 'F',
    MIGRATE_VERSION INTEGER NOT NULL DEFAULT 0,
    CODE VARCHAR (255) DEFAULT NULL,
    CREATED_USER_ID INTEGER,
    UPDATED_USER_ID INTEGER,
    PRIMARY KEY(USER_ID),
    UNIQUE (LOGIN_NAME)
);

-----------------------------------------------------------------------------
-- TURBINE_USER_GROUP_ROLE
-----------------------------------------------------------------------------

CREATE TABLE TURBINE_USER_GROUP_ROLE
(
    ID INTEGER NOT NULL,
    USER_ID INTEGER NOT NULL,
    GROUP_ID INTEGER NOT NULL,
    ROLE_ID INTEGER NOT NULL,
    PRIMARY KEY(ID),
    FOREIGN KEY (USER_ID) REFERENCES TURBINE_USER (USER_ID),
    FOREIGN KEY (GROUP_ID) REFERENCES TURBINE_GROUP (GROUP_ID),
    FOREIGN KEY (ROLE_ID) REFERENCES TURBINE_ROLE (ROLE_ID)
);

-----------------------------------------------------------------------------
-- EIP_M_COMPANY
-----------------------------------------------------------------------------

CREATE TABLE EIP_M_COMPANY
(
    COMPANY_ID INTEGER NOT NULL,
    COMPANY_NAME VARCHAR (64) NOT NULL,
    COMPANY_NAME_KANA VARCHAR (64),
    ZIPCODE VARCHAR (8),
    ADDRESS VARCHAR (64),
    TELEPHONE VARCHAR (15),
    FAX_NUMBER VARCHAR (15),
    URL VARCHAR (99),
    IPADDRESS VARCHAR (99),
    PORT INTEGER,
    IPADDRESS_INTERNAL VARCHAR (99),
    PORT_INTERNAL INTEGER,
    CREATE_DATE DATE,
    UPDATE_DATE TIMESTAMP,
    PRIMARY KEY(COMPANY_ID)
);

-----------------------------------------------------------------------------
-- EIP_M_POST
-----------------------------------------------------------------------------

CREATE TABLE EIP_M_POST
(
    POST_ID INTEGER NOT NULL,
    COMPANY_ID INTEGER NOT NULL,
    POST_NAME VARCHAR (64) NOT NULL,
    ZIPCODE VARCHAR (8) ,
    ADDRESS VARCHAR (64),
    IN_TELEPHONE VARCHAR (15),
    OUT_TELEPHONE VARCHAR (15),
    FAX_NUMBER VARCHAR (15),
    GROUP_NAME VARCHAR (99),
    SORT INTEGER,
    CREATE_DATE DATE,
    UPDATE_DATE TIMESTAMP,
    PRIMARY KEY(POST_ID),
    UNIQUE (GROUP_NAME)
);

-----------------------------------------------------------------------------
-- EIP_M_POSITION
-----------------------------------------------------------------------------

CREATE TABLE EIP_M_POSITION
(
    POSITION_ID INTEGER NOT NULL,
    POSITION_NAME VARCHAR (64) NOT NULL,
    SORT INTEGER,
    CREATE_DATE DATE,
    UPDATE_DATE TIMESTAMP,
    PRIMARY KEY(POSITION_ID)
);


-----------------------------------------------------------------------------
-- EIP_M_USER_POSITION
-----------------------------------------------------------------------------

CREATE TABLE EIP_M_USER_POSITION
(
    ID INTEGER NOT NULL,
    USER_ID INTEGER,
    POSITION INTEGER,
    PRIMARY KEY(ID)
);

CREATE INDEX eip_m_user_position_index ON EIP_M_USER_POSITION(POSITION);

-----------------------------------------------------------------------------
-- EIP_T_COMMON_CATEGORY
-----------------------------------------------------------------------------

CREATE TABLE EIP_T_COMMON_CATEGORY
(
  COMMON_CATEGORY_ID INTEGER NOT NULL,
  NAME VARCHAR (64) NOT NULL,
  NOTE TEXT,
  CREATE_USER_ID INTEGER NOT NULL,
  UPDATE_USER_ID INTEGER NOT NULL,
  CREATE_DATE DATE,
  UPDATE_DATE TIMESTAMP,
  PRIMARY KEY(COMMON_CATEGORY_ID)
);

-----------------------------------------------------------------------------
-- EIP_T_SCHEDULE
-----------------------------------------------------------------------------

CREATE TABLE EIP_T_SCHEDULE
(
    SCHEDULE_ID INTEGER NOT NULL,
    PARENT_ID INTEGER,
    OWNER_ID INTEGER,
    REPEAT_PATTERN VARCHAR (10),
    START_DATE TIMESTAMP,
    END_DATE TIMESTAMP,
    NAME VARCHAR (99),
    PLACE VARCHAR (99),
    NOTE TEXT,
    PUBLIC_FLAG VARCHAR (1),
    EDIT_FLAG VARCHAR (1),
    MAIL_FLAG CHAR (1),
    CREATE_USER_ID INTEGER,
    UPDATE_USER_ID INTEGER,
    CREATE_DATE DATE,
    UPDATE_DATE TIMESTAMP,
    PRIMARY KEY(SCHEDULE_ID)
);

CREATE INDEX eip_t_schedule_date_index ON EIP_T_SCHEDULE(START_DATE, END_DATE, UPDATE_DATE);

-----------------------------------------------------------------------------
-- EIP_T_SCHEDULE_MAP
-----------------------------------------------------------------------------

CREATE TABLE EIP_T_SCHEDULE_MAP
(
   ID INTEGER NOT NULL,
   SCHEDULE_ID INTEGER NOT NULL,
   USER_ID INTEGER NOT NULL,
   STATUS VARCHAR (1),
   TYPE VARCHAR (1),
   COMMON_CATEGORY_ID INTEGER NOT NULL,
   FOREIGN KEY (SCHEDULE_ID) REFERENCES EIP_T_SCHEDULE (SCHEDULE_ID) ON DELETE CASCADE,
   FOREIGN KEY (COMMON_CATEGORY_ID) REFERENCES EIP_T_COMMON_CATEGORY (COMMON_CATEGORY_ID) ON DELETE CASCADE,
   PRIMARY KEY(ID)
);

CREATE INDEX eip_t_schedule_map_schedule_id_index ON EIP_T_SCHEDULE_MAP (SCHEDULE_ID);
CREATE INDEX eip_t_schedule_map_schedule_id_user_id_index ON EIP_T_SCHEDULE_MAP (SCHEDULE_ID, USER_ID);

-----------------------------------------------------------------------------
-- EIP_T_SCHEDULE_FILE
-----------------------------------------------------------------------------

CREATE TABLE EIP_T_SCHEDULE_FILE
(
    FILE_ID INTEGER NOT NULL,
    OWNER_ID INTEGER,
    SCHEDULE_ID INTEGER,
    FILE_NAME VARCHAR (128) NOT NULL,
    FILE_PATH TEXT NOT NULL,
    FILE_THUMBNAIL bytea,
    CREATE_DATE DATE,
    UPDATE_DATE TIMESTAMP,
    FOREIGN KEY (SCHEDULE_ID) REFERENCES EIP_T_SCHEDULE (SCHEDULE_ID) ON DELETE CASCADE,
    PRIMARY KEY (FILE_ID)
);

CREATE INDEX eip_t_file_schedule_id_index ON EIP_T_SCHEDULE_FILE (SCHEDULE_ID);

-----------------------------------------------------------------------------
-- EIP_T_TODO_CATEGORY
-----------------------------------------------------------------------------

CREATE TABLE EIP_T_TODO_CATEGORY
(
    CATEGORY_ID INTEGER NOT NULL,
    USER_ID INTEGER NOT NULL,
    UPDATE_USER_ID INTEGER NOT NULL,
    CATEGORY_NAME VARCHAR (64) NOT NULL,
    NOTE TEXT,
    CREATE_DATE DATE,
    UPDATE_DATE TIMESTAMP,
    PRIMARY KEY(CATEGORY_ID)
);

-----------------------------------------------------------------------------
-- EIP_T_TODO
-----------------------------------------------------------------------------

CREATE TABLE EIP_T_TODO
(
    TODO_ID INTEGER NOT NULL,
    USER_ID INTEGER NOT NULL,
    CREATE_USER_ID INTEGER NOT NULL,
    TODO_NAME VARCHAR (64) NOT NULL,
    CATEGORY_ID INTEGER,
    PRIORITY smallint,
    STATE smallint,
    NOTE TEXT,
    START_DATE DATE,
    END_DATE DATE,
    PUBLIC_FLAG VARCHAR (1) NOT NULL,
    ADDON_SCHEDULE_FLG VARCHAR (1) NOT NULL,
    CREATE_DATE DATE,
    UPDATE_DATE TIMESTAMP,
    FOREIGN KEY (CATEGORY_ID) REFERENCES EIP_T_TODO_CATEGORY (CATEGORY_ID) ON DELETE CASCADE,
    PRIMARY KEY(TODO_ID)
);

-----------------------------------------------------------------------------
-- EIP_M_MAIL_ACCOUNT
-----------------------------------------------------------------------------

CREATE TABLE EIP_M_MAIL_ACCOUNT
(
    ACCOUNT_ID INTEGER NOT NULL,
    USER_ID INTEGER NOT NULL,
    ACCOUNT_NAME VARCHAR (200) NOT NULL,
    ACCOUNT_TYPE VARCHAR (1),
    SMTPSERVER_NAME VARCHAR (64) NOT NULL,
    POP3SERVER_NAME VARCHAR (64) NOT NULL,
    POP3USER_NAME VARCHAR (64) NOT NULL,
    POP3PASSWORD bytea NOT NULL,
    MAIL_USER_NAME VARCHAR (64),
    MAIL_ADDRESS VARCHAR (64) NOT NULL,
    SMTP_PORT VARCHAR (5) NOT NULL,
    SMTP_ENCRYPTION_FLG smallint,
    POP3_PORT VARCHAR (5) NOT NULL,
    POP3_ENCRYPTION_FLG smallint,
    AUTH_SEND_FLG smallint,
    AUTH_SEND_USER_ID VARCHAR (64),
    AUTH_SEND_USER_PASSWD bytea,
    AUTH_RECEIVE_FLG smallint,
    DEL_AT_POP3_FLG VARCHAR (1),
    DEL_AT_POP3_BEFORE_DAYS_FLG VARCHAR (1),
    DEL_AT_POP3_BEFORE_DAYS INTEGER,
    NON_RECEIVED_FLG VARCHAR (1),
    DEFAULT_FOLDER_ID INTEGER,
    LAST_RECEIVED_DATE TIMESTAMP,
    SIGNATURE TEXT,
    CREATE_DATE DATE,
    UPDATE_DATE TIMESTAMP,
    PRIMARY KEY(ACCOUNT_ID)
);

-----------------------------------------------------------------------------
-- EIP_M_MAIL_NOTIFY_CONF
-----------------------------------------------------------------------------

CREATE TABLE EIP_M_MAIL_NOTIFY_CONF
(
    NOTIFY_ID INTEGER NOT NULL,
    USER_ID INTEGER NOT NULL,
    NOTIFY_TYPE INTEGER NOT NULL,
    NOTIFY_FLG INTEGER NOT NULL,
    NOTIFY_TIME TIME,
    CREATE_DATE DATE,
    UPDATE_DATE TIMESTAMP,
    PRIMARY KEY(NOTIFY_ID)
);

-----------------------------------------------------------------------------
-- EIP_T_MAIL
-----------------------------------------------------------------------------

CREATE TABLE EIP_T_MAIL
(
    MAIL_ID INTEGER NOT NULL,
    USER_ID INTEGER NOT NULL,
    ACCOUNT_ID INTEGER NOT NULL,
    FOLDER_ID INTEGER NOT NULL,
    TYPE char (1),
    READ_FLG char (1),
    SUBJECT TEXT,
    PERSON TEXT,
    EVENT_DATE TIMESTAMP,
    FILE_VOLUME INTEGER,
    HAS_FILES char (1),
    FILE_PATH TEXT,
    MAIL bytea,
    CREATE_DATE DATE,
    UPDATE_DATE TIMESTAMP,
    PRIMARY KEY (MAIL_ID)
);

CREATE INDEX eip_t_mail_user_id_index ON EIP_T_MAIL (USER_ID);

-----------------------------------------------------------------------------
-- EIP_T_MAIL_FOLDER
-----------------------------------------------------------------------------

CREATE TABLE EIP_T_MAIL_FOLDER
(
    FOLDER_ID INTEGER NOT NULL,
    ACCOUNT_ID INTEGER,
    FOLDER_NAME VARCHAR(128),
    CREATE_DATE DATE,
    UPDATE_DATE TIMESTAMP,
    PRIMARY KEY (FOLDER_ID)
);

-----------------------------------------------------------------------------
-- EIP_T_MAIL_FILTER
-----------------------------------------------------------------------------

CREATE TABLE EIP_T_MAIL_FILTER
(
    FILTER_ID INTEGER NOT NULL,
    ACCOUNT_ID INTEGER,
    DST_FOLDER_ID INTEGER,
    FILTER_NAME VARCHAR(255),
    FILTER_STRING VARCHAR(255),
    FILTER_TYPE char(1),
    SORT_ORDER INTEGER,
    CREATE_DATE DATE,
    UPDATE_DATE TIMESTAMP,
    PRIMARY KEY (FILTER_ID)
);

-----------------------------------------------------------------------------
-- EIP_M_ADDRESSBOOK
-----------------------------------------------------------------------------

CREATE TABLE EIP_M_ADDRESSBOOK
(
    ADDRESS_ID INTEGER NOT NULL,
    OWNER_ID INTEGER,
    FIRST_NAME VARCHAR(99),
    LAST_NAME VARCHAR(99),
    FIRST_NAME_KANA VARCHAR(99),
    LAST_NAME_KANA VARCHAR(99),
    EMAIL VARCHAR(99),
    TELEPHONE VARCHAR(15),
    CELLULAR_PHONE VARCHAR(15),
    CELLULAR_MAIL VARCHAR(99),
    COMPANY_ID INTEGER,
    POSITION_NAME VARCHAR(64),
    PUBLIC_FLAG VARCHAR(1),
    NOTE TEXT,
    CREATE_USER_ID INTEGER,
    UPDATE_USER_ID INTEGER,
    CREATE_DATE DATE,
    UPDATE_DATE TIMESTAMP,
    PRIMARY KEY(ADDRESS_ID)
);

-----------------------------------------------------------------------------
-- EIP_M_ADDRESS_GROUP
-----------------------------------------------------------------------------

CREATE TABLE EIP_M_ADDRESS_GROUP
(
    GROUP_ID INTEGER NOT NULL,
    GROUP_NAME VARCHAR(99) NOT NULL,
    OWNER_ID INTEGER,
    PUBLIC_FLAG VARCHAR(1),
    CREATE_DATE DATE,
    UPDATE_DATE TIMESTAMP,
    PRIMARY KEY(GROUP_ID)
);

-----------------------------------------------------------------------------
-- EIP_M_ADDRESSBOOK_COMPANY
-----------------------------------------------------------------------------

CREATE TABLE EIP_M_ADDRESSBOOK_COMPANY
(
    COMPANY_ID INTEGER NOT NULL,
    COMPANY_NAME VARCHAR(64) NOT NULL,
    COMPANY_NAME_KANA VARCHAR(64),
    POST_NAME VARCHAR(64),
    ZIPCODE VARCHAR(8),
    ADDRESS VARCHAR(64),
    TELEPHONE VARCHAR(15),
    FAX_NUMBER VARCHAR(15),
    URL VARCHAR(99),
    CREATE_USER_ID INTEGER,
    UPDATE_USER_ID INTEGER,
    CREATE_DATE DATE,
    UPDATE_DATE TIMESTAMP,
    PRIMARY KEY(COMPANY_ID)
);

-----------------------------------------------------------------------------
-- EIP_T_ADDRESSBOOK_GROUP_MAP
-----------------------------------------------------------------------------

CREATE TABLE EIP_T_ADDRESSBOOK_GROUP_MAP
(
    ID INTEGER NOT NULL,
    ADDRESS_ID INTEGER NOT NULL,
    GROUP_ID INTEGER NOT NULL,
    PRIMARY KEY(ID)
);

-----------------------------------------------------------------------------
-- EIP_T_NOTE
-----------------------------------------------------------------------------

CREATE TABLE EIP_T_NOTE
(
    NOTE_ID INTEGER NOT NULL,
    OWNER_ID VARCHAR (99),
    CLIENT_NAME VARCHAR (99),
    COMPANY_NAME VARCHAR (99),
    TELEPHONE VARCHAR (24),
    EMAIL_ADDRESS VARCHAR (99),
    ADD_DEST_TYPE VARCHAR (1),
    SUBJECT_TYPE VARCHAR (1),
    CUSTOM_SUBJECT VARCHAR (99),
    MESSAGE TEXT,
    ACCEPT_DATE TIMESTAMP,
    CREATE_DATE TIMESTAMP,
    UPDATE_DATE TIMESTAMP,
    PRIMARY KEY(NOTE_ID)
);

-----------------------------------------------------------------------------
-- EIP_T_NOTE_MAP
-----------------------------------------------------------------------------

CREATE TABLE EIP_T_NOTE_MAP
(
   ID INTEGER NOT NULL,
   NOTE_ID INTEGER NOT NULL,
   USER_ID VARCHAR (99) NOT NULL,
   DEL_FLG VARCHAR (1),
   NOTE_STAT VARCHAR (1),
   CONFIRM_DATE TIMESTAMP,
   FOREIGN KEY (NOTE_ID) REFERENCES EIP_T_NOTE (NOTE_ID) ON DELETE CASCADE,
   PRIMARY KEY(ID)
);

CREATE INDEX eip_t_note_map_user_id_index ON EIP_T_NOTE_MAP(USER_ID);

-----------------------------------------------------------------------------
-- EIP_T_MSGBOARD_CATEGORY
-----------------------------------------------------------------------------

CREATE TABLE EIP_T_MSGBOARD_CATEGORY
(
    CATEGORY_ID INTEGER NOT NULL,
    OWNER_ID INTEGER,
    CATEGORY_NAME VARCHAR (99),
    NOTE TEXT,
    PUBLIC_FLAG VARCHAR (1),
    CREATE_DATE DATE,
    UPDATE_DATE TIMESTAMP,
    PRIMARY KEY(CATEGORY_ID)
);

-----------------------------------------------------------------------------
-- EIP_T_MSGBOARD_CATEGORY_MAP
-----------------------------------------------------------------------------

CREATE TABLE EIP_T_MSGBOARD_CATEGORY_MAP
(
   ID INTEGER NOT NULL,
   CATEGORY_ID INTEGER,
   USER_ID INTEGER,
   STATUS VARCHAR (1),
   FOREIGN KEY (CATEGORY_ID) REFERENCES EIP_T_MSGBOARD_CATEGORY (CATEGORY_ID) ON DELETE CASCADE,
   PRIMARY KEY(ID)
);

-----------------------------------------------------------------------------
-- EIP_T_MSGBOARD_TOPIC
-----------------------------------------------------------------------------

CREATE TABLE EIP_T_MSGBOARD_TOPIC
(
    TOPIC_ID INTEGER NOT NULL,
    PARENT_ID INTEGER,
    OWNER_ID INTEGER,
    TOPIC_NAME VARCHAR (64) NOT NULL,
    NOTE TEXT,
    CATEGORY_ID INTEGER,
    CREATE_USER_ID INTEGER,
    UPDATE_USER_ID INTEGER,
    CREATE_DATE TIMESTAMP,
    UPDATE_DATE TIMESTAMP,
    FOREIGN KEY (CATEGORY_ID) REFERENCES EIP_T_MSGBOARD_CATEGORY (CATEGORY_ID) ON DELETE CASCADE,
    PRIMARY KEY(TOPIC_ID)
);

CREATE INDEX eip_t_msgboard_topic_category_id_index ON EIP_T_MSGBOARD_TOPIC(CATEGORY_ID);

-----------------------------------------------------------------------------
-- EIP_T_MSGBOARD_FILE
-----------------------------------------------------------------------------

CREATE TABLE EIP_T_MSGBOARD_FILE
(
    FILE_ID INTEGER NOT NULL,
    OWNER_ID INTEGER,
    TOPIC_ID INTEGER,
    FILE_NAME VARCHAR (128) NOT NULL,
    FILE_PATH TEXT NOT NULL,
    FILE_THUMBNAIL bytea,
    CREATE_DATE DATE,
    UPDATE_DATE TIMESTAMP,
    FOREIGN KEY (TOPIC_ID) REFERENCES EIP_T_MSGBOARD_TOPIC (TOPIC_ID) ON DELETE CASCADE,
    PRIMARY KEY (FILE_ID)
);

-----------------------------------------------------------------------------
-- EIP_T_BLOG
-----------------------------------------------------------------------------

CREATE TABLE EIP_T_BLOG
(
    BLOG_ID INTEGER NOT NULL,
    OWNER_ID INTEGER NOT NULL,
    CREATE_DATE DATE,
    UPDATE_DATE TIMESTAMP,
    PRIMARY KEY (BLOG_ID)
);

-----------------------------------------------------------------------------
-- EIP_T_BLOG_THEMA
-----------------------------------------------------------------------------

CREATE TABLE EIP_T_BLOG_THEMA
(
    THEMA_ID INTEGER NOT NULL,
    THEMA_NAME varchar (64) NOT NULL,
    DESCRIPTION TEXT,
    CREATE_USER_ID INTEGER NOT NULL,
    UPDATE_USER_ID INTEGER NOT NULL,
    CREATE_DATE DATE,
    UPDATE_DATE TIMESTAMP,
    PRIMARY KEY (THEMA_ID)
);

-----------------------------------------------------------------------------
-- EIP_T_BLOG_ENTRY
-----------------------------------------------------------------------------

CREATE TABLE EIP_T_BLOG_ENTRY
(
    ENTRY_ID INTEGER NOT NULL,
    OWNER_ID INTEGER NOT NULL,
    TITLE varchar (99) NOT NULL,
    NOTE TEXT,
    BLOG_ID INTEGER NOT NULL,
    THEMA_ID INTEGER,
    ALLOW_COMMENTS varchar (1),
    CREATE_DATE TIMESTAMP,
    UPDATE_DATE TIMESTAMP,
    FOREIGN KEY (BLOG_ID) REFERENCES EIP_T_BLOG (BLOG_ID) ON DELETE CASCADE,
    FOREIGN KEY (THEMA_ID) REFERENCES EIP_T_BLOG_THEMA (THEMA_ID),
    PRIMARY KEY (ENTRY_ID)
);

-----------------------------------------------------------------------------
-- EIP_T_BLOG_FILE
-----------------------------------------------------------------------------

CREATE TABLE EIP_T_BLOG_FILE
(
    FILE_ID INTEGER NOT NULL,
    OWNER_ID INTEGER NOT NULL,
    TITLE varchar (99) NOT NULL,
    FILE_PATH TEXT NOT NULL,
    FILE_THUMBNAIL bytea,
    ENTRY_ID INTEGER NOT NULL,
    CREATE_DATE DATE,
    UPDATE_DATE TIMESTAMP,
    FOREIGN KEY (ENTRY_ID) REFERENCES EIP_T_BLOG_ENTRY (ENTRY_ID) ON DELETE CASCADE,
    PRIMARY KEY (FILE_ID)
);

-----------------------------------------------------------------------------
-- EIP_T_BLOG_COMMENT
-----------------------------------------------------------------------------

CREATE TABLE EIP_T_BLOG_COMMENT
(
    COMMENT_ID INTEGER NOT NULL,
    OWNER_ID INTEGER NOT NULL,
    COMMENT TEXT,
    ENTRY_ID INTEGER NOT NULL,
    CREATE_DATE DATE,
    UPDATE_DATE TIMESTAMP,
    FOREIGN KEY (ENTRY_ID) REFERENCES EIP_T_BLOG_ENTRY (ENTRY_ID) ON DELETE CASCADE,
    PRIMARY KEY (COMMENT_ID)
);

-----------------------------------------------------------------------------
-- EIP_T_BLOG_FOOTMARK_MAP
-----------------------------------------------------------------------------

CREATE TABLE EIP_T_BLOG_FOOTMARK_MAP
(
    ID INTEGER NOT NULL,
    BLOG_ID INTEGER NOT NULL,
    USER_ID INTEGER NOT NULL,
    CREATE_DATE DATE NOT NULL,
    UPDATE_DATE TIMESTAMP,
    FOREIGN KEY (BLOG_ID) REFERENCES EIP_T_BLOG (BLOG_ID) ON DELETE CASCADE,
    PRIMARY KEY (ID)
);

-----------------------------------------------------------------------------
-- EIP_T_CABINET_FOLDER
-----------------------------------------------------------------------------

CREATE TABLE EIP_T_CABINET_FOLDER
(
    FOLDER_ID INTEGER NOT NULL,
    PARENT_ID INTEGER NOT NULL,
    FOLDER_NAME VARCHAR (128) NOT NULL,
    NOTE TEXT,
    CREATE_USER_ID INTEGER,
    UPDATE_USER_ID INTEGER,
    PUBLIC_FLAG VARCHAR (1),
    CREATE_DATE DATE,
    UPDATE_DATE TIMESTAMP,
    PRIMARY KEY (FOLDER_ID)
);

-----------------------------------------------------------------------------
-- EIP_T_CABINET_FOLDER_MAP
-----------------------------------------------------------------------------

CREATE TABLE EIP_T_CABINET_FOLDER_MAP
(
   ID INTEGER NOT NULL,
   FOLDER_ID INTEGER,
   USER_ID INTEGER,
   STATUS VARCHAR (1),
   FOREIGN KEY (FOLDER_ID) REFERENCES EIP_T_CABINET_FOLDER (FOLDER_ID) ON DELETE CASCADE,
   PRIMARY KEY(ID)
);

-----------------------------------------------------------------------------
-- EIP_T_CABINET_FILE
-----------------------------------------------------------------------------

CREATE TABLE EIP_T_CABINET_FILE
(
    FILE_ID INTEGER NOT NULL,
    FOLDER_ID INTEGER NOT NULL,
    FILE_TITLE VARCHAR (128) NOT NULL,
    FILE_NAME VARCHAR (128) NOT NULL,
    FILE_SIZE BIGINT,
    FILE_PATH TEXT NOT NULL,
    NOTE TEXT,
    COUNTER INTEGER,
    CREATE_USER_ID INTEGER,
    UPDATE_USER_ID INTEGER,
    CREATE_DATE DATE,
    UPDATE_DATE TIMESTAMP,
    FOREIGN KEY (FOLDER_ID) REFERENCES EIP_T_CABINET_FOLDER (FOLDER_ID) ON DELETE CASCADE,
    PRIMARY KEY (FILE_ID)
);

-----------------------------------------------------------------------------
-- EIP_M_FACILITY
-----------------------------------------------------------------------------

CREATE TABLE EIP_M_FACILITY
(
    FACILITY_ID INTEGER NOT NULL,
    USER_ID INTEGER NOT NULL,
    FACILITY_NAME VARCHAR (64) NOT NULL,
    NOTE TEXT,
    SORT INTEGER,
    CREATE_DATE DATE,
    UPDATE_DATE TIMESTAMP,
    PRIMARY KEY(FACILITY_ID)
);

-----------------------------------------------------------------------------
-- EIP_FACILITY_GROUP
-----------------------------------------------------------------------------

CREATE TABLE EIP_FACILITY_GROUP
(
    ID INTEGER NOT NULL,
    FACILITY_ID INTEGER NOT NULL,
    GROUP_ID INTEGER NOT NULL,
    SORT INTEGER,
    PRIMARY KEY(ID),
    FOREIGN KEY (FACILITY_ID) REFERENCES EIP_M_FACILITY (FACILITY_ID) ON DELETE CASCADE,
    FOREIGN KEY (GROUP_ID) REFERENCES TURBINE_GROUP (GROUP_ID)
);

-----------------------------------------------------------------------------
-- EIP_T_TIMECARD
-----------------------------------------------------------------------------

CREATE TABLE EIP_T_TIMECARD
(
    TIMECARD_ID INTEGER NOT NULL,
    USER_ID INTEGER NOT NULL,
    WORK_DATE TIMESTAMP,
    WORK_FLAG VARCHAR (1) NOT NULL,
    REASON TEXT,
    CREATE_DATE TIMESTAMP,
    UPDATE_DATE TIMESTAMP,
    PRIMARY KEY(TIMECARD_ID)
);

-----------------------------------------------------------------------------
-- EIP_T_TIMECARD_SETTINGS
-----------------------------------------------------------------------------

CREATE TABLE EIP_T_TIMECARD_SETTINGS
(
  TIMECARD_SETTINGS_ID INTEGER NOT NULL,
  USER_ID INTEGER NOT NULL,
  START_HOUR int4,
  START_MINUTE int4,
  END_HOUR int4,
  END_MINUTE int4,
  WORKTIME_IN int4,
  RESTTIME_IN int4,
  WORKTIME_OUT int4,
  RESTTIME_OUT int4,
  CREATE_DATE TIMESTAMP,
  UPDATE_DATE TIMESTAMP,
  PRIMARY KEY(TIMECARD_SETTINGS_ID)
);

-----------------------------------------------------------------------------
-- EIP_T_EXT_TIMECARD
-----------------------------------------------------------------------------

CREATE TABLE EIP_T_EXT_TIMECARD
(
    TIMECARD_ID INTEGER NOT NULL,
    USER_ID INTEGER NOT NULL,
    PUNCH_DATE DATE,
    TYPE VARCHAR (1),
    CLOCK_IN_TIME TIMESTAMP,
    CLOCK_OUT_TIME TIMESTAMP,
    REASON TEXT,
    OUTGOING_TIME1 TIMESTAMP,
    COMEBACK_TIME1 TIMESTAMP,
    OUTGOING_TIME2 TIMESTAMP,
    COMEBACK_TIME2 TIMESTAMP,
    OUTGOING_TIME3 TIMESTAMP,
    COMEBACK_TIME3 TIMESTAMP,
    OUTGOING_TIME4 TIMESTAMP,
    COMEBACK_TIME4 TIMESTAMP,
    OUTGOING_TIME5 TIMESTAMP,
    COMEBACK_TIME5 TIMESTAMP,
    REMARKS TEXT,
    CREATE_DATE DATE,
    UPDATE_DATE TIMESTAMP,
    PRIMARY KEY(TIMECARD_ID)
);

CREATE INDEX eip_t_ext_timecard_user_id_index ON EIP_T_EXT_TIMECARD(USER_ID);

-----------------------------------------------------------------------------
-- EIP_T_EXT_TIMECARD_SYSTEM
-----------------------------------------------------------------------------

CREATE TABLE EIP_T_EXT_TIMECARD_SYSTEM
(
    SYSTEM_ID INTEGER NOT NULL,
    USER_ID INTEGER NOT NULL,
    SYSTEM_NAME VARCHAR (64),
    START_HOUR INTEGER,
    START_MINUTE INTEGER,
    END_HOUR INTEGER,
    END_MINUTE INTEGER,
    START_DAY SMALLINT,
    WORKTIME_IN INTEGER,
    RESTTIME_IN INTEGER,
    WORKTIME_OUT INTEGER,
    RESTTIME_OUT INTEGER,
    CHANGE_HOUR INTEGER,
    OUTGOING_ADD_FLAG VARCHAR (1),
    OVERTIME_TYPE VARCHAR (8) DEFAULT 'O',
    HOLIDAY_OF_WEEK VARCHAR (32) DEFAULT 'A',
    CREATE_DATE DATE,
    UPDATE_DATE TIMESTAMP,
    PRIMARY KEY(SYSTEM_ID)
);

-----------------------------------------------------------------------------
-- EIP_T_EXT_TIMECARD_SYSTEM_MAP
-----------------------------------------------------------------------------

CREATE TABLE EIP_T_EXT_TIMECARD_SYSTEM_MAP
(
    SYSTEM_MAP_ID INTEGER NOT NULL,
    USER_ID INTEGER NOT NULL,
    SYSTEM_ID INTEGER NOT NULL,
    CREATE_DATE DATE,
    UPDATE_DATE TIMESTAMP,
    FOREIGN KEY (SYSTEM_ID) REFERENCES EIP_T_EXT_TIMECARD_SYSTEM (SYSTEM_ID) ON DELETE CASCADE,
    PRIMARY KEY(SYSTEM_MAP_ID)
);

-----------------------------------------------------------------------------
-- EIP_T_WORKFLOW_ROUTE
-----------------------------------------------------------------------------

CREATE TABLE EIP_T_WORKFLOW_ROUTE
(
    ROUTE_ID INTEGER NOT NULL,
    ROUTE_NAME VARCHAR (64) NOT NULL,
    NOTE TEXT,
    CREATE_DATE DATE,
    UPDATE_DATE TIMESTAMP,
    ROUTE TEXT,
    PRIMARY KEY(ROUTE_ID)
);

-----------------------------------------------------------------------------
-- EIP_T_WORKFLOW_CATEGORY
-----------------------------------------------------------------------------

CREATE TABLE EIP_T_WORKFLOW_CATEGORY
(
    CATEGORY_ID INTEGER NOT NULL,
    USER_ID INTEGER NOT NULL,
    CATEGORY_NAME VARCHAR (64) NOT NULL,
    NOTE TEXT,
    CREATE_DATE DATE,
    UPDATE_DATE TIMESTAMP,
    TEMPLATE TEXT,
    ROUTE_ID INTEGER,
    FOREIGN KEY (ROUTE_ID) REFERENCES EIP_T_WORKFLOW_ROUTE (ROUTE_ID),
    PRIMARY KEY(CATEGORY_ID)
);

-----------------------------------------------------------------------------
-- EIP_T_WORKFLOW_REQUEST
-----------------------------------------------------------------------------

CREATE TABLE EIP_T_WORKFLOW_REQUEST
(
    REQUEST_ID INTEGER NOT NULL,
    PARENT_ID INTEGER,
    USER_ID INTEGER NOT NULL,
    REQUEST_NAME VARCHAR (64),
    CATEGORY_ID INTEGER,
    PRIORITY smallint,
    PROGRESS VARCHAR (1),
    NOTE TEXT,
    PRICE bigint,
    CREATE_DATE TIMESTAMP,
    UPDATE_DATE TIMESTAMP,
    ROUTE_ID INTEGER,
    FOREIGN KEY (CATEGORY_ID) REFERENCES EIP_T_WORKFLOW_CATEGORY (CATEGORY_ID) ON DELETE CASCADE,
    FOREIGN KEY (ROUTE_ID) REFERENCES EIP_T_WORKFLOW_ROUTE (ROUTE_ID),
    PRIMARY KEY(REQUEST_ID)
);

-----------------------------------------------------------------------------
-- EIP_T_WORKFLOW_FILE
-----------------------------------------------------------------------------

CREATE TABLE EIP_T_WORKFLOW_FILE
(
    FILE_ID INTEGER NOT NULL,
    OWNER_ID INTEGER,
    REQUEST_ID INTEGER,
    FILE_NAME VARCHAR (128) NOT NULL,
    FILE_PATH TEXT NOT NULL,
    FILE_THUMBNAIL bytea,
    CREATE_DATE DATE,
    UPDATE_DATE TIMESTAMP,
    FOREIGN KEY (REQUEST_ID) REFERENCES EIP_T_WORKFLOW_REQUEST (REQUEST_ID) ON DELETE CASCADE,
    PRIMARY KEY (FILE_ID)
);

-----------------------------------------------------------------------------
-- EIP_T_WORKFLOW_REQUEST_MAP
-----------------------------------------------------------------------------

CREATE TABLE EIP_T_WORKFLOW_REQUEST_MAP
(
   ID INTEGER NOT NULL,
   REQUEST_ID INTEGER NOT NULL,
   USER_ID INTEGER NOT NULL,
   STATUS VARCHAR (1),
   ORDER_INDEX INTEGER NOT NULL,
   NOTE TEXT,
   CREATE_DATE DATE,
   UPDATE_DATE TIMESTAMP,
   FOREIGN KEY (REQUEST_ID) REFERENCES EIP_T_WORKFLOW_REQUEST (REQUEST_ID) ON DELETE CASCADE,
   PRIMARY KEY(ID)
);

-----------------------------------------------------------------------------
-- EIP_T_MEMO
-----------------------------------------------------------------------------

CREATE TABLE EIP_T_MEMO
(
    MEMO_ID INTEGER NOT NULL,
    OWNER_ID INTEGER NOT NULL,
    MEMO_NAME VARCHAR (64) NOT NULL,
    NOTE TEXT,
    CREATE_DATE DATE,
    UPDATE_DATE TIMESTAMP,
    PRIMARY KEY(MEMO_ID)
);

-----------------------------------------------------------------------------
-- EIP_T_WHATSNEW
-----------------------------------------------------------------------------

CREATE TABLE EIP_T_WHATSNEW
(
    WHATSNEW_ID INTEGER NOT NULL,
    USER_ID INTEGER,
    PORTLET_TYPE INTEGER,
    PARENT_ID INTEGER,
    ENTITY_ID INTEGER,
    CREATE_DATE TIMESTAMP,
    UPDATE_DATE TIMESTAMP,
    PRIMARY KEY(WHATSNEW_ID)
);

-----------------------------------------------------------------------------
-- EIP_T_EVENTLOG
-----------------------------------------------------------------------------

CREATE TABLE EIP_T_EVENTLOG
(
    EVENTLOG_ID INTEGER NOT NULL,
    USER_ID INTEGER,
    EVENT_DATE TIMESTAMP,
    EVENT_TYPE INTEGER,
    PORTLET_TYPE INTEGER,
    ENTITY_ID INTEGER,
    IP_ADDR TEXT,
    NOTE TEXT,
    CREATE_DATE TIMESTAMP,
    UPDATE_DATE TIMESTAMP,
    PRIMARY KEY(EVENTLOG_ID)
);
CREATE INDEX eip_t_eventlog_event_type_index ON EIP_T_EVENTLOG(EVENT_TYPE);
CREATE INDEX eip_t_eventlog_user_id_index ON EIP_T_EVENTLOG(USER_ID);
-----------------------------------------------------------------------------
-- EIP_T_ACL_ROLE
-----------------------------------------------------------------------------

CREATE TABLE EIP_T_ACL_ROLE
(
    ROLE_ID INTEGER NOT NULL,
    ROLE_NAME VARCHAR (99) NOT NULL,
    FEATURE_ID INTEGER NOT NULL,
    ACL_TYPE INTEGER,
    NOTE TEXT,
    CREATE_DATE DATE,
    UPDATE_DATE TIMESTAMP,
    PRIMARY KEY(ROLE_ID)
);

CREATE INDEX eip_t_acl_role_acl_type_index ON EIP_T_ACL_ROLE(ACL_TYPE);

-----------------------------------------------------------------------------
-- EIP_T_ACL_PORTLET_FEATURE
-----------------------------------------------------------------------------

CREATE TABLE EIP_T_ACL_PORTLET_FEATURE
(
  FEATURE_ID INTEGER NOT NULL,
  FEATURE_NAME VARCHAR(99),
  FEATURE_ALIAS_NAME VARCHAR(99),
  ACL_TYPE INTEGER,
  PRIMARY KEY(FEATURE_ID)
);

-----------------------------------------------------------------------------
-- EIP_T_ACL_USER_ROLE_MAP
-----------------------------------------------------------------------------

CREATE TABLE EIP_T_ACL_USER_ROLE_MAP
(
    ID INTEGER NOT NULL,
    USER_ID INT4 NOT NULL,
    ROLE_ID INT4 NOT NULL,
    PRIMARY KEY(ID)
);

CREATE INDEX eip_t_acl_user_role_map_role_id_index ON EIP_T_ACL_USER_ROLE_MAP(ROLE_ID);

CREATE TABLE jetspeed_group_profile (
    COUNTRY varchar(2) NULL,
    GROUP_NAME varchar(99) NULL,
    LANGUAGE varchar(2) NULL,
    MEDIA_TYPE varchar(99) NULL,
    PAGE varchar(99) NULL,
    PROFILE bytea NULL,
    PSML_ID INTEGER NOT NULL,
    PRIMARY KEY (PSML_ID)
)
;

CREATE TABLE jetspeed_user_profile (
    COUNTRY varchar(2) NULL,
    LANGUAGE varchar(2) NULL,
    MEDIA_TYPE varchar(99) NULL,
    PAGE varchar(99) NULL,
    PROFILE bytea NULL,
    PSML_ID INTEGER NOT NULL,
    USER_NAME varchar(32) NULL,
    PRIMARY KEY (PSML_ID)
)
;

CREATE TABLE jetspeed_role_profile (
    COUNTRY varchar(2) NULL,
    LANGUAGE varchar(2) NULL,
    MEDIA_TYPE varchar(99) NULL,
    PAGE varchar(99) NULL,
    PROFILE bytea NULL,
    PSML_ID INTEGER NOT NULL,
    ROLE_NAME varchar(99) NULL,
    PRIMARY KEY (PSML_ID)
)
;

CREATE TABLE eip_m_config (
    ID INTEGER NOT NULL,
    NAME varchar(64) NULL,
    VALUE varchar(255) NULL,
    PRIMARY KEY (ID)
)
;

CREATE INDEX eip_m_config_name ON eip_m_config (NAME);

CREATE TABLE application (
    APP_ID varchar(255) NOT NULL,
    CONSUMER_KEY varchar(99) NULL,
    CONSUMER_SECRET varchar(99) NULL,
    CREATE_DATE date NULL,
    DESCRIPTION text NULL,
    ICON varchar(255) NULL,
    ICON64 varchar(255) NULL,
    ID INTEGER NOT NULL,
    STATUS INTEGER NULL,
    SUMMARY varchar(255) NULL,
    TITLE varchar(99) NULL,
    UPDATE_DATE timestamp with time zone NULL,
    URL varchar(255) NOT NULL,
    PRIMARY KEY (ID)
)
;

CREATE TABLE activity (
    APP_ID varchar(255) NOT NULL,
    BODY text NULL,
    EXTERNAL_ID varchar(99) NULL,
    ICON varchar(255) NULL,
    ID INTEGER NOT NULL,
    LOGIN_NAME varchar(32) NOT NULL,
    MODULE_ID INTEGER NOT NULL,
    PORTLET_PARAMS varchar(99) NULL,
    PRIORITY float NULL,
    TITLE varchar(255) NOT NULL,
    UPDATE_DATE timestamp with time zone NULL,
    PRIMARY KEY (ID)
)
;

CREATE TABLE oauth_token (
    ACCESS_TOKEN varchar(255) NULL,
    ID INTEGER NOT NULL,
    SESSION_HANDLE varchar(255) NULL,
    TOKEN_EXPIRE_MILIS INTEGER NULL,
    TOKEN_SECRET varchar(255) NULL,
    PRIMARY KEY (ID)
)
;

CREATE TABLE oauth_entry (
    APP_ID varchar(255) NULL,
    AUTHORIZED INTEGER NULL,
    CALLBACK_TOKEN varchar(255) NULL,
    CALLBACK_TOKEN_ATTEMPTS INTEGER NULL,
    CALLBACK_URL varchar(255) NULL,
    CALLBACK_URL_SIGNED INTEGER NULL,
    CONSUMER_KEY varchar(255) NULL,
    CONTAINER varchar(32) NULL,
    DOMAIN varchar(255) NULL,
    ID INTEGER NOT NULL,
    ISSUE_TIME timestamp with time zone NULL,
    OAUTH_VERSION varchar(16) NULL,
    TOKEN varchar(255) NULL,
    TOKEN_SECRET varchar(255) NULL,
    TYPE varchar(32) NULL,
    USER_ID varchar(64) NULL,
    PRIMARY KEY (ID)
)
;



CREATE TABLE oauth_consumer (
    APP_ID INTEGER NULL,
    CONSUMER_KEY varchar(255) NULL,
    CONSUMER_SECRET varchar(255) NULL,
    CREATE_DATE date NULL,
    ID INTEGER NOT NULL,
    NAME varchar(99) NULL,
    TYPE varchar(99) NULL,
    UPDATE_DATE timestamp with time zone NULL,
    PRIMARY KEY (ID)
)
;

CREATE TABLE container_config (
    ID INTEGER NOT NULL,
    NAME varchar(64) NOT NULL,
    VALUE varchar(255) NULL,
    PRIMARY KEY (ID)
)
;

CREATE INDEX container_config_name ON container_config (NAME);

CREATE TABLE activity_map (
    ACTIVITY_ID INTEGER NULL,
    ID INTEGER NOT NULL,
    IS_READ INTEGER NULL,
    LOGIN_NAME varchar(32) NOT NULL,
    PRIMARY KEY (ID)
)
;

CREATE TABLE app_data (
    APP_ID varchar(255) NOT NULL,
    ID INTEGER NOT NULL,
    NAME varchar(99) NOT NULL,
    LOGIN_NAME varchar(32) NOT NULL,
    VALUE text NULL,
    PRIMARY KEY (ID)
)
;

CREATE TABLE module_id (
    ID INTEGER NOT NULL,
    PRIMARY KEY (ID)
)
;

ALTER TABLE oauth_consumer ADD FOREIGN KEY (APP_ID) REFERENCES application (ID) ON DELETE CASCADE;
ALTER TABLE activity_map ADD FOREIGN KEY (ACTIVITY_ID) REFERENCES activity (ID) ON DELETE CASCADE;

CREATE TABLE EIP_M_FACILITY_GROUP
(
    GROUP_ID INTEGER NOT NULL,
    GROUP_NAME VARCHAR (64),
    SORT INTEGER,
    PRIMARY KEY(GROUP_ID)
);

CREATE TABLE EIP_M_FACILITY_GROUP_MAP
(
    ID INTEGER NOT NULL,
    FACILITY_ID INTEGER,
    GROUP_ID INTEGER,
    PRIMARY KEY(ID)
);

CREATE TABLE eip_m_inactive_application (
    ID INTEGER NOT NULL,
    NAME varchar(128) NULL,
    PRIMARY KEY (ID)
)
;

-----------------------------------------------------------------------------
-- EIP_T_REPORT
-----------------------------------------------------------------------------

CREATE TABLE EIP_T_REPORT
(
    REPORT_ID INTEGER NOT NULL,
    PARENT_ID INTEGER NOT NULL,
    USER_ID INTEGER NOT NULL,
    START_DATE TIMESTAMP,
    END_DATE TIMESTAMP,
    REPORT_NAME VARCHAR (64),
    NOTE TEXT,
    CREATE_DATE TIMESTAMP,
    UPDATE_DATE TIMESTAMP,
    PRIMARY KEY(REPORT_ID)
);

-----------------------------------------------------------------------------
-- EIP_T_REPORT_FILE
-----------------------------------------------------------------------------

CREATE TABLE EIP_T_REPORT_FILE
(
    FILE_ID INTEGER NOT NULL,
    OWNER_ID INTEGER,
    REPORT_ID INTEGER,
    FILE_NAME VARCHAR (128) NOT NULL,
    FILE_PATH TEXT NOT NULL,
    FILE_THUMBNAIL bytea,
    CREATE_DATE DATE,
    UPDATE_DATE TIMESTAMP,
    FOREIGN KEY (REPORT_ID) REFERENCES EIP_T_REPORT (REPORT_ID) ON DELETE CASCADE,
    PRIMARY KEY (FILE_ID)
);

-----------------------------------------------------------------------------
-- EIP_T_REPORT_MEMBER_MAP
-----------------------------------------------------------------------------

CREATE TABLE EIP_T_REPORT_MEMBER_MAP
(
   ID INTEGER NOT NULL,
   REPORT_ID INTEGER NOT NULL,
   USER_ID INTEGER NOT NULL,
   FOREIGN KEY (REPORT_ID) REFERENCES EIP_T_REPORT (REPORT_ID) ON DELETE CASCADE,
   PRIMARY KEY(ID)
);

-----------------------------------------------------------------------------
-- EIP_T_REPORT_MAP
-----------------------------------------------------------------------------

CREATE TABLE EIP_T_REPORT_MAP
(
   ID INTEGER NOT NULL,
   REPORT_ID INTEGER NOT NULL,
   USER_ID INTEGER NOT NULL,
   STATUS VARCHAR (1),
   CREATE_DATE DATE,
   UPDATE_DATE TIMESTAMP,
   FOREIGN KEY (REPORT_ID) REFERENCES EIP_T_REPORT (REPORT_ID) ON DELETE CASCADE,
   PRIMARY KEY(ID)
);

-----------------------------------------------------------------------------
-- EIP_T_TIMELINE
-----------------------------------------------------------------------------

CREATE TABLE EIP_T_TIMELINE
(
    TIMELINE_ID INTEGER NOT NULL,
    PARENT_ID INTEGER NOT NULL DEFAULT 0,
    OWNER_ID INTEGER,
    APP_ID VARCHAR(255),
    EXTERNAL_ID varchar(99),
    NOTE TEXT,
    TIMELINE_TYPE VARCHAR (2),
    NUM_ON_DAY INTEGER DEFAULT 0,
    PARAMS VARCHAR (99),
    CREATE_DATE TIMESTAMP DEFAULT now(),
    UPDATE_DATE TIMESTAMP DEFAULT now(),
    FOREIGN KEY (TIMELINE_ID) REFERENCES EIP_T_TIMELINE (TIMELINE_ID) ON DELETE CASCADE,
    PRIMARY KEY(TIMELINE_ID)
);

CREATE INDEX eip_t_timeline_parent_id_index ON EIP_T_TIMELINE (PARENT_ID);

-----------------------------------------------------------------------------
-- EIP_T_TIMELINE_MAP
-----------------------------------------------------------------------------

CREATE TABLE EIP_T_TIMELINE_MAP
(
    ID INTEGER NOT NULL,
    TIMELINE_ID INTEGER NULL,
    IS_READ INTEGER NULL,
    LOGIN_NAME varchar(32) NOT NULL,
    PRIMARY KEY(ID)
);
ALTER TABLE EIP_T_TIMELINE_MAP ADD FOREIGN KEY (TIMELINE_ID) REFERENCES EIP_T_TIMELINE (TIMELINE_ID) ON DELETE CASCADE;

-----------------------------------------------------------------------------
-- EIP_T_TIMELINE_LIKE
-----------------------------------------------------------------------------

CREATE TABLE EIP_T_TIMELINE_LIKE
(
    TIMELINE_LIKE_ID INTEGER NOT NULL,
    TIMELINE_ID INTEGER NOT NULL,
    OWNER_ID INTEGER,
    CREATE_DATE TIMESTAMP DEFAULT now(),
    FOREIGN KEY (TIMELINE_ID) REFERENCES EIP_T_TIMELINE (TIMELINE_ID) ON DELETE CASCADE,
    PRIMARY KEY(TIMELINE_LIKE_ID),
    UNIQUE (TIMELINE_ID, OWNER_ID)
);

-----------------------------------------------------------------------------
-- EIP_T_TIMELINE_FILE
-----------------------------------------------------------------------------

CREATE TABLE EIP_T_TIMELINE_FILE
(
    FILE_ID INTEGER NOT NULL,
    OWNER_ID INTEGER,
    TIMELINE_ID INTEGER,
    FILE_NAME VARCHAR (128) NOT NULL,
    FILE_PATH TEXT NOT NULL,
    FILE_THUMBNAIL bytea,
    CREATE_DATE DATE,
    UPDATE_DATE TIMESTAMP,
    FOREIGN KEY (TIMELINE_ID) REFERENCES EIP_T_TIMELINE (TIMELINE_ID) ON DELETE CASCADE,
    PRIMARY KEY (FILE_ID)
);

-----------------------------------------------------------------------------
-- EIP_T_TIMELINE_URL
-----------------------------------------------------------------------------

CREATE TABLE EIP_T_TIMELINE_URL
(
    URL_ID INTEGER NOT NULL,
    TIMELINE_ID INTEGER,
    THUMBNAIL bytea,
    TITLE VARCHAR (128),
    URL TEXT NOT NULL,
    BODY TEXT,
    FOREIGN KEY (TIMELINE_ID) REFERENCES EIP_T_TIMELINE (TIMELINE_ID) ON DELETE CASCADE,
    PRIMARY KEY (URL_ID)
);

-----------------------------------------------------------------------------
-- EIP_T_ACL_MAP
-----------------------------------------------------------------------------

CREATE TABLE EIP_T_ACL_MAP
(
  ACL_ID INTEGER NOT NULL,
  TARGET_ID INTEGER NOT NULL,
  TARGET_TYPE CHARACTER VARYING(8),
  ID INTEGER NOT NULL,
  TYPE VARCHAR(8),
  FEATURE VARCHAR(64),
  LEVEL INTEGER NOT NULL,
  PRIMARY KEY (ACL_ID)
);


-----------------------------------------------------------------------------
-- EIP_T_GPDB
-----------------------------------------------------------------------------

CREATE TABLE EIP_T_GPDB
(
    GPDB_ID INTEGER NOT NULL,
    GPDB_NAME TEXT NOT NULL,
    MAIL_FLG VARCHAR (1) NOT NULL,
    CREATE_USER_ID INTEGER NOT NULL,
    CREATE_DATE TIMESTAMP,
    UPDATE_DATE TIMESTAMP,
    PRIMARY KEY (GPDB_ID)
);

-----------------------------------------------------------------------------
-- EIP_T_GPDB
-----------------------------------------------------------------------------

CREATE TABLE EIP_T_GPDB_ITEM
(
    GPDB_ITEM_ID INTEGER NOT NULL,
    GPDB_ID INTEGER NOT NULL,
    GPDB_ITEM_NAME TEXT NOT NULL,
    TITLE_FLG VARCHAR (1) NOT NULL,
    REQUIRED_FLG VARCHAR (1) NOT NULL,
    TYPE VARCHAR (2) NOT NULL,
    GPDB_KUBUN_ID INTEGER,
    LIST_FLG VARCHAR (1) NOT NULL,
    DETAIL_FLG VARCHAR (1) NOT NULL,
    SIZE_COL INTEGER,
    SIZE_ROW INTEGER,
    LINE INTEGER,
    ORDER_NO INTEGER NOT NULL,
    DEFAULT_SORT_FLG VARCHAR (1) NOT NULL,
    ASC_DESC VARCHAR (4),
    CREATE_USER_ID INTEGER NOT NULL,
    CREATE_DATE TIMESTAMP,
    UPDATE_DATE TIMESTAMP,
    PRIMARY KEY (GPDB_ITEM_ID)
);

-----------------------------------------------------------------------------
-- EIP_T_GPDB_RECORD
-----------------------------------------------------------------------------

CREATE TABLE EIP_T_GPDB_RECORD
(
    GPDB_RECORD_ID INTEGER NOT NULL,
    GPDB_ID INTEGER NOT NULL,
    GPDB_ITEM_ID INTEGER NOT NULL,
    RECORD_NO INTEGER NOT NULL,
    VALUE TEXT,
    CREATE_USER_ID INTEGER NOT NULL,
    UPDATE_USER_ID INTEGER NOT NULL,
    CREATE_DATE TIMESTAMP,
    UPDATE_DATE TIMESTAMP,
    PRIMARY KEY (GPDB_RECORD_ID)
);

CREATE INDEX eip_t_gpdb_record_record_no_index ON EIP_T_GPDB_RECORD (RECORD_NO);
CREATE INDEX eip_t_gpdb_record_gpdb_id_index ON EIP_T_GPDB_RECORD (GPDB_ID);

-----------------------------------------------------------------------------
-- EIP_T_GPDB_RECORD_FILE
-----------------------------------------------------------------------------

CREATE TABLE EIP_T_GPDB_RECORD_FILE
(
    FILE_ID INTEGER NOT NULL,
    OWNER_ID INTEGER,
    GPDB_RECORD_ID INTEGER,
    FILE_NAME VARCHAR (128) NOT NULL,
    FILE_PATH TEXT NOT NULL,
    FILE_THUMBNAIL bytea,
    CREATE_DATE DATE,
    UPDATE_DATE TIMESTAMP,
    PRIMARY KEY (FILE_ID)
);

-----------------------------------------------------------------------------
-- EIP_M_GPDB_KUBUN
-----------------------------------------------------------------------------

CREATE TABLE EIP_M_GPDB_KUBUN
(
    GPDB_KUBUN_ID INTEGER NOT NULL,
    GPDB_KUBUN_NAME TEXT NOT NULL,
    CREATE_DATE TIMESTAMP,
    UPDATE_DATE TIMESTAMP,
    PRIMARY KEY (GPDB_KUBUN_ID)
);

-----------------------------------------------------------------------------
-- EIP_M_GPDB_KUBUN_VALUE
-----------------------------------------------------------------------------

CREATE TABLE EIP_M_GPDB_KUBUN_VALUE
(
    GPDB_KUBUN_VALUE_ID INTEGER NOT NULL,
    GPDB_KUBUN_ID INTEGER NOT NULL,
    GPDB_KUBUN_VALUE TEXT NOT NULL,
    ORDER_NO INTEGER NOT NULL,
    CREATE_DATE TIMESTAMP,
    UPDATE_DATE TIMESTAMP,
    PRIMARY KEY (GPDB_KUBUN_VALUE_ID)
);

-----------------------------------------------------------------------------
-- EIP_T_WIKI
-----------------------------------------------------------------------------

CREATE TABLE EIP_T_WIKI
(
    WIKI_ID INTEGER NOT NULL,
    WIKI_NAME VARCHAR (64) NOT NULL,
    PARENT_ID INTEGER DEFAULT 0 NOT NULL,
    NOTE TEXT,
    CREATE_USER_ID INTEGER,
    UPDATE_USER_ID INTEGER,
    CREATE_DATE DATE,
    UPDATE_DATE TIMESTAMP,
    PRIMARY KEY(WIKI_ID)
);

CREATE INDEX eip_t_wiki_wiki_name_parent_id_index ON EIP_T_WIKI (WIKI_NAME, PARENT_ID);

-----------------------------------------------------------------------------
-- EIP_T_WIKI_FILE
-----------------------------------------------------------------------------

CREATE TABLE EIP_T_WIKI_FILE
(
    FILE_ID INTEGER NOT NULL,
    OWNER_ID INTEGER,
    WIKI_ID INTEGER,
    FILE_NAME VARCHAR (128) NOT NULL,
    FILE_PATH TEXT NOT NULL,
    FILE_THUMBNAIL bytea,
    CREATE_DATE DATE,
    UPDATE_DATE TIMESTAMP,
    FOREIGN KEY (WIKI_ID) REFERENCES EIP_T_WIKI (WIKI_ID) ON DELETE CASCADE,
    PRIMARY KEY (FILE_ID)
);

CREATE INDEX eip_t_file_wiki_id_index ON EIP_T_WIKI_FILE (WIKI_ID);

-----------------------------------------------------------------------------
-- EIP_T_PROJECT
-----------------------------------------------------------------------------

CREATE TABLE EIP_T_PROJECT (
      PROJECT_ID         INTEGER                        NOT NULL    --プロジェクトID
    , PROJECT_NAME       TEXT                           NOT NULL    --プロジェクト名
    , EXPLANATION        TEXT                                       --説明
    , ADMIN_USER_ID      INTEGER                        NOT NULL    --管理者
    , PROGRESS_FLG       CHARACTER VARYING(1)           NOT NULL    --進捗率入力フラグ 1:進捗率を入力する 0:タスクを元に自動計算する
    , PROGRESS_RATE      INTEGER                                    --進捗率
    , CREATE_USER_ID     INTEGER                        NOT NULL    --登録ユーザーID
    , UPDATE_USER_ID     INTEGER                        NOT NULL    --更新ユーザーID
    , CREATE_DATE        TIMESTAMP                                                          --登録日
    , UPDATE_DATE        TIMESTAMP                                                          --更新日
    , PRIMARY KEY (PROJECT_ID)
);

-----------------------------------------------------------------------------
-- EIP_T_PROJECT_MEMBER
-----------------------------------------------------------------------------

CREATE TABLE EIP_T_PROJECT_MEMBER (
      ID          INTEGER                        NOT NULL    --ID
    , PROJECT_ID  INTEGER                        NOT NULL    --プロジェクトID
    , USER_ID     INTEGER                        NOT NULL    --ユーザーID
    , PRIMARY KEY (ID)
);

CREATE INDEX eip_t_project_member_project_id_user_id_index ON EIP_T_PROJECT_MEMBER (PROJECT_ID, USER_ID);

-----------------------------------------------------------------------------
-- EIP_T_PROJECT_TASK
-----------------------------------------------------------------------------

CREATE TABLE EIP_T_PROJECT_TASK (
      TASK_ID                INTEGER                        NOT NULL    --タスクID
    , PARENT_TASK_ID         INTEGER                                    --親タスクID
    , PROJECT_ID             INTEGER                        NOT NULL    --プロジェクトID
    , TRACKER                TEXT                           NOT NULL    --トラッカー 1:機能 2:バグ 3:サポート
    , TASK_NAME              TEXT                           NOT NULL    --タスク名
    , EXPLANATION            TEXT                                       --説明
    , STATUS                 TEXT                           NOT NULL    --ステータス 1:新規 2:進行中 3:解決 4:フィードバック 5:終了 6:却下
    , PRIORITY               TEXT                           NOT NULL    --優先度 1:高 2:中 3:低
    , START_PLAN_DATE        DATE                                       --開始予定日
    , END_PLAN_DATE          DATE                                       --完了予定日
    , START_DATE             DATE                                       --開始実績日
    , END_DATE               DATE                                       --完了実績日
    , PLAN_WORKLOAD          DECIMAL(8,3)                               --計画工数（時間）
    , PROGRESS_RATE          INTEGER                                    --進捗率
    , ORDER_NO               INTEGER                                    --表示順
    , CREATE_USER_ID         INTEGER                        NOT NULL    --登録ユーザーID
    , UPDATE_USER_ID         INTEGER                        NOT NULL    --更新ユーザーID
    , CREATE_DATE            TIMESTAMP                                                          --登録日
    , UPDATE_DATE            TIMESTAMP                                                          --更新日
    , PRIMARY KEY (TASK_ID)
);

CREATE INDEX eip_t_project_task_project_id_index ON EIP_T_PROJECT_TASK (PROJECT_ID);
CREATE INDEX eip_t_project_task_parent_task_id_index ON EIP_T_PROJECT_TASK (PARENT_TASK_ID);

-----------------------------------------------------------------------------
-- EIP_T_PROJECT_TASK_MEMBER
-----------------------------------------------------------------------------

CREATE TABLE EIP_T_PROJECT_TASK_MEMBER (
      ID              INTEGER                NOT NULL    --ID
    , TASK_ID         INTEGER                NOT NULL    --タスクID
    , USER_ID         INTEGER                NOT NULL    --ユーザーID
    , WORKLOAD        DECIMAL(8,3)           NOT NULL    --工数
    , PRIMARY KEY (ID)
);

CREATE INDEX eip_t_project_task_member_task_id_user_id_index ON EIP_T_PROJECT_TASK_MEMBER (TASK_ID, USER_ID);

-----------------------------------------------------------------------------
-- EIP_T_PROJECT_TASK_COMMENT
-----------------------------------------------------------------------------

CREATE TABLE EIP_T_PROJECT_TASK_COMMENT (
      COMMENT_ID            INTEGER                        NOT NULL    --コメントID
    , TASK_ID               INTEGER                        NOT NULL    --タスクID
    , COMMENT               TEXT                           NOT NULL    --コメント
    , CREATE_USER_ID        INTEGER                        NOT NULL    --登録ユーザーID
    , CREATE_DATE           TIMESTAMP                                                          --登録日
    , UPDATE_DATE           TIMESTAMP                                                          --更新日
    , PRIMARY KEY (COMMENT_ID)
);

CREATE INDEX eip_t_project_task_comment_task_id_index ON EIP_T_PROJECT_TASK_COMMENT (TASK_ID);

-----------------------------------------------------------------------------
-- EIP_T_PROJECT_FILE
-----------------------------------------------------------------------------

CREATE TABLE EIP_T_PROJECT_FILE (
      FILE_ID           INTEGER                NOT NULL
    , OWNER_ID          INTEGER
    , PROJECT_ID        INTEGER
    , FILE_NAME         VARCHAR (128) NOT NULL
    , FILE_PATH         TEXT                   NOT NULL
    , FILE_THUMBNAIL    BYTEA
    , CREATE_DATE       DATE
    , UPDATE_DATE       TIMESTAMP
    , PRIMARY KEY (FILE_ID)
);

CREATE INDEX eip_t_project_file_project_id_index ON EIP_T_PROJECT_FILE (PROJECT_ID);

-----------------------------------------------------------------------------
-- EIP_T_PROJECT_TASK_FILE
-----------------------------------------------------------------------------

CREATE TABLE EIP_T_PROJECT_TASK_FILE (
      FILE_ID         INTEGER                NOT NULL
    , OWNER_ID        INTEGER
    , TASK_ID         INTEGER
    , FILE_NAME       VARCHAR (128) NOT NULL
    , FILE_PATH       TEXT                   NOT NULL
    , FILE_THUMBNAIL  BYTEA
    , CREATE_DATE     DATE
    , UPDATE_DATE     TIMESTAMP
    , PRIMARY KEY (FILE_ID)
);

CREATE INDEX eip_t_project_task_file_task_id_index ON EIP_T_PROJECT_TASK_FILE (TASK_ID);

-----------------------------------------------------------------------------
-- EIP_T_PROJECT_TASK_COMMENT_FILE
-----------------------------------------------------------------------------

CREATE TABLE EIP_T_PROJECT_TASK_COMMENT_FILE (
      FILE_ID         INTEGER                NOT NULL
    , OWNER_ID        INTEGER
    , COMMENT_ID      INTEGER
    , FILE_NAME       VARCHAR (128) NOT NULL
    , FILE_PATH       TEXT                   NOT NULL
    , FILE_THUMBNAIL  BYTEA
    , CREATE_DATE     DATE
    , UPDATE_DATE     TIMESTAMP
    , PRIMARY KEY (FILE_ID)
);

CREATE INDEX eip_t_project_task_comment_file_comment_id_index ON EIP_T_PROJECT_TASK_COMMENT_FILE (COMMENT_ID);

-----------------------------------------------------------------------------
-- EIP_M_PROJECT_KUBUN
-----------------------------------------------------------------------------

CREATE TABLE EIP_M_PROJECT_KUBUN (
      PROJECT_KUBUN_ID     INTEGER                        NOT NULL    --区分ID
    , PROJECT_KUBUN_CD     TEXT                           NOT NULL    --区分コード
    , PROJECT_KUBUN_NAME   TEXT                           NOT NULL    --区分名
    , CREATE_DATE          TIMESTAMP WITHOUT TIME ZONE                --登録日
    , UPDATE_DATE          TIMESTAMP WITHOUT TIME ZONE                --更新日
    , PRIMARY KEY (PROJECT_KUBUN_ID)
);

-----------------------------------------------------------------------------
-- EIP_M_PROJECT_KUBUN_VALUE
-----------------------------------------------------------------------------

CREATE TABLE EIP_M_PROJECT_KUBUN_VALUE (
      PROJECT_KUBUN_VALUE_ID  INTEGER                        NOT NULL    --区分値ID
    , PROJECT_KUBUN_ID        INTEGER                        NOT NULL    --区分ID
    , PROJECT_KUBUN_VALUE_CD  TEXT                           NOT NULL    --区分値コード
    , PROJECT_KUBUN_VALUE     TEXT                           NOT NULL    --区分値
    , ORDER_NO                INTEGER                        NOT NULL    --表示順
    , CREATE_DATE             TIMESTAMP WITHOUT TIME ZONE                --登録日
    , UPDATE_DATE             TIMESTAMP WITHOUT TIME ZONE                --更新日
    , PRIMARY KEY (PROJECT_KUBUN_VALUE_ID)
);

-----------------------------------------------------------------------------
-- EIP_T_MESSAGE_ROOM
-----------------------------------------------------------------------------

CREATE TABLE EIP_T_MESSAGE_ROOM
(
    ROOM_ID INTEGER NOT NULL,
    NAME VARCHAR (255),
    ROOM_TYPE VARCHAR(1) DEFAULT 'G',
    AUTO_NAME VARCHAR(1) DEFAULT 'F',
    LAST_MESSAGE TEXT,
    LAST_UPDATE_DATE TIMESTAMP DEFAULT NULL,
    CREATE_USER_ID INTEGER NOT NULL,
    PHOTO bytea,
    PHOTO_SMARTPHONE bytea,
    PHOTO_MODIFIED TIMESTAMP,
    HAS_PHOTO VARCHAR (1) DEFAULT 'F',
    CREATE_DATE TIMESTAMP,
    UPDATE_DATE TIMESTAMP,
    PRIMARY KEY (ROOM_ID)
);

-----------------------------------------------------------------------------
-- EIP_T_MESSAGE
-----------------------------------------------------------------------------

CREATE TABLE EIP_T_MESSAGE
(
    MESSAGE_ID INTEGER NOT NULL,
    ROOM_ID INTEGER NOT NULL,
    USER_ID INTEGER NOT NULL,
    MESSAGE TEXT,
    MEMBER_COUNT INTEGER NOT NULL,
    CREATE_DATE TIMESTAMP,
    UPDATE_DATE TIMESTAMP,
    FOREIGN KEY (ROOM_ID) REFERENCES EIP_T_MESSAGE_ROOM (ROOM_ID) ON DELETE CASCADE,
    PRIMARY KEY (MESSAGE_ID)
);


create index eip_t_message_room_id_create_date ON EIP_T_MESSAGE(ROOM_ID,CREATE_DATE);

-----------------------------------------------------------------------------
-- EIP_T_MESSAGE_ROOM_MEMBER
-----------------------------------------------------------------------------

CREATE TABLE EIP_T_MESSAGE_ROOM_MEMBER
(
    ID INTEGER NOT NULL,
    ROOM_ID INTEGER NOT NULL,
    USER_ID INTEGER NOT NULL,
    LOGIN_NAME VARCHAR (32) NOT NULL,
    TARGET_USER_ID INTEGER NOT NULL,
    AUTHORITY VARCHAR (1) DEFAULT 'A',
    DESKTOP_NOTIFICATION VARCHAR (1) DEFAULT 'A',
    MOBILE_NOTIFICATION VARCHAR (1) DEFAULT 'A',
    HISTORY_LAST_MESSAGE_ID INTEGER NOT NULL DEFAULT 0,
    FOREIGN KEY (ROOM_ID) REFERENCES EIP_T_MESSAGE_ROOM (ROOM_ID) ON DELETE CASCADE,
    PRIMARY KEY (ID)
);


create index eip_t_message_room_member_target_user_id ON EIP_T_MESSAGE_ROOM_MEMBER(TARGET_USER_ID);
create index eip_t_message_room_member_user_id_target_user_id ON EIP_T_MESSAGE_ROOM_MEMBER(USER_ID,TARGET_USER_ID);

-----------------------------------------------------------------------------
-- EIP_T_MESSAGE_FILE
-----------------------------------------------------------------------------

CREATE TABLE EIP_T_MESSAGE_FILE
(
    FILE_ID INTEGER NOT NULL,
    OWNER_ID INTEGER,
    MESSAGE_ID INTEGER,
    ROOM_ID INTEGER,
    FILE_NAME VARCHAR (128) NOT NULL,
    FILE_PATH TEXT NOT NULL,
    FILE_THUMBNAIL bytea,
    CREATE_DATE DATE,
    UPDATE_DATE TIMESTAMP,
    FOREIGN KEY (MESSAGE_ID) REFERENCES EIP_T_MESSAGE (MESSAGE_ID) ON DELETE CASCADE,
    PRIMARY KEY (FILE_ID)
);

-----------------------------------------------------------------------------
-- EIP_T_MESSAGE_READ
-----------------------------------------------------------------------------

CREATE TABLE EIP_T_MESSAGE_READ
(
    ID INTEGER NOT NULL,
    MESSAGE_ID INTEGER NOT NULL,
    ROOM_ID INTEGER NOT NULL,
    USER_ID INTEGER NOT NULL,
    IS_READ VARCHAR(1) DEFAULT 'F',
    FOREIGN KEY (MESSAGE_ID) REFERENCES EIP_T_MESSAGE (MESSAGE_ID) ON DELETE CASCADE,
    PRIMARY KEY (ID)
);

create index eip_t_message_read_index1 ON eip_t_message_read(ROOM_ID,USER_ID,IS_READ);
create index eip_t_message_read_index2 ON eip_t_message_read(ROOM_ID,MESSAGE_ID,IS_READ);

-----------------------------------------------------------------------------
-- CREATE SEQUENCE
-----------------------------------------------------------------------------

CREATE SEQUENCE pk_aipo_license INCREMENT 20;
CREATE SEQUENCE pk_eip_facility_group INCREMENT 20;
CREATE SEQUENCE pk_eip_m_address_group INCREMENT 20;
CREATE SEQUENCE pk_eip_m_addressbook INCREMENT 20;
CREATE SEQUENCE pk_eip_m_addressbook_company INCREMENT 20;
CREATE SEQUENCE pk_eip_m_company INCREMENT 20;
CREATE SEQUENCE pk_eip_m_facility INCREMENT 20;
CREATE SEQUENCE pk_eip_m_mail_account INCREMENT 20;
CREATE SEQUENCE pk_eip_m_mail_notify_conf INCREMENT 20;
CREATE SEQUENCE pk_eip_m_position INCREMENT 20;
CREATE SEQUENCE pk_eip_m_post INCREMENT 20;
CREATE SEQUENCE pk_eip_m_user_position INCREMENT 20;
CREATE SEQUENCE pk_eip_t_acl_portlet_feature INCREMENT 20;
CREATE SEQUENCE pk_eip_t_acl_role INCREMENT 20;
CREATE SEQUENCE pk_eip_t_acl_user_role_map INCREMENT 20;
CREATE SEQUENCE pk_eip_t_addressbook_group_map INCREMENT 20;
CREATE SEQUENCE pk_eip_t_blog INCREMENT 20;
CREATE SEQUENCE pk_eip_t_blog_comment INCREMENT 20;
CREATE SEQUENCE pk_eip_t_blog_entry INCREMENT 20;
CREATE SEQUENCE pk_eip_t_blog_file INCREMENT 20;
CREATE SEQUENCE pk_eip_t_blog_footmark_map INCREMENT 20;
CREATE SEQUENCE pk_eip_t_blog_thema INCREMENT 20;
CREATE SEQUENCE pk_eip_t_cabinet_file INCREMENT 20;
CREATE SEQUENCE pk_eip_t_cabinet_folder INCREMENT 20;
CREATE SEQUENCE pk_eip_t_cabinet_folder_map INCREMENT 20;
CREATE SEQUENCE pk_eip_t_common_category INCREMENT 20;
CREATE SEQUENCE pk_eip_t_eventlog INCREMENT 20;
CREATE SEQUENCE pk_eip_t_ext_timecard INCREMENT 20;
CREATE SEQUENCE pk_eip_t_ext_timecard_system INCREMENT 20;
CREATE SEQUENCE pk_eip_t_ext_timecard_system_map INCREMENT 20;
CREATE SEQUENCE pk_eip_t_mail INCREMENT 20;
CREATE SEQUENCE pk_eip_t_mail_filter INCREMENT 20;
CREATE SEQUENCE pk_eip_t_mail_folder INCREMENT 20;
CREATE SEQUENCE pk_eip_t_memo INCREMENT 20;
CREATE SEQUENCE pk_eip_t_msgboard_category INCREMENT 20;
CREATE SEQUENCE pk_eip_t_msgboard_category_map INCREMENT 20;
CREATE SEQUENCE pk_eip_t_msgboard_file INCREMENT 20;
CREATE SEQUENCE pk_eip_t_msgboard_topic INCREMENT 20;
CREATE SEQUENCE pk_eip_t_note INCREMENT 20;
CREATE SEQUENCE pk_eip_t_note_map INCREMENT 20;
CREATE SEQUENCE pk_eip_t_schedule INCREMENT 20;
CREATE SEQUENCE pk_eip_t_schedule_map INCREMENT 20;
CREATE SEQUENCE pk_eip_t_schedule_file INCREMENT 20;
CREATE SEQUENCE pk_eip_t_timecard INCREMENT 20;
CREATE SEQUENCE pk_eip_t_timecard_settings INCREMENT 20;
CREATE SEQUENCE pk_eip_t_todo INCREMENT 20;
CREATE SEQUENCE pk_eip_t_todo_category INCREMENT 20;
CREATE SEQUENCE pk_eip_t_whatsnew INCREMENT 20;
CREATE SEQUENCE pk_eip_t_workflow_category INCREMENT 20;
CREATE SEQUENCE pk_eip_t_workflow_file INCREMENT 20;
CREATE SEQUENCE pk_eip_t_workflow_request INCREMENT 20;
CREATE SEQUENCE pk_eip_t_workflow_request_map INCREMENT 20;
CREATE SEQUENCE pk_eip_t_workflow_route INCREMENT 20;
CREATE SEQUENCE pk_turbine_group INCREMENT 20;
CREATE SEQUENCE pk_turbine_permission INCREMENT 20;
CREATE SEQUENCE pk_turbine_role INCREMENT 20;
CREATE SEQUENCE pk_turbine_user INCREMENT 20;
CREATE SEQUENCE pk_turbine_user_group_role INCREMENT 20;
CREATE SEQUENCE pk_eip_m_facility_group INCREMENT 20;
CREATE SEQUENCE pk_eip_m_facility_group_map INCREMENT 20;
CREATE SEQUENCE pk_eip_m_inactive_application INCREMENT 20 START 200;

CREATE SEQUENCE pk_eip_m_config INCREMENT 20 START 200;
CREATE SEQUENCE pk_jetspeed_group_profile INCREMENT 20 START 200;
CREATE SEQUENCE pk_jetspeed_role_profile INCREMENT 20 START 200;
CREATE SEQUENCE pk_jetspeed_user_profile INCREMENT 20 START 200;
CREATE SEQUENCE pk_activity INCREMENT 20 START 200;
CREATE SEQUENCE pk_activity_map INCREMENT 20 START 200;
CREATE SEQUENCE pk_app_data INCREMENT 20 START 200;
CREATE SEQUENCE pk_application INCREMENT 20 START 200;
CREATE SEQUENCE pk_container_config INCREMENT 20 START 200;
CREATE SEQUENCE pk_module_id INCREMENT 20 START 200;
CREATE SEQUENCE pk_oauth_consumer INCREMENT 20 START 200;
CREATE SEQUENCE pk_oauth_token INCREMENT 20 START 200;
CREATE SEQUENCE pk_oauth_entry INCREMENT 20 START 200;
CREATE SEQUENCE pk_eip_t_report INCREMENT 20;
CREATE SEQUENCE pk_eip_t_report_file INCREMENT 20;
CREATE SEQUENCE pk_eip_t_report_member_map INCREMENT 20;
CREATE SEQUENCE pk_eip_t_report_map INCREMENT 20;
CREATE SEQUENCE pk_eip_t_timeline INCREMENT 20;
CREATE SEQUENCE pk_eip_t_timeline_map INCREMENT 20 START 200;
CREATE SEQUENCE pk_eip_t_timeline_like INCREMENT 20;
CREATE SEQUENCE pk_eip_t_timeline_file INCREMENT 20;
CREATE SEQUENCE pk_eip_t_timeline_url INCREMENT 20;
CREATE SEQUENCE pk_eip_t_acl_map INCREMENT 20;
CREATE SEQUENCE pk_eip_t_gpdb INCREMENT 20;
CREATE SEQUENCE pk_eip_t_gpdb_item INCREMENT 20;
CREATE SEQUENCE pk_eip_t_gpdb_record INCREMENT 20;
CREATE SEQUENCE pk_eip_t_gpdb_record_file INCREMENT 20;
CREATE SEQUENCE pk_eip_m_gpdb_kubun INCREMENT 20;
CREATE SEQUENCE pk_eip_m_gpdb_kubun_value INCREMENT 20;
CREATE SEQUENCE pk_eip_t_wiki INCREMENT 20;
CREATE SEQUENCE pk_eip_t_wiki_file INCREMENT 20;
CREATE SEQUENCE pk_eip_t_project INCREMENT 20;
CREATE SEQUENCE pk_eip_t_project_task INCREMENT 20;
CREATE SEQUENCE pk_eip_t_project_task_comment INCREMENT 20;
CREATE SEQUENCE pk_eip_t_project_member INCREMENT 20;
CREATE SEQUENCE pk_eip_t_project_task_member INCREMENT 20;
CREATE SEQUENCE pk_eip_t_project_file INCREMENT 20;
CREATE SEQUENCE pk_eip_t_project_task_file INCREMENT 20;
CREATE SEQUENCE pk_eip_t_project_task_comment_file INCREMENT 20;
CREATE SEQUENCE pk_eip_m_project_kubun INCREMENT 20;
CREATE SEQUENCE pk_eip_m_project_kubun_value INCREMENT 20;
CREATE SEQUENCE pk_eip_t_message_room INCREMENT 20;
CREATE SEQUENCE pk_eip_t_message INCREMENT 20;
CREATE SEQUENCE pk_eip_t_message_room_member INCREMENT 20;
CREATE SEQUENCE pk_eip_t_message_file INCREMENT 20;
CREATE SEQUENCE pk_eip_t_message_read INCREMENT 20;

-----------------------------------------------------------------------------
-- ALTER SEQUENCE
-----------------------------------------------------------------------------

ALTER SEQUENCE pk_aipo_license OWNED BY AIPO_LICENSE.LICENSE_ID;
ALTER SEQUENCE pk_turbine_permission OWNED BY TURBINE_PERMISSION.PERMISSION_ID;
ALTER SEQUENCE pk_turbine_role OWNED BY TURBINE_ROLE.ROLE_ID;
ALTER SEQUENCE pk_turbine_group OWNED BY TURBINE_GROUP.GROUP_ID;
ALTER SEQUENCE pk_turbine_user OWNED BY TURBINE_USER.USER_ID;
ALTER SEQUENCE pk_turbine_user_group_role OWNED BY TURBINE_USER_GROUP_ROLE.ID;
ALTER SEQUENCE pk_eip_m_company OWNED BY EIP_M_COMPANY.COMPANY_ID;
ALTER SEQUENCE pk_eip_m_post OWNED BY EIP_M_POST.POST_ID;
ALTER SEQUENCE pk_eip_m_position OWNED BY EIP_M_POSITION.POSITION_ID;
ALTER SEQUENCE pk_eip_m_user_position OWNED BY EIP_M_USER_POSITION.ID;
ALTER SEQUENCE pk_eip_t_common_category OWNED BY EIP_T_COMMON_CATEGORY.COMMON_CATEGORY_ID;
ALTER SEQUENCE pk_eip_t_schedule OWNED BY EIP_T_SCHEDULE.SCHEDULE_ID;
ALTER SEQUENCE pk_eip_t_schedule_map OWNED BY EIP_T_SCHEDULE_MAP.ID;
ALTER SEQUENCE pk_eip_t_schedule_file OWNED BY EIP_T_SCHEDULE_FILE.FILE_ID;
ALTER SEQUENCE pk_eip_t_todo_category OWNED BY EIP_T_TODO_CATEGORY.CATEGORY_ID;
ALTER SEQUENCE pk_eip_t_todo OWNED BY EIP_T_TODO.TODO_ID;
ALTER SEQUENCE pk_eip_m_mail_account OWNED BY EIP_M_MAIL_ACCOUNT.ACCOUNT_ID;
ALTER SEQUENCE pk_eip_m_mail_notify_conf OWNED BY EIP_M_MAIL_NOTIFY_CONF.NOTIFY_ID;
ALTER SEQUENCE pk_eip_t_mail OWNED BY EIP_T_MAIL.MAIL_ID;
ALTER SEQUENCE pk_eip_t_mail_folder OWNED BY EIP_T_MAIL_FOLDER.FOLDER_ID;
ALTER SEQUENCE pk_eip_t_mail_filter OWNED BY EIP_T_MAIL_FILTER.FILTER_ID;
ALTER SEQUENCE pk_eip_m_addressbook OWNED BY EIP_M_ADDRESSBOOK.ADDRESS_ID;
ALTER SEQUENCE pk_eip_m_address_group OWNED BY EIP_M_ADDRESS_GROUP.GROUP_ID;
ALTER SEQUENCE pk_eip_m_addressbook_company OWNED BY EIP_M_ADDRESSBOOK_COMPANY.COMPANY_ID;
ALTER SEQUENCE pk_eip_t_addressbook_group_map OWNED BY EIP_T_ADDRESSBOOK_GROUP_MAP.ID;
ALTER SEQUENCE pk_eip_t_note OWNED BY EIP_T_NOTE.NOTE_ID;
ALTER SEQUENCE pk_eip_t_note_map OWNED BY EIP_T_NOTE_MAP.ID;
ALTER SEQUENCE pk_eip_t_msgboard_category OWNED BY EIP_T_MSGBOARD_CATEGORY.CATEGORY_ID;
ALTER SEQUENCE pk_eip_t_msgboard_category_map OWNED BY EIP_T_MSGBOARD_CATEGORY_MAP.ID;
ALTER SEQUENCE pk_eip_t_msgboard_topic OWNED BY EIP_T_MSGBOARD_TOPIC.TOPIC_ID;
ALTER SEQUENCE pk_eip_t_msgboard_file OWNED BY EIP_T_MSGBOARD_FILE.FILE_ID;
ALTER SEQUENCE pk_eip_t_blog OWNED BY EIP_T_BLOG.BLOG_ID;
ALTER SEQUENCE pk_eip_t_blog_thema OWNED BY EIP_T_BLOG_THEMA.THEMA_ID;
ALTER SEQUENCE pk_eip_t_blog_entry OWNED BY EIP_T_BLOG_ENTRY.ENTRY_ID;
ALTER SEQUENCE pk_eip_t_blog_file OWNED BY EIP_T_BLOG_FILE.FILE_ID;
ALTER SEQUENCE pk_eip_t_blog_comment OWNED BY EIP_T_BLOG_COMMENT.COMMENT_ID;
ALTER SEQUENCE pk_eip_t_blog_footmark_map OWNED BY EIP_T_BLOG_FOOTMARK_MAP.ID;
ALTER SEQUENCE pk_eip_t_cabinet_folder OWNED BY EIP_T_CABINET_FOLDER.FOLDER_ID;
ALTER SEQUENCE pk_eip_t_cabinet_folder_map OWNED BY EIP_T_CABINET_FOLDER_MAP.ID;
ALTER SEQUENCE pk_eip_t_cabinet_file OWNED BY EIP_T_CABINET_FILE.FILE_ID;
ALTER SEQUENCE pk_eip_m_facility OWNED BY EIP_M_FACILITY.FACILITY_ID;
ALTER SEQUENCE pk_eip_facility_group OWNED BY EIP_FACILITY_GROUP.ID;
ALTER SEQUENCE pk_eip_t_timecard OWNED BY EIP_T_TIMECARD.TIMECARD_ID;
ALTER SEQUENCE pk_eip_t_timecard_settings OWNED BY EIP_T_TIMECARD_SETTINGS.TIMECARD_SETTINGS_ID;
ALTER SEQUENCE pk_eip_t_ext_timecard OWNED BY EIP_T_EXT_TIMECARD.TIMECARD_ID;
ALTER SEQUENCE pk_eip_t_ext_timecard_system OWNED BY EIP_T_EXT_TIMECARD_SYSTEM.SYSTEM_ID;
ALTER SEQUENCE pk_eip_t_ext_timecard_system_map OWNED BY EIP_T_EXT_TIMECARD_SYSTEM_MAP.SYSTEM_MAP_ID;
ALTER SEQUENCE pk_eip_t_workflow_route OWNED BY EIP_T_WORKFLOW_ROUTE.ROUTE_ID;
ALTER SEQUENCE pk_eip_t_workflow_category OWNED BY EIP_T_WORKFLOW_CATEGORY.CATEGORY_ID;
ALTER SEQUENCE pk_eip_t_workflow_request OWNED BY EIP_T_WORKFLOW_REQUEST.REQUEST_ID;
ALTER SEQUENCE pk_eip_t_workflow_file OWNED BY EIP_T_WORKFLOW_FILE.FILE_ID;
ALTER SEQUENCE pk_eip_t_workflow_request_map OWNED BY EIP_T_WORKFLOW_REQUEST_MAP.ID;
ALTER SEQUENCE pk_eip_t_memo OWNED BY EIP_T_MEMO.MEMO_ID;
ALTER SEQUENCE pk_eip_t_whatsnew OWNED BY EIP_T_WHATSNEW.WHATSNEW_ID;
ALTER SEQUENCE pk_eip_t_whatsnew OWNED BY EIP_T_EVENTLOG.EVENTLOG_ID;
ALTER SEQUENCE pk_eip_t_acl_role OWNED BY EIP_T_ACL_ROLE.ROLE_ID;
ALTER SEQUENCE pk_eip_t_acl_portlet_feature OWNED BY EIP_T_ACL_PORTLET_FEATURE.FEATURE_ID;
ALTER SEQUENCE pk_eip_t_acl_user_role_map OWNED BY EIP_T_ACL_USER_ROLE_MAP.ID;
ALTER SEQUENCE pk_eip_m_facility_group OWNED BY EIP_M_FACILITY_GROUP.GROUP_ID;
ALTER SEQUENCE pk_eip_m_facility_group_map OWNED BY EIP_M_FACILITY_GROUP_MAP.ID;
ALTER SEQUENCE pk_eip_t_report OWNED BY EIP_T_REPORT.REPORT_ID;
ALTER SEQUENCE pk_eip_t_report_file OWNED BY EIP_T_REPORT_FILE.FILE_ID;
ALTER SEQUENCE pk_eip_t_report_member_map OWNED BY EIP_T_REPORT_MEMBER_MAP.ID;
ALTER SEQUENCE pk_eip_t_report_map OWNED BY EIP_T_REPORT_MAP.ID;
ALTER SEQUENCE pk_eip_t_timeline OWNED BY EIP_T_TIMELINE.TIMELINE_ID;
ALTER SEQUENCE pk_eip_t_timeline_like OWNED BY EIP_T_TIMELINE_LIKE.TIMELINE_LIKE_ID;
ALTER SEQUENCE pk_eip_t_timeline_file OWNED BY EIP_T_TIMELINE_FILE.FILE_ID;
ALTER SEQUENCE pk_eip_t_timeline_url OWNED BY EIP_T_TIMELINE_URL.URL_ID;
ALTER SEQUENCE pk_eip_t_acl_map OWNED BY EIP_T_ACL_MAP.ACL_ID;
ALTER SEQUENCE pk_eip_t_gpdb OWNED BY EIP_T_GPDB.GPDB_ID;
ALTER SEQUENCE pk_eip_t_gpdb_item OWNED BY EIP_T_GPDB_ITEM.GPDB_ITEM_ID;
ALTER SEQUENCE pk_eip_t_gpdb_record OWNED BY EIP_T_GPDB_RECORD.GPDB_RECORD_ID;
ALTER SEQUENCE pk_eip_t_gpdb_record_file OWNED BY EIP_T_GPDB_RECORD_FILE.FILE_ID;
ALTER SEQUENCE pk_eip_m_gpdb_kubun OWNED BY EIP_M_GPDB_KUBUN.GPDB_KUBUN_ID;
ALTER SEQUENCE pk_eip_m_gpdb_kubun_value OWNED BY EIP_M_GPDB_KUBUN_VALUE.GPDB_KUBUN_VALUE_ID;
ALTER SEQUENCE pk_eip_t_wiki OWNED BY EIP_T_WIKI.WIKI_ID;
ALTER SEQUENCE pk_eip_t_wiki_file OWNED BY EIP_T_WIKI_FILE.FILE_ID;
ALTER SEQUENCE pk_eip_t_project OWNED BY EIP_T_PROJECT.PROJECT_ID;
ALTER SEQUENCE pk_eip_t_project_task OWNED BY EIP_T_PROJECT_TASK.TASK_ID;
ALTER SEQUENCE pk_eip_t_project_task_comment OWNED BY EIP_T_PROJECT_TASK_COMMENT.TASK_ID;
ALTER SEQUENCE pk_eip_t_project_member OWNED BY EIP_T_PROJECT_TASK_MEMBER.ID;
ALTER SEQUENCE pk_eip_t_project_task_member OWNED BY EIP_T_PROJECT_TASK_MEMBER.ID;
ALTER SEQUENCE pk_eip_t_project_file OWNED BY EIP_T_PROJECT_FILE.FILE_ID;
ALTER SEQUENCE pk_eip_t_project_task_file OWNED BY EIP_T_PROJECT_TASK_FILE.FILE_ID;
ALTER SEQUENCE pk_eip_t_project_task_comment_file OWNED BY EIP_T_PROJECT_TASK_COMMENT_FILE.FILE_ID;
ALTER SEQUENCE pk_eip_m_project_kubun OWNED BY EIP_M_PROJECT_KUBUN.PROJECT_KUBUN_ID;
ALTER SEQUENCE pk_eip_m_project_kubun_value OWNED BY EIP_M_PROJECT_KUBUN_VALUE.PROJECT_KUBUN_VALUE_ID;
ALTER SEQUENCE pk_eip_t_message_room OWNED BY EIP_T_MESSAGE_ROOM.ROOM_ID;
ALTER SEQUENCE pk_eip_t_message OWNED BY EIP_T_MESSAGE.MESSAGE_ID;
ALTER SEQUENCE pk_eip_t_message_room_member OWNED BY EIP_T_MESSAGE_ROOM_MEMBER.ID;
ALTER SEQUENCE pk_eip_t_message_file OWNED BY EIP_T_MESSAGE_FILE.FILE_ID;
ALTER SEQUENCE pk_eip_t_message_read OWNED BY EIP_T_MESSAGE_READ.ID;

-----------------------------------------------------------------------------
-----------------------------------------------------------------------------
-- Default Data Insert
-----------------------------------------------------------------------------
-----------------------------------------------------------------------------
INSERT INTO TURBINE_PERMISSION VALUES(1,'view',NULL);
INSERT INTO TURBINE_PERMISSION VALUES(2,'customize',NULL);
INSERT INTO TURBINE_PERMISSION VALUES(3,'maximize',NULL);
INSERT INTO TURBINE_PERMISSION VALUES(4,'minimize',NULL);
INSERT INTO TURBINE_PERMISSION VALUES(5,'personalize',NULL);
INSERT INTO TURBINE_PERMISSION VALUES(6,'info',NULL);
INSERT INTO TURBINE_PERMISSION VALUES(7,'close',NULL);
INSERT INTO TURBINE_PERMISSION VALUES(8,'detach',NULL);
SELECT setval('pk_turbine_permission',8);

INSERT INTO TURBINE_ROLE VALUES(1,'user',NULL);
INSERT INTO TURBINE_ROLE VALUES(2,'admin',NULL);
INSERT INTO TURBINE_ROLE VALUES(3,'guest',NULL);
SELECT setval('pk_turbine_role',3);

INSERT INTO TURBINE_GROUP VALUES(1,'Jetspeed',NULL,NULL,NULL,NULL);
INSERT INTO TURBINE_GROUP VALUES(2,'LoginUser',NULL,NULL,NULL,NULL);
INSERT INTO TURBINE_GROUP VALUES(3,'Facility',NULL,NULL,NULL,NULL);
SELECT setval('pk_turbine_group',3);

INSERT INTO TURBINE_USER VALUES(1,'admin','0DPiKuNIrrVmD8IUCuw1hQxNqZc=',' ','Admin','','CONFIRMED',now(),now(),now(),'F',NULL,now(),0,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'F',now(),NULL,'F',now(),'T',0,NULL,NULL,NULL);
INSERT INTO TURBINE_USER VALUES(2,'template','MibsvmUCE6Sc0DrmcUB1Dk80AIM=','Aipo','Template','','CONFIRMED',now(),now(),now(),'T',NULL, now(),NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'F',now(),NULL,'F',now(),NULL,0,NULL,NULL,NULL);
INSERT INTO TURBINE_USER VALUES(3,'anon','YVGPsXFatNaYrKMqeECsey5QfT4=','Anonymous','User','','CONFIRMED',now(),now(),now(),'F',NULL, now(),NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'F',now(),NULL,'F',now(),NULL,0,NULL,NULL,NULL);
SELECT setval('pk_turbine_user',3);

INSERT INTO TURBINE_ROLE_PERMISSION VALUES(1,1);
INSERT INTO TURBINE_ROLE_PERMISSION VALUES(1,2);
INSERT INTO TURBINE_ROLE_PERMISSION VALUES(1,3);
INSERT INTO TURBINE_ROLE_PERMISSION VALUES(1,4);
INSERT INTO TURBINE_ROLE_PERMISSION VALUES(1,5);
INSERT INTO TURBINE_ROLE_PERMISSION VALUES(1,6);
INSERT INTO TURBINE_ROLE_PERMISSION VALUES(2,1);
INSERT INTO TURBINE_ROLE_PERMISSION VALUES(2,2);
INSERT INTO TURBINE_ROLE_PERMISSION VALUES(2,3);
INSERT INTO TURBINE_ROLE_PERMISSION VALUES(2,4);
INSERT INTO TURBINE_ROLE_PERMISSION VALUES(2,5);
INSERT INTO TURBINE_ROLE_PERMISSION VALUES(2,6);
INSERT INTO TURBINE_ROLE_PERMISSION VALUES(2,7);
INSERT INTO TURBINE_ROLE_PERMISSION VALUES(3,1);
INSERT INTO TURBINE_ROLE_PERMISSION VALUES(3,6);

INSERT INTO TURBINE_USER_GROUP_ROLE VALUES(1,2,1,1);
INSERT INTO TURBINE_USER_GROUP_ROLE VALUES(2,1,1,1);
INSERT INTO TURBINE_USER_GROUP_ROLE VALUES(3,1,1,2);
INSERT INTO TURBINE_USER_GROUP_ROLE VALUES(4,3,1,3);
SELECT setval('pk_turbine_user_group_role',4);

INSERT INTO EIP_T_COMMON_CATEGORY VALUES(1,'未分類','',0,0,NULL,NULL);
SELECT setval('pk_eip_t_common_category',1);

INSERT INTO EIP_T_TODO_CATEGORY VALUES(1,0,0,'未分類','',NULL ,NULL);
SELECT setval('pk_eip_t_todo_category',1);

INSERT INTO EIP_M_COMPANY VALUES (1, '', '', '', '', '', '', '', '', 80, '', 80, now(), now());
SELECT setval('pk_eip_m_company',1);

INSERT INTO EIP_T_MSGBOARD_CATEGORY VALUES(1,0,'その他','','T',NULL,NULL);
SELECT setval('pk_eip_t_msgboard_category',1);

INSERT INTO EIP_T_MSGBOARD_CATEGORY_MAP VALUES(1,1,0,'A');
SELECT setval('pk_eip_t_msgboard_category_map',1);

INSERT INTO EIP_M_ADDRESSBOOK_COMPANY VALUES ('1', '','','','','','','','',1,1,NULL,NULL);
SELECT setval('pk_eip_m_addressbook_company',1);

INSERT INTO EIP_M_MAIL_NOTIFY_CONF VALUES(1,1,1,3,'07:00',now(),now());
INSERT INTO EIP_M_MAIL_NOTIFY_CONF VALUES(2,1,21,3,NULL,now(),now());
INSERT INTO EIP_M_MAIL_NOTIFY_CONF VALUES(3,1,22,3,NULL,now(),now());
INSERT INTO EIP_M_MAIL_NOTIFY_CONF VALUES(4,1,23,3,NULL,now(),now());
INSERT INTO EIP_M_MAIL_NOTIFY_CONF VALUES(5,1,24,3,NULL,now(),now());
INSERT INTO EIP_M_MAIL_NOTIFY_CONF VALUES(6,1,25,3,NULL,now(),now());
INSERT INTO EIP_M_MAIL_NOTIFY_CONF VALUES(7,1,26,3,NULL,now(),now());
INSERT INTO EIP_M_MAIL_NOTIFY_CONF VALUES(8,1,27,3,NULL,now(),now());
SELECT setval('pk_eip_m_mail_notify_conf',7);

INSERT INTO EIP_T_TIMECARD_SETTINGS VALUES(1,1,9,0,18,0,360,60,360,60);
SELECT setval('pk_eip_t_timecard_settings',1);

INSERT INTO EIP_T_ACL_PORTLET_FEATURE VALUES(111,'schedule_self','スケジュール（自分の予定）操作',31);
INSERT INTO EIP_T_ACL_PORTLET_FEATURE VALUES(112,'schedule_other','スケジュール（他ユーザーの予定）操作',31);
INSERT INTO EIP_T_ACL_PORTLET_FEATURE VALUES(113,'schedule_facility','スケジュール（設備の予約）操作',12);
INSERT INTO EIP_T_ACL_PORTLET_FEATURE VALUES(121,'blog_entry_self','ブログ（自分の記事）操作',31);
INSERT INTO EIP_T_ACL_PORTLET_FEATURE VALUES(122,'blog_entry_other','ブログ（他ユーザーの記事）操作',27);
INSERT INTO EIP_T_ACL_PORTLET_FEATURE VALUES(123,'blog_entry_reply','ブログ（記事へのコメント）操作',20);
INSERT INTO EIP_T_ACL_PORTLET_FEATURE VALUES(124,'blog_theme','ブログ（テーマ）操作',31);
INSERT INTO EIP_T_ACL_PORTLET_FEATURE VALUES(125,'blog_entry_other_reply','ブログ（他ユーザーの記事へのコメント）操作',16);
INSERT INTO EIP_T_ACL_PORTLET_FEATURE VALUES(131,'msgboard_topic','掲示板（トピック）操作',31);
INSERT INTO EIP_T_ACL_PORTLET_FEATURE VALUES(132,'msgboard_topic_reply','掲示板（トピック返信）操作',20);
INSERT INTO EIP_T_ACL_PORTLET_FEATURE VALUES(133,'msgboard_category','掲示板（自分のカテゴリ）操作',31);
INSERT INTO EIP_T_ACL_PORTLET_FEATURE VALUES(134,'msgboard_category_other','掲示板（他ユーザーのカテゴリ）操作',27);
INSERT INTO EIP_T_ACL_PORTLET_FEATURE VALUES(135,'msgboard_topic_other','掲示板（他ユーザーのトピック）操作',24);
INSERT INTO EIP_T_ACL_PORTLET_FEATURE VALUES(141,'todo_todo_self','ToDo（自分のToDo）操作',31);
INSERT INTO EIP_T_ACL_PORTLET_FEATURE VALUES(142,'todo_todo_other','ToDo（他ユーザーのToDo）操作',31);
INSERT INTO EIP_T_ACL_PORTLET_FEATURE VALUES(143,'todo_category_self','ToDo（カテゴリ）操作',31);
INSERT INTO EIP_T_ACL_PORTLET_FEATURE VALUES(144,'todo_category_other','ToDo（他ユーザのカテゴリ）操作',31);
INSERT INTO EIP_T_ACL_PORTLET_FEATURE VALUES(151,'workflow_request_self','ワークフロー（自分の依頼）操作',31);
INSERT INTO EIP_T_ACL_PORTLET_FEATURE VALUES(152,'workflow_request_other','ワークフロー（他ユーザーの依頼）操作',19);
INSERT INTO EIP_T_ACL_PORTLET_FEATURE VALUES(161,'addressbook_address_inside','ユーザー名簿操作',3);
INSERT INTO EIP_T_ACL_PORTLET_FEATURE VALUES(162,'addressbook_address_outside','アドレス帳（社外アドレス）操作',31);
INSERT INTO EIP_T_ACL_PORTLET_FEATURE VALUES(163,'addressbook_company','アドレス帳（会社情報）操作',31);
INSERT INTO EIP_T_ACL_PORTLET_FEATURE VALUES(164,'addressbook_company_group','アドレス帳（社外グループ）操作',31);
INSERT INTO EIP_T_ACL_PORTLET_FEATURE VALUES(171,'timecard_timecard_self','タイムカード（自分のタイムカード）操作',47);
INSERT INTO EIP_T_ACL_PORTLET_FEATURE VALUES(172,'timecard_timecard_other','タイムカード（他人のタイムカード）操作',45);
INSERT INTO EIP_T_ACL_PORTLET_FEATURE VALUES(181,'cabinet_file','共有フォルダ（ファイル）操作',31);
INSERT INTO EIP_T_ACL_PORTLET_FEATURE VALUES(182,'cabinet_folder','共有フォルダ（フォルダ）操作',30);
-- INSERT INTO EIP_T_ACL_PORTLET_FEATURE VALUES(191,'manhour_summary_self','プロジェクト管理（自分の工数）操作',1);
-- INSERT INTO EIP_T_ACL_PORTLET_FEATURE VALUES(192,'manhour_summary_other','プロジェクト管理（他ユーザーの工数）操作',1);
-- INSERT INTO EIP_T_ACL_PORTLET_FEATURE VALUES(193,'manhour_common_category','プロジェクト管理（自分の共有カテゴリ）操作',31);
-- INSERT INTO EIP_T_ACL_PORTLET_FEATURE VALUES(194,'manhour_common_category_other','プロジェクト管理（他ユーザーの共有カテゴリ）操作',27);
INSERT INTO EIP_T_ACL_PORTLET_FEATURE VALUES(201,'portlet_customize','アプリ配置',29);
INSERT INTO EIP_T_ACL_PORTLET_FEATURE VALUES(211,'report_self','報告書（自分の報告書）操作',31);
INSERT INTO EIP_T_ACL_PORTLET_FEATURE VALUES(212,'report_other','報告書（他ユーザーの報告書）操作',27);
INSERT INTO EIP_T_ACL_PORTLET_FEATURE VALUES(213,'report_reply','報告書（報告書への返信）操作',20);
INSERT INTO EIP_T_ACL_PORTLET_FEATURE VALUES(221,'timeline_post','タイムライン（自分の投稿）操作',21);
INSERT INTO EIP_T_ACL_PORTLET_FEATURE VALUES(222,'timeline_post_other','タイムライン（他ユーザーの投稿）操作',17);
INSERT INTO EIP_T_ACL_PORTLET_FEATURE VALUES(223,'timeline_comment','タイムライン（コメント）操作',20);
INSERT INTO EIP_T_ACL_PORTLET_FEATURE VALUES(231,'attachment','添付ファイル操作',52);


SELECT setval('pk_eip_t_acl_portlet_feature',300);

-- schedule
INSERT INTO EIP_T_ACL_ROLE VALUES(1,'スケジュール（自分の予定）管理者',111,31,'＊追加、編集、削除は一覧表示と詳細表示の権限を持っていないと使用できません');
INSERT INTO EIP_T_ACL_ROLE VALUES(2,'スケジュール（他ユーザーの予定）',112,3,NULL);
INSERT INTO EIP_T_ACL_ROLE VALUES(3,'スケジュール（設備の予約）管理者',113,12,NULL);

-- blog
INSERT INTO EIP_T_ACL_ROLE VALUES(4,'ブログ（自分の記事）管理者',121,31,'＊追加、編集、削除は一覧表示と詳細表示の権限を持っていないと使用できません');
INSERT INTO EIP_T_ACL_ROLE VALUES(5,'ブログ（他ユーザーの記事）管理者',122,3,'＊詳細表示、編集、削除は一覧表示の権限を持っていないと使用できません');
INSERT INTO EIP_T_ACL_ROLE VALUES(6,'ブログ（記事へのコメント）管理者',123,20,NULL);
INSERT INTO EIP_T_ACL_ROLE VALUES(7,'ブログ（テーマ）管理者',124,31,'＊詳細表示、追加、編集、削除は一覧表示の権限を持っていないと使用できません');

-- msgboard
INSERT INTO EIP_T_ACL_ROLE VALUES(8,'掲示板（トピック）管理者',131,31,'＊詳細表示、追加、削除は一覧表示の権限を持っていないと使用できません');
INSERT INTO EIP_T_ACL_ROLE VALUES(9,'掲示板（トピック返信）管理者',132,20,NULL);
INSERT INTO EIP_T_ACL_ROLE VALUES(10,'掲示板（自分のカテゴリ）管理者',133,31,'＊追加、編集、削除は一覧表示と詳細表示の権限を持っていないと使用できません');

-- todo
INSERT INTO EIP_T_ACL_ROLE VALUES(12,'ToDo（自分のToDo）管理者',141,31,'＊詳細表示、追加、削除は一覧表示の権限を持っていないと使用できません');
INSERT INTO EIP_T_ACL_ROLE VALUES(13,'ToDo（他ユーザーのToDo）管理者',142,31,'＊詳細表示、追加、削除は一覧表示の権限を持っていないと使用できません');
INSERT INTO EIP_T_ACL_ROLE VALUES(14,'ToDo（カテゴリ）管理者',143,31,'＊詳細表示、追加、削除は一覧表示の権限を持っていないと使用できません');
INSERT INTO EIP_T_ACL_ROLE VALUES(30,'ToDo（他ユーザのカテゴリ）管理者',144,31,'＊詳細表示、追加、削除は一覧表示の権限を持っていないと使用できません');

-- workflow
INSERT INTO EIP_T_ACL_ROLE VALUES(15,'ワークフロー（自分の依頼）管理者',151,31,'＊詳細表示、追加、削除は一覧表示の権限を持っていないと使用できません ＊承認、再申請や差し戻しは編集の権限が必要です');
INSERT INTO EIP_T_ACL_ROLE VALUES(16,'ワークフロー（他ユーザーの依頼）管理者',152,19,'＊詳細表示は一覧表示の権限を持っていないと使用できません');

-- addressbook
INSERT INTO EIP_T_ACL_ROLE VALUES(17,'ユーザー名簿管理者',161,3,'＊詳細表示は一覧表示の権限を持っていないと使用できません');
INSERT INTO EIP_T_ACL_ROLE VALUES(18,'アドレス帳（社外アドレス）管理者',162,31,'＊詳細表示、追加、編集、削除は一覧表示の権限を持っていないと使用できません');
INSERT INTO EIP_T_ACL_ROLE VALUES(19,'アドレス帳（会社情報）管理者',163,31,'＊詳細表示、追加、編集、削除は一覧表示の権限を持っていないと使用できません');
INSERT INTO EIP_T_ACL_ROLE VALUES(20,'アドレス帳（社外グループ）管理者',164,31,'＊詳細表示、追加、編集、削除は一覧表示の権限を持っていないと使用できません');

-- timecard
INSERT INTO EIP_T_ACL_ROLE VALUES(21,'タイムカード（自分のタイムカード）管理者',171,47,'＊追加、編集、外部出力は一覧表示の権限を持っていないと使用できません');
INSERT INTO EIP_T_ACL_ROLE VALUES(22,'タイムカード（他人のタイムカード）管理者',172,33,'＊自分のタイムカード一覧表示の権限を持っていないと使用できません ＊外部出力は一覧表示の権限を持っていないと使用できません');

-- cabinet
INSERT INTO EIP_T_ACL_ROLE VALUES(23,'共有フォルダ（ファイル）管理者',181,31,'＊詳細表示、追加、編集、削除は一覧表示の権限を持っていないと使用できません');
INSERT INTO EIP_T_ACL_ROLE VALUES(24,'共有フォルダ（フォルダ）管理者',182,30,'＊編集、削除は詳細表示の権限を持っていないと使用できません');

-- manhour
-- INSERT INTO EIP_T_ACL_ROLE VALUES(25,'プロジェクト管理（自分の工数）管理者',191,1,NULL);
-- INSERT INTO EIP_T_ACL_ROLE VALUES(26,'プロジェクト管理（他ユーザーの工数）管理者',192,1,NULL);
-- INSERT INTO EIP_T_ACL_ROLE VALUES(27,'プロジェクト管理（自分の共有カテゴリ）管理者',193,31,NULL);
-- INSERT INTO EIP_T_ACL_ROLE VALUES(28,'プロジェクト管理（他ユーザーの共有カテゴリ）管理者',194,3,NULL);

--portlet
INSERT INTO EIP_T_ACL_ROLE VALUES(29,'アプリ配置管理者',201,29,NULL);

--report
INSERT INTO EIP_T_ACL_ROLE VALUES(31, '報告書（自分の報告書）管理者',211,31,'＊追加、編集、削除は一覧表示と詳細表示の権限を持っていないと使用できません');
INSERT INTO EIP_T_ACL_ROLE VALUES(32,'報告書（他ユーザーの報告書）管理者',212,27,'＊詳細表示は一覧表示の権限を持っていないと使用できません');
INSERT INTO EIP_T_ACL_ROLE VALUES(33,'報告書（報告書への返信）管理者',213,20,NULL);

-- timeline
INSERT INTO EIP_T_ACL_ROLE VALUES(34, 'タイムライン（自分の投稿）管理者',221,21,'＊追加、削除は一覧表示の権限を持っていないと使用できません');
INSERT INTO EIP_T_ACL_ROLE VALUES(35,'タイムライン（他ユーザーの投稿）管理者',222,1,NULL);
INSERT INTO EIP_T_ACL_ROLE VALUES(36,'タイムライン（コメント）管理者',223,20,NULL);
-- attachment
INSERT INTO EIP_T_ACL_ROLE VALUES(37,'添付ファイル操作管理者',231,52,NULL);

SELECT setval('pk_eip_t_acl_role',10000);

INSERT INTO EIP_T_BLOG_THEMA VALUES(1,'未分類','',0,0,NULL ,NULL);
SELECT setval('pk_eip_t_blog_thema',1);

INSERT INTO EIP_T_CABINET_FOLDER VALUES(1,0,'ルートフォルダ','',0,0,0,NULL,NULL);
SELECT setval('pk_eip_t_cabinet_folder',1);

INSERT INTO EIP_T_CABINET_FOLDER_MAP VALUES(1,1,0,null);
SELECT setval('pk_eip_t_cabinet_folder_map',1);

INSERT INTO EIP_T_WORKFLOW_CATEGORY VALUES(1,0,'未分類','',NULL,NULL);
INSERT INTO EIP_T_WORKFLOW_CATEGORY VALUES(2,0,'有給休暇届','',NULL,NULL);
INSERT INTO EIP_T_WORKFLOW_CATEGORY VALUES(3,0,'稟議書','',NULL,NULL);
INSERT INTO EIP_T_WORKFLOW_CATEGORY VALUES(4,0,'結婚休暇届','',NULL,NULL);
INSERT INTO EIP_T_WORKFLOW_CATEGORY VALUES(5,0,'産前産後休暇届','',NULL,NULL);
INSERT INTO EIP_T_WORKFLOW_CATEGORY VALUES(6,0,'育児休暇届','',NULL,NULL);
INSERT INTO EIP_T_WORKFLOW_CATEGORY VALUES(7,0,'育児時間届','',NULL,NULL);
INSERT INTO EIP_T_WORKFLOW_CATEGORY VALUES(8,0,'特別有給休暇届（業務上負傷等）','',NULL,NULL);
INSERT INTO EIP_T_WORKFLOW_CATEGORY VALUES(9,0,'忌引き休暇届','',NULL,NULL);
SELECT setval('pk_eip_t_workflow_category',9);

INSERT INTO EIP_T_EXT_TIMECARD_SYSTEM VALUES(1, 0, '通常', 9, 0, 18, 0, 1, 360, 60, 360, 60, 4, 'T', 'L480', 'A', now(), now());
SELECT setval('pk_eip_t_ext_timecard_system',1);

INSERT INTO EIP_M_GPDB_KUBUN VALUES (1, '都道府県', now(), now());
SELECT setval('pk_eip_m_gpdb_kubun',1);

INSERT INTO EIP_M_GPDB_KUBUN_VALUE VALUES (1, 1, '北海道', 1, now(), now());
INSERT INTO EIP_M_GPDB_KUBUN_VALUE VALUES (2, 1, '青森県', 2, now(), now());
INSERT INTO EIP_M_GPDB_KUBUN_VALUE VALUES (3, 1, '岩手県', 3, now(), now());
INSERT INTO EIP_M_GPDB_KUBUN_VALUE VALUES (4, 1, '宮城県', 4, now(), now());
INSERT INTO EIP_M_GPDB_KUBUN_VALUE VALUES (5, 1, '秋田県', 5, now(), now());
INSERT INTO EIP_M_GPDB_KUBUN_VALUE VALUES (6, 1, '山形県', 6, now(), now());
INSERT INTO EIP_M_GPDB_KUBUN_VALUE VALUES (7, 1, '福島県', 7, now(), now());
INSERT INTO EIP_M_GPDB_KUBUN_VALUE VALUES (8, 1, '茨城県', 8, now(), now());
INSERT INTO EIP_M_GPDB_KUBUN_VALUE VALUES (9, 1, '栃木県', 9, now(), now());
INSERT INTO EIP_M_GPDB_KUBUN_VALUE VALUES (10, 1, '群馬県', 10, now(), now());
INSERT INTO EIP_M_GPDB_KUBUN_VALUE VALUES (11, 1, '埼玉県', 11, now(), now());
INSERT INTO EIP_M_GPDB_KUBUN_VALUE VALUES (12, 1, '千葉県', 12, now(), now());
INSERT INTO EIP_M_GPDB_KUBUN_VALUE VALUES (13, 1, '東京都', 13, now(), now());
INSERT INTO EIP_M_GPDB_KUBUN_VALUE VALUES (14, 1, '神奈川県', 14, now(), now());
INSERT INTO EIP_M_GPDB_KUBUN_VALUE VALUES (15, 1, '新潟県', 15, now(), now());
INSERT INTO EIP_M_GPDB_KUBUN_VALUE VALUES (16, 1, '富山県', 16, now(), now());
INSERT INTO EIP_M_GPDB_KUBUN_VALUE VALUES (17, 1, '石川県', 17, now(), now());
INSERT INTO EIP_M_GPDB_KUBUN_VALUE VALUES (18, 1, '福井県', 18, now(), now());
INSERT INTO EIP_M_GPDB_KUBUN_VALUE VALUES (19, 1, '山梨県', 19, now(), now());
INSERT INTO EIP_M_GPDB_KUBUN_VALUE VALUES (20, 1, '長野県', 20, now(), now());
INSERT INTO EIP_M_GPDB_KUBUN_VALUE VALUES (21, 1, '岐阜県', 21, now(), now());
INSERT INTO EIP_M_GPDB_KUBUN_VALUE VALUES (22, 1, '静岡県', 22, now(), now());
INSERT INTO EIP_M_GPDB_KUBUN_VALUE VALUES (23, 1, '愛知県', 23, now(), now());
INSERT INTO EIP_M_GPDB_KUBUN_VALUE VALUES (24, 1, '三重県', 24, now(), now());
INSERT INTO EIP_M_GPDB_KUBUN_VALUE VALUES (25, 1, '滋賀県', 25, now(), now());
INSERT INTO EIP_M_GPDB_KUBUN_VALUE VALUES (26, 1, '京都府', 26, now(), now());
INSERT INTO EIP_M_GPDB_KUBUN_VALUE VALUES (27, 1, '大阪府', 27, now(), now());
INSERT INTO EIP_M_GPDB_KUBUN_VALUE VALUES (28, 1, '兵庫県', 28, now(), now());
INSERT INTO EIP_M_GPDB_KUBUN_VALUE VALUES (29, 1, '奈良県', 29, now(), now());
INSERT INTO EIP_M_GPDB_KUBUN_VALUE VALUES (30, 1, '和歌山県', 30, now(), now());
INSERT INTO EIP_M_GPDB_KUBUN_VALUE VALUES (31, 1, '鳥取県', 31, now(), now());
INSERT INTO EIP_M_GPDB_KUBUN_VALUE VALUES (32, 1, '島根県', 32, now(), now());
INSERT INTO EIP_M_GPDB_KUBUN_VALUE VALUES (33, 1, '岡山県', 33, now(), now());
INSERT INTO EIP_M_GPDB_KUBUN_VALUE VALUES (34, 1, '広島県', 34, now(), now());
INSERT INTO EIP_M_GPDB_KUBUN_VALUE VALUES (35, 1, '山口県', 35, now(), now());
INSERT INTO EIP_M_GPDB_KUBUN_VALUE VALUES (36, 1, '徳島県', 36, now(), now());
INSERT INTO EIP_M_GPDB_KUBUN_VALUE VALUES (37, 1, '香川県', 37, now(), now());
INSERT INTO EIP_M_GPDB_KUBUN_VALUE VALUES (38, 1, '愛媛県', 38, now(), now());
INSERT INTO EIP_M_GPDB_KUBUN_VALUE VALUES (39, 1, '高知県', 39, now(), now());
INSERT INTO EIP_M_GPDB_KUBUN_VALUE VALUES (40, 1, '福岡県', 40, now(), now());
INSERT INTO EIP_M_GPDB_KUBUN_VALUE VALUES (41, 1, '佐賀県', 41, now(), now());
INSERT INTO EIP_M_GPDB_KUBUN_VALUE VALUES (42, 1, '長崎県', 42, now(), now());
INSERT INTO EIP_M_GPDB_KUBUN_VALUE VALUES (43, 1, '熊本県', 43, now(), now());
INSERT INTO EIP_M_GPDB_KUBUN_VALUE VALUES (44, 1, '大分県', 44, now(), now());
INSERT INTO EIP_M_GPDB_KUBUN_VALUE VALUES (45, 1, '宮崎県', 45, now(), now());
INSERT INTO EIP_M_GPDB_KUBUN_VALUE VALUES (46, 1, '鹿児島県', 46, now(), now());
INSERT INTO EIP_M_GPDB_KUBUN_VALUE VALUES (47, 1, '沖縄県', 47, now(), now());
SELECT setval('pk_eip_m_gpdb_kubun_value',47);

INSERT INTO EIP_M_PROJECT_KUBUN VALUES (1,'tracker','トラッカー', now(), now());
INSERT INTO EIP_M_PROJECT_KUBUN VALUES (2,'status','ステータス', now(), now());
INSERT INTO EIP_M_PROJECT_KUBUN VALUES (3,'priority','優先度', now(), now());
SELECT setval('pk_eip_m_project_kubun',3);

INSERT INTO EIP_M_PROJECT_KUBUN_VALUE VALUES(1,1,'1','機能',1, now(), now());
INSERT INTO EIP_M_PROJECT_KUBUN_VALUE VALUES(2,1,'2','バグ',2, now(), now());
INSERT INTO EIP_M_PROJECT_KUBUN_VALUE VALUES(3,1,'3','サポート',3, now(), now());
INSERT INTO EIP_M_PROJECT_KUBUN_VALUE VALUES(4,2,'1','新規',1, now(), now());
INSERT INTO EIP_M_PROJECT_KUBUN_VALUE VALUES(5,2,'2','進行中',2, now(), now());
INSERT INTO EIP_M_PROJECT_KUBUN_VALUE VALUES(6,2,'3','フィードバック',3, now(), now());
INSERT INTO EIP_M_PROJECT_KUBUN_VALUE VALUES(7,2,'4','完了',4, now(), now());
INSERT INTO EIP_M_PROJECT_KUBUN_VALUE VALUES(8,2,'5','却下',5, now(), now());
INSERT INTO EIP_M_PROJECT_KUBUN_VALUE VALUES(9,2,'6','停止',6, now(), now());
INSERT INTO EIP_M_PROJECT_KUBUN_VALUE VALUES(10,3,'1','高',1, now(), now());
INSERT INTO EIP_M_PROJECT_KUBUN_VALUE VALUES(11,3,'2','中',2, now(), now());
INSERT INTO EIP_M_PROJECT_KUBUN_VALUE VALUES(12,3,'3','低',3, now(), now());
SELECT setval('pk_eip_m_project_kubun_value',12);
