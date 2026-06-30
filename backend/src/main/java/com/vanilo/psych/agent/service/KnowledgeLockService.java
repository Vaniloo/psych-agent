package com.vanilo.psych.agent.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Service
public class KnowledgeLockService {
    private final StringRedisTemplate stringRedisTemplate;
    public KnowledgeLockService(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }
    public boolean tryAddLock(String content) {
        String hashKey = DigestUtils.md5DigestAsHex(content.getBytes(StandardCharsets.UTF_8));
        String key = "knowledge:add:lock:content:" + hashKey;
        try {
            Boolean success = stringRedisTemplate.opsForValue().setIfAbsent(
                    key, "1", Duration.ofSeconds(10)
            );
            return Boolean.TRUE.equals(success);
        } catch (RuntimeException ignored) {
            return true;
        }
    }
}
