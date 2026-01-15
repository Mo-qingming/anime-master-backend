package cn.luopan.animemasterbackend.controller;

import cn.luopan.animemasterbackend.entity.User;
import cn.luopan.animemasterbackend.service.IUserService;
import cn.luopan.animemasterbackend.service.VerificationCodeService;
import cn.luopan.animemasterbackend.utils.TokenBlacklist;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private IUserService userService;

    @Autowired
    private TokenBlacklist tokenBlacklist;
    
    @Autowired
    private VerificationCodeService verificationCodeService;

    @Value("${jwt.secret:AnimeMasterSecretKey123456789012345678901234567890}")
    private String secretKey;

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@RequestBody Map<String, String> requestBody) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // 获取请求参数
            String username = requestBody.get("username");
            String email = requestBody.get("email");
            String password = requestBody.get("password");
            String verificationCode = requestBody.get("code");
            
            logger.info("用户注册请求 - 用户名: {}, 邮箱: {}", username, email);
            
            // 检查必填参数
            if (username == null || email == null || password == null || verificationCode == null) {
                logger.warn("用户注册失败 - 缺少必填参数");
                response.put("success", false);
                response.put("message", "用户名、邮箱、密码和验证码不能为空");
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }
            
            // 验证邮箱格式
            if (!isValidEmail(email)) {
                logger.warn("用户注册失败 - 邮箱格式错误: {}", email);
                response.put("success", false);
                response.put("message", "请输入有效的邮箱地址");
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }
            
            // 验证验证码格式
            if (verificationCode.length() != 6) {
                logger.warn("用户注册失败 - 验证码格式错误: {}", verificationCode);
                response.put("success", false);
                response.put("message", "验证码格式错误，应为6位数字");
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }
            
            // 验证验证码
            boolean validCode = verificationCodeService.verifyCode(
                    email, verificationCode, VerificationCodeService.CodeType.REGISTER);
            
            if (!validCode) {
                logger.warn("用户注册失败 - 验证码错误或已过期: {}", email);
                response.put("success", false);
                response.put("message", "验证码错误或已过期");
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }
            
            // 调用服务层进行注册
            User user = userService.registerUser(username, email, password);
            
            // 构造成功响应
            Map<String, Object> userData = new HashMap<>();
            userData.put("userId", user.getId());
            userData.put("username", user.getUsername());
            userData.put("email", user.getEmail());
            
            response.put("success", true);
            response.put("message", "注册成功");
            response.put("data", userData);
            
            logger.info("用户注册成功 - 用户ID: {}, 用户名: {}", user.getId(), user.getUsername());
            
            return new ResponseEntity<>(response, HttpStatus.OK);
            
        } catch (RuntimeException e) {
            // 处理业务异常
            logger.warn("用户注册失败 - 业务异常: {}", e.getMessage());
            response.put("success", false);
            response.put("message", e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            // 处理其他异常
            logger.error("用户注册失败 - 系统异常: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "注册失败：" + e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> loginRequest) {
        Map<String, Object> response = new HashMap<>();
        try {
            // 参数验证
            String usernameOrEmail = loginRequest.get("username");
            String password = loginRequest.get("password");

            logger.info("用户登录请求 - 用户名/邮箱: {}", usernameOrEmail);

            if (usernameOrEmail == null || usernameOrEmail.isEmpty() || password == null || password.isEmpty()) {
                logger.warn("用户登录失败 - 缺少必填参数");
                response.put("success", false);
                response.put("message", "用户名/邮箱和密码不能为空");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }

            // 调用服务层进行登录
            Map<String, Object> loginResponse = userService.loginUser(usernameOrEmail, password);

            // 构造响应
            response.put("success", true);
            response.put("message", "登录成功");
            response.put("data", loginResponse);

            logger.info("用户登录成功 - 用户名/邮箱: {}", usernameOrEmail);

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            logger.warn("用户登录失败 - 业务异常: {}", e.getMessage());
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        } catch (Exception e) {
            logger.error("用户登录失败 - 系统异常: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "登录失败，请稍后重试");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request) {
        try {
            // 从请求头中获取Authorization令牌
            String authorizationHeader = request.getHeader("Authorization");

            logger.info("用户退出登录请求 - Authorization头: {}", authorizationHeader != null ? authorizationHeader.substring(0, Math.min(15, authorizationHeader.length())) + "..." : null);

            if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
                // 提取Token
                String token = authorizationHeader.substring(7);

                // 将Token加入黑名单
                tokenBlacklist.addToBlacklist(token);

                // 构造成功响应
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", "退出登录成功");

                logger.info("用户退出登录成功 - Token已加入黑名单");

                return ResponseEntity.ok(response);
            } else {
                // 构造失败响应
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "退出登录失败，未提供有效Token");

                logger.warn("用户退出登录失败 - 未提供有效Token");

                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }
        } catch (Exception e) {
            logger.error("用户退出登录失败 - 系统异常: {}", e.getMessage(), e);
            e.printStackTrace();
            // 构造失败响应
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "退出登录失败");

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * 发送验证码
     */
    @PostMapping("/send-code")
    public ResponseEntity<?> sendCode(@RequestBody Map<String, String> requestBody) {
        try {
            // 获取请求参数
            String email = requestBody.get("email");
            String type = requestBody.get("type");
            
            logger.info("发送验证码请求 - 邮箱: {}, 类型: {}", email, type);
            
            // 检查必填参数
            if (email == null || email.isEmpty() || type == null || type.isEmpty()) {
                logger.warn("发送验证码失败 - 缺少必填参数");
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "邮箱和验证码类型不能为空");
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }
            
            // 验证邮箱格式
            if (!isValidEmail(email)) {
                logger.warn("发送验证码失败 - 邮箱格式错误: {}", email);
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "请输入有效的邮箱地址");
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }
            
            // 验证验证码类型
            VerificationCodeService.CodeType codeType;
            try {
                codeType = VerificationCodeService.CodeType.valueOf(type.toUpperCase());
            } catch (IllegalArgumentException e) {
                logger.warn("发送验证码失败 - 验证码类型错误: {}", type);
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "验证码类型错误，请使用register或reset_password");
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }
            
            // 发送验证码
            boolean sent = verificationCodeService.sendVerificationCode(email, codeType);
            
            if (sent) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", "验证码已发送至您的邮箱，请查收");
                
                logger.info("验证码发送成功 - 邮箱: {}, 类型: {}", email, codeType);
                
                return new ResponseEntity<>(response, HttpStatus.OK);
            } else {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "验证码发送失败，请稍后重试");
                
                logger.error("验证码发送失败 - 邮箱: {}, 类型: {}", email, codeType);
                
                return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
            }
            
        } catch (Exception e) {
            logger.error("发送验证码失败 - 系统异常: {}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "发送验证码失败，请稍后重试");
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    /**
     * 重置密码
     */
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> requestBody) {
        try {
            // 获取请求参数
            String email = requestBody.get("email");
            String verificationCode = requestBody.get("verificationCode");
            String newPassword = requestBody.get("newPassword");
            String confirmPassword = requestBody.get("confirmPassword");
            
            logger.info("重置密码请求 - 邮箱: {}", email);
            
            // 检查必填参数
            if (email == null || email.isEmpty() || verificationCode == null || verificationCode.isEmpty() ||
                    newPassword == null || newPassword.isEmpty() || confirmPassword == null || confirmPassword.isEmpty()) {
                logger.warn("重置密码失败 - 缺少必填参数");
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "所有参数都不能为空");
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }
            
            // 验证邮箱格式
            if (!isValidEmail(email)) {
                logger.warn("重置密码失败 - 邮箱格式错误: {}", email);
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "请输入有效的邮箱地址");
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }
            
            // 验证验证码格式
            if (verificationCode.length() != 6) {
                logger.warn("重置密码失败 - 验证码格式错误: {}", verificationCode);
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "验证码格式错误，应为6位数字");
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }
            
            // 验证密码一致性
            if (!newPassword.equals(confirmPassword)) {
                logger.warn("重置密码失败 - 两次输入的密码不一致");
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "两次输入的密码不一致");
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }
            
            // 验证密码长度
            if (newPassword.length() < 6) {
                logger.warn("重置密码失败 - 密码长度不足");
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "密码长度不能少于6位");
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }
            
            // 验证验证码
            boolean validCode = verificationCodeService.verifyCode(
                    email, verificationCode, VerificationCodeService.CodeType.RESET_PASSWORD);
            
            if (!validCode) {
                logger.warn("重置密码失败 - 验证码错误或已过期: {}", email);
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "验证码错误或已过期");
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }
            
            // 重置密码
            boolean passwordReset = userService.resetPassword(email, newPassword);
            
            if (passwordReset) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", "密码重置成功");
                
                logger.info("密码重置成功 - 邮箱: {}", email);
                
                return new ResponseEntity<>(response, HttpStatus.OK);
            } else {
                logger.error("重置密码失败 - 更新数据库失败: {}", email);
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "密码重置失败，请稍后重试");
                return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
            }
            
        } catch (RuntimeException e) {
            logger.warn("重置密码失败 - 业务异常: {}", e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            logger.error("重置密码失败 - 系统异常: {}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "重置密码失败，请稍后重试");
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    /**
     * 验证邮箱格式
     */
    private boolean isValidEmail(String email) {
        String emailRegex = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$";
        return email.matches(emailRegex);
    }
}