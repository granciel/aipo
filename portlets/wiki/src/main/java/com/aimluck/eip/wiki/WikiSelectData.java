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
package com.aimluck.eip.wiki;

import java.util.ArrayList;
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
import org.apache.jetspeed.util.template.JetspeedLink;
import org.apache.jetspeed.util.template.JetspeedLinkFactory;
import org.apache.turbine.util.RunData;
import org.apache.turbine.util.TurbineException;
import org.apache.velocity.context.Context;

import com.aimluck.commons.field.ALStringField;
import com.aimluck.eip.cayenne.om.portlet.EipTWiki;
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
import com.aimluck.eip.modules.actions.common.ALAction;
import com.aimluck.eip.orm.Database;
import com.aimluck.eip.orm.query.ResultList;
import com.aimluck.eip.orm.query.SelectQuery;
import com.aimluck.eip.services.accessctl.ALAccessControlConstants;
import com.aimluck.eip.util.ALEipUtils;
import com.aimluck.eip.wiki.util.WikiFileUtils;
import com.aimluck.eip.wiki.util.WikiUtils;

/**
 * Wiki検索データを管理するクラスです。 <BR>
 *
 */
public class WikiSelectData extends
    ALAbstractMultiFilterSelectData<EipTWiki, EipTWiki> implements ALData {

  /** logger */
  private static final JetspeedLogger logger = JetspeedLogFactoryService
    .getLogger(WikiSelectData.class.getName());

  private List<WikiResultData> topWikiList;

  /** 部署一覧 */
  private List<ALEipGroup> postList;

  /** 初期表示 */
  private int table_colum_num = 2;

  /** カテゴリの初期値を取得する */
  private String filterType = "";

  /** グループID */
  private String postId = "";

  /** グループ名 */
  private String postName = "";

  /** カテゴリ　ID */
  private String categoryId = "";

  /** カテゴリ名 */
  private String categoryName = "";

  /** ターゲット　 */
  private ALStringField target_keyword;

  private String baseInternalLink = null;

  private String baseImageLink = null;

  private String baseImageRawLink = null;

  private String entityId = null;

  private String tempNote = null;

  private String tempWikiNote = null;

  private boolean isTop = false;

  private long tempOwnerId;

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
    super.init(action, rundata, context);

    String sort = ALEipUtils.getTemp(rundata, context, LIST_SORT_STR);
    if (sort == null || sort.equals("")) {
      // default sort
      String sortStr = "update_date";
      ALEipUtils.setTemp(rundata, context, LIST_SORT_STR, sortStr);
      if ("update_date".equals(sortStr)) {
        ALEipUtils.setTemp(rundata, context, LIST_SORT_TYPE_STR, "desc");
      }
    }
    entityId = ALEipUtils.getTemp(rundata, context, ALEipConstants.ENTITY_ID);

    target_keyword = new ALStringField();
    super.init(action, rundata, context);

    postList = ALEipUtils.getMyGroups(rundata);

    try {
      JetspeedLink jsLink = JetspeedLinkFactory.getInstance(rundata);
      VelocityPortlet portlet = ALEipUtils.getPortlet(rundata, context);

      baseInternalLink =
        jsLink
          .getPortletById(portlet.getID())
          .addQueryData("template", "WikiInternalLinkScreen")
          .addQueryData("portletid", portlet.getID())
          .addQueryData("existWidgets", "marking_widgets_" + portlet.getID())
          .addQueryData("callback", "aipo.wiki.onLoadWikiDetail")
          .toString();

      String baseLink = jsLink.getPortletById(portlet.getID()).toString();
      baseImageLink = baseLink + "/template/WikiFileThumbnailScreen";
      baseImageRawLink =
        baseLink + "/template/FileuploadViewScreen/screen/WikiFileRawScreen";

    } catch (TurbineException e) {
      logger.error("init", e);
    }

  }

  /**
   *
   * @param rundata
   * @param context
   */
  public void loadTopWikiList(RunData rundata, Context context) {
    topWikiList = WikiUtils.loadTopWikiList(rundata);
    setCategory(rundata, context);
  }

  public boolean validateCategory() {
    if (StringUtils.isEmpty(categoryId)) {
      return true;
    }
    for (WikiResultData data : topWikiList) {
      if (data.getParentId().toString().equals(categoryId)) {
        return true;
      }
    }
    return false;
  }

  private void updateCategoryName() {
    categoryName = "";
    if (topWikiList == null) {
      return;
    }
    for (WikiResultData data : topWikiList) {
      if (data.getId().toString().equals(categoryId)) {
        categoryName = data.getName();
        return;
      }
    }
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
    for (WikiResultData data : topWikiList) {
      if (categoryId.equals(data.getId().toString())) {
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
   * @throws ALDBErrorException
   */
  @Override
  public ResultList<EipTWiki> selectList(RunData rundata, Context context) {
    try {
      if (WikiUtils.hasResetFlag(rundata, context)) {
        WikiUtils.resetFilter(rundata, context, this.getClass().getName());
        target_keyword.setValue("");
      } else {
        target_keyword.setValue(WikiUtils.getTargetKeyword(rundata, context));
      }

      SelectQuery<EipTWiki> query = getSelectQuery(rundata, context);
      buildSelectQueryForListView(query);
      buildSelectQueryForListViewSort(query, rundata, context);
      ResultList<EipTWiki> list = query.getResultList();
      setPageParam(list.getTotalCount());
      return list;
    } catch (Exception e) {
      logger.error("WikiSelectData.selectList", e);
      return null;
    }
  }

  private SelectQuery<EipTWiki> getSelectQuery(RunData rundata, Context context) {
    SelectQuery<EipTWiki> query = Database.query(EipTWiki.class);
    if ((target_keyword != null) && (!target_keyword.getValue().equals(""))) {
      // 選択したキーワードを指定する．
      String keyword = "%" + target_keyword.getValue() + "%";

      Expression exp =
        ExpressionFactory.likeExp(EipTWiki.WIKI_NAME_PROPERTY, keyword);
      Expression exp2 =
        ExpressionFactory.likeExp(EipTWiki.NOTE_PROPERTY, keyword);
      query.andQualifier(exp.orExp(exp2));
    }
    return buildSelectQueryForFilter(query, rundata, context);
  }

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

  @Override
  protected SelectQuery<EipTWiki> buildSelectQueryForFilter(
      SelectQuery<EipTWiki> query, RunData rundata, Context context) {
    if (current_filterMap.containsKey("category")) {
      // カテゴリを含んでいる場合デフォルトとは別にフィルタを用意
      List<String> categoryIds = current_filterMap.get("category");
      categoryId = categoryIds.get(0).toString();
      if (null == topWikiList) {
        topWikiList = WikiUtils.loadTopWikiList(rundata);
      }
      boolean existCategory = false;
      if (topWikiList != null && topWikiList.size() > 0) {
        for (WikiResultData category : topWikiList) {
          if (categoryId.equals(category.getId().toString())) {
            existCategory = true;
            break;
          }
        }

      }
      if (!existCategory) {
        categoryId = "";
        current_filterMap.remove("category");
      } else {
        Expression exp =
          ExpressionFactory.matchExp(EipTWiki.PARENT_ID_PROPERTY, categoryId);
        Expression exp2 =
          ExpressionFactory.matchDbExp(EipTWiki.WIKI_ID_PK_COLUMN, categoryId);
        query.andQualifier(exp.orExp(exp2));
      }

      updateCategoryName();
    }

    super.buildSelectQueryForFilter(query, rundata, context);

    if (current_filterMap.containsKey("post")) {
      // 部署を含んでいる場合デフォルトとは別にフィルタを用意

      List<String> postIds = current_filterMap.get("post");

      HashSet<Integer> userIds = new HashSet<Integer>();
      for (String post : postIds) {
        List<Integer> userId = ALEipUtils.getUserIds(post);
        userIds.addAll(userId);
      }
      if (userIds.isEmpty()) {
        userIds.add(-1);
      }
      Expression exp =
        ExpressionFactory.inExp(EipTWiki.UPDATE_USER_ID_PROPERTY, userIds);
      query.andQualifier(exp);

      postId = postIds.get(0).toString();
      updatePostName();
    }

    String search = ALEipUtils.getTemp(rundata, context, LIST_SEARCH_STR);

    if (search != null && !"".equals(search)) {
      current_search = search;
      Expression ex1 =
        ExpressionFactory.likeExp(EipTWiki.NOTE_PROPERTY, "%" + search + "%");
      Expression ex2 =
        ExpressionFactory.likeExp(EipTWiki.WIKI_NAME_PROPERTY, "%"
          + search
          + "%");
      SelectQuery<EipTWiki> q = Database.query(EipTWiki.class);
      q.andQualifier(ex1.orExp(ex2));
      List<EipTWiki> queryList = q.fetchList();
      List<Integer> resultid = new ArrayList<Integer>();
      for (EipTWiki item : queryList) {
        resultid.add(item.getWikiId());
      }
      if (resultid.size() == 0) {
        // 検索結果がないことを示すために-1を代入
        resultid.add(-1);
      }
      Expression ex =
        ExpressionFactory.inDbExp(EipTWiki.WIKI_ID_PK_COLUMN, resultid);
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
  protected Object getResultData(EipTWiki record) {
    try {
      WikiResultData rd = new WikiResultData();
      rd.initField();
      rd.setId(record.getWikiId().longValue());
      rd.setName(record.getWikiName());
      // rd.setCategoryId(record.getCategoryId().longValue());
      rd.setParentId(record.getParentId());
      if (record.getParentId() != 0) {
        rd.setParentName(WikiUtils
          .getEipTWiki(record.getParentId())
          .getWikiName());
      }
      rd.setUpdateUser(ALEipUtils
        .getALEipUser(record.getUpdateUserId())
        .getAliasName()
        .getValue());
      rd.setCreateDate(record.getCreateDate());
      rd.setUpdateDate(record.getUpdateDate());
      rd.setUpdateUserId(record.getUpdateUserId());
      return rd;
    } catch (Exception e) {
      logger.error("WikiSelectData.getResultData", e);
      return null;
    }
  }

  /**
   * 詳細データを取得します。 <BR>
   *
   * @param rundata
   * @param context
   * @return
   * @throws ALDBErrorException
   */
  @Override
  public EipTWiki selectDetail(RunData rundata, Context context)
      throws ALPageNotFoundException, ALDBErrorException {
    try {
      EipTWiki wiki = WikiUtils.getEipTWiki(rundata, context);
      return wiki;
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
  protected Object getResultDataDetail(EipTWiki record) {
    try {
      WikiResultData rd = new WikiResultData();
      rd.initField();
      String link =
        baseInternalLink
          + "&name=${title}&parentId="
          + String.valueOf(record.getParentId().intValue() == 0 ? record
            .getWikiId() : record.getParentId());
      rd.initalizeWikiModel(baseImageLink, link);

      // 登録ユーザ名の設定
      ALEipUser createdUser =
        ALEipUtils.getALEipUser(record.getCreateUserId().intValue());
      String createdUserName = createdUser.getAliasName().getValue();
      rd.setCreateUser(createdUserName);

      // 更新ユーザ名の設定
      String updatedUserName;
      if (record.getCreateUserId().equals(record.getUpdateUserId())) {
        updatedUserName = createdUserName;
      } else {
        ALEipUser updatedUser =
          ALEipUtils.getALEipUser(record.getUpdateUserId().intValue());
        updatedUserName = updatedUser.getAliasName().getValue();
      }
      rd.setUpdateUser(updatedUserName);
      rd.setId(record.getWikiId().longValue());
      rd.setName(record.getWikiName());
      // rd.setCategoryId(record.getCategoryId().longValue());
      // rd.setCategoryName(WikiUtils
      // .getEipTWikiCategory(record.getCategoryId())
      // .getCategoryName());
      rd.setNote(record.getNote());
      rd.setOwnerId(record.getCreateUserId());
      rd.setCreateUser(ALEipUtils
        .getALEipUser(record.getCreateUserId())
        .getAliasName()
        .getValue());
      rd.setUpdateUser(ALEipUtils
        .getALEipUser(record.getUpdateUserId())
        .getAliasName()
        .getValue());
      rd.setCreateDate(record.getCreateDate());
      rd.setUpdateDate(record.getUpdateDate());
      rd.setBaseInternalLink(baseImageLink);
      rd.setBaseImageRawLink(baseImageRawLink);

      if (hasAttachmentAuthority()) {
        rd.setAttachmentFiles(WikiFileUtils.getAttachmentFiles(record
          .getWikiId()));
      }

      rd.setParentId(record.getParentId());

      if (record.getParentId().intValue() != 0) {
        EipTWiki parentWiki = WikiUtils.getEipTWiki(record.getParentId());
        rd.setParentName(parentWiki.getWikiName());
      }

      tempOwnerId = rd.getOwnerId().getValue();

      return rd;
    } catch (Exception e) {
      logger.error("WikiSelectData.getResultDataDetail", e);
      return null;
    }
  }

  /**
   * @return
   *
   */
  @Override
  protected Attributes getColumnMap() {
    Attributes map = new Attributes();
    map.putValue("wiki_name", EipTWiki.WIKI_NAME_PROPERTY);
    map.putValue("parent_name", EipTWiki.PARENT_ID_PROPERTY);
    map.putValue("update_user", EipTWiki.UPDATE_USER_ID_PROPERTY);
    map.putValue("update_date", EipTWiki.UPDATE_DATE_PROPERTY);
    return map;
  }

  public String getCategoryId() {
    return categoryId;
  }

  public String getCategoryName() {
    return categoryName;
  }

  public List<WikiResultData> getTopWikiList() {
    return topWikiList;
  }

  /**
   * @return target_keyword
   */
  public ALStringField getTargetKeyword() {
    return target_keyword;
  }

  public List<ALEipGroup> getPostList() {
    return postList;
  }

  public Map<Integer, ALEipPost> getPostMap() {
    return ALEipManager.getInstance().getPostMap();
  }

  public void setTableColumNum(int table_colum_num) {
    this.table_colum_num = table_colum_num;
  }

  public int getTableColumNum() {
    return table_colum_num;
  }

  public String getPostName() {
    return postName;
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

  public void doViewDetailOne(ALAction action, RunData rundata, Context context) {
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

      String sesFilter = ALEipUtils.getTemp(rundata, context, LIST_FILTER_STR);
      String sesFilterType =
        ALEipUtils.getTemp(rundata, context, LIST_FILTER_TYPE_STR);

      String wikiId = WikiUtils.getWikiIdFromSession(sesFilter, sesFilterType);
      EipTWiki obj = null;
      String entityId = rundata.getParameters().getString("entityId", "");
      if (!StringUtils.isEmpty(entityId)) {
        try {
          // 子ページ表示
          EipTWiki eipTWiki = WikiUtils.getEipTWiki(Integer.parseInt(entityId));
          if (eipTWiki != null) {
            obj = eipTWiki;
          }
        } catch (Exception e) {
          logger.error("doViewDetailOne", e);
        }
      } else {
        if (WikiUtils.existWiki(wikiId, topWikiList)) {
          obj = WikiUtils.getEipTWiki(Integer.parseInt(wikiId));
        }
      }
      if (obj == null) {
        obj = WikiUtils.getEipTWikiOne();
      }

      if (obj != null) {
        data = getResultDataDetail(obj);
      }
      action.setResultData(this);
      action.putData(rundata, context);

      if (null != data) {
        Integer categoryId = 0;
        if (obj.getParentId() == 0) {
          categoryId = obj.getWikiId();
        } else {
          categoryId = obj.getParentId();
        }
        ALEipUtils.setTemp(rundata, context, LIST_FILTER_TYPE_STR, "category");
        ALEipUtils.setTemp(rundata, context, LIST_FILTER_STR, String
          .valueOf(categoryId));
        rundata.getParameters().setString("filtertype", "category");
        rundata.getParameters().setString("filter", String.valueOf(categoryId));
        setCategory(rundata, context);
        parseFilterMap(rundata, context);
      }

    } catch (ALPageNotFoundException ignore) {
    } catch (ALDBErrorException e) {
      logger.error("doViewDetailOne", e);
    } catch (ALPermissionException e) {
      logger.error("doViewDetailOne", e);
    }
  }

  public void setIsTop(boolean isTop) {
    this.isTop = isTop;
  }

  public boolean isTop() {
    return isTop;
  }

  public void setTempNote(String tn, RunData rundata, Context context) {
    WikiResultData result = new WikiResultData();
    result.initField();
    result.setNote(tn);
    tempNote = tn;

    // 新規作成でなければ、既にアップロードされている添付ファイルを取得する
    if (entityId != null) {
      result.setId(Long.parseLong(entityId));
      result.setOwnerId(tempOwnerId);
      result.setBaseImageRawLink(baseImageRawLink);
      result.setBaseInternalLink(baseImageLink);
      List<FileuploadBean> tempFileuploadList =
        WikiFileUtils.getAttachmentFiles(Integer.parseInt(entityId));
      result.setAttachmentFiles(tempFileuploadList);
    }

    tempWikiNote = result.getNote();
  }

  public String getTempNote() {
    return tempNote;
  }

  public String getTempWikiNote() {
    return tempWikiNote;
  }

}
