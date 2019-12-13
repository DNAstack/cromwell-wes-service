package com.dnastack.wes.data;


import javax.sql.DataSource;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DatabaseConfiguration {

    @Autowired
    private DataSource dataSource;

    @Bean
    public Jdbi jdbi() {
        return Jdbi.create(dataSource)
            .registerArgument(new JsonMapArgumentFactory())
            .registerRowMapper(new OriginalInputRowMapper())
            .installPlugin(new SqlObjectPlugin());
    }
}
