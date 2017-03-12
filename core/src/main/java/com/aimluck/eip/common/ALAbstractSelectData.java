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
package com.aimluck.eip.common;

import java.util.ArrayList;
import java.util.List;
import java.util.jar.Attributes;

import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.jetspeed.services.logging.JetspeedLogFactoryService;
import org.apache.jetspeed.services.logging.JetspeedLogger;
import org.apache.turbine.services.TurbineServices;
import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;

import com.aimluck.commons.field.ALStringField;
import com.aimluck.eip.modules.actions.common.ALAction;
import com.aimluck.eip.orm.query.ResultList;
import com.aimluck.eip.orm.query.SelectQuery;
import com.aimluck.eip.services.accessctl.ALAccessControlConstants;
import com.aimluck.eip.services.accessctl.ALAccessControlFactoryService;
import com.aimluck.eip.services.accessctl.ALAccessControlHandler;
import com.aimluck.eip.services.orgutils.ALOrgUtilsService;
import com.aimluck.eip.util.ALCommonUtils;
import com.aimluck.eip.util.ALEipUtils;
import com.aimluck.eip.util.ALLocalizationUtils;

/**
 * 検索データを管理するための抽象クラスです。 <br />
 *
 */
public abstract class ALAbstractSelectData<M1, M2> implements ALData {

  /** logger */
  @SuppressWarnings("unused")
  private static final JetspeedLogger logger = JetspeedLogFactoryService
    .getLogger(ALAbstractSelectData.class.getName());

  /** 表示行数 */
  private int rows_num = 10;

  /** 表示文字数 */
  private int strlen = 0;

  /** 開始位置 */
  private int start;

  /** 総件数 */
  private int count;

  /** 一覧データ */
  private List<Object> list;

  /** 詳細データ */
  protected Object data;

  /** 総ページ数 */
  protected int pages_num = 1;

  /** 現在のページ */
  protected int current_page = 1;

  /** 現在のソート */
  protected String current_sort;

  /** 現在のソートタイプ （asc:昇順、desc:降順） */
  protected String current_sort_type;

  /** 現在のフィルタ */
  protected String current_filter;

  /** 現在のフィルタタイプ */
  protected String current_filter_type;

  protected String current_search;

  /** アクセス権限の有無 */
  protected boolean hasAuthority;

  /** 添付ファイルアクセス権限の有無 */
  private boolean hasAttachmentAuthority;

  protected final String LIST_SORT_STR = new StringBuffer()
    .append(this.getClass().getSimpleName())
    .append(ALEipConstants.LIST_SORT)
    .toString();

  protected final String LIST_SORT_TYPE_STR = new StringBuffer()
    .append(this.getClass().getSimpleName())
    .append(ALEipConstants.LIST_SORT_TYPE)
    .toString();

  protected final String LIST_FILTER_STR = new StringBuffer()
    .append(this.getClass().getSimpleName())
    .append(ALEipConstants.LIST_FILTER)
    .toString();

  protected final String LIST_SEARCH_STR = new StringBuffer().append(
    this.getClass().getSimpleName()).append(ALEipConstants.SEARCH).toString();

  protected final String LIST_FILTER_TYPE_STR = new StringBuffer()
    .append(this.getClass().getSimpleName())
    .append(ALEipConstants.LIST_FILTER_TYPE)
    .toString();

  protected final String LIST_INDEX_STR = new StringBuffer()
    .append(this.getClass().getSimpleName())
    .append(ALEipConstants.LIST_INDEX)
    .toString();;

  /**
   *
   */
  @Override
  public void initField() {

  }

