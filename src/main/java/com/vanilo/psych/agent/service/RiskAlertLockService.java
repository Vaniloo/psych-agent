package com.vanilo.psych.agent.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class RiskAlertLockService {
    private final StringRedisTemplate stringRedisTemplate;
    public RiskAlertLockService(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }
    public boolean tryLock(String username,String message){
        String messageHash=String.valueOf(message.hashCode());
        String key="risk:lock:"+username+":"+messageHash;
        Boolean success=stringRedisTemplate.opsForValue().setIfAbsent(key
        ,"1", Duration.ofSeconds(30));
        return Boolean.TRUE.equals(success);
    }
}
