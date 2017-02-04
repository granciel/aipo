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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.jetspeed.services.logging.JetspeedLogFactoryService;
import org.apache.jetspeed.services.logging.JetspeedLogger;

import com.aimluck.commons.field.ALDateField;
import com.aimluck.commons.field.ALDateTimeField;
import com.aimluck.eip.cayenne.om.portlet.EipTExtTimecard;
import com.aimluck.eip.cayenne.om.portlet.EipTExtTimecardSystem;
import com.aimluck.eip.common.ALData;
import com.aimluck.eip.exttimecard.util.ExtTimecardUtils;

/**
 * タイムカードのResultDataです。 <BR>
 *
 */
public class ExtTimecardListResultData implements ALData {

  /** logger */
  private static final JetspeedLogger logger =
    JetspeedLogFactoryService.getLogger(
      ExtTimecardListResultData.class.getName());

  private ALDateField date = null;

  private ExtTimecardResultData rd;

  private List<ExtTimecardResultData> list = null;

  /** タイムカードの設定 */
  private EipTExtTimecardSystem timecard_system;

  /** 計算済み就業時間 */
  // private float calculated_work_hour = NO_DATA;

  /** 計算済み全込み就業時間 */
  private float calculated_total_work_hour = NO_DATA;

  /** 計算済み残業時間 */
  // private float calculated_overtime_hour = NO_DATA;

  /** 時間データがないことを示す数値 */
  public static final float NO_DATA = -1f;

  /** 特定の日付より前から後かを示す変数 */
  private int beforeafter;

  private float _midnight_regular_work_hour = NO_DATA;

  private float _midnight_overtime_work_hour = NO_DATA;

  private float calculated_agreed_hours = NO_DATA;

  private float week_overtime = NO_DATA;

  private float fix_within_statutory_overtime_work_hour = NO_DATA;

  private float fix_inwork_hour = NO_DATA;

  private float fix_overtime_hour = NO_DATA;

  private boolean isNewRule = false;

  private boolean isStatutoryHoliday = false;

  /**
   *
   *
   */
  @Override
  public void initField() {
    date = new ALDateField();
    date.setValue(new Date());
    list = new ArrayList<ExtTimecardResultData>();
    beforeafter = 0;
  }

  /**
   * 日付を設定します。
   *
   * @param resultdata
   */
  public void setDate(Date date) {
    this.date.setValue(date);
  }

  /**
   * 現在設定されている日付を取得します。
   *
   * @return
   */
  public ALDateField getDate() {
    return date;
  }

  /**
   * タイムカードのResultDataを設定します。
   *
   * @param resultdata
   */
  public void setRd(ExtTimecardResultData resultdata) {
    rd = resultdata;
  }

  /**
   * タイムカードのResultDataを取得します。
   *
   * @param
   */
  public ExtTimecardResultData getRd() {
    return rd;
  }

  /**
   * ResultDataがあるかどうか
   *
   * @param
   */
  public boolean getIsResultData() {
    return (rd != null);
  }

  /**
   * 日付が今日より前であるかどうか
   *
   * @param
   */
  public int getIsBeforeOrAfterToday() {
    try {
      Date today = new Date();
      Date now = date.getValue().getDate();
      if (ExtTimecardUtils.sameDay(now, today)) {
        return 0;
      }
      return now.compareTo(today);
    } catch (Exception e) {
      return 0;
    }
  }

  /**
   * 日付が今日より前であるかどうか、日付変更時間を考慮する
   */
  public int getIsBeforeOrafterTodayAlter() {

    try {

      int change_time = timecard_system.getChangeHour().intValue();
      Date today = new Date();
      long time =
        today.getTime()
          - Long.valueOf(Integer.toString(change_time * 60 * 60 * 1000));
      today.setTime(time);

      Date now = date.getValue().getDate();

      logger.debug(today.toString());
      logger.error(now.toString());

      if (ExtTimecardUtils.sameDay(now, today)) {
        return 0;
      }
      return now.compareTo(today);
    } catch (RuntimeException e) {
      // RuntimeException
      return 0;
    } catch (Exception e) {
      return 0;
    }

  }

  /**
   *
   * @return
   */
  public boolean isHoliday() {
    try {
      Date now = date.getValue().getDate();
      return ExtTimecardUtils.isHoliday(timecard_system, now);
    } catch (Throwable ignore) {
      return false;
    }
  }

  /**
   * 出勤時間が空かどうか
   *
   * @return
   */
  public boolean getIsNotNullClockInTime() {
    if (rd == null) {
      return false;
    }
    if (rd.getIsNullClockInTime()) {
      return false;
    }
    return true;
  }

  /**
   * 退勤時間が空かどうか
   *
   * @return
   */
  public boolean getIsNotNullClockOutTime() {
    if (rd == null) {
      return false;
    }
    if (rd.getIsNullClockOutTime()) {
      return false;
    }
    return true;
  }

  /**
   * 外出／復帰の回数を得ます。
   *
   * @return
   */
  public int getOutgoingComebackTimes() {
    if (rd == null) {
      return 1;
    }
    return Math.max(
      Math.max(rd.getAllOutgoingTime().size(), rd.getAllComebackTime().size()),
      1);
  }

