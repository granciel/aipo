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

import static com.aimluck.eip.util.ALLocalizationUtils.*;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.apache.jetspeed.services.logging.JetspeedLogFactoryService;
import org.apache.jetspeed.services.logging.JetspeedLogger;
import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;

import com.aimluck.commons.field.ALStringField;
import com.aimluck.eip.cayenne.om.portlet.EipTProjectTaskComment;
import com.aimluck.eip.cayenne.om.portlet.EipTProjectTaskCommentFile;
import com.aimluck.eip.cayenne.om.security.TurbineUser;
import com.aimluck.eip.common.ALAbstractFormData;
import com.aimluck.eip.common.ALDBErrorException;
import com.aimluck.eip.common.ALEipConstants;
import com.aimluck.eip.common.ALEipUser;
import com.aimluck.eip.common.ALPageNotFoundException;
import com.aimluck.eip.fileupload.beans.FileuploadBean;
import com.aimluck.eip.fileupload.util.FileuploadUtils;
import com.aimluck.eip.modules.actions.common.ALAction;
import com.aimluck.eip.orm.Database;
import com.aimluck.eip.project.util.ProjectFile;
import com.aimluck.eip.project.util.ProjectUtils;
import com.aimluck.eip.services.eventlog.ALEventlogConstants;
import com.aimluck.eip.services.eventlog.ALEventlogFactoryService;
import com.aimluck.eip.util.ALEipUtils;

/**
 * タスクコメントの登録・削除処理を行います。
 */
public class ProjectTaskCommentFormData extends ALAbstractFormData {

  /** logger */
  private static final JetspeedLogger logger = JetspeedLogFactoryService
    .getLogger(ProjectTaskCommentFormData.class.getName());

  /** 内容 */
  private ALStringField comment;

  /** ログインユーザー */
  private ALEipUser loginUser;

  /** ファイルアップロードリスト */
  private List<FileuploadBean> fileuploadList;

  /** 添付フォルダ名 */
  private String folderName;

  /** ログインユーザーID */
  private int uid;

  /** ファイル操作オブジェクト */
  private final ProjectFile<EipTProjectTaskCommentFile, EipTProjectTaskComment> pfile =
    new ProjectFile<EipTProjectTaskCommentFile, EipTProjectTaskComment>(
      EipTProjectTaskCommentFile.class);

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
    super.init(action, rundata, context);

