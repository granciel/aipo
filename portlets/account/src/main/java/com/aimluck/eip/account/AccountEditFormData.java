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
package com.aimluck.eip.account;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;

import org.apache.jetspeed.services.JetspeedSecurity;
import org.apache.jetspeed.services.logging.JetspeedLogFactoryService;
import org.apache.jetspeed.services.logging.JetspeedLogger;
import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;

import com.aimluck.commons.field.ALStringField;
import com.aimluck.commons.utils.ALStringUtil;
import com.aimluck.eip.account.util.AccountUtils;
import com.aimluck.eip.cayenne.om.security.TurbineUser;
import com.aimluck.eip.common.ALAbstractFormData;
import com.aimluck.eip.common.ALBaseUser;
import com.aimluck.eip.common.ALDBErrorException;
import com.aimluck.eip.common.ALEipConstants;
import com.aimluck.eip.common.ALEipManager;
import com.aimluck.eip.common.ALPageNotFoundException;
import com.aimluck.eip.fileupload.beans.FileuploadLiteBean;
import com.aimluck.eip.fileupload.util.FileuploadMinSizeException;
import com.aimluck.eip.fileupload.util.FileuploadUtils;
import com.aimluck.eip.fileupload.util.FileuploadUtils.ShrinkImageSet;
import com.aimluck.eip.modules.actions.common.ALAction;
import com.aimluck.eip.orm.Database;
import com.aimluck.eip.services.datasync.ALDataSyncFactoryService;
import com.aimluck.eip.services.eventlog.ALEventlogConstants;
import com.aimluck.eip.services.eventlog.ALEventlogFactoryService;
import com.aimluck.eip.services.storage.ALStorageService;
import com.aimluck.eip.util.ALEipUtils;
import com.aimluck.eip.util.ALLocalizationUtils;

/**
 * ユーザーアカウントのフォームデータを管理するためのクラスです。 <br />
 */
public class AccountEditFormData extends ALAbstractFormData {

  /** logger */
  private static final JetspeedLogger logger = JetspeedLogFactoryService
    .getLogger(AccountEditFormData.class.getName());

  /** ブラウザに表示するデフォルトのパスワード（ダミーパスワード） */
  private static final String DEFAULT_VIEW_PASSWORD = "******";

  /** プロフィール画像バリデートのサイズ(横幅) */
  public static final int DEF_PHOTO_VALIDATE_WIDTH = 200;

  /** プロフィール画像バリデートのサイズ(縦幅) */
  public static final int DEF_PHOTO_VALIDATE_HEIGHT = 200;

  /** ログイン名 */
  private ALStringField loginname;

  /** 名前（名） */
  private ALStringField firstname;

  /** 名前（姓） */
  private ALStringField lastname;

  /** 新しいパスワード */
  private ALStringField new_password;

  /** 新しいパスワード（確認用） */
  private ALStringField new_password_confirm;

  /** メールアドレス */
  private ALStringField email;

  /** 電話番号（内線） */
  private ALStringField in_telephone;

  /** 電話番号 */
  private ALStringField out_telephone1;

  /** 電話番号 */
  private ALStringField out_telephone2;

  /** 電話番号 */
  private ALStringField out_telephone3;

  /** 電話番号（携帯） */
  private ALStringField cellular_phone1;

  /** 電話番号（携帯） */
  private ALStringField cellular_phone2;

  /** 電話番号（携帯） */
  private ALStringField cellular_phone3;

  /** 携帯メールアドレス */
  private ALStringField cellular_mail;

  /** フリガナ（名） */
  private ALStringField first_name_kana;

  /** フリガナ（姓） */
  private ALStringField last_name_kana;

  /** 部署名リスト */
  private final List<ALStringField> post_name_list =
    new ArrayList<ALStringField>();

  /** 役職 */
  private ALStringField position_name;

  /** 社員コード */
  private ALStringField code;

  /** 顔写真 */
  private ALStringField photo = null;

  private boolean photo_vali_flag = false;

  /** 添付ファイル */
  private FileuploadLiteBean filebean = null;

  /** 添付フォルダ名 */
  private String folderName = null;

  private String orgId;

  /** 顔写真データ */
  private byte[] facePhoto;

  /** 顔写真データ */
  private byte[] facePhoto_smartphone;

