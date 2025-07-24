package dd.project;

import java.net.URI;
import java.util.Map;
import java.util.Properties;
import java.util.HashMap;
import java.util.concurrent.*;

import javax.mail.*;
import javax.mail.internet.*;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

public class WebSocketMultiHealthCheck {

    public static void main(String[] args) {
        Map<String, String> webSockets = new HashMap<>();
        webSockets.put("apollo2.humanbrain.in WebSocket", "wss://apollo2.humanbrain.in/aiAgentServer/ws/ai_agent");
        webSockets.put("dev2mani.humanbrain.in WebSocket", "wss://apollo2.humanbrain.in/aiAgentServer/ws/ai_agent");

        Map<String, String> httpUrls = new HashMap<>();
        httpUrls.put("Apollo2 Recommendation API", "https://apollo2.humanbrain.in/aiAgentServer/agent/recomendation?user_query=hi&page_context=%22%7B%5C%22ssid%5C%22%3A85%2C%5C%22seriesType%5C%22%3A%5C%22NISSL%5C%22%2C%5C%22secid%5C%22%3A52%2C%5C%22biosampleId%5C%22%3A%5C%22201%5C%22%7D%22&page=Atlas%20Editor&first=true&action_context=null");

        // Run WebSocket checks
        for (Map.Entry<String, String> entry : webSockets.entrySet()) {
            String serverName = entry.getKey();
            String webSocketUrl = entry.getValue();
            try {
                testWebSocketConnection(serverName, webSocketUrl);
            } catch (Exception e) {
                System.err.println("❌ Exception while checking " + serverName + ": " + e.getMessage());
                sendAlertMail(serverName, e.getMessage(), "222 1000", "Divya D", 193, "Neurovoyager");
            }
        }

        // Run HTTP GET checks
        for (Map.Entry<String, String> entry : httpUrls.entrySet()) {
            String name = entry.getKey();
            String url = entry.getValue();
            testHttpGetUrl(name, url);
        }
    }

    private static void testHttpGetUrl(String name, String url) {
        System.out.println("🌐 Checking HTTP GET: " + name + " → " + url);
        try {
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) new java.net.URL(url).openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);
            int responseCode = conn.getResponseCode();
            System.out.println("✅ " + name + " responded with HTTP status: " + responseCode);

