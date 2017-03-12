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

import static com.aimluck.eip.util.ALLocalizationUtils.*;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;

import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.commons.lang.StringUtils;
import org.apache.jetspeed.services.logging.JetspeedLogFactoryService;
import org.apache.jetspeed.services.logging.JetspeedLogger;
import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;

import com.aimluck.commons.field.ALNumberField;
import com.aimluck.commons.field.ALStringField;
import com.aimluck.eip.cayenne.om.portlet.EipTWiki;
import com.aimluck.eip.cayenne.om.portlet.EipTWikiFile;
import com.aimluck.eip.cayenne.om.security.TurbineUser;
import com.aimluck.eip.common.ALAbstractFormData;
import com.aimluck.eip.common.ALDBErrorException;
import com.aimluck.eip.common.ALEipConstants;
import com.aimluck.eip.common.ALPageNotFoundException;
import com.aimluck.eip.fileupload.beans.FileuploadLiteBean;
import com.aimluck.eip.fileupload.util.FileuploadUtils;
import com.aimluck.eip.modules.actions.common.ALAction;
import com.aimluck.eip.orm.Database;
import com.aimluck.eip.orm.query.SelectQuery;
import com.aimluck.eip.services.eventlog.ALEventlogConstants;
import com.aimluck.eip.services.eventlog.ALEventlogFactoryService;
import com.aimluck.eip.services.storage.ALStorageService;
import com.aimluck.eip.util.ALEipUtils;
import com.aimluck.eip.util.ALLocalizationUtils;
import com.aimluck.eip.wiki.util.WikiFileUtils;
import com.aimluck.eip.wiki.util.WikiUtils;

/**
 * Wikiのフォームデータを管理するクラスです。 <BR>
 *
 */
public class WikiFormData extends ALAbstractFormData {

  /** wiki名 */
  private ALStringField name;

  /** 親WikiID */
  private ALNumberField parentId;

  /** 親Wiki名 */
  private ALStringField parentName;

  /** 内容 */
  private ALStringField note;

  /** 親ページ判断 */
  private Boolean is_child;

  private EipTWiki category;

  /** TOP階層Wiki */
  private List<WikiResultData> topWikiList;

  private int uid;

  /** logger */
  private static final JetspeedLogger logger = JetspeedLogFactoryService
    .getLogger(WikiFormData.class.getName());

  /** ACL用の変数 * */
  private final String aclPortletFeature = "";

  private String entityId = null;

  private String mode = null;

  private String update_date = null;

  /** 添付ファイルリスト */
  private List<FileuploadLiteBean> fileuploadList =
    new ArrayList<FileuploadLiteBean>();

  /** 添付フォルダ名 */
  private String folderName = null;

  private EipTWiki parentWiki = null;

  private String destWikiName = "";

  /**
   * @param action
   * @param rundata
   * @param context
   */
  @Override
  public void init(ALAction action, RunData rundata, Context context)
      throws ALPageNotFoundException, ALDBErrorException {
    super.init(action, rundata, context);
    String _isChild = rundata.getParameters().getString("is_child");
    if (null != _isChild) {
      is_child = Boolean.valueOf(_isChild);
    }
    uid = ALEipUtils.getUserId(rundata);
    entityId = ALEipUtils.getTemp(rundata, context, ALEipConstants.ENTITY_ID);
    mode = rundata.getParameters().getString(ALEipConstants.MODE, "");
    folderName = rundata.getParameters().getString("folderName");
    update_date = ALEipUtils.getTemp(rundata, context, "update_date");
  }

  /**
   *
   * @param rundata
   * @param context
   */
  public void loadTopWikiList(RunData rundata, Context context) {
    this.topWikiList = WikiUtils.loadTopWikiList(rundata);
  }

  /**
   * 各フィールドを初期化します。 <BR>
   */
  @Override
  public void initField() {
    name = new ALStringField();
    name.setFieldName(getl10n("WIKI_TITLE"));
    name.setTrim(true);
    parentId = new ALNumberField();
    parentId.setFieldName(getl10n("WIKI_PARENT"));
    parentId.setValue(0);
    parentName = new ALStringField();
    parentName.setFieldName(getl10n("WIKI_PARENT"));
    note = new ALStringField();
    note.setFieldName(getl10n("WIKI_NOTE"));
    note.setTrim(false);
  }

