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
import java.util.List;

import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.jetspeed.services.logging.JetspeedLogFactoryService;
import org.apache.jetspeed.services.logging.JetspeedLogger;
import org.apache.turbine.services.TurbineServices;
import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;

import com.aimluck.commons.field.ALNumberField;
import com.aimluck.commons.field.ALStringField;
import com.aimluck.eip.cayenne.om.portlet.EipTMsgboardCategory;
import com.aimluck.eip.cayenne.om.portlet.EipTMsgboardCategoryMap;
import com.aimluck.eip.cayenne.om.portlet.EipTMsgboardFile;
import com.aimluck.eip.cayenne.om.portlet.EipTMsgboardTopic;
import com.aimluck.eip.cayenne.om.security.TurbineUser;
import com.aimluck.eip.common.ALAbstractFormData;
import com.aimluck.eip.common.ALDBErrorException;
import com.aimluck.eip.common.ALEipConstants;
import com.aimluck.eip.common.ALEipUser;
import com.aimluck.eip.common.ALFileNotRemovedException;
import com.aimluck.eip.common.ALPageNotFoundException;
import com.aimluck.eip.common.ALPermissionException;
import com.aimluck.eip.fileupload.beans.FileuploadLiteBean;
import com.aimluck.eip.fileupload.util.FileuploadUtils;
import com.aimluck.eip.modules.actions.common.ALAction;
import com.aimluck.eip.msgboard.util.MsgboardUtils;
import com.aimluck.eip.orm.Database;
import com.aimluck.eip.orm.query.SelectQuery;
import com.aimluck.eip.services.accessctl.ALAccessControlConstants;
import com.aimluck.eip.services.accessctl.ALAccessControlFactoryService;
import com.aimluck.eip.services.accessctl.ALAccessControlHandler;
import com.aimluck.eip.services.eventlog.ALEventlogConstants;
import com.aimluck.eip.services.eventlog.ALEventlogFactoryService;
import com.aimluck.eip.services.storage.ALStorageService;
import com.aimluck.eip.timeline.util.TimelineUtils;
import com.aimluck.eip.util.ALEipUtils;
import com.aimluck.eip.util.ALLocalizationUtils;

/**
 * 掲示板トピックのフォームデータを管理するクラスです。 <BR>
 *
 */
public class MsgboardTopicFormData extends ALAbstractFormData {

  /** logger */
  private static final JetspeedLogger logger = JetspeedLogFactoryService
    .getLogger(MsgboardTopicFormData.class.getName());

  /** トピック名 */
  private ALStringField topic_name;

  /** カテゴリ ID */
  private ALNumberField category_id;

  /** カテゴリ名 */
  private ALStringField category_name;

  /** メモ */
  private ALStringField note;

  /** カテゴリ一覧 */
  private List<MsgboardCategoryResultData> categoryList;

  /** */
  private boolean is_new_category;

  /** 添付ファイルリスト */
  private List<FileuploadLiteBean> fileuploadList = null;

  /** 添付フォルダ名 */
  private String folderName = null;

  private int uid;

  private EipTMsgboardCategory category;

  private String orgId;

  private String aclPortletFeature = null;

  /** 閲覧権限の有無 */
  @SuppressWarnings("unused")
  private boolean hasAclCategoryList;

  /** 他ユーザーの作成したトピックの編集権限 */
  private boolean hasAclUpdateTopicOthers;

  /** 他ユーザーの作成したトピックの削除権限 */
  private boolean hasAclDeleteTopicOthers;

  /**
   *
   * @param action
   * @param rundata
   * @param context
   *
   *
   */
  @Override
  public void init(ALAction action, RunData rundata, Context context)
      throws ALPageNotFoundException, ALDBErrorException {
    super.init(action, rundata, context);
    is_new_category = rundata.getParameters().getBoolean("is_new_category");

    uid = ALEipUtils.getUserId(rundata);
    orgId = Database.getDomainName();

    folderName = rundata.getParameters().getString("folderName");

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
  }

