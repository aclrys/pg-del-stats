package org.prebid.pg.delstats.repository;

import org.prebid.pg.delstats.persistence.SystemState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.List;

@Repository
public interface SystemStateRepository extends JpaRepository<SystemState, Long> {
    @Modifying
    @Query(value = SystemStateConstants.SYSTEM_STATE_STORE_SQL, nativeQuery = true)
    void store(@Param("tag") String tag, @Param("val") String val
    );

    @Query(value = SystemStateConstants.SYSTEM_STATE_RETRIEVE_BY_TAG_SQL,
            nativeQuery = true)
    @QueryHints(value = {
                @javax.persistence.QueryHint(name = "org.hibernate.readOnly", value = "true"),
                @javax.persistence.QueryHint(name = "javax.persistence.query.timeout", value = "15000") }
            )
    SystemState retrieveByTag(@Param("tag") String tag);

    @Modifying
    @Query(value = SystemStateConstants.SAVE_DELIVERY_SUMMARY_REPORT_STATES_SQL, nativeQuery = true)
    void saveDeliverySummaryReportStates(
            @Param("startTime") Timestamp startTime,
            @Param("endTime") Timestamp endTime,
            @Param("serviceInstanceId") String serviceInstanceId,
            @Param("endTimeStr") String endTimeStr,
            @Param("tagSuffix") String tagSuffix);

    @Query(value = SystemStateConstants.GET_LATEST_DELIVERY_SUMMARY_REPORT_STATES_SQL, nativeQuery = true)
    @QueryHints(value = {
            @javax.persistence.QueryHint(name = "org.hibernate.readOnly", value = "true"),
            @javax.persistence.QueryHint(name = "javax.persistence.query.timeout", value = "15000") }
    )
    List<SystemState> getLatestDeliverySummaryReportStates(@Param("since") Timestamp since);

}
