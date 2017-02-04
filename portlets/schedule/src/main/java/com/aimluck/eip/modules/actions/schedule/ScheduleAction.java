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

import java.util.ArrayList;
import java.util.List;

import org.apache.jetspeed.portal.portlets.VelocityPortlet;
import org.apache.jetspeed.services.logging.JetspeedLogFactoryService;
import org.apache.jetspeed.services.logging.JetspeedLogger;
import org.apache.jetspeed.services.rundata.JetspeedRunData;
import org.apache.jetspeed.util.template.JetspeedLink;
import org.apache.jetspeed.util.template.JetspeedLinkFactory;
import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;

import com.aimluck.eip.cayenne.om.portlet.VEipTScheduleList;
import com.aimluck.eip.common.ALAbstractSelectData;
import com.aimluck.eip.common.ALEipConstants;
import com.aimluck.eip.common.ALEipUser;
import com.aimluck.eip.common.ALPageNotFoundException;
import com.aimluck.eip.modules.actions.common.ALBaseAction;
import com.aimluck.eip.schedule.AjaxScheduleWeeklyGroupEmptySelectData;
import com.aimluck.eip.schedule.ScheduleChangeStatusFormData;
import com.aimluck.eip.schedule.ScheduleFormData;
import com.aimluck.eip.schedule.ScheduleListSelectData;
import com.aimluck.eip.schedule.ScheduleMonthlySelectData;
import com.aimluck.eip.schedule.ScheduleOnedayGroupSelectData;
import com.aimluck.eip.schedule.ScheduleOnedaySelectData;
import com.aimluck.eip.schedule.ScheduleSearchSelectData;
import com.aimluck.eip.schedule.ScheduleSelectData;
import com.aimluck.eip.schedule.ScheduleWeeklyGroupSelectData;
import com.aimluck.eip.schedule.ScheduleWeeklySelectData;
import com.aimluck.eip.schedule.util.ScheduleUtils;
import com.aimluck.eip.services.orgutils.ALOrgUtilsService;
import com.aimluck.eip.userfacility.beans.UserFacilityLiteBean;
import com.aimluck.eip.userfacility.util.UserFacilityUtils;
import com.aimluck.eip.util.ALEipUtils;

/**
 * スケジュールのアクションクラスです。
 *
 */
public class ScheduleAction extends ALBaseAction {

  // 週表示のデフォルトフィルター
  static final String LIST_FILTER_STR_W = new StringBuffer().append(
    ScheduleWeeklyGroupSelectData.class.getSimpleName()).append(
    ALEipConstants.LIST_FILTER).toString();

  static final String LIST_FILTER_TYPE_STR_W = new StringBuffer().append(
    ScheduleWeeklyGroupSelectData.class.getSimpleName()).append(
    ALEipConstants.LIST_FILTER_TYPE).toString();

  // 日表示のデフォルトフィルター
  static final String LIST_FILTER_STR_D = new StringBuffer().append(
    ScheduleOnedayGroupSelectData.class.getSimpleName()).append(
    ALEipConstants.LIST_FILTER).toString();

  static final String LIST_FILTER_TYPE_STR_D = new StringBuffer().append(
    ScheduleOnedayGroupSelectData.class.getSimpleName()).append(
    ALEipConstants.LIST_FILTER_TYPE).toString();

  /** <code>logger</code> logger */
  private static final JetspeedLogger logger = JetspeedLogFactoryService
    .getLogger(ScheduleAction.class.getName());

  /** ノーマル画面からのスケジュール入力 */
  private static final String AFTER_BEHAVIOR = "afterbehavior";

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
    clearScheduleSession(rundata, context);

    String tab;
    String portletId;
    ALAbstractSelectData<VEipTScheduleList, VEipTScheduleList> listData;

