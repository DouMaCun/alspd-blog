USE `alspd-ai`;

CREATE TABLE IF NOT EXISTS blog_category (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    name VARCHAR(64) NOT NULL COMMENT '分类名称',
    slug VARCHAR(80) NOT NULL COMMENT 'URL 标识',
    description VARCHAR(255) DEFAULT NULL COMMENT '分类描述',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '排序值',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_blog_category_slug (slug)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='博客分类';

CREATE TABLE IF NOT EXISTS blog_tag (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    name VARCHAR(64) NOT NULL COMMENT '标签名称',
    slug VARCHAR(80) NOT NULL COMMENT 'URL 标识',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_blog_tag_slug (slug)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='博客标签';

CREATE TABLE IF NOT EXISTS blog_post (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    title VARCHAR(180) NOT NULL COMMENT '文章标题',
    slug VARCHAR(200) NOT NULL COMMENT 'URL 标识',
    summary VARCHAR(500) NOT NULL COMMENT '文章摘要',
    content LONGTEXT NOT NULL COMMENT '文章正文',
    cover_image VARCHAR(500) DEFAULT NULL COMMENT '封面图地址',
    category_id BIGINT UNSIGNED DEFAULT NULL COMMENT '分类 ID',
    status VARCHAR(24) NOT NULL DEFAULT 'DRAFT' COMMENT '状态：DRAFT/PUBLISHED',
    featured TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否精选',
    view_count INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '浏览数',
    published_at DATETIME DEFAULT NULL COMMENT '发布时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_blog_post_slug (slug),
    KEY idx_blog_post_status_publish (status, published_at),
    KEY idx_blog_post_category (category_id),
    CONSTRAINT fk_blog_post_category FOREIGN KEY (category_id) REFERENCES blog_category (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='博客文章';

CREATE TABLE IF NOT EXISTS blog_post_tag (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    post_id BIGINT UNSIGNED NOT NULL COMMENT '文章 ID',
    tag_id BIGINT UNSIGNED NOT NULL COMMENT '标签 ID',
    PRIMARY KEY (id),
    UNIQUE KEY uk_blog_post_tag (post_id, tag_id),
    KEY idx_blog_post_tag_tag (tag_id),
    CONSTRAINT fk_blog_post_tag_post FOREIGN KEY (post_id) REFERENCES blog_post (id) ON DELETE CASCADE,
    CONSTRAINT fk_blog_post_tag_tag FOREIGN KEY (tag_id) REFERENCES blog_tag (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文章标签关联';

INSERT INTO blog_category (name, slug, description, sort_order)
VALUES
    ('微服务设计', 'microservices', '边界、通信、治理和演进经验', 10),
    ('识别算法', 'recognition-algorithms', '图像、信号和模型落地中的算法记录', 20),
    ('嵌入式', 'embedded', '硬件接口、实时约束和工程调试', 30),
    ('AI 使用', 'ai-tools', 'AI 工具链、提示词和效率实践', 40),
    ('开发感悟', 'dev-notes', '架构取舍、代码习惯和复盘', 50)
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    description = VALUES(description),
    sort_order = VALUES(sort_order);

INSERT INTO blog_tag (name, slug)
VALUES
    ('Java', 'java'),
    ('MyBatis-Plus', 'mybatis-plus'),
    ('Spring Boot', 'spring-boot'),
    ('Vue', 'vue'),
    ('边界设计', 'boundary-design'),
    ('嵌入式调试', 'embedded-debug'),
    ('AI 工作流', 'ai-workflow')
ON DUPLICATE KEY UPDATE
    name = VALUES(name);

INSERT INTO blog_post (
    title,
    slug,
    summary,
    content,
    category_id,
    status,
    featured,
    view_count,
    published_at
)
SELECT
    '从服务边界开始设计微服务',
    'design-microservices-from-boundaries',
    '微服务不是先拆项目，而是先识别业务边界、数据所有权和变化频率。',
    '微服务设计里最容易犯的错，是直接按现有代码包或数据库表拆分。\n\n我更倾向先看三个问题：谁拥有这份数据、哪个流程最常变化、失败时是否需要独立降级。\n\n当边界足够清楚，接口就不只是 CRUD，而会表达业务动作。后续治理、限流、异步事件和观测也会自然落在这些边界上。',
    c.id,
    'PUBLISHED',
    1,
    128,
    '2026-06-08 09:30:00'
FROM blog_category c
WHERE c.slug = 'microservices'
ON DUPLICATE KEY UPDATE
    title = VALUES(title),
    summary = VALUES(summary),
    content = VALUES(content),
    category_id = VALUES(category_id),
    status = VALUES(status),
    featured = VALUES(featured),
    published_at = VALUES(published_at);

INSERT INTO blog_post (
    title,
    slug,
    summary,
    content,
    category_id,
    status,
    featured,
    view_count,
    published_at
)
SELECT
    '把 AI 当成开发流程的一部分',
    'make-ai-part-of-dev-workflow',
    'AI 工具最稳定的价值不是替代工程判断，而是缩短探索、验证和表达的反馈链路。',
    '使用 AI 写代码时，我会把任务拆成三个阶段：先让它帮助梳理边界，再让它实现小步变更，最后用测试和人工审查收口。\n\n效果最好的提示词通常不是一句“帮我优化”，而是明确上下文、约束、验收方式和不希望改动的范围。\n\nAI 不是免检通道。它更像一个很快的协作者，适合生成候选方案，也需要工程师用事实校验。',
    c.id,
    'PUBLISHED',
    1,
    96,
    '2026-06-07 19:20:00'
FROM blog_category c
WHERE c.slug = 'ai-tools'
ON DUPLICATE KEY UPDATE
    title = VALUES(title),
    summary = VALUES(summary),
    content = VALUES(content),
    category_id = VALUES(category_id),
    status = VALUES(status),
    featured = VALUES(featured),
    published_at = VALUES(published_at);

INSERT INTO blog_post (
    title,
    slug,
    summary,
    content,
    category_id,
    status,
    featured,
    view_count,
    published_at
)
SELECT
    '嵌入式问题排查的一条主线',
    'embedded-debug-main-thread',
    '面对偶现问题，先把电源、时序、通信和状态机分开验证，能减少很多无效猜测。',
    '嵌入式调试很考验耐心。软件日志能看到的只是结果，很多根因藏在电源波动、时钟配置、总线竞争或外设初始化顺序里。\n\n我的基本顺序是先确认硬件前提，再缩小通信链路，最后看状态机是否存在不可恢复分支。\n\n不要急着改很多代码。每次只改变一个变量，才知道到底是什么让系统变好了。',
    c.id,
    'PUBLISHED',
    0,
    72,
    '2026-06-06 15:45:00'
FROM blog_category c
WHERE c.slug = 'embedded'
ON DUPLICATE KEY UPDATE
    title = VALUES(title),
    summary = VALUES(summary),
    content = VALUES(content),
    category_id = VALUES(category_id),
    status = VALUES(status),
    featured = VALUES(featured),
    published_at = VALUES(published_at);

INSERT INTO blog_post_tag (post_id, tag_id)
SELECT p.id, t.id
FROM blog_post p
JOIN blog_tag t ON t.slug IN ('java', 'spring-boot', 'boundary-design')
WHERE p.slug = 'design-microservices-from-boundaries'
ON DUPLICATE KEY UPDATE tag_id = VALUES(tag_id);

INSERT INTO blog_post_tag (post_id, tag_id)
SELECT p.id, t.id
FROM blog_post p
JOIN blog_tag t ON t.slug IN ('ai-workflow', 'java', 'vue')
WHERE p.slug = 'make-ai-part-of-dev-workflow'
ON DUPLICATE KEY UPDATE tag_id = VALUES(tag_id);

INSERT INTO blog_post_tag (post_id, tag_id)
SELECT p.id, t.id
FROM blog_post p
JOIN blog_tag t ON t.slug IN ('embedded-debug')
WHERE p.slug = 'embedded-debug-main-thread'
ON DUPLICATE KEY UPDATE tag_id = VALUES(tag_id);
