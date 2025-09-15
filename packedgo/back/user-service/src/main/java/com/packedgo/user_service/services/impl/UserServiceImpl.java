package com.packedgo.user_service.services.impl;

import com.packedgo.user_service.dtos.UpdateUserDto;
import com.packedgo.user_service.entities.UserEntity;
import com.packedgo.user_service.models.User;
import com.packedgo.user_service.repositories.UserRepository;
import com.packedgo.user_service.services.UserService;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ModelMapper modelMapper;

    @Override
    public User getUserById(Long id) {
        UserEntity entity = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con id " + id));
        return modelMapper.map(entity, User.class);
    }


    @Override
    public User getUserByDocument(Long document) {
        UserEntity entity = userRepository.findByDocument(document)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con documento " + document));
        return modelMapper.map(entity, User.class);
    }

    @Override
    public User getUserByEmail(String email) {
        UserEntity entity = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con email " + email));
        return modelMapper.map(entity, User.class);
    }

    @Override
    public List<User> getUserList() {
        return userRepository.findAll()
                .stream()
                .map(entity -> modelMapper.map(entity, User.class))
                .sorted(Comparator.comparing(User::getId))
                .collect(Collectors.toList());
    }


    @Override
    public User createUser(User user) {
        Optional<UserEntity> userExist = userRepository.findByDocument(user.getDocument());
        if (userExist.isEmpty()) {
            UserEntity userEntity = modelMapper.map(user, UserEntity.class);
            UserEntity userSaved = userRepository.save(userEntity);
            return modelMapper.map(userSaved, User.class);
        } else {
            return null;
        }
    }

    @Override
    public User updateUser(Long document, UpdateUserDto updateUserDto) {
        // Buscar el usuario existente
        UserEntity userEntity = userRepository.findByDocument(document)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con documento " + document));

        // Actualizar solo los campos que vienen en el DTO
        if (updateUserDto.getEmail() != null) {
            userEntity.setEmail(updateUserDto.getEmail());
        }
        if (updateUserDto.getTelephone() != null) {
            userEntity.setTelephone(updateUserDto.getTelephone());
        }

        // Guardar cambios
        UserEntity updatedEntity = userRepository.save(userEntity);

        // Mapear a DTO de salida
        return modelMapper.map(updatedEntity, User.class);
    }


    @Override
    public void deleteUser(Long document) {
        UserEntity userEntity = userRepository.findByDocument(document)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con documento " + document));
        userRepository.delete(userEntity);
    }
}

