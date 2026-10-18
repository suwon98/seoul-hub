package org.seoulhub.backend;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.seoulhub.backend.global.config.*;
import static org.assertj.core.api.Assertions.*;

class ProductionBoundaryTests {
    final String secret = "random-enough-for-test-only-32-characters";
    @Test void proxySecretIsRequiredEvenForPublicEndpoints() throws Exception {
        var filter = new RequestLimitFilter(new RequestRateLimiter(), secret);
        for (String supplied : new String[]{"", "incorrect", secret}) {
            var request = new MockHttpServletRequest("GET", "/api/v1/congestion/all");
            request.addHeader("X-Internal-API-Key", supplied);
            var response = new MockHttpServletResponse();
            var chain = new MockFilterChain();
            filter.doFilter(request, response, chain);
            assertThat(response.getStatus()).isEqualTo(supplied.equals(secret) ? 200 : 403);
            assertThat(chain.getRequest() != null).isEqualTo(supplied.equals(secret));
        }
    }
    @Test void healthRemainsAvailableWithoutProxySecret() throws Exception {
        var response = new MockHttpServletResponse();
        var chain = new MockFilterChain();
        new RequestLimitFilter(new RequestRateLimiter(), secret).doFilter(new MockHttpServletRequest("GET", "/actuator/health"), response, chain);
        assertThat(chain.getRequest()).isNotNull();
    }
    @Test void productionRejectsWeakSecretsAndUnverifiedDatabaseTls() {
        var env = new MockEnvironment().withProperty("account.public-origin", "https://portfolio.example").withProperty("account.mail.mode", "brevo").withProperty("security.jwt.secret", secret)
            .withProperty("security.proxy-secret", secret+"-proxy").withProperty("seoul.api.key", "fixture")
            .withProperty("spring.datasource.url", "jdbc:mariadb://host/db?sslMode=verify-full");
        assertThatCode(() -> new ProductionConfiguration(env)).doesNotThrowAnyException();
        env.setProperty("seoul.api.key", "");
        assertThatThrownBy(() -> new ProductionConfiguration(env)).isInstanceOf(IllegalStateException.class);
        env.setProperty("seoul.api.key", "fixture");
        env.setProperty("security.proxy-secret", secret);
        assertThatThrownBy(() -> new ProductionConfiguration(env)).isInstanceOf(IllegalStateException.class);
        env.setProperty("security.proxy-secret", secret+"-proxy");
        env.setProperty("security.jwt.secret", "short");
        assertThatThrownBy(() -> new ProductionConfiguration(env)).isInstanceOf(IllegalStateException.class);
        env.setProperty("security.jwt.secret", secret);
        env.setProperty("spring.datasource.url", "jdbc:mariadb://host/db?sslMode=trust");
        assertThatThrownBy(() -> new ProductionConfiguration(env)).isInstanceOf(IllegalStateException.class);
        env.setProperty("spring.datasource.url", "jdbc:mariadb://host/db?sslMode=verify-full");
        env.setActiveProfiles("prod", "local");
        assertThatThrownBy(() -> new ProductionConfiguration(env)).isInstanceOf(IllegalStateException.class);
    }
}
