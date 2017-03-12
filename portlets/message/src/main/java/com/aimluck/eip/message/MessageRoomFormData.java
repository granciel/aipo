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

import static com.aimluck.eip.util.ALLocalizationUtils.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;

import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.commons.lang.StringUtils;
import org.apache.jetspeed.services.logging.JetspeedLogFactoryService;
import org.apache.jetspeed.services.logging.JetspeedLogger;
import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;

import com.aimluck.commons.field.ALStringField;
import com.aimluck.commons.utils.ALDeleteFileUtil;
import com.aimluck.eip.cayenne.om.portlet.EipTMessageFile;
import com.aimluck.eip.cayenne.om.portlet.EipTMessageRoom;
import com.aimluck.eip.cayenne.om.portlet.EipTMessageRoomMember;
import com.aimluck.eip.cayenne.om.security.TurbineUser;
import com.aimluck.eip.common.ALAbstractFormData;
import com.aimluck.eip.common.ALDBErrorException;
import com.aimluck.eip.common.ALEipConstants;
import com.aimluck.eip.common.ALEipUser;
import com.aimluck.eip.common.ALPageNotFoundException;
import com.aimluck.eip.fileupload.beans.FileuploadLiteBean;
import com.aimluck.eip.fileupload.util.FileuploadMinSizeException;
import com.aimluck.eip.fileupload.util.FileuploadUtils;
import com.aimluck.eip.fileupload.util.FileuploadUtils.ShrinkImageSet;
import com.aimluck.eip.message.util.MessageUtils;
import com.aimluck.eip.modules.actions.common.ALAction;
import com.aimluck.eip.orm.Database;
import com.aimluck.eip.orm.query.SelectQuery;
import com.aimluck.eip.util.ALEipUtils;
import com.aimluck.eip.util.ALLocalizationUtils;

/**
 *
 */
public class MessageRoomFormData extends ALAbstractFormData {

  /** logger */
  private static final JetspeedLogger logger = JetspeedLogFactoryService
    .getLogger(MessageRoomFormData.class.getName());

  /** プロフィール画像バリデートのサイズ(横幅) */
  public static final int DEF_PHOTO_VALIDATE_WIDTH = 200;

  /** プロフィール画像バリデートのサイズ(縦幅) */
  public static final int DEF_PHOTO_VALIDATE_HEIGHT = 200;

  private ALStringField members;

  private ALStringField name;

  private List<ALEipUser> memberList;

  private ALEipUser login_user;

  private boolean login_user_room_auth = true;

  private boolean isDesktopNotification = true;

  private boolean isMobileNotification = true;

  private FileuploadLiteBean filebean = null;

  private String folderName = null;

  private byte[] facePhoto;

  private byte[] facePhoto_smartphone;

  private boolean photo_vali_flag = false;

  private int roomId;

  private boolean isGroup = true;

  /** 1ルームの最大人数 **/
  private final int MAX_ROOM_MEMBER = 300;

  @Override
  public void init(ALAction action, RunData rundata, Context context)
      throws ALPageNotFoundException, ALDBErrorException {
    super.init(action, rundata, context);

    folderName = rundata.getParameters().getString("folderName");

    login_user = ALEipUtils.getALEipUser(rundata);

  }

  /**
   *
   */
  @Override
  public void initField() {
    name = new ALStringField();
    name.setFieldName(getl10n("MESSAGE_ROOM_NAME"));
    members = new ALStringField();
    members.setTrim(true);
    memberList = new ArrayList<ALEipUser>();
  }