  /**
   * 現在残業中かどうか調べます。
   *
   * @return boolean
   */
  public boolean getIsClockOverTime() {
    if (getIsNotNullClockInTime()) {
      if (!isNewRule()) {
        int end_hour = timecard_system.getEndHour(), end_minute =
          timecard_system.getEndMinute();

        Calendar cal = Calendar.getInstance();

        int now_hour = cal.get(Calendar.HOUR_OF_DAY);
        int now_minute = cal.get(Calendar.MINUTE);

        if (now_hour < end_hour) {
          return false;
        } else if (now_hour == end_hour) {
          if (now_minute < end_minute) {
            return false;
          } else {
            return true;
          }
        } else {
          return true;
        }
      } else {
        // 法定外残業
        Calendar cal = Calendar.getInstance();
        float time = 0f;
        time +=
          (cal.getTime().getTime() - rd.getClockInTime().getValue().getTime())
            / (1000.0 * 60.0 * 60.0);

        /** 外出時間を就業時間に含めない場合 */
        if ("F".equals(timecard_system.getOutgoingAddFlag())) {
          float outgoing_time =
            getOutgoingTime(getChangeDate(), getNextChangeDate());
          if (outgoing_time != NO_DATA) {
            time -= outgoing_time;
          }
        }

        /** 就業時間の中で決まった時間の休憩を取らせます。 */
        /** 決まった時間ごとの休憩時間を取らせます。 */
        float worktimein = (timecard_system.getWorktimeIn() / 60f);
        float resttimein = (timecard_system.getResttimeIn() / 60f);
        if (worktimein != 0F) {
          int resttimes = (int) (time / worktimein);
          time -= resttimes * resttimein;
        }
        float overTime =
          ExtTimecardUtils.getOvertimeMinuteByDay(timecard_system) / 60f;
        if (time >= overTime) {
          return true;
        } else {
          return false;
        }
      }
    }
    return false;
  }

  /**
   * 外出／復帰の配列を得ます。
   *
   * @return
   */
  public List<Map<String, String>> getOutgoingComeback() {
    List<Map<String, String>> result_list =
      new ArrayList<Map<String, String>>();

    Map<String, String> dummymap = new HashMap<String, String>();
    dummymap.put("outgoing", "");
    dummymap.put("comeback", "");
    if (rd == null) {
      result_list.add(dummymap);
    } else {
      List<ALDateTimeField> out = rd.getAllOutgoingTime();
      List<ALDateTimeField> come = rd.getAllComebackTime();
      // ALDateTimeField out[] = rd.getAllOutgoingTime();
      // ALDateTimeField come[] = rd.getAllComebackTime();
      for (int i = 0; i < EipTExtTimecard.OUTGOING_COMEBACK_PER_DAY; i++) {
        boolean isnullout = out.get(i).isNullHour();
        boolean isnullcome = come.get(i).isNullHour();
        if (isnullout && isnullcome) {
          break;
        }
        Map<String, String> map = new HashMap<String, String>();
        map.put("outgoing", isnullout ? "" : out.get(i).getTime());
        map.put("comeback", isnullcome ? "" : come.get(i).getTime());
        result_list.add(map);
      }
      if (result_list.size() == 0) {
        result_list.add(dummymap);
      }
    }
    return result_list;
  }

  /**
   * 外出／復帰の配列を得ます。
   *
   * @return
   */
  public List<Map<String, String>> getOutgoingComeback_xls() {
    List<Map<String, String>> result_list =
      new ArrayList<Map<String, String>>();

    Map<String, String> dummymap = new HashMap<String, String>();
    dummymap.put("outgoing", "");
    dummymap.put("comeback", "");
    if (rd == null) {
      result_list.add(dummymap);
    } else {
      List<ALDateTimeField> out = rd.getAllOutgoingTime();
      List<ALDateTimeField> come = rd.getAllComebackTime();
      SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
      for (int i = 0; i < EipTExtTimecard.OUTGOING_COMEBACK_PER_DAY; i++) {
        boolean isnullout = out.get(i).isNullHour();
        boolean isnullcome = come.get(i).isNullHour();
        if (isnullout && isnullcome) {
          // break;
        }
        String out_str = "", come_str = "";
        try {
          if (!isnullout) {
            out_str = sdf.format(out.get(i).getValue());
          }
          if (!isnullcome) {
            come_str = sdf.format(come.get(i).getValue());
          }
        } catch (Exception e) {
          out_str = "";
          come_str = "";
        }
        Map<String, String> map = new HashMap<String, String>();
        map.put("outgoing", out_str);
        map.put("comeback", come_str);
        result_list.add(map);
      }
      if (result_list.size() == 0) {
        result_list.add(dummymap);
      }
    }
    return result_list;
  }

  @SuppressWarnings("unused")
  private String changeMinute(String minute) {
    if (Integer.parseInt(minute) >= 0 && Integer.parseInt(minute) <= 9) {
      return minute = "0" + minute;
    }
    return minute;
  }

  /**
   * 所定労働時間
   */
  public float getAgreedHours() {
    if (calculated_agreed_hours != NO_DATA) {
      return calculated_agreed_hours;
    }
    Date endDate = getEndDate();
    Date startDate = getStartDate();

    float time = 0f;
    time += (endDate.getTime() - startDate.getTime()) / (1000.0 * 60.0 * 60.0);
    /** 就業時間の中で決まった時間の休憩を取らせます。 */
    /** 決まった時間ごとの休憩時間を取らせます。 */
    float worktimein = (timecard_system.getWorktimeIn() / 60f);
    float resttimein = (timecard_system.getResttimeIn() / 60f);
    if (worktimein != 0F) {
      int resttimes = (int) (time / worktimein);
      time -= resttimes * resttimein;
    }
    calculated_agreed_hours = time;
    return time;
  }

  /**
   * 所定内勤務時間（平日） 就業時間
   *
   * @return float
   */
  public float getWorkHour() {
    if (!getIsNotNullClockInTime() || !getIsNotNullClockOutTime()) {
      return NO_DATA;
    } else {
      float time = 0f;
      float in = getInworkHour(); // 所定内勤務時間
      if (!isHoliday()) {
        if (in != NO_DATA) {
          time += in;
        }
      }
      return time;
    }
  }

  /**
   * 所定内勤務時間（平日） 就業時間（表示用）
   *
   * @return float
   */
  public String getWorkHourValue() {
    if (isHoliday()) {
      return "";
    }
    float value = getWorkHour();
    if (value != NO_DATA) {
      return String.valueOf(ExtTimecardUtils.roundHour(value));
    }
    return "";
  }

