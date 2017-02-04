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
package com.aimluck.eip.schedule;

import java.util.Date;

import com.aimluck.commons.field.ALDateTimeField;
import com.aimluck.eip.common.ALData;
import com.aimluck.eip.common.ALEipHolidaysManager;
import com.aimluck.eip.common.ALHoliday;
import com.aimluck.eip.schedule.util.ScheduleUtils;

/**
 * スケジュールに表示されるTodo コンテナです。
 *
 */
public class ScheduleToDoDayContainer implements ALData {

  /**
   * <code>today</code> 日付
   */
  private ALDateTimeField today;

  /**
   * <code>spanRd</code> ToDo
   */
  private ScheduleToDoResultData todoRd;

  private boolean is_hastodo = false;

  /** <code>holiday</code> 祝日情報 */
  private ALHoliday holiday;

  /*
   *
   */
  @Override
  public void initField() {
    // 日付
    today = new ALDateTimeField("yyyy-MM-dd-HH-mm");
    // JOB
    todoRd = null;
  }

  /**
   * 日付を取得します。
   *
   * @return
   */
  public ALDateTimeField getDate() {
    return today;
  }

  /**
   * 日付を設定します。
   *
   * @param date
   */
  public void setDate(Date date) {
    today.setValue(date);

    // 祝日かどうかを検証する．
    ALEipHolidaysManager holidaysManager = ALEipHolidaysManager.getInstance();
    holiday = holidaysManager.isHoliday(date);
  }

  /**
   * 期間スケジュールを追加します。
   *
   * @param rd
   */
  public void setToDoResultData(ScheduleToDoResultData rd) {
    todoRd = rd;
  }

  /**
   * 期間スケジュールを取得します。
   *
   * @return
   */
  public ScheduleToDoResultData getToDoResultData() {
    return todoRd;
  }

  public void setHasTodo(boolean bool) {
    is_hastodo = bool;
  }

  public boolean isHasTodo() {
    return is_hastodo;
  }

  /**
   * JobがNULLかどうか。
   *
   * @return
   */
  public boolean isTodoNull() {
    return todoRd == null;
  }

  /**
   * 祝日かどうかを検証する． 祝日の場合，true．
   *
   * @return
   */
  public boolean isHoliday() {
    return (holiday == null) ? false : true;
  }

  /**
   * 祝日情報を取得する．
   *
   * @return
   */
  public ALHoliday getHoliday() {
    return holiday;
  }

  /**
   * 祝日を休日にするかどうかを検証する. 休日にする場合 ture
   */
  public boolean isDayOffHoliday() {
    return ScheduleUtils.isDayOffHoliday();
  }

  /**
   * 休日かどうかを判定する 休日の場合 ture
   */
  public boolean isUserHoliday(int DayOfWeek) {
    return ScheduleUtils.isUserHoliday(DayOfWeek);
  }
}
