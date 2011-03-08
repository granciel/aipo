/*
 * Aipo is a groupware program developed by Aimluck,Inc.
 * Copyright (C) 2004-2011 Aimluck,Inc.
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

package com.aimluck.eip.modules.screens;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.jetspeed.om.profile.Entry;
import org.apache.jetspeed.om.profile.Portlets;
import org.apache.jetspeed.om.profile.Profile;
import org.apache.jetspeed.om.registry.PortletEntry;
import org.apache.jetspeed.portal.portlets.VelocityPortlet;
import org.apache.jetspeed.services.logging.JetspeedLogFactoryService;
import org.apache.jetspeed.services.logging.JetspeedLogger;
import org.apache.jetspeed.services.rundata.JetspeedRunData;
import org.apache.jetspeed.util.PortletSessionState;
import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;

import com.aimluck.eip.services.accessctl.ALAccessControlConstants;
import com.aimluck.eip.util.ALEipUtils;
import com.aimluck.eip.util.CustomizeUtils;

/**
 * 伝言メモの一覧を処理するクラスです。
 * 
 */
public class CustomizeFormScreen extends ALVelocityScreen {

  private static final String USER_SELECTIONS =
    "session.portlets.user.selections";

  private static final String UI_PORTLETS_SELECTED = "portletsSelected";

  private static final String PORTLET_LIST = "session.portlets.list";

  public static final String FILTER_FIELDS = "filter_fields";

  public static final String FILTER_VALUES = "filter_values";

  /** アクセス権限の有無 */
  protected boolean hasAuthority;

  /** logger */
  private static final JetspeedLogger logger = JetspeedLogFactoryService
    .getLogger(CustomizeFormScreen.class.getName());

  /**
   * 
   * @param rundata
   * @param context
   * @throws Exception
   */
  @Override
  protected void doOutput(RunData rundata, Context context) throws Exception {
    VelocityPortlet portlet = ALEipUtils.getPortlet(rundata, context);
    try {
      ALEipUtils.CheckAclPermissionForCustomize(
        rundata,
        context,
        ALAccessControlConstants.VALUE_ACL_INSERT);

      JetspeedRunData jdata = (JetspeedRunData) rundata;
      Profile profile = jdata.getProfile();
      String mediaType = profile.getMediaType();

      // make the list of already used panes/portlets available through the
      // 'runs'
      // reference
      List<Entry> portletList = new ArrayList<Entry>();

      String pid = rundata.getParameters().get("js_pane");
      Portlets tabPortlets = profile.getDocument().getPortletsById(pid);
      Entry[] currentPortletEntries = tabPortlets.getEntriesArray();
      for (Entry entry : currentPortletEntries) {
        portletList.add(entry);
      }
      context.put("runs", portletList);

      int start = rundata.getParameters().getInt("start", -1);
      if (start < 0) {
        start = 0;
        PortletSessionState.clearAttribute(rundata, USER_SELECTIONS);
        PortletSessionState.clearAttribute(rundata, PORTLET_LIST);
      }

      List<PortletEntry> allPortlets = new ArrayList<PortletEntry>();
      List<PortletEntry> portlets =
        CustomizeUtils.buildPortletList(rundata, mediaType, allPortlets);
      Map<String, PortletEntry> userSelections =
        CustomizeUtils.getUserSelections(rundata);
      // List<BaseCategory> categories =
      // CustomizeUtils.buildCategoryList(rundata, mediaType, allPortlets);
      // context.put("categories", categories);
      // context.put("parents", PortletFilter.buildParentList(allPortlets));
      // addFiltersToContext(rundata, context);

      int size = getSize(portlet);
      int end = Math.min(start + size, portlets.size());
      if (start > 0) {
        context.put("prev", String.valueOf(Math.max(start - size, 0)));
      }
      if (start + size < portlets.size()) {
        context.put("next", String.valueOf(start + size));
      }
      context.put("browser", portlets.subList(start, end));
      context.put("size", Integer.valueOf(size));
      context.put(UI_PORTLETS_SELECTED, userSelections);

      context.put("portlets", portlets);
      context.put("js_peid", pid);
      rundata.getParameters().setString("js_peid", pid);

      context.put("controller", tabPortlets.getController().getName());

      ALEipUtils.setupContext(rundata, context);
      // putData(rundata, context);

      String layout_template = "portlets/html/ja/ajax-customize-form.vm";
      setTemplate(rundata, context, layout_template);
    } catch (Exception ex) {
      logger.error("[NoteScreen] Exception.", ex);
      ALEipUtils.redirectDBError(rundata);
    }
  }

  @SuppressWarnings("unused")
  private void addFiltersToContext(RunData data, Context context) {
    String[] filterFields =
      (String[]) PortletSessionState.getAttribute(data, FILTER_FIELDS);
    String[] filterValues =
      (String[]) PortletSessionState.getAttribute(data, FILTER_VALUES);
    if (filterFields != null
      && filterValues != null
      && filterFields.length == filterValues.length) {
      for (int i = 0; i < filterFields.length; i++) {
        String field = filterFields[i];
        String value = filterValues[i];

        context.put(field + "_filter_value", value);
      }
    }
  }

  public int getSize(VelocityPortlet portlet) {
    int size = 30;
    try {
      size =
        Integer.parseInt(portlet.getPortletConfig().getInitParameter("size"));
    } catch (Exception e) {
      logger.debug("CustomizeSetAction: Init param 'size' not parsed");
    }
    return size;
  }

}
