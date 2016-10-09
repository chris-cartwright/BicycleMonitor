package com.chris_cartwright.android.bicyclemonitor.thingspeak;

import com.chris_cartwright.android.bicyclemonitor.HistoryEntry;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Converter;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ThingSpeak {
    private static final String API_KEY = "091SL5RZEOOQSSPL";

    private Retrofit retrofit;
    private FeedApi service;

    //public Entry loadLast() {

    //}

    public ChannelEntry send(HistoryEntry entry) throws IOException {
        Call<ChannelEntry> call = service.update(API_KEY, entry.getSpeed(), entry.getCadence(), entry.getUuid().toString(), entry.getCreated().getTime() / 1000);
        Response<ChannelEntry> resp = call.execute();
        if(resp.isSuccessful()) {
            return resp.body();
        }

        throw new IOException(resp.code() + "");
    }

    public ThingSpeak() {
        HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
        interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        OkHttpClient client = new OkHttpClient.Builder().addInterceptor(interceptor).build();

        retrofit = new Retrofit.Builder()
                .baseUrl("https://api.thingspeak.com/")
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        service = retrofit.create(FeedApi.class);
    }
}
