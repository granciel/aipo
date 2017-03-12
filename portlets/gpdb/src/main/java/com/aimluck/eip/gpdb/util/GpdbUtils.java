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
package com.aimluck.eip.gpdb.util;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;

import org.apache.cayenne.DataRow;
import org.apache.cayenne.PersistenceState;
import org.apache.cayenne.access.DataContext;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.commons.lang.StringUtils;
import org.apache.jetspeed.om.security.UserIdPrincipal;
import org.apache.jetspeed.portal.PortletConfig;
import org.apache.jetspeed.portal.portlets.VelocityPortlet;
import org.apache.jetspeed.services.JetspeedSecurity;
import org.apache.jetspeed.services.customlocalization.CustomLocalizationService;
import org.apache.jetspeed.services.logging.JetspeedLogFactoryService;
import org.apache.jetspeed.services.logging.JetspeedLogger;
import org.apache.jetspeed.services.resources.JetspeedResources;
import org.apache.jetspeed.util.ServiceUtil;
import org.apache.turbine.services.localization.LocalizationService;
import org.apache.turbine.util.RunData;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.context.Context;

import com.aimluck.eip.cayenne.om.portlet.EipMGpdbKubun;
import com.aimluck.eip.cayenne.om.portlet.EipMGpdbKubunValue;
import com.aimluck.eip.cayenne.om.portlet.EipTGpdb;
import com.aimluck.eip.cayenne.om.portlet.EipTGpdbItem;
import com.aimluck.eip.cayenne.om.portlet.EipTGpdbRecord;
import com.aimluck.eip.cayenne.om.portlet.EipTGpdbRecordFile;
import com.aimluck.eip.cayenne.om.security.TurbineUser;
import com.aimluck.eip.common.ALBaseUser;
import com.aimluck.eip.common.ALDBErrorException;
import com.aimluck.eip.common.ALEipConstants;
import com.aimluck.eip.common.ALEipUser;
import com.aimluck.eip.common.ALPageNotFoundException;
import com.aimluck.eip.fileupload.beans.FileuploadBean;
import com.aimluck.eip.fileupload.beans.FileuploadLiteBean;
import com.aimluck.eip.fileupload.util.FileuploadUtils;
import com.aimluck.eip.fileupload.util.FileuploadUtils.ShrinkImageSet;
import com.aimluck.eip.gpdb.GpdbItemResultData;
import com.aimluck.eip.gpdb.GpdbKubunResultData;
import com.aimluck.eip.gpdb.GpdbKubunValueResultData;
import com.aimluck.eip.gpdb.GpdbRecordResultData;
import com.aimluck.eip.gpdb.GpdbResultData;
import com.aimluck.eip.mail.ALAdminMailContext;
import com.aimluck.eip.mail.ALAdminMailMessage;
import com.aimluck.eip.mail.ALMailService;
import com.aimluck.eip.mail.util.ALEipUserAddr;
import com.aimluck.eip.mail.util.ALMailUtils;
import com.aimluck.eip.orm.Database;
import com.aimluck.eip.orm.query.SQLTemplate;
import com.aimluck.eip.orm.query.SelectQuery;
import com.aimluck.eip.services.orgutils.ALOrgUtilsService;
import com.aimluck.eip.services.storage.ALStorageService;
import com.aimluck.eip.util.ALEipUtils;
import com.aimluck.eip.util.ALLocalizationUtils;

/**
 * Webデータベースのユーティリティクラスです。
 *
 */
public class GpdbUtils {

  /** logger */
  private static final JetspeedLogger logger = JetspeedLogFactoryService
    .getLogger(GpdbUtils.class.getName());

  /** 項目定義名デフォルト値 */
  public static final String ITEM_NAME_DEFAULT = "タイトル";

  /** 項目定義形式：テキスト */
  public static final String ITEM_TYPE_TEXT = "01";

  /** 項目定義形式：テキストエリア */
  public static final String ITEM_TYPE_TEXTAREA = "02";

  /** 項目定義形式：リンク */
  public static final String ITEM_TYPE_LINK = "03";

  /** 項目定義形式：選択式（択一） */
  public static final String ITEM_TYPE_SELECT = "04";

  /** 項目定義形式：選択式（複数） */
  public static final String ITEM_TYPE_SELECT_MULTI = "05";

  /** 項目定義形式：ファイル */
  public static final String ITEM_TYPE_FILE = "06";

  /** 項目定義形式：画像 */
  public static final String ITEM_TYPE_IMAGE = "07";

  /** 項目定義形式：自動採番 */
  public static final String ITEM_TYPE_SEQ = "08";

  /** 項目定義形式：メール */
  public static final String ITEM_TYPE_MAIL = "09";

  /** 項目定義形式：日付 */
  public static final String ITEM_TYPE_DATE = "10";

  /** 項目定義形式：作成日 */
  public static final String ITEM_TYPE_CREATE_DATE = "11";

  /** 項目定義形式：更新日時 */
  public static final String ITEM_TYPE_UPDATE_DATE = "12";

  /** 項目定義形式：登録者 */
  public static final String ITEM_TYPE_CREATE_USER = "13";

  /** 項目定義形式：更新者 */
  public static final String ITEM_TYPE_UPDATE_USER = "14";

  /** 項目定義形式名：テキスト */
  public static final String ITEM_TYPE_TITLE_TEXT = "テキスト";

  /** 項目定義形式名：テキストエリア */
  public static final String ITEM_TYPE_TITLE_TEXTAREA = "テキストエリア";

  /** 項目定義形式名：リンク */
  public static final String ITEM_TYPE_TITLE_LINK = "リンク";

  /** 項目定義形式名：選択式（択一） */
  public static final String ITEM_TYPE_TITLE_SELECT = "選択式（択一）";

  /** 項目定義形式名：選択式（複数） */
  public static final String ITEM_TYPE_TITLE_SELECT_MULTI = "選択式（複数）";

  /** 項目定義形式名：ファイル */
  public static final String ITEM_TYPE_TITLE_FILE = "ファイル";

  /** 項目定義形式名：画像 */
  public static final String ITEM_TYPE_TITLE_IMAGE = "画像";

  /** 項目定義形式名：自動採番 */
  public static final String ITEM_TYPE_TITLE_SEQ = "自動採番";

  /** 項目定義形式名：メール */
  public static final String ITEM_TYPE_TITLE_MAIL = "メール";

  /** 項目定義形式名：日付 */
  public static final String ITEM_TYPE_TITLE_DATE = "日付";

  /** 項目定義形式名：作成日 */
  public static final String ITEM_TYPE_TITLE_CREATE_DATE = "作成日";

  /** 項目定義形式名：更新日時 */
  public static final String ITEM_TYPE_TITLE_UPDATE_DATE = "更新日時";

  /** 項目定義形式名：登録者 */
  public static final String ITEM_TYPE_TITLE_CREATE_USER = "登録者";

  /** 項目定義形式名：更新者 */
  public static final String ITEM_TYPE_TITLE_UPDATE_USER = "更新者";

  /** エラーメッセージ：タイトル項目削除不可エラー */
  public static final String ERRMSG_TITLE_ITEM_DELETE = "タイトル指定された項目は削除できません。";

  /** エラーメッセージ：データが存在する汎用DB削除不可エラー */
  public static final String ERRMSG_DB_DELETE = "データが登録されているため、削除できません。";

  /** ソート形式 */
  public static final String SORT_STRING = "sort";

  /** 検索キーワード */
  private static final String SEARCH_WORD = "sword";

  /** パラメータリセットの識別子 */
  private static final String RESET_KEYWORD_FLAG = "reset_keyword_params";

  /** 入力形式のマップ */
  @SuppressWarnings("serial")
  public static final Map<String, String> ITEM_TYPE = Collections
    .unmodifiableMap(new LinkedHashMap<String, String>() {
      {
        // この順番でリスト表示されます。
        put(ITEM_TYPE_TEXT, ITEM_TYPE_TITLE_TEXT); // テキスト
        put(ITEM_TYPE_TEXTAREA, ITEM_TYPE_TITLE_TEXTAREA); // テキストエリア
        put(ITEM_TYPE_LINK, ITEM_TYPE_TITLE_LINK); // リンク
        put(ITEM_TYPE_SELECT, ITEM_TYPE_TITLE_SELECT); // 選択式（択一）
        put(ITEM_TYPE_SELECT_MULTI, ITEM_TYPE_TITLE_SELECT_MULTI); // 選択式（複数）
        put(ITEM_TYPE_FILE, ITEM_TYPE_TITLE_FILE); // ファイル
        put(ITEM_TYPE_IMAGE, ITEM_TYPE_TITLE_IMAGE); // 画像
        put(ITEM_TYPE_SEQ, ITEM_TYPE_TITLE_SEQ); // 自動採番
        put(ITEM_TYPE_MAIL, ITEM_TYPE_TITLE_MAIL); // メール
        put(ITEM_TYPE_DATE, ITEM_TYPE_TITLE_DATE); // 日付
        put(ITEM_TYPE_CREATE_USER, ITEM_TYPE_TITLE_CREATE_USER); // 登録者
        put(ITEM_TYPE_UPDATE_USER, ITEM_TYPE_TITLE_UPDATE_USER); // 更新者
        put(ITEM_TYPE_CREATE_DATE, ITEM_TYPE_TITLE_CREATE_DATE); // 作成日
        put(ITEM_TYPE_UPDATE_DATE, ITEM_TYPE_TITLE_UPDATE_DATE); // 更新日時
      }
    });

