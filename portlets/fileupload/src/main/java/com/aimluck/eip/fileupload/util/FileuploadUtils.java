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
package com.aimluck.eip.fileupload.util;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import javax.activation.MimetypesFileTypeMap;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.io.IOUtils;
import org.apache.jetspeed.services.logging.JetspeedLogFactoryService;
import org.apache.jetspeed.services.logging.JetspeedLogger;
import org.apache.jetspeed.services.resources.JetspeedResources;
import org.apache.turbine.util.RunData;

import com.aimluck.eip.fileupload.beans.FileuploadLiteBean;
import com.aimluck.eip.http.HttpServletRequestLocator;
import com.aimluck.eip.services.accessctl.ALAccessControlConstants;
import com.aimluck.eip.services.storage.ALStorageService;
import com.aimluck.eip.util.ALCommonUtils;
import com.aimluck.eip.util.ALEipUtils;
import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.drew.metadata.jpeg.JpegDirectory;

/**
 * ファイルアップロードのユーティリティクラスです。 <BR>
 *
 */
public class FileuploadUtils {

  /** logger */
  private static final JetspeedLogger logger = JetspeedLogFactoryService
    .getLogger(FileuploadUtils.class.getName());

  /** KEY（アップロードしたファイルID一覧） */
  public static final String KEY_FILEUPLOAD_ID_LIST = "attachments";

  /** KEY（アップロード先のフォルダ名） */
  public static final String KEY_FILEUPLOAD_FODLER_NAME = "folderName";

  /** 添付ファイル名を保存するファイルの拡張子 */
  public static final String EXT_FILENAME = ".txt";

  /** 一時添付ファイル名 */
  public static final String ATTACHMENT_TEMP_FILENAME = "file";

  /** 一時添付ファイル名を記録するファイル名 */
  public static final String ATTACHMENT_TEMP_FILENAME_REMAIND = "file.txt";

  /** 保存用ファイル名フォーマット */
  public static final String DEFAULT_FILENAME_DATE_FORMAT = "yyyyMMddHHmmssSSS";

  /** 新仕様：画像サムネイルのサイズ（横幅） */
  public static final int DEF_NORMAL_THUMBNAIL_WIDTH = 100;

  /** 新仕様：画像サムネイルのサイズ（縦幅） */
  public static final int DEF_NORMAL_THUMBNAIL_HEIGHT = 100;

  /** 新仕様：画像サムネイルのサイズ大（横幅） */
  public static final int DEF_LARGE_THUMBNAIL_WIDTH = 200;

  /** 新仕様：画像サムネイルのサイズ大（縦幅） */
  public static final int DEF_LARGE_THUMBNAIL_HEIGHT = 200;

  /** 旧仕様：画像サムネイルのサイズ（横幅） */
  public static final int DEF_THUMBNAIL_WIDTH = 86;

  /** 旧仕様：画像サムネイルのサイズ（縦幅） */
  public static final int DEF_THUMBNAIL_HEIGHT = 86;

  /** 旧仕様：スマートフォンの画像サムネイルのサイズ（横幅） */
  public static final int DEF_THUMBNAIL_WIDTH_SMARTPHONE = 64;

  /** 旧仕様：スマートフォンの画像サムネイルのサイズ（縦幅） */
  public static final int DEF_THUMBNAIL_HEIGHT_SMARTPHONE = 64;

  /** 現在の添付ファイル数 */
  public static final String KEY_NOW_SIZE = "nsize";

  /** 添付可能数 */
  public static final String KEY_MAX_SIZE = "msize";

  /** デフォルトエンコーディングを表わすシステムプロパティのキー */
  public static final String FILE_ENCODING = JetspeedResources.getString(
    "content.defaultencoding",
    "UTF-8");

  /** アカウントの添付ファイルを一時保管するディレクトリの指定 */
  public static final String FOLDER_TMP_FOR_ATTACHMENT_FILES =
    JetspeedResources.getString("aipo.tmp.fileupload.attachment.directory", "");

