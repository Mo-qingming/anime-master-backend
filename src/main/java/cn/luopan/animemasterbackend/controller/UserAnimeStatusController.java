package cn.luopan.animemasterbackend.controller;

import cn.luopan.animemasterbackend.entity.UserAnimeStatus;
import cn.luopan.animemasterbackend.service.IUserAnimeStatusService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/user-anime-status")
public class UserAnimeStatusController {

    @Autowired
    private IUserAnimeStatusService userAnimeStatusService;

    /**
     * 获取用户特定状态的动漫列表
     */
    @GetMapping("/user/{userId}/status/{status}")
    public ResponseEntity<List<UserAnimeStatus>> getUserAnimeListByStatus(
            @PathVariable Long userId,
            @PathVariable String status) {
        List<UserAnimeStatus> userAnimeList = userAnimeStatusService.getUserAnimeListByStatus(userId, status);
        return ResponseEntity.ok(userAnimeList);
    }

    /**
     * 更新用户动漫状态（想看、在看、已看、弃置）
     */
    @PostMapping("/update-status")
    public ResponseEntity<Map<String, Object>> updateUserAnimeStatus(
            @RequestParam Long userId,
            @RequestParam Long animeId,
            @RequestParam String status) {
        
        boolean result = userAnimeStatusService.updateUserAnimeStatus(userId, animeId, status);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", result);
        response.put("message", result ? "状态更新成功" : "状态更新失败");
        
        return ResponseEntity.ok(response);
    }

    /**
     * 更新动漫观看进度
     */
    @PostMapping("/update-progress")
    public ResponseEntity<Map<String, Object>> updateAnimeProgress(
            @RequestParam Long userId,
            @RequestParam Long animeId,
            @RequestParam Integer progress) {
        
        boolean result = userAnimeStatusService.updateAnimeProgress(userId, animeId, progress);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", result);
        response.put("message", result ? "进度更新成功" : "进度更新失败");
        
        return ResponseEntity.ok(response);
    }

    /**
     * 获取用户的所有动漫状态
     */
    @GetMapping("/user/{userId}/all")
    public ResponseEntity<List<UserAnimeStatus>> getAllUserAnimeStatus(@PathVariable Long userId) {
        List<UserAnimeStatus> allStatus = userAnimeStatusService.getAllUserAnimeStatus(userId);
        return ResponseEntity.ok(allStatus);
    }
}