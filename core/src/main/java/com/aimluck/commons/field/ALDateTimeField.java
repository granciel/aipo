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
package com.aimluck.commons.field;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;

import com.aimluck.commons.utils.ALDateUtil;
import com.aimluck.commons.utils.ALStringUtil;
import com.aimluck.eip.util.ALLocalizationUtils;

/**
 * 入力フィールドを表すクラス（年月日時分用）です。 <br />
 *
 */
public class ALDateTimeField extends ALAbstractField {

  /**
   *
   */
  private static final long serialVersionUID = -1361590925614184293L;

  /** 時刻を含む日付の表示フォーマット */
  public static final String DEFAULT_DATE_TIME_FORMAT = "yyyy/MM/dd HH:mm";

  /** 時刻の表示フォーマット */
  public static final String DEFAULT_TIME_FORMAT = "H:mm";

  /** 日付の表示フォーマット */
  public static final String DEFAULT_DATE_FORMAT = "yyyy/MM/dd";

  /** 日付 */
  protected Calendar calendar = null;

  /** 日付のフォーマット */
  protected String format = null;

  /** 年 */
  protected String year = null;

  /** 月 */
  protected String month = null;

  /** 日 */
  protected String day = null;

  /** 時 */
  protected String hour = null;

  /** 分 */
  protected String minute = null;

  /** 時刻(HH:mm) */
  protected String time = null;

  /**
   * コンストラクタ
   *
   */
  public ALDateTimeField() {
    format = DEFAULT_DATE_FORMAT;
  }

  /**
   * コンストラクタ
   *
   * @param dateFormat
   */
  public ALDateTimeField(String dateFormat) {
    if (dateFormat == null) {
      format = DEFAULT_DATE_FORMAT;
    } else {
      format = dateFormat;
    }
  }

  /**
   * 入力フィールド値（日付の文字列）を設定します。
   *
   */
  @Override
  public void setValue(String str) {
    setValue(translateDate(str, format));
  }

  /**
   * 入力フィールド値（日付）を設定します。
   *
   * @param date
   */
  public void setValue(Date date) {
    if (date == null) {
      return;
    }

    if (calendar == null) {
      calendar = new GregorianCalendar();
    }

    calendar.setTime(date);
    calendar.set(Calendar.SECOND, 0);
    calendar.set(Calendar.MILLISECOND, 0);

    try {
      year = Integer.toString(calendar.get(Calendar.YEAR));
      month = Integer.toString(calendar.get(Calendar.MONTH) + 1);
      day = Integer.toString(calendar.get(Calendar.DATE));
      hour = Integer.toString(calendar.get(Calendar.HOUR_OF_DAY));
      minute = Integer.toString(calendar.get(Calendar.MINUTE));
      time =
        new SimpleDateFormat(DEFAULT_TIME_FORMAT).format(calendar.getTime());
    } catch (Throwable ex) {
      year = null;
      month = null;
      day = null;
      hour = null;
      minute = null;
      time = null;
    }
  }

  /**
   * 入力フィールド値を取得します。
   *
   * @return
   */
  public Date getValue() {
    return calendar.getTime();
  }

  /**
   * 入力フィールド値を検証します。
   *
   * @param msgList
   * @return
   */
  public boolean validate(List<String> msgList) {
    if (msgList == null) {
      msgList = new ArrayList<String>();
    }

    if (!isNotNullValue()) {
      if (isNotNull()) {
        // 必須入力属性で値が設定されていない場合
        msgList.add(ALLocalizationUtils.getl10nFormat(
          "COMMONS_FIELD_INPUT_NAME_SPAN",
          fieldName));
        return false;
      }
    } else {
      // 日付として正しいかを調べる
      String dateStr = translateDate(calendar.getTime(), format);
      if ("Unknown".equals(dateStr) || "".equals(dateStr)) {
        msgList.add(ALLocalizationUtils.getl10nFormat(
          "COMMONS_FIELD_DATE_TYPE_CAUTION_SPAN",
          fieldName));
        return false;
      }
    }
    return true;
  }

  /**
   * 年の設定の有無を判別します。
   *
   * @return true:設定されていない<BR>
   *         false:設定されている
   */
  public boolean isNullYear() {
    return isNullDateTime(year);
  }

  /**
   * 月の設定の有無を判別します。
   *
   * @return true:設定されていない<BR>
   *         false:設定されている
   */
  public boolean isNullMonth() {
    return isNullDateTime(month);
  }

  /**
   * 日の設定の有無を判別します。
   *
   * @return true:設定されていない<BR>
   *         false:設定されている
   */
  public boolean isNullDay() {
    return isNullDateTime(day);
  }

  /**
   * 時の設定の有無を判別します。
   *
   * @return true:設定されていない<BR>
   *         false:設定されている
   */
  public boolean isNullHour() {
    return isNullDateTime(hour);
  }

  /**
   * 分の設定の有無を判別します。
   *
   * @return true:設定されていない<BR>
   *         false:設定されている
   */
  public boolean isNullMinute() {
    return isNullDateTime(minute);
  }

  /**
   * 時刻の設定の有無を判別します。
   *
   * @return true:設定されていない<BR>
   *         false:設定されている
   */
  public boolean isNullTime() {
    return isNullDateTime(time);
  }

  /**
   * 指定した文字列の値の有無を検証します。
   *
   * @param str
   * @return true:設定されていない<BR>
   *         false:設定されている
   */
  private boolean isNullDateTime(String str) {
    if (calendar != null) {
      return false;
    }

    if ((str != null) && (str.trim().length() != 0)) {
      return false;
    }

    return true;
  }

