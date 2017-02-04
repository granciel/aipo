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
package com.aimluck.eip.schedule.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import javax.imageio.ImageIO;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.cayenne.DataRow;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.query.Ordering;
import org.apache.commons.lang.time.DateUtils;
import org.apache.jetspeed.om.profile.Entry;
import org.apache.jetspeed.om.profile.Portlets;
import org.apache.jetspeed.om.security.UserIdPrincipal;
import org.apache.jetspeed.portal.portlets.VelocityPortlet;
import org.apache.jetspeed.services.JetspeedSecurity;
import org.apache.jetspeed.services.logging.JetspeedLogFactoryService;
import org.apache.jetspeed.services.logging.JetspeedLogger;
import org.apache.jetspeed.services.resources.JetspeedResources;
import org.apache.jetspeed.services.rundata.JetspeedRunData;
import org.apache.jetspeed.util.template.JetspeedLink;
import org.apache.jetspeed.util.template.JetspeedLinkFactory;
import org.apache.turbine.services.TurbineServices;
import org.apache.turbine.services.velocity.VelocityService;
import org.apache.turbine.util.DynamicURI;
import org.apache.turbine.util.RunData;
import org.apache.turbine.util.TurbineException;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.context.Context;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;

import com.aimluck.commons.field.ALAbstractField;
import com.aimluck.commons.field.ALCellDateField;
import com.aimluck.commons.field.ALCellDateTimeField;
import com.aimluck.commons.field.ALCellStringField;
import com.aimluck.commons.field.ALDateContainer;
import com.aimluck.commons.field.ALDateField;
import com.aimluck.commons.field.ALDateTimeField;
import com.aimluck.commons.field.ALIllegalDateException;
import com.aimluck.commons.field.ALNumberField;
import com.aimluck.commons.field.ALStringField;
import com.aimluck.commons.utils.ALDateUtil;
import com.aimluck.commons.utils.ALDeleteFileUtil;
import com.aimluck.eip.category.util.CommonCategoryUtils;
import com.aimluck.eip.cayenne.om.portlet.EipMFacility;
import com.aimluck.eip.cayenne.om.portlet.EipTCommonCategory;
import com.aimluck.eip.cayenne.om.portlet.EipTSchedule;
import com.aimluck.eip.cayenne.om.portlet.EipTScheduleFile;
import com.aimluck.eip.cayenne.om.portlet.EipTScheduleMap;
import com.aimluck.eip.cayenne.om.portlet.VEipTScheduleList;
import com.aimluck.eip.cayenne.om.portlet.auto._EipTSchedule;
import com.aimluck.eip.cayenne.om.security.TurbineUser;
import com.aimluck.eip.common.ALActivity;
import com.aimluck.eip.common.ALBaseUser;
import com.aimluck.eip.common.ALDBErrorException;
import com.aimluck.eip.common.ALData;
import com.aimluck.eip.common.ALEipConstants;
import com.aimluck.eip.common.ALEipUser;
import com.aimluck.eip.common.ALFileNotRemovedException;
import com.aimluck.eip.common.ALPageNotFoundException;
import com.aimluck.eip.facilities.FacilityResultData;
import com.aimluck.eip.facilities.util.FacilitiesUtils;
import com.aimluck.eip.fileupload.beans.FileuploadBean;
import com.aimluck.eip.fileupload.beans.FileuploadLiteBean;
import com.aimluck.eip.fileupload.util.FileuploadUtils;
import com.aimluck.eip.fileupload.util.FileuploadUtils.ShrinkImageSet;
import com.aimluck.eip.http.HttpServletRequestLocator;
import com.aimluck.eip.mail.util.ALMailUtils;
import com.aimluck.eip.orm.Database;
import com.aimluck.eip.orm.query.ResultList;
import com.aimluck.eip.orm.query.SQLTemplate;
import com.aimluck.eip.orm.query.SelectQuery;
import com.aimluck.eip.schedule.AjaxScheduleResultData;
import com.aimluck.eip.schedule.AjaxTermScheduleWeekContainer;
import com.aimluck.eip.schedule.ScheduleDetailOnedaySelectData;
import com.aimluck.eip.schedule.ScheduleDetailResultData;
import com.aimluck.eip.schedule.ScheduleNotFounfException;
import com.aimluck.eip.schedule.ScheduleOnedayContainer;
import com.aimluck.eip.schedule.ScheduleOnedayResultData;
import com.aimluck.eip.schedule.ScheduleResultData;
import com.aimluck.eip.schedule.ScheduleTermWeekContainer;
import com.aimluck.eip.schedule.ScheduleToDoResultData;
import com.aimluck.eip.schedule.ScheduleToDoWeekContainer;
import com.aimluck.eip.services.accessctl.ALAccessControlConstants;
import com.aimluck.eip.services.accessctl.ALAccessControlFactoryService;
import com.aimluck.eip.services.accessctl.ALAccessControlHandler;
import com.aimluck.eip.services.config.ALConfigHandler;
import com.aimluck.eip.services.config.ALConfigService;
import com.aimluck.eip.services.orgutils.ALOrgUtilsService;
import com.aimluck.eip.services.reminder.ALReminderHandler.ReminderCategory;
import com.aimluck.eip.services.reminder.ALReminderHandler.ReminderNotifyType;
import com.aimluck.eip.services.reminder.ALReminderService;
import com.aimluck.eip.services.reminder.model.ALReminderItem;
import com.aimluck.eip.services.social.ALActivityService;
import com.aimluck.eip.services.social.model.ALActivityPutRequest;
import com.aimluck.eip.services.storage.ALStorageService;
import com.aimluck.eip.user.beans.UserLiteBean;
import com.aimluck.eip.user.util.UserUtils;
import com.aimluck.eip.userfacility.beans.UserFacilityLiteBean;
import com.aimluck.eip.util.ALCellularUtils;
import com.aimluck.eip.util.ALEipUtils;
import com.aimluck.eip.util.ALLocalizationUtils;
import com.aimluck.eip.whatsnew.util.WhatsNewUtils;

/**
 * スケジュールのユーティリティクラスです。
 *
 */
public class ScheduleUtils {

  /** <code>logger</code> loger */
  private static final JetspeedLogger logger = JetspeedLogFactoryService
    .getLogger(ScheduleUtils.class.getName());

  /** <code>SCHEDULEMAP_TYPE_USER</code> ユーザ */
  public static final String SCHEDULEMAP_TYPE_USER = "U";

  /** <code>SCHEDULEMAP_TYPE_FACILITY</code> 設備 */
  public static final String SCHEDULEMAP_TYPE_FACILITY = "F";

  /** スケジュールの添付ファイルを保管するディレクトリの指定 */
  public static final String FOLDER_FILEDIR_SCHEDULE = JetspeedResources
    .getString("aipo.filedir", "");

  /** スケジュールの添付ファイルを保管するディレクトリのカテゴリキーの指定 */
  public static final String CATEGORY_KEY = JetspeedResources.getString(
    "aipo.schedule.categorykey",
    "");

  /** デフォルトエンコーディングを表わすシステムプロパティのキー */
  public static final String FILE_ENCODING = JetspeedResources.getString(
    "content.defaultencoding",
    "UTF-8");

  /** <code>TARGET_FACILITY_ID</code> ユーザによる表示切り替え用変数の識別子 */
  public static final String TARGET_FACILITY_ID = "f";

  public static final String MAIL_FOR_INSERT = "I";

  public static final String MAIL_FOR_UPDATE = "U";

  public static final String MAIL_FOR_DELETE = "D";

  /** すべての人にメール送信 */
  public static final String MAIL_FOR_ALL = "A";

  /** すべての人にメール送信しない */
  public static final String MAIL_FOR_NONE = "N";

  public static final String FLAG_CHANGE_TURN_STR = "flagchangeturn";

  public static final String SCHEDULE_PORTLET_NAME = "Schedule";

  public static final String AJAX_SCHEDULE_PORTLET_NAME = "AjaxSchedule";

  public static final String SCHEDULE_ADMIN_PORTLET_NAME = "ScheduleAdmin";

  /** 検索キーワード変数の識別子 */
  public static final String TARGET_KEYWORD = "keyword";

  public static final String FILTER_NAMESPACE =
    "com.aimluck.eip.schedule.filter";

  public static final String FILTER_NAMESPACE_TYPE =
    "com.aimluck.eip.schedule.filtertype";

  private static Map<String, String> tabToLayOut;

  static {
    tabToLayOut = new HashMap<String, String>();
    tabToLayOut.put("calendar", "schedule-calendar");
    tabToLayOut.put("oneday", "schedule-oneday");
    tabToLayOut.put("weekly", "schedule-weekly");
    tabToLayOut.put("monthly", "schedule-monthly");
    tabToLayOut.put("oneday-group", "schedule-oneday-group");
    tabToLayOut.put("weekly-group", "schedule-weekly-group");
    tabToLayOut.put("list", "schedule-search-list");
  }

  private static Map<String, String> LayOutTotab;

  static {
    LayOutTotab = new HashMap<String, String>();
    LayOutTotab.put("schedule-calendar", "calendar");
    LayOutTotab.put("schedule-oneday", "oneday");
    LayOutTotab.put("schedule-weekly", "weekly");
    LayOutTotab.put("schedule-monthly", "monthly");
    LayOutTotab.put("schedule-oneday-group", "oneday-group");
    LayOutTotab.put("schedule-weekly-group", "weekly-group");
    LayOutTotab.put("schedule-search-list", "list");
  }

  public static String getLayoutFromTabName(String tabname) {
    return tabToLayOut.get(tabname);
  }

  public static String getTabNameFromLayout(String Layout) {
    return LayOutTotab.get(Layout);
  }

  public static final List<Integer> notifyTimingList = Arrays.asList(
    0,
    5,
    15,
    30,
    60);

  /**
   * Scheudle オブジェクトモデルを取得します。
   *
   * @param rundata
   * @param context
   * @return
   */
  public static EipTSchedule getEipTSchedule(RunData rundata, Context context,
      boolean isOwner) throws ALPageNotFoundException, ALDBErrorException {
    return getEipTSchedule(rundata, context, isOwner, ALEipUtils
      .getUserId(rundata));

  }

  /**
   * Scheudle オブジェクトモデルを取得します。
   *
   * @param rundata
   * @param context
   * @return
   */
  public static EipTSchedule getEipTSchedule(RunData rundata, int scheduleid,
      boolean isOwner, int userid) throws ALPageNotFoundException,
      ALDBErrorException {

    try {
      SelectQuery<EipTSchedule> query = Database.query(EipTSchedule.class);

      // スケジュールID
      Expression exp1 =
        ExpressionFactory.matchDbExp(
          EipTSchedule.SCHEDULE_ID_PK_COLUMN,
          Integer.valueOf(scheduleid));
      query.setQualifier(exp1);

      if (isOwner) {
        // ユーザーID
        Expression exp2 =
          ExpressionFactory.matchExp(EipTSchedule.OWNER_ID_PROPERTY, Integer
            .valueOf(ALEipUtils.getUserId(rundata)));
        query.andQualifier(exp2);
      }

      List<EipTSchedule> schedules = query.fetchList();

      // 指定したSchedule IDのレコードが見つからない場合
      if (schedules == null || schedules.size() == 0) {
        logger.error("[ScheduleUtils] Not found record.");
        throw new ALPageNotFoundException();
      }

      EipTSchedule record = schedules.get(0);

      // 条件が足りないかも（by Komori 2006/06/09）
      SelectQuery<EipTScheduleMap> mapquery =
        Database.query(EipTScheduleMap.class);
      Expression mapexp1 =
        ExpressionFactory.matchExp(EipTScheduleMap.SCHEDULE_ID_PROPERTY, record
          .getScheduleId());
      mapquery.setQualifier(mapexp1);
      Expression mapexp2 =
        ExpressionFactory.matchExp(EipTScheduleMap.USER_ID_PROPERTY, Integer
          .valueOf(userid));
      mapquery.andQualifier(mapexp2);
      Expression mapexp3 =
        ExpressionFactory.matchExp(EipTScheduleMap.USER_ID_PROPERTY, Integer
          .valueOf(userid));
      mapquery.andQualifier(mapexp3);

      List<EipTScheduleMap> schedulemaps = mapquery.fetchList();
      boolean is_member =
        (schedulemaps != null && schedulemaps.size() > 0) ? true : false;

      // boolean is_member = orm_map.count(new Criteria().add(
      // EipTScheduleMapConstants.SCHEDULE_ID, record.getScheduleId()).add(
      // EipTScheduleMapConstants.USER_ID, userid).add(
      // EipTScheduleMapConstants.USER_ID, ALEipUtils.getUserId(rundata))) != 0;

      boolean is_public = "O".equals(record.getPublicFlag());

      // アクセス権限がない場合
      if (!is_member && !is_public) {
        logger.error("[ScheduleUtils] Cannnot access this record. ");
        throw new ALPageNotFoundException();
      }

      return schedules.get(0);

    } catch (Exception ex) {
      logger.error("[ScheduleUtils]", ex);
      throw new ALDBErrorException();
    }
  }

  /**
   * Schedule オブジェクトモデルを取得します。
   *
   * @param rundata
   * @param context
   * @return
   */
  public static EipTSchedule getEipTSchedule(RunData rundata, Context context,
      boolean isOwner, int userid) throws ALPageNotFoundException,
      ALDBErrorException {

    // String org_id = OrgORMappingMap.getInstance().getOrgId(rundata);

    // スケジュールIDをパラメータから取得
    String scheduleid =
      rundata.getParameters().getString(ALEipConstants.ENTITY_ID);

    try {
      // IDが空の場合 || IDがintでない場合
      if (scheduleid == null || Integer.valueOf(scheduleid) == null) {
        logger.error("[ScheduleUtils] ENTITYID is empty.");
        throw new ALPageNotFoundException();
      }
    } catch (NumberFormatException ex) {
      logger.error("[ScheduleUtils] NumberFormatException: ENTITYID is wrong.");
      throw new ALPageNotFoundException();

    }

    try {
      SelectQuery<EipTSchedule> query = Database.query(EipTSchedule.class);

      // スケジュールID
      Expression exp1 =
        ExpressionFactory.matchDbExp(
          EipTSchedule.SCHEDULE_ID_PK_COLUMN,
          scheduleid);
      query.setQualifier(exp1);

      if (isOwner) {
        // ユーザーID
        Expression exp2 =
          ExpressionFactory.matchExp(EipTSchedule.OWNER_ID_PROPERTY, Integer
            .valueOf(ALEipUtils.getUserId(rundata)));
        query.andQualifier(exp2);
      }

      List<EipTSchedule> schedules = query.fetchList();

      // 指定したSchedule IDのレコードが見つからない場合
      if (schedules == null || schedules.size() == 0) {
        logger.error("[ScheduleUtils] Not found record.");
        throw new ALPageNotFoundException();
      }

      EipTSchedule record = schedules.get(0);

      // Integer.valueOf(userid)
      // 条件が足りないかも（by Komori 2006/06/09）
      SelectQuery<EipTScheduleMap> mapquery =
        Database.query(EipTScheduleMap.class);
      Expression mapexp1 =
        ExpressionFactory.matchExp(EipTScheduleMap.SCHEDULE_ID_PROPERTY, record
          .getScheduleId());
      mapquery.setQualifier(mapexp1);
      Expression mapexp21 =
        ExpressionFactory.matchExp(EipTScheduleMap.USER_ID_PROPERTY, ALEipUtils
          .getUserId(rundata));
      Expression mapexp22 =
        ExpressionFactory.matchExp(EipTScheduleMap.EIP_TSCHEDULE_PROPERTY
          + "."
          + EipTSchedule.CREATE_USER_ID_PROPERTY, Integer.valueOf(ALEipUtils
          .getUserId(rundata)));
      mapquery.andQualifier(mapexp21.orExp(mapexp22));

      List<EipTScheduleMap> schedulemaps = mapquery.fetchList();
      boolean is_member =
        (schedulemaps != null && schedulemaps.size() > 0) ? true : false;

      // boolean is_member = orm_map.count(new Criteria().add(
      // EipTScheduleMapConstants.SCHEDULE_ID, record.getScheduleId()).add(
      // EipTScheduleMapConstants.USER_ID, userid).add(
      // EipTScheduleMapConstants.USER_ID, ALEipUtils.getUserId(rundata))) != 0;

      int loginuser_id = ALEipUtils.getUserId(rundata);
      boolean is_owner = record.getOwnerId().intValue() == loginuser_id;
      boolean is_createuser =
        loginuser_id == record.getCreateUserId().intValue();
      boolean is_public = "O".equals(record.getPublicFlag());

      // アクセス権限がない場合
      if (!is_public && !is_member && (!(is_createuser || is_owner))) {
        ALEipUtils.redirectPermissionError(rundata);
      }

      return schedules.get(0);

    } catch (Exception ex) {
      logger.error("[ScheduleUtils]", ex);
      throw new ALDBErrorException();
    }
  }

  /**
   * スケジュールへのアクセス権限があるかどうかを調べます。
   *
   * @param rundata
   * @param context
   * @return
   */
  public static boolean hasAuthorityForScheduleDetail(RunData rundata,
      Context context, EipTSchedule record, String type)
      throws ALPageNotFoundException, ALDBErrorException {
    try {
      int userId = ALEipUtils.getUserId(rundata);

      SelectQuery<EipTScheduleMap> mapquery =
        Database.query(EipTScheduleMap.class);
      Expression mapexp1 =
        ExpressionFactory.matchExp(EipTScheduleMap.SCHEDULE_ID_PROPERTY, record
          .getScheduleId());
      mapquery.setQualifier(mapexp1);
      Expression mapexp21 =
        ExpressionFactory.matchExp(EipTScheduleMap.USER_ID_PROPERTY, Integer
          .toString(userId));
      Expression mapexp22 =
        ExpressionFactory.matchExp(EipTScheduleMap.EIP_TSCHEDULE_PROPERTY
          + "."
          + EipTSchedule.CREATE_USER_ID_PROPERTY, Integer.valueOf(userId));
      mapquery.andQualifier(mapexp21.orExp(mapexp22));
      // 設備は除外する
      Expression exp3 =
        ExpressionFactory.matchExp(
          EipTScheduleMap.TYPE_PROPERTY,
          ScheduleUtils.SCHEDULEMAP_TYPE_USER);
      mapquery.andQualifier(exp3);

      List<EipTScheduleMap> schedulemaps = mapquery.fetchList();
      boolean is_member =
        (schedulemaps != null && schedulemaps.size() > 0) ? true : false;

      boolean is_owner = record.getOwnerId().intValue() == userId;
      boolean is_createuser = record.getCreateUserId().intValue() == userId;
      boolean is_public = "O".equals(record.getPublicFlag());

      // アクセス権限がない場合
      if (is_public) {
      } else if (!is_member && (!(is_createuser || is_owner))) {
        return false;
      }
      return true;
    } catch (Exception ex) {
      logger.error("[ScheduleUtils]", ex);
      throw new ALDBErrorException();
    }
  }

  /**
   * ツールチップ表示用の Schedule オブジェクトモデルを取得する．
   *
   * @param rundata
   * @param context
   * @return
   */
  public static EipTSchedule getEipTScheduleDetailForTooltip(RunData rundata,
      Context context, String type) throws ALPageNotFoundException,
      ALDBErrorException {

    String scheduleid = rundata.getParameters().getString("scheduleid");

    try {
      if (scheduleid == null || Integer.valueOf(scheduleid) == null) {
        logger.error("[ScheduleUtils] ENTITYID is empty.");
        return null;
      }

      SelectQuery<EipTSchedule> query = Database.query(EipTSchedule.class);
      query.getQuery().setRefreshingObjects(true);

      Expression exp1 =
        ExpressionFactory.matchDbExp(
          EipTSchedule.SCHEDULE_ID_PK_COLUMN,
          scheduleid);
      query.setQualifier(exp1);

      Expression exp2 =
        ExpressionFactory.matchExp(EipTSchedule.EIP_TSCHEDULE_MAPS_PROPERTY
          + "."
          + EipTScheduleMap.TYPE_PROPERTY, type);
      query.andQualifier(exp2);

      List<EipTSchedule> schedules = query.fetchList();

      if (schedules == null || schedules.size() == 0) {
        logger.error("[ScheduleUtils] Not found record.");
        return null;
      }

      EipTSchedule record = schedules.get(0);
      if (!hasAuthorityForScheduleDetail(rundata, context, record, type)) {
        return null;
      }
      return record;
    } catch (Exception ex) {
      logger.error("[ScheduleUtils]", ex);
      throw new ALDBErrorException();
    }
  }

  /**
   * 詳細表示用の Schedule オブジェクトモデルを取得する．
   *
   * @param rundata
   * @param context
   * @return
   */
  public static EipTSchedule getEipTScheduleDetail(RunData rundata,
      Context context, String type) throws ALPageNotFoundException,
      ALDBErrorException {

    // スケジュールIDをパラメータから取得
    String scheduleid =
      rundata.getParameters().getString(ALEipConstants.ENTITY_ID);

    try {
      // IDが空の場合 || IDがintでない場合
      if (scheduleid == null || Integer.valueOf(scheduleid) == null) {
        logger.error("[ScheduleUtils] ENTITYID is empty.");
        throw new ALPageNotFoundException();
      }
    } catch (NumberFormatException ex) {
      logger.error(
        "[ScheduleUtils] NumberFormatException: ENTITYID is wrong.",
        ex);
      throw new ALPageNotFoundException();

    }

    try {
      SelectQuery<EipTSchedule> query = Database.query(EipTSchedule.class);
      query.getQuery().setRefreshingObjects(true);

      // スケジュールID
      Expression exp1 =
        ExpressionFactory.matchDbExp(
          EipTSchedule.SCHEDULE_ID_PK_COLUMN,
          scheduleid);
      query.setQualifier(exp1);

      // ユーザのスケジュール
      Expression exp2 =
        ExpressionFactory.matchExp(EipTSchedule.EIP_TSCHEDULE_MAPS_PROPERTY
          + "."
          + EipTScheduleMap.TYPE_PROPERTY, type);
      query.andQualifier(exp2);

      List<EipTSchedule> schedules = query.fetchList();

      boolean activity;
      String option = rundata.getParameters().getString("activityId", null);
      if (option != null && option.length() > 0) {
        activity = true;
      } else {
        activity = false;
      }
      // 指定したSchedule IDのレコードが見つからない場合
      if (schedules == null || schedules.size() == 0) {
        if (activity) {// アクテシビティから入った
          logger.error("[ScheduleUtils] Not found record.");
          throw new ScheduleNotFounfException();
        } else {// ポートレットからはいった。
          logger.error("[ScheduleUtils] Not found record.");
          throw new ALPageNotFoundException();
        }
      }

      EipTSchedule record = schedules.get(0);
      if (!hasAuthorityForScheduleDetail(rundata, context, record, type)) {
        ALEipUtils.redirectPermissionError(rundata);
      }
      return record;
    } catch (ALPageNotFoundException ex) {
      ALEipUtils.redirectPageNotFound(rundata);
      return null;
    } catch (ScheduleNotFounfException ex) {
      shceduleNotFound(rundata);
      return null;
    } catch (Exception ex) {
      logger.error("[ScheduleUtils]", ex);
      throw new ALDBErrorException();
    }
  }

  /**
   * @param rundata
   */
  private static Boolean shceduleNotFound(RunData rundata) {
    try {
      JetspeedLink jsLink = JetspeedLinkFactory.getInstance(rundata);
      DynamicURI duri = jsLink.getPage();
      String template =
        rundata.getParameters().getString(JetspeedResources.PATH_TEMPLATE_KEY);
      if (template != null && !("".equals(template))) {
        VelocityContext context = new VelocityContext();
        ALEipUtils.setupContext(rundata, context);
        try {
          ServletOutputStream out = null;
          HttpServletResponse response = rundata.getResponse();
          out = response.getOutputStream();
          BufferedWriter writer =
            new BufferedWriter(new OutputStreamWriter(
              out,
              ALEipConstants.DEF_CONTENT_ENCODING));
          context.put("l10n", ALLocalizationUtils.createLocalization(rundata));
          Template templete =
            Velocity.getTemplate("portlets/html/ajax-schedule-not-found.vm");
          templete.merge(context, writer);
          writer.flush();
          writer.close();
        } catch (ResourceNotFoundException e) {
          logger.error("ALEipUtils.redirectPageNotFound", e);
          throw new RuntimeException(e);
        } catch (ParseErrorException e) {
          logger.error("ALEipUtils.redirectPageNotFound", e);
          throw new RuntimeException(e);
        } catch (Exception e) {
          logger.error("ALEipUtils.redirectPageNotFound", e);
          throw new RuntimeException(e);
        }
        return true;
      }
      duri.addPathInfo("template", "PageNotFound");
      rundata.setRedirectURI(duri.toString());
      rundata.getResponse().sendRedirect(rundata.getRedirectURI());

      JetspeedLinkFactory.putInstance(jsLink);
      jsLink = null;
      return true;
    } catch (TurbineException e) {

      logger.error("ALEipUtils.redirectPageNotFound", e);
      return false;
    } catch (IOException e) {

      logger.error("ALEipUtils.redirectPageNotFound", e);
      return false;
    }
  }

  /**
   * ScheudleMap オブジェクトモデルを取得します。
   *
   * @param rundata
   * @param context
   * @return
   */
  public static EipTScheduleMap getEipTScheduleMap(RunData rundata,
      Context context) throws ALPageNotFoundException, ALDBErrorException {

    // スケジュールIDをパラメータから取得
    String scheduleid =
      rundata.getParameters().getString(ALEipConstants.ENTITY_ID);

    try {
      // IDが空の場合 || IDがintでない場合
      if (scheduleid == null || Integer.valueOf(scheduleid) == null) {
        logger.error("[ScheduleUtils] ENTITYID is empty.");
        throw new ALPageNotFoundException();
      }
    } catch (NumberFormatException ex) {
      logger.error(
        "[ScheduleUtils] NumberFormatException: ENTITYID is wrong.",
        ex);
      throw new ALPageNotFoundException();

    }

    try {
      SelectQuery<EipTScheduleMap> query =
        Database.query(EipTScheduleMap.class);

      // スケジュールID
      Expression exp1 =
        ExpressionFactory.matchExp(
          EipTScheduleMap.SCHEDULE_ID_PROPERTY,
          scheduleid);
      query.setQualifier(exp1);
      // ユーザーID
      Expression exp2 =
        ExpressionFactory.matchExp(EipTScheduleMap.USER_ID_PROPERTY, Integer
          .valueOf(ALEipUtils.getUserId(rundata)));
      query.andQualifier(exp2);

      // typeが"U"のとき抽出
      Expression exp3 =
        ExpressionFactory.matchExp(
          EipTScheduleMap.TYPE_PROPERTY,
          SCHEDULEMAP_TYPE_USER);
      query.andQualifier(exp3);

      List<EipTScheduleMap> schedules = query.fetchList();

      // 指定したIDのレコードが見つからない場合
      if (schedules == null || schedules.size() == 0) {
        logger.error("[ScheduleUtils] Not found record.");
        throw new ALPageNotFoundException();
      }

      return schedules.get(0);

    } catch (Exception ex) {
      logger.error("[ScheduleUtils]", ex);
      throw new ALDBErrorException();

    }
  }