  /**
   * 各フィールドを初期化します。 <BR>
   *
   *
   */
  @Override
  public void initField() {
    // トピック名
    topic_name = new ALStringField();
    topic_name.setFieldName(ALLocalizationUtils.getl10n("MSGBOARD_TITLE"));
    topic_name.setTrim(true);
    // カテゴリID
    category_id = new ALNumberField();
    category_id.setFieldName(ALLocalizationUtils
      .getl10n("MSGBOARD_PORTLET_CATEGORY"));
    // カテゴリ
    category_name = new ALStringField();
    category_name.setFieldName(ALLocalizationUtils
      .getl10n("MSGBOARD_CATEGORY_NAME"));
    // メモ
    note = new ALStringField();
    note.setFieldName(ALLocalizationUtils.getl10n("MSGBOARD_NOTE"));
    note.setTrim(false);

    fileuploadList = new ArrayList<FileuploadLiteBean>();

    aclPortletFeature =
      ALAccessControlConstants.POERTLET_FEATURE_MSGBOARD_TOPIC;
  }

  /**
   *
   * @param rundata
   * @param context
   */
  public void loadCategoryList(RunData rundata, Context context) {
    categoryList = MsgboardUtils.loadCategoryList(rundata);
  }

  /**
   * 掲示板の各フィールドに対する制約条件を設定します。 <BR>
   *
   *
   */
  @Override
  protected void setValidator() {
    // トピック名必須項目
    topic_name.setNotNull(true);
    // トピック名の文字数制限
    topic_name.limitMaxLength(50);
    // メモ必須項目
    note.setNotNull(true);
    // メモの文字数制限
    note.limitMaxLength(10000);
    if (is_new_category) {
      // カテゴリ名必須項目
      category_name.setNotNull(true);
      // カテゴリ名文字数制限
      category_name.limitMaxLength(50);
    }
  }

  /**
   * トピックのフォームに入力されたデータの妥当性検証を行います。 <BR>
   *
   * @param msgList
   * @return TRUE 成功 FALSE 失敗
   *
   */
  @Override
  protected boolean validate(List<String> msgList) {
    // トピック名
    topic_name.validate(msgList);
    // メモ
    note.validate(msgList);
    if (is_new_category) {
      // カテゴリ名
      category_name.validate(msgList);
    }
    return (msgList.size() == 0);

  }

  /**
   * トピックをデータベースから読み出します。 <BR>
   *
   * @param rundata
   * @param context
   * @param msgList
   * @return TRUE 成功 FALSE 失敗
   */
  @Override
  protected boolean loadFormData(RunData rundata, Context context,
      List<String> msgList) {
    try {
      // オブジェクトモデルを取得
      EipTMsgboardTopic topic =
        MsgboardUtils.getEipTMsgboardParentTopic(rundata, context, false);

      // トピック名
      topic_name.setValue(topic.getTopicName());
      // カテゴリID
      category_id.setValue(topic.getEipTMsgboardCategory().getCategoryId());
      // 内容
      note.setValue(topic.getNote());
      // ファイル
      SelectQuery<EipTMsgboardFile> query =
        Database.query(EipTMsgboardFile.class);
      query.andQualifier(ExpressionFactory.matchDbExp(
        EipTMsgboardFile.EIP_TMSGBOARD_TOPIC_PROPERTY,
        topic.getTopicId()));
      List<EipTMsgboardFile> msgboardFileList = query.fetchList();
      for (EipTMsgboardFile file : msgboardFileList) {
        FileuploadLiteBean fbean = new FileuploadLiteBean();
        fbean.initField();
        fbean.setFileId(file.getFileId());
        fbean.setFileName(file.getFileName());
        fileuploadList.add(fbean);
      }

    } catch (RuntimeException ex) {
      throw ex;
    } catch (Exception ex) {
      logger.error("msgboard", ex);
      return false;
    }
    return true;
  }

