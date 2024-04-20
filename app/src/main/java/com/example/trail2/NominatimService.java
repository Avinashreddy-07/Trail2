package com.example.trail2;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface NominatimService {
    @GET("search")
    Call<NominatimResponse> searchNearbyHospitals(
            @Query("q") String query,
            @Query("format") String format,
            @Query("addressdetails") int addressDetails,
            @Query("limit") int limit,
            @Query("type") String type
    );
}
