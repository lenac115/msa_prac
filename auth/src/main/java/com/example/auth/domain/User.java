package com.example.auth.domain;

import com.example.commonevents.auth.Auth;
import com.example.commonevents.auth.UserDto;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String email;

    private String password;

    private String name;

    private String address;

    private String phone;

    private LocalDate birthday;

    @Enumerated(EnumType.STRING)
    private Auth auth;

    public void updateUser(UserDto userDto) {

        this.name = userDto.getName();
        this.address = userDto.getAddress();
        this.phone = userDto.getPhone();
    }

    public void updatePassword(String password) {
        this.password = password;
    }
}
