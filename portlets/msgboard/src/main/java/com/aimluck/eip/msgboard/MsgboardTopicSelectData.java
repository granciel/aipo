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
package com.aimluck.eip.msgboard;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.jar.Attributes;

import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.commons.lang.StringUtils;
import org.apache.jetspeed.portal.portlets.VelocityPortlet;
import org.apache.jetspeed.services.logging.JetspeedLogFactoryService;
import org.apache.jetspeed.services.logging.JetspeedLogger;
import org.apache.turbine.services.TurbineServices;
import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;

import com.aimluck.commons.field.ALStringField;
import com.aimluck.commons.utils.ALStringUtil;
import com.aimluck.eip.cayenne.om.portlet.EipTMsgboardCategory;
import com.aimluck.eip.cayenne.om.portlet.EipTMsgboardCategoryMap;
import com.aimluck.eip.cayenne.om.portlet.EipTMsgboardFile;
import com.aimluck.eip.cayenne.om.portlet.EipTMsgboardTopic;
import com.aimluck.eip.common.ALAbstractMultiFilterSelectData;
import com.aimluck.eip.common.ALDBErrorException;
import com.aimluck.eip.common.ALData;
import com.aimluck.eip.common.ALEipConstants;
import com.aimluck.eip.common.ALEipGroup;
import com.aimluck.eip.common.ALEipManager;
import com.aimluck.eip.common.ALEipPost;
import com.aimluck.eip.common.ALEipUser;
import com.aimluck.eip.common.ALPageNotFoundException;
import com.aimluck.eip.common.ALPermissionException;
import com.aimluck.eip.fileupload.beans.FileuploadBean;
import com.aimluck.eip.fileupload.util.FileuploadUtils;
import com.aimluck.eip.modules.actions.common.ALAction;
import com.aimluck.eip.msgboard.util.MsgboardUtils;
import com.aimluck.eip.orm.Database;
import com.aimluck.eip.orm.query.ResultList;
import com.aimluck.eip.orm.query.SelectQuery;
import com.aimluck.eip.services.accessctl.ALAccessControlConstants;
import com.aimluck.eip.services.accessctl.ALAccessControlFactoryService;
import com.aimluck.eip.services.accessctl.ALAccessControlHandler;
import com.aimluck.eip.util.ALEipUtils;

/**
 * 掲示板トピックの検索データを管理するクラスです。 <BR>
 *
 */
