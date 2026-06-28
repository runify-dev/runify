package com.run.integrations.impl.wecomrobot;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2026/6/20  10:00}
 * {@code @Version 1.0}
 * {@code @注释: 智能机器人流式会话: 工作流异步往里追加增量, 企业微信刷新回调读取当前累积内容 }
 */
public class StreamSession {

    private volatile String content = "";
    private volatile boolean finished = false;
    final long createdAt = System.currentTimeMillis();

    /**
     * 用当前完整渲染快照替换(非追加)
     */
    public void set(String snapshot) {
        if (snapshot != null) {
            this.content = snapshot;
        }
    }

    public String content() {
        return content;
    }

    public void complete(String full) {
        if (content.isEmpty() && full != null) {
            this.content = full;
        }
        finished = true;
    }

    public void fail(String err) {
        if (content.isEmpty()) {
            this.content = "生成失败: " + err;
        }
        finished = true;
    }

    public boolean isFinished() {
        return finished;
    }
}
