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
package com.aimluck.eip.workflow;

import java.util.ArrayList;
import java.util.List;
import java.util.jar.Attributes;

import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.jetspeed.portal.portlets.VelocityPortlet;
import org.apache.jetspeed.services.logging.JetspeedLogFactoryService;
import org.apache.jetspeed.services.logging.JetspeedLogger;
import org.apache.turbine.services.TurbineServices;
import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;

import com.aimluck.commons.field.ALNumberField;
import com.aimluck.commons.field.ALStringField;
import com.aimluck.eip.cayenne.om.portlet.EipTWorkflowCategory;
import com.aimluck.eip.cayenne.om.portlet.EipTWorkflowFile;
import com.aimluck.eip.cayenne.om.portlet.EipTWorkflowRequest;
import com.aimluck.eip.cayenne.om.portlet.EipTWorkflowRequestMap;
import com.aimluck.eip.cayenne.om.security.TurbineUser;
import com.aimluck.eip.cayenne.om.social.Activity;
import com.aimluck.eip.common.ALAbstractSelectData;
import com.aimluck.eip.common.ALDBErrorException;
import com.aimluck.eip.common.ALData;
import com.aimluck.eip.common.ALEipConstants;
import com.aimluck.eip.common.ALEipUser;
import com.aimluck.eip.common.ALPageNotFoundException;
import com.aimluck.eip.modules.actions.common.ALAction;
import com.aimluck.eip.orm.Database;
import com.aimluck.eip.orm.query.ResultList;
import com.aimluck.eip.orm.query.SelectQuery;
import com.aimluck.eip.services.accessctl.ALAccessControlConstants;
import com.aimluck.eip.services.accessctl.ALAccessControlFactoryService;
import com.aimluck.eip.services.accessctl.ALAccessControlHandler;
import com.aimluck.eip.util.ALCommonUtils;
import com.aimluck.eip.util.ALEipUtils;
import com.aimluck.eip.util.ALLocalizationUtils;
import com.aimluck.eip.workflow.util.WorkflowUtils;

/**
 * ワークフロー検索データを管理するクラスです。 <BR>
 *
 */
