/*
 * Aipo is a groupware program developed by TOWN, Inc.
 * Copyright (C) 2004-2015 TOWN, Inc.
 * http://www.aipo.com
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.aimluck.eip.exttimecard;

import java.util.Calendar;
import java.util.List;

import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.jetspeed.services.logging.JetspeedLogFactoryService;
import org.apache.jetspeed.services.logging.JetspeedLogger;
import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;

import com.aimluck.commons.field.ALNumberField;
import com.aimluck.commons.field.ALStringField;
import com.aimluck.eip.cayenne.om.portlet.EipTExtTimecardSystem;
import com.aimluck.eip.common.ALAbstractFormData;
import com.aimluck.eip.common.ALDBErrorException;
import com.aimluck.eip.common.ALEipConstants;
import com.aimluck.eip.common.ALPageNotFoundException;
import com.aimluck.eip.exttimecard.util.ExtTimecardUtils;
import com.aimluck.eip.modules.actions.common.ALAction;
import com.aimluck.eip.orm.Database;
import com.aimluck.eip.orm.query.SelectQuery;
import com.aimluck.eip.services.config.ALConfigHandler;
import com.aimluck.eip.services.config.ALConfigService;
import com.aimluck.eip.services.eventlog.ALEventlogConstants;
import com.aimluck.eip.services.eventlog.ALEventlogFactoryService;
import com.aimluck.eip.util.ALEipUtils;
import com.aimluck.eip.util.ALLocalizationUtils;

/**
 * タイムカード集計のフォームデータを管理するためのクラスです。 <br />
 *
 */

public class ExtTimecardSystemFormData extends ALAbstractFormData {

  /** logger */
  private static final JetspeedLogger logger = JetspeedLogFactoryService
    .getLogger(ExtTimecardSystemFormData.class.getName());

  private ALNumberField system_id;

  private ALNumberField user_id;

  private ALStringField system_name;

  /** 開始時刻 */
  private ALNumberField start_hour;

  private ALNumberField start_minute;

  private ALNumberField start_day;

  /** 終了時刻 */
  private ALNumberField end_hour;

  private ALNumberField end_minute;

  private ALNumberField worktime_in;

  private ALNumberField resttime_in;

  private ALNumberField worktime_out;

  private ALNumberField resttime_out;

  private ALNumberField change_hour;

  private ALStringField outgoing_add_flag;

  private ALNumberField overtime_type_minute;

  private ALNumberField overtime_type_week_hour;

  private ALStringField overtime_type_week;

  private int entity_id;

  private boolean isNewRule = false;

  /** 休日設定 */
  private ALStringField default_holiday_flag;

  private ALStringField week1;

  private ALStringField week2;

  private ALStringField week3;

  private ALStringField week4;

  private ALStringField week5;

  private ALStringField week6;

  private ALStringField week7;

  private ALStringField statutoryHoliday;

  private ALStringField holiday;

  @Override
  public void init(ALAction action, RunData rundata, Context context)
      throws ALPageNotFoundException, ALDBErrorException {
    super.init(action, rundata, context);
  }

