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
package com.aimluck.eip.timeline;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.activation.DataHandler;
import javax.activation.FileDataSource;

import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.jetspeed.portal.portlets.VelocityPortlet;
import org.apache.jetspeed.services.logging.JetspeedLogFactoryService;
import org.apache.jetspeed.services.logging.JetspeedLogger;
import org.apache.turbine.services.TurbineServices;
import org.apache.turbine.util.RunData;
import org.apache.turbine.util.StringUtils;
import org.apache.velocity.context.Context;

import com.aimluck.commons.field.ALStringField;
import com.aimluck.commons.utils.ALStringUtil;
import com.aimluck.eip.cayenne.om.portlet.EipTMsgboardCategory;
import com.aimluck.eip.cayenne.om.portlet.EipTMsgboardCategoryMap;
import com.aimluck.eip.cayenne.om.portlet.EipTMsgboardTopic;
import com.aimluck.eip.cayenne.om.portlet.EipTScheduleMap;
import com.aimluck.eip.cayenne.om.portlet.EipTTimeline;
import com.aimluck.eip.cayenne.om.portlet.EipTTimelineFile;
import com.aimluck.eip.cayenne.om.portlet.EipTTimelineMap;
import com.aimluck.eip.cayenne.om.portlet.EipTTimelineUrl;
import com.aimluck.eip.cayenne.om.portlet.EipTTodo;
import com.aimluck.eip.common.ALAbstractSelectData;
import com.aimluck.eip.common.ALBaseUser;
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
import com.aimluck.eip.orm.Database;
import com.aimluck.eip.orm.query.Operations;
import com.aimluck.eip.orm.query.ResultList;
import com.aimluck.eip.orm.query.SelectQuery;
import com.aimluck.eip.services.accessctl.ALAccessControlConstants;
import com.aimluck.eip.services.accessctl.ALAccessControlFactoryService;
import com.aimluck.eip.services.accessctl.ALAccessControlHandler;
import com.aimluck.eip.timeline.util.TimelineUtils;
import com.aimluck.eip.util.ALEipUtils;

/**
 * タイムライントピックの検索データを管理するクラスです。 <BR>
 *
 */
