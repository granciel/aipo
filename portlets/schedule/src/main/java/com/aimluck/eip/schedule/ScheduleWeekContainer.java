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

import com.aimluck.commons.field.ALDateTimeField;
import com.aimluck.eip.common.ALData;
import com.aimluck.eip.schedule.util.ScheduleUtils;

/**
 * 週間スケジュールのコンテナです。
 *
 */
public class ScheduleWeekContainer implements ALData {

  /** <code>dayList</code> スケジュールリスト */
  private List<ScheduleDayContainer> dayList;

  /*
   *
   */
  @Override
  public void initField() {
    dayList = new ArrayList<ScheduleDayContainer>();
  }

  /**
   * 表示開始する日付を設定します。
   *
   * @param cal
   */
  public void setViewStartDate(Calendar cal) {
    for (int i = 1; i <= 7; i++) {
      // 日付を1日ずつずらす
      ScheduleDayContainer con = new ScheduleDayContainer();
      con.initField();
      con.setDate(cal.getTime());
      dayList.add(con);
      cal.add(Calendar.DATE, 1);
    }
  }

  /**
   * スケジュールをコンテナに格納します。
   *
   * @param rd
   */
  public boolean addResultData(ScheduleResultData rd) {
    int size = dayList.size();

    for (int i = 0; i < size; i++) {
      ScheduleDayContainer con = dayList.get(i);
      ALDateTimeField field = con.getDate();
      if (!rd.getPattern().equals("N")) {
        // 繰り返しスケジュール
        if (ScheduleUtils.isView(con.getDate(), rd.getPattern(), rd
          .getStartDate()
          .getValue(), rd.getEndDate().getValue())) {
          Calendar temp = Calendar.getInstance();
          temp.setTime(field.getValue());
          temp
            .set(Calendar.HOUR, Integer.parseInt(rd.getStartDate().getHour()));
          temp.set(Calendar.MINUTE, Integer.parseInt(rd
            .getStartDate()
            .getMinute()));
          temp.set(Calendar.SECOND, 0);
          temp.set(Calendar.MILLISECOND, 0);
          Calendar temp2 = Calendar.getInstance();
          temp2.setTime(field.getValue());
          temp2.set(Calendar.HOUR, Integer.parseInt(rd.getEndDate().getHour()));
          temp2.set(Calendar.MINUTE, Integer.parseInt(rd
            .getEndDate()
            .getMinute()));
          temp2.set(Calendar.SECOND, 0);
          temp2.set(Calendar.MILLISECOND, 0);
          ScheduleResultData rd3 = new ScheduleResultData();
          rd3.initField();
          rd3.setScheduleId((int) rd.getScheduleId().getValue());
          rd3.setParentId((int) rd.getParentId().getValue());
          rd3.setName(rd.getName().getValue());
          // 開始日を設定し直す
          rd3.setStartDate(temp.getTime());
          // 終了日を設定し直す
          rd3.setEndDate(temp2.getTime());
          rd3.setTmpreserve(rd.isTmpreserve());
          rd3.setPublic(rd.isPublic());
          rd3.setHidden(rd.isHidden());
          rd3.setDummy(rd.isDummy());
          rd3.setLoginuser(rd.isLoginuser());
          rd3.setOwner(rd.isOwner());
          rd3.setMember(rd.isMember());
          rd3.setType(rd.getType());
          // 繰り返しはON
          rd3.setRepeat(true);
          con.addResultData(rd3);
        }
      } else if (field.getYear().equals(rd.getStartDate().getYear())
        && field.getMonth().equals(rd.getStartDate().getMonth())
        && field.getDay().equals(rd.getStartDate().getDay())) {
        con.addResultData(rd);
        return true;
      }
    }
    return false;
  }

  /**
   * 期間スケジュールをコンテナに格納します。
   *
   * @param rd
   */

  /*
   * public void addSpanResultData(int index, ScheduleResultData rd) { int span
   * = rd.getRowspan(); ScheduleDayContainer con = (ScheduleDayContainer)
   * dayList.get(index); con.setHasspan(true); con.setSpanResultData(rd); for
   * (int i = 1; i < span; i++) { con = (ScheduleDayContainer) dayList.get(index
   * + i); con.setHasspan(true); } }
   */

  /**
   * スケジュールリストを取得します。
   *
   * @return
   */
  public List<ScheduleDayContainer> getDayList() {
    return dayList;
  }

}
