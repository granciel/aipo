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
package com.aimluck.eip.project;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.jar.Attributes;

import org.apache.cayenne.DataRow;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.jetspeed.services.logging.JetspeedLogFactoryService;
import org.apache.jetspeed.services.logging.JetspeedLogger;
import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;

import com.aimluck.eip.cayenne.om.portlet.EipTProject;
import com.aimluck.eip.cayenne.om.portlet.EipTProjectFile;
import com.aimluck.eip.cayenne.om.portlet.EipTProjectMember;
import com.aimluck.eip.cayenne.om.portlet.EipTProjectTask;
import com.aimluck.eip.cayenne.om.security.TurbineUser;
import com.aimluck.eip.common.ALAbstractSelectData;
import com.aimluck.eip.common.ALDBErrorException;
import com.aimluck.eip.common.ALData;
import com.aimluck.eip.common.ALPageNotFoundException;
import com.aimluck.eip.fileupload.beans.FileuploadBean;
import com.aimluck.eip.fileupload.util.FileuploadUtils;
import com.aimluck.eip.modules.actions.common.ALAction;
import com.aimluck.eip.orm.Database;
import com.aimluck.eip.orm.query.ResultList;
import com.aimluck.eip.orm.query.SelectQuery;
import com.aimluck.eip.project.util.ProjectUtils;
import com.aimluck.eip.util.ALEipUtils;

/**
 * プロジェクト管理の検索データを管理するクラスです。 <BR>
 *
 */
