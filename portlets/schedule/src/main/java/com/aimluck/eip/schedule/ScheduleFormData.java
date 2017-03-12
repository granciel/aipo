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

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.jetspeed.services.Registry;
import org.apache.jetspeed.services.logging.JetspeedLogFactoryService;
import org.apache.jetspeed.services.logging.JetspeedLogger;
import org.apache.turbine.services.TurbineServices;
import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;

import com.aimluck.commons.field.ALDateContainer;
import com.aimluck.commons.field.ALDateField;
import com.aimluck.commons.field.ALDateTimeField;
import com.aimluck.commons.field.ALNumberField;
import com.aimluck.commons.field.ALStringField;
import com.aimluck.commons.utils.ALDeleteFileUtil;
import com.aimluck.eip.category.util.CommonCategoryUtils;
import com.aimluck.eip.cayenne.om.portlet.EipMFacility;
import com.aimluck.eip.cayenne.om.portlet.EipTCommonCategory;
import com.aimluck.eip.cayenne.om.portlet.EipTSchedule;
import com.aimluck.eip.cayenne.om.portlet.EipTScheduleFile;
import com.aimluck.eip.cayenne.om.portlet.EipTScheduleMap;
import com.aimluck.eip.cayenne.om.security.TurbineUser;
import com.aimluck.eip.common.ALAbstractFormData;
import com.aimluck.eip.common.ALDBErrorException;
import com.aimluck.eip.common.ALEipConstants;
import com.aimluck.eip.common.ALEipGroup;
import com.aimluck.eip.common.ALEipManager;
import com.aimluck.eip.common.ALEipPost;
import com.aimluck.eip.common.ALEipUser;
import com.aimluck.eip.common.ALFileNotRemovedException;
import com.aimluck.eip.common.ALPageNotFoundException;
import com.aimluck.eip.common.ALPermissionException;
import com.aimluck.eip.facilities.FacilityResultData;
import com.aimluck.eip.facilities.util.FacilitiesUtils;
import com.aimluck.eip.fileupload.beans.FileuploadLiteBean;
import com.aimluck.eip.mail.ALAdminMailContext;
import com.aimluck.eip.mail.ALAdminMailMessage;
import com.aimluck.eip.mail.ALMailService;
import com.aimluck.eip.mail.util.ALEipUserAddr;
import com.aimluck.eip.mail.util.ALMailUtils;
import com.aimluck.eip.modules.actions.common.ALAction;
import com.aimluck.eip.orm.Database;
import com.aimluck.eip.orm.query.SelectQuery;
import com.aimluck.eip.schedule.util.ScheduleUtils;
import com.aimluck.eip.services.accessctl.ALAccessControlConstants;
import com.aimluck.eip.services.accessctl.ALAccessControlFactoryService;
import com.aimluck.eip.services.accessctl.ALAccessControlHandler;
import com.aimluck.eip.services.eventlog.ALEventlogConstants;
import com.aimluck.eip.services.eventlog.ALEventlogFactoryService;
import com.aimluck.eip.services.orgutils.ALOrgUtilsService;
import com.aimluck.eip.services.reminder.ALReminderHandler.ReminderCategory;
import com.aimluck.eip.services.reminder.ALReminderService;
import com.aimluck.eip.services.reminder.model.ALReminderDefaultItem;
import com.aimluck.eip.services.reminder.model.ALReminderItem;
import com.aimluck.eip.timeline.util.TimelineUtils;
import com.aimluck.eip.util.ALEipUtils;
import com.aimluck.eip.util.ALLocalizationUtils;

/**
 * スケジュールのフォームデータを管理するクラスです。
 *
 */
public class ScheduleFormData extends ALAbstractFormData {

  /** <code>logger</code> logger */
  private static final JetspeedLogger logger = JetspeedLogFactoryService
    .getLogger(ScheduleFormData.class.getName());

  /** <code>FLAG_EDIT_REPEAT_DEF</code> デフォルト値（繰り返し編集範囲） */
  private static final int FLAG_EDIT_REPEAT_DEF = -1;

  /** <code>FLAG_EDIT_REPEAT_ALL</code> 個別日程を編集（繰り返し編集範囲） */
  private static final int FLAG_EDIT_REPEAT_ALL = 0;

  /** <code>FLAG_EDIT_SCHEDULE_ONE</code> 全日程を編集（繰り返し編集範囲） */
  private static final int FLAG_EDIT_REPEAT_ONE = 1;

  /** <code>FLAG_DEL_MEMBER_ALL</code> [削除フラグ] すべての共有メンバーからこのスケジュールを削除する */
  public static final int FLAG_DEL_MEMBER_ALL = 0;

  /** <code>FLAG_DEL_MEMBER_ONE</code> [削除フラグ] このスケジュールからログインユーザーだけを削除する */
  public static final int FLAG_DEL_MEMBER_ONE = 1;

  /** <code>FLAG_DEL_RANGE_ALL</code> [削除フラグ] この繰り返しスケジュールを削除する */
  public static final int FLAG_DEL_RANGE_ALL = 0;

  /** <code>FLAG_DEL_RANGE_ONE</code> [削除フラグ] この繰り返しスケジュールの特定スケジュールのみを削除する */
  public static final int FLAG_DEL_RANGE_ONE = 1;

  /** <code>start_date</code> 開始日時 */
  private ALDateTimeField start_date;

  /** <code>end_date</code> 終了日時 */
  private ALDateTimeField end_date;

  /** <code>name</code> タイトル */
  private ALStringField name;

  /** <code>place</code> 場所 */
  private ALStringField place;

  /** <code>note</code> 内容 */
  private ALStringField note;

  /** <code>public_flag</code> 公開/非公開フラグ */
  private ALStringField public_flag;

  /** <code>repeat_type</code> 繰り返し種別 */
  private ALStringField repeat_type;

  /** <code>week_0</code> 繰り返し曜日 */
  private ALStringField week_0;

  /** <code>week_1</code> 繰り返し曜日 */
  private ALStringField week_1;

  /** <code>week_2</code> 繰り返し曜日 */
  private ALStringField week_2;

  /** <code>week_3</code> 繰り返し曜日 */
  private ALStringField week_3;

  /** <code>week_4</code> 繰り返し曜日 */
  private ALStringField week_4;

  /** <code>week_5</code> 繰り返し曜日 */
  private ALStringField week_5;

  /** <code>week_6</code> 繰り返し曜日 */
  private ALStringField week_6;

  /** <code>repeat_week</code> 繰り返し週 */
  private ALStringField repeat_week;

  /** <code>limit_flag</code> 期限ありなし */
  private ALStringField limit_flag;

  /** <code>limit_date</code> 繰り返し期限（開始日） */
  private ALDateField limit_start_date;

  /** <code>limit_date</code> 繰り返し期限（終了日） */
  private ALDateField limit_end_date;

  /** <code>month_day</code> 繰り返す日 */
  private ALNumberField month_day;

  /** <code>year_month</code> 毎年繰り返す月 */
  private ALNumberField year_month;

  /** <code>year_day</code> 毎年繰り返す日 */
  private ALNumberField year_day;

  /** <code>memberList</code> メンバーリスト */
  private ArrayList<ALEipUser> memberList;

  /** <code>currentYear</code> 現在の年 */
  private int currentYear;

  /** <code>is_member</code> スケジュールを共有するかどうか */
  private boolean is_member;

  /** <code>is_repeat</code> 繰り返すかどうか */
  private boolean is_repeat;

  /** <code>is_span</code> 期間指定かどうか */
  private boolean is_span;

  /** <code>all_day_flag</code> 終日予定フラグ */
  private ALStringField all_day_flag;

  /** <code>login_user</code> ログインユーザー */
  private ALEipUser login_user;

  /** <code>tmpStart</code> 開始日時の初期値 */
  private String tmpStart;

  /** <code>tmpEnd</code> 終了日時の初期値 */
  private String tmpEnd;

  /** <code>selectData</code> 編集するスケジュールの1日の情報 */
  private ScheduleOnedayGroupSelectData selectData;

  /** <code>groups</code> グループ */
  private List<ALEipGroup> groups;

  /** <code>edit_schedule_flag</code> 繰り返しスケジュールの編集フラグ */
  private ALNumberField edit_repeat_flag;

  /** <code>tmpView</code> 表示する日 */
  private String tmpView;

  /** <code>end_date</code> 終了日時 */
  private ALDateTimeField view_date;

  /** <code>edit_control_flag</code> 共有メンバーによる編集／削除権限フラグ */
  private ALStringField edit_flag;

  /** <code>enable_change_tmpreserve</code> 仮スケジュールに戻すかどうか */
  private ALStringField change_tmpreserve_flag;

  /** <code>del_member_flag</code> [削除フラグ] 共有メンバーを削除するフラグ */
  private ALNumberField del_member_flag;

  /** <code>del_range_flag</code> [削除フラグ] 削除範囲のフラグ */
  private ALNumberField del_range_flag;

  /** <code>isOwner</code> 所有者かどうか */
  private boolean is_owner;

  /** <code>is_facility</code> 設備を予約するかどうか */
  private boolean is_facility;

  /** 設備リスト */
  private List<Object> facilityList;

  /** 全設備リスト */
  private List<FacilityResultData> facilityAllList;

  /** <code>todo_id</code> ToDo ID */
  private ALNumberField common_category_id;

  /** 添付ファイルリスト */
  private List<FileuploadLiteBean> fileuploadList = null;

  /** 添付フォルダ名 */
  private String folderName = null;

  /** スケジュール更新時にメール受信フラグ */
  private String mail_flag = ScheduleUtils.MAIL_FOR_ALL;

  private final int msg_type = 0;

  private String orgId;

  private ALEipUser loginUser;

  private boolean ignore_duplicate_facility;

  private boolean is_copy;

  /** <code>is_same_date</code> 開始日時と終了日時が同じかどうか */
  private boolean is_same_date;

  /** アクセス権限の機能名 */
  private String aclPortletFeature = null;

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
    is_facility = rundata.getParameters().getBoolean("is_facility");
    is_member = rundata.getParameters().getBoolean("is_member");
    is_repeat = rundata.getParameters().getBoolean("is_repeat");
    is_span = rundata.getParameters().getBoolean("is_span");
    is_copy = rundata.getParameters().getBoolean("is_copy");

    String tmp_mail_flag = rundata.getParameters().getString("mail_flag");

    if (ScheduleUtils.MAIL_FOR_ALL.equals(tmp_mail_flag)) {
      mail_flag = ScheduleUtils.MAIL_FOR_ALL;
    } else if (ScheduleUtils.MAIL_FOR_NONE.equals(tmp_mail_flag)) {
      mail_flag = ScheduleUtils.MAIL_FOR_NONE;
    } else {
      // default
      mail_flag = ScheduleUtils.MAIL_FOR_ALL;
    }

    ignore_duplicate_facility =
      rundata.getParameters().getBoolean("ignore_duplicate_facility", false);
    login_user = ALEipUtils.getALEipUser(rundata);

    groups = ALEipUtils.getMyGroups(rundata);

    is_owner = true;

    orgId = Database.getDomainName();
    loginUser = ALEipUtils.getALEipUser(rundata);

    facilityAllList = new ArrayList<FacilityResultData>();
    facilityAllList.addAll(FacilitiesUtils.getFacilityAllList());

    folderName = rundata.getParameters().getString("folderName");

    // 終日設定
    if (tmpEnd != null
      && !tmpEnd.equals("")
      && tmpStart != null
      && !tmpStart.equals("")
      && is_span
      && tmpStart.equals(tmpEnd)) {
      // 新しい予定追加で終日の場合
      all_day_flag.setValue("ON");
    }