  /**
   * トピックをデータベースから削除します。 <BR>
   *
   * @param rundata
   * @param context
   * @param msgList
   * @return TRUE 成功 FALSE 失敗
   */
  @Override
  protected boolean deleteFormData(RunData rundata, Context context,
      List<String> msgList) throws ALPageNotFoundException, ALDBErrorException {
    try {

      // FIX_ME イベントログのために一度IDと名前を取得
      int parentid =
        Integer.parseInt(ALEipUtils.getTemp(
          rundata,
          context,
          ALEipConstants.ENTITY_ID));

      EipTMsgboardTopic parent =
        Database.get(EipTMsgboardTopic.class, (long) parentid);

      // オブジェクトモデルを取得
      List<EipTMsgboardTopic> list;

      if (this.hasAclDeleteTopicOthers) {
        list =
          MsgboardUtils.getEipTMsgboardTopicListToDeleteTopic(
            rundata,
            context,
            true);
      } else {
        list =
          MsgboardUtils.getEipTMsgboardTopicListToDeleteTopic(
            rundata,
            context,
            false);
      }

      if (list == null) {
        // 指定した トピック ID のレコードが見つからない場合
        logger.debug("[MsgboardTopicFormData] Not found List...");
        throw new ALPageNotFoundException();
      }

      List<Integer> topicIdList = new ArrayList<Integer>();
      EipTMsgboardTopic topic;
      int size = list.size();
      for (int i = 0; i < size; i++) {
        topic = list.get(i);
        topicIdList.add(topic.getTopicId());
      }

      // トピックを削除
      SelectQuery<EipTMsgboardTopic> query =
        Database.query(EipTMsgboardTopic.class);
      Expression exp =
        ExpressionFactory.inDbExp(
          EipTMsgboardTopic.TOPIC_ID_PK_COLUMN,
          topicIdList);
      query.setQualifier(exp);

      List<EipTMsgboardTopic> topics = query.fetchList();

      List<String> fpaths = new ArrayList<String>();
      if (topics.size() > 0) {
        int tsize = topics.size();
        for (int i = 0; i < tsize; i++) {
          List<?> files = topics.get(i).getEipTMsgboardFileArray();
          if (files != null && files.size() > 0) {
            int fsize = files.size();
            for (int j = 0; j < fsize; j++) {
              fpaths.add(((EipTMsgboardFile) files.get(j)).getFilePath());
            }
            MsgboardUtils.deleteFiles(topics.get(i).getTopicId(), orgId, topics
              .get(i)
              .getOwnerId(), fpaths);
          }
        }
      }

      Database.deleteAll(topics);
      Database.commit();

      TimelineUtils.deleteTimelineActivity(rundata, context, "Msgboard", parent
        .getTopicId()
        .toString());

      // イベントログに保存
      ALEventlogFactoryService.getInstance().getEventlogHandler().log(
        parent.getTopicId(),
        ALEventlogConstants.PORTLET_TYPE_MSGBOARD_TOPIC,
        parent.getTopicName());

    } catch (ALFileNotRemovedException fe) {
      Database.rollback();
      logger.error("[MsgboardCategorySelectData]", fe);
      msgList.add(ALLocalizationUtils.getl10n("ERROR_FILE_DETELE_FAILURE"));
      return false;
    } catch (RuntimeException e) {
      Database.rollback();
      logger.error("[MsgboardCategorySelectData]", e);
      throw new ALDBErrorException();
    } catch (Exception e) {
      Database.rollback();
      logger.error("[MsgboardCategorySelectData]", e);
      throw new ALDBErrorException();
    }
    return true;
  }

