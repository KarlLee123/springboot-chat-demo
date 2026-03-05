package com.example.service;

import com.alibaba.dashscope.aigc.generation.Generation;
import com.alibaba.dashscope.aigc.generation.GenerationParam;
import com.alibaba.dashscope.aigc.generation.GenerationResult;
import com.alibaba.dashscope.common.Message;
import com.alibaba.dashscope.common.Role;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
public class ChatService {

    @Value("${dashscope.api-key}")
    private String apiKey;

    public String getReply(String message) {
        Generation gen = new Generation();

        Message userMsg = Message.builder()
                .role(Role.USER.getValue())
                .content(message)
                .build();

        // 关键修正：2.0.2+ 版本用 inputMessages
        GenerationParam param = GenerationParam.builder()
                .apiKey(apiKey)
                .model("qwen-plus")
                .messages(Collections.singletonList(userMsg))
                .resultFormat("message")
                .build();

        try {
            GenerationResult result = gen.call(param);
            // 最新返回路径
            return result.getOutput().getChoices().get(0).getMessage().getContent().toString();
        } catch (ApiException e) {
            return "API调用异常: " + e.getMessage();
        } catch (NoApiKeyException e) {
            return "API Key 无效: " + e.getMessage();
        } catch (Exception e) {
            return "未知错误: " + e.getMessage();
        }
    }
}