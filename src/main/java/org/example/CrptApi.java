package org.example;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class CrptApi {
    private static final String API_URL = "https://ismp.crpt.ru/api/v3/lk/documents/create";
    private final Semaphore requestSemaphore;
    private final CloseableHttpClient httpClient;

    public CrptApi(TimeUnit timeUnit, int requestLimit) {
        requestSemaphore = new Semaphore(requestLimit);
        httpClient = HttpClientBuilder.create().build();

        // Очищаем семафор через указанный интервал времени
        new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(timeUnit.toMillis(1));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                requestSemaphore.drainPermits();
            }
        }).start();
    }

    public void createDocument(Object document, String signature) throws Exception {
        requestSemaphore.acquire(); // Ожидаем доступ к API

        JSONObject requestBody = new JSONObject();
        requestBody.put("document", document);
        requestBody.put("signature", signature);

        HttpPost httpPost = new HttpPost(API_URL);
        httpPost.setHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType());
        httpPost.setEntity(new StringEntity(requestBody.toJSONString(), ContentType.APPLICATION_JSON));

        HttpResponse response = httpClient.execute(httpPost);
        HttpEntity responseEntity = response.getEntity();
        if (responseEntity != null) {
            String responseBody = EntityUtils.toString(responseEntity);
            JSONParser jsonParser = new JSONParser();
            JSONObject jsonResponse = (JSONObject) jsonParser.parse(responseBody);

            System.out.println("Response: " + jsonResponse.toJSONString());
        }

        requestSemaphore.release(); // Освобождаем доступ к API
    }
}
