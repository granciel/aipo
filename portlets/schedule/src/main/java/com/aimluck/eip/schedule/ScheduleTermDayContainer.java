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
 * 期間スケジュールコンテナです。
 *
 */
public class ScheduleTermDayContainer implements ALData {

  /**
   * <code>today</code> 日付
   */
  private ALDateTimeField today;

  /**
   * <code>spanRd</code> 期間スケジュール
   */
  private ScheduleResultData termRd;

  /** <code>is_hasterm</code> 期間スケジュールがあるかどうか */
  private boolean is_hasterm = false;

  /** <code>holiday</code> 祝日情報 */
  private ALHoliday holiday;

  /*
   *
   */
  @Override
  public void initField() {
    today = new ALDateTimeField("yyyy-MM-dd-HH-mm");
    termRd = null;
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
  public void setTermResultData(ScheduleResultData rd) {
    termRd = rd;
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

  /**
   * 期間スケジュールを取得します。
   *
   * @return
   */
  public ScheduleResultData getTermResultData() {
    return termRd;
  }

  public void setHasTerm(boolean bool) {
    is_hasterm = bool;
  }

  public boolean isHasTerm() {
    return is_hasterm;
  }

  /**
   * 期間スケジュールがNULLかどうか。
   *
   * @return
   */
  public boolean isTermNull() {
    return termRd == null;
  }
}
