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
package com.aimluck.eip.report.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import javax.imageio.ImageIO;

import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.jetspeed.om.security.UserIdPrincipal;
import org.apache.jetspeed.services.JetspeedSecurity;
import org.apache.jetspeed.services.customlocalization.CustomLocalizationService;
import org.apache.jetspeed.services.logging.JetspeedLogFactoryService;
import org.apache.jetspeed.services.logging.JetspeedLogger;
import org.apache.jetspeed.services.resources.JetspeedResources;
import org.apache.jetspeed.util.ServiceUtil;
import org.apache.turbine.services.InstantiationException;
import org.apache.turbine.services.TurbineServices;
import org.apache.turbine.services.localization.LocalizationService;
import org.apache.turbine.util.RunData;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.context.Context;

import com.aimluck.commons.field.ALDateTimeField;
import com.aimluck.commons.utils.ALDeleteFileUtil;
import com.aimluck.eip.cayenne.om.portlet.EipTBlogFile;
import com.aimluck.eip.cayenne.om.portlet.EipTReport;
import com.aimluck.eip.cayenne.om.portlet.EipTReportFile;
import com.aimluck.eip.cayenne.om.portlet.EipTReportMap;
import com.aimluck.eip.cayenne.om.portlet.EipTReportMemberMap;
import com.aimluck.eip.cayenne.om.security.TurbineUser;
import com.aimluck.eip.common.ALActivity;
import com.aimluck.eip.common.ALBaseUser;
import com.aimluck.eip.common.ALDBErrorException;
import com.aimluck.eip.common.ALEipConstants;
import com.aimluck.eip.common.ALEipUser;
import com.aimluck.eip.common.ALFileNotRemovedException;
import com.aimluck.eip.common.ALPageNotFoundException;
import com.aimluck.eip.fileupload.beans.FileuploadBean;
import com.aimluck.eip.fileupload.beans.FileuploadLiteBean;
import com.aimluck.eip.fileupload.util.FileuploadUtils;
import com.aimluck.eip.fileupload.util.FileuploadUtils.ShrinkImageSet;
import com.aimluck.eip.mail.util.ALMailUtils;
import com.aimluck.eip.orm.Database;
import com.aimluck.eip.orm.query.SelectQuery;
import com.aimluck.eip.services.accessctl.ALAccessControlConstants;
import com.aimluck.eip.services.accessctl.ALAccessControlFactoryService;
import com.aimluck.eip.services.accessctl.ALAccessControlHandler;
import com.aimluck.eip.services.orgutils.ALOrgUtilsService;
import com.aimluck.eip.services.social.ALActivityService;
import com.aimluck.eip.services.social.model.ALActivityPutRequest;
import com.aimluck.eip.services.storage.ALStorageService;
import com.aimluck.eip.user.beans.UserLiteBean;
import com.aimluck.eip.util.ALCellularUtils;
import com.aimluck.eip.util.ALEipUtils;
import com.aimluck.eip.util.ALLocalizationUtils;

/**
 * 報告書のユーティリティクラスです。 <BR>
 *
 */
public class ReportUtils {

  /** logger */
  private static final JetspeedLogger logger = JetspeedLogFactoryService
    .getLogger(ReportUtils.class.getName());

  /** 未読 */
  public static final String DB_STATUS_UNREAD = "U";

  /** 既読 */
  public static final String DB_STATUS_READ = "R";

  public static final String DATE_TIME_FORMAT =
    ALDateTimeField.DEFAULT_DATE_TIME_FORMAT;

  public static final String REPORT_PORTLET_NAME = "Report";

  /** 報告書の添付ファイルを保管するディレクトリの指定 */
  public static final String FOLDER_FILEDIR_REPORT = JetspeedResources
    .getString("aipo.filedir", "");

  /** 報告書の添付ファイルを保管するディレクトリのカテゴリキーの指定 */
  public static final String CATEGORY_KEY = JetspeedResources.getString(
    "aipo.report.categorykey",
    "");

  /** デフォルトエンコーディングを表わすシステムプロパティのキー */
  public static final String FILE_ENCODING = JetspeedResources.getString(
    "content.defaultencoding",
    "UTF-8");

  /** データベースに登録されたファイルを表す識別子 */
  public static final String PREFIX_DBFILE = "DBF";

  /** パラメータリセットの識別子 */
  private static final String RESET_FLAG = "reset_params";

  /** 検索キーワード変数の識別子 */
  public static final String TARGET_KEYWORD = "keyword";

