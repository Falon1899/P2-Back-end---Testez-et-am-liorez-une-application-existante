package com.openclassrooms.etudiant.service;

import com.openclassrooms.etudiant.dto.UserResponseDTO;
import com.openclassrooms.etudiant.entities.User;
import com.openclassrooms.etudiant.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import com.openclassrooms.etudiant.dto.UpdateStudentDTO;

import java.util.Optional;
import java.util.ArrayList;
import java.util.List;


@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public void register(User user) {
        Assert.notNull(user, "User must not be null");
        log.info("Registering new user");

        Optional<User> optionalUser = userRepository.findByLogin(user.getLogin());
        if (optionalUser.isPresent()) {
            throw new IllegalArgumentException("User with login " + user.getLogin() + " already exists");
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        userRepository.save(user);
    }

    public String login(String login, String password) {
        Assert.notNull(login, "Login must not be null");
        Assert.notNull(password, "Password must not be null");
        Optional<User> user = userRepository.findByLogin(login);
        if (user.isPresent() && passwordEncoder.matches(password, user.get().getPassword())) {
            UserDetails userDetails = org.springframework.security.core.userdetails.User.builder()
                    .username(login).password(user.get().getPassword()).build();
                    
            return jwtService.generateToken(userDetails);
        } else {
            throw new BadCredentialsException("Invalid credentials");
        }
    }
        /**
         * Récupère l'ensemble des utilisateurs enregistrés
         * dans la base de données via UserRepository. 
        * @return la liste des utilisateurs sous forme de DTO
        */
    public List<UserResponseDTO> getAllUsers() {

        List<User> users = userRepository.findAll();

        List<UserResponseDTO> dtoList = new ArrayList<>();

        for (User user : users) {
            UserResponseDTO dto = new UserResponseDTO(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getLogin()
            );

            dtoList.add(dto);
        }

        return dtoList;
    }

    
    public UserResponseDTO getUserById(Long id) {

        User user = userRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Utilisateur introuvable"
            ));

        return new UserResponseDTO(
            user.getId(),
            user.getFirstName(),
            user.getLastName(),
            user.getLogin()
        );
    }

    
public UserResponseDTO updateStudent(Long id, UpdateStudentDTO dto) {

    User user = userRepository.findById(id)
        .orElseThrow(() -> new ResponseStatusException(
            HttpStatus.NOT_FOUND,
            "Utilisateur introuvable"
        ));

    if (!user.getLogin().equals(dto.getLogin())
            && userRepository.findByLogin(dto.getLogin()).isPresent()) {
        throw new ResponseStatusException(
            HttpStatus.CONFLICT,
            "Ce login est déjà utilisé"
        );
    }

    user.setFirstName(dto.getFirstName());
    user.setLastName(dto.getLastName());
    user.setLogin(dto.getLogin());

    User updatedUser = userRepository.save(user);

    return new UserResponseDTO(
        updatedUser.getId(),
        updatedUser.getFirstName(),
        updatedUser.getLastName(),
        updatedUser.getLogin()
    );
}


    public void deleteStudent(Long id) {

        User user = userRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Utilisateur introuvable"
            ));

        userRepository.delete(user);
    }



}
