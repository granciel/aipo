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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import net.sf.json.JSONObject;

import org.apache.jetspeed.services.logging.JetspeedLogFactoryService;
import org.apache.jetspeed.services.logging.JetspeedLogger;
import org.apache.turbine.services.TurbineServices;
import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;

import com.aimluck.commons.field.ALDateTimeField;
import com.aimluck.eip.cayenne.om.portlet.EipTSchedule;
import com.aimluck.eip.cayenne.om.portlet.EipTScheduleMap;
import com.aimluck.eip.common.ALDBErrorException;
import com.aimluck.eip.common.ALEipConstants;
import com.aimluck.eip.common.ALEipUser;
import com.aimluck.eip.common.ALPageNotFoundException;
import com.aimluck.eip.common.ALPermissionException;
import com.aimluck.eip.mail.ALAdminMailContext;
import com.aimluck.eip.mail.ALAdminMailMessage;
import com.aimluck.eip.mail.ALMailService;
import com.aimluck.eip.mail.util.ALEipUserAddr;
import com.aimluck.eip.mail.util.ALMailUtils;
import com.aimluck.eip.modules.actions.common.ALAction;
import com.aimluck.eip.orm.Database;
import com.aimluck.eip.schedule.beans.ScheduleBean;
import com.aimluck.eip.schedule.util.ScheduleUtils;
import com.aimluck.eip.services.accessctl.ALAccessControlConstants;
import com.aimluck.eip.services.accessctl.ALAccessControlFactoryService;
import com.aimluck.eip.services.accessctl.ALAccessControlHandler;
import com.aimluck.eip.services.eventlog.ALEventlogConstants;
import com.aimluck.eip.services.eventlog.ALEventlogFactoryService;
import com.aimluck.eip.services.orgutils.ALOrgUtilsService;
import com.aimluck.eip.services.quota.ALQuotaService;
import com.aimluck.eip.services.reminder.ALReminderHandler.ReminderCategory;
import com.aimluck.eip.services.reminder.ALReminderService;
import com.aimluck.eip.services.reminder.model.ALReminderItem;
import com.aimluck.eip.util.ALEipUtils;
import com.aimluck.eip.util.ALLocalizationUtils;

public class ScheduleWeeklyJSONFormData {

  private static final JetspeedLogger logger = JetspeedLogFactoryService
    .getLogger(ScheduleWeeklyJSONFormData.class.getName());

  // update

  private ALDateTimeField startDate;

  private ALDateTimeField endDate;

  private ALDateTimeField viewDate;

  private String start_date;

  private String end_date;

  private String view_date;

  private int entityId;

  private String aclPortletFeature;

  private int userId;

  private EipTSchedule schedule;

  private boolean isEdit;

  private String orgId;

  // private boolean is_span; // 期間スケジュールかどうか
  //
  // private boolean isMember; // 自分はメンバーに含まれるかどうか

  private int edit_repeat_flag;

  private List<ALEipUser> memberList;

  private List<EipTScheduleMap> scheduleMaps;

  private int ownerId;

  private ArrayList<String> msgList;

  private boolean isViewList;

  private boolean ignore_duplicate_facility;

  private boolean isDayOffHoliday; // 祝日を休日にするかどうか

  /** 添付ファイル追加へのアクセス権限の有無 */
  private boolean hasAttachmentInsertAuthority;

  /** 添付ファイル削除へのアクセス権限の有無 */
  private boolean hasAttachmentDeleteAuthority;

  public void initField() {
    aclPortletFeature = ALAccessControlConstants.POERTLET_FEATURE_SCHEDULE_SELF;
  }

  public void init(ALAction action, RunData rundata, Context context)
      throws ALPageNotFoundException, ALDBErrorException {
    start_date = null;
    end_date = null;
    view_date = null;
    entityId = 0;
    userId = 0;
    // is_span = false;
    // isMember = false;
    isEdit = false;
    memberList = new ArrayList<ALEipUser>();
    scheduleMaps = null;
    ownerId = 0;
    edit_repeat_flag = 0;
    startDate = new ALDateTimeField("yyyy-MM-dd-HH-mm");
    endDate = new ALDateTimeField("yyyy-MM-dd-HH-mm");
    viewDate = new ALDateTimeField("yyyy-MM-dd");
    msgList = new ArrayList<String>();
    isViewList = false;
    orgId = Database.getDomainName();

    aclPortletFeature = ALAccessControlConstants.POERTLET_FEATURE_SCHEDULE_SELF;

    ignore_duplicate_facility = false;

  }

