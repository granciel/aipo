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
package com.aimluck.eip.project.util;

import static com.aimluck.eip.util.ALLocalizationUtils.*;

import java.io.StringWriter;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.cayenne.DataRow;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.commons.lang.StringUtils;
import org.apache.fulcrum.localization.LocalizationService;
import org.apache.jetspeed.services.customlocalization.CustomLocalizationService;
import org.apache.jetspeed.services.logging.JetspeedLogFactoryService;
import org.apache.jetspeed.services.logging.JetspeedLogger;
import org.apache.jetspeed.services.resources.JetspeedResources;
import org.apache.jetspeed.util.ServiceUtil;
import org.apache.turbine.util.RunData;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.context.Context;

import com.aimluck.eip.cayenne.om.portlet.EipMProjectKubun;
import com.aimluck.eip.cayenne.om.portlet.EipMProjectKubunValue;
import com.aimluck.eip.cayenne.om.portlet.EipTProject;
import com.aimluck.eip.cayenne.om.portlet.EipTProjectFile;
import com.aimluck.eip.cayenne.om.portlet.EipTProjectMember;
import com.aimluck.eip.cayenne.om.portlet.EipTProjectTask;
import com.aimluck.eip.cayenne.om.portlet.EipTProjectTaskComment;
import com.aimluck.eip.cayenne.om.portlet.EipTProjectTaskCommentFile;
import com.aimluck.eip.cayenne.om.portlet.EipTProjectTaskMember;
import com.aimluck.eip.cayenne.om.security.TurbineUser;
import com.aimluck.eip.common.ALDBErrorException;
import com.aimluck.eip.common.ALEipConstants;
import com.aimluck.eip.common.ALEipUser;
import com.aimluck.eip.common.ALPageNotFoundException;
import com.aimluck.eip.mail.ALAdminMailContext;
import com.aimluck.eip.mail.ALAdminMailMessage;
import com.aimluck.eip.mail.ALMailService;
import com.aimluck.eip.mail.util.ALEipUserAddr;
import com.aimluck.eip.mail.util.ALMailUtils;
import com.aimluck.eip.orm.Database;
import com.aimluck.eip.orm.query.Operations;
import com.aimluck.eip.orm.query.SQLTemplate;
import com.aimluck.eip.orm.query.SelectQuery;
import com.aimluck.eip.project.ProjectResultData;
import com.aimluck.eip.project.ProjectTaskCommentResultData;
import com.aimluck.eip.project.ProjectTaskMemberResultData;
import com.aimluck.eip.project.ProjectTaskResultData;
import com.aimluck.eip.services.orgutils.ALOrgUtilsService;
import com.aimluck.eip.services.storage.ALStorageService;
import com.aimluck.eip.util.ALEipUtils;
import com.aimluck.eip.util.ALLocalizationUtils;

/**
 * プロジェクト管理のユーティリティクラスです。
 *
 */
public class ProjectUtils {

  /** logger */
  private static final JetspeedLogger logger = JetspeedLogFactoryService
    .getLogger(ProjectUtils.class.getName());

  /** ポートレット名 */
  public static final String PROJECT_PORTLET_NAME = "Project";

  /** フラグ：ON */
  public static final String FLG_ON = "t";

  /** フラグ：OFF */
  public static final String FLG_OFF = "f";

  /**
   * 日付空扱い
   */
  public static final String EMPTY_DATE = "9999/12/31";

  /**
   * 日付フォーマット（ロジック用）
   */
  public static final String DATE_FORMAT = "yyyy/MM/dd";

  /**
   * 日付フォーマット（DB用）
   */
  public static final String DB_DATE_FORMAT = "YYYY/MM/DD";

  /**
   * 日付フォーマット（画面表示用）
   */
  public static final String DISP_DATE_FORMAT = "yyyy年M月d日（E）";

  /** 小数値項目フォーマット */
  private final static DecimalFormat df = new DecimalFormat("#.0##");

  /** 新規、進行中、フィードバック */
  public static final List<String> incompleteStatus = Arrays.asList(
    "1",
    "2",
    "3");

  /** 完了、却下、停止 */
  public static final List<String> completeStatus = Arrays
    .asList("4", "5", "6");

  // ---------------------------------------------------
  // プロジェクト関連
  // ---------------------------------------------------

  /**
   * プロジェクト情報オブジェクトモデルのIdを取得します。
   *
   * @param rundata
   *          RunData
   * @param context
   *          Context
   * @return プロジェクト情報オブジェクトモデル
   */
  public static Integer getEipTProjectId(RunData rundata, Context context) {
    String projectId =
      ALEipUtils.getTemp(rundata, context, ALEipConstants.ENTITY_ID);
    Integer id = null;
    if (projectId == null || (id = Integer.valueOf(projectId)) == null) {
      // Todo IDが空の場合
      logger.debug("ProjectUtils] Empty ID...");
      return null;
    } else {
      return id;
    }
  }

  /**
   * プロジェクト情報オブジェクトモデルを取得します。
   *
   * @param rundata
   *          RunData
   * @param context
   *          Context
   * @return プロジェクト情報オブジェクトモデル
   */
  public static EipTProject getEipTProject(RunData rundata, Context context) {
    return getEipTProject(getEipTProjectId(rundata, context));
  }

  /**
   * プロジェクト情報オブジェクトモデルを取得します。
   *
   * @param projectId
   *          プロジェクトID
   * @return プロジェクト情報 オブジェクトモデル
   */
  public static EipTProject getEipTProject(Integer projectId) {

    try {
      if (projectId == null) {
        // Request IDが空の場合
        logger.debug("[ProjectUtils] Empty ID...");
        return null;
      }

      SelectQuery<EipTProject> query = Database.query(EipTProject.class);
      query.setQualifier(ExpressionFactory.matchDbExp(
        EipTProject.PROJECT_ID_PK_COLUMN,
        projectId));

      List<EipTProject> list = query.fetchList();

      if (list == null || list.isEmpty()) {
        // 指定したプロジェクトIDのレコードが見つからない場合
        logger.debug("[ProjectUtils] Not found ID...");
        return null;
      }
      return list.get(0);

    } catch (Exception ex) {
      logger.error("Exception", ex);
      return null;
    }
  }

  /**
   * 全プロジェクトを返す
   *
   * @return 全プロジェクトのリスト
   */
  public static List<ProjectResultData> getAllProject() {
    List<ProjectResultData> allProject = new ArrayList<ProjectResultData>();

    try {
      List<EipTProject> list =
        Database.query(EipTProject.class).orderAscending(
          EipTProject.PROJECT_NAME_PROPERTY).fetchList();

      for (EipTProject project : list) {
        ProjectResultData data = new ProjectResultData();
        data.initField();
        data.setProjectId(project.getProjectId());
        data.setProjectName(project.getProjectName());
        allProject.add(data);
      }
    } catch (Exception ex) {
      logger.error("Exception", ex);
    }

    return allProject;
  }