  /**
   * トピックオブジェクトモデルを取得します。 <BR>
   *
   * @param rundata
   * @param context
   * @param isJoin
   *          カテゴリテーブルをJOINするかどうか
   * @return
   */
  public static EipTReport getEipTReportParentReply(RunData rundata,
      Context context, boolean isJoin) throws ALPageNotFoundException,
      ALDBErrorException {
    String reportid =
      ALEipUtils.getTemp(rundata, context, ALEipConstants.ENTITY_ID);
    try {
      if (reportid == null || Integer.valueOf(reportid) == null) {
        // トピック ID が空の場合
        logger.debug("[EipTReport] Empty ID...");
        throw new ALPageNotFoundException();
      }

      int userid = ALEipUtils.getUserId(rundata);

      SelectQuery<EipTReport> query = Database.query(EipTReport.class);
      Expression exp1 =
        ExpressionFactory.matchDbExp(EipTReport.REPORT_ID_PK_COLUMN, Integer
          .valueOf(reportid));
      query.setQualifier(exp1);
      query.distinct(true);

      List<EipTReport> reports = query.fetchList();
      if (reports == null || reports.size() == 0) {
        // 指定した トピック ID のレコードが見つからない場合
        logger.debug("[ReportTopic] Not found ID...");
        throw new ALPageNotFoundException();
      }

      EipTReport report = reports.get(0);

      // アクセス権限チェック

      return report;
    } catch (ALPageNotFoundException pageNotFound) {
      // logger.error(pageNotFound);
      throw pageNotFound;
    } catch (Exception ex) {
      logger.error("[ReportUtils]", ex);
      throw new ALDBErrorException();

    }
  }

  /**
   * 返信記事オブジェクトモデルを取得します。 <BR>
   *
   * @param rundata
   * @param context
   * @param isSuperUser
   *          カテゴリテーブルをJOINするかどうか
   * @return
   */
  public static EipTReport getEipTReportParentReply(RunData rundata,
      Context context, String reportid, boolean isSuperUser)
      throws ALPageNotFoundException, ALDBErrorException {
    try {
      if (reportid == null || Integer.valueOf(reportid) == null) {
        // トピック ID が空の場合
        logger.debug("[ReportTopic] Empty ID...");
        throw new ALPageNotFoundException();
      }

      SelectQuery<EipTReport> query = Database.query(EipTReport.class);
      Expression exp1 =
        ExpressionFactory.matchDbExp(EipTReport.REPORT_ID_PK_COLUMN, Integer
          .valueOf(reportid));
      query.setQualifier(exp1);

      if (!isSuperUser) {
        Expression exp2 =
          ExpressionFactory.matchExp(EipTReport.USER_ID_PROPERTY, Integer
            .valueOf(ALEipUtils.getUserId(rundata)));
        query.andQualifier(exp2);
      }

      List<EipTReport> reports = query.fetchList();
      if (reports == null || reports.size() == 0) {
        // 指定した トピック ID のレコードが見つからない場合
        logger.debug("[EipTReport] Not found ID...");
        throw new ALPageNotFoundException();
      }
      return reports.get(0);
    } catch (Exception ex) {
      logger.error("[EipUtils]", ex);
      throw new ALDBErrorException();

    }
  }

  public static void deleteFiles(int timelineId, String orgId, int uid,
      List<String> fpaths) throws ALFileNotRemovedException {
    ALDeleteFileUtil.deleteFiles(
      timelineId,
      EipTReportFile.EIP_TREPORT_PROPERTY,
      getSaveDirPath(orgId, uid),
      fpaths,
      EipTReportFile.class);
  }

  public static boolean insertFileDataDelegate(RunData rundata,
      Context context, EipTReport report,
      List<FileuploadLiteBean> fileuploadList, String folderName,
      List<String> msgList) {
    try {
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

      SelectQuery<EipTReportFile> dbquery =
        Database.query(EipTReportFile.class);
      dbquery.andQualifier(ExpressionFactory.matchDbExp(
        EipTReportFile.EIP_TREPORT_PROPERTY,
        report.getReportId()));
      List<EipTReportFile> existsFiles = dbquery.fetchList();
      List<EipTReportFile> delFiles = new ArrayList<EipTReportFile>();
      List<String> fpaths = new ArrayList<String>();
      for (EipTReportFile file : existsFiles) {
        if (!hadfileids.contains(file.getFileId())) {
          delFiles.add(file);
          fpaths.add(file.getFilePath());
        }
      }
      deleteFiles(report.getReportId(), orgId, report.getUserId(), fpaths);

      // ファイル追加処理
      for (FileuploadLiteBean filebean : fileuploadList) {
        if (!filebean.isNewFile()) {
          continue;
        }

        // サムネイル処理
        String[] acceptExts = ImageIO.getWriterFormatNames();
        byte[] fileThumbnail = null;
        ShrinkImageSet bytesShrinkFilebean =
          FileuploadUtils.getBytesShrinkFilebean(
            orgId,
            folderName,
            uid,
            filebean,
            acceptExts,
            FileuploadUtils.DEF_THUMBNAIL_WIDTH,
            FileuploadUtils.DEF_THUMBNAIL_HEIGHT,
            msgList,
            false);
        if (bytesShrinkFilebean != null) {
          fileThumbnail = bytesShrinkFilebean.getShrinkImage();
        }

        String filename = "0_" + String.valueOf(System.nanoTime());

        // 新規オブジェクトモデル
        EipTReportFile file = Database.create(EipTReportFile.class);
        // 所有者
        file.setOwnerId(Integer.valueOf(uid));
        // トピックID
        file.setEipTReport(report);
        // ファイル名
        file.setFileName(filebean.getFileName());
        // ファイルパス
        file.setFilePath(ReportUtils.getRelativePath(filename));
        // サムネイル画像
        if (fileThumbnail != null) {
          file.setFileThumbnail(fileThumbnail);
        }
        // 作成日
        file.setCreateDate(Calendar.getInstance().getTime());
        // 更新日
        file.setUpdateDate(Calendar.getInstance().getTime());

        // ファイルの移動
        ALStorageService.copyTmpFile(uid, folderName, String.valueOf(filebean
          .getFileId()), FOLDER_FILEDIR_REPORT, CATEGORY_KEY
          + ALStorageService.separator()
          + uid, filename);
      }

      // 添付ファイル保存先のフォルダを削除
      ALStorageService.deleteTmpFolder(uid, folderName);
    } catch (ALFileNotRemovedException fe) {
      Database.rollback();
      logger.error("BlogEntryFormData.deleteFormData", fe);
      msgList.add(ALLocalizationUtils.getl10n("ERROR_FILE_DETELE_FAILURE"));
      return false;
    } catch (Exception e) {
      Database.rollback();
      logger.error("report", e);
      return false;
    }
    return true;
  }

