package org.seoulhub.backend;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
@SpringBootTest(properties={
    "spring.datasource.url=${MIGRATION_TEST_DB_URL:jdbc:h2:mem:migration_test;MODE=MySQL;DB_CLOSE_DELAY=-1}",
    "spring.datasource.driver-class-name=${MIGRATION_TEST_DB_DRIVER:org.h2.Driver}",
    "spring.datasource.username=${MIGRATION_TEST_DB_USER:sa}",
    "spring.datasource.password=${MIGRATION_TEST_DB_PASSWORD:}",
    "spring.jpa.database-platform=${MIGRATION_TEST_DB_DIALECT:org.hibernate.dialect.H2Dialect}",
    "spring.flyway.enabled=true", "spring.jpa.hibernate.ddl-auto=validate"
})
class MigrationTests {
    @Test void migrationAndHibernateSchemaAgree() {}
}
