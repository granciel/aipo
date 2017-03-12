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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Date;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import javax.mail.internet.MimeUtility;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.jetspeed.om.profile.Entry;
import org.apache.jetspeed.om.profile.Portlets;
import org.apache.jetspeed.om.profile.Profile;
import org.apache.jetspeed.services.logging.JetspeedLogFactoryService;
import org.apache.jetspeed.services.logging.JetspeedLogger;
import org.apache.jetspeed.services.resources.JetspeedResources;
import org.apache.jetspeed.services.rundata.JetspeedRunData;
import org.apache.jetspeed.util.template.JetspeedLink;
import org.apache.jetspeed.util.template.JetspeedLinkFactory;
import org.apache.turbine.services.upload.TurbineUpload;
import org.apache.turbine.util.DynamicURI;
import org.apache.turbine.util.RunData;

import com.aimluck.eip.services.portal.ALPortalApplicationService;

/**
 *
 */
public class ALCommonUtils {

  /** logger */
  private static final JetspeedLogger logger = JetspeedLogFactoryService
    .getLogger(ALCommonUtils.class.getName());

  /** 乱数生成用アルゴリズム（SHA1） */
  public static final String DEF_RANDOM_ALGORITHM = "SHA1PRNG";

  /** 乱数生成用アルゴリズム（生成するバイト配列の長さ） */
  public static final int DEF_RANDOM_LENGTH = 16;

  /** 乱数生成機保持用 */
  private static SecureRandom random = getSecureRandom();

  private static String CACHE_BUST = null;

  public static String escapeXML(String string) {
    return StringEscapeUtils.escapeXml(string);
  }

  public static String escapeXML(DynamicURI uri) {
    return StringEscapeUtils.escapeXml(uri.toString());
  }

  public static String getCloudUrl() {
    return JetspeedResources.getString("aipo.cloud.url");
  }

  /**
   * * 長いアルファベットのテキストを自動的に折り返すヘルパー
   *
   * @subpackage helper
   * @param string
   *          $text
   * @param int $step
   * @return string
   */
  public static String replaceToAutoCRString(String str) {
    if (str == null || "".equals(str)) {
      return "";
    }
    StringBuffer res = new StringBuffer("");
    int step = 4;
    int size = str.length();
    int count = size / step;
    int j;
    for (int i = 0; i < count; i++) {
      j = i * step;
      res.append(str.substring(j, j + step)).append("<wbr/>");
    }
    if (count * step < size) {
      res.append(str.substring(count * step));
    }
    return res.toString();
  }

  public static int getMaxFileSize() {
    return getMaxFileSize("MB");
  }

  public static int getMaxFileSize(String unit) {
    int size = TurbineUpload.getSizeMax();
    if (unit.equals("MB")) {
      size = size / 1024 / 1024;
    } else if (unit.equals("KB")) {
      size = size / 1024;
    }
    return size;
  }

  /**
   * * 長いアルファベットのテキストを自動的に折り返すヘルパー
   *
   * @subpackage helper
   * @param string
   *          $text
   * @param int $step
   * @return string
   */
  public static String replaceToAutoCRChild(String str) {
    if (str == null || "".equals(str)) {
      return "";
    }

    StringBuffer res = null;
    String head, body, tail;
    int findex = str.indexOf("&");
    int lindex = str.indexOf(";");
    if ((findex == -1) && (lindex == -1)) {
      return replaceToAutoCRString(str);
    } else if (lindex != -1 && (findex == -1 || findex > lindex)) {
      // ";"のみ含まれる場合
      head = str.substring(0, lindex);
      body = str.substring(lindex, lindex + 1);
      tail = str.substring(lindex + 1);
      res = new StringBuffer();
      res.append(replaceToAutoCRString(head));
      res.append(body);
      res.append(replaceToAutoCR(tail));
    } else if (findex != -1 && lindex == -1) {
      // "&"のみ含まれる場合
      head = str.substring(0, findex);
      body = str.substring(findex, findex + 1);
      tail = str.substring(findex + 1);
      res = new StringBuffer();
      res.append(replaceToAutoCRString(head));
      res.append(body);
      res.append(replaceToAutoCR(tail));
    } else {
      head = str.substring(0, findex);
      body = str.substring(findex, lindex + 1);
      tail = str.substring(lindex + 1);
      res = new StringBuffer();
      res.append(replaceToAutoCRString(head));
      res.append(body);
      res.append(replaceToAutoCR(tail));
    }

    return res.toString();
  }

