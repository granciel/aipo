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
package com.aimluck.eip.message;

import java.util.jar.Attributes;

import org.apache.turbine.util.RunData;
import org.apache.turbine.util.StringUtils;
import org.apache.velocity.context.Context;

import com.aimluck.commons.field.ALStringField;
import com.aimluck.eip.cayenne.om.portlet.EipTMessageRoom;
import com.aimluck.eip.common.ALAbstractSelectData;
import com.aimluck.eip.common.ALDBErrorException;
import com.aimluck.eip.common.ALPageNotFoundException;
import com.aimluck.eip.message.util.MessageUtils;
import com.aimluck.eip.modules.actions.common.ALAction;
import com.aimluck.eip.orm.query.ResultList;
import com.aimluck.eip.services.orgutils.ALOrgUtilsService;
import com.aimluck.eip.util.ALEipUtils;

/**
 *
 */
public class MessageRoomListSelectData extends
    ALAbstractSelectData<EipTMessageRoom, EipTMessageRoom> {

  private int userId;

  private int totalUnreadCount = 0;

  private ALStringField keyword = null;

  @Override
  public void init(ALAction action, RunData rundata, Context context)
      throws ALPageNotFoundException, ALDBErrorException {
    super.init(action, rundata, context);

    userId = ALEipUtils.getUserId(rundata);
  }

  @Override
  public void initField() {
    keyword = new ALStringField();
  }

  /**
   * @param rundata
   * @param context
   * @return
   * @throws ALPageNotFoundException
   * @throws ALDBErrorException
   */
  @Override
  protected ResultList<EipTMessageRoom> selectList(RunData rundata,
      Context context) throws ALPageNotFoundException, ALDBErrorException {
    return MessageUtils.getRoomList(userId, keyword.getValue());
  }

  /**
   * @param rundata
   * @param context
   * @return
   * @throws ALPageNotFoundException
   * @throws ALDBErrorException
   */
  @Override
  protected EipTMessageRoom selectDetail(RunData rundata, Context context)
      throws ALPageNotFoundException, ALDBErrorException {
    return null;
  }

  /**
   * @param model
   * @return
   * @throws ALPageNotFoundException
   * @throws ALDBErrorException
   */
  @Override
  protected Object getResultData(EipTMessageRoom model)
      throws ALPageNotFoundException, ALDBErrorException {
    MessageRoomResultData rd = new MessageRoomResultData();
    rd.initField();
    rd.setAutoName("T".equals(model.getAutoName()));
    rd.setLastMessage(model.getLastMessage());
    Integer userId = model.getUserId();
    if (userId != null) {
      rd.setUserId(userId.longValue());
    }
    boolean isDirect = "O".equals(model.getRoomType());
    if (isDirect && model.getUserId().intValue() < 4) {
      rd.setName(ALOrgUtilsService.getAlias());
    } else {
      rd.setName(isDirect
        ? model.getLastName() + " " + model.getFirstName()
        : model.getName());
    }
    rd.setHasPhoto(isDirect
      ? ("T".equals(model.getUserHasPhoto()) || "N".equals(model
        .getUserHasPhoto()))
      : ("T".equals(model.getHasPhoto()) || "N".equals(model.getHasPhoto())));
    if (rd.isHasPhoto()) {
      rd.setPhotoModified(isDirect ? model.getUserPhotoModified() : model
        .getPhotoModified()
        .getTime());
    }
    rd.setRoomId(model.getRoomId());
    rd.setRoomType(model.getRoomType());
    rd.setUnreadCount(model.getUnreadCount());
    rd.setUpdateDate(model.getLastUpdateDate());

    totalUnreadCount += model.getUnreadCount().intValue();

    return rd;
  }

  /**
   * @param model
   * @return
   * @throws ALPageNotFoundException
   * @throws ALDBErrorException
   */
  @Override
  protected Object getResultDataDetail(EipTMessageRoom model)
      throws ALPageNotFoundException, ALDBErrorException {
    return null;
  }

  /**
   * @return
   */
  @Override
  protected Attributes getColumnMap() {
    return null;
  }

  public int getTotalUnreadCount() {
    return totalUnreadCount;
  }

  /**
   * @param keyword
   *          セットする keyword
   */
  public void setKeyword(String keyword) {
    this.keyword.setValue(keyword);
  }

  /**
   * @return keyword
   */
  public ALStringField getKeyword() {
    return keyword;
  }

  public boolean hasKeyword() {
    return !StringUtils.isEmpty(keyword.getValue());
  }

  public int getLoginUserId() {
    return userId;
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
