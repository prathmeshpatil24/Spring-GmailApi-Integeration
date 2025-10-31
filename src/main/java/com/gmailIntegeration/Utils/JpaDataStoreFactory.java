package com.gmailIntegeration.Utils;

import com.gmailIntegeration.Repo.GmailTokenRepository;
import com.google.api.client.util.store.DataStore;
import com.google.api.client.util.store.DataStoreFactory;

import java.io.IOException;
import java.io.Serializable;

public class JpaDataStoreFactory implements DataStoreFactory {

    private final GmailTokenRepository repository;

    public JpaDataStoreFactory(GmailTokenRepository repository) {
        this.repository = repository;
    }

    @Override
    public <V extends Serializable> DataStore<V> getDataStore(String s) throws IOException {
        return (DataStore<V>)new JpaDataStore(this,s,repository);
    }
}
