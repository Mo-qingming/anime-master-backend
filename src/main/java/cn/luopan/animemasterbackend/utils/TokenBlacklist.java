package cn.luopan.animemasterbackend.utils;

import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Token黑名单管理类
 * 用于存储和验证已失效的JWT令牌
 */
@Component
public class TokenBlacklist {

    // 使用线程安全的Set存储黑名单中的Token
    private final Set<String> blacklist = Collections.newSetFromMap(new ConcurrentHashMap<>());

    /**
     * 将Token加入黑名单
     * @param token JWT令牌
     */
    public void addToBlacklist(String token) {
        if (token != null) {
            blacklist.add(token);
        }
    }

    /**
     * 检查Token是否在黑名单中
     * @param token JWT令牌
     * @return true表示Token已失效，false表示Token有效
     */
    public boolean isBlacklisted(String token) {
        return token != null && blacklist.contains(token);
    }

    /**
     * 从黑名单中移除Token（可选，用于清理过期的黑名单Token）
     * @param token JWT令牌
     */
    public void removeFromBlacklist(String token) {
        if (token != null) {
            blacklist.remove(token);
        }
    }
}