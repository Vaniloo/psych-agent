package com.vanilo.psych.agent.service;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;


@Service
public class TextChunkService {
    public List<String> splitText(String text,int chunkSize,int overlap){
        if (text == null || text.isBlank()) {
            throw new RuntimeException("text不能为空");
        }
        if (chunkSize <= 0) {
            throw new RuntimeException("chunkSize必须大于0");
        }
        if (overlap < 0) {
            throw new RuntimeException("overlap不能小于0");
        }
        if (overlap >= chunkSize) {
            throw new RuntimeException("overlap必须小于chunkSize");
        }
        List<String> result = new ArrayList<>();
        int start = 0;
        while(start<text.length()){
            int end = Math.min(start + chunkSize, text.length());
            result.add(text.substring(start, end));
            if(end==text.length()){
                break;
            }
            start = end-overlap;
        }
        return result;

    }
}