  /**
   * トピックをデータベースに格納します。 <BR>
   *
   * @param rundata
   * @param context
   * @param msgList
   * @return TRUE 成功 FALSE 失敗
   */
  @Override
  protected boolean insertFormData(RunData rundata, Context context,
      List<String> msgList) {
    try {
      if (is_new_category) {
        // カテゴリの登録処理
        if (!insertCategoryData(rundata, context, msgList)) {
          return false;
        }
      } else {
        category =
          Database.get(EipTMsgboardCategory.class, Integer
            .valueOf((int) category_id.getValue()));
      }

      // 新規オブジェクトモデル
      EipTMsgboardTopic topic = Database.create(EipTMsgboardTopic.class);
      // トピック名
      topic.setTopicName(topic_name.getValue());
      // 親トピックID
      topic.setParentId(Integer.valueOf(0));
      // カテゴリID
      topic.setEipTMsgboardCategory(category);
      // ユーザーID
      topic.setOwnerId(Integer.valueOf(uid));
      // メモ
      topic.setNote(note.getValue());
      // 作成者
      topic.setCreateUserId(Integer.valueOf(uid));
      // 更新者
      topic.setUpdateUserId(Integer.valueOf(uid));
      // 作成日
      topic.setCreateDate(Calendar.getInstance().getTime());
      // 更新日
      topic.setUpdateDate(Calendar.getInstance().getTime());

      // ファイルをデータベースに登録する．
      if (!MsgboardUtils.insertFileDataDelegate(
        rundata,
        context,
        topic,
        fileuploadList,
        folderName,
        msgList)) {
        return false;
      }

      Database.commit();

      // イベントログに保存
      ALEventlogFactoryService.getInstance().getEventlogHandler().log(
        topic.getTopicId(),
        ALEventlogConstants.PORTLET_TYPE_MSGBOARD_TOPIC,
        topic.getTopicName());

      /* 自分以外の全員に新着ポートレット登録 */
      if ("T".equals(topic.getEipTMsgboardCategory().getPublicFlag())) {

        // アクティビティ
        ALEipUser user = ALEipUtils.getALEipUser(uid);
        MsgboardUtils.createTopicActivity(
          topic,
          user.getName().getValue(),
          true);

      } else {
        List<Integer> userIds =
          MsgboardUtils.getWhatsNewInsertList(rundata, category
            .getCategoryId()
            .intValue(), category.getPublicFlag());

        List<String> recipients = new ArrayList<String>();
        int u_size = userIds.size();
        for (int i = 0; i < u_size; i++) {
          Integer _id = userIds.get(i);
          ALEipUser user = ALEipUtils.getALEipUser(_id);
          if (user != null) {
            recipients.add(user.getName().getValue());
          }
        }

        // アクティビティ
        if (recipients.size() > 0) {
          ALEipUser user = ALEipUtils.getALEipUser(uid);
          MsgboardUtils.createTopicActivity(
            topic,
            user.getName().getValue(),
            recipients,
            true);
        }
      }

      // 添付ファイル保存先のフォルダを削除
      ALStorageService.deleteTmpFolder(uid, folderName);

    } catch (RuntimeException ex) {
      Database.rollback();
      throw ex;
    } catch (Exception ex) {
      Database.rollback();
      logger.error("msgboard", ex);
      return false;
    }
    return true;
  }

  /**
   * トピックカテゴリをデータベースに格納します。 <BR>
   *
   * @param rundata
   * @param context
   * @param msgList
   * @return
   */
  private boolean insertCategoryData(RunData rundata, Context context,
      List<String> msgList) {
    try {

      setAclPortletFeature(ALAccessControlConstants.POERTLET_FEATURE_MSGBOARD_CATEGORY);
      doCheckAclPermission(
        rundata,
        context,
        ALAccessControlConstants.VALUE_ACL_INSERT);
      setAclPortletFeature(ALAccessControlConstants.POERTLET_FEATURE_MSGBOARD_TOPIC);

      int userid = ALEipUtils.getUserId(rundata);

      TurbineUser tuser =
        Database.get(TurbineUser.class, Integer.valueOf(userid));

      // 新規オブジェクトモデル
      category = Database.create(EipTMsgboardCategory.class);
      // カテゴリ名
      category.setCategoryName(category_name.getValue());
      // 公開区分
      category.setPublicFlag(MsgboardUtils.PUBLIC_FLG_VALUE_PUBLIC);
      // カテゴリメモ
      category.setNote("");
      // ユーザーID
      category.setTurbineUser(tuser);
      // 作成日
      category.setCreateDate(Calendar.getInstance().getTime());
      // 更新日
      category.setUpdateDate(Calendar.getInstance().getTime());

      // マップの登録
      EipTMsgboardCategoryMap map =
        Database.create(EipTMsgboardCategoryMap.class);
      map.setEipTMsgboardCategory(category);
      map.setUserId(Integer.valueOf(userid));
      map.setStatus(MsgboardUtils.STAT_VALUE_ALL);

      Database.commit();

      // イベントログに保存
      ALEventlogFactoryService.getInstance().getEventlogHandler().log(
        category.getCategoryId(),
        ALEventlogConstants.PORTLET_TYPE_MSGBOARD_CATEGORY,
        category.getCategoryName());

    } catch (ALPermissionException e) {
      msgList.add(ALAccessControlConstants.DEF_PERMISSION_ERROR_STR);
      return false;
    } catch (Exception ex) {
      Database.rollback();
      logger.error("msgboard", ex);
      msgList.add("エラーが発生しました。");
      return false;
    }
    return true;
  }