  public static List<EipTScheduleMap> getEipTScheduleMaps(EipTSchedule schedule)
      throws ALPageNotFoundException, ALDBErrorException {

    // スケジュールIDを取得
    Integer scheduleid = schedule.getScheduleId();

    try {
      SelectQuery<EipTScheduleMap> query =
        Database.query(EipTScheduleMap.class);

      // スケジュールID
      Expression exp1 =
        ExpressionFactory.matchExp(
          EipTScheduleMap.SCHEDULE_ID_PROPERTY,
          scheduleid);
      query.setQualifier(exp1);

      List<EipTScheduleMap> schedules = query.fetchList();

      // 指定したIDのレコードが見つからない場合
      if (schedules == null || schedules.size() == 0) {
        logger.error("[ScheduleUtils] Not found record.");
        throw new ALPageNotFoundException();
      }

      return schedules;

    } catch (Exception ex) {
      logger.error("[ScheduleUtils]", ex);
      throw new ALDBErrorException();

    }
  }

  /**
   * 共有メンバーを取得します。
   *
   * @param rundata
   * @param context
   * @param includeLoginUser
   *          ログインユーザーを共有メンバーとして取り扱う場合，true．
   * @return
   */
  public static List<ALEipUser> getUsers(RunData rundata, Context context,
      boolean includeLoginUser) throws ALPageNotFoundException,
      ALDBErrorException {
    List<ALEipUser> list = new ArrayList<ALEipUser>();

    // スケジュールIDをパラメータから取得
    String scheduleid =
      rundata.getParameters().getString(ALEipConstants.ENTITY_ID);

    try {
      // IDが空の場合 || IDがintでない場合
      if (scheduleid == null || Integer.valueOf(scheduleid) == null) {
        logger.error("[ScheduleUtils] ENTITYID is empty.");
        throw new ALPageNotFoundException();
      }
    } catch (NumberFormatException ex) {
      logger.error(
        "[ScheduleUtils] NumberFormatException: ENTITYID is wrong.",
        ex);
      throw new ALPageNotFoundException();

    }

    try {
      SelectQuery<EipTScheduleMap> mapquery =
        Database.query(EipTScheduleMap.class);

      // スケジュールID
      Expression exp1 =
        ExpressionFactory.matchExp(
          EipTScheduleMap.SCHEDULE_ID_PROPERTY,
          Integer.valueOf(scheduleid));
      mapquery.setQualifier(exp1);
      if (!includeLoginUser) {
        Expression exp2 =
          ExpressionFactory.noMatchExp(
            EipTScheduleMap.USER_ID_PROPERTY,
            Integer.valueOf(ALEipUtils.getUserId(rundata)));
        mapquery.andQualifier(exp2);
      }
      // 設備は除外する
      Expression exp3 =
        ExpressionFactory.matchExp(
          EipTScheduleMap.TYPE_PROPERTY,
          ScheduleUtils.SCHEDULEMAP_TYPE_USER);
      mapquery.andQualifier(exp3);
      List<EipTScheduleMap> schedulemaps = mapquery.fetchList();

      List<Integer> uidlist = new ArrayList<Integer>();
      EipTScheduleMap map = null;
      int mapsize = schedulemaps.size();
      for (int i = 0; i < mapsize; i++) {
        map = schedulemaps.get(i);
        if (!uidlist.contains(map.getUserId())) {
          uidlist.add(map.getUserId());
        }
      }

      SelectQuery<TurbineUser> userquery = Database.query(TurbineUser.class);
      Expression userexp =
        ExpressionFactory.inDbExp(TurbineUser.USER_ID_PK_COLUMN, uidlist);
      userquery.setQualifier(userexp);
      List<Ordering> orders = new ArrayList<Ordering>();
      orders.add(new Ordering(TurbineUser.LAST_NAME_KANA_PROPERTY, true));
      orders.add(new Ordering(TurbineUser.FIRST_NAME_KANA_PROPERTY, true));
      userquery.getQuery().addOrderings(orders);

      List<TurbineUser> ulist = userquery.fetchList();

      TurbineUser tuser;
      ALEipUser user;
      for (int j = 0; j < ulist.size(); j++) {
        tuser = ulist.get(j);
        user = new ALEipUser();
        user.initField();
        user.setUserId(tuser.getUserId().intValue());
        user.setName(tuser.getLoginName());
        user.setAliasName(tuser.getFirstName(), tuser.getLastName());
        list.add(user);
      }

    } catch (Exception e) {
      logger.error("[ScheduleUtils]", e);
      throw new ALDBErrorException();

    }
    return list;
  }

  /**
   * ファイルオブジェクトモデルを取得します。 <BR>
   *
   * @param rundata
   * @param context
   * @return
   */
  public static EipTScheduleFile getEipTScheduleFile(RunData rundata)
      throws ALPageNotFoundException, ALDBErrorException {
    try {
      int attachmentIndex =
        rundata.getParameters().getInt("attachmentIndex", -1);
      if (attachmentIndex < 0) {
        // ID が空の場合
        logger.debug("[ScheduleUtils] Empty ID...");
        throw new ALPageNotFoundException();

      }

      SelectQuery<EipTScheduleFile> query =
        Database.query(EipTScheduleFile.class);
      Expression exp =
        ExpressionFactory.matchDbExp(
          EipTScheduleFile.FILE_ID_PK_COLUMN,
          Integer.valueOf(attachmentIndex));
      query.andQualifier(exp);
      List<EipTScheduleFile> files = query.fetchList();
      if (files == null || files.size() == 0) {
        // 指定した ID のレコードが見つからない場合
        logger.debug("[ScheduleUtils] Not found ID...");
        throw new ALPageNotFoundException();
      }
      return files.get(0);
    } catch (Exception ex) {
      logger.error("[ScheduleUtils]", ex);
      throw new ALDBErrorException();
    }
  }

  /**
   * 設備メンバーを取得します。
   *
   * @param rundata
   * @param context
   * @return
   */
  public static int[] getFacilityIds(EipTSchedule schedule)
      throws ALPageNotFoundException, ALDBErrorException {
    List<?> list = schedule.getEipTScheduleMaps();

    List<Integer> flist = new ArrayList<Integer>();
    EipTScheduleMap map = null;
    int size = list.size();
    for (int i = 0; i < size; i++) {
      map = (EipTScheduleMap) list.get(i);
      if (SCHEDULEMAP_TYPE_FACILITY.equals(map.getType())) {
        flist.add(map.getUserId());
      }
    }

    int fsize = flist.size();
    int[] ids = new int[fsize];
    for (int i = 0; i < fsize; i++) {
      ids[i] = (flist.get(i)).intValue();
    }
    return ids;
  }

  /**
   * 指定した繰り返しパターンにマッチするかどうかを返します。
   *
   * @param date
   * @param ptn
   * @param startDate
   * @param limitDate
   * @return
   */
  public static boolean isView(ALDateTimeField date, String ptn,
      Date startDate, Date limitDate) {
    int count = 0;
    boolean result = false;
    Calendar cal = Calendar.getInstance();
    cal.setTime(date.getValue());
    // 毎日
    if (ptn.charAt(0) == 'D') {
      result = true;
      count = 1;
      // 毎週, 第何週
    } else if (ptn.charAt(0) == 'W') {

      int dow = cal.get(Calendar.DAY_OF_WEEK);
      // 第何週目かを表すフィールド
      int dowim = cal.get(Calendar.DAY_OF_WEEK_IN_MONTH);
      if (ptn.charAt(8) == 'N'
        || ptn.charAt(8) == 'L'
        || dowim == Character.getNumericValue(ptn.charAt(8))) {
        switch (dow) {
        // 日
          case Calendar.SUNDAY:
            result = ptn.charAt(1) != '0';
            break;
          // 月
          case Calendar.MONDAY:
            result = ptn.charAt(2) != '0';
            break;
          // 火
          case Calendar.TUESDAY:
            result = ptn.charAt(3) != '0';
            break;
          // 水
          case Calendar.WEDNESDAY:
            result = ptn.charAt(4) != '0';
            break;
          // 木
          case Calendar.THURSDAY:
            result = ptn.charAt(5) != '0';
            break;
          // 金
          case Calendar.FRIDAY:
            result = ptn.charAt(6) != '0';
            break;
          // 土
          case Calendar.SATURDAY:
            result = ptn.charAt(7) != '0';
            break;
          default:
            result = false;
            break;
        }
        if (ptn.length() == 9) {
          count = 8;
        } else {
          count = 9;
        }
      }
      // 毎月
    } else if (ptn.charAt(0) == 'M') {
      int mday;
      if (ptn.substring(1, 3).equals("XX")) {
        mday = cal.getActualMaximum(Calendar.DATE);
      } else {
        mday = Integer.parseInt(ptn.substring(1, 3));
      }
      result = Integer.parseInt(date.getDay()) == mday;
      count = 3;
    } else if (ptn.charAt(0) == 'Y') {
      int ymonth = Integer.parseInt(ptn.substring(1, 3));
      int yday = Integer.parseInt(ptn.substring(3, 5));
      int month = Integer.parseInt(date.getMonth());
      int day = Integer.parseInt(date.getDay());
      if (ymonth == month && yday == day) {
        result = true;
        count = 5;
      } else {
        result = false;
      }
    } else {
      return true;
    }

    if (result) {
      if (ptn.charAt(count) == 'L') {
        // 締め切り日がある場合
        if (equalsToDate(startDate, date.getValue(), false)
          || equalsToDate(limitDate, date.getValue(), false)) {
          // 繰り返しの開始日と終了日
          result = true;
        } else {
          // 繰り返しの開始日と終了日の間に指定した日が入っているかを検証．
          result =
            result
              && startDate.before(cal.getTime())
              && limitDate.after(cal.getTime());
        }
      }
    }

    return result;
  }

  /**
   * 指定したエントリー名を持つ個人設定ページに含まれるポートレットへの URI を取得する．
   *
   * @param rundata
   * @param portletEntryName
   *          PSML ファイルに記述されているタグ entry の要素 parent
   * @return
   */
  public static String getPortletURIinPersonalConfigPane(RunData rundata,
      String portletEntryName) {
    try {
      Portlets portlets =
        ((JetspeedRunData) rundata).getProfile().getDocument().getPortlets();
      if (portlets == null) {
        return null;
      }

      Portlets[] portletList = portlets.getPortletsArray();
      if (portletList == null) {
        return null;
      }

      int length = portletList.length;
      for (int i = 0; i < length; i++) {
        Entry[] entries = portletList[i].getEntriesArray();
        if (entries == null || entries.length <= 0) {
          continue;
        }

        int ent_length = entries.length;
        for (int j = 0; j < ent_length; j++) {
          if (entries[j].getParent().equals(portletEntryName)) {
            JetspeedLink jsLink = JetspeedLinkFactory.getInstance(rundata);

            DynamicURI duri =
              jsLink.getLink(
                JetspeedLink.CURRENT,
                null,
                null,
                JetspeedLink.CURRENT,
                null);
            duri =
              duri
                .addPathInfo(
                  JetspeedResources.PATH_PANEID_KEY,
                  portletList[i].getId() + "," + entries[j].getId())
                .addQueryData(
                  JetspeedResources.PATH_ACTION_KEY,
                  "controls.Restore");
            return duri.toString();
          }
        }
      }
    } catch (Exception ex) {
      logger.error("schedule", ex);
      return null;
    }
    return null;
  }

  public static String getPortletURItoTodoDetailPane(RunData rundata,
      String portletEntryName, long entityid, String schedulePortletId) {
    return getPortletURItoTodoModePane(
      rundata,
      portletEntryName,
      entityid,
      schedulePortletId,
      "detail");
  }

  public static String getPortletURItoTodoPublicDetailPane(RunData rundata,
      String portletEntryName, long entityid, String schedulePortletId) {
    return getPortletURItoTodoModePane(
      rundata,
      portletEntryName,
      entityid,
      schedulePortletId,
      "public_detail");
  }

  public static String getPortletURItoTodoModePane(RunData rundata,
      String portletEntryName, long entityid, String schedulePortletId,
      String mode) {
    if (mode == null || "".equals(mode)) {
      return null;
    }

    try {
      Portlets portlets =
        ((JetspeedRunData) rundata).getProfile().getDocument().getPortlets();
      if (portlets == null) {
        return null;
      }

      Portlets[] portletList = portlets.getPortletsArray();
      if (portletList == null) {
        return null;
      }

      int length = portletList.length;
      for (int i = 0; i < length; i++) {
        Entry[] entries = portletList[i].getEntriesArray();
        if (entries == null || entries.length <= 0) {
          continue;
        }

        int ent_length = entries.length;
        for (int j = 0; j < ent_length; j++) {
          if (entries[j].getParent().equals(portletEntryName)) {
            JetspeedLink jsLink = JetspeedLinkFactory.getInstance(rundata);

            DynamicURI duri =
              jsLink.getLink(
                JetspeedLink.CURRENT,
                null,
                null,
                JetspeedLink.CURRENT,
                null);
            duri =
              duri
                .addPathInfo(
                  JetspeedResources.PATH_PANEID_KEY,
                  portletList[i].getId())
                .addPathInfo(
                  JetspeedResources.PATH_PORTLETID_KEY,
                  entries[j].getId())
                .addQueryData(
                  JetspeedResources.PATH_ACTION_KEY,
                  "controls.Maximize")
                .addQueryData(ALEipConstants.MODE, mode)
                .addQueryData(ALEipConstants.ENTITY_ID, entityid)
                .addQueryData("sch", schedulePortletId);
            int jmode = ((JetspeedRunData) rundata).getMode();
            if (jmode == JetspeedRunData.MAXIMIZE) {
              duri.addQueryData("prev", JetspeedRunData.MAXIMIZE);
            } else {
              duri.addQueryData("prev", JetspeedRunData.NORMAL);
            }
            return duri.toString();
          }
        }
      }
    } catch (Exception ex) {
      logger.error("schedule", ex);
      return null;
    }
    return null;
  }

  public static String getPortletURItoTodoDetailPaneForCell(RunData rundata,
      String portletEntryName, long entityid, String schedulePortletId) {
    return getPortletURItoTodoModePaneForCell(
      rundata,
      portletEntryName,
      entityid,
      schedulePortletId,
      "detail");
  }

  public static String getPortletURItoTodoModePaneForCell(RunData rundata,
      String portletEntryName, long entityid, String schedulePortletId,
      String mode) {
    if (mode == null || "".equals(mode)) {
      return null;
    }

    try {
      Portlets portlets =
        ((JetspeedRunData) rundata).getProfile().getDocument().getPortlets();
      if (portlets == null) {
        return null;
      }

      Entry[] entries = portlets.getEntriesArray();
      if (entries == null || entries.length <= 0) {
        return null;
      }

      int ent_length = entries.length;
      for (int j = 0; j < ent_length; j++) {
        if (entries[j].getParent().equals(portletEntryName)) {
          JetspeedLink jsLink = JetspeedLinkFactory.getInstance(rundata);

          DynamicURI duri =
            jsLink.getLink(
              JetspeedLink.CURRENT,
              null,
              null,
              JetspeedLink.CURRENT,
              null);
          duri =
            duri
              .addPathInfo(
                JetspeedResources.PATH_PORTLETID_KEY,
                entries[j].getId())
              .addQueryData(
                JetspeedResources.PATH_ACTION_KEY,
                "controls.Maximize")
              .addQueryData(ALEipConstants.MODE, mode)
              .addQueryData(ALEipConstants.ENTITY_ID, entityid)
              .addQueryData("sch", schedulePortletId);
          return duri.toString();
        }
      }

    } catch (Exception ex) {
      logger.error("schedule", ex);
      return null;
    }
    return null;
  }

  /**
   * 指定した2つの日付を比較する．
   *
   * @param date1
   * @param date2
   * @param checkTime
   *          時間まで比較する場合，true．
   * @return 等しい場合，true．
   */
  public static boolean equalsToDate(Date date1, Date date2, boolean checkTime) {
    Calendar cal = Calendar.getInstance();
    cal.setTime(date1);
    int date1Year = cal.get(Calendar.YEAR);
    int date1Month = cal.get(Calendar.MONTH) + 1;
    int date1Day = cal.get(Calendar.DATE);
    int date1Hour = cal.get(Calendar.HOUR);
    int date1Minute = cal.get(Calendar.MINUTE);
    cal.setTime(date2);
    int date2Year = cal.get(Calendar.YEAR);
    int date2Month = cal.get(Calendar.MONTH) + 1;
    int date2Day = cal.get(Calendar.DATE);
    int date2Hour = cal.get(Calendar.HOUR);
    int date2Minute = cal.get(Calendar.MINUTE);
    if (checkTime) {
      if (date1Year == date2Year
        && date1Month == date2Month
        && date1Day == date2Day
        && date1Hour == date2Hour
        && date1Minute == date2Minute) {
        return true;
      }
    } else {
      if (date1Year == date2Year
        && date1Month == date2Month
        && date1Day == date2Day) {
        return true;
      }
    }
    return false;
  }

  /**
   * 指定した2つの日付を比較する．
   *
   * @param date1
   * @param date2
   * @return
   */
  public static int compareToDate(Date date1, Date date2) {
    Calendar cal = Calendar.getInstance();
    cal.setTime(date1);
    int date1Year = cal.get(Calendar.YEAR);
    int date1Month = cal.get(Calendar.MONTH) + 1;
    int date1Day = cal.get(Calendar.DATE);
    cal.setTime(date2);
    int date2Year = cal.get(Calendar.YEAR);
    int date2Month = cal.get(Calendar.MONTH) + 1;
    int date2Day = cal.get(Calendar.DATE);

    if (date1Year == date2Year
      && date1Month == date2Month
      && date1Day == date2Day) {
      return 0;
    }

    if (date1Year < date2Year) {
      return 1;
    } else if (date2Year > date1Year) {
      return -1;
    } else {
      if (date1Month < date2Month) {
        return 1;
      } else if (date2Month > date1Month) {
        return -1;
      } else {
        if (date1Day < date2Day) {
          return 1;
        } else {
          return -1;
        }
      }
    }
  }

  /**
   * 指定した2つの時刻のみを比較する．
   *
   * @param date1
   * @param date2
   * @return
   */
  public static int compareTime(Date date1, Date date2) {
    Calendar cal = Calendar.getInstance();
    cal.setTime(date1);
    int date1Hour = cal.get(Calendar.HOUR_OF_DAY);
    int date1Minute = cal.get(Calendar.MINUTE);
    cal.setTime(date2);
    int date2Hour = cal.get(Calendar.HOUR_OF_DAY);
    int date2Minute = cal.get(Calendar.MINUTE);

    if (date1Hour == date2Hour && date1Minute == date2Minute) {
      return 0;
    }

    if (date1Hour < date2Hour) {
      return 1;
    } else if (date1Hour > date2Hour) {
      return -1;
    } else {
      if (date1Minute < date2Minute) {
        return 1;
      } else {
        return -1;
      }
    }

  }

  /**
   * ダミースケジュールを登録する．
   *
   * @param schedule
   * @param ownerid
   * @param startDate
   * @param endDate
   * @param memberIdList
   * @throws ALDBErrorException
   */
  public static void insertDummySchedule(EipTSchedule schedule, int ownerid,
      Date startDate, Date endDate, int[] memberIdList, int[] facilityIdList)
      throws ALDBErrorException {

    // ダミーのスケジュールを登録する．
    EipTSchedule dummySchedule = Database.create(EipTSchedule.class);

    // 親スケジュール ID
    dummySchedule.setParentId(schedule.getScheduleId());
    // タイトル
    dummySchedule.setName("dummy");
    // 場所
    dummySchedule.setPlace("");
    // 内容
    dummySchedule.setNote("");
    // 公開フラグ
    dummySchedule.setPublicFlag("P");
    // 共有メンバーによる編集／削除フラグ
    dummySchedule.setEditFlag("F");
    // オーナーID
    dummySchedule.setOwnerId(Integer.valueOf(ownerid));
    // 作成日
    Date now2 = new Date();
    dummySchedule.setCreateDate(now2);
    dummySchedule.setCreateUserId(Integer.valueOf(ownerid));
    // 更新日
    dummySchedule.setUpdateDate(now2);
    dummySchedule.setUpdateUserId(Integer.valueOf(ownerid));
    dummySchedule.setRepeatPattern("N");
    dummySchedule.setStartDate(startDate);
    dummySchedule.setEndDate(endDate);

    EipTCommonCategory category =
      CommonCategoryUtils.getEipTCommonCategory(Long.valueOf(1));

    // orm.doInsert(dummySchedule);
    int size = memberIdList.length;
    for (int i = 0; i < size; i++) {
      EipTScheduleMap map = Database.create(EipTScheduleMap.class);
      int userid = memberIdList[i];
      map.setEipTSchedule(dummySchedule);
      map.setUserId(Integer.valueOf(userid));

      // D: ダミースケジュール
      map.setStatus("D");
      map.setType(ScheduleUtils.SCHEDULEMAP_TYPE_USER);

      map.setCommonCategoryId(Integer.valueOf(1));
      map.setEipTCommonCategory(category);
    }

    if (facilityIdList != null && facilityIdList.length > 0) {
      int fsize = facilityIdList.length;
      for (int i = 0; i < fsize; i++) {
        EipTScheduleMap map = Database.create(EipTScheduleMap.class);
        int fid = facilityIdList[i];
        map.setEipTSchedule(dummySchedule);
        map.setUserId(Integer.valueOf(fid));

        // D: ダミースケジュール
        map.setStatus("D");
        map.setType(ScheduleUtils.SCHEDULEMAP_TYPE_FACILITY);

        map.setCommonCategoryId(Integer.valueOf(1));
        map.setEipTCommonCategory(category);
      }
    }

    // スケジュールを登録
    Database.commit();
  }

  /**
   * ダミースケジュールを登録する（携帯電話対応時には上記のメソッドに変更する）．
   *
   * @param schedule
   * @param ownerid
   * @param startDate
   * @param endDate
   * @param memberIdList
   * @throws ALDBErrorException
   */
  public static void insertDummySchedule(EipTSchedule schedule, int ownerid,
      Date startDate, Date endDate, int[] memberIdList)
      throws ALDBErrorException {

    // ダミーのスケジュールを登録する．
    EipTSchedule dummySchedule = Database.create(EipTSchedule.class);
    // 親スケジュール ID
    dummySchedule.setParentId(schedule.getScheduleId());
    // タイトル
    dummySchedule.setName("dummy");
    // 場所
    dummySchedule.setPlace("");
    // 内容
    dummySchedule.setNote("");
    // 公開フラグ
    dummySchedule.setPublicFlag("P");
    // 共有メンバーによる編集／削除フラグ
    dummySchedule.setEditFlag("F");
    // オーナーID
    dummySchedule.setOwnerId(Integer.valueOf(ownerid));
    // 作成日
    Date now2 = new Date();
    dummySchedule.setCreateDate(now2);
    dummySchedule.setCreateUserId(Integer.valueOf(ownerid));
    // 更新日
    dummySchedule.setUpdateDate(now2);
    dummySchedule.setUpdateUserId(Integer.valueOf(ownerid));
    dummySchedule.setRepeatPattern("N");
    dummySchedule.setStartDate(startDate);
    dummySchedule.setEndDate(endDate);

    EipTCommonCategory category =
      CommonCategoryUtils.getEipTCommonCategory(Long.valueOf(1));

    // // スケジュールを登録
    // orm.doInsert(dummySchedule);
    int size = memberIdList.length;
    for (int i = 0; i < size; i++) {
      EipTScheduleMap map = Database.create(EipTScheduleMap.class);
      int userid = memberIdList[i];
      // map.setPrimaryKey(dummySchedule.getScheduleId(), userid);
      map.setEipTSchedule(dummySchedule);
      map.setUserId(Integer.valueOf(userid));
      // D: ダミースケジュール
      map.setStatus("D");
      map.setType(ScheduleUtils.SCHEDULEMAP_TYPE_USER);

      map.setCommonCategoryId(Integer.valueOf(1));
      map.setEipTCommonCategory(category);
    }

    // スケジュールを登録
    Database.commit();
  }

  public static void insertDummySchedule(EipTSchedule schedule, int ownerid,
      Date startDate, Date endDate, List<Integer> memberIdList,
      List<Integer> facilityIdList) throws ALDBErrorException {

    // ダミーのスケジュールを登録する．
    EipTSchedule dummySchedule = Database.create(EipTSchedule.class);

    // 親スケジュール ID
    dummySchedule.setParentId(schedule.getScheduleId());
    // タイトル
    dummySchedule.setName("dummy");
    // 場所
    dummySchedule.setPlace("");
    // 内容
    dummySchedule.setNote("");
    // 公開フラグ
    dummySchedule.setPublicFlag("P");
    // 共有メンバーによる編集／削除フラグ
    dummySchedule.setEditFlag("F");
    // オーナーID
    dummySchedule.setOwnerId(Integer.valueOf(ownerid));
    // 作成日
    Date now2 = new Date();
    dummySchedule.setCreateDate(now2);
    dummySchedule.setCreateUserId(Integer.valueOf(ownerid));
    // 更新日
    dummySchedule.setUpdateDate(now2);
    dummySchedule.setUpdateUserId(Integer.valueOf(ownerid));
    dummySchedule.setRepeatPattern("N");
    dummySchedule.setStartDate(startDate);
    dummySchedule.setEndDate(endDate);

    EipTCommonCategory category =
      CommonCategoryUtils.getEipTCommonCategory(Long.valueOf(1));

    // orm.doInsert(dummySchedule);
    int size = memberIdList.size();
    for (int i = 0; i < size; i++) {
      EipTScheduleMap map = Database.create(EipTScheduleMap.class);
      int userid = (memberIdList.get(i)).intValue();
      map.setEipTSchedule(dummySchedule);
      map.setUserId(Integer.valueOf(userid));

      // D: ダミースケジュール
      map.setStatus("D");
      map.setType(ScheduleUtils.SCHEDULEMAP_TYPE_USER);

      map.setCommonCategoryId(Integer.valueOf(1));
      map.setEipTCommonCategory(category);
    }

    if (facilityIdList != null) {
      int fsize = facilityIdList.size();
      for (int i = 0; i < fsize; i++) {
        EipTScheduleMap map = Database.create(EipTScheduleMap.class);
        int fid = (facilityIdList.get(i)).intValue();
        map.setEipTSchedule(dummySchedule);
        map.setUserId(Integer.valueOf(fid));

        // D: ダミースケジュール
        map.setStatus("D");
        map.setType(ScheduleUtils.SCHEDULEMAP_TYPE_FACILITY);

        map.setCommonCategoryId(Integer.valueOf(1));
        map.setEipTCommonCategory(category);
      }
    }

    // スケジュールを登録
    Database.commit();
  }

