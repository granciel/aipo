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
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.jar.Attributes;

import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.jetspeed.portal.portlets.VelocityPortlet;
import org.apache.jetspeed.services.logging.JetspeedLogFactoryService;
import org.apache.jetspeed.services.logging.JetspeedLogger;
import org.apache.turbine.services.TurbineServices;
import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;

import com.aimluck.commons.field.ALDateTimeField;
import com.aimluck.eip.cayenne.om.portlet.EipTTodo;
import com.aimluck.eip.cayenne.om.portlet.VEipTScheduleList;
import com.aimluck.eip.cayenne.om.security.TurbineUser;
import com.aimluck.eip.common.ALDBErrorException;
import com.aimluck.eip.common.ALEipGroup;
import com.aimluck.eip.common.ALEipManager;
import com.aimluck.eip.common.ALEipPost;
import com.aimluck.eip.common.ALEipUser;
import com.aimluck.eip.common.ALPageNotFoundException;
import com.aimluck.eip.facilities.FacilityResultData;
import com.aimluck.eip.facilities.util.FacilitiesUtils;
import com.aimluck.eip.modules.actions.common.ALAction;
import com.aimluck.eip.orm.Database;
import com.aimluck.eip.orm.query.ResultList;
import com.aimluck.eip.orm.query.SelectQuery;
import com.aimluck.eip.schedule.util.ScheduleUtils;
import com.aimluck.eip.services.accessctl.ALAccessControlConstants;
import com.aimluck.eip.services.accessctl.ALAccessControlFactoryService;
import com.aimluck.eip.services.accessctl.ALAccessControlHandler;
import com.aimluck.eip.services.portal.ALPortalApplicationService;
import com.aimluck.eip.todo.util.ToDoUtils;
import com.aimluck.eip.util.ALEipUtils;
import com.aimluck.eip.util.ALLocalizationUtils;

/**
 * 月間スケジュールの検索結果を管理するクラスです。
 *
 */
public class ScheduleMonthlySelectData extends AjaxScheduleMonthlySelectData {

  /** <code>TARGET_GROUP_NAME</code> グループによる表示切り替え用変数の識別子 */
  private final String TARGET_GROUP_NAME = "target_group_name";

  /** <code>TARGET_USER_ID</code> ユーザによる表示切り替え用変数の識別子 */
  private final String TARGET_USER_ID = "target_user_id";

  /** <code>logger</code> logger */
  private static final JetspeedLogger logger = JetspeedLogFactoryService
    .getLogger(ScheduleMonthlySelectData.class.getName());

  /** <code>viewMonth</code> 現在の月 */
  private ALDateTimeField viewMonth;

  /** <code>prevMonth</code> 前の月 */
  private ALDateTimeField prevMonth;

  /** <code>nextMonth</code> 次の月 */
  private ALDateTimeField nextMonth;

  /** <code>prevMonth</code> 前の年 */
  private ALDateTimeField prevYear;

  /** <code>nextMonth</code> 次の年 */
  private ALDateTimeField nextYear;

  /** <code>currentMonth</code> 今月 */
  private ALDateTimeField currentMonth;

  /** <code>today</code> 今日 */
  private ALDateTimeField today;

  /** <code>viewStart</code> 表示開始日時 */
  private ALDateTimeField viewStart;

  /** <code>viewEnd</code> 表示終了日時 */
  private ALDateTimeField viewEnd;

  /** <code>viewEndCrt</code> 表示終了日時 (Criteria) */
  private ALDateTimeField viewEndCrt;

  /** <code>viewtype</code> 表示タイプ */
  protected String viewtype;

  /** <code>monthCon</code> 月間スケジュールコンテナ */
  private ScheduleMonthContainer monthCon;

  /** <code>myGroupList</code> グループリスト（My グループと部署） */
  private List<ALEipGroup> myGroupList = null;

  /** <code>groups</code> グループリスト */
  private List<ALEipGroup> facilitiyGroups;

  /** <code>userList</code> 表示切り替え用のユーザリスト */
  private List<ALEipUser> userList = null;

  /** <code>userid</code> ユーザーID */
  private String userid;

  /** <code>user</code> ユーザー */
  private ALEipUser user;

  /** <code>monthTodoCon</code> 期間スケジュール用の月間コンテナ */
  private ScheduleTermMonthContainer termMonthCon;

  /** <code>monthTodoCon</code> 月間 ToDo コンテナ */
  private ScheduleToDoMonthContainer monthTodoCon;

  /** ポートレット ID */
  private String portletId;

  /** <code>facilityList</code> 表示切り替え用の設備リスト */
  private List<FacilityResultData> facilityList;

  /** 閲覧権限の有無 */
  private boolean hasAclviewOther;

  /** <code>hasAuthoritySelfInsert</code> アクセス権限 */
  private boolean hasAuthoritySelfInsert = false;

  /** <code>hasAuthorityFacilityInsert</code> アクセス権限 */
  private boolean hasAuthorityFacilityInsert = false;

  /** <code>target_user_id</code> 表示対象のユーザ ログイン名 */
  private String target_user_name;

  /** <code>viewTodo</code> ToDo 表示設定 */
  protected int viewTodo;

