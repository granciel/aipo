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
import java.util.Map;

import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.jetspeed.services.logging.JetspeedLogFactoryService;
import org.apache.jetspeed.services.logging.JetspeedLogger;
import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;

import com.aimluck.commons.field.ALNumberField;
import com.aimluck.commons.field.ALStringField;
import com.aimluck.eip.cayenne.om.portlet.EipTProject;
import com.aimluck.eip.cayenne.om.portlet.EipTProjectFile;
import com.aimluck.eip.cayenne.om.portlet.EipTProjectMember;
import com.aimluck.eip.cayenne.om.security.TurbineUser;
import com.aimluck.eip.common.ALAbstractFormData;
import com.aimluck.eip.common.ALDBErrorException;
import com.aimluck.eip.common.ALEipConstants;
import com.aimluck.eip.common.ALEipGroup;
import com.aimluck.eip.common.ALEipManager;
import com.aimluck.eip.common.ALEipPost;
import com.aimluck.eip.common.ALEipUser;
import com.aimluck.eip.common.ALPageNotFoundException;
import com.aimluck.eip.fileupload.beans.FileuploadBean;
import com.aimluck.eip.fileupload.util.FileuploadUtils;
import com.aimluck.eip.modules.actions.common.ALAction;
import com.aimluck.eip.orm.Database;
import com.aimluck.eip.orm.query.SelectQuery;
import com.aimluck.eip.project.util.ProjectFile;
import com.aimluck.eip.project.util.ProjectUtils;
import com.aimluck.eip.services.eventlog.ALEventlogConstants;
import com.aimluck.eip.services.eventlog.ALEventlogFactoryService;
import com.aimluck.eip.services.storage.ALStorageService;
import com.aimluck.eip.util.ALEipUtils;

/**
 *
 */
public class ProjectFormData extends ALAbstractFormData {

  /** logger */
  private static final JetspeedLogger logger = JetspeedLogFactoryService
    .getLogger(ProjectFormData.class.getName());

  /** プロジェクト名 */
  private ALStringField project_name;

  /** 説明 */
  private ALStringField explanation;

  /** 管理者ID */
  private ALNumberField admin_user_id;

  /** 管理者名 */
  private ALStringField admin_user_name;

  /** 進捗率 */
  private ALNumberField progress_rate;

  /** 進捗率自動計算フラグ */
  private ALStringField progress_flg;

  /** ログインユーザー */
  private ALEipUser loginUser;

  /** ファイルアップロードリスト */
  private List<FileuploadBean> fileuploadList;

  /** 添付フォルダ名 */
  private String folderName;

  private int uid;

  private String orgId;

  /** メンバーリスト */
  private List<ALEipUser> memberList;

  /** グループ */
  private List<ALEipGroup> myGroupList;

