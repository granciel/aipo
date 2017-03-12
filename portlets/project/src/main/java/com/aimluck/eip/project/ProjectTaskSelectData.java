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
package com.aimluck.eip.project;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.jar.Attributes;

import org.apache.cayenne.DataRow;
import org.apache.commons.lang.StringUtils;
import org.apache.jetspeed.services.logging.JetspeedLogFactoryService;
import org.apache.jetspeed.services.logging.JetspeedLogger;
import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;

import com.aimluck.commons.field.ALDateTimeField;
import com.aimluck.eip.cayenne.om.portlet.EipTProjectTask;
import com.aimluck.eip.cayenne.om.portlet.EipTProjectTaskFile;
import com.aimluck.eip.common.ALAbstractMultiFilterSelectData;
import com.aimluck.eip.common.ALDBErrorException;
import com.aimluck.eip.common.ALData;
import com.aimluck.eip.common.ALEipConstants;
import com.aimluck.eip.common.ALEipHolidaysManager;
import com.aimluck.eip.common.ALEipUser;
import com.aimluck.eip.common.ALPageNotFoundException;
import com.aimluck.eip.modules.actions.common.ALAction;
import com.aimluck.eip.orm.Database;
import com.aimluck.eip.orm.query.ResultList;
import com.aimluck.eip.orm.query.SQLTemplate;
import com.aimluck.eip.project.util.ProjectFile;
import com.aimluck.eip.project.util.ProjectFormUtils;
import com.aimluck.eip.project.util.ProjectUtils;
import com.aimluck.eip.services.accessctl.ALAccessControlConstants;
import com.aimluck.eip.util.ALEipUtils;

/**
 * タスクの検索データを管理するクラスです。 <BR>
 *
 */
