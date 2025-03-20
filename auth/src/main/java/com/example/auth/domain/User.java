package com.example.auth.domain;

import com.example.auth.dto.UserDto;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String email;

    private String password;

    private String name;

    private String address;

    private String phone;

    @Enumerated(EnumType.STRING)
    private User.Auth auth;

    public void updateUser(UserDto userDto) {

        this.name = userDto.getName();
        this.address = userDto.getAddress();
        this.phone = userDto.getPhone();
    }

    public void updatePassword(String password) {
        this.password = password;
    }

    public enum Auth {
        SELLER,
        BUYER
    }
}
