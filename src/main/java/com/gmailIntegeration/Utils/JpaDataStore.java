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
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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
        GmailTokenEntity token = repository.findByUserEmail(userEmail).orElse(new GmailTokenEntity());
        System.out.println("Storing token for user: " + userEmail);
        token.setUserEmail(userEmail);
        token.setAccessToken(storedCredential.getAccessToken());
        token.setRefreshToken(storedCredential.getRefreshToken());
        token.setScope("https://www.googleapis.com/auth/gmail.modify");
        token.setTokenType("Bearer");
        token.setExpiryTime(storedCredential.getExpirationTimeMilliseconds() != null
                ? Instant.ofEpochMilli(storedCredential.getExpirationTimeMilliseconds())
                : null);
        repository.save(token);
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