  /** 選択肢／区分マスタ選択を表示する入力形式 */
  @SuppressWarnings("serial")
  public static final List<String> DISP_FIELD_SELECT_ITEM = Collections
    .unmodifiableList(new ArrayList<String>() {
      {
        add(ITEM_TYPE_SELECT); // 選択式（択一）
        add(ITEM_TYPE_SELECT_MULTI); // 選択式（複数）
      }
    });

  /** 表示サイズ（横）を表示する入力形式 */
  @SuppressWarnings("serial")
  public static final List<String> DISP_FIELD_SIZE_COL = Collections
    .unmodifiableList(new ArrayList<String>() {
      {
        add(ITEM_TYPE_TEXT); // テキスト
        add(ITEM_TYPE_TEXTAREA); // テキストエリア
        add(ITEM_TYPE_LINK); // リンク
        add(ITEM_TYPE_MAIL); // メール
      }
    });

  /** 表示サイズ（横）を表示する入力形式 */
  @SuppressWarnings("serial")
  public static final List<String> DISP_FIELD_SIZE_ROW = Collections
    .unmodifiableList(new ArrayList<String>() {
      {
        add(ITEM_TYPE_TEXTAREA); // テキストエリア
      }
    });

  /** 表示行数を表示する入力形式 */
  @SuppressWarnings("serial")
  public static final List<String> DISP_FIELD_LINE = Collections
    .unmodifiableList(new ArrayList<String>() {
      {
        add(ITEM_TYPE_SELECT_MULTI); // 選択式（複数）
      }
    });

  /** 必須フラグを表示する入力形式 */
  @SuppressWarnings("serial")
  public static final List<String> DISP_FIELD_REQUIRED = Collections
    .unmodifiableList(new ArrayList<String>() {
      {
        add(ITEM_TYPE_TEXT); // テキスト
        add(ITEM_TYPE_TEXTAREA); // テキストエリア
        add(ITEM_TYPE_LINK); // リンク
        add(ITEM_TYPE_SELECT); // 選択式（択一）
        add(ITEM_TYPE_SELECT_MULTI); // 選択式（複数）
        add(ITEM_TYPE_FILE); // ファイル
        add(ITEM_TYPE_IMAGE); // 画像
        add(ITEM_TYPE_MAIL); // メール
        add(ITEM_TYPE_DATE); // 日付
      }
    });

  /** 入力項目を表示する入力形式マップ */
  @SuppressWarnings("serial")
  public static final Map<String, List<String>> ITEM_FIELD = Collections
    .unmodifiableMap(new TreeMap<String, List<String>>() {
      {
        put("select_kubun", DISP_FIELD_SELECT_ITEM); // 区分マスタ選択／選択肢入力
        put("size_col", DISP_FIELD_SIZE_COL); // 表示サイズ（横）
        put("size_row", DISP_FIELD_SIZE_ROW); // 表示サイズ（縦）
        put("line", DISP_FIELD_LINE); // 表示行数
        put("required", DISP_FIELD_REQUIRED); // 必須フラグ
      }
    });

  /** フラグ：ON */
  public static final String FLG_ON = "t";

  /** フラグ：OFF */
  public static final String FLG_OFF = "f";

  /** サイズ（横）デフォルト値 */
  public static final int SIZE_COL_DEFAULT = 300;

  /** 区分値の区切り文字（データ保持用） */
  public static final String KUBUN_VALUE_SEPARATOR = "|";

  /** 区分値の区切り文字（画面表示用） */
  public static final String KUBUN_VALUE_DISP_SEPARATOR = "、";

  /** 添付ファイルを保管するディレクトリの指定 */
  private static final String FOLDER_FILEDIR_GPDB = JetspeedResources
    .getString("aipo.filedir", "");

  /** 添付ファイルを保管するディレクトリのカテゴリキーの指定 */
  protected static final String CATEGORY_KEY = JetspeedResources.getString(
    "aipo.gpdb.categorykey",
    "");;

  /** デフォルトエンコーディングを表わすシステムプロパティのキー */
  public static final String FILE_ENCODING = JetspeedResources.getString(
    "content.defaultencoding",
    "UTF-8");

  /** ポートレット名 */
  public static final String GPDB_PORTLET_NAME = "Gpdb";

  // ---------------------------------------------------
  // 入力形式
  // ---------------------------------------------------

  /**
   * 入力形式名を返す
   *
   * @param type
   *          入力形式
   * @return 入力形式名
   */
  public static String getItemTypeName(String type) {
    return ITEM_TYPE.get(type);
  }

  // ---------------------------------------------------
  // Webデータベース取得
  // ---------------------------------------------------

  /**
   * Webデータベース オブジェクトモデルを取得します。
   *
   * @param rundata
   *          RunData
   * @param context
   *          Context
   * @return Webデータベースオブジェクトモデル
   */
  public static EipTGpdb getEipTGpdb(RunData rundata, Context context) {
    String gpdbid =
      ALEipUtils.getTemp(rundata, context, ALEipConstants.ENTITY_ID);
    return getEipTGpdb(gpdbid);
  }

  /**
   * Webデータベースオブジェクトモデルを取得します。
   *
   * @param gpdbId
   *          WebデータベースID
   * @return Webデータベース オブジェクトモデル
   */
  public static EipTGpdb getEipTGpdb(String gpdbId) {

    try {
      if (gpdbId == null || Integer.valueOf(gpdbId) == null) {
        // Request IDが空の場合
        logger.debug("[GpdbUtils] Empty ID...");
        return null;
      }

      SelectQuery<EipTGpdb> query = Database.query(EipTGpdb.class);
      query.setQualifier(ExpressionFactory.matchDbExp(
        EipTGpdb.GPDB_ID_PK_COLUMN,
        gpdbId));

      List<EipTGpdb> gpdbList = query.fetchList();

      if (gpdbList == null || gpdbList.isEmpty()) {
        // 指定したWebデータベースIDのレコードが見つからない場合
        logger.debug("[GpdbUtils] Not found ID...");
        return null;
      }
      return gpdbList.get(0);

    } catch (Exception ex) {
      logger.error("Exception", ex);
      return null;
    }
  }

  /**
   * 全Webデータベースを返す
   *
   * @return Webデータベースオブジェクト全リスト
   */
  public static List<GpdbResultData> getGpdbAllList() {
    List<GpdbResultData> gpdbAllList = new ArrayList<GpdbResultData>();

    try {
      List<EipTGpdb> gpdbList =
        Database.query(EipTGpdb.class).orderAscending(
          EipTGpdb.GPDB_NAME_PROPERTY).fetchList();

      for (EipTGpdb gpdb : gpdbList) {
        GpdbResultData data = new GpdbResultData();
        data.initField();
        data.setGpdbId(gpdb.getGpdbId());
        data.setGpdbName(gpdb.getGpdbName());
        gpdbAllList.add(data);
      }
    } catch (Exception ex) {
      logger.error("Exception", ex);
    }

    return gpdbAllList;
  }

  /**
   * WebデータベースオブジェクトよりWebデータベースResult情報を返す
   *
   * @param model
   *          Webデータベースオブジェクト
   * @return WebデータベースResult情報
   */
  public static GpdbResultData getGpdbResultData(EipTGpdb model) {

    // 登録者
    TurbineUser user = model.getTurbineUser();

    GpdbResultData data = new GpdbResultData();
    data.initField();
    data.setGpdbId(model.getGpdbId()); // WebデータベースID
    data.setGpdbName(model.getGpdbName()); // Webデータベース名
    data.setMailFlg(model.getMailFlg()); // メール配信フラグ

    data.setCreateUserName(new StringBuffer()
      .append(user.getLastName())
      .append(" ")
      .append(user.getFirstName())
      .toString()); // 登録者名
    data.setCreateUserId(user.getUserId());
    data.setCreateDate(model.getCreateDate());// 作成日
    data.setUpdateDate(model.getUpdateDate());// 更新日

    data.setRecordCnt(getRecordCount("" + model.getGpdbId())); // 登録データ件数

    return data;
  }

  // ---------------------------------------------------
  // 項目定義取得
  // ---------------------------------------------------

  /**
   * 項目定義 オブジェクトモデルを取得します。
   *
   * @param rundata
   *          RunData
   * @param context
   *          Context
   * @return 項目定義オブジェクトモデル
   */
  public static EipTGpdbItem getEipTGpdbItem(RunData rundata, Context context) {
    String gpdbItemId =
      ALEipUtils.getTemp(rundata, context, ALEipConstants.ENTITY_ID);
    return getEipTGpdbItem(gpdbItemId);
  }