  /**
   * プロジェクトオブジェクトよりプロジェクトResult情報を返す
   *
   * @param model
   *          プロジェクトオブジェクト
   * @return プロジェクトResult情報
   */
  public static ProjectResultData getProjectResultData(EipTProject model) {

    try {
      // 登録者
      ALEipUser adminUser = ALEipUtils.getALEipUser(model.getAdminUserId());

      ProjectResultData data = new ProjectResultData();
      data.initField();
      data.setProjectId(model.getProjectId()); // プロジェクトID
      data.setProjectName(model.getProjectName()); // プロジェクト名
      data.setExplanation(model.getExplanation()); // 説明
      data.setAdminUserId(model.getAdminUserId()); // 管理者ID
      data.setAdminUserName(adminUser.getAliasName().getValue()); // 管理者名
      data.setProgressFlg(model.getProgressFlg()); // 進捗率入力フラグ
      data.setProgressRate(model.getProgressRate()); // 進捗率
      data.setCreateDate(model.getCreateDate()); // 作成日
      data.setUpdateDate(model.getUpdateDate()); // 更新日
      data.setTaskUpdateDate(model.getUpdateDate()); // タスク更新日（初期値）
      return data;

    } catch (Exception ex) {
      logger.error("Exception", ex);
      return null;
    }
  }

  /**
   * プロジェクト進捗率を取得します。 最上位のタスクより進捗率を計算します。
   *
   * @param projectId
   *          プロジェクトID
   * @return 紐づく全タスク情報
   */
  public static int getProjectProgressRate(Integer projectId) {

    try {

      List<DataRow> result = getProjectProgress(projectId);

      if (result == null) {
        return 0;
      }

      DataRow row = result.get(0);
      Object per = row.get("result_per");
      if (per == null) {
        return 0;
      }

      return Integer.valueOf(row.get("result_per").toString());

    } catch (Exception ex) {
      logger.error("Exception", ex);
      return 0;
    }
  }

  /**
   * プロジェクトの進捗情報を取得します。
   *
   * @param projectId
   *          プロジェクトID
   * @return 進捗情報
   */
  public static List<DataRow> getProjectProgress(Integer projectId) {

    /** CURRENT_DATE in SQL depend on Database locale, so unify the time in java */

    if (Database.isJdbcMySQL()) {

      SimpleDateFormat sdfSrc = new SimpleDateFormat("yyyy/MM/dd");
      Date date = null;
      try {
        date = sdfSrc.parse(EMPTY_DATE);
      } catch (ParseException e) {
        logger.error("getProjectProgress", e);
        throw new RuntimeException(e);
      }
      SimpleDateFormat sdfDest = new SimpleDateFormat("yyyy-MM-dd");
      String formatedEmptyDate = sdfDest.format(date);

      StringBuilder sb = new StringBuilder();
      sb
        .append("SELECT task.progress_rate, task.start_plan_date, task.end_plan_date, task.plan_workload, member.workload, ");
      sb.append("  CASE WHEN ");
      sb.append("    ").append(getCurrentDateWithCast()).append(
        " < task.end_plan_date ");
      sb.append("  THEN");
      sb.append("    CASE WHEN ");
      sb.append("      ").append(getCurrentDateWithCast()).append(
        " - task.start_plan_date + 1 < 0 THEN 0 ");
      sb.append("    ELSE");
      sb.append("      ").append(getCurrentDateWithCast()).append(
        " - task.start_plan_date + 1 ");
      sb.append("    END");
      sb.append("  ELSE");
      sb.append("    task.end_plan_date - task.start_plan_date + 1 ");
      sb.append("  END");
      sb.append("  AS lapsed_days, ");
      sb
        .append("  task.end_plan_date - task.start_plan_date + 1 AS task_days, task.update_date AS task_update_date FROM eip_t_project_task AS task ");
      sb.append("  JOIN");
      sb.append("  (");
      sb
        .append("    SELECT task_id, SUM(workload) AS workload FROM eip_t_project_task_member GROUP BY task_id");
      sb.append("  ) AS member ON member.task_id = task.task_id ");
      sb.append("  WHERE task.project_id = #bind($project_id) AND NOT EXISTS");
      sb.append("  (");
      sb
        .append("    SELECT 0 FROM eip_t_project_task AS sub WHERE sub.parent_task_id = task.task_id");
      sb.append("  )");
      sb
        .append("  AND start_plan_date <> #bind($empty_date) AND end_plan_date <> #bind($empty_date)");

      String subQuery = sb.toString();

      StringBuilder main = new StringBuilder();
      main
        .append("SELECT COUNT(0) AS cnt, SUM(lapsed_days) AS lapsed_days, SUM(task_days) AS task_days, CONVERT(SUM(task_days * progress_rate) / SUM(task_days), SIGNED) AS result_per, CONVERT(SUM(lapsed_days) * 100 / SUM(task_days), SIGNED) AS plan_per, CONVERT(SUM(plan_workload), SIGNED) AS plan_workload, CONVERT(SUM(workload), SIGNED) AS workload, CONVERT(SUM(task_days * progress_rate) / SUM(task_days), SIGNED) AS result_per, MAX(task_update_date) AS task_update_date FROM");
      main.append("(").append(subQuery).append(") AS base");

      String query = main.toString();
      SQLTemplate<EipTProjectTask> sqltemp =
        Database.sql(EipTProjectTask.class, query);
      sqltemp.param("project_id", projectId);
      sqltemp.param("empty_date", formatedEmptyDate);
      List<DataRow> result = sqltemp.fetchListAsDataRow();
      return result;
    } else {

      StringBuilder sb = new StringBuilder();
      sb.append("WITH base AS (");
      sb.append("  SELECT");
      sb.append("        task.progress_rate"); // 進捗率
      sb.append("      , task.start_plan_date"); // 開始予定日
      sb.append("      , task.end_plan_date"); // 完了予定日
      sb.append("      , task.plan_workload"); // 計画工数
      sb.append("      , member.workload"); // 工数
      sb.append("      , CASE");
      sb.append("          WHEN ").append(getCurrentDateWithCast()).append(
        " < task.end_plan_date");
      sb.append("            THEN");
      sb
        .append("              CASE WHEN ")
        .append(getCurrentDateWithCast())
        .append(" - task.start_plan_date + 1 < 0");
      sb.append("                THEN 0");
      sb
        .append("                ELSE ")
        .append(getCurrentDateWithCast())
        .append(" - task.start_plan_date + 1");
      sb.append("              END");
      sb.append("            ELSE");
      sb.append("              task.end_plan_date - task.start_plan_date + 1");
      sb.append("        END AS lapsed_days"); // 基準日までのタスク経過日数
      sb
        .append("      , task.end_plan_date - task.start_plan_date + 1 AS task_days"); // タスク経過日数
      sb.append("      , task.update_date AS task_update_date"); // 更新日
      sb.append("    FROM");
      sb.append("      eip_t_project_task AS task");
      sb.append("        JOIN ( ");
      sb.append("          SELECT");
      sb.append("              task_id");
      sb.append("              , SUM(workload) AS workload");
      sb.append("            FROM");
      sb.append("              eip_t_project_task_member");
      sb.append("            GROUP BY");
      sb.append("              task_id");
      sb.append("        ) AS member ");
      sb.append("          ON member.task_id = task.task_id");
      sb.append("   WHERE");
      sb.append("         task.project_id = #bind($project_id)");
      sb.append("     AND NOT EXISTS(");
      sb.append("           SELECT 0");
      sb.append("             FROM eip_t_project_task AS sub");
      sb.append("            WHERE");
      sb.append("                  sub.parent_task_id = task.task_id");
      sb.append("         )");
      sb
        .append("     AND start_plan_date <> TO_DATE(#bind($empty_date), #bind($date_format))");
      sb
        .append("     AND end_plan_date <> TO_DATE(#bind($empty_date), #bind($date_format))");
      sb.append(")");
      sb.append("SELECT");
      sb.append("      #result('COUNT(0)' 'int' 'cnt')");
      sb.append("    , #result('SUM(lapsed_days)' 'int' 'lapsed_days')");
      sb.append("    , #result('SUM(task_days)' 'int' 'task_days')");
      sb
        .append("    , #result('SUM(task_days * progress_rate) / SUM(task_days)' 'int' 'result_per')");
      sb
        .append("    , #result('SUM(lapsed_days) * 100 / SUM(task_days)' 'int' 'plan_per')"); // 予定進捗
      sb
        .append("    , #result('SUM(plan_workload)' 'java.math.BigDecimal' 'plan_workload')");// 計画工数
      sb
        .append("    , #result('SUM(workload)' 'java.math.BigDecimal' 'workload')");// 実績工数
      sb
        .append("    , #result('SUM(task_days * progress_rate) / SUM(task_days)' 'int' 'result_per')");
      sb
        .append("    , #result('MAX(task_update_date)' 'java.util.Date' 'task_update_date')");
      sb.append("  FROM");
      sb.append("    base");

      SQLTemplate<EipTProjectTask> sqltemp =
        Database.sql(EipTProjectTask.class, String.valueOf(sb));
      sqltemp.param("project_id", projectId);
      sqltemp.param("date_format", DB_DATE_FORMAT);
      sqltemp.param("empty_date", EMPTY_DATE);

      List<DataRow> result = sqltemp.fetchListAsDataRow();
      return result;
    }
  }

