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
package com.aimluck.eip.account;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.jar.Attributes;

import org.apache.cayenne.ObjectId;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.jetspeed.om.security.Group;
import org.apache.jetspeed.om.security.Role;
import org.apache.jetspeed.services.JetspeedSecurity;
import org.apache.jetspeed.services.logging.JetspeedLogFactoryService;
import org.apache.jetspeed.services.logging.JetspeedLogger;
import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;

import com.aimluck.commons.field.ALStringField;
import com.aimluck.commons.utils.ALStringUtil;
import com.aimluck.eip.account.util.AccountUtils;
import com.aimluck.eip.account.util.AccountUtils.FilterRole;
import com.aimluck.eip.cayenne.om.account.EipMUserPosition;
import com.aimluck.eip.cayenne.om.security.TurbineGroup;
import com.aimluck.eip.cayenne.om.security.TurbineUser;
import com.aimluck.eip.cayenne.om.security.TurbineUserGroupRole;
import com.aimluck.eip.common.ALAbstractSelectData;
import com.aimluck.eip.common.ALBaseUser;
import com.aimluck.eip.common.ALDBErrorException;
import com.aimluck.eip.common.ALEipManager;
import com.aimluck.eip.common.ALEipPost;
import com.aimluck.eip.common.ALPageNotFoundException;
import com.aimluck.eip.modules.actions.common.ALAction;
import com.aimluck.eip.orm.Database;
import com.aimluck.eip.orm.query.Operations;
import com.aimluck.eip.orm.query.ResultList;
import com.aimluck.eip.orm.query.SelectQuery;
import com.aimluck.eip.util.ALEipUtils;

/**
 * ユーザーアカウントの検索データを管理するためのクラスです。 <br />
 *
 */
