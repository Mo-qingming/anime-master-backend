package cn.luopan.animemasterbackend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 验证码服务类
 * 用于生成、存储和验证验证码
 */
@Service
public class VerificationCodeService {

    private static final Logger logger = LoggerFactory.getLogger(VerificationCodeService.class);
    
    @Autowired
    private EmailService emailService;

    // 验证码长度
    private static final int CODE_LENGTH = 6;
    // 验证码有效期（10分钟）
    private static final int CODE_EXPIRY_MINUTES = 10;
    // 验证码发送间隔（60秒）
    private static final int CODE_SEND_INTERVAL_SECONDS = 60;

    // 验证码类型枚举
    public enum CodeType {
        REGISTER, RESET_PASSWORD
    }

    // 存储验证码信息的结构
    private static class CodeInfo {
        String code;
        LocalDateTime expiryTime;
        LocalDateTime sendTime;
        CodeType type;

        public CodeInfo(String code, LocalDateTime expiryTime, LocalDateTime sendTime, CodeType type) {
            this.code = code;
            this.expiryTime = expiryTime;
            this.sendTime = sendTime;
            this.type = type;
        }
    }

    // 存储验证码，key为邮箱地址
    private final Map<String, CodeInfo> codeStore = new ConcurrentHashMap<>();

    /**
     * 生成指定长度的随机数字验证码
     * @return 验证码字符串
     */
    private String generateCode() {
        StringBuilder sb = new StringBuilder(CODE_LENGTH);
        Random random = new Random();
        for (int i = 0; i < CODE_LENGTH; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }

    /**
     * 发送验证码
     * @param email 邮箱地址
     * @param type 验证码类型
     * @return 是否发送成功
     */
    public boolean sendVerificationCode(String email, CodeType type) {
        logger.info("准备发送验证码 - 邮箱: {}, 类型: {}", email, type);

        // 检查是否可以发送新的验证码
        if (codeStore.containsKey(email)) {
            CodeInfo existingCode = codeStore.get(email);
            // 检查是否在发送间隔内
            if (LocalDateTime.now().isBefore(existingCode.sendTime.plusSeconds(CODE_SEND_INTERVAL_SECONDS))) {
                logger.warn("验证码发送失败 - 发送频率过高: {}, 类型: {}", email, type);
                return false;
            }
        }

        // 生成新的验证码
        String code = generateCode();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiryTime = now.plusMinutes(CODE_EXPIRY_MINUTES);

        // 存储验证码信息
        codeStore.put(email, new CodeInfo(code, expiryTime, now, type));

        // 发送验证码邮件
        boolean emailSent = emailService.sendVerificationCode(email, code, type);
        
        if (emailSent) {
            logger.info("验证码已发送 - 邮箱: {}, 类型: {}, 验证码: {}, 过期时间: {}", 
                    email, type, code, expiryTime);
            return true;
        } else {
            logger.error("验证码邮件发送失败 - 邮箱: {}, 类型: {}", email, type);
            // 移除未发送成功的验证码
            codeStore.remove(email);
            return false;
        }
    }

    /**
     * 验证验证码是否有效
     * @param email 邮箱地址
     * @param code 验证码
     * @param type 验证码类型
     * @return 是否有效
     */
    public boolean verifyCode(String email, String code, CodeType type) {
        logger.info("验证验证码 - 邮箱: {}, 类型: {}, 验证码: {}", email, type, code);

        if (!codeStore.containsKey(email)) {
            logger.warn("验证码验证失败 - 验证码不存在: {}, 类型: {}", email, type);
            return false;
        }

        CodeInfo codeInfo = codeStore.get(email);

        // 检查验证码类型是否匹配
        if (codeInfo.type != type) {
            logger.warn("验证码验证失败 - 类型不匹配: {}, 期望: {}, 实际: {}", email, type, codeInfo.type);
            return false;
        }

        // 检查验证码是否过期
        if (LocalDateTime.now().isAfter(codeInfo.expiryTime)) {
            logger.warn("验证码验证失败 - 已过期: {}, 类型: {}", email, type);
            // 移除过期的验证码
            codeStore.remove(email);
            return false;
        }

        // 检查验证码是否匹配
        if (!codeInfo.code.equals(code)) {
            logger.warn("验证码验证失败 - 不正确: {}, 类型: {}", email, type);
            return false;
        }

        // 验证成功，移除验证码
        codeStore.remove(email);
        logger.info("验证码验证成功 - 邮箱: {}, 类型: {}", email, type);

        return true;
    }
}