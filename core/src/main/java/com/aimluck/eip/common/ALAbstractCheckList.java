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
package com.aimluck.eip.common;

import java.util.ArrayList;
import java.util.List;

import org.apache.jetspeed.services.logging.JetspeedLogFactoryService;
import org.apache.jetspeed.services.logging.JetspeedLogger;
import org.apache.turbine.services.TurbineServices;
import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;

import com.aimluck.eip.modules.actions.common.ALAction;
import com.aimluck.eip.services.accessctl.ALAccessControlConstants;
import com.aimluck.eip.services.accessctl.ALAccessControlFactoryService;
import com.aimluck.eip.services.accessctl.ALAccessControlHandler;
import com.aimluck.eip.util.ALEipUtils;

/**
 * チェックボックスで選択された項目に対してのアクションを定義するクラスです。 <br />
 *
 */
public abstract class ALAbstractCheckList {

  /** logger */
  @SuppressWarnings("unused")
  private static final JetspeedLogger logger = JetspeedLogFactoryService
    .getLogger(ALAbstractCheckList.class.getName());

  /** アクセス権限の有無 */
  protected boolean hasAuthority;

  /**
   * チェックリストで選択された項目に対してアクションを実行します。
   *
   * @param action
   * @param rundata
   * @param context
   * @return
   */
  public boolean doMultiAction(ALAction action, RunData rundata, Context context) {
    try {
      if (!doCheckSecurity(rundata, context)) {
        return false;
      }

      doCheckAclPermission(rundata, context, getDefineAclType());

      List<String> values = new ArrayList<String>();
      List<String> msgList = new ArrayList<String>();
      boolean res = false;
      Object[] objs = rundata.getParameters().getKeys();
      int length = objs.length;
      for (int i = 0; i < length; i++) {
        if (objs[i].toString().startsWith("check")) {
          String str = rundata.getParameters().getString(objs[i].toString());
          values.add(str);
          res = true;
        }
      }
      if (res) {
        res = action(rundata, context, values, msgList);
      }
      action.setResultData(this);
      action.setErrorMessages(msgList);
      action.putData(rundata, context);
      return res;
    } catch (ALPermissionException e) {
      ALEipUtils.redirectPermissionError(rundata);
      return false;
    } catch (ALPageNotFoundException e) {
      ALEipUtils.redirectPageNotFound(rundata);
      return false;
    } catch (ALDBErrorException e) {
      ALEipUtils.redirectDBError(rundata);
      return false;
    }
  }

  /**
   * チェックリストで選択された項目に対してアクションを実行するための抽象メソッドです。
   *
   * @param rundata
   * @param context
   * @param values
   *          チェックされた項目の値のリスト
   * @param msgList
   * @return
   */
  protected abstract boolean action(RunData rundata, Context context,
      List<String> values, List<String> msgList)
      throws ALPageNotFoundException, ALDBErrorException;

  /**
   * セキュリティをチェックします。
   *
   * @return
   */
  protected boolean doCheckSecurity(RunData rundata, Context context) {
    String reqSecid =
      rundata.getParameters().getString(ALEipConstants.SECURE_ID);
    String sessionSecid =
      (String) rundata.getUser().getTemp(ALEipConstants.SECURE_ID);
    if (reqSecid == null || !reqSecid.equals(sessionSecid)) {
      return false;
    }

    return true;
  }

  /**
   * アクセス権限をチェックします。
   *
   * @return
   */
  protected boolean doCheckAclPermission(RunData rundata, Context context,
      int defineAclType) throws ALPermissionException {
    if (defineAclType == 0) {
      return true;
    }

    String pfeature = getAclPortletFeature();
    if (pfeature == null || "".equals(pfeature)) {
      return true;
    }

    ALAccessControlFactoryService aclservice =
      (ALAccessControlFactoryService) ((TurbineServices) TurbineServices
        .getInstance()).getService(ALAccessControlFactoryService.SERVICE_NAME);
    ALAccessControlHandler aclhandler = aclservice.getAccessControlHandler();

    hasAuthority =
      aclhandler.hasAuthority(
        ALEipUtils.getUserId(rundata),
        pfeature,
        defineAclType);

    if (!hasAuthority) {
      throw new ALPermissionException();
    }

    return true;
  }

  /**
   * ファイルアップロードのアクセス権限をチェックします。
   *
   * @return
   */
  protected boolean doCheckAttachmentAclPermission(RunData rundata,
      Context context, int defineAclType) {

    if (defineAclType == 0) {
      return true;
    }

    // アクセス権限のチェックをしない場合
    boolean checkAttachmentAuthority = isCheckAttachmentAuthority();
    if (!checkAttachmentAuthority) {
      return true;
    }

    ALAccessControlFactoryService aclservice =
      (ALAccessControlFactoryService) ((TurbineServices) TurbineServices
        .getInstance()).getService(ALAccessControlFactoryService.SERVICE_NAME);
    ALAccessControlHandler aclhandler = aclservice.getAccessControlHandler();

    return aclhandler.hasAuthority(
      ALEipUtils.getUserId(rundata),
      ALAccessControlConstants.POERTLET_FEATURE_ATTACHMENT,
      defineAclType);
  }

  /**
   * ファイルアップロードアクセス権限チェック用メソッド。<br />
   * ファイルアップのアクセス権限をチェックするかどうかを判定します。
   *
   * @return
   */
  public boolean isCheckAttachmentAuthority() {
    return true;
  }

  /**
   * アクセス権限用メソッド。<br />
   * アクセス権限の有無を返します。
   *
   * @return
   */
  public boolean hasAuthority() {
    return hasAuthority;
  }

  /**
   * アクセス権限チェック用メソッド。<br />
   * アクセス権限を返します。
   *
   * @return
   */
  protected int getDefineAclType() {
    return 0;
  }

  /**
   * アクセス権限チェック用メソッド。<br />
   * アクセス権限の機能名を返します。
   *
   * @return
   */
  public String getAclPortletFeature() {
    return null;
  }
}
