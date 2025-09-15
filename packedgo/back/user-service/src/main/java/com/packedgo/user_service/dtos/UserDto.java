package com.packedgo.user_service.dtos;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.packedgo.user_service.models.GENDER;
import com.packedgo.user_service.models.ROLE;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class UserDto {
    private Long id;
    private ROLE role;

    private String name;

    private String lastName;

    private GENDER gender;

    private Long document;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy")
    private LocalDate bornDate;

    private String email;

    private Long telephone;
}