  /**
   * 項目定義 オブジェクトモデルを取得します。
   *
   * @param gpdbItemId
   *          項目定義ID
   * @return 項目定義オブジェクトモデル
   */
  public static EipTGpdbItem getEipTGpdbItem(String gpdbItemId) {

    try {
      if (gpdbItemId == null) {
        // Request IDが空の場合
        logger.debug("[GpdbUtils] Empty ID...");
        return null;
      }

      SelectQuery<EipTGpdbItem> query = Database.query(EipTGpdbItem.class);
      query.setQualifier(ExpressionFactory.matchDbExp(
        EipTGpdbItem.GPDB_ITEM_ID_PK_COLUMN,
        gpdbItemId));

      List<EipTGpdbItem> gpdbItemList = query.fetchList();

      if (gpdbItemList == null || gpdbItemList.isEmpty()) {
        // 指定した項目定義IDのレコードが見つからない場合
        logger.debug("[GpdbUtils] Not found ID...");
        return null;
      }
      return gpdbItemList.get(0);

    } catch (Exception ex) {
      logger.error("Exception", ex);
      return null;
    }
  }

  /**
   * 項目定義オブジェクトリストより項目定義Result情報リストを返す
   *
   * @param gpdbItemList
   *          項目定義オブジェクトリスト
   * @return 項目定義Result情報リスト
   */
  public static List<GpdbItemResultData> getGpdbItemResultList(
      List<EipTGpdbItem> gpdbItemList) {
    List<GpdbItemResultData> resultList = new ArrayList<GpdbItemResultData>();
    for (EipTGpdbItem gpdbItem : gpdbItemList) {
      resultList.add(getGpdbItemResultData(gpdbItem));
    }
    return resultList;
  }

  /**
   * 項目定義オブジェクトより項目定義Result情報を返す
   *
   * @param gpdbItem
   *          項目定義オブジェクトリスト
   * @return 項目定義Result情報
   */
  public static GpdbItemResultData getGpdbItemResultData(EipTGpdbItem gpdbItem) {
    GpdbItemResultData data = new GpdbItemResultData();
    data.initField();
    data.setGpdbItemId(gpdbItem.getGpdbItemId()); // 項目定義ID
    data.setGpdbItemName(gpdbItem.getGpdbItemName()); // 項目定義名
    data.setGpdbId(gpdbItem.getGpdb().getGpdbId()); // WebデータベースID
    data.setGpdbName(gpdbItem.getGpdb().getGpdbName()); // Webデータベース名
    data.setTitleFlg(gpdbItem.getTitleFlg()); // タイトルフラグ
    data.setRequiredFlg(gpdbItem.getRequiredFlg()); // 必須フラグ
    data.setType(gpdbItem.getType()); // 入力形式
    data.setTypeName(ITEM_TYPE.get(gpdbItem.getType())); // 入力形式名
    // 区分選択肢リスト
    data.setListFlg(gpdbItem.getListFlg()); // 一覧画面表示フラグ
    data.setDetailFlg(gpdbItem.getDetailFlg()); // 詳細画面表示フラグ
    data.setDefaultSortFlg(gpdbItem.getDefaultSortFlg()); // デフォルトソートフラグ
    data.setAscDesc(gpdbItem.getAscDesc()); // ソート順

    // 区分マスタ
    EipMGpdbKubun kubun = getEipMGpdbKubun(gpdbItem.getGpdbKubunId());
    if (kubun != null) {
      data.setGpdbKubunId(kubun.getGpdbKubunId());
      data.setGpdbKubunName(kubun.getGpdbKubunName());
    }

    Integer sizeCol = gpdbItem.getSizeCol(); // 表示サイズ（横）
    if (sizeCol != null) {
      data.setSizeCol(sizeCol);
    }
    Integer sizeRow = gpdbItem.getSizeRow(); // 表示サイズ（縦）
    if (sizeRow != null) {
      data.setSizeRow(sizeRow);
    }
    Integer line = gpdbItem.getLine(); // 表示行数
    if (line != null) {
      data.setLine(line);
    }
    return data;
  }

  /**
   * 項目定義Result情報リストを返す
   *
   * @param gpdbId
   *          WebデータベースID
   * @return 項目定義Result情報リスト
   */
  public static List<GpdbItemResultData> getGpdbItemResultList(String gpdbId) {

    SelectQuery<EipTGpdbItem> query = Database.query(EipTGpdbItem.class);
    query.setQualifier(ExpressionFactory.matchDbExp(
      EipTGpdbItem.GPDB_PROPERTY,
      gpdbId));
    query.orderAscending(EipTGpdbItem.ORDER_NO_PROPERTY);

    List<GpdbItemResultData> gpdbItemList =
      getGpdbItemResultList(query.fetchList());
    if (gpdbItemList.isEmpty()) {
      // 指定した項目定義IDのレコードが見つからない場合
      logger.debug("[GpdbUtils] Not found ID...");
      return null;
    }
    return gpdbItemList;
  }

  // ---------------------------------------------------
  // Webデータベースレコード取得
  // ---------------------------------------------------

  /**
   * Webデータベースレコード オブジェクトリストを取得します。 <br/>
   * 1レコード分のリストを取得します。
   *
   * @param gpdbId
   *          WebデータベースID
   * @return レコードオブジェクトモデル
   */
  public static List<EipTGpdbRecord> getEipTGpdbRecord(String gpdbId) {
    return getEipTGpdbRecord(gpdbId, null);
  }

  /**
   * Webデータベースレコード オブジェクトリストを取得します。 <br/>
   * タイトル項目のリストを取得します。
   *
   * @param gpdbId
   *          WebデータベースID
   * @return レコードオブジェクトモデル
   */
  public static List<EipTGpdbRecord> getEipTGpdbRecordTitle(String gpdbId) {
    return getEipTGpdbRecord(gpdbId, null, true);
  }

  /**
   * Webデータベースレコード オブジェクトリストを取得します。 <br/>
   * 1レコード分のリストを取得します。
   *
   * @param gpdbId
   *          WebデータベースID
   * @param recordNo
   *          レコードNo
   * @return レコードオブジェクトモデル
   */
  public static List<EipTGpdbRecord> getEipTGpdbRecord(String gpdbId,
      String recordNo) {
    return getEipTGpdbRecord(gpdbId, recordNo, false);
  }

  /**
   * Webデータベースレコード オブジェクトリストを取得します。 <br/>
   * 1レコード分のリストを取得します。
   *
   * @param gpdbId
   *          WebデータベースID
   * @param recordNo
   *          レコードNo
   * @param titleOnly
   *          TRUE:タイトルの項目のみを取得する
   * @return レコードオブジェクトモデル
   */
  public static List<EipTGpdbRecord> getEipTGpdbRecord(String gpdbId,
      String recordNo, boolean titleOnly) {

    try {
      if (gpdbId == null) {
        // Request IDが空の場合
        logger.debug("[GpdbUtils] Empty ID...");
        return null;
      }

      SelectQuery<EipTGpdbRecord> query = Database.query(EipTGpdbRecord.class);
      query.setQualifier(ExpressionFactory.matchDbExp(
        EipTGpdbRecord.GPDB_PROPERTY,
        gpdbId));

      if (recordNo != null) {
        query.andQualifier(ExpressionFactory.matchExp(
          EipTGpdbRecord.RECORD_NO_PROPERTY,
          recordNo));
      }
      if (titleOnly) {
        SelectQuery<EipTGpdbItem> titleQuery =
          Database.query(EipTGpdbItem.class);
        titleQuery.setQualifier(ExpressionFactory.matchDbExp(
          EipTGpdbItem.GPDB_PROPERTY,
          gpdbId));
        titleQuery.andQualifier(ExpressionFactory.matchExp(
          EipTGpdbItem.TITLE_FLG_PROPERTY,
          FLG_ON));

        query.andQualifier(ExpressionFactory.matchExp(
          EipTGpdbRecord.GPDB_ITEM_PROPERTY,
          titleQuery.fetchSingle()));
      }

      List<EipTGpdbRecord> gpdbRecordList = query.fetchList();

      if (gpdbRecordList == null || gpdbRecordList.isEmpty()) {
        // 指定したレコードNoのレコードが見つからない場合
        logger.debug("[GpdbUtils] Not found ID...");
        return null;
      }
      return gpdbRecordList;

    } catch (Exception ex) {
      logger.error("Exception", ex);
      return null;
    }
  }

  /**
   * Webデータベースレコード オブジェクトリストを取得します。 <br/>
   * 1レコード分のリストを取得します。
   *
   * @param gpdbId
   *          項目定義ID
   * @param recordNo
   *          レコードNo
   * @param itemId
   *          項目定義ID
   * @return レコードオブジェクトモデル
   */
  public static EipTGpdbRecord getEipTGpdbRecord(String gpdbId,
      String recordNo, String itemId) {

    try {
      if (gpdbId == null || recordNo == null) {
        // Request IDが空の場合
        logger.debug("[GpdbUtils] Empty ID...");
        return null;
      }

      SelectQuery<EipTGpdbRecord> query = Database.query(EipTGpdbRecord.class);
      query.setQualifier(ExpressionFactory.matchDbExp(
        EipTGpdbRecord.GPDB_PROPERTY,
        gpdbId));
      query.andQualifier(ExpressionFactory.matchExp(
        EipTGpdbRecord.RECORD_NO_PROPERTY,
        recordNo));
      query.andQualifier(ExpressionFactory.matchExp(
        EipTGpdbRecord.GPDB_ITEM_PROPERTY,
        itemId));

      EipTGpdbRecord gpdbRecord = query.fetchSingle();

      if (gpdbRecord == null) {
        // 指定した項目定義IDのレコードが見つからない場合
        logger.debug("[GpdbUtils] Not found ID...");
        return null;
      }
      return gpdbRecord;

    } catch (Exception ex) {
      logger.error("Exception", ex);
      return null;
    }
  }

