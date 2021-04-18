package net.mikitaro.nicehash.domain.external;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

public class Discord {

    private String webhook;

    public Discord(String webhook) {
        this.webhook = webhook;
    }

    public String post(String payload) {
        StringBuffer result = new StringBuffer();
        HttpClient client = HttpClientBuilder.create().build();
        HttpPost request = new HttpPost(this.webhook);

        StringEntity entity = null;
        if (payload != null) {
            try {
                entity = new StringEntity(payload);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }

        request.setEntity(entity);
        request.setHeader("Accept", "application/json");
        request.setHeader("Content-type", "application/json");

        try {
            client.execute(request);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result.toString();
    }
}