  private static final String[] ACCEPT_CONTENT_TYPES = { "application/pdf" };

  public static String getRealFileName(String name) {
    String filename = null;
    int index = name.lastIndexOf("/");
    if (index < 0) {
      index = name.lastIndexOf("\\");
    }
    if (index > 0) {
      filename = name.substring(index + 1, name.length());
    } else {
      filename = name;
    }
    return filename;
  }

  public static List<FileuploadLiteBean> getFileuploadList(RunData rundata) {
    String[] fileids =
      rundata.getParameters().getStrings(KEY_FILEUPLOAD_ID_LIST);
    if (fileids == null) {
      return null;
    }

    List<FileuploadLiteBean> fileNameList = new ArrayList<FileuploadLiteBean>();
    FileuploadLiteBean filebean = null;
    int fileid = 0;
    int length = fileids.length;

    if (length == 1 && fileids[0] != null && !"".equals(fileids[0])) {
      try {
        fileid = Integer.parseInt(fileids[0]);
      } catch (Exception e) {
        return null;
      }

      if (fileid == 0) {
        filebean = new FileuploadLiteBean();
        filebean.initField();
        filebean.setFolderName("photo");
        filebean.setFileName("以前の写真ファイル");
        fileNameList.add(filebean);
        return fileNameList;
      }
    }

    String folderName =
      rundata.getParameters().getString(
        FileuploadUtils.KEY_FILEUPLOAD_FODLER_NAME);
    if (folderName == null || folderName.equals("")) {
      return null;
    }

    for (int i = 0; i < length; i++) {
      if (fileids[i] == null || fileids[i].equals("")) {
        continue;
      }

      try {
        fileid = Integer.parseInt(fileids[i]);
      } catch (Exception e) {
        continue;
      }

      BufferedReader reader = null;
      InputStream file = null;
      try {
        try {
          file =
            ALStorageService.getFile(
              FOLDER_TMP_FOR_ATTACHMENT_FILES,
              ALEipUtils.getUserId(rundata)
                + ALStorageService.separator()
                + folderName,
              fileids[i] + EXT_FILENAME);
        } catch (Exception e) {
          logger.info("Exception", e);
        }
        if (file == null) {
          continue;
        }
        reader = new BufferedReader(new InputStreamReader(file, FILE_ENCODING));
        String line = reader.readLine();
        if (line == null || line.length() <= 0) {
          continue;
        }

        filebean = new FileuploadLiteBean();
        filebean.initField();
        filebean.setFolderName(fileids[i]);
        filebean.setFileId(fileid);
        filebean.setFileName(line);
        fileNameList.add(filebean);

      } catch (Exception e) {
        logger.error("fileupload", e);
      } finally {
        try {
          if (reader == null) {
            continue;
          }
          reader.close();
        } catch (Exception e) {
          logger.error("fileupload", e);
        }
      }
    }

    return fileNameList;
  }

  /**
   *
   * @param org_id
   * @param folderName
   * @param uid
   * @param fileBean
   * @param acceptExts
   *          String[] ext = { ".jpg", ".jpeg" };
   * @param msgList
   * @return
   */
  public static byte[] getBytesFilebean(String org_id, String folderName,
      int uid, FileuploadLiteBean fileBean, String[] acceptExts,
      List<String> msgList) {
    byte[] result = null;
    try {

      String file_name = fileBean.getFileName();

      if (acceptExts != null && acceptExts.length > 0) {
        // 拡張子をチェックする．
        // ファイルのヘッダで識別するとベスト．
        boolean isAccept = false;
        String tmpExt = null;
        int len = acceptExts.length;
        for (int i = 0; i < len; i++) {
          if (!acceptExts[i].startsWith(".")) {
            tmpExt = "." + acceptExts[i];
          }
          if (file_name.toLowerCase().endsWith(tmpExt)) {
            isAccept = true;
          }
        }
        if (!isAccept) {
          // 期待しない拡張子の場合は，null を返す．
          return null;
        }
      }

      InputStream is = null;
      ByteArrayOutputStream output = new ByteArrayOutputStream();
      try {
        is =
          ALStorageService.getFile(FOLDER_TMP_FOR_ATTACHMENT_FILES, uid
            + ALStorageService.separator()
            + folderName, file_name);
        byte b[] = new byte[512];
        int len = -1;
        while ((len = is.read(b, 0, b.length)) != -1) {
          output.write(b, 0, len);
        }
        output.flush();
      } finally {
        if (is != null) {
          is.close();
        }
      }

      result = output.toByteArray();
    } catch (Exception e) {
      logger.error("fileupload", e);
      return null;
    }
    return result;
  }

