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
package com.aimluck.eip.webmail;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.jar.Attributes;

import javax.mail.Message;

import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.commons.lang.StringUtils;
import org.apache.jetspeed.om.security.JetspeedUser;
import org.apache.jetspeed.portal.portlets.VelocityPortlet;
import org.apache.jetspeed.services.logging.JetspeedLogFactoryService;
import org.apache.jetspeed.services.logging.JetspeedLogger;
import org.apache.jetspeed.services.rundata.JetspeedRunData;
import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;

import com.aimluck.eip.cayenne.om.portlet.EipMMailAccount;
import com.aimluck.eip.cayenne.om.portlet.EipTMail;
import com.aimluck.eip.cayenne.om.portlet.EipTMailFolder;
import com.aimluck.eip.common.ALAbstractSelectData;
import com.aimluck.eip.common.ALDBErrorException;
import com.aimluck.eip.common.ALEipConstants;
import com.aimluck.eip.common.ALPageNotFoundException;
import com.aimluck.eip.mail.ALFolder;
import com.aimluck.eip.mail.ALLocalMailMessage;
import com.aimluck.eip.mail.ALMailFactoryService;
import com.aimluck.eip.mail.ALMailHandler;
import com.aimluck.eip.mail.ALMailMessage;
import com.aimluck.eip.mail.ALPop3MailReceiveThread;
import com.aimluck.eip.mail.util.ALMailUtils;
import com.aimluck.eip.modules.actions.common.ALAction;
import com.aimluck.eip.orm.Database;
import com.aimluck.eip.orm.query.ResultList;
import com.aimluck.eip.orm.query.SelectQuery;
import com.aimluck.eip.util.ALCommonUtils;
import com.aimluck.eip.util.ALEipUtils;
import com.aimluck.eip.util.ALLocalizationUtils;
import com.aimluck.eip.webmail.beans.WebmailAccountLiteBean;
import com.aimluck.eip.webmail.util.WebMailUtils;
import com.sk_jp.mail.MailUtility;

/**
 * Webメール検索データを管理するためのクラスです。 <br />
 */
