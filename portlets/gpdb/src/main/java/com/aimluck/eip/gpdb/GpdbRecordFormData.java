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
package com.aimluck.eip.gpdb;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.jetspeed.services.logging.JetspeedLogFactoryService;
import org.apache.jetspeed.services.logging.JetspeedLogger;
import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;

import com.aimluck.commons.field.ALDateTimeField;
import com.aimluck.commons.utils.ALStringUtil;
import com.aimluck.eip.cayenne.om.portlet.EipTGpdb;
import com.aimluck.eip.cayenne.om.portlet.EipTGpdbRecord;
import com.aimluck.eip.cayenne.om.portlet.EipTGpdbRecordFile;
import com.aimluck.eip.cayenne.om.security.TurbineUser;
import com.aimluck.eip.common.ALAbstractFormData;
import com.aimluck.eip.common.ALDBErrorException;
import com.aimluck.eip.common.ALEipConstants;
import com.aimluck.eip.common.ALEipUser;
import com.aimluck.eip.common.ALPageNotFoundException;
import com.aimluck.eip.fileupload.beans.FileuploadLiteBean;
import com.aimluck.eip.fileupload.util.FileuploadUtils;
import com.aimluck.eip.gpdb.util.GpdbUtils;
import com.aimluck.eip.modules.actions.common.ALAction;
import com.aimluck.eip.orm.Database;
import com.aimluck.eip.orm.query.SelectQuery;
import com.aimluck.eip.services.eventlog.ALEventlogConstants;
import com.aimluck.eip.services.eventlog.ALEventlogFactoryService;
import com.aimluck.eip.util.ALEipUtils;

/**
 *
 */
public class GpdbRecordFormData extends ALAbstractFormData {

  /** logger */
  private static final JetspeedLogger logger = JetspeedLogFactoryService
    .getLogger(GpdbRecordFormData.class.getName());

  /** フィルタ値のキー */
  private static final String LIST_FILTER_STR = new StringBuffer().append(
    GpdbRecordSelectData.class.getSimpleName()).append(
    ALEipConstants.LIST_FILTER).toString();

  /** 入力フォームのフィールド名の先頭文字列 */
  private static final String FIELD_PREFIX = "field_";

  /** 項目定義よりフィールド一覧を保持する */
  private List<GpdbItemResultData> gpdbItemList;

  /** Webデータベースレコードマップ。キー：項目定義ID */
  private Map<String, GpdbRecordResultData> mapGpdbRecord;

  /** 区分値マップ。キー：区分値ID */
  private Map<String, List<GpdbKubunValueResultData>> mapGpdbKubunValue;

  /** WebデータベースID */
  private String gpdbId;

  /** Webデータベースオブジェクト */
  private EipTGpdb gpdb;

  /** RunData(validate用) */
  private RunData rundata;

  /** Context(validate用) */
  private Context context;

