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
package com.aimluck.eip.msgboard.util;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import javax.imageio.ImageIO;

import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.jetspeed.om.security.UserIdPrincipal;
import org.apache.jetspeed.portal.PortletConfig;
import org.apache.jetspeed.portal.portlets.VelocityPortlet;
import org.apache.jetspeed.services.JetspeedSecurity;
import org.apache.jetspeed.services.customlocalization.CustomLocalizationService;
import org.apache.jetspeed.services.logging.JetspeedLogFactoryService;
import org.apache.jetspeed.services.logging.JetspeedLogger;
import org.apache.jetspeed.services.resources.JetspeedResources;
import org.apache.jetspeed.util.ServiceUtil;
import org.apache.turbine.services.TurbineServices;
import org.apache.turbine.services.localization.LocalizationService;
import org.apache.turbine.util.RunData;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.context.Context;

import com.aimluck.commons.utils.ALDeleteFileUtil;
import com.aimluck.eip.cayenne.om.portlet.EipTMsgboardCategory;
import com.aimluck.eip.cayenne.om.portlet.EipTMsgboardCategoryMap;
import com.aimluck.eip.cayenne.om.portlet.EipTMsgboardFile;
import com.aimluck.eip.cayenne.om.portlet.EipTMsgboardTopic;
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
import com.aimluck.eip.msgboard.MsgboardCategoryResultData;
import com.aimluck.eip.orm.Database;
import com.aimluck.eip.orm.query.SelectQuery;
import com.aimluck.eip.services.accessctl.ALAccessControlConstants;
import com.aimluck.eip.services.accessctl.ALAccessControlFactoryService;
import com.aimluck.eip.services.accessctl.ALAccessControlHandler;
import com.aimluck.eip.services.orgutils.ALOrgUtilsService;
import com.aimluck.eip.services.social.ALActivityService;
import com.aimluck.eip.services.social.model.ALActivityPutRequest;
import com.aimluck.eip.services.storage.ALStorageService;
import com.aimluck.eip.util.ALCommonUtils;
import com.aimluck.eip.util.ALEipUtils;
import com.aimluck.eip.whatsnew.util.WhatsNewUtils;

/**
 * 掲示板のユーティリティクラス <BR>
 *
 */
public class MsgboardUtils {

  /** logger */
  private static final JetspeedLogger logger = JetspeedLogFactoryService
    .getLogger(MsgboardUtils.class.getName());

  /** 所有者の識別子 */
  public static final String OWNER_ID = "ownerid";

  /** 掲示板の添付ファイルを保管するディレクトリの指定 */
  private static final String FOLDER_FILEDIR_MSGBOARD = JetspeedResources
    .getString("aipo.filedir", "");

  /** 掲示板の添付ファイルを保管するディレクトリのカテゴリキーの指定 */
  protected static final String CATEGORY_KEY = JetspeedResources.getString(
    "aipo.msgboard.categorykey",
    "");

  /** デフォルトエンコーディングを表わすシステムプロパティのキー */
  public static final String FILE_ENCODING = JetspeedResources.getString(
    "content.defaultencoding",
    "UTF-8");

  /** 全てのユーザーが閲覧／返信可 */
  public static final int ACCESS_PUBLIC_ALL = 0;

  /** 全てのユーザーが閲覧可。ただし返信できるのは所属メンバーのみ。 */
  public static final int ACCESS_PUBLIC_MEMBER = 1;

  /** 所属メンバーのみ閲覧／閲覧可 */
  public static final int ACCESS_SEACRET_MEMBER = 2;

  /** 自分のみ閲覧／返信可 */
  public static final int ACCESS_SEACRET_SELF = 3;

  /** カテゴリの公開／非公開の値（公開） */
  public static final String PUBLIC_FLG_VALUE_PUBLIC = "T";

  /** カテゴリの公開／非公開の値（非公開） */
  public static final String PUBLIC_FLG_VALUE_NONPUBLIC = "F";

  /** カテゴリの状態値（自分のみのカテゴリ） */
  public static final String STAT_VALUE_OWNER = "O";

  /** カテゴリの状態値（共有カテゴリ） */
  public static final String STAT_VALUE_SHARE = "S";

  /** カテゴリの状態値（公開カテゴリ） */
  public static final String STAT_VALUE_ALL = "A";

  public static final String MSGBOARD_PORTLET_NAME = "Msgboard";

  /** 検索キーワード変数の識別子 */
  public static final String TARGET_KEYWORD = "keyword";

  /** パラメータリセットの識別子 */
  private static final String RESET_FLAG = "reset_params";

  /**
   * トピックオブジェクトモデルのIDを取得します。 <BR>
   *
   * @param rundata
   * @param context
   * @return
   */
  public static Integer getEipTMsgboardParentTopicId(RunData rundata,
      Context context) {
    String topicid =
      ALEipUtils.getTemp(rundata, context, ALEipConstants.ENTITY_ID);
    Integer id = null;
    if (topicid == null || (id = Integer.valueOf(topicid)) == null) {
      // Todo IDが空の場合
      logger.debug("[MsgboardTopic] Empty ID...");
      return null;
    } else {
      return id;
    }
  }