  /** ファイル操作オブジェクト */
  private final ProjectFile<EipTProjectFile, EipTProject> pfile =
    new ProjectFile<EipTProjectFile, EipTProject>(EipTProjectFile.class);

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
    myGroupList = ALEipUtils.getMyGroups(rundata);
    folderName = rundata.getParameters().getString("folderName");
    uid = ALEipUtils.getUserId(rundata);
  }

  /**
   * 各フィールドを初期化します。
   */
  @Override
  public void initField() {
    // プロジェクト名
    project_name = new ALStringField();
    project_name.setFieldName(getl10n("PROJECT_NAME"));
    project_name.setTrim(true);
    // 説明
    explanation = new ALStringField();
    explanation.setFieldName(getl10n("PROJECT_EXPLANATION"));
    explanation.setTrim(true);
    // 管理者ID
    admin_user_id = new ALNumberField();
    admin_user_id.setFieldName(getl10n("PROJECT_ADMIN_USER"));
    // 進捗率
    progress_rate = new ALNumberField();
    progress_rate.setFieldName(getl10n("PROJECT_PROGRESS_RATE"));
    progress_rate.setValue(0);
    // 進捗率自動計算フラグ
    progress_flg = new ALStringField();
    progress_flg.setFieldName(getl10n("PROJECT_MAIL_SEND"));
    progress_flg.setTrim(false);
    progress_flg.setValue(ProjectUtils.FLG_OFF);
    // ファイルリスト
    fileuploadList = new ArrayList<FileuploadBean>();
    // メンバーリスト
    memberList = new ArrayList<ALEipUser>();
  }

  /**
   * プロジェクトの各フィールドに対する制約条件を設定します。
   */
  @Override
  protected void setValidator() {
    // プロジェクト名必須項目
    project_name.setNotNull(true);
    // プロジェクト名の文字数制限
    project_name.limitMaxLength(50);
    // 管理者必須項目
    admin_user_id.setNotNull(true);
    // 進捗率の入力範囲制限
    progress_rate.limitMaxValue(100);
    progress_rate.limitMinValue(0);
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

        String memberNames[] = rundata.getParameters().getStrings("members");
        if (memberNames != null && memberNames.length > 0) {
          SelectQuery<TurbineUser> query = Database.query(TurbineUser.class);
          Expression exp =
            ExpressionFactory.inExp(
              TurbineUser.LOGIN_NAME_PROPERTY,
              memberNames);
          query.setQualifier(exp);
          memberList.addAll(ALEipUtils.getUsersFromSelectQuery(query));
        }
        fileuploadList = pfile.getFileuploadList(rundata);
      }
    } catch (Exception ex) {
      logger.error("Exception", ex);
      res = false;
    }
    return res;
  }

  /**
   * プロジェクトのフォームに入力されたデータの妥当性検証を行います。
   *
   * @param msgList
   *          エラーメッセージリスト
   * @return TRUE 成功 FALSE 失敗
   */
  @Override
  protected boolean validate(List<String> msgList) {
    try {

      project_name.validate(msgList);
      admin_user_id.validate(msgList);

    } catch (Exception ex) {
      logger.error("Exception", ex);
      return false;
    }

    return msgList.isEmpty();
  }

  /**
   * プロジェクトをデータベースから読み出します。
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
    try {
      // オブジェクトモデルを取得
      EipTProject project = ProjectUtils.getEipTProject(rundata, context);
      if (project == null) {
        return false;
      }
      // プロジェクト名
      project_name.setValue(project.getProjectName());

      // 説明
      explanation.setValue(project.getExplanation());

      // 管理者ID
      admin_user_id.setValue(project.getAdminUserId());

      // 進捗率
      progress_rate.setValue(project.getProgressRate());

      // 進捗率自動計算フラグ
      progress_flg.setValue(project.getProgressFlg());

      // 参加ユーザー
      List<EipTProjectMember> members =
        ProjectUtils.getEipTProjectMember(project);
      EipTProjectMember member = null;

      int size = members.size();
      for (int i = 0; i < size; i++) {
        member = members.get(i);
        int user_id = member.getUserId().intValue();
        memberList.add(ALEipUtils.getALEipUser(user_id));
      }

      // ファイル
      List<EipTProjectFile> files =
        pfile.getSelectQueryForFiles(
          EipTProject.PROJECT_ID_PK_COLUMN,
          project.getProjectId()).fetchList();
      fileuploadList = pfile.getFileList(files);

    } catch (Exception ex) {
      logger.error("Exception", ex);
      return false;
    }
    return true;
  }

  /**
   * プロジェクトをデータベースに格納します。
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
      EipTProject project = Database.create(EipTProject.class);
      // プロジェクト名
      project.setProjectName(project_name.getValue());
      // 説明
      project.setExplanation(explanation.getValue());
      // 管理者ID
      project.setAdminUserId((int) admin_user_id.getValue());
      // 進捗率自動計算フラグ
      project.setProgressFlg(progress_flg.getValue());
      // 作成者
      project.setTurbineUser(tuser);
      // 更新者
      project.setUpdateUserId(tuser.getUserId());
      // 作成日
      project.setCreateDate(Calendar.getInstance().getTime());
      // 更新日
      project.setUpdateDate(Calendar.getInstance().getTime());

      // 進捗率
      int progressRate = (int) progress_rate.getValue();
      if (ProjectUtils.FLG_ON.equals(progress_flg.getValue())) {
        // 自動計算する場合、プロジェクト作成したてなので進捗0%
        progressRate = 0;
      }
      project.setProgressRate(progressRate);

      // プロジェクトを登録
      Database.commit();

      // -----------------------
      // メンバーの登録
      // -----------------------

      List<ALEipUser> mailUserList = new ArrayList<ALEipUser>();
      boolean containsAdminUser = false;
      for (ALEipUser user : memberList) {
        EipTProjectMember member = Database.create(EipTProjectMember.class);
        member.setEipTProject(project);
        member.setUserId((int) user.getUserId().getValue());
        if (admin_user_id.getValue() == user.getUserId().getValue()) {
          containsAdminUser = true;
        }
        mailUserList.add(user);
      }

      // プロジェクトの管理者がメンバーに含まれない場合は、さらに管理者をメンバーに登録する
      if (!containsAdminUser) {
        EipTProjectMember member = Database.create(EipTProjectMember.class);
        member.setEipTProject(project);
        member.setUserId((int) admin_user_id.getValue());
        ALEipUser user =
          ALEipUtils.getALEipUser((int) admin_user_id.getValue());
        if (user != null) {
          mailUserList.add(user);
        }
      }

      if (mailUserList.size() > 0) {
        ProjectUtils.sendMailForProjectMembers(
          rundata,
          context,
          project,
          mailUserList);
      }

      // メンバーを登録
      Database.commit();

      // 添付ファイルを登録する．
      pfile.insertAttachmentFiles(
        fileuploadList,
        folderName,
        uid,
        project,
        msgList);

      // イベントログに保存
      ALEventlogFactoryService.getInstance().getEventlogHandler().log(
        project.getProjectId(),
        ALEventlogConstants.PORTLET_TYPE_PROJECT,
        getl10nFormat("PROJECT_EVENTLOG_ADD", project.getProjectName()));

    } catch (Exception ex) {
      Database.rollback();
      logger.error("Exception", ex);
      return false;
    }
    return true;
  }

  /**
   * プロジェクトを更新します。
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
      // オブジェクトモデルを取得
      EipTProject project = ProjectUtils.getEipTProject(rundata, context);
      if (project == null) {
        return false;
      }

      // ログインユーザーの取得
      TurbineUser tuser =
        Database
          .get(TurbineUser.class, loginUser.getUserId().getValueWithInt());

      // プロジェクト名
      project.setProjectName(project_name.getValue());
      // 説明
      project.setExplanation(explanation.getValue());
      // 管理者ID
      project.setAdminUserId((int) admin_user_id.getValue());
      // 進捗率自動計算フラグ
      project.setProgressFlg(progress_flg.getValue());
      // 更新者
      project.setUpdateUserId(tuser.getUserId());
      // 更新日
      project.setUpdateDate(Calendar.getInstance().getTime());

      // 進捗率
      int progressRate = (int) progress_rate.getValue();
      if (ProjectUtils.FLG_ON.equals(progress_flg.getValue())) {
        // 自動計算する場合
        progressRate =
          ProjectUtils.getProjectProgressRate(project.getProjectId());
      }
      project.setProgressRate(progressRate);

      // -----------------------
      // メンバーの登録
      // -----------------------

      List<ALEipUser> oldMenber =
        ProjectUtils.getProjectMembers(project.getProjectId());
      List<Integer> oldMemberIdList = new ArrayList<Integer>();
      for (ALEipUser rd : oldMenber) {
        oldMemberIdList.add(rd.getUserId().getValueWithInt());
      }

      // メンバーを削除
      ProjectUtils.removeProjectMember(project);

      List<ALEipUser> mailUserList = new ArrayList<ALEipUser>();
      boolean containsAdminUser = false;
      for (ALEipUser user : memberList) {
        EipTProjectMember member = Database.create(EipTProjectMember.class);
        member.setEipTProject(project);
        member.setUserId((int) user.getUserId().getValue());
        if (admin_user_id.getValue() == user.getUserId().getValue()) {
          containsAdminUser = true;
        }
        if (!oldMemberIdList.contains(member.getUserId())) {
          mailUserList.add(user);
        }
      }

      // プロジェクトの管理者がメンバーに含まれない場合は、さらに管理者をメンバーに登録する
      if (!containsAdminUser) {
        EipTProjectMember member = Database.create(EipTProjectMember.class);
        member.setEipTProject(project);
        member.setUserId((int) admin_user_id.getValue());
        ALEipUser user =
          ALEipUtils.getALEipUser((int) admin_user_id.getValue());
        if (user != null) {
          mailUserList.add(user);
        }
      }

      if (mailUserList.size() > 0) {
        ProjectUtils.sendMailForProjectMembers(
          rundata,
          context,
          project,
          mailUserList);
      }

      // サーバーに残すファイルのID
      List<Integer> attIdList = pfile.getRequestedHasFileIdList(fileuploadList);
      // 現在選択しているエントリが持っているファイル
      List<EipTProjectFile> files =
        pfile.getSelectQueryForFiles(
          EipTProject.PROJECT_ID_PK_COLUMN,
          project.getProjectId()).fetchList();
      if (files != null) {
        for (int i = 0; i < files.size(); i++) {
          EipTProjectFile file = files.get(i);
          if (!attIdList.contains(file.getFileId())) {
            // ファイルシステムから削除
            ALStorageService.deleteFile(ProjectFile.getSaveDirPath(uid)
              + file.getFilePath());

            // DBから削除
            Database.delete(file);

          }
        }
      }

      // 添付ファイルを登録する．
      pfile.insertAttachmentFiles(
        fileuploadList,
        folderName,
        uid,
        project,
        msgList);

      // プロジェクトを更新
      Database.commit();

      // イベントログに保存
      ALEventlogFactoryService.getInstance().getEventlogHandler().log(
        project.getProjectId(),
        ALEventlogConstants.PORTLET_TYPE_PROJECT,
        getl10nFormat("PROJECT_EVENTLOG_UPDATE", project.getProjectName()));

    } catch (Exception ex) {
      Database.rollback();
      logger.error("Exception", ex);
      return false;
    }
    return true;
  }

  /**
   * プロジェクトをデータベースから削除します。
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
      // オブジェクトモデルを取得
      EipTProject project = ProjectUtils.getEipTProject(rundata, context);
      if (project == null) {
        return false;
      }

      if (!ProjectUtils.getEipTProjectTaskCheck(project.getProjectId())) {
        msgList.add(getl10n("PROJECT_VALIDATE_NOT_DELETE"));
        return false;
      }

      // プロジェクトデータベースを削除
      Database.delete(project);

      // ファイル削除処理
      List<String> fpaths = new ArrayList<String>();
      SelectQuery<EipTProjectFile> query =
        Database.query(EipTProjectFile.class);
      query.andQualifier(ExpressionFactory.matchExp(
        EipTProjectFile.EIP_TPROJECT_PROPERTY,
        project.getProjectId()));
      List<EipTProjectFile> files = query.fetchList();
      if (files != null && files.size() > 0) {
        int fsize = files.size();
        for (int j = 0; j < fsize; j++) {
          fpaths.add((files.get(j)).getFilePath());
        }
      }

      if (fpaths.size() > 0) {
        // ローカルファイルに保存されているファイルを削除する．
        int fsize = fpaths.size();
        for (int i = 0; i < fsize; i++) {
          ALStorageService.deleteFile(ProjectUtils.getSaveDirPath(orgId, uid)
            + fpaths.get(i));
        }
      }

      // 参加ユーザーを削除
      List<EipTProjectMember> member =
        ProjectUtils.getEipTProjectMember(project);
      Database.deleteAll(member);

      Database.commit();

      // イベントログに保存
      ALEventlogFactoryService.getInstance().getEventlogHandler().log(
        project.getProjectId(),
        ALEventlogConstants.PORTLET_TYPE_PROJECT,
        getl10nFormat("PROJECT_EVENTLOG_DELETE", project.getProjectName()));

    } catch (Exception ex) {
      Database.rollback();
      logger.error("Exception", ex);
      return false;
    }
    return true;
  }

  /**
   * プロジェクト名を取得します。
   *
   * @return プロジェクト名
   */
  public ALStringField getProjectName() {
    return project_name;
  }

  /**
   * 説明を取得します。
   *
   * @return 説明
   */
  public ALStringField getExplanation() {
    return explanation;
  }

  /**
   * 管理者IDを取得します。
   *
   * @return 管理者ID
   */
  public ALNumberField getAdminUserId() {
    return admin_user_id;
  }

  /**
   * 進捗率自動計算フラグを取得します。
   *
   * @return 進捗率自動計算フラグ
   */
  public ALStringField getProgressFlg() {
    return progress_flg;
  }

  /**
   * 進捗率を取得します。
   *
   * @return 進捗率
   */
  public ALNumberField getProgressRate() {
    return progress_rate;
  }

  /**
   * 自ユーザーのグループリストを取得します。
   *
   * @return 自ユーザーのグループリスト
   */
  public List<ALEipGroup> getMyGroupList() {
    return myGroupList;
  }

  /**
   * 部署情報を取得する
   *
   * @return 部署情報
   */
  public Map<Integer, ALEipPost> getPostMap() {
    return ALEipManager.getInstance().getPostMap();
  }

  /**
   * ログインユーザーIDを返します。
   *
   * @return ログインユーザーID
   */
  public int getLoginUserId() {
    return loginUser.getUserId().getValueWithInt();
  }

  /**
   * アップロードファイルリストを返します。
   *
   * @return アップロードファイルリスト
   */
  public List<FileuploadBean> getAttachmentFileNameList() {
    return fileuploadList;
  }

  /**
   * @return admin_user_name
   */
  public ALStringField getAdmin_user_name() {
    return admin_user_name;
  }

  /**
   * グループメンバーを取得します。 <BR>
   *
   * @return
   */
  public List<ALEipUser> getMemberList() {
    return memberList;
  }

  /**
   * グループメンバーを格納します。 <BR>
   *
   * @param str
   * @return
   */
  public void setMemberList(ArrayList<ALEipUser> list) {
    memberList = list;
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
        // オブジェクトモデルを取得
        Integer projectId = ProjectUtils.getEipTProjectId(rundata, context);
        if (projectId == null) {
          return false;
        }
        // サーバーに残すファイルのID
        List<Integer> formIdList =
          pfile.getRequestedHasFileIdList(fileuploadList);
        // 現在選択しているエントリが持っているファイル
        List<EipTProjectFile> files =
          pfile.getSelectQueryForFiles(
            EipTProject.PROJECT_ID_PK_COLUMN,
            projectId).fetchList();
        List<Integer> existFileIdList = new ArrayList<Integer>();
        if (files != null) {
          for (EipTProjectFile file : files) {
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
        logger.error("ProjectFormData.", ex);
        return false;
      }
    }
    return true;
  }

}
