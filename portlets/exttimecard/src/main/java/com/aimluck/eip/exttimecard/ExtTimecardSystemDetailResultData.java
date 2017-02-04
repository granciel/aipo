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

import com.aimluck.commons.field.ALNumberField;
import com.aimluck.commons.field.ALStringField;
import com.aimluck.commons.utils.ALStringUtil;
import com.aimluck.eip.util.ALLocalizationUtils;

/**
 * タイムカード管理の勤務形態の情報を保持する。
 *
 *
 */
public class ExtTimecardSystemDetailResultData extends
    ExtTimecardSystemResultData {

  private ALNumberField user_id;

  private ALNumberField start_hour;

  private ALNumberField start_minute;

  private ALNumberField end_hour;

  private ALNumberField end_minute;

  private ALNumberField start_day;

  private ALNumberField worktime_in;

  private ALNumberField resttime_in;

  private ALNumberField worktime_out;

  private ALNumberField resttime_out;

  private ALNumberField change_hour;

  private ALStringField outgoing_add_flag;

  private ALStringField create_date;

  private ALStringField update_date;

  private ALNumberField overtime_type_minute_by_day;

  private ALNumberField overtime_type_hour_by_week;

  private boolean isOvertypeWeek;

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

  private boolean hasHoliday;

  /**
   *
   *
   */
  @Override
  public void initField() {
    super.initField();
    user_id = new ALNumberField();
    start_hour = new ALNumberField();
    start_minute = new ALNumberField();
    end_hour = new ALNumberField();
    end_minute = new ALNumberField();
    start_day = new ALNumberField();
    worktime_in = new ALNumberField();
    resttime_in = new ALNumberField();
    worktime_out = new ALNumberField();
    resttime_out = new ALNumberField();
    change_hour = new ALNumberField();
    outgoing_add_flag = new ALStringField();
    create_date = new ALStringField();
    update_date = new ALStringField();
    overtime_type_minute_by_day = new ALNumberField();
    overtime_type_hour_by_week = new ALNumberField();
    default_holiday_flag = new ALStringField();
    week1 = new ALStringField();
    week2 = new ALStringField();
    week3 = new ALStringField();
    week4 = new ALStringField();
    week5 = new ALStringField();
    week6 = new ALStringField();
    week7 = new ALStringField();
    statutoryHoliday = new ALStringField();
    holiday = new ALStringField();
  }

  public String getUserId() {
    return user_id.getValueAsString();
  }

  public String getStartHour() {
    return start_hour.getValueAsString();
  }

  public String getStartMinute() {
    return start_minute.getValueAsString();
  }

  public String getEndHour() {
    return end_hour.getValueAsString();
  }

  public String getEndMinute() {
    return end_minute.getValueAsString();
  }

  public String getSpan() {
    return ALLocalizationUtils.getl10nFormat(
      "EXTTIMECARD_HOUR_MINUTE_FORMAT_SPAN",
      start_hour.toString(),
      ALStringUtil.toTwoDigitString(start_minute),
      end_hour.toString(),
      ALStringUtil.toTwoDigitString(end_minute));
  }

  public String getWorkTimeIn() {
    return worktime_in.getValueAsString();
  }

  public String getRestTimeIn() {
    return resttime_in.getValueAsString();
  }

  public String getWorkTimeInRestTimeInText() {
    return ALLocalizationUtils.getl10nFormat(
      "EXTTIMECARD_RESTTIME_FOR_WORKTIME",
      worktime_in.toString(),
      resttime_in.toString());
  }

  public String getWorkTimeOut() {
    return worktime_out.getValueAsString();
  }

  public String getRestTimeOut() {
    return resttime_out.getValueAsString();
  }

  public String getWorkTimeOutRestTimeOutText() {
    return ALLocalizationUtils.getl10nFormat(
      "EXTTIMECARD_RESTTIME_FOR_WORKTIME",
      worktime_out.toString(),
      resttime_out.toString());
  }

  public String getChangeHour() {
    return change_hour.getValueAsString();
  }

  public String getOutgoingAddFlag() {
    return outgoing_add_flag.getValue();
  }

  public ALStringField getCreateDate() {
    return create_date;
  }

  public ALStringField getUpdateDate() {
    return update_date;
  }

  public ALNumberField getOvertimeTypeMinuteByDay() {
    return overtime_type_minute_by_day;
  }

  public ALNumberField getOvertimeTypeHourByWeek() {
    return overtime_type_hour_by_week;
  }

  public ALNumberField getStartDay() {
    return start_day;
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

  public void setUserId(int i) {
    user_id.setValue(i);
  }

  public void setStartHour(int i) {
    start_hour.setValue(i);
  }

  public void setStartMinute(int i) {
    start_minute.setValue(i);
  }

  public void setEndHour(int i) {
    end_hour.setValue(i);
  }

  public void setEndMinute(int i) {
    end_minute.setValue(i);
  }

  public void setWorkTimeIn(int i) {
    worktime_in.setValue(i);
  }

  public void setRestTimeIn(int i) {
    resttime_in.setValue(i);
  }

  public void setWorkTimeOut(int i) {
    worktime_out.setValue(i);
  }

  public void setRestTimeOut(int i) {
    resttime_out.setValue(i);
  }

  public void setChangeHour(int i) {
    change_hour.setValue(i);
  }

  public void setOutgoingAddFlag(String str) {
    outgoing_add_flag.setValue(str);
  }

  public void setCreateDate(String str) {
    create_date.setValue(str);
  }

  public void setUpdateDate(String str) {
    update_date.setValue(str);
  }

  public void setStartDay(int i) {
    start_day.setValue(i);
  }

  public void setOvertimeTypeMinuteByDay(int value) {
    overtime_type_minute_by_day.setValue(value);
  }

  public void setOvertimeTypeHourByWeek(int value) {
    overtime_type_hour_by_week.setValue(value);
  }

  public void setDefaultHolidayFlag(String str) {
    default_holiday_flag.setValue(str);
  }

  public void setWeek1(String str) {
    week1.setValue(str);
  }

  public void setWeek2(String str) {
    week2.setValue(str);
  }

  public void setWeek3(String str) {
    week3.setValue(str);
  }

  public void setWeek4(String str) {
    week4.setValue(str);
  }

  public void setWeek5(String str) {
    week5.setValue(str);
  }

  public void setWeek6(String str) {
    week6.setValue(str);
  }

  public void setWeek7(String str) {
    week7.setValue(str);
  }

  public void setStatutoryHoliday(String str) {
    statutoryHoliday.setValue(str);
  }

  public void setHoliday(String str) {
    holiday.setValue(str);
  }

  /**
   * @return isOvertypeWeek
   */
  public boolean isOvertypeWeek() {
    return isOvertypeWeek;
  }

  /**
   * @param isOvertypeWeek
   *          セットする isOvertypeWeek
   */
  public void setOvertypeWeek(boolean isOvertypeWeek) {
    this.isOvertypeWeek = isOvertypeWeek;
  }

  /**
   * @return hasHoliday
   */
  public boolean getHasHoliday() {
    return hasHoliday;
  }

  /**
   * @param hasHoliday
   *          セットする hasHoliday
   */
  public void setHasHoliday(boolean hasHoliday) {
    this.hasHoliday = hasHoliday;
  }

}