  /**
   * WebデータベースレコードオブジェクトよりWebデータベースレコードResult情報を返す
   *
   * @param model
   *          Webデータベースレコードオブジェクト
   * @return WebデータベースレコードResult情報
   */
  public static GpdbRecordResultData getGpdbRecordResultData(
      EipTGpdbRecord model) {

    try {
      String value = model.getValue();
      Integer userid = null;
      List<String> valueList = new ArrayList<String>();
      if (value != null) {

        EipTGpdbItem item = getEipTGpdbItem("" + model.getGpdbItemId());
        String type = item.getType();

        if (ITEM_TYPE_SELECT.equals(type)
          || ITEM_TYPE_SELECT_MULTI.equals(type)) {
          // 選択式（複数）の場合、区分値IDを区分値に変換し、区切り文字で値を結合する
          String[] list = value.split("\\" + KUBUN_VALUE_SEPARATOR);

          StringBuilder sbVal = new StringBuilder();
          for (int i = 0; i < list.length; i++) {
            if (i > 0) {
              sbVal.append(KUBUN_VALUE_DISP_SEPARATOR);
            }
            EipMGpdbKubunValue kubunValue = getEipMGpdbKubunValue(list[i]);
            if (kubunValue != null) {
              sbVal.append(kubunValue.getGpdbKubunValue());
            }
            valueList.add(list[i]);
          }

          value = String.valueOf(sbVal);

        } else if (ITEM_TYPE_FILE.equals(type) || ITEM_TYPE_IMAGE.equals(type)) {
          // ファイル、画像の場合、区切り文字で値を結合する
          value =
            value.replace(KUBUN_VALUE_SEPARATOR, KUBUN_VALUE_DISP_SEPARATOR);

        } else if (ITEM_TYPE_CREATE_USER.equals(type)
          || ITEM_TYPE_UPDATE_USER.equals(type)) {
          // 登録者、更新者の場合、名称・ユーザーIDをセットする
          if (!"".equals(value.trim())) {
            userid = Integer.valueOf(value);
            value = ALEipUtils.getALEipUser(userid).getAliasName().getValue();
          }
        } else if (ITEM_TYPE_DATE.equals(type)) {
          // 日付の場合、フォーマットを変換する。
          SimpleDateFormat sdf_data =
            new SimpleDateFormat(ALLocalizationUtils
              .getl10n("GPDB_DATE_FORMAT_DATA"));
          SimpleDateFormat sdf_show =
            new SimpleDateFormat(ALLocalizationUtils
              .getl10n("GPDB_DATE_FORMAT_SHOW"));
          String tmpValue = value;
          try {
            Date date = sdf_data.parse(value);
            value = sdf_show.format(date);
          } catch (Exception e) {
            value = tmpValue;
          }
        } else if (ITEM_TYPE_CREATE_DATE.equals(type)
          || ITEM_TYPE_UPDATE_DATE.equals(type)) {
          // 作成日、更新日の場合、フォーマットを変換する。
          SimpleDateFormat sdf_data =
            new SimpleDateFormat(ALLocalizationUtils
              .getl10n("GPDB_TIMESTAMP_FORMAT_DATA"));
          SimpleDateFormat sdf_show =
            new SimpleDateFormat(ALLocalizationUtils
              .getl10n("GPDB_TIMESTAMP_FORMAT_SHOW"));
          String tmpValue = value;
          try {
            Date date = sdf_data.parse(value);
            value = sdf_show.format(date);
          } catch (Exception e) {
            value = tmpValue;
          }
        }
      }

      GpdbRecordResultData rd = new GpdbRecordResultData();
      rd.initField();
      rd.setGpdbItemId(model.getGpdbItemId());
      rd.setGpdbRecordId(model.getGpdbRecordId());
      rd.setRecordNo(model.getRecordNo());
      rd.setValue(model.getValue());
      rd.setDispValue(value);
      rd.setValueList(valueList);
      if (userid != null) {
        rd.setUserId(userid);
      }
      return rd;
    } catch (RuntimeException ex) {
      logger.error("RuntimeException", ex);
      return null;
    } catch (Exception ex) {
      logger.error("Exception", ex);
      return null;
    }
  }

  /**
   * Webデータベースレコードのマップを返す
   *
   * @param gpdbId
   *          WebデータベースID
   * @param recordNo
   *          レコードNo
   * @return Webデータベースレコードのマップ
   * @throws ALPageNotFoundException
   */
  public static Map<String, GpdbRecordResultData> getGpdbRecordMap(
      String gpdbId, String recordNo) throws ALPageNotFoundException {
    Map<String, GpdbRecordResultData> map =
      new HashMap<String, GpdbRecordResultData>();

    List<EipTGpdbRecord> recordList = getEipTGpdbRecord(gpdbId, recordNo);

    // 指定したgpdbIDのレコードが見つからない場合
    if (recordList == null || recordList.size() == 0) {
      logger.error("[GpdbUtils] Not found record.");
      throw new ALPageNotFoundException();
    }

    List<GpdbRecordResultData> list = new ArrayList<GpdbRecordResultData>();
    for (EipTGpdbRecord rec : recordList) {
      list.add(getGpdbRecordResultData(rec));
    }

    // 項目定義IDをキーにWebデータベースレコード情報を保持する
    for (GpdbRecordResultData data : list) {
      map.put("" + data.getGpdbItemId(), data);
    }

    return map;
  }

  /**
   * Webデータベースレコードの登録件数を取得します
   *
   * @param gpdbId
   *          WebデータベースID
   * @return レコード登録件数
   */
  public static int getRecordCount(String gpdbId) {
    int cnt = 0;

    try {
      String sql =
        "SELECT COUNT(0) AS data_cnt"
          + " FROM ("
          + "   SELECT record_no FROM eip_t_gpdb_record"
          + "   WHERE gpdb_id = #bind($gpdb_id)"
          + "   GROUP BY record_no) a";

      SQLTemplate<EipTGpdbRecord> sqltemp =
        Database.sql(EipTGpdbRecord.class, String.valueOf(sql));
      sqltemp.param("gpdb_id", Integer.valueOf(gpdbId));

      List<DataRow> result = sqltemp.fetchListAsDataRow();

      if (result == null || result.isEmpty()) {
        return 0;
      }

      cnt = Integer.valueOf(result.get(0).get("data_cnt").toString());

    } catch (Exception ex) {
      logger.error("Exception", ex);
      return 0;
    }
    return cnt;
  }

  /**
   * 指定の区分値を使用しているWebデータベースレコードの登録件数を取得します
   *
   * @param gpdbKubunValueId
   *          区分値スID
   * @return レコード登録件数
   */
  public static int getRecordCountKubunUsed(String gpdbKubunValueId) {
    int cnt = 0;

    try {
      String sql = "";
      if (Database.isJdbcMySQL()) {
        sql =
          "SELECT COUNT(0) AS data_cnt"
            + " FROM eip_t_gpdb_record r"
            + "   INNER JOIN eip_t_gpdb_item i"
            + "     ON i.gpdb_item_id = r.gpdb_item_id"
            + "   WHERE i.type IN (#bind($type1), #bind($type2))"
            + "   AND r.value REGEXP #bind($gpdb_kubun_value_id)";
      } else if (Database.isJdbcPostgreSQL()) {
        sql =
          "SELECT COUNT(0) AS data_cnt"
            + " FROM eip_t_gpdb_record r"
            + "   INNER JOIN eip_t_gpdb_item i"
            + "     ON i.gpdb_item_id = r.gpdb_item_id"
            + "   WHERE i.type IN (#bind($type1), #bind($type2))"
            + "   AND r.value ~ #bind($gpdb_kubun_value_id)";
      }

      SQLTemplate<EipTGpdbRecord> sqltemp =
        Database.sql(EipTGpdbRecord.class, String.valueOf(sql));
      sqltemp.param("type1", ITEM_TYPE_SELECT);
      sqltemp.param("type2", ITEM_TYPE_SELECT_MULTI);

      if (Database.isJdbcMySQL()) {
        sqltemp.param("gpdb_kubun_value_id", "(^|\\|)"
          + gpdbKubunValueId
          + "(\\||$)");

      } else if (Database.isJdbcPostgreSQL()) {
        sqltemp.param("gpdb_kubun_value_id", "(?:^|\\||)"
          + gpdbKubunValueId
          + "(?:\\||$)");
      }

      List<DataRow> result = sqltemp.fetchListAsDataRow();

      if (result == null || result.isEmpty()) {
        return 0;
      }

      cnt = Integer.valueOf(result.get(0).get("data_cnt").toString());

    } catch (Exception ex) {
      logger.error("Exception", ex);
      return 0;
    }
    return cnt;
  }

  // ---------------------------------------------------
  // 区分取得
  // ---------------------------------------------------

