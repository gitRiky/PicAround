package com.project.pervsys.picaround.domain;


import com.google.firebase.database.IgnoreExtraProperties;

@IgnoreExtraProperties

public class User {
    private String username;
    private String email;
    private String name;
    private String surname;
    private String age;
    private String profile_picture;

    public User(){
        // Default constructor required for calls to DataSnapshot.getValue(User.class)
    }

    public User(String username, String email, String name, String surname, String age, String profile_picture) {
        this.username = username;
        this.email = email;
        this.name = name;
        this.surname = surname;
        this.age = age;
        this.profile_picture = profile_picture;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSurname() {
        return surname;
    }

    public void setSurname(String surname) {
        this.surname = surname;
    }

    public String getAge() {
        return age;
    }

    public void setAge(String age) {
        this.age = age;
    }

    public String getProfile_picture() {
        return profile_picture;
    }

    public void setProfile_picture(String profile_picture) {
        this.profile_picture = profile_picture;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        User user = (User) o;

        if (age != user.age) return false;
        if (!username.equals(user.username)) return false;
        if (!email.equals(user.email)) return false;
        if (name != null ? !name.equals(user.name) : user.name != null) return false;
        if (surname != null ? !surname.equals(user.surname) : user.surname != null) return false;
        return profile_picture != null ? profile_picture.equals(user.profile_picture) : user.profile_picture == null;

    }

    @Override
    public int hashCode() {
        int result = username.hashCode();
        result = 31 * result + email.hashCode();
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (surname != null ? surname.hashCode() : 0);
        result = 31 * result + age.hashCode();
        result = 31 * result + (profile_picture != null ? profile_picture.hashCode() : 0);
        return result;
    }

    @Override
    public String toString(){
        return name + ", " + surname + ", " + email + "\n" + username + ", " + age;
    }
}
