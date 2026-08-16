package com.readcamp.service;

import com.readcamp.dto.AiConfigDto;
import com.readcamp.entity.AiConfig;

public interface AiConfigService {

    /** 当前配置（内存缓存；表空时用 application.yml 默认值初始化落库） */
    AiConfig get();

    /** 更新配置（更新后刷新缓存，下次生成即生效） */
    AiConfig update(Long operatorId, AiConfigDto dto);
}
