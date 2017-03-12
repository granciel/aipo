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

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.apache.jetspeed.services.logging.JetspeedLogFactoryService;
import org.apache.jetspeed.services.logging.JetspeedLogger;
import org.apache.turbine.services.TurbineServices;
import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;

import com.aimluck.commons.field.ALAbstractField;
import com.aimluck.commons.field.ALDateContainer;
import com.aimluck.commons.field.ALDateField;
import com.aimluck.commons.field.ALDateTimeField;
import com.aimluck.eip.modules.actions.common.ALAction;
import com.aimluck.eip.orm.Database;
import com.aimluck.eip.services.accessctl.ALAccessControlConstants;
import com.aimluck.eip.services.accessctl.ALAccessControlFactoryService;
import com.aimluck.eip.services.accessctl.ALAccessControlHandler;
import com.aimluck.eip.services.quota.ALQuotaService;
import com.aimluck.eip.util.ALEipUtils;
import com.aimluck.eip.util.ALLocalizationUtils;

/**
 * フォームデータを管理するための抽象クラスです。 <br />
 *
 */
public abstract class ALAbstractFormData implements ALData {

  /** logger */
  private static final JetspeedLogger logger = JetspeedLogFactoryService
    .getLogger(ALAbstractFormData.class.getName());

  /** 現在のモード */
  protected String mode;

  /** アクセス権限の有無 */
  protected boolean hasAuthority;

  /** アクセス元ブラウザによるファイルアップロード可否 */
  protected boolean isFileUploadable;

  /** 添付ファイル追加へのアクセス権限の有無 */
  private boolean hasAttachmentInsertAuthority;

  /** 添付ファイル削除へのアクセス権限の有無 */
  private boolean hasAttachmentDeleteAuthority;

  /**
   * 初期化処理を行います。 <br />
   * <code>doViewForm/doInsert/doUpdate/doDelete</code> 実行時に呼ばれます。 <br />
   * 下位クラスで初期化処理を追記する場合は、このメソッドをオーバーライドしてください。
   *
   * @param action
   * @param rundata
   * @param context
   */
  public void init(ALAction action, RunData rundata, Context context)
      throws ALPageNotFoundException, ALDBErrorException {

    // 自ポートレットからのリクエストであれば、パラメータを展開しセッションに保存する。
    if (ALEipUtils.isMatch(rundata, context)) {
      // ENTITY ID
      if (rundata.getParameters().containsKey(ALEipConstants.ENTITY_ID)) {
        // entityid=new を指定することによって明示的にセッション変数を削除することができる。
        if (rundata.getParameters().getString(ALEipConstants.ENTITY_ID).equals(
          "new")) {
          ALEipUtils.removeTemp(rundata, context, ALEipConstants.ENTITY_ID);
        } else {
          ALEipUtils.setTemp(
            rundata,
            context,
            ALEipConstants.ENTITY_ID,
            rundata.getParameters().getString(ALEipConstants.ENTITY_ID));
        }
      }
    }
    
    isFileUploadable = ALEipUtils.isFileUploadable(rundata);
  }

  /**
   * 指定されたフィールドのフィールド名を取得します。 <br />
   * フィールド名が取得できない場合はNULLを返します。
   *
   * @param argString
   * @return フィールド名
   */
  public String getFieldName(String argString) {
    String fieldName = null;
    try {
      Field f = this.getClass().getDeclaredField(argString);
      f.setAccessible(true);
      ALAbstractField field = (ALAbstractField) f.get(this);
      fieldName = field.getFieldName();
    } catch (Exception ex) {
      logger.error("ALAbstractFormData.getFieldName", ex);
    }
    return fieldName;
  }

