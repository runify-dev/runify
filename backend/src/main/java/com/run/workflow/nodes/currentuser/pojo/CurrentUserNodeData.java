package com.run.workflow.nodes.currentuser.pojo;

import lombok.Data;

@Data
public class CurrentUserNodeData {
    /**
     * 会话缓存:凭证 -> 用户对象
     */
    private String sessionCacheId;
    /**
     * 凭证位置 header / cookie / query
     */
    private String credentialLocation;
    /**
     * 凭证字段名,如 Authorization / token / sessionId
     */
    private String credentialField;
    /**
     * 凭证前缀,取值后剥离,如 "Bearer "
     */
    private String credentialPrefix;
    /**
     * 会话缓存 key 前缀,需与登录侧写入规则一致
     */
    private String keyPrefix;
    /**
     * 用户对象中用户标识的字段名,供角色/权限按 userId 查缓存,默认 id
     */
    private String userIdField;
    /**
     * 角色段,默认关闭
     */
    private RefSegment roles;
    /**
     * 权限段,默认关闭
     */
    private RefSegment permissions;

    /**
     * 可选段:关闭则不输出;开启后可从用户对象内抽取(inline),或按 userId 查独立缓存(cache)
     */
    @Data
    public static class RefSegment {
        private Boolean enabled;
        /**
         * inline / cache
         */
        private String source;
        /**
         * inline:从用户对象里取哪个字段
         */
        private String field;
        /**
         * cache:缓存连接 id
         */
        private String cacheId;
        /**
         * cache:key 前缀
         */
        private String keyPrefix;
        /**
         * cache:取出对象后再抽取的子字段,为空表示整个值
         */
        private String valueField;
    }
}
