-- ============================================================
-- 英语精读训练营 种子数据
-- 幂等：已存在则跳过（ON DUPLICATE KEY UPDATE 自赋值 no-op），
--       重复执行不会覆盖用户/管理员修改过的数据
-- 执行：mysql -uroot -p readcamp < seed.sql（先执行 schema.sql）
-- ============================================================

-- ---------- 预置管理员 ----------
-- 默认密码：admin123（BCrypt），首次登录强制改密（must_change_password=1）
INSERT INTO `user` (`username`, `password`, `nickname`, `role`, `must_change_password`)
VALUES (
  'admin',
  '$2y$10$jhMGO4dicIIX5yZ/FMVEDetgPjKUZfsge9Ak8mJXZLE3KjTUywc4i',
  '管理员',
  1,
  1
)
ON DUPLICATE KEY UPDATE `username` = `username`;

-- ---------- 示例文章：The Lion and the Mouse（入门级寓言） ----------
-- 固定 id=10001 保证幂等；content_en 与下方 sentence 行内容一致（13 句 / 126 词）
INSERT INTO `article`
  (`id`, `title`, `summary`, `content_en`, `tags`, `difficulty`, `status`, `word_count`, `sentence_count`, `created_by`)
VALUES (
  10001,
  'The Lion and the Mouse',
  'A classic fable about kindness and unexpected friendship. 经典寓言：善意与意料之外的友谊。',
  'Once upon a time, a lion was sleeping under a big tree. A little mouse ran across the lion''s nose and woke him up. The lion caught the mouse and opened his big mouth to eat him. "Please let me go," cried the little mouse. "If you let me go, I will help you one day." The lion laughed and let the mouse go. A few days later, the lion was caught in a hunter''s net. He roared and struggled, but he could not escape. The little mouse heard the lion''s roar and ran to help. She gnawed through the ropes with her sharp teeth. Soon the lion was free. "Thank you, little mouse," said the lion. "Even the smallest friend can be a great helper."',
  JSON_ARRAY('寓言', '入门'),
  1,
  1,
  126,
  13,
  (SELECT `id` FROM `user` WHERE `username` = 'admin')
)
ON DUPLICATE KEY UPDATE `id` = `id`;

-- 句子（seq/para 0 起，与 SentenceSplitter 规则一致：空行分段 + . ! ? 切分；示例为单段落）
INSERT INTO `sentence` (`article_id`, `seq`, `para`, `content_en`) VALUES
(10001, 0,  0, 'Once upon a time, a lion was sleeping under a big tree.'),
(10001, 1,  0, 'A little mouse ran across the lion''s nose and woke him up.'),
(10001, 2,  0, 'The lion caught the mouse and opened his big mouth to eat him.'),
(10001, 3,  0, '"Please let me go," cried the little mouse.'),
(10001, 4,  0, '"If you let me go, I will help you one day."'),
(10001, 5,  0, 'The lion laughed and let the mouse go.'),
(10001, 6,  0, 'A few days later, the lion was caught in a hunter''s net.'),
(10001, 7,  0, 'He roared and struggled, but he could not escape.'),
(10001, 8,  0, 'The little mouse heard the lion''s roar and ran to help.'),
(10001, 9,  0, 'She gnawed through the ropes with her sharp teeth.'),
(10001, 10, 0, 'Soon the lion was free.'),
(10001, 11, 0, '"Thank you, little mouse," said the lion.'),
(10001, 12, 0, '"Even the smallest friend can be a great helper."')
ON DUPLICATE KEY UPDATE `seq` = `seq`;
