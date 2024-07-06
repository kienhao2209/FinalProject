package com.example.Buoi2.services;


import com.example.Buoi2.entity.Role;
import com.example.Buoi2.entity.User;
import com.example.Buoi2.repository.IRoleRepository;
import com.example.Buoi2.repository.UserRepository;
import com.example.Buoi2.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@Service
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    @Autowired
    private UserService userService;

    @Autowired
    private IRoleRepository roleRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2UserService<OAuth2UserRequest, OAuth2User> delegate = new DefaultOAuth2UserService();
        OAuth2User oauth2User = delegate.loadUser(userRequest);

        String email = oauth2User.getAttribute("email");
        String name = oauth2User.getAttribute("name");

        // Custom logic to handle user registration or update
        Optional<User> userOptional = userService.findByEmail(email);
        User user = userOptional.orElseGet(() -> {
            // User not found, create a new one
            User newUser = new User();
            newUser.setEmail(email);
            newUser.setUsername(name);

            // Determine if the user should have admin role based on your logic
            if (checkIfUserIsAdmin(oauth2User)) {
                Role adminRole = roleRepository.findByName("ADMIN")
                        .orElseGet(() -> {
                            Role newRole = new Role();
                            newRole.setName("ADMIN");
                            return roleRepository.save(newRole);
                        });
                newUser.setRoles(Collections.singleton(adminRole));
            } else {
                Role defaultRole = roleRepository.findByName("USER")
                        .orElseGet(() -> {
                            Role newRole = new Role();
                            newRole.setName("USER");
                            return roleRepository.save(newRole);
                        });
                newUser.setRoles(Collections.singleton(defaultRole));
            }

            userService.save(newUser); // Save new user
            return newUser;
        });

        // Set authorities (roles)
        Set<SimpleGrantedAuthority> authorities = new HashSet<>();
        user.getRoles().forEach(role -> {
            authorities.add(new SimpleGrantedAuthority(role.getName()));
        });

        // Return OAuth2User with authorities
        return new DefaultOAuth2User(authorities, oauth2User.getAttributes(), "email");
    }

    // Example method to check if user should have admin role
    private boolean checkIfUserIsAdmin(OAuth2User oauth2User) {
        // Implement your logic here to check if user should be admin
        // For example, check if user belongs to certain group or has specific attribute
        return false; // Modify this condition based on your requirements
    }
}