  /**
   * Report オブジェクトモデルのIDを取得します。 <BR>
   *
   * @param rundata
   * @param context
   * @return
   */
  public static Integer getEipTReportId(RunData rundata, Context context) {
    String requestid =
      ALEipUtils.getTemp(rundata, context, ALEipConstants.ENTITY_ID);
    Integer id = null;
    if (requestid == null || (id = Integer.valueOf(requestid)) == null) {
      // Request IDが空の場合
      logger.debug("[ReportUtils] Empty ID...");
      return null;
    } else {
      return id;
    }
  }

  /**
   * Report オブジェクトモデルを取得します。 <BR>
   *
   * @param rundata
   * @param context
   * @param mode_update
   * @return
   */
  public static EipTReport getEipTReport(RunData rundata, Context context)
      throws ALDBErrorException {
    String requestid =
      ALEipUtils.getTemp(rundata, context, ALEipConstants.ENTITY_ID);
    try {
      if (requestid == null || Integer.valueOf(requestid) == null) {
        // Request IDが空の場合
        logger.debug("[ReportUtils] Empty ID...");
        return null;
      }

      SelectQuery<EipTReport> query = Database.query(EipTReport.class);
      Expression exp1 =
        ExpressionFactory.matchDbExp(EipTReport.REPORT_ID_PK_COLUMN, requestid);
      query.setQualifier(exp1);

      List<EipTReport> requests = query.fetchList();

      if (requests == null || requests.size() == 0) {
        // 指定した Report IDのレコードが見つからない場合
        logger.debug("[ReportUtils] Not found ID...");
        throw new ALPageNotFoundException();
      }

      return requests.get(0);
    } catch (ALPageNotFoundException ex) {
      ALEipUtils.redirectPageNotFound(rundata);
      return null;
    } catch (Exception ex) {
      logger.error("report", ex);
      throw new ALDBErrorException();
    }
  }

  /**
   * Report オブジェクトモデルを取得します。 <BR>
   *
   * @param rundata
   * @param context
   * @param mode_update
   * @return
   */
  public static List<EipTReport> getEipTReport(EipTReport report)
      throws ALPageNotFoundException {
    try {

      SelectQuery<EipTReport> query = Database.query(EipTReport.class);

      Expression exp =
        ExpressionFactory.matchExp(EipTReport.REPORT_ID_PK_COLUMN, report);

      query.setQualifier(exp);

      return query.fetchList();
    } catch (Exception ex) {
      logger.error("report", ex);
      return null;
    }
  }

  /**
   * ファイルオブジェクトモデルを取得します。 <BR>
   *
   * @param rundata
   * @param context
   * @return
   */
  public static EipTReportFile getEipTReportFile(RunData rundata)
      throws ALPageNotFoundException, ALDBErrorException {
    try {
      int attachmentIndex =
        rundata.getParameters().getInt("attachmentIndex", -1);
      if (attachmentIndex < 0) {
        // ID が空の場合
        logger.debug("[ReportUtils] Empty ID...");
        throw new ALPageNotFoundException();

      }

      SelectQuery<EipTReportFile> query = Database.query(EipTReportFile.class);
      Expression exp =
        ExpressionFactory.matchDbExp(EipTReportFile.FILE_ID_PK_COLUMN, Integer
          .valueOf(attachmentIndex));
      query.andQualifier(exp);
      List<EipTReportFile> files = query.fetchList();
      if (files == null || files.size() == 0) {
        // 指定した ID のレコードが見つからない場合
        logger.debug("[ReportUtils] Not found ID...");
        throw new ALPageNotFoundException();
      }
      return files.get(0);
    } catch (Exception ex) {
      logger.error("[ReportUtils]", ex);
      throw new ALDBErrorException();
    }
  }

  /**
   * マップオブジェクトモデルを取得します。 <BR>
   *
   * @param rundata
   * @param context
   * @return
   */
  public static List<EipTReportFile> getEipTReportFile(EipTReport report) {
    try {
      SelectQuery<EipTReportFile> query = Database.query(EipTReportFile.class);
      Expression exp =
        ExpressionFactory.matchDbExp(EipTReportFile.EIP_TREPORT_PROPERTY
          + "."
          + EipTReport.REPORT_ID_PK_COLUMN, report.getReportId());
      query.setQualifier(exp);

      List<EipTReportFile> maps = query.fetchList();

      if (maps == null || maps.size() == 0) {
        // 指定した Report IDのレコードが見つからない場合
        logger.debug("[ReportSelectData] Not found ID...");
        return null;
      }
      return maps;
    } catch (Exception ex) {
      logger.error("report", ex);
      return null;
    }
  }

