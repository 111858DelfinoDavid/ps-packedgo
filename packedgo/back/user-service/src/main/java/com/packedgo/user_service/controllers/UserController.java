package com.packedgo.user_service.controllers;

import com.packedgo.user_service.dtos.CreateUserDto;
import com.packedgo.user_service.dtos.UpdateUserDto;
import com.packedgo.user_service.dtos.UserDto;
import com.packedgo.user_service.models.User;
import com.packedgo.user_service.services.UserService;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/users")
public class UserController {

    @Autowired
    private UserService userService;
    @Autowired
    private ModelMapper modelMapper;

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserDto>> getUserById(@PathVariable Long id) {
        User user = userService.getUserById(id);
        UserDto userDto = modelMapper.map(user, UserDto.class);
        ApiResponse<UserDto> response = new ApiResponse<>(HttpStatus.OK, "Usuario encontrado exitosamente", userDto);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/by-document/{document}")
    public ResponseEntity<ApiResponse<UserDto>> getUserByDocument(@PathVariable Long document) {
        User user = userService.getUserByDocument(document);
        UserDto userDto = modelMapper.map(user, UserDto.class);
        ApiResponse<UserDto> response = new ApiResponse<>(HttpStatus.OK, "Usuario encontrado exitosamente", userDto);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/by-email/{email}")
    public ResponseEntity<ApiResponse<UserDto>> getUserByEmail(@PathVariable String email) {
        User user = userService.getUserByEmail(email);
        UserDto userDto = modelMapper.map(user, UserDto.class);
        ApiResponse<UserDto> response = new ApiResponse<>(HttpStatus.OK, "Usuario encontrado exitosamente", userDto);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<UserDto>>> getUserList() {
        List<UserDto> userDtoList = userService.getUserList()
                .stream()
                .sorted(Comparator.comparing(User::getName)
                        .thenComparing(User::getLastName))
                .map(user -> modelMapper.map(user, UserDto.class))
                .collect(Collectors.toList());
        ApiResponse<List<UserDto>> response = new ApiResponse<>(HttpStatus.OK, "Listado de usuarios obtenido", userDtoList);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<ApiResponse<UserDto>> createUser(@RequestBody CreateUserDto createUserDto) {
        User user = userService.createUser(modelMapper.map(createUserDto, User.class));
        UserDto userDto = modelMapper.map(user, UserDto.class);
        ApiResponse<UserDto> response = new ApiResponse<>(HttpStatus.CREATED, "Usuario creado exitosamente", userDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{document}")
    public ResponseEntity<ApiResponse<UserDto>> updateUser(@PathVariable Long document, @RequestBody UpdateUserDto updateUserDto) {
        User user = userService.updateUser(document, updateUserDto);
        UserDto userDto = modelMapper.map(user, UserDto.class);
        ApiResponse<UserDto> response = new ApiResponse<>(HttpStatus.OK, "Usuario actualizado exitosamente", userDto);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{document}")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable Long document) {
        userService.deleteUser(document);
        ApiResponse<Void> response = new ApiResponse<>(HttpStatus.NO_CONTENT, "Usuario eliminado exitosamente", null);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).body(response);
    }
}