  /**
   * スケジュールを時刻でソートする．
   *
   * @param list
   * @return
   */
  public static void sortByTime(List<VEipTScheduleList> list) {
    Collections.sort(list, new Comparator<VEipTScheduleList>() {
      private final Calendar cal = Calendar.getInstance();

      @Override
      public int compare(VEipTScheduleList o1, VEipTScheduleList o2) {
        // 開始時刻を取得
        cal.setTime(o1.getStartDate());
        int hour1 = cal.get(Calendar.HOUR_OF_DAY);
        int minute1 = cal.get(Calendar.MINUTE);

        cal.setTime(o2.getStartDate());
        int hour2 = cal.get(Calendar.HOUR_OF_DAY);
        int minute2 = cal.get(Calendar.MINUTE);

        // 開始時刻で比較
        if (hour1 != hour2) {
          return hour1 - hour2;
        } else if (minute1 != minute2) {
          return minute1 - minute2;
        } else {
          // 終了時刻を取得
          cal.setTime(o1.getEndDate());
          hour1 = cal.get(Calendar.HOUR_OF_DAY);
          minute1 = cal.get(Calendar.MINUTE);

          cal.setTime(o2.getEndDate());
          hour2 = cal.get(Calendar.HOUR_OF_DAY);
          minute2 = cal.get(Calendar.MINUTE);

          // 終了時刻で比較
          if (hour1 != hour2) {
            return hour1 - hour2;
          } else if (minute1 != minute2) {
            return minute1 - minute2;
          }
        }

        return 0;
      }
    });
  }

  /**
   * ダミースケジュールが上にくるようにソートする．
   *
   * @param list
   * @return
   */
  public static List<VEipTScheduleList> sortByDummySchedule(
      List<VEipTScheduleList> list) {
    // 重複スケジュールの表示調節のために，
    // ダミースケジュールをリストの始めに寄せる．
    List<VEipTScheduleList> dummyList = new ArrayList<VEipTScheduleList>();
    List<VEipTScheduleList> normalList = new ArrayList<VEipTScheduleList>();
    for (VEipTScheduleList scheduleMap : list) {
      if ("D".equals(scheduleMap.getStatus())) {
        dummyList.add(scheduleMap);
      } else {
        normalList.add(scheduleMap);
      }
    }

    List<VEipTScheduleList> newList = new ArrayList<VEipTScheduleList>();
    newList.addAll(dummyList);
    newList.addAll(normalList);
    return newList;
  }

  public static boolean validateTabName(String tab_name) {
    if (tab_name == null) {
      return false;
    }
    return ("calendar".equals(tab_name)
      || "oneday".equals(tab_name)
      || "weekly".equals(tab_name)
      || "monthly".equals(tab_name)
      || "oneday-group".equals(tab_name)
      || "weekly-group".equals(tab_name)
      || "list".equals(tab_name) || "search".equals(tab_name));
  }

  /**
   * 現在選択しているタブ名を取得する．
   *
   * @param rundata
   * @param context
   * @return
   */
  public static String getCurrentTab(RunData rundata, Context context) {
    // 自ポートレットからのリクエストであれば、パラメータを展開しセッションに保存する。
    if (ALEipUtils.isMatch(rundata, context)) {
      // 現在選択されているタブ
      // oneday : １日表示
      // weekly : 週間表示
      // monthly: 月間表示
      if (rundata.getParameters().containsKey("tab")) {
        String tab = rundata.getParameters().getString("tab");
        ALEipUtils.setTemp(rundata, context, "tab", tab);
      }
    }
    String currentTab;
    String tmpCurrentTab = ALEipUtils.getTemp(rundata, context, "tab");
    if (!ScheduleUtils.validateTabName(tmpCurrentTab)) {
      VelocityPortlet portlet = ALEipUtils.getPortlet(rundata, context);
      currentTab = portlet.getPortletConfig().getInitParameter("pba-template");
    } else {
      currentTab = tmpCurrentTab;
    }
    return currentTab;
  }

  /**
   * Date のオブジェクトを指定した形式の文字列に変換する．
   *
   * @param date
   * @param dateFormat
   * @return
   */
  public static String translateDate(Date date, String dateFormat) {
    if (date == null) {
      return "Unknown";
    }

    // 日付を表示形式に変換
    SimpleDateFormat sdf = new SimpleDateFormat(dateFormat);
    sdf.setTimeZone(TimeZone.getDefault());
    return sdf.format(date);
  }

  /**
   * 指定した曜日が，選択範囲に入っているかを検証する．
   *
   * @param selectedWeek
   *          指定曜日
   * @param startWeek
   *          期間開始曜日
   * @param endWeek
   *          期間終了曜日
   * @return 選択範囲に入っている場合，true．
   */
  public static boolean includeWeek(int selectedWeek, int startWeek, int endWeek) {
    if (startWeek <= endWeek) {
      if (startWeek <= selectedWeek && selectedWeek <= endWeek) {
        return true;
      } else {
        return false;
      }
    } else {
      if (selectedWeek < startWeek && endWeek < selectedWeek) {
        return false;
      } else {
        return true;
      }
    }
  }

  /**
   * 第一引数のリストに，第二引数で指定したユーザ ID が含まれているかを検証する．
   *
   * @param memberIdList
   * @param memberId
   * @return
   */
  public static boolean isContains(List<Integer> memberIdList, int userId) {
    int size = memberIdList.size();
    Integer tmpInt = null;
    for (int i = 0; i < size; i++) {
      tmpInt = memberIdList.get(i);
      if (userId == tmpInt.intValue()) {
        return true;
      }
    }
    return false;
  }

  /**
   * 第一引数のリストに，第二引数で指定したユーザ ID が含まれているかを検証する．
   *
   * @param memberIdList
   * @param memberId
   * @return
   */
  public static boolean isContains(List<ALEipUser> memberList, ALEipUser user) {
    int size = memberList.size();
    long userid = user.getUserId().getValue();
    ALEipUser member = null;
    for (int i = 0; i < size; i++) {
      member = memberList.get(i);
      if (member.getUserId().getValue() == userid) {
        return true;
      }
    }
    return false;
  }

  public static boolean removeUser(List<ALEipUser> memberList, ALEipUser user) {
    if (memberList == null || memberList.size() == 0 || user == null) {
      return false;
    }

    int index = -1;
    int size = memberList.size();
    long userid = user.getUserId().getValue();
    ALEipUser member = null;
    for (int i = 0; i < size; i++) {
      member = memberList.get(i);
      if (member.getUserId().getValue() == userid) {
        index = i;
        break;
      }
    }

    if (index >= 0) {
      memberList.remove(index);
      return true;
    } else {
      return false;
    }
  }

  public static EipTScheduleMap getScheduleMap(
      List<EipTScheduleMap> scheduleMaps, int userid) {
    EipTScheduleMap map = null;
    int size = scheduleMaps.size();
    for (int i = 0; i < size; i++) {
      map = scheduleMaps.get(i);
      if (map.getUserId().intValue() == userid) {
        return map;
      }
    }
    return null;
  }

  public static boolean setFormDataDelegate(RunData rundata, Context context,
      ALData formdata, Field[] fields, List<String> msgList)
      throws ALPageNotFoundException, ALDBErrorException {
    if (fields == null || fields.length == 0) {
      return false;
    }

    boolean res = false;

    try {
      String FORMAT_DATE = "yyyyMMdd";
      String FORMAT_TIME = "HHmm";
      int FORMAT_DATE_LEN = FORMAT_DATE.length();
      int FORMAT_TIME_LEN = FORMAT_TIME.length();

      int length = fields.length;
      for (int i = 0; i < length; i++) {
        fields[i].setAccessible(true);
        String name = fields[i].getName();
        Object obj = fields[i].get(formdata);
        // フィールドが ALCellDateTimeField の場合
        if (obj instanceof ALCellDateTimeField) {
          String dateString =
            new StringBuffer().append(name).append("_date").toString();
          String timeString =
            new StringBuffer().append(name).append("_time").toString();

          ALCellDateTimeField field = (ALCellDateTimeField) obj;
          String dateStr = null;
          String timeStr = null;
          Calendar cal = Calendar.getInstance();
          if (rundata.getParameters().containsKey(dateString)) {
            dateStr = rundata.getParameters().getString(dateString);
          } else {
            continue;
          }
          if (rundata.getParameters().containsKey(timeString)) {
            timeStr = rundata.getParameters().getString(timeString);
          } else {
            continue;
          }

          if (dateStr.length() != FORMAT_DATE_LEN) {
            // 文字列の長さが正しくない場合
            msgList
              .add(ALLocalizationUtils.getl10n("SCHEDULE_MESSAGE_NON_DAY"));
            continue;
          }

          if (timeStr.length() != FORMAT_TIME_LEN) {
            // 文字列の長さが正しくない場合
            msgList.add(ALLocalizationUtils
              .getl10n("SCHEDULE_MESSAGE_NON_TIME"));
            continue;
          }

          List<String> tmpList = new ArrayList<String>();
          ALCellStringField sf = new ALCellStringField(dateStr);
          sf.setTrim(true);
          sf.setCharacterType(ALStringField.TYPE_NUMBER);
          sf.setValue(dateStr);
          sf.validate(tmpList);
          if (tmpList.size() != 0) {
            msgList
              .add(ALLocalizationUtils.getl10n("SCHEDULE_MESSAGE_NON_DAY"));
            continue;
          }

          sf = new ALCellStringField(timeStr);
          sf.setTrim(true);
          sf.setCharacterType(ALStringField.TYPE_NUMBER);
          sf.setValue(timeStr);
          sf.validate(tmpList);
          if (tmpList.size() != 0) {
            msgList.add(ALLocalizationUtils
              .getl10n("SCHEDULE_MESSAGE_NON_TIME"));
            continue;
          }

          Date date = null;
          // 日付を表示形式に変換
          SimpleDateFormat sdf = new SimpleDateFormat(FORMAT_DATE);
          sdf.setLenient(false);
          sdf.setTimeZone(TimeZone.getDefault());
          if (!dateStr.equals("")) {
            try {
              date = sdf.parse(dateStr);
            } catch (Exception e) {
              msgList.add(ALLocalizationUtils
                .getl10n("SCHEDULE_MESSAGE_NON_DAY"));
              continue;
            }
          } else {
            continue;
          }
          Date time = null;
          SimpleDateFormat sdf2 = new SimpleDateFormat(FORMAT_TIME);
          sdf2.setLenient(false);
          sdf2.setTimeZone(TimeZone.getDefault());
          if (!timeStr.equals("")) {
            try {
              time = sdf2.parse(timeStr);
            } catch (Exception e) {
              msgList.add(ALLocalizationUtils
                .getl10n("SCHEDULE_MESSAGE_NON_TIME"));
              continue;
            }
          } else {
            continue;
          }

          Calendar cal2 = Calendar.getInstance();
          cal2.setTime(time);

          cal.setLenient(false);
          cal.setTime(date);
          cal.set(Calendar.HOUR_OF_DAY, cal2.get(Calendar.HOUR_OF_DAY));
          cal.set(Calendar.MINUTE, cal2.get(Calendar.MINUTE));
          cal.set(Calendar.SECOND, 0);
          cal.set(Calendar.MILLISECOND, 0);

          field.setValue(cal.getTime());

          // フィールドが ALCellDateField の場合
        } else if (obj instanceof ALCellDateField) {
          ALCellDateField field = (ALCellDateField) obj;
          Date date = null;
          ALDateContainer con = new ALDateContainer();
          String dateString = new StringBuffer().append(name).toString();
          String dateStr = null;
          if (rundata.getParameters().containsKey(name)) {
            dateStr = rundata.getParameters().getString(dateString);
          } else {
            continue;
          }

          if (dateStr.length() != FORMAT_DATE_LEN) {
            // 文字列の長さが正しくない場合
            msgList
              .add(ALLocalizationUtils.getl10n("SCHEDULE_MESSAGE_NON_DAY"));
            continue;
          }

          List<String> tmpList = new ArrayList<String>();
          ALCellStringField sf = new ALCellStringField(dateStr);
          sf.setTrim(true);
          sf.setCharacterType(ALStringField.TYPE_NUMBER);
          sf.setValue(dateStr);
          sf.validate(tmpList);
          if (tmpList.size() != 0) {
            msgList
              .add(ALLocalizationUtils.getl10n("SCHEDULE_MESSAGE_NON_DAY"));
            continue;
          }

          // 日付を表示形式に変換
          SimpleDateFormat sdf = new SimpleDateFormat(FORMAT_DATE);
          sdf.setLenient(false);
          sdf.setTimeZone(TimeZone.getDefault());
          try {
            date = sdf.parse(dateStr);
          } catch (Exception e) {
            msgList
              .add(ALLocalizationUtils.getl10n("SCHEDULE_MESSAGE_NON_DAY"));
            continue;
          }

          Calendar cal = Calendar.getInstance();
          cal.setLenient(false);
          cal.setTime(date);
          cal.set(Calendar.HOUR_OF_DAY, 0);
          cal.set(Calendar.MINUTE, 0);
          cal.set(Calendar.SECOND, 0);
          cal.set(Calendar.MILLISECOND, 0);

          con.setYear(cal.get(Calendar.YEAR));
          con.setMonth(cal.get(Calendar.MONTH) + 1);
          con.setDay(cal.get(Calendar.DATE));
          field.setValue(con);

          // フィールドが ALAbstractField の場合
        } else if (obj instanceof ALAbstractField) {
          ALAbstractField field = (ALAbstractField) obj;
          if (rundata.getParameters().containsKey(name)) {
            field.setValue(rundata.getParameters().getString(name));
          }
        }
      }

      res = true;
    } catch (RuntimeException e) {
      logger.error("schedule", e);
      return false;
    } catch (Exception ex) {
      logger.error("schedule", ex);
      return false;
    }
    return res;
  }

  /**
   * パラメータを読み込みます。
   *
   * @param rundata
   * @param context
   */
  public static void loadParametersDelegate(RunData rundata, Context context,
      String tmpStart, String tmpEnd, String tmpView) {
    ALDateTimeField dummy = new ALDateTimeField("yyyy-MM-dd-HH-mm");
    dummy.setNotNull(true);
    if (ALEipUtils.isMatch(rundata, context)) {
      Calendar cal = Calendar.getInstance();
      int min = cal.get(Calendar.MINUTE);
      if (min <= 15) {
        cal.set(Calendar.MINUTE, 15);
      } else if (min <= 30) {
        cal.set(Calendar.MINUTE, 30);
      } else if (min <= 45) {
        cal.set(Calendar.MINUTE, 45);
      } else {
        cal.set(Calendar.MINUTE, 60);
      }
      Date now = cal.getTime();

      if (rundata.getParameters().containsKey("form_start")) {
        tmpStart = rundata.getParameters().getString("form_start");
        ALEipUtils.setTemp(rundata, context, "tmpStart", tmpStart);
        dummy.setValue(tmpStart);
        if (!dummy.validate(new ArrayList<String>())) {
          ALEipUtils.removeTemp(rundata, context, "form_start");
          ALEipUtils.removeTemp(rundata, context, "form_end");
          logger.debug("[ScheduleFormData] Parameter cannot validate");
          ALEipUtils.redirectPageNotFound(rundata);
          return;
        }
      } else {
        dummy.setValue(now);
        ALEipUtils.setTemp(rundata, context, "tmpStart", dummy.toString());
      }
      if (rundata.getParameters().containsKey("form_end")) {
        tmpEnd = rundata.getParameters().getString("form_end");
        ALEipUtils.setTemp(rundata, context, "tmpEnd", tmpEnd);
        dummy.setValue(tmpEnd);
        if (!dummy.validate(new ArrayList<String>())) {
          ALEipUtils.removeTemp(rundata, context, "form_start");
          ALEipUtils.removeTemp(rundata, context, "form_end");
          logger.debug("[ScheduleFormData] Parameter cannot validate");
          ALEipUtils.redirectPageNotFound(rundata);
          return;
        }
      } else {
        dummy.setValue(now);
        ALEipUtils.setTemp(rundata, context, "tmpEnd", dummy.toString());
      }
    }
    tmpStart = ALEipUtils.getTemp(rundata, context, "tmpStart");
    // tmpEnd = ALEipUtils.getTemp(rundata, context, "tmpEnd");
    loadParametersViewDate(rundata, context, tmpStart, tmpView);
  }

  public static void loadParametersViewDate(RunData rundata, Context context,
      String tmpStart, String tmpView) {
    if (ALEipUtils.isMatch(rundata, context)) {
      if (rundata.getParameters().containsKey("view_date")) {
        ALDateTimeField dummy = new ALDateTimeField("yyyy-MM-dd");
        tmpView = rundata.getParameters().getString("view_date");
        ALEipUtils.setTemp(rundata, context, "tmpView", tmpView);
        dummy.setValue(tmpView);
        if (!dummy.validate(new ArrayList<String>())) {
          ALEipUtils.removeTemp(rundata, context, "tmpView");
          logger.debug("[ScheduleFormData] Parameter cannot validate");
          ALEipUtils.redirectPageNotFound(rundata);
          return;
        }
      } else {
        if (tmpView == null || tmpView.equals("")) {
          if (tmpStart != null && !tmpStart.equals("")) {
            tmpView = tmpStart;
            ALEipUtils.setTemp(rundata, context, "tmpView", tmpView);
          }
        }
      }
    }
  }

  /**
   * スケジュールの時刻系要素バリデーション処理移譲メソッド
   *
   * @param start_date
   * @param end_date
   * @param repeat_type
   * @param is_repeat
   * @param is_span
   * @param week_0
   * @param week_1
   * @param week_2
   * @param week_3
   * @param week_4
   * @param week_5
   * @param week_6
   * @param repeat_week
   * @param limit_flag
   * @param limit_start_date
   * @param limit_end_date
   * @param month_day
   * @param login_user
   * @param entityid
   * @param msgList
   *          エラーメッセージリスト
   * @param isCellPhone
   *          携帯電話かどうか
   * @return
   * @throws ALDBErrorException
   * @throws ALPageNotFoundException
   */
  public static boolean validateDelegate(ALDateTimeField start_date,
      ALDateTimeField end_date, ALStringField repeat_type, boolean is_repeat,
      boolean is_span, ALStringField week_0, ALStringField week_1,
      ALStringField week_2, ALStringField week_3, ALStringField week_4,
      ALStringField week_5, ALStringField week_6, ALStringField repeat_week,
      ALStringField limit_flag, ALDateField limit_start_date,
      ALDateField limit_end_date, ALNumberField month_day,
      ALNumberField year_month, ALNumberField year_day, ALEipUser login_user,
      String entityid, List<String> msgList, boolean isCellPhone)
      throws ALDBErrorException, ALPageNotFoundException {

    int YEAR_FIRST = 2004;
    boolean dayexist = true;

    switch ((int) year_month.getValue()) {
      case 2:
        if (year_day.getValue() == 30 || year_day.getValue() == 31) {
          dayexist = false;
        }
        break;

      case 4:
      case 6:
      case 9:
      case 11:
        if (year_day.getValue() == 31) {
          dayexist = false;
        }
        break;

      default:
        break;
    }

    if (end_date == null) {
      msgList.add(ALLocalizationUtils
        .getl10n("SCHEDULE_MESSAGE_TYPE_RIGHT_END_DATE"));
      end_date = start_date;
    }

    Calendar startDate = Calendar.getInstance();
    startDate.setTime(start_date.getValue());
    Calendar endDate = Calendar.getInstance();
    endDate.setTime(end_date.getValue());

    if (is_repeat) {
      // 開始日時 と 終了日時 の日付を、開始日時 の日付に一致させる
      Calendar tmp_end_date = Calendar.getInstance();
      tmp_end_date.set(Calendar.YEAR, Integer.valueOf(start_date.getYear()));
      tmp_end_date.set(
        Calendar.MONTH,
        Integer.valueOf(start_date.getMonth()) - 1);
      tmp_end_date.set(Calendar.DATE, Integer.valueOf(start_date.getDay()));
      tmp_end_date.set(Calendar.HOUR_OF_DAY, Integer
        .valueOf(end_date.getHour()));
      tmp_end_date.set(Calendar.MINUTE, Integer.valueOf(end_date.getMinute()));
      tmp_end_date.set(Calendar.SECOND, 0);
      end_date.setValue(tmp_end_date.getTime());
    }

    if (is_span) {
      // 開始日時 と 終了日時 の時間を 0時0分0秒 に設定する
      Calendar tmp_start_date = Calendar.getInstance();
      tmp_start_date.setTime(start_date.getValue());
      tmp_start_date.set(Calendar.HOUR_OF_DAY, 0);
      tmp_start_date.set(Calendar.MINUTE, 0);
      tmp_start_date.set(Calendar.SECOND, 0);
      start_date.setValue(tmp_start_date.getTime());

      Calendar tmp_end_date = Calendar.getInstance();
      tmp_end_date.setTime(end_date.getValue());
      tmp_end_date.set(Calendar.HOUR_OF_DAY, 0);
      tmp_end_date.set(Calendar.MINUTE, 0);
      tmp_end_date.set(Calendar.SECOND, 0);
      end_date.setValue(tmp_end_date.getTime());
    }

    // 開始日時
    start_date.validate(msgList);
    int startyear = startDate.get(Calendar.YEAR);
    if ((startyear < YEAR_FIRST) && isCellPhone) {
      // 携帯画面用条件
      msgList.add(ALLocalizationUtils.getl10nFormat(
        "SCHEDULE_MESSAGE_SELECT_RIGHT_START_DATE",
        YEAR_FIRST));
    }
    if (startDate.get(Calendar.MINUTE) % 15.0 != 0 && isCellPhone) {
      // 携帯画面用条件
      msgList.add(ALLocalizationUtils
        .getl10n("SCHEDULE_MESSAGE_SELECT_START_TIME_PER15"));
    }

    // 終了日時
    end_date.validate(msgList);
    int endyear = endDate.get(Calendar.YEAR);
    if ((endyear < YEAR_FIRST) && isCellPhone) {
      // 携帯画面用条件
      msgList.add(ALLocalizationUtils.getl10nFormat(
        "SCHEDULE_MESSAGE_SELECT_RIGHT_END_DATE",
        YEAR_FIRST));
    }
    if (endDate.get(Calendar.MINUTE) % 15.0 != 0 && isCellPhone) {
      // 携帯画面用条件
      msgList.add(ALLocalizationUtils
        .getl10n("SCHEDULE_MESSAGE_SELECT_END_TIME_PER15"));
    }

    // 開始日時＆終了日時
    if (end_date.getValue().before(start_date.getValue())) {
      msgList.add(ALLocalizationUtils
        .getl10n("SCHEDULE_MESSAGE_SELECT_END_DATE_TO_START_DATE"));
    }

    if (is_repeat) {
      try {
        if ("W".equals(repeat_type.getValue())) {
          // 毎週,第何週の繰り返し
          if (week_0.getValue() == null
            && week_1.getValue() == null
            && week_2.getValue() == null
            && week_3.getValue() == null
            && week_4.getValue() == null
            && week_5.getValue() == null
            && week_6.getValue() == null) {
            switch (repeat_week.getValue()) {
              case "0":
                msgList.add(ALLocalizationUtils
                  .getl10n("SCHEDULE_MESSAGE_SELECT_EVERY_WEEKLY"));
                break;
              case "1":
                msgList.add(ALLocalizationUtils
                  .getl10n("SCHEDULE_MESSAGE_SELECT_1ST_WEEKLY"));
                break;
              case "2":
                msgList.add(ALLocalizationUtils
                  .getl10n("SCHEDULE_MESSAGE_SELECT_2ND_WEEKLY"));
                break;
              case "3":
                msgList.add(ALLocalizationUtils
                  .getl10n("SCHEDULE_MESSAGE_SELECT_3RD_WEEKLY"));
                break;
              case "4":
                msgList.add(ALLocalizationUtils
                  .getl10n("SCHEDULE_MESSAGE_SELECT_4TH_WEEKLY"));
                break;
              case "5":
                msgList.add(ALLocalizationUtils
                  .getl10n("SCHEDULE_MESSAGE_SELECT_5TH_WEEKLY"));
                break;
              default:
                break;
            }
          }
        } else if ("M".equals(repeat_type.getValue())) {
          // 毎月の繰り返し
          if (month_day.getValue() == 0 && isCellPhone) {
            // 携帯画面用条件
            msgList.add(ALLocalizationUtils
              .getl10n("SCHEDULE_MESSAGE_SELECT_EVERY_MONTHLY"));
          } else {
            month_day.validate(msgList);
          }
        } else if ("Y".equals(repeat_type.getValue())) {
          // 毎年の繰り返し
          if (year_month.getValue() == 0 && isCellPhone) {
            // 携帯画面用条件（月が未入力）
            msgList.add(ALLocalizationUtils
              .getl10n("SCHEDULE_MESSAGE_SELECT_EVERY_YEARLY_MONTH"));
          } else if (year_day.getValue() == 0 && isCellPhone) {
            // 携帯画面用条件（日が未入力）
            msgList.add(ALLocalizationUtils
              .getl10n("SCHEDULE_MESSAGE_SELECT_EVERY_YEARLY_DAY"));
          } else if (!dayexist) {
            msgList.add(ALLocalizationUtils
              .getl10n("SCHEDULE_MESSAGE_SELECT_EVERY_YEARLY_DAY_EXIST"));
          } else {
            year_month.validate(msgList);
            year_day.validate(msgList);
          }
        }

        if ("ON".equals(limit_flag.getValue())) {
          if (!ScheduleUtils.equalsToDate(
            limit_start_date.getValue().getDate(),
            limit_end_date.getValue().getDate(),
            false)
            && limit_start_date.getValue().getDate().after(
              limit_end_date.getValue().getDate())) {
            msgList.add(ALLocalizationUtils
              .getl10n("SCHEDULE_MESSAGE_SELECT_REPEAT_END_DAY_TO_START_DAY"));
          }

          if (isCellPhone) {
            // 携帯画面用条件
            Calendar limitStartDate = Calendar.getInstance();
            limitStartDate.setTime(limit_start_date.getValue().getDate());
            int limitstartyear = limitStartDate.get(Calendar.YEAR);
            if ((limitstartyear < YEAR_FIRST)) {
              msgList.add(ALLocalizationUtils.getl10nFormat(
                "SCHEDULE_MESSAGE_SELECT_START_DATE_IN_THIS_TERM",
                YEAR_FIRST));
            }
            Calendar limitEndDate = Calendar.getInstance();
            limitEndDate.setTime(limit_end_date.getValue().getDate());
            int limitendyear = limitEndDate.get(Calendar.YEAR);
            if ((limitendyear < YEAR_FIRST)) {
              msgList.add(ALLocalizationUtils.getl10nFormat(
                "SCHEDULE_MESSAGE_SELECT_END_DATE_IN_THIS_TERM",
                YEAR_FIRST));
            }
          }

          // 繰り返し期間の正当性を調べる
          // リピートパターン文字列作成
          char lim = 'N';
          Calendar cal = Calendar.getInstance();
          cal.setTime(end_date.getValue());
          if ("ON".equals(limit_flag.getValue())) {
            lim = 'L';
          }
          String repeat_pattern;
          int date_count = 0;
          if ("D".equals(repeat_type.getValue())) {
            repeat_pattern =
              new StringBuffer().append('D').append(lim).toString();
          } else if ("W".equals(repeat_type.getValue())) {
            if ("0".equals(repeat_week.getValue())) {
              repeat_pattern =
                new StringBuffer().append('W').append(
                  week_0.getValue() != null ? 1 : 0).append(
                  week_1.getValue() != null ? 1 : 0).append(
                  week_2.getValue() != null ? 1 : 0).append(
                  week_3.getValue() != null ? 1 : 0).append(
                  week_4.getValue() != null ? 1 : 0).append(
                  week_5.getValue() != null ? 1 : 0).append(
                  week_6.getValue() != null ? 1 : 0).append(lim).toString();
              date_count =
                (week_0.getValue() != null ? 1 : 0)
                  + (week_1.getValue() != null ? 1 : 0)
                  + (week_2.getValue() != null ? 1 : 0)
                  + (week_3.getValue() != null ? 1 : 0)
                  + (week_4.getValue() != null ? 1 : 0)
                  + (week_5.getValue() != null ? 1 : 0)
                  + (week_6.getValue() != null ? 1 : 0);
            } else {
              repeat_pattern =
                new StringBuffer().append('W').append(
                  week_0.getValue() != null ? 1 : 0).append(
                  week_1.getValue() != null ? 1 : 0).append(
                  week_2.getValue() != null ? 1 : 0).append(
                  week_3.getValue() != null ? 1 : 0).append(
                  week_4.getValue() != null ? 1 : 0).append(
                  week_5.getValue() != null ? 1 : 0).append(
                  week_6.getValue() != null ? 1 : 0).append(
                  repeat_week.getValue().charAt(0)).append(lim).toString();
              date_count =
                (week_0.getValue() != null ? 1 : 0)
                  + (week_1.getValue() != null ? 1 : 0)
                  + (week_2.getValue() != null ? 1 : 0)
                  + (week_3.getValue() != null ? 1 : 0)
                  + (week_4.getValue() != null ? 1 : 0)
                  + (week_5.getValue() != null ? 1 : 0)
                  + (week_6.getValue() != null ? 1 : 0);
            }
          } else if ("M".equals(repeat_type.getValue())) {
            DecimalFormat format = new DecimalFormat("00");
            if (32 == month_day.getValue()) {
              repeat_pattern =
                new StringBuffer()
                  .append('M')
                  .append("XX")
                  .append(lim)
                  .toString();
            } else {
              repeat_pattern =
                new StringBuffer().append('M').append(
                  format.format(month_day.getValue())).append(lim).toString();
            }
            date_count = 1;
          } else {
            DecimalFormat format = new DecimalFormat("00");
            repeat_pattern =
              new StringBuffer().append('Y').append(
                format.format(year_month.getValue())).append(
                format.format(year_day.getValue())).append(lim).toString();
            date_count = 1;
          }
          // 開始時刻(期間初日)
          Calendar sDate = new GregorianCalendar();
          sDate.set(Calendar.YEAR, Integer.valueOf(limit_start_date.getYear()));
          sDate.set(Calendar.MONTH, Integer
            .valueOf(limit_start_date.getMonth()) - 1);
          sDate.set(Calendar.DATE, Integer.valueOf(limit_start_date.getDay()));
          sDate.set(Calendar.HOUR_OF_DAY, 0);
          sDate.set(Calendar.MINUTE, 0);
          sDate.set(Calendar.SECOND, 0);
          // 繰り返し最終日の終了時刻
          Calendar finalDate = new GregorianCalendar();
          finalDate.set(Calendar.YEAR, Integer
            .valueOf(limit_end_date.getYear()));
          finalDate.set(Calendar.MONTH, Integer.valueOf(limit_end_date
            .getMonth()) - 1);
          finalDate
            .set(Calendar.DATE, Integer.valueOf(limit_end_date.getDay()));
          finalDate.set(Calendar.HOUR_OF_DAY, 23);
          finalDate.set(Calendar.MINUTE, 59);
          finalDate.set(Calendar.SECOND, 59);
          int countAvailableDate = 0;
          while (sDate.before(finalDate) || sDate.equals(finalDate)) {
            if (ScheduleUtils.matchDay(sDate, repeat_pattern)) {
              countAvailableDate++;
              if (countAvailableDate >= date_count) {
                break;
              }
            }
            sDate.add(Calendar.DATE, 1);
          }
          if (countAvailableDate < date_count) {
            msgList.add(ALLocalizationUtils
              .getl10n("SCHEDULE_MESSAGE_SELECT_REPEAT_SPAN_IN_THIS_TERM"));
          }
        }

      } catch (NumberFormatException nfe) {
        logger
          .error("[ScheduleFormData] NumberFormatException: Limit Date is wrong.");
        throw new ALPageNotFoundException();
      } catch (ALIllegalDateException ad) {
        logger
          .error("[ScheduleFormData] ALIllegalDateException: Limit Date is wrong.");
        throw new ALPageNotFoundException();
      }
    }

    return (msgList.size() == 0);
  }

