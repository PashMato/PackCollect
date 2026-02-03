package com.eran.packcollect.Location;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

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
        String url = "https://nominatim.openstreetmap.org/search?q=" + encodedQuery + "&format=json&addressdetails=10&limit=5";

        // 2. Create the request
        JsonArrayRequest request = new JsonArrayRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        if (response.length() == 0) {
                            callback.onNoResult(query);
                        } else {
                            JSONObject item = response.getJSONObject(0);
                            Address location = new Address(getShortenName(item),
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

    private static String getShortenName(JSONObject item) {
        StringBuilder shortName = new StringBuilder();

        try {
            JSONObject addr = item.getJSONObject("address");

            // house_number + road
            if (addr.has("house_number")) shortName.append(addr.getString("house_number")).append(" ");
            if (addr.has("road")) shortName.append(addr.getString("road"));

            // road
            String road = addr.has("road") ? addr.getString("road") : "";
            if (!shortName.isEmpty()) shortName.append(", ");
            shortName.append(road);

            // locality
            String locality = addr.has("locality") ? addr.getString("locality") : "";
            if (!shortName.isEmpty()) shortName.append(", ");
            shortName.append(locality);

            // city or town or village
            String city = addr.has("village") ? addr.getString("village") :
                    addr.has("town") ? addr.getString("town") :
                            addr.has("city") ? addr.getString("city") : "";
            if (!shortName.isEmpty()) shortName.append(", ");
            shortName.append(city);

            // country
            if (addr.has("country")) shortName.append(", ").append(addr.getString("country"));

        } catch (JSONException e) {
            Log.e("LocationMissingAddr", e.getMessage());
        }

        return shortName.toString();
    }
}