public class AccountUserSelectData extends
    ALAbstractSelectData<TurbineUser, ALBaseUser> {

  /** logger */
  private static final JetspeedLogger logger = JetspeedLogFactoryService
    .getLogger(AccountUserSelectData.class.getName());

  /** 現在表示している部署 */
  protected String currentPost = "";

  /** 現在表示しているユーザー */
  protected String currentRole = "";

  protected int registeredUserNum = 0;

  protected static final String LIST_FILTER_ROLE = "filter_role";

  protected final String LIST_FILTER_ROLE_STR = new StringBuffer().append(
    this.getClass().getName()).append(LIST_FILTER_ROLE).toString();

  private boolean adminFilter;

  protected ALStringField target_keyword;

  /** <code>userid</code> ユーザーID */
  private int userid;

  /**
   * 初期化します。
   *
   */
  @Override
  public void init(ALAction action, RunData rundata, Context context)
      throws ALPageNotFoundException, ALDBErrorException {
    String sort = ALEipUtils.getTemp(rundata, context, LIST_SORT_STR);
    if (sort == null || sort.equals("")) {
      ALEipUtils.setTemp(rundata, context, LIST_SORT_STR, "userposition");
    }

    if (rundata.getParameters().containsKey(LIST_FILTER_ROLE)) {
      ALEipUtils.setTemp(rundata, context, LIST_FILTER_ROLE_STR, rundata
        .getParameters()
        .getString(LIST_FILTER_ROLE));
    }

    target_keyword = new ALStringField();

    // ログインユーザの ID を設定する．
    userid = ALEipUtils.getUserId(rundata);

    super.init(action, rundata, context);
  }

  /**
   * アカウント一覧を取得します。 ただし、論理削除されているアカウントは取得しません。
   *
   * @param rundata
   * @param context
   * @return
   */
  @Override
  protected ResultList<TurbineUser> selectList(RunData rundata, Context context) {
    try {
      // 登録済みのユーザ数をデータベースから取得
      target_keyword.setValue(AccountUtils.getTargetKeyword(rundata, context));

      SelectQuery<TurbineUser> query = getSelectQuery(rundata, context);
      buildSelectQueryForListView(query);
      buildSelectQueryForListViewSort(query, rundata, context);
      ResultList<TurbineUser> list = query.getResultList();

      registeredUserNum = list.getTotalCount();

      return list;
    } catch (Exception ex) {
      logger.error("AccountUserSelectData.selectList", ex);
      return null;
    }
  }

  /**
   * 検索条件を設定した SelectQuery を返します。 <BR>
   *
   * @param rundata
   * @param context
   * @return
   */
  protected SelectQuery<TurbineUser> getSelectQuery(RunData rundata,
      Context context) {

    ObjectId oid =
      new ObjectId("TurbineUser", TurbineUser.USER_ID_PK_COLUMN, 3);

    Expression exp_base =
      ExpressionFactory.matchAllDbExp(
        oid.getIdSnapshot(),
        Expression.GREATER_THAN);

    SelectQuery<TurbineUser> query =
      Database.query(TurbineUser.class, exp_base).where(
        Operations.eq(TurbineUser.COMPANY_ID_PROPERTY, Integer.valueOf(1)),
        Operations.ne(TurbineUser.DISABLED_PROPERTY, "T"));

    String filter = ALEipUtils.getTemp(rundata, context, LIST_FILTER_STR);
    String filter_role =
      ALEipUtils.getTemp(rundata, context, LIST_FILTER_ROLE_STR);
    currentPost = filter;
    currentRole = filter_role;

    if (target_keyword != null && !target_keyword.getValue().equals("")) {
      String transWord =
        ALStringUtil.convertHiragana2Katakana(ALStringUtil
          .convertH2ZKana(target_keyword.getValue()));
      transWord = transWord.replace("　", "").replace(" ", ""); // 全角/半角スペースを削除
      String[] transWords = transWord.split(""); // 1文字ずつに分解

      for (int i = 0; i < transWords.length; i++) {
        Expression exp11 =
          ExpressionFactory.likeExp(TurbineUser.FIRST_NAME_PROPERTY, "%"
            + target_keyword.getValue()
            + "%");
        Expression exp12 =
          ExpressionFactory.likeExp(TurbineUser.LAST_NAME_PROPERTY, "%"
            + target_keyword.getValue()
            + "%");
        Expression exp13 =
          ExpressionFactory.likeExp(TurbineUser.FIRST_NAME_KANA_PROPERTY, "%"
            + target_keyword.getValue()
            + "%");
        Expression exp14 =
          ExpressionFactory.likeExp(TurbineUser.LAST_NAME_KANA_PROPERTY, "%"
            + target_keyword.getValue()
            + "%");
        Expression exp15 =
          ExpressionFactory.likeExp(TurbineUser.EMAIL_PROPERTY, "%"
            + target_keyword.getValue()
            + "%");
        Expression exp16 =
          ExpressionFactory.likeExp(
            TurbineUser.TURBINE_USER_GROUP_ROLE_PROPERTY
              + "."
              + TurbineUserGroupRole.TURBINE_GROUP_PROPERTY
              + "."
              + TurbineGroup.GROUP_ALIAS_NAME_PROPERTY,
            "%" + target_keyword.getValue() + "%");
        Expression exp17 =
          ExpressionFactory.likeExp(TurbineUser.LOGIN_NAME_PROPERTY, "%"
            + target_keyword.getValue()
            + "%");
        Expression exp21 =
          ExpressionFactory.likeExp(TurbineUser.OUT_TELEPHONE_PROPERTY, "%"
            + target_keyword.getValue()
            + "%");
        Expression exp22 =
          ExpressionFactory.likeExp(TurbineUser.IN_TELEPHONE_PROPERTY, "%"
            + target_keyword.getValue()
            + "%");
        Expression exp23 =
          ExpressionFactory.likeExp(TurbineUser.CELLULAR_PHONE_PROPERTY, "%"
            + target_keyword.getValue()
            + "%");
        Expression exp31 =
          ExpressionFactory.likeExp(TurbineUser.FIRST_NAME_PROPERTY, "%"
            + transWords[i]
            + "%");
        Expression exp32 =
          ExpressionFactory.likeExp(TurbineUser.LAST_NAME_PROPERTY, "%"
            + transWords[i]
            + "%");
        Expression exp33 =
          ExpressionFactory.likeExp(TurbineUser.FIRST_NAME_KANA_PROPERTY, "%"
            + transWords[i]
            + "%");
        Expression exp34 =
          ExpressionFactory.likeExp(TurbineUser.LAST_NAME_KANA_PROPERTY, "%"
            + transWords[i]
            + "%");
        Expression exp35 =
          ExpressionFactory.likeExp(
            TurbineUser.TURBINE_USER_GROUP_ROLE_PROPERTY
              + "."
              + TurbineUserGroupRole.TURBINE_GROUP_PROPERTY
              + "."
              + TurbineGroup.GROUP_ALIAS_NAME_PROPERTY,
            "%" + transWords[i] + "%");
        Expression exp36 =
          ExpressionFactory.likeExp(TurbineUser.CODE_PROPERTY, "%"
            + transWords[i]
            + "%");

        query.andQualifier(exp11.orExp(exp12).orExp(exp13).orExp(exp14).orExp(
          exp15).orExp(exp16).orExp(exp17).orExp(exp21).orExp(exp22).orExp(
          exp23).orExp(exp31).orExp(exp32).orExp(exp33).orExp(exp34).orExp(
          exp35).orExp(exp36));
      }
    }

    // ユーザーの状態によるフィルターが指定されている場合。
    if (filter_role != null && !filter_role.equals("")) {

      // 管理者かどうか
      if (filter_role.equals(AccountUtils.ROLE_ADMIN.toString())) {
        try {
          Group group = JetspeedSecurity.getGroup("LoginUser");
          Role adminrole = JetspeedSecurity.getRole("admin");
          List<TurbineUserGroupRole> admins =
            Database
              .query(TurbineUserGroupRole.class)
              .where(
                Operations.eq(
                  TurbineUserGroupRole.TURBINE_ROLE_PROPERTY,
                  adminrole.getId()),
                Operations.eq(
                  TurbineUserGroupRole.TURBINE_GROUP_PROPERTY,
                  group.getId()),
                Operations.ne(TurbineUserGroupRole.TURBINE_USER_PROPERTY, 1))
              .distinct(true)
              .fetchList();
          List<Integer> admin_ids = new ArrayList<Integer>();
          admin_ids.add(Integer.valueOf(1));
          for (TurbineUserGroupRole tugr : admins) {
            admin_ids.add(tugr.getTurbineUser().getUserId());
          }
          query.andQualifier(ExpressionFactory.inDbExp(
            TurbineUser.USER_ID_PK_COLUMN,
            admin_ids));

        } catch (Exception ex) {
          logger.error("AccountUserSelectData.getSelectQuery", ex);
        }

      }

      // 有効ユーザーかどうか
      if (filter_role.equals(AccountUtils.ROLE_ACTIVE.toString())) {
        query.andQualifier(ExpressionFactory.matchExp(
          TurbineUser.DISABLED_PROPERTY,
          "F"));
      }

      // 有効ユーザーかどうか
      if (filter_role.equals(AccountUtils.ROLE_IN_ACTIVE.toString())) {
        query.andQualifier(ExpressionFactory.matchExp(
          TurbineUser.DISABLED_PROPERTY,
          "N"));
      }
    }

    // 部署によるフィルターが指定されている場合。
    if (filter != null && !filter.equals("")) {

      String groupName =
        (ALEipManager.getInstance().getPostMap().get(Integer.valueOf(filter)))
          .getGroupName()
          .getValue();

      query.where(Operations.eq(TurbineUser.TURBINE_USER_GROUP_ROLE_PROPERTY
        + "."
        + TurbineUserGroupRole.TURBINE_GROUP_PROPERTY
        + "."
        + TurbineGroup.GROUP_NAME_PROPERTY, groupName));
    }

    query.distinct();

    return query;
  }

  /**
   * フィルタ用の <code>Criteria</code> を構築します。
   *
   * @param crt
   * @param rundata
   * @param context
   * @return
   */
  @Override
  protected SelectQuery<TurbineUser> buildSelectQueryForFilter(
      SelectQuery<TurbineUser> query, RunData rundata, Context context) {
    // 指定部署IDの取得
    String filter = ALEipUtils.getTemp(rundata, context, LIST_FILTER_STR);
    String filter_role =
      ALEipUtils.getTemp(rundata, context, LIST_FILTER_ROLE_STR);

    // 指定部署が存在しているかを確認し、存在していなければ値を削除する
    Map<Integer, ALEipPost> gMap = ALEipManager.getInstance().getPostMap();
    if (filter != null
      && filter.trim().length() != 0
      && !gMap.containsKey(Integer.valueOf(filter))) {
      filter = null;
    }

    String filter_type =
      ALEipUtils.getTemp(rundata, context, LIST_FILTER_TYPE_STR);
    String crt_key = null;
    Attributes map = getColumnMap();
    if (filter == null || filter_type == null || filter.equals("")) {
      return query;
    }
    crt_key = map.getValue(filter_type);
    if (crt_key == null) {
      return query;
    }

    Expression exp = ExpressionFactory.matchDbExp(crt_key, filter);
    query.andQualifier(exp);

    currentPost = filter;
    currentRole = filter_role;
    return query;
  }

  /**
   *
   * @param id
   * @return
   */
  @SuppressWarnings("unused")
  private String getPostName(int id) {
    if (ALEipManager
      .getInstance()
      .getPostMap()
      .containsKey(Integer.valueOf(id))) {
      return (ALEipManager.getInstance().getPostMap().get(Integer.valueOf(id)))
        .getPostName()
        .getValue();
    }
    return null;
  }

  /**
   *
   * @param id
   * @return
   */
  @SuppressWarnings("unused")
  private String getPositionName(int id) {
    if (ALEipManager.getInstance().getPositionMap().containsKey(
      Integer.valueOf(id))) {
      return (ALEipManager.getInstance().getPositionMap().get(Integer
        .valueOf(id))).getPositionName().getValue();
    }
    return null;
  }

  /**
   * @param rundata
   * @param context
   * @return
   */
  @Override
  protected ALBaseUser selectDetail(RunData rundata, Context context) {
    return AccountUtils.getBaseUser(rundata, context);
  }

  /**
   * @param obj
   * @return
   *
   */
  @Override
  protected Object getResultData(TurbineUser record) {
    try {

      AccountResultData rd = new AccountResultData();
      rd.initField();
      rd.setUserId(record.getUserId().intValue());
      rd.setUserName(record.getLoginName());
      rd.setName(new StringBuffer()
        .append(record.getLastName())
        .append(" ")
        .append(record.getFirstName())
        .toString());
      rd.setPostNameList(ALEipUtils.getPostNameList(record.getUserId()));
      rd.setPositionName(ALEipUtils.getPositionName(record
        .getPositionId()
        .intValue()));
      rd.setDisabled(record.getDisabled());
      rd.setHasPhoto("T".equals(record.getHasPhoto())
        || "N".equals(record.getHasPhoto()));
      rd.setPhotoModified(record.getPhotoModified().getTime());
      rd.setEmail(record.getEmail());

      return rd;
    } catch (Exception ex) {
      logger.error("AccountUserSelectData.getResultData", ex);
      return null;
    }
  }

  /**
   * @param obj
   * @return
   */
  @Override
  protected Object getResultDataDetail(ALBaseUser record) {
    try {
      Integer id = Integer.valueOf(record.getUserId());
      AccountResultData rd = new AccountResultData();
      rd.initField();
      rd.setUserId(Integer.valueOf(record.getUserId()).intValue());
      rd.setUserName(record.getUserName());
      rd.setName(new StringBuffer()
        .append(record.getLastName())
        .append(" ")
        .append(record.getFirstName())
        .toString());
      rd.setNameKana(new StringBuffer()
        .append(record.getLastNameKana())
        .append(" ")
        .append(record.getFirstNameKana())
        .toString());
      rd.setEmail(record.getEmail());
      rd.setOutTelephone(record.getOutTelephone());
      rd.setInTelephone(record.getInTelephone());
      rd.setCellularPhone(record.getCellularPhone());
      rd.setCellularMail(record.getCellularMail());
      rd.setPostNameList(ALEipUtils.getPostNameList(id.intValue()));
      rd.setPositionName(ALEipUtils.getPositionName(record.getPositionId()));
      rd.setDisabled(record.getDisabled());
      rd.setIsAdmin(ALEipUtils.isAdmin(Integer.valueOf(record.getUserId())));
      rd.setHasPhoto(record.hasPhoto());
      rd.setIsNewPhotoSpec("N".equals(record.hasPhotoString()));
      rd.setPhotoModified(record.getPhotoModified().getTime());
      rd.setIsOwner(userid == Integer.valueOf(record.getUserId()));
      rd.setCode(record.getCode());

      return rd;
    } catch (Exception ex) {
      logger.error("AccountUserSelectData.getResultDataDetail", ex);
      return null;
    }
  }

  /**
   * @return
   *
   */
  @Override
  protected Attributes getColumnMap() {
    Attributes map = new Attributes();
    map.putValue("post", "POST_ID");
    map.putValue("login_name", TurbineUser.LOGIN_NAME_PROPERTY);
    map.putValue("name_kana", TurbineUser.LAST_NAME_KANA_PROPERTY);
    map.putValue("userposition", TurbineUser.EIP_MUSER_POSITION_PROPERTY
      + "."
      + EipMUserPosition.POSITION_PROPERTY); // ユーザの順番
    return map;
  }

  /**
   *
   * @return
   */
  public String getCurrentPost() {
    return currentPost;
  }

  /**
   *
   * @return
   */
  public Map<Integer, ALEipPost> getPostMap() {
    return ALEipManager.getInstance().getPostMap();
  }

  /**
   *
   * @return
   */
  public Map<Integer, FilterRole> getRoleMap() {
    return AccountUtils.getRoleMap();
  }

  /**
   * 登録ユーザー数を取得する．
   *
   * @return
   */
  public int getRegisteredUserNum() {
    return registeredUserNum;
  }

  public int getRandomNum() {
    SecureRandom random = new SecureRandom();
    return (random.nextInt() * 100);
  }

  public boolean isAdminFiltered() {
    return adminFilter;
  }

  /**
   * @return currentRole
   */
  public String getCurrentRole() {
    return currentRole;
  }

  public ALStringField getTargetKeyword() {
    return target_keyword;
  }

  /**
   * ファイルアップロードアクセス権限チェック用メソッド。<br />
   * ファイルアップのアクセス権限をチェックするかどうかを判定します。
   *
   * @return
   */
  @Override
  public boolean isCheckAttachmentAuthority() {
    return false;
  }
}
