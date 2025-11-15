package com.gmailIntegeration.Utils;

import com.gmailIntegeration.Entity.GmailTokenEntity;
import com.gmailIntegeration.Repo.GmailTokenRepository;
import com.google.api.client.auth.oauth2.StoredCredential;
import com.google.api.client.util.store.AbstractDataStore;
import com.google.api.client.util.store.DataStore;
import com.google.api.client.util.store.DataStoreFactory;

import java.io.IOException;
import java.time.Instant;
import java.util.Collection;

import java.util.Set;
import java.util.stream.Collectors;

//for storing tokens in JPA repository
public class JpaDataStore extends AbstractDataStore<StoredCredential> {

    private final GmailTokenRepository repository;

    protected JpaDataStore(DataStoreFactory dataStoreFactory, String id, GmailTokenRepository repository) {
        super(dataStoreFactory, id);
        this.repository = repository;
    }

    @Override
    public Set<String> keySet() throws IOException {
        return repository.findAll()
                .stream()
                .map(GmailTokenEntity::getUserEmail)
                .collect(Collectors.toSet());
    }

    @Override
    public Collection<StoredCredential> values() throws IOException {
        return repository.findAll()
                .stream()
                .map(this::toStoredCredential)
                .collect(Collectors.toList());
    }

    @Override
    public StoredCredential get(String userEmail) throws IOException {
        return repository.findByUserEmail(userEmail)
                .map(this::toStoredCredential)
                .orElse(null);
    }

    @Override
    public DataStore<StoredCredential> set(String userEmail, StoredCredential storedCredential) throws IOException {

        // Check if a token row already exists
        GmailTokenEntity token = repository.findByUserEmail(userEmail)
                .orElse(new GmailTokenEntity());

        boolean isNew = (token.getUserId() == null);

        System.out.println("[OAuth] Saving token for user: " + userEmail);
        System.out.println("[OAuth] Token status: " + (isNew ? "NEW token entry" : "Updating existing token"));

        //set field
        System.out.println("Storing token for user: " + userEmail);
        token.setUserEmail(userEmail);
        token.setAccessToken(storedCredential.getAccessToken());
        token.setRefreshToken(storedCredential.getRefreshToken());
        token.setScope("https://www.googleapis.com/auth/gmail.modify");
        token.setTokenType("Bearer");

        if (storedCredential.getExpirationTimeMilliseconds() != null) {
            token.setExpiryTime(
                    Instant.ofEpochMilli(storedCredential.getExpirationTimeMilliseconds())
            );
        }

        //saved
        repository.save(token);

        System.out.println("[OAuth] Token saved successfully.");
        if (isNew) {
            System.out.println("[OAuth] Token created at: " + token.getCreatedAt());
        }
        System.out.println("[OAuth] Token updated at: " + token.getUpdatedAt());

        return this;
    }

    @Override
    public DataStore<StoredCredential> clear() throws IOException {
        repository.deleteAll();
        return this;
    }

    @Override
    public DataStore<StoredCredential> delete(String userEmail) throws IOException {
        repository.findByUserEmail(userEmail).ifPresent(repository::delete);
        return this;
    }


    private StoredCredential toStoredCredential(GmailTokenEntity token) {
        StoredCredential credential = new StoredCredential();
        credential.setAccessToken(token.getAccessToken());
        credential.setRefreshToken(token.getRefreshToken());
        if (token.getExpiryTime() != null)
            credential.setExpirationTimeMilliseconds(token.getExpiryTime().toEpochMilli());
        return credential;
    }
}

//com.google.api.client.util.store.AbstractDataStore
//t’s a generic class — meaning you can store any type of object (like StoredCredential, or even something else if you wanted).
//A standard interface for saving and loading data (OAuth tokens, credentials, etc.)
//Thread-safe and consistent behavior across different storage types (file, memory, DB)
//A pluggable mechanism — so developers can choose where credentials live