  /**
   * 縮小した画像のバイナリを返す。
   *
   * @param org_id
   * @param folderName
   * @param uid
   * @param fileBean
   * @param acceptExts
   * @param msgList
   * @return
   */
  public static ShrinkImageSet getBytesShrinkFilebean(String org_id,
      String folderName, int uid, FileuploadLiteBean fileBean,
      String[] acceptExts, int width, int height, List<String> msgList,
      boolean isFixOrgImage) {

    byte[] result = null;
    byte[] fixResult = null;
    InputStream is = null;
    boolean fixed = false;

    try {

      String file_name = fileBean.getFileName();
      String ext = "";

      if (acceptExts != null && acceptExts.length > 0) {
        // 拡張子をチェックする．
        // ファイルのヘッダで識別するとベスト．
        boolean isAccept = false;
        String tmpExt = null;
        int len = acceptExts.length;
        for (int i = 0; i < len; i++) {
          if (!acceptExts[i].startsWith(".")) {
            tmpExt = "." + acceptExts[i];
          }
          if (file_name.toLowerCase().endsWith(tmpExt)) {
            isAccept = true;
            ext = tmpExt.replace(".", "");
            ;
            break;
          }
        }
        if (!isAccept) {
          // 期待しない拡張子の場合は，null を返す．
          return null;
        }
      }

      is =
        ALStorageService.getFile(FOLDER_TMP_FOR_ATTACHMENT_FILES, uid
          + ALStorageService.separator()
          + folderName, String.valueOf(fileBean.getFileId()));

      byte[] imageInBytes = IOUtils.toByteArray(is);
      ImageInformation readImageInformation =
        readImageInformation(new ByteArrayInputStream(imageInBytes));
      BufferedImage bufferdImage =
        ImageIO.read(new ByteArrayInputStream(imageInBytes));
      if (readImageInformation != null) {
        bufferdImage =
          transformImage(
            bufferdImage,
            getExifTransformation(readImageInformation),
            readImageInformation.orientation >= 5
              ? bufferdImage.getHeight()
              : bufferdImage.getWidth(),
            readImageInformation.orientation >= 5
              ? bufferdImage.getWidth()
              : bufferdImage.getHeight());
        fixed = isFixOrgImage;
      }
      if (bufferdImage == null) {
        // ファイルからbufferdImageを生成できなかった場合には,nullを返す.
        return null;
      }

      BufferedImage shrinkImage =
        FileuploadUtils.shrinkAndTrimImage(bufferdImage, width, height);
      Iterator<ImageWriter> writers = ImageIO.getImageWritersBySuffix("jpeg");
      ImageWriter writer = writers.next();

      ByteArrayOutputStream out = new ByteArrayOutputStream();
      ImageOutputStream ios = ImageIO.createImageOutputStream(out);
      writer.setOutput(ios);
      writer.write(shrinkImage);

      result = out.toByteArray();

      if (fixed) {
        Iterator<ImageWriter> writers2 = ImageIO.getImageWritersBySuffix(ext);
        ImageWriter writer2 = writers2.next();

        ByteArrayOutputStream out2 = new ByteArrayOutputStream();
        ImageOutputStream ios2 = ImageIO.createImageOutputStream(out2);
        writer2.setOutput(ios2);
        writer2.write(bufferdImage);

        fixResult = out2.toByteArray();
      }

    } catch (Exception e) {
      logger.error("fileupload", e);
      result = null;
    } finally {
      try {
        if (is != null) {
          is.close();
        }
      } catch (Exception e) {
        logger.error("fileupload", e);
        result = null;
      }
    }

    return new ShrinkImageSet(result, fixed ? fixResult : null);
  }

