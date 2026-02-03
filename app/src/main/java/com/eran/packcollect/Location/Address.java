package com.eran.packcollect.Location;

import android.content.Context;
import android.net.Uri;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class Address {
    public String address;
    public double lat = 0;
    public double lon = 0;
    public Address() {}
    public Address(String address, double lat, double lon) {
        this();

        this.address = address;
        this.lat = lat;
        this.lon = lon;
    }
    public static void searchAddress(Context context, SearchLocationCallback callback, String query) {
        // 1. Encode the query and prepare the URL
        String encodedQuery = Uri.encode(query);
        String url = "https://nominatim.openstreetmap.org/search?q=" + encodedQuery + "&format=json&addressdetails=1&limit=5";

        // 2. Create the request
        JsonArrayRequest request = new JsonArrayRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        if (response.length() == 0) {
                            callback.onNoResult(query);
                        } else {
                            JSONObject item = response.getJSONObject(0);
                            Address location = new Address(item.getString("display_name"),
                                    item.getDouble("lat"),
                                    item.getDouble("lon"));
                            callback.onSuccess(location);
                        }

                    } catch (JSONException e) {
                        callback.onError(e);
                    }
                },
                error -> callback.onError(error)
        ) {
            @Override
            public Map<String, String> getHeaders() {
                // REQUIRED: Identify your app to Nominatim
                Map<String, String> headers = new HashMap<>();
                headers.put("User-Agent", "PackCollect");
                return headers;
            }
        };

        // 3. Add to the queue
        Volley.newRequestQueue(context).add(request);
    }
}