  /**
   *
   * @param rundata
   * @param context
   * @param msgList
   * @return
   */
  @Override
  protected boolean setFormData(RunData rundata, Context context,
      List<String> msgList) throws ALPageNotFoundException, ALDBErrorException {

    boolean res = super.setFormData(rundata, context, msgList);

    if (res) {
      try {

        String memberNames[] = rundata.getParameters().getStrings("member_to");
        String memberAuthorities[] =
          rundata.getParameters().getStrings("member_authority_to");
        String desktopNotification =
          rundata.getParameters().getString("desktop_notification");
        String mobileNotification =
          rundata.getParameters().getString("mobile_notification");

        EipTMessageRoom room = MessageUtils.getRoom(rundata, context);
        memberList.clear();
        if (memberNames != null
          && memberNames.length > 0
          && memberAuthorities != null
          && memberNames.length == memberAuthorities.length) {
          SelectQuery<TurbineUser> query = Database.query(TurbineUser.class);
          Expression exp =
            ExpressionFactory.inExp(
              TurbineUser.LOGIN_NAME_PROPERTY,
              memberNames);
          query.setQualifier(exp);
          memberList.addAll(ALEipUtils.getUsersFromSelectQuery(query));
          Map<String, String> authMap = new HashMap<String, String>();
          Map<String, String> desktopNotificationMap =
            new HashMap<String, String>();
          Map<String, String> mobileNotificationMap =
            new HashMap<String, String>();

          for (int i = 0; i < memberNames.length; i++) {
            authMap.put(memberNames[i], memberAuthorities[i]);
          }
          for (ALEipUser member : memberList) {
            member.setAuthority(authMap.get(member.getName().getValue()));
          }

          if (room != null) {
            @SuppressWarnings("unchecked")
            List<EipTMessageRoomMember> roomMemberList =
              room.getEipTMessageRoomMember();

            // ルームメンバーの通知設定をセット
            for (EipTMessageRoomMember member : roomMemberList) {
              desktopNotificationMap.put(member.getLoginName(), member
                .getDesktopNotification());
              mobileNotificationMap.put(member.getLoginName(), member
                .getMobileNotification());
            }
            for (ALEipUser member : memberList) {
              if (desktopNotificationMap.containsKey(member
                .getName()
                .getValue())) {
                member.setDesktopNotification(desktopNotificationMap.get(member
                  .getName()
                  .getValue()));
              }
              if (mobileNotificationMap
                .containsKey(member.getName().getValue())) {
                member.setMobileNotification(mobileNotificationMap.get(member
                  .getName()
                  .getValue()));
              }
            }
          }

          // ログインユーザーの通知設定をセット
          for (ALEipUser member : memberList) {
            if (member.getUserId().getValueWithInt() == login_user
              .getUserId()
              .getValueWithInt()) {
              if (desktopNotification != null
                && !"".equals(desktopNotification)) {
                member.setDesktopNotification(desktopNotification);
              }
              if (mobileNotification != null && !"".equals(mobileNotification)) {
                member.setMobileNotification(mobileNotification);
              }
            }
          }

        }
        roomId = rundata.getParameters().getInteger(ALEipConstants.ENTITY_ID);

        List<FileuploadLiteBean> fileBeanList =
          FileuploadUtils.getFileuploadList(rundata);
        if (fileBeanList != null && fileBeanList.size() > 0) {
          filebean = fileBeanList.get(0);
          if (filebean.getFileId() != 0) {
            String[] acceptExts = ImageIO.getWriterFormatNames();
            facePhoto = null;
            ShrinkImageSet bytesShrinkFilebean =
              FileuploadUtils.getBytesShrinkFilebean(
                Database.getDomainName(),
                folderName,
                ALEipUtils.getUserId(rundata),
                filebean,
                acceptExts,
                FileuploadUtils.DEF_LARGE_THUMBNAIL_WIDTH,
                FileuploadUtils.DEF_LARGE_THUMBNAIL_HEIGHT,
                msgList,
                false,
                DEF_PHOTO_VALIDATE_WIDTH,
                DEF_PHOTO_VALIDATE_HEIGHT);
            if (bytesShrinkFilebean != null) {
              facePhoto = bytesShrinkFilebean.getShrinkImage();
            }
            facePhoto_smartphone = null;
            ShrinkImageSet bytesShrinkFilebean2 =
              FileuploadUtils.getBytesShrinkFilebean(
                Database.getDomainName(),
                folderName,
                ALEipUtils.getUserId(rundata),
                filebean,
                acceptExts,
                FileuploadUtils.DEF_NORMAL_THUMBNAIL_WIDTH,
                FileuploadUtils.DEF_NORMAL_THUMBNAIL_HEIGHT,
                msgList,
                false,
                DEF_PHOTO_VALIDATE_WIDTH,
                DEF_PHOTO_VALIDATE_HEIGHT);
            if (bytesShrinkFilebean2 != null) {
              facePhoto_smartphone = bytesShrinkFilebean2.getShrinkImage();
            }
          } else {
            facePhoto = null;
            facePhoto_smartphone = null;
          }
        }

        if (room != null) {
          login_user_room_auth =
            MessageUtils.hasAuthorityRoom(room, (int) login_user
              .getUserId()
              .getValue());
          if ("O".equals(room.getRoomType())) {
            login_user_room_auth = true;
            isGroup = false;
          }
        }

      } catch (FileuploadMinSizeException ex) {
        // ignore
        photo_vali_flag = true;
      } catch (Exception ex) {
        logger.error("MessageRoomFormData.setFormData", ex);
      }
    }
    return res;
  }

