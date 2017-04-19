package com.project.pervsys.picaround.domain;


import com.google.firebase.database.IgnoreExtraProperties;

@IgnoreExtraProperties

public class User {
    private String username;
    private String email;
    private String name;
    private String surname;
    private String date;
    private String profilePicture;
    private String id;

    public User(){
        // Default constructor required for calls to DataSnapshot.getValue(User.class)
    }


    public User(String username, String email, String name, String surname, String date, String profilePicture, String id) {
        this.username = username;
        this.email = email;
        this.name = name;
        this.surname = surname;
        this.date = date;
        this.profilePicture = profilePicture;
        this.id = id;
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

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getProfilePicture() {
        return profilePicture;
    }

    public void setProfilePicture(String profilePicture) {
        this.profilePicture = profilePicture;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        User user = (User) o;

        if (date != user.date) return false;
        if (!username.equals(user.username)) return false;
        if (!email.equals(user.email)) return false;
        if (name != null ? !name.equals(user.name) : user.name != null) return false;
        if (surname != null ? !surname.equals(user.surname) : user.surname != null) return false;
        return profilePicture != null ? profilePicture.equals(user.profilePicture) : user.profilePicture == null;

    }

    @Override
    public int hashCode() {
        int result = username.hashCode();
        result = 31 * result + email.hashCode();
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (surname != null ? surname.hashCode() : 0);
        result = 31 * result + date.hashCode();
        result = 31 * result + (profilePicture != null ? profilePicture.hashCode() : 0);
        return result;
    }

    @Override
    public String toString(){
        return name + ", " + surname + ", " + email + "\n" + username + ", " + date;
    }
}