  /**
   * 初期化処理を行います。
   *
   * @param action
   * @param rundata
   * @param context
   */
  public void init(ALAction action, RunData rundata, Context context)
      throws ALPageNotFoundException, ALDBErrorException {
    if (ALEipUtils.isMatch(rundata, context)) {
      // ENTITY ID をセッション変数に設定
      if (rundata.getParameters().containsKey(ALEipConstants.ENTITY_ID)) {
        ALEipUtils.setTemp(rundata, context, ALEipConstants.ENTITY_ID, rundata
          .getParameters()
          .getString(ALEipConstants.ENTITY_ID));
      }

      if (rundata.getParameters().containsKey(ALEipConstants.LIST_SORT)) {
        ALEipUtils.setTemp(rundata, context, LIST_SORT_STR, rundata
          .getParameters()
          .getString(ALEipConstants.LIST_SORT));
      }

      if (rundata.getParameters().containsKey(ALEipConstants.LIST_SORT_TYPE)) {
        ALEipUtils.setTemp(rundata, context, LIST_SORT_TYPE_STR, rundata
          .getParameters()
          .getString(ALEipConstants.LIST_SORT_TYPE));
      }

      if (rundata.getParameters().containsKey(ALEipConstants.LIST_START)) {
        current_page =
          rundata.getParameters().getInt(ALEipConstants.LIST_START);
      }

      if (rundata.getParameters().containsKey(ALEipConstants.LIST_FILTER)) {
        ALEipUtils.setTemp(rundata, context, LIST_FILTER_STR, rundata
          .getParameters()
          .getString(ALEipConstants.LIST_FILTER));
      }

      if (rundata.getParameters().containsKey(ALEipConstants.SEARCH)) {
        ALEipUtils.setTemp(rundata, context, LIST_SEARCH_STR, rundata
          .getParameters()
          .getString(ALEipConstants.SEARCH));
      }

      if (rundata.getParameters().containsKey(ALEipConstants.LIST_FILTER_TYPE)) {
        ALEipUtils.setTemp(rundata, context, LIST_FILTER_TYPE_STR, rundata
          .getParameters()
          .getString(ALEipConstants.LIST_FILTER_TYPE));
      }
    }
  }

  /**
   * 一覧表示します。
   *
   * @param action
   * @param rundata
   * @param context
   * @return TRUE 成功 FASLE 失敗
   */
  public boolean doViewList(ALAction action, RunData rundata, Context context) {
    try {
      init(action, rundata, context);
      doCheckAclPermission(
        rundata,
        context,
        ALAccessControlConstants.VALUE_ACL_LIST);
      doCheckAttachmentAclPermission(
        rundata,
        context,
        ALAccessControlConstants.VALUE_ACL_EXPORT);
      action.setMode(ALEipConstants.MODE_LIST);
      ResultList<M1> resultList = selectList(rundata, context);
      if (resultList != null) {
        if (resultList.getTotalCount() > 0) {
          setPageParam(resultList.getTotalCount());
        }
        list = new ArrayList<Object>();
        for (M1 model : resultList) {
          Object object = getResultData(model);
          if (object != null) {
            list.add(object);
          }
        }
      }
      action.setResultData(this);
      action.putData(rundata, context);
      ALEipUtils.removeTemp(rundata, context, ALEipConstants.ENTITY_ID);
      return (list != null);
    } catch (ALPermissionException e) {
      ALEipUtils.redirectPermissionError(rundata);
      return false;
    } catch (ALPageNotFoundException e) {
      ALEipUtils.redirectPageNotFound(rundata);
      return false;
    } catch (ALDBErrorException e) {
      ALEipUtils.redirectDBError(rundata);
      return false;
    }

  }

