package com.netflix.kayenta.blobs.storage;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.blob.BlobProperties;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.CloudBlockBlob;
import com.microsoft.azure.storage.blob.ListBlobItem;
import com.netflix.kayenta.index.CanaryConfigIndex;
import com.netflix.kayenta.security.AccountCredentialsRepository;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

@Slf4j
public class TestableBlobsStorageService extends BlobsStorageService {

    public HashMap<String,String> blobStored;

    TestableBlobsStorageService(ObjectMapper kayentaObjectMapper, List<String> accountNames, AccountCredentialsRepository accountCredentialsRepository, CanaryConfigIndex canaryConfigIndex) {
        super(kayentaObjectMapper, accountNames, accountCredentialsRepository, canaryConfigIndex);
        this.blobStored = new HashMap<String, String>();
    }

    @Override
    protected Iterable<ListBlobItem> listBlobs(CloudBlobContainer container, String prefix, boolean useFlatBlobListing, boolean isFolder) {
        if(blobStored.get("exceptionKey") == "1")
        {
            throw new IllegalArgumentException("Item not found at "+prefix);
        }
        else
        {
            Iterable<ListBlobItem> mockBlobItems = new ArrayList<ListBlobItem>();
            String filename = "canary_test.json";
            try {
               if (isFolder) {
                   for (int folderItem = 1; folderItem <= 6; folderItem++) {

                       URI uri = new URI("http://cloudblob.blob/sample-container/" + prefix + "/(GUID" + folderItem + ")/" + filename);
                       CloudBlockBlob fakeBlobItem = new CloudBlockBlob(uri);
                       ((ArrayList<ListBlobItem>) mockBlobItems).add((ListBlobItem) fakeBlobItem);
                       blobStored.put(String.format("deleteIfexists(%s)", prefix + "/" + filename), "not_invoked");

                   }
               } else {

                       URI uri = new URI("http://cloudblob.blob/sample-container/" + prefix + "/" + filename);
                       CloudBlockBlob fakeBlobItem = new CloudBlockBlob(uri);
                       ((ArrayList<ListBlobItem>) mockBlobItems).add((ListBlobItem) fakeBlobItem);
                       blobStored.put(String.format("deleteIfexists(%s)", prefix + "/" + filename), "not_invoked");

               }
                return mockBlobItems;
               }
           catch (StorageException | URISyntaxException e) {
               log.error("Failed to initialiaze, Test Blob" + e.getMessage());
               }
            return mockBlobItems;
           }
        }

    @Override
    protected String downloadText(CloudBlockBlob blob) {
        String downloadedTextExample;
        if(blobStored.get("exceptionKey")=="2")
            downloadedTextExample = "{\"applications\":[ + blobStored.get(\"application\") + ]}";
        else
            downloadedTextExample = "{\"applications\":[\"" + blobStored.get("application") + "\"]}";

        return downloadedTextExample;
    }

    @Override
    public CloudBlockBlob getBlockBlobReference(CloudBlobContainer container, final String blobName) throws URISyntaxException, StorageException {
        URI uri = new URI ( "http://cloudblob.blob/sample-container/"+blobName);
        CloudBlockBlob fakeBlobItem = new CloudBlockBlob(uri);
        return fakeBlobItem;
    }

    @Override
    public void uploadFromByteArray(CloudBlockBlob blob, final byte[] bytes, final int offset, final int length) {
        blobStored.put("blob",blob.getName());
        blobStored.put("length",Integer.toString(length));
    }

    @Override
    public boolean deleteIfExists(CloudBlockBlob blob) {
        blobStored.put(String.format("deleteIfexists(%s)",blob.getName()),"invoked");
        return true;
    }

    @Override
    public Date getLastModified(BlobProperties properties) {
        return new Date();
    }

    @Override
    public boolean createIfNotExists(CloudBlobContainer container) {
        return true;
    }

}