  public static ShrinkImageSet getBytesShrinkFilebean(String org_id,
      String folderName, int uid, FileuploadLiteBean fileBean,
      String[] acceptExts, int width, int height, List<String> msgList,
      boolean isFixOrgImage, int validate_width, int validate_height)
      throws FileuploadMinSizeException {

    byte[] result = null;
    byte[] fixResult = null;
    InputStream is = null;
    boolean fixed = false;

    try {

      String file_name = fileBean.getFileName();
      String ext = "";

      if (acceptExts != null && acceptExts.length > 0) {
        // 拡張子をチェックする．
        // ファイルのヘッダで識別するとベスト．
        boolean isAccept = false;
        String tmpExt = null;
        int len = acceptExts.length;
        for (int i = 0; i < len; i++) {
          if (!acceptExts[i].startsWith(".")) {
            tmpExt = "." + acceptExts[i];
          }
          if (file_name.toLowerCase().endsWith(tmpExt)) {
            isAccept = true;
            ext = tmpExt.replace(".", "");
            ;
            break;
          }
        }
        if (!isAccept) {
          // 期待しない拡張子の場合は，null を返す．
          return null;
        }
      }

      is =
        ALStorageService.getFile(FOLDER_TMP_FOR_ATTACHMENT_FILES, uid
          + ALStorageService.separator()
          + folderName, String.valueOf(fileBean.getFileId()));

      byte[] imageInBytes = IOUtils.toByteArray(is);
      ImageInformation readImageInformation =
        readImageInformation(new ByteArrayInputStream(imageInBytes));
      BufferedImage bufferdImage =
        ImageIO.read(new ByteArrayInputStream(imageInBytes));
      if (readImageInformation != null) {
        bufferdImage =
          transformImage(
            bufferdImage,
            getExifTransformation(readImageInformation),
            readImageInformation.orientation >= 5
              ? bufferdImage.getHeight()
              : bufferdImage.getWidth(),
            readImageInformation.orientation >= 5
              ? bufferdImage.getWidth()
              : bufferdImage.getHeight());
        fixed = isFixOrgImage;
      }
      if (bufferdImage == null) {
        // ファイルからbufferdImageを生成できなかった場合には,nullを返す.
        return null;
      }

      BufferedImage shrinkImage =
        FileuploadUtils.shrinkAndTrimImage(
          bufferdImage,
          width,
          height,
          validate_width,
          validate_height);
      Iterator<ImageWriter> writers = ImageIO.getImageWritersBySuffix("jpeg");
      ImageWriter writer = writers.next();

      ByteArrayOutputStream out = new ByteArrayOutputStream();
      ImageOutputStream ios = ImageIO.createImageOutputStream(out);
      writer.setOutput(ios);
      writer.write(shrinkImage);

      result = out.toByteArray();

      if (fixed) {
        Iterator<ImageWriter> writers2 = ImageIO.getImageWritersBySuffix(ext);
        ImageWriter writer2 = writers2.next();

        ByteArrayOutputStream out2 = new ByteArrayOutputStream();
        ImageOutputStream ios2 = ImageIO.createImageOutputStream(out2);
        writer2.setOutput(ios2);
        writer2.write(bufferdImage);

        fixResult = out2.toByteArray();
      }
    } catch (FileuploadMinSizeException e) {
      logger.error("fileupload", e);
      throw e;
    } catch (Exception e) {
      logger.error("fileupload", e);
      result = null;
    } finally {
      try {
        if (is != null) {
          is.close();
        }
      } catch (Exception e) {
        logger.error("fileupload", e);
        result = null;
      }
    }

    return new ShrinkImageSet(result, fixed ? fixResult : null);
  }