  /**
   * 一覧表示のためのデータを取得します。
   *
   * @param action
   * @param rundata
   * @param context
   * @return TRUE 成功 FASLE 失敗
   */
  public boolean doSelectList(ALAction action, RunData rundata, Context context) {
    try {
      init(action, rundata, context);
      doCheckAclPermission(
        rundata,
        context,
        ALAccessControlConstants.VALUE_ACL_LIST);
      doCheckAttachmentAclPermission(
        rundata,
        context,
        ALAccessControlConstants.VALUE_ACL_EXPORT);
      ResultList<M1> resultList = selectList(rundata, context);
      if (resultList != null) {
        if (resultList.getTotalCount() > 0) {
          setPageParam(resultList.getTotalCount());
        }
        list = new ArrayList<Object>();
        for (M1 model : resultList) {
          Object object = getResultData(model);
          if (object != null) {
            list.add(object);
          }
        }
      }
      return (list != null);
    } catch (ALPermissionException e) {
      ALEipUtils.redirectPermissionError(rundata);
      return false;
    } catch (ALPageNotFoundException e) {
      ALEipUtils.redirectPageNotFound(rundata);
      return false;
    } catch (ALDBErrorException e) {
      ALEipUtils.redirectDBError(rundata);
      return false;
    }
  }

  /**
   * 詳細表示します。
   *
   * @param action
   * @param rundata
   * @param context
   * @return TRUE 成功 FASLE 失敗
   */
  public boolean doViewDetail(ALAction action, RunData rundata, Context context) {
    try {
      init(action, rundata, context);
      doCheckAclPermission(
        rundata,
        context,
        ALAccessControlConstants.VALUE_ACL_DETAIL);
      doCheckAttachmentAclPermission(
        rundata,
        context,
        ALAccessControlConstants.VALUE_ACL_EXPORT);
      action.setMode(ALEipConstants.MODE_DETAIL);
      M2 obj = selectDetail(rundata, context);
      if (obj != null) {
        data = getResultDataDetail(obj);
      }
      action.setResultData(this);
      action.putData(rundata, context);
      return (data != null);
    } catch (ALPermissionException e) {
      ALEipUtils.redirectPermissionError(rundata);
      return false;
    } catch (ALPageNotFoundException e) {
      ALEipUtils.redirectPageNotFound(rundata);
      return false;
    } catch (ALDBErrorException e) {
      ALEipUtils.redirectDBError(rundata);
      return false;
    }
  }

  /**
   * ページング結果のリストを取得します。
   *
   * @param records
   *          検索結果
   */
  protected void buildSelectQueryForListView(SelectQuery<M1> query) {
    // query.pageSize(getRowsNum());
    query.limit(getRowsNum());
    query.page(current_page);
  }

  /**
   * ページング結果のリストを取得します。
   *
   * @param records
   *          検索結果
   */
  protected List<M1> buildPaginatedList(List<M1> records) {
    List<M1> list = new ArrayList<M1>();

    setPageParam(records.size());

    int size = records.size();
    int end = (start + rows_num <= size) ? start + rows_num : size;
    for (int i = start; i < end; i++) {
      list.add(records.get(i));
    }

    return list;
  }

  /**
   *
   * @param cnt
   */
  protected void setPageParam(int cnt) {
    // 総件数
    count = cnt;
    // 総ページ数
    pages_num = ((int) (Math.ceil(count / (double) rows_num)));

    // 開始
    if ((pages_num > 0) && (pages_num < current_page)) {
      current_page = pages_num;
    }
    start = rows_num * (current_page - 1);
  }

  /**
   * ソート用の <code>SelectQuery</code> を構築します。
   *
   * @param crt
   * @return
   */
  protected SelectQuery<M1> buildSelectQueryForListViewSort(
      SelectQuery<M1> query, RunData rundata, Context context) {
    String sort = ALEipUtils.getTemp(rundata, context, LIST_SORT_STR);
    String sort_type = ALEipUtils.getTemp(rundata, context, LIST_SORT_TYPE_STR);
    String crt_key = null;

    Attributes map = getColumnMap();
    if (sort == null) {
      return query;
    }
    crt_key = map.getValue(sort);
    if (crt_key == null) {
      return query;
    }
    if (sort_type != null
      && ALEipConstants.LIST_SORT_TYPE_DESC.equals(sort_type)) {
      query.orderDesending(crt_key);
    } else {
      query.orderAscending(crt_key);
      sort_type = ALEipConstants.LIST_SORT_TYPE_ASC;
    }
    current_sort = sort;
    current_sort_type = sort_type;
    return query;
  }