  /** <code>target_group_name</code> 表示対象の部署名 */
  protected String target_group_name;

  /** <code>target_user_id</code> 表示対象のユーザ ID */
  protected String target_user_id;

  /**
   *
   * @param action
   * @param rundata
   * @param context
   * @throws ALPageNotFoundException
   * @throws ALDBErrorException
   */
  @Override
  public void init(ALAction action, RunData rundata, Context context)
      throws ALPageNotFoundException, ALDBErrorException {

    // 展開されるパラメータは以下の通りです。
    // ・viewMonth 形式：yyyy-MM

    // 表示種別の設定
    viewtype = "monthly";
    // POST/GET から yyyy-MM の形式で受け渡される。
    // 現在の月
    viewMonth = new ALDateTimeField("yyyy-MM");
    viewMonth.setNotNull(true);
    // 前の月
    prevMonth = new ALDateTimeField("yyyy-MM");
    // 次の月
    nextMonth = new ALDateTimeField("yyyy-MM");
    // 前の年
    prevYear = new ALDateTimeField("yyyy-MM");
    // 次の年
    nextYear = new ALDateTimeField("yyyy-MM");
    // 今月
    currentMonth = new ALDateTimeField("yyyy-MM");
    // 表示開始日時
    viewStart = new ALDateTimeField("yyyy-MM-dd");
    // 表示終了日時
    viewEnd = new ALDateTimeField("yyyy-MM-dd");
    // 表示終了日時 (Criteria)
    viewEndCrt = new ALDateTimeField("yyyy-MM-dd");
    // 今日
    today = new ALDateTimeField("yyyy-MM-dd");
    Calendar to = Calendar.getInstance();
    to.set(Calendar.HOUR_OF_DAY, 0);
    to.set(Calendar.MINUTE, 0);
    today.setValue(to.getTime());
    currentMonth.setValue(to.getTime());

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

    // 自ポートレットからのリクエストであれば、パラメータを展開しセッションに保存する。
    if (ALEipUtils.isMatch(rundata, context)) {
      // スケジュールの表示開始日時
      // e.g. 2004-3-14
      if (rundata.getParameters().containsKey("view_month")) {
        String tmpViewMonth = rundata.getParameters().getString("view_month");
        if (!tmpViewMonth.equals(ALEipUtils.getTemp(
          rundata,
          context,
          "view_month"))) {
          // ALEipUtils.setTemp(rundata, context, "view_start", tmpViewMonth
          // + "-01");
        }
        ALEipUtils.setTemp(rundata, context, "view_month", tmpViewMonth);
      } else {
        String tmpViewStart =
          ALEipUtils.getTemp(rundata, context, "view_start");
        if (tmpViewStart != null && tmpViewStart.length() >= 7) {
          ALEipUtils.setTemp(rundata, context, "view_month", tmpViewStart
            .substring(0, 7));
        }
      }
    }

    // 現在の月
    String tmpViewMonth = ALEipUtils.getTemp(rundata, context, "view_month");
    if (tmpViewMonth == null || tmpViewMonth.equals("")) {
      Calendar cal = Calendar.getInstance();
      cal.set(Calendar.DATE, 1);
      cal.set(Calendar.HOUR_OF_DAY, 0);
      cal.set(Calendar.MINUTE, 0);
      viewMonth.setValue(cal.getTime());
    } else {
      viewMonth.setValue(tmpViewMonth);
      if (!viewMonth.validate(new ArrayList<String>())) {
        ALEipUtils.removeTemp(rundata, context, "view_month");
        throw new ALPageNotFoundException();
      }
    }
    // MonthlyCalendarに表示する月を登録
    this.setMonthlyCalendarViewMonth(viewMonth.getYear(), viewMonth.getMonth());

    // 表示開始日時
    Calendar cal = Calendar.getInstance();
    cal.setTime(viewMonth.getValue());
    // 週の始まり（月間）の設定に応じて表示開始日時を変更する
    shiftCalToMatchStartDayOfWeek(cal, startDayOfWeek);

    viewStart.setValue(cal.getTime());

    Calendar cal4 = Calendar.getInstance();
    cal4.setTime(cal.getTime());

    Calendar cal5 = Calendar.getInstance();
    cal5.setTime(cal.getTime());

    // 月間スケジュールコンテナの初期化

    try {
      termMonthCon = new ScheduleTermMonthContainer();
      termMonthCon.initField();
      termMonthCon.setViewMonth(cal4);

      monthCon = new ScheduleMonthContainer();
      monthCon.initField();
      monthCon.setViewMonth(cal);

      monthTodoCon = new ScheduleToDoMonthContainer();
      monthTodoCon.initField();
      monthTodoCon.setViewMonth(cal5);
    } catch (Exception e) {
      logger.error("schedule", e);
    }
    // 表示終了日時
    viewEndCrt.setValue(cal.getTime());
    cal.add(Calendar.DATE, -1);
    viewEnd.setValue(cal.getTime());
    // 次の月、前の月
    Calendar cal2 = Calendar.getInstance();
    cal2.setTime(viewMonth.getValue());
    cal2.add(Calendar.MONTH, 1);
    nextMonth.setValue(cal2.getTime());
    cal2.add(Calendar.MONTH, -2);
    prevMonth.setValue(cal2.getTime());
    cal2.add(Calendar.MONTH, 1);
    cal2.add(Calendar.YEAR, 1);
    nextYear.setValue(cal2.getTime());
    cal2.add(Calendar.YEAR, -2);
    prevYear.setValue(cal2.getTime());

    ALEipUtils.setTemp(rundata, context, "tmpStart", viewStart.toString()
      + "-00-00");
    ALEipUtils.setTemp(rundata, context, "tmpEnd", viewStart.toString()
      + "-00-00");

    // ログインユーザの ID を設定する．
    userid = Integer.toString(ALEipUtils.getUserId(rundata));

    // My グループの一覧を取得する．
    List<ALEipGroup> myGroups = ALEipUtils.getMyGroups(rundata);
    myGroupList = new ArrayList<ALEipGroup>();
    facilitiyGroups = ALEipUtils.getALEipGroups();
    int length = myGroups.size();
    for (int i = 0; i < length; i++) {
      myGroupList.add(myGroups.get(i));
    }

    try {
      String groupFilter =
        ALEipUtils.getTemp(rundata, context, TARGET_GROUP_NAME);
      if (groupFilter == null || groupFilter.equals("")) {
        VelocityPortlet portlet = ALEipUtils.getPortlet(rundata, context);
        groupFilter = portlet.getPortletConfig().getInitParameter("p3a-group");
        if (groupFilter != null) {
          ALEipUtils.setTemp(rundata, context, TARGET_GROUP_NAME, groupFilter);
        }
      }
      current_filter = groupFilter;

      // スケジュールを表示するユーザ ID をセッションに設定する．
      String userFilter = ALEipUtils.getTemp(rundata, context, TARGET_USER_ID);
      if (userFilter == null || userFilter.equals("")) {
        VelocityPortlet portlet = ALEipUtils.getPortlet(rundata, context);
        userFilter = portlet.getPortletConfig().getInitParameter("p3a-user");
      }

      if (userFilter != null && (!userFilter.equals(""))) {
        int paramId = -1;
        if ("all".equals(userFilter)) {
          ALEipUtils.setTemp(rundata, context, TARGET_USER_ID, userFilter);
        } else if (userFilter.startsWith(ScheduleUtils.TARGET_FACILITY_ID)) {
          ALEipUtils.setTemp(rundata, context, TARGET_USER_ID, userFilter);
        } else {
          try {
            paramId = Integer.parseInt(userFilter);
            if (paramId > 3) {
              // ユーザーIDを取得する
              String query =
                "SELECT LOGIN_NAME FROM turbine_user WHERE USER_ID = '"
                  + paramId
                  + "' AND DISABLED = 'F'";
              List<TurbineUser> list =
                Database.sql(TurbineUser.class, query).fetchList();
              if (list != null && list.size() != 0) {
                // 指定したユーザが存在する場合，セッションに保存する．
                ALEipUtils
                  .setTemp(rundata, context, TARGET_USER_ID, userFilter);
              } else {
                ALEipUtils.removeTemp(rundata, context, TARGET_USER_ID);
              }
            }
          } catch (NumberFormatException e) {
          }
        }
      }
    } catch (Exception ex) {
      logger.error("schedule", ex);
    }

    // ToDo 表示設定
    viewTodo =
      !ALPortalApplicationService.isActive(ToDoUtils.TODO_PORTLET_NAME)
        ? 0
        : Integer.parseInt(ALEipUtils
          .getPortlet(rundata, context)
          .getPortletConfig()
          .getInitParameter("p5a-view"));

    // アクセスコントロール
    int loginUserId = ALEipUtils.getUserId(rundata);

    ALAccessControlFactoryService aclservice =
      (ALAccessControlFactoryService) ((TurbineServices) TurbineServices
        .getInstance()).getService(ALAccessControlFactoryService.SERVICE_NAME);
    ALAccessControlHandler aclhandler = aclservice.getAccessControlHandler();
    hasAclviewOther =
      aclhandler.hasAuthority(
        loginUserId,
        ALAccessControlConstants.POERTLET_FEATURE_SCHEDULE_OTHER,
        ALAccessControlConstants.VALUE_ACL_LIST);

    hasAuthoritySelfInsert =
      aclhandler.hasAuthority(
        loginUserId,
        ALAccessControlConstants.POERTLET_FEATURE_SCHEDULE_SELF,
        ALAccessControlConstants.VALUE_ACL_INSERT);

    hasAuthorityFacilityInsert =
      aclhandler.hasAuthority(
        loginUserId,
        ALAccessControlConstants.POERTLET_FEATURE_SCHEDULE_FACILITY,
        ALAccessControlConstants.VALUE_ACL_INSERT);

    this.setUser(ALEipUtils.getALEipUser(loginUserId));

    // スーパークラスのメソッドを呼び出す。
    super.init(action, rundata, context);
  }

