package com.hmdp.dto;

import lombok.Data;

//DTO  data transfer object
@Data
public class UserDTO {
    private Long id;
    private String nickName;
    private String icon;
}