  /**
   *
   */
  @Override
  public void initField() {
    system_id = new ALNumberField();
    system_id.setNotNull(true);

    user_id = new ALNumberField();
    user_id.setNotNull(true);

    system_name = new ALStringField();
    system_name.setFieldName(ALLocalizationUtils
      .getl10n("EXTTIMECARD_SETFIELDNAME_WORKING_ARRANGEMENTS"));
    system_name.setNotNull(true);

    start_hour = new ALNumberField();
    start_minute = new ALNumberField();
    end_hour = new ALNumberField();
    end_minute = new ALNumberField();

    start_day = new ALNumberField();
    start_day.setFieldName(ALLocalizationUtils
      .getl10n("EXTTIMECARD_SETFIELDNAME_START_DAY"));

    worktime_in = new ALNumberField();
    worktime_in.setFieldName(ALLocalizationUtils
      .getl10n("EXTTIMECARD_SETFIELDNAME_WORKTIME_IN_WORKTIME"));
    worktime_in.setNotNull(true);
    worktime_in.limitMinValue(0);

    resttime_in = new ALNumberField();
    resttime_in.setFieldName(ALLocalizationUtils
      .getl10n("EXTTIMECARD_SETFIELDNAME_WORKTIME_IN_RESTTIME"));
    resttime_in.setNotNull(true);
    resttime_in.limitValue(0, 360);

    worktime_out = new ALNumberField();
    worktime_out.setFieldName(ALLocalizationUtils
      .getl10n("EXTTIMECARD_SETFIELDNAME_WORKTIME_OUT_WORKTIME"));
    worktime_out.setNotNull(true);
    worktime_out.limitMinValue(0);

    resttime_out = new ALNumberField();
    resttime_out.setFieldName(ALLocalizationUtils
      .getl10n("EXTTIMECARD_SETFIELDNAME_WORKTIME_OUT_RESTTIME"));
    resttime_out.setNotNull(true);
    resttime_out.limitValue(0, 360);

    change_hour = new ALNumberField();
    change_hour.setNotNull(true);

    outgoing_add_flag = new ALStringField();

    overtime_type_minute = new ALNumberField();
    overtime_type_minute.setFieldName(ALLocalizationUtils
      .getl10n("EXTTIMECARD_SETFIELDNAME_OVERTIME_TYPE"));
    overtime_type_minute.limitMinValue(0);
    overtime_type_week_hour = new ALNumberField();
    overtime_type_week_hour.setFieldName(ALLocalizationUtils
      .getl10n("EXTTIMECARD_SETFIELDNAME_OVERTIME_TYPE"));
    overtime_type_week_hour.limitMinValue(0);

    overtime_type_week = new ALStringField();
    overtime_type_week.setFieldName(ALLocalizationUtils
      .getl10n("EXTTIMECARD_SETFIELDNAME_OVERTIME_TYPE"));

    isNewRule = isNewRule();

    default_holiday_flag = new ALStringField();
    default_holiday_flag.setValue("A");
    week1 = new ALStringField();
    week1.setFieldName(ALLocalizationUtils
      .getl10n("EXTTIMECARD_HOLIDAY_SETTING_WEEK1"));
    week2 = new ALStringField();
    week2.setFieldName(ALLocalizationUtils
      .getl10n("EXTTIMECARD_HOLIDAY_SETTING_WEEK2"));
    week3 = new ALStringField();
    week3.setFieldName(ALLocalizationUtils
      .getl10n("EXTTIMECARD_HOLIDAY_SETTING_WEEK3"));
    week4 = new ALStringField();
    week4.setFieldName(ALLocalizationUtils
      .getl10n("EXTTIMECARD_HOLIDAY_SETTING_WEEK4"));
    week5 = new ALStringField();
    week5.setFieldName(ALLocalizationUtils
      .getl10n("EXTTIMECARD_HOLIDAY_SETTING_WEEK5"));
    week6 = new ALStringField();
    week6.setFieldName(ALLocalizationUtils
      .getl10n("EXTTIMECARD_HOLIDAY_SETTING_WEEK6"));
    week7 = new ALStringField();
    week7.setFieldName(ALLocalizationUtils
      .getl10n("EXTTIMECARD_HOLIDAY_SETTING_WEEK7"));
    statutoryHoliday = new ALStringField();
    statutoryHoliday.setFieldName(ALLocalizationUtils
      .getl10n("EXTTIMECARD_HOLIDAY_SETTING_STATUTORY_HOLIDAY"));
    holiday = new ALStringField();
    holiday.setFieldName(ALLocalizationUtils
      .getl10n("EXTTIMECARD_HOLIDAY_SETTING_HOLIDAY"));

  }