  // ---------------------------------------------------
  // プロジェクトメンバー関連
  // ---------------------------------------------------

  /**
   * プロジェクトメンバーを返す
   *
   * @param projectId
   *          プロジェクトID
   * @return プロジェクトメンバーのリスト
   */
  public static List<ALEipUser> getProjectMembers(Integer projectId) {
    List<ALEipUser> members = new ArrayList<ALEipUser>();

    try {
      SelectQuery<EipTProjectMember> query =
        Database.query(EipTProjectMember.class);
      query.setQualifier(ExpressionFactory.matchExp(
        EipTProjectMember.EIP_TPROJECT_PROPERTY,
        projectId));

      List<EipTProjectMember> list = query.fetchList();

      for (EipTProjectMember member : list) {
        members.add(ALEipUtils.getALEipUser(member.getUserId()));
      }
    } catch (Exception ex) {
      logger.error("Exception", ex);
    }

    return members;
  }

  /**
   * プロジェクトメンバーリストを取得します。 <BR>
   *
   * @param rundata
   * @param context
   * @return
   */
  public static List<EipTProjectMember> getEipTProjectMember(EipTProject project) {
    try {
      SelectQuery<EipTProjectMember> query =
        Database.query(EipTProjectMember.class);
      Expression exp =
        ExpressionFactory.matchDbExp(EipTProject.PROJECT_ID_PK_COLUMN, project
          .getProjectId());
      query.setQualifier(exp);

      List<EipTProjectMember> members = query.fetchList();

      if (members == null || members.size() == 0) {
        // 指定した Project IDのレコードが見つからない場合
        logger.debug("[ProjectUtils] Not found ID...");
        return null;
      }
      return members;
    } catch (Exception ex) {
      logger.error("Exception", ex);
      return null;
    }
  }

  /**
   * プロジェクトに紐づくメンバーを削除する
   *
   * @param project
   *          プロジェクトオブジェクト
   * @return TRUE 成功 FALSE 失敗
   */
  public static boolean removeProjectMember(EipTProject project) {

    try {
      // プロジェクトに紐づく担当者
      SelectQuery<EipTProjectMember> query =
        Database.query(EipTProjectMember.class);
      query.setQualifier(ExpressionFactory.matchExp(
        EipTProjectMember.EIP_TPROJECT_PROPERTY,
        project));

      List<EipTProjectMember> projectMemberList = query.fetchList();

      if (projectMemberList == null || projectMemberList.isEmpty()) {
        return true;
      }

      // プロジェクトに紐づくメンバーを削除
      Database.deleteAll(projectMemberList);

    } catch (Exception ex) {
      logger.error("Exception", ex);
      return false;
    }

    return true;
  }

  /**
   * プロジェクト作成時に参加メンバーへ通知メールを送る
   *
   * @param taskId
   *          タスクID
   * @return 成否
   */
  public static boolean sendMailForProjectMembers(RunData rundata,
      Context context, EipTProject project, List<ALEipUser> memberList) {

    String orgId = Database.getDomainName();
    String subject =
      "["
        + ALOrgUtilsService.getAlias()
        + "]"
        + ALLocalizationUtils.getl10n("PROJECT_MAIL_TITLE");

    try {
      List<ALEipUserAddr> destMemberList =
        ALMailUtils.getALEipUserAddrs(
          memberList,
          ALEipUtils.getUserId(rundata),
          false);

      List<ALAdminMailMessage> messageList =
        new ArrayList<ALAdminMailMessage>();
      for (ALEipUserAddr destMember : destMemberList) {
        ALAdminMailMessage message = new ALAdminMailMessage(destMember);
        message.setPcSubject(subject);
        message.setCellularSubject(subject);
        message.setPcBody(createProjectMemberMsg(rundata, message
          .getPcMailAddr(), project));
        message.setCellularBody(createProjectMemberMsg(rundata, message
          .getCellMailAddr(), project));
        messageList.add(message);
      }

      ALMailService.sendAdminMailAsync(new ALAdminMailContext(orgId, ALEipUtils
        .getUserId(rundata), messageList, ALMailUtils
        .getSendDestType(ALMailUtils.KEY_MSGTYPE_WORKFLOW)));

    } catch (Exception ex) {
      logger.error("project", ex);
      return false;
    }
    return true;

  }

