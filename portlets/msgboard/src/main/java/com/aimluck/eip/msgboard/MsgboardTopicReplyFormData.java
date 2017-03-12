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
import java.util.List;

import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.jetspeed.services.logging.JetspeedLogFactoryService;
import org.apache.jetspeed.services.logging.JetspeedLogger;
import org.apache.turbine.services.TurbineServices;
import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;

import com.aimluck.commons.field.ALStringField;
import com.aimluck.eip.cayenne.om.portlet.EipTMsgboardFile;
import com.aimluck.eip.cayenne.om.portlet.EipTMsgboardTopic;
import com.aimluck.eip.cayenne.om.security.TurbineUser;
import com.aimluck.eip.common.ALAbstractFormData;
import com.aimluck.eip.common.ALDBErrorException;
import com.aimluck.eip.common.ALEipConstants;
import com.aimluck.eip.common.ALEipUser;
import com.aimluck.eip.common.ALPageNotFoundException;
import com.aimluck.eip.common.ALPermissionException;
import com.aimluck.eip.fileupload.beans.FileuploadLiteBean;
import com.aimluck.eip.fileupload.util.FileuploadUtils;
import com.aimluck.eip.mail.ALAdminMailContext;
import com.aimluck.eip.mail.ALAdminMailMessage;
import com.aimluck.eip.mail.ALMailService;
import com.aimluck.eip.mail.util.ALEipUserAddr;
import com.aimluck.eip.mail.util.ALMailUtils;
import com.aimluck.eip.modules.actions.common.ALAction;
import com.aimluck.eip.modules.actions.msgboard.MsgboardAction;
import com.aimluck.eip.modules.screens.MsgboardTopicDetailScreen;
import com.aimluck.eip.modules.screens.MsgboardTopicFormJSONScreen;
import com.aimluck.eip.msgboard.util.MsgboardUtils;
import com.aimluck.eip.orm.Database;
import com.aimluck.eip.orm.query.SelectQuery;
import com.aimluck.eip.services.accessctl.ALAccessControlConstants;
import com.aimluck.eip.services.accessctl.ALAccessControlFactoryService;
import com.aimluck.eip.services.accessctl.ALAccessControlHandler;
import com.aimluck.eip.services.eventlog.ALEventlogConstants;
import com.aimluck.eip.services.eventlog.ALEventlogFactoryService;
import com.aimluck.eip.services.orgutils.ALOrgUtilsService;
import com.aimluck.eip.services.storage.ALStorageService;
import com.aimluck.eip.util.ALEipUtils;
import com.aimluck.eip.util.ALLocalizationUtils;

/**
 * 掲示板返信のフォームデータを管理するクラスです。 <BR>
 * 
 */
public class MsgboardTopicReplyFormData extends ALAbstractFormData {

  /** logger */
  private static final JetspeedLogger logger = JetspeedLogFactoryService
    .getLogger(MsgboardTopicReplyFormData.class.getName());

  /** メモ */
  private ALStringField note;

  /** 添付ファイル */
  private ALStringField attachment = null;

  /** 添付ファイルリスト */
  private List<FileuploadLiteBean> fileuploadList = null;

  /** 添付フォルダ名 */
  private String folderName = null;

  private int uid;

  private String orgId;

  /** アクセス権限の機能名 */
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
    // メモ
    note = new ALStringField();
    note.setFieldName(ALLocalizationUtils.getl10n("MSGBOARD_NOTE"));
    note.setTrim(false);
    // Attachment
    attachment = new ALStringField();
    attachment
      .setFieldName(ALLocalizationUtils.getl10n("MSGBOARD_FILE_ATTACH"));
    attachment.setTrim(true);

