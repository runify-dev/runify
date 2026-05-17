package com.run.handler.conversation.dto;

import com.run.auth.constants.TokenTypeConstants;
import com.run.handler.user.dto.UserDTO;
import lombok.Data;

@Data
public class UserProfileDTO {
    private String id;
    private TokenTypeConstants type;
    private UserDTO user;

}