  /**
   * 縮小した画像のバイナリを返す。
   *
   * @param org_id
   * @param folderName
   * @param uid
   * @param fileBean
   * @param acceptExts
   * @param msgList
   * @return
   */
  public static byte[] getBytesShrink(InputStream is, int width, int height,
      List<String> msgList) {

    byte[] result = null;

    try {
      BufferedImage orgImage = ImageIO.read(is);
      BufferedImage shrinkImage =
        FileuploadUtils.shrinkImage(orgImage, width, height);
      Iterator<ImageWriter> writers = ImageIO.getImageWritersBySuffix("png");
      ImageWriter writer = writers.next();

      ByteArrayOutputStream out = new ByteArrayOutputStream();
      ImageOutputStream ios = ImageIO.createImageOutputStream(out);
      writer.setOutput(ios);
      writer.write(shrinkImage);

      result = out.toByteArray();
    } catch (Exception e) {
      logger.error("fileupload", e);
      result = null;
    } finally {
      try {
        if (is != null) {
          is.close();
        }
      } catch (Exception e) {
        logger.error("fileupload", e);
        result = null;
      }
    }
    return result;
  }

  /**
   * Java1.5：BMP, bmp, jpeg, wbmp, gif, png, JPG, jpg, WBMP, JPEG
   *
   * @param fileType
   * @return
   */
  public static boolean isImage(String fileName) {
    if (fileName == null || "".equals(fileName)) {
      return false;
    }

    int index = fileName.lastIndexOf(".");
    if (index < 1) {
      return false;
    }

    String fileType = getFileTypeName(fileName);

    String[] format = ImageIO.getWriterFormatNames();
    int len = format.length;
    for (int i = 0; i < len; i++) {
      if (format[i].equals(fileType)) {
        return true;
      }
    }
    return false;
  }

  /**
   * ファイル名からファイルの拡張子を取得する。
   *
   * @param fileName
   * @return
   */
  public static String getFileTypeName(String fileName) {
    if (fileName == null || "".equals(fileName)) {
      return null;
    }

    int index = fileName.lastIndexOf(".");
    if (index < 1) {
      return null;
    }

    return fileName.substring(index + 1, fileName.length());
  }

  /**
   * 縦横の縮小率で小さい方を縮小率とする。
   *
   * @param imgfile
   * @param dim
   * @return
   */
  public static BufferedImage shrinkImage(BufferedImage imgfile, int width,
      int height) {

    int iwidth = imgfile.getWidth();
    int iheight = imgfile.getHeight();

    double ratio =
      Math.min((double) width / (double) iwidth, (double) height
        / (double) iheight);
    int shrinkedWidth = (int) (iwidth * ratio);
    int shrinkedHeight = (int) (iheight * ratio);

    // イメージデータを縮小する
    Image targetImage =
      imgfile.getScaledInstance(
        shrinkedWidth,
        shrinkedHeight,
        Image.SCALE_AREA_AVERAGING);
    BufferedImage tmpImage =
      new BufferedImage(
        targetImage.getWidth(null),
        targetImage.getHeight(null),
        BufferedImage.TYPE_3BYTE_BGR);
    Graphics2D g = tmpImage.createGraphics();
    g.setColor(Color.WHITE);
    g.fillRect(0, 0, shrinkedWidth, shrinkedHeight);
    g.drawImage(targetImage, 0, 0, null);

    return tmpImage;
  }

