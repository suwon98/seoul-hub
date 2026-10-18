package org.seoulhub.backend.global.config;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.engine.jdbc.env.spi.IdentifierCaseStrategy;
import org.hibernate.engine.jdbc.env.spi.IdentifierHelper;
import org.hibernate.engine.jdbc.env.spi.IdentifierHelperBuilder;

/** MySQL SQL semantics with MariaDB Connector/J's missing identifier-case metadata. */
public class MySqlMariaDbDriverDialect extends MySQLDialect {
    @Override
    public IdentifierHelper buildIdentifierHelper(IdentifierHelperBuilder builder, DatabaseMetaData metadata)
            throws SQLException {
        // Same fallback as Hibernate's MariaDBDialect: preserve case on Linux MySQL.
        builder.setUnquotedCaseStrategy(IdentifierCaseStrategy.MIXED);
        builder.setQuotedCaseStrategy(IdentifierCaseStrategy.MIXED);
        return super.buildIdentifierHelper(builder, metadata);
    }
}
