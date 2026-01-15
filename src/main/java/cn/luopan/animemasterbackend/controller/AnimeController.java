package cn.luopan.animemasterbackend.controller;

import cn.luopan.animemasterbackend.service.IAnimeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 动漫接口控制器
 */
@RestController
@RequestMapping("/api/anime")
public class AnimeController {

    @Autowired
    private IAnimeService animeService;

    /**
     * 获取每日放送动漫列表
     * @return 每日放送动漫数据
     */
    @GetMapping("/daily")
    public ResponseEntity<Map<String, Object>> getDailyAnime() {
        try {
            List<Map<String, Object>> animeList = animeService.getDailyAnime();
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "获取成功");
            response.put("data", animeList);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "获取失败: " + e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 获取动漫排行榜
     * @return 动漫排行榜数据
     */
    @GetMapping("/ranking")
    public ResponseEntity<Map<String, Object>> getAnimeRanking() {
        try {
            List<Map<String, Object>> animeList = animeService.getAnimeRanking();
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "获取成功");
            response.put("data", animeList);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "获取失败: " + e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 搜索动漫
     * @return 搜索结果
     */
    @GetMapping("/search")
    public ResponseEntity<Map<String, Object>> searchAnime(@RequestParam String keyword, 
                                           @RequestParam(required = false) Integer limit, 
                                           @RequestParam(required = false) Integer offset) {
        try {
            List<Map<String, Object>> searchResults = animeService.searchAnime(keyword, limit, offset);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "搜索成功");
            response.put("data", searchResults);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "搜索失败: " + e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}