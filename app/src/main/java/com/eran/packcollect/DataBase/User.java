package com.eran.packcollect.DataBase;

import com.eran.packcollect.Location.Address;

public class User {
    public String fullName;
    public String phoneNumber;
    public Address homeAddress;

    public User() {}

    public User(String fullName, String phoneNumber, Address homeAddress) {
        this.fullName = fullName;
        this.phoneNumber = phoneNumber;
        this.homeAddress = homeAddress;
    }
}