  /**
   * 同一期間内に複数の ToDo を追加する. 第一引数の List を排他制御しないで処理するので注意.
   *
   * @param weekSpanConList
   *          複数の期間スケジュールを保持するリスト
   * @param viewStartDate
   *          表示開始の年月日
   * @param index
   *          期間スケジュールの追加位置
   * @param rd
   *          期間スケジュール
   */
  public static void addToDo(List<ScheduleToDoWeekContainer> weekConList,
      Date viewStartDate, int index, ScheduleToDoResultData rd) {
    try {
      boolean success = false;
      if (weekConList.size() > 0) {
        ScheduleToDoWeekContainer tmpWeekCon = null;
        int size = weekConList.size();
        for (int i = 0; i < size; i++) {
          tmpWeekCon = weekConList.get(i);
          success = tmpWeekCon.canAddTodo(rd);
          if (success) {
            tmpWeekCon.addToDoResultData(index, rd);
            break;
          }
        }
      }

      if (!success) {
        Calendar cal2 = Calendar.getInstance();
        cal2.setTime(viewStartDate);
        // 週間スケジュールコンテナの初期化
        ScheduleToDoWeekContainer weekCon = new ScheduleToDoWeekContainer();
        weekCon.initField();
        weekCon.setViewStartDate(cal2);
        weekCon.addToDoResultData(index, rd);
        weekConList.add(weekCon);
      }

    } catch (Exception e) {
      logger.error("schedule", e);
    }
  }

  public static boolean isZeroLength(String[] strs) {
    if (strs == null || strs.length <= 0) {
      return true;
    }

    int len = strs.length;
    for (int i = 0; i < len; i++) {
      if (strs[i] == null || "".equals(strs[i])) {
        return true;
      }
    }
    return false;
  }

  public static boolean isSpanDuplicate(int user_id, Date start_date,
      Date end_date, Integer update_id) {
    SelectQuery<EipTScheduleMap> query = Database.query(EipTScheduleMap.class);

    Expression exp1 =
      ExpressionFactory.matchExp(EipTScheduleMap.USER_ID_PROPERTY, Integer
        .valueOf(user_id));
    query.setQualifier(exp1);
    Expression exp2 =
      ExpressionFactory.greaterOrEqualExp(
        EipTScheduleMap.EIP_TSCHEDULE_PROPERTY
          + "."
          + EipTSchedule.END_DATE_PROPERTY,
        start_date);
    query.andQualifier(exp2);
    Expression exp3 =
      ExpressionFactory.lessOrEqualExp(EipTScheduleMap.EIP_TSCHEDULE_PROPERTY
        + "."
        + EipTSchedule.START_DATE_PROPERTY, end_date);
    query.andQualifier(exp3);
    Expression exp4 =
      ExpressionFactory.matchExp(EipTScheduleMap.EIP_TSCHEDULE_PROPERTY
        + "."
        + EipTSchedule.REPEAT_PATTERN_PROPERTY, "S");
    query.andQualifier(exp4);

    if (update_id != null) {
      Expression mapexp =
        ExpressionFactory.noMatchExp(
          EipTScheduleMap.SCHEDULE_ID_PROPERTY,
          Integer.valueOf(update_id));
      query.andQualifier(mapexp);
    }

    List<EipTScheduleMap> list = query.fetchList();
    int count = (list != null && list.size() > 0) ? list.size() : 0;
    return count > 0;
  }

  public static List<ALEipUser> getALEipUsers(String[] ids)
      throws ALDBErrorException {

    int ids_len = ids.length;
    Integer tmp_id = null;
    List<Integer> tmp_ids = new ArrayList<Integer>();
    for (int i = 0; i < ids_len; i++) {
      try {
        tmp_id = Integer.valueOf(ids[i]);
        if (tmp_id.intValue() > 3) {
          tmp_ids.add(tmp_id);
        }
      } catch (Exception e) {
      }
    }

    Expression exp =
      ExpressionFactory.inDbExp(TurbineUser.USER_ID_PK_COLUMN, tmp_ids);
    List<TurbineUser> users =
      Database.query(TurbineUser.class, exp).fetchList();
    if (users.size() == 0) {
      return null;
    }

    List<ALEipUser> uList = new ArrayList<ALEipUser>();
    TurbineUser tuser = null;
    ALEipUser user = null;
    int len = users.size();
    for (int i = 0; i < len; i++) {
      tuser = users.get(i);
      user = new ALEipUser();
      user.initField();
      user.setUserId(tuser.getUserId().intValue());
      user.setName(tuser.getLoginName());
      user.setAliasName(tuser.getFirstName(), tuser.getLastName());
      uList.add(user);
    }
    return uList;
  }

  public static List<UserFacilityLiteBean> getALEipUserFacility(String[] ids,
      RunData rundata) throws ALDBErrorException {

    List<UserFacilityLiteBean> ulist = new ArrayList<UserFacilityLiteBean>();

    int ids_len = ids.length;
    Integer tmp_id = null;
    List<Integer> tmp_ids = new ArrayList<Integer>();
    List<String> f_ids = new ArrayList<String>();
    for (int i = 0; i < ids_len; i++) {
      if (ids[i].startsWith("f")) {
        // facilityIDをセット
        f_ids.add(ids[i].replace("f", ""));
      } else {
        // ユーザーID
        try {
          tmp_id = Integer.valueOf(ids[i]);
          if (tmp_id.intValue() > 3) {
            tmp_ids.add(tmp_id);
          }
        } catch (Exception e) {
        }
      }
    }

    UserFacilityLiteBean user = null;
    // tmp_ids と f_ids をつかって
    // ユーザーIDのデータを取得
    if (tmp_ids != null && tmp_ids.size() > 0) {
      StringBuffer tmp_ids_str = new StringBuffer();
      int tmp_ids_len = tmp_ids.size() - 1;
      for (int i = 0; i < tmp_ids_len; i++) {
        tmp_ids_str.append(tmp_ids.get(i)).append(",");
      }
      tmp_ids_str.append(tmp_ids.get(tmp_ids_len));

      // SQLの作成
      StringBuffer statement = new StringBuffer();
      statement.append("SELECT DISTINCT ");
      statement
        .append("  B.USER_ID, B.LOGIN_NAME, B.FIRST_NAME, B.LAST_NAME, D.POSITION ");
      statement.append("FROM turbine_user_group_role as A ");
      statement.append("LEFT JOIN turbine_user as B ");
      statement.append("  on A.USER_ID = B.USER_ID ");
      statement.append("LEFT JOIN turbine_group as C ");
      statement.append("  on A.GROUP_ID = C.GROUP_ID ");
      statement.append("LEFT JOIN eip_m_user_position as D ");
      statement.append("  on A.USER_ID = D.USER_ID ");
      statement.append("WHERE B.USER_ID > 3 AND B.DISABLED = 'F'");
      statement
        .append(" AND B.USER_ID IN (")
        .append(tmp_ids_str.toString())
        .append(")");
      statement.append("ORDER BY D.POSITION");
      String query = statement.toString();

      List<TurbineUser> users =
        Database.sql(TurbineUser.class, query).fetchList();

      int recNum = users.size();
      for (int j = 0; j < recNum; j++) {
        TurbineUser tuser = users.get(j);
        user = new UserFacilityLiteBean();
        user.initField();
        user.setUserFacilityId(tuser.getUserId());
        user.setName(tuser.getLoginName());
        user.setAliasName(tuser.getFirstName(), tuser.getLastName());
        user.setUserFacilityType("U");
        if (Integer.toString(ALEipUtils.getUserId(rundata)).equals(
          user.getUserFacilityId())) {
          ulist.add(0, user);
        } else {
          ulist.add(user);
        }
      }
    }

    // facilityIDを元にデータを取得
    if (f_ids != null && f_ids.size() > 0) {
      Expression f_exp =
        ExpressionFactory.inDbExp(EipMFacility.FACILITY_ID_PK_COLUMN, f_ids);
      List<EipMFacility> facilities =
        Database.query(EipMFacility.class, f_exp).fetchList();
      if (facilities.size() == 0) {
        return null;
      }
      int f_size = facilities.size();
      for (int i = 0; i < f_size; i++) {
        EipMFacility f_user = facilities.get(i);
        user = new UserFacilityLiteBean();
        user.initField();
        user.setUserFacilityId(f_user.getFacilityId().intValue());
        user.setName("f" + user.getUserFacilityId());
        user.setAliasName(f_user.getFacilityName());
        user.setUserFacilityType("F");
        ulist.add(user);
      }
    }

    return ulist;
  }

  public static List<UserFacilityLiteBean> getALEipFacility(String[] ids,
      RunData rundata) throws ALDBErrorException {

    List<UserFacilityLiteBean> ulist = new ArrayList<UserFacilityLiteBean>();

    int ids_len = ids.length;
    List<String> f_ids = new ArrayList<String>();
    for (int i = 0; i < ids_len; i++) {
      if (ids[i].startsWith("f")) {
        // facilityIDをセット
        f_ids.add(ids[i].replace("f", ""));
      }
    }

    UserFacilityLiteBean user = null;

    // facilityIDを元にデータを取得
    if (f_ids != null && f_ids.size() > 0) {
      Expression f_exp =
        ExpressionFactory.inDbExp(EipMFacility.FACILITY_ID_PK_COLUMN, f_ids);
      List<EipMFacility> facilities =
        Database.query(EipMFacility.class, f_exp).fetchList();
      if (facilities.size() == 0) {
        return null;
      }
      int f_size = facilities.size();
      for (int i = 0; i < f_size; i++) {
        EipMFacility f_user = facilities.get(i);
        user = new UserFacilityLiteBean();
        user.initField();
        user.setUserFacilityId(f_user.getFacilityId().intValue());
        user.setName("f" + user.getUserFacilityId());
        user.setAliasName(f_user.getFacilityName());
        user.setUserFacilityType("F");
        ulist.add(user);
      }
    }

    return ulist;
  }

  /*
   * 同一期間内に複数の 期間スケジュール を追加する. 第一引数の List を排他制御しないで処理するので注意.
   * 
   * @param weekSpanConList 複数の期間スケジュールを保持するリスト @param viewStartDate 表示開始の年月日
   * 
   * @param index 期間スケジュールの追加位置 @param rd 期間スケジュール
   */
  public static void addTerm(List<AjaxTermScheduleWeekContainer> weekConList,
      Date viewStartDate, int index, AjaxScheduleResultData rd) {
    try {
      boolean success = false;
      if (weekConList.size() > 0) {
        AjaxTermScheduleWeekContainer tmpWeekCon = null;
        int size = weekConList.size();
        for (int i = 0; i < size; i++) {
          tmpWeekCon = weekConList.get(i);
          success = tmpWeekCon.canAddTerm(rd);
          if (success) {
            tmpWeekCon.addTermResultData(index, rd);
            break;
          }
        }
      }

      if (!success) {
        Calendar cal2 = Calendar.getInstance();
        cal2.setTime(viewStartDate);
        // 週間スケジュールコンテナの初期化
        AjaxTermScheduleWeekContainer weekCon =
          new AjaxTermScheduleWeekContainer();
        weekCon.initField();
        weekCon.setViewStartDate(cal2);
        weekCon.addTermResultData(index, rd);
        weekConList.add(weekCon);
      }

    } catch (Exception e) {
      logger.error("schedule", e);
    }
  }

  /*
   * 同一期間内に複数の 期間スケジュール を追加する. 第一引数の List を排他制御しないで処理するので注意.
   * 
   * @param weekConList 複数の期間スケジュールを保持するリスト @param viewStartDate 表示開始の年月日 @param
   * index 期間スケジュールの追加位置 @param rd 期間スケジュール
   */
  public static void addTermSchedule(
      List<ScheduleTermWeekContainer> weekConList, Date viewStartDate,
      int index, ScheduleResultData rd) {
    try {
      boolean success = false;
      if (weekConList.size() > 0) {
        ScheduleTermWeekContainer tmpWeekCon = null;
        int size = weekConList.size();
        for (int i = 0; i < size; i++) {
          tmpWeekCon = weekConList.get(i);
          success = tmpWeekCon.canAddTerm(rd);
          if (success) {
            tmpWeekCon.addTermResultData(index, rd);
            break;
          }
        }
      }

      if (!success) {
        Calendar cal2 = Calendar.getInstance();
        cal2.setTime(viewStartDate);
        // 週間スケジュールコンテナの初期化
        ScheduleTermWeekContainer weekCon = new ScheduleTermWeekContainer();
        weekCon.initField();
        weekCon.setViewStartDate(cal2);
        weekCon.addTermResultData(index, rd);
        weekConList.add(weekCon);
      }

    } catch (Exception e) {
      logger.error("schedule", e);
    }
  }

  /**
   *
   * @return
   */
  public static Date getEmptyDate() {
    Calendar cal = Calendar.getInstance();
    cal.set(9999, 11, 31);
    return cal.getTime();
  }

  public static String hasAuthOther(RunData rundata) {
    try {
      ALAccessControlFactoryService aclservice =
        (ALAccessControlFactoryService) ((TurbineServices) TurbineServices
          .getInstance())
          .getService(ALAccessControlFactoryService.SERVICE_NAME);
      ALAccessControlHandler aclhandler = aclservice.getAccessControlHandler();
      if (aclhandler.hasAuthority(
        ALEipUtils.getUserId(rundata),
        ALAccessControlConstants.POERTLET_FEATURE_SCHEDULE_OTHER,
        ALAccessControlConstants.VALUE_ACL_LIST)) {
        return "T";
      }
    } catch (Exception e) {
      logger.error("[ScheduleUtils]", e);
    }
    return "F";
  }

  public static String hasAuthSelf(RunData rundata) {
    try {
      ALAccessControlFactoryService aclservice =
        (ALAccessControlFactoryService) ((TurbineServices) TurbineServices
          .getInstance())
          .getService(ALAccessControlFactoryService.SERVICE_NAME);
      ALAccessControlHandler aclhandler = aclservice.getAccessControlHandler();
      if (aclhandler.hasAuthority(
        ALEipUtils.getUserId(rundata),
        ALAccessControlConstants.POERTLET_FEATURE_SCHEDULE_SELF,
        ALAccessControlConstants.VALUE_ACL_LIST)) {
        return "T";
      }
    } catch (Exception e) {
      logger.error("[ScheduleUtils]", e);
    }
    return "F";
  }

  public static String hasAuthOtherUpdate(RunData rundata) {
    try {
      ALAccessControlFactoryService aclservice =
        (ALAccessControlFactoryService) ((TurbineServices) TurbineServices
          .getInstance())
          .getService(ALAccessControlFactoryService.SERVICE_NAME);
      ALAccessControlHandler aclhandler = aclservice.getAccessControlHandler();
      if (aclhandler.hasAuthority(
        ALEipUtils.getUserId(rundata),
        ALAccessControlConstants.POERTLET_FEATURE_SCHEDULE_OTHER,
        ALAccessControlConstants.VALUE_ACL_UPDATE)) {
        return "T";
      }
    } catch (Exception e) {
      logger.error("[ScheduleUtils]", e);
    }
    return "F";
  }

  public static String hasAuthSelfUpdate(RunData rundata) {
    try {
      ALAccessControlFactoryService aclservice =
        (ALAccessControlFactoryService) ((TurbineServices) TurbineServices
          .getInstance())
          .getService(ALAccessControlFactoryService.SERVICE_NAME);
      ALAccessControlHandler aclhandler = aclservice.getAccessControlHandler();
      if (aclhandler.hasAuthority(
        ALEipUtils.getUserId(rundata),
        ALAccessControlConstants.POERTLET_FEATURE_SCHEDULE_SELF,
        ALAccessControlConstants.VALUE_ACL_UPDATE)) {
        return "T";
      }
    } catch (Exception e) {
      logger.error("[ScheduleUtils]", e);
    }
    return "F";
  }

  public static int getOwnerId(RunData rundata) {
    String scheduleId =
      rundata.getParameters().getString(ALEipConstants.ENTITY_ID);
    if (scheduleId == null | "".equals(scheduleId)) {
      return ALEipUtils.getUserId(rundata);
    }
    Expression exp =
      ExpressionFactory.matchDbExp(EipTSchedule.SCHEDULE_ID_PK_COLUMN, Integer
        .valueOf(scheduleId));
    List<EipTSchedule> list =
      Database.query(EipTSchedule.class, exp).fetchList();
    if (list.size() == 0) {
      return ALEipUtils.getUserId(rundata);
    } else {
      return list.get(0).getOwnerId().intValue();
    }
  }

  public static boolean hasRelation(RunData rundata) {
    String scheduleId =
      rundata.getParameters().getString(ALEipConstants.ENTITY_ID);
    if (scheduleId == null | "".equals(scheduleId)) {
      return false;
    }
    return hasRelation(rundata, Integer.valueOf(scheduleId));
  }

  public static boolean hasRelation(RunData rundata, int scheduleId) {
    int userId = ALEipUtils.getUserId(rundata);
    Expression exp11 =
      ExpressionFactory.matchExp(
        EipTScheduleMap.SCHEDULE_ID_PROPERTY,
        scheduleId);
    Expression exp12 =
      ExpressionFactory.matchExp(EipTScheduleMap.USER_ID_PROPERTY, Integer
        .valueOf(userId));
    Expression exp3 =
      ExpressionFactory.matchExp(
        EipTScheduleMap.TYPE_PROPERTY,
        ScheduleUtils.SCHEDULEMAP_TYPE_USER);
    List<EipTScheduleMap> list =
      Database
        .query(EipTScheduleMap.class, exp11)
        .andQualifier(exp12)
        .andQualifier(exp3)
        .fetchList();
    if (list.size() == 0) {
      return false;
    } else {
      return true;
    }
  }

  public static boolean hasMinimumAuthority(RunData rundata) {
    ALAccessControlFactoryService aclservice =
      (ALAccessControlFactoryService) ((TurbineServices) TurbineServices
        .getInstance()).getService(ALAccessControlFactoryService.SERVICE_NAME);
    ALAccessControlHandler aclhandler = aclservice.getAccessControlHandler();

    boolean hasAuthority =
      aclhandler.hasAuthority(
        ALEipUtils.getUserId(rundata),
        ALAccessControlConstants.POERTLET_FEATURE_SCHEDULE_SELF,
        ALAccessControlConstants.VALUE_ACL_LIST);

    if (!hasAuthority) {
      ALEipUtils.redirectPermissionError(rundata);
      return false;
    }
    return true;
  }

