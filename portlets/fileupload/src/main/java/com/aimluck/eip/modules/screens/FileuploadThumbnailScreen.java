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
package com.aimluck.eip.modules.screens;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;

import org.apache.jetspeed.services.logging.JetspeedLogFactoryService;
import org.apache.jetspeed.services.logging.JetspeedLogger;
import org.apache.turbine.modules.screens.RawScreen;
import org.apache.turbine.services.TurbineServices;
import org.apache.turbine.util.RunData;

import com.aimluck.eip.common.ALPermissionException;
import com.aimluck.eip.services.accessctl.ALAccessControlConstants;
import com.aimluck.eip.services.accessctl.ALAccessControlFactoryService;
import com.aimluck.eip.services.accessctl.ALAccessControlHandler;
import com.aimluck.eip.util.ALEipUtils;

/**
 * サムネイル画像を画像データとして出力するクラスです。 <br />
 *
 */
public class FileuploadThumbnailScreen extends RawScreen {

  /** logger */
  private static final JetspeedLogger logger = JetspeedLogFactoryService
    .getLogger(FileuploadThumbnailScreen.class.getName());

  /** ファイル名 */
  private String fileName = null;

  /** サムネイル画像 */
  private byte[] file = null;

  private Date lastModified = null;

  /**
   *
   * @param rundata
   * @return
   */
  @Override
  protected String getContentType(RunData rundata) {
    return "image/jpeg";
  }

  /**
   * ファイル名
   *
   * @return
   */
  protected String getFileName() {
    return fileName;
  }

  /**
   * ファイル名
   *
   * @return
   */
  protected byte[] getFile() {
    return file;
  }

  protected void setFileName(String fileName) {
    this.fileName = fileName;
  }

  protected void setFile(byte[] file) {
    this.file = file;
  }

  protected void setLastModified(Date date) {
    this.lastModified = date;
  }

  protected Date getLastModified() {
    return this.lastModified;
  }

  /**
   *
   * @param rundata
   * @throws Exception
   */
  @Override
  protected void doOutput(RunData rundata) throws Exception {
    ServletOutputStream out = null;
    try {
      doCheckAttachmentAclPermission(
        rundata,
        ALAccessControlConstants.VALUE_ACL_EXPORT);

      HttpServletResponse response = rundata.getResponse();

      long sec = 60 * 60 * 24 * 30;
      response.setHeader("Cache-Control", "private, max-age=" + sec);

      Calendar calendar = Calendar.getInstance();
      calendar.add(Calendar.DATE, 30);
      Date expired = calendar.getTime();
      response.setDateHeader("Expires ", expired.getTime());

      if (this.lastModified != null) {
        response.setDateHeader("Last-Modified", this.lastModified.getTime());
      }
      // ファイル内容の出力
      out = response.getOutputStream();

      byte[] file = getFile();
      if (file == null) {
        // サムネイル画像がデータベースに保存されていない場合
        throw new FileNotFoundException();
      }
      out.write(file);
      out.flush();

    } catch (Exception e) {
      logger.error("FileuploadThumbnailScreen.doOutput", e);
    } finally {
      try {
        if (out != null) {
          out.close();
        }
      } catch (IOException ex) {
        logger.error("FileuploadThumbnailScreen.doOutput", ex);
      }
    }
  }

  protected boolean doCheckAclPermission(RunData rundata, String pfeature,
      int defineAclType) throws ALPermissionException {
    ALAccessControlFactoryService aclservice =
      (ALAccessControlFactoryService) ((TurbineServices) TurbineServices
        .getInstance()).getService(ALAccessControlFactoryService.SERVICE_NAME);
    ALAccessControlHandler aclhandler = aclservice.getAccessControlHandler();
    if (!aclhandler.hasAuthority(
      ALEipUtils.getUserId(rundata),
      pfeature,
      defineAclType)) {
      throw new ALPermissionException();
    }
    return true;
  }

  /**
   * ファイルのアクセス権限をチェックします。
   *
   * @return
   */
  protected boolean doCheckAttachmentAclPermission(RunData rundata,
      int defineAclType) throws ALPermissionException {

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
    if (!aclhandler.hasAuthority(
      ALEipUtils.getUserId(rundata),
      ALAccessControlConstants.POERTLET_FEATURE_ATTACHMENT,
      defineAclType)) {
      throw new ALPermissionException();
    }
    return true;
  }

  /**
   * ファイルアクセス権限チェック用メソッド。<br />
   * ファイルのアクセス権限をチェックするかどうかを判定します。
   *
   * @return
   */
  public boolean isCheckAttachmentAuthority() {
    return true;
  }
}
