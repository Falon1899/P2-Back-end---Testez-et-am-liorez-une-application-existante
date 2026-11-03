package com.openclassrooms.etudiant.controller;

import com.openclassrooms.etudiant.dto.LoginRequestDTO;
import com.openclassrooms.etudiant.dto.RegisterDTO;
import com.openclassrooms.etudiant.dto.UserResponseDTO;
import com.openclassrooms.etudiant.mapper.UserDtoMapper;
import com.openclassrooms.etudiant.service.UserService;
import com.openclassrooms.etudiant.dto.UpdateStudentDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;



@RestController
@RequestMapping
@RequiredArgsConstructor

public class UserController {

    private final UserService userService;
    private final UserDtoMapper userDtoMapper;

    @PostMapping("/api/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterDTO registerDTO) {
        userService.register(userDtoMapper.toEntity(registerDTO));
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @PostMapping("/api/login")

    // Il manquait @RequestBody pour que ça fonctionne correctement 

    public ResponseEntity<?> login(@RequestBody LoginRequestDTO loginRequestDTO) {
        String jwtToken = userService.login(loginRequestDTO.getLogin(), loginRequestDTO.getPassword());
        return ResponseEntity.ok(jwtToken);
    }

    @GetMapping("/api/users")
        public List<UserResponseDTO> getAllUsers() {
        return userService.getAllUsers();
    }

    @PostMapping("/api/students")
        public ResponseEntity<?> createStudent(@Valid @RequestBody RegisterDTO registerDTO) {
            userService.register(userDtoMapper.toEntity(registerDTO));
            return new ResponseEntity<>(HttpStatus.CREATED);
        }

    @GetMapping("/api/students/{id}")
        public UserResponseDTO getStudentById(@PathVariable Long id) {
            return userService.getUserById(id);
        }

    
    @PutMapping("/api/students/{id}")
        public UserResponseDTO updateStudent(
                @PathVariable Long id,
                @RequestBody UpdateStudentDTO dto) {

            return userService.updateStudent(id, dto);
        }

        @DeleteMapping("/api/students/{id}")
        public ResponseEntity<Void> deleteStudent(@PathVariable Long id) {

            userService.deleteStudent(id);

            return ResponseEntity.noContent().build();
        }


}
