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
package com.aimluck.eip.modules.actions.timeline;

import java.util.ArrayList;
import java.util.List;

import org.apache.jetspeed.portal.portlets.VelocityPortlet;
import org.apache.jetspeed.services.logging.JetspeedLogFactoryService;
import org.apache.jetspeed.services.logging.JetspeedLogger;
import org.apache.turbine.services.TurbineServices;
import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;

import com.aimluck.eip.modules.actions.common.ALBaseAction;
import com.aimluck.eip.services.timeline.ALTimelineFactoryService;
import com.aimluck.eip.services.timeline.ALTimelineHandler;
import com.aimluck.eip.timeline.TimelineFormData;
import com.aimluck.eip.timeline.TimelineSelectData;
import com.aimluck.eip.util.ALEipUtils;

/**
 * タイムラインのアクションクラス <BR>
 *
 */
public class TimelineAction extends ALBaseAction {

  /** logger */
  private static final JetspeedLogger logger = JetspeedLogFactoryService
    .getLogger(TimelineAction.class.getName());

  /**
   * 通常表示の際の処理を記述します。
   *
   * @param portlet
   * @param context
   * @param rundata
   * @throws Exception
   */
  @Override
  protected void buildNormalContext(VelocityPortlet portlet, Context context,
      RunData rundata) throws Exception {

    // セッション情報をクリアする．
    clearTimelineSession(rundata, context);

    TimelineSelectData listData = new TimelineSelectData();
    listData.initField();
    listData.setContentHeightMax(Integer.parseInt(ALEipUtils.getPortlet(
      rundata,
      context).getPortletConfig().getInitParameter("p1a-rows", "0")));

    prepareService(rundata, context);

    // PSMLからパラメータをロードする
    // 最大表示件数（最大化時）
    listData.setRowsNum(20);

    if (listData.doViewList(this, rundata, context)) {
      setTemplate(rundata, "timeline");
    }
  }

  /**
   * 最大化表示の際の処理を記述します。 <BR>
   *
   * @param portlet
   * @param context
   * @param rundata
   */
  @Override
  protected void buildMaximizedContext(VelocityPortlet portlet,
      Context context, RunData rundata) {
    try {
      doTimeline_list(rundata, context);
      prepareService(rundata, context);

    } catch (Exception ex) {
      logger.error("timeline", ex);
    }
  }

  /**
   * トピックを一覧表示します。 <BR>
   *
   * @param rundata
   * @param context
   * @throws Exception
   */
  public void doTimeline_list(RunData rundata, Context context)
      throws Exception {
    TimelineSelectData listData = new TimelineSelectData();
    listData.initField();
    listData.setContentHeightMax(Integer.parseInt(ALEipUtils.getPortlet(
      rundata,
      context).getPortletConfig().getInitParameter("p2a-rows", "0")));
    // PSMLからパラメータをロードする
    // 最大表示件数（最大化時）
    listData.setRowsNum(20);
    listData.doViewList(this, rundata, context);
    setTemplate(rundata, "timeline-list");
  }

  /**
   * トピックを登録します。 <BR>
   *
   * @param rundata
   * @param context
   * @throws Exception
   */
  public void doTimeline_insert(RunData rundata, Context context)
      throws Exception {
    TimelineFormData formData = new TimelineFormData();
    formData.initField();
    if (formData.doInsert(this, rundata, context)) {
      // データ登録が成功したとき
    }
    doTimeline_list(rundata, context);
  }

  /**
   * タイムラインで使用したセッション情報を消去する．
   *
   */
  public void clearTimelineSession(RunData rundata, Context context) {
    List<String> list = new ArrayList<String>();
    list.add("entityid");
    ALEipUtils.removeTemp(rundata, context, list);

  }

  /**
   *
   *
   */
  public void prepareService(RunData rundata, Context context) {
    ALTimelineFactoryService tlservice =
      (ALTimelineFactoryService) ((TurbineServices) TurbineServices
        .getInstance()).getService(ALTimelineFactoryService.SERVICE_NAME);
    ALTimelineHandler timelinehandler = tlservice.getTimelineHandler();
    String token = ALEipUtils.getTemp(rundata, context, "timelineToken");
    if (token == null || "".equals(token)) {
      token = timelinehandler.getToken(rundata);
      if (token != null && !("".equals(token))) {
        ALEipUtils.setTemp(rundata, context, "timelineToken", token);
      }
    }

    context.put("token", token);
    context.put("jsapiUrl", timelinehandler.getApiUrl());
    if (rundata.getUserAgent().trim().indexOf("Mac") != -1) {
      context.put("isMacOS", "true");
    }
  }
}