    try {
      // ポートレット ID を取得する．
      portletId = portlet.getID();

      // トップ画面からの操作後に，トップ画面に戻すかどうかを判定する．
      String afterBehavior =
        ALEipUtils
          .getPortlet(rundata, context)
          .getPortletConfig()
          .getInitParameter("p4b-behavior");
      if ("1".equals(afterBehavior)) {
        // ノーマル画面であることを指定．
        context.put(AFTER_BEHAVIOR, "1");
      }

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
      String _days = portlet.getPortletConfig().getInitParameter("p2a-days");
      context.put("weekly_days", _days);

      // 初期共有メンバー表示フラグを取得する
      String showAll = portlet.getPortletConfig().getInitParameter("p7d-schk");
      if (!("t".equals(showAll))) {
        showAll = "f";
      }
      context.put("init_s_all", showAll);

      // load and set xreg info
      ALEipUtils.setTemp(
        rundata,
        context,
        ScheduleUtils.FLAG_CHANGE_TURN_STR,
        portlet.getPortletConfig().getInitParameter("p3b-group"));

      // Velocity テンプレートを読み込む
      String template = "";
      boolean done = false;
      String _template = "";
      if ("simple".equals(top_form)) {
        _template = "schedule-calendar";
      } else {
        _template = ScheduleUtils.getCurrentTab(rundata, context);
        if (!_template.startsWith("schedule-")) {
          _template = "schedule-" + _template;
        }
      }

      // 現在のユーザー名を取得する
      ALEipUser loginuser = ALEipUtils.getALEipUser(rundata);
      String current = loginuser.getAliasName().toString();
      context.put("current_user", current);
      context.put("current_user_ln", loginuser.getName());
      context.put("current_user_id", loginuser.getUserId());

      String has_acl_other = ScheduleUtils.hasAuthOther(rundata);
      context.put("hasAcl", has_acl_other);
      if (has_acl_other.equals("F")) {
        if (rundata.getParameters().get("filter") == null
          || rundata.getParameters().get("filter").equals("Facility")) {
          rundata.getParameters().add("filter", "null");
          rundata.getParameters().remove("filtertype");
        }
      }

      if (("".equals(template)) || (!done)) {
        template = "schedule-calendar";
        if (template.equals(_template)) {
          done = true;
        }
      }
      if (("".equals(template)) || (!done)) {
        template = "schedule-oneday";
        if (template.equals(_template)) {
          template = "schedule-oneday-group";
          done = true;
        }
      }
      if (("".equals(template)) || (!done)) {
        template = "schedule-weekly";
        if (template.equals(_template)) {
          template = "schedule-weekly-group";
          done = true;
        }
      }
      if (("".equals(template)) || (!done)) {
        template = "schedule-monthly";
        if (template.equals(_template)) {
          done = true;
        }
      }
      if (("".equals(template)) || (!done)) {
        template = "schedule-oneday-group";
        if (template.equals(_template)) {
          done = true;
        }
      }
      if (("".equals(template)) || (!done)) {
        template = "schedule-weekly-group";
        if (template.equals(_template)) {
          done = true;
        }
      }
      if (("".equals(template)) || (!done)) {
        template = "schedule-search-list";
        if (template.equals(_template)) {
          done = true;
        }
      }
      if ("".equals(template)) {
        template = _template;
      }

      String useragent = rundata.getUserAgent();
      if ("IPHONE".equals(ALEipUtils.getClient(rundata))) {
        template = "schedule-search-list";
      }

      useragent = useragent.trim();
      if (useragent.indexOf("Mac") != -1 && useragent.indexOf("iPad") == -1) {
        context.put("isMac", 1);
      } else {
        context.put("isMac", 0);
      }

      int schedule_selected_daylist =
        rundata.getCookies().getInteger(
          "schedule_selected_daylist_" + portletId);
      if (schedule_selected_daylist != 0) {
        context.put("dayList", schedule_selected_daylist);
      } else {
        context.put("dayList", 1);
      }
      if (template.equals("schedule-calendar")) {
        tab = "calendar";
        listData = new AjaxScheduleWeeklyGroupEmptySelectData();
        boolean isMsie = ALEipUtils.isMsieBrowser(rundata);
        context.put("isMeie", Boolean.valueOf(isMsie));

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
          List<UserFacilityLiteBean> ulist;
          if ("F".equals(has_acl_other)) {
            ulist = ScheduleUtils.getALEipFacility(selected_users, rundata);
            if (selected_user.contains(String.valueOf(ALEipUtils
              .getUserId(rundata)))) {
              UserFacilityLiteBean login_user =
                UserFacilityUtils.getUserFacilityLiteBean(rundata);
              ulist.add(login_user);
            }
          } else {
            ulist = ScheduleUtils.getALEipUserFacility(selected_users, rundata);
          }
          if (ulist == null || ulist.size() == 0) {
            UserFacilityLiteBean login_user =
              UserFacilityUtils.getUserFacilityLiteBean(rundata);
            memberList.add(login_user);
          } else {
            memberList.addAll(ulist);
          }
        }

        context.put("member_list", memberList);
      } else if (template.equals("schedule-oneday")) {
        tab = "oneday-group";
        listData = new ScheduleOnedaySelectData();
        ((ScheduleOnedaySelectData) listData).setPortletId(portletId);
        // ブラウザ名を受け渡す．
        boolean isMsie = ALEipUtils.isMsieBrowser(rundata);
        context.put("isMeie", Boolean.valueOf(isMsie));
      } else if (template.equals("schedule-weekly")) {
        tab = "weekly-group";
        listData = new ScheduleWeeklySelectData();
        ((ScheduleWeeklySelectData) listData).setPortletId(portletId);
      } else if (template.equals("schedule-monthly")) {
        tab = "monthly";
        listData = new ScheduleMonthlySelectData();
        ((ScheduleMonthlySelectData) listData).setPortletId(portletId);
      } else if (template.equals("schedule-oneday-group")) {
        tab = "oneday-group";
        listData = new ScheduleOnedayGroupSelectData();
        ((ScheduleOnedayGroupSelectData) listData).setPortletId(portletId);
        // ブラウザ名を受け渡す．
        boolean isMsie = ALEipUtils.isMsieBrowser(rundata);
        context.put("isMeie", Boolean.valueOf(isMsie));
      } else if (template.equals("schedule-weekly-group")) {
        tab = "weekly-group";
        listData = new ScheduleWeeklyGroupSelectData();
        ((ScheduleWeeklyGroupSelectData) listData).setPortletId(portletId);
      } else if (template.equals("schedule-search-list")) {
        tab = "list";
        listData = new ScheduleListSelectData();
        ((ScheduleListSelectData) listData).setPortletId(portletId);
      } else {
        tab = "search";
        listData = new ScheduleSearchSelectData();
        ((ScheduleSearchSelectData) listData).setPortletId(portletId);
        // TODO: 表示カスタマイズ
        listData.setRowsNum(20);
      }

