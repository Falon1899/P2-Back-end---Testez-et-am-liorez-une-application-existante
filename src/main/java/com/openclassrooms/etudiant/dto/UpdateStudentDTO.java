package com.openclassrooms.etudiant.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateStudentDTO {
    private String firstName;
    private String lastName;
    private String login;
}