  public String doViewList(ALAction action, RunData rundata, Context context,
      List<String> msgList) {
    try {

      doCheckAclPermission(
        rundata,
        context,
        ALAccessControlConstants.VALUE_ACL_LIST);
      doCheckAttachmentAclPermission(
        rundata,
        context,
        ALAccessControlConstants.VALUE_ACL_EXPORT);

      AjaxScheduleResultData rd;
      ScheduleBean bean;
      List<List<ScheduleBean>> termScheduleList =
        new ArrayList<List<ScheduleBean>>();
      List<AjaxScheduleResultData> _scheduleList =
        new ArrayList<AjaxScheduleResultData>();
      List<ScheduleBean> scheduleList = new ArrayList<ScheduleBean>();
      List<String> holidayList = new ArrayList<String>();
      List<String> dateList = new ArrayList<String>();
      List<String> dayOfWeekList = new ArrayList<String>();

      AjaxScheduleWeeklyGroupSelectData listData =
        new AjaxScheduleWeeklyGroupSelectData();
      // ScheduleWeeklySelectData listData = new
      // ScheduleWeeklySelectData();
      listData.initField();
      listData.doSelectList(null, rundata, context);
      JSONObject json = new JSONObject();
      json.put("hasAcl", hasAcl(rundata));
      json.put("today", listData.getToday().toString());
      json.put("prevDate", listData.getPrevDate().toString());
      json.put("nextDate", listData.getNextDate().toString());
      json.put("prevWeek", listData.getPrevWeek().toString());
      json.put("nextWeek", listData.getNextWeek().toString());
      json.put("prevMonth", listData.getPrevMonth().toString());
      json.put("nextMonth", listData.getNextMonth().toString());

      List<AjaxScheduleDayContainer> dayList =
        listData.getContainer().getDayList();
      List<AjaxTermScheduleWeekContainer> termList =
        listData.getWeekTermContainerList();
      List<AjaxTermScheduleDayContainer> termDayList;

      int dayListSize = dayList.size();

      Date containerDate = null;

      for (AjaxTermScheduleWeekContainer termContainer : termList) {
        if (!termContainer.hasVisibleTerm()) {
          continue;
        }
        termDayList = termContainer.getDayList();
        List<ScheduleBean> _termScheduleList = new ArrayList<ScheduleBean>(); // termSchedule
        int termDayListSize = termDayList.size();
        for (int k = 0; k < termDayListSize; k++) {
          AjaxTermScheduleDayContainer termDayContainer = termDayList.get(k);
          if (k == 0) {
            containerDate = termDayContainer.getDate().getValue();
          }
          rd = termDayContainer.getTermResultData();
          if (rd != null && containerDate != null) {
            int stime =
              (int) (rd.getStartDate().getValue().getTime() / 86400000);
            int etime = (int) (rd.getEndDate().getValue().getTime() / 86400000);
            int ctime = (int) (containerDate.getTime() / 86400000);
            int col = etime - stime + 1;
            int rindex = stime - ctime;
            bean = new ScheduleBean();
            bean.initField();
            bean.setResultData(rd);
            if (!rd.isPublic() && !rd.isMember()) {
              bean.setName(ALLocalizationUtils
                .getl10n("SCHEDULE_CLOSE_PUBLIC_WORD"));
            }
            bean.setColspanReal(col);
            bean.setIndex(k);
            bean.setIndexReal(rindex);
            if (!rd.isHidden() || rd.isMember()) {
              _termScheduleList.add(bean);
            }
          }
        }
        if (!(rundata.getParameters().getString("m_empty").equals("empty"))) {
          termScheduleList.add(_termScheduleList);
        }
      }

      for (int i = 0; i < dayListSize; i++) {
        AjaxScheduleDayContainer container = dayList.get(i);
        if (i == 0) {
          containerDate = container.getDate().getValue();
        }
        dateList.add(container.getDate().toString());
        dayOfWeekList.add(container.getDate().getDayOfWeek());
        _scheduleList = container.getScheduleList();

        if (container.isHoliday()) {
          holidayList.add(container.getHoliday().getName().getValue());
        } else if (container.isUserHoliday()) {
          holidayList.add("set");
        } else {
          holidayList.add("");
        }

        if (i == 0) {
          json.put("startDate", container.getDate().toString());
        } else if (i == dayListSize - 1) {
          json.put("endDate", container.getDate().toString());
        }
        int scheSize = _scheduleList.size();

        for (int j = 0; j < scheSize; j++) {
          rd = _scheduleList.get(j);
          if (rd.isDummy()) {
            continue;
          }
          bean = new ScheduleBean();
          bean.initField();
          bean.setResultData(rd);
          if (!rd.isPublic() && !rd.isMember()) {
            bean.setName(ALLocalizationUtils
              .getl10n("SCHEDULE_CLOSE_PUBLIC_WORD"));
          }
          bean.setIndex(i);
          if (rundata.getParameters().getString("m_empty") != null) {
            if (!(rundata.getParameters().getString("m_empty").equals("empty"))
              && (!rd.isHidden() || rd.isMember())) {
              scheduleList.add(bean);
            }
          }
        }
      }
      json.put("termSchedule", termScheduleList);
      json.put("schedule", scheduleList);
      json.put("holiday", holidayList);
      json.put("date", dateList);
      json.put("dayOfWeek", dayOfWeekList);
      isDayOffHoliday = ScheduleUtils.isDayOffHoliday();
      json.put("dayoff", isDayOffHoliday);
      if ((msgList != null) && (msgList.size() > 0)) {
        json.put("errList", msgList);
      }

      return json.toString();
    } catch (ALPermissionException e) {
      ALEipUtils.redirectPermissionError(rundata);
      return null;
    } catch (Exception e) {
      logger.error("[ScheduleWeeklyJSONFormData]", e);
      return null;
    }
  }

