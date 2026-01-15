package cn.luopan.animemasterbackend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.http.codec.ClientCodecConfigurer;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

/**
 * WebClient配置类
 */
@Configuration
public class WebClientConfig {

    @Bean
    public WebClient bangumiWebClient() {
        return WebClient.builder()
                .baseUrl("https://api.bgm.tv")
                .defaultHeader("User-Agent", "AnimeMaster/1.0.0")
                .codecs(configurer -> {
                    // 增加缓冲区大小限制，设置为1MB
                    configurer.defaultCodecs().maxInMemorySize(1024 * 1024);
                })
                .build();
    }
}