public class ProjectTaskSelectData extends
    ALAbstractMultiFilterSelectData<EipTProjectTask, EipTProjectTask> implements
    ALData {

  /** logger */
  private static final JetspeedLogger logger = JetspeedLogFactoryService
    .getLogger(ProjectTaskSelectData.class.getName());

  /** タスクの総数 */
  private int taskCount;

  /** 全プロジェクトの一覧 */
  private List<ProjectResultData> allProject;

  /** 選択されているプロジェクトID */
  private Integer selectedProjectId;

  /** ファイル操作オブジェクト */
  private final ProjectFile<EipTProjectTaskFile, EipTProjectTask> pfile =
    new ProjectFile<EipTProjectTaskFile, EipTProjectTask>(
      EipTProjectTaskFile.class);

  /** プロジェクトメンバーリスト */
  private List<ALEipUser> projectMembers;

  /** 基準日From（検索用） */
  private ALDateTimeField base_date_from;

  /** 基準日To（検索用） */
  private ALDateTimeField base_date_to;

  /** キーワード（検索用） */
  private String target_keyword;

  /** 担当者（検索用） */
  private String target_user_id;

  /** 分類（検索用） */
  private String target_tracker;

  /** 優先度（検索用） */
  private String target_priority;

  /** ステータス（検索用） */
  private String target_status;

  /** 進捗率FROM（検索用） */
  private String target_progress_rate_from;

  /** 進捗率TO（検索用） */
  private String target_progress_rate_to;

  /** 進捗遅れフラグ（検索用） */
  private String target_delay;

  /** イナズマ線表示フラグ（検索用） */
  private String progress_line_checked;

  /** 選択最大年 */
  private int viewDateMaxYear;

  /** インデント表示フラグ */
  private boolean indentFlg = true;

  private Calendar calFrom;

  private Calendar calTo;

  /** Android2かどうか */
  private boolean isAndroid2;

  private Integer loginUserId;

  private boolean isFileUploadable;

  /** 添付ファイル追加へのアクセス権限の有無 */
  private boolean hasAttachmentInsertAuthority;

  /**
   * 初期設定
   *
   * @param action
   *          ALAction
   * @param rundata
   *          RunData
   * @param context
   *          Context
   */
  @Override
  public void init(ALAction action, RunData rundata, Context context)
      throws ALPageNotFoundException, ALDBErrorException {

    doCheckAttachmentInsertAclPermission(rundata, context);

    // 検索条件・ソートリセット（プロジェクト選択時）
    String resetCondition = rundata.getParameters().get("reset_condition");
    if (resetCondition != null && resetCondition.length() > 0) {
      ALEipUtils.setTemp(rundata, context, "target_keyword", "");
      ALEipUtils.setTemp(rundata, context, LIST_SORT_STR, "");
      ALEipUtils.setTemp(rundata, context, LIST_SORT_TYPE_STR, "");
    }

    allProject = ProjectUtils.getAllProject(); // 全プロジェクト
    setProject(rundata, context);

    super.init(action, rundata, context);

    // 年
    Calendar cal = Calendar.getInstance();
    viewDateMaxYear = cal.get(Calendar.YEAR) + 10;

    // メンバー
    if (0 != selectedProjectId.intValue()) {
      projectMembers = ProjectUtils.getProjectMembers(selectedProjectId);
    } else {
      projectMembers = ALEipUtils.getUsers("LoginUser");
    }

    // 基準日(From,To)
    calFrom = ProjectUtils.getViewCalendar(rundata, context, "base_date_from");
    calTo = ProjectUtils.getViewCalendar(rundata, context, "base_date_to");
    calFrom.set(Calendar.DAY_OF_MONTH, 1);
    calTo.set(Calendar.DAY_OF_MONTH, calTo.getActualMaximum(Calendar.DATE));
    base_date_from = new ALDateTimeField();
    base_date_to = new ALDateTimeField();
    base_date_from.setValue(calFrom.getTime());
    base_date_to.setValue(calTo.getTime());
    // イナズマ線表示
    progress_line_checked =
      ProjectUtils.getParameter(rundata, context, "progress_line_checked");
    // ユーザーエージェントがAndroid2ならTrue
    isAndroid2 = ALEipUtils.isAndroid2Browser(rundata);

    loginUserId = ALEipUtils.getUserId(rundata);

    isFileUploadable = ALEipUtils.isFileUploadable(rundata);

  }

  /**
   * 一覧データを取得します。 <BR>
   *
   * @param rundata
   *          RunData
   * @param context
   *          Context
   * @return ResultList
   */
  @Override
  protected ResultList<EipTProjectTask> selectList(RunData rundata,
      Context context) {

    if (null == selectedProjectId) {
      return null;
    }

    setSessionParams(rundata, context);

    SQLTemplate<EipTProjectTask> sqltemp = null;
    SQLTemplate<EipTProjectTask> sqlCountTemp = null;

    sqltemp =
      Database.sql(EipTProjectTask.class, getFetchQuery(rundata, context));
    sqlCountTemp =
      Database.sql(EipTProjectTask.class, getCountQuery(rundata, context));
    setPostgresParams(sqltemp, sqlCountTemp);

    ResultList<EipTProjectTask> list = new ResultList<EipTProjectTask>();

    // 取得値を設定する
    List<DataRow> result = sqltemp.fetchListAsDataRow();
    for (int i = 0; i < result.size(); i++) {

      DataRow row = result.get(i);

      Object parentTaskId = row.get("parent_task_id");
      Object explanation = row.get("explanation");
      Object startPlanDate = row.get("start_plan_date");
      Object endPlanDate = row.get("end_plan_date");
      Object startDate = row.get("start_date");
      Object endDate = row.get("end_date");
      Object updateDate = row.get("update_date");

      EipTProjectTask task = new EipTProjectTask();
      // タスクID
      task.setTaskId(row.get("task_id").toString());
      // タスク名
      task.setTaskName(row.get("task_name").toString());
      // 親タスクID
      if (parentTaskId != null) {
        task.setParentTaskId(Integer.valueOf(parentTaskId.toString()));
      }
      // プロジェクトID
      task.setProjectId(Integer.valueOf(row.get("project_id").toString()));
      // プロジェクト名
      task.setProjectName(row.get("project_name").toString());
      // 分類
      task.setTracker(row.get("tracker").toString());
      // 説明
      if (explanation != null) {
        task.setExplanation(explanation.toString());
      }
      // ステータス
      task.setStatus(row.get("status").toString());
      // 優先度
      task.setPriority(row.get("priority").toString());
      // 開始予定日
      if (startPlanDate == null) {
        task.setStartPlanDate(ProjectUtils.getEmptyDate());
      } else {
        task.setStartPlanDate((Date) startPlanDate);
      }
      // 完了予定日
      if (endPlanDate == null) {
        task.setEndPlanDate(ProjectUtils.getEmptyDate());
      } else {
        task.setEndPlanDate((Date) endPlanDate);
      }
      // 開始実績日
      if (startDate == null) {
        task.setStartDate(ProjectUtils.getEmptyDate());
      } else {
        task.setStartDate((Date) startDate);
      }
      // 完了日
      if (endDate == null) {
        task.setEndDate(ProjectUtils.getEmptyDate());
      } else {
        task.setEndDate((Date) endDate);
      }
      // 計画工数
      task.setPlanWorkload(new BigDecimal(row.get("plan_workload").toString()));
      // 進捗率
      task
        .setProgressRate(Integer.valueOf(row.get("progress_rate").toString()));
      // 更新日
      task.setUpdateDate((Date) updateDate);
      // タスク名表示インデント
      task.setIndent(Integer.valueOf(row.get("indent").toString()));

      list.add(task);
    }

    // 全体の件数
    int count = 0;
    List<DataRow> countResult = sqlCountTemp.fetchListAsDataRow();
    for (DataRow row : countResult) {
      Long tmp = (Long) row.get("count");
      count = tmp != null ? tmp.intValue() : 0;
    }

    // 件数をセットする．
    setPageParam(count);
    taskCount = count;

    // インデント表示フラグ
    if (!getWhereList().isEmpty()) {
      indentFlg = false;
    }

    return list;
  }

  private void setSessionParams(RunData rundata, Context context) {
    // キーワード
    target_keyword =
      ProjectUtils.getParameter(rundata, context, "target_keyword");
    // 担当者
    target_user_id =
      ProjectUtils.getParameter(rundata, context, "target_user_id");
    // 分類
    target_tracker =
      ProjectUtils.getParameter(rundata, context, "target_tracker");
    // 優先度
    target_priority =
      ProjectUtils.getParameter(rundata, context, "target_priority");
    // ステータス
    target_status =
      ProjectUtils.getParameter(rundata, context, "target_status");
    // 進捗率FROM
    target_progress_rate_from =
      ProjectUtils.getParameter(rundata, context, "target_progress_rate_from");
    // 進捗率TO
    target_progress_rate_to =
      ProjectUtils.getParameter(rundata, context, "target_progress_rate_to");
    // 進捗遅れ
    target_delay = ProjectUtils.getParameter(rundata, context, "target_delay");

    /** for sql injection */
    {
      if (isRejectParameter(target_user_id)) {
        target_user_id = "0";
      }

      if (isRejectParameter(target_tracker)) {
        target_tracker = "0";
      }

      if (isRejectParameter(target_priority)) {
        target_priority = "0";
      }

      if (isRejectParameter(target_status)) {
        target_status = "0";
      }

      if (isRejectParameter(target_progress_rate_from)) {
        target_progress_rate_from = "0";
      }

      if (isRejectParameter(target_progress_rate_to)) {
        target_progress_rate_to = "0";
      }
    }
  }

  private String getCommonSQL() {
    // -------------------------
    // 共通SELECT句
    // -------------------------
    StringBuilder sb = new StringBuilder();
    sb.append("    FROM");
    sb.append("      eip_t_project_task AS task, eip_t_project AS project");
    sb.append("    WHERE ");
    sb.append("      task.project_id = project.project_id AND ");
    if (0 != selectedProjectId) {
      sb.append(" task.project_id = #bind($project_id) AND ");
    }
    sb.append("       task.parent_task_id IS NULL");
    return sb.toString();
  }

  private String getFetchQuery(RunData rundata, Context context) {

    StringBuilder sb = new StringBuilder();
    sb.append("SELECT");
    sb.append(" 0 AS indent");
    sb.append(", task.task_id"); // タスクID
    sb.append(", task.task_name"); // タスク名
    sb.append(", task.parent_task_id");// 親タスクID
    sb.append(", task.project_id");// プロジェクトID
    sb.append(", project.project_name");
    sb.append(", task.tracker");// 分類
    sb.append(", task.explanation"); // 説明
    sb.append(", task.status"); // ステータス
    sb.append(", task.priority"); // 優先度
    sb.append(", task.start_plan_date"); // 開始予定日
    sb.append(", task.end_plan_date"); // 完了予定日
    sb.append(", task.start_date"); // 開始実績日
    sb.append(", task.end_date"); // 完了実績日
    sb.append(", task.plan_workload"); // 計画工数
    sb.append(", task.progress_rate"); // 進捗率
    sb.append(", task.update_date"); // 更新日
    sb.append(getCommonSQL());

    List<String> whereList = getWhereList();
    // WHERE句セット
    for (int i = 0; i < whereList.size(); i++) {
      sb.append(" AND ");
      sb.append(whereList.get(i));
    }

    sb.append(getOrderBy(rundata, context));
    sb.append(" LIMIT ").append(getRowsNum()).append(" OFFSET ").append(
      (current_page - 1) * getRowsNum());
    return sb.toString();
  }

  private String getCountQuery(RunData rundata, Context context) {
    StringBuilder sb = new StringBuilder();
    sb.append("select count(task_id) AS count ");
    sb.append(getCommonSQL());

    List<String> whereList = getWhereList();
    // WHERE句セット
    for (int i = 0; i < whereList.size(); i++) {
      sb.append(" AND ");
      sb.append(whereList.get(i));
    }

    return sb.toString();
  }

  private List<String> getWhereList() {
    List<String> whereList = new ArrayList<String>();
    // キーワード
    if (target_keyword != null && target_keyword.trim().length() > 0) {
      if (0 == selectedProjectId) {
        whereList
          .add(" (task.task_name LIKE #bind($target_keyword) OR project.project_name LIKE #bind($target_keyword))");
      } else {
        whereList.add(" task.task_name LIKE #bind($target_keyword)");
      }

    }
    // 担当者
    if (StringUtils.isNotEmpty(target_user_id) && !target_user_id.equals("all")) {
      StringBuilder where = new StringBuilder();
      where.append(" EXISTS(");
      where.append("   SELECT 0");
      where.append("     FROM eip_t_project_task_member AS member");
      where.append("    WHERE member.task_id = task.task_id");
      where.append("      AND member.user_id = #bind($target_user_id)");
      where.append(" )");
      whereList.add(String.valueOf(where));
    }
    // 分類
    if (StringUtils.isNotEmpty(target_tracker) && !target_tracker.equals("all")) {
      whereList.add(" task.tracker = #bind($target_tracker)");
    }
    // 優先度
    if (StringUtils.isNotEmpty(target_priority)
      && !target_priority.equals("all")) {
      whereList.add(" task.priority = #bind($target_priority)");
    }
    // ステータス
    if (StringUtils.isNotEmpty(target_status) && !target_status.equals("all")) {
      whereList.add(" task.status = #bind($target_status)");
    }
    // 進捗率FROM
    if (StringUtils.isNotEmpty(target_progress_rate_from)
      && !target_progress_rate_from.equals("0")) {
      whereList.add(" task.progress_rate >= #bind($target_progress_rate_from)");
    }
    // 進捗率TO
    if (StringUtils.isNotEmpty(target_progress_rate_to)
      && !target_progress_rate_to.equals("100")) {
      whereList.add(" task.progress_rate <= #bind($target_progress_rate_to)");
    }
    // 進捗遅れ
    if (StringUtils.isNotEmpty(target_delay)
      && target_delay.equals(ProjectUtils.FLG_ON)) {

      StringBuilder task_days = new StringBuilder();
      task_days
        .append(" CASE WHEN task.end_plan_date - task.start_plan_date + 1 < 0");
      task_days.append("    THEN 0");
      task_days
        .append("    ELSE task.end_plan_date - task.start_plan_date + 1");
      task_days.append("  END "); // タスク日数

      StringBuilder lapsed_days = new StringBuilder();
      lapsed_days.append(" CASE");
      lapsed_days
        .append("    WHEN task.start_plan_date IS NULL OR task.end_plan_date IS NULL");
      lapsed_days.append("      THEN 0");
      lapsed_days.append("    WHEN ").append(
        ProjectUtils.getCurrentDateWithCast()).append(" < task.end_plan_date");
      lapsed_days.append("      THEN");
      lapsed_days.append("        CASE WHEN ").append(
        ProjectUtils.getCurrentDateWithCast()).append(
        " - task.start_plan_date + 1 < 0");
      lapsed_days.append("          THEN 0");
      lapsed_days.append("          ELSE ").append(
        ProjectUtils.getCurrentDateWithCast()).append(
        " - task.start_plan_date + 1");
      lapsed_days.append("        END");
      lapsed_days.append("      ELSE");
      lapsed_days
        .append("        task.end_plan_date - task.start_plan_date + 1");
      lapsed_days.append("  END "); // 基準日までのタスク経過日数

      StringBuilder sb = new StringBuilder();

      sb.append(" ( ");
      sb.append(" ( " + task_days + " ) ");
      sb.append("  <> 0 AND ( ");
      sb.append(" ( " + lapsed_days + " ) ");
      sb.append(" * 100 / ");
      sb.append(" ( " + task_days + " ) ");
      sb.append(" > task.progress_rate ");
      sb.append(" ) ");
      sb.append(" ) ");
      whereList.add(sb.toString());
    }
    return whereList;
  }

  private void setPostgresParams(SQLTemplate<EipTProjectTask> sqltemp,
      SQLTemplate<EipTProjectTask> sqlCountTemp) {
    sqltemp.param("project_id", selectedProjectId);
    sqlCountTemp.param("project_id", selectedProjectId);

    // 分類
    if (StringUtils.isNotEmpty(target_keyword)
      && target_keyword.trim().length() > 0) {
      sqltemp.param("target_keyword", "%" + target_keyword + "%");
      sqlCountTemp.param("target_keyword", "%" + target_keyword + "%");
    }
    // 担当者
    if (StringUtils.isNotEmpty(target_user_id) && !target_user_id.equals("all")) {
      sqltemp.param("target_user_id", Integer.valueOf(target_user_id));
      sqlCountTemp.param("target_user_id", Integer.valueOf(target_user_id));
    }
    // 分類
    if (StringUtils.isNotEmpty(target_tracker) && !target_tracker.equals("all")) {
      sqltemp.param("target_tracker", target_tracker);
      sqlCountTemp.param("target_tracker", target_tracker);
    }
    // 優先度
    if (StringUtils.isNotEmpty(target_priority)
      && !target_priority.equals("all")) {
      sqltemp.param("target_priority", target_priority);
      sqlCountTemp.param("target_priority", target_priority);
    }
    // ステータス
    if (StringUtils.isNotEmpty(target_status) && !target_status.equals("all")) {
      sqltemp.param("target_status", target_status);
      sqlCountTemp.param("target_status", target_status);
    }
    // 進捗率FROM
    if (StringUtils.isNotEmpty(target_progress_rate_from)
      && !target_progress_rate_from.equals("0")) {
      sqltemp.param("target_progress_rate_from", Integer
        .valueOf(target_progress_rate_from));
      sqlCountTemp.param("target_progress_rate_from", Integer
        .valueOf(target_progress_rate_from));
    }
    // 進捗率TO
    if (StringUtils.isNotEmpty(target_progress_rate_to)
      && !target_progress_rate_to.equals("100")) {
      sqltemp.param("target_progress_rate_to", Integer
        .valueOf(target_progress_rate_to));
      sqlCountTemp.param("target_progress_rate_to", Integer
        .valueOf(target_progress_rate_to));
    }
  }

  /**
   * ソート句を構築します。
   *
   * @param crt
   * @return
   */
  protected String getOrderBy(RunData rundata, Context context) {

    String sort = ALEipUtils.getTemp(rundata, context, LIST_SORT_STR);
    String sort_type = ALEipUtils.getTemp(rundata, context, LIST_SORT_TYPE_STR);
    String crt_key = null;

    Attributes map = getColumnMap();
    if (sort != null && sort.length() > 0) {
      crt_key = map.getValue(sort);
    }

    current_sort = sort;
    current_sort_type = sort_type;

    if (crt_key != null && crt_key.length() > 0) {

      // インデントを非表示
      indentFlg = false;

      if (sort_type != null
        && ALEipConstants.LIST_SORT_TYPE_DESC.equals(sort_type)) {
        return "  ORDER BY " + crt_key + " DESC";
      } else {
        return "  ORDER BY " + crt_key;
      }

    } else {
      return "  ORDER BY order_no";
    }
  }

  /**
   * 詳細データを取得します。 <BR>
   *
   * @param rundata
   *          RunData
   * @param context
   *          Context
   * @return 汎用データベースオブジェクトモデル
   */
  @Override
  protected EipTProjectTask selectDetail(RunData rundata, Context context) {
    // オブジェクトモデルを取得
    return ProjectUtils.getEipTProjectTask(rundata, context);
  }

  /**
   * ResultDataを取得します。（一覧データ） <BR>
   *
   * @param record
   *          レコード
   * @return ResultData
   */
  @Override
  protected Object getResultData(EipTProjectTask record) {
    ProjectTaskResultData data = ProjectUtils.getProjectTaskResultData(record);
    Integer taskId = (int) data.getTaskId().getValue();

    // 子タスク件数
    int cntChild = ProjectUtils.getCountChildrenTask(taskId);

    // 子タスクが2件以上あればtrue
    // 並び替えボタン表示用
    data.setHasChildren(cntChild >= 2);

    // 進捗更新用
    data.setHasChildrenForForm(cntChild > 0);

    // 経過タスク日数
    int lapsedDays =
      ProjectUtils.getLapsedDays(ProjectUtils.toString(record
        .getStartPlanDate()), ProjectUtils.toString(Calendar
        .getInstance()
        .getTime()));
    // 予定タスク日数
    int taskDays =
      ProjectUtils.getLapsedDays(ProjectUtils.toString(record
        .getStartPlanDate()), ProjectUtils.toString(record.getEndPlanDate()));
    data.setPlanTerm(taskDays);

    if (lapsedDays > taskDays) {
      // 経過タスク日数≦タスク日数にする
      lapsedDays = taskDays;
    }

    // 予定進捗率
    data
      .setPlanProgressRate(ProjectUtils.getPlanWorkload(lapsedDays, taskDays));

    // 実績工数
    List<ProjectTaskMemberResultData> memberList = data.getMemberList();
    BigDecimal workload = BigDecimal.valueOf(0);
    workload = workload.setScale(1);
    for (int i = 0; i < memberList.size(); i++) {
      ProjectTaskMemberResultData member = memberList.get(i);
      workload = workload.add(member.getWorkload());
    }
    data.setWorkload(workload);

    // 完了予測工数
    BigDecimal forecastWorkload = BigDecimal.valueOf(0);
    if (data.getProgressRate().getValue() != 0) {
      forecastWorkload =
        workload.multiply(BigDecimal.valueOf(100)).divide(
          BigDecimal.valueOf(data.getProgressRate().getValue()),
          2,
          BigDecimal.ROUND_HALF_UP);
    }
    data.setForecastWorkload(forecastWorkload);

    // インデント表示フラグ
    data.setIndentFlg(indentFlg);

    data.setEditable(ProjectFormUtils.isEditable(taskId, loginUserId));

    return data;
  }

  /**
   * ResultDataを取得します。（詳細データ） <BR>
   *
   * @param record
   *          レコード
   * @return ResultData
   */
  @Override
  protected Object getResultDataDetail(EipTProjectTask record) {
    ProjectTaskResultData data = ProjectUtils.getProjectTaskResultData(record);
    int taskId = (int) data.getTaskId().getValue();
    if (hasAttachmentAuthority()) {
      // ファイルリスト
      List<EipTProjectTaskFile> list =
        pfile
          .getSelectQueryForFiles(EipTProjectTask.TASK_ID_PK_COLUMN, taskId)
          .fetchList();
      data.setAttachmentFiles(pfile.getFileList(list));
    }
    // コメントリスト
    data.setCommentList(ProjectUtils.getProjectTaskCommentList(
      "" + taskId,
      hasAttachmentAuthority()));
    // パンくずリスト
    data.setTopicPath(ProjectUtils.getTaskTopicPath(record.getProjectId()));
    // ログインユーザーID
    data.setLoginUserId(loginUserId.longValue());
    return data;
  }

  /**
   * 項目情報を取得する
   *
   * @return 項目情報
   */
  @Override
  protected Attributes getColumnMap() {
    Attributes map = new Attributes();
    map.putValue("task_name", "task_name");
    map.putValue("project_name", "project_name");
    map.putValue("tracker", "tracker");
    map.putValue("status", "status");
    map.putValue("workload", "workload");
    map.putValue("plan_workload", "plan_workload");
    map.putValue("start_plan_date", "start_plan_date");
    map.putValue("end_plan_date", "end_plan_date");
    map.putValue("progress_rate", "progress_rate");
    return map;
  }

  /**
   * プロジェクトの総数を取得する
   *
   * @return プロジェクト総数
   */
  public int getTaskCount() {
    return taskCount;
  }

  /**
   * 全プロジェクトのリストを返す
   *
   * @return 全プロジェクトのリスト
   */
  public List<ProjectResultData> getAllProject() {
    return allProject;
  }

  /**
   * 選択されているプロジェクトIDを取得する
   *
   * @return プロジェクトID
   */
  public Integer getSelectedProjectId() {
    return selectedProjectId;
  }

  /**
   * プロジェクトメンバーリストを取得します。
   *
   * @return プロジェクトメンバーリスト
   */
  public List<ALEipUser> getProjectMembers() {
    return projectMembers;
  }

  /**
   * 検索したキーワードを取得します。
   *
   * @return 検索キーワード
   */
  public String getTargetKeyword() {
    return target_keyword;
  }

  /**
   * 検索した担当者を取得します。
   *
   * @return 検索担当者
   */
  public String getTargetUserId() {
    return target_user_id;
  }

  public String getTargetUserName() {
    if (!"".equals(getTargetUserId()) && getTargetUserId() != null) {
      try {
        return ALEipUtils
          .getALEipUser(Integer.parseInt(target_user_id))
          .getAliasName()
          .toString();
      } catch (Exception e) {
        return null;

      }
    } else {
      return null;
    }
  }

  /**
   * 検索した分類を取得します。
   *
   * @return 検索分類
   */
  public String getTargetTracker() {
    return target_tracker;
  }

  public String getTargetTrackerName() {
    if (!"".equals(getTargetTracker()) && getTargetTracker() != null) {
      return getTrackerMap().get(target_tracker);
    } else {
      return null;
    }
  }

  /**
   * 検索した優先度を取得します。
   *
   * @return 検索優先度
   */
  public String getTargetPriority() {
    return target_priority;
  }

  public String getTargetPriorityName() {
    if (!"".equals(getTargetPriority()) && getTargetPriority() != null) {
      return getPriorityMap().get(target_priority);
    } else {
      return null;
    }
  }

  /**
   * 検索したステータスを取得します。
   *
   * @return 検索ステータス
   */
  public String getTargetStatus() {
    return target_status;
  }

  public String getTargetStatusName() {
    if (!"".equals(getTargetStatus()) && getTargetStatus() != null) {
      return getStatusMap().get(target_status);
    } else {
      return null;
    }
  }

  /**
   * 検索した進捗率FROMを取得します。
   *
   * @return 進捗率FROM
   */
  public String getTargetProgressRateFrom() {
    return target_progress_rate_from;
  }

  /**
   * 検索した進捗率TOを取得します。
   *
   * @return 進捗率TO
   */
  public String getTargetProgressRateTo() {
    return target_progress_rate_to;
  }

  /**
   * 検索した進捗遅れフラグを取得します。
   *
   * @return 進捗遅れフラグ
   */
  public String getTargetDelay() {
    return target_delay;
  }

  /**
   * イナズマ線表示フラグを取得します。
   *
   * @return イナズマ線表示フラグ
   */
  public String getProgressLineChecked() {
    return progress_line_checked;
  }

  /**
   * Android2かどうかを返します。
   *
   * @return Android2かどうか
   */
  public boolean isAndroid2() {
    return isAndroid2;
  }

  /**
   * 日付の年最大値を取得する。
   *
   * @return 年最大値
   */
  public int getViewDateMaxYear() {
    return viewDateMaxYear;
  }

  /**
   * 基準日Fromを取得する。
   *
   * @return 基準日From
   */
  public ALDateTimeField getBaseDateFrom() {
    return base_date_from;
  }

  /**
   * 基準日Toを取得する。
   *
   * @return 基準日To
   */
  public ALDateTimeField getBaseDateTo() {
    return base_date_to;
  }

  /**
   * 基準日Fromの年を取得する。
   *
   * @return 基準日Fromの年
   */
  public int getBaseDateFromYear() {
    return calFrom.get(Calendar.YEAR);
  }

  /**
   * 基準日Toの年を取得する。
   *
   * @return 基準日Toの年
   */
  public int getBaseDateToYear() {
    return calTo.get(Calendar.YEAR);
  }

  /**
   * 加算日数を加えた日を取得する。
   *
   * @return 加算後の日
   */
  public int getDay(int days) {
    Calendar cal = (Calendar) calFrom.clone();
    cal.add(Calendar.DAY_OF_MONTH, days - 1);
    return cal.get(Calendar.DAY_OF_MONTH);
  }

  /**
   * 加算日数を加えた日がシステム日付かチェックする。
   *
   * @return TRUE:システム日付である
   */
  public boolean isToday(int days) {
    Calendar today = Calendar.getInstance();
    Calendar cal = (Calendar) calFrom.clone();
    cal.add(Calendar.DAY_OF_MONTH, days - 1);

    return today.get(Calendar.YEAR) == cal.get(Calendar.YEAR)
      && today.get(Calendar.MONTH) == cal.get(Calendar.MONTH)
      && today.get(Calendar.DAY_OF_MONTH) == cal.get(Calendar.DAY_OF_MONTH);
  }

  /**
   * 表示年月の日数を取得する。
   *
   * @return 日数
   */
  public int getDays() {
    return ProjectUtils.getLapsedDays(calFrom, calTo);
  }

  /**
   * 表示年月の日数を取得する。
   *
   * @return 日数
   */
  public int getDays(Date dateTo) {
    Calendar cal = Calendar.getInstance();
    cal.setTime(dateTo);
    return ProjectUtils.getLapsedDays(calFrom, cal);
  }

  /**
   * 年を取得する。
   *
   * @return 週番号
   */
  public int getYear(int days) {
    Calendar cal = (Calendar) calFrom.clone();
    cal.add(Calendar.DAY_OF_MONTH, days - 1);
    return cal.get(Calendar.YEAR);
  }

  /**
   * 月を取得する。
   *
   * @return 月
   */
  public int getMonth(int days) {
    Calendar cal = (Calendar) calFrom.clone();
    cal.add(Calendar.DAY_OF_MONTH, days - 1);
    return cal.get(Calendar.MONTH);
  }

  /**
   * 週を取得する。
   *
   * @return 週番号
   */
  public int getWeek(int days) {
    Calendar cal = (Calendar) calFrom.clone();
    cal.add(Calendar.DAY_OF_MONTH, days - 1);
    return cal.get(Calendar.WEEK_OF_YEAR);
  }

  /**
   * 分類リストを返します。
   *
   * @return 分類リスト
   */
  public Map<String, String> getTrackerMap() {
    return ProjectUtils.getKubunMap("tracker");
  }

  /**
   * ステータスリストを返します。
   *
   * @return ステータスリスト
   */
  public Map<String, String> getStatusMap() {
    return ProjectUtils.getKubunMap("status");
  }

  /**
   * 優先度リストを返します。
   *
   * @return 優先度リスト
   */
  public Map<String, String> getPriorityMap() {
    return ProjectUtils.getKubunMap("priority");
  }

  /**
   * 現在時刻の long 値を返します。
   *
   * @return 現在時刻の long 値
   */
  public long getNowTime() {
    return Calendar.getInstance().getTimeInMillis();
  }

  /**
   * 加算日数を加えた日付が休日かをチェックする。
   *
   * @return TRUE:加算後の日が休日
   */

  public boolean isHoliday(int days) {
    Calendar cal = (Calendar) calFrom.clone();
    cal.add(Calendar.DAY_OF_MONTH, days - 1);

    // 祝日かどうかを検証する．
    ALEipHolidaysManager holidaysManager = ALEipHolidaysManager.getInstance();
    return holidaysManager.isHoliday(cal.getTime()) != null;
  }

  /**
   * 加算日数を加えた日付が土曜日かをチェックする。
   *
   * @return TRUE:加算後の日が土曜
   */

  public boolean isSaturday(int days) {
    Calendar cal = (Calendar) calFrom.clone();
    cal.add(Calendar.DAY_OF_MONTH, days - 1);
    return Calendar.SATURDAY == cal.get(Calendar.DAY_OF_WEEK);
  }

  /**
   * 加算日数を加えた日付が土曜日かをチェックする。
   *
   * @return TRUE:加算後の日が日曜
   */

  public boolean isSunday(int days) {
    Calendar cal = (Calendar) calFrom.clone();
    cal.add(Calendar.DAY_OF_MONTH, days - 1);
    return Calendar.SUNDAY == cal.get(Calendar.DAY_OF_WEEK);
  }

  /**
   * ビュータイプを取得する
   *
   * @return ビュータイプ
   */
  public String getViewtype() {
    return "project";
  }

  private boolean isRejectParameter(String numeric) {
    return StringUtils.isNotEmpty(numeric)
      && !"all".equals(numeric)
      && !StringUtils.isNumeric(numeric);
  }

  public void setProject(RunData rundata, Context context) {
    // validate projectId and reset projectId

    String filter = rundata.getParameters().getString("filter", "");
    String filterType = rundata.getParameters().getString("filtertype", "");
    String sesFilter = ALEipUtils.getTemp(rundata, context, LIST_FILTER_STR);
    String sesFilterType =
      ALEipUtils.getTemp(rundata, context, LIST_FILTER_TYPE_STR);

    sesFilter = sesFilter == null ? "" : sesFilter;
    sesFilterType = sesFilterType == null ? "" : sesFilterType;

    if (filterType.isEmpty()) {
      filter = sesFilter;
      filterType = sesFilterType;
    }

    if (StringUtils.isEmpty(filter) || StringUtils.isEmpty(filterType)) {
      selectedProjectId = 0;
      setFilter(rundata, context);
      return;
    }

    boolean existProject = false;
    for (ProjectResultData data : allProject) {
      if (filter.equals(data.getProjectId().toString())) {
        existProject = true;
        break;
      }
    }

    if (!existProject) {
      selectedProjectId = 0;
    } else {
      selectedProjectId = Integer.valueOf(filter);
    }
    setFilter(rundata, context);

  }

  private void setFilter(RunData rundata, Context context) {
    ALEipUtils.setTemp(rundata, context, LIST_FILTER_STR, String
      .valueOf(selectedProjectId));
    ALEipUtils.setTemp(rundata, context, LIST_FILTER_TYPE_STR, "project_id");
  }

  /**
   * @return isFileUploadable
   */
  public boolean isFileUploadable() {
    return isFileUploadable;
  }

  /**
   * ファイルアップロードのアクセス権限をチェックします。
   *
   * @return
   */
  protected void doCheckAttachmentInsertAclPermission(RunData rundata,
      Context context) { // ファイル追加権限の有無
    hasAttachmentInsertAuthority =
      doCheckAttachmentAclPermission(
        rundata,
        context,
        ALAccessControlConstants.VALUE_ACL_INSERT);
  }

  public boolean hasAttachmentInsertAuthority() {
    return hasAttachmentInsertAuthority;
  }
}