  @Override
  protected boolean loadFormData(RunData rundata, Context context,
      List<String> msgList) throws ALPageNotFoundException, ALDBErrorException {
    try {
      EipTExtTimecardSystem record =
        ExtTimecardUtils.getEipTExtTimecardSystem(rundata, context);
      if (record == null) {
        return false;
      }

      system_id.setValue(String.valueOf(record.getSystemId()));
      user_id.setValue(String.valueOf(record.getUserId()));
      system_name.setValue(record.getSystemName());

      start_hour.setValue(String.valueOf(record.getStartHour()));
      start_minute.setValue(String.valueOf(record.getStartMinute()));
      end_hour.setValue(String.valueOf(record.getEndHour()));
      end_minute.setValue(String.valueOf(record.getEndMinute()));

      start_day.setValue(String.valueOf(record.getStartDay()));

      worktime_in.setValue(record.getWorktimeIn());
      resttime_in.setValue(record.getResttimeIn());
      worktime_out.setValue(record.getWorktimeOut());
      resttime_out.setValue(record.getResttimeOut());

      change_hour.setValue(String.valueOf(record.getChangeHour()));
      outgoing_add_flag.setValue(record.getOutgoingAddFlag());
      if (isNewRule) {
        overtime_type_minute.setValue(ExtTimecardUtils
          .getOvertimeMinuteByDay(record.getOvertimeType()));
        overtime_type_week_hour.setValue(ExtTimecardUtils
          .getOvertimeHourByWeek(record.getOvertimeType()));
        overtime_type_week.setValue(ExtTimecardUtils
          .isOvertimeHourByWeek(record.getOvertimeType()) ? "T" : "F");
      }

      default_holiday_flag.setValue(String.valueOf(record
        .getHolidayOfWeek()
        .charAt(0)));
      if (default_holiday_flag.getValue().toString().equals("A")) {
        // 会社の設定を読み込む
        String holidayOfWeek =
          ALConfigService.get(ALConfigHandler.Property.HOLIDAY_OF_WEEK);
        week1.setValue(holidayOfWeek.charAt(0) != '0' ? "1" : null);
        week2.setValue(holidayOfWeek.charAt(1) != '0' ? "1" : null);
        week3.setValue(holidayOfWeek.charAt(2) != '0' ? "1" : null);
        week4.setValue(holidayOfWeek.charAt(3) != '0' ? "1" : null);
        week5.setValue(holidayOfWeek.charAt(4) != '0' ? "1" : null);
        week6.setValue(holidayOfWeek.charAt(5) != '0' ? "1" : null);
        week7.setValue(holidayOfWeek.charAt(6) != '0' ? "1" : null);
        statutoryHoliday.setValue(String.valueOf(holidayOfWeek.charAt(7)));
        holiday.setValue(holidayOfWeek.charAt(8) != '0' ? "1" : null);
      } else {
        week1.setValue(record.getHolidayOfWeek().charAt(1) != '0' ? "1" : null);
        week2.setValue(record.getHolidayOfWeek().charAt(2) != '0' ? "1" : null);
        week3.setValue(record.getHolidayOfWeek().charAt(3) != '0' ? "1" : null);
        week4.setValue(record.getHolidayOfWeek().charAt(4) != '0' ? "1" : null);
        week5.setValue(record.getHolidayOfWeek().charAt(5) != '0' ? "1" : null);
        week6.setValue(record.getHolidayOfWeek().charAt(6) != '0' ? "1" : null);
        week7.setValue(record.getHolidayOfWeek().charAt(7) != '0' ? "1" : null);
        statutoryHoliday.setValue(String.valueOf(record
          .getHolidayOfWeek()
          .charAt(8)));
        holiday.setValue(record.getHolidayOfWeek().charAt(9) != '0'
          ? "1"
          : null);
      }

    } catch (Exception ex) {
      logger.error("exttimecard", ex);
      return false;
    }
    return true;
  }