  /** パスワード変更の可否．変更する場合は，false． */
  private boolean dontUpdatePasswd = false;

  /** 登録済顔写真削除 */
  private boolean delete_photo = false;

  private boolean isNewPhotoSpec = false;

  /**
   * 初期化処理を行います。 <BR>
   *
   * @param action
   * @param rundata
   * @param context
   */
  @Override
  public void init(ALAction action, RunData rundata, Context context)
      throws ALPageNotFoundException, ALDBErrorException {
    super.init(action, rundata, context);

    if (ALEipUtils.isMatch(rundata, context)) {
      ALEipUtils.setTemp(rundata, context, ALEipConstants.ENTITY_ID, rundata
        .getUser()
        .getUserName());
    }

    folderName = rundata.getParameters().getString("folderName");

    orgId = Database.getDomainName();
    delete_photo = rundata.getParameters().getBoolean("delete_photo", false);
  }

  /**
   *
   *
   */
  @Override
  public void initField() {
    // ログイン名
    loginname = new ALStringField();
    loginname.setFieldName(ALLocalizationUtils
      .getl10nFormat("ACCOUNT_LOGIN_NAME"));
    loginname.setTrim(true);
    // 名
    firstname = new ALStringField();
    firstname.setFieldName(ALLocalizationUtils
      .getl10nFormat("ACCOUNT_LASTNAME"));
    firstname.setTrim(true);
    // 姓
    lastname = new ALStringField();
    lastname.setFieldName(ALLocalizationUtils
      .getl10nFormat("ACCOUNT_FIRSTNAME"));
    lastname.setTrim(true);
    // 新しいパスワード
    new_password = new ALStringField();
    new_password
      .setFieldName(ALLocalizationUtils.getl10nFormat("ACCOUNT_PASS"));
    new_password.setTrim(true);

    // 新しいパスワード（確認用）
    new_password_confirm = new ALStringField();
    new_password_confirm.setFieldName(ALLocalizationUtils
      .getl10nFormat("ACCOUNT_PASSWORDCONFIRMMSG_NOT_INDENTION"));
    new_password_confirm.setTrim(true);

    // メールアドレス
    email = new ALStringField();
    email.setFieldName(ALLocalizationUtils.getl10nFormat("ACCOUNT_USER_EMAIL"));
    email.setTrim(true);
    // 内線番号
    in_telephone = new ALStringField();
    in_telephone.setFieldName(ALLocalizationUtils
      .getl10nFormat("ACCOUNT_IN_TELEPHONE"));
    in_telephone.setTrim(true);
    // 外線番号
    out_telephone1 = new ALStringField();
    out_telephone1.setFieldName(ALLocalizationUtils
      .getl10nFormat("ACCOUNT_OUT_TELEPHONE"));
    out_telephone1.setTrim(true);
    out_telephone2 = new ALStringField();
    out_telephone2.setFieldName(ALLocalizationUtils
      .getl10nFormat("ACCOUNT_OUT_TELEPHONE"));
    out_telephone2.setTrim(true);
    out_telephone3 = new ALStringField();
    out_telephone3.setFieldName(ALLocalizationUtils
      .getl10nFormat("ACCOUNT_OUT_TELEPHONE"));
    out_telephone3.setTrim(true);

    // 携帯番号
    cellular_phone1 = new ALStringField();
    cellular_phone1.setFieldName(ALLocalizationUtils
      .getl10nFormat("ACCOUNT_USER_CELLULAR_PHONE"));
    cellular_phone1.setTrim(true);
    cellular_phone2 = new ALStringField();
    cellular_phone2.setFieldName(ALLocalizationUtils
      .getl10nFormat("ACCOUNT_USER_CELLULAR_PHONE"));
    cellular_phone2.setTrim(true);
    cellular_phone3 = new ALStringField();
    cellular_phone3.setFieldName(ALLocalizationUtils
      .getl10nFormat("ACCOUNT_USER_CELLULAR_PHONE"));
    cellular_phone3.setTrim(true);
    // 携帯アドレス
    cellular_mail = new ALStringField();
    cellular_mail.setFieldName(ALLocalizationUtils
      .getl10nFormat("ACCOUNT_USER_CELLULAR_MAIL"));
    cellular_mail.setTrim(true);
    // 名（フリガナ）
    first_name_kana = new ALStringField();
    first_name_kana.setFieldName(ALLocalizationUtils
      .getl10nFormat("ACCOUNT_LAST_NAME_SPELL1"));
    first_name_kana.setTrim(true);
    // 姓（フリガナ）
    last_name_kana = new ALStringField();
    last_name_kana.setFieldName(ALLocalizationUtils
      .getl10nFormat("ACCOUNT_FIRST_NAME_SPELL1"));
    last_name_kana.setTrim(true);

    // 役職
    position_name = new ALStringField();
    position_name.setFieldName(ALLocalizationUtils
      .getl10nFormat("ACCOUNT_POSITION"));
    position_name.setTrim(true);

    // 社員コード
    code = new ALStringField();
    code.setFieldName(ALLocalizationUtils.getl10nFormat("ACCOUNT_CODE"));
    code.setTrim(true);

    // 顔写真
    photo = new ALStringField();
    photo.setFieldName(ALLocalizationUtils.getl10nFormat("ACCOUNT_USER_PHOTO"));
    photo.setTrim(true);
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

    try {
      if (res) {
        List<FileuploadLiteBean> fileBeanList =
          FileuploadUtils.getFileuploadList(rundata);
        if (fileBeanList != null && fileBeanList.size() > 0) {
          filebean = fileBeanList.get(0);
          if (filebean.getFileId() != 0) {
            // 顔写真をセットする．
            String[] acceptExts = ImageIO.getWriterFormatNames();
            facePhoto_smartphone = null;

            ShrinkImageSet bytesShrinkFilebean =
              FileuploadUtils.getBytesShrinkFilebean(
                orgId,
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

            if (bytesShrinkFilebean != null) {
              facePhoto_smartphone = bytesShrinkFilebean.getShrinkImage();
            }

            String[] acceptExts2 = ImageIO.getWriterFormatNames();
            facePhoto = null;
            ShrinkImageSet bytesShrinkFilebean2 =
              FileuploadUtils.getBytesShrinkFilebean(
                orgId,
                folderName,
                ALEipUtils.getUserId(rundata),
                filebean,
                acceptExts2,
                FileuploadUtils.DEF_LARGE_THUMBNAIL_WIDTH,
                FileuploadUtils.DEF_LARGE_THUMBNAIL_HEIGHT,
                msgList,
                false,
                DEF_PHOTO_VALIDATE_WIDTH,
                DEF_PHOTO_VALIDATE_HEIGHT);
            if (bytesShrinkFilebean2 != null) {
              facePhoto = bytesShrinkFilebean2.getShrinkImage();
            }
            isNewPhotoSpec = true;
          } else {
            facePhoto = null;
          }
        }
      }
    } catch (FileuploadMinSizeException ex) {
      // ignore
      photo_vali_flag = true;
    } catch (Exception ex) {
      logger.error("AccountEditFormData.setFormData", ex);
      res = false;
    }
    return res;
  }

  /**
   *
   *
   */
  @Override
  protected void setValidator() {
    // 名
    firstname.setNotNull(true);
    firstname.limitMaxLength(50);
    // 姓
    lastname.setNotNull(true);
    lastname.limitMaxLength(50);
    // 名（フリガナ）
    first_name_kana.setNotNull(true);
    first_name_kana.limitMaxLength(50);
    // 姓（フリガナ）
    last_name_kana.setNotNull(true);
    last_name_kana.limitMaxLength(50);
    // 新しいパスワード
    new_password.setNotNull(true);
    new_password.setCharacterType(ALStringField.TYPE_ASCII);
    new_password.limitMaxLength(16);

    // 新しいパスワード（確認用）
    new_password_confirm.setNotNull(true);
    new_password_confirm.setCharacterType(ALStringField.TYPE_ASCII);
    new_password_confirm.limitMaxLength(16);
    // 内線
    in_telephone.setCharacterType(ALStringField.TYPE_ASCII);
    in_telephone.limitMaxLength(13);
    // メールアドレス
    email.setCharacterType(ALStringField.TYPE_ASCII);

    // 外線
    out_telephone1.setCharacterType(ALStringField.TYPE_NUMBER);
    out_telephone1.limitMaxLength(5);
    out_telephone2.setCharacterType(ALStringField.TYPE_NUMBER);
    out_telephone2.limitMaxLength(4);
    out_telephone3.setCharacterType(ALStringField.TYPE_NUMBER);
    out_telephone3.limitMaxLength(4);

    // 携帯
    cellular_phone1.setCharacterType(ALStringField.TYPE_NUMBER);
    cellular_phone1.limitMaxLength(5);
    cellular_phone2.setCharacterType(ALStringField.TYPE_NUMBER);
    cellular_phone2.limitMaxLength(4);
    cellular_phone3.setCharacterType(ALStringField.TYPE_NUMBER);
    cellular_phone3.limitMaxLength(4);
    // 携帯メール
    cellular_mail.setCharacterType(ALStringField.TYPE_ASCII);
    cellular_mail.limitMaxLength(50);
  }

  /**
   * @param msgList
   * @return
   *
   */
  @Override
  protected boolean validate(List<String> msgList) {
    List<String> dummy = new ArrayList<String>();
    firstname.validate(msgList);
    lastname.validate(msgList);

    // 名前(フリガナ)をカタカナへと変換します
    first_name_kana.setValue(ALStringUtil.convertHiragana2Katakana(ALStringUtil
      .convertH2ZKana(first_name_kana.toString())));
    last_name_kana.setValue(ALStringUtil.convertHiragana2Katakana(ALStringUtil
      .convertH2ZKana(last_name_kana.toString())));
    first_name_kana.validate(msgList);
    last_name_kana.validate(msgList);

    if (!new_password.toString().equals(new_password_confirm.toString())) {
      msgList.add(ALLocalizationUtils.getl10nFormat("ACCOUNT_ALERT_NEWPW"));
    }

    // 内線
    in_telephone.validate(msgList);
    // ハイフン以外の記号とアルファベットの入力をはじきます
    Pattern pattern = Pattern.compile(".*[^-0-9]+.*");
    Matcher matcher = pattern.matcher(in_telephone.getValue());
    Boolean ext_validater = matcher.matches();
    if (ext_validater) {
      msgList.add(ALLocalizationUtils
        .getl10nFormat("ACCOUNT_POST_WITHIN_SIXTEEN"));
    }

    // メールアドレス
    email.validate(msgList);
    if (email.getValue() != null
      && email.getValue().trim().length() > 0
      && !ALStringUtil.isMailAddress(email.getValue())) {
      msgList.add(ALLocalizationUtils.getl10nFormat("ACCOUNT_ALERT_EMAIL"));
    }

    // 携帯メールアドレス
    cellular_mail.validate(msgList);
    if (cellular_mail.getValue().trim().length() > 0
      && !ALStringUtil.isCellPhoneMailAddress(cellular_mail.getValue())) {
      msgList.add(ALLocalizationUtils
        .getl10nFormat("ACCOUNT_ALERT_EMAIL_MOBILE"));
    }

    if (!out_telephone1.getValue().equals("")
      || !out_telephone2.getValue().equals("")
      || !out_telephone3.getValue().equals("")) {
      if (!out_telephone1.validate(dummy)
        || !out_telephone2.validate(dummy)
        || !out_telephone3.validate(dummy)) {
        msgList.add(ALLocalizationUtils
          .getl10nFormat("ACCOUNT_POST_SET_TELEPHON_EXTERNAL_NOMBER"));
      }
    }

    if (!cellular_phone1.getValue().equals("")
      || !cellular_phone2.getValue().equals("")
      || !cellular_phone3.getValue().equals("")) {
      if (!cellular_phone1.validate(dummy)
        || !cellular_phone2.validate(dummy)
        || !cellular_phone3.validate(dummy)) {
        msgList.add(ALLocalizationUtils
          .getl10nFormat("ACCOUNT_ALERT_TELEPHONE_MOBILE"));
      }
    }

    // 顔写真
    if (photo_vali_flag) {
      msgList
        .add(ALLocalizationUtils.getl10nFormat("ACCOUNT_ALERT_PHOTO_SIZE"));
    } else if (filebean != null
      && filebean.getFileId() != 0
      && facePhoto == null) {
      msgList.add(ALLocalizationUtils.getl10nFormat("ACCOUNT_ALERT_PHOTO"));
    }

    // パスワードの確認
    if (ALEipConstants.MODE_INSERT.equals(getMode())) {
      if (!new_password.getValue().equals(new_password_confirm.getValue())) {
        msgList
          .add(ALLocalizationUtils.getl10nFormat("ACCOUNT_ALERT_PASSWORD"));
      } else {
        new_password.validate(msgList);
        new_password_confirm.validate(msgList);
      }
    } else if (ALEipConstants.MODE_UPDATE.equals(getMode())) {
      if (new_password.getValue().equals(DEFAULT_VIEW_PASSWORD)
        && new_password_confirm.getValue().equals(DEFAULT_VIEW_PASSWORD)) {
        dontUpdatePasswd = true;
      } else {
        if (!new_password.getValue().equals(new_password_confirm.getValue())) {
          msgList.add(ALLocalizationUtils
            .getl10nFormat("ACCOUNT_ALERT_PASSWORD"));
        } else {
          new_password.validate(msgList);
          new_password_confirm.validate(msgList);
        }
      }
    }

    return (msgList.size() == 0);
  }

  /**
   * @param rundata
   * @param context
   * @param msgList
   * @return
   */
  @Override
  protected boolean loadFormData(RunData rundata, Context context,
      List<String> msgList) {
    try {
      ALBaseUser user = AccountUtils.getBaseUser(rundata, context);
      if (user == null) {
        logger
          .debug("Not found. (" + AccountEditFormData.class.getName() + ")");
        return false;
      }
      loginname.setValue(user.getUserName());
      firstname.setValue(user.getFirstName());
      lastname.setValue(user.getLastName());
      email.setValue(user.getEmail());
      // 電話番号（内線）
      in_telephone.setValue(user.getInTelephone());
      // 電話番号（外線）
      StringTokenizer token;
      if (user.getOutTelephone() != null) {
        token = new StringTokenizer(user.getOutTelephone(), "-");
        if (token.countTokens() == 3) {
          out_telephone1.setValue(token.nextToken());
          out_telephone2.setValue(token.nextToken());
          out_telephone3.setValue(token.nextToken());
        }
      }
      // 電話番号（携帯）
      if (user.getCellularPhone() != null) {
        token = new StringTokenizer(user.getCellularPhone(), "-");
        if (token.countTokens() == 3) {
          cellular_phone1.setValue(token.nextToken());
          cellular_phone2.setValue(token.nextToken());
          cellular_phone3.setValue(token.nextToken());
        }
      }
      cellular_mail.setValue(user.getCellularMail());
      first_name_kana.setValue(user.getFirstNameKana());
      last_name_kana.setValue(user.getLastNameKana());

      List<ALStringField> postNames =
        ALEipUtils.getPostNameList(Integer.valueOf(user.getUserId()));

      setPostNameList(postNames);

      position_name.setValue(getPositionName(user.getPositionId()));

      code.setValue(user.getCode());

      if (user.getPhoto() != null) {
        filebean = new FileuploadLiteBean();
        filebean.initField();
        filebean.setFolderName("");
        filebean.setFileId(0);
        filebean.setFileName(ALLocalizationUtils
          .getl10nFormat("ACCOUNT_OLD_PHOTO"));
        filebean.setUserId(Integer.parseInt(user.getUserId()));
        filebean.setPhotoModified(String.valueOf(user
          .getPhotoModified()
          .getTime()));
      }

      new_password.setValue(DEFAULT_VIEW_PASSWORD);
      new_password_confirm.setValue(DEFAULT_VIEW_PASSWORD);

      isNewPhotoSpec = "N".equals(user.hasPhotoString());

      return true;
    } catch (Exception e) {
      logger.error("AccountEditFormData.loadFormData", e);
      return false;
    }
  }

  /**
   * メールアドレスを設定します。
   *
   * @return
   */
  public void loadEmail(RunData rundata) {
    try {
      ALBaseUser user = (ALBaseUser) rundata.getUser();
      if (user == null) {
        logger
          .debug("Not found. (" + AccountEditFormData.class.getName() + ")");
        email.setValue("");
      } else {
        email.setValue(user.getEmail());
      }
    } catch (Exception e) {
      logger.error("AccountEditFormData.loadEmail", e);
    }
  }

  /**
   * 未使用。
   *
   * @param rundata
   * @param context
   * @param msgList
   * @return
   */
  @Override
  protected boolean insertFormData(RunData rundata, Context context,
      List<String> msgList) {
    return false;
  }

  /**
   *
   * @param rundata
   * @param context
   * @param msgList
   * @return
   */
  @Override
  protected boolean updateFormData(RunData rundata, Context context,
      List<String> msgList) {
    try {
      // 編集者自身を示すオブジェクト
      ALBaseUser user = AccountUtils.getBaseUser(rundata, context);
      if (user == null) {
        return false;
      }

      // WebAPIのDBへ接続できるか確認
      if (!ALDataSyncFactoryService
        .getInstance()
        .getDataSyncHandler()
        .checkConnect()) {
        msgList.add(ALLocalizationUtils
          .getl10nFormat("ACCOUNT_ALERT_CONNECT_DB_FAILED"));
        return false;
      }

      user.setLastAccessDate();

      user.setFirstName(firstname.getValue());
      user.setLastName(lastname.getValue());
      user.setInTelephone(in_telephone.getValue());
      if (!out_telephone1.getValue().equals("")
        && !out_telephone2.getValue().equals("")
        && !out_telephone3.getValue().equals("")) {
        user.setOutTelephone(new StringBuffer().append(
          out_telephone1.getValue()).append("-").append(
          out_telephone2.getValue()).append("-").append(
          out_telephone3.getValue()).toString());
      } else {
        user.setOutTelephone("");
      }

      if (!cellular_phone1.getValue().equals("")
        && !cellular_phone2.getValue().equals("")
        && !cellular_phone3.getValue().equals("")) {
        user.setCellularPhone(new StringBuffer().append(
          cellular_phone1.getValue()).append("-").append(
          cellular_phone2.getValue()).append("-").append(
          cellular_phone3.getValue()).toString());
      } else {
        user.setCellularPhone("");
      }
      user.setCellularMail(cellular_mail.getValue());
      user.setFirstNameKana(first_name_kana.getValue());
      user.setLastNameKana(last_name_kana.getValue());
      user.setEmail(email.getValue());

      if (filebean != null && filebean.getFileId() != 0) {
        // 顔写真を登録する．
        user.setPhotoSmartphone(facePhoto_smartphone);
        user.setHasPhotoSmartphone("N");
        user.setPhotoModifiedSmartphone(new Date());
        user.setPhoto(facePhoto);
        user.setHasPhoto("N");
        user.setPhotoModified(new Date());
      } else if (delete_photo) {
        user.setPhoto(null);
        user.setHasPhoto("F");
        user.setPhotoSmartphone(null);
        user.setHasPhotoSmartphone("F");

      }

      // 新しいパスワードをセットする
      if (!dontUpdatePasswd) {
        JetspeedSecurity.forcePassword(user, new_password.getValue());
      } else {
        /* パスワード変更日時が空だったときの処置 */
        if (user.getPasswordChanged() == null) {
          user.setPasswordChanged(user.getCreated());
        }
        TurbineUser tuser = Database.get(TurbineUser.class, user.getUserId());
        user.setPassword(tuser.getPasswordValue());
      }

      // ユーザーを更新
      JetspeedSecurity.saveUser(user);

      // WebAPIとのDB同期
      if (!ALDataSyncFactoryService
        .getInstance()
        .getDataSyncHandler()
        .updateUser(user)) {
        return false;
      }
      ALBaseUser currentUser = (ALBaseUser) rundata.getUser();
      if (currentUser.getUserName().equals(user.getUserName())) {
        currentUser.setPassword(user.getPassword());
        currentUser.setHasPhoto(user.hasPhotoString());
        currentUser.setPhotoModified(user.getPhotoModified());
        currentUser.setPasswordChanged(user.getPasswordChanged());
      }

      // イベントログに保存
      ALEventlogFactoryService.getInstance().getEventlogHandler().log(
        Integer.valueOf(user.getUserId()),
        ALEventlogConstants.PORTLET_TYPE_ACCOUNTPERSON,
        null);

      // 一時的な添付ファイルの削除
      ALStorageService.deleteTmpFolder(
        ALEipUtils.getUserId(rundata),
        folderName);

      return true;
    } catch (Exception e) {
      logger.error("AccountEditFormData.updateFormData", e);
      return false;
    }
  }

  /**
   * @param rundata
   * @param context
   * @param msgList
   * @return
   */
  @Override
  protected boolean deleteFormData(RunData rundata, Context context,
      List<String> msgList) {
    return false;
  }

  /**
   * 携帯メールアドレスを取得します。 <BR>
   *
   * @return
   */
  public ALStringField getCellularMail() {
    return cellular_mail;
  }

  /**
   * メールアドレスを取得します。 <BR>
   *
   * @return
   */
  public ALStringField getEmail() {
    return email;
  }

  /**
   * フリガナ（名）を取得します。 <BR>
   *
   * @return
   */
  public ALStringField getFirstNameKana() {
    return first_name_kana;
  }

  /**
   * 名前（名）を取得します。 <BR>
   *
   * @return
   */
  public ALStringField getFirstName() {
    return firstname;
  }

  /**
   * @return
   */
  public ALStringField getNewPassword() {
    return new_password;
  }

  /**
   * @return
   */
  public ALStringField getNewPasswordConfirm() {
    return new_password_confirm;
  }

  /**
   * @param field
   */
  public void setNewPassword(String field) {
    new_password.setValue(field);
  }

  /**
   * @param field
   */
  public void setNewPasswordConfirm(String field) {
    new_password_confirm.setValue(field);
  }

  /**
   * 電話番号（内線）を取得します。 <BR>
   *
   * @return
   */
  public ALStringField getInTelephone() {
    return in_telephone;
  }

  /**
   * フリガナ（姓）を取得します。 <BR>
   *
   * @return
   */
  public ALStringField getLastNameKana() {
    return last_name_kana;
  }

  /**
   * 名前（姓）を取得します。 <BR>
   *
   * @return
   */
  public ALStringField getLastName() {
    return lastname;
  }

  /**
   * 携帯電話番号を取得します。 <BR>
   *
   * @return
   */
  public ALStringField getCellularPhone1() {
    return cellular_phone1;
  }

  /**
   * 携帯電話番号を取得します。 <BR>
   *
   * @return
   */
  public ALStringField getCellularPhone2() {
    return cellular_phone2;
  }

  /**
   * 携帯電話番号を取得します。 <BR>
   *
   * @return
   */
  public ALStringField getCellularPhone3() {
    return cellular_phone3;
  }

  /**
   * 電話番号を取得します。 <BR>
   *
   * @return
   */
  public ALStringField getOutTelephone1() {
    return out_telephone1;
  }

  /**
   * 電話番号を取得します。 <BR>
   *
   * @return
   */
  public ALStringField getOutTelephone2() {
    return out_telephone2;
  }

  /**
   * 電話番号を取得します。 <BR>
   *
   * @return
   */
  public ALStringField getOutTelephone3() {
    return out_telephone3;
  }

  public String getFolderName() {
    return folderName;
  }

  public List<FileuploadLiteBean> getAttachmentFileNameList() {
    if (filebean == null) {
      return null;
    }
    List<FileuploadLiteBean> list = new ArrayList<FileuploadLiteBean>();
    list.add(filebean);
    return list;
  }

  /**
   * 部署を取得します。 <BR>
   *
   * @return
   */
  public List<ALStringField> getPostNameList() {
    return post_name_list;
  }

  public void setPostNameList(List<ALStringField> list) {
    post_name_list.addAll(list);
  }

  /**
   * 役職を取得します。 <BR>
   *
   * @return
   */
  public ALStringField getPositionName() {
    return position_name;
  }

  /**
   * @param loginname
   *          セットする loginname
   */
  public void setLoginName(ALStringField loginname) {
    this.loginname = loginname;
  }

  /**
   * @return loginname
   */
  public ALStringField getLoginName() {
    return loginname;
  }

  /**
   *
   * @param id
   * @return
   */
  private String getPositionName(int id) {
    if (ALEipManager.getInstance().getPositionMap().containsKey(
      Integer.valueOf(id))) {
      return (ALEipManager.getInstance().getPositionMap().get(Integer
        .valueOf(id))).getPositionName().getValue();
    }
    return null;
  }

  public boolean isNewPhotoSpec() {
    return isNewPhotoSpec;
  }

  /**
   * @return code
   */
  public ALStringField getCode() {
    return code;
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
