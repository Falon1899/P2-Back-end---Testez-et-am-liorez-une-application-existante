
package com.openclassrooms.etudiant.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * Définit les données modifiables d'un étudiant.
 * Exclut le mot de passe afin de ne pas permettre
 * sa modification via cet endpoint.
 */
@Getter
@Setter
public class UpdateStudentDTO {

    private String firstName;
    private String lastName;
    private String login;
}
