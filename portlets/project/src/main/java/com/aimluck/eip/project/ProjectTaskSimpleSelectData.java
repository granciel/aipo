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
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.jar.Attributes;

import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.commons.lang.StringUtils;
import org.apache.jetspeed.portal.portlets.VelocityPortlet;
import org.apache.jetspeed.services.logging.JetspeedLogFactoryService;
import org.apache.jetspeed.services.logging.JetspeedLogger;
import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;

import com.aimluck.commons.field.ALDateTimeField;
import com.aimluck.eip.cayenne.om.portlet.EipTProjectMember;
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
import com.aimluck.eip.orm.query.SelectQuery;
import com.aimluck.eip.project.util.ProjectFile;
import com.aimluck.eip.project.util.ProjectUtils;
import com.aimluck.eip.util.ALEipUtils;

/**
 * タスクの検索データを管理するクラスです。 <BR>
 *
 */
public class ProjectTaskSimpleSelectData extends
    ALAbstractMultiFilterSelectData<EipTProjectTask, EipTProjectTask> implements
    ALData {

  /** logger */
  private static final JetspeedLogger logger = JetspeedLogFactoryService
    .getLogger(ProjectTaskSimpleSelectData.class.getName());

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
  private final boolean indentFlg = true;

  private Calendar calFrom;

  private Calendar calTo;

  private boolean topView = false;

  private Integer loginUserId = null;

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

    loginUserId = ALEipUtils.getUserId(rundata);
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
    try {
      if (null == selectedProjectId) {
        return null;
      }

      setSessionParams(rundata, context);
      SelectQuery<EipTProjectTask> query = getSelectQuery(rundata, context);
      buildSelectQueryForListView(query);
      buildSelectQueryForListViewSortOrder(query, rundata, context);
      // 表示するカラムのみデータベースから取得する．
      ResultList<EipTProjectTask> list = query.getResultList();
      // 件数をセットする．
      taskCount = list.getTotalCount();
      return list;
    } catch (Exception ex) {
      logger.error("ProjectTaskSimpleSelectData", ex);
      return null;
    }
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

  /**
   * 検索条件を設定した SelectQuery を返します。 <BR>
   *
   * @param rundata
   * @param context
   * @return
   */

  private SelectQuery<EipTProjectTask> getSelectQuery(RunData rundata,
      Context context) {

    SelectQuery<EipTProjectTask> query = Database.query(EipTProjectTask.class);
    query.setQualifier(ExpressionFactory.matchExp(
      EipTProjectTask.PARENT_TASK_ID_PROPERTY,
      null));

    // プロジェクト
    if (0 != selectedProjectId) {
      Expression ex1 =
        ExpressionFactory.matchExp(
          EipTProjectTask.PROJECT_ID_PROPERTY,
          selectedProjectId);
      query.andQualifier(ex1);
    }

    // キーワード
    if (target_keyword != null && target_keyword.trim().length() > 0) {
      Expression ex1 =
        ExpressionFactory.likeExp(EipTProjectTask.TASK_NAME_PROPERTY, "%"
          + target_keyword
          + "%");
      query.andQualifier(ex1);
    }

    // 担当者
    if (StringUtils.isNotEmpty(target_user_id) && !target_user_id.equals("all")) {
      Expression exp2 =
        ExpressionFactory.matchExp(
          EipTProjectTask.EIP_TPROJECT_TASK_MEMBER_PROPERTY
            + "."
            + EipTProjectMember.USER_ID_PROPERTY,
          Integer.valueOf(target_user_id));
      query.andQualifier(exp2);
    }

    // 分類
    if (StringUtils.isNotEmpty(target_tracker) && !target_tracker.equals("all")) {
      Expression ex3 =
        ExpressionFactory.matchExp(
          EipTProjectTask.TRACKER_PROPERTY,
          target_tracker);
      query.andQualifier(ex3);
    }

    // 優先度
    if (StringUtils.isNotEmpty(target_priority)
      && !target_priority.equals("all")) {
      Expression ex4 =
        ExpressionFactory.matchExp(
          EipTProjectTask.PRIORITY_PROPERTY,
          target_priority);
      query.andQualifier(ex4);
    }

    // ステータス
    if (StringUtils.isNotEmpty(target_status) && !target_status.equals("all")) {
      Expression ex5 =
        ExpressionFactory.matchExp(
          EipTProjectTask.STATUS_PROPERTY,
          target_status);
      query.andQualifier(ex5);
    }

    // 進捗率FROM
    if (StringUtils.isNotEmpty(target_progress_rate_from)
      && !target_progress_rate_from.equals("0")) {
      Expression ex6 =
        ExpressionFactory.greaterOrEqualExp(
          EipTProjectTask.PROGRESS_RATE_PROPERTY,
          Integer.valueOf(target_progress_rate_from));
      query.andQualifier(ex6);
    }

    // 進捗率TO
    if (StringUtils.isNotEmpty(target_progress_rate_to)
      && !target_progress_rate_to.equals("100")) {
      Expression ex7 =
        ExpressionFactory.lessOrEqualExp(
          EipTProjectTask.PROGRESS_RATE_PROPERTY,
          Integer.valueOf(target_progress_rate_to));
      query.andQualifier(ex7);
    }

    // 進捗遅れ
    if (StringUtils.isNotEmpty(target_delay)
      && target_delay.equals(ProjectUtils.FLG_ON)) {
      /**
       * CURRENT_DATE in SQL depend on Database locale, so unify the time in
       * java
       */
      // @Todo fix

      StringBuilder sl = new StringBuilder();
      sl.append("(");
      sl
        .append("( CASE WHEN task.end_plan_date - task.start_plan_date + 1 < 0");
      sl.append("    THEN 0");
      sl.append("    ELSE task.end_plan_date - task.start_plan_date + 1");
      sl.append("  END)"); // タスク日数
      sl.append(" <> 0 ");
      sl.append(" AND ");
      sl.append("(");

      sl.append("    ( CASE");
      sl
        .append("    WHEN task.start_plan_date IS NULL OR task.end_plan_date IS NULL");
      sl.append("      THEN 0");
      sl
        .append("    WHEN ")
        .append(ProjectUtils.getCurrentDateWithCast())
        .append(" < task.end_plan_date");
      sl.append("      THEN");
      sl.append("        CASE WHEN ").append(
        ProjectUtils.getCurrentDateWithCast()).append(
        " - task.start_plan_date + 1 < 0");
      sl.append("          THEN 0");
      sl
        .append("          ELSE ")
        .append(ProjectUtils.getCurrentDateWithCast())
        .append(" - task.start_plan_date + 1");
      sl.append("        END");
      sl.append("      ELSE");
      sl.append("        task.end_plan_date - task.start_plan_date + 1");
      sl.append("  END )"); // 基準日までのタスク経過日数
      sl.append("  * 100 / ");
      sl
        .append(" ( CASE WHEN task.end_plan_date - task.start_plan_date + 1 < 0");
      sl.append("    THEN 0");
      sl.append("    ELSE task.end_plan_date - task.start_plan_date + 1");
      sl.append("  END ) "); // タスク日数
      sl.append("   > progress_rate ");
      Expression exp = Expression.fromString(sl.toString());
      query.andQualifier(exp);
    }

    if (topView) {
      /** ノーマル画面では新規、進行中、フィードバック、担当しているタスクのみ表示する */
      Expression exp9 =
        ExpressionFactory.matchExp(
          EipTProjectTask.EIP_TPROJECT_TASK_MEMBER_PROPERTY
            + "."
            + EipTProjectMember.USER_ID_PROPERTY,
          Integer.valueOf(loginUserId));
      query.andQualifier(exp9);
      Expression exp10 =
        ExpressionFactory.inExp(
          EipTProjectTask.STATUS_PROPERTY,
          ProjectUtils.incompleteStatus);
      query.andQualifier(exp10);
    }

    return query;
  }

  /**
   * ソート句を構築します。
   *
   * @param crt
   * @return
   */
  protected SelectQuery<EipTProjectTask> buildSelectQueryForListViewSortOrder(
      SelectQuery<EipTProjectTask> query, RunData rundata, Context context) {
    buildSelectQueryForListViewSort(query, rundata, context);

    String sort = ALEipUtils.getTemp(rundata, context, LIST_SORT_STR);
    String crt_key = null;

    Attributes map = getColumnMap();
    if (sort != null && sort.length() > 0) {
      crt_key = map.getValue(sort);
    }
    if (crt_key == null) {
      if (topView) {
        query.orderAscending(EipTProjectTask.END_PLAN_DATE_PROPERTY);
        query.orderAscending(EipTProjectTask.ORDER_NO_PROPERTY);
      } else {
        query.orderAscending(EipTProjectTask.ORDER_NO_PROPERTY);
      }
    }
    return query;
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

  /**
   * 検索した分類を取得します。
   *
   * @return 検索分類
   */
  public String getTargetTracker() {
    return target_tracker;
  }

  /**
   * 検索した優先度を取得します。
   *
   * @return 検索優先度
   */
  public String getTargetPriority() {
    return target_priority;
  }

  /**
   * 検索したステータスを取得します。
   *
   * @return 検索ステータス
   */
  public String getTargetStatus() {
    return target_status;
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

  public void setFiltersFromPSML(VelocityPortlet portlet, Context context,
      RunData rundata) {
    ALEipUtils.setTemp(rundata, context, LIST_FILTER_STR, portlet
      .getPortletConfig()
      .getInitParameter("p12f-filters"));

    ALEipUtils.setTemp(rundata, context, LIST_FILTER_TYPE_STR, portlet
      .getPortletConfig()
      .getInitParameter("p12g-filtertypes"));
  }

  public boolean isProjectEmpty() {
    return allProject.isEmpty();
  }

  public boolean isTaskEmpty() {
    return null == getList() || getList().isEmpty();
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

    ALEipUtils
      .setTemp(rundata, context, new StringBuffer().append(
        ProjectTaskSelectData.class.getSimpleName()).append(
        ALEipConstants.LIST_FILTER).toString(), String
        .valueOf(selectedProjectId));
    ALEipUtils.setTemp(rundata, context, new StringBuffer().append(
      ProjectTaskSelectData.class.getSimpleName()).append(
      ALEipConstants.LIST_FILTER_TYPE).toString(), "project_id");
  }

  public void setTopView(boolean bool) {
    this.topView = bool;
  }

}