  /**
   * データベースに格納されているトピックを更新します。 <BR>
   *
   * @param rundata
   * @param context
   * @param msgList
   * @return TRUE 成功 FALSE 失敗
   */
  @Override
  protected boolean updateFormData(RunData rundata, Context context,
      List<String> msgList) {
    try {
      if (is_new_category) {
        // カテゴリの登録処理
        if (!insertCategoryData(rundata, context, msgList)) {
          return false;
        }
      } else {
        category =
          Database.get(EipTMsgboardCategory.class, Integer
            .valueOf((int) category_id.getValue()));
      }

      // 新規オブジェクトモデル
      EipTMsgboardTopic topic =
        MsgboardUtils.getEipTMsgboardParentTopic(rundata, context, false);
      // トピック名
      topic.setTopicName(topic_name.getValue());
      // カテゴリID
      topic.setEipTMsgboardCategory(category);
      // メモ
      topic.setNote(note.getValue());
      // 更新者
      topic.setUpdateUserId(Integer.valueOf(uid));
      // 更新日
      topic.setUpdateDate(Calendar.getInstance().getTime());

      // ファイルをデータベースに登録する．
      if (!MsgboardUtils.insertFileDataDelegate(
        rundata,
        context,
        topic,
        fileuploadList,
        folderName,
        msgList)) {
        return false;
      }

      Database.commit();

      // イベントログに保存
      ALEventlogFactoryService.getInstance().getEventlogHandler().log(
        topic.getTopicId(),
        ALEventlogConstants.PORTLET_TYPE_MSGBOARD_TOPIC,
        topic.getTopicName());

      /* 自分以外の全員に新着ポートレット登録 */
      if ("T".equals(topic.getEipTMsgboardCategory().getPublicFlag())) {

        // アクティビティ
        ALEipUser user = ALEipUtils.getALEipUser(uid);
        MsgboardUtils.createTopicActivity(
          topic,
          user.getName().getValue(),
          false);

      } else {
        List<Integer> userIds =
          MsgboardUtils.getWhatsNewInsertList(rundata, category
            .getCategoryId()
            .intValue(), category.getPublicFlag());

        List<String> recipients = new ArrayList<String>();
        int u_size = userIds.size();
        for (int i = 0; i < u_size; i++) {
          Integer _id = userIds.get(i);
          ALEipUser user = ALEipUtils.getALEipUser(_id);
          if (user != null) {
            recipients.add(user.getName().getValue());
          }
        }

        // アクティビティ
        if (recipients.size() > 0) {
          ALEipUser user = ALEipUtils.getALEipUser(uid);
          MsgboardUtils.createTopicActivity(
            topic,
            user.getName().getValue(),
            recipients,
            false);
        }
      }

      // 添付ファイル保存先のフォルダを削除
      ALStorageService.deleteTmpFolder(uid, folderName);
    } catch (RuntimeException ex) {
      Database.rollback();
      throw ex;
    } catch (Exception ex) {
      Database.rollback();
      logger.error("msgboard", ex);
      return false;
    }
    return true;
  }

  /**
   *
   * @param rundata
   * @param context
   * @param msgList
   * @return
   * @throws ALPageNotFoundException
   * @throws ALDBErrorException
   */
  @Override
  protected boolean setFormData(RunData rundata, Context context,
      List<String> msgList) throws ALPageNotFoundException, ALDBErrorException {

    boolean res = super.setFormData(rundata, context, msgList);

    try {
      fileuploadList = MsgboardUtils.getFileuploadList(rundata);
    } catch (Exception ex) {
      logger.error("msgboard", ex);
    }

    return res;
  }

