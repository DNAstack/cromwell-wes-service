package com.dnastack.wes.transfer;

import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.sqlobject.transaction.Transaction;

import java.time.ZonedDateTime;
import java.util.List;

public interface TrackedTransferDao {


    @Transaction
    @SqlUpdate("INSERT INTO tracked_transfers(cromwell_id,transfer_job_ids,failure_attempts,last_update,created) VALUES(:cromwellId,:transferJobIds::jsonb,:failureAttempts,:lastUpdate,:created)")
    void saveTrackedTransfer(@BindBean TrackedTransfer trackedTransfer);

    @Transaction
    @SqlUpdate("DELETE FROM tracked_transfers WHERE cromwell_id = :cromwellId")
    void removeTrackedTransfer(@Bind("cromwellId") String transferId);

    @Transaction
    @SqlUpdate("UPDATE tracked_transfers SET last_update = :lastUpdate WHERE cromwell_id = :cromwellId")
    void updateTransfer(@Bind("lastUpdate") ZonedDateTime lastUpdate, @Bind("cromwellId") String cromwellId);

    @Transaction
    @SqlUpdate("UPDATE tracked_transfers SET last_update = :lastUpdate, failure_attempts = :failureAttempts WHERE cromwell_id = :cromwellId")
    void updateTransfeFailureAttempts(
        @Bind("lastUpdate") ZonedDateTime lastUpdate,
        @Bind("failureAttempts") int failureAttempts,
        @Bind("cromwellId") String cromwellId
    );

    @SqlQuery("SELECT * FROM tracked_transfers")
    @RegisterRowMapper(TrackedTransferRowMapper.class)
    List<TrackedTransfer> getTransfers();


}
