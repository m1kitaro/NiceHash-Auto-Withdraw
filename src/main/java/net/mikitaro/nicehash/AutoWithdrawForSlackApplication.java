package net.mikitaro.nicehash;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.mikitaro.nicehash.domain.external.Api;
import net.mikitaro.nicehash.domain.external.Slack;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

public class AutoWithdrawForSlackApplication implements RequestHandler<Map<String, Object>, Map<String, Object>> {

    static final String URL_ROOT = "https://api2.nicehash.com/"; // NiceHash 本番APIのURLです。
    static final String ORG_ID = System.getenv("ORG_ID"); // API Keys の Organization ID
    static final String API_KEY = System.getenv("API_KEY");// API Key
    static final String API_SECRET = System.getenv("API_SECRET");// API Secret
    static final BigDecimal WITHDRAW_THRESHOLD = new BigDecimal(System.getenv("WITHDRAW_THRESHOLD")); // 送金する閾値です。
    static final BigDecimal WITHDRAW_AMOUNT = new BigDecimal(System.getenv("WITHDRAW_AMOUNT")); // 送金する金額です。
    static final String WITHDRAW_ADDRESS_ID = System.getenv("WITHDRAW_ADDRESS_ID");// withdraw address の ID
    static final String WEBHOOK_URL = System.getenv("WEBHOOK_URL");// 結果送信用のSlack webhook URL

    /**
     * AWS Lambda のハンドラーメソッドです。
     *
     * @param input   入力データ
     * @param context AWS Lambda Context オブジェクト
     * @return 出力データ
     */
    @Override
    public Map<String, Object> handleRequest(Map<String, Object> input, Context context) {
        Map<String, Object> output = new HashMap<>();
        output.put("input", input); // 入力情報を見たいので出力に含める
        output.put("context", context); // コンテキスト情報を見たいので出力に含める

        Api api = new Api(URL_ROOT, ORG_ID, API_KEY, API_SECRET);
        Slack slack = new Slack(WEBHOOK_URL);

        String timeResponse = api.get("api/v2/time");
        JsonObject timeObject = new Gson().fromJson(timeResponse, JsonObject.class);
        String time = timeObject.get("serverTime").getAsString();
        System.out.println("server time: " + time);

        String activityResponse = api.get("main/api/v2/accounting/account2/BTC", true, time);
        JsonObject accountsArray = new Gson().fromJson(activityResponse, JsonObject.class);
        System.out.println("BTC balance: " + accountsArray.get("available").toString());
        String replacedBalanceStr = accountsArray.get("available").toString().replaceAll("\"", "");
        BigDecimal balance = new BigDecimal(replacedBalanceStr);

        if (0 <= balance.compareTo(WITHDRAW_THRESHOLD)) {
            // 送金対象
            System.out.println("送金対象");
        } else {
            System.out.println("送金しない");
            slack.post("no withdraw balance = " + balance.toString());
            return output;
        }
        String feeResponse = api.get("main/api/v2/public/service/fee/info", false, time);
        JsonObject feeObject = new Gson().fromJson(feeResponse, JsonObject.class);
        BigDecimal addFee = feeObject.get("withdrawal").getAsJsonObject().get("BITGO").getAsJsonObject().get("rules")
                .getAsJsonObject().get("BTC").getAsJsonObject().get("intervals").getAsJsonArray().get(0)
                .getAsJsonObject().get("element").getAsJsonObject().get("sndValue").getAsBigDecimal();

        System.out.println("add fee : " + addFee);
        if (addFee.compareTo(new BigDecimal("0")) == 0) {
            withdraw(api, time, balance);
            slack.post("@here success!! fee = " + addFee.toString());
        } else {
            slack.post("no withdraw fee = " + addFee.toString());
        }
        return output;
    }

    private void withdraw(Api api, String time, BigDecimal balance) {
        BigDecimal withdrawAmount = (WITHDRAW_AMOUNT.compareTo(new BigDecimal("0")) == 0) ? balance : WITHDRAW_AMOUNT;
        String payload = "{\"currency\": \"BTC\",\"amount\": \"" + withdrawAmount.toString() + "\",\"withdrawalAddressId\": \"" + WITHDRAW_ADDRESS_ID + "\"}";
        String result = api.post("main/api/v2/accounting/withdrawal", payload, time, true);
        System.out.println(result);
    }
}