  /**
   * 区分マスタ オブジェクトモデルを取得します。
   *
   * @param rundata
   *          RunData
   * @param context
   *          Context
   * @return 区分マスタオブジェクトモデル
   */
  public static EipMGpdbKubun getEipMGpdbKubun(RunData rundata, Context context) {
    String gpdbKubunId =
      ALEipUtils.getTemp(rundata, context, ALEipConstants.ENTITY_ID);
    try {
      if (gpdbKubunId == null) {
        // Request IDが空の場合
        logger.debug("[GpdbUtils] Empty ID...");
        return null;
      }

      SelectQuery<EipMGpdbKubun> query = Database.query(EipMGpdbKubun.class);
      query.setQualifier(ExpressionFactory.matchDbExp(
        EipMGpdbKubun.GPDB_KUBUN_ID_PK_COLUMN,
        gpdbKubunId));

      List<EipMGpdbKubun> kubunList = query.fetchList();

      if (kubunList == null || kubunList.isEmpty()) {
        // 指定した区分マスタIDのレコードが見つからない場合
        logger.debug("[GpdbUtils] Not found ID...");
        return null;
      }
      return kubunList.get(0);

    } catch (Exception ex) {
      logger.error("Exception", ex);
      return null;
    }
  }

  /**
   * 区分マスタ オブジェクトモデルを取得します。
   *
   * @param gpdbKubunId
   *          区分マスタID
   * @return 区分マスタオブジェクトモデル
   */
  public static EipMGpdbKubun getEipMGpdbKubun(Integer gpdbKubunId) {
    try {

      if (gpdbKubunId == null) {
        return null;
      }

      SelectQuery<EipMGpdbKubun> query = Database.query(EipMGpdbKubun.class);
      query.setQualifier(ExpressionFactory.matchDbExp(
        EipMGpdbKubun.GPDB_KUBUN_ID_PK_COLUMN,
        gpdbKubunId));

      List<EipMGpdbKubun> kubunList = query.fetchList();

      if (kubunList == null || kubunList.isEmpty()) {
        // 指定した区分マスタIDのレコードが見つからない場合
        logger.debug("[GpdbUtils] Not found ID...");
        return null;
      }
      return kubunList.get(0);

    } catch (Exception ex) {
      logger.error("Exception", ex);
      return null;
    }
  }

  /**
   * 区分値 オブジェクトモデルを取得します。
   *
   * @param rundata
   *          RunData
   * @param context
   *          Context
   * @return 区分値オブジェクトモデル
   */
  public static EipMGpdbKubunValue getEipMGpdbKubunValue(RunData rundata,
      Context context) {
    String gpdbKubunValueId =
      ALEipUtils.getTemp(rundata, context, ALEipConstants.ENTITY_ID);
    return getEipMGpdbKubunValue(gpdbKubunValueId);
  }

  /**
   * 区分値 オブジェクトモデルを取得します。
   *
   * @param gpdbKubunValueId
   *          区分値ID
   * @return 区分値オブジェクトモデル
   */
  public static EipMGpdbKubunValue getEipMGpdbKubunValue(String gpdbKubunValueId) {
    try {

      if (gpdbKubunValueId == null || gpdbKubunValueId.isEmpty()) {
        // Request IDが空の場合
        logger.debug("[GpdbUtils] Empty ID...");
        return null;
      }

      SelectQuery<EipMGpdbKubunValue> query =
        Database.query(EipMGpdbKubunValue.class);
      query.setQualifier(ExpressionFactory.matchDbExp(
        EipMGpdbKubunValue.GPDB_KUBUN_VALUE_ID_PK_COLUMN,
        gpdbKubunValueId));

      List<EipMGpdbKubunValue> kubunValueList = query.fetchList();

      if (kubunValueList == null || kubunValueList.isEmpty()) {
        // 指定した区分値IDのレコードが見つからない場合
        logger.debug("[GpdbUtils] Not found ID...");
        return null;
      }
      return kubunValueList.get(0);

    } catch (Exception ex) {
      logger.error("Exception", ex);
      return null;
    }
  }

  /**
   * 全区分のリストを返す
   *
   * @return 全区分リスト
   */
  public static List<GpdbKubunResultData> getGpdbKubunAllList() {
    List<GpdbKubunResultData> kubunAllList =
      new ArrayList<GpdbKubunResultData>();

    try {
      List<EipMGpdbKubun> kubunList =
        Database.query(EipMGpdbKubun.class).orderAscending(
          EipMGpdbKubun.GPDB_KUBUN_NAME_PROPERTY).fetchList();

      for (EipMGpdbKubun kubun : kubunList) {
        GpdbKubunResultData data = new GpdbKubunResultData();
        data.initField();
        data.setGpdbKubunId(kubun.getGpdbKubunId());
        data.setGpdbKubunName(kubun.getGpdbKubunName());
        kubunAllList.add(data);
      }
    } catch (Exception ex) {
      logger.error("Exception", ex);
    }

    return kubunAllList;
  }

  /**
   * 指定区分の区分値リストを返す
   *
   * @param gpdbKubunId
   *          区分ID
   * @return 全区分値リスト
   */
  public static List<GpdbKubunValueResultData> getGpdbKubunValueList(
      String gpdbKubunId) {
    List<GpdbKubunValueResultData> kubunValueResultList =
      new ArrayList<GpdbKubunValueResultData>();

    try {

      SelectQuery<EipMGpdbKubunValue> query =
        Database.query(EipMGpdbKubunValue.class);
      query.orderAscending(EipMGpdbKubunValue.GPDB_KUBUN_PROPERTY);
      query.orderAscending(EipMGpdbKubunValue.ORDER_NO_PROPERTY);
      if (gpdbKubunId != null) {
        query.setQualifier(ExpressionFactory.matchDbExp(
          EipMGpdbKubunValue.GPDB_KUBUN_PROPERTY,
          gpdbKubunId));
      }

      List<EipMGpdbKubunValue> kubunValueList = query.fetchList();

      for (EipMGpdbKubunValue kubunValue : kubunValueList) {
        GpdbKubunValueResultData data = new GpdbKubunValueResultData();
        data.initField();
        data.setGpdbKubunValueId(kubunValue.getGpdbKubunValueId());
        data.setGpdbKubunValue(kubunValue.getGpdbKubunValue());
        data.setGpdbKubunId(kubunValue.getGpdbKubun().getGpdbKubunId());
        data.setGpdbKubunName(kubunValue.getGpdbKubun().getGpdbKubunName());
        kubunValueResultList.add(data);
      }
    } catch (Exception ex) {
      logger.error("Exception", ex);
    }

    return kubunValueResultList;
  }

  /**
   * 全区分値のリストを返す
   *
   * @return 全区分値リスト
   */
  public static List<GpdbKubunValueResultData> getGpdbKubunValueAllList() {
    return getGpdbKubunValueList(null);
  }

  /**
   * 区分IDをキーとした全区分値のマップを返す
   *
   * @return 全区分値のマップ
   */
  public static Map<String, List<GpdbKubunValueResultData>> getKubunMap() {
    Map<String, List<GpdbKubunValueResultData>> map =
      new HashMap<String, List<GpdbKubunValueResultData>>();

    // 全区分値リストを取得
    List<GpdbKubunValueResultData> allList = getGpdbKubunValueAllList();

    // 区分ごとに格納する区分値リスト
    List<GpdbKubunValueResultData> list = null;

    // 比較用区分マスタID
    String compareKubunId = null;

    for (GpdbKubunValueResultData data : allList) {
      String kubunId = data.getGpdbKubunId().toString();
      if (!kubunId.equals(compareKubunId)) {
        // 区分マスタIDごとにリストをマップに格納
        list = new ArrayList<GpdbKubunValueResultData>();
        map.put(kubunId, list);
      }
      list.add(data);
      compareKubunId = kubunId;
    }

    return map;
  }

  /**
   * 区分値オブジェクトリストより区分値Result情報リストを返す
   *
   * @param gpdbKubunValueList
   *          区分値オブジェクトリスト
   * @return 区分値Result情報リスト
   */
  public static List<GpdbKubunValueResultData> getGpdbKubunValueResultList(
      List<EipMGpdbKubunValue> gpdbKubunValueList) {
    List<GpdbKubunValueResultData> resultList =
      new ArrayList<GpdbKubunValueResultData>();
    for (EipMGpdbKubunValue gpdbKubunValue : gpdbKubunValueList) {
      resultList.add(getGpdbKubunValueResultData(gpdbKubunValue));
    }
    return resultList;
  }

  /**
   * 区分値オブジェクトより区分値Result情報を返す
   *
   * @param model
   *          区分値オブジェクト
   * @return 区分値Result情報
   */
  public static GpdbKubunValueResultData getGpdbKubunValueResultData(
      EipMGpdbKubunValue model) {
    GpdbKubunValueResultData data = new GpdbKubunValueResultData();
    data.initField();
    data.setGpdbKubunValueId(model.getGpdbKubunValueId());
    data.setGpdbKubunName(model.getGpdbKubun().getGpdbKubunName());
    data.setGpdbKubunValue(model.getGpdbKubunValue());
    return data;
  }

  // ---------------------------------------------------
  // データベース操作
  // ---------------------------------------------------

  /**
   * Webデータベースに紐づく項目定義を削除する
   *
   * @param gpdb
   *          Webデータベースオブジェクト
   * @return TRUE 成功 FALSE 失敗
   */
  public static boolean removeGpdbItem(EipTGpdb gpdb) {

    try {
      // Webデータベースに紐づく項目定義
      SelectQuery<EipTGpdbItem> query = Database.query(EipTGpdbItem.class);
      query.setQualifier(ExpressionFactory.matchExp(
        EipTGpdbItem.GPDB_PROPERTY,
        gpdb.getGpdbId()));

      List<EipTGpdbItem> gpdbItemList = query.fetchList();

      if (gpdbItemList == null || gpdbItemList.isEmpty()) {
        return true;
      }

      // Webデータベースに紐づく項目定義を削除
      Database.deleteAll(gpdbItemList);

    } catch (Exception ex) {
      logger.error("Exception", ex);
      return false;
    }

    return true;
  }

