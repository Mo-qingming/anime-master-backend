package cn.luopan.animemasterbackend.controller;

import cn.luopan.animemasterbackend.entity.UserAnimeStatus;
import cn.luopan.animemasterbackend.service.IUserAnimeStatusService;
import cn.luopan.animemasterbackend.utils.JwtUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/collection")
public class CollectionController {

    @Autowired
    private IUserAnimeStatusService userAnimeStatusService;
    
    @Autowired
    private JwtUtils jwtUtils;

    /**
     * 获取当前用户的动漫收藏列表
     * 按状态分类返回：想看、在看、已看、弃置
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getCollections(HttpServletRequest request) {
        // 从JWT中获取用户ID
        Long userId = getUserIdFromToken(request);
        
        // 获取按状态分类的收藏列表
        Map<String, Object> response = new HashMap<>();
        Map<String, Object> data = new HashMap<>();
        
        try {
            Map<String, Object> collections = new HashMap<>(userAnimeStatusService.getCollectionsByStatus(userId));
            
            response.put("success", true);
            response.put("message", "获取收藏列表成功");
            data.put("wantToWatch", collections.getOrDefault("wantToWatch", new java.util.ArrayList<>()));
            data.put("watching", collections.getOrDefault("watching", new java.util.ArrayList<>()));
            data.put("watched", collections.getOrDefault("watched", new java.util.ArrayList<>()));
            data.put("dropped", collections.getOrDefault("dropped", new java.util.ArrayList<>()));
            response.put("data", data);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "获取收藏列表失败: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 添加动漫到用户收藏列表
     */
    @PostMapping("/add")
    public ResponseEntity<Map<String, Object>> addToCollection(@RequestBody UserAnimeStatus userAnimeStatus, HttpServletRequest request) {
        // 从JWT中获取用户ID
        Long userId = getUserIdFromToken(request);
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            // 验证必填字段
            if (userAnimeStatus.getAnimeId() == null || userAnimeStatus.getTitle() == null) {
                response.put("success", false);
                response.put("message", "参数错误，缺少必填字段");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 设置默认状态
            if (userAnimeStatus.getStatus() == null) {
                userAnimeStatus.setStatus("wantToWatch");
            }
            
            // 设置用户ID
            userAnimeStatus.setUserId(userId);
            
            // 添加到收藏
            boolean result = userAnimeStatusService.addToCollection(userAnimeStatus);
            
            if (result) {
                response.put("success", true);
                response.put("message", "添加到收藏成功");
                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("message", "该动漫已在收藏列表中");
                return ResponseEntity.badRequest().body(response);
            }
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "添加到收藏失败: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 更新收藏动漫的状态
     */
    @PostMapping("/update")
    public ResponseEntity<Map<String, Object>> updateCollectionStatus(@RequestBody Map<String, Object> requestBody, HttpServletRequest request) {
        // 从JWT中获取用户ID
        Long userId = getUserIdFromToken(request);
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            // 验证必填字段
            if (!requestBody.containsKey("animeId") || !requestBody.containsKey("status")) {
                response.put("success", false);
                response.put("message", "参数错误，缺少必填字段");
                return ResponseEntity.badRequest().body(response);
            }
            
            Long animeId = Long.valueOf(requestBody.get("animeId").toString());
            String status = requestBody.get("status").toString();
            Integer progress = requestBody.containsKey("progress") ? Integer.valueOf(requestBody.get("progress").toString()) : null;
            
            // 直接使用前端传入的状态值，不进行映射
            boolean result = userAnimeStatusService.updateCollectionStatus(userId, animeId, status, progress);
            
            if (result) {
                response.put("success", true);
                response.put("message", "收藏状态更新成功");
                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("message", "该动漫不在收藏列表中");
                return ResponseEntity.badRequest().body(response);
            }
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "更新收藏状态失败: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 从用户收藏列表中移除动漫
     */
    @PostMapping("/remove")
    public ResponseEntity<Map<String, Object>> removeFromCollection(@RequestBody Map<String, Object> requestBody, HttpServletRequest request) {
        // 从JWT中获取用户ID
        Long userId = getUserIdFromToken(request);
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            // 验证必填字段
            if (!requestBody.containsKey("animeId")) {
                response.put("success", false);
                response.put("message", "参数错误，缺少必填字段");
                return ResponseEntity.badRequest().body(response);
            }
            
            Long animeId = Long.valueOf(requestBody.get("animeId").toString());
            
            // 从收藏中移除
            boolean result = userAnimeStatusService.removeFromCollection(userId, animeId);
            
            if (result) {
                response.put("success", true);
                response.put("message", "从收藏中移除成功");
                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("message", "该动漫不在收藏列表中");
                return ResponseEntity.badRequest().body(response);
            }
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "从收藏中移除失败: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
    
    /**
     * 从请求头的JWT令牌中获取用户ID
     * @param request HTTP请求对象
     * @return 用户ID
     */
    private Long getUserIdFromToken(HttpServletRequest request) {
        // 获取Authorization头
        String authorizationHeader = request.getHeader("Authorization");
        
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            String token = authorizationHeader.substring(7);
            return jwtUtils.getUserIdFromToken(token);
        }
        
        throw new RuntimeException("无效的令牌");
    }
}