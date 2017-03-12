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
package com.aimluck.eip.workflow.util;

import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;

import javax.imageio.ImageIO;

import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.fulcrum.localization.LocalizationService;
import org.apache.jetspeed.services.customlocalization.CustomLocalizationService;
import org.apache.jetspeed.services.logging.JetspeedLogFactoryService;
import org.apache.jetspeed.services.logging.JetspeedLogger;
import org.apache.jetspeed.services.resources.JetspeedResources;
import org.apache.jetspeed.util.ServiceUtil;
import org.apache.turbine.services.InstantiationException;
import org.apache.turbine.services.TurbineServices;
import org.apache.turbine.util.RunData;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.context.Context;

import com.aimluck.commons.field.ALDateTimeField;
import com.aimluck.commons.utils.ALDeleteFileUtil;
import com.aimluck.eip.cayenne.om.portlet.EipTWorkflowCategory;
import com.aimluck.eip.cayenne.om.portlet.EipTWorkflowFile;
import com.aimluck.eip.cayenne.om.portlet.EipTWorkflowRequest;
import com.aimluck.eip.cayenne.om.portlet.EipTWorkflowRequestMap;
import com.aimluck.eip.cayenne.om.portlet.EipTWorkflowRoute;
import com.aimluck.eip.cayenne.om.security.TurbineUser;
import com.aimluck.eip.common.ALActivity;
import com.aimluck.eip.common.ALDBErrorException;
import com.aimluck.eip.common.ALEipConstants;
import com.aimluck.eip.common.ALEipUser;
import com.aimluck.eip.common.ALFileNotRemovedException;
import com.aimluck.eip.common.ALPageNotFoundException;
import com.aimluck.eip.fileupload.beans.FileuploadBean;
import com.aimluck.eip.fileupload.beans.FileuploadLiteBean;
import com.aimluck.eip.fileupload.util.FileuploadUtils;
import com.aimluck.eip.fileupload.util.FileuploadUtils.ShrinkImageSet;
import com.aimluck.eip.mail.ALAdminMailContext;
import com.aimluck.eip.mail.ALAdminMailMessage;
import com.aimluck.eip.mail.ALMailService;
import com.aimluck.eip.mail.util.ALEipUserAddr;
import com.aimluck.eip.mail.util.ALMailUtils;
import com.aimluck.eip.orm.Database;
import com.aimluck.eip.orm.query.ResultList;
import com.aimluck.eip.orm.query.SelectQuery;
import com.aimluck.eip.services.accessctl.ALAccessControlConstants;
import com.aimluck.eip.services.accessctl.ALAccessControlFactoryService;
import com.aimluck.eip.services.accessctl.ALAccessControlHandler;
import com.aimluck.eip.services.orgutils.ALOrgUtilsService;
import com.aimluck.eip.services.social.ALActivityService;
import com.aimluck.eip.services.social.model.ALActivityPutRequest;
import com.aimluck.eip.services.storage.ALStorageService;
import com.aimluck.eip.user.beans.UserLiteBean;
import com.aimluck.eip.util.ALEipUtils;
import com.aimluck.eip.util.ALLocalizationUtils;
import com.aimluck.eip.workflow.WorkflowCategoryResultData;
import com.aimluck.eip.workflow.WorkflowDecisionRecordData;
import com.aimluck.eip.workflow.WorkflowDetailResultData;
import com.aimluck.eip.workflow.WorkflowOldRequestResultData;
import com.aimluck.eip.workflow.WorkflowRouteResultData;
import com.aimluck.eip.workflow.beans.WorkflowMailBean;

/**
 * ワークフローのユーティリティクラスです。 <BR>
 * 
 */
public class WorkflowUtils {

  /** logger */
  private static final JetspeedLogger logger = JetspeedLogFactoryService
    .getLogger(WorkflowUtils.class.getName());

  /** 申請 */
  public static final String DB_STATUS_REQUEST = "R";

  /** 確認中 */
  public static final String DB_STATUS_CONFIRM = "C";

  /** 確認前 */
  public static final String DB_STATUS_WAIT = "W";

  /** 承認 */
  public static final String DB_STATUS_ACCEPT = "A";

  /** 否認 */
  public static final String DB_STATUS_DENIAL = "D";

  /** 削除・無効化のため自動承認 */
  public static final String DB_STATUS_THROUGH = "T";

  /** すべて承認 */
  public static final String DB_PROGRESS_ACCEPT = "A";

  /** 確認中 */
  public static final String DB_PROGRESS_WAIT = "W";

  /** 申請者に差し戻し */
  public static final String DB_PROGRESS_DENAIL = "D";

  /** 差し戻し後、再申請済み */
  public static final String DB_PROGRESS_REAPPLY = "R";

  /** データベースに登録されたファイルを表す識別子 */
  public static final String PREFIX_DBFILE = "DBF";

  /** デフォルトエンコーディングを表わすシステムプロパティのキー */
  public static final String FILE_ENCODING = JetspeedResources.getString(
    "content.defaultencoding",
    "UTF-8");

  /** ワークフローの添付ファイルを保管するディレクトリの指定 */
  private static final String FOLDER_FILEDIR_WORKFLOW = JetspeedResources
    .getString("aipo.filedir", "");

  /** ワークフローの添付ファイルを保管するディレクトリのカテゴリキーの指定 */
  protected static final String CATEGORY_KEY = JetspeedResources.getString(
    "aipo.workflow.categorykey",
    "");

  public enum Type {
    REQUEST, DENAIL, ACCEPT
  }

  public static final String WORKFLOW_PORTLET_NAME = "Workflow";

  public static final String WORKFLOW_CATEGORY_PORTLET_NAME =
    "WorkflowCategory";

  /** 検索キーワード変数の識別子 */
  public static final String TARGET_KEYWORD = "keyword";

  /** パラメータリセットの識別子 */
  private static final String RESET_FLAG = "reset_params";

  /**
   * Request オブジェクトモデルを取得します。 <BR>
   * 
   * @param rundata
   * @param context
   * @param mode_update
   * @return
   */
  public static EipTWorkflowRequest getEipTWorkflowRequest(RunData rundata,
      Context context, boolean mode_update) throws ALPageNotFoundException {
    int uid = ALEipUtils.getUserId(rundata);
    String requestid =
      ALEipUtils.getTemp(rundata, context, ALEipConstants.ENTITY_ID);
    try {
      if (requestid == null || Integer.valueOf(requestid) == null) {
        // Request IDが空の場合
        logger.debug("[WorkflowUtils] Empty ID...");
        throw new ALPageNotFoundException();
      }

      SelectQuery<EipTWorkflowRequest> query =
        Database.query(EipTWorkflowRequest.class);
      Expression exp1 =
        ExpressionFactory.matchDbExp(
          EipTWorkflowRequest.REQUEST_ID_PK_COLUMN,
          requestid);
      query.setQualifier(exp1);

      if (mode_update) {
        Expression exp3 =
          ExpressionFactory.matchExp(
            EipTWorkflowRequest.EIP_TWORKFLOW_REQUEST_MAP_PROPERTY
              + "."
              + EipTWorkflowRequestMap.STATUS_PROPERTY,
            DB_STATUS_CONFIRM);
        query.andQualifier(exp3);

        Expression exp4 =
          ExpressionFactory.matchExp(
            EipTWorkflowRequest.PROGRESS_PROPERTY,
            DB_PROGRESS_WAIT);
        query.andQualifier(exp4);
      }

      List<EipTWorkflowRequest> requests = query.fetchList();

      if (requests == null || requests.size() == 0) {
        // 指定した Request IDのレコードが見つからない場合
        logger.debug("[WorkflowUtils] Not found ID...");
        throw new ALPageNotFoundException();
      }

      // アクセス権の判定
      Set<Integer> relatedUserIds = getRelatedUserIdList(requests.get(0));
      if (!relatedUserIds.contains(uid)) {
        // 指定したアカウントIDのレコードが見つからない場合
        logger.debug("[WorkFlow] Invalid user access...");
        throw new ALPageNotFoundException();
      }
      return requests.get(0);
    } catch (ALPageNotFoundException pageNotFound) {
      // logger.error("[WorkflowUtils]", pageNotFound);
      throw pageNotFound;
    } catch (Exception ex) {
      logger.error("workflow", ex);
      return null;
    }
  }