  /**
   *
   * @param rundata
   * @param context
   * @return
   * @throws ALPageNotFoundException
   * @throws ALDBErrorException
   */
  @Override
  protected ResultList<VEipTScheduleList> selectList(RunData rundata,
      Context context) throws ALPageNotFoundException, ALDBErrorException {
    try {
      // 指定グループや指定ユーザをセッションに設定する．
      setupLists(rundata, context);

      List<VEipTScheduleList> list = getScheduleList(rundata, context);

      if (!target_user_id.startsWith(ScheduleUtils.TARGET_FACILITY_ID)
        && viewTodo == 1) {
        // ToDo の読み込み
        loadTodo(rundata, context);
      }

      // 時刻でソート
      ScheduleUtils.sortByTime(list);

      return new ResultList<VEipTScheduleList>(ScheduleUtils
        .sortByDummySchedule(list));
    } catch (Exception e) {
      logger.error("[ScheduleMonthlySelectData]", e);
      throw new ALDBErrorException();
    }

  }

  /**
   * 検索条件を設定した SelectQuery を返します。
   *
   * @param rundata
   * @param context
   * @return
   */
  private List<VEipTScheduleList> getScheduleList(RunData rundata,
      Context context) {

    Integer targetId = null;
    boolean isFacility = false;
    if ((target_user_id != null) && (!target_user_id.equals(""))) {
      if (target_user_id.startsWith(ScheduleUtils.TARGET_FACILITY_ID)) {
        String fid =
          target_user_id.substring(
            ScheduleUtils.TARGET_FACILITY_ID.length(),
            target_user_id.length());
        targetId = Integer.valueOf(fid);
        isFacility = true;
      } else {
        targetId = Integer.valueOf(target_user_id);
      }
    } else {
      // 表示できるユーザがいない場合の処理
      return new ArrayList<VEipTScheduleList>();
    }

    return ScheduleUtils.getScheduleList(Integer.valueOf(userid), viewStart
      .getValue(), viewEndCrt.getValue(), isFacility ? null : Arrays
      .asList(targetId), isFacility ? Arrays.asList(targetId) : null);
  }

