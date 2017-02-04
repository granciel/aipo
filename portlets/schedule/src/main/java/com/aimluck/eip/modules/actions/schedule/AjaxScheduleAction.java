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
package com.aimluck.eip.modules.actions.schedule;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import org.apache.jetspeed.portal.portlets.VelocityPortlet;
import org.apache.jetspeed.services.logging.JetspeedLogFactoryService;
import org.apache.jetspeed.services.logging.JetspeedLogger;
import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;

import com.aimluck.eip.common.ALEipConstants;
import com.aimluck.eip.common.ALEipUser;
import com.aimluck.eip.modules.actions.common.ALBaseAction;
import com.aimluck.eip.schedule.util.ScheduleUtils;
import com.aimluck.eip.services.orgutils.ALOrgUtilsService;
import com.aimluck.eip.userfacility.beans.UserFacilityLiteBean;
import com.aimluck.eip.userfacility.util.UserFacilityUtils;
import com.aimluck.eip.util.ALCommonUtils;
import com.aimluck.eip.util.ALEipUtils;

/**
 * カレンダーのアクションクラスです。
 *
 * @deprecated
 */
@Deprecated
public class AjaxScheduleAction extends ALBaseAction {

  /** <code>logger</code> logger */
  private static final JetspeedLogger logger =
    JetspeedLogFactoryService.getLogger(AjaxScheduleAction.class.getName());

  private boolean isMax = false;

  /**
   *
   * @param portlet
   * @param context
   * @param rundata
   */
  @Override
  protected void buildNormalContext(VelocityPortlet portlet, Context context,
      RunData rundata) {

    // セッション情報をクリアする
    clearAjaxScheduleSession(rundata, context);

    String tab;

    try {
      context.put("theme", ALOrgUtilsService.getTheme());

      // 表示形式（トップページ）を取得する．
      String top_form = portlet.getPortletConfig().getInitParameter("p19-rows");
      context.put("top_form", top_form);

      // 表示形式（月間）を取得する
      String display_month =
        portlet.getPortletConfig().getInitParameter("p195-rows");
      if (display_month == null || "".equals(display_month)) {
        display_month = "detail";
      }
      context.put("display_month", display_month);

      // 表示開始時間を取得する．
      String time_start =
        portlet.getPortletConfig().getInitParameter("p1a-rows");
      context.put("time_start", time_start);

      // 表示終了時間を取得する．
      String time_end = portlet.getPortletConfig().getInitParameter("p1b-rows");
      context.put("time_end", time_end);

      // 表示時間間隔を取得する．
      String time_interval =
        portlet.getPortletConfig().getInitParameter("p1c-rows");
      context.put("time_interval", time_interval);

      // 表示日数を取得する.
      String weekly_days =
        portlet.getPortletConfig().getInitParameter("p2a-days");
      context.put("weekly_days", weekly_days);

      // 初期選択グループを取得する.
      String weekly_group =
        portlet.getPortletConfig().getInitParameter("p3a-group");
      context.put("weekly_group", weekly_group);

      // 初期選択ユーザー／設備を取得する
      String init_user =
        portlet.getPortletConfig().getInitParameter("p3a-user");
      context.put("init_user", init_user);

      // 初期選択メンバーリストを取得する
      List<UserFacilityLiteBean> memberList =
        new ArrayList<UserFacilityLiteBean>();
      String selected_user =
        portlet.getPortletConfig().getInitParameter("p6a-uids");
      if (selected_user == null || "".equals(selected_user)) {
        UserFacilityLiteBean login_user =
          UserFacilityUtils.getUserFacilityLiteBean(rundata);
        memberList.add(login_user);
      } else {
        String selected_users[] = selected_user.split(",");
        List<UserFacilityLiteBean> ulist =
          ScheduleUtils.getALEipUserFacility(selected_users, rundata);
        if (ulist == null || ulist.size() == 0) {
          UserFacilityLiteBean login_user =
            UserFacilityUtils.getUserFacilityLiteBean(rundata);
          memberList.add(login_user);
        } else {
          memberList.addAll(ulist);
        }
      }
      context.put("member_list", memberList);

      // 初期共有メンバー表示フラグを取得する
      String showAll = portlet.getPortletConfig().getInitParameter("p7d-schk");
      if (!("t".equals(showAll))) {
        showAll = "f";
      }
      context.put("init_s_all", showAll);

      // 現在のユーザー名を取得する
      ALEipUser loginuser = ALEipUtils.getALEipUser(rundata);
      String current = loginuser.getAliasName().toString();
      context.put("current_user", current);
      context.put("current_user_ln", loginuser.getName());
      context.put("current_user_id", loginuser.getUserId());

      // 表示時間を取得する．
      /*
       * ALDateTimeField view_date = AjaxScheduleUtils.getViewDate(rundata,
       * context); context.put("view_date", view_date);
       */

      // Velocity テンプレートを読み込む
      String template = portlet.getPortletConfig().getInitParameter("template");
      if (template.equals("ajax-schedule-oneday")) {
        tab = "oneday";
      } else if (template.equals("ajax-schedule-oneday-group")) {
        tab = "oneday-group";
      } else if (template.equals("ajax-schedule-monthly")) {
        tab = "monthly";
      } else if (template.equals("ajax-calendar-weekly")) {
        tab = "weekly";
      } else if (template.equals("ajax-schedule-weekly-group")) {
        tab = "weekly-group";
      } else {
        tab = "list";
      }

      // アクセスコントロール
      context.put("hasAcl", ScheduleUtils.hasAuthOther(rundata));

      if (isMax) {
        this.putData(rundata, context);
        setTemplate(rundata, "ajax-calendar-list");
      } else {
        if ("ajax-schedule-weekly".equals(template)) {
          setTemplate(rundata, "ajax-calendar-weekly");
        } else {
          setTemplate(rundata, template);
        }
      }
      ALEipUtils.setTemp(rundata, context, "tab", tab);

      // For security
      context.put(
        ALEipConstants.SECURE_ID,
        URLEncoder.encode(
          (String) rundata.getUser().getTemp(ALEipConstants.SECURE_ID),
          ALEipConstants.DEF_CONTENT_ENCODING));

      // For sanitizing
      context.put("utils", new ALCommonUtils());

    } catch (RuntimeException ex) {
      // RuntimeException
      logger.error("[AjaxScheduleAction] Exception.", ex);
      ALEipUtils.redirectDBError(rundata);
    } catch (Exception ex) {
      logger.error("[AjaxScheduleAction] Exception.", ex);
      ALEipUtils.redirectDBError(rundata);
    }
  }

  /**
   *
   * @param portlet
   * @param context
   * @param rundata
   */
  @Override
  protected void buildMaximizedContext(VelocityPortlet portlet, Context context,
      RunData rundata) {
    isMax = true;
    buildNormalContext(portlet, context, rundata);
  }

  /**
   * スケジュールを一覧表示します。
   *
   * @param rundata
   * @param context
   */
  public void doSchedule_list(RunData rundata, Context context) {
    isMax = true;

  }

  private void clearAjaxScheduleSession(RunData rundata, Context context) {
    ALEipUtils.removeTemp(rundata, context, "entityid");
  }
}
