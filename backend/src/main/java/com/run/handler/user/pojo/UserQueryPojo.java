package com.run.handler.user.pojo;

import io.vertx.core.MultiMap;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/9/12  23:49}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
@Getter
@Setter
public class UserQueryPojo {
    /**
     * 混合条件
     * username or nickname or email or phone
     */
    private String mixing;
    private String username;
    private String nickname;

    public UserQueryPojo(MultiMap multiMap) {
        this.mixing = multiMap.get("mixing");
        this.username = multiMap.get("username");
        this.nickname = multiMap.get("nickname");
    }

    public Map<String, Object> toMap() {
        Map<String, Object> result = new HashMap<>();
        if (StringUtils.isNotEmpty(mixing)) {
            result.put("mixing", "%" + mixing + "%");
        }
        if (StringUtils.isNotEmpty(username)) {
            result.put("username", "%" + username + "%");
        }
        if (StringUtils.isNotEmpty(nickname)) {
            result.put("nick_name", "%" + nickname + "%");
        }

        return result;
    }
}
