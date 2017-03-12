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
import java.util.List;
import java.util.jar.Attributes;

import org.apache.jetspeed.services.logging.JetspeedLogFactoryService;
import org.apache.jetspeed.services.logging.JetspeedLogger;
import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;

import com.aimluck.commons.field.ALStringField;
import com.aimluck.eip.account.util.AccountUtils;
import com.aimluck.eip.cayenne.om.account.EipMCompany;
import com.aimluck.eip.common.ALAbstractSelectData;
import com.aimluck.eip.common.ALBaseUser;
import com.aimluck.eip.common.ALEipManager;
import com.aimluck.eip.orm.query.ResultList;
import com.aimluck.eip.util.ALEipUtils;

/**
 * ユーザーアカウントの検索データを管理するためのクラスです。 <br />
 */
public class AccountEditSelectData extends
    ALAbstractSelectData<ALBaseUser, ALBaseUser> {

  /** logger */
  private static final JetspeedLogger logger = JetspeedLogFactoryService
    .getLogger(AccountEditSelectData.class.getName());

  /**
   * @param rundata
   * @param context
   * @return
   */
  @Override
  protected ResultList<ALBaseUser> selectList(RunData rundata, Context context) {
    return null;
  }

  /**
   * @param rundata
   * @param context
   * @return
   */
  @Override
  protected ALBaseUser selectDetail(RunData rundata, Context context) {
    ALBaseUser baseUser = AccountUtils.getBaseUser(rundata, context);
    return baseUser;
  }

  /**
   *
   * @param obj
   * @return
   */
  @Override
  protected Object getResultData(ALBaseUser obj) {
    return null;
  }

  /**
   *
   * @param record
   * @return
   */
  @Override
  protected Object getResultDataDetail(ALBaseUser record) {
    try {

      List<ALStringField> postNames =
        ALEipUtils.getPostNameList(Integer.valueOf(record.getUserId()));
      List<Integer> postIds =
        ALEipUtils.getPostIdList(Integer.valueOf(record.getUserId()));
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
      rd.setCompanyId(record.getCompanyId());

      rd.setHasPhoto(record.hasPhoto());
      rd.setIsNewPhotoSpec("N".equals(record.hasPhotoString()));
      rd.setPhotoModified(record.getPhotoModified().getTime());

      rd.setPostIdList(postIds);
      rd.setPostNameList(postNames);
      rd.setPositionName(getPositionName(record.getPositionId()));
      rd.setCode(record.getCode());

      EipMCompany company_data =
        AccountUtils.getEipMCompany(Long
          .toString(rd.getCompany_id().getValue()));
      if (company_data != null) {
        // 会社情報を保存します。
        rd.setCompanyId(company_data.getCompanyId().intValue());
        rd.setCompanyName(company_data.getCompanyName());
        rd.setCompanyZipcode(company_data.getZipcode());
        rd.setCompanyAddress(company_data.getAddress());
        rd.setCompanyTelephone(company_data.getTelephone());
        rd.setCompanyFaxNumber(company_data.getFaxNumber());
      }
      return rd;

    } catch (Exception ex) {
      logger.error("AccountEditSelectData.getResultDataDetail", ex);
      return null;
    }
  }

  /**
   *
   * @return
   */
  @Override
  protected Attributes getColumnMap() {
    return null;
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
  private String getPositionName(int id) {
    if (ALEipManager.getInstance().getPositionMap().containsKey(
      Integer.valueOf(id))) {
      return (ALEipManager.getInstance().getPositionMap().get(Integer
        .valueOf(id))).getPositionName().getValue();
    }
    return null;
  }

  public int getRandomNum() {
    SecureRandom random = new SecureRandom();
    return (random.nextInt() * 100);
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
