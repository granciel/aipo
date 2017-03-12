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
public class WorkflowAllSelectData extends
    ALAbstractSelectData<EipTWorkflowRequest, EipTWorkflowRequest> implements
    ALData {

  /** logger */
  private static final JetspeedLogger logger = JetspeedLogFactoryService
    .getLogger(WorkflowAllSelectData.class.getName());

  //
  /** サブメニュー（作成分）のタブ（未完了） */
  public static final String TAB_UNFINISHED = "unfinished";

  /** サブメニュー（作成分）のタブ（完了） */
  public static final String TAB_FINISHED = "finished";

  /** サブメニュー（すべての依頼）のタブ */
  public static final String TAB_ALLDISPLAY = "alldisplay";

  /** 現在選択されているタブ */
  private String currentTab;

  /** カテゴリ一覧 */
  private List<WorkflowCategoryResultData> categoryList;

  /** 申請経路一覧 */
  private List<WorkflowRouteResultData> routeList;

  /** 依頼総数 */
  private int requestSum;

  private ALEipUser login_user;

  /** ACL用の変数 */
  private String aclPortletFeature;

  /**  */
  private ALNumberField previous_id;

  private ALStringField target_keyword;

  /** 他ユーザーのワークフローの削除権限 */
  private boolean hasAuthorityOtherDelete;

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
    String sort = ALEipUtils.getTemp(rundata, context, LIST_SORT_STR);
    String sorttype = ALEipUtils.getTemp(rundata, context, LIST_SORT_TYPE_STR);
    if (sort == null || sort.equals("")) {
      ALEipUtils.setTemp(rundata, context, LIST_SORT_STR, ALEipUtils
        .getPortlet(rundata, context)
        .getPortletConfig()
        .getInitParameter("p2a-sort"));
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

    String allTabParam = rundata.getParameters().getString("alltab");
    String tabParam = rundata.getParameters().getString("tab");
    currentTab = ALEipUtils.getTemp(rundata, context, "tab");
    if (tabParam == null && currentTab == null) {
      currentTab = null;
    } else if (tabParam != null) {
      currentTab = tabParam;
    }
    ALEipUtils.setTemp(rundata, context, "alltab", allTabParam);
    ALEipUtils.setTemp(rundata, context, "tab", currentTab);

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

    login_user = ALEipUtils.getALEipUser(rundata);

    super.init(action, rundata, context);

    String entityId_Str =
      rundata.getParameters().getString(ALEipConstants.ENTITY_ID);
    if (entityId_Str == null || "".equals(entityId_Str)) {
      aclPortletFeature =
        ALAccessControlConstants.POERTLET_FEATURE_WORKFLOW_REQUEST_OTHER;
    } else {
      Integer entityId = Integer.parseInt(entityId_Str);
      int aimUserId = getUserId(rundata, context, entityId);
      int uid = ALEipUtils.getUserId(rundata);
      if (aimUserId != uid) {
        aclPortletFeature =
          ALAccessControlConstants.POERTLET_FEATURE_WORKFLOW_REQUEST_OTHER;
      } else {
        aclPortletFeature =
          ALAccessControlConstants.POERTLET_FEATURE_WORKFLOW_REQUEST_SELF;
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
    // アクセス権限
    ALAccessControlFactoryService aclservice =
      (ALAccessControlFactoryService) ((TurbineServices) TurbineServices
        .getInstance()).getService(ALAccessControlFactoryService.SERVICE_NAME);
    ALAccessControlHandler aclhandler = aclservice.getAccessControlHandler();

    hasAuthorityOtherDelete =
      aclhandler.hasAuthority(
        ALEipUtils.getUserId(rundata),
        ALAccessControlConstants.POERTLET_FEATURE_WORKFLOW_REQUEST_OTHER,
        ALAccessControlConstants.VALUE_ACL_DELETE);

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

    if (currentTab == null) {
      // 全取得
    } else if (TAB_UNFINISHED.equals(currentTab)) {
      Expression exp1 =
        ExpressionFactory.noMatchExp(
          EipTWorkflowRequest.PROGRESS_PROPERTY,
          WorkflowUtils.DB_PROGRESS_ACCEPT);
      query.setQualifier(exp1);
    } else if (TAB_FINISHED.equals(currentTab)) {
      Expression exp1 =
        ExpressionFactory.matchExp(
          EipTWorkflowRequest.PROGRESS_PROPERTY,
          WorkflowUtils.DB_PROGRESS_ACCEPT);
      query.setQualifier(exp1);
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
        LastUpdateUser = ALEipUtils.getALEipUser(map.getUserId().intValue());
      } else {
        for (int i = 0; i < size; i++) {
          map = maps.get(i);
          if (WorkflowUtils.DB_STATUS_CONFIRM.equals(map.getStatus())) {
            // 最終閲覧者を取得する
            LastUpdateUser =
              ALEipUtils.getALEipUser(map.getUserId().intValue());
            break;
          }
        }
      }

      ALEipUser clientUser = ALEipUtils.getALEipUser(record.getTurbineUser());
      rd.setClientUser(clientUser);

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

  private int getUserId(RunData rundata, Context context, Integer entityId) {
    Expression exp =
      ExpressionFactory.matchDbExp(
        EipTWorkflowRequest.REQUEST_ID_PK_COLUMN,
        entityId);
    SelectQuery<EipTWorkflowRequest> query =
      Database.query(EipTWorkflowRequest.class, exp);
    List<EipTWorkflowRequest> record = query.fetchList();
    if (record.size() > 0) {
      return (record.get(0)).getUserId().intValue();
    } else {
      return -1;
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
  public EipTWorkflowRequest selectDetail(RunData rundata, Context context) {
    return WorkflowUtils.getEipTWorkflowRequestAll(rundata, context);
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
    map.putValue("category", EipTWorkflowCategory.CATEGORY_ID_PK_COLUMN);
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

  /**
   * アクセス権限チェック用メソッド。<br />
   * アクセス権限の機能名を返します。
   *
   * @return
   */
  @Override
  public String getAclPortletFeature() {
    return aclPortletFeature;
  }

  /**
   * アクセス権限用メソッド。<br />
   * アクセス権限の有無を返します。
   *
   * @return
   */
  public boolean hasAuthorityOther() {
    return hasAuthority;
  }

  public boolean getHasAuthorityOtherDelete() {
    return hasAuthorityOtherDelete;
  }

  public ALStringField getTargetKeyword() {
    return target_keyword;
  }
}
