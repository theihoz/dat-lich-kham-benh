package com.datlichkhambenh.backend.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import java.net.URI;
import java.util.Arrays;
import java.util.Set;

/** Fail before connecting to external services; exception messages never contain secrets. */
public class ProductionSafetyEnvironment implements EnvironmentPostProcessor, Ordered {
    @Override public int getOrder() { return Ordered.LOWEST_PRECEDENCE; }
    @Override public void postProcessEnvironment(ConfigurableEnvironment env, SpringApplication application) {
        Set<String> profiles = Set.copyOf(Arrays.asList(env.getActiveProfiles()));
        if (!profiles.contains("prod")) return;
        if (profiles.contains("dev") || profiles.contains("demo") || profiles.contains("test")) fail("prod cannot be combined with dev/demo/test");
        for (String key : new String[]{"spring.datasource.url", "spring.datasource.username", "spring.mail.host", "spring.mail.username", "spring.mail.password", "app.public-url"}) required(env, key);
        for (String key : new String[]{"app.jwt.secret", "spring.datasource.password", "spring.data.redis.password", "spring.rabbitmq.password", "app.metrics-token"}) {
            String value = required(env, key);
            if (value.length() < ((key.equals("app.jwt.secret") || key.equals("app.metrics-token")) ? 32 : 12) || Set.of("root", "guest", "changeme").contains(value.toLowerCase()) || value.contains("dev-only") || value.contains("classroom-demo")) fail("weak secret: " + key);
        }
        if ("guest".equalsIgnoreCase(required(env,"spring.rabbitmq.username"))) fail("guest broker user forbidden");
        if ("root".equalsIgnoreCase(env.getProperty("spring.datasource.username"))) fail("root database user forbidden");
        if (!required(env,"spring.datasource.url").startsWith("jdbc:mysql:")) fail("production requires MySQL");
        URI publicUrl;
        try { publicUrl = URI.create(required(env,"app.public-url")); }
        catch (IllegalArgumentException ex) { throw new IllegalStateException("Unsafe production configuration: invalid public URL"); }
        if (!"https".equals(publicUrl.getScheme()) || publicUrl.getHost()==null || publicUrl.getUserInfo()!=null || publicUrl.getQuery()!=null || publicUrl.getFragment()!=null) fail("public URL requires HTTPS origin");
        if (!"validate".equals(env.getProperty("spring.jpa.hibernate.ddl-auto"))) fail("ddl-auto must be validate");
        if (!env.getProperty("spring.flyway.enabled",Boolean.class,false)) fail("versioned migrations required");
        if (env.getProperty("spring.flyway.baseline-on-migrate",Boolean.class,false)) fail("automatic baseline forbidden");
    }
    private String required(ConfigurableEnvironment env,String key) {
        String value=env.getProperty(key);
        if(value==null || value.isBlank() || value.contains("${")) fail("missing " + key);
        return value;
    }
    private void fail(String reason) { throw new IllegalStateException("Unsafe production configuration: " + reason); }
}
