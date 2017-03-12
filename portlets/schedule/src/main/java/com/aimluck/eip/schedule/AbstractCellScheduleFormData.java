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
package com.aimluck.eip.schedule;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

import org.apache.jetspeed.services.logging.JetspeedLogFactoryService;
import org.apache.jetspeed.services.logging.JetspeedLogger;
import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;

import com.aimluck.commons.field.ALCellStringField;
import com.aimluck.eip.cayenne.om.portlet.EipMFacilityGroup;
import com.aimluck.eip.cayenne.om.portlet.EipTSchedule;
import com.aimluck.eip.common.ALAbstractFormData;
import com.aimluck.eip.common.ALDBErrorException;
import com.aimluck.eip.common.ALEipConstants;
import com.aimluck.eip.common.ALEipGroup;
import com.aimluck.eip.common.ALEipManager;
import com.aimluck.eip.common.ALEipPost;
import com.aimluck.eip.common.ALEipUser;
import com.aimluck.eip.common.ALPageNotFoundException;
import com.aimluck.eip.common.ALPermissionException;
import com.aimluck.eip.modules.actions.common.ALAction;
import com.aimluck.eip.orm.Database;
import com.aimluck.eip.schedule.beans.CellScheduleFormBean;
import com.aimluck.eip.schedule.util.CellScheduleUtils;
import com.aimluck.eip.schedule.util.ScheduleUtils;
import com.aimluck.eip.services.accessctl.ALAccessControlConstants;
import com.aimluck.eip.util.ALEipUtils;

/**
 * スケジュールのフォームデータを管理するクラスです。
 *
 */
public abstract class AbstractCellScheduleFormData extends ALAbstractFormData {

  /** <code>logger</code> logger */
  private static final JetspeedLogger logger = JetspeedLogFactoryService
    .getLogger(AbstractCellScheduleFormData.class.getName());

  /** <code>FLAG_EDIT_REPEAT_DEF</code> デフォルト値（繰り返し編集範囲） */
  public static final int FLAG_EDIT_REPEAT_DEF = -1;

  /** <code>FLAG_EDIT_REPEAT_ALL</code> 個別日程を編集（繰り返し編集範囲） */
  public static final int FLAG_EDIT_REPEAT_ALL = 0;

  /** <code>FLAG_EDIT_SCHEDULE_ONE</code> 全日程を編集（繰り返し編集範囲） */
  public static final int FLAG_EDIT_REPEAT_ONE = 1;

  /** パラメータで持ち回すデータ */
  public CellScheduleFormBean form_data;

  /** <code>is_repeat</code> スケジュールタイプ */
  private ALCellStringField schedule_type;

  /** <code>login_user</code> ログインユーザー */
  private ALEipUser login_user;

  /** <code>selectData</code> 編集するスケジュールの1日の情報 */
  private ScheduleOnedayGroupSelectData selectData;

  /** <code>groups</code> グループ */
  private List<ALEipGroup> groups;

  /** <code>groups</code> グループ */
  private List<EipMFacilityGroup> facilities;

  /** <code>isOwner</code> 所有者かどうか */
  private boolean is_owner;

  private String tmpStart;

  private String tmpEnd;

  private String tmpView;

  protected String entity_id;

  private String aclPortletFeature;

  private boolean is_first;

  protected boolean is_copy;

  /**
   *
   * @param action
   * @param rundata
   * @param context
   * @throws ALPageNotFoundException
   * @throws ALDBErrorException
   */
  @Override
  public void init(ALAction action, RunData rundata, Context context)
      throws ALPageNotFoundException, ALDBErrorException {
    login_user = ALEipUtils.getALEipUser(rundata);

    is_owner = true;

    groups = ALEipUtils.getMyGroups(rundata);

    facilities = Database.query(EipMFacilityGroup.class, null).fetchList();

    entity_id = rundata.getParameters().getString(ALEipConstants.ENTITY_ID);

    aclPortletFeature = ALAccessControlConstants.POERTLET_FEATURE_SCHEDULE_SELF;

    is_first = !("".equals(rundata.getParameters().getString("is_first", "")));

    super.init(action, rundata, context);

    is_copy =
      Boolean.parseBoolean(ALEipUtils.getTemp(rundata, context, "is_copy"));
  }