  @Override
  protected boolean updateFormData(RunData rundata, Context context,
      List<String> msgList) throws ALPageNotFoundException, ALDBErrorException {
    try {
      EipTExtTimecardSystem record =
        ExtTimecardUtils.getEipTExtTimecardSystem(rundata, context);
      if (record == null) {
        return false;
      }

      record.setSystemName(system_name.getValue());
      record.setUserId((int) user_id.getValue());
      record.setStartHour((int) start_hour.getValue());
      record.setStartMinute((int) start_minute.getValue());
      record.setEndHour((int) end_hour.getValue());
      record.setEndMinute((int) end_minute.getValue());
      record.setStartDay((int) start_day.getValue());
      record.setWorktimeIn((int) worktime_in.getValue());
      record.setResttimeIn((int) resttime_in.getValue());
      record.setWorktimeOut((int) worktime_out.getValue());
      record.setResttimeOut((int) resttime_out.getValue());
      record.setChangeHour((int) change_hour.getValue());
      String tmp;
      if ("T".equals(outgoing_add_flag.getValue())) {
        tmp = "T";
      } else {
        tmp = "F";
      }
      record.setOutgoingAddFlag(tmp);

      // 更新日
      record.setUpdateDate(Calendar.getInstance().getTime());

      if (isNewRule) {
        String overType =
          +overtime_type_minute.getValue()
            + "-"
            + ("T".equals(overtime_type_week.getValue()) ? "T" : "F")
            + overtime_type_week_hour.getValue();
        record.setOvertimeType(overType);
      }

      StringBuilder b = new StringBuilder();
      if (default_holiday_flag.getValue().toString().equals("A")) {
        b.append("A");
      } else {
        String value = statutoryHoliday.getValue();
        if ("1".equals(value)) {
          week1.setValue("1");
        }
        if ("2".equals(value)) {
          week2.setValue("1");
        }
        if ("3".equals(value)) {
          week3.setValue("1");
        }
        if ("4".equals(value)) {
          week4.setValue("1");
        }
        if ("5".equals(value)) {
          week5.setValue("1");
        }
        if ("6".equals(value)) {
          week6.setValue("1");
        }
        if ("7".equals(value)) {
          week7.setValue("1");
        }
        b.append("C");
        b.append("1".equals(week1.getValue()) ? "1" : "0");
        b.append("1".equals(week2.getValue()) ? "1" : "0");
        b.append("1".equals(week3.getValue()) ? "1" : "0");
        b.append("1".equals(week4.getValue()) ? "1" : "0");
        b.append("1".equals(week5.getValue()) ? "1" : "0");
        b.append("1".equals(week6.getValue()) ? "1" : "0");
        b.append("1".equals(week7.getValue()) ? "1" : "0");
        b.append(statutoryHoliday.getValue());
        b.append("1".equals(holiday.getValue()) ? "1" : "0");
      }
      record.setHolidayOfWeek(b.toString());

      // イベントログに保存
      ALEventlogFactoryService.getInstance().getEventlogHandler().log(
        record.getSystemId(),
        ALEventlogConstants.PORTLET_TYPE_EXTTIMECARD_SYSTEM,
        record.getSystemName());

      Database.commit();
    } catch (Exception ex) {
      Database.rollback();
      logger.error("exttimecard", ex);
      return false;
    }
    return true;
  }

  @Override
  protected boolean deleteFormData(RunData rundata, Context context,
      List<String> msgList) throws ALPageNotFoundException, ALDBErrorException {
    try {
      // オブジェクトモデルを取得
      EipTExtTimecardSystem record =
        ExtTimecardUtils.getEipTExtTimecardSystem(rundata, context);
      if (record == null) {
        return false;
      }

      if (record.getSystemId().intValue() == 1) {
        // 勤務形態「通常」は削除不可
        msgList.add(ALLocalizationUtils
          .getl10n("EXTTIMECARD_ALERT_DELETE_WORKING_ARRANGEMENTS"));
        return false;
      }

      Database.delete(record);

      // イベントログに保存
      ALEventlogFactoryService.getInstance().getEventlogHandler().log(
        record.getSystemId(),
        ALEventlogConstants.PORTLET_TYPE_EXTTIMECARD_SYSTEM,
        record.getSystemName());

      Database.commit();
    } catch (Exception ex) {
      Database.rollback();
      logger.error("exttimecard", ex);
      return false;
    }
    return true;
  }

