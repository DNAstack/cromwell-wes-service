package com.dnastack.wes.api;

import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.sqlobject.transaction.Transaction;

public interface OriginalInputsDao {

    @Transaction
    @SqlUpdate("INSERT INTO original_inputs(id,inputs) VALUES(:id,:mapping::jsonb)")
    void saveInputs(@BindBean OriginalInputs inputs);


    @RegisterRowMapper(OriginalInputRowMapper.class)
    @SqlQuery("SELECT * FROM original_inputs WHERE id = :id")
    OriginalInputs getInputs(@Bind("id") String id);

}