  /** 添付フォルダ名 */
  private String folderName = null;

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
    this.rundata = rundata;
    this.context = context;
    gpdbId = ALEipUtils.getTemp(rundata, context, LIST_FILTER_STR);
    if (gpdbId != null && !"".equals(gpdbId)) {
      // Webデータベースオブジェクト
      gpdb = GpdbUtils.getEipTGpdb(gpdbId);
      // 項目定義を取得（編集画面ではsetFormDataが呼ばれないためここに記述）
      gpdbItemList = GpdbUtils.getGpdbItemResultList(gpdbId);

    }
    // 区分値を取得（編集画面ではsetFormDataが呼ばれないためここに記述）
    mapGpdbKubunValue = GpdbUtils.getKubunMap();
  }

  /**
   * 各フィールドを初期化します。
   */
  @Override
  public void initField() {
  }

  /**
   * Webデータベースの各フィールドに対する制約条件を設定します。
   */
  @Override
  protected void setValidator() {
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
    } catch (Exception ex) {
      logger.error("Exception", ex);
      res = false;
    }
    return res;
  }

  /**
   * Webデータベースのフォームに入力されたデータの妥当性検証を行います。
   *
   * @param msgList
   *          エラーメッセージリスト
   * @return TRUE 成功 FALSE 失敗
   */
  @Override
  protected boolean validate(List<String> msgList) {
    try {
      for (GpdbItemResultData item : gpdbItemList) {
        String field = FIELD_PREFIX + item.getGpdbItemId(); // フィールド名
        String type = item.getType().getValue(); // 入力形式
        String value = null; // 入力値

        if (GpdbUtils.ITEM_TYPE_SEQ.equals(type)
          || GpdbUtils.ITEM_TYPE_CREATE_USER.equals(type)
          || GpdbUtils.ITEM_TYPE_UPDATE_USER.equals(type)
          || GpdbUtils.ITEM_TYPE_CREATE_DATE.equals(type)
          || GpdbUtils.ITEM_TYPE_UPDATE_DATE.equals(type)) {
          // 登録者、更新者、作成日、更新日時、自動採番はチェックしない
          continue;

        } else if (GpdbUtils.ITEM_TYPE_SELECT_MULTI.equals(type)) {
          // 選択式（複数）
          // 未選択の場合に入力値が無視される（更新前の値になる）ため別で取得
          value = GpdbUtils.getParamValueString(rundata, field); // 入力値

        } else if (GpdbUtils.ITEM_TYPE_FILE.equals(type)
          || GpdbUtils.ITEM_TYPE_IMAGE.equals(type)) {

          List<FileuploadLiteBean> fileuploadList =
            GpdbUtils.getFileuploadList(rundata, item
              .getGpdbItemId()
              .toString());

          if (fileuploadList != null && !fileuploadList.isEmpty()) {
            value = fileuploadList.get(0).getFileName();
          }

        } else {
          value = ALEipUtils.getParameter(rundata, context, field); // 入力値
        }

        if (GpdbUtils.FLG_ON.equals(item.getRequiredFlg().getValue())) {
          // 必須チェック

          if (value == null || "".equals(value.trim())) {
            msgList.add("『 <span class='em'>"
              + item.getGpdbItemName()
              + "</span> 』を入力してください。");

          } else if (GpdbUtils.ITEM_TYPE_DATE.equals(type)) {
            // 日付の場合、選択されていなければエラー
            // 未選択でもvalueには日付が入るため、年の項目で判断
            String dateYear =
              ALEipUtils.getParameter(rundata, context, field + "_year");

            if (dateYear == null || "".equals(dateYear.trim())) {
              msgList.add("『 <span class='em'>"
                + item.getGpdbItemName()
                + "</span> 』を入力してください。");
            }
          }
        }

        if (GpdbUtils.ITEM_TYPE_MAIL.equals(type)) {
          // メールアドレス
          if (value != null
            && value.trim().length() > 0
            && !ALStringUtil.isMailAddress(value)) {
            msgList.add("『 <span class='em'>"
              + item.getGpdbItemName()
              + " </span>』を正しく入力してください。");
          }
        } else if (GpdbUtils.ITEM_TYPE_LINK.equals(type)) {
          // リンク
          if (value != null
            && value.trim().length() > 0
            && !GpdbUtils.isUrl(value)) {
            msgList.add("『 <span class='em'>"
              + item.getGpdbItemName()
              + "</span> 』を正しく入力してください。");
          }
        }
      }

    } catch (Exception ex) {
      logger.error("Exception", ex);
      return false;
    }

    return msgList.isEmpty();
  }

  /**
   * Webデータベースをデータベースから読み出します。
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

      // レコード情報を取得
      mapGpdbRecord =
        GpdbUtils.getGpdbRecordMap(gpdbId, ALEipUtils.getTemp(
          rundata,
          context,
          ALEipConstants.ENTITY_ID));

      // 添付ファイルを設定する
      for (GpdbItemResultData item : gpdbItemList) {

        String type = item.getType().getValue();
        if (!GpdbUtils.ITEM_TYPE_FILE.equals(type)
          && !GpdbUtils.ITEM_TYPE_IMAGE.equals(type)) {
          continue;
        }

        GpdbRecordResultData data =
          mapGpdbRecord.get("" + item.getGpdbItemId().getValue());

        data.setAttachmentFiles(GpdbUtils.getAttachmentFiles((int) data
          .getGpdbRecordId()
          .getValue()));
      }

    } catch (Exception ex) {
      logger.error("Exception", ex);
      return false;
    }
    return true;
  }

  /**
   * Webデータベースをデータベースに格納します。
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
        Database.get(TurbineUser.class, ALEipUtils.getUserId(rundata));

      // レコードNOを決定
      SelectQuery<EipTGpdbRecord> query = Database.query(EipTGpdbRecord.class);
      query.setQualifier(ExpressionFactory.matchExp(
        EipTGpdbRecord.GPDB_PROPERTY,
        gpdbId));
      query.orderDesending(EipTGpdbRecord.RECORD_NO_PROPERTY);
      EipTGpdbRecord maxRecordNoObj = query.fetchSingle();

      int recordNo = 1;
      if (maxRecordNoObj != null) {
        recordNo = maxRecordNoObj.getRecordNo() + 1;
      }

      String mailGpdbItemName = "";
      String mailDispValue = "";

      // 新規オブジェクトモデル
      for (GpdbItemResultData item : gpdbItemList) {
        String field = FIELD_PREFIX + item.getGpdbItemId();
        String value;
        String type = item.getType().getValue();

        EipTGpdbRecord gpdbRecord = Database.create(EipTGpdbRecord.class);

        if (GpdbUtils.ITEM_TYPE_SELECT_MULTI.equals(type)) {
          // 選択式（複数）
          value = GpdbUtils.getParamValueString(rundata, field);

        } else if (GpdbUtils.ITEM_TYPE_SEQ.equals(type)) {
          // 自動採番

          // 最大値＋１を設定する
          // （基本的にレコードNoと同じ値になる気がする）
          SelectQuery<EipTGpdbRecord> querySeq =
            Database.query(EipTGpdbRecord.class);
          querySeq.setQualifier(ExpressionFactory.matchExp(
            EipTGpdbRecord.GPDB_ITEM_PROPERTY,
            item.getGpdbItemId().getValue()));
          querySeq.orderDesending(EipTGpdbRecord.RECORD_NO_PROPERTY);
          EipTGpdbRecord maxSeqObj = querySeq.fetchSingle();

          int seqNo = 1;
          if (maxSeqObj != null && maxSeqObj.getValue() != null) {
            try {
              seqNo = Integer.valueOf(maxSeqObj.getValue()) + 1;
            } catch (Exception e) {
              seqNo = 1;
            }
          }

          value = "" + seqNo;

        } else if (GpdbUtils.ITEM_TYPE_FILE.equals(type)
          || GpdbUtils.ITEM_TYPE_IMAGE.equals(type)) {
          // ファイル、画像

          // 添付ファイルリスト
          List<FileuploadLiteBean> fileuploadList =
            GpdbUtils.getFileuploadList(rundata, item
              .getGpdbItemId()
              .toString());

          folderName =
            rundata.getParameters().getString(
              "folderName_" + item.getGpdbItemId().toString());

          // ファイルをデータベースに登録する．
          if (!GpdbUtils.insertFileDataDelegate(
            rundata,
            context,
            gpdbRecord,
            fileuploadList,
            folderName,
            msgList)) {
            return false;
          }

          StringBuilder sb = new StringBuilder();
          if (fileuploadList != null) {
            for (FileuploadLiteBean bean : fileuploadList) {
              if (sb.length() > 0) {
                sb.append(GpdbUtils.KUBUN_VALUE_SEPARATOR);
              }
              sb.append(bean.getFileName());
            }
          }
          value = sb.toString();

        } else if (GpdbUtils.ITEM_TYPE_CREATE_USER.equals(type)
          || GpdbUtils.ITEM_TYPE_UPDATE_USER.equals(type)) {
          // 登録者、更新者
          value =
            ALEipUtils.getALEipUser(rundata).getUserId().getValueAsString();

        } else if (GpdbUtils.ITEM_TYPE_CREATE_DATE.equals(type)
          || GpdbUtils.ITEM_TYPE_UPDATE_DATE.equals(type)) {
          // 作成日、更新日時
          ALDateTimeField dt = new ALDateTimeField();
          dt.setValue(Calendar.getInstance().getTime());
          value = dt.toStringDateTime();

        } else if (GpdbUtils.ITEM_TYPE_DATE.equals(type)) {
          // 日付

          // 未選択でもvalueには日付が入るため、別の項目（年）で判断
          String dateYear =
            ALEipUtils.getParameter(rundata, context, field + "_year");

          if (dateYear == null || "".equals(dateYear.trim())) {
            value = "";
          } else {
            value = ALEipUtils.getParameter(rundata, context, field);
          }

        } else {
          value = ALEipUtils.getParameter(rundata, context, field);
        }

        // -----------------------------
        // レコード情報の登録
        // -----------------------------
        // Webデータベースオブジェクト
        gpdbRecord.setGpdb(gpdb);
        // Webデータベース項目定義
        gpdbRecord.setGpdbItem(GpdbUtils.getEipTGpdbItem(item
          .getGpdbItemId()
          .getValueAsString()));
        // レコードNo
        gpdbRecord.setRecordNo(recordNo);
        // 値
        gpdbRecord.setValue(value);
        // 作成者
        gpdbRecord.setTurbineUser(tuser);
        // レコードNo
        gpdbRecord.setUpdateUserId(tuser.getUserId());
        // 作成日
        gpdbRecord.setCreateDate(Calendar.getInstance().getTime());
        // 更新日
        gpdbRecord.setUpdateDate(Calendar.getInstance().getTime());

        if ("t".equals(item.getTitleFlg().toString())) {
          mailGpdbItemName = item.getRawGpdbItemName();
          mailDispValue = value;
        }
      }

      // レコード情報を登録
      Database.commit();

      if (GpdbUtils.FLG_ON.equals(gpdb.getMailFlg())) {
        // テーブル作成者へメール送信

        // 作成者の取得
        ALEipUser createUser =
          ALEipUtils.getALEipUser(gpdb.getTurbineUser().getUserId());

        GpdbUtils.sendMail(
          rundata,
          createUser,
          gpdb,
          mailGpdbItemName,
          mailDispValue);
      }

      // イベントログに保存
      ALEventlogFactoryService.getInstance().getEventlogHandler().log(
        recordNo,
        ALEventlogConstants.PORTLET_TYPE_GPDB,
        "Webデータベース 「" + gpdb.getGpdbName() + "」 データ追加");

    } catch (Exception ex) {
      Database.rollback();
      logger.error("Exception", ex);
      return false;
    }
    return true;
  }

  /**
   * Webデータベースを更新します。
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
      // ログインユーザーの取得
      TurbineUser tuser =
        Database.get(TurbineUser.class, ALEipUtils.getUserId(rundata));

      String recordNo =
        ALEipUtils.getTemp(rundata, context, ALEipConstants.ENTITY_ID);

      // 新規オブジェクトモデル
      for (GpdbItemResultData item : gpdbItemList) {
        String field = FIELD_PREFIX + item.getGpdbItemId();
        String value;
        String type = item.getType().getValue();

        EipTGpdbRecord gpdbRecord =
          GpdbUtils.getEipTGpdbRecord(gpdbId, recordNo, item
            .getGpdbItemId()
            .toString());

        if (GpdbUtils.ITEM_TYPE_SELECT_MULTI.equals(type)) {
          // 選択式（複数）
          value = GpdbUtils.getParamValueString(rundata, field);

        } else if (GpdbUtils.ITEM_TYPE_FILE.equals(type)
          || GpdbUtils.ITEM_TYPE_IMAGE.equals(type)) {
          // ファイル、画像

          // 添付ファイルリスト
          List<FileuploadLiteBean> fileuploadList =
            GpdbUtils.getFileuploadList(rundata, item
              .getGpdbItemId()
              .toString());

          folderName =
            rundata.getParameters().getString(
              "folderName_" + item.getGpdbItemId().toString());

          // ファイルをデータベースに登録する．
          if (!GpdbUtils.insertFileDataDelegate(
            rundata,
            context,
            gpdbRecord,
            fileuploadList,
            folderName,
            msgList)) {
            return false;
          }

          StringBuilder sb = new StringBuilder();
          if (fileuploadList != null) {
            for (FileuploadLiteBean bean : fileuploadList) {
              if (sb.length() > 0) {
                sb.append(GpdbUtils.KUBUN_VALUE_SEPARATOR);
              }
              sb.append(bean.getFileName());
            }
          }
          value = sb.toString();

        } else if (GpdbUtils.ITEM_TYPE_UPDATE_USER.equals(type)) {
          // 更新者
          value =
            ALEipUtils.getALEipUser(rundata).getUserId().getValueAsString();

        } else if (GpdbUtils.ITEM_TYPE_UPDATE_DATE.equals(type)) {
          // 更新日時
          ALDateTimeField dt = new ALDateTimeField();
          dt.setValue(Calendar.getInstance().getTime());
          value = dt.toStringDateTime();

        } else if (GpdbUtils.ITEM_TYPE_DATE.equals(type)) {
          // 日付

          String dateYear =
            ALEipUtils.getParameter(rundata, context, field + "_year");

          if (dateYear == null || "".equals(dateYear.trim())) {
            // 未選択でもvalueには日付が入るため、年の項目で判断
            value = "";
          } else {
            value = ALEipUtils.getParameter(rundata, context, field);
          }

        } else if (GpdbUtils.ITEM_TYPE_SEQ.equals(type)
          || GpdbUtils.ITEM_TYPE_CREATE_USER.equals(type)
          || GpdbUtils.ITEM_TYPE_CREATE_DATE.equals(type)) {
          // 登録者、作成日、自動採番
          value = gpdbRecord.getValue(); // 変更なし（更新日のみ変更する）

        } else {
          value = ALEipUtils.getParameter(rundata, context, field);
        }

        // -----------------------------
        // レコード情報の更新
        // -----------------------------
        // 値
        gpdbRecord.setValue(value);
        // 更新者
        gpdbRecord.setUpdateUserId(tuser.getUserId());
        // 更新日
        gpdbRecord.setUpdateDate(Calendar.getInstance().getTime());
      }

      // Webデータベースを更新
      Database.commit();

      // イベントログに保存
      ALEventlogFactoryService.getInstance().getEventlogHandler().log(
        Integer.valueOf(recordNo),
        ALEventlogConstants.PORTLET_TYPE_GPDB,
        "Webデータベース 「" + gpdb.getGpdbName() + "」 データ更新");

    } catch (Exception ex) {
      Database.rollback();
      logger.error("Exception", ex);
      return false;
    }
    return true;
  }

  /**
   * Webデータベースをデータベースから削除します。
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
      String recordNo =
        ALEipUtils.getTemp(rundata, context, ALEipConstants.ENTITY_ID);

      // -----------------------------
      // レコード情報の削除
      // -----------------------------
      List<EipTGpdbRecord> gpdbRecordList =
        GpdbUtils.getEipTGpdbRecord(gpdbId, recordNo);

      // Webデータベースレコードを削除
      Database.deleteAll(gpdbRecordList);

      // 添付ファイルを削除
      GpdbUtils.removeGpdbRecordFile(rundata, gpdbRecordList);

      Database.commit();

      // イベントログに保存
      ALEventlogFactoryService.getInstance().getEventlogHandler().log(
        Integer.valueOf(recordNo),
        ALEventlogConstants.PORTLET_TYPE_GPDB,
        "Webデータベース 「" + gpdb.getGpdbName() + "」 データ削除");

    } catch (Exception ex) {
      Database.rollback();
      logger.error("Exception", ex);
      return false;
    }
    return true;
  }

  /**
   * 項目定義リストを取得します。
   *
   * @return 項目定義リスト
   */
  public List<GpdbItemResultData> getGpdbItemList() {
    return gpdbItemList;
  }

  /**
   * 指定した項目定義IDのレコード情報を取得します。
   *
   * @param gpdbItemId
   *          項目定義ID
   * @return レコード情報
   */
  public GpdbRecordResultData getRecordResult(String gpdbItemId) {
    if (mapGpdbRecord == null) {
      // 登録画面はmapGpdbRecordを取得しない
      return null;
    }
    return mapGpdbRecord.get(gpdbItemId);
  }

  /**
   * 指定区分の区分値リストを取得します。
   *
   * @param kubunId
   *          区分マスタID
   * @return 区分値リスト
   */
  public List<GpdbKubunValueResultData> getKubunValueList(String kubunId) {
    return mapGpdbKubunValue.get(kubunId);
  }

  /**
   * フォルダ名を取得します。
   *
   * @return フォルダ名
   */
  public String getFolderName() {
    return folderName;
  }

  /**
   * Webデータベースを取得します。
   *
   * @return Webデータベース
   */
  public EipTGpdb getGpdb() {
    return gpdb;
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
  // @Override
  @Override
  protected boolean extValidate(RunData rundata, Context context,
      List<String> msgList) {
    if (ALEipConstants.MODE_INSERT.equals(getMode())) {
      for (GpdbItemResultData item : gpdbItemList) {
        String type = item.getType().getValue(); // 入力形式
        if (GpdbUtils.ITEM_TYPE_FILE.equals(type)
          || GpdbUtils.ITEM_TYPE_IMAGE.equals(type)) {
          List<FileuploadLiteBean> fileuploadList =
            GpdbUtils.getFileuploadList(rundata, item
              .getGpdbItemId()
              .toString());

          return FileuploadUtils.insertValidate(
            msgList,
            fileuploadList,
            hasAttachmentInsertAuthority());
        }
      }
    } else if (ALEipConstants.MODE_UPDATE.equals(getMode())) {

      try {
        for (GpdbItemResultData item : gpdbItemList) {
          String type = item.getType().getValue(); // 入力形式
          if (GpdbUtils.ITEM_TYPE_FILE.equals(type)
            || GpdbUtils.ITEM_TYPE_IMAGE.equals(type)) {

            List<FileuploadLiteBean> fileuploadList =
              GpdbUtils.getFileuploadList(rundata, item
                .getGpdbItemId()
                .toString());

            // サーバーに残すファイルのID
            List<Integer> formIdList =
              getRequestedHasFileIdList(fileuploadList);
            // 現在選択しているエントリが持っているファイル
            List<EipTGpdbRecordFile> files =
              GpdbUtils.getEipTGpdbRecordFileList(item
                .getGpdbItemId()
                .getValueWithInt());
            List<Integer> existFileIdList = new ArrayList<Integer>();
            if (files != null) {
              for (EipTGpdbRecordFile file : files) {
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
          }
        }

      } catch (Exception ex) {
        logger.error("BlogEntryFormData.", ex);
        return false;
      }
    }
    return true;
  }
}