  /**
   * フォームを表示します。
   *
   * @param action
   * @param rundata
   * @param context
   * @return TRUE 成功 FALSE 失敗
   */
  public boolean doViewForm(ALAction action, RunData rundata, Context context) {
    try {
      init(action, rundata, context);
      boolean isedit =
        (ALEipUtils.getTemp(rundata, context, ALEipConstants.ENTITY_ID) != null);

      int aclType = ALAccessControlConstants.VALUE_ACL_INSERT;
      if (isedit) {
        aclType = ALAccessControlConstants.VALUE_ACL_UPDATE;
      }
      doCheckAclPermission(rundata, context, aclType);

      doCheckAttachmentInsertAclPermission(rundata, context);
      doCheckAttachmentDeleteAclPermission(rundata, context);

      action.setMode(isedit
        ? ALEipConstants.MODE_EDIT_FORM
        : ALEipConstants.MODE_NEW_FORM);
      mode = action.getMode();

      List<String> msgList = new ArrayList<String>();
      boolean res =
        (isedit) ? loadFormData(rundata, context, msgList) : setFormData(
          rundata,
          context,
          msgList);
      action.setResultData(this);
      if (!msgList.isEmpty()) {
        action.addErrorMessages(msgList);
      }
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

  protected boolean isOverQuota() {
    return ALQuotaService.isOverQuota(Database.getDomainName());
  }

  /**
   * データを新規登録します。
   *
   * @param action
   * @param rundata
   * @param context
   * @return TRUE 成功 FALSE 失敗
   */
  public boolean doInsert(ALAction action, RunData rundata, Context context) {
    List<String> msgList = new ArrayList<String>();
    try {
      if (!doCheckSecurity(rundata, context)) {
        msgList.add(ALLocalizationUtils.getl10n("ERROR_SECID_UNMATCH"));
        action.addErrorMessages(msgList);
        action.putData(rundata, context);
        return false;
      }

      init(action, rundata, context);

      doCheckAclPermission(
        rundata,
        context,
        ALAccessControlConstants.VALUE_ACL_INSERT);

      doCheckAttachmentInsertAclPermission(rundata, context);
      doCheckAttachmentDeleteAclPermission(rundata, context);

      action.setMode(ALEipConstants.MODE_INSERT);
      mode = action.getMode();
      rundata.getParameters().add(
        ALEipConstants.MODE,
        ALEipConstants.MODE_INSERT);
      setValidator();

      boolean res = false;
      if (isOverQuota()) {
        msgList.add(ALLocalizationUtils
          .getl10n("COMMON_FULL_DISK_DELETE_DETA_OR_CHANGE_PLAN"));
      } else {
        res =
          (setFormData(rundata, context, msgList)
            && validate(msgList)
            && extValidate(rundata, context, msgList) && insertFormData(
            rundata,
            context,
            msgList));
      }
      if (!res) {
        action.setMode(ALEipConstants.MODE_NEW_FORM);
        mode = action.getMode();
      }
      action.setResultData(this);
      if (!msgList.isEmpty()) {
        action.addErrorMessages(msgList);
      } else if (!res) {
        msgList.add(ALLocalizationUtils.getl10n("ERROR_INSERT_FAILURE"));
        action.addErrorMessages(msgList);
      }
      action.putData(rundata, context);

      return res;
    } catch (ALPermissionException e) {
      ALEipUtils.redirectPermissionError(rundata);
      return false;
    } catch (ALPageNotFoundException e) {
      ALEipUtils.redirectPageNotFound(rundata);
      return false;
    } catch (ALDBErrorException e) {
      msgList.add(ALLocalizationUtils.getl10n("ERROR_INSERT_FAILURE"));
      action.addErrorMessages(msgList);
      action.putData(rundata, context);
      return false;
    }
  }

  /**
   * データを更新します。
   *
   * @param action
   * @param rundata
   * @param context
   * @return TRUE 成功 FALSE 失敗
   */
  public boolean doUpdate(ALAction action, RunData rundata, Context context) {
    List<String> msgList = new ArrayList<String>();
    try {
      if (!doCheckSecurity(rundata, context)) {
        return false;
      }

      init(action, rundata, context);

      doCheckAclPermission(
        rundata,
        context,
        ALAccessControlConstants.VALUE_ACL_UPDATE);

      doCheckAttachmentInsertAclPermission(rundata, context);
      doCheckAttachmentDeleteAclPermission(rundata, context);

      action.setMode(ALEipConstants.MODE_UPDATE);
      mode = action.getMode();
      rundata.getParameters().add(
        ALEipConstants.MODE,
        ALEipConstants.MODE_UPDATE);
      setValidator();

      boolean res = false;
      if (isOverQuota()) {
        msgList.add(ALLocalizationUtils
          .getl10n("COMMON_FULL_DISK_DELETE_DETA_OR_CHANGE_PLAN"));
      } else {
        res =
          (setFormData(rundata, context, msgList)
            && validate(msgList)
            && extValidate(rundata, context, msgList) && updateFormData(
            rundata,
            context,
            msgList));
      }

      if (!res) {
        action.setMode(ALEipConstants.MODE_EDIT_FORM);
        mode = action.getMode();
      }
      action.setResultData(this);
      if (!msgList.isEmpty()) {
        action.addErrorMessages(msgList);
      } else if (!res) {
        msgList.add(ALLocalizationUtils.getl10n("ERROR_UPDATE_FAILURE"));
        action.addErrorMessages(msgList);
      }
      action.putData(rundata, context);

      return res;
    } catch (ALPermissionException e) {
      ALEipUtils.redirectPermissionError(rundata);
      return false;
    } catch (ALPageNotFoundException e) {
      ALEipUtils.redirectPageNotFound(rundata);
      return false;
    } catch (ALDBErrorException e) {
      msgList.add(ALLocalizationUtils.getl10n("ERROR_UPDATE_FAILURE"));
      action.addErrorMessages(msgList);
      action.putData(rundata, context);
      return false;
    }
  }

  /**
   * データを削除します。
   *
   * @param action
   * @param rundata
   * @param context
   * @return TRUE 成功 FALSE 失敗
   */
  public boolean doDelete(ALAction action, RunData rundata, Context context) {
    try {
      if (!doCheckSecurity(rundata, context)) {
        return false;
      }

      init(action, rundata, context);

      doCheckAclPermission(
        rundata,
        context,
        ALAccessControlConstants.VALUE_ACL_DELETE);

      doCheckAttachmentInsertAclPermission(rundata, context);
      doCheckAttachmentDeleteAclPermission(rundata, context);

      action.setMode(ALEipConstants.MODE_DELETE);
      mode = action.getMode();
      rundata.getParameters().add(
        ALEipConstants.MODE,
        ALEipConstants.MODE_DELETE);

      List<String> msgList = new ArrayList<String>();
      boolean res =
        extValidate(rundata, context, msgList)
          && deleteFormData(rundata, context, msgList);
      action.setResultData(this);
      if (!msgList.isEmpty()) {
        action.addErrorMessages(msgList);
      } else if (!res) {
        msgList.add(ALLocalizationUtils.getl10n("ERROR_DELETE_FAILURE"));
        action.addErrorMessages(msgList);
      }
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
   * データに値を設定します。
   *
   * @param rundata
   * @param context
   * @param msgList
   *          エラーメッセージのリスト
   * @return TRUE 成功 FALSE 失敗
   */
  protected boolean setFormData(RunData rundata, Context context,
      List<String> msgList) throws ALPageNotFoundException, ALDBErrorException {
    try {
      Field[] fields = this.getClass().getDeclaredFields();
      int length = fields.length;
      for (int i = 0; i < length; i++) {
        fields[i].setAccessible(true);
        String name = fields[i].getName();
        Object obj = fields[i].get(this);
        // フィールドが ALDateTimeField の場合
        if (obj instanceof ALDateTimeField) {
          ALDateTimeField field = (ALDateTimeField) obj;
          String yearString =
            new StringBuffer().append(name).append(
              ALEipConstants.POST_DATE_YEAR).toString();
          String monthString =
            new StringBuffer().append(name).append(
              ALEipConstants.POST_DATE_MONTH).toString();
          String dayString =
            new StringBuffer()
              .append(name)
              .append(ALEipConstants.POST_DATE_DAY)
              .toString();
          String hourString =
            new StringBuffer().append(name).append(
              ALEipConstants.POST_DATE_HOUR).toString();
          String minitusString =
            new StringBuffer().append(name).append(
              ALEipConstants.POST_DATE_MINUTE).toString();
          int year;
          int month;
          int day;
          int hour;
          int minitue;
          if (rundata.getParameters().containsKey(yearString)) {
            year = rundata.getParameters().getInt(yearString);
          } else {
            continue;
          }
          if (rundata.getParameters().containsKey(monthString)) {
            month = rundata.getParameters().getInt(monthString) - 1;
          } else {
            continue;
          }
          if (rundata.getParameters().containsKey(dayString)) {
            day = rundata.getParameters().getInt(dayString);
          } else {
            continue;
          }
          if (rundata.getParameters().containsKey(hourString)) {
            hour = rundata.getParameters().getInt(hourString);
          } else {
            continue;
          }
          if (rundata.getParameters().containsKey(minitusString)) {
            minitue = rundata.getParameters().getInt(minitusString);
          } else {
            continue;
          }
          Calendar cal = Calendar.getInstance();
          cal.set(year, month, day, hour, minitue);
          cal.set(Calendar.SECOND, 0);
          cal.set(Calendar.MILLISECOND, 0);
          field.setValue(cal.getTime());

          // フィールドが ALDateField の場合
        } else if (obj instanceof ALDateField) {
          ALDateField field = (ALDateField) obj;
          String yearString =
            new StringBuffer().append(name).append(
              ALEipConstants.POST_DATE_YEAR).toString();
          String monthString =
            new StringBuffer().append(name).append(
              ALEipConstants.POST_DATE_MONTH).toString();
          String dayString =
            new StringBuffer()
              .append(name)
              .append(ALEipConstants.POST_DATE_DAY)
              .toString();
          ALDateContainer con = new ALDateContainer();
          if (rundata.getParameters().containsKey(yearString)) {
            con.setYear(rundata.getParameters().getString(yearString));
          } else {
            continue;
          }
          if (rundata.getParameters().containsKey(monthString)) {
            con.setMonth(rundata.getParameters().getString(monthString));
          } else {
            continue;
          }
          if (rundata.getParameters().containsKey(dayString)) {
            con.setDay(rundata.getParameters().getString(dayString));
          } else {
            continue;
          }
          field.setValue(con);

          // フィールドが ALAbstractField の場合
        } else if (obj instanceof ALAbstractField) {
          ALAbstractField field = (ALAbstractField) obj;
          if (rundata.getParameters().containsKey(name)) {
            field.setValue(rundata.getParameters().getString(name));
          }
        }
      }
    } catch (IllegalAccessException e) {
      logger.error("ALAbstractFormData.setFormData", e);
      return false;
    } catch (Exception e) {
      logger.error("ALAbstractFormData.setFormData", e);
      return false;

    }
    return true;
  }

  /**
   * 各フィールドに対する制約条件を設定する抽象メソッドです。
   *
   */
  protected abstract void setValidator() throws ALPageNotFoundException,
      ALDBErrorException;

  /**
   * フォームに入力されたデータの妥当性検証を行う抽象メソッドです。
   *
   * @param msgList
   *          エラーメッセージのリスト
   */
  protected abstract boolean validate(List<String> msgList)
      throws ALPageNotFoundException, ALDBErrorException;

  /**
   * データを読み込む抽象メソッドです。
   *
   * @param rundata
   * @param context
   * @param msgList
   *          エラーメッセージのリスト
   * @return TRUE 成功 FALSE 失敗
   */
  protected abstract boolean loadFormData(RunData rundata, Context context,
      List<String> msgList) throws ALPageNotFoundException, ALDBErrorException;

  /**
   * データを新規登録する抽象メソッドです。
   *
   * @param rundata
   * @param context
   * @param msgList
   *          エラーメッセージのリスト
   * @return TRUE 成功 FALSE 失敗
   */
  protected abstract boolean insertFormData(RunData rundata, Context context,
      List<String> msgList) throws ALPageNotFoundException, ALDBErrorException;

  /**
   * データを更新する抽象メソッドです。
   *
   * @param rundata
   * @param context
   * @param msgList
   *          エラーメッセージのリスト
   * @return TRUE 成功 FALSE 失敗
   */
  protected abstract boolean updateFormData(RunData rundata, Context context,
      List<String> msgList) throws ALPageNotFoundException, ALDBErrorException;

  /**
   * データを削除する抽象メソッドです。
   *
   * @param rundata
   * @param context
   * @param msgList
   *          エラーメッセージのリスト
   * @return TRUE 成功 FALSE 失敗
   */
  protected abstract boolean deleteFormData(RunData rundata, Context context,
      List<String> msgList) throws ALPageNotFoundException, ALDBErrorException;

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
   * ファイルアップロードのアクセス権限をチェックします。
   *
   * @return
   */
  protected void doCheckAttachmentInsertAclPermission(RunData rundata,
      Context context) { // ファイル追加権限の有無
    hasAttachmentInsertAuthority =
      doCheckAttachmentAclPermission(
        rundata,
        context,
        ALAccessControlConstants.VALUE_ACL_INSERT);
  }

  /**
   * ファイルアップロードのアクセス権限をチェックします。
   *
   * @return
   */
  protected void doCheckAttachmentDeleteAclPermission(RunData rundata,
      Context context) { // ファイル削除権限の有無
    hasAttachmentDeleteAuthority =
      doCheckAttachmentAclPermission(
        rundata,
        context,
        ALAccessControlConstants.VALUE_ACL_DELETE);
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
   * アクセス権限の機能名を返します。
   *
   * @return
   */
  public String getAclPortletFeature() {
    return null;
  }

  /**
   *
   * @return
   */
  public String getMode() {
    return mode;
  }

  /**
   *
   * @param string
   */
  public void setMode(String string) {
    mode = string;
  }

  public boolean isFileUploadable() {
    return isFileUploadable;
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

  public boolean hasAttachmentInsertAuthority() {
    return hasAttachmentInsertAuthority;
  }

  public boolean hasAttachmentDeleteAuthority() {
    return hasAttachmentDeleteAuthority;
  }

  /**
   * フォームに入力された添データの妥当性検証を行う抽象メソッドです。
   *
   * @param msgList
   *          エラーメッセージのリスト
   */
  protected boolean extValidate(RunData rundata, Context context,
      List<String> msgList) {
    return true;
  }

}