  /**
   * 項目定義に紐づくレコードを削除する
   *
   * @param rundata
   *          RunData
   * @param gpdbItem
   *          項目定義オブジェクト
   * @return TRUE 成功 FALSE 失敗
   */
  public static boolean removeGpdbRecord(RunData rundata, EipTGpdbItem gpdbItem) {

    try {
      // 項目定義に紐づくレコード
      SelectQuery<EipTGpdbRecord> query = Database.query(EipTGpdbRecord.class);
      query.setQualifier(ExpressionFactory.matchExp(
        EipTGpdbRecord.GPDB_ITEM_PROPERTY,
        gpdbItem.getGpdbItemId()));

      List<EipTGpdbRecord> gpdbRecordList = query.fetchList();
      if (gpdbRecordList == null || gpdbRecordList.isEmpty()) {
        return true;
      }

      // 項目定義に紐づくレコードを削除
      Database.deleteAll(gpdbRecordList);

      // 添付ファイルデータを削除
      removeGpdbRecordFile(rundata, gpdbRecordList);

    } catch (Exception ex) {
      logger.error("Exception", ex);
      return false;
    }

    return true;
  }

  /**
   * 添付ファイルを削除する<br/>
   * DBのデータとともに実際のファイルも削除
   *
   * @param rundata
   *          RunData
   * @param gpdbRecordList
   *          Webデータベースレコードリスト
   * @return TRUE 成功 FALSE 失敗
   */
  public static boolean removeGpdbRecordFile(RunData rundata,
      List<EipTGpdbRecord> gpdbRecordList) {

    try {
      // 添付ファイルデータを削除
      List<String> fpaths = new ArrayList<String>();
      List<Integer> fownerIds = new ArrayList<Integer>();
      if (gpdbRecordList != null) {
        for (EipTGpdbRecord record : gpdbRecordList) {
          List<EipTGpdbRecordFile> fileList =
            getEipTGpdbRecordFileList(record.getGpdbRecordId());
          Database.deleteAll(fileList);

          for (EipTGpdbRecordFile file : fileList) {
            if (file.getFilePath() != null && file.getOwnerId() != null) {
              fpaths.add(file.getFilePath());
              fownerIds.add(file.getOwnerId());
            }
          }
        }
      }

      if (!fpaths.isEmpty()) {
        // 保存されている添付ファイルを削除する
        int fsize = fpaths.size();
        for (int i = 0; i < fsize; i++) {
          ALStorageService.deleteFile(GpdbUtils
            .getSaveDirPath(fownerIds.get(i))
            + fpaths.get(i));
        }
      }

    } catch (Exception ex) {
      logger.error("Exception", ex);
      return false;
    }
    return true;
  }

  /**
   * 区分値を持たない区分を削除する
   *
   * @return TRUE 成功 FALSE 失敗
   */
  public static boolean removeGpdbKubunNoValue() {

    try {
      String sql =
        "SELECT * FROM eip_m_gpdb_kubun a"
          + " WHERE NOT EXISTS("
          + "   SELECT 0 FROM eip_m_gpdb_kubun_value b"
          + "   WHERE a.gpdb_kubun_id = b.gpdb_kubun_id)";

      List<EipMGpdbKubun> kubunList =
        Database.sql(EipMGpdbKubun.class, sql).fetchList();

      if (kubunList == null || kubunList.isEmpty()) {
        return true;
      }

      for (EipMGpdbKubun kubun : kubunList) {
        // 上記SQLで取得するとEipMGpdbKubunのステータスがTRANSIENTになり、
        // 削除などができなくなるためCOMMITTEDを設定
        DataContext.getThreadDataContext().registerNewObject(kubun);
        kubun.setPersistenceState(PersistenceState.COMMITTED);
      }

      Database.deleteAll(kubunList);

    } catch (Exception ex) {
      logger.error("Exception", ex);
      return false;
    }
    return true;
  }

  /**
   * 項目定義でタイトル指定されているものをOFFにする
   *
   * @param gpdb
   *          Webデータベースオブジェクト
   * @return TRUE 成功 FALSE 失敗
   */
  public static boolean gpdbItemTitleOff(EipTGpdb gpdb) {
    // 現在タイトル指定されているものをOFFにする
    SelectQuery<EipTGpdbItem> query = Database.query(EipTGpdbItem.class);
    query.setQualifier(ExpressionFactory.matchExp(
      EipTGpdbItem.GPDB_PROPERTY,
      gpdb));
    query.andQualifier(ExpressionFactory.matchExp(
      EipTGpdbItem.TITLE_FLG_PROPERTY,
      FLG_ON));

    EipTGpdbItem titleItem = query.fetchSingle();
    if (titleItem == null) {
      return true;
    }
    titleItem.setTitleFlg(FLG_OFF);

    return true;
  }

  /**
   * 項目定義でデフォルトソート指定されているものをOFFにする
   *
   * @param gpdb
   *          Webデータベースオブジェクト
   * @return TRUE 成功 FALSE 失敗
   */
  public static boolean gpdbItemDefaultSortOff(EipTGpdb gpdb) {
    // 現在タイトル指定されているものをOFFにする
    SelectQuery<EipTGpdbItem> query = Database.query(EipTGpdbItem.class);
    query.setQualifier(ExpressionFactory.matchExp(
      EipTGpdbItem.GPDB_PROPERTY,
      gpdb));
    query.andQualifier(ExpressionFactory.matchExp(
      EipTGpdbItem.DEFAULT_SORT_FLG_PROPERTY,
      FLG_ON));

    EipTGpdbItem titleItem = query.fetchSingle();
    if (titleItem == null) {
      return true;
    }
    titleItem.setDefaultSortFlg(FLG_OFF);
    titleItem.setAscDesc("");

    return true;
  }

  // ---------------------------------------------------
  // 変換系
  // ---------------------------------------------------

  /**
   * rundata.getParameters().getStrings()などで取得した文字列のエンコードを行う
   *
   * @param str
   *          変換前文字列
   * @return 変換後文字列
   */
  public static String convertEncording(String str) {
    try {
      String replace = new String(str.getBytes("8859_1"), "UTF-8");
      if (replace != null && replace.matches("^\\?+$")) {
        // 変換後文字列が全て?の場合は変換せずに返す
        // firefoxの場合にrundata.getParameters().getStrings()で文字化けなしで取得できるため
        // TODO 原因わかり次第対応
        return str;
      }
      return replace;
    } catch (Exception e) {
      return null;
    }
  }

  /**
   * 選択肢の入力値から区分値の登録値を取得する。
   *
   * @param rundata
   *          RunData
   * @param field
   *          フィールド
   * @return 区分選択肢文字列
   */
  public static String getParamValueString(RunData rundata, String field) {
    String[] param = rundata.getParameters().getStrings(field);
    if (param == null) {
      return null;
    }
    List<String> list = new ArrayList<String>();
    for (int i = 0; i < param.length; i++) {
      param[i] = convertEncording(param[i]);
      if (param[i] != null && !"".equals(param[i].trim())) {
        list.add(param[i]);
      }
    }
    return StringUtils.join(list.toArray(), KUBUN_VALUE_SEPARATOR);
  }

  // ---------------------------------------------------
  // チェックメソッド
  // ---------------------------------------------------

  /**
   * 入力項目が出力対象の入力形式かどうかを判定する
   *
   * @param field
   *          入力項目フィールド名
   * @param type
   *          入力形式
   * @return 出力する：TRUE 出力しない：FALSE
   */
  public static boolean dipslayField(String field, String type) {
    if (type == null || "".equals(type.trim())) {
      type = ITEM_TYPE_TEXT;
    }
    if (!ITEM_FIELD.containsKey(field)) {
      return false;
    }
    List<String> list = ITEM_FIELD.get(field);
    return list.contains(type);
  }

  /**
   * URL形式であるかを判定します。
   *
   * @param str
   *          チェック対象文字列
   * @return URL形式であればtrue、それ以外はfalse。
   */
  public static boolean isUrl(String str) {
    Pattern mailPattern =
      Pattern.compile(
        "^(https?|ftp)(:\\/\\/[-_.!~*\\'()a-zA-Z0-9;\\/?:\\@&=+\\$,%#]+)$",
        Pattern.CASE_INSENSITIVE);
    Matcher objMch = mailPattern.matcher(str);
    return objMch.matches();
  }

  // ---------------------------------------------------
  // メール
  // ---------------------------------------------------

