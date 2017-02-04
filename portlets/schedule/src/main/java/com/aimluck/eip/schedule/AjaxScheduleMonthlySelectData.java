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
import java.util.jar.Attributes;

import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;

import com.aimluck.commons.field.ALDateTimeField;
import com.aimluck.eip.cayenne.om.portlet.VEipTScheduleList;
import com.aimluck.eip.common.ALAbstractSelectData;
import com.aimluck.eip.common.ALDBErrorException;
import com.aimluck.eip.common.ALPageNotFoundException;
import com.aimluck.eip.modules.actions.common.ALAction;
import com.aimluck.eip.orm.query.ResultList;
import com.aimluck.eip.schedule.util.ScheduleUtils;
import com.aimluck.eip.util.ALEipUtils;
import com.aimluck.eip.util.ALLocalizationUtils;

/**
 * Widgetsで表示するカレンダーのクラスです。
 *
 */
public class AjaxScheduleMonthlySelectData extends
    ALAbstractSelectData<VEipTScheduleList, VEipTScheduleList> {

  /** <code>viewMonth</code> 今日 */
  private ALDateTimeField today;

  /** <code>viewMonth</code> 現在の月 */
  private ALDateTimeField viewMonth;

  /** <code>monthCon</code> 月間スケジュールコンテナ */
  private ScheduleMonthContainer monthCon;

  /** <code>prevMonth</code> 前の月 */
  private ALDateTimeField prevMonth;

  /** <code>nextMonth</code> 次の月 */
  private ALDateTimeField nextMonth;

  /** <code>nextMonth</code> 表示されている日 */
  private ALDateTimeField viewStart;

  /** <code>startDayOfWeek</code> 週の始まり(月間)　日:1,月:2,火:3,水:4,木:5,金:6,土:7 */
  protected int startDayOfWeek;

  /** <code>weekRevised</code> 月間スケジュール用の週初めの曜日から始まる曜日の文字列のリスト */
  private List<String> weekRevised;

  private final String[] dayOfWeekStr = {
    "",
    ALLocalizationUtils.getl10n("SCHEDULE_SUNDAY"),
    ALLocalizationUtils.getl10n("SCHEDULE_MONDAY"),
    ALLocalizationUtils.getl10n("SCHEDULE_TUSEDAY"),
    ALLocalizationUtils.getl10n("SCHEDULE_WEDNESDAY"),
    ALLocalizationUtils.getl10n("SCHEDULE_THURSDAY"),
    ALLocalizationUtils.getl10n("SCHEDULE_FRIDAY"),
    ALLocalizationUtils.getl10n("SCHEDULE_SATURDAY") };

  /**
   * 現在の月を取得します。
   *
   * @return
   */
  public ALDateTimeField getMonthlyCalendarToday() {
    return today;
  }

  /**
   * 現在の月を取得します。
   *
   * @return
   */
  public ALDateTimeField getMonthlyCalendarViewMonth() {
    return viewMonth;
  }

  /**
   * 月間スケジュールコンテナを取得します。
   *
   * @return
   */
  public ScheduleMonthContainer getMonthlyCalendarContainer() {
    return monthCon;
  }

  /**
   * 前の月を取得します。
   *
   * @return
   */
  public ALDateTimeField getMonthlyCalendarPrevMonth() {
    return prevMonth;
  }

  /**
   * 次の月を取得します。
   *
   * @return
   */
  public ALDateTimeField getMonthlyCalendarNextMonth() {
    return nextMonth;
  }

  /**
   * 現在の月を取得します。
   *
   * @return
   */
  public void setMonthlyCalendarViewMonth(String year, String month) {
    if (month.length() == 1) {
      month = "0" + month;
    }
    viewMonth.setValue(year + "-" + month);
  }

  /**
   * Widgetsで表示する用のカレンダーデータをセットします。
   *
   * @param rundata
   * @param context
   * @throws ALPageNotFoundException
   */
  public void setMonthlyCalendar(RunData rundata, Context context)
      throws ALPageNotFoundException {

    // 自ポートレットからのリクエストであれば、パラメータを展開しセッションに保存する。
    if (ALEipUtils.isMatch(rundata, context)) {
      // スケジュールの表示開始日時
      // e.g. 2004-3-14
      if (rundata.getParameters().containsKey("monthly_calendar_month")) {
        ALEipUtils.setTemp(rundata, context, "monthly_calendar_month", rundata
          .getParameters()
          .getString("monthly_calendar_month"));
      }
    }

    // 現在の月
    if ("".equals(viewMonth.toString())) {
      String tmpViewMonth =
        ALEipUtils.getTemp(rundata, context, "monthly_calendar_month");
      if (tmpViewMonth == null || tmpViewMonth.equals("")) {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DATE, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        viewMonth.setValue(cal.getTime());
      } else {
        viewMonth.setValue(tmpViewMonth);
        if (!viewMonth.validate(new ArrayList<String>())) {
          ALEipUtils.removeTemp(rundata, context, "monthly_calendar_month");
          throw new ALPageNotFoundException();
        }
      }
    } else {
      ALEipUtils.setTemp(rundata, context, "monthly_calendar_month", viewMonth
        .toString());
    }

    // 週の始まり
    String startDayOfWeekStr =
      ALEipUtils
        .getPortlet(rundata, context)
        .getPortletConfig()
        .getInitParameter("z1a-rows");
    if (startDayOfWeekStr != null) {
      startDayOfWeek = Integer.parseInt(startDayOfWeekStr);
    } else {
      // ガラケーの場合 日曜始まりにしておく
      startDayOfWeek = 1;
    }

    // 今日
    Calendar cal = Calendar.getInstance();
    today.setValue(cal.getTime());
    // 表示開始日時
    cal.setTime(viewMonth.getValue());
    // 週の始まり（月間）の設定に応じて表示開始日時を変更する
    shiftCalToMatchStartDayOfWeek(cal, startDayOfWeek);

    // 月間スケジュールコンテナの初期化
    try {
      monthCon = new ScheduleMonthContainer();
      monthCon.initField();
      monthCon.setViewMonth(cal);
    } catch (Exception e) {
      // logger.error("schedule", e);
    }

    // 次の月、前の月
    Calendar cal2 = Calendar.getInstance();
    cal2.setTime(viewMonth.getValue());
    cal2.add(Calendar.MONTH, 1);
    nextMonth.setValue(cal2.getTime());
    cal2.add(Calendar.MONTH, -2);
    prevMonth.setValue(cal2.getTime());

    // 週初めの曜日に合わせて文字列リスト作成
    weekRevised = new ArrayList<String>();
    for (int i = 0; i < Calendar.DAY_OF_WEEK; i++) {
      int dayOfWeek = startDayOfWeek + i;
      if (dayOfWeek > Calendar.DAY_OF_WEEK) {
        dayOfWeek = dayOfWeek % Calendar.DAY_OF_WEEK;
      }
      weekRevised.add(dayOfWeekStr[dayOfWeek]);
    }
  }

  /**
   * 週の始まり（月間）の設定に応じて表示開始日時(cal)を変更する
   *
   * 例：木曜始まりの月(dayofweek=5)で、 週の始まり（月間）の設定が土曜（startDayOfWeek=7）の場合、
   * diff=-5+7=2なので、calを+2ずらせば、土曜始まりの表示開始日時になる。
   * しかし、+2すると今月の一日と二日が表示されなくなるので、diffが正のときには、diff-7=-5ずらす。
   *
   * @param cal
   * @param startDayOfWeek
   *
   */
  protected void shiftCalToMatchStartDayOfWeek(Calendar cal, int startDayOfWeek) {
    int dayofweek = cal.get(Calendar.DAY_OF_WEEK);
    int diff = -dayofweek + startDayOfWeek;
    if (diff > 0) {
      cal.add(Calendar.DATE, diff - 7);
    } else {
      cal.add(Calendar.DATE, diff);
    }
  }

  /**
   *
   */
  @Override
  public void initField() {
    super.initField();
    // 前の月
    prevMonth = new ALDateTimeField("yyyy-MM");
    // 次の月
    nextMonth = new ALDateTimeField("yyyy-MM");
    // 現在の月
    viewMonth = new ALDateTimeField("yyyy-MM");
    viewMonth.setNotNull(true);
    // 今日
    today = new ALDateTimeField("yyyy-MM-dd");
    // 表示開始日時
    viewStart = new ALDateTimeField("yyyy-MM-dd");
    viewStart.setNotNull(true);
  }

  /**
   * @param action
   * @param rundata
   * @param context
   * @throws ALPageNotFoundException
   * @throws ALDBErrorException
   */
  @Override
  public void init(ALAction action, RunData rundata, Context context)
      throws ALPageNotFoundException, ALDBErrorException {
    super.init(action, rundata, context);

    setMonthlyCalendar(rundata, context);

    String tmpViewStart = ALEipUtils.getTemp(rundata, context, "view_start");
    if (tmpViewStart == null || "".equals(tmpViewStart)) {
      Calendar cal = Calendar.getInstance();
      cal.set(Calendar.HOUR_OF_DAY, 0);
      cal.set(Calendar.MINUTE, 0);
      viewStart.setValue(cal.getTime());
    } else {
      viewStart.setValue(tmpViewStart);
    }
  }

  /**
   * @param rundata
   * @param context
   * @return
   * @throws ALPageNotFoundException
   * @throws ALDBErrorException
   */
  @Override
  protected ResultList<VEipTScheduleList> selectList(RunData rundata,
      Context context) throws ALPageNotFoundException, ALDBErrorException {
    return null;
  }

  /**
   * @param rundata
   * @param context
   * @return
   * @throws ALPageNotFoundException
   * @throws ALDBErrorException
   */
  @Override
  protected VEipTScheduleList selectDetail(RunData rundata, Context context)
      throws ALPageNotFoundException, ALDBErrorException {
    return null;
  }

  /**
   * @param obj
   * @return
   * @throws ALPageNotFoundException
   * @throws ALDBErrorException
   */
  @Override
  protected Object getResultData(VEipTScheduleList obj)
      throws ALPageNotFoundException, ALDBErrorException {
    return null;
  }

  /**
   * @param obj
   * @return
   * @throws ALPageNotFoundException
   * @throws ALDBErrorException
   */
  @Override
  protected Object getResultDataDetail(VEipTScheduleList obj)
      throws ALPageNotFoundException, ALDBErrorException {
    return null;
  }

  /**
   * @return
   */
  @Override
  protected Attributes getColumnMap() {
    return null;
  }

  /**
   * @return viewDate
   */
  public ALDateTimeField getViewDate() {
    return viewStart;
  }

  /**
   * 週の始まり(月間)を取得します
   *
   * @return
   */
  public int getStartDayOfWeek() {
    return startDayOfWeek;
  }

  /**
   * 月間スケジュール用の、週初めの曜日から始まる曜日の文字列のリストを取得します
   *
   * @return
   */
  public List<String> getWeekRevised() {
    return weekRevised;
  }

  /**
   * 曜日の文字列（例："木"）から、その曜日が休日かどうかを判定する
   *
   * @param str
   * @return
   */
  public boolean isHoliday(String str) {
    for (int i = 0; i < dayOfWeekStr.length; i++) {
      if (dayOfWeekStr[i].equals(str)) {
        // 日曜の時:0,土曜の時:6を引数とするようにする
        return ScheduleUtils.isUserHoliday(i - 1);
      }
    }
    return false;

  }

}