  /**
   * Request オブジェクトモデルを取得します。 <BR>
   * 
   * @param rundata
   * @param context
   * @param mode_update
   * @return
   */
  public static List<EipTWorkflowRequest> getEipTWorkflowRequest(
      EipTWorkflowRoute route) throws ALPageNotFoundException {
    try {

      SelectQuery<EipTWorkflowRequest> query =
        Database.query(EipTWorkflowRequest.class);

      Expression exp =
        ExpressionFactory.matchExp(
          EipTWorkflowRequest.EIP_TWORKFLOW_ROUTE_PROPERTY,
          route);

      query.setQualifier(exp);

      return query.fetchList();
    } catch (Exception ex) {
      logger.error("workflow", ex);
      return null;
    }
  }

  /**
   * Category オブジェクトモデルを取得します。 <BR>
   * 
   * @param rundata
   * @param context
   * @param mode_update
   * @return
   */
  public static List<EipTWorkflowCategory> getEipTworkflowCategory(
      EipTWorkflowRoute route) throws ALPageNotFoundException {

    try {

      SelectQuery<EipTWorkflowCategory> query =
        Database.query(EipTWorkflowCategory.class);

      Expression exp =
        ExpressionFactory.matchExp(
          EipTWorkflowCategory.EIP_TWORKFLOW_ROUTE_PROPERTY,
          route);

      query.setQualifier(exp);

      return query.fetchList();
    } catch (Exception ex) {
      logger.error("workflow", ex);
      return null;
    }

  }

  /**
   * Request オブジェクトモデルを取得します。 <BR>
   * 
   * @param rundata
   * @param context
   * @param mode_update
   * @return
   */
  public static EipTWorkflowRequest getEipTWorkflowRequestAll(RunData rundata,
      Context context) {
    String requestid =
      ALEipUtils.getTemp(rundata, context, ALEipConstants.ENTITY_ID);

    try {
      if (requestid == null || Integer.valueOf(requestid) == null) {
        // Request IDが空の場合
        logger.debug("[WorkflowUtils] Empty ID...");
        return null;
      }

      SelectQuery<EipTWorkflowRequest> query =
        Database.query(EipTWorkflowRequest.class);
      Expression exp1 =
        ExpressionFactory.matchDbExp(
          EipTWorkflowRequest.REQUEST_ID_PK_COLUMN,
          requestid);
      query.setQualifier(exp1);

      List<EipTWorkflowRequest> requests = query.fetchList();
      if (requests == null || requests.size() == 0) {
        // 指定した Request IDのレコードが見つからない場合
        logger.debug("[WorkflowUtils] Not found ID...");
        return null;
      }
      return requests.get(0);
    } catch (Exception ex) {
      logger.error("workflow", ex);
      return null;
    }
  }

  /**
   * Request オブジェクトモデルのIDを取得します。 <BR>
   *
   * @param rundata
   * @param context
   * @return
   */
  public static Integer getEipTWorkflowRequestForOwnerId(RunData rundata,
      Context context) {
    String requestid =
      ALEipUtils.getTemp(rundata, context, ALEipConstants.ENTITY_ID);
    Integer id = null;
    if (requestid == null || (id = Integer.valueOf(requestid)) == null) {
      // Todo IDが空の場合
      logger.debug("[WorkflowUtils] Empty ID...");
      return null;
    } else {
      return id;
    }
  }