  /**
   * @throws ALPageNotFoundException
   * @throws ALDBErrorException
   */
  @Override
  protected void setValidator() throws ALPageNotFoundException,
      ALDBErrorException {
    name.limitMaxLength(50);

  }

  /**
   * @param msgList
   * @return
   * @throws ALPageNotFoundException
   * @throws ALDBErrorException
   */
  @Override
  protected boolean validate(List<String> msgList)
      throws ALPageNotFoundException, ALDBErrorException {

    // ログインユーザーに権限がない場合、またRoomTypeがOの場合、通知設定以外の値の検証は不要
    if (!login_user_room_auth || !isGroup) {
      return true;
    }

    boolean hasAuthority = false;

    for (ALEipUser user : memberList) {
      if (user.getUserId().getValue() == login_user.getUserId().getValue()) {
        if ("A".equals(user.getAuthority().getValue())) {
          hasAuthority = true;
        }
      }
    }

    if (memberList.size() < 1) {
      msgList.add(getl10n("MESSAGE_VALIDATE_ROOM_MEMBER1"));
    }
    if (!hasAuthority) {
      msgList.add(getl10n("MESSAGE_VALIDATE_ROOM_MEMBER3"));
    }
    if (memberList.size() > MAX_ROOM_MEMBER) {
      msgList.add(ALLocalizationUtils.getl10nFormat(
        "MESSAGE_VALIDATE_ROOM_MEMBER5",
        MAX_ROOM_MEMBER));
    }
    if (photo_vali_flag) {
      msgList.add(ALLocalizationUtils
        .getl10nFormat("MESSAGE_VALIDATE_ROOM_PHOTO_SIZE"));
    }
    name.validate(msgList);
    return msgList.size() == 0;
  }

  /**
   * @param rundata
   * @param context
   * @param msgList
   * @return
   * @throws ALPageNotFoundException
   * @throws ALDBErrorException
   */
  @Override
  protected boolean loadFormData(RunData rundata, Context context,
      List<String> msgList) throws ALPageNotFoundException, ALDBErrorException {
    try {
      EipTMessageRoom room = MessageUtils.getRoom(rundata, context);

      if (room == null) {
        throw new ALPageNotFoundException();
      }

      login_user_room_auth =
        MessageUtils.hasAuthorityRoom(room, (int) login_user
          .getUserId()
          .getValue());

      if ("O".equals(room.getRoomType())) {
        login_user_room_auth = true;
        isGroup = false;
      }

      if ("F".equals(room.getAutoName())) {
        name.setValue(room.getName());
      }
      @SuppressWarnings("unchecked")
      List<EipTMessageRoomMember> members = room.getEipTMessageRoomMember();
      List<String> memberNames = new ArrayList<String>();
      for (EipTMessageRoomMember member : members) {
        memberNames.add(member.getLoginName());

        if (member.getUserId().intValue() == login_user
          .getUserId()
          .getValueWithInt()) {
          if (member.getDesktopNotification() != null
            && member.getMobileNotification() != null) {
            isDesktopNotification = member.getDesktopNotification().equals("A");
            isMobileNotification = member.getMobileNotification().equals("A");
          }
        }
      }
      SelectQuery<TurbineUser> query = Database.query(TurbineUser.class);
      Expression exp =
        ExpressionFactory.inExp(TurbineUser.LOGIN_NAME_PROPERTY, memberNames);
      query.setQualifier(exp);
      memberList.addAll(ALEipUtils.getUsersFromSelectQuery(query));

      List<String> membersHasAuthority = new ArrayList<String>();
      for (EipTMessageRoomMember member : members) {
        if ("A".equals(member.getAuthority())) {
          membersHasAuthority.add(member.getLoginName());
        }
      }
      for (ALEipUser member : memberList) {
        if (membersHasAuthority.contains(member.getName().getValue())) {
          member.setAuthority("A");
        }
      }

      if (room.getPhoto() != null) {
        filebean = new FileuploadLiteBean();
        filebean.initField();
        filebean.setFolderName("");
        filebean.setFileId(0);
        filebean.setPhotoModified(String.valueOf(room
          .getPhotoModified()
          .getTime()));
        filebean.setFileName(ALLocalizationUtils
          .getl10nFormat("MESSAGE_ROOM_OLD_PHOTO"));
      }

      roomId = room.getRoomId();

    } catch (ALPageNotFoundException e) {
      throw e;
    } catch (Throwable t) {
      logger.error("MessageRoomFormData.loadFormData", t);
      return false;
    }

    return true;
  }

