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
package com.aimluck.eip.util;

import org.apache.jetspeed.services.logging.JetspeedLogFactoryService;
import org.apache.jetspeed.services.logging.JetspeedLogger;
import org.apache.turbine.services.TurbineServices;
import org.apache.turbine.util.RunData;

import com.aimluck.eip.services.accessctl.ALAccessControlConstants;
import com.aimluck.eip.services.accessctl.ALAccessControlFactoryService;
import com.aimluck.eip.services.accessctl.ALAccessControlHandler;

/**
 *
 */
public class ALAccessControlUtils {

  /** logger */
  @SuppressWarnings("unused")
  private static final JetspeedLogger logger = JetspeedLogFactoryService
    .getLogger(ALAccessControlUtils.class.getName());

  /**
   * ファイルアップロードのアクセス権限をチェックします。
   *
   * @return
   */
  public static boolean hasAttachmentInsertAuthority(RunData rundata) {
    return doCheckAttachmentAclPermission(
      rundata,
      ALAccessControlConstants.VALUE_ACL_INSERT);
  }

  /**
   * ファイルアップロードのアクセス権限をチェックします。
   *
   * @return
   */
  public static boolean hasAttachmentDeleteAuthority(RunData rundata) {
    return doCheckAttachmentAclPermission(
      rundata,
      ALAccessControlConstants.VALUE_ACL_DELETE);
  }

  /**
   * ファイルアップロードのアクセス権限をチェックします。
   *
   * @return
   */
  protected static boolean doCheckAttachmentAclPermission(RunData rundata,
      int defineAclType) {

    if (defineAclType == 0) {
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
}
