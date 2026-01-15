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
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.Arrays;
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

            // 提取所有subjects并收集bangumi_id
            List<JsonNode> allSubjects = new ArrayList<>();
            List<Integer> bangumiIds = new ArrayList<>();
            subjects.forEach(subject -> {
                allSubjects.add(subject);
                bangumiIds.add(subject.path("id").asInt());
            });

            // 批量查询本地数据库中的动漫信息
            Map<Integer, Anime> existingAnimeMap = new HashMap<>();
            if (!bangumiIds.isEmpty()) {
                QueryWrapper<Anime> queryWrapper = new QueryWrapper<>();
                queryWrapper.in("bangumi_id", bangumiIds);
                List<Anime> existingAnimes = animeMapper.selectList(queryWrapper);
                existingAnimes.forEach(anime -> existingAnimeMap.put(anime.getBangumiId(), anime));
            }

            // 处理数据：创建新动漫或更新现有动漫
            List<Anime> newAnimeList = new ArrayList<>();
            List<Anime> updateAnimeList = new ArrayList<>();
            LocalDate oneYearAgo = LocalDate.now().minusYears(1);

            for (JsonNode subject : allSubjects) {
                Integer bangumiId = subject.path("id").asInt();
                Anime existingAnime = existingAnimeMap.get(bangumiId);

                if (existingAnime == null) {
                    // 本地不存在，创建新动漫
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

                    newAnimeList.add(anime);
                } else {
                    // 本地存在，根据date字段判断是否需要更新
                    String dateStr = subject.path("date").asText();
                    if (!dateStr.isEmpty()) {
                        try {
                            // 解析date字段（格式可能为"2023-04"或"2023-04-10"）
                            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM[-dd]");
                            LocalDate animeDate = LocalDate.parse(dateStr, formatter);
                            
                            // 如果date字段大于一年前，则更新
                            if (!animeDate.isBefore(oneYearAgo)) {
                                // 更新现有动漫
                                Anime updateAnime = existingAnime;
                                updateAnime.setName(subject.path("name").asText());
                                updateAnime.setNameCn(subject.path("name_cn").asText());
                                
                                // 更新图片信息
                                JsonNode images = subject.path("images");
                                if (!images.isMissingNode()) {
                                    try {
                                        String imagesJson = objectMapper.writeValueAsString(images);
                                        updateAnime.setImages(imagesJson);
                                    } catch (JsonProcessingException e) {
                                        e.printStackTrace();
                                    }
                                }

                                // 更新评分信息
                                JsonNode rating = subject.path("rating");
                                if (!rating.isMissingNode()) {
                                    try {
                                        String ratingJson = objectMapper.writeValueAsString(rating);
                                        updateAnime.setRating(ratingJson);
                                    } catch (JsonProcessingException e) {
                                        e.printStackTrace();
                                    }
                                }

                                // 更新标签信息
                                JsonNode tags = subject.path("tags");
                                if (!tags.isMissingNode()) {
                                    try {
                                        String tagsJson = objectMapper.writeValueAsString(tags);
                                        updateAnime.setTags(tagsJson);
                                    } catch (JsonProcessingException e) {
                                        e.printStackTrace();
                                    }
                                }

                                updateAnime.setType(subject.path("type").asInt());
                                updateAnime.setDate(subject.path("date").asText());
                                updateAnime.setEps(subject.path("eps").asInt());
                                updateAnime.setDescription(subject.path("summary").asText());
                                updateAnime.setUpdatedAt(LocalDateTime.now());

                                updateAnimeList.add(updateAnime);
                            }
                        } catch (DateTimeParseException e) {
                            // 如果日期解析失败，跳过更新
                            e.printStackTrace();
                        }
                    }
                }
            }

            // 批量保存新动漫到数据库
            if (!newAnimeList.isEmpty()) {
                saveBatch(newAnimeList);
            }

            // 批量更新现有动漫到数据库
            if (!updateAnimeList.isEmpty()) {
                updateBatchById(updateAnimeList);
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

            // 提取所有subjects并收集bangumi_id
            List<JsonNode> allSubjects = new ArrayList<>();
            List<Integer> bangumiIds = new ArrayList<>();
            subjects.forEach(subject -> {
                allSubjects.add(subject);
                bangumiIds.add(subject.path("id").asInt());
            });

            // 批量查询本地数据库中的动漫信息
            Map<Integer, Anime> existingAnimeMap = new HashMap<>();
            if (!bangumiIds.isEmpty()) {
                QueryWrapper<Anime> queryWrapper = new QueryWrapper<>();
                queryWrapper.in("bangumi_id", bangumiIds);
                List<Anime> existingAnimes = animeMapper.selectList(queryWrapper);
                existingAnimes.forEach(anime -> existingAnimeMap.put(anime.getBangumiId(), anime));
            }

            // 处理数据：创建新动漫或更新现有动漫
            List<Anime> newAnimeList = new ArrayList<>();
            List<Anime> updateAnimeList = new ArrayList<>();
            LocalDate oneYearAgo = LocalDate.now().minusYears(1);

            for (JsonNode subject : allSubjects) {
                Integer bangumiId = subject.path("id").asInt();
                Anime existingAnime = existingAnimeMap.get(bangumiId);

                if (existingAnime == null) {
                    // 本地不存在，创建新动漫
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

                    newAnimeList.add(anime);
                } else {
                    // 本地存在，根据date字段判断是否需要更新
                    String dateStr = subject.path("date").asText();
                    if (!dateStr.isEmpty()) {
                        try {
                            // 解析date字段（格式可能为"2023-04"或"2023-04-10"）
                            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM[-dd]");
                            LocalDate animeDate = LocalDate.parse(dateStr, formatter);
                            
                            // 如果date字段大于一年前，则更新
                            if (!animeDate.isBefore(oneYearAgo)) {
                                // 更新现有动漫
                                Anime updateAnime = existingAnime;
                                updateAnime.setName(subject.path("name").asText());
                                updateAnime.setNameCn(subject.path("name_cn").asText());
                                
                                // 更新图片信息
                                JsonNode images = subject.path("images");
                                if (!images.isMissingNode()) {
                                    try {
                                        String imagesJson = objectMapper.writeValueAsString(images);
                                        updateAnime.setImages(imagesJson);
                                    } catch (JsonProcessingException e) {
                                        e.printStackTrace();
                                    }
                                }

                                // 更新评分信息
                                JsonNode rating = subject.path("rating");
                                if (!rating.isMissingNode()) {
                                    try {
                                        String ratingJson = objectMapper.writeValueAsString(rating);
                                        updateAnime.setRating(ratingJson);
                                    } catch (JsonProcessingException e) {
                                        e.printStackTrace();
                                    }
                                }

                                // 更新标签信息
                                JsonNode tags = subject.path("tags");
                                if (!tags.isMissingNode()) {
                                    try {
                                        String tagsJson = objectMapper.writeValueAsString(tags);
                                        updateAnime.setTags(tagsJson);
                                    } catch (JsonProcessingException e) {
                                        e.printStackTrace();
                                    }
                                }

                                updateAnime.setType(subject.path("type").asInt());
                                updateAnime.setDate(subject.path("date").asText());
                                updateAnime.setEps(subject.path("eps").asInt());
                                updateAnime.setDescription(subject.path("summary").asText());
                                updateAnime.setUpdatedAt(LocalDateTime.now());

                                updateAnimeList.add(updateAnime);
                            }
                        } catch (DateTimeParseException e) {
                            // 如果日期解析失败，跳过更新
                            e.printStackTrace();
                        }
                    }
                }
            }

            // 批量保存新动漫到数据库
            if (!newAnimeList.isEmpty()) {
                saveBatch(newAnimeList);
            }

            // 批量更新现有动漫到数据库
            if (!updateAnimeList.isEmpty()) {
                updateBatchById(updateAnimeList);
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

    @Override
    public List<Map<String, Object>> searchAnime(String keyword, Integer limit, Integer offset) {
        try {
            // 设置默认值并创建最终变量用于lambda表达式
            final Integer finalLimit = (limit == null) ? 20 : limit;
            final Integer finalOffset = (offset == null) ? 0 : offset;

            // 从Bangumi API获取搜索结果
            // 创建搜索请求体
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("keyword", keyword);
            
            // 创建筛选条件
            Map<String, Object> filter = new HashMap<>();
            filter.put("type", Arrays.asList(2)); // TV动画
            requestBody.put("filter", filter);
            
            // 发送POST请求
            String response = bangumiWebClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .path("/v0/search/subjects")
                            .queryParam("limit", finalLimit) // 返回数据数量
                            .queryParam("offset", finalOffset) // 偏移量
                            .build())
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            // 解析API响应
            JsonNode root = objectMapper.readTree(response);
            JsonNode subjects = root.path("data");

            // 提取所有subjects并收集bangumi_id
            List<JsonNode> allSubjects = new ArrayList<>();
            List<Integer> bangumiIds = new ArrayList<>();
            subjects.forEach(subject -> {
                allSubjects.add(subject);
                bangumiIds.add(subject.path("id").asInt());
            });

            // 批量查询本地数据库中的动漫信息
            Map<Integer, Anime> existingAnimeMap = new HashMap<>();
            if (!bangumiIds.isEmpty()) {
                QueryWrapper<Anime> queryWrapper = new QueryWrapper<>();
                queryWrapper.in("bangumi_id", bangumiIds);
                List<Anime> existingAnimes = animeMapper.selectList(queryWrapper);
                existingAnimes.forEach(anime -> existingAnimeMap.put(anime.getBangumiId(), anime));
            }

            // 处理数据：创建新动漫或更新现有动漫
            List<Anime> newAnimeList = new ArrayList<>();
            List<Anime> updateAnimeList = new ArrayList<>();
            LocalDate oneYearAgo = LocalDate.now().minusYears(1);

            for (JsonNode subject : allSubjects) {
                Integer bangumiId = subject.path("id").asInt();
                Anime existingAnime = existingAnimeMap.get(bangumiId);

                if (existingAnime == null) {
                    // 本地不存在，创建新动漫
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

                    newAnimeList.add(anime);
                } else {
                    // 本地存在，根据date字段判断是否需要更新
                    String dateStr = subject.path("date").asText();
                    if (!dateStr.isEmpty()) {
                        try {
                            // 解析date字段（格式可能为"2023-04"或"2023-04-10"）
                            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM[-dd]");
                            LocalDate animeDate = LocalDate.parse(dateStr, formatter);
                            
                            // 如果date字段大于一年前，则更新
                            if (!animeDate.isBefore(oneYearAgo)) {
                                // 更新现有动漫
                                Anime updateAnime = existingAnime;
                                updateAnime.setName(subject.path("name").asText());
                                updateAnime.setNameCn(subject.path("name_cn").asText());
                                
                                // 更新图片信息
                                JsonNode images = subject.path("images");
                                if (!images.isMissingNode()) {
                                    try {
                                        String imagesJson = objectMapper.writeValueAsString(images);
                                        updateAnime.setImages(imagesJson);
                                    } catch (JsonProcessingException e) {
                                        e.printStackTrace();
                                    }
                                }

                                // 更新评分信息
                                JsonNode rating = subject.path("rating");
                                if (!rating.isMissingNode()) {
                                    try {
                                        String ratingJson = objectMapper.writeValueAsString(rating);
                                        updateAnime.setRating(ratingJson);
                                    } catch (JsonProcessingException e) {
                                        e.printStackTrace();
                                    }
                                }

                                // 更新标签信息
                                JsonNode tags = subject.path("tags");
                                if (!tags.isMissingNode()) {
                                    try {
                                        String tagsJson = objectMapper.writeValueAsString(tags);
                                        updateAnime.setTags(tagsJson);
                                    } catch (JsonProcessingException e) {
                                        e.printStackTrace();
                                    }
                                }

                                updateAnime.setType(subject.path("type").asInt());
                                updateAnime.setDate(subject.path("date").asText());
                                updateAnime.setEps(subject.path("eps").asInt());
                                updateAnime.setDescription(subject.path("summary").asText());
                                updateAnime.setUpdatedAt(LocalDateTime.now());

                                updateAnimeList.add(updateAnime);
                            }
                        } catch (DateTimeParseException e) {
                            // 如果日期解析失败，跳过更新
                            e.printStackTrace();
                        }
                    }
                }
            }

            // 批量保存新动漫到数据库
            if (!newAnimeList.isEmpty()) {
                saveBatch(newAnimeList);
            }

            // 批量更新现有动漫到数据库
            if (!updateAnimeList.isEmpty()) {
                updateBatchById(updateAnimeList);
            }

            // 获取搜索结果中的bangumi_id列表（已在前面定义）

            // 从数据库获取对应的动漫信息
            if (!bangumiIds.isEmpty()) {
                QueryWrapper<Anime> queryWrapper = new QueryWrapper<>();
                queryWrapper.in("bangumi_id", bangumiIds);
                List<Anime> searchResults = animeMapper.selectList(queryWrapper);

                // 按照搜索结果的顺序排序
                Map<Integer, Anime> animeMap = searchResults.stream()
                        .collect(Collectors.toMap(Anime::getBangumiId, anime -> anime));

                List<Anime> orderedResults = new ArrayList<>();
                for (Integer bangumiId : bangumiIds) {
                    if (animeMap.containsKey(bangumiId)) {
                        orderedResults.add(animeMap.get(bangumiId));
                    }
                }

                // 转换为响应格式
                return orderedResults.stream()
                        .map(this::convertToResponseMap)
                        .collect(Collectors.toList());
            }

            return Collections.emptyList();

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("搜索动漫失败", e);
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