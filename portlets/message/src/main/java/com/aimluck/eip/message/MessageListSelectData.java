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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.Attributes;

import javax.activation.DataHandler;
import javax.activation.FileDataSource;

import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;

import com.aimluck.commons.field.ALDateTimeField;
import com.aimluck.commons.field.ALStringField;
import com.aimluck.eip.cayenne.om.portlet.EipTMessage;
import com.aimluck.eip.cayenne.om.portlet.EipTMessageFile;
import com.aimluck.eip.cayenne.om.portlet.EipTMessageRoom;
import com.aimluck.eip.common.ALAbstractSelectData;
import com.aimluck.eip.common.ALDBErrorException;
import com.aimluck.eip.common.ALPageNotFoundException;
import com.aimluck.eip.fileupload.beans.FileuploadBean;
import com.aimluck.eip.fileupload.util.FileuploadUtils;
import com.aimluck.eip.message.util.MessageUtils;
import com.aimluck.eip.modules.actions.common.ALAction;
import com.aimluck.eip.orm.Database;
import com.aimluck.eip.orm.query.Operations;
import com.aimluck.eip.orm.query.ResultList;
import com.aimluck.eip.orm.query.SelectQuery;
import com.aimluck.eip.util.ALEipUtils;

/**
 *
 */
public class MessageListSelectData extends
    ALAbstractSelectData<EipTMessage, EipTMessage> {

  public static final int MESSAGE_LIMIT = 50;

  private int cursor = 0;

  private boolean latest = false;

  private int targetUserId;

  private int userId;

  private int lastMessageId;

  private boolean jump = false;

  private EipTMessageRoom room;

  private ALStringField keyword;

  private boolean isSearch = false;

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
  protected ResultList<EipTMessage> selectList(RunData rundata, Context context)
      throws ALPageNotFoundException, ALDBErrorException {
    if (targetUserId > 0) {
      return new ResultList<EipTMessage>(
        new ArrayList<EipTMessage>(),
        1,
        MESSAGE_LIMIT,
        0);
    }
    try {
      cursor = rundata.getParameters().getInt("c");
    } catch (Throwable ignore) {
      // ignore
    }
    try {
      int param = rundata.getParameters().getInt("latest");
      latest = (param == 1);
    } catch (Throwable ignore) {
      // ignore
    }
    try {
      int param = rundata.getParameters().getInt("jump");
      jump = (param == 1);
    } catch (Throwable ignore) {
      // ignore
    }

    List<Integer> roomIds = new ArrayList<Integer>(1);
    if (isSearch) {
      List<Integer> roomIds2 = MessageUtils.getRoomIds(userId);
      if (roomIds2 != null && roomIds2.size() > 0) {
        roomIds.addAll(roomIds2);
      } else {
        roomIds.add(-1);
      }
    } else {
      roomIds.add(room.getRoomId());
    }
    if (jump) {
      return MessageUtils.getMessageJumpList(room.getRoomId(), cursor);
    } else {
      return MessageUtils.getMessageList(
        roomIds,
        keyword.getValue(),
        cursor,
        MESSAGE_LIMIT,
        latest);
    }
  }

  /**
   * @param rundata
   * @param context
   * @return
   * @throws ALPageNotFoundException
   * @throws ALDBErrorException
   */
  @Override
  protected EipTMessage selectDetail(RunData rundata, Context context)
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
  protected Object getResultData(EipTMessage model)
      throws ALPageNotFoundException, ALDBErrorException {
    MessageResultData rd = new MessageResultData();
    rd.initField();
    rd.setMessageId(model.getMessageId());
    rd.setRoomId(model.getRoomId());
    rd.setUserId(model.getUserId());
    rd.setFirstName(model.getFirstName());
    rd.setLastName(model.getLastName());
    rd.setHasPhoto("T".equals(model.getHasPhoto())
      || "N".equals(model.getHasPhoto()));
    rd.setPhotoModified(model.getPhotoModified());
    rd.setMemberCount(model.getMemberCount());
    rd.setUnreadCount(model.getUnreadCount());
    rd.setMessage(model.getMessage());
    rd.setKeyword(keyword.getValue());
    rd.setCreateDate(model.getCreateDate());
    rd.setOwner(model.getUserId().intValue() == userId);
    if (model.getMessageId().intValue() > lastMessageId) {
      lastMessageId = model.getMessageId().intValue();
    }
    return rd;
  }

  @Override
  public boolean doViewList(ALAction action, RunData rundata, Context context) {
    boolean res = super.doViewList(action, rundata, context);

    List<Object> result = getList();
    List<Integer> parentIds = new ArrayList<Integer>(result.size());
    for (Object obj : result) {
      MessageResultData rd = (MessageResultData) obj;
      parentIds.add((int) rd.getMessageId().getValue());
    }

    Map<Integer, List<FileuploadBean>> filesMap = getFiles(parentIds);

    for (Object obj : result) {
      MessageResultData rd = (MessageResultData) obj;
      List<FileuploadBean> list =
        filesMap.get((int) rd.getMessageId().getValue());
      if (list != null) {
        rd.setAttachmentFileList(list);
      }
    }

    return res;
  }

  protected Map<Integer, List<FileuploadBean>> getFiles(List<Integer> parentIds) {
    if (parentIds == null || parentIds.size() == 0 || !hasAttachmentAuthority()) {
      return new HashMap<Integer, List<FileuploadBean>>();
    }
    SelectQuery<EipTMessageFile> query = Database.query(EipTMessageFile.class);
    query.where(Operations.in(EipTMessageFile.MESSAGE_ID_PROPERTY, parentIds));

    query.orderAscending(EipTMessageFile.UPDATE_DATE_PROPERTY);
    query.orderAscending(EipTMessageFile.FILE_PATH_PROPERTY);

    List<EipTMessageFile> list = query.fetchList();
    Map<Integer, List<FileuploadBean>> result =
      new HashMap<Integer, List<FileuploadBean>>(parentIds.size());
    for (EipTMessageFile model : list) {
      Integer id = model.getMessageId();
      List<FileuploadBean> rdList = result.get(id);
      if (rdList == null) {
        rdList = new ArrayList<FileuploadBean>();
      }

      String realname = model.getFileName();
      DataHandler hData = new DataHandler(new FileDataSource(realname));

      FileuploadBean filebean = new FileuploadBean();
      filebean.setFileId(model.getFileId().intValue());
      filebean.setFileName(realname);
      if (hData != null) {
        filebean.setContentType(hData.getContentType());
      }
      filebean.setIsImage(FileuploadUtils.isImage(realname));
      rdList.add(filebean);
      result.put(id, rdList);
    }

    return result;
  }

  /**
   * @param model
   * @return
   * @throws ALPageNotFoundException
   * @throws ALDBErrorException
   */
  @Override
  protected Object getResultDataDetail(EipTMessage model)
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

  /**
   *
   * @param room
   */
  public void setRoom(EipTMessageRoom room) {
    this.room = room;
  }

  public int getRoomId() {
    return room.getRoomId();
  }

  public int getUserId() {
    return userId;
  }

  /**
   * @return lastMessageId
   */
  public int getLastMessageId() {
    return lastMessageId;
  }

  /**
   * @param targetUserId
   *          セットする targetUserId
   */
  public void setTargetUserId(int targetUserId) {
    this.targetUserId = targetUserId;
  }

  /**
   * @return targetUserId
   */
  public int getTargetUserId() {
    return targetUserId;
  }

  public boolean isFirstView() {
    return cursor == 0 && !latest;
  }

  public boolean isMatch(int id1, long id2) {
    return id1 == (int) id2;
  }

  /**
   * @return isSearch
   */
  public boolean isSearch() {
    return isSearch;
  }

  /**
   * @param isSearch
   *          セットする isSearch
   */
  public void setSearch(boolean isSearch) {
    this.isSearch = isSearch;
  }

  /**
   * @param keyword
   */
  public void setKeyword(String keyword) {
    this.keyword.setValue(keyword);
  }

  public ALStringField getKeyword() {
    return keyword;
  }

  public boolean isSameDate(ALDateTimeField a, ALDateTimeField b) {
    return a.getYear().equals(b.getYear())
      && a.getMonth().equals(b.getMonth())
      && a.getDay().equals(b.getDay());
  }

  public boolean isAdmin() {
    if (room == null) {
      return false;
    }
    return MessageUtils.hasAuthorityRoom(room, userId);
  }

  public boolean isDirect() {
    if (room == null) {
      return false;
    }
    return "O".equals(room.getRoomType());
  }
}
