package com.payment.reconcile.mapper.mysql;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.payment.reconcile.entity.ReconcileTask;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;

@Mapper
public interface ReconcileTaskMapper extends BaseMapper<ReconcileTask> {

    @Update("UPDATE reconcile_task SET task_status = #{taskStatus}, " +
            "total_count = #{totalCount}, success_count = #{successCount}, " +
            "diff_count = #{diffCount}, failed_count = #{failedCount}, " +
            "progress_pct = #{progressPct}, update_time = NOW() " +
            "WHERE task_id = #{taskId}")
    int updateProgress(@Param("taskId") String taskId,
                       @Param("taskStatus") String taskStatus,
                       @Param("totalCount") Integer totalCount,
                       @Param("successCount") Integer successCount,
                       @Param("diffCount") Integer diffCount,
                       @Param("failedCount") Integer failedCount,
                       @Param("progressPct") BigDecimal progressPct);

    @Update("UPDATE reconcile_task SET task_status = #{taskStatus}, " +
            "finish_time = NOW(), error_msg = #{errorMsg}, update_time = NOW() " +
            "WHERE task_id = #{taskId}")
    int updateFinish(@Param("taskId") String taskId,
                     @Param("taskStatus") String taskStatus,
                     @Param("errorMsg") String errorMsg);
}