  /**
   * パソコンへ送信するメールの内容を作成する．
   *
   * @return
   */
  public static String createMsg(RunData rundata, EipTSchedule schedule,
      List<ALEipUser> memberList, Integer destUserID, String mode) {
    boolean enableAsp = JetspeedResources.getBoolean("aipo.asp", false);
    ALEipUser loginUser = null;
    ALBaseUser user = null;
    String date_detail = "";

    try {
      loginUser = ALEipUtils.getALEipUser(rundata);
      user =
        (ALBaseUser) JetspeedSecurity.getUser(new UserIdPrincipal(loginUser
          .getUserId()
          .toString()));
      date_detail = getMsgDate(schedule);
    } catch (Exception e) {
      return "";
    }

    StringWriter out = null;
    try {
      VelocityService service =
        (VelocityService) ((TurbineServices) TurbineServices.getInstance())
          .getService(VelocityService.SERVICE_NAME);
      Context context = service.getContext();

      context.put("userName", loginUser.getAliasName().toString());
      context.put("mailAddress", user.getEmail());
      if ("new".equals(mode)) {
        context.put("addScheduleMSG", ALLocalizationUtils
          .getl10n("SCHEDULE_ADD_SCHEDULE_FROM_USER"));
      } else if ("edit".equals(mode)) {
        context.put("addScheduleMSG", ALLocalizationUtils
          .getl10n("SCHEDULE_EDIT_SCHEDULE_FROM_USER"));
      } else if ("delete".equals(mode)) {
        context.put("addScheduleMSG", ALLocalizationUtils
          .getl10n("SCHEDULE_DELETE_SCHEDULE_FROM_USER"));
      } else {
        throw new IllegalArgumentException();
      }
      context.put("title", ALLocalizationUtils.getl10n("SCHEDULE_SUB_TITLE"));
      context.put("titleValue", schedule.getName().toString());
      context.put("date", ALLocalizationUtils.getl10n("SCHEDULE_SUB_DATE"));
      context.put("dateValue", date_detail);

      if (schedule.getPlace().toString().length() > 0) {
        context.put("place", ALLocalizationUtils.getl10n("SCHEDULE_SUB_PLACE"));
        context.put("placeValue", schedule.getPlace().toString());
      }

      if (schedule.getNote().toString().length() > 0) {
        context.put("note", ALLocalizationUtils.getl10n("SCHEDULE_SUB_NOTE"));
        context.put("noteValue", schedule.getNote().toString());
      }

      if (memberList != null) {
        int size = memberList.size();
        int i;
        StringBuffer body = new StringBuffer("");
        context.put("menbers", ALLocalizationUtils
          .getl10n("SCHEDULE_SUB_MENBERS"));
        for (i = 0; i < size; i++) {
          if (i != 0) {
            body.append(", ");
          }
          ALEipUser member = memberList.get(i);
          body.append(member.getAliasName());
        }
        context.put("menbersList", body.toString());
      }

      context.put("Alias", ALOrgUtilsService.getAlias());
      context
        .put("accessTo", ALLocalizationUtils.getl10n("SCHEDULE_ACCESS_TO"));

      if (destUserID != null) {
        ALEipUser destUser;
        try {
          destUser = ALEipUtils.getALEipUser(destUserID);
        } catch (ALDBErrorException ex) {
          logger.error("schedule", ex);
          return "";
        }
        context.put("globalUrl1", ALMailUtils.getGlobalurl()
          + "?key="
          + ALCellularUtils.getCellularKey(destUser));
      } else {
        if (enableAsp) {
          context.put("globalUrl1", ALMailUtils.getGlobalurl());
        } else {
          context.put("outsideOffice", ALLocalizationUtils
            .getl10n("SCHEDULE_OUTSIDE_OFFICE"));
          context.put("globalurl2", ALMailUtils.getGlobalurl());
          context.put("insideOffice", ALLocalizationUtils
            .getl10n("SCHEDULE_INSIDE_OFFICE"));
          context.put("globalUrl3", ALMailUtils.getLocalurl());
        }
      }

      out = new StringWriter();
      service.handleRequest(context, "mail/createSchedule.vm", out);
      out.flush();
      return out.toString();
    } catch (IllegalArgumentException e) {

    } catch (Exception e) {
      String message = e.getMessage();
      logger.warn(message, e);
      e.printStackTrace();
    } finally {
      if (out != null) {
        try {
          out.close();
        } catch (IOException e) {
          // ignore
        }
      }
    }
    return null;
  }

  public static String getMsgDate(EipTSchedule schedule) {
    Calendar start_cal = Calendar.getInstance();
    start_cal.setTime(schedule.getStartDate());
    Calendar end_cal = Calendar.getInstance();
    end_cal.setTime(schedule.getEndDate());

    StringBuffer result = new StringBuffer();
    // DN -> 毎日 (A = N -> 期限なし A = L -> 期限あり)
    // WnnnnnnnN W01111110 -> 毎週(月～金用)
    // MnnN M25 -> 毎月25日
    // YnnmmN Y0825N ->　毎年8月25日
    // S -> 期間での指定
    String ptn = schedule.getRepeatPattern();
    int count = 0;
    boolean is_repeat = true;
    boolean is_span = false;
    // 毎日
    if (ptn.charAt(0) == 'D') {
      result.append(ALLocalizationUtils.getl10n("SCHEDULE_EVERY_DAY"));
      count = 1;
      // 毎週
    } else if (ptn.charAt(0) == 'W') {
      result.append(new StringBuffer().append(
        ALLocalizationUtils.getl10n("SCHEDULE_EVERY_WEEK")).append(
        ptn.charAt(1) != '0'
          ? ALLocalizationUtils.getl10n("SCHEDULE_SUNDAY")
          : "").append(
        ptn.charAt(2) != '0'
          ? ALLocalizationUtils.getl10n("SCHEDULE_MONDAY")
          : "").append(
        ptn.charAt(3) != '0'
          ? ALLocalizationUtils.getl10n("SCHEDULE_TUSEDAY")
          : "").append(
        ptn.charAt(4) != '0' ? ALLocalizationUtils
          .getl10n("SCHEDULE_WEDNESDAY") : "").append(
        ptn.charAt(5) != '0'
          ? ALLocalizationUtils.getl10n("SCHEDULE_THURSDAY")
          : "").append(
        ptn.charAt(6) != '0'
          ? ALLocalizationUtils.getl10n("SCHEDULE_FRIDAY")
          : "").append(
        ptn.charAt(7) != '0'
          ? ALLocalizationUtils.getl10n("SCHEDULE_SATURDAY")
          : "").append(
        ALLocalizationUtils.getl10n("SCHEDULE_A_DAY_OF_THE_WEEK")).toString());
      count = 8;
      // 毎月
    } else if (ptn.charAt(0) == 'M') {
      if (ptn.substring(1, 3).equals("XX")) {
        result
          .append(ALLocalizationUtils.getl10n("SCHEDULE_EVERY_MONTH_SPACE"))
          .append(ALLocalizationUtils.getl10n("SCHEDULE_END_OF_MONTH"))
          .append(ALLocalizationUtils.getl10n("SCHEDULE_DAY"))
          .toString();
      } else {
        result
          .append(ALLocalizationUtils.getl10n("SCHEDULE_EVERY_MONTH_SPACE"))
          .append(Integer.parseInt(ptn.substring(1, 3)))
          .append(ALLocalizationUtils.getl10n("SCHEDULE_DAY"))
          .toString();
      }
      count = 3;
      // 毎年
    } else if (ptn.charAt(0) == 'Y') {
      result
        .append(ALLocalizationUtils.getl10n("SCHEDULE_EVERY_YEAR_SPACE"))
        .append(Integer.parseInt(ptn.substring(1, 3)))
        .append(ALLocalizationUtils.getl10n("SCHEDULE_MONTH"))
        .append(Integer.parseInt(ptn.substring(3, 5)))
        .append(ALLocalizationUtils.getl10n("SCHEDULE_DAY"))
        .toString();
      count = 5;
      // 期間
    } else if (ptn.charAt(0) == 'S') {// 期間はS
      is_span = true;
      is_repeat = false;
    } else {
      is_repeat = false;
    }

    ALDateTimeField date_field = new ALDateTimeField("yyyy/MM/dd");
    ALDateTimeField time_field = new ALDateTimeField("HH:mm");

    if (!is_span) {
      if (!is_repeat) {
        date_field.setValue(schedule.getStartDate());
        result.append(date_field.toString());
      }
      time_field.setValue(schedule.getStartDate());
      // メールの場合は期間を表すのに - を用いる
      result.append(" ").append(time_field.toString()).append(" - ");
      time_field.setValue(schedule.getEndDate());
      result.append(time_field.toString()).append(" ");
    } else {
      Date start = schedule.getStartDate();
      Date end = schedule.getEndDate();
      date_field.setValue(start);
      result.append(date_field.toString());
      if (!start.equals(end)) {
        result.append(" - ");
        date_field.setValue(end);
        result.append(date_field.toString());
      }
    }

    if (is_repeat) {
      if (ptn.charAt(count) == 'N') {
        // schedule.setLimit(false);
      } else {
        // schedule.setLimit(true);
        // 期限
        date_field.setValue(schedule.getStartDate());
        result.append(" （").append(date_field.toString()).append(" - ");
        date_field.setValue(schedule.getEndDate());
        result.append(date_field.toString()).append("）").toString();
      }
    }
    return result.toString();
  }

  public static boolean isDuplicateFacilitySchedule(EipTSchedule schedule,
      List<Integer> facilityIdList, Integer _old_scheduleid, Date _old_viewDate) {
    /* ダミースケジュール検索用 */
    GregorianCalendar cald = new GregorianCalendar();

    boolean result = false;
    {

      Date start_date;
      Date end_date;
      String repeat_pattern;
      String repeat_type;
      String repeat_week = null;
      boolean week_0;
      boolean week_1;
      boolean week_2;
      boolean week_3;
      boolean week_4;
      boolean week_5;
      boolean week_6;
      boolean day_of_week_in_month_1;
      boolean day_of_week_in_month_2;
      boolean day_of_week_in_month_3;
      boolean day_of_week_in_month_4;
      boolean day_of_week_in_month_5;
      boolean[] day_of_week_in_month_array = new boolean[5];
      String limit_flag;
      int month_day = -1;
      int year_month = -1;
      int year_day = -1;
      Integer db_scheduleid = null;
      boolean[] week_array = new boolean[7];
      boolean unlimited_repeat = false;
      try {
        start_date = schedule.getStartDate();

        end_date = schedule.getEndDate();

        repeat_pattern = schedule.getRepeatPattern();

        repeat_type = repeat_pattern.substring(0, 1);

        day_of_week_in_month_1 = repeat_pattern.matches("W.......1.?");

        day_of_week_in_month_2 = repeat_pattern.matches("W.......2.?");

        day_of_week_in_month_3 = repeat_pattern.matches("W.......3.?");

        day_of_week_in_month_4 = repeat_pattern.matches("W.......4.?");

        day_of_week_in_month_5 = repeat_pattern.matches("W.......5.?");

        if (repeat_type.equals("W")) {
          if (repeat_pattern.length() == 9) {
            repeat_week = "0";
            day_of_week_in_month_1 = true;
            day_of_week_in_month_2 = true;
            day_of_week_in_month_3 = true;
            day_of_week_in_month_4 = true;
            day_of_week_in_month_5 = true;
          } else {
            repeat_week = repeat_pattern.substring(8, 9);
          }
        }

        limit_flag = repeat_pattern.substring(repeat_pattern.length() - 1);

        week_0 = repeat_pattern.matches("W1........?");

        week_1 = repeat_pattern.matches("W.1.......?");

        week_2 = repeat_pattern.matches("W..1......?");

        week_3 = repeat_pattern.matches("W...1.....?");

        week_4 = repeat_pattern.matches("W....1....?");

        week_5 = repeat_pattern.matches("W.....1...?");

        week_6 = repeat_pattern.matches("W......1..?");

        if (repeat_pattern.startsWith("M")) {
          month_day = Integer.parseInt(repeat_pattern.substring(1, 3));
        }

        if (repeat_pattern.startsWith("Y")) {
          year_month = Integer.parseInt(repeat_pattern.substring(1, 3));
          year_day = Integer.parseInt(repeat_pattern.substring(3, 5));
        }
        // 単体スケジュールは期限1日のみのスケジュールとして判定
        if (repeat_pattern.startsWith("N")) {
          Calendar cal = Calendar.getInstance();
          cal.setTime(start_date);
          int dow = cal.get(Calendar.DAY_OF_WEEK);
          week_0 = (dow == Calendar.SUNDAY);
          week_1 = (dow == Calendar.MONDAY);
          week_2 = (dow == Calendar.TUESDAY);
          week_3 = (dow == Calendar.WEDNESDAY);
          week_4 = (dow == Calendar.THURSDAY);
          week_5 = (dow == Calendar.FRIDAY);
          week_6 = (dow == Calendar.SATURDAY);
          month_day = cal.get(Calendar.DAY_OF_MONTH);
          int dowim = cal.get(Calendar.DAY_OF_WEEK_IN_MONTH);
          day_of_week_in_month_1 = (dowim == 1);
          day_of_week_in_month_2 = (dowim == 2);
          day_of_week_in_month_3 = (dowim == 3);
          day_of_week_in_month_4 = (dowim == 4);
          day_of_week_in_month_5 = (dowim == 5);
          year_month = cal.get(Calendar.MONTH) + 1;
          year_day = cal.get(Calendar.DAY_OF_MONTH);
        } else if (repeat_pattern.endsWith("N")) {
          unlimited_repeat = true;
        }

        week_array[0] = week_0;
        week_array[1] = week_1;
        week_array[2] = week_2;
        week_array[3] = week_3;
        week_array[4] = week_4;
        week_array[5] = week_5;
        week_array[6] = week_6;

        day_of_week_in_month_array[0] = day_of_week_in_month_1;
        day_of_week_in_month_array[1] = day_of_week_in_month_2;
        day_of_week_in_month_array[2] = day_of_week_in_month_3;
        day_of_week_in_month_array[3] = day_of_week_in_month_4;
        day_of_week_in_month_array[4] = day_of_week_in_month_5;

      } catch (RuntimeException e) {
        logger.error("schedule", e);
        return false;
      } catch (Exception e) {
        logger.error("schedule", e);
        return false;
      }

      if (repeat_type.equals("S")) {
        // 期間スケジュールで終了時刻が0:00:00になっている分を23:59:59に補正する
        Calendar cal = Calendar.getInstance();
        cal.setTime(end_date);
        cal.add(Calendar.DATE, 1);
        cal.add(Calendar.MINUTE, -1);
        end_date = cal.getTime();
      }

      // 設備予約状況をチェックする
      if (facilityIdList.size() > 0) {//
        List<Integer> fids = facilityIdList;
        SelectQuery<EipTScheduleMap> fquery =
          Database.query(EipTScheduleMap.class);
        Expression fexp1 =
          ExpressionFactory.inExp(EipTScheduleMap.USER_ID_PROPERTY, fids);
        fquery.setQualifier(fexp1);

        Expression fexp2 =
          ExpressionFactory.matchExp(
            EipTScheduleMap.TYPE_PROPERTY,
            ScheduleUtils.SCHEDULEMAP_TYPE_FACILITY);
        fquery.andQualifier(fexp2);

        Expression oneexp = null;// 1日
        Expression spanexp = null;// 期間
        Expression rdexp = null;// 毎日
        Expression rwexp = null;// 毎週何曜日
        Expression rwexp2 = null;
        // Expression rwlexp = null;
        Expression rmexp = null;// 毎月何日
        Expression ryexp = null;

        { // １日スケジュールの検索
          Expression exp100 =
            ExpressionFactory.matchExp(EipTScheduleMap.EIP_TSCHEDULE_PROPERTY
              + "."
              + EipTSchedule.REPEAT_PATTERN_PROPERTY, "N");

          try {
            if (!unlimited_repeat) {
              Expression exp101 =
                ExpressionFactory.lessOrEqualExp(
                  EipTScheduleMap.EIP_TSCHEDULE_PROPERTY
                    + "."
                    + EipTSchedule.START_DATE_PROPERTY,
                  end_date);// EipTSchedule.START_DATE_PROPERTY <= end_date
              Expression exp102 =
                ExpressionFactory.greaterExp(
                  EipTScheduleMap.EIP_TSCHEDULE_PROPERTY
                    + "."
                    + EipTSchedule.END_DATE_PROPERTY,
                  start_date);// EipTSchedule.END_DATE_PROPERTY > start_date

              oneexp = exp100.andExp(exp101.andExp(exp102));

            } else {
              oneexp = exp100;
            }
          } catch (Exception e) {

          }
        }

        { // 期間スケジュールの検索
          Expression exp200 =
            ExpressionFactory.matchExp(EipTScheduleMap.EIP_TSCHEDULE_PROPERTY
              + "."
              + EipTSchedule.REPEAT_PATTERN_PROPERTY, "S");

          try {
            if (!unlimited_repeat) {
              // 期間スケジュールは、00:00に切り捨てたスケジュールで検索する
              Calendar cal_end = Calendar.getInstance();
              cal_end.setTime(end_date);
              cal_end = DateUtils.truncate(cal_end, Calendar.DAY_OF_MONTH);
              Expression exp201 =
                ExpressionFactory.lessOrEqualExp(
                  EipTScheduleMap.EIP_TSCHEDULE_PROPERTY
                    + "."
                    + EipTSchedule.START_DATE_PROPERTY,
                  cal_end.getTime());
              // EipTSchedule.START_DATE_PROPERTY <= end_date
              Calendar cal_start = Calendar.getInstance();
              cal_start.setTime(start_date);
              cal_start = DateUtils.truncate(cal_start, Calendar.DAY_OF_MONTH);
              Expression exp202 =
                ExpressionFactory.greaterOrEqualExp(
                  EipTScheduleMap.EIP_TSCHEDULE_PROPERTY
                    + "."
                    + EipTSchedule.END_DATE_PROPERTY,
                  cal_start.getTime());
              // EipTSchedule.END_DATE_PROPERTY >= start_date

              spanexp = exp200.andExp(exp201.andExp(exp202));

            } else {
              spanexp = exp200;
            }
          } catch (Exception e) {

          }
        }

        { // 繰り返しスケジュールの検索
          // char lim = 'N';
          if ("ON".equals(limit_flag)) {
            // lim = 'L';
          }

          { // "D".equals(repeat_type.getValue())
            Expression dexp01 =
              ExpressionFactory.likeExp(EipTScheduleMap.EIP_TSCHEDULE_PROPERTY
                + "."
                + EipTSchedule.REPEAT_PATTERN_PROPERTY, "D_");
            rdexp = dexp01;
          }

          { // "W".equals(repeat_type.getValue())
            Expression wexp = null;
            List<Expression> wexps = new ArrayList<Expression>();
            if (week_0 == true) {
              wexp =
                ExpressionFactory.likeExp(
                  EipTScheduleMap.EIP_TSCHEDULE_PROPERTY
                    + "."
                    + EipTSchedule.REPEAT_PATTERN_PROPERTY,
                  "W1_______");
              wexps.add(wexp);
            }
            if (week_1 == true) {
              wexp =
                ExpressionFactory.likeExp(
                  EipTScheduleMap.EIP_TSCHEDULE_PROPERTY
                    + "."
                    + EipTSchedule.REPEAT_PATTERN_PROPERTY,
                  "W_1______");
              wexps.add(wexp);
            }
            if (week_2 == true) {
              wexp =
                ExpressionFactory.likeExp(
                  EipTScheduleMap.EIP_TSCHEDULE_PROPERTY
                    + "."
                    + EipTSchedule.REPEAT_PATTERN_PROPERTY,
                  "W__1_____");
              wexps.add(wexp);
            }
            if (week_3 == true) {
              wexp =
                ExpressionFactory.likeExp(
                  EipTScheduleMap.EIP_TSCHEDULE_PROPERTY
                    + "."
                    + EipTSchedule.REPEAT_PATTERN_PROPERTY,
                  "W___1____");
              wexps.add(wexp);
            }
            if (week_4 == true) {
              wexp =
                ExpressionFactory.likeExp(
                  EipTScheduleMap.EIP_TSCHEDULE_PROPERTY
                    + "."
                    + EipTSchedule.REPEAT_PATTERN_PROPERTY,
                  "W____1___");
              wexps.add(wexp);
            }
            if (week_5 == true) {
              wexp =
                ExpressionFactory.likeExp(
                  EipTScheduleMap.EIP_TSCHEDULE_PROPERTY
                    + "."
                    + EipTSchedule.REPEAT_PATTERN_PROPERTY,
                  "W_____1__");
              wexps.add(wexp);
            }
            if (week_6 == true) {
              wexp =
                ExpressionFactory.likeExp(
                  EipTScheduleMap.EIP_TSCHEDULE_PROPERTY
                    + "."
                    + EipTSchedule.REPEAT_PATTERN_PROPERTY,
                  "W______1_");
              wexps.add(wexp);
            }
            if (wexps.size() > 0) {
              rwexp = wexps.get(0);
              int wexpssize = wexps.size();
              for (int k = 1; k < wexpssize; k++) {
                rwexp = rwexp.orExp(wexps.get(k));
              }
            } else {
              rwexp =
                ExpressionFactory.likeExp(
                  EipTScheduleMap.EIP_TSCHEDULE_PROPERTY
                    + "."
                    + EipTSchedule.REPEAT_PATTERN_PROPERTY,
                  "W________");
            }
          }
          {
            Expression wexp2 = null;
            List<Expression> wexps2 = new ArrayList<Expression>();
            Expression wnexp = null;
            List<Expression> wnexp2 = new ArrayList<Expression>();

            if (week_0 == true) {
              wexp2 =
                ExpressionFactory.likeExp(
                  EipTScheduleMap.EIP_TSCHEDULE_PROPERTY
                    + "."
                    + EipTSchedule.REPEAT_PATTERN_PROPERTY,
                  "W1________");
              wexps2.add(wexp2);
            }
            if (week_1 == true) {
              wexp2 =
                ExpressionFactory.likeExp(
                  EipTScheduleMap.EIP_TSCHEDULE_PROPERTY
                    + "."
                    + EipTSchedule.REPEAT_PATTERN_PROPERTY,
                  "W_1_______");
              wexps2.add(wexp2);
            }
            if (week_2 == true) {
              wexp2 =
                ExpressionFactory.likeExp(
                  EipTScheduleMap.EIP_TSCHEDULE_PROPERTY
                    + "."
                    + EipTSchedule.REPEAT_PATTERN_PROPERTY,
                  "W__1______");
              wexps2.add(wexp2);
            }
            if (week_3 == true) {
              wexp2 =
                ExpressionFactory.likeExp(
                  EipTScheduleMap.EIP_TSCHEDULE_PROPERTY
                    + "."
                    + EipTSchedule.REPEAT_PATTERN_PROPERTY,
                  "W___1_____");
              wexps2.add(wexp2);
            }
            if (week_4 == true) {
              wexp2 =
                ExpressionFactory.likeExp(
                  EipTScheduleMap.EIP_TSCHEDULE_PROPERTY
                    + "."
                    + EipTSchedule.REPEAT_PATTERN_PROPERTY,
                  "W____1____");
              wexps2.add(wexp2);
            }
            if (week_5 == true) {
              wexp2 =
                ExpressionFactory.likeExp(
                  EipTScheduleMap.EIP_TSCHEDULE_PROPERTY
                    + "."
                    + EipTSchedule.REPEAT_PATTERN_PROPERTY,
                  "W_____1___");
              wexps2.add(wexp2);
            }
            if (week_6 == true) {
              wexp2 =
                ExpressionFactory.likeExp(
                  EipTScheduleMap.EIP_TSCHEDULE_PROPERTY
                    + "."
                    + EipTSchedule.REPEAT_PATTERN_PROPERTY,
                  "W______1__");
              wexps2.add(wexp2);
            }
            if (repeat_week != null
              && repeat_week.equals("1")
              || day_of_week_in_month_1) {
              wnexp =
                ExpressionFactory.likeExp(
                  EipTScheduleMap.EIP_TSCHEDULE_PROPERTY
                    + "."
                    + EipTSchedule.REPEAT_PATTERN_PROPERTY,
                  "W_______1_");
              wnexp2.add(wnexp);
            }
            if (repeat_week != null
              && repeat_week.equals("2")
              || day_of_week_in_month_2) {
              wnexp =
                ExpressionFactory.likeExp(
                  EipTScheduleMap.EIP_TSCHEDULE_PROPERTY
                    + "."
                    + EipTSchedule.REPEAT_PATTERN_PROPERTY,
                  "W_______2_");
              wnexp2.add(wnexp);
            }
            if (repeat_week != null
              && repeat_week.equals("3")
              || day_of_week_in_month_3) {
              wnexp =
                ExpressionFactory.likeExp(
                  EipTScheduleMap.EIP_TSCHEDULE_PROPERTY
                    + "."
                    + EipTSchedule.REPEAT_PATTERN_PROPERTY,
                  "W_______3_");
              wnexp2.add(wnexp);
            }
            if (repeat_week != null
              && repeat_week.equals("4")
              || day_of_week_in_month_4) {
              wnexp =
                ExpressionFactory.likeExp(
                  EipTScheduleMap.EIP_TSCHEDULE_PROPERTY
                    + "."
                    + EipTSchedule.REPEAT_PATTERN_PROPERTY,
                  "W_______4_");
              wnexp2.add(wnexp);
            }
            if (repeat_week != null
              && repeat_week.equals("5")
              || day_of_week_in_month_5) {
              wnexp =
                ExpressionFactory.likeExp(
                  EipTScheduleMap.EIP_TSCHEDULE_PROPERTY
                    + "."
                    + EipTSchedule.REPEAT_PATTERN_PROPERTY,
                  "W_______5_");
              wnexp2.add(wnexp);
            }
            if (wexps2.size() > 0 && wnexp2.size() > 0) {
              for (int k = 0; k < wexps2.size(); k++) {
                for (int l = 0; l < wnexp2.size(); l++) {
                  if (k == 0 && l == 0) {
                    rwexp2 = wexps2.get(k).andExp(wnexp2.get(l));
                  } else {
                    rwexp2 = rwexp2.orExp(wexps2.get(k).andExp(wnexp2.get(l)));
                  }
                }
              }
            } else {
              rwexp2 =
                ExpressionFactory.likeExp(
                  EipTScheduleMap.EIP_TSCHEDULE_PROPERTY
                    + "."
                    + EipTSchedule.REPEAT_PATTERN_PROPERTY,
                  "W_________");
            }

          }

          { // "M".equals(repeat_type.getValue())
            if (month_day > 0) { // 毎月、もしくは単体の場合
              DecimalFormat exF = new DecimalFormat("00");
              String md_str = exF.format(month_day);
              rmexp =
                ExpressionFactory.likeExp(
                  EipTScheduleMap.EIP_TSCHEDULE_PROPERTY
                    + "."
                    + EipTSchedule.REPEAT_PATTERN_PROPERTY,
                  "M" + md_str + "_");

            } else if (year_day > 0 && year_month > 0) { // 毎年の場合
              DecimalFormat exG = new DecimalFormat("00");
              String yd_str = exG.format(year_day);
              rmexp =
                ExpressionFactory.likeExp(
                  EipTScheduleMap.EIP_TSCHEDULE_PROPERTY
                    + "."
                    + EipTSchedule.REPEAT_PATTERN_PROPERTY,
                  "M" + yd_str + "_");
            } else {
              rmexp =
                ExpressionFactory.likeExp(
                  EipTScheduleMap.EIP_TSCHEDULE_PROPERTY
                    + "."
                    + EipTSchedule.REPEAT_PATTERN_PROPERTY,
                  "M___");
            }
          }

          { // "Y".equals(repeat_type.getValue())
            if (year_day > 0 && year_month > 0) { // 毎年、もしくは単体の場合
              DecimalFormat exG = new DecimalFormat("00");
              String ym_str = exG.format(year_month);
              String yd_str = exG.format(year_day);

              ryexp =
                ExpressionFactory.likeExp(
                  EipTScheduleMap.EIP_TSCHEDULE_PROPERTY
                    + "."
                    + EipTSchedule.REPEAT_PATTERN_PROPERTY,
                  "Y" + ym_str + yd_str + "_");
            } else if (month_day > 0) { // 毎月の場合
              DecimalFormat exF = new DecimalFormat("00");
              String md_str = exF.format(month_day);
              ryexp =
                ExpressionFactory.likeExp(
                  EipTScheduleMap.EIP_TSCHEDULE_PROPERTY
                    + "."
                    + EipTSchedule.REPEAT_PATTERN_PROPERTY,
                  "Y__" + md_str + "_");
            } else {
              ryexp =
                ExpressionFactory.likeExp(
                  EipTScheduleMap.EIP_TSCHEDULE_PROPERTY
                    + "."
                    + EipTSchedule.REPEAT_PATTERN_PROPERTY,
                  "Y_____");
            }
          }

          Expression repeatexp = oneexp;
          if (rdexp != null) {
            repeatexp = repeatexp.orExp(rdexp);
          }
          if (rwexp != null) {
            repeatexp = repeatexp.orExp(rwexp);
          }
          if (rwexp2 != null) {
            repeatexp = repeatexp.orExp(rwexp2);
          }
          if (rmexp != null) {
            repeatexp = repeatexp.orExp(rmexp);
          }
          if (spanexp != null) {
            repeatexp = repeatexp.orExp(spanexp);
          }
          if (ryexp != null) {
            repeatexp = repeatexp.orExp(ryexp);
          }
          fquery.andQualifier(repeatexp);
        }

        db_scheduleid = schedule.getScheduleId();
        if (db_scheduleid != null && db_scheduleid >= 0) {
          Expression exp00 =
            ExpressionFactory.noMatchDbExp(
              EipTScheduleMap.EIP_TSCHEDULE_PROPERTY
                + "."
                + EipTSchedule.SCHEDULE_ID_PK_COLUMN,
              db_scheduleid);
          fquery.andQualifier(exp00);
        }

        fquery.distinct(true);
        List<EipTScheduleMap> f_list = fquery.fetchList();
        if (f_list != null && f_list.size() > 0) {
          // 繰り返しスケジュール同士の時刻幅での比較
          boolean existFacility = false;
          int f_list_size = f_list.size();
          for (int i = 0; i < f_list_size; i++) {
            EipTScheduleMap map = f_list.get(i);

            Date dbStartDate = map.getEipTSchedule().getStartDate();
            Date dbEndDate = map.getEipTSchedule().getEndDate();

            boolean containtsRs = false;
            // 繰り返し期限付きの処理
            String ptn = map.getEipTSchedule().getRepeatPattern();
            if (ptn.charAt(0) == 'S') { // 期間スケジュール
              try {
                // 期間スケジュールで終了時刻が0:00:00になっている分を23:59:59に補正する
                Calendar cal = Calendar.getInstance();
                cal.setTime(dbEndDate);
                cal.add(Calendar.DATE, 1);
                cal.add(Calendar.MINUTE, -1);
                dbEndDate = cal.getTime();

                if ((end_date.after(dbStartDate) && start_date
                  .before(dbEndDate))
                  || unlimited_repeat) {
                  containtsRs = true;
                }
              } catch (Exception e) {
                containtsRs = false;
              }
            } else if (ptn.charAt(0) == 'N') { // 単体スケジュール
              if ("D".equals(repeat_type) || "N".equals(repeat_type)) { // 毎日 or
                // 単体
                try {
                  if ((dbStartDate.before(end_date) && dbEndDate
                    .after(start_date))
                    || unlimited_repeat) {
                    containtsRs = true;
                  }
                } catch (Exception e) {
                  containtsRs = false;
                }
              } else {
                if ((dbStartDate.before(end_date) && dbEndDate
                  .after(start_date))
                  || unlimited_repeat) {
                  containtsRs = true;
                }
              }
            } else if (ptn.charAt(0) == 'D') {// 毎日
              if (ptn.charAt(1) == 'L') {
                try {
                  if ((dbStartDate.before(end_date) && dbEndDate
                    .after(start_date))
                    || unlimited_repeat) {
                    containtsRs = true;
                  }
                } catch (Exception e) {
                  containtsRs = false;
                }
              } else {
                containtsRs = true;
              }
            } else if (ptn.charAt(0) == 'W') {
              if (ptn.length() == 9) {
                if (ptn.charAt(8) == 'L') {
                  try {
                    if ((dbStartDate.before(end_date) && dbEndDate
                      .after(start_date))
                      || unlimited_repeat) {
                      containtsRs = true;
                    }
                  } catch (Exception e) {
                    containtsRs = false;
                  }
                } else {
                  containtsRs = true;
                }
              } else if (ptn.length() == 10) {
                if (ptn.charAt(9) == 'L') {
                  try {
                    if ((dbStartDate.before(end_date) && dbEndDate
                      .after(start_date))
                      || unlimited_repeat) {
                      containtsRs = true;
                    }
                  } catch (Exception e) {
                    containtsRs = false;
                  }
                } else {
                  containtsRs = true;
                }
              }
            } else if (ptn.charAt(0) == 'M') {
              if (ptn.charAt(3) == 'L') {
                try {
                  if ((dbStartDate.before(end_date) && dbEndDate
                    .after(start_date))
                    || unlimited_repeat) {
                    containtsRs = true;
                  }
                } catch (Exception e) {
                  containtsRs = false;
                }
              } else {
                containtsRs = true;
              }
            } else if (ptn.charAt(0) == 'Y') {
              if (ptn.charAt(5) == 'L') {
                try {
                  if ((dbStartDate.before(end_date) && dbEndDate
                    .after(start_date))
                    || unlimited_repeat) {
                    containtsRs = true;
                  }
                } catch (Exception e) {
                  containtsRs = false;
                }
              } else {
                containtsRs = true;
              }
            } else {
              containtsRs = true;
            }

            if (containtsRs) {
              /* ここで時間の重複をチェック */
              int ss_flg = ScheduleUtils.compareTime(start_date, dbEndDate);
              int se_flg = ScheduleUtils.compareTime(end_date, dbStartDate);
              if (ss_flg > 0 && se_flg < 0) {
                /* 期限無しのスケジュール同士の場合は重複とみなす */
                if (!"N".equals(ptn) && ptn.endsWith("N") && unlimited_repeat) {// 新は(単体ではない&&期限なし)&&元は期限なし
                  existFacility = true;
                } else {
                  Date _start_date = null;
                  Date _end_date = null;

                  if (!"N".equals(ptn)
                    && ptn.endsWith("N")
                    && !unlimited_repeat) {// 元は(！単体&&制限なし)&&新は制限あり
                    _start_date = (Date) start_date.clone();
                    _end_date = (Date) end_date.clone();
                  } else if (("N".equals(ptn) || !ptn.endsWith("N"))
                    && unlimited_repeat) {// 元は(単体||制限あり)&&新は制限なし
                    _start_date = (Date) dbStartDate.clone();
                    _end_date = (Date) dbEndDate.clone();
                  } else if (("N".equals(ptn) || !ptn.endsWith("N"))
                    && !unlimited_repeat) {// 元は(単体||制限あり)&&新は制限あり

                    if (dbStartDate.after(start_date)) {
                      _start_date = (Date) dbStartDate.clone();
                    } else {
                      _start_date = (Date) start_date.clone();
                    }

                    if (dbEndDate.before(end_date)) {
                      _end_date = (Date) dbEndDate.clone();
                    } else {
                      _end_date = (Date) end_date.clone();
                    }

                  }

                  if ((_start_date == null) || (_end_date == null)) {
                    continue;
                  }

                  /* 期限内の日付を全て比較 */
                  Expression dexp1 =
                    ExpressionFactory.matchExp(
                      EipTSchedule.NAME_PROPERTY,
                      "dummy");// ダミースケジュール

                  Expression dexp2 =
                    ExpressionFactory.matchExp(
                      EipTSchedule.PARENT_ID_PROPERTY,
                      map.getScheduleId());

                  if (db_scheduleid != null) {
                    Expression dexp21 =
                      ExpressionFactory.matchExp(
                        EipTSchedule.PARENT_ID_PROPERTY,
                        db_scheduleid);
                    dexp2 = dexp2.orExp(dexp21);
                  }
                  Expression dexp3 = null;

                  cald.setTime(_start_date);
                  cald.set(Calendar.MILLISECOND, 0);
                  cald.set(Calendar.SECOND, 0);
                  cald.set(Calendar.MINUTE, 0);
                  cald.set(Calendar.HOUR_OF_DAY, 0);
                  Date ddate = cald.getTime();// _start_dateの時間
                  List<EipTSchedule> temp = null;

                  if ("N".equals(repeat_pattern)) {
                    /* 繰り返しスケジュールのうちひとつだけを移動した場合の処理 */
                    if ((_old_scheduleid != null) && (_old_viewDate != null)) {
                      if ((_old_scheduleid.intValue() == map
                        .getScheduleId()
                        .intValue())
                        && compareToDate(_start_date, _old_viewDate) == 0) {
                        continue;
                      }
                    }

                    try {
                      dexp3 =
                        ExpressionFactory.matchExp(
                          EipTSchedule.START_DATE_PROPERTY,
                          ddate);
                      temp =
                        Database.query(
                          EipTSchedule.class,
                          dexp1.andExp(dexp2).andExp(dexp3)).fetchList();
                      if (temp == null || temp.size() <= 0) {
                        existFacility = true;
                        break;
                      }
                    } catch (Exception e) {
                      logger.error("[DuplicateFacilityCheck]: ", e);
                      existFacility = true;
                      break;
                    }
                  } else if (repeat_pattern.startsWith("D")) {
                    while (!ddate.after(_end_date)) {
                      if (matchDay(cald, ptn)) {
                        try {
                          dexp3 =
                            ExpressionFactory.matchExp(
                              EipTSchedule.START_DATE_PROPERTY,
                              ddate);
                          temp =
                            Database.query(
                              EipTSchedule.class,
                              dexp1.andExp(dexp2).andExp(dexp3)).fetchList();
                          if (temp == null || temp.size() <= 0) {
                            existFacility = true;
                            break;
                          }
                        } catch (Exception e) {
                          logger.error("[DuplicateFacilityCheck]: ", e);
                          existFacility = true;
                          break;
                        }
                      }
                      cald.add(Calendar.DATE, 1);
                      ddate = cald.getTime();
                    }
                  } else if (repeat_pattern.startsWith("S")) {
                    while (!ddate.after(_end_date)) {
                      try {
                        dexp3 =
                          ExpressionFactory.matchExp(
                            EipTSchedule.START_DATE_PROPERTY,
                            ddate);
                        temp =
                          Database.query(
                            EipTSchedule.class,
                            dexp1.andExp(dexp2).andExp(dexp3)).fetchList();
                        if (temp == null || temp.size() <= 0) {
                          existFacility = true;
                          break;
                        }
                      } catch (Exception e) {
                        logger.error("[DuplicateFacilityCheck]: ", e);
                        existFacility = true;
                        break;
                      }
                      cald.add(Calendar.DATE, 1);
                      ddate = cald.getTime();
                    }
                  } else if (repeat_pattern.startsWith("W")) {
                    /* ダミースケジュールを探す */
                    int wlen = week_array.length;
                    int wlen2 = day_of_week_in_month_array.length;
                    if (wlen < 1 || wlen2 < 1) {
                      continue;
                    }
                    int k;
                    int l;
                    while (!ddate.after(_end_date)) {
                      k = (cald.get(Calendar.DAY_OF_WEEK) - 1) % wlen;
                      l = (cald.get(Calendar.DAY_OF_WEEK_IN_MONTH) - 1) % wlen2;
                      if ((week_array[k] == true)
                        && (day_of_week_in_month_array[l] == true)
                        && matchDay(cald, ptn)) {
                        try {
                          dexp3 =
                            ExpressionFactory.matchExp(
                              EipTSchedule.START_DATE_PROPERTY,
                              ddate);
                          temp =
                            Database.query(
                              EipTSchedule.class,
                              dexp1.andExp(dexp2).andExp(dexp3)).fetchList();// SQL発行
                          if (temp == null || temp.size() <= 0) {
                            existFacility = true;// trueなら引っかかる
                            break;
                          }
                        } catch (Exception e) {
                          logger.error("[DuplicateFacilityCheck]: ", e);
                          existFacility = true;
                          break;
                        }
                      }
                      cald.add(Calendar.DATE, 1);
                      ddate = cald.getTime();
                    }
                  } else if (repeat_pattern.startsWith("M")) {
                    /* 比較開始日までカレンダー移動 */
                    cald.setTime(dbStartDate);
                    cald.set(Calendar.MILLISECOND, 0);
                    cald.set(Calendar.SECOND, 0);
                    cald.set(Calendar.MINUTE, 0);
                    cald.set(Calendar.HOUR_OF_DAY, 0);

                    if (month_day > 0) {
                      cald.set(Calendar.DAY_OF_MONTH, month_day);
                    } else {
                      continue;
                    }
                    Date tmp_date = cald.getTime();
                    while (tmp_date.before(ddate)) {
                      cald.add(Calendar.MONTH, 1);
                      /* 月によって日にちがないときのための処理 */
                      while (month_day > cald
                        .getActualMaximum(Calendar.DAY_OF_MONTH)) {
                        cald.add(Calendar.MONTH, 1);
                        cald.set(Calendar.DAY_OF_MONTH, month_day);
                        if (tmp_date.before(tmp_date)) {
                          break;
                        }
                      }
                      tmp_date = cald.getTime();
                    }
                    ddate = tmp_date;
                    /* 比較開始 */
                    while (!ddate.after(_end_date)) {
                      if (matchDay(cald, ptn)) {
                        try {
                          dexp3 =
                            ExpressionFactory.matchExp(
                              EipTSchedule.START_DATE_PROPERTY,
                              ddate);
                          temp =
                            Database.query(
                              EipTSchedule.class,
                              dexp1.andExp(dexp2).andExp(dexp3)).fetchList();
                          if (temp == null || temp.size() <= 0) {
                            existFacility = true;
                            break;
                          }

                        } catch (Exception e) {
                          logger.error("[DuplicateFacilityCheck]: ", e);
                          existFacility = true;
                          break;
                        }
                      }
                      cald.add(Calendar.MONTH, 1);
                      /* 月によって日にちがないときのための処理 */
                      while (month_day > cald
                        .getActualMaximum(Calendar.DAY_OF_MONTH)) {
                        cald.add(Calendar.MONTH, 1);
                        cald.set(Calendar.DAY_OF_MONTH, month_day);
                        if (!ddate.after(_end_date)) {
                          break;
                        }
                      }
                      ddate = cald.getTime();
                    }
                  } else if (repeat_pattern.startsWith("Y")) {
                    /* 比較開始日までカレンダー移動 */
                    cald.setTime(dbStartDate);
                    cald.set(Calendar.MILLISECOND, 0);
                    cald.set(Calendar.SECOND, 0);
                    cald.set(Calendar.MINUTE, 0);
                    cald.set(Calendar.HOUR_OF_DAY, 0);

                    if (year_month > 0 && year_day > 0) {
                      cald.set(Calendar.MONTH, year_month - 1);
                      cald.set(Calendar.DAY_OF_MONTH, year_day);
                    } else {
                      continue;
                    }
                    Date tmp_date = cald.getTime();
                    while (tmp_date.before(ddate)) {
                      cald.add(Calendar.MONTH, 1);
                      /* 月によって日にちがないときのための処理 */
                      while (year_day > cald
                        .getActualMaximum(Calendar.DAY_OF_MONTH)) {
                        cald.add(Calendar.MONTH, 1);
                        cald.set(Calendar.DAY_OF_MONTH, year_day);
                        if (tmp_date.before(tmp_date)) {
                          break;
                        }
                      }
                      tmp_date = cald.getTime();
                    }
                    ddate = tmp_date;
                    /* 比較開始 */
                    while (!ddate.after(_end_date)) {
                      if (matchDay(cald, ptn)) {
                        try {
                          dexp3 =
                            ExpressionFactory.matchExp(
                              EipTSchedule.START_DATE_PROPERTY,
                              ddate);
                          temp =
                            Database.query(
                              EipTSchedule.class,
                              dexp1.andExp(dexp2).andExp(dexp3)).fetchList();
                          if (temp == null || temp.size() <= 0) {
                            existFacility = true;
                            break;
                          }
                        } catch (Exception e) {
                          logger.error("[DuplicateFacilityCheck]: ", e);
                          existFacility = true;
                          break;
                        }
                      }
                      cald.add(Calendar.MONTH, 1);
                      /* 月によって日にちがないときのための処理 */
                      while (year_day > cald
                        .getActualMaximum(Calendar.DAY_OF_MONTH)) {
                        cald.add(Calendar.MONTH, 1);
                        cald.set(Calendar.DAY_OF_MONTH, year_day);
                        if (!ddate.after(_end_date)) {
                          break;
                        }
                      }
                      ddate = cald.getTime();
                    }
                  } else {
                    continue;
                  }
                }
              }
            }
            if (existFacility) {
              break;
            }
          }
          if (existFacility) {
            return existFacility;
          }
        }
      }
    }
    return result;
  }