public class WorkflowSelectData extends
    ALAbstractSelectData<EipTWorkflowRequest, EipTWorkflowRequest> implements
    ALData {

  /** logger */
  private static final JetspeedLogger logger = JetspeedLogFactoryService
    .getLogger(WorkflowSelectData.class.getName());

  /** サブメニュー（確認依頼） */
  public static final String SUBMENU_REQUESTED = "requested";

  /** サブメニュー（作成分） */
  public static final String SUBMENU_CREATED = "created";

  /** サブメニュー（確認依頼）のタブ（未確認） */
  public static final String TAB_UNCONFIRMED = "unconfirmed";

  /** サブメニュー（確認依頼）のタブ（確認済） */
  public static final String TAB_CONFIRMED = "confirmed";

  /** サブメニュー（確認依頼）のタブ（完了） */
  public static final String TAB_COMPLETED = "completed";

  /** サブメニュー（作成分）のタブ（未完了） */
  public static final String TAB_UNFINISHED = "unfinished";

  /** サブメニュー（作成分）のタブ（完了） */
  public static final String TAB_FINISHED = "finished";

  /** 現在選択されているサブメニュー */
  private String currentSubMenu;

  /** 現在選択されているタブ */
  private String currentTab;

  /** <code>viewtype</code> 表示タイプ */
  protected String viewtype;

  /** カテゴリ一覧 */
  private List<WorkflowCategoryResultData> categoryList;

  /** 申請経路一覧 */
  private List<WorkflowRouteResultData> routeList;

  /** リクエストの総数 */
  private int requestSum;

  private ALEipUser login_user;

  private ALNumberField previous_id;

  /** 他ユーザーのワークフローの閲覧権限 */
  private boolean hasAuthorityOther;

  private ALStringField target_keyword;

  /**
   *
   * @param action
   * @param rundata
   * @param context
   */
  @Override
  public void init(ALAction action, RunData rundata, Context context)
      throws ALPageNotFoundException, ALDBErrorException {
    String sort = ALEipUtils.getTemp(rundata, context, LIST_SORT_STR);
    String sorttype = ALEipUtils.getTemp(rundata, context, LIST_SORT_TYPE_STR);
    if (sort == null || sort.equals("")) {
      VelocityPortlet portlet = ALEipUtils.getPortlet(rundata, context);
      // when call from activity , there are no portlets
      if (portlet != null) {
        ALEipUtils.setTemp(rundata, context, LIST_SORT_STR, portlet
          .getPortletConfig()
          .getInitParameter("p2a-sort"));
      } else {
        ALEipUtils.setTemp(rundata, context, LIST_SORT_STR, "create_date");
      }
    }

    if ("create_date".equals(ALEipUtils
      .getTemp(rundata, context, LIST_SORT_STR))
      && (sorttype == null || "".equals(sorttype))) {
      ALEipUtils.setTemp(
        rundata,
        context,
        LIST_SORT_TYPE_STR,
        ALEipConstants.LIST_SORT_TYPE_DESC);
    }

    String subMenuParam = rundata.getParameters().getString("submenu");
    currentSubMenu = ALEipUtils.getTemp(rundata, context, "submenu");
    if (subMenuParam == null && currentSubMenu == null) {
      ALEipUtils.setTemp(rundata, context, "submenu", SUBMENU_REQUESTED);
      currentSubMenu = SUBMENU_REQUESTED;
    } else if (subMenuParam != null) {
      ALEipUtils.setTemp(rundata, context, "submenu", subMenuParam);
      currentSubMenu = subMenuParam;
    }

    String tabParam = rundata.getParameters().getString("tab");
    currentTab = ALEipUtils.getTemp(rundata, context, "tab");
    if (tabParam == null && currentTab == null) {
      if (SUBMENU_REQUESTED.equals(currentSubMenu)) {
        tabParam = TAB_UNCONFIRMED;
      } else {
        tabParam = TAB_UNFINISHED;
      }
      ALEipUtils.setTemp(rundata, context, "tab", tabParam);
      currentTab = tabParam;
    } else if (tabParam != null) {
      ALEipUtils.setTemp(rundata, context, "tab", tabParam);
      currentTab = tabParam;
    }
    if (SUBMENU_REQUESTED.equals(currentSubMenu)) {
      if (TAB_UNFINISHED.equals(currentTab) || TAB_FINISHED.equals(currentTab)) {
        currentTab = TAB_UNCONFIRMED;
      }
    } else {
      if (TAB_UNCONFIRMED.equals(currentTab)
        || TAB_CONFIRMED.equals(currentTab)
        || TAB_COMPLETED.equals(currentTab)) {
        currentTab = TAB_UNFINISHED;
      }
    }

    try {
      previous_id = new ALNumberField();
      String previd = rundata.getParameters().getString("prvid");
      if (previd == null) {
        previd = rundata.getParameters().getString("entityid");
      }
      previous_id.setValue(previd);
    } catch (Exception e) {
      previous_id = null;
    }

    login_user = ALEipUtils.getALEipUser(rundata);

    // アクセス権限
    ALAccessControlFactoryService aclservice =
      (ALAccessControlFactoryService) ((TurbineServices) TurbineServices
        .getInstance()).getService(ALAccessControlFactoryService.SERVICE_NAME);
    ALAccessControlHandler aclhandler = aclservice.getAccessControlHandler();

    hasAuthorityOther =
      aclhandler.hasAuthority(
        ALEipUtils.getUserId(rundata),
        ALAccessControlConstants.POERTLET_FEATURE_WORKFLOW_REQUEST_OTHER,
        ALAccessControlConstants.VALUE_ACL_LIST);

    super.init(action, rundata, context);
    viewtype = "detail";
    // カテゴリの初期値を取得する
    try {
      String filter = ALEipUtils.getTemp(rundata, context, LIST_FILTER_STR);
      if (filter == null) {
        VelocityPortlet portlet = ALEipUtils.getPortlet(rundata, context);
        String categoryId =
          portlet.getPortletConfig().getInitParameter("p3a-category");
        if (categoryId != null) {
          ALEipUtils.setTemp(rundata, context, LIST_FILTER_STR, categoryId);
          ALEipUtils
            .setTemp(rundata, context, LIST_FILTER_TYPE_STR, "category");
        }
      }
    } catch (Exception ex) {
      logger.debug("Exception", ex);
    }

    target_keyword = new ALStringField();

    super.init(action, rundata, context);

  }

  /**
   *
   * @param rundata
   * @param context
   */
  public void loadCategoryList(RunData rundata, Context context) {
    categoryList = WorkflowUtils.loadCategoryList(rundata, context);
  }

  /**
   *
   * @param rundata
   * @param context
   */
  public void loadRouteList(RunData rundata, Context context) {
    routeList = WorkflowUtils.loadRouteList(rundata, context);
  }

  /**
   * 一覧データを取得します。 <BR>
   *
   * @param rundata
   * @param context
   * @return
   */
  @Override
  public ResultList<EipTWorkflowRequest> selectList(RunData rundata,
      Context context) {
    try {
      if (WorkflowUtils.hasResetFlag(rundata, context)) {
        WorkflowUtils.resetFilter(rundata, context, this.getClass().getName());
        target_keyword.setValue("");
      } else {
        target_keyword.setValue(WorkflowUtils
          .getTargetKeyword(rundata, context));
      }

      SelectQuery<EipTWorkflowRequest> query = getSelectQuery(rundata, context);
      buildSelectQueryForListView(query);
      buildSelectQueryForListViewSort(query, rundata, context);

      ResultList<EipTWorkflowRequest> list = query.getResultList();
      // リクエストの総数をセットする．
      requestSum = list.getTotalCount();
      return list;
    } catch (Exception ex) {
      logger.error("workflow", ex);
      return null;
    }
  }

  /**
   * 検索条件を設定した SelectQuery を返します。 <BR>
   *
   * @param rundata
   * @param context
   * @return
   */
  private SelectQuery<EipTWorkflowRequest> getSelectQuery(RunData rundata,
      Context context) {

    if ((target_keyword != null) && (!target_keyword.getValue().equals(""))) {
      ALEipUtils.setTemp(rundata, context, LIST_SEARCH_STR, target_keyword
        .getValue());
    } else {
      ALEipUtils.removeTemp(rundata, context, LIST_SEARCH_STR);
    }

    SelectQuery<EipTWorkflowRequest> query =
      Database.query(EipTWorkflowRequest.class);

    Integer login_user_id =
      Integer.valueOf((int) login_user.getUserId().getValue());

    // 受信
    if (SUBMENU_REQUESTED.equals(currentSubMenu)) {

      Expression exp1 =
        ExpressionFactory.matchExp(
          EipTWorkflowRequest.EIP_TWORKFLOW_REQUEST_MAP_PROPERTY
            + "."
            + EipTWorkflowRequestMap.USER_ID_PROPERTY,
          login_user_id);
      query.setQualifier(exp1);

      // TAB_UNCONFIRMED.equals(currentTab)
      Expression exp21 =
        ExpressionFactory.matchExp(
          EipTWorkflowRequest.PROGRESS_PROPERTY,
          WorkflowUtils.DB_PROGRESS_WAIT);
      Expression exp22 =
        ExpressionFactory.matchExp(
          EipTWorkflowRequest.EIP_TWORKFLOW_REQUEST_MAP_PROPERTY
            + "."
            + EipTWorkflowRequestMap.STATUS_PROPERTY,
          WorkflowUtils.DB_STATUS_CONFIRM);
      Expression exp31 =
        ExpressionFactory.matchExp(
          EipTWorkflowRequest.EIP_TWORKFLOW_REQUEST_MAP_PROPERTY
            + "."
            + EipTWorkflowRequestMap.STATUS_PROPERTY,
          WorkflowUtils.DB_STATUS_REQUEST);
      Expression exp32 =
        ExpressionFactory.matchExp(
          EipTWorkflowRequest.PROGRESS_PROPERTY,
          WorkflowUtils.DB_PROGRESS_DENAIL);

      // TAB_CONFIRMED.equals(currentTab)
      Expression exp2 =
        ExpressionFactory.matchExp(
          EipTWorkflowRequest.PROGRESS_PROPERTY,
          WorkflowUtils.DB_PROGRESS_WAIT);

      Expression exp3 =
        ExpressionFactory.matchExp(
          EipTWorkflowRequest.EIP_TWORKFLOW_REQUEST_MAP_PROPERTY
            + "."
            + EipTWorkflowRequestMap.STATUS_PROPERTY,
          WorkflowUtils.DB_STATUS_ACCEPT);

      // TAB_COMPLETED.equals(currentTab)
      Expression exp52 =
        ExpressionFactory.matchExp(
          EipTWorkflowRequest.PROGRESS_PROPERTY,
          WorkflowUtils.DB_PROGRESS_ACCEPT);

      Expression exp53 =
        ExpressionFactory.matchExp(
          EipTWorkflowRequest.EIP_TWORKFLOW_REQUEST_MAP_PROPERTY
            + "."
            + EipTWorkflowRequestMap.STATUS_PROPERTY,
          WorkflowUtils.DB_STATUS_ACCEPT);

      if (ALEipUtils.getTemp(rundata, context, "Workflow_Maximize") == "false") {
        query.andQualifier((exp21.andExp(exp22)).orExp(exp31.andExp(exp32)));
      } else {
        query.andQualifier((exp21.andExp(exp22))
          .orExp(exp31.andExp(exp32))
          .orExp(exp3.andExp(exp2))
          .orExp(exp53.andExp(exp52)));
      }

      // 送信
    } else {
      // 作成分
      Expression exp1 =
        ExpressionFactory.matchExp(
          EipTWorkflowRequest.USER_ID_PROPERTY,
          login_user_id);
      query.setQualifier(exp1);

      query.distinct();
    }

    return buildSelectQueryForFilter(query, rundata, context);
  }

  @Override
  protected SelectQuery<EipTWorkflowRequest> buildSelectQueryForFilter(
      SelectQuery<EipTWorkflowRequest> query, RunData rundata, Context context) {
    String filter = ALEipUtils.getTemp(rundata, context, LIST_FILTER_STR);
    String filter_type =
      ALEipUtils.getTemp(rundata, context, LIST_FILTER_TYPE_STR);
    String crt_key = null;
    Attributes map = getColumnMap();
    if (filter_type != null) {
      crt_key = map.getValue(filter_type);
    }
    if (filter != null
      && filter_type != null
      && !filter.equals("")
      && crt_key != null) {
      Expression exp = ExpressionFactory.matchDbExp(crt_key, filter);
      query.andQualifier(exp);
      current_filter = filter;
      current_filter_type = filter_type;
    }
    String search = ALEipUtils.getTemp(rundata, context, LIST_SEARCH_STR);
    if (search != null && !search.equals("")) {
      current_search = search;
      Expression ex1 =
        ExpressionFactory.likeExp(EipTWorkflowRequest.NOTE_PROPERTY, "%"
          + search
          + "%");
      Expression ex11 =
        ExpressionFactory.likeExp(EipTWorkflowRequestMap.NOTE_PROPERTY, "%"
          + search
          + "%");
      Expression ex2 =
        ExpressionFactory.likeExp(
          EipTWorkflowRequest.REQUEST_NAME_PROPERTY,
          "%" + search + "%");
      Expression ex3 =
        ExpressionFactory.likeExp(
          EipTWorkflowRequest.EIP_TWORKFLOW_CATEGORY_PROPERTY
            + "."
            + EipTWorkflowCategory.CATEGORY_NAME_PROPERTY,
          "%" + search + "%");

      SelectQuery<EipTWorkflowRequest> q =
        Database.query(EipTWorkflowRequest.class);
      SelectQuery<EipTWorkflowRequestMap> qm =
        Database.query(EipTWorkflowRequestMap.class);

      q.andQualifier(ex1.orExp(ex2).orExp(ex3));
      qm.andQualifier(ex11);

      List<EipTWorkflowRequest> queryList = q.fetchList();
      List<EipTWorkflowRequestMap> queryListMap = qm.fetchList();
      List<Integer> resultid = new ArrayList<Integer>();
      for (EipTWorkflowRequest item : queryList) {
        if (item.getParentId() != 0 && !resultid.contains(item.getParentId())) {
          resultid.add(item.getParentId());
        } else if (!resultid.contains(item.getRequestId())) {
          resultid.add(item.getRequestId());
        }
      }
      for (EipTWorkflowRequestMap item : queryListMap) {
        if (item.getEipTWorkflowRequest().getParentId() != 0
          && !resultid.contains(item.getEipTWorkflowRequest().getParentId())) {
          resultid.add(item.getEipTWorkflowRequest().getParentId());
        } else if (!resultid.contains(item
          .getEipTWorkflowRequest()
          .getRequestId())) {
          resultid.add(item.getEipTWorkflowRequest().getRequestId());
        }
      }
      if (resultid.size() == 0) {
        // 検索結果がないことを示すために-1を代入
        resultid.add(-1);
      }
      Expression ex =
        ExpressionFactory.inDbExp(
          EipTWorkflowRequest.REQUEST_ID_PK_COLUMN,
          resultid);
      query.andQualifier(ex);
    }
    return query;
  }

  /**
   * ResultData に値を格納して返します。（一覧データ） <BR>
   *
   * @param obj
   * @return
   */
  @Override
  protected Object getResultData(EipTWorkflowRequest record) {
    try {
      WorkflowResultData rd = new WorkflowResultData();
      rd.initField();
      rd.setRequestId(record.getRequestId().intValue());
      rd.setCategoryId(record
        .getEipTWorkflowCategory()
        .getCategoryId()
        .longValue());
      rd.setCategoryName(ALCommonUtils.compressString(record
        .getEipTWorkflowCategory()
        .getCategoryName(), getStrLength()));
      rd.setRequestName(ALCommonUtils.compressString(
        record.getRequestName(),
        getStrLength()));
      rd.setPriority(record.getPriority().intValue());
      rd.setPriorityImage(WorkflowUtils.getPriorityImage(record
        .getPriority()
        .intValue()));
      rd.setPriorityString(WorkflowUtils.getPriorityString(record
        .getPriority()
        .intValue()));
      rd.setProgress(record.getProgress());
      rd.setPrice(record.getPrice().longValue());

      Expression exp2 =
        ExpressionFactory.matchExp(Activity.EXTERNAL_ID_PROPERTY, rd
          .getRequestId()
          .toString());
      Expression exp3 =
        ExpressionFactory.matchExp(Activity.APP_ID_PROPERTY, "Workflow");
      Expression exp4 = exp2.andExp(exp3);
      List<Activity> list = Database.query(Activity.class, exp4).fetchList();

      for (Activity activity : list) {
        rd.setActivityId(activity.getId());
      }

      ALEipUser LastUpdateUser = new ALEipUser();
      EipTWorkflowRequestMap map = null;
      List<EipTWorkflowRequestMap> maps =
        WorkflowUtils.getEipTWorkflowRequestMap(record);
      int size = maps.size();

      if (WorkflowUtils.DB_PROGRESS_ACCEPT.equals(record.getProgress())) {
        // すべて承認済みの場合、最終承認者をセットする
        map = maps.get(size - 1);
        ALEipUser user = ALEipUtils.getALEipUser(map.getUserId().intValue());
        LastUpdateUser = user;
      } else {
        for (int i = 0; i < size; i++) {
          map = maps.get(i);
          if (WorkflowUtils.DB_STATUS_CONFIRM.equals(map.getStatus())) {
            // 最終閲覧者を取得する
            ALEipUser user =
              ALEipUtils.getALEipUser(map.getUserId().intValue());
            LastUpdateUser = user;
            break;
          }
        }
      }

      ALEipUser clientUser = ALEipUtils.getALEipUser(record.getTurbineUser());
      rd.setClientUser(clientUser);
      rd.setUpdateDateTime(record.getUpdateDate());

      String state = "";
      if (WorkflowUtils.DB_PROGRESS_ACCEPT.equals(record.getProgress())) {
        state = ALLocalizationUtils.getl10n("WORKFLOW_COMPLETION");
      } else if (WorkflowUtils.DB_PROGRESS_WAIT.equals(record.getProgress())) {
        state = ALLocalizationUtils.getl10n("WORKFLOW_PROGRESS");
      } else {
        state = ALLocalizationUtils.getl10n("WORKFLOW_DENIAL");
      }
      rd.setStateString(state);
      rd.setLastUpdateUser(LastUpdateUser);
      rd.setCreateDateTime(record.getCreateDate());
      rd.setCreateDate(WorkflowUtils.translateDate(
        record.getCreateDate(),
        ALLocalizationUtils.getl10n("WORKFLOW_YEAR_MONTH_DAY_HOUR_MINIT")));
      return rd;
    } catch (Exception ex) {
      logger.error("workflow", ex);
      return null;
    }
  }

  /**
   * 詳細データを取得します。 <BR>
   *
   * @param rundata
   * @param context
   * @return
   */
  @Override
  public EipTWorkflowRequest selectDetail(RunData rundata, Context context)
      throws ALPageNotFoundException {
    try {

      EipTWorkflowRequest request =
        WorkflowUtils.getEipTWorkflowRequest(rundata, context, false);

      return request;
    } catch (ALPageNotFoundException pageNotFound) {
      throw pageNotFound;
    }
  }

  /**
   * ResultData に値を格納して返します。（詳細データ） <BR>
   *
   * @param obj
   * @return
   */
  @Override
  protected Object getResultDataDetail(EipTWorkflowRequest obj) {
    return WorkflowUtils.getResultDataDetail(
      obj,
      login_user,
      hasAttachmentAuthority());
  }

  /**
   * ファイル検索のクエリを返します
   *
   * @return
   */
  @SuppressWarnings("unused")
  private SelectQuery<EipTWorkflowFile> getSelectQueryForFiles(int requestid) {
    SelectQuery<EipTWorkflowFile> query =
      Database.query(EipTWorkflowFile.class);
    Expression exp =
      ExpressionFactory.matchDbExp(
        EipTWorkflowRequest.REQUEST_ID_PK_COLUMN,
        Integer.valueOf(requestid));
    query.setQualifier(exp);
    return query;
  }

  /**
   *
   * @return
   */
  public List<WorkflowCategoryResultData> getCategoryList() {
    return categoryList;
  }

  public List<WorkflowRouteResultData> getRouteList() {
    return routeList;
  }

  /**
   * 現在選択されているサブメニューを取得します。 <BR>
   *
   * @return
   */
  public String getCurrentSubMenu() {
    return this.currentSubMenu;
  }

  /**
   * 現在選択されているタブを取得します。 <BR>
   *
   * @return
   */
  public String getCurrentTab() {
    return currentTab;
  }

  /**
   * リクエストの総数を返す． <BR>
   *
   * @return
   */
  public int getRequestSum() {
    return requestSum;
  }

  /**
   * @return
   *
   */
  @Override
  protected Attributes getColumnMap() {
    Attributes map = new Attributes();
    map.putValue("request_name", EipTWorkflowRequest.REQUEST_NAME_PROPERTY);
    map.putValue("priority", EipTWorkflowRequest.PRIORITY_PROPERTY);
    map.putValue("price", EipTWorkflowRequest.PRICE_PROPERTY);
    map.putValue("create_date", EipTWorkflowRequest.CREATE_DATE_PROPERTY);
    map.putValue("progress", EipTWorkflowRequest.PROGRESS_PROPERTY);
    map.putValue(
      "category",
      EipTWorkflowRequest.EIP_TWORKFLOW_CATEGORY_PROPERTY
        + "."
        + EipTWorkflowCategory.CATEGORY_ID_PK_COLUMN);
    map.putValue("user_name", EipTWorkflowRequest.TURBINE_USER_PROPERTY
      + "."
      + TurbineUser.LAST_NAME_KANA_PROPERTY);
    return map;
  }

  public ALEipUser getLoginUser() {
    return login_user;
  }

  /**
   *
   * @param id
   * @return
   */
  public boolean isMatch(int id1, long id2) {
    return id1 == (int) id2;
  }

  public ALNumberField getPreviousID() {
    return previous_id;
  }

  public String getViewtype() {
    return viewtype;
  }

  /**
   * アクセス権限チェック用メソッド。<br />
   * アクセス権限の機能名を返します。
   *
   * @return
   */
  @Override
  public String getAclPortletFeature() {
    return ALAccessControlConstants.POERTLET_FEATURE_WORKFLOW_REQUEST_SELF;
  }

  public boolean hasAuthorityOther() {
    return hasAuthorityOther;
  }

  public ALStringField getTargetKeyword() {
    return target_keyword;
  }
}