public class TimelineSelectData extends
    ALAbstractSelectData<EipTTimeline, EipTTimeline> implements ALData {

  /** <code>TARGET_GROUP_NAME</code> グループによる表示切り替え用変数の識別子 */
  private final String TARGET_GROUP_NAME = "target_group_name";

  private final String TARGET_DISPLAY_NAME = "target_display_name";

  /** logger */
  private static final JetspeedLogger logger = JetspeedLogFactoryService
    .getLogger(TimelineSelectData.class.getName());

  /** トピックの総数 */
  private int topicSum;

  /** トピックの高さ（通常画面） */
  private int contentHeight;

  /** トピックの高さ（最大化画面） */
  private int contentHeightMax;

  /** スクロールの位置 */
  private int scrollTop;

  /** ログインユーザ ID */
  private int uid;

  private ALBaseUser baseuser;

  private ALEipUser user;

  /** 返信フォーム表示の有無（トピック詳細表示） */
  private final boolean showReplyForm = false;

  /** アクセス権限の機能名「タイムライン（他ユーザーの投稿）管理者」の一覧表示権限 */
  private boolean hasAclTimelineListOther;

  /** アクセス権限の機能名「タイムライン（他ユーザーの投稿）管理者」の削除権限 */
  private boolean hasAclTimelineDeleteOther;

  /** アクセス権限の機能名「タイムライン（自分の投稿）管理者」の追加権限 */
  private boolean hasAclTimelineInsert;

  /** アクセス権限の機能名「タイムライン（自分の投稿）管理者」の削除権限 */
  private boolean hasAclTimelineDelete;

  /** アクセス権限の機能名「タイムライン（コメント）管理者」の追加権限 */
  private boolean hasAclTimelineCommentInsert;

  /** アクセス権限の機能名「タイムライン（コメント）管理者」の削除権限 */
  private boolean hasAclTimelineCommentDelete;

  /** アクセス権限の機能名「掲示板（トピック）管理者」の一覧表示権限 */
  private boolean hasAclTopicList;

  /** 他ユーザーの作成したトピックの編集権限 */
  private boolean hasAclUpdateTopicOthers;

  /** 他ユーザーの作成したトピックの削除権限 */
  private boolean hasAclDeleteTopicOthers;

  private ALStringField target_keyword;

  private List<Object> list;

  private final List<Integer> users = new ArrayList<Integer>();

  /** <code>userList</code> 表示切り替え用のユーザリスト */
  private List<ALEipUser> userList = null;

  private final List<Integer> useridList = new ArrayList<Integer>();

  /** <code>target_group_name</code> 表示対象の部署名 */
  protected String target_group_name;

  /** <code>target_display_name</code> 記事の絞り込み */
  protected String target_display_name;

  private String displayParam = "";

  /** <code>myGroupList</code> グループリスト（My グループと部署） */
  private List<ALEipGroup> myGroupList = null;

  private boolean isAdmin = false;

  private boolean isFileUploadable;

  /** 添付ファイル追加へのアクセス権限の有無 */
  private boolean hasAttachmentInsertAuthority;

  /** AppNameからportletIdを取得するハッシュ */
  private HashMap<String, String> portletIdFromAppId;

  /** アクセス権限の機能名（ブログ（他ユーザの記事））の一覧権限を持っているか **/
  private boolean hasBlogOtherAclList;

  /** アクセス権限の機能名（スケジュール（他ユーザーの予定））の一覧表示権限を持っているか **/
  private boolean hasScheduleOtherAclList;

  /** アクセス権限の機能名（ToDo（自分のToDo））の一覧表示権限を持っているか */
  private boolean hasTodoAclList;

  /** アクセス権限の機能名（ToDo（他ユーザーのToDo））の一覧表示権限を持っているか **/
  private boolean hasTodoOtherAclList;

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

    portletIdFromAppId = ALEipUtils.getPortletFromAppIdMap(rundata);

    ALEipUtils.setTemp(rundata, context, LIST_SORT_STR, "update_date");
    ALEipUtils.setTemp(
      rundata,
      context,
      LIST_SORT_TYPE_STR,
      ALEipConstants.LIST_SORT_TYPE_DESC);

    uid = ALEipUtils.getUserId(rundata);
    baseuser = (ALBaseUser) rundata.getUser();
    user = ALEipUtils.getALEipUser(uid);
    isAdmin = ALEipUtils.isAdmin(uid);

    // My グループの一覧を取得する．
    List<ALEipGroup> myGroups = ALEipUtils.getMyGroups(rundata);
    myGroupList = new ArrayList<ALEipGroup>();
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

      String displayFilter =
        ALEipUtils.getTemp(rundata, context, TARGET_DISPLAY_NAME);
      if (displayFilter == null || displayFilter.equals("")) {
        VelocityPortlet portlet = ALEipUtils.getPortlet(rundata, context);
        displayFilter =
          portlet.getPortletConfig().getInitParameter("p4a-group");
        if (displayFilter != null) {
          ALEipUtils.setTemp(
            rundata,
            context,
            TARGET_DISPLAY_NAME,
            displayFilter);
        }
      }

      ALAccessControlFactoryService aclservice =
        (ALAccessControlFactoryService) ((TurbineServices) TurbineServices
          .getInstance())
          .getService(ALAccessControlFactoryService.SERVICE_NAME);
      ALAccessControlHandler aclhandler = aclservice.getAccessControlHandler();

      // タイムライン（他ユーザーの投稿）一覧表示権限
      hasAclTimelineListOther =
        aclhandler.hasAuthority(
          uid,
          ALAccessControlConstants.POERTLET_FEATURE_TIMELINE_POST_OTHER,
          ALAccessControlConstants.VALUE_ACL_LIST);
      // タイムライン（他ユーザーの投稿）削除権限
      hasAclTimelineDeleteOther =
        aclhandler.hasAuthority(
          uid,
          ALAccessControlConstants.POERTLET_FEATURE_TIMELINE_POST_OTHER,
          ALAccessControlConstants.VALUE_ACL_DELETE);
      // タイムライン（自分の投稿）の追加権限
      hasAclTimelineInsert =
        aclhandler.hasAuthority(
          uid,
          ALAccessControlConstants.POERTLET_FEATURE_TIMELINE_POST,
          ALAccessControlConstants.VALUE_ACL_INSERT);
      // タイムライン（自分の投稿）の削除権限
      hasAclTimelineDelete =
        aclhandler.hasAuthority(
          uid,
          ALAccessControlConstants.POERTLET_FEATURE_TIMELINE_POST,
          ALAccessControlConstants.VALUE_ACL_DELETE);
      // タイムライン（コメント）の追加権限
      hasAclTimelineCommentInsert =
        aclhandler.hasAuthority(
          uid,
          ALAccessControlConstants.POERTLET_FEATURE_TIMELINE_COMMENT,
          ALAccessControlConstants.VALUE_ACL_INSERT);
      // タイムライン（コメント）の削除権限
      hasAclTimelineCommentDelete =
        aclhandler.hasAuthority(
          uid,
          ALAccessControlConstants.POERTLET_FEATURE_TIMELINE_COMMENT,
          ALAccessControlConstants.VALUE_ACL_DELETE);

      /** 更新情報についての一覧表示権限のチェック **/
      hasScheduleOtherAclList =
        aclhandler.hasAuthority(
          uid,
          ALAccessControlConstants.POERTLET_FEATURE_SCHEDULE_OTHER,
          ALAccessControlConstants.VALUE_ACL_LIST);
      hasAclTopicList =
        aclhandler.hasAuthority(
          uid,
          ALAccessControlConstants.POERTLET_FEATURE_MSGBOARD_TOPIC,
          ALAccessControlConstants.VALUE_ACL_LIST);

      /** hasBlogOtherAclListの権限チェック **/
      hasBlogOtherAclList =
        aclhandler.hasAuthority(
          uid,
          ALAccessControlConstants.POERTLET_FEATURE_BLOG_ENTRY_OTHER,
          ALAccessControlConstants.VALUE_ACL_LIST);

      /** hasTodoAclListの権限チェック **/
      hasTodoAclList =
        aclhandler.hasAuthority(
          uid,
          ALAccessControlConstants.POERTLET_FEATURE_TODO_TODO_SELF,
          ALAccessControlConstants.VALUE_ACL_LIST);

      /** hasTodoOtherAclListの権限チェック **/
      hasTodoOtherAclList =
        aclhandler.hasAuthority(
          uid,
          ALAccessControlConstants.POERTLET_FEATURE_TODO_TODO_OTHER,
          ALAccessControlConstants.VALUE_ACL_LIST);

    } catch (Exception ex) {
      logger.error("timeline", ex);
    }

    target_keyword = new ALStringField();
    isFileUploadable = ALEipUtils.isFileUploadable(rundata);
  }

  /**
   * My グループの一覧を取得する．
   *
   * @return
   */
  public List<ALEipGroup> getMyGroupList() {
    return myGroupList;
  }

  /**
   * 一覧データを取得します。 <BR>
   *
   * @param rundata
   * @param context
   * @return
   */
  @Override
  public ResultList<EipTTimeline> selectList(RunData rundata, Context context) {
    try {

      // 指定グループや指定ユーザをセッションに設定する．
      setupLists(rundata, context);

      // 他ユーザーの投稿の一覧表示権限がない場合には自分の投稿のみを表示する
      if (!hasAclTimelineListOther) {
        useridList.clear();
        useridList.add(uid);
        // ガイドユーザー表示用
        useridList.add(2);
      }

      if (TimelineUtils.hasResetFlag(rundata, context)) {
        TimelineUtils.resetKeyword(rundata, context);
        target_keyword.setValue("");
      } else {
        target_keyword.setValue(TimelineUtils
          .getTargetKeyword(rundata, context));
      }

      ResultList<EipTTimeline> list = new ResultList<EipTTimeline>();

      if ((useridList != null && useridList.size() > 0)) {
        // 表示するカラムのみデータベースから取得する．
        list =
          TimelineUtils.getTimelineList(
            uid,
            Arrays.asList(0),
            null,
            current_page,
            getRowsNum(),
            0,
            useridList,
            target_keyword.getValue(),
            displayParam);
      }
      return list;
    } catch (Exception ex) {
      logger.error("timeline", ex);
      return null;
    }
  }

  public ResultList<EipTTimeline> selectListNew(RunData rundata, Context context) {
    try {

      int minId =
        Integer.valueOf(ALEipUtils.getParameter(
          rundata,
          context,
          "lastTimelineId"));

      // 表示するカラムのみデータベースから取得する．
      ResultList<EipTTimeline> list =
        TimelineUtils.getTimelineList(
          uid,
          Arrays.asList(0),
          null,
          0,
          0,
          minId,
          useridList,
          null,
          displayParam);

      return list;
    } catch (Exception ex) {
      logger.error("timeline", ex);
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
  protected SelectQuery<EipTTimeline> getSelectQuery(RunData rundata,
      Context context) {

    SelectQuery<EipTTimeline> query = Database.query(EipTTimeline.class);
    query.where(Operations.eq(EipTTimeline.PARENT_ID_PROPERTY, Integer
      .valueOf(0)));

    return query;
  }

  /**
   * ResultData に値を格納して返します。（一覧データ） <BR>
   *
   * @param obj
   * @return
   */
  @Override
  protected Object getResultData(EipTTimeline record) {
    try {
      TimelineResultData rd = new TimelineResultData();
      rd.initField();
      rd.setTimelineId(record.getTimelineId().longValue());
      rd.setNote(record.getNote());
      rd.setOwnerId(record.getOwnerId().longValue());
      rd.setCreateDate(record.getCreateDate());
      rd.setUpdateDate(record.getUpdateDate());
      rd.setTimelineType(record.getTimelineType());
      rd.setAppId(record.getAppId());
      rd.setParams(record.getParams());
      rd.setLike(record.isLike());
      rd.setLikeCount(record.getLikeCount());
      rd.setKeyword(target_keyword.getValue());
      String AppId = record.getAppId();
      // ToDoUtils.java・BlogUtils.javaに修正を加えてあるので、以下の６行はその内不要になる。
      if ("todo".equals(AppId)) {
        AppId = "ToDo";
      }
      if ("blog".equals(AppId)) {
        AppId = "Blog";
      }
      rd.setPortletId(portletIdFromAppId.get(AppId));

      if (!users.contains(record.getOwnerId())) {
        users.add(record.getOwnerId());
      }

      return rd;
    } catch (Exception ex) {
      logger.error("timeline", ex);
      return null;
    }
  }

  /**
   * ResultData に値を格納して返します。（詳細データ） <BR>
   *
   * @param obj
   * @return
   */
  @Override
  protected Object getResultDataDetail(EipTTimeline record)
      throws ALPageNotFoundException, ALDBErrorException {

    TimelineResultData rd = (TimelineResultData) getResultData(record);

    Integer id = record.getTimelineId();
    TimelineLikeSelectData ls = new TimelineLikeSelectData();
    List<TimelineLikeResultData> likeList =
      ls.getLikeList(record.getTimelineId());
    rd.setLikeList(likeList);
    rd.setLikeCount(likeList.size());
    for (TimelineLikeResultData lrd : likeList) {
      int value = (int) lrd.getUserId().getValue();
      if (value == uid) {
        rd.setLike(true);
        break;
      }
    }

    // 更新情報
    Map<Integer, List<TimelineResultData>> activitiesMap =
      getActivities(Arrays.asList(id));

    // コメント
    Map<Integer, List<TimelineResultData>> commentsMap =
      getComments(Arrays.asList(id));

    // URL
    Map<Integer, List<TimelineUrlResultData>> urlsMap =
      getUrls(Arrays.asList(id));

    // ファイル
    Map<Integer, List<FileuploadBean>> filesMap = getFiles(Arrays.asList(id));

    rd.setCoTopicList(commentsMap.get(id));
    rd.setCoActivityList(activitiesMap.get(id));
    rd.setUrlList(urlsMap.get(id));
    rd.setAttachmentFileList(filesMap.get(id));
    rd.setReplyCount(rd.getCoTopicList().size());
    rd.setParentId(record.getParentId().longValue());
    rd.setTimelineType(record.getTimelineType());

    loadAggregateUsers();

    return rd;
  }

  @Override
  public EipTTimeline selectDetail(RunData rundata, Context context)
      throws ALDBErrorException, ALPageNotFoundException {
    try {
      EipTTimeline timeline =
        TimelineUtils.getEipTTimelineParentEntry(rundata, context);
      return timeline;
    } catch (ALPageNotFoundException pageNotFound) {
      throw pageNotFound;
    }
  }

  protected Map<Integer, List<TimelineResultData>> getComments(
      List<Integer> parentIds) {
    List<EipTTimeline> list =
      TimelineUtils.getTimelineList(
        uid,
        parentIds,
        "T",
        -1,
        -1,
        0,
        null,
        null,
        "");
    Map<Integer, List<TimelineResultData>> result =
      new HashMap<Integer, List<TimelineResultData>>(parentIds.size());
    for (EipTTimeline model : list) {
      Integer id = model.getParentId();
      List<TimelineResultData> rdList = result.get(id);
      if (rdList == null) {
        rdList = new ArrayList<TimelineResultData>();
      }
      rdList.add((TimelineResultData) getResultData(model));
      result.put(id, rdList);
    }

    return result;
  }

  protected Map<Integer, List<TimelineResultData>> getActivities(
      List<Integer> parentIds) {
    List<EipTTimeline> list =
      TimelineUtils.getTimelineList(
        uid,
        parentIds,
        "A",
        -1,
        -1,
        0,
        useridList,
        target_keyword.toString(),
        "");

    /* ブログ（他ユーザの記事）の権限を持っていない場合、listからブログの情報を削除 */
    if (!hasBlogOtherAclList) {
      for (int i = list.size() - 1; i >= 0; i--) {
        if (list.get(i).getAppId().equals("Blog")
          && !list.get(i).getOwnerId().equals(uid)) {
          list.remove(i);
        }
      }
    }

    /* スケジュール（他ユーザーの予定）の権限を持っていない場合、listから自分が関係しないスケジュールの情報を削除 */
    if (!hasScheduleOtherAclList) {
      ArrayList<Integer> scheduleIdList = new ArrayList<Integer>();
      for (EipTTimeline model : list) {
        if (model.getParams() != null
          && !"".equals(model.getParams())
          && model.getAppId() != null
          && !"".equals(model.getAppId())) {
          if (model.getAppId().equals("Schedule")) {
            Matcher m =
              Pattern.compile("entityid=([0-9]+)").matcher(model.getParams());
            if (m.find()) {
              Integer scheduleId = Integer.parseInt(m.group(1));
              scheduleIdList.add(scheduleId);
            }
          }
        }
      }

      if (scheduleIdList != null && scheduleIdList.size() > 0) {
        // eip_t_schedule_mapから、schedule_idがscheduleIdListに含まれ、
        // user_idが自分のもののリストを得る
        List<EipTScheduleMap> scheduleMapList =
          TimelineUtils.getRelatedEipTScheduleMap(uid, scheduleIdList);
        ArrayList<Integer> relatedScheduleIdList = new ArrayList<Integer>();
        if (scheduleMapList != null && scheduleMapList.size() > 0) {
          for (EipTScheduleMap eipTScheduleMap : scheduleMapList) {
            relatedScheduleIdList.add(eipTScheduleMap.getScheduleId());
          }
        }
        for (Iterator<EipTTimeline> iter = list.iterator(); iter.hasNext();) {
          EipTTimeline tmpEipTTimeline = iter.next();
          if (uid != tmpEipTTimeline.getOwnerId().intValue()) {
            if (tmpEipTTimeline.getParams() != null
              && !"".equals(tmpEipTTimeline.getParams())
              && tmpEipTTimeline.getAppId() != null
              && !"".equals(tmpEipTTimeline.getAppId())) {
              if (tmpEipTTimeline.getAppId().equals("Schedule")) {
                Matcher m =
                  Pattern.compile("entityid=([0-9]+)").matcher(
                    tmpEipTTimeline.getParams());
                if (m.find()) {
                  Integer scheduleId = Integer.parseInt(m.group(1));
                  if (relatedScheduleIdList == null
                    || (relatedScheduleIdList.size() == 0 || !relatedScheduleIdList
                      .contains(scheduleId))) {
                    // relatedScheduleIdListが空 or
                    // relatedScheduleIdListに含まれない時は削除
                    iter.remove();
                  }
                }
              }// スケジュール以外のTimeline
            }
          }// 自分がオーナーのTimeline
        }
      }
    }

    removePrivateMsgboardTopic(list);
    removePrivateTodo(list);

    Map<Integer, List<TimelineResultData>> result =
      new HashMap<Integer, List<TimelineResultData>>(parentIds.size());
    for (EipTTimeline model : list) {
      Integer id = model.getParentId();
      List<TimelineResultData> rdList = result.get(id);
      if (rdList == null) {
        rdList = new ArrayList<TimelineResultData>();
      }
      rdList.add((TimelineResultData) getResultData(model));
      result.put(id, rdList);
    }

    return result;
  }

  protected Map<Integer, List<TimelineUrlResultData>> getUrls(
      List<Integer> parentIds) {
    if (parentIds == null || parentIds.size() == 0) {
      return new HashMap<Integer, List<TimelineUrlResultData>>();
    }
    SelectQuery<EipTTimelineUrl> query = Database.query(EipTTimelineUrl.class);
    query.where(Operations.in(EipTTimelineUrl.TIMELINE_ID_PROPERTY, parentIds));

    List<EipTTimelineUrl> list = query.fetchList();
    Map<Integer, List<TimelineUrlResultData>> result =
      new HashMap<Integer, List<TimelineUrlResultData>>(parentIds.size());
    for (EipTTimelineUrl model : list) {
      Integer id = model.getTimelineId();
      List<TimelineUrlResultData> rdList = result.get(id);
      if (rdList == null) {
        rdList = new ArrayList<TimelineUrlResultData>();
      }

      TimelineUrlResultData rd = new TimelineUrlResultData();
      rd.initField();
      rd.setTimelineUrlId(model.getTimelineUrlId().longValue());
      rd.setTimelineId(model.getTimelineId().longValue());
      rd.setThumbnail(model.getThumbnail());
      rd.setTitle(model.getTitle());
      rd.setUrl(model.getUrl());
      rd.setBody(model.getBody());
      boolean flag = false;
      if (model.getThumbnail() != null) {
        flag = true;
      }
      rd.setThumbnailFlag(flag);

      String url = model.getUrl();
      if (TimelineUtils.isYoutubeUrl(url)) {
        rd.setYoutubeId(TimelineUtils.getYoutubeId(url));
        rd.setYoutubeFlag(true);
      }
      rdList.add(rd);

      result.put(id, rdList);
    }

    return result;
  }

  protected Map<Integer, List<FileuploadBean>> getFiles(List<Integer> parentIds) {
    if (parentIds == null || parentIds.size() == 0 || !hasAttachmentAuthority()) {
      return new HashMap<Integer, List<FileuploadBean>>();
    }
    SelectQuery<EipTTimelineFile> query =
      Database.query(EipTTimelineFile.class);
    query
      .where(Operations.in(EipTTimelineFile.TIMELINE_ID_PROPERTY, parentIds));

    query.orderAscending(EipTTimelineFile.UPDATE_DATE_PROPERTY);
    query.orderAscending(EipTTimelineFile.FILE_PATH_PROPERTY);

    List<EipTTimelineFile> list = query.fetchList();
    Map<Integer, List<FileuploadBean>> result =
      new HashMap<Integer, List<FileuploadBean>>(parentIds.size());
    for (EipTTimelineFile model : list) {
      Integer id = model.getTimelineId();
      List<FileuploadBean> rdList = result.get(id);
      if (rdList == null) {
        rdList = new ArrayList<FileuploadBean>();
      }

      String realname = model.getFileName();
      DataHandler hData = new DataHandler(new FileDataSource(realname));

      FileuploadBean filebean = new FileuploadBean();
      filebean.setFileId(model.getFileId().intValue());
      filebean.setFileName(realname);
      if (hData != null) {
        filebean.setContentType(hData.getContentType());
      }
      filebean.setIsImage(FileuploadUtils.isImage(realname));
      rdList.add(filebean);
      result.put(id, rdList);
    }

    return result;
  }

  @Override
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

      // 投稿
      ResultList<EipTTimeline> resultList = selectList(rundata, context);
      if (resultList == null) {
        return false;
      }
      List<Integer> parentIds = new ArrayList<Integer>(resultList.size());
      for (EipTTimeline model : resultList) {
        parentIds.add(model.getTimelineId());
      }

      // 更新情報
      Map<Integer, List<TimelineResultData>> activitiesMap =
        getActivities(parentIds);

      // コメント
      List<Integer> commentIds = new ArrayList<Integer>();
      commentIds.addAll(parentIds);
      Iterator<List<TimelineResultData>> activitiesIter =
        activitiesMap.values().iterator();
      while (activitiesIter.hasNext()) {
        List<TimelineResultData> next = activitiesIter.next();
        for (TimelineResultData rd : next) {
          commentIds.add(Integer.valueOf((int) rd.getTimelineId().getValue()));
        }
      }

      Map<Integer, List<TimelineResultData>> commentsMap =
        getComments(commentIds);

      // URL
      Map<Integer, List<TimelineUrlResultData>> urlsMap = getUrls(parentIds);

      // ファイル
      Map<Integer, List<FileuploadBean>> filesMap = getFiles(parentIds);

      // ユーザー

      if (resultList.getTotalCount() > 0) {
        setPageParam(resultList.getTotalCount());
      }
      list = new ArrayList<Object>();
      for (EipTTimeline model : resultList) {
        Object object = getResultData(model);
        TimelineResultData rd = (TimelineResultData) object;

        rd.setCoTopicList(commentsMap.get(model.getTimelineId()));
        rd.setCoActivityList(activitiesMap.get(model.getTimelineId()));
        rd.setUrlList(urlsMap.get(model.getTimelineId()));
        rd.setAttachmentFileList(filesMap.get(model.getTimelineId()));
        rd.setReplyCount(rd.getCoTopicList().size());

        List<TimelineResultData> coac = rd.getCoActivityList();

        // 権限のあるアクティビティのみ表示する
        for (Iterator<TimelineResultData> iter = coac.iterator(); iter
          .hasNext();) {
          TimelineResultData coac_item = iter.next();
          coac_item.setCoTopicList(commentsMap.get(Integer
            .valueOf((int) coac_item.getTimelineId().getValue())));
          coac_item.setReplyCount(coac_item.getCoTopicList().size());

          SelectQuery<EipTTimelineMap> query_map =
            Database.query(EipTTimelineMap.class);
          Expression exp1 =
            ExpressionFactory.matchExp(
              EipTTimelineMap.EIP_TTIMELINE_PROPERTY,
              coac_item.getTimelineId().getValue());
          query_map.setQualifier(exp1);
          List<EipTTimelineMap> data_map = query_map.fetchList();

          List<String> userlist = new ArrayList<String>();
          for (int j = 0; j < data_map.size(); j++) {
            userlist.add(data_map.get(j).getLoginName());
          }

          if (!(user.getUserId().toString().equals(
            coac_item.getOwnerId().toString())
            || userlist.contains(user.getName().toString()) || userlist
              .contains("-1"))) {
            iter.remove();
          }
        }

        if (!(rd.getCoActivityList().size() == 0
          && rd.getCoTopicList().size() == 0 && rd.getNote().equals(""))) {
          list.add(rd);
        }
      }

      loadAggregateUsers();

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

  public boolean doViewListNew(ALAction action, RunData rundata, Context context) {
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

      // 投稿
      ResultList<EipTTimeline> resultList = selectListNew(rundata, context);
      if (resultList == null) {
        return false;
      }
      List<Integer> parentIds = new ArrayList<Integer>(resultList.size());
      for (EipTTimeline model : resultList) {
        parentIds.add(model.getTimelineId());
      }

      // 更新情報
      Map<Integer, List<TimelineResultData>> activitiesMap =
        getActivities(parentIds);

      // コメント
      List<Integer> commentIds = new ArrayList<Integer>();
      commentIds.addAll(parentIds);
      Iterator<List<TimelineResultData>> activitiesIter =
        activitiesMap.values().iterator();
      while (activitiesIter.hasNext()) {
        List<TimelineResultData> next = activitiesIter.next();
        for (TimelineResultData rd : next) {
          commentIds.add(Integer.valueOf((int) rd.getTimelineId().getValue()));
        }
      }

      Map<Integer, List<TimelineResultData>> commentsMap =
        getComments(commentIds);

      // URL
      Map<Integer, List<TimelineUrlResultData>> urlsMap = getUrls(parentIds);

      // ファイル
      Map<Integer, List<FileuploadBean>> filesMap = getFiles(parentIds);

      // ユーザー

      if (resultList.getTotalCount() > 0) {
        setPageParam(resultList.getTotalCount());
      }
      list = new ArrayList<Object>();
      for (EipTTimeline model : resultList) {
        Object object = getResultData(model);
        TimelineResultData rd = (TimelineResultData) object;

        rd.setCoTopicList(commentsMap.get(model.getTimelineId()));
        rd.setCoActivityList(activitiesMap.get(model.getTimelineId()));
        rd.setUrlList(urlsMap.get(model.getTimelineId()));
        rd.setAttachmentFileList(filesMap.get(model.getTimelineId()));
        rd.setReplyCount(rd.getCoTopicList().size());

        List<TimelineResultData> coac = rd.getCoActivityList();

        // 権限のあるアクティビティのみ表示する
        for (Iterator<TimelineResultData> iter = coac.iterator(); iter
          .hasNext();) {
          TimelineResultData coac_item = iter.next();
          coac_item.setCoTopicList(commentsMap.get(Integer
            .valueOf((int) coac_item.getTimelineId().getValue())));
          coac_item.setReplyCount(coac_item.getCoTopicList().size());

          SelectQuery<EipTTimelineMap> query_map =
            Database.query(EipTTimelineMap.class);
          Expression exp1 =
            ExpressionFactory.matchExp(
              EipTTimelineMap.EIP_TTIMELINE_PROPERTY,
              coac_item.getTimelineId().getValue());
          query_map.setQualifier(exp1);
          List<EipTTimelineMap> data_map = query_map.fetchList();

          List<String> userlist = new ArrayList<String>();
          for (int j = 0; j < data_map.size(); j++) {
            userlist.add(data_map.get(j).getLoginName());
          }

          if (!(user.getUserId().toString().equals(
            coac_item.getOwnerId().toString())
            || userlist.contains(user.getName().toString()) || userlist
              .contains("-1"))) {
            iter.remove();
          }
        }

        if (!(rd.getCoActivityList().size() == 0
          && rd.getCoTopicList().size() == 0 && rd.getNote().equals(""))) {
          list.add(rd);
        }
      }

      loadAggregateUsers();

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
      // 「いいね」の表示画面を見るためには、詳細表示権限の代わりにタイムライン（自分の投稿）の一覧表示権限が必要
      doCheckAclPermission(
        rundata,
        context,
        ALAccessControlConstants.VALUE_ACL_LIST);
      doCheckAttachmentAclPermission(
        rundata,
        context,
        ALAccessControlConstants.VALUE_ACL_EXPORT);
      action.setMode(ALEipConstants.MODE_DETAIL);
      EipTTimeline obj = selectDetail(rundata, context);
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

  private void removePrivateMsgboardTopic(List<EipTTimeline> list) {

    if (!hasAclTopicList) {
      list.removeIf(obj -> (obj.getAppId().equals("Msgboard")));
      return;
    }

    /* listから自分が関係しないmsgboardの情報を削除 */

    List<Integer> ids = new ArrayList<Integer>();
    for (EipTTimeline obj : list) {
      if ("Msgboard".equals(obj.getAppId())) {
        ids.add(Integer.parseInt(obj.getExternalId()));
      }
    }
    if (ids.size() == 0) {
      return;
    }

    // MsgboardTopicSelectData.getSelectQuery()で取得出来るtopicIdだけをtopicListに格納する
    List<EipTMsgboardTopic> topicList = null;
    {
      SelectQuery<EipTMsgboardTopic> query =
        Database.query(EipTMsgboardTopic.class);

      Expression exp1 =
        ExpressionFactory.matchExp(
          EipTMsgboardTopic.PARENT_ID_PROPERTY,
          Integer.valueOf(0));
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
          Integer.valueOf(uid));

      query.andQualifier((exp01.andExp(exp02.orExp(exp03))).orExp(exp11
        .andExp(exp12)));

      Expression exp001 =
        ExpressionFactory.inDbExp(EipTMsgboardTopic.TOPIC_ID_PK_COLUMN, ids);
      query.andQualifier(exp001);

      query.distinct(true);

      topicList = query.fetchList();
    }

    // topicListからidを抜き出す
    List<Integer> topicIdList = new ArrayList<Integer>();
    for (EipTMsgboardTopic obj : topicList) {
      topicIdList.add(obj.getTopicId());
    }
    // listのなかでIDがtopicIdListに入っていないものを削除
    list.removeIf(obj -> (obj.getAppId().equals("Msgboard") && !topicIdList
      .contains(Integer.parseInt(obj.getExternalId()))));

  }

  private void removePrivateTodo(List<EipTTimeline> list) {

    if (!hasTodoAclList) {
      list.removeIf(obj -> (obj.getAppId().equals("ToDo")));
      return;
    }

    /* listから自分が関係しないToDoの情報を削除 */
    List<Integer> ids = new ArrayList<Integer>();

    for (EipTTimeline obj : list) {
      if ("ToDo".equals(obj.getAppId())) {
        ids.add(Integer.parseInt(obj.getExternalId()));
      }
    }
    if (ids.size() == 0) {
      return;
    }

    // TodoSelectData.getSelectQuery()で取得出来るtopicIdだけをtopicListに格納する
    List<EipTTodo> todoList = null;
    {
      SelectQuery<EipTTodo> query = Database.query(EipTTodo.class);

      // アクセス制御

      Expression exp01 =
        ExpressionFactory.inDbExp(EipTTodo.TODO_ID_PK_COLUMN, ids);

      Expression exp001 =
        ExpressionFactory.matchExp(EipTTodo.PUBLIC_FLAG_PROPERTY, "T");

      Expression exp002 =
        ExpressionFactory.matchExp(EipTTodo.USER_ID_PROPERTY, Integer
          .valueOf(uid));

      if (hasTodoOtherAclList) {
        // 更新情報にあるTODOの内、公開されているTODOか自分が担当者のTODOのみ取得する
        query.setQualifier(exp01.andExp(exp001.orExp(exp002)));
      } else {
        // 更新情報にあるTODOの内、自分が担当者のTODOのみ取得する(ToDo（他ユーザーのToDo）の権限を持っていない場合、listからTodoの情報を削除)
        query.setQualifier(exp01.andExp(exp002));
      }

      query.distinct(true);

      todoList = query.fetchList();
    }

    // topicListからidを抜き出す
    List<Integer> todoIdList = new ArrayList<Integer>();
    for (EipTTodo obj : todoList) {
      todoIdList.add(obj.getTodoId());
    }
    // listのなかでIDがtodoIdListに入っていないものを削除
    list.removeIf(obj -> (obj.getAppId().equals("ToDo") && !todoIdList
      .contains(Integer.parseInt(obj.getExternalId()))));
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
   * トピックの総数を返す． <BR>
   *
   * @return
   */
  public int getContentHeight() {
    return contentHeight;
  }

  /**
   * トピックの総数を返す． <BR>
   *
   * @return
   */
  public int getContentHeightMax() {
    return contentHeightMax;
  }

  /**
   * @return
   *
   */
  @Override
  protected Attributes getColumnMap() {
    Attributes map = new Attributes();
    map.putValue("update_date", EipTTimeline.UPDATE_DATE_PROPERTY);
    map.putValue("owner_name", EipTTimeline.OWNER_ID_PROPERTY);

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

  public int getUserId() {
    return uid;
  }

  public boolean showReplyForm() {
    return showReplyForm;
  }

  /**
   * アクセス権限チェック用メソッド。<br />
   * アクセス権限の機能名を返します。
   *
   * @return
   */
  @Override
  public String getAclPortletFeature() {
    return ALAccessControlConstants.POERTLET_FEATURE_TIMELINE_POST;
  }

  /**
   * 他ユーザーの投稿を一覧表示する権限があるかどうかを返します。
   *
   * @return
   */
  public boolean hasAclTimelineListOther() {
    return hasAclTimelineListOther;
  }

  /**
   * @return hasAclTimelineDeleteOther
   */
  public boolean hasAclTimelineDeleteOther() {
    return hasAclTimelineDeleteOther;
  }

  /**
   * @return hasAclTimelineInsert
   */
  public boolean hasAclTimelineInsert() {
    return hasAclTimelineInsert;
  }

  /**
   * @return hasAclTimelineDelete
   */
  public boolean hasAclTimelineDelete() {
    return hasAclTimelineDelete;
  }

  /**
   * @return hasAclTimelineCommentInsert
   */
  public boolean hasAclTimelineCommentInsert() {
    return hasAclTimelineCommentInsert;
  }

  /**
   * @return hasAclTimelineCommentDelete
   */
  public boolean hasAclTimelineCommentDelete() {
    return hasAclTimelineCommentDelete;
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

  /**
   * 部署の一覧を取得する．
   *
   * @return
   */
  public Map<Integer, ALEipPost> getPostMap() {
    return ALEipManager.getInstance().getPostMap();
  }

  public String getCurrentSearchWithSanitize() {
    return ALStringUtil.sanitizing(getCurrentSearch());
  }

  /**
   * @param keyword
   *          セットする keyword
   */
  public void setKeyword(String keyword) {
    this.target_keyword.setValue(keyword);
  }

  /**
   * @return target_keyword
   */
  public ALStringField getKeyword() {
    return target_keyword;
  }

  public boolean hasKeyword() {
    return !StringUtils.isEmpty(target_keyword.getValue());
  }

  /**
   * @return scrollTop
   */
  public int getScrollTop() {
    return scrollTop;
  }

  /**
   * @param scrollTop
   *          セットする scrollTop
   */
  public void setScrollTop(int scrollTop) {
    this.scrollTop = scrollTop;
  }

  public boolean hasMyPhoto() {
    if (baseuser == null) {
      return false;
    }
    return baseuser.hasPhoto();
  }

  public ALEipUser getMyUser() {
    return user;
  }

  /**
   *
   * @param id
   * @return
   */
  public boolean isAdmin() {
    return isAdmin;
  }

  @Override
  public List<Object> getList() {
    return list;
  }

  protected void loadAggregateUsers() {
    ALEipManager.getInstance().getUsers(users);
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
    current_filter = target_group_name;
    target_display_name = getTargetDisplayName(rundata, context);
    if ((!target_group_name.equals("")) && (!target_group_name.equals("all"))) {
      boolean existPost = false;
      for (int i = 0; i < myGroupList.size(); i++) {
        String pid = myGroupList.get(i).getName().toString();
        if (pid.equals(target_group_name)) {
          existPost = true;
          break;
        }
      }
      Map<Integer, ALEipPost> map = ALEipManager.getInstance().getPostMap();
      for (Map.Entry<Integer, ALEipPost> item : map.entrySet()) {
        String pid = item.getValue().getGroupName().toString();
        if (pid.equals(target_group_name)) {
          existPost = true;
          break;
        }
      }
      if (existPost) {
        userList = ALEipUtils.getUsers(target_group_name);
      } else {
        target_group_name = "all";
        userList = ALEipUtils.getUsers("LoginUser");
      }
    } else {
      userList = ALEipUtils.getUsers("LoginUser");
    }
    if ((!"".equals(target_display_name))
      && (!"all".equals(target_display_name))) {
      if ("posting".equals(target_display_name)) {
        displayParam = "P";
      } else if ("update".equals(target_display_name)) {
        displayParam = "U";
      }
    }
    for (int i = 0; i < userList.size(); i++) {
      useridList.add((int) (userList.get(i).getUserId().getValue()));
    }
    // ガイドユーザー表示用
    useridList.add(2);
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
   * 更新情報のみ・投稿のみの記事を絞り込むIDを取得する
   *
   * @param rundata
   * @param context
   * @return
   */
  protected String getTargetDisplayName(RunData rundata, Context context) {
    return getTargetDisplayName(rundata, context, TARGET_DISPLAY_NAME);
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

    if (idParam == null
      && (target_group_name == null || "".equals(target_group_name))) {
      ALEipUtils.setTemp(rundata, context, target_key, "all");
      target_group_name = "all";
    } else if (idParam != null) {
      ALEipUtils.setTemp(rundata, context, target_key, idParam);
      target_group_name = idParam;
    }
    return target_group_name;
  }

  /**
   * 更新情報のみ・投稿のみの記事を絞り込むIDを取得する
   *
   * @param rundata
   * @param target_key
   * @param context
   * @return
   */
  protected String getTargetDisplayName(RunData rundata, Context context,
      String target_key) {
    String target_display_name = null;
    String idParam = null;
    if (ALEipUtils.isMatch(rundata, context)) {
      // 自ポートレットへのリクエストの場合に，グループ名を取得する．
      idParam = rundata.getParameters().getString(target_key);
    }
    target_display_name = ALEipUtils.getTemp(rundata, context, target_key);

    if (idParam == null
      && (target_display_name == null || "".equals(target_display_name))) {
      ALEipUtils.setTemp(rundata, context, target_key, "all");
      target_display_name = "all";
    } else if (idParam != null) {
      ALEipUtils.setTemp(rundata, context, target_key, idParam);
      target_display_name = idParam;
    }
    return target_display_name;
  }

  /**
   * 表示切り替え時に指定するグループ名
   *
   * @return
   */
  public String getTargetGroupName() {
    return target_group_name;
  }

  public String getTargetDisplayName() {
    return target_display_name;
  }

  public boolean isFileUploadable() {
    return isFileUploadable;
  }

  /**
   * @param ContentHeightMax
   *          セットする ContentHeightMax
   */
  public void setContentHeightMax(int height) {
    contentHeight = height;
    contentHeightMax = height;
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