  /**
   * 総労働時間（所定内勤務時間 + 残業時間 + 法内残業時間）
   *
   * @return float
   */
  public float getTotalWorkHour() {
    if (!getIsNotNullClockInTime() || !getIsNotNullClockOutTime()) {
      return NO_DATA;
    } else if (calculated_total_work_hour != NO_DATA) {
      return calculated_total_work_hour;
    } else {
      float time = 0f;
      float in = getInworkHour();// 所定内勤務時間
      float over = getOvertimeHour();// 残業時間
      if (in != NO_DATA) {
        time += in;
      }
      if (over != NO_DATA) {
        time += over;
      }
      if (isNewRule()) {
        float etc = getWithinStatutoryOvertimeWorkHour(); // 法内残業時間
        if (etc != NO_DATA) {
          time += etc;
        }
      }
      calculated_total_work_hour = time;
      return time;
    }
  }

  /**
   * 総労働時間（表示用）
   *
   * @return
   */
  public String getTotalWorkHourValue() {
    float value = getTotalWorkHour();
    if (value != NO_DATA) {
      return String.valueOf(ExtTimecardUtils.roundHour(value));
    }
    return "";
  }

  /**
   * 所定内勤務時間（平日、休日）
   *
   * @return float
   */
  public float getInworkHour() {
    if (fix_inwork_hour != NO_DATA) {
      return fix_inwork_hour;
    }
    if (!getIsNotNullClockInTime() || !getIsNotNullClockOutTime()) {
      return -1f;
    } else {

      float time = 0f;
      time +=
        (rd.getClockOutTime().getValue().getTime()
          - rd.getClockInTime().getValue().getTime()) / (1000.0 * 60.0 * 60.0);

      // 就業時間だけなので、残業を引く
      float agreedHours = 0f;
      if (!isNewRule()) {
        Date start_date = getStartDate(), end_date = getEndDate();
        long start_time = start_date.getTime(), end_time = end_date.getTime();
        /** 早出残業 */
        if (rd.getClockInTime().getValue().getTime() < start_time) {
          agreedHours += start_time - rd.getClockInTime().getValue().getTime();
          if (rd.getClockOutTime().getValue().getTime() < start_time) {
            agreedHours -=
              start_time - rd.getClockOutTime().getValue().getTime();
          }
        }
        /** 残業 */
        if (end_time < rd.getClockOutTime().getValue().getTime()) {
          agreedHours += rd.getClockOutTime().getValue().getTime() - end_time;
          if (end_time < rd.getClockInTime().getValue().getTime()) {
            agreedHours -= rd.getClockInTime().getValue().getTime() - end_time;
          }
        }
        agreedHours /= (1000.0 * 60.0 * 60.0);
        time -= agreedHours;

        /** 外出時間を就業時間に含めない場合 */
        if ("F".equals(timecard_system.getOutgoingAddFlag())) {
          float outgoing_time = getOutgoingTime(getStartDate(), getEndDate());
          if (outgoing_time != NO_DATA) {
            time -= outgoing_time;
          } else {
            return NO_DATA;
          }
        }

        /** 就業時間の中で決まった時間の休憩を取らせます。 */
        /** 決まった時間ごとの休憩時間を取らせます。 */
        float worktimein = (timecard_system.getWorktimeIn() / 60f);
        float resttimein = (timecard_system.getResttimeIn() / 60f);
        if (worktimein == 0F) {
          return time;
        }
        int resttimes = (int) (time / worktimein);
        return time - resttimes * resttimein;
      } else {
        // 法定外残業の場合 就業時間の合計が決められた残業時間以上の場合 残業時間を返す
        /** 外出時間を就業時間に含めない場合 */
        if ("F".equals(timecard_system.getOutgoingAddFlag())) {
          float outgoing_time =
            getOutgoingTime(getChangeDate(), getNextChangeDate());
          if (outgoing_time != NO_DATA) {
            time -= outgoing_time;
          }
        }

        /** 就業時間の中で決まった時間の休憩を取らせます。 */
        /** 決まった時間ごとの休憩時間を取らせます。 */
        float worktimein = (timecard_system.getWorktimeIn() / 60f);
        float resttimein = (timecard_system.getResttimeIn() / 60f);
        if (worktimein != 0F) {
          int resttimes = (int) (time / worktimein);
          time -= resttimes * resttimein;
        }

        agreedHours = this.getAgreedHours();
        if (time >= agreedHours) {
          return agreedHours;
        } else {
          return time;
        }
      }
    }
  }

  public float getWorkHourWithoutRestHour(boolean round) {
    float time = getWorkHour();
    if (round) {
      time = ExtTimecardUtils.roundHour(time);
    }
    return time;
  }

