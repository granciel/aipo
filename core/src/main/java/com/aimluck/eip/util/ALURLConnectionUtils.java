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

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLProtocolException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 *
 */
public class ALURLConnectionUtils {
  /** User-Agent */
  public static final String USER_AGENT =
    "Mozilla/5.0 (compatible; MSIE 10.0; Windows NT 6.1; WOW64; Trident/6.0; Avant Browser)";

  /** Accept-Encoding */
  public static final String ACCEPT_ENCODING = "identity";

  /**
   * HttpURLConnectionまたはHttpsURLConnectionを返す
   *
   * @param url
   * @return HttpURLConnectionまたはHttpsURLConnection
   * @throws NoSuchAlgorithmException
   * @throws KeyManagementException
   * @throws IOException
   */
  public static HttpURLConnection openUrlConnection(URL connectURL)
      throws NoSuchAlgorithmException, KeyManagementException, IOException {
    HttpURLConnection urlconnection = null;

    // SSLHandshakeExceptionが発生しないか調べる
    urlconnection = (HttpURLConnection) connectURL.openConnection();
    try {
      urlconnection.getResponseMessage();
    } catch (SSLHandshakeException | SSLProtocolException ex) {
      // SSLHandshakeExceptionの場合には証明書をチェックしないhttps接続に切り替える
      if ("https".equals(connectURL.getProtocol())) {
        TrustManager[] tm = { new X509TrustManager() {
          @Override
          public X509Certificate[] getAcceptedIssuers() {
            return null;
          }

          @Override
          public void checkClientTrusted(X509Certificate[] chain,
              String authType) throws CertificateException {
          }

          @Override
          public void checkServerTrusted(X509Certificate[] chain,
              String authType) throws CertificateException {
          }
        } };
        SSLContext sslcontext = SSLContext.getInstance("SSL");
        sslcontext.init(null, tm, null);

        urlconnection = (HttpsURLConnection) connectURL.openConnection();

        ((HttpsURLConnection) urlconnection).setSSLSocketFactory(sslcontext
          .getSocketFactory());
        ((HttpsURLConnection) urlconnection)
          .setHostnameVerifier(new HostnameVerifier() {
            @Override
            public boolean verify(String hostname, SSLSession session) {
              return true;
            }
          });

        return urlconnection;
      }
    }

    // http接続の場合
    urlconnection = (HttpURLConnection) connectURL.openConnection();

    return urlconnection;
  }
}