  /**
   * Request オブジェクトモデルを取得します。 <BR>
   * 
   * @param rundata
   * @param context
   * @param mode_update
   * @return
   */
  public static EipTWorkflowRequest getEipTWorkflowRequestForOwner(
      RunData rundata, Context context) {
    String requestid =
      ALEipUtils.getTemp(rundata, context, ALEipConstants.ENTITY_ID);
    try {
      if (requestid == null || Integer.valueOf(requestid) == null) {
        // Request IDが空の場合
        logger.debug("[WorkflowUtils] Empty ID...");
        return null;
      }

      SelectQuery<EipTWorkflowRequest> query =
        Database.query(EipTWorkflowRequest.class);
      Expression exp1 =
        ExpressionFactory.matchDbExp(
          EipTWorkflowRequest.REQUEST_ID_PK_COLUMN,
          requestid);
      query.setQualifier(exp1);

      Expression exp2 =
        ExpressionFactory.matchExp(
          EipTWorkflowRequest.USER_ID_PROPERTY,
          Integer.valueOf(ALEipUtils.getUserId(rundata)));
      query.andQualifier(exp2);

      List<EipTWorkflowRequest> requests = query.fetchList();

      if (requests == null || requests.size() == 0) {
        // 指定した Request IDのレコードが見つからない場合
        logger.debug("[WorkflowUtils] Not found ID...");
        return null;
      }
      return requests.get(0);
    } catch (Exception ex) {
      logger.error("workflow", ex);
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
  public static EipTWorkflowFile getEipTWorkflowFile(RunData rundata)
      throws ALPageNotFoundException, ALDBErrorException {
    try {
      int attachmentIndex =
        rundata.getParameters().getInt("attachmentIndex", -1);
      if (attachmentIndex < 0) {
        // ID が空の場合
        logger.debug("[WorkflowUtils] Empty ID...");
        throw new ALPageNotFoundException();

      }

      SelectQuery<EipTWorkflowFile> query =
        Database.query(EipTWorkflowFile.class);
      Expression exp =
        ExpressionFactory.matchDbExp(
          EipTWorkflowFile.FILE_ID_PK_COLUMN,
          Integer.valueOf(attachmentIndex));
      query.andQualifier(exp);
      List<EipTWorkflowFile> files = query.fetchList();
      if (files == null || files.size() == 0) {
        // 指定した ID のレコードが見つからない場合
        logger.debug("[WorkflowUtils] Not found ID...");
        throw new ALPageNotFoundException();
      }
      return files.get(0);
    } catch (Exception ex) {
      logger.error("[WorkflowUtils]", ex);
      throw new ALDBErrorException();
    }
  }

  public static void deleteFiles(int timelineId, String orgId, int uid,
      List<String> fpaths) throws ALFileNotRemovedException {
    ALDeleteFileUtil.deleteFiles(
      timelineId,
      EipTWorkflowFile.EIP_TWORKFLOW_REQUEST_PROPERTY,
      getSaveDirPath(orgId, uid),
      fpaths,
      EipTWorkflowFile.class);
  }

  public static List<EipTWorkflowRequestMap> getEipTWorkflowRequestMap(
      EipTWorkflowRequest request) {
    try {
      SelectQuery<EipTWorkflowRequestMap> query =
        Database.query(EipTWorkflowRequestMap.class);
      Expression exp =
        ExpressionFactory.matchDbExp(
          EipTWorkflowRequestMap.EIP_TWORKFLOW_REQUEST_PROPERTY
            + "."
            + EipTWorkflowRequest.REQUEST_ID_PK_COLUMN,
          request.getRequestId());
      query.setQualifier(exp);
      query.orderAscending(EipTWorkflowRequestMap.ORDER_INDEX_PROPERTY);

      List<EipTWorkflowRequestMap> maps = query.fetchList();

      if (maps == null || maps.size() == 0) {
        // 指定した Request IDのレコードが見つからない場合
        logger.debug("[WorkflowSelectData] Not found ID...");
        return null;
      }
      return maps;
    } catch (Exception ex) {
      logger.error("workflow", ex);
      return null;
    }
  }

  /**
   * ワークフローカテゴリ オブジェクトモデルを取得します。 <BR>
   * 
   * @param rundata
   * @param context
   * @param mode_update
   * @return
   */
  public static EipTWorkflowCategory getEipTWorkflowCategory(RunData rundata,
      Context context) {
    String categoryid =
      ALEipUtils.getTemp(rundata, context, ALEipConstants.ENTITY_ID);
    try {
      if (categoryid == null || Integer.valueOf(categoryid) == null) {
        // Request IDが空の場合
        logger.debug("[WorkflowUtils] Empty ID...");
        return null;
      }

      SelectQuery<EipTWorkflowCategory> query =
        Database.query(EipTWorkflowCategory.class);
      Expression exp1 =
        ExpressionFactory.matchDbExp(
          EipTWorkflowCategory.CATEGORY_ID_PK_COLUMN,
          categoryid);
      query.setQualifier(exp1);

      List<EipTWorkflowCategory> categories = query.fetchList();

      if (categories == null || categories.size() == 0) {
        // 指定したカテゴリIDのレコードが見つからない場合
        logger.debug("[WorkflowUtils] Not found ID...");
        return null;
      }
      return categories.get(0);
    } catch (Exception ex) {
      logger.error("workflow", ex);
      return null;
    }
  }

  /**
   * ワークフローカテゴリ オブジェクトモデルを取得します。 <BR>
   * 
   * @param rundata
   * @param context
   * @return
   */
  public static EipTWorkflowCategory getEipTWorkflowCategory(Long category_id) {
    try {
      EipTWorkflowCategory category =
        Database.get(EipTWorkflowCategory.class, category_id);

      return category;
    } catch (Exception ex) {
      logger.error("workflow", ex);
      return null;
    }
  }

  /**
   * カテゴリの一覧を取得する。
   * 
   * @param rundata
   * @param context
   */
  public static List<WorkflowCategoryResultData> loadCategoryList(
      RunData rundata, Context context) {
    try {
      // カテゴリ一覧
      List<WorkflowCategoryResultData> categoryList =
        new ArrayList<WorkflowCategoryResultData>();

      SelectQuery<EipTWorkflowCategory> query1 =
        Database.query(EipTWorkflowCategory.class);
      Expression exp1 =
        ExpressionFactory.matchDbExp(
          EipTWorkflowCategory.CATEGORY_ID_PK_COLUMN,
          1);
      query1.setQualifier(exp1);
      List<EipTWorkflowCategory> aList1 = query1.fetchList();

      for (EipTWorkflowCategory record : aList1) {
        WorkflowCategoryResultData rd = new WorkflowCategoryResultData();
        rd.initField();
        rd.setCategoryId(record.getCategoryId().longValue());
        rd.setCategoryName(record.getCategoryName());
        rd.setOrderTemplate(record.getTemplate());
        categoryList.add(rd);
      }

      SelectQuery<EipTWorkflowCategory> query =
        Database.query(EipTWorkflowCategory.class);
      Expression exp2 =
        ExpressionFactory.noMatchDbExp(
          EipTWorkflowCategory.CATEGORY_ID_PK_COLUMN,
          1);
      query.setQualifier(exp2);
      query.orderAscending(EipTWorkflowCategory.CATEGORY_NAME_PROPERTY);
      List<EipTWorkflowCategory> aList = query.fetchList();

      for (EipTWorkflowCategory record : aList) {
        WorkflowCategoryResultData rd = new WorkflowCategoryResultData();
        rd.initField();
        rd.setCategoryId(record.getCategoryId().longValue());
        rd.setCategoryName(record.getCategoryName());
        rd.setOrderTemplate(record.getTemplate());
        categoryList.add(rd);
      }

      return categoryList;
    } catch (Exception ex) {
      logger.error("workflow", ex);
      return null;
    }
  }

  /**
   * ワークフロー申請経路 オブジェクトモデルを取得します。 <BR>
   * 
   * @param rundata
   * @param context
   * @param mode_update
   * @return
   */
  public static EipTWorkflowRoute getEipTWorkflowRoute(RunData rundata,
      Context context) {
    String routeid =
      ALEipUtils.getTemp(rundata, context, ALEipConstants.ENTITY_ID);
    try {
      if (routeid == null || Integer.valueOf(routeid) == null) {
        // Request IDが空の場合
        logger.debug("[WorkflowUtils] Empty ID...");
        return null;
      }

      SelectQuery<EipTWorkflowRoute> query =
        Database.query(EipTWorkflowRoute.class);
      Expression exp1 =
        ExpressionFactory.matchDbExp(
          EipTWorkflowRoute.ROUTE_ID_PK_COLUMN,
          routeid);
      query.setQualifier(exp1);

      List<EipTWorkflowRoute> routes = query.fetchList();

      if (routes == null || routes.size() == 0) {
        // 指定したカテゴリIDのレコードが見つからない場合
        logger.debug("[WorkflowUtils] Not found ID...");
        return null;
      }
      return routes.get(0);
    } catch (Exception ex) {
      logger.error("workflow", ex);
      return null;
    }
  }

  /**
   * ワークフロー申請経路 オブジェクトモデルを取得します。 <BR>
   * 
   * @param rundata
   * @param context
   * @return
   */
  public static EipTWorkflowRoute getEipTWorkflowRoute(Long route_id) {
    try {
      EipTWorkflowRoute route = Database.get(EipTWorkflowRoute.class, route_id);

      return route;
    } catch (Exception ex) {
      logger.error("workflow", ex);
      return null;
    }
  }

  /**
   * 申請経路の一覧を取得する。
   * 
   * @param rundata
   * @param context
   */
  public static List<WorkflowRouteResultData> loadRouteList(RunData rundata,
      Context context) {
    try {
      // 申請経路一覧
      List<WorkflowRouteResultData> routeList =
        new ArrayList<WorkflowRouteResultData>();

      SelectQuery<EipTWorkflowRoute> query =
        Database.query(EipTWorkflowRoute.class);
      query.orderAscending(EipTWorkflowRoute.ROUTE_NAME_PROPERTY);
      List<EipTWorkflowRoute> aList = query.fetchList();

      for (EipTWorkflowRoute record : aList) {
        WorkflowRouteResultData rd = new WorkflowRouteResultData();
        rd.initField();
        rd.setRouteId(record.getRouteId().longValue());
        rd.setRouteName(record.getRouteName());
        rd.setRoute(record.getRoute());
        routeList.add(rd);
      }

      return routeList;
    } catch (Exception ex) {
      logger.error("workflow", ex);
      return null;
    }
  }

  /**
   * 依頼の詳細情報を取得する。
   *
   * @param rundata
   * @param context
   */
  @Deprecated
  public static Object getResultDataDetail(Object obj, ALEipUser login_user) {
    return getResultDataDetail(obj, login_user, false);
  }

  /**
   * 依頼の詳細情報を取得する。
   *
   * @param rundata
   * @param context
   */
  public static Object getResultDataDetail(Object obj, ALEipUser login_user,
      boolean hasAttachmentAuthority) {
    try {
      EipTWorkflowRequest record = (EipTWorkflowRequest) obj;
      WorkflowDetailResultData rd = new WorkflowDetailResultData();
      rd.initField();
      rd.setUserId(record.getUserId().longValue());
      rd.setRequestName(record.getRequestName());
      rd.setRequestId(record.getRequestId().longValue());
      rd.setCategoryId(record
        .getEipTWorkflowCategory()
        .getCategoryId()
        .longValue());
      rd.setCategoryName(record.getEipTWorkflowCategory().getCategoryName());
      rd.setPriorityString(WorkflowUtils.getPriorityString(record
        .getPriority()
        .intValue()));
      rd.setNote(record.getNote());
      rd.setPrice(record.getPrice().longValue());
      rd.setProgress(record.getProgress());
      rd.setCanRemandApplicant(true);
      if (record.getEipTWorkflowRoute() != null) {
        rd.setRouteName(record.getEipTWorkflowRoute().getRouteName());
      }

      List<WorkflowDecisionRecordData> drList =
        new ArrayList<WorkflowDecisionRecordData>();
      List<WorkflowDecisionRecordData> remandList =
        new ArrayList<WorkflowDecisionRecordData>();
      ALEipUser user = null;
      EipTWorkflowRequestMap map = null;
      WorkflowDecisionRecordData drd = null;
      List<EipTWorkflowRequestMap> maps =
        WorkflowUtils.getEipTWorkflowRequestMap(record);
      int size = maps.size();
      boolean is_past = true;
      for (int i = 0; i < size; i++) {
        map = maps.get(i);
        drd = new WorkflowDecisionRecordData();
        drd.initField();

        // ログインユーザより後なら差し戻し先として選ばない
        if (login_user.getUserId().getValue() == map.getUserId().longValue()) {
          is_past = false;
        }

        user = ALEipUtils.getALEipUser(map.getUserId().intValue());
        drd.setUserId(map.getUserId().intValue());
        drd.setUserAliasName(user.getAliasName().getValue());
        drd.setStatus(map.getStatus());
        drd.setStatusString(WorkflowUtils.getStatusString(map.getStatus()));
        drd.setOrder(map.getOrderIndex().intValue());
        drd.setNote(map.getNote());
        drd.setUpdateDate(WorkflowUtils.translateDate(
          map.getUpdateDate(),
          ALLocalizationUtils.getl10n("WORKFLOW_YEAR_MONTH_DAY_HOUR_MINIT")));
        drList.add(drd);

        // 無効化、もしくは削除されているユーザーは差し戻し先として選べないようにする
        if (is_past && !isDisabledOrDeleted(map.getUserId().toString())) {
          remandList.add(drd);
        } else {
          // 申請者に差し戻し不可能
          if (i == 0) {
            rd.setCanRemandApplicant(false);
          }
        }
      }
      rd.setDecisionRecords(drList);
      rd.setRemandingRecords(remandList);

      // 過去の申請内容
      if (record.getParentId().intValue() != 0) {
        List<EipTWorkflowRequest> oldReuqests = getOldRequests(record);
        if (oldReuqests != null && oldReuqests.size() > 0) {
          List<WorkflowOldRequestResultData> oldList =
            new ArrayList<WorkflowOldRequestResultData>();
          int osize = oldReuqests.size();
          for (int i = 0; i < osize; i++) {
            EipTWorkflowRequest request = oldReuqests.get(i);
            WorkflowOldRequestResultData orrd =
              new WorkflowOldRequestResultData();
            orrd.initField();
            orrd.setRequestId(request.getRequestId().intValue());
            orrd.setRequestName(request.getRequestName());
            orrd.setCategoryName(request
              .getEipTWorkflowCategory()
              .getCategoryName());
            orrd.setUpdateDate(WorkflowUtils.translateDate(request
              .getUpdateDate(), ALLocalizationUtils
              .getl10n("WORKFLOW_YEAR_MONTH_DAY_HOUR_MINIT")));
            oldList.add(orrd);
          }
          rd.setOldRequestLinks(oldList);
        }
      }

      if (hasAttachmentAuthority) {
        // ファイルリスト
        List<EipTWorkflowFile> list =
          getSelectQueryForFiles(record.getRequestId().intValue()).fetchList();
        if (list != null && list.size() > 0) {
          List<FileuploadBean> attachmentFileList =
            new ArrayList<FileuploadBean>();
          FileuploadBean filebean = null;
          for (EipTWorkflowFile file : list) {
            String realname = file.getFileName();
            javax.activation.DataHandler hData =
              new javax.activation.DataHandler(
                new javax.activation.FileDataSource(realname));

            filebean = new FileuploadBean();
            filebean.setFileId(file.getFileId().intValue());
            filebean.setFileName(realname);
            if (hData != null) {
              filebean.setContentType(hData.getContentType());
            }
            filebean.setIsImage(FileuploadUtils.isImage(realname));
            attachmentFileList.add(filebean);
          }
          rd.setAttachmentFiles(attachmentFileList);
        }
      }

      rd.setCreateDate(WorkflowUtils.translateDate(
        record.getCreateDate(),
        ALLocalizationUtils.getl10n("WORKFLOW_YEAR_MONTH_DAY_HOUR_MINIT")));
      rd.setUpdateDate(WorkflowUtils.translateDate(
        record.getUpdateDate(),
        ALLocalizationUtils.getl10n("WORKFLOW_YEAR_MONTH_DAY_HOUR_MINIT")));
      return rd;
    } catch (Exception ex) {
      logger.error("workflow", ex);
      return null;
    }
  }

  private static List<EipTWorkflowRequest> getOldRequests(
      EipTWorkflowRequest request) {
    try {
      SelectQuery<EipTWorkflowRequest> query =
        Database.query(EipTWorkflowRequest.class);
      Expression exp11 =
        ExpressionFactory.matchDbExp(
          EipTWorkflowRequest.REQUEST_ID_PK_COLUMN,
          request.getParentId());
      Expression exp12 =
        ExpressionFactory.matchExp(
          EipTWorkflowRequest.PARENT_ID_PROPERTY,
          request.getParentId());
      query.setQualifier(exp11.orExp(exp12));
      Expression exp2 =
        ExpressionFactory.noMatchDbExp(
          EipTWorkflowRequest.REQUEST_ID_PK_COLUMN,
          request.getRequestId());
      query.andQualifier(exp2);

      query.orderAscending(EipTWorkflowRequest.UPDATE_DATE_PROPERTY);

      List<EipTWorkflowRequest> requests = query.fetchList();

      if (requests == null || requests.size() == 0) {
        // 指定した Request IDのレコードが見つからない場合
        logger.debug("[WorkflowSelectData] Not found ID...");
        return null;
      }
      return requests;
    } catch (Exception ex) {
      logger.error("workflow", ex);
      return null;
    }
  }

  /**
   * 重要度を表す画像名を取得します。 <BR>
   * 1 : 高い : priority_high.gif <BR>
   * 2 : やや高い : priority_middle_high.gif <BR>
   * 3 : 普通 : priority_middle.gif <BR>
   * 4 : やや低い : priority_middle_low.gif <BR>
   * 5 : 低い : priority_low.gif <BR>
   * 
   * @param i
   * @return
   */
  public static String getPriorityImage(int i) {
    String[] temp =
      {
        "priority_high.gif",
        "priority_middle_high.gif",
        "priority_middle.gif",
        "priority_middle_low.gif",
        "priority_low.gif" };
    String image = null;
    try {
      image = temp[i - 1];
    } catch (Exception ex) {
      // logger.error("Exeption", ex);
    }
    return image;
  }

  /**
   * 重要度を表す文字列を取得します。 <BR>
   * 1 : 高い : priority_high.gif <BR>
   * 2 : やや高い : priority_middle_high.gif <BR>
   * 3 : 普通 : priority_middle.gif <BR>
   * 4 : やや低い : priority_middle_low.gif <BR>
   * 5 : 低い : priority_low.gif <BR>
   * 
   * @param i
   * @return
   */
  public static String getPriorityString(int i) {
    String[] temp =
      {
        ALLocalizationUtils.getl10n("WORKFLOW_HIGH"),
        ALLocalizationUtils.getl10n("WORKFLOW_FEW_HIGH"),
        ALLocalizationUtils.getl10n("WORKFLOW_USUALLY"),
        ALLocalizationUtils.getl10n("WORKFLOW_FEW_LOW"),
        ALLocalizationUtils.getl10n("WORKFLOW_LOW") };
    String string = null;
    try {
      string = temp[i - 1];
    } catch (Exception ex) {
      // logger.error("Exeption", ex);
    }
    return string;
  }

  /**
   * 状態を表す画像名を取得します。 <BR>
   * 0 : 未着手 <BR>
   * 10 : 10% <BR>
   * 20 : 20% <BR>
   * : :<BR>
   * 90 : 90% <BR>
   * 100 : 完了 <BR>
   * 
   * @param i
   * @return
   */
  public static String getStateImage(int i) {
    String[] temp =
      {
        "state_000.gif",
        "state_010.gif",
        "state_020.gif",
        "state_030.gif",
        "state_040.gif",
        "state_050.gif",
        "state_060.gif",
        "state_070.gif",
        "state_080.gif",
        "state_090.gif",
        "state_100.gif" };
    String image = null;
    try {
      image = temp[i / 10];
    } catch (Exception ex) {
      // logger.error("Exeption", ex);
    }
    return image;
  }

  /**
   * 状態を表す文字列を取得します。 <BR>
   * 0 : 未着手 <BR>
   * 10 : 10% <BR>
   * 20 : 20% <BR>
   * : :<BR>
   * 90 : 90% <BR>
   * 100 : 完了 <BR>
   * 
   * @param i
   * @return
   */
  public static String getStateString(int i) {
    if (i == 0) {
      return ALLocalizationUtils.getl10n("TODO_NOT_START");
    } else if (i == 100) {
      return ALLocalizationUtils.getl10n("TODO_FINISHING");
    } else {
      return new StringBuffer().append(i).append("%").toString();
    }
  }

  /**
   * 
   * @param status
   */
  public static String getStatusString(String status) {
    String res = "";
    if (DB_STATUS_REQUEST.equals(status)) {
      res = ALLocalizationUtils.getl10n("WORKFLOW_APPLICATION");
    } else if (DB_STATUS_CONFIRM.equals(status)) {
      res = ALLocalizationUtils.getl10n("WORKFLOW_CHECKING");
    } else if (DB_STATUS_WAIT.equals(status)) {
      res = ALLocalizationUtils.getl10n("WORKFLOW_CHECKING_BEFORE");
    } else if (DB_STATUS_ACCEPT.equals(status)) {
      res = ALLocalizationUtils.getl10n("WORKFLOW_APPROVAL");
    } else if (DB_STATUS_DENIAL.equals(status)) {
      res = ALLocalizationUtils.getl10n("WORKFLOW_DENIAL");
    } else if (DB_STATUS_THROUGH.equals(status)) {
      res = ALLocalizationUtils.getl10n("WORKFLOW_APPROVAL_AUTO");
    }
    return res;
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
    if (dateFormat == null) {
      dateFormat = ALDateTimeField.DEFAULT_DATE_TIME_FORMAT;
    }

    // 日付を表示形式に変換
    SimpleDateFormat sdf = new SimpleDateFormat(dateFormat);
    sdf.setTimeZone(TimeZone.getDefault());
    return sdf.format(date);
  }

  /**
   * 3 桁でカンマ区切りした文字列を取得する．
   * 
   * @param money
   * @return
   */
  public static String translateMoneyStr(String money) {
    if (money == null || money.length() == 0) {
      return money;
    }

    StringBuffer sb = new StringBuffer();
    int len = money.length();
    int count = len / 3;
    int del = len % 3;
    sb.append(money.substring(0, del));
    if (count > 0) {
      if (len > 3 && del != 0) {
        sb.append(",");
      }
      for (int i = 0; i < count; i++) {
        sb.append(money.substring(del + 3 * i, del + 3 * i + 3));
        if (i != count - 1) {
          sb.append(",");
        }
      }
    }
    return sb.toString();
  }

  /**
   * メール送信
   */
  public static boolean sendMail(RunData rundata, EipTWorkflowRequest request,
      List<ALEipUser> destUsers, List<String> msgList) throws Exception {

    String orgId = Database.getDomainName();
    String subject =
      "["
        + ALOrgUtilsService.getAlias()
        + "]"
        + ALLocalizationUtils.getl10n("WORKFLOW_WORKFLOW");

    try {
      List<ALEipUser> memberList = new ArrayList<ALEipUser>();
      memberList.addAll(destUsers);
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
        // 申請時はcommentは空
        message.setPcBody(WorkflowUtils.createMsgForPc(rundata, request, ""));
        message.setCellularBody(WorkflowUtils.createMsgForCellPhone(
          rundata,
          request));
        messageList.add(message);
      }

      ALMailService.sendAdminMailAsync(new ALAdminMailContext(orgId, ALEipUtils
        .getUserId(rundata), messageList, ALMailUtils
        .getSendDestType(ALMailUtils.KEY_MSGTYPE_WORKFLOW)));
      // msgList.addAll(errors);

    } catch (Exception ex) {
      logger.error("workflow", ex);
      return false;
    }
    return true;
  }

  public static boolean sendMailForUpdate(RunData rundata,
      List<EipTWorkflowRequestMap> sendMailMaps, EipTWorkflowRequest request,
      String comment, Type flowStatus) {

    String orgId = Database.getDomainName();

    List<ALEipUserAddr> userAddressList = new ArrayList<ALEipUserAddr>();

    WorkflowMailBean mailBean = new WorkflowMailBean();
    mailBean.setOrgId(Database.getDomainName());
    mailBean.setSubject("["
      + ALOrgUtilsService.getAlias()
      + "]"
      + ALLocalizationUtils.getl10n("WORKFLOW_WORKFLOW"));
    mailBean.setLoginUserId(ALEipUtils.getUserId(rundata));
    mailBean.setAipoAlias(ALOrgUtilsService.getAlias());
    mailBean.setGlobalUrl(ALMailUtils.getGlobalurl());
    mailBean.setLocalUrl(ALMailUtils.getLocalurl());

    String msgForPcPrefix =
      createMsgForPcAtUpdatePrefix(request, comment, flowStatus, mailBean);
    String msgForCellPrefix =
      createMsgForCellAtUpdatePrefix(request, comment, flowStatus, mailBean);

    if ("".equals(msgForPcPrefix) || "".equals(msgForCellPrefix)) {
      return false;
    }

    switch (flowStatus) {
      case REQUEST:
        for (EipTWorkflowRequestMap requestMap : sendMailMaps) {
          userAddressList.add(ALMailUtils.getALEipUserAddrByUserId(requestMap
            .getUserId()));
        }
        break;
      case DENAIL:
        for (EipTWorkflowRequestMap requestMap : sendMailMaps) {
          userAddressList.add(ALMailUtils.getALEipUserAddrByUserId(requestMap
            .getUserId()));
        }
        break;
      case ACCEPT:
        userAddressList.add(ALMailUtils.getALEipUserAddrByUserId(request
          .getUserId()));
        break;
      default:

    }

    try {

      List<ALAdminMailMessage> messageList =
        new ArrayList<ALAdminMailMessage>();
      for (ALEipUserAddr userAddr : userAddressList) {

        ALAdminMailMessage message = new ALAdminMailMessage(userAddr);
        message.setPcSubject(mailBean.getSubject());
        message.setCellularSubject(mailBean.getSubject());
        message.setPcBody(createMsgForPc(rundata, request, comment));
        message.setCellularBody(createMsgForPc(rundata, request, comment));
        messageList.add(message);

      }

      ALMailService.sendAdminMailAsync(new ALAdminMailContext(orgId, ALEipUtils
        .getUserId(rundata), messageList, ALMailUtils
        .getSendDestType(ALMailUtils.KEY_MSGTYPE_WORKFLOW)));
      // msgList.addAll(errors);

    } catch (Exception e) {
      logger.error("[WorkflowUtils]", e);
      return false;
    }

    return true;
  }

  @Deprecated
  public static boolean sendMailForUpdateCore(
      List<EipTWorkflowRequestMap> sendMailMaps, EipTWorkflowRequest request,
      Type flowStatus, WorkflowMailBean mailBean) {

    List<String> msgList = new ArrayList<String>();

    List<ALEipUserAddr> userAddressList = new ArrayList<ALEipUserAddr>();

    String msgForPcPrefix =
      createMsgForPcAtUpdatePrefix(request, "", flowStatus, mailBean);
    String msgForCellPrefix =
      createMsgForCellAtUpdatePrefix(request, "", flowStatus, mailBean);

    if ("".equals(msgForPcPrefix) || "".equals(msgForCellPrefix)) {
      return false;
    }

    for (EipTWorkflowRequestMap requestMap : sendMailMaps) {
      userAddressList.add(ALMailUtils.getALEipUserAddrByUserId(requestMap
        .getUserId()));
    }

    try {
      for (ALEipUserAddr userAddr : userAddressList) {
        String msgForPc = msgForPcPrefix;
        String msgForCell = msgForCellPrefix;
        ALMailUtils.sendMailDelegateOne(mailBean.getOrgId(), mailBean
          .getLoginUserId(), userAddr, mailBean.getSubject(), mailBean
          .getSubject(), msgForPc, msgForCell, ALMailUtils
          .getSendDestType(ALMailUtils.KEY_MSGTYPE_WORKFLOW), msgList);
      }
    } catch (Exception e) {
      logger.error("[WorkflowUtils]", e);
      return false;
    }

    return true;
  }

  public static String createMsgForPcAtUpdatePrefix(
      EipTWorkflowRequest request, String comment, Type flowStatus,
      WorkflowMailBean mailBean) {

    ALEipUser user;
    try {
      user = ALEipUtils.getALEipUser(request.getUserId());
    } catch (ALDBErrorException e) {
      logger.error("[WorkflowUtils]", e);
      user = new ALEipUser();
    }

    try {
      StringBuilder body = new StringBuilder("");
      body.append(user.getAliasName().toString());
      body.append(getMessageHead(flowStatus, ALMailUtils.CR));
      body.append(getMessageContent(
        request,
        comment,
        ALMailUtils.CR,
        false,
        mailBean));
      return body.toString();
    } catch (Exception e) {
      logger.error("[WorkflowUtils]", e);
      return "";
    }

  }

  public static String createMsgForCellAtUpdatePrefix(
      EipTWorkflowRequest request, String comment, Type flowStatus,
      WorkflowMailBean mailBean) {

    ALEipUser user;
    try {
      user = ALEipUtils.getALEipUser(request.getUserId());
    } catch (ALDBErrorException e) {
      logger.error("[WorkflowUtils]", e);
      user = new ALEipUser();
    }

    try {
      StringBuilder body = new StringBuilder("");
      body.append(user.getAliasName().toString());
      body.append(getMessageHead(flowStatus, ALMailUtils.CR));
      body.append(getMessageContent(
        request,
        comment,
        ALMailUtils.CR,
        true,
        mailBean));
      return body.toString();
    } catch (Exception e) {
      logger.error("[WorkflowUtils]", e);
      return "";
    }
  }

  public static String getMessageHead(Type flowStatus, String CR)
      throws Exception {
    StringBuilder body = new StringBuilder("");

    switch (flowStatus) {
      case REQUEST:
        body
          .append(ALLocalizationUtils.getl10n("WORKFLOW_RECEIVE_REQUEST_MSG"))
          .append(CR)
          .append(CR);
        body
          .append("[")
          .append(ALLocalizationUtils.getl10n("WORKFLOW_FLOW_STATUS"))
          .append("]")
          .append(CR);
        body
          .append(ALLocalizationUtils.getl10n("WORKFLOW_WAITING_DECISION"))
          .append(CR);
        break;
      case DENAIL:
        body
          .append(ALLocalizationUtils.getl10n("WORKFLOW_RECEIVE_DENIAL_MSG"))
          .append(CR)
          .append(CR);
        body
          .append("[")
          .append(ALLocalizationUtils.getl10n("WORKFLOW_FLOW_STATUS"))
          .append("]")
          .append(CR);
        body
          .append(
            ALLocalizationUtils.getl10n("WORKFLOW_NEED_TO_CHECK_PASSBACK"))
          .append(CR);
        break;
      case ACCEPT:
        body
          .append(ALLocalizationUtils.getl10n("WORKFLOW_RECEIVE_ACCEPT_MSG"))
          .append(CR)
          .append(CR);
        body
          .append("[")
          .append(ALLocalizationUtils.getl10n("WORKFLOW_FLOW_STATUS"))
          .append("]")
          .append(CR);
        body.append(ALLocalizationUtils.getl10n("WORKFLOW_CONFIRMED")).append(
          CR);
        break;
      default:

    }

    return body.toString();
  }

  // [タイトル][申請日][申請内容][コメント]
  public static String getMessageContent(EipTWorkflowRequest request,
      String comment, String CR, boolean isCell, WorkflowMailBean mailBean) {

    StringBuilder body = new StringBuilder("");

    body.append("[").append(
      ALLocalizationUtils.getl10n("WORKFLOW_REQUEST_NAME")).append("]").append(
      CR);
    body.append(request.getEipTWorkflowCategory().getCategoryName()).append(CR);

    if (request.getRequestName() != null
      && (!"".equals(request.getRequestName()))) {
      body.append(request.getRequestName()).append(CR);
    }

    body
      .append("[")
      .append(ALLocalizationUtils.getl10n("WORKFLOW_CREATEDATE"))
      .append("]")
      .append(CR)
      .append(
        WorkflowUtils.translateDate(
          request.getCreateDate(),
          ALLocalizationUtils.getl10n("WORKFLOW_YEAR_MONTH_DAY_HOUR_MINIT")))
      .append(CR);

    body
      .append("[")
      .append(ALLocalizationUtils.getl10n("WORKFLOW_PRIORITY_VALUE"))
      .append("]")
      .append(CR)
      .append(WorkflowUtils.getPriorityString(request.getPriority().intValue()))
      .append(CR);

    if (!isCell) {
      body
        .append("[")
        .append(ALLocalizationUtils.getl10n("WORKFLOW_ACCESS_TO"))
        .append("]")
        .append(CR)
        .append(request.getNote());

      if (comment != null && !"".equals(comment)) {
        body.append(CR).append("[").append(
          ALLocalizationUtils.getl10n("WORKFLOW_COMMENT")).append("]").append(
          CR).append(comment);
      }
    }
    return body.toString();
  }

  /**
   * パソコンへ送信するメールの内容を作成する．
   * 
   * @return
   */
  public static String createMsgForPc(RunData rundata,
      EipTWorkflowRequest request, String comment) {
    VelocityContext context = new VelocityContext();
    boolean enableAsp = JetspeedResources.getBoolean("aipo.asp", false);
    String CR = ALMailUtils.CR;

    WorkflowMailBean mailBean = new WorkflowMailBean();
    mailBean.setOrgId(Database.getDomainName());
    mailBean.setSubject("["
      + ALOrgUtilsService.getAlias()
      + "]"
      + ALLocalizationUtils.getl10n("WORKFLOW_WORKFLOW"));
    mailBean.setLoginUserId(ALEipUtils.getUserId(rundata));
    mailBean.setAipoAlias(ALOrgUtilsService.getAlias());
    mailBean.setGlobalUrl(ALMailUtils.getGlobalurl());
    mailBean.setLocalUrl(ALMailUtils.getLocalurl());

    TurbineUser tuser = null;
    ALEipUser auser = null;

    try {
      tuser = request.getTurbineUser();
      auser = ALEipUtils.getALEipUser(tuser);
    } catch (ALDBErrorException e) {
      logger.error("[WorkflowUtils]", e);
      return "";
    }
    // 依頼者とemail
    StringBuffer user_email = new StringBuffer("");
    user_email.append(auser.getAliasName().toString());
    if (!tuser.getEmail().equals("")) {
      user_email.append("(").append(tuser.getEmail()).append(")");
    }
    context.put("user_email", user_email);

    // （さんの申請は承認されました。など）
    StringBuffer message = new StringBuffer("");
    if ("D".equals(request.getProgress())) {
      message
        .append(ALLocalizationUtils.getl10n("WORKFLOW_RECEIVE_DENIAL_MSG"));
    } else if ("A".equals(request.getProgress())) {
      message
        .append(ALLocalizationUtils.getl10n("WORKFLOW_RECEIVE_ACCEPT_MSG"));
    } else {
      message.append(ALLocalizationUtils
        .getl10n("WORKFLOW_RECEIVE_REQUEST_MSG"));
    }
    context.put("message", message);

    // 決裁状況
    StringBuffer progress = new StringBuffer("");
    if ("D".equals(request.getProgress())) {
      progress.append(ALLocalizationUtils
        .getl10n("WORKFLOW_NEED_TO_CHECK_PASSBACK"));
    } else if ("A".equals(request.getProgress())) {
      progress.append(ALLocalizationUtils.getl10n("WORKFLOW_CONFIRMED"));
    } else {
      progress.append(ALLocalizationUtils.getl10n("WORKFLOW_WAITING_DECISION"));
    }
    context.put("progress", progress);

    // タイトル,申請日,重要度,申請内容,コメント
    context.put("content", getMessageContent(
      request,
      comment,
      CR,
      false,
      mailBean));
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
            + "/workflow-notification-mail.vm", "utf-8");
        template.merge(context, writer);
      } else {
        Template template =
          Velocity.getTemplate(
            "portlets/mail/workflow-notification-mail.vm",
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
      EipTWorkflowRequest request) {
    VelocityContext context = new VelocityContext();
    boolean enableAsp = JetspeedResources.getBoolean("aipo.asp", false);
    String CR = ALMailUtils.CR;
    WorkflowMailBean mailBean = new WorkflowMailBean();
    mailBean.setOrgId(Database.getDomainName());
    mailBean.setSubject("["
      + ALOrgUtilsService.getAlias()
      + "]"
      + ALLocalizationUtils.getl10n("WORKFLOW_WORKFLOW"));
    mailBean.setLoginUserId(ALEipUtils.getUserId(rundata));
    mailBean.setAipoAlias(ALOrgUtilsService.getAlias());
    mailBean.setGlobalUrl(ALMailUtils.getGlobalurl());
    mailBean.setLocalUrl(ALMailUtils.getLocalurl());

    TurbineUser tuser = null;
    ALEipUser auser = null;

    try {
      tuser = request.getTurbineUser();
      auser = ALEipUtils.getALEipUser(tuser);
    } catch (ALDBErrorException e) {
      logger.error("[WorkflowUtils]", e);
      return "";
    }
    // 依頼者とemail
    StringBuffer user_email = new StringBuffer("");
    user_email.append(auser.getAliasName().toString());
    if (!tuser.getEmail().equals("")) {
      user_email.append("(").append(tuser.getEmail()).append(")");
    }
    context.put("user_email", user_email);

    // （さんの申請は承認されました。など）
    StringBuffer message = new StringBuffer("");
    if ("D".equals(request.getProgress())) {
      message
        .append(ALLocalizationUtils.getl10n("WORKFLOW_RECEIVE_DENIAL_MSG"));
    } else if ("A".equals(request.getProgress())) {
      message
        .append(ALLocalizationUtils.getl10n("WORKFLOW_RECEIVE_ACCEPT_MSG"));
    } else {
      message.append(ALLocalizationUtils
        .getl10n("WORKFLOW_RECEIVE_REQUEST_MSG"));
    }
    context.put("message", message);

    // 決裁状況
    StringBuffer progress = new StringBuffer("");
    if ("D".equals(request.getProgress())) {
      progress.append(ALLocalizationUtils
        .getl10n("WORKFLOW_NEED_TO_CHECK_PASSBACK"));
    } else if ("A".equals(request.getProgress())) {
      progress.append(ALLocalizationUtils.getl10n("WORKFLOW_CONFIRMED"));
    } else {
      progress.append(ALLocalizationUtils.getl10n("WORKFLOW_WAITING_DECISION"));
    }
    context.put("progress", progress);

    // タイトル,申請日,重要度,申請内容
    context.put("content", getMessageContent(request, "", CR, false, mailBean));
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
            + "/workflow-notification-mail.vm", "utf-8");
        template.merge(context, writer);
      } else {
        Template template =
          Velocity.getTemplate(
            "portlets/mail/workflow-notification-mail.vm",
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

  public static boolean insertFileDataDelegate(RunData rundata,
      Context context, EipTWorkflowRequest request,
      EipTWorkflowRequest oldrequest, List<FileuploadLiteBean> fileuploadList,
      String folderName, List<String> msgList) {

    int uid = ALEipUtils.getUserId(rundata);
    String orgId = Database.getDomainName();
    String[] fileids = rundata.getParameters().getStrings("attachments");

    // fileidsがnullなら、ファイルがアップロードされていないので、trueを返して終了
    if (fileids == null) {
      return true;
    }

    int fileIDsize;
    if (fileids[0].equals("")) {
      fileIDsize = 0;
    } else {
      fileIDsize = fileids.length;
    }
    // 送られてきたFileIDの個数とDB上の当該RequestID中の添付ファイル検索を行った結果の個数が一致したら、
    // 変更が無かったとみなし、trueを返して終了。
    SelectQuery<EipTWorkflowFile> dbquery =
      Database.query(EipTWorkflowFile.class);
    dbquery.andQualifier(ExpressionFactory.matchDbExp(
      EipTWorkflowFile.EIP_TWORKFLOW_REQUEST_PROPERTY,
      request.getRequestId()));
    for (int i = 0; i < fileIDsize; i++) {
      dbquery.orQualifier(ExpressionFactory.matchDbExp(
        EipTWorkflowFile.FILE_ID_PK_COLUMN,
        fileids[i]));
    }
    List<EipTWorkflowFile> files = dbquery.fetchList();
    ;

    if (files.size() == fileIDsize
      && (fileuploadList == null || fileuploadList.size() <= 0)) {
      return true;
    }

    SelectQuery<EipTWorkflowFile> query =
      Database.query(EipTWorkflowFile.class);
    query.andQualifier(ExpressionFactory.matchDbExp(
      EipTWorkflowFile.EIP_TWORKFLOW_REQUEST_PROPERTY,
      request.getRequestId()));
    for (int i = 0; i < fileIDsize; i++) {
      Expression exp =
        ExpressionFactory.matchDbExp(
          EipTWorkflowFile.FILE_ID_PK_COLUMN,
          Integer.parseInt(fileids[i]));
      query.andQualifier(exp.notExp());
    }
    // DB上でトピックに属すが、送られてきたFileIDにIDが含まれていないファイルのリスト(削除されたファイルのリスト)
    List<EipTWorkflowFile> delFiles = query.fetchList();

    if (delFiles.size() > 0) {
      // ローカルファイルに保存されているファイルを削除する．
      int delsize = delFiles.size();
      for (int i = 0; i < delsize; i++) {
        ALStorageService.deleteFile(WorkflowUtils.getSaveDirPath(orgId, uid)
          + (delFiles.get(i)).getFilePath());
      }
      // データベースから添付ファイルのデータ削除
      Database.deleteAll(delFiles);
    }

    // 追加ファイルが無ければtrueを返して終了
    if (fileuploadList == null || fileuploadList.size() <= 0) {
      return true;
    }

    // ファイル追加処理
    try {
      FileuploadLiteBean filebean = null;
      int size = fileuploadList.size();
      for (int i = 0; i < size; i++) {
        filebean = fileuploadList.get(i);

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

        String filename = i + "_" + String.valueOf(System.nanoTime());

        // 新規オブジェクトモデル
        EipTWorkflowFile file = Database.create(EipTWorkflowFile.class);
        // 所有者
        file.setOwnerId(Integer.valueOf(uid));
        // リクエストID
        file.setEipTWorkflowRequest(request);
        // ファイル名
        file.setFileName(filebean.getFileName());
        // ファイルパス
        file.setFilePath(WorkflowUtils.getRelativePath(filename));
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
          .getFileId()), FOLDER_FILEDIR_WORKFLOW, CATEGORY_KEY
          + ALStorageService.separator()
          + uid, filename);
      }

      // 添付ファイル保存先のフォルダを削除
      ALStorageService.deleteTmpFolder(uid, folderName);
    } catch (Exception e) {
      logger.error("workflow", e);
      return false;
    }
    return true;
  }

