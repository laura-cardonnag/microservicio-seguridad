package com.lk.microservicio_seguridad.Interceptors;

import com.lk.microservicio_seguridad.Services.ValidatorsService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class SecurityInterceptor implements HandlerInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(SecurityInterceptor.class);

    @Autowired
    private ValidatorsService validatorService;

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {

        logger.info("SecurityInterceptor ejecutándose para: {}", request.getRequestURI());

        // 🔥 SOLUCIÓN CLAVE
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            logger.info("Petición OPTIONS permitida (CORS preflight)");
            response.setStatus(HttpServletResponse.SC_OK);
            return true;
        }

        try {
            boolean hasAccess = validatorService.validationRolePermission(
                    request,
                    request.getRequestURI(),
                    request.getMethod()
            );

            if (!hasAccess) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.getWriter().write("Acceso denegado");
                return false;
            }

            return true;

        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("Sesión expirada o inválida");
            return false;
        }
    }}