  protected boolean isOverQuota() {
    return ALQuotaService.isOverQuota(Database.getDomainName());
  }

  public boolean doUpdate(ALAction action, RunData rundata, Context context) {
    if (!ScheduleUtils.hasRelation(rundata)) {
      aclPortletFeature =
        ALAccessControlConstants.POERTLET_FEATURE_SCHEDULE_OTHER;
    }
    try {
      if (!doCheckSecurity(rundata, context)) {
        return false;
      }
      doCheckAclPermission(
        rundata,
        context,
        ALAccessControlConstants.VALUE_ACL_UPDATE);

      doCheckAttachmentInsertAclPermission(rundata, context);
      doCheckAttachmentDeleteAclPermission(rundata, context);
      boolean res = false;
      if (isOverQuota()) {
        msgList.add(ALLocalizationUtils
          .getl10n("COMMON_FULL_DISK_DELETE_DETA_OR_CHANGE_PLAN"));
      } else {
        res =
          (setFormData(rundata, context, msgList) && validate(msgList) && updateFormData(
            rundata,
            context,
            msgList));
      }
      return res;

    } catch (ALPermissionException e) {
      msgList.add("PermissionError");
      msgList.add(ALAccessControlConstants.DEF_PERMISSION_ERROR_STR);
      return false;
    } catch (Exception e) {
      Database.rollback();
      logger.error("schedule", e);
      return false;
    }
  }

  public boolean doInsert(ALAction action, RunData rundata, Context context) {
    if (!ScheduleUtils.hasRelation(rundata)) {
      aclPortletFeature =
        ALAccessControlConstants.POERTLET_FEATURE_SCHEDULE_OTHER;
    }
    try {
      if (!doCheckSecurity(rundata, context)) {
        return false;
      }
      doCheckAclPermission(
        rundata,
        context,
        ALAccessControlConstants.VALUE_ACL_INSERT);

      doCheckAttachmentInsertAclPermission(rundata, context);
      doCheckAttachmentDeleteAclPermission(rundata, context);
      boolean res = false;
      if (isOverQuota()) {
        msgList.add(ALLocalizationUtils
          .getl10n("COMMON_FULL_DISK_DELETE_DETA_OR_CHANGE_PLAN"));
      } else {
        res =
          (setFormData(rundata, context, msgList) && validate(msgList) && insertFormData(
            rundata,
            context,
            msgList));
      }
      return res;

    } catch (ALPermissionException e) {
      msgList.add("PermissionError");
      msgList.add(ALAccessControlConstants.DEF_PERMISSION_ERROR_STR);
      return false;
    } catch (Exception e) {
      Database.rollback();
      logger.error("schedule", e);
      return false;
    }
  }

  public void loadParameters(RunData rundata, Context context,
      List<String> msgList) {
    ALDateTimeField dummy = new ALDateTimeField("yyyy-MM-dd-HH-mm");
    dummy.setNotNull(true);
    if (ALEipUtils.isMatch(rundata, context)) {
      if (rundata.getParameters().containsKey("start_date")
        && rundata.getParameters().containsKey("end_date")) {
        start_date = rundata.getParameters().getString("start_date");
        dummy.setValue(start_date);
        if (!dummy.validate(new ArrayList<String>())) {
          ALEipUtils.removeTemp(rundata, context, "start_date");
          ALEipUtils.removeTemp(rundata, context, "end_date");
          msgList.add("starDate_irregular");
          return;
        }
        end_date = rundata.getParameters().getString("end_date");
        dummy.setValue(end_date);
        if (!dummy.validate(new ArrayList<String>())) {
          ALEipUtils.removeTemp(rundata, context, "end_date");
          ALEipUtils.removeTemp(rundata, context, "end_date");
          msgList.add("endDate_irregular");
          return;
        }
        if (rundata.getParameters().containsKey("view_date")) {
          view_date = rundata.getParameters().getString("view_date");
          dummy.setValue(view_date);
          if (!dummy.validate(new ArrayList<String>())) {
            ALEipUtils.removeTemp(rundata, context, "view_date");
            ALEipUtils.removeTemp(rundata, context, "view_date");
            msgList.add("viewDate_irregular");
            return;
          }
        }
        if (rundata.getParameters().containsKey(ALEipConstants.ENTITY_ID)) {
          entityId = rundata.getParameters().getInt(ALEipConstants.ENTITY_ID);
        } else {
          msgList.add("entityId_missing");
          return;
        }
        if (rundata.getParameters().containsKey("ign_dup_f")) {
          ignore_duplicate_facility =
            rundata.getParameters().getBoolean("ign_dup_f", false);
        }
      } else {
        // null
        isViewList = true;
      }
    } else {
      msgList.add("not own portlet");
      return;
    }
  }

