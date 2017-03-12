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
package com.aimluck.eip.wiki.util;

import java.util.ArrayList;
import java.util.List;

import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.commons.lang.StringUtils;
import org.apache.jetspeed.services.logging.JetspeedLogFactoryService;
import org.apache.jetspeed.services.logging.JetspeedLogger;
import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;

import com.aimluck.eip.cayenne.om.portlet.EipTWiki;
import com.aimluck.eip.common.ALDBErrorException;
import com.aimluck.eip.common.ALEipConstants;
import com.aimluck.eip.common.ALPageNotFoundException;
import com.aimluck.eip.orm.Database;
import com.aimluck.eip.orm.query.SelectQuery;
import com.aimluck.eip.util.ALEipUtils;
import com.aimluck.eip.wiki.WikiResultData;

/**
 * Wikiのユーティリティクラスです。 <BR>
 * 
 */
public class WikiUtils {

  /** logger */
  private static final JetspeedLogger logger = JetspeedLogFactoryService
    .getLogger(WikiUtils.class.getName());

  /** グループによる表示切り替え用変数の識別子 */
  public static final String TARGET_GROUP_NAME = "target_group_name";

  /** ユーザによる表示切り替え用変数の識別子 */
  public static final String TARGET_USER_ID = "target_user_id";

  /** 検索キーワード変数の識別子 */
  public static final String TARGET_KEYWORD = "keyword";

  /** パラメータリセットの識別子 */
  private static final String RESET_KEYWORD_FLAG = "reset_keyword_params";

  /** パラメータリセットの識別子 */
  private static final String RESET_TARGET_FLAG = "reset_target_params";

  /** パラメータリセットの識別子 */
  private static final String RESET_FLAG = "reset_params";

  /** 期限状態（期限前） */
  public static final int LIMIT_STATE_BEFORE = -1;

  /** 期限状態（期限当日） */
  public static final int LIMIT_STATE_TODAY = 0;

  /** 期限状態（期限後） */
  public static final int LIMIT_STATE_AFTER = 1;

  public static final String WIKI_PORTLET_NAME = "Wiki";

  /** トップレベルwiki */
  public static final int PARENT_WIKI = 0;

  /**
   * Wikiカテゴリ オブジェクトモデルを取得します。 <BR>
   * 
   * @param rundata
   * @param context
   * @return
   */
  public static EipTWiki getEipTWikiCategory(RunData rundata, Context context)
      throws ALPageNotFoundException, ALDBErrorException {
    String categoryid =
      ALEipUtils.getTemp(rundata, context, ALEipConstants.ENTITY_ID);

    if (categoryid == null || Integer.valueOf(categoryid) == null) {
      // カテゴリIDが空の場合
      logger.debug("[Wiki] Empty ID...");
      throw new ALPageNotFoundException();
    }

    EipTWiki category = getEipTWikiCategory(Integer.parseInt(categoryid));

    if (null == category) {
      throw new ALPageNotFoundException();
    }

    return category;
  }

  /**
   * Wikiカテゴリ オブジェクトモデルを取得します。 <BR>
   * 
   * @param rundata
   * @param context
   * @return
   */
  public static EipTWiki getEipTWikiCategory(Integer integer) {
    try {
      SelectQuery<EipTWiki> query = Database.query(EipTWiki.class);
      return query
        .setQualifier(
          ExpressionFactory.matchExp(EipTWiki.PARENT_ID_PROPERTY, integer))
        .fetchSingle();
    } catch (Exception ex) {
      logger.error("wiki", ex);
      return null;
    }
  }

