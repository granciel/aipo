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
package com.aimluck.eip.exttimecard.util;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.jetspeed.services.logging.JetspeedLogFactoryService;
import org.apache.jetspeed.services.logging.JetspeedLogger;
import org.apache.jetspeed.services.resources.JetspeedResources;
import org.apache.turbine.services.TurbineServices;
import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;

import com.aimluck.eip.cayenne.om.portlet.EipTExtTimecard;
import com.aimluck.eip.cayenne.om.portlet.EipTExtTimecardSystem;
import com.aimluck.eip.cayenne.om.portlet.EipTExtTimecardSystemMap;
import com.aimluck.eip.common.ALEipConstants;
import com.aimluck.eip.common.ALEipHolidaysManager;
import com.aimluck.eip.exttimecard.ExtTimecardListResultData;
import com.aimluck.eip.exttimecard.ExtTimecardListResultDataContainer;
import com.aimluck.eip.exttimecard.ExtTimecardResultData;
import com.aimluck.eip.http.HttpServletRequestLocator;
import com.aimluck.eip.orm.Database;
import com.aimluck.eip.orm.query.SelectQuery;
import com.aimluck.eip.services.accessctl.ALAccessControlConstants;
import com.aimluck.eip.services.accessctl.ALAccessControlFactoryService;
import com.aimluck.eip.services.accessctl.ALAccessControlHandler;
import com.aimluck.eip.services.config.ALConfigHandler;
import com.aimluck.eip.services.config.ALConfigService;
import com.aimluck.eip.util.ALEipUtils;

/**
 * タイムカードのユーティリティクラスです。 <BR>
 *
 */
public class ExtTimecardUtils {

  /** logger */
  private static final JetspeedLogger logger = JetspeedLogFactoryService
    .getLogger(ExtTimecardUtils.class.getName());

  /** <code>TARGET_GROUP_NAME</code> グループによる表示切り替え用変数の識別子 */
  public static final String TARGET_GROUP_NAME = "target_group_name";

  /** <code>TARGET_USER_ID</code> ユーザによる表示切り替え用変数の識別子 */
  public static final String TARGET_USER_ID = "target_user_id";

  /** 出退勤フラグ（出勤） */
  public static final String WORK_FLG_ON = "1";

  /** 出退勤フラグ（退勤） */
  public static final String WORK_FLG_OFF = "0";

  /** 出退勤フラグ（ダミー） */
  public static final String WORK_FLG_DUMMY = "-1";

  /** 日分を超える労働を残業とする */
  public static final Integer OVERTIME_TYPE_DEFAULT_MINUTE = 480;

  /** 週時間を超える労働を残業とする */
  public static final Integer OVERTIME_TYPE_DEFAULT_HOUR_BY_WEEK = 40;

  /** 勤務時間外の労働を残業とする */
  public static final String OVERTIME_TYPE_O = "O";

  /** タイムカードファイルを一時保管するディレクトリの指定 */
  public static final String FOLDER_TMP_FOR_TIMECARD_FILES = JetspeedResources
    .getString("aipo.tmp.timecard.directory", "");

  public static final String EXTTIMECARD_PORTLET_NAME = "ExtTimecard";

  public static final String EXTTIMECARD_SYSTEM_PORTLET_NAME =
    "ExtTimecardSystem";

  /**
   * ExtTimecard オブジェクトモデルを取得します。 <BR>
   *
   * @param rundata
   * @param context
   * @param isJoin
   *          カテゴリテーブルをJOINするかどうか
   * @return
   */
  public static EipTExtTimecard getEipTExtTimecard(RunData rundata,
      Context context) {
    String timecardid =
      ALEipUtils.getTemp(rundata, context, ALEipConstants.ENTITY_ID);

    ALAccessControlFactoryService aclservice =
      (ALAccessControlFactoryService) ((TurbineServices) TurbineServices
        .getInstance()).getService(ALAccessControlFactoryService.SERVICE_NAME);
    ALAccessControlHandler aclhandler = aclservice.getAccessControlHandler();

    try {
      if (timecardid == null || Integer.valueOf(timecardid) == null) {
        logger.debug("[ExtTimecardUtils.getEipTExtTimecard] Empty ID...");
        return null;
      }

      SelectQuery<EipTExtTimecard> query =
        Database.query(EipTExtTimecard.class);
      Expression exp11 =
        ExpressionFactory.matchDbExp(
          EipTExtTimecard.TIMECARD_ID_PK_COLUMN,
          timecardid);
      query.setQualifier(exp11);

      // have "others timecard acl", then not check user_id
      if (!(aclhandler.hasAuthority(
        ALEipUtils.getUserId(rundata),
        ALAccessControlConstants.POERTLET_FEATURE_TIMECARD_TIMECARD_OTHER,
        ALAccessControlConstants.VALUE_ACL_UPDATE))) {
        Expression exp21 =
          ExpressionFactory.matchExp(EipTExtTimecard.USER_ID_PROPERTY, Integer
            .valueOf(ALEipUtils.getUserId(rundata)));
        query.andQualifier(exp21);
      }

      List<EipTExtTimecard> timecards = query.fetchList();
      if (timecards == null || timecards.size() == 0) {
        // 指定したTimecard IDのレコードが見つからない場合
        logger.debug("[Timecard] Not found ID...");
        return null;
      }
      return timecards.get(0);
    } catch (Exception ex) {
      logger.error("exttimecard", ex);
      return null;
    }
  }