  /**
   * 残業時間
   *
   * @return
   */
  public float getOvertimeHour() {
    if (fix_overtime_hour != NO_DATA) {
      return fix_overtime_hour;
    }
    if (!getIsNotNullClockInTime() || !getIsNotNullClockOutTime()) {
      return NO_DATA;
    } else {
      float time = 0f;
      Date start_date = getStartDate(), end_date = getEndDate(), change_date =
        getChangeDate(), nextchange_date = getNextChangeDate();
      if (!isNewRule()) {
        long start_time = start_date.getTime(), end_time = end_date.getTime();
        /** 早出残業 */
        if (rd.getClockInTime().getValue().getTime() < start_time) {
          time += start_time - rd.getClockInTime().getValue().getTime();
          if (rd.getClockOutTime().getValue().getTime() < start_time) {
            time -= start_time - rd.getClockOutTime().getValue().getTime();
          }
        }

        /** 残業 */
        if (end_time < rd.getClockOutTime().getValue().getTime()) {
          time += rd.getClockOutTime().getValue().getTime() - end_time;
          if (end_time < rd.getClockInTime().getValue().getTime()) {
            time -= rd.getClockInTime().getValue().getTime() - end_time;
          }
        }
        time /= (1000.0 * 60.0 * 60.0);

        /** 外出時間を残業時間に含めない場合 */
        if ("F".equals(timecard_system.getOutgoingAddFlag())) {
          float outgoing_time;
          outgoing_time = getOutgoingTime(change_date, start_date);
          if (outgoing_time != NO_DATA) {
            time -= outgoing_time;
          }
          outgoing_time = getOutgoingTime(end_date, nextchange_date);
          if (outgoing_time != NO_DATA) {
            time -= outgoing_time;
          }
        }

        /** 就業時間の中で決まった時間の休憩を取らせます。 */
        /** 決まった時間ごとの休憩時間を取らせます。 */
        float worktimeout = (timecard_system.getWorktimeOut() / 60f);
        float resttimeout = (timecard_system.getResttimeOut() / 60f);
        if (worktimeout == 0F) {// 0の時は休憩なし？
          return time;
        }
        int resttimes = (int) (time / worktimeout);
        return time - resttimes * resttimeout;
      } else {
        // 法定外残業
        time +=
          (rd.getClockOutTime().getValue().getTime()
            - rd.getClockInTime().getValue().getTime())
            / (1000.0 * 60.0 * 60.0);

        /** 外出時間を就業時間に含めない場合 */
        if ("F".equals(timecard_system.getOutgoingAddFlag())) {
          float outgoing_time =
            getOutgoingTime(getChangeDate(), getNextChangeDate());
          if (outgoing_time != NO_DATA) {
            time -= outgoing_time;
          }
        }

        /** 就業時間の中で決まった時間の休憩を取らせます。 */
        /** 決まった時間ごとの休憩時間を取らせます。 */
        /** 法定外残業は就業内の休憩の設定 */
        float worktimein = (timecard_system.getWorktimeOut() / 60f);
        float resttimein = (timecard_system.getResttimeOut() / 60f);
        if (worktimein != 0F) {
          int resttimes = (int) (time / worktimein);
          time -= resttimes * resttimein;
        }
        float overTime =
          ExtTimecardUtils.getOvertimeMinuteByDay(timecard_system) / 60f;
        if (time >= overTime) {
          return time - overTime;
        } else {
          return 0f;
        }
      }
    }
  }

  /**
   * 残業時間（平日）
   *
   * @return float
   */
  public float getOvertimeHourWithoutRestHour() {
    return getOvertimeHourWithoutRestHour(false);
  }

  /**
   * 残業時間（平日）
   *
   * @param round
   * @return
   */
  public float getOvertimeHourWithoutRestHour(boolean round) {
    float time = getOvertimeHour();
    if (round) {
      time = ExtTimecardUtils.roundHour(time);
    }
    // 休日を除外
    if (isHoliday()) {
      if (time != NO_DATA) {
        time = 0f;
      }
    }
    return time;
  }

  /**
   * 残業時間（平日）（表示用）
   *
   * @param round
   * @return
   */
  public String getOvertimeHourValue() {
    if (isHoliday()) {
      return "";
    }
    float value = getOvertimeHour();
    if (value != NO_DATA) {
      return String.valueOf(ExtTimecardUtils.roundHour(value));
    }
    return "";
  }

  /**
   * 所定休日労働時間
   *
   * @return
   */
  public float getTotalOfficialOffHour() {
    if (isHoliday()) {
      if (!isStatutoryHoliday()) {
        return getTotalWorkHour();
      }
    }
    return NO_DATA;
  }

  /**
   * 所定休日労働時間（表示用）
   *
   * @return
   */
  public String getTotalOfficialOffHourValue() {
    float value = getTotalOfficialOffHour();
    if (value != NO_DATA) {
      return String.valueOf(ExtTimecardUtils.roundHour(value));
    }
    return "";
  }

  /**
   * 法定休日労働時間
   *
   * @return
   */
  public float getTotalStatutoryOffHour() {
    if (isHoliday()) {
      if (isStatutoryHoliday()) {
        return getTotalWorkHour();
      }
    }
    return NO_DATA;
  }

  /**
   * 法定休日労働時間（表示用）
   *
   * @return
   */
  public String getTotalStatutoryOffHourValue() {
    float value = getTotalStatutoryOffHour();
    if (value != NO_DATA) {
      return String.valueOf(ExtTimecardUtils.roundHour(value));
    }
    return "";
  }

  /**
   * 休出時間
   *
   * @return
   */
  public float getOffHour() {
    return getOffHour(false);
  }

  /**
   * 休出時間
   *
   * @param round
   * @return
   */
  public float getOffHour(boolean round) {
    if (isHoliday()) {
      float time = NO_DATA;
      if (calculated_total_work_hour != NO_DATA) {
        time = calculated_total_work_hour;
      } else {
        time = getTotalWorkHour();
      }
      if (round) {
        time = ExtTimecardUtils.roundHour(time);
      }
      return time;
    }
    return NO_DATA;
  }

  /**
   * 休憩時間
   *
   * @return float
   */
  public float getRestHour() {
    if (!getIsNotNullClockInTime() || !getIsNotNullClockOutTime()) {
      return NO_DATA;
    } else {
      float time = 0f;
      time +=
        (rd.getClockOutTime().getValue().getTime()
          - rd.getClockInTime().getValue().getTime()) / (1000.0 * 60.0 * 60.0);
      /** 外出時間を就業時間に含めない場合 */
      if ("F".equals(timecard_system.getOutgoingAddFlag())) {
        float outgoing_time =
          getOutgoingTime(getChangeDate(), getNextChangeDate());
        if (outgoing_time != NO_DATA) {
          time -= outgoing_time;
        }
      }
      float total = getTotalWorkHour();
      if (time > total) {
        return time - total;
      } else {
        return 0f;
      }
    }
  }

  /**
   * その日遅刻したかどうか
   *
   * @return boolean
   */
  public boolean isLateComing() {
    if (!getIsNotNullClockInTime() || !getIsNotNullClockOutTime()) {
      return false;
    }
    Date start_date = getStartDate();
    if (rd.getClockInTime().getValue().after(start_date)) {
      return true;
    }
    return false;
  }

  /**
   * その日早退したかどうか
   *
   * @return boolean
   */
  public boolean isEarlyLeaving() {
    if (!getIsNotNullClockInTime() || !getIsNotNullClockOutTime()) {
      return false;
    }
    Date end_date = getEndDate();
    if (rd.getClockOutTime().getValue().before(end_date)) {
      return true;
    }
    return false;
  }

