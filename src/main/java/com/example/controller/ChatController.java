package com.example.controller;

import com.example.service.ChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/chat")
public class ChatController {

    @Autowired
    private ChatService chatService;

    // 线程安全 Map，存储用户剩余免费次数（key: userId, value: 剩余次数）
    private static final Map<String, Integer> userRemaining = new ConcurrentHashMap<>();
    private static final int FREE_LIMIT = 3;  // 每天免费3次

    @PostMapping
    public ResponseEntity<Map<String, Object>> chat(@RequestBody Map<String, String> request) {
        String message = request.get("message");
        if (message == null || message.trim().isEmpty()) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "消息不能为空");
            return ResponseEntity.badRequest().body(error);
        }

        // 用户标识：测试用固定ID，实际项目换成 JWT userId 或 request.getRemoteAddr()
        String userId = "test-user";  // 生产环境替换为真实用户ID

        // 原子获取/初始化剩余次数
        int remaining = userRemaining.compute(userId, (key, oldValue) ->
                oldValue == null ? FREE_LIMIT : oldValue
        );

        if (remaining <= 0) {
            Map<String, Object> resp = new HashMap<>();
            resp.put("error", "免费次数已用完，请付费解锁无限生成");
            resp.put("remaining", 0);
            return ResponseEntity.ok(resp);
        }

        String mode = request.get("mode");
        String style = request.getOrDefault("style", "自然亲切");

        long startTime = System.currentTimeMillis();

        String reply;
        if ("wenan".equals(mode)) {
            // 精简 prompt，减少 token 消耗和推理时间（关键优化！）
            String prompt = String.format(
                    "基于主题：%s，风格：%s，生成3条小红书/朋友圈/抖音短文案（标题+正文+表情/标签），" +
                            "每条简洁吸引人，直接输出编号1.2.3.格式，不要多余解释。",
                    message, style
            );
            reply = chatService.getReply(prompt);
        } else {
            reply = chatService.getReply(message);
        }

        long endTime = System.currentTimeMillis();
        System.out.println("qwen API 调用耗时: " + (endTime - startTime) + " ms");

        // 原子扣减
        userRemaining.compute(userId, (key, oldValue) ->
                oldValue == null || oldValue <= 0 ? 0 : oldValue - 1
        );

        Map<String, Object> response = new HashMap<>();
        response.put("reply", reply);
        response.put("remaining", remaining - 1);
        return ResponseEntity.ok(response);
    }
}