    String scheduleId =
      rundata.getParameters().getString(ALEipConstants.ENTITY_ID);
    if (scheduleId == null || scheduleId.equals("new")) {
      String members[] = rundata.getParameters().getStrings("member_to");
      if (members != null && members.length > 0) {
        aclPortletFeature =
          ALAccessControlConstants.POERTLET_FEATURE_SCHEDULE_OTHER;

        String user_name = login_user.getName().toString();
        for (String member : members) {
          if (user_name.equals(member)) {
            aclPortletFeature =
              ALAccessControlConstants.POERTLET_FEATURE_SCHEDULE_SELF;
            break;
          }
        }
      } else {
        aclPortletFeature =
          ALAccessControlConstants.POERTLET_FEATURE_SCHEDULE_SELF;
      }
    } else if (ScheduleUtils.hasRelation(rundata)) {
      aclPortletFeature =
        ALAccessControlConstants.POERTLET_FEATURE_SCHEDULE_SELF;
    } else {
      aclPortletFeature =
        ALAccessControlConstants.POERTLET_FEATURE_SCHEDULE_OTHER;
    }
    /*
     */
    // スーパークラスのメソッドを呼び出す。
    super.init(action, rundata, context);
  }

  public void loadParametersViewDate(RunData rundata, Context context) {
    if (ALEipUtils.isMatch(rundata, context)) {
      if (rundata.getParameters().containsKey("view_date")) {
        ALDateTimeField dummy = new ALDateTimeField("yyyy-MM-dd");
        tmpView = rundata.getParameters().getString("view_date");
        ALEipUtils.setTemp(rundata, context, "tmpView", tmpView);
        dummy.setValue(tmpView);
        if (!dummy.validate(new ArrayList<String>())) {
          ALEipUtils.removeTemp(rundata, context, "tmpView");
          logger.debug("[ScheduleFormData] Parameter cannot validate");
          ALEipUtils.redirectPageNotFound(rundata);
          return;
        }
      } else {
        if (tmpView == null || tmpView.equals("")) {
          if (tmpStart != null && !tmpStart.equals("")) {
            tmpView = tmpStart;
            ALEipUtils.setTemp(rundata, context, "tmpView", tmpView);
          }
        }
      }
    }
  }

  /**
   * パラメータを読み込みます。
   *
   * @param rundata
   * @param context
   */
  public void loadParameters(RunData rundata, Context context) {
    ALDateTimeField dummy = new ALDateTimeField("yyyy-MM-dd-HH-mm");
    dummy.setNotNull(true);
    if (ALEipUtils.isMatch(rundata, context)) {
      if (rundata.getParameters().containsKey("form_start")) {
        tmpStart = rundata.getParameters().getString("form_start");
        ALEipUtils.setTemp(rundata, context, "tmpStart", tmpStart);
        dummy.setValue(tmpStart);
        if (!dummy.validate(new ArrayList<String>())) {
          ALEipUtils.removeTemp(rundata, context, "form_start");
          ALEipUtils.removeTemp(rundata, context, "form_end");
          logger.debug("[ScheduleFormData] Parameter cannot validate");
          ALEipUtils.redirectPageNotFound(rundata);
          return;
        }
      } else {
        Date date = new Date();
        Calendar tmpStartCal = Calendar.getInstance();
        tmpStartCal.setTime(date);
        int startHour = tmpStartCal.get(Calendar.HOUR_OF_DAY);
        if (startHour != 23) {
          tmpStartCal.set(Calendar.HOUR_OF_DAY, startHour + 1);
          tmpStartCal.set(Calendar.MINUTE, 0);
        }
        dummy.setValue(tmpStartCal.getTime());
        ALEipUtils.setTemp(rundata, context, "tmpStart", dummy.toString());
      }
      if (rundata.getParameters().containsKey("form_end")) {
        tmpEnd = rundata.getParameters().getString("form_end");
        ALEipUtils.setTemp(rundata, context, "tmpEnd", tmpEnd);
        dummy.setValue(tmpEnd);
        if (!dummy.validate(new ArrayList<String>())) {
          ALEipUtils.removeTemp(rundata, context, "form_start");
          ALEipUtils.removeTemp(rundata, context, "form_end");
          logger.debug("[ScheduleFormData] Parameter cannot validate");
          ALEipUtils.redirectPageNotFound(rundata);
          return;
        }
      } else {
        Date date = new Date();
        Calendar tmpEndCal = Calendar.getInstance();
        tmpEndCal.setTime(date);
        int endHour = tmpEndCal.get(Calendar.HOUR_OF_DAY);
        if (endHour != 22 && endHour != 23) {
          tmpEndCal.set(Calendar.HOUR_OF_DAY, endHour + 2);
          tmpEndCal.set(Calendar.MINUTE, 0);
        } else if (endHour == 22) {
          tmpEndCal.set(Calendar.HOUR_OF_DAY, endHour + 1);
          tmpEndCal.set(Calendar.MINUTE, 55);
        }
        dummy.setValue(tmpEndCal.getTime());
        ALEipUtils.setTemp(rundata, context, "tmpEnd", dummy.toString());
      }
    }
    tmpStart = ALEipUtils.getTemp(rundata, context, "tmpStart");
    tmpEnd = ALEipUtils.getTemp(rundata, context, "tmpEnd");

    loadParametersViewDate(rundata, context);
  }

  /*
   *
   */
  @Override
  public void initField() {
    Date now = new Date();
    Calendar cal = Calendar.getInstance();
    int min = cal.get(Calendar.MINUTE);

    for (int _min = 5; _min <= 60; _min += 5) {
      if (_min - 5 <= min && min <= _min) {
        cal.set(Calendar.MINUTE, _min);
        break;
      }
    }
    // 開始日時
    start_date = new ALDateTimeField("yyyy-MM-dd-HH-mm");
    if (tmpStart == null || tmpStart.equals("")) {
      start_date.setValue(now);
    } else {
      start_date.setValue(tmpStart);
    }
    start_date.setFieldName(ALLocalizationUtils
      .getl10n("SCHEDULE_SETFIELDNAME_START_DATE"));
    // 終了日時
    end_date = new ALDateTimeField("yyyy-MM-dd-HH-mm");
    if (tmpEnd == null || tmpEnd.equals("")) {
      if (tmpStart == null || tmpStart.equals("")) {
        end_date.setValue(now);
      } else {
        end_date.setValue(tmpStart);
      }
    } else {
      end_date.setValue(tmpEnd);
    }
    end_date.setFieldName(ALLocalizationUtils
      .getl10n("SCHEDULE_SETFIELDNAME_END_DATE"));
    // 開始日時と終了日時が同じか
    if (start_date.toString().equals(end_date.toString())) {
      is_same_date = true;
      Calendar tmpEndCal = Calendar.getInstance();
      tmpEndCal.setTime(end_date.getValue());
      int endHour = tmpEndCal.get(Calendar.HOUR_OF_DAY);
      if (endHour != 23) {
        tmpEndCal.set(Calendar.HOUR_OF_DAY, endHour);
      }
      end_date.setValue(tmpEndCal.getTime());
    } else {
      is_same_date = false;
    }

    // 分の端数切上げ
    Calendar tmpStartCal = Calendar.getInstance();
    tmpStartCal.setTime(start_date.getValue());
    int startMinute = tmpStartCal.get(Calendar.MINUTE);
    if (startMinute > 55) {
      int startHour = tmpStartCal.get(Calendar.HOUR_OF_DAY);
      if (startHour != 23) {
        tmpStartCal.set(Calendar.HOUR_OF_DAY, startHour + 1);
        tmpStartCal.set(Calendar.MINUTE, 0);
      } else {
        tmpStartCal.set(Calendar.MINUTE, 55);
      }
    } else if (startMinute % 5 != 0) {
      tmpStartCal.set(Calendar.MINUTE, startMinute + (5 - (startMinute % 5)));
    }
    start_date.setValue(tmpStartCal.getTime());

    Calendar tmpEndCal = Calendar.getInstance();
    tmpEndCal.setTime(end_date.getValue());
    int endMinute = tmpEndCal.get(Calendar.MINUTE);
    if (endMinute > 55) {
      int endHour = tmpEndCal.get(Calendar.HOUR_OF_DAY);
      if (endHour != 23) {
        tmpEndCal.set(Calendar.HOUR_OF_DAY, endHour + 1);
        tmpEndCal.set(Calendar.MINUTE, 0);
      } else {
        tmpEndCal.set(Calendar.MINUTE, 55);
      }
    } else if (endMinute % 5 != 0) {
      tmpEndCal.set(Calendar.MINUTE, endMinute + (5 - (endMinute % 5)));
    }
    end_date.setValue(tmpEndCal.getTime());

    // 繰り返しタイプ
    repeat_type = new ALStringField();
    repeat_type.setFieldName(ALLocalizationUtils
      .getl10n("SCHEDULE_SETFIELDNAME_REPEAT_TYPE"));
    repeat_type.setValue("D");
    repeat_type.setTrim(true);
    // 日
    week_0 = new ALStringField();
    week_0.setFieldName(ALLocalizationUtils
      .getl10n("SCHEDULE_SETFIELDNAME_SUNDAY"));
    week_0.setTrim(true);
    // 月
    week_1 = new ALStringField();
    week_1.setFieldName(ALLocalizationUtils
      .getl10n("SCHEDULE_SETFIELDNAME_MANDAY"));
    week_1.setTrim(true);
    // 火
    week_2 = new ALStringField();
    week_2.setFieldName(ALLocalizationUtils
      .getl10n("SCHEDULE_SETFIELDNAME_TUSEDAY"));
    week_2.setTrim(true);
    // 水
    week_3 = new ALStringField();
    week_3.setFieldName(ALLocalizationUtils
      .getl10n("SCHEDULE_SETFIELDNAME_WEDNESDAY"));
    week_3.setTrim(true);
    // 木
    week_4 = new ALStringField();
    week_4.setFieldName(ALLocalizationUtils
      .getl10n("SCHEDULE_SETFIELDNAME_THURSDAY"));
    week_4.setTrim(true);
    // 金
    week_5 = new ALStringField();
    week_5.setFieldName(ALLocalizationUtils
      .getl10n("SCHEDULE_SETFIELDNAME_FRIDAY"));
    week_5.setTrim(true);
    // 土
    week_6 = new ALStringField();
    week_6.setFieldName(ALLocalizationUtils
      .getl10n("SCHEDULE_SETFIELDNAME_SATURDAY"));
    week_6.setTrim(true);
    // 繰り返し週
    repeat_week = new ALStringField();
    repeat_week.setFieldName(ALLocalizationUtils
      .getl10n("SCHEDULE_SETFIELDNAME_REPEAT_WEEK"));
    repeat_week.setTrim(true);
    // 繰り返し日
    month_day = new ALNumberField();
    month_day.setFieldName(ALLocalizationUtils
      .getl10n("SCHEDULE_SETFIELDNAME_REPEAT_MONTH"));
    // 繰り返し年日
    year_day = new ALNumberField();
    year_day.setFieldName(ALLocalizationUtils
      .getl10n("SCHEDULE_SETFIELDNAME_REPEAT_YEAR_DAY"));
    // 繰り返し年月
    year_month = new ALNumberField();
    year_month.setFieldName(ALLocalizationUtils
      .getl10n("SCHEDULE_SETFIELDNAME_REPEAT_YEAR_MONTH"));
    // 繰り返しフラグ
    limit_flag = new ALStringField();
    limit_flag.setFieldName(ALLocalizationUtils
      .getl10n("SCHEDULE_SETFIELDNAME_REPEAT"));
    limit_flag.setValue("OFF");
    limit_flag.setTrim(true);
    // 繰り返し期限
    limit_start_date = new ALDateField();
    limit_start_date.setFieldName(ALLocalizationUtils
      .getl10n("SCHEDULE_SETFIELDNAME_REPEAT_LIMIT"));
    limit_start_date.setValue(start_date.getValue());
    limit_end_date = new ALDateField();
    limit_end_date.setFieldName(ALLocalizationUtils
      .getl10n("SCHEDULE_SETFIELDNAME_REPEAT_LIMIT"));
    ALDateTimeField tmp_date = new ALDateTimeField("yyyy-MM-dd-HH-mm");
    if (tmpEnd == null || tmpEnd.equals("")) {
      if (tmpStart == null || tmpStart.equals("")) {
        tmp_date.setValue(now);
        limit_end_date.setValue(tmp_date.getValue());
      } else {
        tmp_date.setValue(tmpStart);
        limit_end_date.setValue(tmp_date.getValue());
      }
    } else {
      tmp_date.setValue(tmpEnd);
      limit_end_date.setValue(tmp_date.getValue());
    }
    // 終日フラグ
    all_day_flag = new ALStringField();
    all_day_flag.setFieldName(ALLocalizationUtils
      .getl10n("SCHEDULE_SETFIELDNAME_ALL_DAY"));
    all_day_flag.setTrim(true);
    all_day_flag.setValue("OFF");
    // タイトル
    name = new ALStringField();
    name.setFieldName(ALLocalizationUtils
      .getl10n("SCHEDULE_SETFIELDNAME_TITLE"));
    name.setTrim(true);
    // 場所
    place = new ALStringField();
    place.setFieldName(ALLocalizationUtils
      .getl10n("SCHEDULE_SETFIELDNAME_PLACE"));
    place.setTrim(true);
    // 内容
    note = new ALStringField();
    note
      .setFieldName(ALLocalizationUtils.getl10n("SCHEDULE_SETFIELDNAME_NOTE"));
    note.setTrim(false);
    // 公開区分
    public_flag = new ALStringField();
    public_flag.setFieldName(ALLocalizationUtils
      .getl10n("SCHEDULE_SETFIELDNAME_PUBLIC"));
    public_flag.setTrim(true);
    public_flag.setValue("O");
    // 繰り返しスケジュールの編集フラグ
    edit_repeat_flag = new ALNumberField();
    edit_repeat_flag.setFieldName(ALLocalizationUtils
      .getl10n("SCHEDULE_SETFIELDNAME_REPEAT_EDIT_AREA"));
    edit_repeat_flag.setValue(FLAG_EDIT_REPEAT_DEF);

    // 指定日時
    view_date = new ALDateTimeField("yyyy-MM-dd");
    if (tmpView == null || tmpView.equals("")) {
      view_date.setValue(now);
    } else {
      view_date.setValue(tmpView);
    }
    view_date.setFieldName(ALLocalizationUtils
      .getl10n("SCHEDULE_SETFIELDNAME_SELECT_DATE"));

    // 共有メンバーによる編集／削除権限フラグ
    edit_flag = new ALStringField();
    edit_flag.setFieldName(ALLocalizationUtils
      .getl10n("SCHEDULE_SETFIELDNAME_EDIT_DELETE_PERMISSION"));
    edit_flag.setTrim(true);
    edit_flag.setValue("T");

    // 仮スケジュールに戻すかどうか
    change_tmpreserve_flag = new ALStringField();
    change_tmpreserve_flag.setFieldName(ALLocalizationUtils
      .getl10n("SCHEDULE_SETFIELDNAME_TEMP_SCHEDULE"));
    change_tmpreserve_flag.setTrim(true);
    change_tmpreserve_flag.setValue("F");

    // メンバーリスト
    memberList = new ArrayList<ALEipUser>();
    // 現在の年（年を選択するリストボックスに利用）
    currentYear = Calendar.getInstance().get(Calendar.YEAR);

    // [削除フラグ] 共有メンバーを削除するフラグ
    del_member_flag = new ALNumberField();
    del_member_flag.setValue(FLAG_DEL_MEMBER_ALL);
    // [削除フラグ] 削除範囲のフラグ
    del_range_flag = new ALNumberField();
    del_range_flag.setValue(FLAG_DEL_RANGE_ALL);

    // 設備リスト
    facilityList = new ArrayList<Object>();

    // 添付ファイルリスト
    fileuploadList = new ArrayList<FileuploadLiteBean>();

    // 2007.3.28 ToDo連携
    common_category_id = new ALNumberField();
    common_category_id.setFieldName(ALLocalizationUtils
      .getl10n("SCHEDULE_SETFIELDNAME_CATEGORY"));
    common_category_id.setValue(1);
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
    boolean res = super.setFormData(rundata, context, msgList);
    if (res) {
      try {
        // 終了日時がnullの場合、開始日時と同じにする
        if (!is_span) {
          if (rundata.getParameters().containsKey("end_date_hour")
            && rundata.getParameters().containsKey("end_date_minute")) {
            if ("".equals(rundata.getParameters().get("end_date_hour"))
              && "".equals(rundata.getParameters().get("end_date_minute"))) {
              end_date = start_date;
            } else if ("".equals(rundata.getParameters().get("end_date_hour"))
              || "".equals(rundata.getParameters().get("end_date_minute"))) {
              // 片方だけが"--"の時は開始日時と同じにせずにバリデートエラーにする
              end_date = null;
            }
          }
        }
        // 終日
        if (all_day_flag.getValue().equals("ON") && is_span) {
          end_date.setValue(start_date.getValue());
        }

        String memberNames[] = rundata.getParameters().getStrings("member_to");
        if (memberNames != null && memberNames.length > 0) {
          SelectQuery<TurbineUser> query = Database.query(TurbineUser.class);
          Expression exp =
            ExpressionFactory.inExp(
              TurbineUser.LOGIN_NAME_PROPERTY,
              memberNames);
          query.setQualifier(exp);
          memberList.addAll(ALEipUtils.getUsersFromSelectQuery(query));
        }
        if (memberList.size() == 0) {
          memberList.add(login_user);
        }

        String facilityIds[] =
          rundata.getParameters().getStrings("facility_to");
        if (!ScheduleUtils.isZeroLength(facilityIds)) {
          SelectQuery<EipMFacility> fquery = Database.query(EipMFacility.class);
          Expression fexp =
            ExpressionFactory.inDbExp(
              EipMFacility.FACILITY_ID_PK_COLUMN,
              facilityIds);
          fquery.setQualifier(fexp);
          List<EipMFacility> facilities = fquery.fetchList();
          for (EipMFacility facility : facilities) {
            FacilityResultData rd = new FacilityResultData();
            rd.initField();
            rd.setFacilityId(facility.getFacilityId().longValue());
            rd.setFacilityName(facility.getFacilityName());
            facilityList.add(rd);
          }
        }
      } catch (Exception ex) {
        logger.error("schedule", ex);
      }
    }
    try {
      fileuploadList = ScheduleUtils.getFileuploadList(rundata);
    } catch (Exception ex) {
      logger.error("schedule", ex);
    }
    return res;
  }

  /*
   *
   */
  @Override
  protected void setValidator() {
    // タイトル
    name.setNotNull(true);
    name.limitMaxLength(50);
    // 場所
    place.limitMaxLength(50);
    // 内容
    note.limitMaxLength(1000);
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

    try {
      ScheduleUtils.validateDelegate(
        getStartDate(),
        getEndDate(),
        getRepeatType(),
        is_repeat,
        is_span,
        getWeek0(),
        getWeek1(),
        getWeek2(),
        getWeek3(),
        getWeek4(),
        getWeek5(),
        getWeek6(),
        getRepeatWeek(),
        getLimitFlag(),
        getLimitStartDate(),
        getLimitEndDate(),
        getMonthDay(),
        getYearMonth(),
        getYearDay(),
        loginUser,
        null,
        msgList,
        false);
    } catch (NumberFormatException nfe) {
      logger
        .error("[ScheduleFormData] NumberFormatException: Limit Date is wrong.");
      throw new ALPageNotFoundException();
    }

    // タイトル
    name.validate(msgList);
    // 場所
    place.validate(msgList);
    // 内容
    note.validate(msgList);

    common_category_id.validate(msgList);

    return (msgList.size() == 0);
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
  @SuppressWarnings("unchecked")
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
        (record.getOwnerId().intValue() == login_user.getUserId().getValue())
          ? true
          : false;

      // タイトル
      name.setValue(record.getName());
      // 場所
      place.setValue(record.getPlace());
      // 内容
      note.setValue(record.getNote());
      // ファイル
      SelectQuery<EipTScheduleFile> querySelectFile =
        Database.query(EipTScheduleFile.class);
      querySelectFile.andQualifier(ExpressionFactory.matchDbExp(
        EipTScheduleFile.EIP_TSCHEDULE_PROPERTY,
        record.getScheduleId()));
      List<EipTScheduleFile> scheduleFileList = querySelectFile.fetchList();
      for (EipTScheduleFile file : scheduleFileList) {
        // スケジュールに添付ファイルがあった場合
        // fileidを新しくセット
        int fileid = file.getFileId();
        FileuploadLiteBean fbean = new FileuploadLiteBean();
        fbean.initField();
        fbean.setFileId(fileid);
        fbean.setFileName(file.getFileName());
        if (!is_copy) {
          fileuploadList.add(fbean);
        }
      }
      // 公開フラグ
      public_flag.setValue(record.getPublicFlag());
      // メールフラグ
      mail_flag = record.getMailFlag();
      // 共有メンバーによる編集／削除フラグ
      if ("T".equals(record.getEditFlag())) {
        if (is_owner) {
          edit_flag.setValue(record.getEditFlag());
        } else {
          // スケジュールの登録ユーザがすでにメンバーから抜けているかを検証する．
          int createUserId = record.getOwnerId().intValue();
          boolean inculudeCreateUser = false;
          List<EipTScheduleMap> scheduleMaps = record.getEipTScheduleMaps();

          for (EipTScheduleMap map : scheduleMaps) {
            if (createUserId == map.getUserId().intValue()
              && !"R".equals(map.getStatus())) {
              inculudeCreateUser = true;
              break;
            }
          }
          if (inculudeCreateUser) {
            edit_flag.setValue("F");
          } else {
            edit_flag.setValue("T");
          }
        }
      } else {
        edit_flag.setValue("F");
      }
      // DN -> 毎日 (A = N -> 期限なし A = L -> 期限あり)
      // WnnnnnnnN W01111110 -> 毎週(月～金用)
      // WnnnnnnnmN -> 第m週
      // MnnN M25 -> 毎月25日
      // MXXL -> 毎月月末
      // YnnnnN Y0101N -> 毎年01月01日
      // S -> 期間での指定
      String ptn = record.getRepeatPattern();
      int count = 0;
      is_repeat = true;
      is_span = false;
      // 毎日
      if (ptn.charAt(0) == 'D') {
        repeat_type.setValue("D");
        count = 1;
        // 毎週
      } else if (ptn.charAt(0) == 'W' && ptn.length() == 9) {
        repeat_type.setValue("W");
        week_0.setValue(ptn.charAt(1) != '0' ? "TRUE" : null);
        week_1.setValue(ptn.charAt(2) != '0' ? "TRUE" : null);
        week_2.setValue(ptn.charAt(3) != '0' ? "TRUE" : null);
        week_3.setValue(ptn.charAt(4) != '0' ? "TRUE" : null);
        week_4.setValue(ptn.charAt(5) != '0' ? "TRUE" : null);
        week_5.setValue(ptn.charAt(6) != '0' ? "TRUE" : null);
        week_6.setValue(ptn.charAt(7) != '0' ? "TRUE" : null);
        count = 8;
        // 第何週
      } else if (ptn.charAt(0) == 'W' && ptn.length() == 10) {
        repeat_type.setValue("W");
        week_0.setValue(ptn.charAt(1) != '0' ? "TRUE" : null);
        week_1.setValue(ptn.charAt(2) != '0' ? "TRUE" : null);
        week_2.setValue(ptn.charAt(3) != '0' ? "TRUE" : null);
        week_3.setValue(ptn.charAt(4) != '0' ? "TRUE" : null);
        week_4.setValue(ptn.charAt(5) != '0' ? "TRUE" : null);
        week_5.setValue(ptn.charAt(6) != '0' ? "TRUE" : null);
        week_6.setValue(ptn.charAt(7) != '0' ? "TRUE" : null);
        repeat_week.setValue("0");
        switch (ptn.charAt(8)) {
          case '1':
            repeat_week.setValue("1");
            break;
          case '2':
            repeat_week.setValue("2");
            break;
          case '3':
            repeat_week.setValue("3");
            break;
          case '4':
            repeat_week.setValue("4");
            break;
          case '5':
            repeat_week.setValue("5");
            break;
          default:
            break;
        }
        count = 9;
        // 毎月
      } else if (ptn.charAt(0) == 'M') {
        repeat_type.setValue("M");
        if (ptn.substring(1, 3).equals("XX")) {
          month_day.setValue(32);
        } else {
          month_day.setValue(Integer.parseInt(ptn.substring(1, 3)));
        }
        count = 3;

        // 毎年
      } else if (ptn.charAt(0) == 'Y') {
        repeat_type.setValue("Y");
        year_month.setValue(Integer.parseInt(ptn.substring(1, 3)));
        year_day.setValue(Integer.parseInt(ptn.substring(3, 5)));
        count = 5;
        // 期間
      } else if (ptn.charAt(0) == 'S') {
        is_span = true;
        is_repeat = false;
      } else {
        is_repeat = false;
      }
      if (is_repeat) {
        // 開始日時
        Calendar tmpViewCal = Calendar.getInstance();
        tmpViewCal.setTime(view_date.getValue());
        Calendar tmpStartCal = Calendar.getInstance();
        tmpStartCal.setTime(record.getStartDate());
        tmpViewCal.set(Calendar.HOUR_OF_DAY, tmpStartCal
          .get(Calendar.HOUR_OF_DAY));
        tmpViewCal.set(Calendar.MINUTE, tmpStartCal.get(Calendar.MINUTE));
        start_date.setValue(tmpViewCal.getTime());
        // 終了日時
        Calendar tmpStopCal = Calendar.getInstance();
        tmpStopCal.setTime(record.getEndDate());
        tmpViewCal.set(Calendar.HOUR_OF_DAY, tmpStopCal
          .get(Calendar.HOUR_OF_DAY));
        tmpViewCal.set(Calendar.MINUTE, tmpStopCal.get(Calendar.MINUTE));
        end_date.setValue(tmpViewCal.getTime());

        if (ptn.charAt(count) == 'N') {
          limit_start_date.setValue(view_date.getValue());
          limit_end_date.setValue(view_date.getValue());
          limit_flag.setValue("OFF");
        } else {
          limit_flag.setValue("ON");
          limit_start_date.setValue(record.getStartDate());
          limit_end_date.setValue(record.getEndDate());
        }

        // 繰り返しスケジュールの編集フラグ
        edit_repeat_flag.setValue(FLAG_EDIT_REPEAT_ALL);
      }

      if (is_span) {
        // 開始日時
        start_date.setValue(record.getStartDate());
        // 終了日時
        end_date.setValue(record.getEndDate());

        limit_start_date.setValue(record.getStartDate());
        limit_end_date.setValue(record.getEndDate());

        Calendar startDate = Calendar.getInstance();
        startDate.setTime(record.getStartDate());
        Calendar endDate = Calendar.getInstance();
        endDate.setTime(record.getEndDate());
        startDate.add(Calendar.DATE, 1);
        startDate.add(Calendar.SECOND, -1);
        Date sDate = startDate.getTime();

        if ((record.getStartDate().equals(record.getEndDate()))
          || (sDate.equals(record.getEndDate()))) {
          // 終日予定
          all_day_flag.setValue("ON");
        }
      }

      if (!is_repeat && !is_span) {
        // 開始日時
        start_date.setValue(record.getStartDate());
        // 終了日時
        end_date.setValue(record.getEndDate());

        limit_start_date.setValue(record.getStartDate());
        limit_end_date.setValue(record.getEndDate());
      }

      if (start_date.toString().equals(end_date.toString())) {
        is_same_date = true;
      } else {
        is_same_date = false;
      }

      // このスケジュールを共有しているメンバーを取得
      SelectQuery<EipTScheduleMap> mapquery =
        Database.query(EipTScheduleMap.class);
      Expression mapexp =
        ExpressionFactory.matchExp(EipTScheduleMap.SCHEDULE_ID_PROPERTY, record
          .getScheduleId());
      mapquery.setQualifier(mapexp);
      List<EipTScheduleMap> scheduleMaps = mapquery.fetchList();

      List<Integer> userIds = new ArrayList<Integer>();
      List<Integer> facilityIds = new ArrayList<Integer>();
      for (EipTScheduleMap map : scheduleMaps) {
        if (ScheduleUtils.SCHEDULEMAP_TYPE_USER.equals(map.getType())) {
          userIds.add(map.getUserId());
          EipTCommonCategory category = map.getEipTCommonCategory();
          if (category == null) {
            common_category_id.setValue(1);
          } else {
            common_category_id.setValue(category
              .getCommonCategoryId()
              .longValue());
          }
        } else {
          facilityIds.add(map.getUserId());
        }
      }

      if (userIds.size() > 0) {
        SelectQuery<TurbineUser> query = Database.query(TurbineUser.class);
        Expression exp =
          ExpressionFactory.inDbExp(TurbineUser.USER_ID_PK_COLUMN, userIds);
        query.setQualifier(exp);
        memberList.addAll(ALEipUtils.getUsersFromSelectQuery(query));
      } else {
        memberList.add(login_user);
      }

      if (facilityIds.size() > 0) {
        SelectQuery<EipMFacility> fquery = Database.query(EipMFacility.class);
        Expression fexp =
          ExpressionFactory.inDbExp(
            EipMFacility.FACILITY_ID_PK_COLUMN,
            facilityIds);
        fquery.setQualifier(fexp);
        facilityList.addAll(FacilitiesUtils
          .getFacilitiesFromSelectQuery(fquery));
      }
    } catch (Exception e) {
      logger.error("[ScheduleFormData]", e);
      throw new ALDBErrorException();
    }
    return true;
  }

  /**
   *
   * @param rundata
   * @param context
   * @param msgList
   * @return true 成功 false 失敗
   * @throws ALDBErrorException
   */
  @Override
  protected boolean insertFormData(RunData rundata, Context context,
      List<String> msgList) throws ALDBErrorException {
    EipTSchedule schedule = null;
    try {
      // 設備のアクセスコントロールのチェック
      int f_size = facilityList.size();
      if (!facilityCheckAclPermission(
        rundata,
        ALAccessControlConstants.VALUE_ACL_INSERT)
        && f_size > 0) {
        msgList.add(ALLocalizationUtils
          .getl10n("SCHEDULE_NO_PERMISSION_TO_MAKE_A_RESERVATION"));
        return false;
      }

      // Validate のときに SELECT していることに注意する

      if (is_span) {
        Calendar startDate = Calendar.getInstance();
        startDate.setTime(start_date.getValue());
        startDate.set(Calendar.HOUR_OF_DAY, 0);
        startDate.set(Calendar.MINUTE, 0);

        Calendar endDate = Calendar.getInstance();
        endDate.setTime(end_date.getValue());
        endDate.set(Calendar.HOUR_OF_DAY, 0);
        endDate.set(Calendar.MINUTE, 0);

        start_date.setValue(startDate.getTime());
        end_date.setValue(endDate.getTime());
      } else {
        Calendar startcal = new GregorianCalendar();
        startcal.setTime(start_date.getValue());
        Calendar endcal = Calendar.getInstance();
        endcal.setTime(end_date.getValue());
        endcal.set(Calendar.YEAR, startcal.get(Calendar.YEAR));
        endcal.set(Calendar.MONTH, startcal.get(Calendar.MONTH));
        endcal.set(Calendar.DATE, startcal.get(Calendar.DATE));
        end_date.setValue(endcal.getTime());
      }

      int ownerid = ALEipUtils.getUserId(rundata);
      // 新規オブジェクトモデル
      schedule = Database.create(EipTSchedule.class);
      // 親スケジュール ID
      schedule.setParentId(Integer.valueOf(0));
      // タイトル
      schedule.setName(name.getValue());
      // 場所
      schedule.setPlace(place.getValue());
      // 内容
      schedule.setNote(note.getValue());
      // 公開フラグ
      schedule.setPublicFlag(public_flag.getValue());
      // 共有メンバーによる編集／削除フラグ
      if (memberList.size() >= 1) {
        schedule.setEditFlag(edit_flag.getValue());
      } else {
        schedule.setEditFlag("F");
      }
      // send mail flag
      schedule.setMailFlag(mail_flag);

      // オーナーID
      schedule.setOwnerId(Integer.valueOf(ownerid));
      // 作成日
      Date now = new Date();
      schedule.setCreateDate(now);
      schedule.setCreateUserId(Integer.valueOf(ownerid));
      // 更新日
      schedule.setUpdateDate(now);
      schedule.setUpdateUserId(Integer.valueOf(ownerid));

      if (is_span) {
        // 期間スケジュール設定の場合
        schedule.setEndDate(end_date.getValue());
        schedule.setRepeatPattern("S");

        schedule.setStartDate(start_date.getValue());
      } else if (!is_repeat) {
        // 終了日時
        schedule.setEndDate(end_date.getValue());
        schedule.setRepeatPattern("N");

        schedule.setStartDate(start_date.getValue());
      } else {
        // 繰り返しスケジュール設定の場合
        char lim = 'N';
        Calendar cal = Calendar.getInstance();
        cal.setTime(end_date.getValue());
        if ("ON".equals(limit_flag.getValue())) {
          lim = 'L';
          cal.set(limit_end_date.getValue().getYear(), limit_end_date
            .getValue()
            .getMonth() - 1, limit_end_date.getValue().getDay());

          ALDateContainer container = limit_start_date.getValue();
          Calendar limitStartCal = Calendar.getInstance();
          limitStartCal.setTime(start_date.getValue());
          limitStartCal.set(Calendar.YEAR, container.getYear());
          limitStartCal.set(Calendar.MONTH, container.getMonth() - 1);
          limitStartCal.set(Calendar.DATE, container.getDay());
          schedule.setStartDate(limitStartCal.getTime());
        } else {
          schedule.setStartDate(start_date.getValue());
        }

        schedule.setEndDate(cal.getTime());
        if ("D".equals(repeat_type.getValue())) {
          schedule.setRepeatPattern(new StringBuffer()
            .append('D')
            .append(lim)
            .toString());
        } else if ("W".equals(repeat_type.getValue())) {
          if ("0".equals(repeat_week.getValue())) {
            schedule.setRepeatPattern(new StringBuffer().append('W').append(
              week_0.getValue() != null ? 1 : 0).append(
              week_1.getValue() != null ? 1 : 0).append(
              week_2.getValue() != null ? 1 : 0).append(
              week_3.getValue() != null ? 1 : 0).append(
              week_4.getValue() != null ? 1 : 0).append(
              week_5.getValue() != null ? 1 : 0).append(
              week_6.getValue() != null ? 1 : 0).append(lim).toString());
          } else {
            schedule.setRepeatPattern(new StringBuffer().append('W').append(
              week_0.getValue() != null ? 1 : 0).append(
              week_1.getValue() != null ? 1 : 0).append(
              week_2.getValue() != null ? 1 : 0).append(
              week_3.getValue() != null ? 1 : 0).append(
              week_4.getValue() != null ? 1 : 0).append(
              week_5.getValue() != null ? 1 : 0).append(
              week_6.getValue() != null ? 1 : 0).append(
              repeat_week.getValue().charAt(0)).append(lim).toString());
          }
        } else if ("M".equals(repeat_type.getValue())) {
          DecimalFormat format = new DecimalFormat("00");
          if (32 == month_day.getValue()) {
            schedule.setRepeatPattern(new StringBuffer().append('M').append(
              "XX").append(lim).toString());
          } else {
            schedule.setRepeatPattern(new StringBuffer().append('M').append(
              format.format(month_day.getValue())).append(lim).toString());
          }
        } else {
          DecimalFormat format = new DecimalFormat("00");
          schedule.setRepeatPattern(new StringBuffer().append('Y').append(
            format.format(year_month.getValue())).append(
            format.format(year_day.getValue())).append(lim).toString());
        }
      }

      EipTCommonCategory category1 =
        CommonCategoryUtils.getEipTCommonCategory(Long.valueOf(1));

      // 2007.3.28 ToDo連携
      // // スケジュールを登録
      // orm.doInsert(schedule);
      for (ALEipUser user : memberList) {
        EipTScheduleMap map = Database.create(EipTScheduleMap.class);
        int userid = (int) user.getUserId().getValue();

        map.setEipTSchedule(schedule);
        map.setUserId(Integer.valueOf(userid));
        // O: 自スケジュール T: 仮スケジュール C: 確定スケジュール
        if (userid == ALEipUtils.getUserId(rundata)) {
          map.setStatus("O");
        } else {
          map.setStatus("T");
        }
        EipTCommonCategory category =
          CommonCategoryUtils.getEipTCommonCategory(common_category_id
            .getValue());
        if (category == null) {
          map.setCommonCategoryId(Integer.valueOf(1));
          map.setEipTSchedule(schedule);
          map.setEipTCommonCategory(category1);
        } else {
          map.setCommonCategoryId(Integer.valueOf((int) (common_category_id
            .getValue())));
          map.setEipTSchedule(schedule);
          map.setEipTCommonCategory(category);
        }
        map.setType(ScheduleUtils.SCHEDULEMAP_TYPE_USER);
      }

      // 完全に隠すスケジュール以外の場合は、グループに設備を追加する
      if (("O".equals(public_flag.toString()) || "C".equals(public_flag
        .toString()))) {
        for (Object record : facilityList) {
          FacilityResultData frd = (FacilityResultData) record;

          EipTScheduleMap map = Database.create(EipTScheduleMap.class);
          int facilityid = (int) frd.getFacilityId().getValue();

          map.setEipTSchedule(schedule);
          map.setUserId(Integer.valueOf(facilityid));
          // O: 自スケジュール T: 仮スケジュール C: 確定スケジュール
          map.setStatus("O");
          map.setType(ScheduleUtils.SCHEDULEMAP_TYPE_FACILITY);
          map.setEipTCommonCategory(category1);
          map.setCommonCategoryId(Integer.valueOf(1));
        }
      }

      /* 設備重複判定 */
      if (!ignore_duplicate_facility) {
        if (facilityList.size() > 0) {
          ArrayList<Integer> facilityIds = new ArrayList<Integer>();

          for (Object record : facilityList) {
            FacilityResultData frd = (FacilityResultData) record;
            facilityIds.add(Integer.valueOf((int) frd
              .getFacilityId()
              .getValue()));
          }

          if (ScheduleUtils.isDuplicateFacilitySchedule(
            schedule,
            facilityIds,
            null,
            null)) {
            if (msgList.size() == 0) {
              msgList.add("duplicate_facility");
            }
            Database.rollback();
            return false;
          }
        }
      }
      // ファイルをデータベースに登録する．
      if (!ScheduleUtils.insertFileDataDelegate(
        rundata,
        context,
        schedule,
        fileuploadList,
        folderName,
        msgList)) {
        return false;
      }

      // スケジュールを登録
      Database.commit();

      // アラーム
      if (ALReminderService.isEnabled()) {
        boolean isLimit = false;
        if ("ON".equals(limit_flag.getValue())) {
          isLimit = true;
        }
        // メンバーに対して それぞれのDefaultJob をもとにリマインドを作成
        for (ALEipUser user : memberList) {
          ALReminderDefaultItem defaultItem =
            ALReminderService.getDefault(orgId, user
              .getUserId()
              .getValueAsString(), ReminderCategory.SCHEDULE);
          // DefaultItemがない場合は通知しない
          if (defaultItem != null && defaultItem.isEnabled()) {
            ScheduleUtils.setupReminderJob(
              Database.getDomainName(),
              user.getUserId().toString(),
              schedule,
              defaultItem.getNotifyTiming(),
              defaultItem.hasNotifyTypeMail(),
              defaultItem.hasNotifyTypeMessage(),
              is_repeat,
              isLimit,
              is_span);
          }
        }

      }

      // イベントログに保存
      ALEventlogFactoryService.getInstance().getEventlogHandler().log(
        schedule.getScheduleId(),
        ALEventlogConstants.PORTLET_TYPE_SCHEDULE,
        schedule.getName());

      // アクティビティを「あなた宛のお知らせ」に表示させる
      String loginName = loginUser.getName().getValue();
      List<String> recipients = new ArrayList<String>();
      for (ALEipUser user : memberList) {
        if (loginUser.getUserId().getValue() != user.getUserId().getValue()) {
          recipients.add(user.getName().getValue());
        }
      }
      ScheduleUtils.createShareScheduleActivity(
        schedule,
        loginName,
        recipients,
        "new",
        ownerid);

      // アクティビティが公開スケジュールである場合、「更新情報」に表示させる。
      if ("O".equals(public_flag.toString())) {
        ScheduleUtils.createNewScheduleActivity(
          schedule,
          loginName,
          "new",
          ownerid);
      }
    } catch (CayenneRuntimeException e) {
      if (e.getCause().getMessage().indexOf("Incorrect string value:") == 0) {
        msgList.add(ALLocalizationUtils
          .getl10n("SCHEDULE_UNAVAILABLE_CHARACTER"));
      }
      Database.rollback();
      logger.error("[ScheduleFormData]", e);
      return false;
    } catch (Exception e) {
      Database.rollback();
      logger.error("[ScheduleFormData]", e);
      return false;
    }
    if (ScheduleUtils.MAIL_FOR_ALL.equals(schedule.getMailFlag())
      || ScheduleUtils.MAIL_FOR_INSERT.equals(schedule.getMailFlag())) {
      try {
        // メール送信
        int msgType =
          ALMailUtils.getSendDestType(ALMailUtils.KEY_MSGTYPE_SCHEDULE);
        if (msgType > 0) {
          // パソコンへメールを送信
          List<ALEipUserAddr> destMemberList =
            ALMailUtils.getALEipUserAddrs(memberList, ALEipUtils
              .getUserId(rundata), false);
          String subject = "[" + ALOrgUtilsService.getAlias() + "]スケジュール";
          String orgId = Database.getDomainName();

          List<ALAdminMailMessage> messageList =
            new ArrayList<ALAdminMailMessage>();
          for (ALEipUserAddr destMember : destMemberList) {
            ALAdminMailMessage message = new ALAdminMailMessage(destMember);
            message.setPcSubject(subject);
            message.setCellularSubject(subject);
            message.setPcBody(ScheduleUtils.createMsg(
              rundata,
              schedule,
              memberList,
              null,
              "new"));
            message.setCellularBody(ScheduleUtils.createMsg(
              rundata,
              schedule,
              memberList,
              destMember.getUserId(),
              "new"));
            messageList.add(message);
          }

          ALMailService.sendAdminMailAsync(new ALAdminMailContext(
            orgId,
            ALEipUtils.getUserId(rundata),
            messageList,
            ALMailUtils.getSendDestType(ALMailUtils.KEY_MSGTYPE_SCHEDULE)));
          // msgList.addAll(errors);

        }
      } catch (Exception ex) {
        msgList.add(ALLocalizationUtils.getl10n("SCHEDULE_DONOT_SEND_MAIL"));
        logger.error("schedule", ex);
        return false;
      }
    }
    return true;
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
  protected boolean updateFormData(RunData rundata, Context context,
      List<String> msgList) throws ALPageNotFoundException, ALDBErrorException {
    List<Integer> newmemberList = new ArrayList<Integer>();
    List<ALEipUser> removememberList = new ArrayList<ALEipUser>();
    List<Integer> memberIdsList = new ArrayList<Integer>();
    EipTSchedule schedule = null;
    EipTSchedule newSchedule = null;
    EipTSchedule tmpSchedule = null;
    try {
      // Validate のときに SELECT していることに注意する

      if (is_span) {
        Calendar startDate = Calendar.getInstance();
        startDate.setTime(start_date.getValue());
        startDate.set(Calendar.HOUR_OF_DAY, 0);
        startDate.set(Calendar.MINUTE, 0);

        Calendar endDate = Calendar.getInstance();
        endDate.setTime(end_date.getValue());
        endDate.set(Calendar.HOUR_OF_DAY, 0);
        endDate.set(Calendar.MINUTE, 0);

        start_date.setValue(startDate.getTime());
        end_date.setValue(endDate.getTime());
      } else {
        Calendar startcal = new GregorianCalendar();
        startcal.setTime(start_date.getValue());
        Calendar endcal = Calendar.getInstance();
        endcal.setTime(end_date.getValue());
        endcal.set(Calendar.YEAR, startcal.get(Calendar.YEAR));
        endcal.set(Calendar.MONTH, startcal.get(Calendar.MONTH));
        endcal.set(Calendar.DATE, startcal.get(Calendar.DATE));
        end_date.setValue(endcal.getTime());
      }

      // オブジェクトモデルを取得
      schedule = ScheduleUtils.getEipTSchedule(rundata, context, false);
      if (schedule == null) {
        return false;
      }
      tmpSchedule = schedule;
      // int ownerid = ALEipUtils.getUserId(rundata);
      int ownerid = schedule.getOwnerId();

      // 設備のアクセスコントロールのチェック
      if (!facilityCheckAclPermission(
        rundata,
        ALAccessControlConstants.VALUE_ACL_UPDATE)) {
        int[] old_ids = ScheduleUtils.getFacilityIds(schedule);
        if (old_ids.length != facilityList.size()) {
          msgList.add(ALLocalizationUtils
            .getl10n("SCHEDULE_NO_PERMISSION_TO_MAKE_A_RESERVATION"));
          return false;
        }
        boolean check = false;
        for (int old_id : old_ids) {
          for (Object record : facilityList) {
            FacilityResultData frd = (FacilityResultData) record;
            int facilityid = (int) frd.getFacilityId().getValue();
            if (old_id == facilityid) {
              check = true;
              break;
            }
          }
          if (!check) {
            msgList.add(ALLocalizationUtils
              .getl10n("SCHEDULE_NO_PERMISSION_TO_MAKE_A_RESERVATION"));
            return false;
          }
          check = false;
        }
      }

      // スケジュールのアップデート権限を検証する．
      /*
       * if (ownerid != schedule.getOwnerId().intValue() &&
       * "F".equals(schedule.getEditFlag())) { // アップデート失敗時は、スケジュールの一覧を表示させる．
       * return true; }
       */

      // 新たに追加されたメンバーのリストを取得
      List<?> scheduleMapList = schedule.getEipTScheduleMaps();
      List<Integer> oldmemberIdList = new ArrayList<Integer>();
      for (Object record : scheduleMapList) {
        EipTScheduleMap map = (EipTScheduleMap) record;
        if (ScheduleUtils.SCHEDULEMAP_TYPE_USER.equals(map.getType())) {
          oldmemberIdList.add(map.getUserId());
        }
      }
      for (ALEipUser user : memberList) {
        int memberId = (int) user.getUserId().getValue();
        if (!isContains(oldmemberIdList, memberId)) {
          newmemberList.add(memberId);
        }
        memberIdsList.add(memberId);
      }

      for (Integer memberId : oldmemberIdList) {
        if (!isContains(memberIdsList, memberId)) {
          removememberList.add(ALEipUtils.getALEipUser(memberId));
        }
      }

      EipTCommonCategory category1 =
        CommonCategoryUtils.getEipTCommonCategory(Long.valueOf(1));

      if (edit_repeat_flag.getValue() == FLAG_EDIT_REPEAT_ONE) {
        // 繰り返しスケジュールの個別日程を変更する．
        // 新規オブジェクトモデル
        newSchedule = Database.create(EipTSchedule.class);
        // 繰り返しの親スケジュール ID
        newSchedule.setParentId(schedule.getScheduleId());
        // タイトル
        newSchedule.setName(name.getValue());
        // 場所
        newSchedule.setPlace(place.getValue());
        // 内容
        newSchedule.setNote(note.getValue());
        // 公開フラグ
        newSchedule.setPublicFlag(public_flag.getValue());
        // 共有メンバーによる編集／削除フラグ
        newSchedule.setEditFlag("F");
        newSchedule.setEditFlag(schedule.getEditFlag());
        // メール受信フラグ
        newSchedule.setMailFlag(mail_flag);
        // オーナーID
        newSchedule.setOwnerId(Integer.valueOf(ownerid));
        // 作成日
        Date now = new Date();
        newSchedule.setCreateDate(now);
        newSchedule.setCreateUserId(Integer.valueOf(ownerid));
        // 更新日
        newSchedule.setUpdateDate(now);
        newSchedule.setUpdateUserId(Integer.valueOf(ALEipUtils
          .getUserId(rundata)));
        // 終了日時
        newSchedule.setEndDate(end_date.getValue());
        newSchedule.setRepeatPattern("N");
        newSchedule.setStartDate(start_date.getValue());

        // 2007.3.28 ToDo連携

        int allsize = memberList.size() + facilityList.size();
        if (allsize > 0) {
          List<?> scheduleMaps = schedule.getEipTScheduleMaps();
          for (ALEipUser user : memberList) {
            EipTScheduleMap map = Database.create(EipTScheduleMap.class);
            int userid = (int) user.getUserId().getValue();

            map.setEipTSchedule(newSchedule);
            map.setUserId(Integer.valueOf(userid));

            if (userid == ownerid) {
              map.setStatus("O");
            } else {
              if ("T".equals(change_tmpreserve_flag.getValue())) {
                map.setStatus("T");
              } else {
                EipTScheduleMap tmpMap =
                  getScheduleMap(
                    scheduleMaps,
                    userid,
                    ScheduleUtils.SCHEDULEMAP_TYPE_USER);
                if (tmpMap != null) {
                  map.setStatus(tmpMap.getStatus());
                } else {
                  map.setStatus("T");
                }
              }
            }
            EipTCommonCategory category =
              CommonCategoryUtils.getEipTCommonCategory(common_category_id
                .getValue());
            if (category == null) {
              map.setCommonCategoryId(Integer.valueOf(1));
              map.setEipTCommonCategory(category1);
            } else {
              map.setCommonCategoryId(Integer.valueOf((int) (common_category_id
                .getValue())));
              map.setEipTCommonCategory(category);
            }
            map.setType(ScheduleUtils.SCHEDULEMAP_TYPE_USER);
          }

          // 完全に隠すスケジュール以外の場合は、グループに設備を追加する
          if (("O".equals(public_flag.toString()) || "C".equals(public_flag
            .toString()))) {
            for (Object record : facilityList) {
              FacilityResultData frd = (FacilityResultData) record;
              int facilityid = (int) frd.getFacilityId().getValue();

              EipTScheduleMap map = Database.create(EipTScheduleMap.class);
              map.setEipTSchedule(newSchedule);
              map.setUserId(Integer.valueOf(facilityid));
              map.setStatus("O");
              map.setType(ScheduleUtils.SCHEDULEMAP_TYPE_FACILITY);
              map.setEipTCommonCategory(category1);
              map.setCommonCategoryId(Integer.valueOf(1));
            }
          }
        }

        // 登録されていたメンバーと今回追加されたメンバーのユーザー ID を取得する．
        List<?> scheduleMaps = schedule.getEipTScheduleMaps();
        List<Integer> memberIdList = new ArrayList<Integer>();
        List<Integer> facilityIdList = new ArrayList<Integer>();
        for (Object record : scheduleMaps) {
          EipTScheduleMap map = (EipTScheduleMap) record;
          if (ScheduleUtils.SCHEDULEMAP_TYPE_USER.equals(map.getType())) {
            memberIdList.add(map.getUserId());
          } else {
            facilityIdList.add(map.getUserId());
          }
        }

        for (ALEipUser user : memberList) {
          int memberId = (int) user.getUserId().getValue();
          if (!isContains(memberIdList, memberId)) {
            memberIdList.add(Integer.valueOf(memberId));
          }
        }
        int memberIdListSize = memberIdList.size();
        int[] memberIds = new int[memberIdListSize];
        for (int i = 0; i < memberIdListSize; i++) {
          memberIds[i] = memberIdList.get(i).intValue();
        }

        for (ListIterator<Object> record =
          facilityList.listIterator(facilityList.size()); record.hasNext();) {
          int facilityId =
            (int) ((FacilityResultData) record).getFacilityId().getValue();
          if (!isContains(facilityIdList, facilityId)) {
            facilityList.add(Integer.valueOf(facilityId));
          }
        }
        int facilityIdListSize = facilityIdList.size();
        int[] facilityIds = new int[facilityIdListSize];
        for (int i = 0; i < facilityIdListSize; i++) {
          facilityIds[i] = facilityIdList.get(i).intValue();
        }

        // 設備重複判定
        if (!ignore_duplicate_facility) {
          if (facilityIdList.size() > 0) {
            if (ScheduleUtils.isDuplicateFacilitySchedule(
              newSchedule,
              facilityIdList,
              schedule.getScheduleId(),
              view_date.getValue())) {
              if (msgList.size() == 0) {
                msgList.add("duplicate_facility");
              }
              Database.rollback();
              return false;
            }
          }
        }

        // ダミーのスケジュールを登録する。
        // 内部でDatabase.commit()が呼び出されることに注意
        ScheduleUtils.insertDummySchedule(schedule, ownerid, view_date
          .getValue(), view_date.getValue(), memberIds, facilityIds);
        tmpSchedule = newSchedule;
      } else {
        // タイトル
        schedule.setName(name.getValue());
        // 場所
        schedule.setPlace(place.getValue());
        // 内容
        schedule.setNote(note.getValue());
        // 公開フラグ
        schedule.setPublicFlag(public_flag.getValue());
        // メールフラグ
        schedule.setMailFlag(mail_flag);
        // 共有メンバーによる編集／削除フラグ
        if (schedule.getOwnerId().intValue() == ALEipUtils.getUserId(rundata)
          || schedule.getOwnerId().intValue() == 0) {
          schedule.setEditFlag(edit_flag.getValue());
        }

        // スケジュールの所有ユーザがすでにメンバーから抜けているかを検証する．
        int ownerUserId = schedule.getOwnerId().intValue();
        boolean rejectOwnerUser = false;
        List<?> tmpScheduleMaps = schedule.getEipTScheduleMaps();
        for (Object record : tmpScheduleMaps) {
          EipTScheduleMap map = (EipTScheduleMap) record;
          if (ownerUserId == map.getUserId().intValue()
            && "R".equals(map.getStatus())) {
            rejectOwnerUser = true;
            break;
          }
        }

        if (rejectOwnerUser) {
          // スケジュールの登録ユーザがすでにメンバーから抜けている場合、
          // 最後に更新した人のユーザ ID をオーナ ID に設定する．
          schedule.setOwnerId(Integer.valueOf(ALEipUtils.getUserId(rundata)));
        }

        // 更新日
        schedule.setUpdateDate(new Date());
        schedule
          .setUpdateUserId(Integer.valueOf(ALEipUtils.getUserId(rundata)));

        if (is_span) {
          schedule.setEndDate(end_date.getValue());
          schedule.setRepeatPattern("S");

          schedule.setStartDate(start_date.getValue());
        } else if (!is_repeat) {
          // 終了日時
          schedule.setEndDate(end_date.getValue());
          schedule.setRepeatPattern("N");

          schedule.setStartDate(start_date.getValue());
        } else {
          char lim = 'N';
          Calendar cal = Calendar.getInstance();
          cal.setTime(end_date.getValue());
          if ("ON".equals(limit_flag.getValue())) {
            lim = 'L';
            cal.set(limit_end_date.getValue().getYear(), limit_end_date
              .getValue()
              .getMonth() - 1, limit_end_date.getValue().getDay());

            ALDateContainer container = limit_start_date.getValue();
            Calendar limitStartCal = Calendar.getInstance();
            limitStartCal.setTime(start_date.getValue());
            limitStartCal.set(Calendar.YEAR, container.getYear());
            limitStartCal.set(Calendar.MONTH, container.getMonth() - 1);
            limitStartCal.set(Calendar.DATE, container.getDay());

            schedule.setStartDate(limitStartCal.getTime());
          } else {
            schedule.setStartDate(start_date.getValue());
          }

          schedule.setEndDate(cal.getTime());
          if ("D".equals(repeat_type.getValue())) {
            String tmpPattern =
              new StringBuffer().append('D').append(lim).toString();
            schedule.setRepeatPattern(tmpPattern);
          } else if ("W".equals(repeat_type.getValue())) {
            if ("0".equals(repeat_week.getValue())) {
              String tmpPattern =
                new StringBuffer().append('W').append(
                  week_0.getValue() != null ? 1 : 0).append(
                  week_1.getValue() != null ? 1 : 0).append(
                  week_2.getValue() != null ? 1 : 0).append(
                  week_3.getValue() != null ? 1 : 0).append(
                  week_4.getValue() != null ? 1 : 0).append(
                  week_5.getValue() != null ? 1 : 0).append(
                  week_6.getValue() != null ? 1 : 0).append(lim).toString();

              schedule.setRepeatPattern(tmpPattern);
            } else {
              String tmpPattern =
                new StringBuffer().append('W').append(
                  week_0.getValue() != null ? 1 : 0).append(
                  week_1.getValue() != null ? 1 : 0).append(
                  week_2.getValue() != null ? 1 : 0).append(
                  week_3.getValue() != null ? 1 : 0).append(
                  week_4.getValue() != null ? 1 : 0).append(
                  week_5.getValue() != null ? 1 : 0).append(
                  week_6.getValue() != null ? 1 : 0).append(
                  repeat_week.getValue().charAt(0)).append(lim).toString();
              schedule.setRepeatPattern(tmpPattern);
            }
          } else if ("M".equals(repeat_type.getValue())) {
            DecimalFormat format = new DecimalFormat("00");
            if (32 == month_day.getValue()) {
              schedule.setRepeatPattern(new StringBuffer().append('M').append(
                "XX").append(lim).toString());
            } else {
              schedule.setRepeatPattern(new StringBuffer().append('M').append(
                format.format(month_day.getValue())).append(lim).toString());
            }
          } else {
            DecimalFormat format = new DecimalFormat("00");
            schedule.setRepeatPattern(new StringBuffer().append('Y').append(
              format.format(year_month.getValue())).append(
              format.format(year_day.getValue())).append(lim).toString());
          }
        }

        List<?> scheduleMaps = schedule.getEipTScheduleMaps();

        Database.deleteAll(scheduleMaps);
        // スケジュールを登録
        // Database.commit();

        for (ALEipUser user : memberList) {
          EipTScheduleMap map = Database.create(EipTScheduleMap.class);
          int userid = (int) user.getUserId().getValue();
          map.setScheduleId(schedule.getScheduleId());
          map.setEipTSchedule(schedule);
          map.setUserId(Integer.valueOf(userid));

          // O: 自スケジュール T: 仮スケジュール C: 確定スケジュール
          if (userid == schedule.getOwnerId().intValue()) {
            map.setStatus("O");
          } else {
            if ("T".equals(change_tmpreserve_flag.getValue())) {
              map.setStatus("T");
            } else {
              EipTScheduleMap tmpMap =
                getScheduleMap(
                  scheduleMaps,
                  userid,
                  ScheduleUtils.SCHEDULEMAP_TYPE_USER);
              if (tmpMap != null) {
                map.setStatus(tmpMap.getStatus());
              } else {
                map.setStatus("T");
              }
            }
          }

          EipTCommonCategory category =
            CommonCategoryUtils.getEipTCommonCategory(common_category_id
              .getValue());
          if (category == null) {
            map.setCommonCategoryId(Integer.valueOf(1));
            map.setEipTSchedule(schedule);
            map.setEipTCommonCategory(category1);
          } else {
            map.setCommonCategoryId(Integer.valueOf((int) (common_category_id
              .getValue())));
            map.setEipTSchedule(schedule);
            map.setEipTCommonCategory(category);
          }

          map.setType(ScheduleUtils.SCHEDULEMAP_TYPE_USER);
          schedule.addToEipTScheduleMaps(map);
        }

        // 完全に隠すスケジュール以外の場合は、グループに設備を追加する
        if (("O".equals(public_flag.toString()) || "C".equals(public_flag
          .toString()))) {
          for (Object record : facilityList) {
            FacilityResultData frd = (FacilityResultData) record;
            int facilityid = (int) frd.getFacilityId().getValue();

            EipTScheduleMap map = Database.create(EipTScheduleMap.class);
            map.setEipTSchedule(schedule);
            map.setUserId(Integer.valueOf(facilityid));
            // O: 自スケジュール T: 仮スケジュール C: 確定スケジュール
            map.setStatus("O");
            map.setType(ScheduleUtils.SCHEDULEMAP_TYPE_FACILITY);
            map.setEipTCommonCategory(category1);
            map.setCommonCategoryId(Integer.valueOf(1));
          }
        }

        // 設備重複判定
        if (!ignore_duplicate_facility) {
          if (facilityList.size() > 0) {
            ArrayList<Integer> fids = new ArrayList<Integer>();
            FacilityResultData facility = null;
            for (Object record : facilityList) {
              facility = (FacilityResultData) record;
              fids.add(Integer.valueOf((int) facility
                .getFacilityId()
                .getValue()));
            }
            if (ScheduleUtils.isDuplicateFacilitySchedule(
              schedule,
              fids,
              null,
              null)) {
              if (msgList.size() == 0) {
                msgList.add("duplicate_facility");
              }
              Database.rollback();
              return false;
            }
          }
        }
      }

      if (!ScheduleUtils.insertFileDataDelegate(
        rundata,
        context,
        tmpSchedule,
        fileuploadList,
        folderName,
        msgList)) {
        return false;
      }

      // スケジュールを登録
      Database.commit();

      // アラーム
      if (ALReminderService.isEnabled() || ALReminderService.isPastEnabled()) {
        boolean isLimit = false;
        if ("ON".equals(limit_flag.getValue())) {
          isLimit = true;
        }
        EipTSchedule targetSchedule = null;
        if (edit_repeat_flag.getValue() == FLAG_EDIT_REPEAT_ONE) {
          targetSchedule = newSchedule;
        } else {
          targetSchedule = schedule;
        }
        // メンバーからはずれた人のJobを削除する
        for (ALEipUser user : removememberList) {
          if (schedule.getScheduleId().intValue() == targetSchedule
            .getScheduleId()
            .intValue()) {
            ALReminderItem item = new ALReminderItem();
            item.setOrgId(Database.getDomainName());
            item.setUserId(user.getUserId().toString());
            item.setItemId(schedule.getScheduleId().intValue());
            item.setCategory(ReminderCategory.SCHEDULE);
            ALReminderService.removeJob(item);
          }
        }
        // メンバーに対しては それぞれのJob をもとにリマインドを作成
        for (ALEipUser user : memberList) {
          int memberId = (int) user.getUserId().getValue();
          if (isContains(newmemberList, memberId)) {
            // 追加されたメンバー
            ALReminderDefaultItem defaultItem =
              ALReminderService.getDefault(orgId, user
                .getUserId()
                .getValueAsString(), ReminderCategory.SCHEDULE);
            // DefaultItemがない場合は通知しない
            if (defaultItem != null && defaultItem.isEnabled()) {
              ScheduleUtils.setupReminderJob(
                Database.getDomainName(),
                user.getUserId().toString(),
                targetSchedule,
                defaultItem.getNotifyTiming(),
                defaultItem.hasNotifyTypeMail(),
                defaultItem.hasNotifyTypeMessage(),
                is_repeat,
                isLimit,
                is_span);
            }
          } else {
            // もともといたメンバー
            ALReminderItem item =
              ALReminderService.getJob(orgId, user
                .getUserId()
                .getValueAsString(), ReminderCategory.SCHEDULE, schedule
                .getScheduleId()
                .intValue());
            if (item != null) {
              if (!is_span) {
                ScheduleUtils.setupReminderJob(
                  Database.getDomainName(),
                  user.getUserId().toString(),
                  targetSchedule,
                  item.getNotifyTiming(),
                  item.hasNotifyTypeMail(),
                  item.hasNotifyTypeMessage(),
                  is_repeat,
                  isLimit,
                  is_span);
              } else {
                if (schedule.getScheduleId().intValue() == targetSchedule
                  .getScheduleId()
                  .intValue()) {
                  ALReminderService.removeJob(item);
                }
              }
            }
          }
        }
      }

      // イベントログに保存
      ALEventlogFactoryService.getInstance().getEventlogHandler().log(
        schedule.getScheduleId(),
        ALEventlogConstants.PORTLET_TYPE_SCHEDULE,
        schedule.getName());

      // アクティビティ
      String loginName = loginUser.getName().getValue();
      List<String> recipients = new ArrayList<String>();
      for (ALEipUser user : memberList) {
        if (loginUser.getUserId().getValue() != user.getUserId().getValue()) {
          recipients.add(user.getName().getValue());
        }
      }
      int userid = ALEipUtils.getUserId(rundata);
      ScheduleUtils.createShareScheduleActivity(
        tmpSchedule,
        loginName,
        recipients,
        "edit",
        userid);

      // アクティビティが公開スケジュールである場合、「更新情報」に表示させる。
      if ("O".equals(public_flag.toString())) {
        ScheduleUtils.createNewScheduleActivity(
          schedule,
          loginName,
          "edit",
          userid);
      }
    } catch (RuntimeException e) {
      // RuntimeException
      Database.rollback();
      logger.error("[ScheduleFormData]", e);
      throw new ALDBErrorException();
    } catch (Exception e) {
      Database.rollback();
      logger.error("[ScheduleFormData]", e);
      msgList.add(ALLocalizationUtils.getl10n("ERROR_UPDATE_FAILURE"));
      return false;
      // throw new ALDBErrorException();
    }
    if (ScheduleUtils.MAIL_FOR_ALL.equals(schedule.getMailFlag())
      || ScheduleUtils.MAIL_FOR_UPDATE.equals(schedule.getMailFlag())) {
      try {
        // メール送信
        int msgType =
          ALMailUtils.getSendDestType(ALMailUtils.KEY_MSGTYPE_SCHEDULE);
        if (msgType > 0) {
          // パソコンへメールを送信
          List<ALEipUserAddr> destMemberList =
            ALMailUtils.getALEipUserAddrs(memberList, ALEipUtils
              .getUserId(rundata), false);
          String subject = "[" + ALOrgUtilsService.getAlias() + "]スケジュール";

          if (edit_repeat_flag.getValue() == FLAG_EDIT_REPEAT_ONE) {
            List<ALAdminMailMessage> messageList =
              new ArrayList<ALAdminMailMessage>();
            for (ALEipUserAddr destMember : destMemberList) {
              ALAdminMailMessage message = new ALAdminMailMessage(destMember);
              message.setPcSubject(subject);
              message.setCellularSubject(subject);
              message.setPcBody(ScheduleUtils.createMsg(
                rundata,
                newSchedule,
                memberList,
                null,
                "edit"));
              message.setCellularBody(ScheduleUtils.createMsg(
                rundata,
                newSchedule,
                memberList,
                destMember.getUserId(),
                "edit"));
            }

            ALMailService.sendAdminMailAsync(new ALAdminMailContext(
              orgId,
              ALEipUtils.getUserId(rundata),
              messageList,
              ALMailUtils.getSendDestType(ALMailUtils.KEY_MSGTYPE_SCHEDULE)));
            // msgList.addAll(errors);

          } else {
            List<ALAdminMailMessage> messageList =
              new ArrayList<ALAdminMailMessage>();
            for (ALEipUserAddr destMember : destMemberList) {
              ALAdminMailMessage message = new ALAdminMailMessage(destMember);
              message.setPcSubject(subject);
              message.setCellularSubject(subject);
              message.setPcBody(ScheduleUtils.createMsg(
                rundata,
                schedule,
                memberList,
                null,
                "edit"));
              message.setCellularBody(ScheduleUtils.createMsg(
                rundata,
                schedule,
                memberList,
                destMember.getUserId(),
                "edit"));
              messageList.add(message);
            }

            ALMailService.sendAdminMailAsync(new ALAdminMailContext(
              orgId,
              ALEipUtils.getUserId(rundata),
              messageList,
              ALMailUtils.getSendDestType(ALMailUtils.KEY_MSGTYPE_SCHEDULE)));
            // msgList.addAll(errors);
          }
        }
      } catch (Exception ex) {
        msgList.add(ALLocalizationUtils.getl10n("SCHEDULE_DONOT_SEND_MAIL"));
        logger.error("schedule", ex);
        return false;
      }
    }
    return true;
  }

  private EipTScheduleMap getScheduleMap(List<?> scheduleMaps, int userid,
      String type) {
    for (Object record : scheduleMaps) {
      EipTScheduleMap map = (EipTScheduleMap) record;
      if (map.getUserId().intValue() == userid && type.equals(map.getType())) {
        return map;
      }
    }
    return null;
  }

  /**
   * 参加ユーザー全員の予定を完全に削除します。
   *
   * @param schedule
   * @param members
   */
  private void deleteMemberAllRangeAll(EipTSchedule schedule,
      List<ALEipUser> members) throws ALDBErrorException,
      ALPageNotFoundException {
    deleteSchedule(schedule);
  }

  /**
   * 参加ユーザー全員の一日分の予定を削除します。
   *
   * @param schedule
   * @param members
   * @throws ALDBErrorException
   * @throws ALPageNotFoundException
   */
  private void deleteMemberAllRangeOneday(EipTSchedule schedule,
      List<ALEipUser> members) throws ALDBErrorException,
      ALPageNotFoundException {
    if (!"N".equals(schedule.getRepeatPattern())) {
      int ownerid = (int) loginUser.getUserId().getValue();

      // ダミーのスケジュールを登録する．
      int memberIdListSize = memberList.size();
      int[] memberIdList = new int[memberIdListSize];
      for (int i = 0; i < memberIdListSize; i++) {
        memberIdList[i] = (int) memberList.get(i).getUserId().getValue();
      }

      // 同時に削除する設備ID一覧を取得する。
      int[] facilityIdList = ScheduleUtils.getFacilityIds(schedule);

      ScheduleUtils.insertDummySchedule(
        schedule,
        ownerid,
        view_date.getValue(),
        view_date.getValue(),
        memberIdList,
        facilityIdList);
    }
  }

  /**
   * 特定のメンバーの予定を完全に削除します。
   *
   * @param schedule
   * @param members
   * @throws ALDBErrorException
   * @throws ALPageNotFoundException
   */
  private void deleteMemberOneRangeAll(EipTSchedule schedule,
      List<ALEipUser> members, boolean isFacility, int deleteUserId,
      boolean acl_delete_other) throws ALPageNotFoundException,
      ALDBErrorException {

    List<?> scheduleMaps = ScheduleUtils.getEipTScheduleMaps(schedule);
    if (scheduleMaps != null && scheduleMaps.size() > 0) {
      if (isFacility) {
        // 設備を削除する場合
        for (Object reocrd : scheduleMaps) {
          EipTScheduleMap scheduleMap = (EipTScheduleMap) reocrd;
          if (ScheduleUtils.SCHEDULEMAP_TYPE_FACILITY.equals(scheduleMap
            .getType())) {
            if (scheduleMap.getUserId().intValue() == deleteUserId) {
              Database.delete(scheduleMap);
            }
          }
        }
      } else {
        // ユーザを削除する場合
        List<EipTScheduleMap> tmpScheduleMaps =
          new ArrayList<EipTScheduleMap>();
        int countRejectSchedule = 0;
        for (Object reocrd : scheduleMaps) {
          EipTScheduleMap scheduleMap = (EipTScheduleMap) reocrd;
          if (ScheduleUtils.SCHEDULEMAP_TYPE_USER.equals(scheduleMap.getType())) {
            tmpScheduleMaps.add(scheduleMap);
            if ("R".equals(scheduleMap.getStatus())) {
              countRejectSchedule += 1;
            }
          }
        }

        int scheduleMapsSize = tmpScheduleMaps.size();

        if (countRejectSchedule >= scheduleMapsSize - 1) {
          // この schedule ID に関係するスケジュールがすべて reject されたため，
          // すべて削除する．
          deleteSchedule(schedule);
        } else {
          for (int i = 0; i < scheduleMapsSize; i++) {
            EipTScheduleMap scheduleMap = tmpScheduleMaps.get(i);
            if (scheduleMap.getUserId().intValue() == deleteUserId) {
              int loginUserId = (int) login_user.getUserId().getValue();
              if ((scheduleMap.getUserId().intValue() == loginUserId)
                || (schedule.getCreateUserId().intValue() == loginUserId)
                || acl_delete_other) {
                if ("O".equals(scheduleMap.getStatus())) {
                  schedule.setOwnerId(Integer.valueOf(0));
                  if ("F".equals(schedule.getEditFlag())) {
                    // 削除するユーザーが，スケジュールの登録者であり，
                    // かつ，そのスケジュールの編集権限が他の共有メンバーに与えられていないときには，
                    // そのスケジュールの編集権限を 'T' に設定する．
                    schedule.setEditFlag("T");
                  }
                }
                scheduleMap.setStatus("R");
              }
            }
          }
        }
      }
    }
  }

  /**
   * 特定のメンバーの一日分の予定を削除します。
   *
   * @param schedule
   * @param members
   * @throws ALDBErrorException
   * @throws ALPageNotFoundException
   */
  private void deleteMemberOneRangeOneday(EipTSchedule schedule,
      List<ALEipUser> members, boolean isFacility, int deleteUserId,
      boolean acl_delete_other, RunData rundata, Context context)
      throws ALDBErrorException, ALPageNotFoundException {
    if (!"N".equals(schedule.getRepeatPattern())) {
      int loginUserId = (int) loginUser.getUserId().getValue();

      if (isFacility) {
        memberList = new ArrayList<ALEipUser>();
        this.loadFormData(rundata, context, new ArrayList<String>());
        for (int i = 0; facilityList.size() > i; i++) {
          if (((FacilityResultData) facilityList.get(i))
            .getFacilityId()
            .getValue() == deleteUserId) {
            facilityList.remove(i);
            break;
          }
        }
        edit_repeat_flag.setValue(ScheduleFormData.FLAG_EDIT_REPEAT_ONE);
        this.updateFormData(rundata, context, new ArrayList<String>());
      } else {
        // 参加ユーザー数を調べる
        EipTScheduleMap map;
        int user_count = 0;
        List<?> maps = schedule.getEipTScheduleMaps();
        for (Object record : maps) {
          map = (EipTScheduleMap) record;
          if (map.getType().equals(ScheduleUtils.SCHEDULEMAP_TYPE_USER)) {
            user_count++;
          }
        }

        // 既に登録されているダミースケジュールを検索する
        SelectQuery<EipTSchedule> dummy_query =
          Database.query(EipTSchedule.class);
        Expression exp1 =
          ExpressionFactory.matchExp(EipTSchedule.PARENT_ID_PROPERTY, schedule
            .getScheduleId());
        Expression exp2 =
          ExpressionFactory.matchExp(
            EipTSchedule.START_DATE_PROPERTY,
            view_date.getValue());
        Expression exp3 =
          ExpressionFactory.matchExp(EipTSchedule.END_DATE_PROPERTY, view_date
            .getValue());
        dummy_query.setQualifier(exp1);
        dummy_query.andQualifier(exp2);
        dummy_query.andQualifier(exp3);
        List<EipTSchedule> list = dummy_query.fetchList();

        // ダミー登録されている人数をカウントする
        int dummy_count = 0;
        for (EipTSchedule dummy : list) {
          List<?> dummymaps = dummy.getEipTScheduleMaps();
          for (Object record : dummymaps) {
            EipTScheduleMap dummymap = (EipTScheduleMap) record;
            if (dummymap.getType().equals(ScheduleUtils.SCHEDULEMAP_TYPE_USER)) {
              dummy_count++;
            }
          }
        }

        int[] facilityIdList = null;
        if (dummy_count >= user_count - 1) {
          // if( 登録済みダミースケジュール数合計 >= スケジュール参加者数 - 今回予定を消す人 )
          // 全員の予定が消されているので、同時に削除する設備ID一覧を取得する。
          facilityIdList = ScheduleUtils.getFacilityIds(schedule);
        }
        ScheduleUtils.insertDummySchedule(
          schedule,
          loginUserId,
          view_date.getValue(),
          view_date.getValue(),
          new int[] { deleteUserId },
          facilityIdList);
      }
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
  protected boolean deleteFormData(RunData rundata, Context context,
      List<String> msgList) throws ALPageNotFoundException, ALDBErrorException {
    try {
      // パラメータを取得する．
      if (rundata.getParameters().containsKey("del_member_flag")) {
        del_member_flag.setValue(rundata.getParameters().getString(
          "del_member_flag"));
      }
      if (rundata.getParameters().containsKey("del_range_flag")) {
        del_range_flag.setValue(rundata.getParameters().getString(
          "del_range_flag"));
      }
      int userid = 0; // 削除対象のユーザID
      if (rundata.getParameters().containsKey("userid")) {
        userid = rundata.getParameters().getInt("userid");
      }
      boolean is_facility = false; // 削除のユーザIDが設備かどうか
      if (rundata.getParameters().containsKey("type")) {
        is_facility =
          ScheduleUtils.SCHEDULEMAP_TYPE_FACILITY.equals(rundata
            .getParameters()
            .getString("type"));
      }

      // オブジェクトモデルを取得
      EipTSchedule schedule =
        ScheduleUtils.getEipTSchedule(rundata, context, false);
      if (schedule == null) {
        return false;
      }

      // 共有メンバーを取得する．
      List<ALEipUser> members = ScheduleUtils.getUsers(rundata, context, true);
      if (members != null && members.size() > 0) {
        memberList.addAll(members);
      }

      boolean acl_delete_other =
        ScheduleUtils.hasAuthorityForOtherSchedule(
          rundata,
          ALAccessControlConstants.VALUE_ACL_DELETE);

      boolean isMember = false;
      int loginuserId = (int) login_user.getUserId().getValue();
      for (ALEipUser user : memberList) {
        if (loginuserId == user.getUserId().getValue()) {
          isMember = true;
          break;
        }
      }
      if (!acl_delete_other
        && !isMember
        && (schedule.getCreateUserId().intValue() != loginuserId)) {
        logger
          .error("[ScheduleFormData] ALPageNotFoundException: The user does not have the auth to delete the schedule.");
        throw new ALPermissionException();
      }

      if (!acl_delete_other
        && loginuserId != schedule.getOwnerId().intValue()
        && "F".equals(schedule.getEditFlag())
        && FLAG_DEL_MEMBER_ONE != del_member_flag.getValue()) {
        logger
          .error("[ScheduleFormData] ALPageNotFoundException: The user does not have the auth to delete the schedule.");
        throw new ALPermissionException();
      }

      if (del_member_flag.getValue() == FLAG_DEL_MEMBER_ALL) {
        if (del_range_flag.getValue() == FLAG_DEL_RANGE_ALL) {
          deleteMemberAllRangeAll(schedule, members);
        } else {
          deleteMemberAllRangeOneday(schedule, members);
        }
      } else {
        if (del_range_flag.getValue() == FLAG_DEL_RANGE_ALL) {
          deleteMemberOneRangeAll(
            schedule,
            members,
            is_facility,
            userid,
            acl_delete_other);
        } else {
          deleteMemberOneRangeOneday(
            schedule,
            members,
            is_facility,
            userid,
            acl_delete_other,
            rundata,
            context);
        }
      }
      Database.commit();

      // アラーム
      if (ALReminderService.isEnabled() || ALReminderService.isPastEnabled()) {
        if (del_member_flag.getValue() == FLAG_DEL_MEMBER_ALL) {
          if (del_range_flag.getValue() == FLAG_DEL_RANGE_ALL) {
            // 完全に削除する（元のスケジュール、関連するダミースケジュール）
            for (ALEipUser member : members) {
              ALReminderItem item =
                ALReminderService.getJob(Database.getDomainName(), member
                  .getUserId()
                  .getValueAsString(), ReminderCategory.SCHEDULE, schedule
                  .getScheduleId()
                  .intValue());
              if (item != null) {
                ALReminderService.removeJob(item);
              }
            }
          }
        } else {
          if (del_range_flag.getValue() == FLAG_DEL_RANGE_ALL) {
            // 自分の全てのスケジュールを削除する、最後のメンバーの場合は完全に削除する

            SelectQuery<EipTSchedule> query =
              Database.query(EipTSchedule.class);

            // スケジュールID
            Expression exp1 =
              ExpressionFactory.matchDbExp(
                EipTSchedule.SCHEDULE_ID_PK_COLUMN,
                schedule.getScheduleId());
            query.setQualifier(exp1);

            List<EipTSchedule> schedules = query.fetchList();

            // 指定したSchedule IDのレコードが見つからない場合
            if (schedules == null || schedules.size() == 0) {
              // 最後のメンバーの場合は完全に削除する
              for (ALEipUser member : members) {
                ALReminderItem item =
                  ALReminderService.getJob(Database.getDomainName(), member
                    .getUserId()
                    .getValueAsString(), ReminderCategory.SCHEDULE, schedule
                    .getScheduleId()
                    .intValue());
                if (item != null) {
                  ALReminderService.removeJob(item);
                }
              }
            }
            if (schedules != null && schedules.size() > 0) {
              // 削除対象ユーザーのみ削除
              if (!is_facility) {
                ALReminderItem item =
                  ALReminderService.getJob(Database.getDomainName(), String
                    .valueOf(userid), ReminderCategory.SCHEDULE, schedule
                    .getScheduleId()
                    .intValue());
                if (item != null) {
                  ALReminderService.removeJob(item);
                }
              }
            }
          }
        }
      }

      if (del_member_flag.getValue() == FLAG_DEL_MEMBER_ALL
        && del_range_flag.getValue() == FLAG_DEL_RANGE_ALL) {
        TimelineUtils.deleteTimelineActivity(
          rundata,
          context,
          "Schedule",
          schedule.getScheduleId().toString());
      } else if (del_member_flag.getValue() == FLAG_DEL_MEMBER_ONE) {
        SelectQuery<EipTScheduleMap> query2 =
          Database.query(EipTScheduleMap.class);
        Expression exp11 =
          ExpressionFactory.matchExp(
            EipTScheduleMap.SCHEDULE_ID_PROPERTY,
            Integer.valueOf(rundata.getParameters().getString("entityid")));
        query2.andQualifier(exp11);
        List<EipTScheduleMap> scheduleMap = query2.fetchList();
        if (scheduleMap.size() == 0) {
          TimelineUtils.deleteTimelineActivity(
            rundata,
            context,
            "Schedule",
            schedule.getScheduleId().toString());
        }
      }

      // イベントログに保存
      ALEventlogFactoryService.getInstance().getEventlogHandler().log(
        schedule.getScheduleId(),
        ALEventlogConstants.PORTLET_TYPE_SCHEDULE,
        schedule.getName());

      // 「あなた宛のお知らせ」に表示させる。
      String loginName = loginUser.getName().getValue();
      List<String> recipients = new ArrayList<String>();
      for (ALEipUser user : memberList) {
        if (loginUser.getUserId().getValue() != user.getUserId().getValue()) {
          recipients.add(user.getName().getValue());
        }
      }
      int ownerid = ALEipUtils.getUserId(rundata);
      ScheduleUtils.createShareScheduleActivity(
        schedule,
        loginName,
        recipients,
        "delete",
        ownerid);
      // アクティビティが公開スケジュールで参加メンバーがいる場合、「更新情報」に表示させる。
      if ("O".equals(schedule.getPublicFlag())) {
        if (recipients != null && recipients.size() > 0) {
          ScheduleUtils.createNewScheduleActivity(
            schedule,
            loginName,
            "delete",
            ownerid);
        }
      }

      if (ScheduleUtils.MAIL_FOR_ALL.equals(schedule.getMailFlag())
        || ScheduleUtils.MAIL_FOR_INSERT.equals(schedule.getMailFlag())) {
        try {
          // メール送信
          int msgType =
            ALMailUtils.getSendDestType(ALMailUtils.KEY_MSGTYPE_SCHEDULE);
          if (msgType > 0) {
            // パソコンへメールを送信
            List<ALEipUserAddr> destMemberList =
              ALMailUtils.getALEipUserAddrs(memberList, ALEipUtils
                .getUserId(rundata), false);
            String subject = "[" + ALOrgUtilsService.getAlias() + "]スケジュール";
            String orgId = Database.getDomainName();

            List<ALAdminMailMessage> messageList =
              new ArrayList<ALAdminMailMessage>();
            for (ALEipUserAddr destMember : destMemberList) {
              ALAdminMailMessage message = new ALAdminMailMessage(destMember);
              message.setPcSubject(subject);
              message.setCellularSubject(subject);
              message.setPcBody(ScheduleUtils.createMsg(
                rundata,
                schedule,
                memberList,
                null,
                "delete"));
              message.setCellularBody(ScheduleUtils.createMsg(
                rundata,
                schedule,
                memberList,
                destMember.getUserId(),
                "delete"));
              messageList.add(message);
            }

            ALMailService.sendAdminMailAsync(new ALAdminMailContext(
              orgId,
              ALEipUtils.getUserId(rundata),
              messageList,
              ALMailUtils.getSendDestType(ALMailUtils.KEY_MSGTYPE_SCHEDULE)));
            // msgList.addAll(errors);
          }
        } catch (Exception e) {
          throw new MailSendException();
        }
      }
    } catch (RuntimeException e) {
      // RuntimeException
      Database.rollback();
      logger.error("[ScheduleFormData]", e);
      throw new ALDBErrorException();
    } catch (MailSendException e) {
      msgList.add(ALLocalizationUtils.getl10n("SCHEDULE_DONOT_SEND_MAIL"));
      logger.error("schedule", e);
      return false;
    } catch (Exception e) {
      Database.rollback();
      logger.error("[ScheduleFormData]", e);
      throw new ALDBErrorException();
    }
    return true;
  }

  /**
   *
   * @param action
   * @param rundata
   * @param context
   * @return
   */
  @Override
  public boolean doViewForm(ALAction action, RunData rundata, Context context) {
    // for schedule copy
    try {
      init(action, rundata, context);
      boolean isedit =
        (rundata.getParameters().containsKey(ALEipConstants.ENTITY_ID) && !"new"
          .equals(rundata.getParameters().getString(ALEipConstants.ENTITY_ID)));

      if (is_copy) {
        isedit = false;
      }

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
      setMode(action.getMode());

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
   *
   * @param action
   * @param rundata
   * @param context
   * @return
   */
  @Override
  public boolean doInsert(ALAction action, RunData rundata, Context context) {
    boolean res = super.doInsert(action, rundata, context);
    // post(action, rundata, context);
    return res;
  }

  /**
   *
   * @param action
   * @param rundata
   * @param context
   * @return
   */
  @Override
  public boolean doUpdate(ALAction action, RunData rundata, Context context) {
    boolean res = super.doUpdate(action, rundata, context);
    // post(action, rundata, context);
    return res;
  }

  private boolean facilityCheckAclPermission(RunData rundata, int aclType) {
    ALAccessControlFactoryService aclservice =
      (ALAccessControlFactoryService) ((TurbineServices) TurbineServices
        .getInstance()).getService(ALAccessControlFactoryService.SERVICE_NAME);
    ALAccessControlHandler aclhandler = aclservice.getAccessControlHandler();

    hasAuthority =
      aclhandler.hasAuthority(
        ALEipUtils.getUserId(rundata),
        ALAccessControlConstants.POERTLET_FEATURE_SCHEDULE_FACILITY,
        aclType);
    return hasAuthority;
  }

  @SuppressWarnings("unused")
  private void post(ALAction action, RunData rundata, Context context) {
    selectData = new ScheduleOnedayGroupSelectData();
    selectData.initField();

    if (tmpView != null && !tmpView.equals("")) {
      selectData.setTmpViewDate(tmpView);
    } else {
      start_date.setNotNull(true);
      if (start_date.validate(new ArrayList<String>())) {
        selectData.setTmpViewDate(start_date.toString());
      } else if (tmpStart == null || tmpStart.equals("")) {
        ALDateTimeField now = new ALDateTimeField("yyyy-MM-dd-HH-mm");
        now.setValue(new Date());
        selectData.setTmpViewDate(now.toString());
      } else {
        selectData.setTmpViewDate(tmpStart);
      }
    }

    selectData.doSelectList(action, rundata, context);

    // 期限の調節：終了日を開始日より後の日時にする．
    // try {
    // if (limit_end_date.getValue().getDate().before(
    // limit_start_date.getValue().getDate())) {
    // limit_end_date.setValue(limit_start_date.getValue().getDate());
    // }
    // } catch (Exception ex) {
    // }
  }

  /**
   * 指定した曜日が，選択範囲に入っているかを検証する．
   *
   * @param selectedWeek
   *          指定曜日
   * @param startWeek
   *          期間開始曜日
   * @param endWeek
   *          期間終了曜日
   * @return 選択範囲に入っている場合，true．
   */
  @SuppressWarnings("unused")
  private boolean includeWeek(int selectedWeek, int startWeek, int endWeek) {
    if (startWeek <= endWeek) {
      if (startWeek <= selectedWeek && selectedWeek <= endWeek) {
        return true;
      } else {
        return false;
      }
    } else {
      if (selectedWeek < startWeek && endWeek < selectedWeek) {
        return false;
      } else {
        return true;
      }
    }
  }

  /**
   * 指定したスケジュールを削除する．
   *
   * @param schedule
   */
  private void deleteSchedule(EipTSchedule schedule) {
    Integer scheduleId = schedule.getScheduleId();
    Database.delete(schedule);
    // ダミースケジュールの取得
    SelectQuery<EipTSchedule> query = Database.query(EipTSchedule.class);
    Expression exp1 =
      ExpressionFactory.matchExp(EipTSchedule.PARENT_ID_PROPERTY, scheduleId);
    query.setQualifier(exp1);
    Expression exp2 =
      ExpressionFactory.matchExp(EipTSchedule.EIP_TSCHEDULE_MAPS_PROPERTY
        + "."
        + EipTScheduleMap.STATUS_PROPERTY, "D");
    query.andQualifier(exp2);
    List<EipTSchedule> dellist = query.fetchList();
    // ダミースケジュールの削除
    Database.deleteAll(dellist);

    // 2007.3.28 ToDo連携

    // 添付ファイルの削除
    SelectQuery<EipTScheduleFile> dbquery =
      Database.query(EipTScheduleFile.class);
    dbquery.andQualifier(ExpressionFactory.matchDbExp(
      EipTScheduleFile.EIP_TSCHEDULE_PROPERTY,
      schedule.getScheduleId()));
    List<EipTScheduleFile> existsFiles = dbquery.fetchList();

    if (existsFiles.size() > 0) {
      try {
        ALDeleteFileUtil.deleteFiles(
          ScheduleUtils.FOLDER_FILEDIR_SCHEDULE,
          ScheduleUtils.CATEGORY_KEY,
          existsFiles);
      } catch (ALFileNotRemovedException e) {
        Database.rollback();
        logger.error("schedule", e);
      }
    }

  }

  /**
   * 第一引数のリストに，第二引数で指定したユーザ ID が含まれているかを検証する．
   *
   * @param memberIdList
   * @param memberId
   * @return
   */
  private boolean isContains(List<Integer> memberIdList, int userId) {
    int size = memberIdList.size();
    Integer tmpInt = null;
    for (int i = 0; i < size; i++) {
      tmpInt = memberIdList.get(i);
      if (userId == tmpInt.intValue()) {
        return true;
      }
    }
    return false;
  }

  /**
   * 開始日時を取得します。
   *
   * @return
   */
  public ALDateTimeField getStartDate() {
    return start_date;
  }

  /**
   * 開始日時を取得します。
   *
   * @return
   */
  public ALDateTimeField getStartDateSub() {
    ALDateTimeField tmpDate = new ALDateTimeField("yyyy/MM/dd");
    tmpDate.setValue(start_date.getValue());
    return tmpDate;
  }

  /**
   * 開始日時を取得します。
   *
   * @return
   */
  public ALDateTimeField getStartDateTime() {
    ALDateTimeField tmpDate = new ALDateTimeField("HH:mm");
    tmpDate.setValue(start_date.getValue());
    return tmpDate;
  }

  /**
   * 終了日時を取得します。
   *
   * @return
   */
  public ALDateTimeField getEndDate() {
    return end_date;
  }

  /**
   * 終了日時を取得します。
   *
   * @return
   */
  public ALDateTimeField getEndDateSub() {
    ALDateTimeField tmpDate = new ALDateTimeField("yyyy/MM/dd");
    tmpDate.setValue(end_date.getValue());
    return tmpDate;
  }

  /**
   * 終了日時を取得します。
   *
   * @return
   */
  public ALDateTimeField getEndDateTime() {
    ALDateTimeField tmpDate = new ALDateTimeField("HH:mm");
    tmpDate.setValue(end_date.getValue());
    return tmpDate;
  }

  /**
   * グループメンバーを取得します。
   *
   * @return
   */
  public List<ALEipUser> getMemberList() {
    return memberList;
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
   * タイトルを取得します。
   *
   * @return
   */
  public ALStringField getName() {
    return name;
  }

  /**
   * 内容を取得します。
   *
   * @return
   */
  public ALStringField getNote() {
    return note;
  }

  /**
   * 場所を取得します。
   *
   * @return
   */
  public ALStringField getPlace() {
    return place;
  }

  /**
   * 終了日時を取得します。
   *
   * @return
   */
  public int getCurrentYear() {
    return currentYear;
  }

  /**
   *
   * @return
   */
  public boolean isMember() {
    return (is_member || memberList.size() > 1);
  }

  /**
   *
   * @return
   */
  public boolean isOwner() {
    return is_owner;
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
   * 公開/非公開フラグを取得します。
   *
   * @return
   */
  public ALStringField getPublicFlag() {
    return public_flag;
  }

  /**
   * メールフラグを取得
   */
  public String getMailFlag() {
    return mail_flag;
  }

  /**
   * 繰り返すかどうか。
   *
   * @return
   */
  public boolean isRepeat() {
    return is_repeat;
  }

  /**
   * 期間スケジュールかどうか。
   *
   * @return
   */
  public boolean isSpan() {
    return is_span;
  }

  /**
   * コピーを作るかどうか。
   *
   * @return
   */
  public boolean isCopy() {
    return is_copy;
  }

  /**
   * 期限を取得します。
   *
   * @return
   */
  public ALDateField getLimitStartDate() {
    return limit_start_date;
  }

  /**
   * 期限を取得します。
   *
   * @return
   */
  public ALDateTimeField getLimitStartDateSub() {
    ALDateTimeField tmpDate = new ALDateTimeField("yyyy/MM/dd");
    try {
      tmpDate.setValue(limit_start_date.getValue().getDate());
    } catch (Exception e) {
      logger.error("[ScheduleFormData]", e);

    }
    return tmpDate;
  }

  /**
   * 期限を取得します。
   *
   * @return
   */
  public ALDateField getLimitEndDate() {
    return limit_end_date;
  }

  /**
   * 期限を取得します。
   *
   * @return
   */
  public ALDateTimeField getLimitEndDateSub() {
    ALDateTimeField tmpDate = new ALDateTimeField("yyyy/MM/dd");
    try {
      tmpDate.setValue(limit_end_date.getValue().getDate());
    } catch (Exception e) {
      logger.error("[ScheduleFormData]", e);

    }
    return tmpDate;
  }

  /**
   * 期限フラグを取得します。
   *
   * @return
   */
  public ALStringField getLimitFlag() {
    return limit_flag;
  }

  /**
   * 終日フラグを取得します。
   *
   * @return
   */
  public ALStringField getAllDayFlag() {
    return all_day_flag;
  }

  /**
   * 毎月繰り返す日を取得します。
   *
   * @return
   */
  public ALNumberField getMonthDay() {
    return month_day;
  }

  /**
   * 毎年繰り返す月を取得します。
   *
   * @return
   */
  public ALNumberField getYearMonth() {
    return year_month;
  }

  /**
   * 毎年繰り返す日を取得します。
   *
   * @return
   */
  public ALNumberField getYearDay() {
    return year_day;
  }

  /**
   * 繰り返しタイプを取得します。
   *
   * @return
   */
  public ALStringField getRepeatType() {
    return repeat_type;
  }

  /**
   * 繰り返し曜日を取得します。
   *
   * @return
   */
  public ALStringField getWeek0() {
    return week_0;
  }

  /**
   * 繰り返し曜日を取得します。
   *
   * @return
   */
  public ALStringField getWeek1() {
    return week_1;
  }

  /**
   * 繰り返し曜日を取得します。
   *
   * @return
   */
  public ALStringField getWeek2() {
    return week_2;
  }

  /**
   * 繰り返し曜日を取得します。
   *
   * @return
   */
  public ALStringField getWeek3() {
    return week_3;
  }

  /**
   * 繰り返し曜日を取得します。
   *
   * @return
   */
  public ALStringField getWeek4() {
    return week_4;
  }

  /**
   * 繰り返し曜日を取得します。
   *
   * @return
   */
  public ALStringField getWeek5() {
    return week_5;
  }

  /**
   * 繰り返し曜日を取得します。
   *
   * @return
   */
  public ALStringField getWeek6() {
    return week_6;
  }

  /**
   * 繰り返し週を取得します。
   *
   * @return
   */
  public ALStringField getRepeatWeek() {
    return repeat_week;
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
   * 繰り返しスケジュールの編集フラグ
   *
   * @return
   */
  public ALNumberField getEditRepeatFlag() {
    return edit_repeat_flag;
  }

  /**
   * 共有メンバーによる編集／削除権限フラグ
   *
   * @return
   */
  public ALStringField getEditFlag() {
    return edit_flag;
  }

  public ALStringField getChangeTmpreserveFlag() {
    return change_tmpreserve_flag;
  }

  public ALDateTimeField getViewDate() {
    return view_date;
  }

  public String getOnlyViewDateText() {
    return ALLocalizationUtils.getl10nFormat(
      "SCHEDULE_ONLY_DATE_FORMAT",
      view_date.getYear(),
      view_date.getMonth(),
      view_date.getDay());
  }

  public int getInt(long num) {
    return (int) num;
  }

  /**
   *
   * @return
   */
  public boolean isFacility() {
    return (is_facility || facilityList.size() > 0);// 設備を予約するまたは設備リストは空でない
  }

  public List<Object> getFacilityList() {
    return facilityList;
  }

  public List<FacilityResultData> getFacilityAllList() {
    return facilityAllList;
  }

  public List<FileuploadLiteBean> getAttachmentFileNameList() {
    return fileuploadList;
  }

  public String getFolderName() {
    return folderName;
  }

  /**
   * 共有カテゴリ ID
   *
   * @return
   */
  public ALNumberField getCommonCategoryId() {
    return common_category_id;
  }

  public int getMsgType() {
    return msg_type;
  }

  /**
   * 開始日時と終了日時が同じかどうか返します。
   *
   * @return
   */
  public boolean getIsSameDate() {
    return is_same_date;
  }

  /**
   * 開始時刻と終了時刻が同じかどうか返します。
   *
   * @return
   */
  public boolean getIsSameTime() {
    return start_date.getTime().equals(end_date.getTime());
  }

  /**
   * アクセス権限チェック用メソッド。<br />
   * アクセス権限の機能名を返します。
   *
   * @return
   */
  @Override
  public String getAclPortletFeature() {
    return aclPortletFeature;
  }

  public boolean isDisplayManHour() {
    return !Registry.getEntry(Registry.PORTLET, "ManHour").isHidden();
  }

  public boolean isReminderEnabled() {
    return ALReminderService.isEnabled();
  }
}
