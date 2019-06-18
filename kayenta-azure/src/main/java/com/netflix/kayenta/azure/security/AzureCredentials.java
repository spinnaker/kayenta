/*
 * Copyright 2017 Google, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netflix.kayenta.azure.security;

import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import com.microsoft.azure.storage.*;
import com.microsoft.azure.storage.blob.*;

import java.io.IOException;
import java.util.Optional;


@ToString
@Slf4j
public class AzureCredentials {

    private static String applicationVersion =
            Optional.ofNullable(AzureCredentials.class.getPackage().getImplementationVersion()).orElse("Unknown");
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

        // Create a permissions object.
        BlobContainerPermissions containerPermissions = new BlobContainerPermissions();

        // Include public access in the permissions object.
        containerPermissions.setPublicAccess(BlobContainerPublicAccessType.CONTAINER);

        // Set the permissions on the container.
        container.uploadPermissions(containerPermissions);
        return container;

    }
}
