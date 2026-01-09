package com.eran.packcollect.DataBase;

public class User {
    public String fullName;
    public String phoneNumber;
    public String homeAddress;

    public User() {}

    public User(String fullName, String phoneNumber, String homeAddress) {
        this.fullName = fullName;
        this.phoneNumber = phoneNumber;
        this.homeAddress = homeAddress;
    }
}
