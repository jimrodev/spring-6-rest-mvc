package guru.springframework.spring6restmvc.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SpringSecurityConfig {

    // V2019-220 LE pasamos las credenciales * CUIDADO * en los test el profile es * default * coge la configuración del application.properties no del .ym.l
    // ERROR: 403 NO 401
    // java.lang.AssertionError: Status expected:<201> but was:<403>
    // ACTIVANDO LA TRAZA: logging.level.org.springframework.security=trace
    // 2024-02-05T12:10:23.305+01:00 TRACE 13828 --- [main] o.s.security.web.FilterChainProxy : Invoking CsrfFilter (5/15)
    // 2024-02-05T12:10:23.312+01:00 DEBUG 13828 --- [main] o.s.security.web.csrf.CsrfFilter  : Invalid CSRF token found for http://localhost/api/v1/beer
    // SOLUCIÓN: Disable CSFR
    // Al deshabilitarlo no tenemos que pasarle las credenciales en la petición
    // PROBLEMA: V221
    // Al desabilitar el CSFR, sobreescribe la configuración por defecto de Spring Secirity
    // SOLUCIÓN:
    // http.authorizeHttpRequests()
    //                .anyRequest().authenticated()
    //                .and().httpBasic(Customizer.withDefaults())
    // CON ESTA CONFIGURACIÓN TENEMOS QUE PASARLE LA CREDENCIALES EN LAS PETICIONES DE MockMvc
    // Tambíen hay que indicarle en el appication * wac * web Applicaton Context Setup
    // .apply(springSecurity()) from org.springframework.security.test.web.servlet.setup
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception{
        // V221
        // http.csrf().ignoringRequestMatchers("/api/**");
        http.authorizeHttpRequests()
                .anyRequest().authenticated()
                .and().httpBasic(Customizer.withDefaults())
                .csrf().ignoringRequestMatchers("/api/**");
        return http.build();
    }
}
