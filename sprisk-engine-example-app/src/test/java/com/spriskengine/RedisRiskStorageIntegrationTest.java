package com.spriskengine;

import com.spriskengine.storage.RiskStorage;
import com.spriskengine.starter.storage.RedisRiskStorage;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(properties = "sprisk-example.storage.type=redis")
@AutoConfigureMockMvc
class RedisRiskStorageIntegrationTest {

    private static final DockerImageName REDIS_IMAGE = DockerImageName.parse("redis:7.2.5-alpine");

    @Container
    static final GenericContainer<?> redis = new GenericContainer<>(REDIS_IMAGE)
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
    }

    @Autowired
    private RiskStorage riskStorage;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void redisStorageIsUsedWhenConfigured() throws Exception {
        assertThat(riskStorage).isInstanceOf(RedisRiskStorage.class);

        String counterKey = "sprisk:test:counter:" + UUID.randomUUID();
        Long counterValue = riskStorage.increment(counterKey);
        assertThat(counterValue).isEqualTo(1L);
        assertThat(redisTemplate.opsForValue().get(counterKey)).isEqualTo("1");

        riskStorage.expireKey(counterKey, 5L);
        Long counterTtl = redisTemplate.getExpire(counterKey);
        assertThat(counterTtl).isNotNull().isGreaterThan(0);

        mockMvc.perform(get("/hello")
                        .param("user", "redis-user")
                        .header("X-User-Id", "redis-user"))
                .andExpect(status().isOk())
                .andExpect(content().string("Helloredis-user"));
    }
}