  /**
   * ユーザ毎のルート保存先（絶対パス）を取得します。
   * 
   * @param uid
   * @return
   */
  public static String getSaveDirPath(String orgId, int uid) {
    return ALStorageService.getDocumentPath(
      FOLDER_FILEDIR_WORKFLOW,
      CATEGORY_KEY + ALStorageService.separator() + uid);
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
          ALAccessControlConstants.POERTLET_FEATURE_WORKFLOW_REQUEST_SELF,
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
   * ファイル検索のクエリを返します
   * 
   * @param requestid
   *          ファイルを検索するリクエストのid
   * @return query
   */
  private static SelectQuery<EipTWorkflowFile> getSelectQueryForFiles(
      int requestid) {
    SelectQuery<EipTWorkflowFile> query =
      Database.query(EipTWorkflowFile.class);
    Expression exp =
      ExpressionFactory.matchDbExp(
        EipTWorkflowRequest.REQUEST_ID_PK_COLUMN,
        Integer.valueOf(requestid));
    query.setQualifier(exp);
    query.orderAscending(EipTWorkflowFile.UPDATE_DATE_PROPERTY);
    query.orderAscending(EipTWorkflowFile.FILE_PATH_PROPERTY);
    return query;
  }

  /**
   * 指定した ID に対するユーザの名前を取得する．
   * 
   * @param userId
   * @return
   */
  public static String getName(String userId) {
    if (userId == null || userId.equals("")) {
      return null;
    }

    String firstName = null;
    String lastName = null;
    StringBuffer buffer = new StringBuffer();

    try {
      SelectQuery<TurbineUser> query = Database.query(TurbineUser.class);
      Expression exp =
        ExpressionFactory.matchDbExp(TurbineUser.USER_ID_PK_COLUMN, Integer
          .valueOf(userId));
      query.setQualifier(exp);
      List<TurbineUser> destUserList = query.fetchList();
      if (destUserList == null || destUserList.size() <= 0) {
        return null;
      }
      firstName = (destUserList.get(0)).getFirstName();
      lastName = (destUserList.get(0)).getLastName();
    } catch (Exception ex) {
      logger.error("workflow", ex);
      return null;
    }
    return buffer.append(lastName).append(" ").append(firstName).toString();
  }

  /**
   * 指定した ID に対するユーザのログイン名を取得する．
   * 
   * @param userId
   * @return
   */
  public static String getUserName(String userId) {
    if (userId == null || userId.equals("")) {
      return null;
    }

    String userName = null;

    try {
      SelectQuery<TurbineUser> query = Database.query(TurbineUser.class);
      Expression exp =
        ExpressionFactory.matchDbExp(TurbineUser.USER_ID_PK_COLUMN, Integer
          .valueOf(userId));
      query.setQualifier(exp);
      List<TurbineUser> destUserList = query.fetchList();
      if (destUserList == null || destUserList.size() <= 0) {
        return null;
      }
      userName = (destUserList.get(0)).getLoginName();
    } catch (Exception ex) {
      logger.error("workflow", ex);
      return null;
    }
    return userName;
  }

  public static Integer getRouteIdFromCategoryId(Integer categoryId) {
    if (categoryId == null || categoryId <= 0) {
      return null;
    }

    Integer routeId = null;

    try {
      SelectQuery<EipTWorkflowCategory> query =
        Database.query(EipTWorkflowCategory.class);
      Expression exp =
        ExpressionFactory.matchDbExp(
          EipTWorkflowCategory.CATEGORY_ID_PK_COLUMN,
          Integer.valueOf(categoryId));
      query.setQualifier(exp);
      List<EipTWorkflowCategory> list = query.fetchList();
      if (list == null || list.size() <= 0) {
        return null;
      }
      routeId = (list.get(0)).getEipTWorkflowRoute().getRouteId();
    } catch (Exception ex) {
      return null;
    }
    return routeId;
  }

  /**
   * 指定した ID のユーザを取得する
   * 
   * @param userId
   * @return
   */
  public static TurbineUser getTurbineUser(String userId) {
    if (userId == null || userId.equals("")) {
      return null;
    }
    try {
      SelectQuery<TurbineUser> query = Database.query(TurbineUser.class);
      Expression exp =
        ExpressionFactory.matchDbExp(TurbineUser.USER_ID_PK_COLUMN, Integer
          .valueOf(userId));
      query.setQualifier(exp);
      List<TurbineUser> destUserList = query.fetchList();
      if (destUserList == null || destUserList.size() <= 0) {
        return null;
      }
      return destUserList.get(0);
    } catch (Exception ex) {
      logger.error("workflow", ex);
      return null;
    }
  }

  /**
   * 指定した ID のユーザが削除済みかどうかを調べる。
   * 
   * @param userId
   * @return
   */
  public static boolean isDisabledOrDeleted(String userId) {
    if (null == userId || "".equals(userId)) {
      return true;
    }
    int _userId = Integer.parseInt(userId);
    return isDisabledOrDeleted(_userId);
  }

  public static boolean isDisabledOrDeleted(int userId) {
    try {
      TurbineUser user = ALEipUtils.getTurbineUser(userId);

      if (user == null) {
        return true;
      }

      String disabled = user.getDisabled();
      return ("T".equals(disabled) || "N".equals(disabled));

    } catch (ALDBErrorException e) {
      logger.error("workflow", e);
      return true;
    }

  }

  public static void createWorkflowRequestActivity(EipTWorkflowRequest request,
      String loginName, List<String> recipients, Type type) {
    if (recipients != null && recipients.size() > 0) {
      ALActivity RecentActivity =
        ALActivity.getRecentActivity("Workflow", request.getRequestId(), 1f);
      boolean isDeletePrev =
        RecentActivity != null && RecentActivity.isReplace(loginName);

      EipTWorkflowCategory category = request.getEipTWorkflowCategory();
      String name = request.getRequestName();

      StringBuilder b =
        new StringBuilder(ALLocalizationUtils
          .getl10n("WORKFLOW_APPLICATION_ETC"));
      if (category != null) {
        b.append(category.getCategoryName());
        if (name != null && name.length() > 0) {
          b.append("：");
        }
      }
      b.append(request.getRequestName()).append("」");

      switch (type) {
        case REQUEST:
          b.append(ALLocalizationUtils.getl10n("WORKFLOW_REQUEST_MSG"));
          break;
        case DENAIL:
          b.append(ALLocalizationUtils.getl10n("WORKFLOW_DENIAL_MSG"));
          break;
        case ACCEPT:
          b.append(ALLocalizationUtils.getl10n("WORKFLOW_ACCEPT_MSG"));
          recipients.clear();
          recipients.add(getUserName(request.getUserId().toString()));
          break;
        default:

      }
      String portletParams =
        new StringBuilder("?template=WorkflowDetailScreen")
          .append("&entityid=")
          .append(request.getRequestId())
          .toString();
      ALActivityService.create(new ALActivityPutRequest()
        .withAppId("Workflow")
        .withLoginName(loginName)
        .withUserId(
          request.getUserId() == null ? request
            .getTurbineUser()
            .getUpdatedUserId() : request.getUserId())
        .withPortletParams(portletParams)
        .withRecipients(recipients)
        .withTitle(b.toString())
        .withPriority(1f)
        .withExternalId(String.valueOf(request.getRequestId())));

      if (isDeletePrev) {
        RecentActivity.delete();
      }
    }
  }

  /**
   * 一連のワークフローに関連するユーザのIdを列挙します
   * 
   * @param request
   * @return
   */
  public static Set<Integer> getRelatedUserIdList(EipTWorkflowRequest request) {
    Set<Integer> ids = new HashSet<Integer>();
    SelectQuery<EipTWorkflowRequestMap> query =
      Database.query(EipTWorkflowRequestMap.class);
    Expression exp11 =
      ExpressionFactory.matchDbExp(
        EipTWorkflowRequestMap.EIP_TWORKFLOW_REQUEST_PROPERTY
          + "."
          + EipTWorkflowRequest.REQUEST_ID_PK_COLUMN,
        request.getParentId());
    Expression exp12 =
      ExpressionFactory.matchExp(
        EipTWorkflowRequestMap.EIP_TWORKFLOW_REQUEST_PROPERTY
          + "."
          + EipTWorkflowRequest.PARENT_ID_PROPERTY,
        request.getParentId());
    Expression exp13 =
      ExpressionFactory.matchExp(
        EipTWorkflowRequestMap.EIP_TWORKFLOW_REQUEST_PROPERTY
          + "."
          + EipTWorkflowRequest.PARENT_ID_PROPERTY,
        request.getRequestId());
    query.setQualifier(exp11.orExp(exp12).orExp(exp13));
    ResultList<EipTWorkflowRequestMap> mapList = query.getResultList();
    for (EipTWorkflowRequestMap map : mapList) {
      ids.add(map.getUserId());
    }
    return ids;
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
}
