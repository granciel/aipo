/*
 * Aipo is a groupware program developed by Aimluck,Inc.
 * Copyright (C) 2004-2008 Aimluck,Inc.
 * http://aipostyle.com/
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package com.aimluck.eip.facilities;

import java.util.List;

import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.jetspeed.services.logging.JetspeedLogFactoryService;
import org.apache.jetspeed.services.logging.JetspeedLogger;
import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;

import com.aimluck.eip.cayenne.om.portlet.EipMFacility;
import com.aimluck.eip.cayenne.om.portlet.EipTScheduleMap;
import com.aimluck.eip.common.ALAbstractCheckList;
import com.aimluck.eip.orm.Database;
import com.aimluck.eip.orm.query.SelectQuery;

/**
 * 施設の複数削除を行うためのクラスです。 <BR>
 * 
 */
public class FacilityMultiDelete extends ALAbstractCheckList {

  /** logger */
  private static final JetspeedLogger logger = JetspeedLogFactoryService
    .getLogger(FacilityMultiDelete.class.getName());

  /**
   * 
   * @param rundata
   * @param context
   * @param values
   * @param msgList
   * @return
   * @see com.aimluck.eip.common.ALAbstractCheckList#action(org.apache.turbine.util.RunData,
   *      org.apache.velocity.context.Context, java.util.ArrayList,
   *      java.util.ArrayList)
   */
  @Override
  protected boolean action(RunData rundata, Context context,
      List<String> values, List<String> msgList) {
    try {

      SelectQuery<EipTScheduleMap> query1 =
        Database.query(EipTScheduleMap.class);
      Expression exp1 =
        ExpressionFactory.inExp(EipTScheduleMap.USER_ID_PROPERTY, values);
      Expression exp2 =
        ExpressionFactory.matchExp(EipTScheduleMap.TYPE_PROPERTY, "F");
      query1.setQualifier(exp1.andExp(exp2));

      List<EipTScheduleMap> slist = query1.fetchList();
      if (slist != null && slist.size() > 0) {
        // 施設のスケジュールを削除
        Database.deleteAll(slist);
      }

      SelectQuery<EipMFacility> query = Database.query(EipMFacility.class);
      Expression exp =
        ExpressionFactory.inDbExp(EipMFacility.FACILITY_ID_PK_COLUMN, values);
      query.setQualifier(exp);

      List<EipMFacility> flist = query.fetchList();
      if (flist == null || flist.size() == 0) {
        return false;
      }

      // 施設を削除
      Database.deleteAll(flist);
      Database.commit();
    } catch (Exception ex) {
      Database.rollback();
      logger.error("Exception", ex);
      return false;
    }
    return true;
  }

}