    fileuploadList = new ArrayList<FileuploadLiteBean>();
  }

  /**
   * 掲示板の各フィールドに対する制約条件を設定します。 <BR>
   * 
   * 
   */
  @Override
  protected void setValidator() {
    // メモ必須項目
    note.setNotNull(true);
    // メモの文字数制限
    note.limitMaxLength(10000);
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
    // メモ
    note.validate(msgList);
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
    return false;
  }

  /**
   * 返信記事をデータベースから削除します。 <BR>
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
      String topicid = rundata.getParameters().getString("topic_reply_id");

      // オブジェクトモデルを取得
      EipTMsgboardTopic topic;

      if (this.hasAclDeleteTopicOthers) {
        topic =
          MsgboardUtils.getEipTMsgboardTopicReply(
            rundata,
            context,
            topicid,
            true);
      } else {
        topic =
          MsgboardUtils.getEipTMsgboardTopicReply(
            rundata,
            context,
            topicid,
            false);
      }
      if (topic == null) {
        // 指定した トピック ID のレコードが見つからない場合
        logger.debug("[MsgboardTopicReplyFormData] Not found ID...");
        throw new ALPageNotFoundException();
      }

      List<String> fpaths = new ArrayList<String>();
      List<?> files = topic.getEipTMsgboardFileArray();
      if (files != null && files.size() > 0) {
        int fsize = files.size();
        for (int i = 0; i < fsize; i++) {
          fpaths.add(((EipTMsgboardFile) files.get(i)).getFilePath());
        }
      }

      // 返信記事を削除
      // 添付ファイルはカスケードで自動的に削除される．
      Database.delete(topic);
      Database.commit();

      // イベントログに保存
      ALEventlogFactoryService.getInstance().getEventlogHandler().log(
        topic.getTopicId(),
        ALEventlogConstants.PORTLET_TYPE_MSGBOARD_TOPIC,
        topic.getTopicName());

      if (fpaths.size() > 0) {
        // ローカルファイルに保存されているファイルを削除する．
        int fsize = fpaths.size();
        for (int i = 0; i < fsize; i++) {
          ALStorageService.deleteFile(MsgboardUtils.getSaveDirPath(orgId, uid)
            + fpaths.get(i));
        }
      }
    } catch (Exception e) {
      Database.rollback();
      logger.error("[MsgboardTopicReplyFormData]", e);
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
      List<String> msgList) throws ALPageNotFoundException, ALDBErrorException {
    try {
      // オブジェクトモデルを取得
      EipTMsgboardTopic parenttopic =
        MsgboardUtils.getEipTMsgboardParentTopic(rundata, context, false);
      if (parenttopic == null) {
        // 指定した トピック ID のレコードが見つからない場合
        logger.debug("[MsgboardTopicReplyFormData] Not found ID...");
        throw new ALPageNotFoundException();
      }

      if (!MsgboardUtils.hasAuthorityToReply(uid, parenttopic
        .getEipTMsgboardCategory())) {
        // 返信権限がない場合弾く
        msgList.add(" このトピックに返信する権限がありません。 ");
        return false;
      }

      Date updateDate = Calendar.getInstance().getTime();

      // 新規オブジェクトモデル
      EipTMsgboardTopic topic = Database.create(EipTMsgboardTopic.class);
      // カテゴリID
      topic.setEipTMsgboardCategory(parenttopic.getEipTMsgboardCategory());
      // トピック名
      topic.setTopicName("");
      // 親トピック ID
      topic.setParentId(parenttopic.getTopicId());
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
      topic.setUpdateDate(updateDate);

      // 親トピックの更新情報を更新する．
      parenttopic.setUpdateUserId(Integer.valueOf(uid));
      parenttopic.setUpdateDate(updateDate);

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

      List<ALEipUser> memberList = selectMsgMember(rundata, context);

      // イベントログに保存
      ALEventlogFactoryService.getInstance().getEventlogHandler().log(
        topic.getTopicId(),
        ALEventlogConstants.PORTLET_TYPE_MSGBOARD_TOPIC,
        parenttopic.getTopicName(),
        "insert");

      /* 自分以外の全員に新着ポートレット登録 */
      if ("T".equals(topic.getEipTMsgboardCategory().getPublicFlag())) {
        // アクティビティ
        ALEipUser user = ALEipUtils.getALEipUser(uid);
        // 更新情報
        MsgboardUtils.createNewCommentActivity(parenttopic, user
          .getName()
          .getValue(), topic);
        // あなた宛のお知らせ
        List<String> recipient = new ArrayList<String>();
        for (int i = 0; i < memberList.size(); i++) {
          recipient.add(memberList.get(i).getName().toString());
        }
        MsgboardUtils.createNewTopicActivity(parenttopic, user
          .getName()
          .toString(), recipient, topic);
      } else {
        List<Integer> userIds =
          MsgboardUtils.getWhatsNewInsertList(rundata, topic
            .getEipTMsgboardCategory()
            .getCategoryId()
            .intValue(), topic.getEipTMsgboardCategory().getPublicFlag());

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
          // 更新情報
          MsgboardUtils.createNewCommentActivity(parenttopic, user
            .getName()
            .getValue(), recipients, topic);
          // あなた宛のお知らせ
          List<String> recipient = new ArrayList<String>();
          for (int i = 0; i < memberList.size(); i++) {
            recipient.add(memberList.get(i).getName().toString());
          }
          MsgboardUtils.createNewTopicActivity(parenttopic, user
            .getName()
            .toString(), recipient, topic);
        }
      }
      // 添付ファイル保存先のフォルダを削除
      ALStorageService.deleteTmpFolder(uid, folderName);

      // メール送信
      try {
        int msgType =
          ALMailUtils.getSendDestType(ALMailUtils.KEY_MSGTYPE_MSGBOARD);
        if (msgType > 0) {
          // パソコンへメールを送信
          List<ALEipUserAddr> destMemberList =
            ALMailUtils.getALEipUserAddrs(memberList, ALEipUtils
              .getUserId(rundata), false);
          String subject = "[" + ALOrgUtilsService.getAlias() + "]掲示板";
          String orgId = Database.getDomainName();

          List<ALAdminMailMessage> messageList =
            new ArrayList<ALAdminMailMessage>();
          for (ALEipUserAddr destMember : destMemberList) {
            ALAdminMailMessage message = new ALAdminMailMessage(destMember);
            message.setPcSubject(subject);
            message.setCellularSubject(subject);
            message.setPcBody(MsgboardUtils.createReplyMsgForPc(
              rundata,
              parenttopic,
              topic,
              memberList));
            message.setCellularBody(MsgboardUtils.createReplyMsgForCellPhone(
              rundata,
              parenttopic,
              topic,
              memberList));
            messageList.add(message);
          }
          ALMailService.sendAdminMailAsync(new ALAdminMailContext(
            orgId,
            ALEipUtils.getUserId(rundata),
            messageList,
            ALMailUtils.getSendDestType(ALMailUtils.KEY_MSGTYPE_MSGBOARD)));
        }
      } catch (Exception ex) {
        msgList.add("メールを送信できませんでした。");
        logger.error("msgboard", ex);
        return false;
      }
    } catch (Exception e) {
      Database.rollback();
      logger.error("[MsgboardTopicReplyFormData]", e);
      throw new ALDBErrorException();
    }
    return true;
  }

  /**
   * 自分以外のトピック関係者のuser_idのListを習得します。 <BR>
   * 
   * @param rundata
   * @param context
   * @return
   * @throws ALDBErrorException
   * @throws ALPageNotFoundException
   */
  private List<ALEipUser> selectMsgMember(RunData rundata, Context context)
      throws ALPageNotFoundException, ALDBErrorException {

    // 関連トピックをすべて取得する
    EipTMsgboardTopic parenttopic =
      MsgboardUtils.getEipTMsgboardParentTopic(rundata, context, false);
    SelectQuery<EipTMsgboardTopic> topicQuery =
      Database.query(EipTMsgboardTopic.class);
    Expression topicExp =
      ExpressionFactory.matchExp(
        EipTMsgboardTopic.PARENT_ID_PROPERTY,
        parenttopic.getTopicId());
    topicQuery.setQualifier(topicExp);

    // トピックに関連する全てのユーザーIDを取得する
    List<EipTMsgboardTopic> topicList = topicQuery.fetchList();
    topicList.add(parenttopic);
    List<Integer> userIdList = new ArrayList<Integer>();
    for (EipTMsgboardTopic topic : topicList) {
      Integer userId = topic.getCreateUserId();
      if (!userId.equals(uid) && !userIdList.contains(userId)) {
        userIdList.add(userId);
      }
    }
    userIdList.add(Integer.valueOf(-1));

    // ユーザーIDからユーザー情報を取得する。
    SelectQuery<TurbineUser> userQuery = Database.query(TurbineUser.class);
    Expression userExp =
      ExpressionFactory.inDbExp(TurbineUser.USER_ID_PK_COLUMN, userIdList);
    userQuery.setQualifier(userExp);
    return ALEipUtils.getUsersFromSelectQuery(userQuery);

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
    return false;
  }

  /**
   * トピック詳細表示ページからデータを新規登録します。
   * 
   * @param action
   * @param rundata
   * @param context
   * @return TRUE 成功 FALSE 失敗
   */
  @Override
  public boolean doInsert(ALAction action, RunData rundata, Context context) {
    try {
      if (!doCheckSecurity(rundata, context)) {
        return false;
      }
      init(action, rundata, context);

      doCheckAclPermission(
        rundata,
        context,
        ALAccessControlConstants.VALUE_ACL_INSERT);

      doCheckAttachmentInsertAclPermission(rundata, context);
      doCheckAttachmentDeleteAclPermission(rundata, context);

      action.setMode(ALEipConstants.MODE_INSERT);
      List<String> msgList = new ArrayList<String>();
      setValidator();
      boolean res = false;
      if (isOverQuota()) {
        msgList.add(ALLocalizationUtils
          .getl10n("COMMON_FULL_DISK_DELETE_DETA_OR_CHANGE_PLAN"));
      } else {
        res =
          (setFormData(rundata, context, msgList)
            && validate(msgList)
            && extValidate(rundata, context, msgList) && insertFormData(
            rundata,
            context,
            msgList));
      }
      if (!res) {
        action.setMode(ALEipConstants.MODE_NEW_FORM);
        setMode(action.getMode());
      }
      if (action instanceof MsgboardTopicFormJSONScreen) {
        action.setResultData(this);
        action.addErrorMessages(msgList);
        action.putData(rundata, context);
      } else {
        MsgboardAction msgboardAction = (MsgboardAction) action;
        msgboardAction.setResultDataOnTopicDetail(this);
        msgboardAction.addErrorMessagesOnTopicDetail(msgList);
        msgboardAction.putDataOnTopicDetail(rundata, context);
      }
      return res;
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
   * トピック詳細表示ページにフォームを表示します。
   * 
   * @param action
   * @param rundata
   * @param context
   * @return TRUE 成功 FALSE 失敗
   */
  @Override
  public boolean doViewForm(ALAction action, RunData rundata, Context context) {
    try {
      init(action, rundata, context);

      // doCheckAclPermission(rundata, context,
      // ALAccessControlConstants.VALUE_ACL_INSERT);

      action.setMode("reply");
      // mode = action.getMode();
      List<String> msgList = new ArrayList<String>();
      boolean res = setFormData(rundata, context, msgList);
      if (action instanceof MsgboardTopicDetailScreen) {
        MsgboardTopicDetailScreen msgboardAction =
          (MsgboardTopicDetailScreen) action;
        msgboardAction.setResultDataOnTopicDetail(this);
        msgboardAction.addErrorMessagesOnTopicDetail(msgList);
        msgboardAction.putDataOnTopicDetail(rundata, context);
      } else {
        MsgboardAction msgboardAction = (MsgboardAction) action;
        msgboardAction.setResultDataOnTopicDetail(this);
        msgboardAction.addErrorMessagesOnTopicDetail(msgList);
        msgboardAction.putDataOnTopicDetail(rundata, context);
      }
      return res;
      // } catch (ALPermissionException e) {
      // ALEipUtils.redirectPermissionError(rundata);
      // return false;
    } catch (ALPageNotFoundException e) {
      ALEipUtils.redirectPageNotFound(rundata);
      return false;
    } catch (ALDBErrorException e) {
      ALEipUtils.redirectDBError(rundata);
      return false;
    }
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
      fileuploadList = FileuploadUtils.getFileuploadList(rundata);
    } catch (Exception ex) {
      logger.error("msgboard", ex);
    }

    return res;
  }

  public void setAclPortletFeature(String featureName) {
    aclPortletFeature = featureName;
  }

  /**
   * メモを取得します。 <BR>
   * 
   * @return
   */
  public ALStringField getNote() {
    return note;
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
    if (aclPortletFeature == null || "".equals(aclPortletFeature)) {
      return ALAccessControlConstants.POERTLET_FEATURE_MSGBOARD_TOPIC_REPLY;
    } else {
      return aclPortletFeature;
    }
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
   * 添付ファイルに関する権限チェック
   *
   * @param msgList
   * @return
   */
  @Override
  protected boolean extValidate(RunData rundata, Context context,
      List<String> msgList) {
    return FileuploadUtils.insertValidate(
      msgList,
      fileuploadList,
      hasAttachmentInsertAuthority());
  }
}