  /**
   * parent_id:0のデータ一覧を取得する
   * 
   * @param rundata
   * @return
   */
  public static List<WikiResultData> loadTopWikiList(RunData rundata) {
    // カテゴリ一覧
    List<WikiResultData> categoryList = new ArrayList<WikiResultData>();
    try {
      SelectQuery<EipTWiki> query = Database.query(EipTWiki.class);
      query.setQualifier(ExpressionFactory.matchExp(
        EipTWiki.PARENT_ID_PROPERTY,
        PARENT_WIKI));

      List<EipTWiki> aList =
        query.orderAscending(EipTWiki.WIKI_NAME_PROPERTY).fetchList();

      int size = aList.size();
      for (int i = 0; i < size; i++) {
        EipTWiki record = aList.get(i);
        WikiResultData rd = new WikiResultData();
        rd.initField();
        rd.setId(record.getWikiId().longValue());
        rd.setName(record.getWikiName());
        rd.setUpdateUser(ALEipUtils
          .getALEipUser(record.getUpdateUserId())
          .getAliasName()
          .getValue());
        rd.setCreateDate(record.getCreateDate());
        rd.setUpdateDate(record.getUpdateDate());
        categoryList.add(rd);
      }
    } catch (Exception ex) {
      logger.error("wiki", ex);
      return null;
    }
    return categoryList;
  }

  /**
   * 表示切り替えで指定したグループ ID を取得する．
   * 
   * @param rundata
   * @param context
   * @return
   */
  public static String getTargetGroupName(RunData rundata, Context context) {
    String target_group_name = null;
    String idParam = rundata.getParameters().getString(TARGET_GROUP_NAME);
    target_group_name = ALEipUtils.getTemp(rundata, context, TARGET_GROUP_NAME);
    if (idParam == null && target_group_name == null) {
      ALEipUtils.setTemp(rundata, context, TARGET_GROUP_NAME, "all");
      target_group_name = "all";
    } else if (idParam != null) {
      ALEipUtils.setTemp(rundata, context, TARGET_GROUP_NAME, idParam);
      target_group_name = idParam;
    }
    return target_group_name;
  }

  /**
   * 表示切り替えで指定したユーザ ID を取得する．
   * 
   * @param rundata
   * @param context
   * @return
   */
  public static String getTargetUserId(RunData rundata, Context context) {
    String target_user_id = null;
    String idParam = rundata.getParameters().getString(TARGET_USER_ID);
    target_user_id = ALEipUtils.getTemp(rundata, context, TARGET_USER_ID);

    if (idParam == null && (target_user_id == null)) {
      ALEipUtils.setTemp(rundata, context, TARGET_USER_ID, "all");
      target_user_id = "all";
    } else if (idParam != null) {
      ALEipUtils.setTemp(rundata, context, TARGET_USER_ID, idParam);
      target_user_id = idParam;
    }
    return target_user_id;
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
    ALEipUtils.setTemp(rundata, context, TARGET_KEYWORD, "");
  }

  /**
   * 表示切り替えのリセットフラグがあるかを返す．
   * 
   * @param rundata
   * @param context
   * @return
   */
  public static boolean hasResetTargetFlag(RunData rundata, Context context) {
    String resetflag = rundata.getParameters().getString(RESET_TARGET_FLAG);
    return resetflag != null;
  }

  /**
   * フィルターを初期化する．
   * 
   * @param rundata
   * @param context
   * @param className
   */
  public static void resetTarget(RunData rundata, Context context,
      String className) {
    ALEipUtils.setTemp(rundata, context, TARGET_GROUP_NAME, "all");
    ALEipUtils.setTemp(rundata, context, TARGET_USER_ID, "all");
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
    ALEipUtils.removeTemp(rundata, context, new StringBuffer()
      .append(className)
      .append(ALEipConstants.LIST_FILTER)
      .toString());
    ALEipUtils.removeTemp(rundata, context, new StringBuffer()
      .append(className)
      .append(ALEipConstants.LIST_FILTER_TYPE)
      .toString());
    ALEipUtils.setTemp(rundata, context, TARGET_KEYWORD, "");
  }

  /**
   * エントリーオブジェクトのIDを取得します。 <BR>
   *
   * @param rundata
   * @param context
   * @return
   */
  public static Integer getEipTWikiId(RunData rundata, Context context) {
    String wikiId =
      ALEipUtils.getTemp(rundata, context, ALEipConstants.ENTITY_ID);
    Integer id = null;
    if (wikiId == null || (id = Integer.valueOf(wikiId)) == null) {
      // Todo IDが空の場合
      logger.debug("[WikiUtils] Empty ID...");
      return null;
    } else {
      return id;
    }
  }