  /**
   * その日欠勤したかどうか
   *
   * @return boolean
   */
  public boolean isAbsent() {
    return false;
  }

  /**
   *
   *
   */
  public String getHourToString(float time) {
    if (time == NO_DATA) {
      return "";
    }
    return time + "h";
  }

  /**
   * 日付を取得します。
   *
   * @return
   */
  public String getDateStr() {
    try {
      SimpleDateFormat sdf = new SimpleDateFormat("M月d日(EE)");
      return sdf.format(date.getValue().getDate());
    } catch (Exception e) {
      return "";
    }
  }

  /**
   * 日付を取得します。
   *
   * @return
   */
  public String getDateStr(String str) {
    try {
      if (str == null || "".equals(str)) {
        str = "yyyy年MM月dd日(EE)";
      }
      SimpleDateFormat sdf = new SimpleDateFormat(str);
      return sdf.format(date.getValue().getDate());
    } catch (RuntimeException e) {
      // RuntimeException
      return "";
    } catch (Exception e) {
      return "";
    }
  }

  /**
   * タイムカードの設定を取得します。
   *
   * @return
   */
  public EipTExtTimecardSystem getTimecardSystem() {
    return timecard_system;
  }

  /**
   * タイムカードの設定を読み込みます。
   *
   * @return
   */
  public void setTimecardSystem(EipTExtTimecardSystem system) {
    timecard_system = system;
  }

  public Date getClockInDate() {
    return rd.getClockInTime().getValue();
  }

  public List<ExtTimecardResultData> getList() {
    return list;
  }

  public List<ExtTimecardResultData> getViewList() {
    List<ExtTimecardResultData> viewlist =
      new ArrayList<ExtTimecardResultData>();

    // ExtTimecardResultData rd = null;
    //
    // int size = list.size();
    // for (int i = 0; i < size; i++) {
    // rd = (ExtTimecardResultData) list.get(i);
    //
    // if (!ExtTimecardUtils.WORK_FLG_DUMMY.equals(rd.getWorkFlag().getValue()))
    // {
    // viewlist.add(rd);
    // }
    // }

    return viewlist;
  }

  public void addExtTimecardResultData(ExtTimecardResultData rd) {
    list.add(rd);
  }

  public String getSummayTimes() {
    int size = list.size();

    if (size < 1) {
      return "0";
    }
    return "0";
  }

  /**
   * 始業時間を取得します。
   *
   * @return
   */
  protected Date getStartDate() {
    int start_hour = timecard_system.getStartHour(), start_minute =
      timecard_system.getStartMinute();
    Calendar cal = Calendar.getInstance();
    try {
      cal.setTime(rd.getPunchDate().getValue());
    } catch (Exception e) {
    }
    cal.set(Calendar.HOUR_OF_DAY, start_hour);
    cal.set(Calendar.MINUTE, start_minute);
    cal.set(Calendar.SECOND, 0);
    cal.set(Calendar.MILLISECOND, 0);

    if (start_hour >= 0 && start_hour < timecard_system.getChangeHour()) {
      cal.add(Calendar.DAY_OF_MONTH, 1);
    }
    return cal.getTime();
  }

  /**
   * 就業時間を取得します。
   *
   * @return
   */
  protected Date getEndDate() {
    int end_hour = timecard_system.getEndHour(), end_minute =
      timecard_system.getEndMinute();
    Calendar cal = Calendar.getInstance();
    try {
      cal.setTime(rd.getPunchDate().getValue());
    } catch (Exception e) {
    }
    cal.set(Calendar.HOUR_OF_DAY, end_hour);
    cal.set(Calendar.MINUTE, end_minute);
    cal.set(Calendar.SECOND, 0);
    cal.set(Calendar.MILLISECOND, 0);

    if (end_hour >= 0 && end_hour < timecard_system.getChangeHour()) {
      cal.add(Calendar.DAY_OF_MONTH, 1);
    }
    return cal.getTime();
  }

  protected Date getChangeDate() {
    int change_hour = timecard_system.getChangeHour();
    Calendar cal = Calendar.getInstance();
    try {
      cal.setTime(rd.getPunchDate().getValue());
    } catch (Exception e) {
    }
    cal.set(Calendar.HOUR_OF_DAY, change_hour);
    cal.set(Calendar.MINUTE, 0);
    cal.set(Calendar.SECOND, 0);
    cal.set(Calendar.MILLISECOND, 0);

    return cal.getTime();
  }

  protected Date getNextChangeDate() {
    int change_hour = timecard_system.getChangeHour();
    Calendar cal = Calendar.getInstance();
    try {
      cal.setTime(rd.getPunchDate().getValue());
    } catch (Exception e) {
    }
    cal.add(Calendar.DATE, 1);
    cal.set(Calendar.HOUR_OF_DAY, change_hour);
    cal.set(Calendar.MINUTE, 0);
    cal.set(Calendar.SECOND, 0);
    cal.set(Calendar.MILLISECOND, 0);

    return cal.getTime();
  }