  /**
   * マップオブジェクトモデルを取得します。 <BR>
   *
   * @param rundata
   * @param context
   * @return
   */
  public static List<EipTReportMap> getEipTReportMap(EipTReport report) {
    try {
      SelectQuery<EipTReportMap> query = Database.query(EipTReportMap.class);
      Expression exp =
        ExpressionFactory.matchDbExp(EipTReportMap.EIP_TREPORT_PROPERTY
          + "."
          + EipTReport.REPORT_ID_PK_COLUMN, report.getReportId());
      query.setQualifier(exp);

      List<EipTReportMap> maps = query.fetchList();

      if (maps == null || maps.size() == 0) {
        // 指定した Report IDのレコードが見つからない場合
        logger.debug("[ReportSelectData] Not found ID...");
        return null;
      }
      return maps;
    } catch (Exception ex) {
      logger.error("report", ex);
      return null;
    }
  }

  /**
   * マップオブジェクトモデルを取得します。 <BR>
   *
   * @param rundata
   * @param context
   * @return
   */
  public static List<EipTReportMemberMap> getEipTReportMemberMap(
      EipTReport report) {
    try {
      SelectQuery<EipTReportMemberMap> query =
        Database.query(EipTReportMemberMap.class);
      Expression exp =
        ExpressionFactory.matchDbExp(EipTReportMemberMap.EIP_TREPORT_PROPERTY
          + "."
          + EipTReport.REPORT_ID_PK_COLUMN, report.getReportId());
      query.setQualifier(exp);

      List<EipTReportMemberMap> members = query.fetchList();

      if (members == null || members.size() == 0) {
        // 指定した Report IDのレコードが見つからない場合
        logger.debug("[ReportSelectData] Not found ID...");
        return null;
      }
      return members;
    } catch (Exception ex) {
      logger.error("report", ex);
      return null;
    }
  }

  /**
   * ユーザ毎のルート保存先（絶対パス）を取得します。
   *
   * @param uid
   * @return
   */
  public static String getSaveDirPath(String orgId, int uid) {
    return ALStorageService.getDocumentPath(FOLDER_FILEDIR_REPORT, CATEGORY_KEY
      + ALStorageService.separator()
      + uid);
  }

  /**
   * ユーザ毎の保存先（相対パス）を取得します。
   *
   * @param uid
   * @return
   */
  public static String getRelativePath(String fileName) {
    return new StringBuffer().append("/").append(fileName).toString();
  }

  public static List<UserLiteBean> getAuthorityUsers(RunData rundata,
      String groupname, boolean includeLoginuser) {

    try {
      // アクセス権限
      ALAccessControlFactoryService aclservice =
        (ALAccessControlFactoryService) ((TurbineServices) TurbineServices
          .getInstance())
          .getService(ALAccessControlFactoryService.SERVICE_NAME);
      ALAccessControlHandler aclhandler = aclservice.getAccessControlHandler();

      List<TurbineUser> ulist =
        aclhandler.getAuthorityUsersFromGroup(
          rundata,
          ALAccessControlConstants.POERTLET_FEATURE_REPORT_SELF,
          groupname,
          includeLoginuser);

      List<UserLiteBean> list = new ArrayList<UserLiteBean>();

      UserLiteBean user;
      // ユーザデータを作成し、返却リストへ格納
      for (TurbineUser tuser : ulist) {
        user = new UserLiteBean();
        user.initField();
        user.setUserId(tuser.getUserId());
        user.setName(tuser.getLoginName());
        user.setAliasName(tuser.getFirstName(), tuser.getLastName());
        list.add(user);
      }
      return list;
    } catch (InstantiationException e) {
      return null;
    }

  }

  /**
   * 表示切り替えのリセットフラグがあるかを返す．
   *
   * @param rundata
   * @param context
   * @return
   */
  public static boolean hasResetFlag(RunData rundata, Context context) {
    String resetflag = rundata.getParameters().getString(RESET_FLAG);
    return resetflag != null;
  }

  public static void clearReportSession(RunData rundata, Context context) {
    List<String> list = new ArrayList<String>();
    list.add("entityid");
    list.add("submenu");
    list.add("ReportSelectDatasort");
    list.add("ReportSelectDatasorttype");
    list.add("ReportSelectDatafiltertype");
    list.add("ReportSelectDatafilter");
    list.add(TARGET_KEYWORD);
    ALEipUtils.removeTemp(rundata, context, list);
  }

  public static int getViewId(RunData rundata, Context context, int uid)
      throws ALDBErrorException {
    int view_uid = -1;
    EipTReport record = ReportUtils.getEipTReport(rundata, context);
    if (record != null) {
      view_uid = record.getUserId();
    } else {
      if (rundata.getParameters().containsKey("view_uid")) {
        view_uid =
          Integer.parseInt(rundata.getParameters().getString("view_uid"));
      } else {
        view_uid = uid;
      }
    }
    ALEipUtils.setTemp(rundata, context, "view_uid", String.valueOf(view_uid));
    return view_uid;
  }

