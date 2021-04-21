package net.mikitaro.nicehash.domain.external;

import com.slack.api.webhook.Payload;
import com.slack.api.webhook.WebhookResponse;

public class Slack {

    private final String webhook;

    public Slack(String webhook) {
        this.webhook = webhook;
    }

    public WebhookResponse post(String message) {
        WebhookResponse response = null;
        com.slack.api.Slack slack = com.slack.api.Slack.getInstance();
        Payload payload = Payload.builder().text(message).build();

        try {
            response = slack.send(this.webhook, payload);

            if (!response.getCode().equals(200)){
                throw new Exception(response.getCode().toString());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return response;
    }
}