  /**
   * * 長いアルファベットのテキストを自動的に折り返すヘルパー
   *
   * @subpackage helper
   * @param string
   *          $text
   * @param int $step
   * @return string
   */
  public static String replaceToAutoCR(String str) {
    if (str == null || "".equals(str)) {
      return "";
    }

    StringBuffer res = null;
    String head, body, tail;
    int findex = str.indexOf("<");
    int lindex = str.indexOf(">");
    if ((findex == -1) && (lindex == -1)) {
      return replaceToAutoCRChild(str);
    } else if (findex == -1 || findex > lindex) {
      // ">"のみ含まれる場合
      head = str.substring(0, lindex);
      body = str.substring(lindex, lindex + 1);
      tail = str.substring(lindex + 1);
      res = new StringBuffer();
      res.append(replaceToAutoCRChild(head));
      res.append(body);
      res.append(replaceToAutoCR(tail));
    } else if (lindex == -1) {
      // "<"のみ含まれる場合
      head = str.substring(0, findex);
      body = str.substring(findex, findex + 1);
      tail = str.substring(findex + 1);
      res = new StringBuffer();
      res.append(replaceToAutoCRChild(head));
      res.append(body);
      res.append(replaceToAutoCR(tail));
    } else {
      head = str.substring(0, findex);
      body = str.substring(findex, lindex + 1);
      tail = str.substring(lindex + 1);
      res = new StringBuffer();
      res.append(replaceToAutoCRChild(head));
      res.append(body);
      res.append(replaceToAutoCR(tail));
    }

    return res.toString();
  }

  /**
   * 第二引数で指定した長さで、第一引数の文字列を丸める。
   *
   * @param src
   *          元の文字列
   * @param length
   *          丸めの長さ
   * @return ●処理後の文字列
   */
  public static String compressString(String src, int length) {
    if (src == null || src.length() == 0 || length <= 0) {
      return src;
    }

    String subject;
    if (src.length() > length) {
      subject = src.substring(0, length);
      subject += "・・・";
    } else {
      subject = src;
    }
    return subject;
  }

  /**
   * ランダムなセキュリティIDを生成する。
   *
   * @return string ランダムIDの文字列型
   */
  public static String getSecureRandomString() {
    String res = null;
    MessageDigest md;
    try {
      if (null == random) {
        return null;
      }
      byte b[] = new byte[DEF_RANDOM_LENGTH];
      random.nextBytes(b);

      md = MessageDigest.getInstance("SHA-1");
      md.update(b);
      StringBuffer sb = new StringBuffer();
      for (byte _b : b) {
        sb.append(String.format("%02x", _b));
      }
      res = sb.toString();
    } catch (Exception e) {
      logger.error("ALCommonUtils.getSecureRandomString", e);
      return null;
    }

    return res;
  }

  /**
   * セキュリティID生成用のSecureRandomを生成します。
   *
   * @return random セキュリティID生成用のSecureRandom
   */
  public static SecureRandom getSecureRandom() {
    SecureRandom random = null;
    try {
      random = SecureRandom.getInstance(DEF_RANDOM_ALGORITHM);
      byte seed[] = random.generateSeed(DEF_RANDOM_LENGTH);
      random.setSeed(seed);
    } catch (Exception e) {
      logger.error("ALCommonUtils.getSecureRandom", e);
      return null;
    }
    return random;
  }

