package cn.luopan.animemasterbackend.config;

import cn.luopan.animemasterbackend.utils.JwtUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;

/**
 * JWT认证过滤器，用于验证请求头中的Token
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    @Autowired
    private JwtUtils jwtUtils;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        // 获取Authorization头
        String authorizationHeader = request.getHeader("Authorization");

        String token = null;
        String username = null;

        // 检查Authorization头是否存在且格式正确
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            token = authorizationHeader.substring(7);
            try {
                // 从Token中获取用户名
                username = jwtUtils.getUsernameFromToken(token);
            } catch (Exception e) {
                // Token无效，记录日志但不抛出异常
                log.error("Invalid token: {}", e.getMessage());
            }
        }

        // 如果Token有效且用户未认证
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            // 验证Token
            if (jwtUtils.validateToken(token)) {
                // 创建认证对象
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        username, null, new ArrayList<>());
                
                // 设置认证详情
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                
                // 将认证对象设置到安全上下文中
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }

        // 继续执行过滤链
        filterChain.doFilter(request, response);
    }
}