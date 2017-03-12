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

import java.util.Date;

import org.apache.jetspeed.services.logging.JetspeedLogFactoryService;
import org.apache.jetspeed.services.logging.JetspeedLogger;
import org.apache.turbine.util.RunData;

import com.aimluck.eip.cayenne.om.portlet.EipTMessageRoom;
import com.aimluck.eip.message.util.MessageUtils;
import com.aimluck.eip.util.ALEipUtils;

/**
 *
 */
public class MessageFileuploadRoomPhotoScreen extends FileuploadThumbnailScreen {

  /** logger */
  private static final JetspeedLogger logger = JetspeedLogFactoryService
    .getLogger(MessageFileuploadRoomPhotoScreen.class.getName());

  private static final String KEY_ROOM_ID = "rid";

  /** 画像サイズ */
  private static final String KEY_PHOTO_SIZE = "size";

  /**
   *
   * @param rundata
   * @throws Exception
   */
  @Override
  protected void doOutput(RunData rundata) throws Exception {
    try {
      Integer rid = rundata.getParameters().getInt(KEY_ROOM_ID);
      if (rid == null || rid <= 0) {
        return;
      }

      EipTMessageRoom room = MessageUtils.getRoom(rid);
      if (room == null) {
        return;
      }

      if (!MessageUtils.isJoinRoom(room, ALEipUtils.getUserId(rundata))) {
        return;
      }

      byte[] photo;
      String hasPhoto = room.getHasPhoto();

      // 新仕様（HAS_PHOTO=N）の場合
      if ("N".equals(hasPhoto)) {
        String size = rundata.getParameters().getString(KEY_PHOTO_SIZE);
        if ("large".equals(size)) {
          photo = room.getPhoto();
          if (photo == null) {
            return;
          }
          Date date = room.getPhotoModified();
          if (date != null) {
            super.setLastModified(date);
          }
        } else {
          photo = room.getPhotoSmartphone();
          if (photo == null) {
            return;
          }
          Date date = room.getPhotoModified();
          if (date != null) {
            super.setLastModified(date);
          }
        }
        // 旧仕様（HAS_PHOTO=T）の場合
      } else {
        // すべて getPhoto() で 86x86 をダウンロード
        photo = room.getPhoto();
        if (photo == null) {
          return;
        }
        Date date = room.getPhotoModified();
        if (date != null) {
          super.setLastModified(date);
        }
      }

      super.setFile(photo);
      super.setFileName("photo.jpg");
      super.doOutput(rundata);
    } catch (Exception e) {
      logger.error("MessageFileuploadRoomPhotoScreen.doOutput", e);
    }
  }

  /**
   * ファイルアクセス権限チェック用メソッド。<br />
   * ファイルのアクセス権限をチェックするかどうかを判定します。
   *
   * @return
   */
  @Override
  public boolean isCheckAttachmentAuthority() {
    return false;
  }
}