  public boolean setFormData(RunData rundata, Context context,
      List<String> msgList) throws ALPageNotFoundException, ALDBErrorException {
    try {
      userId = ALEipUtils.getUserId(rundata);
      schedule =
        ScheduleUtils.getEipTSchedule(rundata, entityId, false, userId);
      isEdit = "T".equals(schedule.getEditFlag());
      // if (rundata.getParameters().containsKey("is_span")) {
      // is_span = rundata.getParameters().getBoolean("is_span");
      // }
      if (rundata.getParameters().containsKey("edit_repeat_flag")) {
        edit_repeat_flag = rundata.getParameters().getInt("edit_repeat_flag");
      } else {
        // msgList.add("edit_repeat_flag none");
      }
      scheduleMaps = ScheduleUtils.getEipTScheduleMaps(schedule);

      int listSize = scheduleMaps.size();
      for (int i = 0; i < listSize; i++) {
        EipTScheduleMap map = scheduleMaps.get(i);
        if (ScheduleUtils.SCHEDULEMAP_TYPE_USER.equals(map.getType())) {
          int targetUserId = map.getUserId().intValue();
          // if (userId == targetUserId) {
          // isMember = true;
          // }
          memberList.add(ALEipUtils.getALEipUser(targetUserId));
        }
        // else
        // if(ScheduleUtils.SCHEDULEMAP_TYPE_FACILITY.equals(map.getType())){
        // }
      }
      ownerId = schedule.getOwnerId().intValue();
      startDate.setValue(start_date);
      endDate.setValue(end_date);
      viewDate.setValue(view_date);
    } catch (Exception e) {
      logger.error("schedule", e);
      return false;
    }
    return true;
  }

  public boolean validate(List<String> msgList) throws ALDBErrorException,
      ALPageNotFoundException {
    return !(msgList.size() > 0);
  }

