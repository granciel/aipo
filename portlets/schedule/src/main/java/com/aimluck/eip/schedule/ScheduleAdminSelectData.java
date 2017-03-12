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

import java.util.jar.Attributes;

import org.apache.jetspeed.om.registry.PortletEntry;
import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;

import com.aimluck.commons.field.ALStringField;
import com.aimluck.eip.common.ALAbstractSelectData;
import com.aimluck.eip.common.ALDBErrorException;
import com.aimluck.eip.common.ALData;
import com.aimluck.eip.common.ALPageNotFoundException;
import com.aimluck.eip.modules.actions.common.ALAction;
import com.aimluck.eip.orm.query.ResultList;
import com.aimluck.eip.schedule.util.ScheduleUtils;
import com.aimluck.eip.services.config.ALConfigHandler;
import com.aimluck.eip.services.config.ALConfigService;
import com.aimluck.eip.util.CustomizeUtils;

/**
 *
 */
public class ScheduleAdminSelectData extends
    ALAbstractSelectData<PortletEntry, PortletEntry> implements ALData {

  private ALStringField enabled_maps;

  /**
   *
   * @param action
   * @param rundata
   * @param context
   */
  @Override
  public void init(ALAction action, RunData rundata, Context context)
      throws ALPageNotFoundException, ALDBErrorException {
    super.init(action, rundata, context);
    enabled_maps = new ALStringField();
    enabled_maps.setValue(ALConfigService
      .get(ALConfigHandler.Property.SCHEDULE_MAPS_ENABLED));
  }

  /**
   * 一覧データを取得します。 <BR>
   *
   * @param rundata
   * @param context
   * @return
   */
  @Override
  public ResultList<PortletEntry> selectList(RunData rundata, Context context) {
    return null;
  }

  /**
   * ResultData に値を格納して返します。（一覧データ） <BR>
   *
   * @param obj
   * @return
   */
  @Override
  protected Object getResultData(PortletEntry record) {
    return null;
  }

  /**
   * ResultData に値を格納して返します。（詳細データ） <BR>
   *
   * @param obj
   * @return
   */
  @Override
  protected Object getResultDataDetail(PortletEntry record)
      throws ALPageNotFoundException, ALDBErrorException {
    ScheduleAdminDetailResultData rd = new ScheduleAdminDetailResultData(record);
    rd.initField();
    return rd;
  }

  @Override
  public PortletEntry selectDetail(RunData rundata, Context context)
      throws ALDBErrorException, ALPageNotFoundException {
    return CustomizeUtils.getPortletEntry(
      rundata,
      ScheduleUtils.SCHEDULE_PORTLET_NAME);
  }

  /**
   * @return
   */
  @Override
  protected Attributes getColumnMap() {
    return null;
  }

  /**
   * @return
   */
  public String getEnabledMapsFlag() {
    return enabled_maps.toString();
  }
}