  /**
   *
   * @param record
   * @return
   * @throws ALPageNotFoundException
   * @throws ALDBErrorException
   */
  @Override
  protected Object getResultData(VEipTScheduleList record)
      throws ALPageNotFoundException, ALDBErrorException {
    ScheduleResultData rd = new ScheduleResultData();
    rd.initField();
    try {
      // スケジュールが棄却されている場合は表示しない
      if ("R".equals(record.getStatus())) {
        return rd;
      }
      int userid_int = Integer.parseInt(userid);

      boolean is_member = record.isMember();

      // Dummy スケジュールではない
      // 完全に隠す
      // 自ユーザー以外
      // 共有メンバーではない
      // オーナーではない
      if ((!"D".equals(record.getStatus()))
        && "P".equals(record.getPublicFlag())
        && (userid_int != record.getUserId().intValue())
        && (userid_int != record.getOwnerId().intValue())
        && !is_member) {
        return rd;
      } else if (!hasAclviewOther && !is_member) {// 閲覧権限がなく、グループでもない
        return rd;
      }
      if ("C".equals(record.getPublicFlag())
        && ("F".equals(record.getType()) || ("U".equals(record.getType()) && userid_int != record
          .getUserId()
          .intValue()))
        && (userid_int != record.getOwnerId().intValue())
        && !is_member) {
        // 名前
        rd.setName(ALLocalizationUtils.getl10n("SCHEDULE_CLOSE_PUBLIC_WORD"));
        // 仮スケジュールかどうか
        rd.setTmpreserve(false);
      } else {
        // 名前
        rd.setName(record.getName());
        // 仮スケジュールかどうか
        rd.setTmpreserve("T".equals(record.getStatus()));
      }
      // 場所
      rd.setPlace(record.getPlace());
      // ID
      rd.setScheduleId(record.getScheduleId().intValue());
      // 親スケジュール ID
      rd.setParentId(record.getParentId().intValue());
      // 開始日時
      rd.setStartDate(record.getStartDate());
      // 終了日時
      rd.setEndDate(record.getEndDate());
      // 公開するかどうか
      rd.setPublic("O".equals(record.getPublicFlag()));
      // 非表示にするかどうか
      rd.setHidden("P".equals(record.getPublicFlag()));
      // ダミーか
      rd.setDummy("D".equals(record.getStatus()));
      // ログインユーザかどうか
      rd.setLoginuser(record.getUserId().intValue() == userid_int);
      // オーナーかどうか
      rd.setOwner(record.getOwnerId().intValue() == userid_int);
      // 共有メンバーかどうか
      rd.setMember(is_member);
      // 繰り返しパターン
      rd.setPattern(record.getRepeatPattern());

      // 期間スケジュールの場合
      if (rd.getPattern().equals("S")) {
        int stime =
          -(int) ((viewStart.getValue().getTime() - rd
            .getStartDate()
            .getValue()
            .getTime()) / 86400000);
        int etime =
          -(int) ((viewStart.getValue().getTime() - rd
            .getEndDate()
            .getValue()
            .getTime()) / 86400000);
        if (stime < 0) {
          stime = 0;
        }
        int count = stime;
        int col = etime - stime + 1;
        int row = count / 7;
        count = count % 7;
        // 行をまたがる場合
        while (count + col > 7) {
          ScheduleResultData rd3 = (ScheduleResultData) rd.clone();
          rd3.setRowspan(7 - count);
          // monthCon.addSpanResultData(count, row, rd3);
          termMonthCon.addTermResultData(count, row, rd3);
          count = 0;
          col -= rd3.getRowspan();
          row++;
        }
        // rowspanを設定
        rd.setRowspan(col);
        if (col > 0) {
          // 期間スケジュールをコンテナに格納
          termMonthCon.addTermResultData(count, row, rd);
        } else {

        }
        return rd;
      }

      // スケジュールをコンテナに格納
      monthCon.addResultData(rd);
    } catch (Exception e) {
      logger.error("schedule", e);

      return null;
    }
    return rd;
  }

