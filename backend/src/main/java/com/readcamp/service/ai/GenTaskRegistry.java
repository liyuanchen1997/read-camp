package com.readcamp.service.ai;

import lombok.Getter;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 内存生成任务注册表（doc/00-design.md §3）
 * 仅用于：任务互斥（同文章同时仅一个任务）、取消标记、ETA 展示；
 * 持久状态以 DB（gen_status）为唯一事实源，服务重启后由 DB 恢复。
 */
@Component
public class GenTaskRegistry {

    @Getter
    public static class GenTask {
        private final Long articleId;
        private final LocalDateTime startedAt;
        private volatile boolean cancelled;

        GenTask(Long articleId) {
            this.articleId = articleId;
            this.startedAt = LocalDateTime.now();
        }

        public boolean isCancelled() {
            return cancelled;
        }
    }

    private final ConcurrentHashMap<Long, GenTask> tasks = new ConcurrentHashMap<>();

    /** 注册任务；已存在返回 null（互斥） */
    public GenTask register(Long articleId) {
        GenTask task = new GenTask(articleId);
        return tasks.putIfAbsent(articleId, task) == null ? task : null;
    }

    /** 任务完成/失败时移除 */
    public void remove(Long articleId) {
        tasks.remove(articleId);
    }

    /** 文章是否有进行中任务 */
    public boolean isRunning(Long articleId) {
        return tasks.containsKey(articleId);
    }

    /** 取消（批间生效） */
    public boolean cancel(Long articleId) {
        GenTask task = tasks.get(articleId);
        if (task == null) {
            return false;
        }
        task.cancelled = true;
        return true;
    }

    public GenTask get(Long articleId) {
        return tasks.get(articleId);
    }
}