  public static boolean matchDay(Calendar cal, String repeat_ptn) { // カレンダーの日付が繰り返しパターンと重なっているか判定
    if (repeat_ptn == null || "".equals(repeat_ptn)) {
      return false;
    }
    if (repeat_ptn.startsWith("M")) {
      int month_day;
      // 月末処理
      if (repeat_ptn.substring(1, 3).equals("XX")) {
        month_day = cal.getActualMaximum(Calendar.DATE);
      } else {
        month_day = Integer.parseInt(repeat_ptn.substring(1, 3));
      }
      int ptn_day = cal.get(Calendar.DAY_OF_MONTH);
      return (month_day == ptn_day);
    }
    if (repeat_ptn.startsWith("Y")) {
      int year_month = Integer.parseInt(repeat_ptn.substring(1, 3));
      int year_day = Integer.parseInt(repeat_ptn.substring(3, 5));
      int ptn_month = cal.get(Calendar.MONTH);
      int ptn_day = cal.get(Calendar.DAY_OF_MONTH);
      return (year_day == ptn_day && (year_month - 1) == ptn_month);
    } else if (repeat_ptn.startsWith("W")) {
      int dow = cal.get(Calendar.DAY_OF_WEEK);
      int dowim = cal.get(Calendar.DAY_OF_WEEK_IN_MONTH);
      if (repeat_ptn.length() == 9
        || dowim == Character.getNumericValue(repeat_ptn.charAt(8))) {
        if (dow == Calendar.SUNDAY) {
          return repeat_ptn.matches("W1........?");
        }
        if (dow == Calendar.MONDAY) {
          return repeat_ptn.matches("W.1.......?");
        }
        if (dow == Calendar.TUESDAY) {
          return repeat_ptn.matches("W..1......?");
        }
        if (dow == Calendar.WEDNESDAY) {
          return repeat_ptn.matches("W...1.....?");
        }
        if (dow == Calendar.THURSDAY) {
          return repeat_ptn.matches("W....1....?");
        }
        if (dow == Calendar.FRIDAY) {
          return repeat_ptn.matches("W.....1...?");
        }
        if (dow == Calendar.SATURDAY) {
          return repeat_ptn.matches("W......1..?");
        }
      }
      return false;
    } else {
      return true;
    }
  }

  /**
   * 他人のスケジュールに対する権限があるかどうかを調べます。
   *
   * @param rundata
   * @param context
   * @return
   */
  public static boolean hasAuthorityForOtherSchedule(RunData rundata, int type) {
    boolean acl_delete_other = false;
    ALAccessControlFactoryService aclservice =
      (ALAccessControlFactoryService) ((TurbineServices) TurbineServices
        .getInstance()).getService(ALAccessControlFactoryService.SERVICE_NAME);
    ALAccessControlHandler aclhandler = aclservice.getAccessControlHandler();
    if (aclhandler.hasAuthority(
      ALEipUtils.getUserId(rundata),
      ALAccessControlConstants.POERTLET_FEATURE_SCHEDULE_OTHER,
      type)) {
      acl_delete_other = true;
    }

    return acl_delete_other;
  }

  public static List<VEipTScheduleList> getScheduleList(int userId,
      List<Integer> users, List<Integer> facilities, String keyword, int page,
      int limit, boolean auth) {
    return getScheduleList(
      userId,
      null,
      null,
      users,
      facilities,
      keyword,
      page,
      limit,
      true,
      true,
      auth);
  }

  public static List<VEipTScheduleList> getScheduleList(int userId,
      Date viewStart, Date viewEnd, List<Integer> users,
      List<Integer> facilities, boolean isDetail) {
    return getScheduleList(
      userId,
      viewStart,
      viewEnd,
      users,
      facilities,
      null,
      -1,
      -1,
      false,
      isDetail,
      true);
  }

  public static List<VEipTScheduleList> getScheduleList(int userId,
      Date viewStart, Date viewEnd, List<Integer> users,
      List<Integer> facilities) {
    return getScheduleList(
      userId,
      viewStart,
      viewEnd,
      users,
      facilities,
      null,
      -1,
      -1,
      false,
      false,
      true);
  }