  /**
   * メールを送信します
   *
   * @param rundata
   *          RunData
   * @param destUser
   *          送信先ユーザ
   * @param gpdb
   *          メッセージ
   * @param gpdbItemList
   * @return 成功：TRUE 失敗：FALSE
   * @throws Exception
   *           例外
   */
  public static boolean sendMail(RunData rundata, ALEipUser destUser,
      EipTGpdb gpdb, String gpdbItemName, String dispValue) throws Exception {

    String orgId = Database.getDomainName();
    String subject = "[" + ALOrgUtilsService.getAlias() + "]Webデータベース";

    try {
      List<ALEipUser> memberList = new ArrayList<ALEipUser>();
      memberList.add(destUser);

      List<ALEipUserAddr> destMemberList =
        ALMailUtils.getALEipUserAddrs(
          memberList,
          ALEipUtils.getUserId(rundata),
          false);

      List<ALAdminMailMessage> messageList =
        new ArrayList<ALAdminMailMessage>();

      for (ALEipUserAddr destMember : destMemberList) {
        ALAdminMailMessage message = new ALAdminMailMessage(destMember);
        message.setPcSubject(subject);
        message.setCellularSubject(subject);
        message.setPcBody(createMsgForPc(
          rundata,
          gpdb,
          gpdbItemName,
          dispValue,
          true));
        message.setCellularBody(createMsgForCellPhone(
          rundata,
          gpdb,
          gpdbItemName,
          dispValue,
          true));
        messageList.add(message);
      }

      ALMailService.sendAdminMailAsync(new ALAdminMailContext(orgId, ALEipUtils
        .getUserId(rundata), messageList, ALMailUtils
        .getSendDestType(ALMailUtils.KEY_MSGTYPE_WORKFLOW)));

    } catch (Exception ex) {
      logger.error("Exception", ex);
      return false;
    }
    return true;
  }

