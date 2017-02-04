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
package com.aimluck.eip.cayenne.om.portlet;

import org.apache.cayenne.ObjectId;

import com.aimluck.eip.cayenne.om.portlet.auto._EipTExtTimecardSystem;

public class EipTExtTimecardSystem extends _EipTExtTimecardSystem {

  public static final String SYSTEM_NAME_COLUMN = "SYSTEM_NAME";

  public Integer getSystemId() {
    if (getObjectId() != null && !getObjectId().isTemporary()) {
      Object obj = getObjectId().getIdSnapshot().get(SYSTEM_ID_PK_COLUMN);
      if (obj instanceof Long) {
        Long value = (Long) obj;
        return Integer.valueOf(value.intValue());
      } else {
        return (Integer) obj;
      }
    } else {
      return null;
    }
  }

  public void setSystemId(String id) {
    setObjectId(new ObjectId(
      "EipTExtTimecardSystem",
      SYSTEM_ID_PK_COLUMN,
      Integer.valueOf(id)));
  }

  public boolean isDefaultHoliday() {
    return getHolidayOfWeek().charAt(0) == 'A';
  }

  public String getOriginalHolidayOfWeek() {
    return getHolidayOfWeek().substring(1, 10);
  }
}
