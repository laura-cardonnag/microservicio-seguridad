package com.lk.microservicio_seguridad.Configurations;

import com.lk.microservicio_seguridad.Interceptors.SecurityInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

//Al activar determinados endpoints alguien pueda entrar o no
@Configuration
public class WebConfig implements WebMvcConfigurer {
    //Inyección de un security... Mirar si el usuario tiene rol, permiso, etc
    @Autowired
    private SecurityInterceptor securityInterceptor;

    //implementación de un interceptor
    //Filtro a la entrada del sistema
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        //registry es objeto propio del framework, el interceptor es securityInterceptor
        registry.addInterceptor(securityInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns("/api/public/**");
    }

    //Configuración de CORS para permitir peticiones desde Angular
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins("http://localhost:4200")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
    }
}