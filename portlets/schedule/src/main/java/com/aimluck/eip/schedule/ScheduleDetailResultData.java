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
package com.aimluck.eip.schedule;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.aimluck.commons.field.ALDateTimeField;
import com.aimluck.commons.field.ALStringField;
import com.aimluck.eip.common.ALEipUser;
import com.aimluck.eip.fileupload.beans.FileuploadBean;
import com.aimluck.eip.util.ALCommonUtils;
import com.aimluck.eip.util.ALEipUtils;
import com.aimluck.eip.util.ALLocalizationUtils;

/**
 * 詳細スケジュールの検索データを管理するクラスです。
 *
 */
public class ScheduleDetailResultData extends ScheduleResultData {

  /** <code>user</code> ユーザー */
  private ALEipUser user;

  /** <code>place</code> 場所 */
  private ALStringField place;

  /** <code>note</code> 内容 */
  private ALStringField note;

  /** <code>createUser</code> 登録ユーザー */
  private ALEipUser createUser;

  /** <code>updateUser</code> 更新ユーザー */
  private ALEipUser updateUser;

  /** <code>createDate</code> 作成日 */
  private ALDateTimeField createDate;

  /** <code>updateDate</code> 更新日時 */
  private ALDateTimeField updateDate;

  /** <code>text</code> テキスト */
  private ALStringField text;

  /** 添付ファイルリスト */
  private List<FileuploadBean> attachmentFileList = null;

  /** <code>is_span</code> 期間スケジュールかどうか */
  boolean is_span;

  /** <code>is_limit</code> 期限があるかどうか */
  boolean is_limit;

  /** <code>is_createuser</code> 登録ユーザーかどうか */
  private boolean is_createuser;

  /** <code>mail_flag</code> send mail for update */
  private String mail_flag;

  /*
   *
   */
  @Override
  public void initField() {
    // スーパークラスのメソッドを呼び出します。
    super.initField();
    // 場所
    place = new ALStringField();
    // 内容
    note = new ALStringField();
    note.setTrim(false);
    // テキスト
    text = new ALStringField();
    text.setValue("");
    // 作成日
    createDate =
      new ALDateTimeField(ALLocalizationUtils
        .getl10n("SCHEDULE_SIMPLE_DATE_FORMAT_DAY"));
    // 更新日時
    updateDate =
      new ALDateTimeField(ALLocalizationUtils
        .getl10n("SCHEDULE_SIMPLE_DATE_FORMAT_DAY"));
    // ファイルリスト
    attachmentFileList = new ArrayList<FileuploadBean>();
  }

  /**
   * 内容を取得します。
   *
   * @return
   */
  public String getNote() {
    return ALEipUtils.getMessageList(note.getValue());
  }

  public String getNoteStr() {
    return note.getValue();
  }

  /**
   * 場所を取得します。
   *
   * @return
   */
  @Override
  public ALStringField getPlace() {
    return place;
  }

  // URLエンコード
  public String getPlaceEncode() {
    return URLEncoder.encode(place.toString());
  }

  public String getPlaceHtml() {
    return ALCommonUtils.replaceToAutoCR(place.toString());
  }

  /**
   * 内容を取得します。
   *
   * @param string
   */
  public void setNote(String string) {
    note.setValue(string);
  }

  /**
   * 場所を設定します。
   *
   * @param string
   */
  @Override
  public void setPlace(String string) {
    place.setValue(string);
  }

  /**
   * ユーザーを取得します。
   *
   * @return
   */
  public ALEipUser getUser() {
    return user;
  }

  /**
   * ユーザーを設定します。
   *
   * @param user
   */
  public void setUser(ALEipUser user) {
    this.user = user;
  }

  /**
   * 作成日を取得します。
   *
   * @return
   */
  public ALDateTimeField getCreateDate() {
    return createDate;
  }

  /**
   * 登録ユーザーを取得します。
   *
   * @return
   */
  public ALEipUser getCreateUser() {
    return createUser;
  }

