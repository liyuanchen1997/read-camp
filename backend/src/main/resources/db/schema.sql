-- ============================================================
-- 英语精读训练营 数据库结构（7 张表）
-- 幂等：CREATE TABLE IF NOT EXISTS，可重复执行
-- 设计依据：doc/00-design.md §1（修改表结构需先更新该文档）
-- 执行：mysql -uroot -p readcamp < schema.sql
-- ============================================================

-- 用户表
CREATE TABLE IF NOT EXISTS `user` (
  `id`                   BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  `username`             VARCHAR(50)  NOT NULL COMMENT '用户名（唯一）',
  `password`             VARCHAR(100) NOT NULL COMMENT 'BCrypt 哈希',
  `nickname`             VARCHAR(50)  NOT NULL DEFAULT '' COMMENT '昵称',
  `avatar_url`           VARCHAR(255) NOT NULL DEFAULT '' COMMENT '头像 URL',
  `role`                 TINYINT      NOT NULL DEFAULT 0 COMMENT '0 普通用户 / 1 管理员',
  `must_change_password` TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '1=首登强制改密',
  `status`               TINYINT      NOT NULL DEFAULT 1 COMMENT '1 正常 / 0 禁用',
  `created_at`           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户';

-- 文章表
CREATE TABLE IF NOT EXISTS `article` (
  `id`             BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  `title`          VARCHAR(200) NOT NULL COMMENT '标题',
  `summary`        VARCHAR(500) NOT NULL DEFAULT '' COMMENT '简介',
  `cover_url`      VARCHAR(255) NOT NULL DEFAULT '' COMMENT '封面 URL（空则前端生成渐变）',
  `content_en`     MEDIUMTEXT   NOT NULL COMMENT '英文全文原文（管理端回显/重切分用）',
  `tags`           JSON         DEFAULT NULL COMMENT '标签数组，如 ["科技","教育"]',
  `difficulty`     TINYINT      NOT NULL DEFAULT 2 COMMENT '1 入门 / 2 进阶 / 3 挑战',
  `status`         TINYINT      NOT NULL DEFAULT 0 COMMENT '0 下架(草稿) / 1 上架',
  `word_count`     INT          NOT NULL DEFAULT 0 COMMENT '英文词数（上传时统计）',
  `sentence_count` INT          NOT NULL DEFAULT 0 COMMENT '切分后的句子数',
  `created_by`     BIGINT UNSIGNED NOT NULL COMMENT '上传的管理员 id',
  `created_at`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY `idx_status_created` (`status`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文章';

-- 句子表（上传时服务端切分落库，阅读页零切分）
CREATE TABLE IF NOT EXISTS `sentence` (
  `id`         BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  `article_id` BIGINT UNSIGNED NOT NULL COMMENT '文章 id',
  `seq`        INT             NOT NULL COMMENT '句序，0 起',
  `para`       INT             NOT NULL DEFAULT 0 COMMENT '段落号，0 起（按原文空行分段，阅读页段落流式排版）',
  `content_en` TEXT            NOT NULL COMMENT '英文句子',
  `created_at` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY `uk_article_seq` (`article_id`, `seq`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='句子';

-- 句子精读标注（AI 生成，1:1 sentence）
CREATE TABLE IF NOT EXISTS `sentence_annotation` (
  `id`           BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  `sentence_id`  BIGINT UNSIGNED NOT NULL COMMENT '句子 id',
  `content_zh`   TEXT            DEFAULT NULL COMMENT '逐句中文翻译（右侧栏+全文拼接）',
  `explanation`  TEXT            DEFAULT NULL COMMENT '句子讲解（中文）',
  `components`   JSON            DEFAULT NULL COMMENT '句子成分 [{type,text,detail}]',
  `words`        JSON            DEFAULT NULL COMMENT '单词标注 [{word,pos,meaning,role}]',
  `gen_status`   TINYINT         NOT NULL DEFAULT 0 COMMENT '0 未生成 / 1 生成中 / 2 已生成 / 3 生成失败',
  `gen_error`    VARCHAR(500)    DEFAULT NULL COMMENT '失败原因',
  `created_at`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY `uk_sentence` (`sentence_id`),
  KEY `idx_gen_status` (`gen_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='句子精读标注（AI 生成）';

-- 阅读进度表（按句子计数）
CREATE TABLE IF NOT EXISTS `user_progress` (
  `id`             BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  `user_id`        BIGINT UNSIGNED NOT NULL COMMENT '用户 id',
  `article_id`     BIGINT UNSIGNED NOT NULL COMMENT '文章 id',
  `read_sentences` JSON            NOT NULL COMMENT '已读句索引数组 [0,1,5,...]',
  `read_count`     INT             NOT NULL DEFAULT 0 COMMENT '已读句数（冗余）',
  `total_count`    INT             NOT NULL DEFAULT 0 COMMENT '句子总数快照（防重切分漂移）',
  `progress`       TINYINT         NOT NULL DEFAULT 0 COMMENT '0-100 进度（冗余，近期阅读免解析 JSON）',
  `is_completed`   TINYINT(1)      NOT NULL DEFAULT 0 COMMENT 'progress=100 置 1',
  `completed_at`   DATETIME        DEFAULT NULL COMMENT '精读完成时间',
  `last_read_at`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '最近阅读时间（近期阅读排序）',
  `created_at`     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY `uk_user_article` (`user_id`, `article_id`),
  KEY `idx_recent` (`user_id`, `last_read_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='阅读进度';

-- 生词本
CREATE TABLE IF NOT EXISTS `user_vocab` (
  `id`                BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  `user_id`           BIGINT UNSIGNED NOT NULL COMMENT '用户 id',
  `word`              VARCHAR(100)    NOT NULL COMMENT '单词（小写规范化）',
  `source_article_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '出处文章 id',
  `context_sentence`  TEXT            DEFAULT NULL COMMENT '出处原句（复习展示用）',
  `created_at`        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY `uk_user_word` (`user_id`, `word`),
  KEY `idx_user_created` (`user_id`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='生词本';

-- 例句收藏
CREATE TABLE IF NOT EXISTS `user_favorite_sentence` (
  `id`          BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  `user_id`     BIGINT UNSIGNED NOT NULL COMMENT '用户 id',
  `sentence_id` BIGINT UNSIGNED NOT NULL COMMENT '句子 id',
  `note`        VARCHAR(255)    DEFAULT NULL COMMENT '备注',
  `created_at`  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY `uk_user_sentence` (`user_id`, `sentence_id`),
  KEY `idx_user_created` (`user_id`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='例句收藏';

-- AI 模型配置（单行 id=1；管理后台可编辑，支持切换 OpenAI 兼容模型/本地模型）
CREATE TABLE IF NOT EXISTS `ai_config` (
  `id`               BIGINT UNSIGNED PRIMARY KEY COMMENT '固定 1（单行配置）',
  `base_url`         VARCHAR(255) NOT NULL COMMENT 'OpenAI 兼容接口地址',
  `api_key`          VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'API Key（本地模型可为空）',
  `model`            VARCHAR(100) NOT NULL COMMENT '模型名',
  `batch_size`       INT          NOT NULL DEFAULT 3 COMMENT '每批句子数',
  `temperature`      DECIMAL(2,1) NOT NULL DEFAULT 0.3 COMMENT '采样温度',
  `timeout_seconds`  INT          NOT NULL DEFAULT 120 COMMENT '请求超时（秒）',
  `updated_by`       BIGINT UNSIGNED DEFAULT NULL COMMENT '最后修改人',
  `updated_at`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI 模型配置';