  /**
   * アスペクト比を保存する。（縦横を切り取る）
   *
   * @param imgfile
   * @param width
   * @param height
   * @return
   */
  public static BufferedImage shrinkAndTrimImage(BufferedImage imgfile,
      int width, int height) {
    int iwidth = imgfile.getWidth();
    int iheight = imgfile.getHeight();
    double ratio =
      Math.max((double) width / (double) iwidth, (double) height
        / (double) iheight);

    int shrinkedWidth;
    int shrinkedHeight;

    if ((iwidth <= width) || (iheight < height)) {
      shrinkedWidth = iwidth;
      shrinkedHeight = iheight;
    } else {
      shrinkedWidth = (int) (iwidth * ratio);
      shrinkedHeight = (int) (iheight * ratio);
    }

    // イメージデータを縮小する
    Image targetImage =
      imgfile.getScaledInstance(
        shrinkedWidth,
        shrinkedHeight,
        Image.SCALE_AREA_AVERAGING);

    int w_size = targetImage.getWidth(null);
    int h_size = targetImage.getHeight(null);
    if (targetImage.getWidth(null) < width) {
      w_size = width;
    }
    if (targetImage.getHeight(null) < height) {
      h_size = height;
    }
    BufferedImage tmpImage =
      new BufferedImage(w_size, h_size, BufferedImage.TYPE_INT_RGB);
    Graphics2D g = tmpImage.createGraphics();
    g.setBackground(Color.WHITE);
    g.setColor(Color.WHITE);
    // 画像が小さい時には余白を追加してセンタリングした画像にする
    g.fillRect(0, 0, w_size, h_size);
    int diff_w = 0;
    int diff_h = 0;
    if (width > shrinkedWidth) {
      diff_w = (width - shrinkedWidth) / 2;
    }
    if (height > shrinkedHeight) {
      diff_h = (height - shrinkedHeight) / 2;
    }
    g.drawImage(targetImage, diff_w, diff_h, null);

    int _iwidth = tmpImage.getWidth();
    int _iheight = tmpImage.getHeight();
    BufferedImage _tmpImage;
    if (_iwidth > _iheight) {
      int diff = _iwidth - width;
      _tmpImage = tmpImage.getSubimage(diff / 2, 0, width, height);
    } else {
      int diff = _iheight - height;
      _tmpImage = tmpImage.getSubimage(0, diff / 2, width, height);
    }
    return _tmpImage;
  }

  public static BufferedImage shrinkAndTrimImage(BufferedImage imgfile,
      int width, int height, int validate_width, int validate_height)
      throws FileuploadMinSizeException {
    int iwidth = imgfile.getWidth();
    int iheight = imgfile.getHeight();
    if (iwidth < validate_width || iheight < validate_height) {
      throw new FileuploadMinSizeException();
    }

    double ratio =
      Math.max((double) width / (double) iwidth, (double) height
        / (double) iheight);

    int shrinkedWidth;
    int shrinkedHeight;

    if ((iwidth <= width) || (iheight < height)) {
      shrinkedWidth = iwidth;
      shrinkedHeight = iheight;
    } else {
      shrinkedWidth = (int) (iwidth * ratio);
      shrinkedHeight = (int) (iheight * ratio);
    }

    // イメージデータを縮小する
    Image targetImage =
      imgfile.getScaledInstance(
        shrinkedWidth,
        shrinkedHeight,
        Image.SCALE_AREA_AVERAGING);

    int w_size = targetImage.getWidth(null);
    int h_size = targetImage.getHeight(null);
    if (targetImage.getWidth(null) < width) {
      w_size = width;
    }
    if (targetImage.getHeight(null) < height) {
      h_size = height;
    }
    BufferedImage tmpImage =
      new BufferedImage(w_size, h_size, BufferedImage.TYPE_INT_RGB);
    Graphics2D g = tmpImage.createGraphics();
    g.setBackground(Color.WHITE);
    g.setColor(Color.WHITE);
    // 画像が小さい時には余白を追加してセンタリングした画像にする
    g.fillRect(0, 0, w_size, h_size);
    int diff_w = 0;
    int diff_h = 0;
    if (width > shrinkedWidth) {
      diff_w = (width - shrinkedWidth) / 2;
    }
    if (height > shrinkedHeight) {
      diff_h = (height - shrinkedHeight) / 2;
    }
    g.drawImage(targetImage, diff_w, diff_h, null);

    int _iwidth = tmpImage.getWidth();
    int _iheight = tmpImage.getHeight();
    BufferedImage _tmpImage;
    if (_iwidth > _iheight) {
      int diff = _iwidth - width;
      _tmpImage = tmpImage.getSubimage(diff / 2, 0, width, height);
    } else {
      int diff = _iheight - height;
      _tmpImage = tmpImage.getSubimage(0, diff / 2, width, height);
    }
    return _tmpImage;
  }