  @Override
  protected boolean insertFormData(RunData rundata, Context context,
      List<String> msgList) throws ALPageNotFoundException, ALDBErrorException {
    try {
      EipTExtTimecardSystem record =
        Database.create(EipTExtTimecardSystem.class);
      record.setSystemName(system_name.getValue());
      record.setUserId((int) user_id.getValue());
      record.setStartHour((int) start_hour.getValue());
      record.setStartMinute((int) start_minute.getValue());
      record.setEndHour((int) end_hour.getValue());
      record.setEndMinute((int) end_minute.getValue());
      record.setStartDay((int) start_day.getValue());
      record.setWorktimeIn((int) worktime_in.getValue());
      record.setResttimeIn((int) resttime_in.getValue());
      record.setWorktimeOut((int) worktime_out.getValue());
      record.setResttimeOut((int) resttime_out.getValue());
      record.setChangeHour((int) change_hour.getValue());
      String tmp;
      if ("T".equals(outgoing_add_flag.getValue())) {
        tmp = "T";
      } else {
        tmp = "F";
      }
      record.setOutgoingAddFlag(tmp);

      String overType;
      if (isNewRule) {
        overType =
          +overtime_type_minute.getValue()
            + "-"
            + ("T".equals(overtime_type_week.getValue()) ? "T" : "F")
            + overtime_type_week_hour.getValue();
      } else {
        overType = ExtTimecardUtils.OVERTIME_TYPE_O;
      }
      record.setOvertimeType(overType);
      record.setCreateDate(Calendar.getInstance().getTime());
      record.setUpdateDate(Calendar.getInstance().getTime());
      StringBuilder b = new StringBuilder();
      if (default_holiday_flag.getValue().toString().equals("A")) {
        b.append("A");
      } else {
        String value = statutoryHoliday.getValue();
        if ("1".equals(value)) {
          week1.setValue("1");
        }
        if ("2".equals(value)) {
          week2.setValue("1");
        }
        if ("3".equals(value)) {
          week3.setValue("1");
        }
        if ("4".equals(value)) {
          week4.setValue("1");
        }
        if ("5".equals(value)) {
          week5.setValue("1");
        }
        if ("6".equals(value)) {
          week6.setValue("1");
        }
        if ("7".equals(value)) {
          week7.setValue("1");
        }
        b.append("C");
        b.append("1".equals(week1.getValue()) ? "1" : "0");
        b.append("1".equals(week2.getValue()) ? "1" : "0");
        b.append("1".equals(week3.getValue()) ? "1" : "0");
        b.append("1".equals(week4.getValue()) ? "1" : "0");
        b.append("1".equals(week5.getValue()) ? "1" : "0");
        b.append("1".equals(week6.getValue()) ? "1" : "0");
        b.append("1".equals(week7.getValue()) ? "1" : "0");
        b.append(statutoryHoliday.getValue());
        b.append("1".equals(holiday.getValue()) ? "1" : "0");
      }
      record.setHolidayOfWeek(b.toString());
      Database.commit();

      // イベントログに保存
      ALEventlogFactoryService.getInstance().getEventlogHandler().log(
        record.getSystemId(),
        ALEventlogConstants.PORTLET_TYPE_EXTTIMECARD_SYSTEM,
        record.getSystemName());
    } catch (Exception ex) {
      Database.rollback();
      logger.error("exttimecard", ex);
      return false;
    }
    return true;
  }