  /**
   * 更新日時を取得します。
   *
   * @return
   */
  public ALDateTimeField getUpdateDate() {
    return updateDate;
  }

  /**
   * 更新ユーザーを取得します。
   *
   * @return
   */
  public ALEipUser getUpdateUser() {
    return updateUser;
  }

  /**
   * 作成日を設定します。
   *
   * @param date
   */
  public void setCreateDate(Date date) {
    createDate.setValue(date);
  }

  /**
   * 登録ユーザーを設定します。
   *
   * @param user
   */
  public void setCreateUser(ALEipUser user) {
    createUser = user;
  }

  /**
   * 更新日時を設定します。
   *
   * @param date
   */
  public void setUpdateDate(Date date) {
    updateDate.setValue(date);
  }

  /**
   * 更新ユーザーを設定します。
   *
   * @param user
   */
  public void setUpdateUser(ALEipUser user) {
    updateUser = user;
  }

  /**
   * ファイルリストを取得します。
   *
   * @return
   */
  @Override
  public List<FileuploadBean> getAttachmentFileList() {
    return attachmentFileList;
  }

  /**
   * ファイルリストを設定します。
   *
   * @param list
   */
  @Override
  public void setAttachmentFiles(List<FileuploadBean> list) {
    attachmentFileList = list;
  }

  /**
   * 期限があるかどうかを取得します。
   *
   * @return is_limit
   */
  public boolean isLimit() {
    return is_limit;
  }

  /**
   * 期限があるかどうかを設定します。
   *
   * @param is_limit
   */
  public void setLimit(boolean is_limit) {
    this.is_limit = is_limit;
  }

  /**
   * 期間スケジュールかどうかを取得します。
   *
   * @return is_span
   */
  public boolean isSpan() {
    return is_span;
  }

  /**
   * 期間スケジュールかどうかを設定します。
   *
   * @param is_span
   */
  public void setSpan(boolean is_span) {
    this.is_span = is_span;
  }

  /**
   * テキストを追加します。
   *
   * @param string
   */
  public void addText(String string) {
    text.setValue(new StringBuffer()
      .append(text.getValue())
      .append(string)
      .toString());
  }

  /**
   * テキストを取得します。
   *
   * @return
   */
  public ALStringField getText() {
    return text;
  }

  /**
   * 登録ユーザーかどうかのフラグ。
   *
   * @return
   */
  public boolean isCreateuser() {
    return is_createuser;
  }

  /**
   * 登録ユーザーかどうかを設定します。
   *
   * @param is_span
   */
  public void setIsCreateuser(boolean is_createuser) {
    this.is_createuser = is_createuser;
  }

  /**
   * 日付を取得します。
   *
   * @return
   */
  public String getDateForCell() {
    if (getStartDate().getValue().equals(getEndDate().getValue())) {
      return getStartDate().toString();
    } else if ((getStartDate().getYear().equals(getEndDate().getYear())
      && getStartDate().getMonth().equals(getEndDate().getMonth()) && getStartDate()
      .getDay()
      .equals(getEndDate().getDay()))
      || isRepeat()) {
      return new StringBuffer()
        .append(getStartDate().toString())
        .append('-')
        .append(getEndDate().toString())
        .toString();
    } else {
      return new StringBuffer()
        .append(getStartDate().toString())
        .append('-')
        .append("24:00")
        .toString();
    }
  }

  public void setMailFlag(String mail_flag) {
    this.mail_flag = mail_flag;
  }

  public String getMailFlag() {
    return mail_flag;
  }

  public String getStartDateFormatSpace() {
    return ALLocalizationUtils.getl10nFormat(
      "SCHEDULE_DATE_FORMAT",
      getStartDate().getYear(),
      getStartDate().getMonth(),
      getStartDate().getDay());
  }

  public String getEndDateFormatSpace() {
    return ALLocalizationUtils.getl10nFormat(
      "SCHEDULE_DATE_FORMAT",
      getEndDate().getYear(),
      getEndDate().getMonth(),
      getEndDate().getDay());
  }

}