  /**
   *
   * @param rundata
   * @param context
   * @return
   */
  @Override
  protected VEipTScheduleList selectDetail(RunData rundata, Context context) {
    return null;
  }

  /**
   *
   * @param record
   * @return
   */
  @Override
  protected Object getResultDataDetail(VEipTScheduleList record) {
    return null;
  }

  /*
   *
   */
  @Override
  protected Attributes getColumnMap() {
    return null;
  }

  public void loadTodo(RunData rundata, Context context) {
    try {
      SelectQuery<EipTTodo> query = getSelectQueryForTodo(rundata, context);
      if (query != null) {
        List<EipTTodo> todos = query.fetchList();

        int todossize = todos.size();
        for (int i = 0; i < todossize; i++) {
          EipTTodo record = todos.get(i);
          ScheduleToDoResultData rd = new ScheduleToDoResultData();
          rd.initField();

          // ポートレット ToDo のへのリンクを取得する．
          String todo_url = "";
          if (userid.equals(target_user_id)) {
            todo_url =
              ScheduleUtils.getPortletURItoTodoDetailPane(
                rundata,
                "ToDo",
                record.getTodoId().longValue(),
                portletId);
          } else {
            todo_url =
              ScheduleUtils.getPortletURItoTodoPublicDetailPane(
                rundata,
                "ToDo",
                record.getTodoId().longValue(),
                portletId);
          }
          rd.setTodoId(record.getTodoId().longValue());
          rd.setTodoName(record.getTodoName());
          rd.setUserId(record.getTurbineUser().getUpdatedUserId().intValue());
          rd.setStartDate(record.getStartDate());
          rd.setEndDate(record.getEndDate());
          rd.setTodoUrl(todo_url);
          // 公開/非公開を設定する．
          rd.setPublicFlag("T".equals(record.getPublicFlag()));

          int stime;
          if (ScheduleUtils.equalsToDate(ToDoUtils.getEmptyDate(), rd
            .getStartDate()
            .getValue(), false)) {
            stime = 0;
          } else {
            stime =
              -(int) ((viewStart.getValue().getTime() - rd
                .getStartDate()
                .getValue()
                .getTime()) / 86400000);
          }
          int etime =
            -(int) ((viewStart.getValue().getTime() - rd
              .getEndDate()
              .getValue()
              .getTime()) / 86400000);
          if (stime < 0) {
            stime = 0;
          }
          int count = stime;
          int col = etime - stime + 1;
          int row = count / 7;
          count = count % 7;
          // 行をまたがる場合
          while (count + col > 7) {
            ScheduleToDoResultData rd3 = (ScheduleToDoResultData) rd.clone();
            rd3.setRowspan(7 - count);
            monthTodoCon.addToDoResultData(count, row, rd3);
            count = 0;
            col -= rd3.getRowspan();
            row++;
          }
          // rowspanを設定
          rd.setRowspan(col);
          if (col > 0) {
            // 期間スケジュールをコンテナに格納
            monthTodoCon.addToDoResultData(count, row, rd);
          }
        }
      }
    } catch (Exception ex) {
      logger.error("schedule", ex);
      return;
    }
  }

  private SelectQuery<EipTTodo> getSelectQueryForTodo(RunData rundata,
      Context context) {
    SelectQuery<EipTTodo> query = Database.query(EipTTodo.class);
    Expression exp1 =
      ExpressionFactory.noMatchExp(EipTTodo.STATE_PROPERTY, Short
        .valueOf((short) 100));
    query.setQualifier(exp1);
    Expression exp2 =
      ExpressionFactory.matchExp(EipTTodo.ADDON_SCHEDULE_FLG_PROPERTY, "T");
    query.andQualifier(exp2);

    if ((target_user_id != null) && (!target_user_id.equals(""))) {
      // 指定ユーザをセットする．
      Expression exp3 =
        ExpressionFactory.matchDbExp(TurbineUser.USER_ID_PK_COLUMN, Integer
          .valueOf(target_user_id));
      query.andQualifier(exp3);
    } else {
      // 表示できるユーザがいない場合の処理
      return null;
    }

    if (!userid.equals(target_user_id)) {
      Expression exp4 =
        ExpressionFactory.matchExp(EipTTodo.PUBLIC_FLAG_PROPERTY, "T");
      query.andQualifier(exp4);
    }

    // 終了日時
    Expression exp11 =
      ExpressionFactory.greaterOrEqualExp(
        EipTTodo.END_DATE_PROPERTY,
        getViewStart().getValue());
    // 開始日時
    Expression exp12 =
      ExpressionFactory.lessOrEqualExp(
        EipTTodo.START_DATE_PROPERTY,
        getViewEnd().getValue());

    // 開始日時のみ指定されている ToDo を検索
    Expression exp21 =
      ExpressionFactory.lessOrEqualExp(
        EipTTodo.START_DATE_PROPERTY,
        getViewEnd().getValue());
    Expression exp22 =
      ExpressionFactory.matchExp(EipTTodo.END_DATE_PROPERTY, ToDoUtils
        .getEmptyDate());

    // 終了日時のみ指定されている ToDo を検索
    Expression exp31 =
      ExpressionFactory.greaterOrEqualExp(
        EipTTodo.END_DATE_PROPERTY,
        getViewStart().getValue());
    Expression exp32 =
      ExpressionFactory.matchExp(EipTTodo.START_DATE_PROPERTY, ToDoUtils
        .getEmptyDate());

    query.andQualifier((exp11.andExp(exp12)).orExp(exp21.andExp(exp22)).orExp(
      exp31.andExp(exp32)));

    query.orderAscending(EipTTodo.START_DATE_PROPERTY);
    return query;
  }

