package com.aidanwhiteley.books.util.preprod;

import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class DataLoader {

    private static final String BOOKS_COLLECTION = "book";
    private static final String USERS_COLLECTION = "user";
    private static final Logger LOGGER = LoggerFactory.getLogger(DataLoader.class);
    private static final String AUTO_LOGON_ID = "Dummy12345678";

    private final MongoTemplate template;
    private final PreProdWarnings preProdWarnings;

    @Value("${books.reload.development.data}")
    private boolean reloadDevelopmentData;

    @Value("${books.autoAuthUser}")
    private boolean autoAuthUser;

    @Autowired
    public DataLoader(MongoTemplate mongoTemplate, PreProdWarnings preProdWarnings) {
        this.template = mongoTemplate;
        this.preProdWarnings = preProdWarnings;
    }


    /**
     * Reload data for development and integration tests. Whether this runs or
     * not is also controlled by the books.reload.development.data config
     * setting.
     * <p>
     * Reads from files where each line is expected to be a valid JSON object but
     * the whole file itself isnt a valid JSON object (hence the .data extension rather than .json).
     * <p>
     * "Fail safe" checking for required Spring profile being active and the config switch setting.
     */
    @Bean
    @Profile({"dev-mongo-java-server", "dev-mongo-java-server-no-auth", "dev-mongodb-no-auth", "dev-mongodb", "travis", "container-demo-no-auth"})
    public CommandLineRunner populateDummyData() {
        return args -> {

            if (reloadDevelopmentData) {

                preProdWarnings.displayDataReloadWarningMessage();
                loadBooksData();
                loadUserData();

            } else {
                LOGGER.info("Development data not reloaded due to config settings");
            }
        };
    }

    private void loadBooksData() throws IOException {
        List<String> jsons;
        ClassPathResource classPathResource = new ClassPathResource("sample_data/books.data");
        try (InputStream resource = classPathResource.getInputStream();
             InputStreamReader inputStreamReader = new InputStreamReader(resource, StandardCharsets.UTF_8);
             BufferedReader bufferedReader = new BufferedReader(inputStreamReader)) {

            // Clearing and loading data into books collection. We do this _after_ checking for the
            // existence of the file that holds the test data.
            LOGGER.info("Clearing books collection and loading development data for books project");
            template.dropCollection(BOOKS_COLLECTION);

            jsons = bufferedReader.lines().collect(Collectors.toList());
            jsons.stream().map(Document::parse).forEach(i -> template.insert(i, BOOKS_COLLECTION));
        }
    }

    private void loadUserData() throws IOException {
        List<String> jsons;
        ClassPathResource classPathResource = new ClassPathResource("sample_data/users.data");
        try (InputStream resource = classPathResource.getInputStream();
             InputStreamReader inputStreamReader = new InputStreamReader(resource, StandardCharsets.UTF_8);
             BufferedReader bufferedReader = new BufferedReader(inputStreamReader)) {

            // Clearing and loading data into user collection - happens after user creation file found and loaded
            LOGGER.info("Clearing users collection and loading development data for books project");
            template.dropCollection(USERS_COLLECTION);

            jsons = bufferedReader.lines().collect(Collectors.toList());
        }
        jsons.stream().map(Document::parse).forEach(i -> {
            boolean autoAuthUserServiceId = i.get("authenticationServiceId").toString().contains(AUTO_LOGON_ID);
            // Only insert the user data for the "auto logon" user if the config says to
            if (!autoAuthUserServiceId || autoAuthUser) {
                template.insert(i, USERS_COLLECTION);
            }
        });
    }

}