  /**
   * 年の文字列表現を取得します。
   *
   * @return
   */
  public String toStringYear() {
    if (calendar == null || isNullYear()) {
      return ALStringUtil.sanitizing(null);
    } else {
      return ALStringUtil.sanitizing(year);
    }
  }

  /**
   * 時刻を含む月日(MM/dd HH:mm)の文字列表現を取得します。
   *
   * @return
   */
  public String toStringDateTime() {
    String date_time;
    try {
      date_time =
        new SimpleDateFormat(DEFAULT_DATE_TIME_FORMAT).format(calendar
          .getTime());
    } catch (Exception ex) {
      date_time = null;
    }
    if (calendar == null || isNullDateTime(date_time)) {
      return ALStringUtil.sanitizing(null);
    } else {
      return ALStringUtil.sanitizing(date_time);
    }
  }

  /**
   * 時刻(HH:mm)の文字列表現を取得します。
   *
   * @return
   */
  public String toStringTime() {
    if (calendar == null || isNullTime()) {
      return ALStringUtil.sanitizing(null);
    } else {
      return ALStringUtil.sanitizing(time);
    }
  }

  /**
   * 月の文字列表現を取得します。
   *
   * @return
   */
  public String toStringMonth() {
    if (calendar == null || isNullMonth()) {
      return ALStringUtil.sanitizing(null);
    } else {
      return ALStringUtil.sanitizing(month);
    }
  }

  /**
   * 日の文字列表現を取得します。
   *
   * @return
   */
  public String toStringDay() {
    if (calendar == null || isNullDay()) {
      return ALStringUtil.sanitizing(null);
    } else {
      return ALStringUtil.sanitizing(day);
    }
  }

  /**
   * 時の文字列表現を取得します。
   *
   * @return
   */
  public String toStringHour() {
    if (calendar == null || isNullHour()) {
      return ALStringUtil.sanitizing(null);
    } else {
      return ALStringUtil.sanitizing(hour);
    }
  }

  /**
   * 分の文字列表現を取得します。
   *
   * @return
   */
  public String toStringMinute() {
    if (calendar == null || isNullMinute()) {
      return ALStringUtil.sanitizing(null);
    } else {
      return ALStringUtil.sanitizing(minute);
    }
  }

  /**
   * 曜日の文字列表現を取得します。
   *
   * @return
   */
  public String toStringDayOfWeek() {
    if (calendar == null || isNullYear() || isNullMonth() || isNullDay()) {
      return ALStringUtil.sanitizing(null);
    } else {
      return ALStringUtil.sanitizing(ALDateUtil.getDayOfWeek(calendar));
    }
  }

  /**
   * 曜日の数値表現を取得します
   *
   * @return
   */
  public Integer getDayOfWeekNum() {
    if (calendar == null || isNullYear() || isNullMonth() || isNullDay()) {
      return null;
    }
    return calendar.get(Calendar.DAY_OF_WEEK);
  }

  /**
   * 年の値を取得します。
   *
   * @return
   */
  public String getYear() {
    return toStringYear();
  }

  /**
   * 時刻を含む月日(MM/dd HH:mm)の値を取得します。
   *
   * @return
   */
  public String getDateTime() {
    return toStringDateTime();
  }

  /**
   * 時刻(HH:mm)の値を取得します。
   *
   * @return
   */
  public String getTime() {
    return toStringTime();
  }

  /**
   * 月の値を取得します。
   *
   * @return
   */
  public String getMonth() {
    return toStringMonth();
  }

  /**
   * 日の値を取得します。
   *
   * @return
   */
  public String getDay() {
    return toStringDay();
  }

  /**
   * 時の値を取得します。
   *
   * @return
   */
  public String getHour() {
    return toStringHour();
  }

  /**
   * 分の値を取得します。
   *
   * @return
   */
  public String getMinute() {
    return toStringMinute();
  }

  /**
   * 曜日の値を取得します。
   *
   * @return
   */
  public String getDayOfWeek() {
    return toStringDayOfWeek();
  }

  /**
   * 入力フィールド値がNullではないかどうかを判定します。
   *
   * @return
   */
  public boolean isNotNullValue() {
    if (calendar == null) {
      return false;
    }
    return true;
  }

  /**
   * Date のオブジェクトを指定した形式の文字列に変換します。
   *
   * @param date
   * @param dateFormat
   * @return
   */
  protected String translateDate(Date date, String dateFormat) {
    if (date == null) {
      return "Unknown";
    }

    // 日付を表示形式に変換
    SimpleDateFormat sdf = new SimpleDateFormat(dateFormat);
    sdf.setTimeZone(TimeZone.getDefault());
    return sdf.format(date);
  }

  /**
   * 指定した形式の文字列を Date のオブジェクトに変換します。
   *
   * @param dateStr
   * @param dateFormat
   * @return
   */
  protected Date translateDate(String dateStr, String dateFormat) {
    if (dateStr == null || dateStr.equals("")) {
      return null;
    }
    Date date = null;

    // 日付を表示形式に変換
    SimpleDateFormat sdf = new SimpleDateFormat(dateFormat);
    sdf.setTimeZone(TimeZone.getDefault());

    try {
      date = sdf.parse(dateStr);
    } catch (Exception e) {
      return null;
    }
    return date;
  }

  /**
   * 入力フィールド値の文字列表現を取得します。
   *
   */
  @Override
  public String toString() {
    if (calendar == null) {
      return ALStringUtil.sanitizing(null);
    } else {
      return ALStringUtil.sanitizing(translateDate(getValue(), format));
    }
  }

  public String toString(String format) {
    if (calendar == null) {
      return ALStringUtil.sanitizing(null);
    } else {
      return ALStringUtil.sanitizing(translateDate(getValue(), format));
    }
  }

}