  /**
   * Wikiの各フィールドに対する制約条件を設定します。 <BR>
   */
  @Override
  protected void setValidator() {
    // wiki名必須項目
    name.setNotNull(true);
    // wiki名の文字数制限
    name.limitMaxLength(50);
    // メモ必須項目
    note.setNotNull(true);
    // メモの文字数制限
    note.limitMaxLength(10000);
    if (is_child != null && is_child) {
      parentId.setNotNull(true);
      parentId.limitValue(0, Integer.MAX_VALUE);
    }
  }

  /**
   * Wikiのフォームに入力されたデータの妥当性検証を行います。 <BR>
   *
   * @param msgList
   * @return TRUE 成功 FALSE 失敗
   *
   */
  @Override
  protected boolean validate(List<String> msgList) {
    // wiki名
    name.validate(msgList);
    // メモ
    note.validate(msgList);
    if (is_child != null && is_child) {
      // 親wiki名
      if (parentId.validate(msgList)) {
        if (parentId.getValueWithInt() != 0) {
          EipTWiki parentWiki =
            WikiUtils.getEipTWiki(parentId.getValueWithInt());
          if (parentWiki != null) {
            if (parentWiki.getParentId() != 0) {
              msgList.add(ALLocalizationUtils
                .getl10n("WIKI_CONFLICT_PARENT_ERROR"));
            }
          } else {
            msgList.add(ALLocalizationUtils
              .getl10n("WIKI_CONFLICT_PARENT_ERROR"));
          }
        }
      }
    }

    boolean duplication = false;
    if (ALEipConstants.MODE_UPDATE.equals(mode)
      && !StringUtils.isEmpty(entityId)
      && StringUtils.isNumeric(entityId)) {
      duplication =
        WikiUtils.isTitleDuplicate(name.getValue(), Integer.parseInt(parentId
          .toString()), Integer.parseInt(entityId));
    } else {
      duplication =
        WikiUtils.isTitleDuplicate(name.getValue(), Integer.parseInt(parentId
          .toString()));
    }
    if (duplication) {
      msgList.add(getl10n("WIKI_DUPLICATE_TITLE"));
    }

    /** 親wikiは自分自身を親に選択できないようにする */
    if (StringUtils.isNotEmpty(entityId)
      && entityId.equals(parentId.toString())) {
      msgList.add(getl10n("WIKI_PARENT_ERROR"));
    }

    if (update_date != null && entityId != null) {
      EipTWiki eipTWiki = WikiUtils.getEipTWiki(Integer.parseInt(entityId));
      if (eipTWiki != null) {
        if (!update_date.equals(eipTWiki.getUpdateDate().toString())) {
          msgList.add(ALLocalizationUtils.getl10n("WIKI_CONFLICT_ERROR"));
        }
      } else {
        msgList.add(ALLocalizationUtils.getl10n("WIKI_ALREADY_DELETE"));
      }
    }

    return (msgList.size() == 0);
  }