  public static int getMaxFileSize() {
    return ALCommonUtils.getMaxFileSize("MB");
  }

  public static int getMaxFileSize(String unit) {
    return ALCommonUtils.getMaxFileSize(unit);
  }

  public static class ImageInformation {
    public final int orientation;

    public final int width;

    public final int height;

    public ImageInformation(int orientation, int width, int height) {
      this.orientation = orientation;
      this.width = width;
      this.height = height;
    }

    @Override
    public String toString() {
      return String.format(
        "%dx%d,%d",
        this.width,
        this.height,
        this.orientation);
    }
  }

  public static ImageInformation readImageInformation(InputStream in) {
    try {
      Metadata metadata =
        ImageMetadataReader.readMetadata(new BufferedInputStream(in), true);
      Directory directory = metadata.getDirectory(ExifIFD0Directory.class);
      JpegDirectory jpegDirectory = metadata.getDirectory(JpegDirectory.class);

      int orientation = 1;
      try {
        orientation = directory.getInt(ExifIFD0Directory.TAG_ORIENTATION);
      } catch (Exception me) {

      }
      int width = jpegDirectory.getImageWidth();
      int height = jpegDirectory.getImageHeight();

      return new ImageInformation(orientation, width, height);
    } catch (IOException e) {
      logger.debug(e.getMessage(), e);
    } catch (ImageProcessingException e) {
      logger.debug(e.getMessage(), e);
    } catch (Throwable e) {
      logger.debug(e.getMessage(), e);
    }
    return null;
  }

  public static AffineTransform getExifTransformation(ImageInformation info) {

    AffineTransform t = new AffineTransform();
    if (info == null) {
      return t;
    }

    switch (info.orientation) {
      case 1:
        break;
      case 2: // Flip X
        t.scale(-1.0, 1.0);
        t.translate(-info.width, 0);
        break;
      case 3: // PI rotation
        t.translate(info.width, info.height);
        t.rotate(Math.PI);
        break;
      case 4: // Flip Y
        t.scale(1.0, -1.0);
        t.translate(0, -info.height);
        break;
      case 5: // - PI/2 and Flip X
        t.rotate(-Math.PI / 2);
        t.scale(-1.0, 1.0);
        break;
      case 6: // -PI/2 and -width
        t.translate(info.height, 0);
        t.rotate(Math.PI / 2);
        break;
      case 7: // PI/2 and Flip
        t.scale(-1.0, 1.0);
        t.translate(-info.height, 0);
        t.translate(0, info.width);
        t.rotate(3 * Math.PI / 2);
        break;
      case 8: // PI / 2
        t.translate(0, info.width);
        t.rotate(3 * Math.PI / 2);
        break;
      default:
        break;
    }

    return t;
  }

  public static BufferedImage transformImage(BufferedImage image,
      AffineTransform transform, int newWidth, int newHeight) throws Exception {

    AffineTransformOp op =
      new AffineTransformOp(transform, AffineTransformOp.TYPE_BICUBIC);

    BufferedImage destinationImage =
      new BufferedImage(newWidth, newHeight, image.getType());
    Graphics2D g = destinationImage.createGraphics();
    g.setColor(Color.WHITE);

    destinationImage = op.filter(image, destinationImage);

    return destinationImage;
  }

  public static class ShrinkImageSet {

    private byte[] shrinkImage = null;

    private byte[] fixImage = null;

