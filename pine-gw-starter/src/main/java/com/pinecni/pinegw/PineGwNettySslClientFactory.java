package com.pinecni.pinegw;

import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import org.springframework.http.client.ReactorClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import reactor.netty.http.client.HttpClient;

class PineGwNettySslClientFactory {

    static RestClient create() throws Exception {
        io.netty.handler.ssl.SslContext nettySsl = SslContextBuilder.forClient()
            .trustManager(InsecureTrustManagerFactory.INSTANCE)
            .build();
        HttpClient httpClient = HttpClient.create()
            .secure(spec -> spec.sslContext(nettySsl));
        return RestClient.builder()
            .requestFactory(new ReactorClientHttpRequestFactory(httpClient))
            .build();
    }
}