  /**
   * 特定の時間中に含まれる外出時間を計算します。
   *
   * @param from_date
   * @param to_date
   * @return
   */
  protected float getOutgoingTime(Date from_date, Date to_date) {
    long outgoing_time = 0;
    int comeback_num = 0, outgoing_num = 0, from_num = -1, to_num = -1;
    if (from_date.getTime() > to_date.getTime()) {
      return 0.0f;
    }
    for (ALDateTimeField field : rd.getAllComebackTime()) {
      if (field.isNullHour()) {
        break;
      }
      comeback_num++;
      /** 勤務時間より前だった場合は無視。はじめて勤務時間より後になった番号を記録する */
      if (field.getValue().getTime() < from_date.getTime()) {
        continue;
      } else if (from_num == -1) {
        from_num = comeback_num;
      }
      /** 勤務時間より後だった場合は番号を記録する */
      if (field.getValue().getTime() > to_date.getTime()) {
        to_num = comeback_num;
        outgoing_time += to_date.getTime();
        continue;
      }
      outgoing_time += field.getValue().getTime();
    }

    /** 外出時間がすべて勤務時間より前だった場合は、外出時間は0とする */
    if (from_num != -1) {
      for (ALDateTimeField field : rd.getAllOutgoingTime()) {
        if (field.isNullHour()) {
          break;
        }
        outgoing_num++;
        if (outgoing_num < from_num) {
          continue;
        }
        if (outgoing_num == from_num) {
          if (field.getValue().getTime() < from_date.getTime()) {
            outgoing_time -= from_date.getTime();
            continue;
          }
        }
        if (field.getValue().getTime() > to_date.getTime()) {
          outgoing_time -= to_date.getTime();
          continue;
        }
        outgoing_time -= field.getValue().getTime();
        if (outgoing_num == to_num) {
          break;
        }
      }
      if (comeback_num == outgoing_num) {
        return (float) outgoing_time / (1000 * 60 * 60);
      } else {
        /** 外出／復帰が対応しないため計算できない */
        return NO_DATA;
      }
    }
    return 0.0f;
  }

  public void setBeforeAfter() {
    try {

      if (timecard_system != null) {
        int change_time = timecard_system.getChangeHour().intValue();
        Date today = new Date();
        long time =
          today.getTime()
            - Long.valueOf(Integer.toString(change_time * 60 * 60 * 1000));
        today.setTime(time);

        Date now = date.getValue().getDate();

        if (ExtTimecardUtils.sameDay(now, today)) {
          beforeafter = 0;
          return;
        }
        beforeafter = now.compareTo(today);
      } else {
        beforeafter = 0;
      }
    } catch (Exception e) {
      beforeafter = 0;
      logger.error("error", e);
    }
  }

  public int getBeforeAfter() {
    return beforeafter;
  }

  /**
   * その日の遅刻時間。遅刻していない場合は0。
   *
   * @return boolean
   */
  public float getLateComingHour() {
    if (!isLateComing()) {
      return 0;
    } else {
      Date start_date = getStartDate();
      float time = 0f;
      time +=
        (getRd().getClockInTime().getValue().getTime() - start_date.getTime())
          / (1000.0 * 60.0 * 60.0);

      return time;
    }
  }

  /**
   * その日の早退時間。早退していない場合は0。
   *
   * @return boolean
   */
  public float getEarlyLeavingHour() {
    if (!isEarlyLeaving()) {
      return 0;
    } else {
      Date end_date = getEndDate();
      float time = 0f;
      time +=
        (end_date.getTime() - getRd().getClockInTime().getValue().getTime())
          / (1000.0 * 60.0 * 60.0);

      return time;
    }
  }

  private Date dateOf(int hour, int minute) {
    Calendar cal = Calendar.getInstance();

    try {
      cal.setTime(getDate().getValue().getDate());
    } catch (Exception e) {
    }

    cal.set(Calendar.HOUR_OF_DAY, hour);
    cal.set(Calendar.MINUTE, minute);
    return cal.getTime();
  }

  private Date nextDateOf(int hour, int minute) {
    Calendar cal = Calendar.getInstance();

    try {
      cal.setTime(getDate().getValue().getDate());
    } catch (Exception e) {
    }

    cal.set(Calendar.HOUR_OF_DAY, hour);
    cal.set(Calendar.MINUTE, minute);
    cal.set(Calendar.DATE, cal.get(Calendar.DATE) + 1);
    return cal.getTime();
  }

  /**
   * 通常業務を開始した時刻を返却します。 残業計算のタイプによって返す値が異なります。
   * <dl>
   * <dt>所定勤務時間外の労働をすべて時間外労働とする計算方式の場合</dt>
   * <dd>所定勤務開始時間または実際の始業時間のいずれか遅い方の時間。</dd>
   * <dt>所定労働時間を超えた労働時間を時間外とする計算方式の場合</dt>
   * <dd>実際の始業時間。</dd>
   * </dl>
   *
   * @return
   */
  public Date getRoundedInDate() {
    Date actual = getRd().getClockInTime().getValue();
    if (!isNewRule()) {
      Date standard = getStartDate();
      if (actual.after(standard)) {
        return actual;
      } else {
        return standard;
      }
    } else {
      return actual;
    }
  }

  /**
   * 通常業務が終了した時刻を返却します。 残業計算のタイプによって返す値が異なります。
   * <dl>
   * <dt>所定勤務時間外の労働をすべて時間外労働とする計算方式の場合</dt>
   * <dd>所定勤務終了時間、または実際の終業時間のいずれか早い方の時間。</dd>
   * <dt>所定労働時間を超えた労働時間を時間外とする計算方式の場合</dt>
   * <dd>実際の就業時間、または実際の始業時間から数えて労働時間が所定労働時間に達した時間のいずれか早い方の時間。</dd>
   * </dl>
   *
   * @return
   */
  public Date getRoundedOutDate() {
    Date actual = getRd().getClockOutTime().getValue();
    if (!isNewRule()) {
      Date standard = getEndDate();
      if (actual.before(standard)) {
        return actual;
      } else {
        return standard;
      }
    } else {
      float time = 0f;
      Date from = getRoundedInDate();
      int overTime = ExtTimecardUtils.getOvertimeMinuteByDay(timecard_system);
      int weekOvertimeMin = (int) (week_overtime * 60);
      if (week_overtime != NO_DATA) {
        if (overTime > weekOvertimeMin) {
          overTime = overTime - weekOvertimeMin;
        } else {
          overTime = 0;
        }
      }
      Calendar cal = Calendar.getInstance();
      cal.setTime(from);
      cal.add(Calendar.MINUTE, overTime);
      Date to = cal.getTime();

      float add = 0f;
      float worktimein = (timecard_system.getWorktimeIn() / 60f);
      while (true) {
        add = 0f;
        time = (to.getTime() - from.getTime()) / (float) (60 * 60 * 1000);
        if (worktimein != 0F) {
          float resttimein = (timecard_system.getResttimeIn() / 60f);
          int resttimes = (int) (time / worktimein);
          float rest = resttimein * resttimes;
          if (rest > 0) {
            add += rest;
          }
        }
        if (getTimecardSystem().getOutgoingAddFlag().equals("F")) {
          float outGoing = getOutgoingTime(from, to);
          if (outGoing > 0) {
            add += outGoing;
          }
        }
        if (add <= 0) {
          break;
        }
        cal.add(Calendar.MILLISECOND, (int) (add * 60 * 60 * 1000));
        from.setTime(to.getTime());
        to = cal.getTime();
      }

      if (to.before(actual)) {
        return to;
      } else {
        return actual;
      }
    }
  }

