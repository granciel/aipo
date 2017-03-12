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
package com.aimluck.eip.cabinet;

import java.util.ArrayList;
import java.util.List;

import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.jetspeed.services.logging.JetspeedLogFactoryService;
import org.apache.jetspeed.services.logging.JetspeedLogger;
import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;

import com.aimluck.eip.cabinet.util.CabinetUtils;
import com.aimluck.eip.cayenne.om.portlet.EipTCabinetFile;
import com.aimluck.eip.common.ALAbstractCheckList;
import com.aimluck.eip.orm.Database;
import com.aimluck.eip.orm.query.SelectQuery;
import com.aimluck.eip.services.accessctl.ALAccessControlConstants;
import com.aimluck.eip.services.eventlog.ALEventlogConstants;
import com.aimluck.eip.services.eventlog.ALEventlogFactoryService;
import com.aimluck.eip.services.storage.ALStorageService;
import com.aimluck.eip.util.ALLocalizationUtils;

/**
 * 共有フォルダのファイルの複数削除を行うためのクラスです。 <BR>
 *
 */
public class CabinetFileMultiDelete extends ALAbstractCheckList {

  /** logger */
  private static final JetspeedLogger logger = JetspeedLogFactoryService
    .getLogger(CabinetFileMultiDelete.class.getName());

  /**
   *
   * @param rundata
   * @param context
   * @param values
   * @param msgList
   * @return
   */
  @Override
  protected boolean action(RunData rundata, Context context,
      List<String> values, List<String> msgList) {
    try {
      boolean hasAttachmentDeleteAuthority =
        doCheckAttachmentAclPermission(
          rundata,
          context,
          ALAccessControlConstants.VALUE_ACL_DELETE);
      if (!hasAttachmentDeleteAuthority) {
        msgList
          .add(ALAccessControlConstants.DEF_ATTACHMENT_PERMISSION_ERROR_STR);
        return false;
      }

      SelectQuery<EipTCabinetFile> query =
        Database.query(EipTCabinetFile.class);
      Expression exp =
        ExpressionFactory.inDbExp(EipTCabinetFile.FILE_ID_PK_COLUMN, values);
      query.setQualifier(exp);

      List<EipTCabinetFile> filelist = query.fetchList();
      if (filelist == null || filelist.size() == 0) {
        return false;
      }

      for (int i = 0; i < filelist.size(); i++) {
        if (!CabinetUtils.isEditableFolder(
          filelist.get(i).getFolderId(),
          rundata)) {
          msgList.add(ALLocalizationUtils
            .getl10n("CABINET_DONOT_AUTHORITY_FILE"));
          return false;
        }

        List<String> fpaths = new ArrayList<String>();
        for (i = 0; i < filelist.size(); i++) {
          EipTCabinetFile cabinetfile = filelist.get(i);
          fpaths.add(cabinetfile.getFilePath());
        }

        // ファイルを削除

        int filelistsize = filelist.size();
        for (i = 0; i < filelistsize; i++) {
          EipTCabinetFile file = filelist.get(i);

          // entityIdを取得
          Integer entityId = file.getFileId();
          // file名を取得
          String fileName = file.getFileTitle();

          // fileを削除
          Database.delete(file);
          Database.commit();

          // ログに保存
          ALEventlogFactoryService.getInstance().getEventlogHandler().log(
            entityId,
            ALEventlogConstants.PORTLET_TYPE_CABINET_FILE,
            fileName);
        }
        int fpathSize = fpaths.size();
        if (fpathSize > 0) {
          // ローカルファイルに保存されているファイルを削除する．
          String orgId = Database.getDomainName();
          for (i = 0; i < fpathSize; i++) {
            ALStorageService.deleteFile(CabinetUtils.getSaveDirPath(orgId)
              + fpaths.get(i));
          }
        }
      }
    } catch (Exception ex) {
      Database.rollback();
      logger.error("cabinet", ex);
      return false;
    }
    return true;
  }

  /**
   * アクセス権限チェック用メソッド。<br />
   * アクセス権限を返します。
   *
   * @return
   */
  @Override
  protected int getDefineAclType() {
    return ALAccessControlConstants.VALUE_ACL_DELETE;
  }

  /**
   * アクセス権限チェック用メソッド。<br />
   * アクセス権限の機能名を返します。
   *
   * @return
   */
  @Override
  public String getAclPortletFeature() {
    return ALAccessControlConstants.POERTLET_FEATURE_CABINET_FILE;
  }
}
