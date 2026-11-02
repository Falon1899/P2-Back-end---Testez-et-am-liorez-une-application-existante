package com.openclassrooms.etudiant.dto;

import lombok.Getter;

@Getter
public class UserResponseDTO {

    private Long id;
    private String firstName;
    private String lastName;
    private String login;

    public UserResponseDTO(
        Long id,
        String firstName,
        String lastName,
        String login
    ) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.login = login;
    }
}