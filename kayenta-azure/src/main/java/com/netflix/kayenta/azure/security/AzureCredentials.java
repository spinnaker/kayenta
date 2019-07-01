package com.netflix.kayenta.azure.security;

import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import com.microsoft.azure.storage.*;
import com.microsoft.azure.storage.blob.*;

import java.io.IOException;

@ToString
@Slf4j
public class AzureCredentials {

    @Getter
    private String storageAccountName;

    @Getter
    private String accountAccessKey;

    @Getter
    private String endpointSuffix;

    public AzureCredentials(String storageAccountName, String accountAccessKey , String endpointSuffix  ) {
        this.storageAccountName = storageAccountName;
        this.accountAccessKey = accountAccessKey;
        this.endpointSuffix = endpointSuffix;

    }

    public CloudBlobContainer getAzureContainer(String containerName) throws IOException,Exception{
        final String storageConnectionString =
                "DefaultEndpointsProtocol=http;"
                        + "AccountName="+this.storageAccountName+";"
                        + "AccountKey="+this.accountAccessKey+";"
                        + "EndpointSuffix="+this.endpointSuffix;

        // Retrieve storage account from connection-string.
        CloudStorageAccount storageAccount = CloudStorageAccount.parse(storageConnectionString);

        // Create the blob client.
        CloudBlobClient blobClient = storageAccount.createCloudBlobClient();

        // Get a reference to a container.
        // The container name must be lower case

        CloudBlobContainer container = blobClient.getContainerReference(containerName);

        // Create the container if it does not exist.
        container.createIfNotExists();

        return container;
    }
}
