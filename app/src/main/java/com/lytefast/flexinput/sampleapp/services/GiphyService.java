package com.lytefast.flexinput.sampleapp.services;

import com.lytefast.flexinput.sampleapp.model.GiphyResponse;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;


/**
 * Retrofit service for giphy.com
 * @author Sam Shih
 */
public interface GiphyService {
  @GET("/v1/gifs/search")
  Call<GiphyResponse> search(
      @Query("q") String searchTerm,
      @Query("limit") Integer limit,
      @Query("offset") Integer offset,
      @Query("rating") String rating);

}