  /**
   * 現在ログイン中のユーザーのEipTExtTimecardSystemを取得します。
   *
   * @param rundata
   * @param context
   * @return
   */
  public static EipTExtTimecardSystem getEipTExtTimecardSystemCurrentUserId(
      RunData rundata, Context context) {
    int user_id = Integer.valueOf(ALEipUtils.getUserId(rundata));
    return getEipTExtTimecardSystemByUserId(user_id);
  }

  /**
   * 特定ユーザーのEipTExtTimecardSystemを取得します。
   *
   * @param rundata
   * @param context
   * @return
   */
  public static EipTExtTimecardSystem getEipTExtTimecardSystemByUserId(
      int user_id) {
    try {
      SelectQuery<EipTExtTimecardSystemMap> query =
        Database.query(EipTExtTimecardSystemMap.class);
      Expression exp1 =
        ExpressionFactory.matchExp(
          EipTExtTimecardSystemMap.USER_ID_PROPERTY,
          Integer.valueOf(user_id));
      query.setQualifier(exp1);
      List<EipTExtTimecardSystemMap> slist = query.fetchList();
      if (slist == null || slist.size() == 0) {
        return getEipTExtTimecardSystemById(1);
      }
      return slist.get(0).getEipTExtTimecardSystem();
    } catch (Exception ex) {
      logger.error("exttimecard", ex);
      return null;
    }
  }

  public static EipTExtTimecardSystem getEipTExtTimecardSystemById(int system_id) {
    try {
      SelectQuery<EipTExtTimecardSystem> query =
        Database.query(EipTExtTimecardSystem.class);
      Expression exp1 =
        ExpressionFactory.matchDbExp(
          EipTExtTimecardSystem.SYSTEM_ID_PK_COLUMN,
          system_id);
      query.setQualifier(exp1);
      List<EipTExtTimecardSystem> slist = query.fetchList();
      if (slist == null || slist.size() == 0) {
        return null;
      }
      return slist.get(0);
    } catch (Exception ex) {
      logger.error("exttimecard", ex);
      return null;
    }
  }

  /**
   *
   * @return
   */
  public static List<EipTExtTimecardSystem> getAllEipTExtTimecardSystem() {
    try {
      SelectQuery<EipTExtTimecardSystem> query =
        Database.query(EipTExtTimecardSystem.class);
      List<EipTExtTimecardSystem> slist = query.fetchList();
      if (slist == null || slist.size() == 0) {
        // 指定したSetting IDのレコードが見つからない場合
        logger.debug("[ExtTimecardUtils] Not found ID...");
        return null;
      }
      return slist;
    } catch (Exception ex) {
      logger.error("exttimecard", ex);
      return null;
    }
  }

  /**
   *
   * @return
   */
  public static EipTExtTimecardSystem getEipTExtTimecardSystem(RunData rundata,
      Context context) {
    String systemid =
      ALEipUtils.getTemp(rundata, context, ALEipConstants.ENTITY_ID);
    try {
      if (systemid == null || Integer.valueOf(systemid) == null) {
        // Setting IDが空の場合
        logger.debug("[ExtTimecardUtils] Empty ID...");
        return null;
      }

      SelectQuery<EipTExtTimecardSystem> query =
        Database.query(EipTExtTimecardSystem.class);
      Expression exp1 =
        ExpressionFactory.matchDbExp(
          EipTExtTimecardSystem.SYSTEM_ID_PK_COLUMN,
          systemid);

      query.setQualifier(exp1);
      List<EipTExtTimecardSystem> slist = query.fetchList();
      if (slist == null || slist.size() == 0) {
        // 指定したSetting IDのレコードが見つからない場合
        logger.debug("[ExtTimecardUtils] Not found ID...");
        return null;
      }

      return slist.get(0);
    } catch (Exception ex) {
      logger.error("exttimecard", ex);
      return null;
    }
  }

