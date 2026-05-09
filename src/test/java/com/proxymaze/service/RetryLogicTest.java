package com.proxymaze.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proxymaze.model.Alert;
import com.proxymaze.model.WebhookRegistration;
import com.proxymaze.storage.DataStore;
import okhttp3.*;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class RetryLogicTest {

    @Test
    public void testRetryOn5xx() throws Exception {
        DataStore dataStore = mock(DataStore.class);
        ObjectMapper mapper = new ObjectMapper();
        OkHttpClient client = mock(OkHttpClient.class);
        Call call = mock(Call.class);

        // First response 503, second response 200
        Response response503 = new Response.Builder()
                .request(new Request.Builder().url("http://test.com").build())
                .protocol(Protocol.HTTP_1_1)
                .code(503)
                .message("Service Unavailable")
                .body(ResponseBody.create("", MediaType.parse("application/json")))
                .build();

        Response response200 = new Response.Builder()
                .request(new Request.Builder().url("http://test.com").build())
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body(ResponseBody.create("", MediaType.parse("application/json")))
                .build();

        when(client.newCall(any())).thenReturn(call);
        when(call.execute()).thenReturn(response503, response200);

        WebhookRegistration wh = new WebhookRegistration();
        wh.setWebhookId("wh-1");
        wh.setUrl("http://test.com");

        when(dataStore.getAllWebhooks()).thenReturn(List.of(wh));
        when(dataStore.getAllIntegrations()).thenReturn(List.of());

        WebhookDeliveryService service = new WebhookDeliveryService(dataStore, mapper, client);
        
        Alert alert = new Alert();
        alert.setAlertId("a-1");
        
        // We need to run this in a way that we can wait for completion or just test the retry loop logic
        // Since it's async in dispatch, let's call sendWithRetry directly for testing
        
        // Use reflection or make it package-private for testing
        java.lang.reflect.Method method = WebhookDeliveryService.class.getDeclaredMethod("sendWithRetry", String.class, Object.class, String.class, int.class);
        method.setAccessible(true);
        method.invoke(service, "http://test.com", new Object(), "key-1", 10);

        // Verify it was called twice
        verify(call, times(2)).execute();
        verify(dataStore, times(1)).incrementWebhookDeliveries();
    }
}