  /**
   * Wikiをデータベースから読み出します。 <BR>
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
      EipTWiki wiki = WikiUtils.getEipTWiki(rundata, context);

      // wiki名
      name.setValue(wiki.getWikiName());

      parentId.setValue(wiki.getParentId());
      // 内容
      note.setValue(wiki.getNote());

      // ファイル
      SelectQuery<EipTWikiFile> query = Database.query(EipTWikiFile.class);
      query.andQualifier(ExpressionFactory.matchExp(
        EipTWikiFile.WIKI_ID_PROPERTY,
        wiki.getWikiId()));
      List<EipTWikiFile> fileList = query.fetchList();
      for (EipTWikiFile file : fileList) {
        FileuploadLiteBean fbean = new FileuploadLiteBean();
        fbean.initField();
        fbean.setFileId(file.getFileId());
        fbean.setFileName(file.getFileName());
        fileuploadList.add(fbean);
      }

      if (wiki.getParentId().intValue() != 0) {
        setParentWiki(WikiUtils.getEipTWiki(wiki.getParentId()));
        is_child = true;
      } else {
        is_child = false;
      }
      ALEipUtils.setTemp(rundata, context, "update_date", wiki
        .getUpdateDate()
        .toString());

      if (!is_child) {
        /** remove this wiki from topWikiList */
        Iterator<WikiResultData> it = topWikiList.iterator();
        while (it.hasNext()) {
          WikiResultData result = it.next();
          if (result
            .getId()
            .toString()
            .equals(String.valueOf(wiki.getWikiId()))) {
            it.remove();
          }
        }
      }

    } catch (Exception e) {
      logger.error("WikiFormData.loadFormData", e);
      return false;
    }
    return true;
  }

  /**
   * Wikiをデータベースから削除します。 <BR>
   *
   * @param rundata
   * @param context
   * @param msgList
   * @return TRUE 成功 FALSE 失敗
   */
  @Override
  protected boolean deleteFormData(RunData rundata, Context context,
      List<String> msgList) {
    try {
      // オブジェクトモデルを取得
      EipTWiki wiki = WikiUtils.getEipTWiki(rundata, context);
      if (wiki == null) {
        return false;
      }

      // entityIdの取得
      int entityId = wiki.getWikiId();

      if (WikiUtils.getChildCount(entityId) > 0) {
        msgList.add(getl10n("WIKI_HAS_CHILDREN"));
        return false;
      }

      // タイトルの取得
      String wikiName = wiki.getWikiName();
      // 添付ファイルの削除
      WikiFileUtils.deleteFiles(wiki.getWikiId());

      // Wikiを削除
      Database.delete(wiki);
      Database.commit();

      // イベントログに保存
      ALEventlogFactoryService.getInstance().getEventlogHandler().log(
        entityId,
        ALEventlogConstants.PORTLET_TYPE_WIKI,
        wikiName);

    } catch (Throwable t) {
      Database.rollback();
      logger.error("WikiFormData.deleteFormData", t);
      return false;
    }
    return true;
  }

  /**
   * Wikiをデータベースに格納します。 <BR>
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

      // 新規オブジェクトモデル
      EipTWiki wiki = Database.create(EipTWiki.class);
      // トピック名
      wiki.setWikiName(name.getValue());
      // 親ID

      if (parentId.isNotNullValue()) {
        wiki.setParentId(Integer.valueOf(parentId.toString()));
      } else if (null == category) {
        wiki.setParentId(WikiUtils.PARENT_WIKI);
      } else {
        wiki.setParentId(category.getWikiId());
      }
      // メモ
      wiki.setNote(note.getValue());
      // 作成者
      TurbineUser turbineUser = ALEipUtils.getTurbineUser(Integer.valueOf(uid));
      wiki.setCreateUser(turbineUser);
      // 更新者
      wiki.setUpdateUser(turbineUser);
      // 作成日
      wiki.setCreateDate(Calendar.getInstance().getTime());
      // 更新日
      wiki.setUpdateDate(Calendar.getInstance().getTime());

      // ファイルをデータベースに登録する．
      if (!WikiFileUtils.insertFileDataDelegate(
        rundata,
        context,
        wiki,
        fileuploadList,
        folderName,
        msgList)) {
        return false;
      }

      Database.commit();

      // 添付ファイル保存先のフォルダを削除
      ALStorageService.deleteTmpFolder(uid, folderName);

      // イベントログに保存
      ALEventlogFactoryService.getInstance().getEventlogHandler().log(
        wiki.getWikiId(),
        ALEventlogConstants.PORTLET_TYPE_WIKI,
        wiki.getWikiName());

    } catch (Exception e) {
      Database.rollback();
      logger.error("WikiFormData.insertFormData", e);
      return false;
    }
    return true;
  }

  /**
   * データベースに格納されているWikiを更新します。 <BR>
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

      // 新規オブジェクトモデル
      EipTWiki wiki = WikiUtils.getEipTWiki(rundata, context);
      // トピック名
      wiki.setWikiName(name.getValue());

      if (parentId.isNotNullValue()) {

        int entityId = wiki.getWikiId();
        if (parentId.getValue() != 0) {
          if (WikiUtils.getChildCount(entityId) > 0) {
            msgList.add(getl10n("WIKI_HAS_CHILDREN_EDIT"));
            return false;
          }
        }
        wiki.setParentId((int) parentId.getValue());
      }
      // メモ
      wiki.setNote(note.getValue());
      TurbineUser turbineUser = ALEipUtils.getTurbineUser(Integer.valueOf(uid));
      // 更新者
      wiki.setUpdateUser(turbineUser);
      // 作成日
      wiki.setCreateDate(Calendar.getInstance().getTime());
      // 更新日
      wiki.setUpdateDate(Calendar.getInstance().getTime());

      // ファイルをデータベースに登録する．
      if (!WikiFileUtils.insertFileDataDelegate(
        rundata,
        context,
        wiki,
        fileuploadList,
        folderName,
        msgList)) {
        return false;
      }

      Database.commit();

      // 添付ファイル保存先のフォルダを削除
      ALStorageService.deleteTmpFolder(uid, folderName);

      // イベントログに保存
      ALEventlogFactoryService.getInstance().getEventlogHandler().log(
        wiki.getWikiId(),
        ALEventlogConstants.PORTLET_TYPE_WIKI,
        wiki.getWikiName());

    } catch (Exception e) {
      Database.rollback();
      logger.error("WikiFormData.updateFormData", e);
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
      fileuploadList = WikiFileUtils.getFileuploadList(rundata);
    } catch (Exception e) {
      logger.error("WikiFormData.setFormData", e);
    }

    if (ALEipConstants.MODE_NEW_FORM.equals(getMode())) {
      if (isChild()) {
        /** set selected parent wiki */
        String filtertype =
          ALEipUtils.getTemp(rundata, context, WikiSelectData.class
            .getSimpleName()
            + ALEipConstants.LIST_FILTER_TYPE);
        String fileterValue =
          ALEipUtils.getTemp(rundata, context, WikiSelectData.class
            .getSimpleName()
            + ALEipConstants.LIST_FILTER);
        String wikiId =
          WikiUtils.getWikiIdFromSession(fileterValue, filtertype);
        if (StringUtils.isNotEmpty(wikiId) && StringUtils.isNumeric(wikiId)) {
          setParentWiki(WikiUtils.getEipTWiki(Integer.parseInt(wikiId)));
        }
      }
    }

    if (ALEipConstants.MODE_INSERT.equals(getMode())) {
      ALEipUtils.removeTemp(rundata, context, "update_date");
      update_date = null;
    }

    return res;
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
   * 子ページかどうか判断します。 <BR>
   *
   * @return
   */
  public boolean isChild() {
    return is_child == null ? true : is_child.booleanValue() || isChildForm();
  }

  public void setIsChild(boolean isChild) {
    this.is_child = isChild;
  }

  /**
   * カテゴリ名を取得します。
   *
   * @return
   */
  public ALStringField getParentName() {
    return parentName;
  }

  /**
   * メモを設定します。 <BR>
   *
   * @return
   */
  public void setNote(String n) {
    note.setValue(n);
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
  public ALStringField getName() {
    return name;
  }

  public List<FileuploadLiteBean> getAttachmentFileNameList() {
    return fileuploadList;
  }

  public String getFolderName() {
    return folderName;
  }

  public void setParentWiki(EipTWiki wiki) {
    this.parentWiki = wiki;
  }

  public String getParentIdString() {
    if (null != parentWiki
      && parentWiki.getParentId() != null
      && parentWiki.getParentId().intValue() != 0) {
      return String.valueOf(parentWiki.getParentId());
    } else {
      return "";
    }
  }

  public String getParentWikiIdString() {
    if (null != parentWiki) {
      return String.valueOf(parentWiki.getWikiId());
    } else {
      return "0";
    }
  }

  public String getParentWikiName() {
    if (null == parentWiki) {
      return "";
    }
    String name = parentWiki.getWikiName();
    if (StringUtils.isEmpty(name)) {
      return "";
    }
    return new ALStringField(name).toString();
  }

  public boolean isChildForm() {
    return null != parentWiki;
  }

  public void setDestWikiName(String name) {
    this.destWikiName = name;
  }

  public String getDestWikiName() {
    return new ALStringField(destWikiName).toString();
  }

  public String getParentId() {
    return parentId.toString();
  }

  public List<WikiResultData> getTopWikiList() {
    return topWikiList;
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
        int wikiId = WikiUtils.getEipTWikiId(rundata, context);

        // サーバーに残すファイルのID
        List<Integer> formIdList = getRequestedHasFileIdList(fileuploadList);
        // 現在選択しているエントリが持っているファイル
        // この辺の効率化が課題とは思うものの、一応動作はしているはず。
        SelectQuery<EipTWikiFile> dbquery = Database.query(EipTWikiFile.class);
        dbquery.andQualifier(ExpressionFactory.matchExp(
          EipTWikiFile.WIKI_ID_PROPERTY,
          wikiId));
        // dbquery.andQualifier(ExpressionFactory.matchExp(
        // EipTWikiFile.WIKI_ID_PROPERTY,
        // wikiId));
        List<EipTWikiFile> existsFiles = dbquery.fetchList();

        List<Integer> existFileIdList = new ArrayList<Integer>();
        if (existsFiles != null) {
          for (EipTWikiFile file : existsFiles) {
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
        logger.error("WikiFormData.", ex);
        return false;
      }
    }
    return true;
  }
}
