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
    //This method is called automatically by Google’s OAuth library when it needs to create or access a data store (a place to save credentials like tokens).
    //method can handle any kind of serializable data type
    public <V extends Serializable> DataStore<V> getDataStore(String s) throws IOException {
        return (DataStore<V>)new JpaDataStore(this,s,repository);
    }
}

//DataStoreFactory is an interface that defines how to create a DataStore.
//Google’s client library uses it to decide where tokens are stored — file, memory, database, etc.
//A factory that produces the actual storage mechanism for credentials