  @Override
  protected boolean setFormData(RunData rundata, Context context,
      List<String> msgList) throws ALPageNotFoundException, ALDBErrorException {
    boolean res = super.setFormData(rundata, context, msgList);

    if (res) {
      if (ALEipConstants.MODE_NEW_FORM.equals(this.getMode())) {
        try {
          SelectQuery<EipTExtTimecardSystem> query =
            Database.query(EipTExtTimecardSystem.class);
          Expression exp1 =
            ExpressionFactory.matchDbExp(
              EipTExtTimecardSystem.SYSTEM_ID_PK_COLUMN,
              1);
          query.setQualifier(exp1);

          List<EipTExtTimecardSystem> list = query.fetchList();
          EipTExtTimecardSystem record = list.get(0);

          user_id.setValue(String.valueOf(record.getUserId()));
          system_name.setValue("");

          start_hour.setValue(String.valueOf(record.getStartHour()));
          start_minute.setValue(String.valueOf(record.getStartMinute()));
          end_hour.setValue(String.valueOf(record.getEndHour()));
          end_minute.setValue(String.valueOf(record.getEndMinute()));

          start_day.setValue(String.valueOf(record.getStartDay()));

          worktime_in.setValue(record.getWorktimeIn());
          resttime_in.setValue(record.getResttimeIn());
          worktime_out.setValue(record.getWorktimeOut());
          resttime_out.setValue(record.getResttimeOut());

          change_hour.setValue(String.valueOf(record.getChangeHour()));
          outgoing_add_flag.setValue(record.getOutgoingAddFlag());
          overtime_type_minute
            .setValue(ExtTimecardUtils.OVERTIME_TYPE_DEFAULT_MINUTE);
          overtime_type_week_hour
            .setValue(ExtTimecardUtils.OVERTIME_TYPE_DEFAULT_HOUR_BY_WEEK);
          overtime_type_week.setValue("F");

          default_holiday_flag.setValue(String.valueOf(record
            .getHolidayOfWeek()
            .charAt(0)));
          if (default_holiday_flag.getValue().toString().equals("A")) {
            // 会社の設定を読み込む
            String holidayOfWeek =
              ALConfigService.get(ALConfigHandler.Property.HOLIDAY_OF_WEEK);
            week1.setValue(holidayOfWeek.charAt(0) != '0' ? "1" : null);
            week2.setValue(holidayOfWeek.charAt(1) != '0' ? "1" : null);
            week3.setValue(holidayOfWeek.charAt(2) != '0' ? "1" : null);
            week4.setValue(holidayOfWeek.charAt(3) != '0' ? "1" : null);
            week5.setValue(holidayOfWeek.charAt(4) != '0' ? "1" : null);
            week6.setValue(holidayOfWeek.charAt(5) != '0' ? "1" : null);
            week7.setValue(holidayOfWeek.charAt(6) != '0' ? "1" : null);
            statutoryHoliday.setValue(String.valueOf(holidayOfWeek.charAt(7)));
            holiday.setValue(holidayOfWeek.charAt(8) != '0' ? "1" : null);
          } else {
            week1.setValue(record.getHolidayOfWeek().charAt(1) != '0'
              ? "1"
              : null);
            week2.setValue(record.getHolidayOfWeek().charAt(2) != '0'
              ? "1"
              : null);
            week3.setValue(record.getHolidayOfWeek().charAt(3) != '0'
              ? "1"
              : null);
            week4.setValue(record.getHolidayOfWeek().charAt(4) != '0'
              ? "1"
              : null);
            week5.setValue(record.getHolidayOfWeek().charAt(5) != '0'
              ? "1"
              : null);
            week6.setValue(record.getHolidayOfWeek().charAt(6) != '0'
              ? "1"
              : null);
            week7.setValue(record.getHolidayOfWeek().charAt(7) != '0'
              ? "1"
              : null);
            statutoryHoliday.setValue(String.valueOf(record
              .getHolidayOfWeek()
              .charAt(8)));
            holiday.setValue(record.getHolidayOfWeek().charAt(9) != '0'
              ? "1"
              : null);
          }
        } catch (Exception ex) {
          logger.error("[ExtTimecardSystemFormData]", ex);
        }
      }
      if (ALEipConstants.MODE_UPDATE.equals(this.getMode())) {
        if (!(this.entity_id > 0)) {
          entity_id =
            Integer.parseInt(ALEipUtils.getTemp(
              rundata,
              context,
              ALEipConstants.ENTITY_ID));
          system_id.setValue(entity_id);
        }
      }
    }
    return res;
  }

  @Override
  protected void setValidator() throws ALPageNotFoundException,
      ALDBErrorException {
    start_hour.limitValue(0, 23);
    start_minute.limitValue(0, 59);
    end_hour.limitValue(0, 23);
    end_minute.limitValue(0, 59);
    start_day.limitValue(0, 28);
    worktime_in.limitValue(0, 480);
    worktime_out.limitValue(0, 480);
    resttime_in.limitValue(0, 480);
    resttime_out.limitValue(0, 480);
    overtime_type_minute.limitValue(0, 1440);
    overtime_type_week_hour.limitValue(0, 168);
  }

  @Override
  protected boolean validate(List<String> msgList)
      throws ALPageNotFoundException, ALDBErrorException {

    try {
      SelectQuery<EipTExtTimecardSystem> query =
        Database.query(EipTExtTimecardSystem.class);
      Expression exp1 =
        ExpressionFactory.matchExp(
          EipTExtTimecardSystem.SYSTEM_NAME_PROPERTY,
          system_name.getValue());
      query.setQualifier(exp1);
      if (ALEipConstants.MODE_UPDATE.equals(getMode())) {

        Expression exp2 =
          ExpressionFactory.noMatchDbExp(
            EipTExtTimecardSystem.SYSTEM_ID_PK_COLUMN,
            system_id.getValue());
        query.andQualifier(exp2);
      }
      if (query.fetchList().size() != 0) {
        msgList.add(ALLocalizationUtils.getl10nFormat(
          "EXTTIMECARD_ALERT_ALREADY_ADDED_WORKING_ARRANGEMENTS",
          system_name.toString()));
      }

      long start_time = start_hour.getValue() * 60 + start_minute.getValue();
      long end_time = end_hour.getValue() * 60 + end_minute.getValue();
      long change_time = change_hour.getValue() * 60;
      if (!isValidChangeTime(start_time, end_time, change_time)) {
        msgList.add(ALLocalizationUtils
          .getl10n("EXTTIMECARD_ALERT_SELECT_CHANGE_HOUR"));
      }
      if (isNewRule) {
        overtime_type_minute.validate(msgList);
        overtime_type_week_hour.validate(msgList);
      }
    } catch (Exception ex) {
      logger.error("exttimecard", ex);
      return false;
    }

    system_name.validate(msgList);
    worktime_in.validate(msgList);
    resttime_in.validate(msgList);
    worktime_out.validate(msgList);
    resttime_out.validate(msgList);

    return (msgList.size() == 0);
  }