  /**
   * 表示タイプを取得します。
   *
   * @return
   */
  public String getViewtype() {
    return viewtype;
  }

  /**
   * 表示開始日時を取得します。
   *
   * @return
   */
  public ALDateTimeField getViewStart() {
    return viewStart;
  }

  /**
   * 表示終了日時を取得します。
   *
   * @return
   */
  public ALDateTimeField getViewEnd() {
    return viewEnd;
  }

  /**
   * 表示終了日時 (Criteria) を取得します。
   *
   * @return
   */
  public ALDateTimeField getViewEndCrt() {
    return viewEndCrt;
  }

  /**
   * 前の月を取得します。
   *
   * @return
   */
  public ALDateTimeField getPrevMonth() {
    return prevMonth;
  }

  /**
   * 次の月を取得します。
   *
   * @return
   */
  public ALDateTimeField getNextMonth() {
    return nextMonth;
  }

  /**
   * 前の年を取得します。
   *
   * @return
   */
  public ALDateTimeField getPrevYear() {
    return prevYear;
  }

  /**
   * 次の年を取得します。
   *
   * @return
   */
  public ALDateTimeField getNextYear() {
    return nextYear;
  }

  /**
   * 現在の月を取得します。
   *
   * @return
   */
  public ALDateTimeField getViewMonth() {
    return viewMonth;
  }

  public String getViewMonthText() {
    return ALLocalizationUtils.getl10nFormat(
      "SCHEDULE_DATE_MONTH_FORMAT",
      viewMonth.getYear(),
      viewMonth.getMonth());
  }

  /**
   * 今日を取得します。
   *
   * @return
   */
  public ALDateTimeField getToday() {
    return today;
  }

  /**
   * 今月を取得します。
   *
   * @return
   */
  public ALDateTimeField getCurrentMonth() {
    return currentMonth;
  }

  /**
   * 月間スケジュールコンテナを取得します。
   *
   * @return
   */
  public ScheduleMonthContainer getContainer() {
    return monthCon;
  }

  /**
   * 指定グループや指定ユーザをセッションに設定する．
   *
   * @param rundata
   * @param context
   * @throws ALDBErrorException
   */
  protected void setupLists(RunData rundata, Context context) {
    target_group_name = getTargetGroupName(rundata, context);
    boolean fgroup_flag = false;
    String target_group_id = "";
    current_filter = target_group_name;
    String[] target = target_group_name.split(";");
    String[] target2 = target_group_name.split("_");
    if ("f".equals(target[0])) {
      target_group_id = target[1];
    }
    if ("f".equals(target2[0])) {
      target_group_id = target2[1];
      fgroup_flag = true;
    }
    if ((!target_group_name.equals(""))
      && (!target_group_name.equals("all"))
      && (target_group_name.equals("Facility"))) {
      userList = ALEipUtils.getUsers(target_group_name);
      facilityList = FacilitiesUtils.getFacilityList(target_group_name);
    } else if ((!target_group_name.equals(""))
      && (!target_group_name.equals("all"))) {
      userList = ALEipUtils.getUsers(target_group_name);
      if (fgroup_flag) {
        facilityList =
          FacilitiesUtils
            .getFacilityGroupList(Integer.valueOf(target_group_id));
      } else {
        facilityList = FacilitiesUtils.getFacilityList(target_group_name);
      }
    } else {
      userList = ALEipUtils.getUsers("LoginUser");
      facilityList = FacilitiesUtils.getFacilityAllList();
    }

    if ((userList == null || userList.size() == 0)
      && (facilityList == null || facilityList.size() == 0)) {
      target_user_id = "";
      ALEipUtils.removeTemp(rundata, context, TARGET_USER_ID);
      return;
    }

    target_user_id = getTargetUserId(rundata, context);
    if (target_user_id == null) {
      target_user_id = "";
    }
    try {
      if ("".equals(target_user_id)
        || target_user_id.startsWith("f")
        || "all".equals(target_user_id)) {
        target_user_name = null;
      } else {
        ALEipUser tempuser =
          ALEipUtils.getALEipUser(Integer.parseInt(target_user_id));
        target_user_name = tempuser.getName().getValue();
      }
    } catch (Exception e) {
      logger.error("[ScheduleMonthlySelectData]", e);
      target_user_name = null;
    }

  }

