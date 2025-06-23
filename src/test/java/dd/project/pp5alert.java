package dd.project;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.testng.annotations.Test;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;

public class pp5alert {
	@Test
    public void testStorageDetails() {
        JSch jsch = new JSch();
        com.jcraft.jsch.Session session = null;
        try {
            String user = "appUser";
            String host = "pp5.humanbrain.in";
            String password = "Brain@123";  // ⚠ Consider using environment variables instead.
            int port = 22;
            session = jsch.getSession(user, host, port);
            session.setPassword(password);
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect();

            Channel channel = session.openChannel("exec");
            ((ChannelExec) channel).setCommand("ls -lh --time-style=long-iso /mnt/local/nvmestorage/postImageProcessor");
            channel.setInputStream(null);
            ((ChannelExec) channel).setErrStream(System.err);
            InputStream in = channel.getInputStream();
            channel.connect();

            byte[] tmp = new byte[1024];
            StringBuilder output = new StringBuilder();
            while (true) {
                while (in.available() > 0) {
                    int i = in.read(tmp, 0, 1024);
                    if (i < 0) break;
                    output.append(new String(tmp, 0, i));
                }
                if (channel.isClosed()) {
                    if (in.available() > 0) continue;
                    break;
                }
                Thread.sleep(1000);
            }

            channel.disconnect();
            session.disconnect();

            String[] lines = output.toString().split("\n");
            System.out.println("Files in  /mnt/local/nvmestorage/postImageProcessor:\n");

            int todayFileCount = 0;
            int oldFileCount = 0;
            StringBuilder todayFiles = new StringBuilder();
            StringBuilder oldFiles = new StringBuilder();
        
            // Define date format and today's date outside the loop
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            String todayDate = sdf.format(new Date());

            for (String line : lines) {
                System.out.println("DEBUG: " + line);  // Print each line for debugging

                if (!line.startsWith("total") && !line.startsWith("drwx")) {
                    String[] parts = line.trim().split("\\s+", 8); // Splitting properly

                    if (parts.length >= 8) {
                        String fileDate = parts[5];   // Picking date in YYYY-MM-DD format
                        String fileName = parts[7];   // This should be the filename

                        System.out.println("Parsed Date: " + fileDate + ", File: " + fileName); // Debugging output

                        if (fileDate.equals(todayDate)) {
                            todayFileCount++;
                            todayFiles.append("<span style='color:red;'>" + fileDate + " - " + fileName + "</span><br>");
                        } else {
                            oldFileCount++;
                            oldFiles.append(fileDate + " - " + fileName + "<br>");
                        }
                    }
                }
            }

            // **Send email only if old files exist**
            if (oldFileCount > 0) {  
                sendEmailAlert(todayFiles.toString(), oldFiles.toString(), todayFileCount, oldFileCount, host);
            } else {
                System.out.println("No old files found. Email not sent.");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }  

    private void sendEmailAlert(String todayFiles, String oldFiles, int todayFileCount, int oldFileCount, String machineName) {
        String[] to = {"nathan.i@htic.iitm.ac.in"};
        String[] cc = {"venip@htic.iitm.ac.in"};
        String[] bcc = {"divya.d@htic.iitm.ac.in"};

        String from = "gayathri@htic.iitm.ac.in";
        String host = "smtp.gmail.com";

        Properties properties = System.getProperties();
        properties.put("mail.smtp.host", host);
        properties.put("mail.smtp.port", "465");
        properties.put("mail.smtp.ssl.enable", "true");
        properties.put("mail.smtp.auth", "true");

        Session session = Session.getInstance(properties, new javax.mail.Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication("gayathri@htic.iitm.ac.in", "Gayu@0918"); // Fix: Use app password
            }
        });

        session.setDebug(true);
        try {
            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(from));

            for (String recipient : to) {
                message.addRecipient(Message.RecipientType.TO, new InternetAddress(recipient));
            }
            for (String ccRecipient : cc) {
                message.addRecipient(Message.RecipientType.CC, new InternetAddress(ccRecipient));
            }
            for (String bccRecipient : bcc) {
                message.addRecipient(Message.RecipientType.BCC, new InternetAddress(bccRecipient));
            }

            message.setSubject("ALERT: Old Files Found in " + machineName + " 📂");
            String content = "<p>This is an automated alert:</p>" +
                    "<p>The directory <b> /mnt/local/nvmestorage/postImageProcessor</b> on machine <b style='color:blue;'>" + machineName + "</b> contains old files.</p>" +
                    "<p><b>" + oldFileCount + "</b> old files exist:</p>" +
                    "<pre>" + oldFiles + "</pre>" +
                    "<p>Please review and take necessary action.</p>" +
                    "<p>Best Regards,<br>Automated Monitoring System</p>";

            message.setContent(content, "text/html");
            System.out.println("Sending alert email...");
            Transport.send(message);
            System.out.println("Email sent successfully!");

        } catch (MessagingException mex) {
            mex.printStackTrace();
        }
    }
}