      ALEipUtils.setPsmlParameters(rundata, context, "pba-template", template);
      if (!"simple".equals(top_form)) {
        ALEipUtils.setTemp(rundata, context, "tab", tab);
      }
      listData.initField();

      // 最低限表示するのに必要な権限のチェック
      if (!ScheduleUtils.hasMinimumAuthority(rundata)) {
        setTemplate(rundata, "schedule");
        context.put("hasMinimumAuthority", false);
      } else {
        if (listData.doViewList(this, rundata, context)) {
          setTemplate(rundata, "schedule");
          context.put("hasMinimumAuthority", true);
        }
      }
    } catch (Exception ex) {
      logger.error("[ScheduleAction] Exception.", ex);
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
  protected void buildMaximizedContext(VelocityPortlet portlet,
      Context context, RunData rundata) {

    // MODEを取得
    String mode = rundata.getParameters().getString(ALEipConstants.MODE);

    context.put("theme", ALOrgUtilsService.getTheme());

    // 表示形式（月間）を取得する
    String display_month =
      portlet.getPortletConfig().getInitParameter("p195-rows");
    if (display_month == null || "".equals(display_month)) {
      display_month = "detail";
    }
    context.put("display_month", display_month);

    // 表示開始時間を取得する．
    String time_start = portlet.getPortletConfig().getInitParameter("p1a-rows");
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

    // 週頭を取得する
    String startDayOfWeek =
      portlet.getPortletConfig().getInitParameter("z1a-rows");
    context.put("startDayOfWeek", startDayOfWeek);

    // 初期共有メンバー表示フラグを取得する
    String showAll = portlet.getPortletConfig().getInitParameter("p7d-schk");
    if (!("t".equals(showAll))) {
      showAll = "f";
    }
    context.put("init_s_all", showAll);

    // log error回避用
    context.put("top_form", "");
    String useragent = rundata.getUserAgent();
    useragent = useragent.trim();
    if (useragent.indexOf("Mac") != -1 && useragent.indexOf("iPad") == -1) {
      context.put("isMac", 1);
    } else {
      context.put("isMac", 0);
    }

    // load and set xreg info
    ALEipUtils.setTemp(
      rundata,
      context,
      ScheduleUtils.FLAG_CHANGE_TURN_STR,
      portlet.getPortletConfig().getInitParameter("p3b-group"));

    try {

      // 現在のユーザー名を取得する
      ALEipUser loginuser = ALEipUtils.getALEipUser(rundata);
      String current = loginuser.getAliasName().toString();
      context.put("current_user", current);
      context.put("current_user_ln", loginuser.getName());
      context.put("current_user_id", loginuser.getUserId());

      String has_acl_other = ScheduleUtils.hasAuthOther(rundata);
      context.put("hasAcl", has_acl_other);
      if (has_acl_other.equals("F")) {
        if (rundata.getParameters().get("filter") == null
          || rundata.getParameters().get("filter").equals("Facility")) {
          rundata.getParameters().add("filter", "null");
          rundata.getParameters().remove("filtertype");
        }
      }

      if (ALEipConstants.MODE_FORM.equals(mode)) {
        doSchedule_form(rundata, context);
      } else if (ALEipConstants.MODE_DETAIL.equals(mode)) {
        doSchedule_detail(rundata, context);
      } else if (ALEipConstants.MODE_LIST.equals(mode)) {
        doSchedule_list(rundata, context);
      }

      if (getMode() == null) {
        doSchedule_list(rundata, context);
      }
    } catch (Exception ex) {
      logger.error("[ScheduleAction] Exception.", ex);
      ALEipUtils.redirectDBError(rundata);
    }

  }

  /**
   * スケジュール登録のフォームを表示します。
   *
   * @param rundata
   * @param context
   */
  public void doSchedule_form(RunData rundata, Context context) {
    try {
      ScheduleFormData formData = new ScheduleFormData();
      formData.loadParameters(rundata, context);
      formData.initField();
      formData.doViewForm(this, rundata, context);

      // ブラウザ名を受け渡す．
      boolean isMsie = ALEipUtils.isMsieBrowser(rundata);
      context.put("isMeie", Boolean.valueOf(isMsie));

      // トップ画面からのスケジュール入力であるかを判定する．
      String afterBehavior = rundata.getRequest().getParameter(AFTER_BEHAVIOR);
      if (afterBehavior != null) {
        context.put(AFTER_BEHAVIOR, "1");
      }

      setTemplate(rundata, "schedule-form");
    } catch (Exception ex) {
      logger.error("[ScheduleAction] Exception.", ex);
      ALEipUtils.redirectDBError(rundata);
    }
  }

  /**
   * スケジュールを登録します。
   *
   * @param rundata
   * @param context
   */
  public void doSchedule_insert(RunData rundata, Context context) {
    try {
      ScheduleFormData formData = new ScheduleFormData();
      formData.initField();

      // ブラウザ名を受け渡す．
      boolean isMsie = ALEipUtils.isMsieBrowser(rundata);
      context.put("isMeie", Boolean.valueOf(isMsie));

      String afterBehavior = rundata.getRequest().getParameter(AFTER_BEHAVIOR);
      setTemplate(rundata, "schedule-form");
      if (formData.doInsert(this, rundata, context)) {
        if ("1".equals(afterBehavior)) {
          JetspeedLink jsLink = JetspeedLinkFactory.getInstance(rundata);
          rundata.setRedirectURI(jsLink.getPortletById(
            ALEipUtils.getPortlet(rundata, context).getID()).addQueryData(
            "action",
            "controls.Restore").toString());
          rundata.getResponse().sendRedirect(rundata.getRedirectURI());
          jsLink = null;
        } else {
          doSchedule_list(rundata, context);
          // rundata.setRedirectURI(jsLink.getPortletById(
          // ALEipUtils.getPortlet(rundata, context).getID()).addQueryData(
          // "eventSubmit_doSchedule_list", "1").toString());
        }

      } else {
        if ("1".equals(afterBehavior)) {
          // 追加処理後にノーマル画面に画面遷移することを指定．
          context.put(AFTER_BEHAVIOR, "1");
        }
      }
    } catch (Exception ex) {
      logger.error("[ScheduleAction] Exception.", ex);
      ALEipUtils.redirectDBError(rundata);
    }
  }

  /**
   * スケジュールを更新します。
   *
   * @param rundata
   * @param context
   */
  public void doSchedule_update(RunData rundata, Context context) {
    try {
      ScheduleFormData formData = new ScheduleFormData();
      formData.loadParametersViewDate(rundata, context);
      formData.initField();

      // ブラウザ名を受け渡す．
      boolean isMsie = ALEipUtils.isMsieBrowser(rundata);
      context.put("isMeie", Boolean.valueOf(isMsie));

      String afterBehavior = rundata.getRequest().getParameter(AFTER_BEHAVIOR);
      setTemplate(rundata, "schedule-form");
      if (formData.doUpdate(this, rundata, context)) {
        if ("1".equals(afterBehavior)) {
          JetspeedLink jsLink = JetspeedLinkFactory.getInstance(rundata);
          rundata.setRedirectURI(jsLink.getPortletById(
            ALEipUtils.getPortlet(rundata, context).getID()).addQueryData(
            "action",
            "controls.Restore").toString());
          rundata.getResponse().sendRedirect(rundata.getRedirectURI());
          jsLink = null;
        } else {
          doSchedule_list(rundata, context);
          // rundata.setRedirectURI(jsLink.getPortletById(
          // ALEipUtils.getPortlet(rundata, context).getID()).addQueryData(
          // "eventSubmit_doSchedule_list", "1").toString());
        }

      } else {
        if ("1".equals(afterBehavior)) {
          // 変更処理後にノーマル画面に画面遷移することを指定．
          context.put(AFTER_BEHAVIOR, "1");
        }
      }
    } catch (Exception ex) {
      logger.error("[ScheduleAction] Exception.", ex);
      ALEipUtils.redirectDBError(rundata);
    }
  }

  /**
   * スケジュールを削除します。
   *
   * @param rundata
   * @param context
   */
  public void doSchedule_delete(RunData rundata, Context context) {
    try {
      String afterBehavior = rundata.getRequest().getParameter(AFTER_BEHAVIOR);
      ScheduleFormData formData = new ScheduleFormData();
      formData.loadParametersViewDate(rundata, context);
      formData.initField();
      if (formData.doDelete(this, rundata, context)) {
        setTemplate(rundata, "schedule-form");
        if ("1".equals(afterBehavior)) {
          JetspeedLink jsLink = JetspeedLinkFactory.getInstance(rundata);
          rundata.setRedirectURI(jsLink.getPortletById(
            ALEipUtils.getPortlet(rundata, context).getID()).addQueryData(
            "action",
            "controls.Restore").toString());
          rundata.getResponse().sendRedirect(rundata.getRedirectURI());
          jsLink = null;
        } else {
          doSchedule_list(rundata, context);
          // rundata.setRedirectURI(jsLink.getPortletById(
          // ALEipUtils.getPortlet(rundata, context).getID()).addQueryData(
          // "eventSubmit_doSchedule_list", "1").toString());
        }

      } else {
        if ("1".equals(afterBehavior)) {
          // 削除処理後にノーマル画面に画面遷移することを指定．
          context.put(AFTER_BEHAVIOR, "1");
        }
      }
    } catch (Exception ex) {
      logger.error("[ScheduleAction] Exception.", ex);
      ALEipUtils.redirectDBError(rundata);
    }
  }

  /**
   * スケジュールを一覧表示します。
   *
   * @param rundata
   * @param context
   */

  public void doSchedule_list(RunData rundata, Context context) {
    try {
      // ポートレット ID を取得する．
      String portletId = ((JetspeedRunData) rundata).getJs_peid();
      VelocityPortlet portlet = ALEipUtils.getPortlet(rundata, context);
      // 自ポートレットからのリクエストであれば、パラメータを展開しセッションに保存する。

      String currentTab;
      ALAbstractSelectData<VEipTScheduleList, VEipTScheduleList> listData;

      currentTab =
        ScheduleUtils.getCurrentTab(rundata, context).replaceFirst(
          "schedule-",
          "");

      if ("search".equals(currentTab) || "search-list".equals(currentTab)) {
        currentTab =
          ScheduleUtils.getTabNameFromLayout(portlet
            .getPortletConfig()
            .getInitParameter("pba-template"));
      }

      String useragent = rundata.getUserAgent();
      if ("IPHONE".equals(ALEipUtils.getClient(rundata))) {
        currentTab = "list";

        context.put("time_start", 0);
        context.put("time_end", 24);
        context.put("top_form", "simple");
        context.put("dayList", 1);
        context.put("startDayOfWeek", 1);
        ALEipUtils.setTemp(rundata, context, "tab", currentTab);
      }

      useragent = useragent.trim();
      if (useragent.indexOf("Mac") != -1 && useragent.indexOf("iPad") == -1) {
        context.put("isMac", 1);
      }

      if (currentTab.equals("calendar")) {
        listData = new AjaxScheduleWeeklyGroupEmptySelectData();
        boolean isMsie = ALEipUtils.isMsieBrowser(rundata);
        context.put("isMeie", Boolean.valueOf(isMsie));

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
      } else if (currentTab.equals("oneday")) {
        listData = new ScheduleOnedaySelectData();
        ((ScheduleOnedaySelectData) listData).setPortletId(portletId);
        // ブラウザ名を受け渡す．
        boolean isMsie = ALEipUtils.isMsieBrowser(rundata);
        context.put("isMeie", Boolean.valueOf(isMsie));
      } else if (currentTab.equals("weekly")) {
        listData = new ScheduleWeeklySelectData();
        ((ScheduleWeeklySelectData) listData).setPortletId(portletId);
      } else if (currentTab.equals("monthly")) {
        listData = new ScheduleMonthlySelectData();
        ((ScheduleMonthlySelectData) listData).setPortletId(portletId);
      } else if (currentTab.equals("oneday-group")) {
        listData = new ScheduleOnedayGroupSelectData();
        ((ScheduleOnedayGroupSelectData) listData).setPortletId(portletId);
        // ブラウザ名を受け渡す．
        boolean isMsie = ALEipUtils.isMsieBrowser(rundata);
        context.put("isMeie", Boolean.valueOf(isMsie));
      } else if (currentTab.equals("list")) {
        listData = new ScheduleListSelectData();
        ((ScheduleListSelectData) listData).setPortletId(portletId);
        // ブラウザ名を受け渡す．
        boolean isMsie = ALEipUtils.isMsieBrowser(rundata);
        context.put("isMeie", Boolean.valueOf(isMsie));
      } else {
        listData = new ScheduleWeeklyGroupSelectData();
        ((ScheduleWeeklyGroupSelectData) listData).setPortletId(portletId);
      }
      ALEipUtils.setTemp(rundata, context, "tab", currentTab);
      listData.initField();
      if (!ScheduleUtils.hasMinimumAuthority(rundata)) {
        setTemplate(rundata, "schedule-list");
      } else {
        listData.doViewList(this, rundata, context);
        setTemplate(rundata, "schedule-list");
      }
    } catch (Exception ex) {
      logger.error("[ScheduleAction] Exception.", ex);
      ALEipUtils.redirectDBError(rundata);
    }
  }

  /**
   * スケジュールを詳細表示します。
   *
   * @param rundata
   * @param context
   */
  public void doSchedule_detail(RunData rundata, Context context) {
    try {
      // トップ画面からのスケジュール入力であるかを判定する．
      String afterBehavior = rundata.getRequest().getParameter(AFTER_BEHAVIOR);
      if (afterBehavior != null) {
        context.put(AFTER_BEHAVIOR, "1");
      }

      ScheduleSelectData detailData = new ScheduleSelectData();
      detailData.initField();
      detailData.doViewDetail(this, rundata, context);
      setTemplate(rundata, "schedule-detail");
    } catch (Exception ex) {
      logger.error("[ScheduleAction] Exception.", ex);
      ALEipUtils.redirectDBError(rundata);
    }
  }

  /**
   * スケジュールの状態を変更します。
   *
   * @param rundata
   * @param context
   */
  public void doSchedule_change_status(RunData rundata, Context context) {
    try {
      ScheduleChangeStatusFormData formData =
        new ScheduleChangeStatusFormData();
      formData.loadParametersViewDate(rundata, context);
      formData.initField();
      if (formData.doUpdate(this, rundata, context)) {
        String viewDate = formData.getViewDate().toString();
        setTemplate(rundata, "schedule-detail");

        if (viewDate == null || viewDate.equals("")) {
          logger
            .error("[ScheduleAction] ALPageNotFoundException: View Date is wrong.");
          throw new ALPageNotFoundException();
        }

        doSchedule_detail(rundata, context);
        // JetspeedLink jsLink = JetspeedLinkFactory.getInstance(rundata);
        // rundata.setRedirectURI(jsLink.getPortletById(
        // ALEipUtils.getPortlet(rundata, context).getID()).addQueryData(
        // "eventSubmit_doSchedule_detail", "1").addQueryData("view_date",
        // viewDate).toString());
        // rundata.getResponse().sendRedirect(rundata.getRedirectURI());
        // jsLink = null;
      }
    } catch (Exception ex) {
      logger.error("[ScheduleAction] Exception.", ex);
      ALEipUtils.redirectDBError(rundata);
    }
  }

  /**
   * 編集画面でキャンセルを押したときの処理．
   *
   * @param rundata
   * @param context
   */
  public void doSchedule_cancel(RunData rundata, Context context) {
    try {
      // トップ画面からのスケジュール入力であるかを判定する．
      String afterBehavior = rundata.getRequest().getParameter(AFTER_BEHAVIOR);
      if (afterBehavior != null && "1".equals(afterBehavior)) {
        JetspeedLink jsLink = JetspeedLinkFactory.getInstance(rundata);
        rundata.setRedirectURI(jsLink.getPortletById(
          ALEipUtils.getPortlet(rundata, context).getID()).addQueryData(
          "action",
          "controls.Restore").toString());
        rundata.getResponse().sendRedirect(rundata.getRedirectURI());
        jsLink = null;
      } else {
        doSchedule_list(rundata, context);
      }
    } catch (Exception ex) {
      logger.error("[ScheduleAction] Exception.", ex);
    }
  }

  private void clearScheduleSession(RunData rundata, Context context) {
    List<String> list = new ArrayList<String>();
    list.add("entityid");
    list.add("target_user_id");
    list.add(ScheduleUtils.TARGET_KEYWORD);
    ALEipUtils.removeTemp(rundata, context, list);
  }

}
