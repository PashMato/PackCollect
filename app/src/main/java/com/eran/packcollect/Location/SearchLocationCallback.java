package com.eran.packcollect.Location;

public interface SearchLocationCallback {
    void onSuccess(Address location);
    void onNoResult(String query);
    void onError(Exception e);
}
