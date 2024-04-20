package com.example.trail2;
import com.google.gson.annotations.SerializedName;

public class NominatimResponse {
    @SerializedName("error")
    private String error;

    @SerializedName("hospital")
    private String hospitalName;

    // Add other fields as needed

    public String getError() {
        return error;
    }

    public String getHospitalName() {
        return hospitalName;
    }

    // Add getters for other fields
}