  /**
   * @param rundata
   * @param context
   * @param msgList
   * @return
   * @throws ALPageNotFoundException
   * @throws ALDBErrorException
   */
  @Override
  protected boolean insertFormData(RunData rundata, Context context,
      List<String> msgList) throws ALPageNotFoundException, ALDBErrorException {
    try {
      EipTMessageRoom model = Database.create(EipTMessageRoom.class);

      Date now = new Date();

      boolean isFirst = true;
      StringBuilder autoName = new StringBuilder();
      for (ALEipUser user : memberList) {
        EipTMessageRoomMember map =
          Database.create(EipTMessageRoomMember.class);
        int userid = (int) user.getUserId().getValue();
        map.setEipTMessageRoom(model);
        map.setTargetUserId(1);
        map.setUserId(Integer.valueOf(userid));
        map.setLoginName(user.getName().getValue());
        map.setAuthority(user.getAuthority().getValue());
        map.setDesktopNotification(user.getDesktopNotification().getValue());
        map.setMobileNotification(user.getMobileNotification().getValue());
        if (!isFirst) {
          autoName.append(",");
        }
        autoName.append(user.getAliasName().getValue());
        isFirst = false;
      }

      if (StringUtils.isEmpty(name.getValue())) {
        model.setAutoName("T");
        model.setName(autoName.toString());
      } else {
        model.setAutoName("F");
        model.setName(name.getValue());
      }

      model.setRoomType("G");
      model.setLastUpdateDate(now);
      model.setCreateDate(now);
      model.setCreateUserId((int) login_user.getUserId().getValue());
      model.setUpdateDate(now);

      if (filebean != null && filebean.getFileId() != 0) {
        model.setPhotoSmartphone(facePhoto_smartphone);
        model.setPhoto(facePhoto);
        model.setPhotoModified(new Date());
        model.setHasPhoto("N");
      } else {
        model.setHasPhoto("F");
      }

      Database.commit();

      roomId = model.getRoomId();

    } catch (Exception ex) {
      Database.rollback();
      logger.error("MessageRoomFormData.insertFormData", ex);
      return false;
    }

    return true;
  }