  /**
   * 表示切り替えで指定したグループ ID を取得する．
   *
   * @param rundata
   * @param context
   * @return
   */
  protected String getTargetGroupName(RunData rundata, Context context) {
    return getTargetGroupName(rundata, context, TARGET_GROUP_NAME);
  }

  /**
   * 表示切り替えで指定したグループ ID を取得する．
   *
   * @param rundata
   * @param target_key
   * @param context
   * @return
   */
  protected String getTargetGroupName(RunData rundata, Context context,
      String target_key) {
    String target_group_name = null;
    String idParam = null;
    if (ALEipUtils.isMatch(rundata, context)) {
      // 自ポートレットへのリクエストの場合に，グループ名を取得する．
      idParam = rundata.getParameters().getString(target_key);
    }
    target_group_name = ALEipUtils.getTemp(rundata, context, target_key);

    if (idParam == null && target_group_name == null) {
      ALEipUtils.setTemp(rundata, context, target_key, "all");
      target_group_name = "all";
    } else if (idParam != null) {
      ALEipUtils.setTemp(rundata, context, target_key, idParam);
      target_group_name = idParam;
    }
    return target_group_name;
  }

  /**
   * 表示切り替えで指定したユーザ ID を取得する．
   *
   * @param rundata
   * @param context
   * @param target_key
   *          TARGET_USER_ID or TARGET_USER_ID_AT_SERCH
   * @return
   */
  protected String getTargetUserId(RunData rundata, Context context,
      String target_key) {
    String target_user_id = null;
    String idParam = null;
    String tmp_user_id = "";

    if (ALEipUtils.isMatch(rundata, context)) {
      // 自ポートレットへのリクエストの場合に，ユーザ ID を取得する．
      idParam = rundata.getParameters().getString(target_key);
    }
    target_user_id = ALEipUtils.getTemp(rundata, context, target_key);

    if ("Facility".equals(getTargetGroupName())) {
      // 表示グループで「設備一覧」が選択されている場合
      if (facilityList != null && facilityList.size() > 0) {
        if (idParam == null && (target_user_id == null)) {
          tmp_user_id = "";
        } else if (idParam != null) {
          tmp_user_id = idParam;
        } else {
          tmp_user_id = target_user_id;
        }

        if ("all".equals(tmp_user_id) && !"monthly".equals(viewtype)) {
          target_user_id = "all";
        } else if (containsFacilityId(facilityList, tmp_user_id)) {
          target_user_id = tmp_user_id;
        } else {
          FacilityResultData rd = facilityList.get(0);
          target_user_id = "f" + rd.getFacilityId().getValue();
        }
        ALEipUtils.setTemp(rundata, context, target_key, target_user_id);
      }
    } else {
      if (idParam == null && (target_user_id == null)) {
        tmp_user_id = (target_key.matches(TARGET_USER_ID)) ? userid : "all";
      } else if (idParam != null) {
        tmp_user_id = idParam;
      } else {
        tmp_user_id = target_user_id;
      }

      if (tmp_user_id.startsWith("f")) {
        if (containsFacilityId(facilityList, tmp_user_id)) {
          ALEipUtils.setTemp(rundata, context, target_key, tmp_user_id);
          target_user_id = tmp_user_id;
        } else {
          if (facilityList != null && facilityList.size() > 0) {
            FacilityResultData rd = facilityList.get(0);
            target_user_id = "f" + rd.getFacilityId().getValue();
            ALEipUtils.setTemp(rundata, context, target_key, target_user_id);
          } else {
            target_user_id = userid;
          }
        }
      } else {
        if (userList != null && userList.size() > 0) {
          // グループで表示を切り替えた場合，
          // ログインユーザもしくはユーザリストの一番初めのユーザを
          // 表示するため，ユーザ ID を設定する．
          if ("all".equals(tmp_user_id) && !"monthly".equals(viewtype)) {
            target_user_id = "all";
          } else if (containsUserId(userList, tmp_user_id)) {
            target_user_id = tmp_user_id;
          } else if (containsUserId(userList, userid)) {
            // ログインユーザのスケジュールを表示するため，ログイン ID を設定する．
            target_user_id = userid;
          } else {
            ALEipUser eipUser = userList.get(0);
            String userId = eipUser.getUserId().getValueAsString();
            target_user_id = userId;
          }
          ALEipUtils.setTemp(rundata, context, target_key, target_user_id);
        } else if (facilityList != null && facilityList.size() > 0) {
          // 設備グループで表示を切り替えた場合,
          // 設備グループリストの一番初めの設備を
          // 表示するため、設備 ID を設定する.
          FacilityResultData rd = facilityList.get(0);
          target_user_id = "f" + rd.getFacilityId().getValue();
          ALEipUtils.setTemp(rundata, context, target_key, target_user_id);
        }
      }
    }

    return target_user_id;
  }

