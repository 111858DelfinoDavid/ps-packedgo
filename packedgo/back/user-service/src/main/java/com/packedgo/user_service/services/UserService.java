package com.packedgo.user_service.services;
import com.packedgo.user_service.dtos.UpdateUserDto;
import com.packedgo.user_service.models.User;

import java.util.List;


public interface UserService {
    User getUserById(Long id);

    User getUserByDocument(Long document);

    User getUserByEmail(String email);

    List<User> getUserList();

    User createUser(User user);

    User updateUser(Long document, UpdateUserDto updateUserDto);

    void deleteUser(Long document);
}