  public boolean updateFormData(RunData rundata, Context context,
      List<String> msgList) throws ALPageNotFoundException, ALDBErrorException {
    boolean res;

    boolean authorityForOtherSchedule =
      ScheduleUtils.hasAuthorityForOtherSchedule(
        rundata,
        ALAccessControlConstants.VALUE_ACL_UPDATE);
    if (isEdit
      || userId == ownerId
      || aclPortletFeature == ALAccessControlConstants.POERTLET_FEATURE_SCHEDULE_OTHER
      || authorityForOtherSchedule) {

      if (edit_repeat_flag == 0) {
        /** 繰り返しでないスケジュールをコピーしようとした場合 */
        /** スケジュールに変更が加わっていない場合は、更新処理をスキップする */
        if (schedule.getStartDate().equals(startDate.getValue())
          && schedule.getEndDate().equals(endDate.getValue())) {
          return true;
        }

        schedule.setStartDate(startDate.getValue());
        schedule.setEndDate(endDate.getValue());
        Date now = new Date();
        schedule.setUpdateDate(now);
        schedule.setUpdateUserId(userId);

        /* 設備重複判定 */
        {
          int listSize = scheduleMaps.size();
          List<Integer> facilityIdList = new ArrayList<Integer>();

          for (int i = 0; i < listSize; i++) {
            EipTScheduleMap map = scheduleMaps.get(i);
            if (ScheduleUtils.SCHEDULEMAP_TYPE_FACILITY.equals(map.getType())) {
              facilityIdList.add(map.getUserId());
            }
          }
          if (!ignore_duplicate_facility) {
            if (facilityIdList.size() > 0) {
              if (ScheduleUtils.isDuplicateFacilitySchedule(
                schedule,
                facilityIdList,
                null,
                null)) {
                msgList.add("duplicate_facility");
                Database.rollback();
                return false;
              }
            }
          }
        }

        Database.commit();
        res = true;
        // アラーム
        updateReminder(schedule, schedule.getScheduleId().intValue());

        // イベントログに保存
        sendEventLog(rundata, context);
        /* メンバー全員に新着ポートレット登録 */
        sendWhatsNew(schedule, false);

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

            }
          } catch (Exception ex) {
            msgList
              .add(ALLocalizationUtils.getl10n("SCHEDULE_DONOT_SEND_MAIL"));
            logger.error("schedule", ex);
            return false;
          }
        }
      } else {
        /** 繰り返しスケジュールを変更しようとした場合 */
        /**
         * 以下の場合は変更が加わっていないとみなし、更新をスキップする。
         * 保存されている開始時刻と終了時刻がendDateとstartDateと一致。 viewDateの日付がstartDateの物と一致。
         */
        Calendar saved_startdate = Calendar.getInstance();
        saved_startdate.setTime(schedule.getStartDate());
        Calendar saved_enddate = Calendar.getInstance();
        saved_enddate.setTime(schedule.getEndDate());
        if (Integer.valueOf(startDate.getHour()) == saved_startdate
          .get(Calendar.HOUR_OF_DAY)
          && Integer.valueOf(startDate.getMinute()) == saved_startdate
            .get(Calendar.MINUTE)
          && Integer.valueOf(endDate.getHour()) == saved_enddate
            .get(Calendar.HOUR_OF_DAY)
          && Integer.valueOf(endDate.getMinute()) == saved_enddate
            .get(Calendar.MINUTE)
          && viewDate.getMonth().equals(startDate.getMonth())
          && viewDate.getDay().equals(startDate.getDay())
          && viewDate.getYear().equals(startDate.getYear())) {
          return true;
        }

        // if(schedule.getStartDate())

        EipTSchedule newSchedule = Database.create(EipTSchedule.class);
        // 繰り返しの親スケジュール ID
        newSchedule.setParentId(schedule.getScheduleId());
        // タイトル
        newSchedule.setName(schedule.getName());
        // 場所
        newSchedule.setPlace(schedule.getPlace());
        // 内容
        newSchedule.setNote(schedule.getNote());
        // 公開フラグ
        newSchedule.setPublicFlag(schedule.getPublicFlag());
        // 共有メンバーによる編集／削除フラグ
        newSchedule.setEditFlag(schedule.getEditFlag());

        // newSchedule.setEditFlag("F");
        // オーナーID
        // newSchedule.setOwnerId(Integer.valueOf(userId));
        newSchedule.setOwnerId(Integer.valueOf(ownerId));
        // 作成日
        Date now = new Date();
        newSchedule.setCreateDate(now);
        // newSchedule.setCreateUserId(Integer.valueOf(userId));
        newSchedule.setCreateUserId(Integer.valueOf(ownerId));
        // 更新日
        newSchedule.setUpdateDate(now);
        newSchedule.setUpdateUserId(Integer.valueOf(userId));
        // 終了日時
        newSchedule.setEndDate(endDate.getValue());
        newSchedule.setRepeatPattern("N");
        newSchedule.setStartDate(startDate.getValue());

        // 2007.3.28 ToDo連携

        int listSize = scheduleMaps.size();
        List<Integer> memberIdList = new ArrayList<Integer>();
        List<Integer> facilityIdList = new ArrayList<Integer>();
        // List newMaps = new ArrayList();

        for (int i = 0; i < listSize; i++) {
          EipTScheduleMap newMap = Database.create(EipTScheduleMap.class);
          EipTScheduleMap map = scheduleMaps.get(i);
          newMap.setEipTSchedule(newSchedule);
          newMap.setUserId(map.getUserId());

          if (map.getUserId() == ownerId) {
            // if (map.getUserId() == userId) {
            newMap.setStatus("O");
          } else {
            EipTScheduleMap tmpMap =
              getScheduleMap(
                scheduleMaps,
                map.getUserId().intValue(),
                ScheduleUtils.SCHEDULEMAP_TYPE_USER);
            if (tmpMap != null) {
              newMap.setStatus(tmpMap.getStatus());
            } else {
              newMap.setStatus("T");
            }
          }
          newMap.setType(map.getType());
          newMap.setCommonCategoryId(map.getCommonCategoryId());
          newMap.setEipTCommonCategory(map.getEipTCommonCategory());

          if (ScheduleUtils.SCHEDULEMAP_TYPE_USER.equals(map.getType())) {
            memberIdList.add(map.getUserId().intValue());
          } else {
            facilityIdList.add(map.getUserId().intValue());
          }
        }

        /* 設備重複判定 */
        if (!ignore_duplicate_facility) {
          if (facilityIdList.size() > 0) {
            if (ScheduleUtils.isDuplicateFacilitySchedule(
              newSchedule,
              facilityIdList,
              schedule.getScheduleId(),
              viewDate.getValue())) {
              msgList.add("duplicate_facility");
              Database.rollback();
              return false;
            }
          }
        }

        if (viewDate != null) {
          ScheduleUtils.insertDummySchedule(schedule, userId, viewDate
            .getValue(), viewDate.getValue(), memberIdList, facilityIdList);
        }

        Database.commit();
        res = true;
        // アラーム
        updateReminder(newSchedule, schedule.getScheduleId().intValue());

        // イベントログに保存
        sendEventLog(rundata, context);
        /* メンバー全員に新着ポートレット登録 */
        sendWhatsNew(newSchedule, false);

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
                messageList.add(message);
              }

              ALMailService.sendAdminMailAsync(new ALAdminMailContext(
                orgId,
                ALEipUtils.getUserId(rundata),
                messageList,
                ALMailUtils.getSendDestType(ALMailUtils.KEY_MSGTYPE_SCHEDULE)));

            }

          } catch (Exception ex) {
            msgList
              .add(ALLocalizationUtils.getl10n("SCHEDULE_DONOT_SEND_MAIL"));
            logger.error("schedule", ex);
            return false;
          }

        }

      }
    } else {
      msgList.add("そのスケジュールは編集することができません");
      res = false;
    }
    return res;
  }

  /**
   *
   * @param schedule2
   * @param oldItemId
   */
  private void updateReminder(EipTSchedule schedule2, int oldItemId) {
    if (ALReminderService.isEnabled() || ALReminderService.isPastEnabled()) {

      String ptn = schedule2.getRepeatPattern();
      int count = 0;
      boolean isRepeat = false;
      boolean isSpan = false;
      boolean isLimit = false;
      if (ptn.charAt(0) == 'D') {
        count = 1;
        isRepeat = true;
        isSpan = false;
      } else if (ptn.charAt(0) == 'W') {
        if (ptn.length() == 9) {
          count = 8;
        } else {
          count = 9;
        }
        isRepeat = true;
        isSpan = false;
      } else if (ptn.charAt(0) == 'M') {
        count = 3;
        isRepeat = true;
        isSpan = false;
      } else if (ptn.charAt(0) == 'Y') {
        count = 5;
        isRepeat = true;
        isSpan = false;
      } else if (ptn.charAt(0) == 'S') {
        isRepeat = false;
        isSpan = true;
      } else {
        isRepeat = false;
        isSpan = false;
      }

      if (isRepeat) {
        if (ptn.charAt(count) == 'N') {
          isLimit = false;
        } else {
          isLimit = true;
        }
      }

      for (ALEipUser user : memberList) {
        ALReminderItem item =
          ALReminderService.getJob(
            orgId,
            user.getUserId().getValueAsString(),
            ReminderCategory.SCHEDULE,
            oldItemId);
        if (item != null) {
          ScheduleUtils.setupReminderJob(
            Database.getDomainName(),
            user.getUserId().toString(),
            schedule2,
            item.getNotifyTiming(),
            item.hasNotifyTypeMail(),
            item.hasNotifyTypeMessage(),
            isRepeat,
            isLimit,
            isSpan);
        }
      }
    }
  }

  /**
   *
   * @param rundata
   * @param context
   * @param msgList
   * @return
   * @throws ALDBErrorException
   */
  protected boolean insertFormData(RunData rundata, Context context,
      List<String> msgList) throws ALDBErrorException {
    boolean res;

    boolean authorityForOtherSchedule =
      ScheduleUtils.hasAuthorityForOtherSchedule(
        rundata,
        ALAccessControlConstants.VALUE_ACL_INSERT);
    if (isEdit
      || userId == ownerId
      || aclPortletFeature == ALAccessControlConstants.POERTLET_FEATURE_SCHEDULE_OTHER
      || authorityForOtherSchedule) {

      // 繰り返しでないスケジュールを変更しようとした場合
      if (edit_repeat_flag == 0) {

        // スケジュールをコピーする
        EipTSchedule newSchedule = Database.create(EipTSchedule.class);

        newSchedule.setStartDate(startDate.getValue());
        newSchedule.setEndDate(endDate.getValue());

        Date now = new Date();

        newSchedule.setCreateDate(now);

        newSchedule.setUpdateDate(now);

        newSchedule.setUpdateUserId(userId);

        newSchedule.setName(schedule.getName());

        newSchedule.setNote(schedule.getNote());

        newSchedule.setPlace(schedule.getPlace());

        newSchedule.setEditFlag(schedule.getEditFlag());

        newSchedule.setPublicFlag(schedule.getPublicFlag());

        newSchedule.setRepeatPattern(schedule.getRepeatPattern());

        newSchedule.setCreateUserId(schedule.getCreateUserId());

        newSchedule.setOwnerId(schedule.getOwnerId());

        newSchedule.setParentId(schedule.getParentId());

        List<EipTScheduleMap> newScheduleMaps =
          new ArrayList<EipTScheduleMap>();

        for (EipTScheduleMap scheduleMap : scheduleMaps) {
          EipTScheduleMap newScheduleMap =
            Database.create(EipTScheduleMap.class);
          newScheduleMap.setEipTSchedule(newSchedule);

          newScheduleMap.setEipTCommonCategory(scheduleMap
            .getEipTCommonCategory());

          newScheduleMap.setStatus(scheduleMap.getStatus());

          newScheduleMap.setType(scheduleMap.getType());

          newScheduleMap.setUserId(scheduleMap.getUserId());

          newScheduleMaps.add(newScheduleMap);
        }

        /* 設備重複判定 */
        List<Integer> facilityIdList = new ArrayList<Integer>();
        for (EipTScheduleMap newScheduleMap : newScheduleMaps) {
          if (ScheduleUtils.SCHEDULEMAP_TYPE_FACILITY.equals(newScheduleMap
            .getType())) {
            facilityIdList.add(newScheduleMap.getUserId());
          }
        }

        if (!ignore_duplicate_facility) {
          if (facilityIdList.size() > 0) {
            if (ScheduleUtils.isDuplicateFacilitySchedule(
              newSchedule,
              facilityIdList,
              null,
              null)) {
              msgList.add("duplicate_facility");
              Database.rollback();
              return false;
            }
          }
        }

        Database.commit();
        res = true;
        updateReminder(newSchedule, schedule.getScheduleId().intValue());
        // イベントログに保存
        sendEventLog(rundata, context);
        /* メンバー全員に新着ポートレット登録 */
        sendWhatsNew(newSchedule, true);

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

          }
        } catch (Exception ex) {
          msgList.add(ALLocalizationUtils.getl10n("SCHEDULE_DONOT_SEND_MAIL"));
          logger.error("schedule", ex);
          return false;
        }

      } else {
        /** 繰り返しスケジュールを変更しようとした場合 */
        /**
         * 以下の場合は変更が加わっていないとみなし、更新をスキップする。
         * 保存されている開始時刻と終了時刻がendDateとstartDateと一致。 viewDateの日付がstartDateの物と一致。
         */
        Calendar saved_startdate = Calendar.getInstance();
        saved_startdate.setTime(schedule.getStartDate());
        Calendar saved_enddate = Calendar.getInstance();
        saved_enddate.setTime(schedule.getEndDate());
        if (Integer.valueOf(startDate.getHour()) == saved_startdate
          .get(Calendar.HOUR_OF_DAY)
          && Integer.valueOf(startDate.getMinute()) == saved_startdate
            .get(Calendar.MINUTE)
          && Integer.valueOf(endDate.getHour()) == saved_enddate
            .get(Calendar.HOUR_OF_DAY)
          && Integer.valueOf(endDate.getMinute()) == saved_enddate
            .get(Calendar.MINUTE)
          && viewDate.getMonth().equals(startDate.getMonth())
          && viewDate.getDay().equals(startDate.getDay())
          && viewDate.getYear().equals(startDate.getYear())) {
          return true;
        }

        // if(schedule.getStartDate())

        // スケジュールをコピーする
        EipTSchedule newSchedule = Database.create(EipTSchedule.class);

        newSchedule.setStartDate(startDate.getValue());
        newSchedule.setEndDate(endDate.getValue());

        Date now = new Date();

        newSchedule.setCreateDate(now);

        newSchedule.setUpdateDate(now);

        newSchedule.setUpdateUserId(userId);

        newSchedule.setParentId(schedule.getScheduleId());

        newSchedule.setName(schedule.getName());

        newSchedule.setNote(schedule.getNote());

        newSchedule.setPlace(schedule.getPlace());

        newSchedule.setEditFlag(schedule.getEditFlag());

        newSchedule.setPublicFlag(schedule.getPublicFlag());

        newSchedule.setRepeatPattern(schedule.getRepeatPattern());

        newSchedule.setCreateUserId(schedule.getCreateUserId());

        newSchedule.setOwnerId(schedule.getOwnerId());

        newSchedule.setParentId(schedule.getParentId());

        // 2007.3.28 ToDo連携
        List<Integer> memberIdList = new ArrayList<Integer>();
        List<Integer> facilityIdList = new ArrayList<Integer>();

        for (EipTScheduleMap map : scheduleMaps) {
          EipTScheduleMap newMap = Database.create(EipTScheduleMap.class);
          newMap.setEipTSchedule(newSchedule);
          newMap.setUserId(map.getUserId());

          if (map.getUserId() == ownerId) {
            // if (map.getUserId() == userId) {
            newMap.setStatus("O");
          } else {
            EipTScheduleMap tmpMap =
              getScheduleMap(
                scheduleMaps,
                map.getUserId().intValue(),
                ScheduleUtils.SCHEDULEMAP_TYPE_USER);
            if (tmpMap != null) {
              newMap.setStatus(tmpMap.getStatus());
            } else {
              newMap.setStatus("T");
            }
          }
          newMap.setType(map.getType());
          newMap.setCommonCategoryId(map.getCommonCategoryId());
          newMap.setEipTCommonCategory(map.getEipTCommonCategory());

          if (ScheduleUtils.SCHEDULEMAP_TYPE_USER.equals(map.getType())) {
            memberIdList.add(map.getUserId().intValue());
          } else {
            facilityIdList.add(map.getUserId().intValue());
          }
        }

        // 設備重複判定
        if (!ignore_duplicate_facility) {
          if (facilityIdList.size() > 0) {
            if (ScheduleUtils.isDuplicateFacilitySchedule(
              newSchedule,
              facilityIdList,
              schedule.getScheduleId(),
              viewDate.getValue())) {
              msgList.add("duplicate_facility");
              Database.rollback();
              return false;
            }
          }
        }

        Database.commit();
        res = true;
        updateReminder(newSchedule, schedule.getScheduleId().intValue());

        // イベントログに保存
        sendEventLog(rundata, context);
        // メンバー全員に新着ポートレット登録
        sendWhatsNew(newSchedule, true);

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
                "new"));
              message.setCellularBody(ScheduleUtils.createMsg(
                rundata,
                newSchedule,
                memberList,
                destMember.getUserId(),
                "new"));
              messageList.add(message);
            }

            for (ALEipUserAddr destMember : destMemberList) {
              List<ALEipUserAddr> destMembers = new ArrayList<ALEipUserAddr>();
              destMembers.add(destMember);

              ALMailService.sendAdminMailAsync(new ALAdminMailContext(
                orgId,
                ALEipUtils.getUserId(rundata),
                messageList,
                ALMailUtils.getSendDestType(ALMailUtils.KEY_MSGTYPE_SCHEDULE)));

            }

          }

        } catch (Exception ex) {
          msgList.add(ALLocalizationUtils.getl10n("SCHEDULE_DONOT_SEND_MAIL"));
          logger.error("schedule", ex);
          return false;
        }
      }
    } else {
      msgList.add(ALLocalizationUtils
        .getl10n("SCHEDULE_YOU_DONOT_EDIT_THE_SCHEDULE"));
      res = false;
    }
    return res;
  }

  private boolean doCheckAclPermission(RunData rundata, Context context,
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

    boolean hasAuthority =
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
  private boolean doCheckAttachmentAclPermission(RunData rundata,
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
  private void doCheckAttachmentInsertAclPermission(RunData rundata,
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
  private void doCheckAttachmentDeleteAclPermission(RunData rundata,
      Context context) { // ファイル削除権限の有無
    hasAttachmentDeleteAuthority =
      doCheckAttachmentAclPermission(
        rundata,
        context,
        ALAccessControlConstants.VALUE_ACL_DELETE);
  }

  private void sendEventLog(RunData rundata, Context context) {
    ALEipUtils.setTemp(
      rundata,
      context,
      ALEipConstants.MODE,
      ALEipConstants.MODE_UPDATE);
    ALEventlogFactoryService.getInstance().getEventlogHandler().log(
      schedule.getScheduleId(),
      ALEventlogConstants.PORTLET_TYPE_SCHEDULE,
      schedule.getName());
  }

  private void sendWhatsNew(EipTSchedule newSchedule, boolean isNew) {

    // アクティビティ
    ALEipUser loginUser = null;
    try {
      loginUser = ALEipUtils.getALEipUser(userId);
    } catch (ALDBErrorException e) {
      //
    }
    if (loginUser != null) {
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
        "edit",
        userId);

      // アクティビティが公開スケジュールである場合、「更新情報」に表示させる。
      if ("O".equals(newSchedule.getPublicFlag())) {
        ScheduleUtils.createNewScheduleActivity(
          schedule,
          loginName,
          "edit",
          userId);
      }
    }
  }

  private boolean hasAcl(RunData rundata) {
    ALAccessControlFactoryService aclservice =
      (ALAccessControlFactoryService) ((TurbineServices) TurbineServices
        .getInstance()).getService(ALAccessControlFactoryService.SERVICE_NAME);
    ALAccessControlHandler aclhandler = aclservice.getAccessControlHandler();
    boolean hasAuthority =
      aclhandler.hasAuthority(
        ALEipUtils.getUserId(rundata),
        aclPortletFeature =
          ALAccessControlConstants.POERTLET_FEATURE_SCHEDULE_OTHER,
        ALAccessControlConstants.VALUE_ACL_INSERT);
    if (!hasAuthority) {
      return false;
    }
    return true;
  }

  private String getAclPortletFeature() {
    return aclPortletFeature;
  }

  public List<String> getMsgList() {
    return msgList;
  }

  public boolean getIsViewList() {
    return isViewList;
  }

  private EipTScheduleMap getScheduleMap(List<EipTScheduleMap> scheduleMaps,
      int userid, String type) {
    EipTScheduleMap map = null;
    int size = scheduleMaps.size();
    for (int i = 0; i < size; i++) {
      map = scheduleMaps.get(i);
      if (map.getUserId().intValue() == userid && type.equals(map.getType())) {
        return map;
      }
    }
    return null;
  }

  /**
   * セキュリティをチェックします。
   *
   * @return
   */
  private boolean doCheckSecurity(RunData rundata, Context context) {
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
   * ファイルアップロードアクセス権限チェック用メソッド。<br />
   * ファイルアップのアクセス権限をチェックするかどうかを判定します。
   *
   * @return
   */
  private boolean isCheckAttachmentAuthority() {
    return true;
  }

  private boolean hasAttachmentInsertAuthority() {
    return hasAttachmentInsertAuthority;
  }

  private boolean hasAttachmentDeleteAuthority() {
    return hasAttachmentDeleteAuthority;
  }

}
