package org.seoulhub.backend.global.config;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
@Configuration @Profile("prod")
public class ProductionConfiguration {
    public ProductionConfiguration(Environment env) {
        if (java.util.Arrays.asList(env.getActiveProfiles()).contains("local")) throw new IllegalStateException("prod and local profiles cannot be combined");
        for (String name : new String[]{"security.jwt.secret","security.proxy-secret"}) {
            String value=env.getProperty(name, "");
            if(value.length()<32 || value.startsWith("local-") || value.startsWith("test-") || value.contains("change-me")) throw new IllegalStateException("A unique 32+ character secret is required for "+name);
        }
        if(env.getProperty("security.jwt.secret", "").equals(env.getProperty("security.proxy-secret", ""))) throw new IllegalStateException("JWT and proxy secrets must be different");
        if(env.getProperty("seoul.collection.enabled",Boolean.class,true) && env.getProperty("seoul.api.key", "").isBlank()) throw new IllegalStateException("SEOUL_API_KEY is required when collection is enabled");
        String origin=env.getProperty("account.public-origin", "");
        try {
            var uri=java.net.URI.create(origin);
            if(!"https".equals(uri.getScheme()) || uri.getHost()==null || uri.getUserInfo()!=null || (uri.getPath()!=null && !uri.getPath().isEmpty()) || uri.getQuery()!=null || uri.getFragment()!=null) throw new IllegalArgumentException();
        } catch(IllegalArgumentException e) { throw new IllegalStateException("APP_PUBLIC_ORIGIN must be an HTTPS origin without a trailing slash"); }
        if(!"brevo".equals(env.getProperty("account.mail.mode"))) throw new IllegalStateException("Production requires MAIL_MODE=brevo");
        String url=env.getProperty("spring.datasource.url", "");
        if(!url.contains("sslMode=verify-full")) throw new IllegalStateException("Production database requires sslMode=verify-full");
    }
}