  /**
   * パソコンへ送信するメールの内容を作成する．
   *
   * @param gpdbItemList
   *
   * @return
   */
  public static String createMsgForPc(RunData rundata, EipTGpdb gpdb,
      String gpdbItemName, String dispValue, Boolean isNew)
      throws ALDBErrorException {
    VelocityContext context = new VelocityContext();
    boolean enableAsp = JetspeedResources.getBoolean("aipo.asp", false);
    ALEipUser loginUser = null;
    ALBaseUser user = null;

    try {
      loginUser = ALEipUtils.getALEipUser(rundata);
      user =
        (ALBaseUser) JetspeedSecurity.getUser(new UserIdPrincipal(loginUser
          .getUserId()
          .toString()));
    } catch (Exception e) {
      return "";
    }
    context.put("loginUser", loginUser.getAliasName().toString());
    context.put("hasEmail", !user.getEmail().equals(""));
    context.put("email", user.getEmail());
    context.put("isNew", isNew);
    // タイトル
    context.put("GpdbName", gpdb.getGpdbName());
    // 件名
    context.put("GpdbItemName", gpdbItemName);
    context.put("DispValue", dispValue);

    // サービス
    context.put("serviceAlias", ALOrgUtilsService.getAlias());
    // サービス（Aipo）へのアクセス
    context.put("enableAsp", enableAsp);
    context.put("globalurl", ALMailUtils.getGlobalurl());
    context.put("localurl", ALMailUtils.getLocalurl());
    CustomLocalizationService locService =
      (CustomLocalizationService) ServiceUtil
        .getServiceByName(LocalizationService.SERVICE_NAME);
    String lang = locService.getLocale(rundata).getLanguage();
    StringWriter writer = new StringWriter();
    try {
      if (lang != null && lang.equals("ja")) {
        Template template =
          Velocity.getTemplate("portlets/mail/"
            + lang
            + "/gpdb-notification-mail.vm", "utf-8");
        template.merge(context, writer);
      } else {
        Template template =
          Velocity.getTemplate(
            "portlets/mail/gpdb-notification-mail.vm",
            "utf-8");
        template.merge(context, writer);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    writer.flush();
    String ret = writer.getBuffer().toString();
    return ret;
  }

  /**
   * 携帯電話へ送信するメールの内容を作成する．
   *
   * @param gpdbItemList
   *
   * @return
   */
  public static String createMsgForCellPhone(RunData rundata, EipTGpdb gpdb,
      String gpdbItemName, String dispValue, Boolean isNew)
      throws ALDBErrorException {
    return createMsgForPc(rundata, gpdb, gpdbItemName, dispValue, isNew);
  }

  // ---------------------------------------------------
  // ファイル操作
  // ---------------------------------------------------

  /**
   * ファイルをアップロードする
   *
   * @param rundata
   *          RunData
   * @param context
   *          Context
   * @param record
   *          Webデータベースレコードオブジェクト
   * @param fileuploadList
   *          アップロードファイルリスト
   * @param folderName
   *          フォルダ名
   * @param msgList
   *          エラーメッセージリスト
   * @return 成功：TRUE 失敗：FALSE
   */
  public static boolean insertFileDataDelegate(RunData rundata,
      Context context, EipTGpdbRecord record,
      List<FileuploadLiteBean> fileuploadList, String folderName,
      List<String> msgList) {
    if (fileuploadList == null || fileuploadList.size() <= 0) {
      fileuploadList = new ArrayList<FileuploadLiteBean>();
    }

    int uid = ALEipUtils.getUserId(rundata);
    String orgId = Database.getDomainName();

    List<Integer> hadfileids = new ArrayList<Integer>();
    for (FileuploadLiteBean file : fileuploadList) {
      if (!file.isNewFile()) {
        hadfileids.add(file.getFileId());
      }
    }

    SelectQuery<EipTGpdbRecordFile> dbquery =
      Database.query(EipTGpdbRecordFile.class);
    dbquery.andQualifier(ExpressionFactory.matchDbExp(
      EipTGpdbRecordFile.EIP_TGPDB_RECORD_PROPERTY,
      record.getGpdbRecordId()));
    List<EipTGpdbRecordFile> existsFiles = dbquery.fetchList();
    List<EipTGpdbRecordFile> delFiles = new ArrayList<EipTGpdbRecordFile>();
    for (EipTGpdbRecordFile file : existsFiles) {
      if (!hadfileids.contains(file.getFileId())) {
        delFiles.add(file);
      }
    }

    // ローカルファイルに保存されているファイルを削除する．
    if (delFiles.size() > 0) {
      int delsize = delFiles.size();
      for (int i = 0; i < delsize; i++) {
        ALStorageService
          .deleteFile(getSaveDirPath(delFiles.get(i).getOwnerId())
            + (delFiles.get(i)).getFilePath());
      }
      // データベースから添付ファイルのデータ削除
      Database.deleteAll(delFiles);
    }

    // ファイル追加処理
    try {
      for (FileuploadLiteBean filebean : fileuploadList) {
        if (!filebean.isNewFile()) {
          continue;
        }

        // サムネイル処理
        String[] acceptExts = ImageIO.getWriterFormatNames();
        ShrinkImageSet shrinkImageSet =
          FileuploadUtils.getBytesShrinkFilebean(
            orgId,
            folderName,
            uid,
            filebean,
            acceptExts,
            FileuploadUtils.DEF_THUMBNAIL_WIDTH,
            FileuploadUtils.DEF_THUMBNAIL_HEIGHT,
            msgList,
            true);

        String filename = "0_" + String.valueOf(System.nanoTime());

        // 新規オブジェクトモデル
        EipTGpdbRecordFile file = Database.create(EipTGpdbRecordFile.class);
        // 所有者
        file.setOwnerId(Integer.valueOf(uid));
        // トピックID
        file.setEipTGpdbRecord(record);
        // ファイル名
        file.setFileName(filebean.getFileName());
        // ファイルパス
        file.setFilePath(getRelativePath(filename));
        // サムネイル画像
        if (shrinkImageSet != null && shrinkImageSet.getShrinkImage() != null) {
          file.setFileThumbnail(shrinkImageSet.getShrinkImage());
        }
        // 作成日
        file.setCreateDate(Calendar.getInstance().getTime());
        // 更新日
        file.setUpdateDate(Calendar.getInstance().getTime());

        if (shrinkImageSet != null && shrinkImageSet.getFixImage() != null) {
          // ファイルの作成
          ALStorageService.createNewFile(new ByteArrayInputStream(
            shrinkImageSet.getFixImage()), FOLDER_FILEDIR_GPDB
            + ALStorageService.separator()
            + Database.getDomainName()
            + ALStorageService.separator()
            + CATEGORY_KEY
            + ALStorageService.separator()
            + uid
            + ALStorageService.separator()
            + filename);
        } else {
          // ファイルの移動
          ALStorageService.copyTmpFile(uid, folderName, String.valueOf(filebean
            .getFileId()), FOLDER_FILEDIR_GPDB, CATEGORY_KEY
            + ALStorageService.separator()
            + uid, filename);
        }
      }

      // 添付ファイル保存先のフォルダを削除
      if (folderName != null && !"".equals(folderName.trim())) {
        ALStorageService.deleteTmpFolder(uid, folderName);
      }
    } catch (Exception e) {
      Database.rollback();
      logger.error("Exception", e);
      return false;
    }
    return true;
  }

  /**
   * ユーザ毎のルート保存先（絶対パス）を取得します。
   *
   * @param uid
   *          ユーザーID
   * @return パス
   */
  public static String getSaveDirPath(int uid) {
    return ALStorageService.getDocumentPath(FOLDER_FILEDIR_GPDB, CATEGORY_KEY
      + ALStorageService.separator()
      + uid);
  }

  /**
   * ユーザ毎の保存先（相対パス）を取得します。
   *
   * @param fileName
   *          ファイル名
   * @return パス
   */
  public static String getRelativePath(String fileName) {
    return new StringBuffer().append("/").append(fileName).toString();
  }

  /**
   * 添付ファイルを取得します。
   *
   * @param rundata
   *          RunData
   * @param keyid
   *          キーとなるID
   * @return ファイルリスト
   */
  public static List<FileuploadLiteBean> getFileuploadList(RunData rundata,
      String keyid) {

    String[] fileids =
      rundata.getParameters().getStrings(
        FileuploadUtils.KEY_FILEUPLOAD_ID_LIST + "_" + keyid);
    if (fileids == null) {
      return null;
    }

    ArrayList<String> hadfileids = new ArrayList<String>();
    ArrayList<String> newfileids = new ArrayList<String>();

    for (int j = 0; j < fileids.length; j++) {
      if (fileids[j].trim().startsWith("s")) {
        hadfileids.add(fileids[j].trim().substring(1));
      } else {
        newfileids.add(fileids[j].trim());
      }
    }

    ArrayList<FileuploadLiteBean> fileNameList =
      new ArrayList<FileuploadLiteBean>();
    FileuploadLiteBean filebean = null;

    // 新規にアップロードされたファイルの処理
    if (newfileids.size() > 0) {
      String folderName =
        rundata.getParameters().getString(
          FileuploadUtils.KEY_FILEUPLOAD_FODLER_NAME + "_" + keyid);
      if (folderName == null || folderName.equals("")) {
        return null;
      }

      for (String newfileid : newfileids) {
        if ("".equals(newfileid)) {
          continue;
        }
        int fileid = 0;
        try {
          fileid = Integer.parseInt(newfileid);
        } catch (Exception e) {
          continue;
        }

        if (fileid == 0) {
          filebean = new FileuploadLiteBean();
          filebean.initField();
          filebean.setFolderName("photo");
          filebean.setFileName("以前の写真ファイル");
          fileNameList.add(filebean);
        } else {
          BufferedReader reader = null;
          try {
            reader =
              new BufferedReader(new InputStreamReader(ALStorageService
                .getFile(
                  FileuploadUtils.FOLDER_TMP_FOR_ATTACHMENT_FILES,
                  ALEipUtils.getUserId(rundata)
                    + ALStorageService.separator()
                    + folderName,
                  fileid + FileuploadUtils.EXT_FILENAME), FILE_ENCODING));
            String line = reader.readLine();
            if (line == null || line.length() <= 0) {
              continue;
            }
            filebean = new FileuploadLiteBean();
            filebean.initField();
            filebean.setFolderName(newfileid);
            filebean.setFileId(fileid);
            filebean.setFileName(line);
            fileNameList.add(filebean);
          } catch (Exception e) {
            logger.error("Exception", e);
          } finally {
            try {
              reader.close();
            } catch (Exception e) {
              logger.error("Exception", e);
            }
          }
        }
      }
    }

    // すでにあるファイルの処理
    if (hadfileids.size() > 0) {
      ArrayList<Integer> hadfileidsValue = new ArrayList<Integer>();
      for (String hadfileid : hadfileids) {
        int fileid = 0;
        try {
          fileid = Integer.parseInt(hadfileid);
          hadfileidsValue.add(fileid);
        } catch (Exception e) {
          continue;
        }
      }

      try {
        SelectQuery<EipTGpdbRecordFile> reqquery =
          Database.query(EipTGpdbRecordFile.class);
        Expression reqexp1 =
          ExpressionFactory.inDbExp(
            EipTGpdbRecordFile.FILE_ID_PK_COLUMN,
            hadfileidsValue);
        reqquery.setQualifier(reqexp1);
        List<EipTGpdbRecordFile> requests = reqquery.fetchList();
        for (EipTGpdbRecordFile file : requests) {
          filebean = new FileuploadBean();
          filebean.initField();
          filebean.setFileId(file.getFileId());
          filebean.setFileName(file.getFileName());
          filebean.setFlagNewFile(false);
          fileNameList.add(filebean);
        }
      } catch (Exception ex) {
        logger.error("[BlogUtils] Exception.", ex);
      }
    }
    return fileNameList;
  }

  /**
   * ファイルオブジェクトモデルを取得します。 <BR>
   *
   * @param rundata
   *          RunData
   * @return ファイルオブジェクトモデル
   * @exception ALPageNotFoundException
   *              ページ存在例外
   * @exception ALDBErrorException
   *              DB例外
   */
  public static EipTGpdbRecordFile getEipTGpdbRecordFile(RunData rundata)
      throws ALPageNotFoundException, ALDBErrorException {
    try {
      int attachmentIndex =
        rundata.getParameters().getInt("attachmentIndex", -1);
      if (attachmentIndex < 0) {
        // ID が空の場合
        logger.debug("[GpdbUtils] Empty ID...");
        throw new ALPageNotFoundException();

      }

      SelectQuery<EipTGpdbRecordFile> query =
        Database.query(EipTGpdbRecordFile.class);
      Expression exp =
        ExpressionFactory.matchDbExp(
          EipTGpdbRecordFile.FILE_ID_PK_COLUMN,
          Integer.valueOf(attachmentIndex));
      query.andQualifier(exp);

      List<EipTGpdbRecordFile> files = query.fetchList();
      if (files == null || files.size() == 0) {
        // 指定した ID のレコードが見つからない場合
        logger.debug("[GpdbUtils] Not found ID...");
        throw new ALPageNotFoundException();
      }
      return files.get(0);
    } catch (Exception ex) {
      logger.error("[GpdbUtils]", ex);
      throw new ALDBErrorException();
    }
  }

  /**
   * 添付ファイルリストを取得する
   *
   * @param gpdbRecordId
   *          WebデータベースレコードID
   * @return 添付ファイルリスト
   */
  public static List<EipTGpdbRecordFile> getEipTGpdbRecordFileList(
      int gpdbRecordId) {

    // SQL
    SelectQuery<EipTGpdbRecordFile> query =
      Database.query(EipTGpdbRecordFile.class);
    query.setQualifier(ExpressionFactory.matchDbExp(
      EipTGpdbRecord.GPDB_RECORD_ID_PK_COLUMN,
      gpdbRecordId));

    return query.fetchList();
  }

  /**
   * 添付ファイル一覧を取得する
   *
   * @param gpdbRecordId
   *          WebデータベースレコードID
   * @return 添付ファイル一覧
   */
  public static List<FileuploadBean> getAttachmentFiles(int gpdbRecordId) {

    List<EipTGpdbRecordFile> fileList = getEipTGpdbRecordFileList(gpdbRecordId);

    if (fileList != null && fileList.size() > 0) {

      List<FileuploadBean> attachmentFileList = new ArrayList<FileuploadBean>();
      FileuploadBean filebean = null;
      EipTGpdbRecordFile file = null;

      int size = fileList.size();
      for (int i = 0; i < size; i++) {
        file = fileList.get(i);
        String realname = file.getFileName();
        javax.activation.DataHandler hData =
          new javax.activation.DataHandler(new javax.activation.FileDataSource(
            realname));

        filebean = new FileuploadBean();
        filebean.setFileId(file.getFileId().intValue());
        filebean.setFileName(realname);
        if (hData != null) {
          filebean.setContentType(hData.getContentType());
        }
        filebean.setIsImage(FileuploadUtils.isImage(realname));
        attachmentFileList.add(filebean);
      }
      return attachmentFileList;
    }
    return new ArrayList<FileuploadBean>();
  }

  /**
   *
   * PSMLに設定されているデータと比較して valueが正しい値ならその値を新しくPSMLに保存。
   *
   *
   * @param rundata
   * @param context
   * @param config
   * @return
   */
  public static String passPSML(RunData rundata, Context context, String key,
      String value) {
    VelocityPortlet portlet = ALEipUtils.getPortlet(rundata, context);
    PortletConfig config = portlet.getPortletConfig();
    if (value == null || "".equals(value)) {
      value = config != null ? config.getInitParameter(key) : "";
    } else {
      ALEipUtils.setPsmlParameters(rundata, context, key, value);
    }
    return value;
  }

  /**
   * 表示切り替えのリセットフラグがあるかを返す．
   *
   * @param rundata
   * @param context
   * @return
   */
  public static boolean hasResetKeywordFlag(RunData rundata, Context context) {
    String resetflag = rundata.getParameters().getString(RESET_KEYWORD_FLAG);
    return resetflag != null;
  }

  /**
   * フィルターを初期化する．
   *
   * @param rundata
   * @param context
   * @param className
   */
  public static void resetKeyword(RunData rundata, Context context,
      String className) {
    ALEipUtils.setTemp(rundata, context, SEARCH_WORD, "");
  }

  /**
   * 検索クエリ用のキーワードを取得します。
   *
   * @param rundata
   * @param context
   * @return
   */
  public static String getSearchword(RunData rundata, Context context) {
    String keyword = null;
    String keywordParm = rundata.getParameters().getString(SEARCH_WORD);
    keyword = ALEipUtils.getTemp(rundata, context, SEARCH_WORD);
    if (keywordParm == null && (keyword == null)) {
      ALEipUtils.setTemp(rundata, context, SEARCH_WORD, "");
      keyword = "";
    } else if (keywordParm != null) {
      keywordParm = keywordParm.trim();
      ALEipUtils.setTemp(rundata, context, SEARCH_WORD, keywordParm);
      keyword = keywordParm;
    }

    return keyword;
  }
}
