package cn.luopan.animemasterbackend.service.impl;

import cn.luopan.animemasterbackend.entity.Anime;
import cn.luopan.animemasterbackend.mapper.AnimeMapper;
import cn.luopan.animemasterbackend.service.IAnimeService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AnimeServiceImpl extends ServiceImpl<AnimeMapper, Anime> implements IAnimeService {

    @Autowired
    private AnimeMapper animeMapper;

    @Autowired
    private WebClient bangumiWebClient;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Anime getAnimeByName(String name) {
        QueryWrapper<Anime> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("name", name);
        return animeMapper.selectOne(queryWrapper);
    }

    @Override
    public List<Map<String, Object>> getDailyAnime() {
        try {
            // 从Bangumi API获取每日放送动漫数据
            String response = bangumiWebClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/v0/subjects")
                            .queryParam("type", 2) // TV动画
                            .queryParam("sort", "date") // 按日期排序
                            .queryParam("limit", 50) // 获取50条数据
                            .build())
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            // 解析API响应
            JsonNode root = objectMapper.readTree(response);
            JsonNode subjects = root.path("data");

            // 保存到本地数据库
            List<Anime> animeList = new ArrayList<>();
            subjects.forEach(subject -> {
                Integer bangumiId = subject.path("id").asInt();
                
                // 检查是否已存在
                QueryWrapper<Anime> queryWrapper = new QueryWrapper<>();
                queryWrapper.eq("bangumi_id", bangumiId);
                Anime existingAnime = animeMapper.selectOne(queryWrapper);

                if (existingAnime == null) {
                    Anime anime = new Anime();
                    anime.setBangumiId(bangumiId);
                    anime.setName(subject.path("name").asText());
                    anime.setNameCn(subject.path("name_cn").asText());
                    
                    // 保存图片信息
                    JsonNode images = subject.path("images");
                    if (!images.isMissingNode()) {
                        try {
                            String imagesJson = objectMapper.writeValueAsString(images);
                            anime.setImages(imagesJson);
                        } catch (JsonProcessingException e) {
                            e.printStackTrace();
                        }
                    }

                    // 保存评分信息
                    JsonNode rating = subject.path("rating");
                    if (!rating.isMissingNode()) {
                        try {
                            String ratingJson = objectMapper.writeValueAsString(rating);
                            anime.setRating(ratingJson);
                        } catch (JsonProcessingException e) {
                            e.printStackTrace();
                        }
                    }

                    // 保存标签信息
                    JsonNode tags = subject.path("tags");
                    if (!tags.isMissingNode()) {
                        try {
                            String tagsJson = objectMapper.writeValueAsString(tags);
                            anime.setTags(tagsJson);
                        } catch (JsonProcessingException e) {
                            e.printStackTrace();
                        }
                    }

                    anime.setType(subject.path("type").asInt());
                    anime.setDate(subject.path("date").asText());
                    anime.setEps(subject.path("eps").asInt());
                    anime.setDescription(subject.path("summary").asText());
                    anime.setCreatedAt(LocalDateTime.now());
                    anime.setUpdatedAt(LocalDateTime.now());

                    animeList.add(anime);
                }
            });

            // 批量保存到数据库
            if (!animeList.isEmpty()) {
                saveBatch(animeList);
            }

            // 从数据库获取所有TV动画
            QueryWrapper<Anime> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("type", 2);
            List<Anime> allTvAnime = animeMapper.selectList(queryWrapper);

            // 随机选择20个
            Collections.shuffle(allTvAnime);
            List<Anime> randomAnime = allTvAnime.stream()
                    .limit(20)
                    .collect(Collectors.toList());

            // 转换为响应格式
            return randomAnime.stream()
                    .map(this::convertToResponseMap)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("获取每日放送动漫失败", e);
        }
    }

    @Override
    public List<Map<String, Object>> getAnimeRanking() {
        try {
            // 从Bangumi API获取排行榜数据
            String response = bangumiWebClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/v0/subjects")
                            .queryParam("type", 2) // TV动画
                            .queryParam("sort", "rank") // 按排名排序
                            .queryParam("limit", 50) // 获取50条数据
                            .build())
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            // 解析API响应
            JsonNode root = objectMapper.readTree(response);
            JsonNode subjects = root.path("data");

            // 保存到本地数据库
            List<Anime> animeList = new ArrayList<>();
            subjects.forEach(subject -> {
                Integer bangumiId = subject.path("id").asInt();
                
                // 检查是否已存在
                QueryWrapper<Anime> queryWrapper = new QueryWrapper<>();
                queryWrapper.eq("bangumi_id", bangumiId);
                Anime existingAnime = animeMapper.selectOne(queryWrapper);

                if (existingAnime == null) {
                    Anime anime = new Anime();
                    anime.setBangumiId(bangumiId);
                    anime.setName(subject.path("name").asText());
                    anime.setNameCn(subject.path("name_cn").asText());
                    
                    // 保存图片信息
                    JsonNode images = subject.path("images");
                    if (!images.isMissingNode()) {
                        try {
                            String imagesJson = objectMapper.writeValueAsString(images);
                            anime.setImages(imagesJson);
                        } catch (JsonProcessingException e) {
                            e.printStackTrace();
                        }
                    }

                    // 保存评分信息
                    JsonNode rating = subject.path("rating");
                    if (!rating.isMissingNode()) {
                        try {
                            String ratingJson = objectMapper.writeValueAsString(rating);
                            anime.setRating(ratingJson);
                        } catch (JsonProcessingException e) {
                            e.printStackTrace();
                        }
                    }

                    // 保存标签信息
                    JsonNode tags = subject.path("tags");
                    if (!tags.isMissingNode()) {
                        try {
                            String tagsJson = objectMapper.writeValueAsString(tags);
                            anime.setTags(tagsJson);
                        } catch (JsonProcessingException e) {
                            e.printStackTrace();
                        }
                    }

                    anime.setType(subject.path("type").asInt());
                    anime.setDate(subject.path("date").asText());
                    anime.setEps(subject.path("eps").asInt());
                    anime.setDescription(subject.path("summary").asText());
                    anime.setCreatedAt(LocalDateTime.now());
                    anime.setUpdatedAt(LocalDateTime.now());

                    animeList.add(anime);
                }
            });

            // 批量保存到数据库
            if (!animeList.isEmpty()) {
                saveBatch(animeList);
            }

            // 从数据库获取排名前20的TV动画
            QueryWrapper<Anime> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("type", 2)
                    .orderByAsc("rating") // 按评分排序
                    .last("LIMIT 20");
            List<Anime> topAnime = animeMapper.selectList(queryWrapper);

            // 转换为响应格式
            return topAnime.stream()
                    .map(this::convertToResponseMap)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("获取动漫排行榜失败", e);
        }
    }

    /**
     * 将Anime对象转换为响应格式的Map
     */
    private Map<String, Object> convertToResponseMap(Anime anime) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", anime.getId());
        map.put("name", anime.getName());
        map.put("name_cn", anime.getNameCn());
        
        try {
            // 解析图片信息
            if (anime.getImages() != null) {
                JsonNode images = objectMapper.readTree(anime.getImages());
                map.put("images", images);
            }

            // 解析评分信息
            if (anime.getRating() != null) {
                JsonNode rating = objectMapper.readTree(anime.getRating());
                map.put("rating", rating);
            }

            // 解析标签信息
            if (anime.getTags() != null) {
                JsonNode tags = objectMapper.readTree(anime.getTags());
                map.put("tags", tags);
            }
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

        map.put("type", anime.getType());
        map.put("date", anime.getDate());
        map.put("eps", anime.getEps());
        
        return map;
    }
}