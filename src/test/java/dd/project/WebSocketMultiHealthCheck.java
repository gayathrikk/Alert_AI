package dd.project;

import io.restassured.RestAssured;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.net.URI;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class WebSocketAndApiTest {

    @Test
    public void testRestApiEndpoint() {
        String apiUrl = "https://apollo2.humanbrain.in/aiAgentServer/agent/recomendation?user_query=hi&page_context=%22%7B%5C%22ssid%5C%22%3A85%2C%5C%22seriesType%5C%22%3A%5C%22NISSL%5C%22%2C%5C%22secid%5C%22%3A52%2C%5C%22biosampleId%5C%22%3A%5C%22201%5C%22%7D%22&page=Atlas%20Editor&first=true&action_context=null";

        int statusCode = RestAssured
                .given()
                .when()
                .get(apiUrl)
                .then()
                .extract()
                .statusCode();

        Assert.assertEquals(statusCode, 200, "API is not healthy!");
    }

    @Test
    public void testWebSocketConnection() throws Exception {
        String wsUrl = "wss://apollo2.humanbrain.in/aiAgentServer/ws/ai_agent";
        CountDownLatch latch = new CountDownLatch(1);

        WebSocketClient client = new WebSocketClient(new URI(wsUrl)) {
            @Override
            public void onOpen(ServerHandshake handshakedata) {
                System.out.println("WebSocket Connected ✅");
                latch.countDown(); // Mark success
            }

            @Override
            public void onMessage(String message) {
                System.out.println("Received message: " + message);
            }

            @Override
            public void onClose(int code, String reason, boolean remote) {
                System.out.println("WebSocket Closed ❌: " + reason);
            }

            @Override
            public void onError(Exception ex) {
                System.err.println("WebSocket Error ❌: " + ex.getMessage());
            }
        };

        client.connect();
        boolean connected = latch.await(10, TimeUnit.SECONDS); // Wait max 10 seconds
        Assert.assertTrue(connected, "WebSocket connection failed!");
        client.close();
    }
}