  /**
   * 指定した2つの日付を比較する．
   *
   * @param date1
   * @param date2
   * @param checkTime
   *          時間まで比較する場合，true．
   * @return 等しい場合，0. date1>date2の場合, 1. date1 <date2の場合, 2.
   */
  public static boolean sameDay(Date date1, Date date2) {
    Calendar cal1 = Calendar.getInstance();
    Calendar cal2 = Calendar.getInstance();
    cal1.setTime(date1);
    cal2.setTime(date2);

    int date1Year = cal1.get(Calendar.YEAR);
    int date1Month = cal1.get(Calendar.MONTH) + 1;
    int date1Day = cal1.get(Calendar.DATE);
    int date2Year = cal2.get(Calendar.YEAR);
    int date2Month = cal2.get(Calendar.MONTH) + 1;
    int date2Day = cal2.get(Calendar.DATE);

    if (date1Year == date2Year
      && date1Month == date2Month
      && date1Day == date2Day) {
      return true;
    }
    return false;
  }

  /**
   * 現在時刻とEipTTimecardSettingsの情報から update文を発行すべきかどうかを判断します。
   *
   * @param org_id
   * @param userId
   * @return
   */
  public static EipTExtTimecard getUpdateEipTExtTimecard(RunData rundata,
      Context context) {
    EipTExtTimecardSystem system =
      ExtTimecardUtils.getEipTExtTimecardSystemCurrentUserId(rundata, context);

    Calendar calendar_now = Calendar.getInstance();
    Calendar from_calendar = Calendar.getInstance();

    int hour = system.getChangeHour().intValue();
    from_calendar.set(Calendar.HOUR_OF_DAY, hour);
    from_calendar.set(Calendar.MINUTE, 0);
    from_calendar.set(Calendar.SECOND, 0);
    if (calendar_now.before(from_calendar)) {
      from_calendar.add(Calendar.DAY_OF_MONTH, -1);
    }
    from_calendar.set(Calendar.HOUR_OF_DAY, 0);

    SelectQuery<EipTExtTimecard> query = Database.query(EipTExtTimecard.class);
    Expression exp1 =
      ExpressionFactory.matchExp(EipTExtTimecard.USER_ID_PROPERTY, Integer
        .valueOf(ALEipUtils.getUserId(rundata)));
    Expression exp2 =
      ExpressionFactory.matchExp(
        EipTExtTimecard.PUNCH_DATE_PROPERTY,
        from_calendar.getTime());
    query.setQualifier(exp1.andExp(exp2));
    List<EipTExtTimecard> slist = query.fetchList();

    // データが無かった
    if (slist == null || slist.size() == 0) {
      return null;
    }

    return slist.get(0);
  }

  /**
   * 集計した時間を丸めます。
   *
   * @return
   */
  public static float roundHour(float time) {
    if (isNewRule()) {
      time *= 100;
      time = Math.round(time);
      time /= 100;
    } else {
      time *= 10;
      time = Math.round(time);
      time /= 10;
    }
    return time;
  }

  public static JetspeedLogger getLogger() {
    return logger;
  }

  /**
   * 休日（所定休日、法定休日）の曜日一覧
   *
   * @return
   */
  public static List<Integer> getOffdayDayOfWeek(EipTExtTimecardSystem model) {
    List<Integer> list = new ArrayList<Integer>();
    String week = getHolidayOfWeek(model);
    // 標準は日・土
    if (week.charAt(0) != '0') {
      list.add(Calendar.SUNDAY);
    }
    if (week.charAt(1) != '0') {
      list.add(Calendar.MONDAY);
    }
    if (week.charAt(2) != '0') {
      list.add(Calendar.TUESDAY);
    }
    if (week.charAt(3) != '0') {
      list.add(Calendar.WEDNESDAY);
    }
    if (week.charAt(4) != '0') {
      list.add(Calendar.THURSDAY);
    }
    if (week.charAt(5) != '0') {
      list.add(Calendar.FRIDAY);
    }
    if (week.charAt(6) != '0') {
      list.add(Calendar.SATURDAY);
    }
    return list;
  }

  /**
   * 法定休日
   *
   * @return
   */
  public static int getStatutoryHoliday(EipTExtTimecardSystem model) {
    String week = getHolidayOfWeek(model);
    try {
      return Character.getNumericValue(week.charAt(7));
    } catch (Throwable ignore) {

    }
    return 0;
  }

