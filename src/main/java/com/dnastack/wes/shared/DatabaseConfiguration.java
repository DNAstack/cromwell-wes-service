package com.dnastack.wes.shared;


import com.dnastack.wes.translation.OriginalInputRowMapper;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
public class DatabaseConfiguration {

    private final DataSource dataSource;

    public DatabaseConfiguration(DataSource dataSource) {this.dataSource = dataSource;}

    @Bean
    public Jdbi jdbi() {
        return Jdbi.create(dataSource)
            .registerArgument(new JsonMapArgumentFactory())
            .registerArgument(new JsonArrayArgumentFactory())
            .registerRowMapper(new OriginalInputRowMapper())
            .installPlugin(new SqlObjectPlugin());
    }

}