            if (responseCode != 200) {
                sendAlertMail(name, "HTTP GET returned status " + responseCode, "hi", "Divya D", 193, "Atlas Editor");
            }
        } catch (Exception e) {
            System.err.println("❌ HTTP GET failed for " + name + ": " + e.getMessage());
            sendAlertMail(name, e.getMessage(), "hi", "Divya D", 193, "Atlas Editor");
        }
    }

    private static void testWebSocketConnection(String serverName, String webSocketUrl) throws InterruptedException {
        System.out.println("🔍 Connecting to " + serverName + " → " + webSocketUrl);

        CountDownLatch latch = new CountDownLatch(1);
        final boolean[] success = {false};

        WebSocketClient client = new WebSocketClient(URI.create(webSocketUrl)) {
            private final StringBuilder responseBuffer = new StringBuilder();
            private ScheduledExecutorService scheduler;
            private ScheduledFuture<?> timeoutFuture;

            @Override
            public void onOpen(ServerHandshake handshake) {
                String testMessage = "{"
                        + "\"query\": \"222 1000\","
                        + "\"user\": \"Divya D\","
                        + "\"userId\": 193,"
                        + "\"page\": \"Neurovoyager\","
                        + "\"page_context\": {}"
                        + "}";
                send(testMessage);
                System.out.println("📤 Sent: " + testMessage);

                scheduler = Executors.newSingleThreadScheduledExecutor();
                timeoutFuture = scheduler.schedule(() -> {
                    System.err.println("❌ Timeout (15 seconds) waiting for response from " + serverName);
                    latch.countDown();
                    close();
                }, 15, TimeUnit.SECONDS);
            }

            @Override
            public void onMessage(String message) {
                System.out.println("📥 Received: " + message);
                responseBuffer.append(message);
                if (message.contains("###END")) {
                    if (timeoutFuture != null) timeoutFuture.cancel(true);
                    System.out.println("✅ Full AI Agent response received from " + serverName);
                    success[0] = true;
                    latch.countDown();
                    close();
                }
            }

            @Override
            public void onClose(int code, String reason, boolean remote) {
                System.out.println("❌ WebSocket Closed for " + serverName + ". Code: " + code + ", Reason: " + reason);
                if (timeoutFuture != null) timeoutFuture.cancel(true);
                if (scheduler != null) scheduler.shutdownNow();
                latch.countDown();
            }

            @Override
            public void onError(Exception ex) {
                System.err.println("❌ Error in " + serverName + " WebSocket: " + ex.getMessage());
                if (timeoutFuture != null) timeoutFuture.cancel(true);
                if (scheduler != null) scheduler.shutdownNow();
                latch.countDown();
            }
        };

        try {
            client.connectBlocking();
        } catch (Exception e) {
            System.err.println("❌ Could not connect to " + serverName + ": " + e.getMessage());
            sendAlertMail(serverName, e.getMessage(), "222 1000", "Divya D", 193, "Neurovoyager");
            return;
        }

        latch.await();

        if (success[0]) {
            System.out.println("✅ WebSocket connection to " + serverName + " succeeded.");
        } else {
            sendAlertMail(serverName, "No complete response received.", "222 1000", "Divya D", 193, "Neurovoyager");
        }
    }

    private static void sendAlertMail(String serverName, String reason, String query, String user, int userId, String page) {
        String[] to = {"sriramv@htic.iitm.ac.in"};
        String[] cc = {"venip@htic.iitm.ac.in", "divya.d@htic.iitm.ac.in", "gayathri@htic.iitm.ac.in"};
        String from = "gayathri@htic.iitm.ac.in";
        String host = "smtp.gmail.com";

        Properties props = System.getProperties();
        props.put("mail.smtp.host", host);
        props.put("mail.smtp.port", "587");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.auth", "true");

        Session session = Session.getInstance(props, new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication("automationsoftware25@gmail.com", "wjzcgaramsqvagxu");
            }
        });

        session.setDebug(false);

        try {
            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(from));

            for (String recipient : to) {
                message.addRecipient(Message.RecipientType.TO, new InternetAddress(recipient));
            }
            for (String ccRecipient : cc) {
                message.addRecipient(Message.RecipientType.CC, new InternetAddress(ccRecipient));
            }

            message.setSubject("AI Agent - Connection Issue Alert: " + serverName);

            String currentTime = java.time.LocalDateTime.now().toString();

            String content = "<div style='font-family: Arial, sans-serif; font-size: 14px; color: #333;'>"
                    + "<h3 style='color: #D9534F;'>🚨 AI Agent Connection Failure Alert</h3>"
                    + "<p>Hi Team,</p>"
                    + "<p><strong>Connection to <span style='color:#5bc0de;'>" + serverName + "</span> failed at <strong>" + currentTime + "</strong>.</strong></p>"
                    + "<p><u><strong>Error Details:</strong></u></p>"
                    + "<ul>"
                    + "<li><strong>Page:</strong> " + page + "</li>"
                    + "<li><strong>User:</strong> " + user + " (ID: " + userId + ")</li>"
                    + "<li><strong>Query:</strong> " + query + "</li>"
                    + "<li><strong>Reason:</strong> " + reason + "</li>"
                    + "</ul>"
                    + "<p><u><strong>Action:</strong></u></p>"
                    + "<p>Please check WebSocket/HTTP server status, nginx configuration, or SSL certificate.</p>"
                    + "<br><p style='color: #555;'>Regards,<br><b>Automated Monitoring</b></p>"
                    + "</div>";

            message.setContent(content, "text/html");

            System.out.println("📧 Sending alert email...");
            Transport.send(message);
            System.out.println("✅ Alert email sent successfully.");

        } catch (MessagingException mex) {
            System.err.println("❌ Email sending failed: " + mex.getMessage());
        }
    }
}