  /**
   * フィルタ用の <code>SelectQuery</code> を構築します。
   *
   * @param crt
   * @param rundata
   * @param context
   * @return
   */
  protected SelectQuery<M1> buildSelectQueryForFilter(SelectQuery<M1> query,
      RunData rundata, Context context) {
    String filter = ALEipUtils.getTemp(rundata, context, LIST_FILTER_STR);
    String filter_type =
      ALEipUtils.getTemp(rundata, context, LIST_FILTER_TYPE_STR);
    String crt_key = null;
    Attributes map = getColumnMap();
    if (filter == null || filter_type == null || filter.equals("")) {
      return query;
    }
    crt_key = map.getValue(filter_type);
    if (crt_key == null) {
      return query;
    }
    Expression exp = ExpressionFactory.matchDbExp(crt_key, filter);
    query.andQualifier(exp);
    current_filter = filter;
    current_filter_type = filter_type;
    return query;
  }

  /**
   * 表示する項目数を設定します。
   *
   * @param num
   */
  public void setRowsNum(int num) {
    if (num >= 1) {
      rows_num = num;
    }
  }

  /**
   * 表示文字数を設定します。
   *
   * @param num
   */
  public void setStrLength(int num) {
    if (num >= 0) {
      strlen = num;
    }
  }

  /**
   * 表示文字数を取得します。
   *
   * @return
   */
  public int getStrLength() {
    return strlen;
  }

  /**
   * 表示する項目数を取得します。
   *
   * @return
   */
  public int getRowsNum() {
    return rows_num;
  }

  public String getRowsNum2() {
    return ALLocalizationUtils.getl10nFormat("WAR_BEFORE_NUMBER", rows_num);
  }

  public String getRowsNum3() {
    return ALLocalizationUtils.getl10nFormat("WAR_NEXT_NUMBER", rows_num);
  }

  /**
   * 総件数を取得します。
   *
   * @return
   */
  public int getCount() {
    return count;
  }

  public String getCount2() {
    return ALLocalizationUtils.getl10nFormat(
      "WAR_ISSUE_NUMBER",
      (getCurrentPage() - 1) * getRowsNum() + 1);
  }

  public String getCount3() {
    return ALLocalizationUtils.getl10nFormat("WAR_ALL_NUMBER", count);
  }

  public String getCount4() {
    if (getCurrentPage() * getRowsNum() < getCount()) {
      return ALLocalizationUtils.getl10nFormat(
        "WAR_ISSUE_NUMBER_EXPRESSION",
        getCount());
    } else {
      return ALLocalizationUtils.getl10nFormat(
        "WAR_ISSUE_NUMBER_EXPRESSION",
        getCurrentPage() * getRowsNum());
    }
  }

  /**
   * 総ページ数を取得します。
   *
   * @return
   */
  public int getPagesNum() {
    return pages_num;
  }

  /**
   * 現在表示されているページを取得します。
   *
   * @return
   */
  public int getCurrentPage() {
    return current_page;
  }

  /**
   * 一覧データを取得します。
   *
   * @return
   */
  public List<Object> getList() {
    return list;
  }

  /**
   * 詳細データを取得します。
   *
   * @return
   */
  public Object getDetail() {
    return data;
  }

  /**
   *
   * @return
   */
  public String getCurrentSort() {
    return current_sort;
  }

  /**
   *
   * @return
   */
  public String getCurrentSortType() {
    return current_sort_type;
  }

  /**
   *
   * @return
   */
  public String getCurrentFilter() {
    return current_filter;
  }

  /**
   *
   * @return
   */
  public String getCurrentFilterType() {
    return current_filter_type;
  }

  public String getCurrentSearch() {
    return current_search;
  }

  /**
   * 一覧データを取得する抽象メソッドです。
   *
   * @param rundata
   * @param context
   * @return
   */
  protected abstract ResultList<M1> selectList(RunData rundata, Context context)
      throws ALPageNotFoundException, ALDBErrorException;

