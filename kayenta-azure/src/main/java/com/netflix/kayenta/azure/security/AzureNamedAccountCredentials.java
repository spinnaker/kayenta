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


import com.microsoft.azure.storage.blob.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.netflix.kayenta.security.AccountCredentials;
import lombok.Builder;
import lombok.Data;
import lombok.Singular;

import javax.validation.constraints.NotNull;
import java.util.List;

@Builder
@Data
public class AzureNamedAccountCredentials implements AccountCredentials<AzureCredentials> {

    @NotNull
    private String name;

    @NotNull
    @Singular
    private List<Type> supportedTypes;

    @NotNull
    private AzureCredentials credentials;

    private String container;
    private String storageAccountName;
    private String rootFolder;

    @Override
    public String getType() {
        return "azure";
    }

    @JsonIgnore
    private CloudBlobContainer azureContainer;
}

