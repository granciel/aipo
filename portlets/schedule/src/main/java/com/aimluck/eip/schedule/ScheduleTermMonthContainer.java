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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.aimluck.eip.common.ALData;
import com.aimluck.eip.schedule.util.ScheduleUtils;

/**
 * 期間スケジュール用の月間スケジュールコンテナを取得します。
 *
 */
public class ScheduleTermMonthContainer implements ALData {

  /** <code>map</code> 期間スケジュールマップ．key は「0」から始まる Integer. */
  private Map<Integer, List<ScheduleTermWeekContainer>> map;

  private Calendar viewStartCal;

  /*
   *
   */
  @Override
  public void initField() {
    map = new LinkedHashMap<Integer, List<ScheduleTermWeekContainer>>();
  }

  /**
   * 表示する月を設定します。
   *
   * @param cal
   */
  public void setViewMonth(Calendar cal) {
    List<ScheduleTermWeekContainer> weekList;
    viewStartCal = Calendar.getInstance();
    viewStartCal.setTime(cal.getTime());
    for (int i = 1; i <= 6; i++) {
      // if ((i == 5 || i == 6)
      // && (cal.get(Calendar.MONTH) != vcal.get(Calendar.MONTH))) {
      // break;
      // }
      weekList = new ArrayList<ScheduleTermWeekContainer>();
      map.put(Integer.valueOf(i - 1), weekList);
      // 一週間ずらす
      cal.add(Calendar.DATE, 7);
    }
  }

  /**
   * 期間スケジュールを追加します。
   *
   * @param count
   * @param rd
   */
  public void addTermResultData(int count, int row, ScheduleResultData rd) {
    int size = map.size();
    if (row < size) {
      List<ScheduleTermWeekContainer> weekTermConList =
        map.get(Integer.valueOf(row));

      viewStartCal.add(Calendar.DATE, 7 * row);

      ScheduleUtils.addTermSchedule(
        weekTermConList,
        viewStartCal.getTime(),
        count,
        rd);

      viewStartCal.add(Calendar.DATE, -7 * row);
    }
  }

  /**
   * 期間スケジュールコンテナを取得します。
   *
   * @param id
   * @return
   */
  public List<ScheduleTermWeekContainer> getWeekList(int id) {
    return map.get(Integer.valueOf(id));
  }

}