  /**
   * 画像表示用の整数をランダムで生成します。
   *
   * @return
   */
  public int getImageRandomNumber() {
    SecureRandom random = new SecureRandom();
    return (random.nextInt() * 100);
  }

  /**
   * URL文字列にスキーマ部分が含まれていなかった場合、先頭に付加した物を返します
   *
   * @param url
   * @return
   */
  public static String normalizeURL(String url) {
    String res = url;
    if (!res.contains("://")) {
      res = "http://" + res;
    }
    return res;
  }

  /**
   * 指定したエントリー名を持つ個人設定ページに含まれるポートレットへの URI を取得する．
   *
   * @param rundata
   * @param portletEntryName
   *          PSML ファイルに記述されているタグ entry の要素 parent
   * @return
   */
  public static DynamicURI getPortletURIinPersonalConfigPane(RunData rundata,
      String portletEntryName) {
    try {
      Portlets portlets =
        ((JetspeedRunData) rundata).getProfile().getDocument().getPortlets();
      if (portlets == null) {
        return null;
      }

      Portlets[] portletList = portlets.getPortletsArray();
      if (portletList == null) {
        return null;
      }

      int length = portletList.length;
      for (int i = 0; i < length; i++) {
        Entry[] entries = portletList[i].getEntriesArray();
        if (entries == null || entries.length <= 0) {
          continue;
        }

        int ent_length = entries.length;
        for (int j = 0; j < ent_length; j++) {
          if (entries[j].getParent().equals(portletEntryName)) {
            JetspeedLink jsLink = JetspeedLinkFactory.getInstance(rundata);

            DynamicURI duri =
              jsLink.getLink(
                JetspeedLink.CURRENT,
                null,
                null,
                JetspeedLink.CURRENT,
                null);
            duri =
              duri
                .addPathInfo(
                  JetspeedResources.PATH_PANEID_KEY,
                  portletList[i].getId() + "," + entries[j].getId())
                .addQueryData(
                  JetspeedResources.PATH_ACTION_KEY,
                  "controls.Restore");
            return duri;
          }
        }
      }
    } catch (Exception ex) {
      logger.error("ALCommonUtils.getPortletURIinPersonalConfigPane", ex);
      return null;
    }
    return null;
  }

  /**
   * 指定したエントリー名を持つ個人設定ページに含まれるポートレットへの URI を取得する．
   *
   * @param rundata
   * @param portletEntryName
   *          PSML ファイルに記述されているタグ entry の要素 parent
   * @return
   */
  // これで本当に合ってるかはよくわかってない
  public static DynamicURI getPortletURIinPersonalConfigPeid(RunData rundata,
      String portletEntryName) {
    try {
      Profile profile = ((JetspeedRunData) rundata).getProfile();
      if (profile == null) {
        return null;
      }

      Portlets portlets = profile.getDocument().getPortlets();
      if (portlets == null) {
        return null;
      }

      Portlets[] portletList = portlets.getPortletsArray();
      if (portletList == null) {
        return null;
      }

      int length = portletList.length;
      for (int i = 0; i < length; i++) {
        Entry[] entries = portletList[i].getEntriesArray();
        if (entries == null || entries.length <= 0) {
          continue;
        }

        int ent_length = entries.length;
        for (int j = 0; j < ent_length; j++) {
          if (entries[j].getParent().equals(portletEntryName)) {
            JetspeedLink jsLink = JetspeedLinkFactory.getInstance(rundata);

            DynamicURI duri =
              jsLink.getLink(
                JetspeedLink.CURRENT,
                null,
                null,
                JetspeedLink.CURRENT,
                null);
            duri =
              duri.addPathInfo(JetspeedResources.PATH_PORTLETID_KEY, entries[j]
                .getId());
            return duri;
          }
        }
      }
    } catch (Exception ex) {
      logger.error("ALCommonUtils.getPortletURIinPersonalConfigPeid", ex);
      return null;
    }
    return null;
  }

