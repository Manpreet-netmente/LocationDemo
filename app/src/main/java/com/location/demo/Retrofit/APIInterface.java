package com.location.demo.Retrofit;

import com.location.demo.LocationModel;

import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.POST;

/**
 * Created by Manpreet.kaur on 12-06-2019.
 */

public interface APIInterface {
    @FormUrlEncoded
    @POST("index.php")
    Call<LocationModel> sendLocation(@Field("phone") String phone, @Field("lat") String lat, @Field("lng") String lng);
}