  /**
   * 表示切り替えで指定したユーザ ID を取得する．
   *
   * @param rundata
   * @param context
   * @return
   */
  protected String getTargetUserId(RunData rundata, Context context) {
    return getTargetUserId(rundata, context, TARGET_USER_ID);
  }

  private boolean containsUserId(List<ALEipUser> list, String userid) {
    if (list == null || list.size() <= 0) {
      return false;
    }

    ALEipUser eipUser;
    int size = list.size();
    for (int i = 0; i < size; i++) {
      eipUser = list.get(i);
      String eipUserId = eipUser.getUserId().getValueAsString();
      if (userid.equals(eipUserId)) {
        return true;
      }
    }
    return false;
  }

  private boolean containsFacilityId(List<FacilityResultData> list,
      String facility_id) {
    if (list == null || list.size() <= 0) {
      return false;
    }

    FacilityResultData facility;
    int size = list.size();
    for (int i = 0; i < size; i++) {
      facility = list.get(i);
      String fid = "f" + facility.getFacilityId().toString();
      if (facility_id.equals(fid)) {
        return true;
      }
    }
    return false;
  }

  /**
   * 表示切り替え時に指定するグループ名
   *
   * @return
   */
  public String getTargetGroupName() {
    return target_group_name;
  }

  /**
   * 表示切り替え時に指定するユーザ ID
   *
   * @return
   */
  public String getTargetUserId() {
    return target_user_id;
  }

  /**
   * 指定グループに属するユーザの一覧を取得する．
   *
   * @param groupname
   * @return
   */
  public List<ALEipUser> getUsers() {
    if (hasAclviewOther || target_group_name.equals("Facility")) {
      return userList;
    } else {
      try {
        List<ALEipUser> users = new ArrayList<ALEipUser>();
        users.add(ALEipUtils.getALEipUser(Integer.parseInt(userid)));
        return users;
      } catch (Exception e) {
        return null;
      }
    }
  }

  /**
   * 部署の一覧を取得する．
   *
   * @return
   */
  public Map<Integer, ALEipPost> getPostMap() {
    if (hasAclviewOther) {
      return ALEipManager.getInstance().getPostMap();
    } else {
      return null;
    }
  }

  /**
   * My グループの一覧を取得する．
   *
   * @return
   */
  public List<ALEipGroup> getMyGroupList() {
    if (hasAclviewOther) {
      return myGroupList;
    } else {
      return new ArrayList<ALEipGroup>(0);
    }
  }

  /**
   * ログインユーザの ID を取得する．
   *
   * @return
   */
  public String getUserId() {
    return userid;
  }

  /**
   * 期間スケジュール用の月間コンテナを取得する.
   *
   * @return
   */
  public ScheduleTermMonthContainer getTermContainer() {
    return termMonthCon;
  }

  /**
   * 月間 ToDo コンテナを取得する.
   *
   * @return
   */
  public ScheduleToDoMonthContainer getToDoContainer() {
    return monthTodoCon;
  }

  public void setPortletId(String id) {
    portletId = id;
    // userid.substring(1);
  }

  public List<FacilityResultData> getFacilityList() {
    return facilityList;
  }

  /**
   * アクセス権限チェック用メソッド。<br />
   * アクセス権限の機能名を返します。
   *
   * @return
   */
  @Override
  public String getAclPortletFeature() {
    return ALAccessControlConstants.POERTLET_FEATURE_SCHEDULE_SELF;
  }

  public boolean hasAuthoritySelfInsert() {
    return hasAuthoritySelfInsert;
  }

  public boolean hasAuthorityFacilityInsert() {
    return hasAuthorityFacilityInsert;
  }

  /**
   * 設備のグループリストを取得します。
   *
   * @return
   */
  public List<ALEipGroup> getFacilitiyGroupList() {
    return facilitiyGroups;
  }

  public String getTargetName() {

    if (target_user_id.length() < 1) {
      return "";
    }

    try {
      if (target_user_id.substring(0, 1).equals("f")) {
        for (FacilityResultData record : facilityList) {
          int id =
            Integer
              .valueOf(
                target_user_id.substring(target_user_id.lastIndexOf("f") + 1))
              .intValue();
          if (record.getFacilityId().getValue() == id) {
            return record.getFacilityName().toString();
          }
        }
        return "";
      } else {
        return ALEipUtils
          .getALEipUser(Integer.parseInt(target_user_id))
          .getAliasName()
          .toString();
      }

    } catch (NumberFormatException e) {
      logger.error("[ScheduleMonthlySelectData]", e);
    } catch (ALDBErrorException e) {
      logger.error("[ScheduleMonthlySelectData]", e);
    }
    return "";
  }

  /**
   * 表示切り替え時に指定するユーザ のログイン名前
   *
   * @return
   */
  public String getTargetUserName() {
    return target_user_name;
  }

  /**
   * ユーザーを取得します。
   *
   * @return
   */
  public ALEipUser getUser() {
    return user;
  }

  /**
   * ユーザーを設定します。
   *
   * @param user
   */
  public void setUser(ALEipUser user) {
    this.user = user;
  }

  public boolean isHoliday(int DayOfWeek) {
    return ScheduleUtils.isUserHoliday(DayOfWeek);
  }

}
