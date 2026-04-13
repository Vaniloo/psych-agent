package com.vanilo.psych.agent.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.time.Duration;

@Service
public class UserMessageLockService {
    private final StringRedisTemplate stringRedisTemplate;
    public  UserMessageLockService(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }
    public boolean tryUserLock(String username,String message){
        String hashKey= DigestUtils.md5DigestAsHex(message.getBytes());
        String key="user:lock:message:"+username+":"+hashKey;
        Boolean success=stringRedisTemplate.opsForValue().setIfAbsent(
                key,
                "1",
                Duration.ofSeconds(3)
        );
        return Boolean.TRUE.equals(success);
    }
}
