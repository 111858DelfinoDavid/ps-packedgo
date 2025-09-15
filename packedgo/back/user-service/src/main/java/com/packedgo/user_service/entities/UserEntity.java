package com.packedgo.user_service.entities;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.packedgo.user_service.models.GENDER;
import com.packedgo.user_service.models.ROLE;
import jakarta.persistence.*;

import java.time.LocalDate;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "users")
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private ROLE role;


    private String name;


    private String lastName;


    @Enumerated(EnumType.STRING)
    private GENDER gender;

    @Column(unique = true, nullable = false)
    private Long document;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy")
    private LocalDate bornDate;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(unique = true, nullable = false)
    private Long telephone;
}