  public static ExtTimecardListResultDataContainer groupByWeek(
      Date queryStartDate, List<ExtTimecardResultData> flat,
      EipTExtTimecardSystem timecard_system) {
    boolean isNewRule = ExtTimecardUtils.isNewRule();
    EipTExtTimecardSystem tmpTimecardSyste = timecard_system;
    if (tmpTimecardSyste == null) {
      for (ExtTimecardResultData rd : flat) {
        tmpTimecardSyste = rd.getTimecardSystem();
        break;
      }
    }
    ExtTimecardListResultDataContainer result =
      new ExtTimecardListResultDataContainer(queryStartDate, tmpTimecardSyste);
    for (ExtTimecardResultData rd : flat) {
      ExtTimecardListResultData lrd = new ExtTimecardListResultData();
      lrd.initField();
      lrd.setDate(rd.getPunchDate().getValue());
      lrd.setRd(rd);
      lrd.setTimecardSystem(tmpTimecardSyste);
      lrd.setNewRule(isNewRule);
      result.add(lrd);
    }
    return result;
  }

  /**
   * 新しい集計ルールかどうか
   *
   * @return
   */
  public static boolean isNewRule() {
    HttpServletRequest request = HttpServletRequestLocator.get();
    String cacheVersion = null;
    if (request != null) {
      try {
        cacheVersion =
          (String) request
            .getAttribute(ALConfigHandler.Property.EXTTIMECARD_VERTION
              .toString());
      } catch (Throwable ignore) {

      }
    }
    if (cacheVersion == null) {
      cacheVersion =
        ALConfigService.get(ALConfigHandler.Property.EXTTIMECARD_VERTION);
      if (request != null) {
        request.setAttribute(ALConfigHandler.Property.EXTTIMECARD_VERTION
          .toString(), cacheVersion);
      }
    }
    return "2".equals(cacheVersion);
  }

  public static int getOvertimeMinuteByDay(EipTExtTimecardSystem model) {
    String type = model.getOvertimeType();
    return getOvertimeMinuteByDay(type);
  }

  public static int getOvertimeMinuteByDay(String type) {
    int value = OVERTIME_TYPE_DEFAULT_MINUTE;
    if (type != null) {
      String[] split = type.split("-");
      if (split.length == 2) {
        try {
          return Integer.valueOf(split[0]);
        } catch (Throwable ignore) {

        }
      }
    }
    return value;
  }

  public static int getOvertimeHourByWeek(EipTExtTimecardSystem model) {
    String type = model.getOvertimeType();
    return getOvertimeHourByWeek(type);
  }

  public static int getOvertimeHourByWeek(String type) {
    int value = OVERTIME_TYPE_DEFAULT_HOUR_BY_WEEK;
    if (type != null) {
      String[] split = type.split("-");
      if (split.length == 2) {
        try {
          String substring = split[1].substring(1);
          return Integer.valueOf(substring);
        } catch (Throwable ignore) {

        }
      }
    }
    return value;
  }

  public static boolean isOvertimeHourByWeek(String type) {
    if (type != null) {
      String[] split = type.split("-");
      if (split.length == 2) {
        try {
          String substring = split[1].substring(0, 1);
          return "T".equals(substring);
        } catch (Throwable ignore) {

        }
      }
    }
    return false;
  }

  public static String getHolidayOfWeek(EipTExtTimecardSystem model) {
    if (model.isDefaultHoliday()) {
      HttpServletRequest request = HttpServletRequestLocator.get();
      String cacheHoliday = null;
      if (request != null) {
        try {
          cacheHoliday =
            (String) request
              .getAttribute(ALConfigHandler.Property.HOLIDAY_OF_WEEK.toString());
        } catch (Throwable ignore) {

        }
      }
      if (cacheHoliday == null) {
        cacheHoliday =
          ALConfigService.get(ALConfigHandler.Property.HOLIDAY_OF_WEEK);
        if (request != null) {
          request.setAttribute(ALConfigHandler.Property.HOLIDAY_OF_WEEK
            .toString(), cacheHoliday);
        }
      }
      return cacheHoliday;
    } else {
      return model.getOriginalHolidayOfWeek();
    }
  }

  public static boolean isHoliday(EipTExtTimecardSystem model, Date date) {
    String cacheHoliday = getHolidayOfWeek(model);
    boolean holiday = cacheHoliday.charAt(8) != '0';
    if (holiday) {
      ALEipHolidaysManager holidaysManager = ALEipHolidaysManager.getInstance();
      if (holidaysManager.isHoliday(date) != null) {
        return true;
      }
    }
    Calendar cal = Calendar.getInstance();
    cal.setTime(date);
    return cacheHoliday.charAt(cal.get(Calendar.DAY_OF_WEEK) - 1) != '0';
  }
}