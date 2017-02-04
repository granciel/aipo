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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.aimluck.commons.field.ALDateTimeField;
import com.aimluck.eip.common.ALData;
import com.aimluck.eip.common.ALEipHolidaysManager;
import com.aimluck.eip.common.ALHoliday;
import com.aimluck.eip.schedule.util.ScheduleUtils;

/**
 * カレンダー用スケジュールコンテナです。
 *
 */
public class AjaxScheduleDayContainer implements ALData {

  /** <code>today</code> 日付 */
  private ALDateTimeField today;

  /** <code>scheduleList</code> スケジュールリスト */
  private List<AjaxScheduleResultData> scheduleList;

  /** <code>doneList</code> 入力済みスケジュールリスト */
  private List<Integer> doneList;

  /** <code>holiday</code> 祝日情報 */
  private ALHoliday holiday;

  /*
   *
   */
  @Override
  public void initField() {
    // 日付
    today = new ALDateTimeField("yyyy-MM-dd-HH-mm");
    // スケジュールリスト
    scheduleList = new ArrayList<AjaxScheduleResultData>();
    doneList = new ArrayList<Integer>();
  }

  /**
   * スケジュールリストを取得します。
   *
   * @return
   */
  public List<AjaxScheduleResultData> getScheduleList() {
    return scheduleList;
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
   * 日付を取得します。
   *
   * @return
   */
  public ALDateTimeField getDate() {
    return today;
  }

  /**
   * スケジュールを追加します。
   *
   * @param rd
   */
  public void addResultData(AjaxScheduleResultData rd, boolean show_all) {
    int size = scheduleList.size();
    int position = 0;
    boolean canAdd = true;
    boolean repeat_del = false;
    boolean pos_bool = false;
    for (int i = 0; i < size; i++) {
      repeat_del = false;
      AjaxScheduleResultData rd2 = scheduleList.get(i);
      if (rd.isRepeat()
        && rd.getUserId() == rd2.getUserId()
        && rd2.isDummy()
        && rd.getScheduleId().getValue() == rd2.getParentId().getValue()
        && ScheduleUtils.equalsToDate(rd.getStartDate().getValue(), rd2
          .getStartDate()
          .getValue(), false)) {
        // [繰り返しスケジュール] 親の ID を検索
        canAdd = false;
        break;
      }
      if (rd2.isRepeat()
        && rd.isDummy()
        && rd2.getScheduleId().getValue() == rd.getParentId().getValue()
        && ScheduleUtils.equalsToDate(rd.getStartDate().getValue(), rd2
          .getStartDate()
          .getValue(), false)) {
        // [繰り返しスケジュール] 親の ID を検索
        scheduleList.remove(rd2);
        canAdd = true;
        repeat_del = true;
      }

      // スケジュールを挿入する位置を捕捉する。
      // 開始日時の昇順
      if (!pos_bool
        && rd.getStartDate().getValue().before(rd2.getStartDate().getValue())) {
        position = i;
        pos_bool = true;
      }

      if (!repeat_del) {
        // 繰り返しスケジュールの変更／削除が無い場合

        if (!rd.isDummy()
          && !rd2.isDummy()
          && (rd.getType().equals(rd2.getType()))
          && (rd.getUserId() == rd2.getUserId())) {
          // ダミースケジュールではないときに
          // 重複スケジュールを検出する。
          // 時間が重なっている場合重複スケジュールとする。
          if ((rd.getStartDate().getValue().before(
            rd2.getStartDate().getValue()) && rd2
            .getStartDate()
            .getValue()
            .before(rd.getEndDate().getValue()))
            || (rd2.getStartDate().getValue().before(
              rd.getStartDate().getValue()) && rd
              .getStartDate()
              .getValue()
              .before(rd2.getEndDate().getValue()))
            || (rd
              .getStartDate()
              .getValue()
              .before(rd2.getEndDate().getValue()) && rd2
              .getEndDate()
              .getValue()
              .before(rd.getEndDate().getValue()))
            || (rd2
              .getStartDate()
              .getValue()
              .before(rd.getEndDate().getValue()) && rd
              .getEndDate()
              .getValue()
              .before(rd2.getEndDate().getValue()))
            || (rd.getEndDate().getValue().equals(rd2.getEndDate().getValue()) && rd
              .getStartDate()
              .getValue()
              .equals(rd2.getStartDate().getValue()))) {
            rd2.setDuplicate(true);
            rd.setDuplicate(true);
          }
        }
      }
    }
    if (canAdd) {
      int schedule_id = (int) rd.getScheduleId().getValue();
      if (!(doneList.contains(schedule_id))) {
        if (pos_bool) {
          scheduleList.add(position, rd);
        } else {
          scheduleList.add(rd);
        }
        if (!show_all && !rd.isDummy()) {
          doneList.add(schedule_id);
        }
      }
    }
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
   * ユーザーが設定した休日かどうか
   *
   * 
   */
  public boolean isUserHoliday() {
    Integer weekNum = today.getDayOfWeekNum();
    if (weekNum == null) {
      return false;
    }
    return ScheduleUtils.isUserHoliday(weekNum - 1);
  }

  /**
   * 祝日を休日にするかどうか
   *
   * @return
   */
  public boolean isDayOffHoliday() {
    return ScheduleUtils.isDayOffHoliday();
  }

  /**
   * 祝日情報を取得する．
   *
   * @return
   */
  public ALHoliday getHoliday() {
    return holiday;
  }

}