  /**
   * 詳細データを取得する抽象メソッドです。
   *
   * @param rundata
   * @param context
   * @return
   */
  protected abstract M2 selectDetail(RunData rundata, Context context)
      throws ALPageNotFoundException, ALDBErrorException;

  /**
   * ResultDataを取得する抽象メソッドです。（一覧データ）
   *
   * @param obj
   * @return
   */
  protected abstract Object getResultData(M1 obj)
      throws ALPageNotFoundException, ALDBErrorException;

  /**
   * ResultDataを取得する抽象メソッドです。（詳細データ）
   *
   * @param obj
   * @return
   */
  protected abstract Object getResultDataDetail(M2 obj)
      throws ALPageNotFoundException, ALDBErrorException;

  /**
   *
   * @return
   */
  protected abstract Attributes getColumnMap();

  /**
   * アクセス権限をチェックします。
   *
   * @return
   */
  protected boolean doCheckAclPermission(RunData rundata, Context context,
      int defineAclType) throws ALPermissionException {

    if (defineAclType == 0) {
      return true;
    }

    String pfeature = getAclPortletFeature();
    if (pfeature == null || "".equals(pfeature)) {
      return true;
    }

    ALAccessControlFactoryService aclservice =
      (ALAccessControlFactoryService) ((TurbineServices) TurbineServices
        .getInstance()).getService(ALAccessControlFactoryService.SERVICE_NAME);
    ALAccessControlHandler aclhandler = aclservice.getAccessControlHandler();

    hasAuthority =
      aclhandler.hasAuthority(
        ALEipUtils.getUserId(rundata),
        pfeature,
        defineAclType);

    if (!hasAuthority) {
      throw new ALPermissionException();
    }

    return true;
  }

  /**
   * ファイルのアクセス権限をチェックします。
   *
   * @return
   */
  protected boolean doCheckAttachmentAclPermission(RunData rundata,
      Context context, int defineAclType) {

    if (defineAclType == 0) {
      return true;
    }

    // アクセス権限のチェックをしない場合
    boolean checkAttachmentAuthority = isCheckAttachmentAuthority();
    if (!checkAttachmentAuthority) {
      return true;
    }

    ALAccessControlFactoryService aclservice =
      (ALAccessControlFactoryService) ((TurbineServices) TurbineServices
        .getInstance()).getService(ALAccessControlFactoryService.SERVICE_NAME);
    ALAccessControlHandler aclhandler = aclservice.getAccessControlHandler();

    hasAttachmentAuthority =
      aclhandler.hasAuthority(
        ALEipUtils.getUserId(rundata),
        ALAccessControlConstants.POERTLET_FEATURE_ATTACHMENT,
        defineAclType);
    return hasAttachmentAuthority;
  }

  /**
   * アクセス権限用メソッド。<br />
   * アクセス権限の有無を返します。
   *
   * @return
   */
  public boolean hasAuthority() {
    return hasAuthority;
  }

  /**
   * アクセス権限チェック用メソッド。<br />
   * アクセス権限の機能名を返します。
   *
   * @return
   */
  public String getAclPortletFeature() {
    return null;
  }

  /**
   * @return
   */
  public int getStart() {
    return start;
  }

  public String getStringCR(ALStringField field) {
    return ALCommonUtils.replaceToAutoCR(field.toString());
  }

  /**
   * レイアウトテーマを取得
   *
   * @return
   */
  public String getTheme() {
    return ALOrgUtilsService.getTheme();
  }

  /**
   * ファイルアクセス権限チェック用メソッド。<br />
   * ファイルのアクセス権限をチェックするかどうかを判定します。
   *
   * @return
   */
  public boolean isCheckAttachmentAuthority() {
    return true;
  }

  public boolean hasAttachmentAuthority() {
    return hasAttachmentAuthority;
  }

}