  /**
   * カテゴリIDを取得します。 <BR>
   *
   * @return
   */
  public ALNumberField getCategoryId() {
    return category_id;
  }

  /**
   * メモを取得します。 <BR>
   *
   * @return
   */
  public ALStringField getNote() {
    return note;
  }

  /**
   * トピック名を取得します。 <BR>
   *
   * @return
   */
  public ALStringField getTopicName() {
    return topic_name;
  }

  /**
   * カテゴリ一覧を取得します。 <BR>
   *
   * @return
   */
  public List<MsgboardCategoryResultData> getCategoryList() {
    return categoryList;
  }

  /**
   * @return
   */
  public boolean isNewCategory() {
    return is_new_category;
  }

  /**
   * @return
   */
  public void setIsNewCategory(boolean bool) {
    is_new_category = bool;
  }

  /**
   * カテゴリ名を取得します。
   *
   * @return
   */
  public ALStringField getCategoryName() {
    return category_name;
  }

  public List<FileuploadLiteBean> getAttachmentFileNameList() {
    return fileuploadList;
  }

  public String getFolderName() {
    return folderName;
  }

  /**
   * アクセス権限チェック用メソッド。 アクセス権限の機能名を返します。
   *
   * @return
   */
  @Override
  public String getAclPortletFeature() {
    return aclPortletFeature;
  }

  public void setAclPortletFeature(String aclPortletFeature) {
    this.aclPortletFeature = aclPortletFeature;
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

  private List<Integer> getRequestedHasFileIdList(
      List<FileuploadLiteBean> attachmentFileNameList) {
    List<Integer> idlist = new ArrayList<Integer>();
    FileuploadLiteBean filebean = null;
    // if (attachmentFileNameList != null && !"".equals(attachmentFileNameList))
    // {
    if (attachmentFileNameList != null) {
      int size = attachmentFileNameList.size();
      for (int i = 0; i < size; i++) {
        filebean = attachmentFileNameList.get(i);
        if (!filebean.isNewFile()) {
          int index = filebean.getFileId();
          idlist.add(Integer.valueOf(index));
        }
      }
    }
    return idlist;
  }

  /**
   * 添付ファイルに関する権限チェック
   *
   * @param msgList
   * @return
   */
  @Override
  protected boolean extValidate(RunData rundata, Context context,
      List<String> msgList) {
    if (ALEipConstants.MODE_INSERT.equals(getMode())) {
      return FileuploadUtils.insertValidate(
        msgList,
        fileuploadList,
        hasAttachmentInsertAuthority());
    } else if (ALEipConstants.MODE_UPDATE.equals(getMode())) {
      try {
        Integer topicId =
          MsgboardUtils.getEipTMsgboardParentTopicId(rundata, context);

        // サーバーに残すファイルのID
        List<Integer> formIdList = getRequestedHasFileIdList(fileuploadList);
        // 現在選択しているエントリが持っているファイル
        // この辺の効率化が課題とは思うものの、一応動作はしているはず。
        SelectQuery<EipTMsgboardFile> dbquery =
          Database.query(EipTMsgboardFile.class);
        dbquery.andQualifier(ExpressionFactory.matchDbExp(
          EipTMsgboardFile.EIP_TMSGBOARD_TOPIC_PROPERTY,
          topicId));
        List<EipTMsgboardFile> existsFiles = dbquery.fetchList();

        List<Integer> existFileIdList = new ArrayList<Integer>();
        if (existsFiles != null) {
          for (EipTMsgboardFile file : existsFiles) {
            existFileIdList.add(file.getFileId());
          }
        }

        return FileuploadUtils.updateValidate(
          msgList,
          formIdList,
          existFileIdList,
          fileuploadList,
          hasAttachmentInsertAuthority(),
          hasAttachmentDeleteAuthority());
      } catch (Exception ex) {
        logger.error("MsgboardTopicFormData.", ex);
        return false;
      }
    }
    return true;
  }
}