  /**
   * ファイル検索のクエリを返します
   *
   * @param requestid
   *          ファイルを検索するリクエストのid
   * @return query
   */
  public static SelectQuery<EipTReportFile> getSelectQueryForFiles(int requestid) {
    SelectQuery<EipTReportFile> query = Database.query(EipTReportFile.class);
    Expression exp =
      ExpressionFactory.matchDbExp(EipTReport.REPORT_ID_PK_COLUMN, Integer
        .valueOf(requestid));
    query.setQualifier(exp);
    query.orderAscending(EipTReportFile.UPDATE_DATE_PROPERTY);
    query.orderAscending(EipTReportFile.FILE_PATH_PROPERTY);
    return query;
  }

  /**
   * 子のレポート検索のクエリを返します
   *
   * @param requestid
   *          レポートを検索するリクエストのid
   * @return query
   */
  public static SelectQuery<EipTReport> getSelectQueryForCoReports(int requestid) {
    SelectQuery<EipTReport> query = Database.query(EipTReport.class);
    Expression exp =
      ExpressionFactory.matchDbExp(EipTReport.PARENT_ID_PROPERTY, Integer
        .valueOf(requestid));
    query.setQualifier(exp);
    return query;
  }

  /**
   * 添付ファイルを取得します。
   *
   * @param uid
   * @return
   */
  public static ArrayList<FileuploadLiteBean> getFileuploadList(RunData rundata) {
    String[] fileids =
      rundata
        .getParameters()
        .getStrings(FileuploadUtils.KEY_FILEUPLOAD_ID_LIST);
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
    int fileid = 0;

    // 新規にアップロードされたファイルの処理
    if (newfileids.size() > 0) {
      String folderName =
        rundata.getParameters().getString(
          FileuploadUtils.KEY_FILEUPLOAD_FODLER_NAME);
      if (folderName == null || folderName.equals("")) {
        return null;
      }

      int length = newfileids.size();
      for (int i = 0; i < length; i++) {
        if (newfileids.get(i) == null || newfileids.get(i).equals("")) {
          continue;
        }

        try {
          fileid = Integer.parseInt(newfileids.get(i));
        } catch (Exception e) {
          continue;
        }

        if (fileid == 0) {
          filebean = new FileuploadLiteBean();
          filebean.initField();
          filebean.setFolderName("photo");
          filebean.setFileName(ALLocalizationUtils
            .getl10n("REPORT_PREVIOUS_PICTURE_FILE"));
          fileNameList.add(filebean);
        } else {
          BufferedReader reader = null;
          try {
            reader =
              new BufferedReader(new InputStreamReader(ALStorageService
                .getTmpFile(ALEipUtils.getUserId(rundata), folderName, fileid
                  + FileuploadUtils.EXT_FILENAME), FILE_ENCODING));
            String line = reader.readLine();
            if (line == null || line.length() <= 0) {
              continue;
            }

            filebean = new FileuploadLiteBean();
            filebean.initField();
            filebean.setFolderName(fileids[i]);
            filebean.setFileId(fileid);
            filebean.setFileName(line);
            fileNameList.add(filebean);
          } catch (Exception e) {
            logger.error("report", e);
          } finally {
            try {
              reader.close();
            } catch (Exception e) {
              logger.error("report", e);
            }
          }
        }

      }
    }

    if (hadfileids.size() > 0) {
      // すでにあるファイルの処理
      ArrayList<Integer> hadfileidsValue = new ArrayList<Integer>();
      for (int k = 0; k < hadfileids.size(); k++) {
        try {
          fileid = Integer.parseInt(hadfileids.get(k));
          hadfileidsValue.add(fileid);
        } catch (Exception e) {
          continue;
        }
      }

      try {
        SelectQuery<EipTReportFile> reqquery =
          Database.query(EipTReportFile.class);
        Expression reqexp1 =
          ExpressionFactory.inDbExp(
            EipTBlogFile.FILE_ID_PK_COLUMN,
            hadfileidsValue);
        reqquery.setQualifier(reqexp1);
        List<EipTReportFile> requests = reqquery.fetchList();
        int requestssize = requests.size();
        for (int i = 0; i < requestssize; i++) {
          EipTReportFile file = requests.get(i);
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
   * Date のオブジェクトを指定した形式の文字列に変換する．
   *
   * @param date
   * @param dateFormat
   * @return
   */
  public static String translateDate(Date date, String dateFormat) {
    if (date == null) {
      return "Unknown";
    }

    // 日付を表示形式に変換
    SimpleDateFormat sdf = new SimpleDateFormat(dateFormat);
    sdf.setTimeZone(TimeZone.getDefault());
    return sdf.format(date);
  }

  /**
   * 返信記事オブジェクトモデルを取得します。 <BR>
   *
   * @param rundata
   * @param context
   * @param isSuperUser
   *          カテゴリテーブルをJOINするかどうか
   * @return
   */
  public static EipTReport getEipTReportReply(RunData rundata, Context context,
      String reportid, boolean isSuperUser) throws ALPageNotFoundException,
      ALDBErrorException {
    try {
      if (reportid == null || Integer.valueOf(reportid) == null) {
        // トピック ID が空の場合
        logger.debug("[ReportTopic] Empty ID...");
        throw new ALPageNotFoundException();
      }

      SelectQuery<EipTReport> query = Database.query(EipTReport.class);
      Expression exp1 =
        ExpressionFactory.matchDbExp(EipTReport.REPORT_ID_PK_COLUMN, Integer
          .valueOf(reportid));
      query.setQualifier(exp1);

      if (!isSuperUser) {
        Expression exp2 =
          ExpressionFactory.matchExp(EipTReport.USER_ID_PROPERTY, Integer
            .valueOf(ALEipUtils.getUserId(rundata)));
        query.andQualifier(exp2);
      }

      List<EipTReport> reports = query.fetchList();
      if (reports == null || reports.size() == 0) {
        // 指定した トピック ID のレコードが見つからない場合
        logger.debug("[Report] Not found ID...");
        throw new ALPageNotFoundException();
      }
      return reports.get(0);
    } catch (Exception ex) {
      logger.error("[ReportUtils]", ex);
      throw new ALDBErrorException();

    }
  }

  /**
   * アクティビティを通知先・社内参加者の「あなた宛のお知らせ」に表示させる
   *
   * @param report
   * @param loginName
   * @param recipients
   * @param type
   */
  public static void createReportActivity(EipTReport report, String loginName,
      List<String> recipients, Boolean type) {
    if (recipients != null && recipients.size() > 0) {
      ALActivity RecentActivity =
        ALActivity.getRecentActivity("Report", report.getReportId(), 1f);
      boolean isDeletePrev =
        RecentActivity != null && RecentActivity.isReplace(loginName);

      StringBuilder b =
        new StringBuilder(ALLocalizationUtils.getl10n("REPORT_REPORT") + "「");

      b.append(report.getReportName()).append("」").append(
        type
          ? ALLocalizationUtils.getl10n("REPORT_SEND_REQUEST_MSG")
          : ALLocalizationUtils.getl10n("REPORT_UPDATED_MSG"));

      String portletParams =
        new StringBuilder("?template=ReportDetailScreen")
          .append("&entityid=")
          .append(report.getReportId())
          .toString();
      ALActivityService.create(new ALActivityPutRequest()
        .withAppId("Report")
        .withUserId(report.getUserId())
        .withLoginName(loginName)
        .withPortletParams(portletParams)
        .withRecipients(recipients)
        .withTitle(b.toString())
        .withPriority(1f)
        .withExternalId(String.valueOf(report.getReportId())));

      if (isDeletePrev) {
        RecentActivity.delete();
      }
    }
  }

  /**
   * アクティビティを通知先・社内参加者の「あなた宛のお知らせ」に表示させる（返信用）
   *
   * @param report
   * @param loginName
   * @param recipients
   * @param type
   */
  public static void createReportReplyActivity(EipTReport report,
      String loginName, List<String> recipient) {
    if (recipient != null) {
      recipient.remove(loginName);

      ALActivity RecentActivity =
        ALActivity.getRecentActivity("Report", report.getReportId(), 1f);
      boolean isDeletePrev =
        RecentActivity != null && RecentActivity.isReplace(loginName);
      StringBuilder b =
        new StringBuilder(ALLocalizationUtils.getl10n("REPORT_REPORT") + "「");

      b.append(report.getReportName()).append("」").append(
        ALLocalizationUtils.getl10n("REPORT_REPLY_MSG"));

      String portletParams =
        new StringBuilder("?template=ReportDetailScreen")
          .append("&entityid=")
          .append(report.getReportId())
          .toString();
      ALActivityService.create(new ALActivityPutRequest()
        .withAppId("Report")
        .withUserId(report.getUserId())
        .withLoginName(loginName)
        .withPortletParams(portletParams)
        .withRecipients(recipient)
        .withTitle(b.toString())
        .withPriority(1f)
        .withExternalId(String.valueOf(report.getReportId())));

      if (isDeletePrev) {
        RecentActivity.delete();
      }
    }
  }

  /**
   * パソコンへ送信するメールの内容を作成する．
   *
   * @return
   */
  public static String createMsgForPc(RunData rundata, EipTReport report,
      List<ALEipUser> memberList, List<ALEipUser> mapList, Boolean isNew)
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
    context.put("getReportName", report.getReportName());
    // 日時（年月日分秒）
    ALDateTimeField alDateTimeField = new ALDateTimeField();
    alDateTimeField.setValue(report.getCreateDate());
    StringBuffer createDate = new StringBuffer();
    createDate.append(alDateTimeField.getYear()).append(
      ALLocalizationUtils.getl10nFormat("NOTE_YEAR")).append(
      alDateTimeField.getMonth()).append(
      ALLocalizationUtils.getl10nFormat("NOTE_MONTH")).append(
      alDateTimeField.getDay()).append(
      ALLocalizationUtils.getl10nFormat("NOTE_DAY")).append(
      alDateTimeField.getHour()).append(
      ALLocalizationUtils.getl10nFormat("NOTE_HOUR")).append(
      alDateTimeField.getMinute()).append(
      ALLocalizationUtils.getl10nFormat("NOTE_MINUTE"));
    context.put("createDate", createDate);
    // 内容
    context.put("getNote", report.getNote());
    // 社内参加者
    StringBuffer reportName = new StringBuffer();
    if (memberList != null) {
      int size = memberList.size();
      int i;
      for (i = 0; i < size; i++) {
        if (i != 0) {
          reportName.append(", ");
        }
        ALEipUser member = memberList.get(i);
        reportName.append(member.getAliasName());
      }
    }
    context.put("reportName", reportName);
    // 通知先
    StringBuffer eipTReportMemberMap = new StringBuffer();
    if (mapList != null) {
      int size = mapList.size();
      int i;
      for (i = 0; i < size; i++) {
        if (i != 0) {
          eipTReportMemberMap.append(", ");
        }
        ALEipUser member = mapList.get(i);
        eipTReportMemberMap.append(member.getAliasName());
      }
    }
    context.put("eipTReportMemberMap", eipTReportMemberMap);
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
            + "/report-notification-mail.vm", "utf-8");
        template.merge(context, writer);
      } else {
        Template template =
          Velocity.getTemplate(
            "portlets/mail/report-notification-mail.vm",
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
   * @return
   */
  public static String createMsgForCellPhone(RunData rundata,
      EipTReport report, List<ALEipUser> memberList, List<ALEipUser> mapList,
      Boolean isNew) throws ALDBErrorException {
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
    context.put("getReportName", report.getReportName());
    // 日時
    ALDateTimeField alDateTimeField = new ALDateTimeField();
    alDateTimeField.setValue(report.getCreateDate());
    StringBuffer createDate = new StringBuffer();
    createDate.append(alDateTimeField.getYear()).append(
      ALLocalizationUtils.getl10nFormat("NOTE_YEAR")).append(
      alDateTimeField.getMonth()).append(
      ALLocalizationUtils.getl10nFormat("NOTE_MONTH")).append(
      alDateTimeField.getDay()).append(
      ALLocalizationUtils.getl10nFormat("NOTE_DAY")).append(
      alDateTimeField.getHour()).append(
      ALLocalizationUtils.getl10nFormat("NOTE_HOUR")).append(
      alDateTimeField.getMinute()).append(
      ALLocalizationUtils.getl10nFormat("NOTE_MINUTE"));
    context.put("createDate", createDate);
    // 内容
    context.put("getNote", report.getNote());
    // 社内参加者
    StringBuffer reportName = new StringBuffer();
    if (memberList != null) {
      int size = memberList.size();
      int i;
      for (i = 0; i < size; i++) {
        if (i != 0) {
          reportName.append(", ");
        }
        ALEipUser member = memberList.get(i);
        reportName.append(member.getAliasName());
      }
    }
    context.put("reportName", reportName);
    // 通知先
    StringBuffer eipTReportMemberMap = new StringBuffer();
    if (mapList != null) {
      int size = mapList.size();
      int i;
      for (i = 0; i < size; i++) {
        if (i != 0) {
          eipTReportMemberMap.append(", ");
        }
        ALEipUser member = mapList.get(i);
        eipTReportMemberMap.append(member.getAliasName());
      }
    }
    context.put("eipTReportMemberMap", eipTReportMemberMap);
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
            + "/report-notification-mail.vm", "utf-8");
        template.merge(context, writer);
      } else {
        Template template =
          Velocity.getTemplate(
            "portlets/mail/report-notification-mail.vm",
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
   * パソコンへ送信するメールの内容を作成する（返信用）．
   *
   * @return
   */
  public static String createReplyMsgForPc(RunData rundata, EipTReport report,
      EipTReport reportparentreport) {
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
    String CR = System.getProperty("line.separator");
    StringBuffer body = new StringBuffer("");
    body.append(loginUser.getAliasName().toString());
    if (!"".equals(user.getEmail())) {
      body.append("(").append(user.getEmail()).append(")");
    }
    body.append(ALLocalizationUtils.getl10n("REPORT_REPORT_MSG")).append(
      ALLocalizationUtils.getl10n("REPORT_REPLY_MSG")).append(CR).append(CR);
    body
      .append(
        "["
          + ALLocalizationUtils.getl10n("REPORT_SETFIELDNAME_REPORT_NAME")
          + "]")
      .append(CR)
      .append(reportparentreport.getReportName().toString())
      .append(CR);
    body.append(
      "["
        + ALLocalizationUtils.getl10n("REPORT_RETURN_REPORT_CREATEDATE")
        + "]").append(CR).append(
      translateDate(report.getCreateDate(), ALLocalizationUtils
        .getl10n("REPORT_TIME"))).append(CR);

    if (report.getNote().toString().length() > 0) {
      body
        .append(
          "[" + ALLocalizationUtils.getl10n("REPORT_RETURN_REPORT_NOTE") + "]")
        .append(CR)
        .append(report.getNote().toString())
        .append(CR);
    }
    body.append(CR);
    body.append("[").append(ALOrgUtilsService.getAlias()).append(
      ALLocalizationUtils.getl10n("REPORT_ACCESS") + "]").append(CR);
    if (enableAsp) {
      body.append("　").append(ALMailUtils.getGlobalurl()).append(CR);
    } else {
      body
        .append("・" + ALLocalizationUtils.getl10n("REPORT_OUTSIDE_OFFICE"))
        .append(CR);
      body.append("　").append(ALMailUtils.getGlobalurl()).append(CR);
      body
        .append("・" + ALLocalizationUtils.getl10n("REPORT_IN_OFFICE"))
        .append(CR);
      body.append("　").append(ALMailUtils.getLocalurl()).append(CR).append(CR);
    }

    body.append("---------------------").append(CR);
    body.append(ALOrgUtilsService.getAlias()).append(CR);

    return body.toString();
  }

  /**
   * 携帯電話へ送信するメールの内容を作成する（返信用）．
   *
   * @return
   */
  public static String createReplyMsgForCellPhone(RunData rundata,
      EipTReport report, EipTReport reportparentreport, int destUserID) {
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
    String CR = System.getProperty("line.separator");
    StringBuffer body = new StringBuffer("");
    body.append(loginUser.getAliasName().toString());
    if (!"".equals(user.getEmail())) {
      body.append("(").append(user.getEmail()).append(")");
    }
    body.append(ALLocalizationUtils.getl10n("REPORT_REPORT_MSG")).append(
      ALLocalizationUtils.getl10n("REPORT_REPLY_MSG")).append(CR).append(CR);
    body
      .append(
        "["
          + ALLocalizationUtils.getl10n("REPORT_SETFIELDNAME_REPORT_NAME")
          + "]")
      .append(CR)
      .append(reportparentreport.getReportName().toString())
      .append(CR);
    body.append(
      "["
        + ALLocalizationUtils.getl10n("REPORT_RETURN_REPORT_CREATEDATE")
        + "]").append(CR).append(
      translateDate(report.getCreateDate(), ALLocalizationUtils
        .getl10n("REPORT_TIME"))).append(CR);
    body.append(CR);

    ALEipUser destUser;
    try {
      destUser = ALEipUtils.getALEipUser(destUserID);
    } catch (ALDBErrorException ex) {
      logger.error("report", ex);
      return "";
    }
    body.append("[").append(ALOrgUtilsService.getAlias()).append(
      ALLocalizationUtils.getl10n("REPORT_ACCESS") + "]").append(CR);
    body.append("　").append(ALMailUtils.getGlobalurl()).append("?key=").append(
      ALCellularUtils.getCellularKey(destUser)).append(CR);
    body.append("---------------------").append(CR);
    body.append(ALOrgUtilsService.getAlias()).append(CR);
    return body.toString();
  }

  /**
   * フィルターを初期化する．
   *
   * @param rundata
   * @param context
   * @param className
   */
  public static void resetFilter(RunData rundata, Context context,
      String className) {
    ALEipUtils.setTemp(rundata, context, TARGET_KEYWORD, "");
  }

  /**
   * 表示切り替えで指定した検索キーワードを取得する．
   *
   * @param rundata
   * @param context
   * @return
   */
  public static String getTargetKeyword(RunData rundata, Context context) {
    String target_keyword = null;
    String keywordParam = rundata.getParameters().getString(TARGET_KEYWORD);
    target_keyword = ALEipUtils.getTemp(rundata, context, TARGET_KEYWORD);

    if (keywordParam == null && (target_keyword == null)) {
      ALEipUtils.setTemp(rundata, context, TARGET_KEYWORD, "");
      target_keyword = "";
    } else if (keywordParam != null) {
      ALEipUtils.setTemp(rundata, context, TARGET_KEYWORD, keywordParam.trim());
      target_keyword = keywordParam;
    }
    return target_keyword;
  }

  /**
   * 報告書の通知メンバーに入っているか.
   *
   * @param rundata
   * @param context
   * @return
   */
  public static boolean isSelf(RunData rundata, Context context) {
    boolean isSelf = false;
    if (rundata.getParameters().getStringKey("entityid") != null) {
      SelectQuery<EipTReportMap> q = Database.query(EipTReportMap.class);
      Expression exp1 =
        ExpressionFactory.matchExp(EipTReportMap.REPORT_ID_PROPERTY, rundata
          .getParameters()
          .getStringKey("entityid")
          .toString());
      q.andQualifier(exp1);
      List<EipTReportMap> queryList = q.fetchList();
      for (EipTReportMap repo : queryList) {
        if (repo.getUserId() == ALEipUtils.getUserId(rundata)) {
          isSelf = true;
        }
      }
    }

    return isSelf;
  }

  public static List<EipTReport> getChildReports(Integer reportId) {
    SelectQuery<EipTReport> rquery = Database.query(EipTReport.class);
    rquery.andQualifier(ExpressionFactory.matchExp(
      EipTReport.PARENT_ID_PROPERTY,
      reportId));
    return rquery.fetchList();
  }

  public static List<EipTReportFile> getFiles(Integer reportId) {
    SelectQuery<EipTReportFile> fquery = Database.query(EipTReportFile.class);
    fquery.andQualifier(ExpressionFactory.matchExp(
      EipTReportFile.EIP_TREPORT_PROPERTY,
      reportId));
    return fquery.fetchList();
  }

}