  public static EipTWiki getEipTWiki(RunData rundata, Context context)
      throws ALPageNotFoundException, ALDBErrorException {
    String id = ALEipUtils.getTemp(rundata, context, ALEipConstants.ENTITY_ID);
    try {
      if (StringUtils.isEmpty(id)) {
        return null;
      }
      return Database.get(EipTWiki.class, Integer.valueOf(id));
    } catch (Exception e) {
      logger.error("[WikiUtils]", e);
      throw new ALDBErrorException();
    }
  }

  public static EipTWiki getEipTWiki(Integer pk) {
    return Database.get(EipTWiki.class, pk);
  }

  public static int getEipTWikiCategoryWikiCount(EipTWiki category) {
    SelectQuery<EipTWiki> query = Database.query(EipTWiki.class);
    query.setQualifier(ExpressionFactory.matchDbExp(
      EipTWiki.PARENT_ID_PROPERTY,
      category.getWikiId()));
    return query.getCount();

  }

  public static boolean isTitleDuplicate(String title, Integer parentId) {
    return isTitleDuplicate(title, parentId, null);
  }

  public static boolean isTitleDuplicate(String title, Integer parentId,
      Integer pk) {
    SelectQuery<EipTWiki> query = Database.query(EipTWiki.class);
    Expression titleExp =
      ExpressionFactory.likeIgnoreCaseExp(EipTWiki.WIKI_NAME_PROPERTY, title);
    Expression parentExp =
      ExpressionFactory.matchExp(EipTWiki.PARENT_ID_PROPERTY, parentId);
    query.andQualifier(titleExp).andQualifier(parentExp);
    if (pk != null) {
      Expression pkExp =
        ExpressionFactory.noMatchDbExp(EipTWiki.WIKI_ID_PK_COLUMN, pk);
      query.andQualifier(pkExp);
    }
    int result = query.getCount();
    return result != 0;
  }

  public static EipTWiki getEipTWiki(String name, String parentId) {
    SelectQuery<EipTWiki> query = Database.query(EipTWiki.class);
    Expression nameExp =
      ExpressionFactory.likeIgnoreCaseExp(EipTWiki.WIKI_NAME_PROPERTY, name);
    Expression parentExp =
      ExpressionFactory.matchExp(EipTWiki.PARENT_ID_PROPERTY, parentId);
    query.setQualifier(nameExp.andExp(parentExp));
    query.orderAscending(EipTWiki.WIKI_NAME_PROPERTY);
    return query.fetchSingle();
  }

  public static EipTWiki getEipTWikiOne() {
    SelectQuery<EipTWiki> query = Database.query(EipTWiki.class);
    Expression parentExp =
      ExpressionFactory.matchExp(EipTWiki.PARENT_ID_PROPERTY, PARENT_WIKI);
    query.setQualifier(parentExp);
    query.orderAscending(EipTWiki.WIKI_NAME_PROPERTY);
    return query.fetchSingle();
  }

  public static int getChildCount(Integer parentId) {
    SelectQuery<EipTWiki> query = Database.query(EipTWiki.class);
    Expression exp =
      ExpressionFactory.matchExp(EipTWiki.PARENT_ID_PROPERTY, parentId);
    return query.andQualifier(exp).getCount();
  }

  public static String getWikiIdFromSession(String filter, String filterType) {
    String sesFilter = filter == null ? "" : filter;
    String sesFilterType = filterType == null ? "" : filterType;

    if ("category".equals(sesFilterType)) {
      if (StringUtils.isNotEmpty(sesFilter) && StringUtils.isNumeric(sesFilter)) {
        return sesFilter;
      }
    } else if ("post,category".equals(sesFilterType)) {
      String[] splited = filter.split(",");
      if (splited.length < 2) {
        return "";
      }
      String id = filter.split(",")[1];
      if (StringUtils.isNotEmpty(id) && StringUtils.isNumeric(id)) {
        return id;
      }
    }

    return "";
  }

  public static boolean existWiki(String wikiId,
      List<WikiResultData> topWikiList) {
    for (WikiResultData data : topWikiList) {
      if (data.getId().toString().equals(wikiId)) {
        return true;
      }
    }
    return false;
  }

}