  /**
   * @param rundata
   * @param context
   * @param msgList
   * @return
   * @throws ALPageNotFoundException
   * @throws ALDBErrorException
   */
  @Override
  protected boolean updateFormData(RunData rundata, Context context,
      List<String> msgList) throws ALPageNotFoundException, ALDBErrorException {

    try {

      EipTMessageRoom model = MessageUtils.getRoom(rundata, context);

      if (model == null) {
        return false;
      }

      // ログインユーザーに権限がない場合、またRoomTypeがOの場合、通知設定のみ更新
      if (!login_user_room_auth || !isGroup) {
        EipTMessageRoomMember currentMember =
          MessageUtils.getRoomMember(roomId, login_user
            .getUserId()
            .getValueWithInt());
        for (ALEipUser user : memberList) {
          if (user.getUserId().getValueWithInt() == login_user
            .getUserId()
            .getValueWithInt()) {
            currentMember.setDesktopNotification(user
              .getDesktopNotification()
              .getValue());
            currentMember.setMobileNotification(user
              .getMobileNotification()
              .getValue());
          }
        }
      } else {

        Date now = new Date();

        Database.deleteAll(model.getEipTMessageRoomMember());

        boolean isFirst = true;
        StringBuilder autoName = new StringBuilder();
        for (ALEipUser user : memberList) {
          EipTMessageRoomMember map =
            Database.create(EipTMessageRoomMember.class);
          int userid = (int) user.getUserId().getValue();
          map.setEipTMessageRoom(model);
          map.setTargetUserId(1);
          map.setUserId(Integer.valueOf(userid));
          map.setLoginName(user.getName().getValue());
          map.setAuthority(user.getAuthority().getValue());
          map.setDesktopNotification(user.getDesktopNotification().getValue());
          map.setMobileNotification(user.getMobileNotification().getValue());
          if (!isFirst) {
            autoName.append(",");
          }
          autoName.append(user.getAliasName().getValue());
          isFirst = false;
        }

        if (StringUtils.isEmpty(name.getValue())) {
          model.setAutoName("T");
          model.setName(autoName.toString());
        } else {
          model.setAutoName("F");
          model.setName(name.getValue());
        }

        model.setRoomType("G");
        model.setUpdateDate(now);

        if (filebean != null && filebean.getFileId() != 0) {
          model.setPhotoSmartphone(facePhoto_smartphone);
          model.setPhoto(facePhoto);
          model.setPhotoModified(new Date());
          model.setHasPhoto("N");
        }

        if (filebean != null) {
          if (filebean.getFileId() != 0) {
            model.setPhoto(facePhoto);
            model.setPhotoSmartphone(facePhoto_smartphone);
            model.setPhotoModified(new Date());
            model.setHasPhoto("N");
          }
        } else {
          model.setPhoto(null);
          model.setPhotoSmartphone(null);
          model.setPhotoModified(null);
          model.setHasPhoto("F");
        }
      }

      Database.commit();

      roomId = model.getRoomId();

    } catch (Throwable t) {
      Database.rollback();
      logger.error("MessageRoomFormData.updateFormData", t);
      return false;
    }
    return true;
  }

  /**
   * @param rundata
   * @param context
   * @param msgList
   * @return
   * @throws ALPageNotFoundException
   * @throws ALDBErrorException
   */
  @Override
  protected boolean deleteFormData(RunData rundata, Context context,
      List<String> msgList) throws ALPageNotFoundException, ALDBErrorException {
    try {
      EipTMessageRoom model = MessageUtils.getRoom(rundata, context);
      if (model == null) {
        return false;
      }
      if (!MessageUtils.hasAuthorityRoom(model, (int) login_user
        .getUserId()
        .getValue())) {
        msgList.add(getl10n("MESSAGE_VALIDATE_ROOM_AUTHORITY_DENIED"));
        return false;
      }
      List<EipTMessageFile> files =
        MessageUtils.getEipTMessageFilesByRoom(model.getRoomId());

      ALDeleteFileUtil.deleteFiles(
        MessageUtils.FOLDER_FILEDIR_MESSAGE,
        MessageUtils.CATEGORY_KEY,
        files);

      Database.delete(model);
      Database.commit();
    } catch (Throwable t) {
      Database.rollback();
      logger.error("MessageRoomFormData.deleteFormData", t);
      return false;
    }
    return true;
  }

  public ALStringField getName() {
    return name;
  }

  public List<ALEipUser> getMemberList() {
    return memberList;
  }

  public int getRoomId() {
    return roomId;
  }

  public FileuploadLiteBean getFileBean() {
    return filebean;
  }

  public boolean hasAuth() {
    return login_user_room_auth;
  }

  public boolean isDesktopNotification() {
    return isDesktopNotification;
  }

  public boolean isMobileNotification() {
    return isMobileNotification;
  }

  public boolean isGroup() {
    return isGroup;
  }

  public List<FileuploadLiteBean> getAttachmentFileNameList() {
    if (filebean == null) {
      return null;
    }
    ArrayList<FileuploadLiteBean> list = new ArrayList<FileuploadLiteBean>();
    list.add(filebean);
    return list;
  }

  /**
   * ファイルアップロードアクセス権限チェック用メソッド。<br />
   * ファイルアップのアクセス権限をチェックするかどうかを判定します。
   *
   * @return
   */
  @Override
  public boolean isCheckAttachmentAuthority() {
    return false;
  }

}
