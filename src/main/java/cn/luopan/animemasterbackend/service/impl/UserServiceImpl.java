package cn.luopan.animemasterbackend.service.impl;

import cn.luopan.animemasterbackend.entity.User;
import cn.luopan.animemasterbackend.mapper.UserMapper;
import cn.luopan.animemasterbackend.service.IUserService;
import cn.luopan.animemasterbackend.utils.JwtUtils;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    private static final Logger logger = LoggerFactory.getLogger(UserServiceImpl.class);

    @Autowired
    private UserMapper userMapper;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    private JwtUtils jwtUtils;
    
    // 邮箱格式正则表达式
    private static final String EMAIL_REGEX = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$";
    private static final Pattern EMAIL_PATTERN = Pattern.compile(EMAIL_REGEX);
    
    // 密码最小长度
    private static final int MIN_PASSWORD_LENGTH = 6;
    
    // 登录失败次数限制
    private static final int MAX_LOGIN_FAIL_COUNT = 5;
    
    // 账号锁定时间（30分钟）
    private static final int ACCOUNT_LOCK_MINUTES = 30;

    @Override
    public User getUserByUsername(String username) {
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("username", username);
        return userMapper.selectOne(queryWrapper);
    }
    
    @Override
    public User getUserByEmail(String email) {
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("email", email);
        return userMapper.selectOne(queryWrapper);
    }
    
    @Override
    public User getUserByUsernameOrEmail(String usernameOrEmail) {
        // 判断输入是邮箱还是用户名
        if (EMAIL_PATTERN.matcher(usernameOrEmail).matches()) {
            // 是邮箱
            return getUserByEmail(usernameOrEmail);
        } else {
            // 是用户名
            return getUserByUsername(usernameOrEmail);
        }
    }
    
    @Override
    public User registerUser(String username, String email, String password) {
        logger.info("开始注册用户 - 用户名: {}, 邮箱: {}", username, email);
        
        // 检查用户名是否已存在
        if (getUserByUsername(username) != null) {
            logger.warn("用户注册失败 - 用户名 '{}' 已存在", username);
            throw new RuntimeException("用户名已存在");
        }
        
        // 检查邮箱是否已存在
        if (getUserByEmail(email) != null) {
            logger.warn("用户注册失败 - 邮箱 '{}' 已被注册", email);
            throw new RuntimeException("邮箱已被注册");
        }
        
        // 检查邮箱格式是否正确
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            logger.warn("用户注册失败 - 邮箱 '{}' 格式不正确", email);
            throw new RuntimeException("邮箱格式不正确");
        }
        
        // 检查密码长度
        if (password == null || password.length() < MIN_PASSWORD_LENGTH) {
            logger.warn("用户注册失败 - 密码长度不足");
            throw new RuntimeException("密码长度不少于6位");
        }
        
        // 创建用户对象
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password)); // 对密码进行加密
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        user.setDisabled(false); // 默认不禁用
        user.setLoginFailCount(0); // 默认登录失败次数为0
        
        // 保存用户
        userMapper.insert(user);
        
        logger.info("用户注册成功 - 用户名: {}, 邮箱: {}, 用户ID: {}", username, email, user.getId());
        
        return user;
    }
    
    @Override
    public Map<String, Object> loginUser(String usernameOrEmail, String password) {
        logger.info("开始用户登录 - 用户名/邮箱: {}", usernameOrEmail);
        
        // 根据用户名或邮箱查找用户
        User user = getUserByUsernameOrEmail(usernameOrEmail);
        
        // 检查用户是否存在
        if (user == null) {
            logger.warn("用户登录失败 - 用户不存在: {}", usernameOrEmail);
            throw new RuntimeException("用户不存在");
        }
        
        // 检查用户是否被禁用
        if (user.getDisabled() != null && user.getDisabled()) {
            logger.warn("用户登录失败 - 账号已被禁用: {}", usernameOrEmail);
            throw new RuntimeException("该账号已被禁用");
        }
        
        // 检查用户账号是否被锁定
        if (isAccountLocked(user)) {
            logger.warn("用户登录失败 - 账号已被锁定: {}", usernameOrEmail);
            throw new RuntimeException("该账号已被锁定，请30分钟后再试");
        }
        
        // 验证密码
        if (!passwordEncoder.matches(password, user.getPassword())) {
            // 密码错误，增加登录失败次数
            increaseLoginFailCount(user);
            logger.warn("用户登录失败 - 密码错误，当前失败次数: {}, 用户名/邮箱: {}", user.getLoginFailCount(), usernameOrEmail);
            
            // 检查是否达到锁定阈值
            if (user.getLoginFailCount() >= MAX_LOGIN_FAIL_COUNT) {
                // 锁定账号
                lockUserAccount(user);
                logger.warn("用户账号已被锁定 - 用户名/邮箱: {}, 锁定时间: {}", usernameOrEmail, user.getLockedUntil());
                throw new RuntimeException("该账号已被锁定，请30分钟后再试");
            }
            throw new RuntimeException("用户名或密码错误");
        }
        
        // 登录成功，重置登录失败次数
        resetLoginFailCount(user);
        
        // 更新最后登录时间
        user.setLastLoginTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(user);
        
        // 生成Token
        String token = jwtUtils.generateToken(user.getId(), user.getUsername(), user.getEmail());
        
        // 构造返回结果
        Map<String, Object> result = new HashMap<>();
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("userId", user.getId());
        userInfo.put("username", user.getUsername());
        userInfo.put("email", user.getEmail());
        userInfo.put("token", token);
        
        result.put("user", userInfo);
        result.put("token", token);
        
        logger.info("用户登录成功 - 用户ID: {}, 用户名: {}, 登录时间: {}", user.getId(), user.getUsername(), user.getLastLoginTime());
        
        return result;
    }
    
    @Override
    public void increaseLoginFailCount(User user) {
        if (user.getLoginFailCount() == null) {
            user.setLoginFailCount(1);
        } else {
            user.setLoginFailCount(user.getLoginFailCount() + 1);
        }
        user.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(user);
        logger.debug("更新登录失败次数 - 用户ID: {}, 失败次数: {}", user.getId(), user.getLoginFailCount());
    }
    
    @Override
    public void resetLoginFailCount(User user) {
        user.setLoginFailCount(0);
        user.setLockedUntil(null); // 清除锁定时间
        user.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(user);
        logger.debug("重置登录失败次数 - 用户ID: {}", user.getId());
    }
    
    @Override
    public void lockUserAccount(User user) {
        user.setLockedUntil(LocalDateTime.now().plusMinutes(ACCOUNT_LOCK_MINUTES));
        user.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(user);
        logger.info("锁定用户账号 - 用户ID: {}, 锁定时间: {}", user.getId(), user.getLockedUntil());
    }
    
    @Override
    public boolean isAccountLocked(User user) {
        if (user.getLockedUntil() == null) {
            return false;
        }
        // 检查锁定时间是否已过期
        boolean isLocked = user.getLockedUntil().isAfter(LocalDateTime.now());
        if (isLocked) {
            logger.debug("用户账号被锁定 - 用户ID: {}, 锁定时间: {}", user.getId(), user.getLockedUntil());
        }
        return isLocked;
    }
    
    @Override
    public boolean resetPassword(String email, String newPassword) {
        logger.info("开始重置密码 - 邮箱: {}", email);
        
        // 检查密码长度
        if (newPassword == null || newPassword.length() < MIN_PASSWORD_LENGTH) {
            logger.warn("密码重置失败 - 密码长度不足");
            throw new RuntimeException("密码长度不少于6位");
        }
        
        // 查找用户
        User user = getUserByEmail(email);
        if (user == null) {
            logger.warn("密码重置失败 - 用户不存在: {}", email);
            throw new RuntimeException("用户不存在");
        }
        
        // 更新密码
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setUpdateTime(LocalDateTime.now());
        
        // 保存更新
        int updated = userMapper.updateById(user);
        
        if (updated > 0) {
            logger.info("密码重置成功 - 用户ID: {}, 邮箱: {}", user.getId(), email);
            return true;
        } else {
            logger.error("密码重置失败 - 更新数据库失败: {}", email);
            return false;
        }
    }
}