    public ShrinkImageSet(byte[] shrinkImage, byte[] fixImage) {
      this.shrinkImage = shrinkImage;
      this.fixImage = fixImage;
    }

    public byte[] getShrinkImage() {
      return this.shrinkImage;
    }

    public byte[] getFixImage() {
      return this.fixImage;
    }
  }

  public static String getInlineContentType(String filename) {
    MimetypesFileTypeMap mimeTypesMap = new MimetypesFileTypeMap();
    mimeTypesMap.addMimeTypes("application/pdf pdf");
    mimeTypesMap.addMimeTypes("image/png png");
    mimeTypesMap.addMimeTypes("image/gif gif");
    mimeTypesMap.addMimeTypes("image/jpeg jpg jpeg");
    mimeTypesMap.addMimeTypes("image/bmp bmp");
    mimeTypesMap.addMimeTypes("image/vnd.wap.wbmp wbmp");
    String contentType = null;
    try {
      contentType = mimeTypesMap.getContentType(filename);
    } catch (Throwable ignore) {
      // ignore
    }
    return contentType;
  }

  public static boolean isDeskopApp() {
    HttpServletRequest request = HttpServletRequestLocator.get();
    if (request != null) {
      String userAgent = request.getHeader("User-Agent");
      if (userAgent == null || "".equals(userAgent)) {
        return false;
      }
      return userAgent.indexOf("Aipo.com/") > -1;
    }
    return false;
  }

  public static boolean isAcceptInline(String filename) {
    String contentType = getInlineContentType(filename);
    return !isDeskopApp()
      && Arrays.asList(ACCEPT_CONTENT_TYPES).contains(contentType);
  }

  /**
   * データ新規登録時のバリデート
   *
   * @param msgList
   * @param fileuploadList
   * @param hasInsertAuthority
   * @return
   */
  public static boolean insertValidate(List<String> msgList,
      List<? extends FileuploadLiteBean> fileuploadList,
      boolean hasInsertAuthority) {
    if ((fileuploadList != null && fileuploadList.size() > 0 && !hasInsertAuthority)) {
      msgList.add(ALAccessControlConstants.DEF_ATTACHMENT_PERMISSION_ERROR_STR);
      return false;
    }
    return true;
  }

  /**
   * @param msgList
   * @param formIdList
   *          ：POSTされたファイルID一覧
   * @param existFileIdList
   *          ：登録済ファイルID一覧
   * @param fileuploadList
   * @param hasAttachmentInsertAuthority
   * @param hasAttachmentDeleteAuthority
   * @return
   */
  public static boolean updateValidate(List<String> msgList,
      List<Integer> formIdList, List<Integer> existFileIdList,
      List<? extends FileuploadLiteBean> fileuploadList,
      boolean hasAttachmentInsertAuthority, boolean hasAttachmentDeleteAuthority) {

    int deleting = 0;
    int adding = 0;
    // ファイル削除時の権限判定
    if (existFileIdList != null && existFileIdList.size() > 0) {
      for (Integer fileId : existFileIdList) {
        if (!formIdList.contains(fileId)) {
          // 削除数に加える
          deleting++;
        }
      }

      if (deleting > 0 && !hasAttachmentDeleteAuthority) {
        // 添付ファイルに関する権限違反
        msgList
          .add(ALAccessControlConstants.DEF_ATTACHMENT_PERMISSION_ERROR_STR);
        return false;
      }
    }

    // ファイル追加時の権限判定
    if (fileuploadList != null && fileuploadList.size() > 0) {
      for (FileuploadLiteBean filebean : fileuploadList) {
        if (filebean.isNewFile()) {
          // 追加数に加える
          adding++;
        }
      }
      if (adding > 0 && !hasAttachmentInsertAuthority) {
        // 添付ファイルに関する権限違反
        msgList
          .add(ALAccessControlConstants.DEF_ATTACHMENT_PERMISSION_ERROR_STR);
        return false;
      }
    }

    return true;
  }

}
