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
import java.util.Calendar;
import java.util.List;

import com.aimluck.eip.common.ALData;

/**
 * 月間スケジュールコンテナを取得します。
 *
 */
public class ScheduleMonthContainer implements ALData {

  /** <code>weekList</code> 週間スケジュールリスト */
  private List<ScheduleWeekContainer> weekList;

  /*
   *
   */
  @Override
  public void initField() {
    weekList = new ArrayList<ScheduleWeekContainer>();
  }

  /**
   * 表示する月を設定します。
   *
   * @param cal
   */
  public void setViewMonth(Calendar cal) {
    for (int i = 1; i <= 6; i++) {
      // 日付を7日ずつずらす
      ScheduleWeekContainer con = new ScheduleWeekContainer();
      con.initField();
      con.setViewStartDate(cal);
      weekList.add(con);
      // cal.add(Calendar.DATE, 7);
    }
  }

  /**
   * スケジュールを追加します。
   *
   * @param rd
   */
  public boolean addResultData(ScheduleResultData rd) {
    int size = weekList.size();
    for (int i = 0; i < size; i++) {
      ScheduleWeekContainer con = weekList.get(i);
      if (con.addResultData(rd)) {
        return true;
      }
    }
    return false;
  }

  /**
   * 週間スケジュールリストを取得します。
   *
   * @return
   */
  public List<ScheduleWeekContainer> getWeekList() {
    return weekList;
  }

}
