package cn.luopan.animemasterbackend.service;

import cn.luopan.animemasterbackend.entity.User;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.Map;

public interface IUserService extends IService<User> {
    /**
     * 根据用户名查找用户
     * @param username 用户名
     * @return 用户对象
     */
    User getUserByUsername(String username);
    
    /**
     * 根据邮箱查找用户
     * @param email 邮箱
     * @return 用户对象
     */
    User getUserByEmail(String email);
    
    /**
     * 用户注册
     * @param username 用户名
     * @param email 邮箱
     * @param password 密码
     * @return 注册后的用户对象
     */
    User registerUser(String username, String email, String password);
    
    /**
     * 用户登录
     * @param usernameOrEmail 用户名或邮箱
     * @param password 密码
     * @return 包含用户信息和Token的Map
     */
    Map<String, Object> loginUser(String usernameOrEmail, String password);
    
    /**
     * 根据用户名或邮箱查找用户
     * @param usernameOrEmail 用户名或邮箱
     * @return 用户对象
     */
    User getUserByUsernameOrEmail(String usernameOrEmail);
    
    /**
     * 增加登录失败次数
     * @param user 用户对象
     */
    void increaseLoginFailCount(User user);
    
    /**
     * 重置登录失败次数
     * @param user 用户对象
     */
    void resetLoginFailCount(User user);
    
    /**
     * 锁定用户账号
     * @param user 用户对象
     */
    void lockUserAccount(User user);
    
    /**
     * 检查用户账号是否被锁定
     * @param user 用户对象
     * @return 是否被锁定
     */
    boolean isAccountLocked(User user);
    
    /**
     * 重置用户密码
     * @param email 邮箱地址
     * @param newPassword 新密码
     * @return 是否重置成功
     */
    boolean resetPassword(String email, String newPassword);
}