  public static String getCacheBust() {
    if (CACHE_BUST == null) {
      File file = new File(JetspeedResources.getString("aipo.cached.file"));
      long lastModified = file.lastModified();
      CACHE_BUST = String.valueOf(lastModified);
    }
    return CACHE_BUST;
  }

  public static boolean isAndroidBrowser(RunData rundata) {
    return ALEipUtils.isAndroidBrowser(rundata);
  }

  public static boolean isAndroid2Browser(RunData rundata) {
    return ALEipUtils.isAndroid2Browser(rundata);
  }

  /**
   * 指定したポートレットが利用可能かどうかを返す
   *
   * @param portletName
   *
   * @return
   */
  public static boolean isActive(String portletName) {
    return ALPortalApplicationService.isActive(portletName);
  }

  // :HACK
  // can't be called...
  // public static String getl10nFormat(String key, Object... values) {
  // return ALLocalizationUtils.getl10nFormat(key, values);
  // }

  public static String getl10nFormat1(String key, Object values) {
    return ALLocalizationUtils.getl10nFormat(key, values);
  }

  public static String replaceToTelLink(String tel) {
    if (!StringUtils.isEmpty(tel)) {
      return tel.replaceAll("-", "");
    } else {
      return "";
    }
  }

  public static long getCurrentTime() {
    return new Date().getTime();
  }

  public static boolean isFileUploadable(RunData rundata) {
    return ALEipUtils.isFileUploadable(rundata);
  }

  public static boolean hasAttachmentInsertAuthority(RunData rundata) {
    return ALAccessControlUtils.hasAttachmentInsertAuthority(rundata);
  }

  public static boolean hasAttachmentDeleteAuthority(RunData rundata) {
    return ALAccessControlUtils.hasAttachmentDeleteAuthority(rundata);
  }

  public static byte[] encrypt(String key, String text) {
    try {
      SecretKeySpec sksSpec = new SecretKeySpec(key.getBytes(), "Blowfish");
      Cipher cipher = Cipher.getInstance("Blowfish");
      cipher.init(javax.crypto.Cipher.ENCRYPT_MODE, sksSpec);
      return cipher.doFinal(text.getBytes());
    } catch (Throwable t) {
      logger.error(t.getMessage(), t);
    }
    return null;
  }

  public static String decrypt(String key, byte[] encrypted) {
    try {
      SecretKeySpec sksSpec = new SecretKeySpec(key.getBytes(), "Blowfish");
      Cipher cipher = Cipher.getInstance("Blowfish");
      cipher.init(Cipher.DECRYPT_MODE, sksSpec);
      return new String(cipher.doFinal(encrypted));
    } catch (Throwable t) {
      logger.error(t.getMessage(), t);
    }
    return null;
  }

  public static String encodeBase64(byte[] data) {

    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    OutputStream os = null;
    try {
      os = MimeUtility.encode(bos, "base64");
      os.write(data);
    } catch (Throwable t) {
      logger.error(t.getMessage(), t);
    } finally {
      if (os != null) {
        try {
          os.close();
        } catch (IOException e) {
          logger.error(e.getMessage(), e);
        }
      }
    }

    try {
      return bos.toString("iso-8859-1");
    } catch (UnsupportedEncodingException e) {
      logger.error(e.getMessage(), e);
    }

    return null;
  }

  public static byte[] decodeBase64(String base64) {

    byte[] buf = new byte[1024];
    ByteArrayOutputStream os = new ByteArrayOutputStream();

    InputStream is = null;
    try {
      is =
        MimeUtility.decode(
          new ByteArrayInputStream(base64.getBytes()),
          "base64");

      for (int len = -1; (len = is.read(buf)) != -1;) {
        os.write(buf, 0, len);
      }
    } catch (Throwable t) {
      if (is != null) {
        try {
          is.close();
        } catch (IOException e) {
          logger.error(e.getMessage(), e);
        }
      }
    }

    return os.toByteArray();
  }
}