public class ProjectSelectData extends
    ALAbstractSelectData<EipTProject, EipTProject> implements ALData {

  /** logger */
  private static final JetspeedLogger logger = JetspeedLogFactoryService
    .getLogger(ProjectSelectData.class.getName());

  /** プロジェクトの総数 */
  private int projectCount;

  /** 全プロジェクトの一覧 */
  private List<ProjectResultData> allProject;

  /** ログインユーザーID */
  private Integer loginUserId;

  /**
   * 初期設定
   *
   * @param action
   *          ALAction
   * @param rundata
   *          RunData
   * @param context
   *          Context
   */
  @Override
  public void init(ALAction action, RunData rundata, Context context)
      throws ALPageNotFoundException, ALDBErrorException {
    String sort = ALEipUtils.getTemp(rundata, context, LIST_SORT_STR);
    if (sort == null || sort.equals("")) {
      ALEipUtils.setTemp(rundata, context, LIST_SORT_STR, "sort");
    }

    allProject = ProjectUtils.getAllProject(); // 全プロジェクト

    loginUserId = ALEipUtils.getUserId(rundata);

    super.init(action, rundata, context);
  }

  /**
   * 一覧データを取得します。 <BR>
   *
   * @param rundata
   *          RunData
   * @param context
   *          Context
   * @return ResultList
   */
  @Override
  protected ResultList<EipTProject> selectList(RunData rundata, Context context) {
    try {
      SelectQuery<EipTProject> query = getSelectQuery(rundata, context);
      buildSelectQueryForListView(query);
      buildSelectQueryForListViewSort(query, rundata, context);

      ResultList<EipTProject> list = query.getResultList();
      // 件数をセットする．
      projectCount = list.getTotalCount();

      return list;

    } catch (Exception ex) {
      logger.error("Exception", ex);
      return null;
    }
  }

  /**
   * 検索条件を設定した SelectQuery を返します。 <BR>
   *
   * @param rundata
   *          RunData
   * @param context
   *          Context
   * @return SelectQuery
   */
  private SelectQuery<EipTProject> getSelectQuery(RunData rundata,
      Context context) {
    SelectQuery<EipTProject> query = Database.query(EipTProject.class);
    return buildSelectQueryForFilter(query, rundata, context);
  }

  /**
   * 詳細データを取得します。 <BR>
   *
   * @param rundata
   *          RunData
   * @param context
   *          Context
   * @return 汎用データベースオブジェクトモデル
   */
  @Override
  protected EipTProject selectDetail(RunData rundata, Context context) {
    // オブジェクトモデルを取得
    return ProjectUtils.getEipTProject(rundata, context);
  }

  /**
   * ResultDataを取得します。（一覧データ） <BR>
   *
   * @param record
   *          レコード
   * @return ResultData
   */
  @Override
  protected Object getResultData(EipTProject record) {
    ProjectResultData data = ProjectUtils.getProjectResultData(record);

    // プロジェクトの進捗情報を取得
    List<DataRow> result =
      ProjectUtils.getProjectProgress(record.getProjectId());

    if (result != null && !result.isEmpty()) {

      DataRow row = result.get(0);

      if (Double.valueOf(row.get("cnt").toString()) > 0) {

        // 経過タスク日数
        int lapsedDays =
          (int) (Double.valueOf(row.get("lapsed_days").toString())
            .doubleValue());
        // タスク日数
        int taskDays =
          (int) (Double.valueOf(row.get("task_days").toString()).doubleValue());

        if (lapsedDays > taskDays) {
          // 経過タスク日数≦タスク日数にする
          lapsedDays = taskDays;
        }

        // 予定進捗率
        data.setPlanProgressRate(ProjectUtils.getPlanWorkload(
          lapsedDays,
          taskDays));

        // 計画工数
        data
          .setPlanWorkload(new BigDecimal(row.get("plan_workload").toString()));
        // 工数
        BigDecimal workload = new BigDecimal(row.get("workload").toString());
        data.setWorkload(workload);

        // 完了予測工数
        BigDecimal forecastWorkload = BigDecimal.valueOf(0);
        if (data.getProgressRate().getValue() != 0) {
          forecastWorkload =
            workload.multiply(BigDecimal.valueOf(100)).divide(
              BigDecimal.valueOf(data.getProgressRate().getValue()),
              2,
              BigDecimal.ROUND_HALF_UP);
        }
        data.setForecastWorkload(forecastWorkload);
        data.setTaskUpdateDate((Date) row.get("task_update_date"));

        SelectQuery<EipTProjectTask> query =
          Database.query(EipTProjectTask.class);
        Expression exp1 =
          ExpressionFactory.matchExp(
            EipTProjectTask.PROJECT_ID_PROPERTY,
            record.getProjectId());
        Expression exp2 =
          ExpressionFactory
            .lessExp(EipTProjectTask.PROGRESS_RATE_PROPERTY, 100);
        /** ステータスが新規、進行中、フィードバックのものだけ残りタスクとする */
        query.setQualifier(exp1.andExp(exp2).andExp(
          ProjectUtils.getIncompleteExpression()));
        data.setReminderTask(query.getCount());
      }

    }

    return data;
  }

  /**
   * ResultDataを取得します。（詳細データ） <BR>
   *
   * @param record
   *          レコード
   * @return ResultData
   */
  @Override
  protected Object getResultDataDetail(EipTProject record) {

    ProjectResultData data = new ProjectResultData();
    data.initField();
    data.setProjectId(record.getProjectId()); // プロジェクトID
    data.setProjectName(record.getProjectName()); // プロジェクト名
    data.setExplanation(record.getExplanation()); // 説明
    data.setAdminUserId(record.getAdminUserId()); // 管理者ID
    data.setProgressFlg(record.getProgressFlg()); // 進捗率入力フラグ
    data.setProgressRate(record.getProgressRate()); // 進捗率
    data.setLoginUserId(loginUserId.longValue()); // ログインユーザーID
    data.setCreateUserId(record.getTurbineUser().getUserId().longValue()); // 作成者ID
    data.setUpdateUserId(record.getUpdateUserId()); // 更新者ID

    try {
      // 作成者
      data.setCreateUserName(ALEipUtils.getALEipUser(
        record.getTurbineUser().getUserId()).getAliasName().getValue());

      // 更新者
      data.setUpdateUserName(ALEipUtils
        .getALEipUser(record.getUpdateUserId())
        .getAliasName()
        .getValue());

      // 管理者名
      data.setAdminUserName(ALEipUtils
        .getALEipUser(record.getAdminUserId())
        .getAliasName()
        .getValue());
    } catch (ALDBErrorException ex) {
      logger.error("Exception", ex);
    }

    List<Integer> users1 = new ArrayList<Integer>();
    EipTProjectMember map1 = null;
    List<EipTProjectMember> tmp_maps1 =
      ProjectUtils.getEipTProjectMember(record);
    int size1 = tmp_maps1.size();
    for (int i = 0; i < size1; i++) {
      map1 = tmp_maps1.get(i);
      users1.add(map1.getUserId());
    }
    SelectQuery<TurbineUser> query1 = Database.query(TurbineUser.class);
    Expression exp1 =
      ExpressionFactory.inDbExp(TurbineUser.USER_ID_PK_COLUMN, users1);
    query1.setQualifier(exp1);
    data.setMemberList(ALEipUtils.getUsersFromSelectQuery(query1));

    if (hasAttachmentAuthority()) {
      // ファイルリスト
      List<EipTProjectFile> list =
        ProjectUtils
          .getSelectQueryForFiles(record.getProjectId().intValue())
          .fetchList();
      if (list != null && list.size() > 0) {
        List<FileuploadBean> attachmentFileList =
          new ArrayList<FileuploadBean>();
        FileuploadBean filebean = null;
        for (EipTProjectFile file : list) {
          String realname = file.getFileName();
          javax.activation.DataHandler hData =
            new javax.activation.DataHandler(
              new javax.activation.FileDataSource(realname));

          filebean = new FileuploadBean();
          filebean.setFileId(file.getFileId().intValue());
          filebean.setFileName(realname);
          if (hData != null) {
            filebean.setContentType(hData.getContentType());
          }
          filebean.setIsImage(FileuploadUtils.isImage(realname));
          attachmentFileList.add(filebean);
        }
        data.setAttachmentFileList(attachmentFileList);
      }
    }

    data.setCreateDate(record.getCreateDate());// 作成日
    data.setUpdateDate(record.getUpdateDate());// 更新日
    return data;

  }

  /**
   * 項目情報を取得する
   *
   * @return 項目情報
   */
  @Override
  protected Attributes getColumnMap() {
    Attributes map = new Attributes();
    map.putValue("project_name", EipTProject.PROJECT_NAME_PROPERTY);
    return map;
  }

  /**
   * プロジェクトの総数を取得する
   *
   * @return プロジェクト総数
   */
  public int getProjectCount() {
    return projectCount;
  }

  /**
   * 全プロジェクトのリストを返す
   *
   * @return 全プロジェクトのリスト
   */
  public List<ProjectResultData> getAllProject() {
    return allProject;
  }

  /**
   * ビュータイプを取得する
   *
   * @return ビュータイプ
   */
  public String getViewtype() {
    return "project";
  }
}
