package org.jobrunr.storyline.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpMethod;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.datasource.init.DataSourceInitializer;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.jobrunr.storyline.model.Storyline;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.authentication.ott.JdbcOneTimeTokenService;
import org.springframework.security.authentication.ott.OneTimeTokenService;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;

import javax.sql.DataSource;

@AutoConfiguration
@ConditionalOnClass(SecurityFilterChain.class)
@EnableConfigurationProperties(StorylineSecurityProperties.class)
public class StorylineSecurityAutoConfiguration {

    @Configuration
    @ConditionalOnProperty(name = "storyline.security.enabled", havingValue = "true")
    @EnableWebSecurity
    @Import({StorylineAuthController.class, SecurityModelEnricher.class})
    static class SecuredConfiguration {

        @Bean
        SecurityFilterChain storylineSecuredFilterChain(HttpSecurity http,
                @Value("${jobrunr.dashboard.context-path:}") String dashboardContextPath) throws Exception {
            return http
                    .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.GET, "/", "/storyline/**", "/code/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/dashboard/**", "/api/**", "/sse/**").permitAll()
                        .requestMatchers("/login/**", "/register/**", "/ott/**", "/error", "/css/**", "/js/**", "/images/**").permitAll()
                        .requestMatchers("/*.svg", "/*.webp", "/*.png", "/*.ico").permitAll()
                        .anyRequest().authenticated())
                    .headers(headers -> headers
                        .frameOptions(frame -> frame.sameOrigin()))
                    .oneTimeTokenLogin(ott -> ott
                        .showDefaultSubmitPage(false)
                        .defaultSuccessUrl("/storyline", true))
                    .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(new LoginUrlAuthenticationEntryPoint("/login")))
                    .logout(logout -> logout.logoutSuccessUrl("/"))
                    .csrf(csrf -> csrf.ignoringRequestMatchers( "/api/**"))
                    .build();
        }

        @Bean
        OneTimeTokenService storylineOneTimeTokenService(JdbcOperations jdbcOperations) {
            return new JdbcOneTimeTokenService(jdbcOperations);
        }

        @Bean
        UserDetailsService storylineUserDetailsService(StorylineUserRepository userRepository) {
            return new StorylineUserDetailsService(userRepository);
        }

        @Bean
        StorylineMagicLinkService storylineMagicLinkService(OneTimeTokenService oneTimeTokenService,
                JavaMailSender mailSender, StorylineSecurityProperties properties, Storyline storyline) {
            return new StorylineMagicLinkService(oneTimeTokenService, mailSender, properties.getMail().getFrom(), storyline);
        }

        @Bean
        DataSourceInitializer storylineSecuritySchemaInitializer(DataSource dataSource) {
            var populator = new ResourceDatabasePopulator();
            populator.addScript(new ClassPathResource("storyline-security-schema.sql"));
            var initializer = new DataSourceInitializer();
            initializer.setDataSource(dataSource);
            initializer.setDatabasePopulator(populator);
            return initializer;
        }
    }

    @Configuration
    @ConditionalOnProperty(name = "storyline.security.enabled", havingValue = "false", matchIfMissing = true)
    static class PermissiveConfiguration {

        @Bean
        SecurityFilterChain storylinePermissiveFilterChain(HttpSecurity http) throws Exception {
            return http
                    .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                    .csrf(csrf -> csrf.disable())
                    .build();
        }
    }
}
