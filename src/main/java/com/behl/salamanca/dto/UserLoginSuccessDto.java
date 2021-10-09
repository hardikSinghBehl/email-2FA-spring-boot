package com.behl.salamanca.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

@Getter
@Builder
@Jacksonized
public class UserLoginSuccessDto {

    private final String accessToken;

}
