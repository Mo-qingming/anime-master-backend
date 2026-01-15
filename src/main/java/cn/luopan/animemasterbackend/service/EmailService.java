package cn.luopan.animemasterbackend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * 邮件发送服务
 */
@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${spring.mail.nickname:AnimeMaster}")
    private String fromNickname;

    /**
     * 发送验证码邮件
     * @param toEmail 收件人邮箱
     * @param code 验证码
     * @param type 验证码类型
     * @return 是否发送成功
     */
    public boolean sendVerificationCode(String toEmail, String code, VerificationCodeService.CodeType type) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);

            // 设置邮件主题
            String subject;
            if (type == VerificationCodeService.CodeType.REGISTER) {
                subject = fromNickname + " - 注册验证码";
            } else if (type == VerificationCodeService.CodeType.RESET_PASSWORD) {
                subject = fromNickname + " - 密码重置验证码";
            } else {
                subject = fromNickname + " - 验证码";
            }
            message.setSubject(subject);

            // 设置邮件内容
            String content = String.format(
                    "您好！\n\n您正在%s，您的验证码是：%s\n\n验证码有效期为10分钟，请及时使用。\n\n如果您没有进行此操作，请忽略此邮件。\n\n%s团队",
                    type == VerificationCodeService.CodeType.REGISTER ? "注册" + fromNickname + "账号" : "重置" + fromNickname + "账号密码",
                    code,
                    fromNickname
            );
            message.setText(content);

            // 发送邮件
            mailSender.send(message);
            logger.info("验证码邮件发送成功 - 收件人: {}, 类型: {}", toEmail, type);
            return true;
        } catch (Exception e) {
            logger.error("验证码邮件发送失败 - 收件人: {}, 类型: {}, 错误: {}", toEmail, type, e.getMessage(), e);
            return false;
        }
    }
}