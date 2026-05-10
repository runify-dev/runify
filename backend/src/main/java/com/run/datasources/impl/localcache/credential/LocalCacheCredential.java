package com.run.datasources.impl.localcache.credential;

import com.run.datasources.BaseDatasourceCredential;

import java.util.Map;

public class LocalCacheCredential extends BaseDatasourceCredential {

    @Override
    public void validateCredential(Map<String, Object> credential) {
        // 本地缓存无需验证
    }

    @Override
    public Map<String, Object> encryption(Map<String, Object> credential) {
        return credential;
    }
}