public class WebMailSelectData extends
    ALAbstractSelectData<EipTMail, ALMailMessage> {
  /** logger */
  private static final JetspeedLogger logger = JetspeedLogFactoryService
    .getLogger(WebMailSelectData.class.getName());

  /** 現在選択されているタブ (＝受信メール or 送信メール) */
  private String currentTab = null;

  JetspeedUser user = null;

  /** ユーザーID */
  private int userId = -1;

  /** アカウントID */
  private int accountId = -1;

  /** フォルダID */
  private int folderId = -1;

  /** フォルダに対する未読メール数のマップ */
  private Map<Integer, Integer> unreadMailSumMap;

  /** 最終受信日 */
  private final String finalAccessDateStr = null;

  private String orgId;

  /** メール受信スレッドの状態 */
  private String mailReceiveThreadStatus;

  /** 受信トレイと送信トレイ */
  private ALFolder folder;

  /** 選択されたフォルダ */
  private WebMailFolderResultData selectedFolder;

  /** メールアカウント一覧 */
  private List<WebmailAccountLiteBean> mailAccountList =
    new ArrayList<WebmailAccountLiteBean>();

  /** メールフォルダ一覧 */
  private List<WebMailFolderResultData> mailFolderList =
    new ArrayList<WebMailFolderResultData>();

  /**
   * 
   * @param action
   * @param rundata
   * @param context
   */
  @Override
  public void init(ALAction action, RunData rundata, Context context)
      throws ALPageNotFoundException, ALDBErrorException {
    String sort = ALEipUtils.getTemp(rundata, context, LIST_SORT_STR);
    if (sort == null || sort.equals("")) {
      ALEipUtils.setTemp(rundata, context, LIST_SORT_STR, ALEipUtils
        .getPortlet(rundata, context)
        .getPortletConfig()
        .getInitParameter("p2a-sort"));
    }

    orgId = Database.getDomainName();
    userId = ALEipUtils.getUserId(rundata);
    user = (JetspeedUser) ((JetspeedRunData) rundata).getUser();

    // メール受信スレッドの状態を取得
    mailReceiveThreadStatus =
      ALEipUtils.getTemp(rundata, context, "start_recieve");
    ALEipUtils.removeTemp(rundata, context, "start_recieve");

    String tabParam = rundata.getParameters().getString("tab");
    currentTab = ALEipUtils.getTemp(rundata, context, "tab");
    if (tabParam == null && currentTab == null) {
      ALEipUtils.setTemp(rundata, context, "tab", WebMailUtils.TAB_RECEIVE);
      currentTab = WebMailUtils.TAB_RECEIVE;
    } else if (tabParam != null) {
      ALEipUtils.setTemp(rundata, context, "tab", tabParam);
      currentTab = tabParam;
    }

    // 自ポートレットからのリクエストであれば、パラメータを展開しセッションに保存する。
    if (ALEipUtils.isMatch(rundata, context)) {

      // アカウントID
      if (rundata.getParameters().containsKey(WebMailUtils.ACCOUNT_ID)) {
        ALEipUtils.setTemp(rundata, context, WebMailUtils.ACCOUNT_ID, rundata
          .getParameters()
          .getString(WebMailUtils.ACCOUNT_ID));
      }

      // フォルダID
      if (rundata.getParameters().containsKey(WebMailUtils.FOLDER_ID)) {
        ALEipUtils.setTemp(rundata, context, WebMailUtils.FOLDER_ID, rundata
          .getParameters()
          .getString(WebMailUtils.FOLDER_ID));
      }

    } else {
      String _accountId =
        ALEipUtils.getTemp(rundata, context, WebMailUtils.ACCOUNT_ID);
      if (StringUtils.isEmpty(_accountId) || !StringUtils.isNumeric(_accountId)) {
        ALEipUtils.setTemp(
          rundata,
          context,
          WebMailUtils.ACCOUNT_ID,
          ALEipUtils
            .getPortlet(rundata, context)
            .getPortletConfig()
            .getInitParameter("p3a-accounts"));
      }
    }

    try {
      accountId =
        Integer.parseInt(ALEipUtils.getTemp(
          rundata,
          context,
          WebMailUtils.ACCOUNT_ID));
    } catch (Exception ignore) {
      accountId = 0;
    }

    try {
      folderId =
        Integer.parseInt(ALEipUtils.getTemp(
          rundata,
          context,
          WebMailUtils.FOLDER_ID));
    } catch (Exception e) {
      folderId = 0;
    }

    // accountIdが消されてしまっていないかチェックする
    if (!validateAccountId()) {
      accountId = 0;
    }

    // アカウントIDが取得できなかったとき、デフォルトのアカウントIDを取得する
    if (accountId == 0) {
      try {
        Expression exp =
          ExpressionFactory.matchExp(EipMMailAccount.USER_ID_PROPERTY, userId);
        SelectQuery<EipMMailAccount> query =
          Database.query(EipMMailAccount.class, exp);

        query.select(EipMMailAccount.ACCOUNT_ID_PK_COLUMN);
        List<EipMMailAccount> accounts = query.fetchList();
        if (accounts != null && accounts.size() > 0) {
          EipMMailAccount account = accounts.get(0);
          accountId = account.getAccountId();
          ALEipUtils.setTemp(rundata, context, WebMailUtils.ACCOUNT_ID, Integer
            .toString(accountId));
        } else {
          // アカウントが一つも見つからなかった
          return;
        }
      } catch (Exception e) {
      }
    }

    // アカウントを取得
    EipMMailAccount account = ALMailUtils.getMailAccount(userId, accountId);
    if (account == null) {
      action.addErrorMessage(ALLocalizationUtils
        .getl10nFormat("WEBMAIL_NO_ACCOUNT"));
      return;
    }

    // 現在選択中のフォルダを取得
    EipTMailFolder mailFolder =
      WebMailUtils.getEipTMailFolder(account, String.valueOf(folderId));

    // フォルダが取得できなかったとき、アカウントに紐付いたデフォルトのフォルダIDを取得する
    if (mailFolder == null) {
      folderId = account.getDefaultFolderId();

      // セッションにセット
      ALEipUtils.setTemp(rundata, context, WebMailUtils.FOLDER_ID, String
        .valueOf(folderId));

      // 再取得
      mailFolder =
        WebMailUtils.getEipTMailFolder(account, String.valueOf(folderId));
    }

    selectedFolder = new WebMailFolderResultData(mailFolder);

    // フォルダリストを取得
    mailFolderList = WebMailUtils.getMailFolderAll(account);

    // 現在選択しているタブが受信トレイか送信トレイか
    if (accountId > 0) {
      int type_mail =
        (WebMailUtils.TAB_RECEIVE.equals(currentTab))
          ? ALFolder.TYPE_RECEIVE
          : ALFolder.TYPE_SEND;
      ALMailHandler handler =
        ALMailFactoryService.getInstance().getMailHandler();
      folder =
        handler.getALFolder(type_mail, orgId, userId, Integer
          .valueOf(accountId));
      folder.setRowsNum(super.getRowsNum());
    }

    loadUnreadMailSumMap(rundata, context);

    super.init(action, rundata, context);

    // ソート対象が日時だった場合、ソート順を逆にする．
    if ("date".equals(ALEipUtils.getTemp(rundata, context, LIST_SORT_STR))) {
      String sort_type =
        ALEipUtils.getTemp(rundata, context, LIST_SORT_TYPE_STR);
      if (sort_type == null || sort_type.equals("")) {
        ALEipUtils.setTemp(
          rundata,
          context,
          LIST_SORT_TYPE_STR,
          ALEipConstants.LIST_SORT_TYPE_DESC);
      }
    }
  }

  private boolean validateAccountId() {
    for (WebmailAccountLiteBean bean : mailAccountList) {
      if (accountId == (int) bean.getAccountId().getValue()) {
        return true;
      }
    }
    return false;
  }

  /**
   * 
   * @param rundata
   * @param context
   */
  public void loadMailAccountList(RunData rundata, Context context) {
    mailAccountList = WebMailUtils.getMailAccountList(rundata, context);
  }

  /**
   * メールの一覧を取得する．
   * 
   * @param rundata
   * @param context
   * @return
   */
  @Override
  protected ResultList<EipTMail> selectList(RunData rundata, Context context) {
    try {
      if (folder == null) {
        return null;
      }

      loadUnreadMailSumMap(rundata, context);
      return folder.getIndexRows(rundata, context);
    } catch (Exception ex) {
      logger.error("webmail", ex);
      return null;
    }
  }

  /**
   * フォルダごとの未読メール数を取得<br>
   * ・フォルダの切り替え、受信送信タブの移動、ソート時には未読メール数をセッションから取得する<br>
   * ・メールのフォルダ間移動、メール詳細画面を出した後は未読メール数をデータベースから取得する<br>
   * ・セッションが空の場合は未読メール数をデータベースから取得する
   * 
   * @param rundata
   * @param context
   */
  private void loadUnreadMailSumMap(RunData rundata, Context context) {
    String unreadMailSumMapString =
      ALEipUtils.getTemp(rundata, context, WebMailUtils.UNREAD_MAIL);
    if ((rundata.getParameters().containsKey("noupdateunread")
      || rundata.getParameters().containsKey("sort") || rundata
      .getParameters()
      .containsKey("tab"))
      && unreadMailSumMapString != null
      && !rundata.getParameters().containsKey("updateunread")) {
      // セッションから得た文字列をHashMapに再構成
      unreadMailSumMap =
        WebMailUtils.getUnreadMailSumMapFromString(unreadMailSumMapString);
    } else {
      // セッションが空か、パラメータが指定されていなければ取得しなおす
      unreadMailSumMap =
        WebMailUtils.getUnreadMailNumberMap(rundata, userId, accountId);
    }
    // セッションに保存
    ALEipUtils.setTemp(
      rundata,
      context,
      WebMailUtils.UNREAD_MAIL,
      unreadMailSumMap.toString());
  }

  /**
   * 
   * @param rundata
   * @param context
   * @return
   */
  @Override
  protected ALMailMessage selectDetail(RunData rundata, Context context) {
    String mailid =
      ALEipUtils.getTemp(rundata, context, ALEipConstants.ENTITY_ID);

    if (mailid == null || Integer.valueOf(mailid) == null) {
      // Mail IDが空の場合
      logger.debug("[Mail] Empty ID...");
      return null;
    }
    return folder.getMail(Integer.valueOf(mailid));
  }

  /**
   * ResultDataを取得する（メールの一覧） <BR>
   * 
   * 
   */
  @Override
  protected Object getResultData(EipTMail record) {

    WebMailIndexRowResultData rd = new WebMailIndexRowResultData();
    rd.initField();

    rd.setMailId(record.getMailId().toString());

    String isRead = record.getReadFlg();
    if ("T".equals(isRead)) {
      rd.setRead(true);
      rd.setReadImage("themes/"
        + getTheme()
        + "/images/icon/webmail_readmail.gif");
      rd.setReadImageDescription("既読");
      rd.setRead(true);
    } else {
      rd.setRead(false);
      rd.setReadImage("themes/"
        + getTheme()
        + "/images/icon/webmail_unreadmail.gif");
      rd.setReadImageDescription("未読");
      rd.setRead(false);
    }

    String subject = ALMailUtils.decodeSubject(record.getSubject());
    rd.setSubject(ALCommonUtils.compressString(subject, getStrLength()));

    rd.setPerson(MailUtility.decodeText(record.getPerson()));

    rd.setDate(record.getEventDate());
    rd.setFileVolume(record.getFileVolume().toString());

    boolean hasAttachments = ("T".equals(record.getHasFiles()));

    if (hasAttachments) {
      rd.setWithFilesImage("images/webmail/webmail_withfiles.gif");
      rd.setWithFilesImageDescription("添付有");
    }
    rd.hasAttachments(hasAttachments);

    return rd;
  }

  /**
   * 
   * @param obj
   * @return
   */
  @Override
  protected Object getResultDataDetail(ALMailMessage obj) {
    WebMailResultData rd = null;
    try {
      ALLocalMailMessage msg = (ALLocalMailMessage) obj;

      String date = "";

      Date sentDate = msg.getSentDate();
      if (sentDate == null) {
        date = "Unknown";
      } else {
        date = ALMailUtils.translateDate(sentDate);
      }
      rd = new WebMailResultData();
      rd.initField();
      rd.setHeaders(msg.getHeaderArray());
      rd.setSubject(msg.getSubject());
      rd.setFrom(ALMailUtils.getFromDelegate(msg));
      rd.setTo(ALMailUtils.getAddressString(msg.getRecipients(
        Message.RecipientType.TO,
        false)));
      rd.setDate(date);
      rd.setBody(msg.getBodyText());
      if (hasAttachmentAuthority()) {
        rd.setAttachmentFileNames(msg.getAttachmentFileNameArray());
      }
    } catch (Exception e) {
      logger.error("webmail", e);
    }
    return rd;
  }

  /**
   * 現在選択されているタブを取得します。 <BR>
   * 
   * @return
   */
  public String getCurrentTab() {
    return currentTab;
  }

  /**
   * 現在のアカウントが持つメールフォルダを取得します。
   * 
   * @return
   */
  public List<WebMailFolderResultData> getFolderList() {
    return mailFolderList;
  }

  /**
   * @return
   * 
   */
  @Override
  protected Attributes getColumnMap() {
    return null;
  }

  /**
   * 
   * @return
   */
  public List<WebmailAccountLiteBean> getMailAccountList() {
    return mailAccountList;
  }

  /**
   * 現在選択中のアカウントIDを取得します。
   * 
   * @return
   */
  public int getAccountId() {
    return accountId;
  }

  public boolean isTheMailAccountSelected(WebmailAccountLiteBean record) {
    return accountId == record.getAccountId().getValue();
  }

  /**
   * 現在選択中のフォルダIDを取得します。
   * 
   * @return
   */
  public int getFolderId() {
    return folderId;
  }

  /**
   * 現在選択中のフォルダを取得します。
   * 
   * @return
   */
  public WebMailFolderResultData getSelectedFolder() {
    return selectedFolder;
  }

  /**
   * フォルダ別未読メール数を取得する。
   * 
   * @return
   */
  public int getUnReadMailSumByFolderId(Integer folder_id) {
    int result = 0;
    try {
      if (folder_id != null) {
        if (unreadMailSumMap.containsKey(folder_id)) {
          result = unreadMailSumMap.get(folder_id);
        }
      }
    } catch (Exception e) {
      logger.error("[WebMailSelectData]", e);
      result = 0;
    }
    return result;
  }

  public String getFinalAccessDate() {
    return finalAccessDateStr;
  }

  /**
   * 表示する項目数を取得します。
   * 
   * @return
   */
  @Override
  public int getRowsNum() {
    return folder.getRowsNum();
  }

  /**
   * 総件数を取得します。
   * 
   * @return
   */
  @Override
  public int getCount() {
    return folder.getCount();
  }

  /**
   * 総ページ数を取得します。
   * 
   * @return
   */
  @Override
  public int getPagesNum() {
    return folder.getPagesNum();
  }

  /**
   * 現在表示されているページを取得します。
   * 
   * @return
   */
  @Override
  public int getCurrentPage() {
    return folder.getCurrentPage();
  }

  /**
   * 
   * @return
   */
  @Override
  public String getCurrentSort() {
    return folder.getCurrentSort();
  }

  /**
   * 
   * @return
   */
  @Override
  public String getCurrentSortType() {
    return folder.getCurrentSortType();
  }

  /**
   * @return
   */
  @Override
  public int getStart() {
    return folder.getStart();
  }

  /**
   * 
   * @param id
   * @return
   */
  public boolean isMatch(int id1, long id2) {
    return id1 == (int) id2;
  }

  public String getStatStr() {
    return ALPop3MailReceiveThread.getReceiveMailResultStr(
      user,
      accountId,
      mailReceiveThreadStatus);
  }

  public void setFiltersPSML(VelocityPortlet portlet, Context context,
      RunData rundata) {
    ALEipUtils.setTemp(rundata, context, WebMailUtils.ACCOUNT_ID, portlet
      .getPortletConfig()
      .getInitParameter("p3a-accounts"));
  }

}