  /**
   * 勤務時間と日付切替時刻の関係の妥当性を検証します。
   *
   * @param start_time
   * @param end_time
   * @param change_time
   * @return
   */
  private boolean isValidChangeTime(long start_time, long end_time,
      long change_time) {
    if (end_time <= start_time) {
      end_time += 24 * 60;
    }

    if (start_time <= change_time && change_time <= end_time) {
      return false;
    } else {
      change_time += 24 * 60;
      if (end_time >= change_time) {
        return false;
      }
    }
    return true;
  }

  /**
   * 詳細データを取得する抽象メソッドです。
   *
   * @param rundata
   * @param context
   * @return
   */
  protected Object selectDetail(RunData rundata, Context context)
      throws ALPageNotFoundException, ALDBErrorException {
    return null;
  }

  /**
   *
   * @return
   */
  public ALNumberField getStartTimeHour() {
    return this.start_hour;
  }

  /**
   *
   * @return
   */
  public ALNumberField getStartTimeMinute() {
    return this.start_minute;
  }

  /**
   *
   * @return
   */
  public ALNumberField getEndTimeHour() {
    return this.end_hour;
  }

  /**
   *
   * @return
   */
  public ALNumberField getEndTimeMinute() {
    return this.end_minute;
  }

  /**
   *
   * @return
   */
  public ALNumberField getWorktimeIn() {
    return this.worktime_in;
  }

  /**
   *
   * @return
   */
  public ALNumberField getWorktimeOut() {
    return this.worktime_out;
  }

  /**
   *
   * @return
   */
  public ALNumberField getResttimeIn() {
    return this.resttime_in;
  }

  /**
   *
   * @return
   */
  public ALNumberField getResttimeOut() {
    return this.resttime_out;
  }

  public String getWorkTimeInRestTimeInText() {
    return ALLocalizationUtils.getl10nFormat(
      "EXTTIMECARD_INPUT_RESTTIME_FOR_WORKTIMEIN",
      worktime_in.toString(),
      resttime_in.toString());
  }

  public String getWorkTimeOutRestTimeOutText() {
    return ALLocalizationUtils.getl10nFormat(
      "EXTTIMECARD_INPUT_RESTTIME_FOR_WORKTIMEOUT",
      worktime_out.toString(),
      resttime_out.toString());
  }

  /**
   *
   * @return
   */
  public ALNumberField getChangeHour() {
    return this.change_hour;
  }

  /**
   *
   * @return
   */
  public ALStringField getSystemName() {
    return this.system_name;
  }

  /**
   *
   * @return
   */
  public ALStringField getOutgoingAddFlag() {
    return this.outgoing_add_flag;
  }

  /**
   * @return
   */
  public ALNumberField getStartDay() {
    return start_day;
  }

  public ALNumberField getSystemId() {
    return this.system_id;
  }

  public ALNumberField getOvertimeTypeMinute() {
    return overtime_type_minute;
  }

  public ALNumberField getOvertimeTypeWeekHour() {
    return overtime_type_week_hour;
  }

  public ALStringField getOvertimeTypeWeek() {
    return overtime_type_week;
  }

  public boolean isNewRule() {
    return ExtTimecardUtils.isNewRule();
  }

  public ALStringField getDefaultHolidayFlag() {
    return default_holiday_flag;
  }

  public ALStringField getWeek1() {
    return week1;
  }

  public ALStringField getWeek2() {
    return week2;
  }

  public ALStringField getWeek3() {
    return week3;
  }

  public ALStringField getWeek4() {
    return week4;
  }

  public ALStringField getWeek5() {
    return week5;
  }

  public ALStringField getWeek6() {
    return week6;
  }

  public ALStringField getWeek7() {
    return week7;
  }

  public ALStringField getStatutoryHoliday() {
    return statutoryHoliday;
  }

  public ALStringField getHoliday() {
    return holiday;
  }

}
