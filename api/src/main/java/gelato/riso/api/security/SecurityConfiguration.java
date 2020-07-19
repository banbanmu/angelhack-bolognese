package gelato.riso.api.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.HttpStatusServerEntryPoint;
import org.springframework.security.web.server.authorization.HttpStatusServerAccessDeniedHandler;
import org.springframework.security.web.server.context.ServerSecurityContextRepository;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class SecurityConfiguration {

    private final ReactiveAuthenticationManager authenticationManager;
    private final ServerSecurityContextRepository securityContextRepository;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityWebFilterChain httpSecurity(ServerHttpSecurity httpSecurity) {
        return httpSecurity.cors().disable()
                           .csrf().disable()
                           .formLogin().disable()
                           .logout().disable()
                           .exceptionHandling()
                           .authenticationEntryPoint(new HttpStatusServerEntryPoint(HttpStatus.UNAUTHORIZED))
                           .accessDeniedHandler(new HttpStatusServerAccessDeniedHandler(HttpStatus.FORBIDDEN))
                           .and()
                           .httpBasic().and()
                           .authenticationManager(authenticationManager)
                           .securityContextRepository(securityContextRepository)
                           .authorizeExchange()
                           .pathMatchers("/auth/**").permitAll()
                           .pathMatchers("/internal/**").permitAll()
                           .anyExchange().hasRole("USER")
                           .and().build();
    }
}