public class MsgboardTopicSelectData extends
    ALAbstractMultiFilterSelectData<EipTMsgboardTopic, EipTMsgboardTopic>
    implements ALData {

  /** logger */
  private static final JetspeedLogger logger = JetspeedLogFactoryService
    .getLogger(MsgboardTopicSelectData.class.getName());

  /** カテゴリ一覧 */
  private List<MsgboardCategoryResultData> categoryList;

  /** 部署一覧 */
  private List<ALEipGroup> postList;

  /** トピックの総数 */
  private int topicSum;

  /** 親トピックオブジェクト */
  private Object parentTopic;

  /** 子トピックオブジェクト */
  private List<MsgboardTopicResultData> coTopicList;

  /** ログインユーザ ID */
  private int uid;

  /** 返信フォーム表示の有無（トピック詳細表示） */
  private boolean showReplyForm = false;

  /** 閲覧権限の有無 */
  private boolean hasAclCategoryList;

  /** 他ユーザーの作成したトピックの編集権限 */
  private boolean hasAclUpdateTopicOthers;

  /** 他ユーザーの作成したトピックの削除権限 */
  private boolean hasAclDeleteTopicOthers;

  /** 初期表示 */
  private int table_colum_num;

  /** カテゴリの初期値を取得する */
  private String filterType = "";

  /** カテゴリ　ID */
  private String categoryId = "";

  /** ターゲット　 */
  private ALStringField target_keyword;

  /** グループID */
  private String postId = "";

  /** カテゴリ名 */
  private String categoryName = "";

  /** グループ名 */
  private String postName = "";

  private boolean isFileUploadable;

  /** 添付ファイル追加へのアクセス権限の有無 */
  private boolean hasAttachmentInsertAuthority;

  /**
   *
   * @param action
   * @param rundata
   * @param context
   */
  @Override
  public void init(ALAction action, RunData rundata, Context context)
      throws ALPageNotFoundException, ALDBErrorException {
    super.init(action, rundata, context);

    doCheckAttachmentInsertAclPermission(rundata, context);

    String sort = ALEipUtils.getTemp(rundata, context, LIST_SORT_STR);
    if (sort == null || sort.equals("")) {
      VelocityPortlet portlet = ALEipUtils.getPortlet(rundata, context);
      String sortStr = null;
      if (portlet != null) {
        sortStr = portlet.getPortletConfig().getInitParameter("p2a-sort");
      } else {
        sortStr = "update_date";
      }
      ALEipUtils.setTemp(rundata, context, LIST_SORT_STR, sortStr);
      if ("create_date".equals(sortStr) || "update_date".equals(sortStr)) {
        ALEipUtils.setTemp(rundata, context, LIST_SORT_TYPE_STR, "desc");
      }
    }

    uid = ALEipUtils.getUserId(rundata);

    ALAccessControlFactoryService aclservice =
      (ALAccessControlFactoryService) ((TurbineServices) TurbineServices
        .getInstance()).getService(ALAccessControlFactoryService.SERVICE_NAME);
    ALAccessControlHandler aclhandler = aclservice.getAccessControlHandler();

    hasAclCategoryList =
      aclhandler.hasAuthority(
        ALEipUtils.getUserId(rundata),
        ALAccessControlConstants.POERTLET_FEATURE_MSGBOARD_CATEGORY,
        ALAccessControlConstants.VALUE_ACL_LIST);

    hasAclDeleteTopicOthers =
      aclhandler.hasAuthority(
        ALEipUtils.getUserId(rundata),
        ALAccessControlConstants.POERTLET_FEATURE_MSGBOARD_TOPIC_OTHER,
        ALAccessControlConstants.VALUE_ACL_DELETE);

    hasAclUpdateTopicOthers =
      aclhandler.hasAuthority(
        ALEipUtils.getUserId(rundata),
        ALAccessControlConstants.POERTLET_FEATURE_MSGBOARD_TOPIC_OTHER,
        ALAccessControlConstants.VALUE_ACL_UPDATE);

    // My グループの一覧を取得する．
    postList = ALEipUtils.getMyGroups(rundata);

    // カテゴリの初期値を取得する
    try {
      updateCategoryName();
    } catch (Exception ex) {
      logger.error("msgboard", ex);
    }

    target_keyword = new ALStringField();
    isFileUploadable = ALEipUtils.isFileUploadable(rundata);
  }

  /**
   *
   * @param rundata
   * @param context
   */
  public void loadCategoryList(RunData rundata, Context context) {
    // カテゴリ一覧
    categoryList = MsgboardUtils.loadCategoryList(rundata);
    setCategory(rundata, context);
  }

  /**
   *
   * @param rundata
   * @param context
   */
  public void setCategory(RunData rundata, Context context) {
    // validate categoryId and filterType, categoryId set
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

    if (filterType.equals("category")) {
      filter = "0," + filter;
      filterType = "post,category";
    } else if (filterType.equals("post,category")) {
      // do nothing
    } else {
      filter = "";
      filterType = "";
    }

    if (StringUtils.isEmpty(filter) || StringUtils.isEmpty(filterType)) {
      this.filterType = "";
      categoryId = "";
      return;
    }

    String[] splited = filter.split(",");
    if (splited.length < 2) {
      this.filterType = "";
      categoryId = "";
      return;
    }

    categoryId = filter.split(",")[1];
    this.filterType = filterType;

    boolean existCategory = false;
    for (MsgboardCategoryResultData data : categoryList) {
      if (categoryId.equals(data.getCategoryId().toString())) {
        existCategory = true;
        break;
      }
    }

    if (!existCategory) {
      categoryId = "0";
      ALEipUtils.setTemp(
        rundata,
        context,
        LIST_FILTER_STR,
        filter.split(",")[0] + "," + categoryId);
      ALEipUtils.setTemp(
        rundata,
        context,
        LIST_FILTER_TYPE_STR,
        "post,category");
    }
  }

  /**
   * 一覧データを取得します。 <BR>
   *
   * @param rundata
   * @param context
   * @return
   */
  @Override
  public ResultList<EipTMsgboardTopic> selectList(RunData rundata,
      Context context) {
    try {
      if (MsgboardUtils.hasResetFlag(rundata, context)) {
        MsgboardUtils.resetFilter(rundata, context, this.getClass().getName());
        target_keyword.setValue("");
      } else {
        target_keyword.setValue(MsgboardUtils
          .getTargetKeyword(rundata, context));
      }

      SelectQuery<EipTMsgboardTopic> query = getSelectQuery(rundata, context);
      buildSelectQueryForListView(query);
      buildSelectQueryForListViewSort(query, rundata, context);

      // 表示するカラムのみデータベースから取得する．
      ResultList<EipTMsgboardTopic> list = query.getResultList();
      // 件数をセットする．
      topicSum = list.getTotalCount();
      return list;
    } catch (Exception ex) {
      logger.error("msgboard", ex);
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

  private SelectQuery<EipTMsgboardTopic> getSelectQuery(RunData rundata,
      Context context) {

    if ((target_keyword != null) && (!target_keyword.getValue().equals(""))) {
      ALEipUtils.setTemp(rundata, context, LIST_SEARCH_STR, target_keyword
        .getValue());
    } else {
      ALEipUtils.removeTemp(rundata, context, LIST_SEARCH_STR);
    }

    SelectQuery<EipTMsgboardTopic> query =
      Database.query(EipTMsgboardTopic.class);

    Expression exp1 =
      ExpressionFactory.matchExp(EipTMsgboardTopic.PARENT_ID_PROPERTY, Integer
        .valueOf(0));
    query.setQualifier(exp1);

    // アクセス制御
    Expression exp01 =
      ExpressionFactory.matchExp(
        EipTMsgboardTopic.EIP_TMSGBOARD_CATEGORY_PROPERTY
          + "."
          + EipTMsgboardCategory.PUBLIC_FLAG_PROPERTY,
        "T");

    Expression exp02 =
      ExpressionFactory.matchExp(
        EipTMsgboardTopic.EIP_TMSGBOARD_CATEGORY_PROPERTY
          + "."
          + EipTMsgboardCategory.EIP_TMSGBOARD_CATEGORY_MAPS_PROPERTY
          + "."
          + EipTMsgboardCategoryMap.STATUS_PROPERTY,
        "O");
    Expression exp03 =
      ExpressionFactory.matchExp(
        EipTMsgboardTopic.EIP_TMSGBOARD_CATEGORY_PROPERTY
          + "."
          + EipTMsgboardCategory.EIP_TMSGBOARD_CATEGORY_MAPS_PROPERTY
          + "."
          + EipTMsgboardCategoryMap.STATUS_PROPERTY,
        "A");
    Expression exp11 =
      ExpressionFactory.matchExp(
        EipTMsgboardTopic.EIP_TMSGBOARD_CATEGORY_PROPERTY
          + "."
          + EipTMsgboardCategory.PUBLIC_FLAG_PROPERTY,
        "F");
    Expression exp12 =
      ExpressionFactory.matchExp(
        EipTMsgboardTopic.EIP_TMSGBOARD_CATEGORY_PROPERTY
          + "."
          + EipTMsgboardCategory.EIP_TMSGBOARD_CATEGORY_MAPS_PROPERTY
          + "."
          + EipTMsgboardCategoryMap.USER_ID_PROPERTY,
        Integer.valueOf(ALEipUtils.getUserId(rundata)));
    query.andQualifier((exp01.andExp(exp02.orExp(exp03))).orExp(exp11
      .andExp(exp12)));
    query.distinct(true);

    return buildSelectQueryForFilter(query, rundata, context);
  }

  /**
   * パラメータをマップに変換します。
   *
   * @param key
   * @param val
   */
  @Override
  protected void parseFilterMap(String key, String val) {
    super.parseFilterMap(key, val);

    Set<String> unUse = new HashSet<String>();

    for (Entry<String, List<String>> pair : current_filterMap.entrySet()) {
      if (pair.getValue().contains("0")) {
        unUse.add(pair.getKey());
      }
    }
    for (String unusekey : unUse) {
      current_filterMap.remove(unusekey);
    }
  }

  // :TODO
  @Override
  protected SelectQuery<EipTMsgboardTopic> buildSelectQueryForFilter(
      SelectQuery<EipTMsgboardTopic> query, RunData rundata, Context context) {
    if (current_filterMap.containsKey("category")) {
      // カテゴリを含んでいる場合デフォルトとは別にフィルタを用意
      List<String> categoryIds = current_filterMap.get("category");
      categoryId = categoryIds.get(0).toString();
      List<MsgboardCategoryResultData> categoryList =
        MsgboardUtils.loadCategoryList(rundata);
      boolean existCategory = false;
      if (categoryList != null && categoryList.size() > 0) {
        for (MsgboardCategoryResultData category : categoryList) {
          if (categoryId.equals(category.getCategoryId().toString())) {
            existCategory = true;
            break;
          }
        }

      }
      if (!existCategory) {
        categoryId = "";
        current_filterMap.remove("category");

      }

      updateCategoryName();
    }

    super.buildSelectQueryForFilter(query, rundata, context);

    if (current_filterMap.containsKey("post")) {
      // 部署を含んでいる場合デフォルトとは別にフィルタを用意

      List<String> postIds = current_filterMap.get("post");
      boolean existPost = false;
      for (int i = 0; i < postList.size(); i++) {
        String pid = postList.get(i).getName().toString();
        if (pid.equals(postIds.get(0).toString())) {
          existPost = true;
          break;
        }
      }
      Map<Integer, ALEipPost> map = ALEipManager.getInstance().getPostMap();
      if (postIds != null && !postIds.isEmpty()) {
        for (Map.Entry<Integer, ALEipPost> item : map.entrySet()) {
          String pid = item.getValue().getGroupName().toString();
          if (pid.equals(postIds.get(0).toString())) {
            existPost = true;
            break;
          }
        }
      }
      if (existPost) {
        HashSet<Integer> userIds = new HashSet<Integer>();
        for (String post : postIds) {
          List<Integer> userId = ALEipUtils.getUserIds(post);
          userIds.addAll(userId);
        }
        if (userIds.isEmpty()) {
          userIds.add(-1);
        }
        Expression exp =
          ExpressionFactory.inExp(EipTMsgboardTopic.OWNER_ID_PROPERTY, userIds);
        query.andQualifier(exp);

        postId = postIds.get(0).toString();
        updatePostName();
      } else {
        postId = "";
        updatePostName();
        current_filterMap.remove("post");
      }
    }

    String search = ALEipUtils.getTemp(rundata, context, LIST_SEARCH_STR);

    if (search != null && !"".equals(search)) {
      current_search = search;
      Expression ex1 =
        ExpressionFactory.likeExp(EipTMsgboardTopic.NOTE_PROPERTY, "%"
          + search
          + "%");
      Expression ex2 =
        ExpressionFactory.likeExp(EipTMsgboardTopic.TOPIC_NAME_PROPERTY, "%"
          + search
          + "%");
      SelectQuery<EipTMsgboardTopic> q =
        Database.query(EipTMsgboardTopic.class);
      q.andQualifier(ex1.orExp(ex2));
      List<EipTMsgboardTopic> queryList = q.fetchList();
      List<Integer> resultid = new ArrayList<Integer>();
      for (EipTMsgboardTopic item : queryList) {
        if (item.getParentId() != 0 && !resultid.contains(item.getParentId())) {
          resultid.add(item.getParentId());
        } else if (!resultid.contains(item.getTopicId())) {
          resultid.add(item.getTopicId());
        }
      }
      if (resultid.size() == 0) {
        // 検索結果がないことを示すために-1を代入
        resultid.add(-1);
      }
      Expression ex =
        ExpressionFactory.inDbExp(
          EipTMsgboardTopic.TOPIC_ID_PK_COLUMN,
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
  protected Object getResultData(EipTMsgboardTopic record) {
    try {
      MsgboardTopicResultData rd = new MsgboardTopicResultData();
      rd.initField();
      rd.setTopicId(record.getTopicId().longValue());
      rd.setTopicName(record.getTopicName());
      rd.setCategoryId(record
        .getEipTMsgboardCategory()
        .getCategoryId()
        .longValue());
      rd.setCategoryName(record.getEipTMsgboardCategory().getCategoryName());

      // 公開/非公開を設定する．
      rd.setPublicFlag("T".equals(record
        .getEipTMsgboardCategory()
        .getPublicFlag()));
      rd.setOwnerId(record.getOwnerId().longValue());
      rd.setOwnerName(ALEipUtils
        .getUserFullName(record.getOwnerId().intValue()));
      rd.setUpdateUser(ALEipUtils.getUserFullName(record
        .getUpdateUserId()
        .intValue()));
      rd.setCreateDate(record.getCreateDate());
      rd.setUpdateDate(record.getUpdateDate());

      // 新着を設定する（期限は最終更新日からの 1 日間）．
      Date date = record.getUpdateDate();
      Calendar now = Calendar.getInstance();
      now.add(Calendar.DATE, -1);
      rd.setNewTopicFlag(date.after(now.getTime()));
      rd.setReplyCount(MsgboardUtils.countReply(record.getTopicId()));
      rd.setLoginUserId(uid);

      return rd;
    } catch (Exception ex) {
      logger.error("msgboard", ex);
      return null;
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
  @Override
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
      List<EipTMsgboardTopic> aList = selectDetailList(rundata, context);
      if (aList != null) {
        coTopicList = new ArrayList<MsgboardTopicResultData>();
        int size = aList.size();
        for (int i = 0; i < size; i++) {
          coTopicList.add((MsgboardTopicResultData) getResultDataDetail(aList
            .get(i)));
        }
      }

      action.setResultData(this);
      action.putData(rundata, context);
      return true;
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
   * 詳細データを取得します。 <BR>
   *
   * @param rundata
   * @param context
   * @return
   */
  public List<EipTMsgboardTopic> selectDetailList(RunData rundata,
      Context context) throws ALPageNotFoundException, ALDBErrorException {
    String topicid =
      ALEipUtils.getTemp(rundata, context, ALEipConstants.ENTITY_ID);

    if (topicid == null || Integer.valueOf(topicid) == null) {
      // トピック ID が空の場合
      logger.debug("[MsgboardTopic] Empty ID...");
      throw new ALPageNotFoundException();
    }

    VelocityPortlet portlet = ALEipUtils.getPortlet(rundata, context);
    String cotopicsort = null;
    if (portlet != null) {
      cotopicsort = portlet.getPortletConfig().getInitParameter("p2b-sort");
    } else {
      cotopicsort = "response_old";
    }

    try {
      parentTopic =
        getResultDataDetail(MsgboardUtils.getEipTMsgboardParentTopic(
          rundata,
          context,
          false));

      SelectQuery<EipTMsgboardTopic> query =
        getSelectQueryForCotopic(rundata, context, topicid, cotopicsort);
      /** 詳細画面は全件表示する */
      // buildSelectQueryForListView(query);
      if ("response_new".equals(cotopicsort)) {
        query.orderDesending(EipTMsgboardTopic.CREATE_DATE_PROPERTY);
      } else {
        query.orderAscending(EipTMsgboardTopic.CREATE_DATE_PROPERTY);
      }

      List<EipTMsgboardTopic> resultList = query.fetchList();

      // 表示するカラムのみデータベースから取得する．
      return resultList;
    } catch (ALPageNotFoundException pageNotFound) {
      // logger.error("[MsgboardTopicSelectData]", pageNotFound);
      throw pageNotFound;
    } catch (Exception ex) {
      logger.error("[MsgboardTopicSelectData]", ex);
      throw new ALDBErrorException();
    }
  }

  @Override
  public EipTMsgboardTopic selectDetail(RunData rundata, Context context) {
    ALEipUtils.redirectPageNotFound(rundata);
    return null;
  }

  private SelectQuery<EipTMsgboardTopic> getSelectQueryForCotopic(
      RunData rundata, Context context, String topicid, String cotopicsort) {
    SelectQuery<EipTMsgboardTopic> query =
      Database.query(EipTMsgboardTopic.class);
    Expression exp =
      ExpressionFactory.matchExp(EipTMsgboardTopic.PARENT_ID_PROPERTY, Integer
        .valueOf(topicid));
    query.setQualifier(exp);
    query.distinct(true);
    return query;
  }

  private SelectQuery<EipTMsgboardFile> getSelectQueryForFiles(int topicid) {
    SelectQuery<EipTMsgboardFile> query =
      Database.query(EipTMsgboardFile.class);
    Expression exp =
      ExpressionFactory.matchDbExp(
        EipTMsgboardTopic.TOPIC_ID_PK_COLUMN,
        Integer.valueOf(topicid));
    query.setQualifier(exp);
    query.orderAscending(EipTMsgboardFile.UPDATE_DATE_PROPERTY);
    query.orderAscending(EipTMsgboardFile.FILE_PATH_PROPERTY);
    return query;
  }

  /**
   * ResultData に値を格納して返します。（詳細データ） <BR>
   *
   * @param obj
   * @return
   */
  @Override
  protected Object getResultDataDetail(EipTMsgboardTopic record)
      throws ALPageNotFoundException, ALDBErrorException {
    try {

      // 親トピックのアクセス権限をもとに，返信フォーム表示の有無を決定する．
      if (record.getParentId().intValue() == 0) {
        EipTMsgboardCategory category = record.getEipTMsgboardCategory();
        if ("T".equals(category.getPublicFlag())) {
          List<?> categoryMap = category.getEipTMsgboardCategoryMaps();
          int mapsize = categoryMap.size();
          for (int i = 0; i < mapsize; i++) {
            EipTMsgboardCategoryMap map =
              (EipTMsgboardCategoryMap) categoryMap.get(i);
            if ("A".equals(map.getStatus())) {
              showReplyForm = true;
            } else {
              if (map.getUserId().intValue() == uid) {
                // ログインユーザが所属メンバの場合
                showReplyForm = true;
                break;
              }
            }

          }
        } else {
          // 非公開のトピックの場合は，
          // データベースの検索時にアクセスをフィルタリングしている．
          showReplyForm = true;
        }
      }

      MsgboardTopicResultData rd = new MsgboardTopicResultData();
      rd.initField();
      rd.setTopicId(record.getTopicId().longValue());
      rd.setTopicName(record.getTopicName());
      rd.setParentId(record.getParentId().longValue());
      rd.setCategoryId(record
        .getEipTMsgboardCategory()
        .getCategoryId()
        .longValue());
      rd.setCategoryName(record.getEipTMsgboardCategory().getCategoryName());
      // 公開/非公開を設定する．
      rd.setPublicFlag("T".equals(record
        .getEipTMsgboardCategory()
        .getPublicFlag()));
      rd.setOwnerId(record.getOwnerId().longValue());
      rd.setOwnerName(ALEipUtils
        .getUserFullName(record.getOwnerId().intValue()));
      ALEipUser user = ALEipUtils.getALEipUser(record.getOwnerId().intValue());
      if (user != null) {
        rd.setOwnerHasPhoto(user.hasPhoto());
      }
      rd.setNote(record.getNote());
      rd.setCreateDate(record.getCreateDate());
      rd.setUpdateDate(record.getUpdateDate());
      rd.setLoginUserId(uid);

      if (hasAttachmentAuthority()) {
        List<EipTMsgboardFile> list =
          getSelectQueryForFiles(record.getTopicId().intValue()).fetchList();
        if (list != null && list.size() > 0) {
          List<FileuploadBean> attachmentFileList =
            new ArrayList<FileuploadBean>();
          FileuploadBean filebean = null;
          EipTMsgboardFile file = null;
          int size = list.size();
          for (int i = 0; i < size; i++) {
            file = list.get(i);
            String realname = file.getFileName();
            javax.activation.DataHandler hData =
              new javax.activation.DataHandler(
                new javax.activation.FileDataSource(realname));

            filebean = new FileuploadBean();
            filebean.setFileId(file.getFileId().intValue());
            filebean.setFileName(realname);
            if (hData != null) {
              filebean.setContentType(hData.getContentType());
            }
            filebean.setIsImage(FileuploadUtils.isImage(realname));
            attachmentFileList.add(filebean);
          }
          rd.setAttachmentFiles(attachmentFileList);
        }
      }

      return rd;
    } catch (Exception e) {
      logger.error("[MsgboardTopicSelectData]", e);
      throw new ALDBErrorException();
    }
  }

  /**
   *
   * @return
   */
  public List<MsgboardCategoryResultData> getCategoryList() {
    if (hasAclCategoryList) {
      return categoryList;
    } else {
      return new ArrayList<MsgboardCategoryResultData>();
    }
  }

  /**
   * トピックの総数を返す． <BR>
   *
   * @return
   */
  public int getTopicSum() {
    return topicSum;
  }

  /**
   * @return
   *
   */
  @Override
  protected Attributes getColumnMap() {
    Attributes map = new Attributes();
    map.putValue("topic_name", EipTMsgboardTopic.TOPIC_NAME_PROPERTY);
    map.putValue("create_date", EipTMsgboardTopic.CREATE_DATE_PROPERTY);
    map.putValue("update_date", EipTMsgboardTopic.UPDATE_DATE_PROPERTY);
    map.putValue("category", EipTMsgboardCategory.CATEGORY_ID_PK_COLUMN);
    map.putValue(
      "category_name",
      EipTMsgboardTopic.EIP_TMSGBOARD_CATEGORY_PROPERTY
        + "."
        + EipTMsgboardCategory.CATEGORY_NAME_PROPERTY);
    map.putValue("owner_name", EipTMsgboardTopic.OWNER_ID_PROPERTY);
    map.putValue("update_user", EipTMsgboardTopic.UPDATE_USER_ID_PROPERTY);

    return map;
  }

  /**
   *
   * @param id
   * @return
   */
  public boolean isMatch(int id1, long id2) {
    return id1 == (int) id2;
  }

  public List<ALEipUser> getUsers(String groupname) {
    return ALEipUtils.getUsers(groupname);
  }

  public Object getParentTopic() {
    return parentTopic;
  }

  public List<MsgboardTopicResultData> getCoTopicList() {
    return coTopicList;
  }

  public int getUserId() {
    return uid;
  }

  public boolean showReplyForm() {
    return showReplyForm;
  }

  /**
   * アクセス権限チェック用メソッド。 アクセス権限の機能名を返します。
   *
   * @return
   */
  @Override
  public String getAclPortletFeature() {
    return ALAccessControlConstants.POERTLET_FEATURE_MSGBOARD_TOPIC;
  }

  /**
   * 他ユーザのトピックを編集する権限があるかどうかを返します。
   *
   * @return
   */
  public boolean hasAclUpdateTopicOthers() {
    return hasAclUpdateTopicOthers;
  }

  /**
   * 他ユーザのトピックを削除する権限があるかどうかを返します。
   *
   * @return
   */
  public boolean hasAclDeleteTopicOthers() {
    return hasAclDeleteTopicOthers;
  }

  public String getCurrentSearchWithSanitize() {
    return ALStringUtil.sanitizing(getCurrentSearch());
  }

  /**
   * @return target_keyword
   */
  public ALStringField getTargetKeyword() {
    return target_keyword;
  }

  /**
   * @return table_colum_num
   */
  public int getTableColumNum() {
    return table_colum_num;
  }

  /**
   * @param table_colum_num
   *          セットする table_colum_num
   */
  public void setTableColumNum(int table_colum_num) {
    this.table_colum_num = table_colum_num;
  }

  /**
   * 部署一覧を取得します
   *
   * @return postList
   */
  public List<ALEipGroup> getPostList() {
    return postList;
  }

  /**
   * 部署の一覧を取得する．
   *
   * @return
   */
  public Map<Integer, ALEipPost> getPostMap() {
    return ALEipManager.getInstance().getPostMap();
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

  public String getCategoryId() {
    return categoryId;
  }

  public String getPostId() {
    return postId;
  }

  private void updateCategoryName() {
    categoryName = "";
    if (categoryList != null) {
      for (int i = 0; i < categoryList.size(); i++) {
        String cid = categoryList.get(i).getCategoryId().toString();
        if (cid != null && categoryId != null) {
          if (cid.equals(categoryId.toString())) {
            categoryName = categoryList.get(i).getCategoryName().toString();
            return;
          }
        }
      }
    }
  }

  public String getCategoryName() {
    return categoryName;
  }

  private void updatePostName() {
    postName = "";
    for (int i = 0; i < postList.size(); i++) {
      String pid = postList.get(i).getName().toString();
      if (pid.equals(postId.toString())) {
        postName = postList.get(i).getAliasName().toString();
        return;
      }
    }
    Map<Integer, ALEipPost> map = ALEipManager.getInstance().getPostMap();
    for (Map.Entry<Integer, ALEipPost> item : map.entrySet()) {
      String pid = item.getValue().getGroupName().toString();
      if (pid.equals(postId.toString())) {
        postName = item.getValue().getPostName().toString();
        return;
      }
    }
  }

  public String getPostName() {
    return postName;
  }

  public boolean isFileUploadable() {
    return isFileUploadable;
  }

  /**
   * アクセス権限をチェックします。
   *
   * @return
   */
  @Override
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