  /**
   * 深夜労働時間
   *
   * @return
   */
  public float getMidnightWorkHour() {
    if (!getIsNotNullClockInTime() || !getIsNotNullClockOutTime()) {
      return NO_DATA;
    }
    Date from = getRd().getClockInTime().getValue();
    Date to = getRd().getClockOutTime().getValue();
    Date midTimeEarly = dateOf(5, 0);
    Date midTimeLate = dateOf(22, 0);
    Date nextEarly = nextDateOf(5, 0);

    long early = 0;
    if (from.before(midTimeEarly)) {
      early = midTimeEarly.getTime() - from.getTime();
    }
    long late = 0;
    if (to.after(midTimeLate)) {
      if (to.before(nextEarly)) {
        late = to.getTime() - midTimeLate.getTime();
      } else {
        late = nextEarly.getTime() - midTimeLate.getTime();
      }
    }
    float time = 0f;
    if (early + late > 0) {
      time += (early + late) / (1000.0 * 60.0 * 60.0);

      /** 外出時間を就業時間に含めない場合 */
      if (getTimecardSystem().getOutgoingAddFlag().equals("F")) {
        // 外出時間が深夜にかかる時間帯のみ差し引く
        if (from.before(midTimeEarly)) {
          float e = getOutgoingTime(from, midTimeEarly);
          if (e != NO_DATA) {
            time -= e;
          }
        }
        if (midTimeLate.before(to)) {
          float l = NO_DATA;
          if (to.before(nextEarly)) {
            l = getOutgoingTime(midTimeLate, to);
          } else {
            l = getOutgoingTime(midTimeLate, nextEarly);
          }
          if (l != NO_DATA) {
            time -= l;
          }
        }
      }
      return time;
    }
    return 0f;
  }

  /**
   * 深夜労働時間（表示用）
   *
   * @return
   */
  public String getMidnightWorkHourValue() {
    float value = getMidnightWorkHour();
    if (value != NO_DATA) {
      return String.valueOf(ExtTimecardUtils.roundHour(value));
    }
    return "";
  }

  /**
   * 所定内深夜出勤時間
   *
   * @return
   */
  public float getMidnightRegularWorkHour() {
    if (!getIsNotNullClockInTime() || !getIsNotNullClockOutTime()) {
      return NO_DATA;
    }
    if (_midnight_regular_work_hour != NO_DATA) {
      return _midnight_regular_work_hour;
    } else {
      Date from = getRoundedInDate();
      Date to = getRoundedOutDate();
      Date midTimeEarly = dateOf(5, 0);
      Date midTimeLate = dateOf(22, 0);
      Date nextEarly = nextDateOf(5, 0);

      long early = 0;
      if (from.before(midTimeEarly)) {
        // from ~ midTimeEarlyまでの時間は深夜業務時間に含まれる
        early = midTimeEarly.getTime() - from.getTime();
      }
      long late = 0;
      if (to.after(midTimeLate)) {
        // midTimeLate ~ toまでの時間は深夜業務時間に含まれる。
        if (to.before(nextEarly)) {
          late = to.getTime() - midTimeLate.getTime();
        } else {
          late = nextEarly.getTime() - midTimeLate.getTime();
        }
      }
      float time = 0f;
      if (early + late > 0) {
        time += (early + late) / (1000.0 * 60.0 * 60.0);

        /** 外出時間を就業時間に含めない場合 */
        if (getTimecardSystem().getOutgoingAddFlag().equals("F")) {
          if (from.before(midTimeEarly)) {
            float e = getOutgoingTime(getStartDate(), midTimeEarly);
            if (e != NO_DATA) {
              time -= e;
            }
          }
          if (midTimeLate.before(to)) {
            float l = NO_DATA;
            if (to.before(nextEarly)) {
              l = getOutgoingTime(midTimeLate, getEndDate());
            } else {
              l = getOutgoingTime(midTimeLate, nextEarly);
            }
            if (l != NO_DATA) {
              time -= l;
            }
          }
        }

        _midnight_regular_work_hour = time;
      } else {
        _midnight_regular_work_hour = 0f;
      }
      return _midnight_regular_work_hour;
    }
  }

  /**
   * 深夜残業時間
   *
   * @return
   */
  public float getMidnightOvertimeWorkHour() {
    if (!getIsNotNullClockInTime() || !getIsNotNullClockOutTime()) {
      return NO_DATA;
    }
    if (_midnight_overtime_work_hour == NO_DATA) {
      Date from = getRd().getClockInTime().getValue();
      Date to = getRd().getClockOutTime().getValue();
      Date midTimeEarly = dateOf(5, 0);
      Date midTimeLate = dateOf(22, 0);
      Date nextEarly = nextDateOf(5, 0);

      long early = 0;
      if (from.before(midTimeEarly)) {
        early = midTimeEarly.getTime() - from.getTime();
      }
      long late = 0;
      if (to.after(midTimeLate)) {
        if (to.before(nextEarly)) {
          late = to.getTime() - midTimeLate.getTime();
        } else {
          late = nextEarly.getTime() - midTimeLate.getTime();
        }
      }
      float time = 0f;
      if (early + late > 0) {
        time += (early + late) / (1000.0 * 60.0 * 60.0);

        /** 外出時間を就業時間に含めない場合 */
        if (getTimecardSystem().getOutgoingAddFlag().equals("F")) {
          // 外出時間が深夜にかかる時間帯のみ差し引く
          if (from.before(midTimeEarly)) {
            float e = getOutgoingTime(from, midTimeEarly);
            if (e != NO_DATA) {
              time -= e;
            }
          }
          if (midTimeLate.before(to)) {
            float l = NO_DATA;
            if (to.before(nextEarly)) {
              l = getOutgoingTime(midTimeLate, to);
            } else {
              l = getOutgoingTime(midTimeLate, nextEarly);
            }
            if (l != NO_DATA) {
              time -= l;
            }
          }
        }

        // 深夜業務時間から所定内深夜出勤時間を除く
        _midnight_overtime_work_hour = time - getMidnightRegularWorkHour();
      } else {
        _midnight_overtime_work_hour = 0f;
      }
    }
    return _midnight_overtime_work_hour;
  }

