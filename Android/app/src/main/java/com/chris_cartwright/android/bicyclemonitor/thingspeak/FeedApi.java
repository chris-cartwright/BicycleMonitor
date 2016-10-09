package com.chris_cartwright.android.bicyclemonitor.thingspeak;

import retrofit2.Call;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface FeedApi {
    @POST("update.json")
    Call<ChannelEntry> update(@Query("api_key") String api_key, @Query("field1") double speed, @Query("field2") int cadence, @Query("field3") String uuid, @Query("field4") long created);
    void load();
    void loadLast();
}
