package com.marcura.client;

import com.marcura.model.flixer.LatestExchangeRate;
import feign.Headers;
import feign.Param;
import feign.RequestLine;

public interface FixerClient {

    @Headers("apikey: {accessKey}")
    @RequestLine("GET /latest?access_key={accessKey}&base={base}")
    LatestExchangeRate getLatestExchangeRate(@Param("accessKey") String accessKey,
                                             @Param("base") String base);
}