  /**
   * 法定内残業時間（平日）
   *
   * @return
   */
  public float getWithinStatutoryOvertimeWorkHourWithoutOffday() {
    if (!getIsNotNullClockInTime() || !getIsNotNullClockOutTime()) {
      return NO_DATA;
    }
    if (!isNewRule()) {
      return NO_DATA;
    }
    if (isHoliday()) {
      return 0f;
    }
    return getWithinStatutoryOvertimeWorkHour();
  }

  /**
   * 法定内残業時間
   *
   * @return
   */
  public float getWithinStatutoryOvertimeWorkHour() {
    if (fix_within_statutory_overtime_work_hour != NO_DATA) {
      return fix_within_statutory_overtime_work_hour;
    }
    if (!getIsNotNullClockInTime() || !getIsNotNullClockOutTime()) {
      return NO_DATA;
    }
    if (!isNewRule()) {
      return NO_DATA;
    }
    float time = 0f;
    time +=
      (rd.getClockOutTime().getValue().getTime()
        - rd.getClockInTime().getValue().getTime()) / (1000.0 * 60.0 * 60.0);

    /** 外出時間を就業時間に含めない場合 */
    if ("F".equals(timecard_system.getOutgoingAddFlag())) {
      float outgoing_time =
        getOutgoingTime(getChangeDate(), getNextChangeDate());
      if (outgoing_time != NO_DATA) {
        time -= outgoing_time;
      }
    }

    /** 就業時間の中で決まった時間の休憩を取らせます。 */
    /** 決まった時間ごとの休憩時間を取らせます。 */
    float worktimein = (timecard_system.getWorktimeIn() / 60f);
    float resttimein = (timecard_system.getResttimeIn() / 60f);
    if (worktimein != 0F) {
      int resttimes = (int) (time / worktimein);
      time -= resttimes * resttimein;
    }
    float tmp1 = time - getAgreedHours();
    if (tmp1 <= 0) {
      return 0f;
    }
    float overTime =
      ExtTimecardUtils.getOvertimeMinuteByDay(timecard_system) / 60f;
    if (time < overTime) {
      return tmp1;
    } else {
      float tmp3 = overTime - getAgreedHours();
      if (tmp3 > 0) {
        return tmp3;
      } else {
        return 0f;
      }
    }
  }

  /**
   * 法定内残業時間（平日）
   *
   * @param round
   * @return
   */
  public float getWithinStatutoryOvertimeWorkHourWithoutOffday(boolean round) {
    float time = getWithinStatutoryOvertimeWorkHourWithoutOffday();
    if (round) {
      time = ExtTimecardUtils.roundHour(time);
    }
    return time;
  }

  /**
   * 法定内残業時間
   *
   * @return
   */
  public float getWithinStatutoryOvertimeWorkHour(boolean round) {
    float time = getWithinStatutoryOvertimeWorkHour();
    if (round) {
      time = ExtTimecardUtils.roundHour(time);
    }
    return time;
  }

  /**
   * @return week_overtime
   */
  public float getWeekOvertime() {
    return week_overtime;
  }

  /**
   * @param week_overtime
   *          セットする week_overtime
   */
  public void setWeekOvertime(float week_overtime) {
    this.week_overtime = week_overtime;
  }

  public void calculateWeekOvertime() {
    if (week_overtime == NO_DATA) {
      return;
    }
    if (!isNewRule()) {
      return;
    }
    float inworkHour = getInworkHour();
    float withinStatutoryOvertimeWorkHour =
      getWithinStatutoryOvertimeWorkHour();
    float overTimeHour = getOvertimeHour();
    if (inworkHour == NO_DATA || withinStatutoryOvertimeWorkHour == NO_DATA) {
      return;
    }
    float time = week_overtime;
    if (week_overtime > withinStatutoryOvertimeWorkHour) {
      time = week_overtime - withinStatutoryOvertimeWorkHour;
      fix_within_statutory_overtime_work_hour = 0f;
    } else {
      fix_within_statutory_overtime_work_hour =
        withinStatutoryOvertimeWorkHour
          - fix_within_statutory_overtime_work_hour;
      return;
    }
    if (inworkHour > time) {
      fix_inwork_hour = inworkHour - time;
    } else {
      fix_inwork_hour = 0f;
    }
    fix_overtime_hour = week_overtime + overTimeHour;
  }

  /**
   * 新しい集計方式の場合
   *
   * @return
   */
  public boolean isNewRule() {
    return isNewRule;
  }

  /**
   * @param isNewRule
   *          セットする isNewRule
   */
  public void setNewRule(boolean isNewRule) {
    this.isNewRule = isNewRule;
  }

  /**
   * @return isStatutoryHoliday
   */
  public boolean isStatutoryHoliday() {
    return isStatutoryHoliday;
  }

  /**
   * @param isStatutoryHoliday
   *          セットする isStatutoryHoliday
   */
  public void setStatutoryHoliday(boolean isStatutoryHoliday) {
    this.isStatutoryHoliday = isStatutoryHoliday;
  }

}