  /**
   * 送信するメールの内容を作成する．
   *
   * @return
   */
  public static String createProjectMemberMsg(RunData rundata, String addr,
      EipTProject project) {
    VelocityContext context = new VelocityContext();
    boolean enableAsp = JetspeedResources.getBoolean("aipo.asp", false);
    String CR = ALMailUtils.CR;

    context.put("user_email", addr);

    // （さんの申請は承認されました。など）
    StringBuffer message = new StringBuffer("");
    message.append(CR);
    message.append(
      getl10nFormat("PROJECT_MAIL_TEXT2", project.getProjectName())).append(CR);
    context.put("message", message);

    // サービス
    context.put("serviceAlias", ALOrgUtilsService.getAlias());
    // サービス（Aipo）へのアクセス
    context.put("enableAsp", enableAsp);
    context.put("globalurl", ALMailUtils.getGlobalurl());
    context.put("localurl", ALMailUtils.getLocalurl());
    CustomLocalizationService locService =
      (CustomLocalizationService) ServiceUtil
        .getServiceByName(LocalizationService.SERVICE_NAME);
    String lang = locService.getLocale(rundata).getLanguage();
    StringWriter writer = new StringWriter();
    try {
      if (lang != null && lang.equals("ja")) {
        Template template =
          Velocity.getTemplate("portlets/mail/"
            + lang
            + "/project-notification-mail.vm", "utf-8");
        template.merge(context, writer);
      } else {
        Template template =
          Velocity.getTemplate(
            "portlets/mail/project-notification-mail.vm",
            "utf-8");
        template.merge(context, writer);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    writer.flush();
    String ret = writer.getBuffer().toString();
    return ret;

  }

  // ---------------------------------------------------
  // タスク関連
  // ---------------------------------------------------

  /**
   * タスクオブジェクトモデルのIdを取得します。
   *
   * @param rundata
   *          RunData
   * @param context
   *          Context
   * @return タスクオブジェクトモデル
   */
  public static Integer getEipTProjectTaskId(RunData rundata, Context context) {
    String taskId =
      ALEipUtils.getTemp(rundata, context, ALEipConstants.ENTITY_ID);
    Integer id = null;
    if (taskId == null || (id = Integer.valueOf(taskId)) == null) {
      // Todo IDが空の場合
      logger.debug("ProjectUtils] Empty ID...");
      return null;
    } else {
      return id;
    }
  }

  /**
   * タスクオブジェクトモデルを取得します。
   *
   * @param rundata
   *          RunData
   * @param context
   *          Context
   * @return プロジェクト情報オブジェクトモデル
   */
  public static EipTProjectTask getEipTProjectTask(RunData rundata,
      Context context) {
    String taskId =
      ALEipUtils.getTemp(rundata, context, ALEipConstants.ENTITY_ID);
    return getEipTProjectTask(taskId);
  }

  /**
   * タスク一覧を返す
   *
   * @param taskId
   *          プロジェクトID
   * @return タスク一覧
   */
  public static EipTProjectTask getEipTProjectTask(String taskId) {

    try {
      if (taskId == null || Integer.valueOf(taskId) == null) {
        // Request IDが空の場合
        logger.debug("[ProjectUtils] Empty ID...");
        return null;
      }

      SelectQuery<EipTProjectTask> query =
        Database.query(EipTProjectTask.class);
      query.setQualifier(ExpressionFactory.matchDbExp(
        EipTProjectTask.TASK_ID_PK_COLUMN,
        taskId));

      List<EipTProjectTask> list = query.fetchList();

      if (list == null || list.isEmpty()) {
        // 指定したプロジェクトIDのレコードが見つからない場合
        logger.debug("[ProjectUtils] Not found ID...");
        return null;
      }
      return list.get(0);

    } catch (Exception ex) {
      logger.error("Exception", ex);
      return null;
    }

  }

  /**
   * タスクの存在チェック
   *
   * @param projectId
   *          プロジェクトID
   * @return
   */
  public static boolean getEipTProjectTaskCheck(Integer projectId) {

    try {

      SelectQuery<EipTProjectTask> query =
        Database.query(EipTProjectTask.class);
      query.setQualifier(ExpressionFactory.matchExp(
        EipTProjectTask.EIP_TPROJECT_PROPERTY,
        projectId));

      List<EipTProjectTask> list = query.fetchList();

      if (list == null || list.isEmpty()) {
        return true;
      }
      return false;

    } catch (Exception ex) {
      logger.error("Exception", ex);
      return false;
    }
  }

  /**
   * タスクオブジェクトリストより項目定義Result情報リストを返す
   *
   * @param taskList
   *          タスクオブジェクトリスト
   * @return タスクResult情報リスト
   */
  public static List<ProjectTaskResultData> getProjectTaskResultList(
      List<EipTProjectTask> taskList) {
    List<ProjectTaskResultData> resultList =
      new ArrayList<ProjectTaskResultData>();
    for (EipTProjectTask task : taskList) {
      resultList.add(getProjectTaskResultData(task));
    }
    return resultList;
  }

  /**
   * タスクオブジェクトよりタスクResult情報を返す
   *
   * @param model
   *          タスクオブジェクト
   * @return タスクResult情報
   */
  public static ProjectTaskResultData getProjectTaskResultData(
      EipTProjectTask model) {
    ProjectTaskResultData data = new ProjectTaskResultData();
    data.initField();
    data.setTaskId(model.getTaskId()); // タスクID
    data.setProjectId(model.getProjectId()); // プロジェクトID
    data.setTracker(model.getTracker()); // 分類
    data.setTaskName(model.getTaskName()); // タスク
    data.setProjectName(model.getProjectName()); // プロジェクト名
    data.setExplanation(model.getExplanation()); // 説明
    data.setStatus(model.getStatus()); // ステータス
    data.setPriority(model.getPriority()); // 優先度
    // 開始予定日
    if (!ProjectUtils.isEmptyDate(model.getStartPlanDate())) {
      data.setStartPlanDate(model.getStartPlanDate());
    }
    // 完了予定日
    if (!ProjectUtils.isEmptyDate(model.getEndPlanDate())) {
      data.setEndPlanDate(model.getEndPlanDate());
    }
    // 開始実績日
    if (!ProjectUtils.isEmptyDate(model.getStartDate())) {
      data.setStartDate(model.getStartDate());
    }
    // 完了実績日
    if (!ProjectUtils.isEmptyDate(model.getEndDate())) {
      data.setEndDate(model.getEndDate());
    }
    data.setPlanWorkload(new BigDecimal(df.format(model.getPlanWorkload()))); // 計画工数
    data.setProgressRate(model.getProgressRate()); // 進捗率
    data.setCreateDate(model.getCreateDate());// 作成日
    data.setUpdateDate(model.getUpdateDate());// 更新日

    // 担当者
    data.setMemberList(ProjectUtils.getProjectTaskMembers(model
      .getTaskId()
      .toString()));

    // 親タスクの有無
    Integer parentTaskId = model.getParentTaskId();
    if (parentTaskId != null) {
      EipTProjectTask parent = getEipTProjectTask("" + parentTaskId);
      data.setParentTaskId(parent.getTaskId()); // 親タスクID
      data.setParentTaskName(parent.getTaskName());
    }
    data.setIndent(model.getIndent()); // タスク名表示インデント

    return data;
  }

  /**
   * 子タスクの件数を取得します。
   *
   * @param taskId
   *          タスクID
   * @return 子タスクの件数
   */
  public static int getCountChildrenTask(Integer taskId) {

    try {

      if (taskId == null) {
        return 0;
      }

      SelectQuery<EipTProjectTask> query =
        Database.query(EipTProjectTask.class);
      query.setQualifier(ExpressionFactory.matchExp(
        EipTProjectTask.PARENT_TASK_ID_PROPERTY,
        taskId));

      List<EipTProjectTask> list = query.fetchList();

      if (list == null || list.isEmpty()) {
        return 0;
      }
      return list.size();

    } catch (Exception ex) {
      logger.error("Exception", ex);
      return -1;
    }

  }

  // ---------------------------------------------------
  // タスク担当者関連
  // ---------------------------------------------------

  /**
   * タスク担当者を返す
   *
   * @param taskId
   *          タスクID
   * @return タスク担当者のリスト
   */
  public static List<ProjectTaskMemberResultData> getProjectTaskMembers(
      String taskId) {
    List<ProjectTaskMemberResultData> members =
      new ArrayList<ProjectTaskMemberResultData>();

    try {
      SelectQuery<EipTProjectTaskMember> query =
        Database.query(EipTProjectTaskMember.class);
      query.setQualifier(ExpressionFactory.matchExp(
        EipTProjectTaskMember.EIP_TPROJECT_TASK_PROPERTY,
        taskId));
      query.orderAscending(EipTProjectTaskMember.USER_ID_PROPERTY);

      List<EipTProjectTaskMember> list = query.fetchList();

      for (EipTProjectTaskMember member : list) {
        ALEipUser user = ALEipUtils.getALEipUser(member.getUserId());
        ProjectTaskMemberResultData data = new ProjectTaskMemberResultData();
        data.initField();
        data.setUserId(user.getUserId().getValue());
        data.setUserName(user.getAliasName().getValue());
        BigDecimal workload = new BigDecimal(df.format(member.getWorkload()));
        data.setWorkload(workload);
        members.add(data);
      }
    } catch (Exception ex) {
      logger.error("Exception", ex);
    }

    return members;
  }

  /**
   * タスクに紐づく担当者を削除する
   *
   * @param task
   *          タスクオブジェクト
   * @return TRUE 成功 FALSE 失敗
   */
  public static boolean removeProjectTaskMember(EipTProjectTask task) {

    try {
      // タスクに紐づく担当者
      SelectQuery<EipTProjectTaskMember> query =
        Database.query(EipTProjectTaskMember.class);
      query.setQualifier(ExpressionFactory.matchExp(
        EipTProjectTaskMember.EIP_TPROJECT_TASK_PROPERTY,
        task));

      List<EipTProjectTaskMember> taskMemberList = query.fetchList();

      if (taskMemberList == null || taskMemberList.isEmpty()) {
        return true;
      }

      // タスクに紐づく担当者を削除
      Database.deleteAll(taskMemberList);

    } catch (Exception ex) {
      logger.error("Exception", ex);
      return false;
    }

    return true;
  }

  /**
   * タスク担当者へ通知メールを送る
   *
   * @param taskId
   *          タスクID
   * @return 成否
   */
  public static boolean sendMailForTaskMembers(RunData rundata,
      Context context, EipTProjectTask task, EipTProject project,
      List<ProjectTaskMemberResultData> mailUserList) {

    String orgId = Database.getDomainName();
    String subject =
      "["
        + ALOrgUtilsService.getAlias()
        + "]"
        + ALLocalizationUtils.getl10n("PROJECT_MAIL_TITLE");

    try {
      List<ALEipUser> memberList = new ArrayList<ALEipUser>();
      for (ProjectTaskMemberResultData rd : mailUserList) {
        memberList.add(ALEipUtils
          .getALEipUser(rd.getUserId().getValueWithInt()));
      }

      List<ALEipUserAddr> destMemberList =
        ALMailUtils.getALEipUserAddrs(
          memberList,
          ALEipUtils.getUserId(rundata),
          false);

      List<ALAdminMailMessage> messageList =
        new ArrayList<ALAdminMailMessage>();
      for (ALEipUserAddr destMember : destMemberList) {
        ALAdminMailMessage message = new ALAdminMailMessage(destMember);
        message.setPcSubject(subject);
        message.setCellularSubject(subject);
        message.setPcBody(createTaskMemberMsg(
          rundata,
          message.getPcMailAddr(),
          task,
          project));
        message.setCellularBody(createTaskMemberMsg(rundata, message
          .getCellMailAddr(), task, project));
        messageList.add(message);
      }

      ALMailService.sendAdminMailAsync(new ALAdminMailContext(orgId, ALEipUtils
        .getUserId(rundata), messageList, ALMailUtils
        .getSendDestType(ALMailUtils.KEY_MSGTYPE_WORKFLOW)));

    } catch (Exception ex) {
      logger.error("project", ex);
      return false;
    }
    return true;

  }

  /**
   * 送信するメールの内容を作成する．
   *
   * @return
   */
  public static String createTaskMemberMsg(RunData rundata, String addr,
      EipTProjectTask task, EipTProject project) {
    VelocityContext context = new VelocityContext();
    boolean enableAsp = JetspeedResources.getBoolean("aipo.asp", false);
    String CR = ALMailUtils.CR;

    context.put("user_email", addr);

    // （さんの申請は承認されました。など）
    StringBuffer message = new StringBuffer("");
    message.append(CR);
    message.append(
      getl10nFormat("PROJECT_MAIL_TEXT", project.getProjectName(), task
        .getTaskName())).append(CR);
    context.put("message", message);

    // サービス
    context.put("serviceAlias", ALOrgUtilsService.getAlias());
    // サービス（Aipo）へのアクセス
    context.put("enableAsp", enableAsp);
    context.put("globalurl", ALMailUtils.getGlobalurl());
    context.put("localurl", ALMailUtils.getLocalurl());
    CustomLocalizationService locService =
      (CustomLocalizationService) ServiceUtil
        .getServiceByName(LocalizationService.SERVICE_NAME);
    String lang = locService.getLocale(rundata).getLanguage();
    StringWriter writer = new StringWriter();
    try {
      if (lang != null && lang.equals("ja")) {
        Template template =
          Velocity.getTemplate("portlets/mail/"
            + lang
            + "/project-notification-mail.vm", "utf-8");
        template.merge(context, writer);
      } else {
        Template template =
          Velocity.getTemplate(
            "portlets/mail/project-notification-mail.vm",
            "utf-8");
        template.merge(context, writer);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    writer.flush();
    String ret = writer.getBuffer().toString();
    return ret;

  }

  // ---------------------------------------------------
  // タスクコメント関連
  // ---------------------------------------------------

  /**
   * タスクコメントオブジェクトモデルを取得します。
   *
   * @param rundata
   *          RunData
   * @param context
   *          Context
   * @return プロジェクト情報オブジェクトモデル
   */
  public static EipTProjectTaskComment getEipTProjectTaskComment(
      RunData rundata, Context context) {
    String commentId =
      ALEipUtils.getTemp(rundata, context, ALEipConstants.ENTITY_ID);
    return getEipTProjectTaskComment(commentId);
  }

  /**
   * タスクコメント一覧を返す
   *
   * @param commentId
   *          コメントID
   * @return コメント一覧
   */
  public static EipTProjectTaskComment getEipTProjectTaskComment(
      String commentId) {

    try {
      if (commentId == null || Integer.valueOf(commentId) == null) {
        // Request IDが空の場合
        logger.debug("[ProjectUtils] Empty ID...");
        return null;
      }

      SelectQuery<EipTProjectTaskComment> query =
        Database.query(EipTProjectTaskComment.class);
      query.setQualifier(ExpressionFactory.matchDbExp(
        EipTProjectTaskComment.COMMENT_ID_PK_COLUMN,
        commentId));

      List<EipTProjectTaskComment> list = query.fetchList();

      if (list == null || list.isEmpty()) {
        // 指定したプロジェクトIDのレコードが見つからない場合
        logger.debug("[ProjectUtils] Not found ID...");
        return null;
      }
      return list.get(0);

    } catch (Exception ex) {
      logger.error("Exception", ex);
      return null;
    }

  }

  /**
   * タスクコメントリストを返す
   *
   * @param taskId
   *          タスクID
   * @return タスクコメントのリスト
   */
  public static List<ProjectTaskCommentResultData> getProjectTaskCommentList(
      String taskId, boolean hasAttachmentAuthority) {
    List<ProjectTaskCommentResultData> commentList =
      new ArrayList<ProjectTaskCommentResultData>();

    try {
      SelectQuery<EipTProjectTaskComment> query =
        Database.query(EipTProjectTaskComment.class);
      query.setQualifier(ExpressionFactory.matchExp(
        EipTProjectTaskComment.EIP_TPROJECT_TASK_PROPERTY,
        taskId));

      List<EipTProjectTaskComment> list = query.fetchList();

      /** ファイル操作オブジェクト */
      ProjectFile<EipTProjectTaskCommentFile, EipTProjectTaskComment> pfile =
        new ProjectFile<EipTProjectTaskCommentFile, EipTProjectTaskComment>(
          EipTProjectTaskCommentFile.class);

      for (EipTProjectTaskComment comment : list) {
        ALEipUser user =
          ALEipUtils.getALEipUser(comment.getTurbineUser().getUserId());
        ProjectTaskCommentResultData data = new ProjectTaskCommentResultData();
        data.initField();
        data.setCommentId(comment.getCommentId());
        data.setTaskId(comment.getTaskId());
        data.setComment(comment.getComment());
        data.setCreateUserId(user.getUserId().getValue());
        data.setCreateUser(user.getAliasName().getValue());
        data.setCreateDate(comment.getCreateDate());
        data.setUpdateDate(comment.getUpdateDate());
        data.setHasPhoto(user.hasPhoto());

        if (hasAttachmentAuthority) {
          // ファイルリスト
          List<EipTProjectTaskCommentFile> filelist =
            pfile.getSelectQueryForFiles(
              EipTProjectTaskComment.COMMENT_ID_PK_COLUMN,
              Integer.valueOf(comment.getCommentId())).fetchList();
          data.setAttachmentFiles(pfile.getFileList(filelist));
        }

        commentList.add(data);
      }
    } catch (Exception ex) {
      logger.error("Exception", ex);
    }

    return commentList;
  }

  /**
   * タスクに紐づくコメントを削除する
   *
   * @param task
   *          タスクオブジェクト
   * @return TRUE 成功 FALSE 失敗
   */
  public static boolean removeProjectTaskComment(RunData rundata,
      EipTProjectTask task) {

    try {
      // タスクに紐づくコメント
      SelectQuery<EipTProjectTaskComment> query =
        Database.query(EipTProjectTaskComment.class);
      query.setQualifier(ExpressionFactory.matchExp(
        EipTProjectTaskComment.EIP_TPROJECT_TASK_PROPERTY,
        task));

      List<EipTProjectTaskComment> taskCommentList = query.fetchList();

      if (taskCommentList == null || taskCommentList.isEmpty()) {
        return true;
      }

      /** ファイル操作オブジェクト */
      ProjectFile<EipTProjectTaskCommentFile, EipTProjectTaskComment> pfile =
        new ProjectFile<EipTProjectTaskCommentFile, EipTProjectTaskComment>(
          EipTProjectTaskCommentFile.class);

      for (EipTProjectTaskComment comment : taskCommentList) {
        // タスクコメントに紐づくファイルを削除
        pfile.removeFile(
          rundata,
          EipTProjectTaskCommentFile.EIP_TPROJECT_TASK_COMMENT_PROPERTY,
          comment.getCommentId());
      }

      // タスクに紐づくコメントを削除
      Database.deleteAll(taskCommentList);

    } catch (Exception ex) {
      logger.error("Exception", ex);
      return false;
    }

    return true;
  }

  // ---------------------------------------------------
  // ファイル情報取得
  // ---------------------------------------------------

  /**
   * ファイル検索のクエリを返します
   *
   * @param requestid
   *          ファイルを検索するリクエストのid
   * @return query
   */
  public static SelectQuery<EipTProjectFile> getSelectQueryForFiles(
      int requestid) {
    SelectQuery<EipTProjectFile> query = Database.query(EipTProjectFile.class);
    Expression exp =
      ExpressionFactory.matchDbExp(EipTProject.PROJECT_ID_PK_COLUMN, Integer
        .valueOf(requestid));
    query.setQualifier(exp);
    return query;
  }

  /**
   * ファイルオブジェクトモデルを取得します。 <BR>
   *
   * @param rundata
   * @param context
   * @return
   */
  public static EipTProjectFile getEipTProjectFile(RunData rundata)
      throws ALPageNotFoundException, ALDBErrorException {
    try {
      int attachmentIndex =
        rundata.getParameters().getInt("attachmentIndex", -1);
      if (attachmentIndex < 0) {
        // ID が空の場合
        logger.debug("[ProjectUtils] Empty ID...");
        throw new ALPageNotFoundException();

      }

      SelectQuery<EipTProjectFile> query =
        Database.query(EipTProjectFile.class);
      Expression exp =
        ExpressionFactory.matchDbExp(EipTProjectFile.FILE_ID_PK_COLUMN, Integer
          .valueOf(attachmentIndex));
      query.andQualifier(exp);
      List<EipTProjectFile> files = query.fetchList();
      if (files == null || files.size() == 0) {
        // 指定した ID のレコードが見つからない場合
        logger.debug("[ProjectUtils] Not found ID...");
        throw new ALPageNotFoundException();
      }
      return files.get(0);
    } catch (Exception ex) {
      logger.error("[ProjectUtils]", ex);
      throw new ALDBErrorException();
    }
  }

  // ---------------------------------------------------
  // 区分値取得
  // ---------------------------------------------------
  /**
   * 指定区分の区分を取得します。
   *
   * @param key
   *          区分名
   * @return 区分
   */
  public static EipMProjectKubun getEipMProjectKubun(String key) {

    try {

      SelectQuery<EipMProjectKubun> query =
        Database.query(EipMProjectKubun.class);
      query.setQualifier(ExpressionFactory.matchExp(
        EipMProjectKubun.PROJECT_KUBUN_CD_PROPERTY,
        key));

      return query.fetchSingle();

    } catch (Exception ex) {
      logger.error("Exception", ex);
      return null;
    }
  }

  /**
   * 指定区分の区分値を取得します。
   *
   *
   * @param cd
   *          区分コード
   * @param valCd
   *          区分値コード
   * @return 区分値マップ
   */
  public static EipMProjectKubunValue getEipMProjectKubunValue(String cd,
      String valCd) {

    try {

      // 区分
      EipMProjectKubun kubun = getEipMProjectKubun(cd);

      SelectQuery<EipMProjectKubunValue> query =
        Database.query(EipMProjectKubunValue.class);
      query.setQualifier(ExpressionFactory.matchExp(
        EipMProjectKubunValue.PROJECT_KUBUN_PROPERTY,
        kubun));
      query.andQualifier(ExpressionFactory.matchExp(
        EipMProjectKubunValue.PROJECT_KUBUN_VALUE_CD_PROPERTY,
        valCd));

      return query.fetchSingle();

    } catch (Exception ex) {
      logger.error("Exception", ex);
      return null;
    }
  }

  /**
   * 指定区分の区分値マップを取得します。
   *
   * @param cd
   *          区分コード
   * @return 区分値マップ
   */
  public static Map<String, String> getKubunMap(String cd) {

    try {

      // 区分
      EipMProjectKubun kubun = getEipMProjectKubun(cd);

      Map<String, String> map = new LinkedHashMap<String, String>();

      SelectQuery<EipMProjectKubunValue> query =
        Database.query(EipMProjectKubunValue.class);
      query.setQualifier(ExpressionFactory.matchExp(
        EipMProjectKubunValue.PROJECT_KUBUN_PROPERTY,
        kubun));
      query.orderAscending(EipMProjectKubunValue.ORDER_NO_PROPERTY);

      List<EipMProjectKubunValue> list = query.fetchList();

      for (EipMProjectKubunValue value : list) {
        map.put(value.getProjectKubunValueCd(), value.getProjectKubunValue());
      }

      return map;

    } catch (Exception ex) {
      logger.error("Exception", ex);
      return null;
    }

  }

  /**
   * 区分値を取得する
   *
   * @param cd
   *          区分コード
   * @param valCd
   *          区分値コード
   * @return 区分値文字列
   */
  public static String getKubunValueString(String key, String valCd) {
    try {
      EipMProjectKubunValue val = getEipMProjectKubunValue(key, valCd);
      return val.getProjectKubunValue();

    } catch (Exception e) {
      return null;
    }
  }

  // ---------------------------------------------------
  // 日付用
  // ---------------------------------------------------

  /**
   * 空設定用日付を取得する
   *
   * @return 日付
   */
  public static Date getEmptyDate() {
    Calendar cal = Calendar.getInstance();
    cal.set(9999, 11, 31);
    return cal.getTime();
  }

  /**
   * 空設定用日付かをチェックする
   *
   * @param date
   *          日付
   * @return TRUE:空設定日付 FALSE:通常の日付
   */
  public static boolean isEmptyDate(Date date) {
    if (date == null) {
      return false;
    }
    Calendar calendar = Calendar.getInstance();
    calendar.setTime(date);
    return calendar.get(Calendar.YEAR) == 9999;
  }

  /**
   * 経過日数を計算します。<br />
   * 差分日数ではなく当日も含めたものとなります。<br />
   * 例：from:2013/11/11 to:2013/11/11 → return:1
   *
   * @param from
   *          開始日
   * @param to
   *          終了日
   * @return 経過日数
   */
  public static int getLapsedDays(Calendar from, Calendar to) {
    return getLapsedDays(toString(from.getTime()), toString(to.getTime()));
  }

  /**
   * 経過日数を計算します。<br />
   * 差分日数ではなく当日も含めたものとなります。<br />
   * 例：from:2013/11/11 to:2013/11/11 → return:1
   *
   * @param from
   *          開始日
   * @param to
   *          終了日
   * @return 経過日数
   */
  public static int getLapsedDays(String from, String to) {

    if (EMPTY_DATE.equals(from) || EMPTY_DATE.equals(to)) {
      return 0;
    }

    SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);
    Date dateFrom = null;
    Date dateTo = null;

    try {
      dateFrom = sdf.parse(from);
      dateTo = sdf.parse(to);
    } catch (Exception ex) {
      logger.error("Exception", ex);
      return 0;
    }
    return (int) ((dateTo.getTime() - dateFrom.getTime())
      / (1000 * 60 * 60 * 24) + 1);
  }

  /**
   * 文字列をDate型に変換します。
   *
   * @param str
   *          日付文字列
   * @return 日付
   */
  public static Date toDate(String str) {
    try {
      return (new SimpleDateFormat(DATE_FORMAT)).parse(str);
    } catch (ParseException e) {
      return null;
    }
  }

  /**
   * Date型を文字列に変換します。
   *
   * @param date
   *          日付
   * @return 日付文字列
   */
  public static String toString(Date date) {
    if (date == null) {
      return null;
    }
    return (new SimpleDateFormat(DATE_FORMAT)).format(date);
  }

  /**
   * 年月日フォームよりカレンダークラスを取得する（検索用）
   *
   * @param rundata
   *          Rundata
   * @param context
   *          Context
   * @param key
   *          日付フォーム名
   * @return 入力日カレンダー
   */
  public static Calendar getViewCalendar(RunData rundata, Context context,
      String key) {
    Calendar cal = Calendar.getInstance();
    cal.set(Calendar.HOUR_OF_DAY, 0);
    cal.set(Calendar.MINUTE, 0);
    cal.set(Calendar.SECOND, 0);

    String year = getParameter(rundata, context, key + "_year");
    String month = getParameter(rundata, context, key + "_month");
    String day = getParameter(rundata, context, key + "_day");

    if (year != null) {
      cal.set(Calendar.YEAR, Integer.valueOf(year));
    }
    if (month != null) {
      cal.set(Calendar.MONTH, Integer.valueOf(month) - 1);
    }
    if (day != null) {
      cal.set(Calendar.DAY_OF_MONTH, Integer.valueOf(day));
    }

    return cal;
  }

  // ---------------------------------------------------
  // その他
  // ---------------------------------------------------

  /**
   * 入力値を取得する。フォーム値、セッション値の順で取得する。
   *
   * @param rundata
   *          Rundata
   * @param context
   *          Context
   * @param key
   *          フォーム名
   * @return 値
   */
  public static String getParameter(RunData rundata, Context context, String key) {
    String val = rundata.getParameters().getString(key);
    if (val == null) {
      val = ALEipUtils.getTemp(rundata, context, key);
    } else {
      ALEipUtils.setTemp(rundata, context, key, val);
    }
    return val;
  }

  public static void setParameterList(RunData rundata, Context context) {
    String filter = rundata.getParameters().getString("filter", "");
    String filterType = rundata.getParameters().getString("filtertype", "");
    if (filterType.isEmpty()) {
      return;
    }

    if (filterType
      .equals("target_user_id,target_tracker,target_priority,target_status")) {
      String[] filterTypeList = filterType.split(",");
      String[] filterList = filter.split(",");
      for (int i = 0; i <= 4; i++) {
        ALEipUtils.setTemp(rundata, context, filterTypeList[i], filterList[i]);

      }

    }
  }

  /**
   * 予定進捗率を計算します。
   *
   * @param lapsedDays
   *          経過日数
   * @param taskDays
   *          タスク日数
   * @return 予定進捗率
   */
  public static Integer getPlanWorkload(Integer lapsedDays, Integer taskDays) {

    if (taskDays == 0) {
      return 0;
    }

    Integer planProgressRate = lapsedDays * 100 / taskDays;
    if (planProgressRate < 0) {
      planProgressRate = 0;
    }
    return planProgressRate;
  }

  /**
   * ユーザ毎のルート保存先（絶対パス）を取得します。
   *
   * @param uid
   * @return
   */
  public static String getSaveDirPath(String orgId, int uid) {
    return ALStorageService.getDocumentPath(
      ProjectFile.FOLDER_FILEDIR,
      ProjectFile.CATEGORY_KEY + ALStorageService.separator() + uid);
  }

  /**
   * タスクのパンくずリストを取得します。
   *
   * @param taskId
   *          タスクID
   * @return パンくずリスト
   */
  public static String getTaskTopicPath(Integer projectId) {
    String path = "";
    EipTProject project = getEipTProject(projectId);
    if (project != null) {
      path = project.getProjectName();
    }
    return path;
  }

  public static String getEscapedStringForMysql(String text) {
    if (StringUtils.isEmpty(text)) {
      return text;
    }
    StringBuffer buf = new StringBuffer();
    for (int i = 0; i < text.length(); i++) {
      char c = text.charAt(i);
      switch (c) {
        case 0: /* Must be escaped for 'mysql' */
          buf.append('\\');
          buf.append('0');
          break;
        case '\n': /* Must be escaped for logs */
          buf.append('\\');
          buf.append('n');
          break;
        case '\r':
          buf.append('\\');
          buf.append('r');
          break;
        case '\\':
          buf.append('\\');
          buf.append('\\');
          break;
        case '\'':
          buf.append('\\');
          buf.append('\'');
          break;
        case '"': /* Better safe than sorry */
          buf.append('\\');
          buf.append('"');
          break;
        case '\032': /* This gives problems on Win32 */
          buf.append('\\');
          buf.append('Z');
          break;
        default:
          buf.append(c);
      }
    }
    return buf.toString();
  }

  public static String getLikeEnclosed(String text) {
    return new StringBuffer().append("'%").append(text).append("%'").toString();
  }

  public static String getSingleQuoteEnclosed(String text) {
    return new StringBuffer().append("'").append(text).append("'").toString();
  }

  public static String getCurrentDateWithCast() {
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
    String currentDate = sdf.format(new Date());
    return new StringBuilder()
      .append("CAST(")
      .append(ProjectUtils.getSingleQuoteEnclosed(currentDate))
      .append(" AS DATE)")
      .toString();
  }

  public static Expression getIncompleteExpression() {
    Expression exp = null;
    for (String status : incompleteStatus) {
      Expression tmp =
        ExpressionFactory.matchExp(EipTProjectTask.STATUS_PROPERTY, status);
      if (null == exp) {
        exp = tmp;
      } else {
        exp = exp.orExp(tmp);
      }
    }
    return exp;
  }

  public static String getIncompleteSQL(String column) {
    StringBuilder build = new StringBuilder();
    build.append(" ").append(column).append(" IN (");
    Iterator<String> it = incompleteStatus.iterator();
    while (it.hasNext()) {
      String status = it.next();
      build.append("'").append(status).append("'");
      if (it.hasNext()) {
        build.append(",");
      }
    }
    build.append(") ");
    return build.toString();
  }

  public static List<EipTProjectMember> getProjectMember(Integer projectId) {
    List<EipTProjectMember> result =
      Database.query(EipTProjectMember.class).where(
        Operations.eq("projectId", projectId)).fetchList();
    return result;
  }

  public static Map<String, String> getProjectMemberMap(Integer projectId) {
    Map<String, String> map = new HashMap<String, String>();
    List<EipTProjectMember> result = getProjectMember(projectId);
    if (result.isEmpty()) {
      return map;
    }
    List<Integer> idList = new ArrayList<Integer>();
    for (EipTProjectMember member : result) {
      idList.add(member.getUserId());
    }
    List<ALEipUser> users = getALEipUsers(idList);
    for (EipTProjectMember member : result) {
      for (ALEipUser user : users) {
        if (user.getUserId().getValueWithInt() == member.getUserId().intValue()) {
          map.put(user.getUserId().toString(), user.getAliasName().toString());
          continue;
        }
      }
    }
    return map;
  }

  private static List<TurbineUser> getTurbineUsers(List<Integer> idList) {
    if (idList.isEmpty()) {
      return new ArrayList<TurbineUser>(0);
    }
    SelectQuery<TurbineUser> query = Database.query(TurbineUser.class);
    Expression inExp =
      ExpressionFactory.inDbExp(TurbineUser.USER_ID_PK_COLUMN, idList);
    query.setQualifier(inExp);
    return query.fetchList();
  }

  private static List<ALEipUser> getALEipUsers(List<Integer> idList) {
    List<TurbineUser> list = getTurbineUsers(idList);
    List<ALEipUser> users = new ArrayList<ALEipUser>();
    for (TurbineUser user : list) {
      try {
        users.add(ALEipUtils.getALEipUser(user));
      } catch (ALDBErrorException ignore) {
        // no occur
      }
    }
    return users;
  }
}