  /**
   * トピックオブジェクトモデルを取得します。 <BR>
   *
   * @param rundata
   * @param context
   * @param isJoin
   *          カテゴリテーブルをJOINするかどうか
   * @return
   */
  public static EipTMsgboardTopic getEipTMsgboardParentTopic(RunData rundata,
      Context context, boolean isJoin) throws ALPageNotFoundException,
      ALDBErrorException {
    String topicid =
      ALEipUtils.getTemp(rundata, context, ALEipConstants.ENTITY_ID);
    try {
      if (topicid == null || Integer.valueOf(topicid) == null) {
        // トピック ID が空の場合
        logger.debug("[MsgboardTopic] Empty ID...");
        throw new ALPageNotFoundException();
      }

      int userid = ALEipUtils.getUserId(rundata);

      SelectQuery<EipTMsgboardTopic> query =
        Database.query(EipTMsgboardTopic.class);
      Expression exp1 =
        ExpressionFactory.matchDbExp(
          EipTMsgboardTopic.TOPIC_ID_PK_COLUMN,
          Integer.valueOf(topicid));
      query.setQualifier(exp1);
      Expression exp2 =
        ExpressionFactory.matchExp(
          EipTMsgboardTopic.PARENT_ID_PROPERTY,
          Integer.valueOf(0));
      query.andQualifier(exp2);
      query.distinct(true);

      List<EipTMsgboardTopic> topics = query.fetchList();
      if (topics == null || topics.size() == 0) {
        // 指定した トピック ID のレコードが見つからない場合
        logger.debug("[MsgboardTopic] Not found ID...");
        throw new ALPageNotFoundException();
      }

      EipTMsgboardTopic topic = topics.get(0);

      // アクセス権限チェック
      EipTMsgboardCategory category = topic.getEipTMsgboardCategory();
      boolean accessible = false;
      if (category == null) {
        // 掲示板画面を表示後カテゴリが削除された場合
        logger.debug("[MsgboardTopic] Not found Category...");
        throw new ALPageNotFoundException();
      } else if (category.getPublicFlag().equals("T")) {
        accessible = true;
      } else {
        @SuppressWarnings("unchecked")
        List<EipTMsgboardCategoryMap> maps =
          category.getEipTMsgboardCategoryMaps();
        for (EipTMsgboardCategoryMap map : maps) {
          if (map.getUserId().equals(Integer.valueOf(userid))) {
            accessible = true;
            break;
          }
        }
      }
      if (!accessible) {
        ALEipUtils.redirectPermissionError(rundata);
      }

      return topic;
    } catch (ALPageNotFoundException pageNotFound) {
      // logger.error("[MsgboardUtils]", pageNotFound);
      throw pageNotFound;
    } catch (Exception ex) {
      logger.error("[MsgboardUtils]", ex);
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
  public static EipTMsgboardTopic getEipTMsgboardTopicReply(RunData rundata,
      Context context, String topicid, boolean isSuperUser)
      throws ALPageNotFoundException, ALDBErrorException {
    try {
      if (topicid == null || Integer.valueOf(topicid) == null) {
        // トピック ID が空の場合
        logger.debug("[MsgboardTopic] Empty ID...");
        throw new ALPageNotFoundException();
      }

      SelectQuery<EipTMsgboardTopic> query =
        Database.query(EipTMsgboardTopic.class);
      Expression exp1 =
        ExpressionFactory.matchDbExp(
          EipTMsgboardTopic.TOPIC_ID_PK_COLUMN,
          Integer.valueOf(topicid));
      query.setQualifier(exp1);

      if (!isSuperUser) {
        Expression exp2 =
          ExpressionFactory.matchExp(
            EipTMsgboardTopic.OWNER_ID_PROPERTY,
            Integer.valueOf(ALEipUtils.getUserId(rundata)));
        query.andQualifier(exp2);
      }

      List<EipTMsgboardTopic> topics = query.fetchList();
      if (topics == null || topics.size() == 0) {
        // 指定した トピック ID のレコードが見つからない場合
        logger.debug("[MsgboardTopic] Not found ID...");
        throw new ALPageNotFoundException();
      }
      return topics.get(0);
    } catch (Exception ex) {
      logger.error("[MsgboardUtils]", ex);
      throw new ALDBErrorException();

    }
  }

  /**
   * ファイルオブジェクトモデルを取得します。 <BR>
   *
   * @param rundata
   * @param context
   * @return
   */
  public static EipTMsgboardFile getEipTMsgboardFile(RunData rundata)
      throws ALPageNotFoundException, ALDBErrorException {
    try {
      int attachmentIndex =
        rundata.getParameters().getInt("attachmentIndex", -1);
      if (attachmentIndex < 0) {
        // ID が空の場合
        logger.debug("[MsgboardUtils] Empty ID...");
        throw new ALPageNotFoundException();

      }

      SelectQuery<EipTMsgboardFile> query =
        Database.query(EipTMsgboardFile.class);
      Expression exp =
        ExpressionFactory.matchDbExp(
          EipTMsgboardFile.FILE_ID_PK_COLUMN,
          Integer.valueOf(attachmentIndex));
      query.andQualifier(exp);

      List<EipTMsgboardFile> files = query.fetchList();
      if (files == null || files.size() == 0) {
        // 指定した ID のレコードが見つからない場合
        logger.debug("[MsgboardUtils] Not found ID...");
        throw new ALPageNotFoundException();
      }
      return files.get(0);
    } catch (Exception ex) {
      logger.error("[MsgboardUtils]", ex);
      throw new ALDBErrorException();
    }
  }

  /**
   * トピックオブジェクトモデルを取得します。 <BR>
   *
   * @param rundata
   * @param context
   * @param isJoin
   *          カテゴリテーブルをJOINするかどうか
   * @return
   */
  public static List<EipTMsgboardTopic> getEipTMsgboardTopicList(
      RunData rundata, Context context, boolean isJoin)
      throws ALPageNotFoundException, ALDBErrorException {
    String topicid =
      ALEipUtils.getTemp(rundata, context, ALEipConstants.ENTITY_ID);
    try {
      if (topicid == null || Integer.valueOf(topicid) == null) {
        // トピック ID が空の場合
        logger.debug("[MsgboardTopic] Empty ID...");
        throw new ALPageNotFoundException();
      }

      SelectQuery<EipTMsgboardTopic> query =
        Database.query(EipTMsgboardTopic.class);
      Expression exp001 =
        ExpressionFactory.matchExp(
          EipTMsgboardTopic.PARENT_ID_PROPERTY,
          Integer.valueOf(topicid));

      // アクセス制御
      Expression exp01 =
        ExpressionFactory.matchDbExp(
          EipTMsgboardTopic.TOPIC_ID_PK_COLUMN,
          Integer.valueOf(topicid));
      Expression exp11 =
        ExpressionFactory.matchExp(
          EipTMsgboardTopic.EIP_TMSGBOARD_CATEGORY_PROPERTY
            + "."
            + EipTMsgboardCategory.PUBLIC_FLAG_PROPERTY,
          "T");
      Expression exp21 =
        ExpressionFactory.matchExp(
          EipTMsgboardTopic.EIP_TMSGBOARD_CATEGORY_PROPERTY
            + "."
            + EipTMsgboardCategory.PUBLIC_FLAG_PROPERTY,
          "F");
      Expression exp22 =
        ExpressionFactory.matchExp(
          EipTMsgboardTopic.EIP_TMSGBOARD_CATEGORY_PROPERTY
            + "."
            + EipTMsgboardCategory.EIP_TMSGBOARD_CATEGORY_MAPS_PROPERTY
            + "."
            + EipTMsgboardCategoryMap.USER_ID_PROPERTY,
          Integer.valueOf(ALEipUtils.getUserId(rundata)));
      query.setQualifier((exp01.andExp(exp11.orExp(exp21.andExp(exp22))))
        .orExp(exp001));
      query.distinct(true);

      List<EipTMsgboardTopic> topics = query.fetchList();
      if (topics == null || topics.size() == 0) {
        // 指定した トピック ID のレコードが見つからない場合
        logger.debug("[MsgboardTopic] Not found ID...");
        throw new ALPageNotFoundException();
      }
      return topics;
    } catch (Exception ex) {
      logger.error("[MsgboardUtils]", ex);
      throw new ALDBErrorException();

    }
  }

  /**
   * トピックオブジェクトモデルを取得します。 <BR>
   *
   * @param rundata
   * @param context
   * @param isSuperUser
   *          カテゴリテーブルをJOINするかどうか
   * @return
   */
  public static List<EipTMsgboardTopic> getEipTMsgboardTopicListToDeleteTopic(
      RunData rundata, Context context, boolean isSuperUser)
      throws ALPageNotFoundException, ALDBErrorException {
    String topicid =
      ALEipUtils.getTemp(rundata, context, ALEipConstants.ENTITY_ID);
    try {
      if (topicid == null || Integer.valueOf(topicid) == null) {
        // トピック ID が空の場合
        logger.debug("[MsgboardTopic] Empty ID...");
        throw new ALPageNotFoundException();
      }

      int userid = ALEipUtils.getUserId(rundata);

      SelectQuery<EipTMsgboardTopic> query =
        Database.query(EipTMsgboardTopic.class);

      Expression exp01 =
        ExpressionFactory.matchDbExp(EipTMsgboardTopic.OWNER_ID_COLUMN, Integer
          .valueOf(userid));
      Expression exp02 =
        ExpressionFactory.matchDbExp(
          EipTMsgboardTopic.TOPIC_ID_PK_COLUMN,
          Integer.valueOf(topicid));
      Expression exp03 =
        ExpressionFactory.matchExp(
          EipTMsgboardTopic.PARENT_ID_PROPERTY,
          Integer.valueOf(topicid));

      if (isSuperUser) {
        query.andQualifier((exp02).orExp(exp03));
      } else {
        query.andQualifier((exp01.andExp(exp02)).orExp(exp03));
      }

      List<EipTMsgboardTopic> topics = query.fetchList();
      if (topics == null || topics.size() == 0) {
        // 指定した トピック ID のレコードが見つからない場合
        logger.debug("[MsgboardTopic] Not found ID...");
        throw new ALPageNotFoundException();
      }

      boolean isdelete = false;
      int size = topics.size();
      for (int i = 0; i < size; i++) {
        EipTMsgboardTopic topic = topics.get(i);
        if (topic.getOwnerId().intValue() == userid || isSuperUser) {
          isdelete = true;
          break;
        }
      }
      if (!isdelete) {
        // 指定した トピック ID のレコードが見つからない場合
        logger.debug("[MsgboardTopic] Not found ID...");
        throw new ALPageNotFoundException();
      }

      return topics;
    } catch (Exception ex) {
      logger.error("[MsgboardUtils]", ex);
      throw new ALDBErrorException();

    }
  }

  /**
   * カテゴリオブジェクトモデルを取得します。 <BR>
   *
   * @param rundata
   * @param context
   * @return
   */
  public static EipTMsgboardCategory getEipTMsgboardCategory(RunData rundata,
      Context context, boolean ownerOnly) throws ALPageNotFoundException,
      ALDBErrorException {
    String categoryid =
      ALEipUtils.getTemp(rundata, context, ALEipConstants.ENTITY_ID);
    try {
      if (categoryid == null || Integer.valueOf(categoryid) == null) {
        // カテゴリ IDが空の場合
        logger.debug("[MsgboardCategory] Empty ID...");
        throw new ALPageNotFoundException();
      }

      SelectQuery<EipTMsgboardCategory> query =
        Database.query(EipTMsgboardCategory.class);

      Expression exp1 =
        ExpressionFactory.matchDbExp(
          EipTMsgboardCategory.CATEGORY_ID_PK_COLUMN,
          Integer.valueOf(categoryid));
      query.setQualifier(exp1);
      Expression exp2 =
        ExpressionFactory.noMatchDbExp(
          EipTMsgboardCategory.TURBINE_USER_PROPERTY
            + "."
            + TurbineUser.USER_ID_PK_COLUMN,
          Integer.valueOf(0));
      query.andQualifier(exp2);
      if (ownerOnly) {
        /*
         * Expression exp3 = ExpressionFactory.matchDbExp(
         * EipTMsgboardCategory.TURBINE_USER_PROPERTY + "." +
         * TurbineUser.USER_ID_PK_COLUMN, Integer.valueOf(ALEipUtils
         * .getUserId(rundata))); query.andQualifier(exp3);
         */
      }

      // アクセス制御

      int loginUserId = ALEipUtils.getUserId(rundata);

      ALAccessControlFactoryService aclservice =
        (ALAccessControlFactoryService) ((TurbineServices) TurbineServices
          .getInstance())
          .getService(ALAccessControlFactoryService.SERVICE_NAME);
      ALAccessControlHandler aclhandler = aclservice.getAccessControlHandler();
      boolean hasAclviewOther =
        aclhandler.hasAuthority(
          loginUserId,
          ALAccessControlConstants.POERTLET_FEATURE_MSGBOARD_CATEGORY_OTHER,
          ALAccessControlConstants.VALUE_ACL_LIST);

      Expression exp01 =
        ExpressionFactory.matchExp(
          EipTMsgboardCategory.PUBLIC_FLAG_PROPERTY,
          "T");
      Expression exp02 =
        ExpressionFactory.matchExp(
          EipTMsgboardCategory.EIP_TMSGBOARD_CATEGORY_MAPS_PROPERTY
            + "."
            + EipTMsgboardCategoryMap.STATUS_PROPERTY,
          "O");
      Expression exp03 =
        ExpressionFactory.matchExp(
          EipTMsgboardCategory.EIP_TMSGBOARD_CATEGORY_MAPS_PROPERTY
            + "."
            + EipTMsgboardCategoryMap.STATUS_PROPERTY,
          "A");
      Expression exp11 =
        ExpressionFactory.matchExp(
          EipTMsgboardCategory.PUBLIC_FLAG_PROPERTY,
          "F");
      Expression exp12 =
        ExpressionFactory.matchExp(
          EipTMsgboardCategory.EIP_TMSGBOARD_CATEGORY_MAPS_PROPERTY
            + "."
            + EipTMsgboardCategoryMap.USER_ID_PROPERTY,
          Integer.valueOf(loginUserId));

      if (!hasAclviewOther) {
        query.andQualifier((exp01.andExp(exp02.orExp(exp03))).orExp(exp11
          .andExp(exp12)));
      } else {
        query.andQualifier((exp01.andExp(exp02.orExp(exp03))).orExp(exp11
          .andExp(exp02.orExp(exp03))));
      }
      query.distinct(true);

      List<EipTMsgboardCategory> categories = query.fetchList();
      if (categories == null || categories.size() == 0) {
        // 指定したカテゴリ IDのレコードが見つからない場合
        logger.debug("[MsgboardUtils] Not found ID...");
        throw new ALPageNotFoundException();
      }
      return categories.get(0);
    } catch (Exception ex) {
      logger.error("[MsgboardUtils]", ex);
      throw new ALDBErrorException();

    }
  }

  /**
   * <BR>
   *
   * @param rundata
   * @param context
   * @return
   */
  public static List<Integer> getWhatsNewInsertList(RunData rundata,
      int categoryid, String is_public) throws ALPageNotFoundException,
      ALDBErrorException {

    int userid = ALEipUtils.getUserId(rundata);
    List<ALEipUser> result = new ArrayList<ALEipUser>();

    if ("F".equals(is_public)) {
      try {

        SelectQuery<EipTMsgboardCategoryMap> query =
          Database.query(EipTMsgboardCategoryMap.class);
        query.select(EipTMsgboardCategoryMap.USER_ID_COLUMN);

        Expression exp1 =
          ExpressionFactory.matchExp(
            EipTMsgboardCategoryMap.CATEGORY_ID_PROPERTY,
            Integer.valueOf(categoryid));
        query.setQualifier(exp1);

        // アクセス制御
        Expression exp11 =
          ExpressionFactory.matchExp(
            EipTMsgboardCategoryMap.EIP_TMSGBOARD_CATEGORY_PROPERTY
              + "."
              + EipTMsgboardCategory.PUBLIC_FLAG_PROPERTY,
            PUBLIC_FLG_VALUE_PUBLIC);
        Expression exp12 =
          ExpressionFactory.matchExp(
            EipTMsgboardCategoryMap.EIP_TMSGBOARD_CATEGORY_PROPERTY
              + "."
              + EipTMsgboardCategory.PUBLIC_FLAG_PROPERTY,
            PUBLIC_FLG_VALUE_NONPUBLIC);
        Expression exp13 =
          ExpressionFactory.matchExp(
            EipTMsgboardCategoryMap.STATUS_PROPERTY,
            STAT_VALUE_SHARE);
        Expression exp14 =
          ExpressionFactory.matchExp(
            EipTMsgboardCategoryMap.STATUS_PROPERTY,
            STAT_VALUE_OWNER);
        query.andQualifier(exp11.orExp(exp12.andExp(exp13)).orExp(
          exp12.andExp(exp14)));
        query.distinct(true);

        List<EipTMsgboardCategoryMap> uids = query.fetchList();
        List<Integer> userIds = new ArrayList<Integer>();
        if (uids != null && uids.size() != 0) {
          int size = uids.size();
          for (int i = 0; i < size; i++) {
            EipTMsgboardCategoryMap uid = uids.get(i);
            Integer id = uid.getUserId();
            if (id.intValue() != userid) {
              result.add(ALEipUtils.getALEipUser(id.intValue()));
              userIds.add(id.intValue());
            }
          }
        }
        return userIds;

        /* メンバー全員に新着ポートレット登録 */
        /*-
        ALAccessControlFactoryService aclservice =
          (ALAccessControlFactoryService) ((TurbineServices) TurbineServices
            .getInstance())
            .getService(ALAccessControlFactoryService.SERVICE_NAME);
        ALAccessControlHandler aclhandler =
          aclservice.getAccessControlHandler();
        List<Integer> userIds =
          aclhandler.getAcceptUserIdsInListExceptLoginUser(
            ALEipUtils.getUserId(rundata),
            ALAccessControlConstants.POERTLET_FEATURE_MSGBOARD_TOPIC,
            ALAccessControlConstants.VALUE_ACL_DETAIL,
            result);

        return userIds;
         */
      } catch (Exception ex) {
        logger.error("[MsgboardUtils]", ex);
        throw new ALDBErrorException();
      }
    } else {
      /* 自分以外の全員に新着ポートレット登録 */
      ALAccessControlFactoryService aclservice =
        (ALAccessControlFactoryService) ((TurbineServices) TurbineServices
          .getInstance())
          .getService(ALAccessControlFactoryService.SERVICE_NAME);
      ALAccessControlHandler aclhandler = aclservice.getAccessControlHandler();
      List<Integer> userIds =
        aclhandler.getAcceptUserIdsExceptLoginUser(
          ALEipUtils.getUserId(rundata),
          ALAccessControlConstants.POERTLET_FEATURE_MSGBOARD_TOPIC,
          ALAccessControlConstants.VALUE_ACL_DETAIL);
      return userIds;

    }
  }

  public static List<MsgboardCategoryResultData> loadCategoryList(
      RunData rundata) {
    // カテゴリ一覧
    List<MsgboardCategoryResultData> categoryList =
      new ArrayList<MsgboardCategoryResultData>();
    try {
      SelectQuery<EipTMsgboardCategory> query =
        Database.query(EipTMsgboardCategory.class);

      // アクセス制御
      Expression exp01 =
        ExpressionFactory.matchExp(
          EipTMsgboardCategory.PUBLIC_FLAG_PROPERTY,
          MsgboardUtils.PUBLIC_FLG_VALUE_PUBLIC);
      Expression exp02 =
        ExpressionFactory.matchExp(
          EipTMsgboardCategory.EIP_TMSGBOARD_CATEGORY_MAPS_PROPERTY
            + "."
            + EipTMsgboardCategoryMap.STATUS_PROPERTY,
          MsgboardUtils.STAT_VALUE_OWNER);
      Expression exp03 =
        ExpressionFactory.matchExp(
          EipTMsgboardCategory.EIP_TMSGBOARD_CATEGORY_MAPS_PROPERTY
            + "."
            + EipTMsgboardCategoryMap.STATUS_PROPERTY,
          MsgboardUtils.STAT_VALUE_ALL);
      Expression exp11 =
        ExpressionFactory.matchExp(
          EipTMsgboardCategory.PUBLIC_FLAG_PROPERTY,
          MsgboardUtils.PUBLIC_FLG_VALUE_NONPUBLIC);
      Expression exp12 =
        ExpressionFactory.matchExp(
          EipTMsgboardCategory.EIP_TMSGBOARD_CATEGORY_MAPS_PROPERTY
            + "."
            + EipTMsgboardCategoryMap.USER_ID_PROPERTY,
          Integer.valueOf(ALEipUtils.getUserId(rundata)));
      query.setQualifier((exp01.andExp(exp02.orExp(exp03))).orExp(exp11
        .andExp(exp12)));
      query.orderAscending(EipTMsgboardCategory.CATEGORY_NAME_PROPERTY);
      query.distinct(true);

      MsgboardCategoryResultData otherRd = null;

      List<EipTMsgboardCategory> aList = query.fetchList();
      int size = aList.size();
      for (int i = 0; i < size; i++) {
        EipTMsgboardCategory record = aList.get(i);
        MsgboardCategoryResultData rd = new MsgboardCategoryResultData();
        rd.initField();
        rd.setCategoryId(record.getCategoryId().longValue());
        rd.setCategoryName(record.getCategoryName());
        if (record.getCategoryId().longValue() == 1) {
          // カテゴリ「その他」は最後に追加するため，ここではリストに追加しない．
          otherRd = rd;
        } else {
          categoryList.add(rd);
        }
      }
      if (otherRd != null) {
        categoryList.add(otherRd);
      }
    } catch (Exception ex) {
      logger.error("msgboard", ex);
      return null;
    }
    return categoryList;
  }

  public static void deleteFiles(int timelineId, String orgId, int uid,
      List<String> fpaths) throws ALFileNotRemovedException {
    ALDeleteFileUtil.deleteFiles(
      timelineId,
      EipTMsgboardFile.EIP_TMSGBOARD_TOPIC_PROPERTY,
      getSaveDirPath(orgId, uid),
      fpaths,
      EipTMsgboardFile.class);
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

    // 新規にアップロードされたファイルの処理
    if (newfileids.size() > 0) {
      String folderName =
        rundata.getParameters().getString(
          FileuploadUtils.KEY_FILEUPLOAD_FODLER_NAME);
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
            logger.error("msgboard", e);
          } finally {
            try {
              reader.close();
            } catch (Exception e) {
              logger.error("msgboard", e);
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
        SelectQuery<EipTMsgboardFile> reqquery =
          Database.query(EipTMsgboardFile.class);
        Expression reqexp1 =
          ExpressionFactory.inDbExp(
            EipTMsgboardFile.FILE_ID_PK_COLUMN,
            hadfileidsValue);
        reqquery.setQualifier(reqexp1);
        List<EipTMsgboardFile> requests = reqquery.fetchList();
        for (EipTMsgboardFile file : requests) {
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

  public static boolean insertFileDataDelegate(RunData rundata,
      Context context, EipTMsgboardTopic topic,
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

    SelectQuery<EipTMsgboardFile> dbquery =
      Database.query(EipTMsgboardFile.class);
    dbquery.andQualifier(ExpressionFactory.matchDbExp(
      EipTMsgboardFile.EIP_TMSGBOARD_TOPIC_PROPERTY,
      topic.getTopicId()));
    List<EipTMsgboardFile> existsFiles = dbquery.fetchList();
    List<EipTMsgboardFile> delFiles = new ArrayList<EipTMsgboardFile>();
    for (EipTMsgboardFile file : existsFiles) {
      if (!hadfileids.contains(file.getFileId())) {
        delFiles.add(file);
      }
    }

    // ローカルファイルに保存されているファイルを削除する．
    if (delFiles.size() > 0) {
      int delsize = delFiles.size();
      for (int i = 0; i < delsize; i++) {
        ALStorageService.deleteFile(MsgboardUtils.getSaveDirPath(orgId, uid)
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
        EipTMsgboardFile file = Database.create(EipTMsgboardFile.class);
        // 所有者
        file.setOwnerId(Integer.valueOf(uid));
        // トピックID
        file.setEipTMsgboardTopic(topic);
        // ファイル名
        file.setFileName(filebean.getFileName());
        // ファイルパス
        file.setFilePath(MsgboardUtils.getRelativePath(filename));
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
            shrinkImageSet.getFixImage()), FOLDER_FILEDIR_MSGBOARD
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
            .getFileId()), FOLDER_FILEDIR_MSGBOARD, CATEGORY_KEY
            + ALStorageService.separator()
            + uid, filename);
        }
      }

      // 添付ファイル保存先のフォルダを削除
      ALStorageService.deleteTmpFolder(uid, folderName);
    } catch (Exception e) {
      Database.rollback();
      logger.error("msgboard", e);
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
      FOLDER_FILEDIR_MSGBOARD,
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

  @Deprecated
  public static void shiftWhatsNewReadFlag(RunData rundata, int entityid) {
    int uid = ALEipUtils.getUserId(rundata);
    boolean isPublic = false;

    SelectQuery<EipTMsgboardTopic> query =
      Database.query(EipTMsgboardTopic.class);
    Expression exp =
      ExpressionFactory
        .matchExp(EipTMsgboardTopic.PARENT_ID_PROPERTY, entityid);
    query.setQualifier(exp);
    query.select(EipTMsgboardTopic.TOPIC_ID_PK_COLUMN);
    query.distinct(true);

    List<EipTMsgboardTopic> topics = query.fetchList();

    query = Database.query(EipTMsgboardTopic.class);
    exp =
      ExpressionFactory.matchDbExp(
        EipTMsgboardTopic.TOPIC_ID_PK_COLUMN,
        entityid);
    query.setQualifier(exp);

    List<EipTMsgboardTopic> topic = query.fetchList();
    if (topic != null
      && ((topic.get(0)).getEipTMsgboardCategory().getPublicFlag().equals("T"))) {
      isPublic = true;
    }

    if (topics != null) {

      int size = topics.size();
      Integer _id = null;

      if (isPublic) {
        for (int i = 0; i < size; i++) {
          EipTMsgboardTopic record = topics.get(i);
          _id = record.getTopicId();
          WhatsNewUtils.shiftWhatsNewReadFlagPublic(
            WhatsNewUtils.WHATS_NEW_TYPE_MSGBOARD_TOPIC,
            _id.intValue(),
            uid);
        }
      } else {
        for (int i = 0; i < size; i++) {
          EipTMsgboardTopic record = topics.get(i);
          _id = record.getTopicId();
          WhatsNewUtils.shiftWhatsNewReadFlag(
            WhatsNewUtils.WHATS_NEW_TYPE_MSGBOARD_TOPIC,
            _id.intValue(),
            uid);
        }
      }
    }
    if (isPublic) {
      WhatsNewUtils.shiftWhatsNewReadFlagPublic(
        WhatsNewUtils.WHATS_NEW_TYPE_MSGBOARD_TOPIC,
        entityid,
        uid);
    } else {
      WhatsNewUtils.shiftWhatsNewReadFlag(
        WhatsNewUtils.WHATS_NEW_TYPE_MSGBOARD_TOPIC,
        entityid,
        uid);
    }

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

  /**
   * アクセス権限をチェックします。
   *
   * @return
   */
  public static boolean checkPermission(RunData rundata, Context context,
      int defineAclType, String pfeature) {

    if (defineAclType == 0) {
      return true;
    }

    if (pfeature == null || "".equals(pfeature)) {
      return true;
    }

    ALAccessControlFactoryService aclservice =
      (ALAccessControlFactoryService) ((TurbineServices) TurbineServices
        .getInstance()).getService(ALAccessControlFactoryService.SERVICE_NAME);
    ALAccessControlHandler aclhandler = aclservice.getAccessControlHandler();
    boolean hasAuthority =
      aclhandler.hasAuthority(
        ALEipUtils.getUserId(rundata),
        pfeature,
        defineAclType);

    return hasAuthority;
  }

  /**
   * トピックに対する返信数を返します
   *
   * @param topic_id
   * @return
   */
  public static Integer countReply(Integer topic_id) {
    SelectQuery<EipTMsgboardTopic> query =
      Database.query(EipTMsgboardTopic.class);

    Expression exp1 =
      ExpressionFactory
        .matchDbExp(EipTMsgboardTopic.PARENT_ID_COLUMN, topic_id);
    query.setQualifier(exp1);

    return query.getCount();
  }

  public static void createTopicActivity(EipTMsgboardTopic topic,
      String loginName, boolean isNew) {
    createTopicActivity(topic, loginName, null, isNew);
  }

  public static void createTopicActivity(EipTMsgboardTopic topic,
      String loginName, List<String> recipients, boolean isNew) {
    ALActivity RecentActivity =
      ALActivity.getRecentActivity("Msgboard", topic.getTopicId(), 0f);
    boolean isDeletePrev =
      RecentActivity != null && RecentActivity.isReplace(loginName);

    String title =
      new StringBuilder("掲示板「")
        .append(topic.getTopicName())
        .append("」を")
        .append(isNew ? "作成しました。" : "編集しました。")
        .toString();
    String portletParams =
      new StringBuilder("?template=MsgboardTopicDetailScreen").append(
        "&entityid=").append(topic.getTopicId()).toString();

    if (recipients != null && recipients.size() > 0) {
      ALActivityService.create(new ALActivityPutRequest()
        .withAppId("Msgboard")
        .withUserId(topic.getUpdateUserId())
        .withLoginName(loginName)
        .withPortletParams(portletParams)
        .withRecipients(recipients)
        .withTitle(title)
        .withPriority(0f)
        .withExternalId(String.valueOf(topic.getTopicId())));
    } else {
      ALActivityService.create(new ALActivityPutRequest()
        .withAppId("Msgboard")
        .withLoginName(loginName)
        .withUserId(topic.getUpdateUserId())
        .withPortletParams(portletParams)
        .withTitle(title)
        .withPriority(0f)
        .withExternalId(String.valueOf(topic.getTopicId())));
    }
    if (isDeletePrev) {
      RecentActivity.delete();
    }
  }

  /**
   * アクティビティを通知先・社内参加者の「あなた宛のお知らせ」に表示させる（返信用）
   *
   * @param topic
   * @param loginName
   * @param recipients
   */
  public static void createNewTopicActivity(EipTMsgboardTopic topic,
      String loginName, List<String> recipient, EipTMsgboardTopic childTopic) {
    ALActivity RecentActivity =
      ALActivity.getRecentActivity("Msgboard", topic.getTopicId(), 1f);
    boolean isDeletePrev =
      RecentActivity != null && RecentActivity.isReplace(loginName);

    if (recipient != null) {
      StringBuilder b = new StringBuilder("掲示板「");

      b.append(ALCommonUtils.compressString(topic.getTopicName(), 30)).append(
        "」").append("に返信しました。");

      String portletParams =
        new StringBuilder("?template=MsgboardTopicDetailScreen").append(
          "&entityid=").append(topic.getTopicId()).toString();
      ALActivityService.create(new ALActivityPutRequest()
        .withAppId("Msgboard")
        .withUserId(childTopic.getOwnerId())
        .withLoginName(loginName)
        .withPortletParams(portletParams)
        .withRecipients(recipient)
        .withTitle(b.toString())
        .withPriority(1f)
        .withExternalId(String.valueOf(topic.getTopicId())));
    } else {
      StringBuilder b = new StringBuilder("掲示板「");

      b.append(ALCommonUtils.compressString(topic.getTopicName(), 30)).append(
        "」").append("に返信しました。");

      String portletParams =
        new StringBuilder("?template=MsgboardTopicDetailScreen").append(
          "&entityid=").append(topic.getTopicId()).toString();
      ALActivityService.create(new ALActivityPutRequest()
        .withAppId("Msgboard")
        .withUserId(childTopic.getOwnerId())
        .withLoginName(loginName)
        .withPortletParams(portletParams)
        .withTitle(b.toString())
        .withPriority(1f)
        .withExternalId(String.valueOf(topic.getTopicId())));
    }
    if (isDeletePrev) {
      RecentActivity.delete();
    }
  }

  public static void createNewCommentActivity(EipTMsgboardTopic topic,
      String loginName, EipTMsgboardTopic childTopic) {
    createNewCommentActivity(topic, loginName, null, childTopic);
  }

  public static void createNewCommentActivity(EipTMsgboardTopic topic,
      String loginName, List<String> recipients, EipTMsgboardTopic childTopic) {
    String title =
      new StringBuilder("掲示板「")
        .append(ALCommonUtils.compressString(topic.getTopicName(), 30))
        .append("」に")
        .append("返信しました。")
        .toString();
    String portletParams =
      new StringBuilder("?template=MsgboardTopicDetailScreen").append(
        "&entityid=").append(topic.getTopicId()).toString();

    if (recipients != null && recipients.size() > 0) {
      ALActivityService.create(new ALActivityPutRequest()
        .withAppId("Msgboard")
        .withUserId(childTopic.getOwnerId())
        .withLoginName(loginName)
        .withPortletParams(portletParams)
        .withRecipients(recipients)
        .withTitle(title)
        .withPriority(0f)
        .withExternalId(String.valueOf(topic.getTopicId())));
    } else {
      ALActivityService.create(new ALActivityPutRequest()
        .withUserId(childTopic.getOwnerId())
        .withAppId("Msgboard")
        .withLoginName(loginName)
        .withPortletParams(portletParams)
        .withTitle(title)
        .withPriority(0f)
        .withExternalId(String.valueOf(topic.getTopicId())));
    }
  }

  /**
   * パソコンへ送信するメールの内容を作成する（返信用）．
   *
   * @return
   */
  public static String createReplyMsgForPc(RunData rundata,
      EipTMsgboardTopic parenttopic, EipTMsgboardTopic topic,
      List<ALEipUser> memberList) throws ALDBErrorException {
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

    context.put("topicName", parenttopic.getTopicName().toString());

    StringBuffer Note = new StringBuffer();
    if (topic.getNote().toString().length() > 0) {
      Note.append(topic.getNote().toString());
    }
    context.put("note", Note);

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
            + "/msgboard-notification-mail.vm", "utf-8");
        template.merge(context, writer);
      } else {
        Template template =
          Velocity.getTemplate(
            "portlets/mail/msgboard-notification-mail.vm",
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
   * 携帯電話へ送信するメールの内容を作成する（返信用）．
   *
   * @return
   */
  public static String createReplyMsgForCellPhone(RunData rundata,
      EipTMsgboardTopic parenttopic, EipTMsgboardTopic topic,
      List<ALEipUser> memberList) throws ALDBErrorException {
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

    context.put("topicName", parenttopic.getTopicName().toString());

    StringBuffer Note = new StringBuffer();
    if (topic.getNote().toString().length() > 0) {
      Note.append(topic.getNote().toString());
    }
    context.put("note", Note);

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
            + "/msgboard-notification-mail.vm", "utf-8");
        template.merge(context, writer);
      } else {
        Template template =
          Velocity.getTemplate(
            "portlets/mail/msgboard-notification-mail.vm",
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
   * トピックに添付されたすべての添付ファイルを物理削除します。
   *
   * @param topic
   */
  @SuppressWarnings("unchecked")
  public static void deleteAttachmentFiles(EipTMsgboardTopic topic) {
    String orgId = Database.getDomainName();
    List<EipTMsgboardFile> files = topic.getEipTMsgboardFileArray();
    int uid = topic.getOwnerId();
    for (EipTMsgboardFile file : files) {
      ALStorageService.deleteFile(MsgboardUtils.getSaveDirPath(orgId, uid)
        + file.getFilePath());
    }
  }

  /**
   * 指定されたユーザが指定カテゴリのトピックに対して返信できるかどうか調べます。
   *
   * @param user_id
   * @param category
   * @return
   */
  public static boolean hasAuthorityToReply(int user_id,
      EipTMsgboardCategory category) {
    if (category.getTurbineUser().getUserId() == user_id) {
      return true;
    }

    boolean canAllReply = "T".equals(category.getPublicFlag());
    List<?> categoryMap = category.getEipTMsgboardCategoryMaps();
    int mapsize = categoryMap.size();
    for (int i = 0; i < mapsize; i++) {
      EipTMsgboardCategoryMap map =
        (EipTMsgboardCategoryMap) categoryMap.get(i);

      // 全員が返信可能
      if (canAllReply && "A".equals(map.getStatus())) {
        return true;
      } else {
        // ログインユーザが所属メンバの場合
        if (map.getUserId().intValue() == user_id) {
          return true;
        }
      }
    }

    return false;
  }

  /**
   * PSMLに設定されているデータと比較して valueが正しい値ならその値を新しくPSMLに保存。
   *
   * @deprecated {@link ALEipUtils#passPSML(RunData,Context,String)}
   * @param rundata
   * @param context
   * @param config
   * @return
   */
  @Deprecated
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
}