    loginUser = ALEipUtils.getALEipUser(rundata);
    folderName = rundata.getParameters().getString("folderName");
    uid = ALEipUtils.getUserId(rundata);
  }

  /**
   * 各フィールドを初期化します。
   */
  @Override
  public void initField() {
    // 内容
    comment = new ALStringField();
    comment.setFieldName(getl10n("PROJECT_CONTENT"));
    comment.setNotNull(true);
    // ファイルリスト
    fileuploadList = new ArrayList<FileuploadBean>();
  }

  /**
   * 汎用データベースの各フィールドに対する制約条件を設定します。
   */
  @Override
  protected void setValidator() {
    // 内容必須項目
    comment.setNotNull(true);
  }

  /**
   * データに値を設定します。
   *
   * @param rundata
   *          RunData
   * @param context
   *          Context
   * @param msgList
   *          エラーメッセージリスト
   * @return TRUE 成功 FALSE 失敗
   */
  @Override
  protected boolean setFormData(RunData rundata, Context context,
      List<String> msgList) throws ALPageNotFoundException, ALDBErrorException {
    boolean res = super.setFormData(rundata, context, msgList);
    try {
      if (res) {
        // アップロードファイル
        fileuploadList = pfile.getFileuploadList(rundata);
      }
    } catch (Exception ex) {
      logger.error("Exception", ex);
      res = false;
    }
    return res;
  }

  /**
   * タスクのフォームに入力されたデータの妥当性検証を行います。
   *
   * @param msgList
   *          エラーメッセージリスト
   * @return TRUE 成功 FALSE 失敗
   */
  @Override
  protected boolean validate(List<String> msgList) {
    try {

      comment.validate(msgList);

    } catch (Exception ex) {
      logger.error("Exception", ex);
      return false;
    }

    return msgList.isEmpty();
  }

  /**
   * タスクコメントをデータベースから読み出します。
   *
   * @param rundata
   *          RunData
   * @param context
   *          Context
   * @param msgList
   *          エラーメッセージリスト
   * @return TRUE 成功 FALSE 失敗
   */
  @Override
  protected boolean loadFormData(RunData rundata, Context context,
      List<String> msgList) {
    return true;
  }

  /**
   * タスクコメントをデータベースに格納します。
   *
   * @param rundata
   *          RunData
   * @param context
   *          Context
   * @param msgList
   *          エラーメッセージリスト
   * @return TRUE 成功 FALSE 失敗
   */
  @Override
  protected boolean insertFormData(RunData rundata, Context context,
      List<String> msgList) {
    try {

      // ログインユーザーの取得
      TurbineUser tuser =
        Database
          .get(TurbineUser.class, loginUser.getUserId().getValueWithInt());

      // 新規オブジェクトモデル
      EipTProjectTaskComment newComment =
        Database.create(EipTProjectTaskComment.class);

      // タスク
      newComment.setEipTProjectTask(ProjectUtils.getEipTProjectTask(
        rundata,
        context));
      // コメント
      newComment.setComment(comment.getValue());
      // 作成者
      newComment.setTurbineUser(tuser);
      // 作成日
      newComment.setCreateDate(Calendar.getInstance().getTime());
      // 更新日
      newComment.setUpdateDate(Calendar.getInstance().getTime());

      // 添付ファイルを登録する．
      pfile.insertAttachmentFiles(
        fileuploadList,
        folderName,
        uid,
        newComment,
        msgList);

      // タスクを登録
      Database.commit();

      // イベントログに保存
      ALEventlogFactoryService.getInstance().getEventlogHandler().log(
        newComment.getCommentId(),
        ALEventlogConstants.PORTLET_TYPE_PROJECT,
        getl10n("PROJECT_EVENTLOG_TASK_COMMENT_ADD"));

    } catch (Exception ex) {
      Database.rollback();
      logger.error("Exception", ex);
      return false;
    }
    return true;
  }

  /**
   * タスクコメントを更新します。
   *
   * @param rundata
   *          RunData
   * @param context
   *          Context
   * @param msgList
   *          エラーメッセージリスト
   * @return TRUE 成功 FALSE 失敗
   */
  @Override
  protected boolean updateFormData(RunData rundata, Context context,
      List<String> msgList) {
    try {

    } catch (Exception ex) {
      Database.rollback();
      logger.error("Exception", ex);
      return false;
    }
    return true;
  }

  /**
   * タスクコメントをデータベースから削除します。
   *
   * @param rundata
   *          RunData
   * @param context
   *          Context
   * @param msgList
   *          エラーメッセージリスト
   * @return TRUE 成功 FALSE 失敗
   */
  @Override
  protected boolean deleteFormData(RunData rundata, Context context,
      List<String> msgList) {
    try {

      String commentId =
        ALEipUtils.getParameter(rundata, context, "comment_id");

      // オブジェクトモデルを取得
      EipTProjectTaskComment taskComment =
        ProjectUtils.getEipTProjectTaskComment(commentId);
      if (taskComment == null) {
        return false;
      }

      // タスクコメントに紐づくファイルを削除
      pfile.removeFile(
        rundata,
        EipTProjectTaskCommentFile.EIP_TPROJECT_TASK_COMMENT_PROPERTY,
        taskComment.getCommentId());

      // タスクコメントを削除
      Database.delete(taskComment);
      Database.commit();

      // イベントログに保存
      ALEventlogFactoryService.getInstance().getEventlogHandler().log(
        taskComment.getCommentId(),
        ALEventlogConstants.PORTLET_TYPE_PROJECT,
        getl10n("PROJECT_EVENTLOG_TASK_COMMENT_DELETE"));

    } catch (Exception ex) {
      Database.rollback();
      logger.error("Exception", ex);
      return false;
    }
    return true;
  }

  /**
   * コメントを取得します。
   *
   * @return コメント
   */
  public ALStringField getComment() {
    return comment;
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
    }
    return true;
  }
}