  @Override
  public boolean doViewForm(ALAction action, RunData rundata, Context context) {
    try {
      init(action, rundata, context);
      boolean isedit =
        (rundata.getParameters().containsKey(ALEipConstants.ENTITY_ID) && !"new"
          .equals(rundata.getParameters().getString(ALEipConstants.ENTITY_ID)));
      int aclType = ALAccessControlConstants.VALUE_ACL_INSERT;
      if (isedit) {
        aclType = ALAccessControlConstants.VALUE_ACL_UPDATE;
      }
      if (is_copy) {
        isedit = false;
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
        (isedit || is_copy)
          ? loadFormData(rundata, context, msgList)
          : setFormData(rundata, context, msgList);
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

  /**
   * パラメータを読み込みます。
   *
   * @param rundata
   * @param context
   */
  public void loadParameters(RunData rundata, Context context) {
    ScheduleUtils.loadParametersDelegate(
      rundata,
      context,
      tmpStart,
      tmpEnd,
      tmpView);
    tmpView = ALEipUtils.getTemp(rundata, context, "tmpView");

    ALEipUtils.setTemp(rundata, context, "tmpStart", tmpView + "-00-00");
    ALEipUtils.setTemp(rundata, context, "tmpEnd", tmpView + "-00-00");
    tmpStart = ALEipUtils.getTemp(rundata, context, "tmpStart");
    tmpEnd = ALEipUtils.getTemp(rundata, context, "tmpEnd");
  }

  /*
   *
   */
  @Override
  public void initField() {
    form_data = new CellScheduleFormBean();
    form_data.initField(tmpStart, tmpEnd, tmpView);

    schedule_type = new ALCellStringField();
  }

  /*
   *
   */
  @Override
  protected void setValidator() {
    form_data.getMonthDay().setNotNull(true);
    form_data.getLimitStartDate().setNotNull(true);
    form_data.getLimitEndDate().setNotNull(true);
  }

  /**
   *
   * @param msgList
   * @return
   * @throws ALDBErrorException
   * @throws ALPageNotFoundException
   */
  @Override
  protected boolean validate(List<String> msgList) throws ALDBErrorException,
      ALPageNotFoundException {
    return form_data.validateDelegate(
      msgList,
      getLoginUser(),
      entity_id,
      schedule_type.getValue());
  }

  /**
   *
   * @param rundata
   * @param context
   * @param msgList
   * @return
   * @throws ALPageNotFoundException
   * @throws ALDBErrorException
   */
  @Override
  protected boolean loadFormData(RunData rundata, Context context,
      List<String> msgList) throws ALPageNotFoundException, ALDBErrorException {
    try {
      // オブジェクトモデルを取得
      EipTSchedule record =
        ScheduleUtils.getEipTSchedule(rundata, context, false);
      if (record == null) {
        return false;
      }

      is_owner =
        (record.getOwnerId().longValue() == login_user.getUserId().getValue())
          ? true
          : false;

      schedule_type.setValue(CellScheduleUtils.getScheduleType(record));

      form_data.setResultData(record);

      loadCustomFormData(record);

      setFormData(rundata, context, msgList);

    } catch (Exception e) {
      logger.error("[ScheduleFormData]", e);
      throw new ALDBErrorException();

    }
    return true;
  }

  protected abstract void loadCustomFormData(EipTSchedule record);

  /**
   * 入力データを検証する．
   *
   * @param action
   * @param rundata
   * @param context
   * @return TRUE 成功 FALSE 失敗
   */
  public boolean doCheck(ALAction action, RunData rundata, Context context) {
    try {
      init(action, rundata, context);
      // action.setMode(ALEipConstants.MODE_INSERT);
      List<String> msgList = new ArrayList<String>();
      setValidator();
      boolean res =
        (setFormData(rundata, context, msgList) && validate(msgList));
      if (!res) {
        action.setMode(ALEipConstants.MODE_NEW_FORM);
      }
      action.setResultData(this);
      action.addErrorMessages(msgList);
      action.putData(rundata, context);
      return res;
    } catch (ALPageNotFoundException e) {
      ALEipUtils.redirectPageNotFound(rundata);
      return false;
    } catch (ALDBErrorException e) {
      ALEipUtils.redirectDBError(rundata);
      return false;
    }
  }

  /**
   *
   * @param rundata
   * @param context
   * @param msgList
   * @return
   * @throws ALPageNotFoundException
   * @throws ALDBErrorException
   */
  @Override
  protected boolean setFormData(RunData rundata, Context context,
      List<String> msgList) throws ALPageNotFoundException, ALDBErrorException {

    String st = rundata.getParameters().get("schedule_type");
    if (st != null) {
      schedule_type.setValue(st);
    }

    Field[] fields = form_data.getClass().getDeclaredFields();
    ScheduleUtils.setFormDataDelegate(
      rundata,
      context,
      form_data,
      fields,
      msgList);

    if (msgList.size() >= 1) {
      return false;
    }

    if (!is_first) {
      form_data.getMemberList().clear();
      form_data.getMemberList().addAll(
        CellScheduleUtils.getShareUserMemberList(rundata));
    }

    if (!isSpan()) {
      Calendar startDate = Calendar.getInstance();
      startDate.setTime(form_data.getStartDate().getValue());

      Calendar endDate = Calendar.getInstance();
      endDate.setTime(form_data.getEndDate().getValue());
      endDate.set(Calendar.YEAR, startDate.get(Calendar.YEAR));
      endDate.set(Calendar.MONTH, startDate.get(Calendar.MONTH));
      endDate.set(Calendar.DATE, startDate.get(Calendar.DATE));
      form_data.getEndDate().setValue(endDate.getTime());
    }

    if (!is_first) {
      form_data.getFacilityMemberList().clear();
      form_data.getFacilityMemberList().addAll(
        CellScheduleUtils.getShareFacilityMemberList(rundata));
    }
    return true;
  }

  /**
   * 指定したグループ名のユーザーを取得します。
   *
   * @param groupname
   * @return
   */
  public List<ALEipUser> getUsers(String groupname) {
    return ALEipUtils.getUsers(groupname);
  }

  /**
   * 部署マップを取得します。
   *
   * @return
   */
  public Map<Integer, ALEipPost> getPostMap() {
    return ALEipManager.getInstance().getPostMap();
  }

  /**
   * フォームデータを取得します。
   *
   * @return
   */
  public CellScheduleFormBean getFormData() {
    return form_data;
  }

  /**
   *
   * @return
   */
  public boolean isOwner() {
    return is_owner;
  }

  public boolean isFirst() {
    return is_first;
  }

  public boolean isOneday() {
    return CellScheduleUtils.SCHEDULE_TYPE_ONEDAY.equals(schedule_type
      .getValue());
  }

  public boolean isCopy() {
    return is_copy;
  }

  public boolean isSpan() {
    return CellScheduleUtils.SCHEDULE_TYPE_SPAN
      .equals(schedule_type.getValue());
  }

  public boolean isRepeat() {
    return CellScheduleUtils.SCHEDULE_TYPE_REPEAT.equals(schedule_type
      .getValue());
  }

  /**
   * ログインユーザを取得します。
   *
   * @return
   */
  public ALEipUser getLoginUser() {
    return login_user;
  }

  /**
   * 編集するスケジュールの1日の情報を取得します。
   *
   * @return
   */
  public ScheduleOnedayGroupSelectData getSelectData() {
    return selectData;
  }

  /**
   * スケジュールタイプを取得します。
   *
   * @return
   */
  public ALCellStringField getScheduleType() {
    return schedule_type;
  }

  /**
   * グループリストを取得します。
   *
   * @return
   */
  public List<ALEipGroup> getGroupList() {
    return groups;
  }

  /**
   * グループリストを取得します
   *
   * @return
   */
  public List<EipMFacilityGroup> getFacilityGroupList() {
    return facilities;
  }

  public int getInt(long num) {
    return (int) num;
  }

  public String getEntityId() {
    return entity_id;
  }

  @Override
  public String getAclPortletFeature() {
    return aclPortletFeature;
  }
}
