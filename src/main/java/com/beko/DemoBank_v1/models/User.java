package com.beko.DemoBank_v1.models;
import javax.persistence.*;
import java.time.LocalDateTime;
import javax.validation.constraints.*;


@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer user_id;

    @NotEmpty(message = "The first name field cannot be empty.")
    @Size(min = 3, message = "The first name must be greater than 3 characters.")
    private String first_name;

    @NotEmpty(message = "The last name field cannot be empty.")
    @Size(min = 3, message = "The last name must be greater than 3 characters.")
    private String last_name;

    @Email(message = "Please enter a valid email.")
    @NotEmpty(message = "Email field cannot be empty.")
    private String email;

    @NotEmpty(message = "Password field cannot be empty.")
    private String password;

    private String token;
    private String code;

    private int verified;

    private LocalDateTime verified_at;

    @Column(updatable = false)
    private LocalDateTime create_at;

    private LocalDateTime updated_at;

    // Getters and Setters
    public Integer getUser_id() {
        return user_id;
    }

    public void setUser_id(Integer user_id) {
        this.user_id = user_id;
    }

    public String getFirst_name() {
        return first_name;
    }

    public void setFirst_name(String first_name) {
        this.first_name = first_name;
    }

    public String getLast_name() {
        return last_name;
    }

    public void setLast_name(String last_name) {
        this.last_name = last_name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public int getVerified() {
        return verified;
    }

    public void setVerified(int verified) {
        this.verified = verified;
    }

    public LocalDateTime getVerified_at() {
        return verified_at;
    }

    public void setVerified_at(LocalDateTime verified_at) {
        this.verified_at = verified_at;
    }

    public LocalDateTime getCreate_at() {
        return create_at;
    }

    public void setCreate_at(LocalDateTime create_at) {
        this.create_at = create_at;
    }

    public LocalDateTime getUpdated_at() {
        return updated_at;
    }

    public void setUpdated_at(LocalDateTime updated_at) {
        this.updated_at = updated_at;
    }
}
