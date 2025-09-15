package com.packedgo.user_service.models;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
public class User {

    private Long id;

    @NotNull(message = "Role can't be null")
    private ROLE role;

    @NotNull(message = "Name can't be null")
    private String name;

    @NotNull(message = "Last name can't be null")
    private String lastName;

    @NotNull(message = "Gender can't be null")
    private GENDER gender;

    @NotNull(message = "Document can't be null")
    private Long document;

    @NotNull(message = "Born date can't be null")
    private LocalDate bornDate;

    @NotNull(message = "Email can't be null")
    private String email;

    @NotNull(message = "Telephone can't be null")
    private Long telephone;
}