  protected static List<VEipTScheduleList> getScheduleList(int userId,
      Date viewStart, Date viewEnd, List<Integer> users,
      List<Integer> facilities, String keyword, int page, int limit,
      boolean isSearch, boolean isDetail, boolean auth) {

    boolean isMySQL = Database.isJdbcMySQL();

    StringBuilder select = new StringBuilder();

    select.append("select");
    if (!isSearch) {
      select.append(" t3.id, ");
      select.append(" t3.user_id, ");
      select.append(" t3.status, ");
      select.append(" t3.type, ");
      select.append(" t3.common_category_id, ");
    }
    select.append(" t4.schedule_id,");
    select.append(" t4.owner_id,");
    select.append(" t4.parent_id,");
    select.append(" t4.name,");
    select.append(" t4.place,");
    select.append(" t4.start_date,");
    select.append(" t4.end_date,");
    select.append(" t4.update_date,");
    select.append(" t4.public_flag,");
    select.append(" t4.repeat_pattern,");
    select.append(" t4.create_user_id,");
    select.append(" t4.edit_flag,");
    if (isDetail) {
      select.append(" t4.note,");
    }
    select
      .append(" (SELECT COUNT(*) FROM eip_t_schedule_map t0 WHERE (t0.schedule_id = t4.schedule_id) AND (t0.user_id = #bind($user_id)) AND (t0.type = 'U')) AS is_member,");
    select
      .append(" (SELECT COUNT(*) FROM eip_t_schedule_map t1 WHERE (t1.schedule_id = t4.schedule_id) AND (t1.status <> 'R') AND (t1.type = 'F')) AS f_count,");
    select
      .append(" (SELECT COUNT(*) FROM eip_t_schedule_map t2 WHERE (t2.schedule_id = t4.schedule_id) AND (t2.status <> 'R') AND (t2.type <> 'F')) AS u_count");

    StringBuilder count = new StringBuilder();
    count.append("select count(t4.schedule_id) AS c ");

    boolean hasKeyword = false;
    StringBuilder body = new StringBuilder();
    if (isSearch) {
      body.append(" FROM eip_t_schedule t4 ");
      body.append(" WHERE ");
      body.append(" EXISTS ( ");
      body
        .append(" SELECT NULL FROM eip_t_schedule_map t3 WHERE t3.schedule_id = t4.schedule_id AND t3.status NOT IN('D', 'R') ");
      if (!auth) {
        body.append(" AND t3.user_id = #bind($user_id) AND t3.type = 'U' ");
      }
      if ((users != null && users.size() > 0)
        || (facilities != null && facilities.size() > 0)) {
        body.append(" AND (t3.type, t3.user_id) IN ( ");
        boolean isFirst = true;
        if (users != null && users.size() > 0) {
          for (Integer num : users) {
            if (!isFirst) {
              body.append(",");
            }
            body.append(" ('U', ");
            body.append(num.intValue());
            body.append(" ) ");
            isFirst = false;
          }
        }
        if (facilities != null && facilities.size() > 0) {
          for (Integer num : facilities) {
            if (!isFirst) {
              body.append(",");
            }
            body.append(" ('F', ");
            body.append(num.intValue());
            body.append(" ) ");
            isFirst = false;
          }
        }
        body.append(" ) ");
      }
      if (keyword != null && keyword.length() > 0) {
        hasKeyword = true;
        body.append(" AND (");
        body
          .append(" t4.name LIKE #bind($keyword) OR t4.note LIKE #bind($keyword) OR t4.place LIKE #bind($keyword) ");
        body.append(" ) ");

      }
      body
        .append(" AND ( t4.public_flag = 'O' OR  ( t3.type = 'U' AND t3.user_id = #bind($user_id) ) OR  (t4.owner_id = #bind($user_id)) ) ");
      body.append(" ) ");
    } else {
      body.append(" FROM eip_t_schedule_map t3 ");
      if (isMySQL) {
        body.append(" FORCE INDEX (eip_t_schedule_map_schedule_id_index) ");
      }
      body.append(" , eip_t_schedule t4 ");
      body.append(" WHERE ");
      body.append(" t3.schedule_id = t4.schedule_id AND (t3.status <> 'R') ");
      if ((users != null && users.size() > 0)
        || (facilities != null && facilities.size() > 0)) {
        body.append(" AND (t3.type, t3.user_id) IN ( ");
        boolean isFirst = true;
        if (users != null && users.size() > 0) {
          for (Integer num : users) {
            if (!isFirst) {
              body.append(",");
            }
            body.append(" ('U', ");
            body.append(num.intValue());
            body.append(" ) ");
            isFirst = false;
          }
        }
        if (facilities != null && facilities.size() > 0) {
          for (Integer num : facilities) {
            if (!isFirst) {
              body.append(",");
            }
            body.append(" ('F', ");
            body.append(num.intValue());
            body.append(" ) ");
            isFirst = false;
          }
        }
        body.append(" ) ");
      }
      if (viewStart != null && viewEnd != null) {
        body.append(" AND ( ");
        body.append(" ( ");
        body.append(" t4.start_date <= '");
        body
          .append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(viewEnd));
        body.append("' ");
        body.append(" AND t4.end_date >= '");
        body.append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
          .format(viewStart));
        body.append("' ");
        body.append(" ) ");
        body.append(" OR  t4.repeat_pattern NOT IN ('N', 'S') ");
        body.append(" ) ");
      }
    }

    StringBuilder last = new StringBuilder();

    last
      .append(" ORDER BY t4.start_date DESC, t4.end_date DESC, t4.update_date DESC ");
    if (!isSearch) {
      last.append(" , t3.type DESC, t3.user_id ");
    }

    SQLTemplate<VEipTScheduleList> countQuery =
      Database
        .sql(VEipTScheduleList.class, count.toString() + body.toString())
        .param("user_id", Integer.valueOf(userId));
    if (hasKeyword) {
      countQuery.param("keyword", "%" + keyword + "%");
    }

    int countValue = 0;
    if (page > 0 && limit > 0) {
      List<DataRow> fetchCount = countQuery.fetchListAsDataRow();

      for (DataRow row : fetchCount) {
        countValue = ((Long) row.get("c")).intValue();
      }

      int offset = 0;
      if (limit > 0) {
        int num = ((int) (Math.ceil(countValue / (double) limit)));
        if ((num > 0) && (num < page)) {
          page = num;
        }
        offset = limit * (page - 1);
      } else {
        page = 1;
      }

      last.append(" LIMIT ");
      last.append(limit);
      last.append(" OFFSET ");
      last.append(offset);
    }

    SQLTemplate<VEipTScheduleList> query =
      Database.sql(
        VEipTScheduleList.class,
        select.toString() + body.toString() + last.toString()).param(
        "user_id",
        Integer.valueOf(userId));
    if (hasKeyword) {
      query.param("keyword", "%" + keyword + "%");
    }

    List<DataRow> fetchList = query.fetchListAsDataRow();

    List<VEipTScheduleList> list = new ArrayList<VEipTScheduleList>();
    for (DataRow row : fetchList) {
      Long is_member = (Long) row.get("is_member");
      Long u_count = (Long) row.get("u_count");
      Long f_count = (Long) row.get("f_count");
      VEipTScheduleList object =
        Database.objectFromRowData(row, VEipTScheduleList.class);
      object.setMember(is_member.intValue() > 0);
      object.setUserCount(u_count.intValue());
      object.setFacilityCount(f_count.intValue());
      list.add(object);

    }

    if (page > 0 && limit > 0) {
      return new ResultList<VEipTScheduleList>(list, page, limit, countValue);
    } else {
      return new ResultList<VEipTScheduleList>(list, -1, -1, list.size());
    }

  }

  public static void createShareScheduleActivity(EipTSchedule schedule,
      String loginName, List<String> recipients, String mode, int userid) {
    try {
      if (recipients != null && recipients.size() > 0) {
        ALActivity RecentActivity =
          ALActivity
            .getRecentActivity("Schedule", schedule.getScheduleId(), 1f);
        boolean isDeletePrev =
          RecentActivity != null && RecentActivity.isReplace(loginName);
        String message = "";
        if ("new".equals(mode)) {
          message = ALLocalizationUtils.getl10n("SCHEDULE_ADD_A_SCHEDULE");
        } else if ("edit".equals(mode)) {
          message = ALLocalizationUtils.getl10n("SCHEDULE_EDIT_A_SCHEDULE");
        } else if ("delete".equals(mode)) {
          message = ALLocalizationUtils.getl10n("SCHEDULE_DELETE_A_SCHEDULE");
        } else {
          throw new IllegalAccessException();
        }

        String title =
          new StringBuilder(ALLocalizationUtils
            .getl10n("SCHEDULE_SCHEDULE_BRACKET"))
            .append(schedule.getName())
            .append(message)
            .toString();
        String portletParams =
          new StringBuilder("?template=ScheduleDetailScreen")
            .append("&entityid=")
            .append(schedule.getScheduleId())
            .append("&view_date=")
            .append(
              ALDateUtil.format(schedule.getStartDate(), "yyyy-MM-dd-00-00"))
            .toString();
        ALActivityService.create(new ALActivityPutRequest()
          .withAppId("Schedule")
          .withUserId(userid)
          .withLoginName(loginName)
          .withPortletParams(portletParams)
          .withRecipients(recipients)
          .withTitle(title)
          .withPriority(1f)
          .withExternalId(String.valueOf(schedule.getScheduleId())));

        if (isDeletePrev) {
          RecentActivity.delete();
        }
      }
    } catch (IllegalAccessException e) {

    }
  }

  public static void createNewScheduleActivity(EipTSchedule schedule,
      String loginName, String mode, int userid) {
    try {
      ALActivity RecentActivity =
        ALActivity.getRecentActivity("Schedule", schedule.getScheduleId(), 0f);
      boolean isDeletePrev =
        RecentActivity != null && RecentActivity.isReplace(loginName);

      String message = "";
      if ("new".equals(mode)) {
        message = ALLocalizationUtils.getl10n("SCHEDULE_ADD_A_SCHEDULE");
      } else if ("edit".equals(mode)) {
        message = ALLocalizationUtils.getl10n("SCHEDULE_EDIT_A_SCHEDULE");
      } else if ("delete".equals(mode)) {
        message = ALLocalizationUtils.getl10n("SCHEDULE_DELETE_A_SCHEDULE");
      } else {
        throw new IllegalArgumentException();
      }
      String title =
        new StringBuilder(ALLocalizationUtils
          .getl10n("SCHEDULE_SCHEDULE_BRACKET"))
          .append(schedule.getName())
          .append(message)
          .toString();
      String portletParams =
        new StringBuilder("?template=ScheduleDetailScreen")
          .append("&entityid=")
          .append(schedule.getScheduleId())
          .append("&view_date=")
          .append(
            ALDateUtil.format(schedule.getStartDate(), "yyyy-MM-dd-00-00"))
          .toString();
      ALActivityService.create(new ALActivityPutRequest()
        .withAppId("Schedule")
        .withUserId(userid)
        .withLoginName(loginName)
        .withPortletParams(portletParams)
        .withTitle(title)
        .withPriority(0f)
        .withExternalId(String.valueOf(schedule.getScheduleId())));

      if (isDeletePrev) {
        RecentActivity.delete();
      }
    } catch (IllegalArgumentException e) {

    }
  }

  public static String getTargetKeyword(RunData rundata, Context context) {
    String target_keyword = null;
    String keywordParam = rundata.getParameters().getString(TARGET_KEYWORD);
    target_keyword = ALEipUtils.getTemp(rundata, context, TARGET_KEYWORD);

    if (keywordParam == null && (target_keyword == null)) {
      ALEipUtils.setTemp(rundata, context, TARGET_KEYWORD, "");
      target_keyword = "";
    } else if (keywordParam != null) {
      ALEipUtils.setTemp(rundata, context, TARGET_KEYWORD, keywordParam.trim());
      target_keyword = keywordParam;
    }
    return target_keyword;
  }

  /**
   *
   * @param rundata
   * @return
   */
  public static List<UserFacilityLiteBean> getUserFacilityLiteBeansFromGroup(
      RunData rundata, String groupname) {

    List<UserLiteBean> tmp_u_list =
      UserUtils.getUserLiteBeansFromGroup(rundata, groupname, true);
    int t_size = tmp_u_list.size();

    List<UserFacilityLiteBean> list = new ArrayList<UserFacilityLiteBean>();
    UserLiteBean t_user;
    UserFacilityLiteBean user;
    for (int i = 0; i < t_size; i++) {
      t_user = tmp_u_list.get(i);
      user = new UserFacilityLiteBean();
      user.initField();
      user.setUserFacilityId(Integer.parseInt(t_user.getUserId()));
      user.setName(t_user.getUserId());
      user.setAliasName(t_user.getAliasName());
      user.setUserFacilityType("U");
      if (user.getUserFacilityId().equals(
        String.valueOf(ALEipUtils.getUserId(rundata)))) {
        list.add(user);
      }
    }

    List<EipMFacility> aList =
      Database.query(EipMFacility.class).orderAscending(
        EipMFacility.SORT_PROPERTY).fetchList();
    for (EipMFacility record : aList) {
      user = new UserFacilityLiteBean();
      user.initField();
      user.setUserFacilityId(record.getFacilityId().intValue());
      user.setName("f" + user.getUserFacilityId());
      user.setAliasName(record.getFacilityName());
      user.setUserFacilityType("F");
      list.add(user);
    }

    return list;
  }

  /**
   * 入力フィールド値が数字かどうかを判定します。
   *
   * @return
   */
  public static boolean isNumberValue(String string) {
    try {
      Integer.valueOf(string);
    } catch (NumberFormatException ex) {
      return false;
    }
    return true;
  }

  /**
   * 添付ファイルを取得します。
   *
   * @param uid
   * @return
   */
  public static ArrayList<FileuploadLiteBean> getFileuploadList(RunData rundata) {
    String[] fileids =
      rundata
        .getParameters()
        .getStrings(FileuploadUtils.KEY_FILEUPLOAD_ID_LIST);
    if (fileids == null) {
      return null;
    }

    ArrayList<String> hadfileids = new ArrayList<String>();
    ArrayList<String> newfileids = new ArrayList<String>();

    for (int j = 0; j < fileids.length; j++) {
      if (fileids[j].trim().startsWith("s")) {
        hadfileids.add(fileids[j].trim().substring(1));
      } else {
        newfileids.add(fileids[j].trim());
      }
    }

    ArrayList<FileuploadLiteBean> fileNameList =
      new ArrayList<FileuploadLiteBean>();
    FileuploadLiteBean filebean = null;

    // 新規にアップロードされたファイルの処理
    if (newfileids.size() > 0) {
      String folderName =
        rundata.getParameters().getString(
          FileuploadUtils.KEY_FILEUPLOAD_FODLER_NAME);
      if (folderName == null || folderName.equals("")) {
        return null;
      }

      for (String newfileid : newfileids) {
        if ("".equals(newfileid)) {
          continue;
        }
        int fileid = 0;
        try {
          fileid = Integer.parseInt(newfileid);
        } catch (Exception e) {
          continue;
        }

        if (fileid == 0) {
          filebean = new FileuploadLiteBean();
          filebean.initField();
          filebean.setFolderName("photo");
          filebean.setFileName("以前の写真ファイル");
          fileNameList.add(filebean);
        } else {
          BufferedReader reader = null;
          try {
            reader =
              new BufferedReader(new InputStreamReader(ALStorageService
                .getFile(
                  FileuploadUtils.FOLDER_TMP_FOR_ATTACHMENT_FILES,
                  ALEipUtils.getUserId(rundata)
                    + ALStorageService.separator()
                    + folderName,
                  fileid + FileuploadUtils.EXT_FILENAME), FILE_ENCODING));
            String line = reader.readLine();
            if (line == null || line.length() <= 0) {
              continue;
            }
            filebean = new FileuploadLiteBean();
            filebean.initField();
            filebean.setFolderName(newfileid);
            filebean.setFileId(fileid);
            filebean.setFileName(line);
            fileNameList.add(filebean);
          } catch (Exception e) {
            logger.error("schedule", e);
          } finally {
            try {
              reader.close();
            } catch (Exception e) {
              logger.error("schedule", e);
            }
          }
        }
      }
    }

    // すでにあるファイルの処理
    if (hadfileids.size() > 0) {
      ArrayList<Integer> hadfileidsValue = new ArrayList<Integer>();
      for (String hadfileid : hadfileids) {
        int fileid = 0;
        try {
          fileid = Integer.parseInt(hadfileid);
          hadfileidsValue.add(fileid);
        } catch (Exception e) {
          continue;
        }
      }
      try {
        SelectQuery<EipTScheduleFile> reqquery =
          Database.query(EipTScheduleFile.class);
        Expression reqexp1 =
          ExpressionFactory.inDbExp(
            EipTScheduleFile.FILE_ID_PK_COLUMN,
            hadfileidsValue);
        reqquery.setQualifier(reqexp1);
        List<EipTScheduleFile> requests = reqquery.fetchList();

        for (EipTScheduleFile file : requests) {
          int fileid = file.getFileId();
          filebean = new FileuploadBean();
          filebean.initField();
          filebean.setFileId(fileid);
          filebean.setFileName(file.getFileName());
          filebean.setFlagNewFile(false);
          fileNameList.add(filebean);
        }
      } catch (Exception ex) {
        logger.error("[BlogUtils] Exception.", ex);
      }
    }
    return fileNameList;
  }

  public static boolean insertFileDataDelegate(RunData rundata,
      Context context, EipTSchedule schedule,
      List<FileuploadLiteBean> fileuploadList, String folderName,
      List<String> msgList) {
    if (fileuploadList == null || fileuploadList.size() <= 0) {
      fileuploadList = new ArrayList<FileuploadLiteBean>();
    }

    int uid = ALEipUtils.getUserId(rundata);
    String orgId = Database.getDomainName();

    List<Integer> hadfileids = new ArrayList<Integer>();
    for (FileuploadLiteBean file : fileuploadList) {
      if (!file.isNewFile()) {
        hadfileids.add(file.getFileId());
      }
    }

    SelectQuery<EipTScheduleFile> dbquery =
      Database.query(EipTScheduleFile.class);
    dbquery.andQualifier(ExpressionFactory.matchDbExp(
      EipTScheduleFile.EIP_TSCHEDULE_PROPERTY,
      schedule.getScheduleId()));
    List<EipTScheduleFile> existsFiles = dbquery.fetchList();
    List<EipTScheduleFile> delFiles = new ArrayList<EipTScheduleFile>();
    for (EipTScheduleFile file : existsFiles) {
      if (!hadfileids.contains(file.getFileId())) {
        delFiles.add(file);
      }
    }

    // ローカルファイルに保存されているファイルを削除する．
    if (delFiles.size() > 0) {
      try {
        ALDeleteFileUtil.deleteFiles(
          ScheduleUtils.FOLDER_FILEDIR_SCHEDULE,
          ScheduleUtils.CATEGORY_KEY,
          delFiles);
      } catch (ALFileNotRemovedException e) {
        Database.rollback();
        logger.error("schedule", e);
        return false;
      }
    }

    // ファイル追加処理
    try {
      for (FileuploadLiteBean filebean : fileuploadList) {
        if (!filebean.isNewFile()) {
          continue;
        }

        // サムネイル処理
        String[] acceptExts = ImageIO.getWriterFormatNames();
        ShrinkImageSet shrinkImageSet =
          FileuploadUtils.getBytesShrinkFilebean(
            orgId,
            folderName,
            uid,
            filebean,
            acceptExts,
            FileuploadUtils.DEF_THUMBNAIL_WIDTH,
            FileuploadUtils.DEF_THUMBNAIL_HEIGHT,
            msgList,
            true);

        String filename = "0_" + String.valueOf(System.nanoTime());

        // 新規オブジェクトモデル
        EipTScheduleFile file = Database.create(EipTScheduleFile.class);
        // 所有者
        file.setOwnerId(Integer.valueOf(uid));
        // トピックID
        file.setEipTSchedule(schedule);
        // ファイル名
        file.setFileName(filebean.getFileName());
        // ファイルパス
        file.setFilePath(ScheduleUtils.getRelativePath(filename));
        // サムネイル画像
        if (shrinkImageSet != null && shrinkImageSet.getShrinkImage() != null) {
          file.setFileThumbnail(shrinkImageSet.getShrinkImage());
        }
        // 作成日
        file.setCreateDate(Calendar.getInstance().getTime());
        // 更新日
        file.setUpdateDate(Calendar.getInstance().getTime());

        if (shrinkImageSet != null && shrinkImageSet.getFixImage() != null) {
          // ファイルの作成
          ALStorageService.createNewFile(new ByteArrayInputStream(
            shrinkImageSet.getFixImage()), FOLDER_FILEDIR_SCHEDULE
            + ALStorageService.separator()
            + Database.getDomainName()
            + ALStorageService.separator()
            + CATEGORY_KEY
            + ALStorageService.separator()
            + uid
            + ALStorageService.separator()
            + filename);
        } else {
          // ファイルの移動
          ALStorageService.copyTmpFile(uid, folderName, String.valueOf(filebean
            .getFileId()), FOLDER_FILEDIR_SCHEDULE, CATEGORY_KEY
            + ALStorageService.separator()
            + uid, filename);
        }
      }

      ALStorageService.deleteTmpFolder(uid, folderName);

    } catch (Exception e) {
      Database.rollback();
      logger.error("schedule", e);
      return false;
    }
    return true;
  }

  /**
   * ユーザ毎のルート保存先（絶対パス）を取得します。
   *
   * @param uid
   * @return
   */
  public static String getSaveDirPath(int uid) {
    return ALStorageService.getDocumentPath(
      FOLDER_FILEDIR_SCHEDULE,
      CATEGORY_KEY + ALStorageService.separator() + uid);
  }

  /**
   * ユーザ毎の保存先（相対パス）を取得します。
   *
   * @param uid
   * @return
   */
  public static String getRelativePath(String fileName) {
    return new StringBuffer().append("/").append(fileName).toString();
  }

  @Deprecated
  public static void shiftWhatsNewReadFlag(RunData rundata, int entityid) {
    int uid = ALEipUtils.getUserId(rundata);
    boolean isPublic = false;

    SelectQuery<EipTSchedule> query = Database.query(EipTSchedule.class);
    Expression exp =
      ExpressionFactory.matchExp(EipTSchedule.PARENT_ID_PROPERTY, entityid);
    query.setQualifier(exp);
    query.select(EipTSchedule.SCHEDULE_ID_PK_COLUMN);
    query.distinct(true);

    List<EipTSchedule> schedules = query.fetchList();

    query = Database.query(EipTSchedule.class);
    exp =
      ExpressionFactory
        .matchDbExp(EipTSchedule.SCHEDULE_ID_PK_COLUMN, entityid);
    query.setQualifier(exp);

    List<EipTSchedule> schedule = query.fetchList();
    if (schedule != null
      && (((_EipTSchedule) (schedule.get(0)).getEipTScheduleMaps())
        .getPublicFlag().equals("T"))) {
      isPublic = true;
    }

    if (schedules != null) {

      int size = schedules.size();
      Integer _id = null;

      if (isPublic) {
        for (int i = 0; i < size; i++) {
          EipTSchedule record = schedules.get(i);
          _id = record.getScheduleId();
          WhatsNewUtils.shiftWhatsNewReadFlagPublic(
            WhatsNewUtils.WHATS_NEW_TYPE_SCHEDULE,
            _id.intValue(),
            uid);
        }
      } else {
        for (int i = 0; i < size; i++) {
          EipTSchedule record = schedules.get(i);
          _id = record.getScheduleId();
          WhatsNewUtils.shiftWhatsNewReadFlag(
            WhatsNewUtils.WHATS_NEW_TYPE_SCHEDULE,
            _id.intValue(),
            uid);
        }
      }
    }
    if (isPublic) {
      WhatsNewUtils.shiftWhatsNewReadFlagPublic(
        WhatsNewUtils.WHATS_NEW_TYPE_SCHEDULE,
        entityid,
        uid);
    } else {
      WhatsNewUtils.shiftWhatsNewReadFlag(
        WhatsNewUtils.WHATS_NEW_TYPE_SCHEDULE,
        entityid,
        uid);
    }
  }

  /*
   * リマインダー送信が有効な共有メンバーを取得します。
   * 
   * @param schedule
   * 
   * @param viewDate
   * 
   * @return
   */
  public static List<ALEipUser> getUsersForReminder(EipTSchedule schedule,
      ALDateTimeField viewDate) {
    List<ALEipUser> memberList = new ArrayList<ALEipUser>();
    ArrayList<Integer> arrayList = new ArrayList<Integer>();
    try {

      if (!isView(viewDate, schedule.getRepeatPattern(), schedule
        .getStartDate(), schedule.getEndDate())) {
        return null;
      }
      // 該当するユーザーが送信除外対象になっていないかチェック
      List<ALEipUser> users = getEffectiveUsers(schedule);
      if (users == null || users.size() == 0) {
        return null;
      }

      // 選択した予定に対するダミースケジュールを検索
      SelectQuery<EipTSchedule> schedulequery =
        Database.query(EipTSchedule.class);
      Expression exp1 =
        ExpressionFactory.matchExp(EipTSchedule.PARENT_ID_PROPERTY, schedule
          .getScheduleId());
      Expression exp2 =
        ExpressionFactory.matchExp(EipTSchedule.START_DATE_PROPERTY, viewDate
          .getValue());
      schedulequery.setQualifier(exp1);
      schedulequery.andQualifier(exp2);
      List<EipTSchedule> dummyScheduleList = schedulequery.fetchList();

      // ダミースケジュールに登録されているマップを検索
      if (dummyScheduleList != null && dummyScheduleList.size() > 0) {
        SelectQuery<EipTScheduleMap> mapquery =
          Database.query(EipTScheduleMap.class);
        Expression mapexp1 =
          ExpressionFactory.inExp(
            EipTScheduleMap.SCHEDULE_ID_PROPERTY,
            dummyScheduleList);
        mapquery.setQualifier(mapexp1);
        // 設備は除外する
        Expression mapexp2 =
          ExpressionFactory.matchExp(
            EipTScheduleMap.TYPE_PROPERTY,
            ScheduleUtils.SCHEDULEMAP_TYPE_USER);
        mapquery.andQualifier(mapexp2);
        mapquery.orderAscending(EipTScheduleMap.SCHEDULE_ID_PROPERTY);

        List<EipTScheduleMap> list = mapquery.fetchList();
        for (EipTScheduleMap map : list) {
          arrayList.add(map.getUserId());
        }
      }

      for (ALEipUser member : users) {
        // アクセス権限チェック
        if (hasAuthorityForScheduleDetail(schedule, member
          .getUserId()
          .getValueWithInt())) {
          // ダミースケジュールに登録されているユーザーを除外
          if (!arrayList.contains(member.getUserId().getValueWithInt())) {
            memberList.add(member);
          }
        }
      }
    } catch (Exception ex) {
      return null;
    }
    return memberList;
  }

  public static void setupReminderJob(String orgId, String userId,
      EipTSchedule schedule, int notifyTiming, boolean isMail, boolean isMessage) {
    String ptn = schedule.getRepeatPattern();
    int count = 0;
    boolean isRepeat = true;
    boolean isSpan = false;
    boolean isLimit = false;
    // 毎日
    if (ptn.charAt(0) == 'D') {
      count = 1;
      // 毎週
    } else if (ptn.charAt(0) == 'W') {
      count = 8;
      // 毎月
    } else if (ptn.charAt(0) == 'M') {
      count = 3;
      // 毎年
    } else if (ptn.charAt(0) == 'Y') {
      count = 5;
      // 期間
    } else if (ptn.charAt(0) == 'S') {// 期間はS
      isRepeat = false;
      isSpan = true;
    } else {
      isRepeat = false;
    }

    if (isRepeat) {
      if (ptn.charAt(count) == 'N') {
        // schedule.setLimit(false);
      } else {
        isLimit = true;
      }
    }

    setupReminderJob(
      orgId,
      userId,
      schedule,
      notifyTiming,
      isMail,
      isMessage,
      isRepeat,
      isLimit,
      isSpan);
  }

  /**
   *
   * @param orgId
   * @param userId
   * @param schedule
   * @param notifyTiming
   * @param isMail
   * @param isMessage
   * @param isRepeat
   * @param isLimit
   * @param isSpan
   */
  public static void setupReminderJob(String orgId, String userId,
      EipTSchedule schedule, int notifyTiming, boolean isMail,
      boolean isMessage, boolean isRepeat, boolean isLimit, boolean isSpan) {

    ALReminderItem item = new ALReminderItem();
    item.setOrgId(orgId);
    item.setUserId(userId);
    item.setItemId(schedule.getScheduleId().intValue());
    item.setCategory(ReminderCategory.SCHEDULE);
    item.setNotifyTiming(notifyTiming);
    item.setRepeatPattern(schedule.getRepeatPattern());
    if (isRepeat) {
      ALDateTimeField field =
        ScheduleUtils.getNextDateRepeat(schedule, notifyTiming, isLimit);
      if (field != null) {
        item.setEventStartDate(field.getValue());
        if (isLimit) {
          item.setLimitEndDate(schedule.getEndDate());
        }
      }
    } else {
      // アラーム送信時間チェック
      Calendar today = Calendar.getInstance();
      today.add(Calendar.MINUTE, notifyTiming);
      if (schedule.getStartDate().after(today.getTime())) {
        item.setEventStartDate(schedule.getStartDate());
      }
    }

    if (isMail) {
      item.addNotifyType(ReminderNotifyType.MAIL);
    }
    if (isMessage) {
      item.addNotifyType(ReminderNotifyType.MESSAGE);
    }
    if (!isSpan && item.getEventStartDate() != null) {
      ALReminderService.updateJob(item);
    }

  }

  /**
   * ＠ToDo ScheduleSelectDataのgetResultDataDetailとリファクタリング
   *
   * @param record
   * @param view_date
   * @param loginuserid
   * @param userid
   * @param type
   * @param ignoreViewdate
   * @param ondaySelectData
   * @return
   * @throws ALDBErrorException
   */
  public static ScheduleDetailResultData getResultDataDetail(
      EipTSchedule record, ALDateTimeField view_date, int loginuserid,
      int userid, String type, boolean ignoreViewdate,
      ScheduleDetailOnedaySelectData ondaySelectData) throws ALDBErrorException {
    ScheduleDetailResultData rd = new ScheduleDetailResultData();
    rd.initField();

    Map<Integer, String> statusList = new HashMap<Integer, String>();
    List<FacilityResultData> facilities = new ArrayList<FacilityResultData>();
    try {

      // 選択した予定に対するダミースケジュールを検索
      SelectQuery<EipTSchedule> schedulequery =
        Database.query(EipTSchedule.class);
      Expression exp1 =
        ExpressionFactory.matchExp(EipTSchedule.PARENT_ID_PROPERTY, record
          .getScheduleId());
      Expression exp2 =
        ExpressionFactory.matchExp(EipTSchedule.START_DATE_PROPERTY, view_date
          .getValue());
      schedulequery.setQualifier(exp1);
      schedulequery.andQualifier(exp2);
      List<Integer> scheduleList = new ArrayList<Integer>();
      List<EipTSchedule> dummyScheduleList = schedulequery.fetchList();

      scheduleList.add(record.getScheduleId());
      for (EipTSchedule dummy : dummyScheduleList) {
        scheduleList.add(dummy.getScheduleId());
      }

      // 元のスケジュール及びダミースケジュールに登録されているマップを検索
      SelectQuery<EipTScheduleMap> mapquery =
        Database.query(EipTScheduleMap.class);
      Expression mapexp1 =
        ExpressionFactory.inExp(
          EipTScheduleMap.SCHEDULE_ID_PROPERTY,
          scheduleList);
      mapquery.setQualifier(mapexp1);
      mapquery.orderAscending(EipTScheduleMap.SCHEDULE_ID_PROPERTY);

      List<EipTScheduleMap> list = mapquery.fetchList();

      List<Integer> users = new ArrayList<Integer>();
      List<Integer> facilityIds = new ArrayList<Integer>();
      // 表示するユーザーがスケジュールの参加者かどうか
      boolean isMember = false;
      int size = list.size();
      for (int i = 0; i < size; i++) {
        EipTScheduleMap map = list.get(i);
        if (ScheduleUtils.SCHEDULEMAP_TYPE_USER.equals(map.getType())) {
          statusList.put(map.getUserId(), map.getStatus());
          // 表示するユーザーの場合
          if (map.getUserId().intValue() == loginuserid) {
            // 仮スケジュールかどうか
            rd.setTmpreserve("T".equals(map.getStatus()));
            // 確定スケジュールかどうか
            rd.setConfirm("C".equals(map.getStatus()));
            // スケジュールの参加者かどうか
            isMember = !"R".equals(map.getStatus());
          }
          users.add(map.getUserId());

          if (userid == map.getUserId().intValue()) {
            EipTCommonCategory category = map.getEipTCommonCategory();
            if (category == null) {
              rd.setCommonCategoryName(null);
            } else {
              rd.setCommonCategoryName(category.getName());
            }
          }

        } else {
          facilityIds.add(map.getUserId());
        }
      }
      SelectQuery<TurbineUser> query = Database.query(TurbineUser.class);
      Expression exp =
        ExpressionFactory.inDbExp(TurbineUser.USER_ID_PK_COLUMN, users);
      query.setQualifier(exp);
      List<ALEipUser> members = ALEipUtils.getUsersFromSelectQuery(query);
      // members = ALEipUtils.getUsersFromCriteria(rundata, new
      // Criteria().addIn(
      // TurbineUserConstants.USER_ID, users));

      if (facilityIds.size() > 0) {
        SelectQuery<EipMFacility> fquery = Database.query(EipMFacility.class);
        Expression fexp =
          ExpressionFactory.inDbExp(
            EipMFacility.FACILITY_ID_PK_COLUMN,
            facilityIds);
        fquery.setQualifier(fexp);
        facilities = FacilitiesUtils.getFacilitiesFromSelectQuery(fquery);
      }
      // facilities = FacilitiesUtils.getFacilitiesFromCriteria(org_id,
      // new Criteria().addIn(EipMFacilityConstants.FACILITY_ID, facilityIds));

      if (ScheduleUtils.SCHEDULEMAP_TYPE_FACILITY.equals(type)) {
        // 設備
        ALEipUser facilityuser = new ALEipUser();
        facilityuser.initField();
        facilityuser.setUserId(userid);
        facilityuser.setName("");
        facilityuser.setAliasName("", getFacilityName(facilities, userid));
        rd.setUser(facilityuser);
      } else {
        // ユーザー
        rd.setUser(ALEipUtils.getALEipUser(userid));
        // もし表示するユーザーが、ログインユーザーかつ参加していないユーザーの場合（タイムラインの更新情報から、ログインユーザーが作成者でログインユーザーが参加していない予定を開いた場合）には、
        // 表示するユーザーを適当な参加ユーザーで置き換える
        if (userid == loginuserid && !isMember) {
          for (EipTScheduleMap map : list) {
            if (!"R".equals(map.getStatus())) {
              rd.setUser(ALEipUtils.getALEipUser(map.getUserId()));
              break;
            }
          }
        }
      }
      // タイプ
      rd.setType(type);
      // 開始日時
      rd.setStartDate(record.getStartDate());
      // オーナー
      rd.setOwner(record.getOwnerId().intValue() == loginuserid);
      // 終了日時
      rd.setEndDate(record.getEndDate());
      // タイトル
      rd.setName(record.getName());
      // ID
      rd.setScheduleId(record.getScheduleId().intValue());
      // 場所
      rd.setPlace(record.getPlace());
      // 内容
      rd.setNote(record.getNote());
      // 公開するかどうか
      rd.setPublic("O".equals(record.getPublicFlag()));
      // 非表示にするかどうか
      rd.setHidden("P".equals(record.getPublicFlag()));
      // 共有メンバーによる編集／削除フラグ
      rd.setEditFlag("T".equals(record.getEditFlag()));

      // DN -> 毎日 (A = N -> 期限なし A = L -> 期限あり)
      // WnnnnnnnN W01111110 -> 毎週(月～金用)
      // WnnnnnnnmN -> 第m週
      // MnnN M25 -> 毎月25日
      // S -> 期間での指定
      String ptn = record.getRepeatPattern();
      int count = 0;
      boolean is_repeat = true;
      rd.setRepeat(true);
      // 毎日
      if (ptn.charAt(0) == 'D') {
        rd.addText(ALLocalizationUtils.getl10n("SCHEDULE_EVERY_DAY"));
        count = 1;
        // 毎週
      } else if (ptn.charAt(0) == 'W') {
        if (ptn.length() == 9) {
          rd.addText(new StringBuffer()
            .append(ALLocalizationUtils.getl10n("SCHEDULE_EVERY_WEEK_SPACE"))
            .toString());
          count = 8;
        } else {
          switch (ptn.charAt(8)) {
            case '1':
              rd.addText(new StringBuffer()
                .append(ALLocalizationUtils.getl10n("SCHEDULE_1ST_WEEK_SPACE"))
                .toString());
              break;
            case '2':
              rd.addText(new StringBuffer()
                .append(ALLocalizationUtils.getl10n("SCHEDULE_2ND_WEEK_SPACE"))
                .toString());
              break;
            case '3':
              rd.addText(new StringBuffer()
                .append(ALLocalizationUtils.getl10n("SCHEDULE_3RD_WEEK_SPACE"))
                .toString());
              break;
            case '4':
              rd.addText(new StringBuffer()
                .append(ALLocalizationUtils.getl10n("SCHEDULE_4TH_WEEK_SPACE"))
                .toString());
              break;
            case '5':
              rd.addText(new StringBuffer()
                .append(ALLocalizationUtils.getl10n("SCHEDULE_5TH_WEEK_SPACE"))
                .toString());
              break;
            default:
              break;
          }
          count = 9;
        }
        rd
          .addText(new StringBuffer()
            .append(
              ptn.charAt(1) != '0' ? ALLocalizationUtils
                .getl10n("SCHEDULE_SUNDAY") : "")
            .append(
              ptn.charAt(2) != '0' ? ALLocalizationUtils
                .getl10n("SCHEDULE_MONDAY") : "")
            .append(
              ptn.charAt(3) != '0' ? ALLocalizationUtils
                .getl10n("SCHEDULE_TUSEDAY") : "")
            .append(
              ptn.charAt(4) != '0' ? ALLocalizationUtils
                .getl10n("SCHEDULE_WEDNESDAY") : "")
            .append(
              ptn.charAt(5) != '0' ? ALLocalizationUtils
                .getl10n("SCHEDULE_THURSDAY") : "")
            .append(
              ptn.charAt(6) != '0' ? ALLocalizationUtils
                .getl10n("SCHEDULE_FRIDAY") : "")
            .append(
              ptn.charAt(7) != '0' ? ALLocalizationUtils
                .getl10n("SCHEDULE_SATURDAY") : "")
            .append(ALLocalizationUtils.getl10n("SCHEDULE_A_DAY_OF_THE_WEEK"))
            .toString());
        // 毎月
      } else if (ptn.charAt(0) == 'M') {
        if (ptn.substring(1, 3).equals("XX")) {
          rd.addText(new StringBuffer().append(
            ALLocalizationUtils.getl10n("SCHEDULE_EVERY_MONTH_SPACE")).append(
            ALLocalizationUtils.getl10n("SCHEDULE_END_OF_MONTH")).append(
            ALLocalizationUtils.getl10n("SCHEDULE_DAY")).toString());
        } else {
          rd.addText(new StringBuffer().append(
            ALLocalizationUtils.getl10n("SCHEDULE_EVERY_MONTH_SPACE")).append(
            Integer.parseInt(ptn.substring(1, 3))).append(
            ALLocalizationUtils.getl10n("SCHEDULE_DAY")).toString());
        }
        count = 3;
        // 毎年
      } else if (ptn.charAt(0) == 'Y') {
        rd.addText(new StringBuffer().append(
          ALLocalizationUtils.getl10n("SCHEDULE_EVERY_YEAR_SPACE")).append(
          Integer.parseInt(ptn.substring(1, 3))).append(
          ALLocalizationUtils.getl10n("SCHEDULE_MONTH")).append(
          Integer.parseInt(ptn.substring(3, 5))).append(
          ALLocalizationUtils.getl10n("SCHEDULE_DAY")).toString());
        count = 5;
        // 期間
      } else if (ptn.charAt(0) == 'S') {
        rd.setSpan(true);
        rd.setRepeat(false);
        is_repeat = false;
      } else {
        rd.setRepeat(false);
        is_repeat = false;

      }
      if (is_repeat) {
        if (ptn.charAt(count) == 'N') {
          rd.setLimit(false);
        } else {
          rd.setLimit(true);
          // 期限
          rd.addText(new StringBuffer().append(" （").append(
            rd.getStartDate().getYear()).append(
            ALLocalizationUtils.getl10n("SCHEDULE_YEAR")).append(
            rd.getStartDate().getMonth()).append(
            ALLocalizationUtils.getl10n("SCHEDULE_MONTH")).append(
            rd.getStartDate().getDay()).append(
            ALLocalizationUtils.getl10n("SCHEDULE_UNTIL_DAY")).append(
            rd.getEndDate().getYear()).append(
            ALLocalizationUtils.getl10n("SCHEDULE_YEAR")).append(
            rd.getEndDate().getMonth()).append(
            ALLocalizationUtils.getl10n("SCHEDULE_MONTH")).append(
            rd.getEndDate().getDay()).append(
            ALLocalizationUtils.getl10n("SCHEDULE_FROM_DAY")).toString());
        }
      }
      // 登録者
      rd.setCreateUser(ALEipUtils.getALEipUser(record
        .getCreateUserId()
        .intValue()));
      // 更新者
      rd.setUpdateUser(ALEipUtils.getALEipUser(record
        .getUpdateUserId()
        .intValue()));
      // 作成日
      rd.setCreateDate(record.getCreateDate());
      // 更新日時
      rd.setUpdateDate(record.getUpdateDate());
      // ログインユーザーID
      if (ignoreViewdate
        || ScheduleUtils.SCHEDULEMAP_TYPE_FACILITY.equals(type)) {
        rd.setLoginuser(false);
        for (ALEipUser member : members) {
          ALNumberField memberId = member.getUserId();
          if (loginuserid == memberId.getValue()) {
            rd.setLoginuser(true);
            break;
          }
        }
      } else {
        rd.setLoginuser(loginuserid == userid);
      }
      // Calendar cal = Calendar.getInstance();
      // cal.setTime(record.getStartDate());
      // cal.set(Calendar.HOUR, 0);
      // cal.set(Calendar.MINUTE, 0);
      // view_date.setValue(cal.getTime());
      // メールフラグ
      rd.setMailFlag(record.getMailFlag());

      ScheduleOnedayContainer con =
        ondaySelectData.getScheduleOnedayContainer();
      List<ScheduleOnedayResultData> selectList = con.getDuplicateSchedule();
      selectList.addAll(con.getSchedule());
      for (ScheduleOnedayResultData onedayrd : selectList) {
        if (rd.getScheduleId().getValue() == onedayrd
          .getScheduleId()
          .getValue()) {
          rd.setDuplicate(onedayrd.isDuplicate());
          break;
        }
      }

    } catch (RuntimeException e) {
      logger.error("schedule", e);

      return null;
    } catch (Exception e) {
      logger.error("schedule", e);

      return null;
    }

    // 過去のスケジュールに対してはアラームの設定状況を表示しない
    rd.setLastStarted(isLastStarted(rd.getStartDate().getValue(), rd
      .getEndDate()
      .getValue(), rd.isSpan(), rd.isRepeat(), rd.isLimit()));

    return rd;
  }

  /**
   * 開始済判定
   *
   * @param startDate
   * @param endDate
   * @param isSpan
   * @param isRepeat
   * @return
   */
  public static boolean isLastStarted(Date startDate, Date endDate,
      boolean isSpan, boolean isRepeat, boolean isLimit) {
    boolean isStarted = false;
    Calendar today = Calendar.getInstance();
    Calendar cal = Calendar.getInstance();
    cal.setTime(startDate);
    if (!isSpan && isRepeat && !isLimit) {
      // 期間指定なしの繰り返しスケジュール
      isStarted = false;
    } else {
      if (!isSpan && isRepeat && isLimit) {
        // 期間指定の繰り返しスケジュール
        Calendar cal2 = Calendar.getInstance();
        cal2.setTime(endDate);
        cal.set(Calendar.YEAR, cal2.get(Calendar.YEAR));
        cal.set(Calendar.MONTH, cal2.get(Calendar.MONTH));
        cal.set(Calendar.DATE, cal2.get(Calendar.DATE));
      }
      if (cal.getTime().before(today.getTime())) {
        isStarted = true;
      }
    }
    return isStarted;
  }

  public static String getFacilityName(List<FacilityResultData> list, int id) {
    FacilityResultData rd = null;
    int size = list.size();
    for (int i = 0; i < size; i++) {
      rd = list.get(i);
      if (rd.getFacilityId().getValue() == id) {
        return rd.getFacilityName().getValue();
      }
    }
    return "";
  }

  /**
   * Scheudle オブジェクトモデルを取得します。
   *
   * @ToDo getEipTScheduleDetailとリファクタリング
   *
   * @param rundata
   * @param context
   * @return
   */
  public static EipTSchedule getEipTScheduleDetailForReminder(int scheduleid) {

    try {

      SelectQuery<EipTSchedule> query = Database.query(EipTSchedule.class);
      query.getQuery().setRefreshingObjects(true);

      // スケジュールID
      Expression exp1 =
        ExpressionFactory.matchDbExp(
          EipTSchedule.SCHEDULE_ID_PK_COLUMN,
          scheduleid);
      query.setQualifier(exp1);

      // ユーザのスケジュール
      Expression exp2 =
        ExpressionFactory.matchExp(EipTSchedule.EIP_TSCHEDULE_MAPS_PROPERTY
          + "."
          + EipTScheduleMap.TYPE_PROPERTY, ScheduleUtils.SCHEDULEMAP_TYPE_USER);
      query.andQualifier(exp2);

      List<EipTSchedule> schedules = query.fetchList();

      // 指定したSchedule IDのレコードが見つからない場合
      if (schedules == null || schedules.size() == 0) {
        throw new ALPageNotFoundException();
      }

      EipTSchedule record = schedules.get(0);
      return record;
    } catch (Exception ex) {
      logger.error("[ScheduleUtils]", ex);
      return null;
    }
  }

  /**
   * スケジュールへのアクセス権限があるかどうかを調べます。
   *
   * @ToDo hasAuthorityForScheduleDetailとリファクタリング
   *
   * @param rundata
   * @param context
   * @return
   * @throws ALDBErrorException
   */
  public static boolean hasAuthorityForScheduleDetail(EipTSchedule record,
      int userId) throws ALDBErrorException {
    try {

      SelectQuery<EipTScheduleMap> mapquery =
        Database.query(EipTScheduleMap.class);
      Expression mapexp1 =
        ExpressionFactory.matchExp(EipTScheduleMap.SCHEDULE_ID_PROPERTY, record
          .getScheduleId());
      mapquery.setQualifier(mapexp1);
      Expression mapexp21 =
        ExpressionFactory.matchExp(EipTScheduleMap.USER_ID_PROPERTY, Integer
          .toString(userId));
      Expression mapexp22 =
        ExpressionFactory.matchExp(EipTScheduleMap.EIP_TSCHEDULE_PROPERTY
          + "."
          + EipTSchedule.CREATE_USER_ID_PROPERTY, Integer.valueOf(userId));
      mapquery.andQualifier(mapexp21.orExp(mapexp22));
      // 設備は除外する
      Expression exp3 =
        ExpressionFactory.matchExp(
          EipTScheduleMap.TYPE_PROPERTY,
          ScheduleUtils.SCHEDULEMAP_TYPE_USER);
      mapquery.andQualifier(exp3);

      List<EipTScheduleMap> schedulemaps = mapquery.fetchList();
      boolean is_member =
        (schedulemaps != null && schedulemaps.size() > 0) ? true : false;

      boolean is_owner = record.getOwnerId().intValue() == userId;
      boolean is_createuser = record.getCreateUserId().intValue() == userId;
      boolean is_public = "O".equals(record.getPublicFlag());

      // アクセス権限がない場合
      if (is_public) {
      } else if (!is_member && (!(is_createuser || is_owner))) {
        return false;
      }
      return true;
    } catch (Exception ex) {
      logger.error("[ScheduleUtils]", ex);
      throw new ALDBErrorException();
    }
  }

  /**
   * 共有メンバーを取得します。
   *
   * @param rundata
   * @param context
   * @param includeLoginUser
   *          ログインユーザーを共有メンバーとして取り扱う場合，true．
   * @return
   */
  public static List<ALEipUser> getEffectiveUsers(EipTSchedule schedule)
      throws ALPageNotFoundException, ALDBErrorException {
    List<ALEipUser> list = new ArrayList<ALEipUser>();

    if (schedule == null) {
      logger.error("[ScheduleUtils] ENTITYID is empty.");
      throw new ALPageNotFoundException();
    }

    try {
      SelectQuery<EipTScheduleMap> mapquery =
        Database.query(EipTScheduleMap.class);

      // スケジュールID
      Expression exp1 =
        ExpressionFactory.matchExp(
          EipTScheduleMap.SCHEDULE_ID_PROPERTY,
          schedule.getScheduleId());
      mapquery.setQualifier(exp1);
      // 削除済は除外する
      Expression exp2 =
        ExpressionFactory.noMatchExp(EipTScheduleMap.STATUS_PROPERTY, "R");
      mapquery.andQualifier(exp2);
      // 設備は除外する
      Expression exp3 =
        ExpressionFactory.matchExp(
          EipTScheduleMap.TYPE_PROPERTY,
          ScheduleUtils.SCHEDULEMAP_TYPE_USER);
      mapquery.andQualifier(exp3);
      List<EipTScheduleMap> schedulemaps = mapquery.fetchList();

      List<Integer> uidlist = new ArrayList<Integer>();
      EipTScheduleMap map = null;
      int mapsize = schedulemaps.size();
      for (int i = 0; i < mapsize; i++) {
        map = schedulemaps.get(i);
        if (!uidlist.contains(map.getUserId())) {
          uidlist.add(map.getUserId());
        }
      }

      SelectQuery<TurbineUser> userquery = Database.query(TurbineUser.class);
      Expression userexp =
        ExpressionFactory.inDbExp(TurbineUser.USER_ID_PK_COLUMN, uidlist);
      userquery.setQualifier(userexp);

      List<TurbineUser> ulist = userquery.fetchList();

      TurbineUser tuser;
      ALEipUser user;
      for (int j = 0; j < ulist.size(); j++) {
        tuser = ulist.get(j);
        user = new ALEipUser();
        user.initField();
        user.setUserId(tuser.getUserId().intValue());
        user.setName(tuser.getLoginName());
        user.setAliasName(tuser.getFirstName(), tuser.getLastName());
        list.add(user);
      }

    } catch (Exception e) {
      logger.error("[ScheduleUtils]", e);
      throw new ALDBErrorException();

    }
    return list;
  }

  public static ALDateTimeField getNextDate(ALDateTimeField date, String ptn,
      Date startDate, Date limitDate, boolean isLimit) {
    if (isView(date, ptn, startDate, limitDate)) {
      return date;
    } else {
      Calendar cal = Calendar.getInstance();
      cal.setTime(date.getValue());
      cal.add(Calendar.DATE, 1);
      if (isLimit && cal.getTime().after(limitDate)) {
        return null;
      }
      ALDateTimeField field = new ALDateTimeField();
      field.setValue(cal.getTime());
      return getNextDate(field, ptn, startDate, limitDate, isLimit);
    }
  }

  public static ALDateTimeField getNextDateRepeat(EipTSchedule schedule,
      int notifyTiming, boolean isLimit) {

    // 次のアラーム日を算出
    Calendar today = Calendar.getInstance();
    Calendar cal = Calendar.getInstance();
    cal.setTime(schedule.getStartDate());
    cal.set(Calendar.YEAR, today.get(Calendar.YEAR));
    cal.set(Calendar.MONTH, today.get(Calendar.MONTH));
    cal.set(Calendar.DATE, today.get(Calendar.DATE));
    // 今日のアラーム送信時間が過ぎている場合は翌日にする
    today.add(Calendar.MINUTE, notifyTiming);
    if (cal.getTime().before(today.getTime())) {
      cal.add(Calendar.DATE, 1);
    }
    ALDateTimeField next = new ALDateTimeField();
    next.setValue(cal.getTime());
    return ScheduleUtils.getNextDate(
      next,
      schedule.getRepeatPattern(),
      schedule.getStartDate(),
      schedule.getEndDate(),
      isLimit);

  }

  /**
   * パソコンへ送信するメールの内容を作成する．
   *
   * @return
   */
  public static String createReminderMsgForPc(EipTSchedule schedule,
      List<ALEipUser> memberList) {
    boolean enableAsp = JetspeedResources.getBoolean("aipo.asp", false);

    String date_detail = "";

    try {

      date_detail = getMsgDate(schedule);
    } catch (Exception e) {
      return "";
    }

    StringWriter out = null;
    try {
      VelocityService service =
        (VelocityService) ((TurbineServices) TurbineServices.getInstance())
          .getService(VelocityService.SERVICE_NAME);
      Context context = service.getContext();

      context.put("titleValue", schedule.getName().toString());
      context.put("dateValue", date_detail);

      if (schedule.getPlace().toString().length() > 0) {
        context.put("placeValue", schedule.getPlace().toString());
      }

      if (schedule.getNote().toString().length() > 0) {
        context.put("noteValue", schedule.getNote().toString());
      }

      if (memberList != null) {
        int size = memberList.size();
        int i;
        StringBuffer body = new StringBuffer("");
        for (i = 0; i < size; i++) {
          if (i != 0) {
            body.append(", ");
          }
          ALEipUser member = memberList.get(i);
          body.append(member.getAliasName());
        }
        context.put("menbersList", body.toString());
      }

      context.put("Alias", ALOrgUtilsService.getAlias());

      if (enableAsp) {
        context.put("globalUrl1", ALMailUtils.getGlobalurl());
      } else {
        context.put("globalurl2", ALMailUtils.getGlobalurl());
        context.put("globalUrl3", ALMailUtils.getLocalurl());
      }

      out = new StringWriter();
      service.handleRequest(context, "mail/scheduleReminder.vm", out);
      out.flush();
      return out.toString();
    } catch (IllegalArgumentException e) {

    } catch (Exception e) {
      String message = e.getMessage();
      logger.warn(message, e);
      e.printStackTrace();
    } finally {
      if (out != null) {
        try {
          out.close();
        } catch (IOException e) {
          // ignore
        }
      }
    }
    return null;
  }

  /**
   * 携帯電話へ送信するメールの内容を作成する．
   *
   * @return
   */
  public static String createReminderMsgForCellPhone(EipTSchedule schedule,
      List<ALEipUser> memberList, int destUserID) {
    String date_detail = "";
    try {
      date_detail = getMsgDate(schedule);
    } catch (Exception e) {
      return "";
    }

    StringWriter out = null;
    try {
      VelocityService service =
        (VelocityService) ((TurbineServices) TurbineServices.getInstance())
          .getService(VelocityService.SERVICE_NAME);
      Context context = service.getContext();

      context.put("titleValue", schedule.getName().toString());
      context.put("dateValue", date_detail);

      if (memberList != null) {
        int size = memberList.size();
        int i;
        StringBuffer body = new StringBuffer("");
        for (i = 0; i < size; i++) {
          if (i != 0) {
            body.append(", ");
          }
          ALEipUser member = memberList.get(i);
          body.append(member.getAliasName());
        }
        context.put("menbersList", body.toString());
      }

      ALEipUser destUser;
      try {
        destUser = ALEipUtils.getALEipUser(destUserID);
      } catch (ALDBErrorException ex) {
        logger.error("schedule", ex);
        return "";
      }

      context.put("Alias", ALOrgUtilsService.getAlias());

      context.put("globalUrl1", ALMailUtils.getGlobalurl()
        + "?key="
        + ALCellularUtils.getCellularKey(destUser));

      out = new StringWriter();
      service.handleRequest(context, "mail/scheduleReminder.vm", out);
      out.flush();
      return out.toString();
    } catch (IllegalArgumentException e) {

    } catch (RuntimeException e) {
      String message = e.getMessage();
      logger.warn(message, e);
      e.printStackTrace();
    } catch (Exception e) {
      String message = e.getMessage();
      logger.warn(message, e);
      e.printStackTrace();
    } finally {
      if (out != null) {
        try {
          out.close();
        } catch (IOException e) {
          // ignore
        }
      }
    }
    return null;
  }

  public static String createReminderMsgForMessage(EipTSchedule schedule,
      List<ALEipUser> memberList) {
    boolean enableAsp = JetspeedResources.getBoolean("aipo.asp", false);

    String date_detail = "";

    try {

      date_detail = getMsgDate(schedule);
    } catch (Exception e) {
      return "";
    }

    StringWriter out = null;
    try {
      VelocityService service =
        (VelocityService) ((TurbineServices) TurbineServices.getInstance())
          .getService(VelocityService.SERVICE_NAME);
      Context context = service.getContext();

      context.put("titleValue", schedule.getName().toString());
      context.put("dateValue", date_detail);

      if (schedule.getPlace().toString().length() > 0) {
        context.put("placeValue", schedule.getPlace().toString());
      }

      if (schedule.getNote().toString().length() > 0) {
        context.put("noteValue", schedule.getNote().toString());
      }

      if (memberList != null) {
        int size = memberList.size();
        int i;
        StringBuffer body = new StringBuffer("");
        for (i = 0; i < size; i++) {
          if (i != 0) {
            body.append(", ");
          }
          ALEipUser member = memberList.get(i);
          body.append(member.getAliasName());
        }
        context.put("menbersList", body.toString());
      }

      context.put("Alias", ALOrgUtilsService.getAlias());

      if (enableAsp) {
        context.put("globalUrl1", ALMailUtils.getGlobalurl());
      } else {
        context.put("globalurl2", ALMailUtils.getGlobalurl());
        context.put("globalUrl3", ALMailUtils.getLocalurl());
      }

      out = new StringWriter();
      service.handleRequest(context, "mail/scheduleReminderMessage.vm", out);
      out.flush();
      return out.toString();
    } catch (IllegalArgumentException e) {

    } catch (Exception e) {
      String message = e.getMessage();
      logger.warn(message, e);
      e.printStackTrace();
    } finally {
      if (out != null) {
        try {
          out.close();
        } catch (IOException e) {
          // ignore
        }
      }
    }
    return null;

  }

  public static String getHolidayOfWeek() {
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
  }

  /**
   * 祝日を休日にするかどうかを検証する. 休日にする場合 true
   */
  public static boolean isDayOffHoliday() {
    String cacheHoliday = getHolidayOfWeek();
    return (cacheHoliday.charAt(8) == '0') ? false : true;
  }

  public static boolean isUserHoliday(int DayOfWeek) {
    String cacheHoliday = getHolidayOfWeek();
    return cacheHoliday.charAt(DayOfWeek) != '0';

  }
}
