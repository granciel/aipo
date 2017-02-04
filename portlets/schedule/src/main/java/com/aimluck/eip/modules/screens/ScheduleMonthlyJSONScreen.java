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
package com.aimluck.eip.modules.screens;

import java.util.Iterator;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.apache.jetspeed.services.logging.JetspeedLogFactoryService;
import org.apache.jetspeed.services.logging.JetspeedLogger;
import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;

import com.aimluck.eip.schedule.AjaxScheduleMonthlySelectData;
import com.aimluck.eip.schedule.ScheduleDayContainer;
import com.aimluck.eip.schedule.ScheduleWeekContainer;

/**
 * カレンダーを処理するクラスです。 <br />
 *
 */
public class ScheduleMonthlyJSONScreen extends ALJSONScreen {

  /** logger */
  private static final JetspeedLogger logger = JetspeedLogFactoryService
    .getLogger(ScheduleMonthlyJSONScreen.class.getName());

  @Override
  protected String getJSONString(RunData rundata, Context context)
      throws Exception {

    JSONObject result = new JSONObject();
    try {
      AjaxScheduleMonthlySelectData listData =
        new AjaxScheduleMonthlySelectData();
      listData.initField();
      listData.setMonthlyCalendar(rundata, context);

      // JSONデータに整形する
      JSONArray month = new JSONArray();
      for (Iterator<ScheduleWeekContainer> it =
        listData.getMonthlyCalendarContainer().getWeekList().iterator(); it
        .hasNext();) {
        JSONArray week = new JSONArray();
        for (Iterator<ScheduleDayContainer> it2 =
          it.next().getDayList().iterator(); it2.hasNext();) {
          ScheduleDayContainer dayContainer = it2.next();
          JSONObject day = new JSONObject();
          day.accumulate("year", dayContainer.getDate().getYear());
          day.accumulate("month", dayContainer.getDate().getMonth());
          day.accumulate("day", dayContainer.getDate().getDay());
          day.accumulate("is_holiday", dayContainer.isHoliday());
          day.accumulate("today", dayContainer.getToday());
          week.add(day.toString());
        }
        month.add(week.toString());
      }
      result.accumulate("monthly_container", month.toString());

      result.accumulate("year", listData
        .getMonthlyCalendarViewMonth()
        .getYear());
      result.accumulate("month", listData
        .getMonthlyCalendarViewMonth()
        .getMonth());
      result.accumulate("next_month", listData
        .getMonthlyCalendarNextMonth()
        .toString());
      result.accumulate("prev_month", listData
        .getMonthlyCalendarPrevMonth()
        .toString());
      result.accumulate("today", listData.getMonthlyCalendarToday().toString());
    } catch (Exception e) {
      logger.error("[ScheduleMonthlyJSONScreen]", e);
    }
    return result.toString();
  }
}
