package at.ac.tuwien.inso.ctrl.config;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.GmailScopes;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;

import java.io.File;
import java.io.InputStreamReader;
import java.util.List;

@Configuration
@ConditionalOnMissingBean(Gmail.class)
public class GmailConfig {
  private static final List<String> SCOPES = List.of(GmailScopes.GMAIL_SEND);
  private static String TOKENS_DIR = "tokens";

  @Bean
  public Gmail gmailService() throws Exception{
    NetHttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
    GsonFactory jsonFactory = GsonFactory.getDefaultInstance();

    GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(
        jsonFactory,
        new InputStreamReader(new ClassPathResource("credentials.json").getInputStream())
    );

    GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
        httpTransport, jsonFactory, clientSecrets, SCOPES)
        .setDataStoreFactory(new FileDataStoreFactory(new File(TOKENS_DIR)))
        .setAccessType("offline").build();

    LocalServerReceiver receiver = new LocalServerReceiver.Builder()
        .setPort(8888).build();

    Credential credential = new AuthorizationCodeInstalledApp(flow, receiver)
        .authorize("user");

    return new Gmail.Builder(httpTransport, jsonFactory, credential)
        .setApplicationName("CTRL")
        .build();
  }
}
