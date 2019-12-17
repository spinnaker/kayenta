/*
 * Copyright 2019 Alibaba Group.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
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

package com.netflix.kayenta.oss.storage;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSException;
import com.aliyun.oss.model.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.netflix.kayenta.alicloud.security.AliCloudNamedAccountCredentials;
import com.netflix.kayenta.canary.CanaryConfig;
import com.netflix.kayenta.index.CanaryConfigIndex;
import com.netflix.kayenta.index.config.CanaryConfigIndexAction;
import com.netflix.kayenta.security.AccountCredentialsRepository;
import com.netflix.kayenta.storage.ObjectType;
import com.netflix.kayenta.storage.StorageService;
import com.netflix.kayenta.util.Retry;
import com.netflix.spinnaker.kork.web.exceptions.NotFoundException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.*;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.Singular;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;

@Builder
@Slf4j
public class OssStorageService implements StorageService {

  public final int MAX_RETRIES = 10;
  public final long RETRY_BACKOFF = 1000;

  @NotNull private ObjectMapper objectMapper;

  @NotNull @Singular @Getter private List<String> accountNames;

  @Autowired AccountCredentialsRepository accountCredentialsRepository;

  @Autowired CanaryConfigIndex canaryConfigIndex;

  @Override
  public boolean servicesAccount(String accountName) {
    return accountNames.contains(accountName);
  }

  private final Retry retry = new Retry();

  /** Check to see if the bucket exists, creating it if it is not there. */
  public void ensureBucketExists(String accountName) {
    AliCloudNamedAccountCredentials credentials =
        (AliCloudNamedAccountCredentials)
            accountCredentialsRepository
                .getOne(accountName)
                .orElseThrow(
                    () ->
                        new IllegalArgumentException(
                            "Unable to resolve account " + accountName + "."));

    OSS ossClient = credentials.getOssClient();
    String bucket = credentials.getBucket();
    CreateBucketRequest createBucketRequest = new CreateBucketRequest(bucket);

    try {
      ossClient.createBucket(createBucketRequest);
    } catch (OSSException e) {
      log.error("Could not create bucket {}: {}", bucket, e);
      throw e;
    }
  }

  @Override
  public <T> T loadObject(String accountName, ObjectType objectType, String objectKey)
      throws IllegalArgumentException, NotFoundException {
    AliCloudNamedAccountCredentials credentials =
        (AliCloudNamedAccountCredentials)
            accountCredentialsRepository
                .getOne(accountName)
                .orElseThrow(
                    () ->
                        new IllegalArgumentException(
                            "Unable to resolve account " + accountName + "."));

    OSS ossClient = credentials.getOssClient();
    String bucket = credentials.getBucket();
    String path;
    try {
      path = resolveSingularPath(objectType, objectKey, credentials, ossClient, bucket);
    } catch (IllegalArgumentException e) {
      throw new NotFoundException(e.getMessage());
    }

    try {
      OSSObject ossObject = ossClient.getObject(bucket, path);
      return deserialize(ossObject, objectType.getTypeReference());
    } catch (IOException e) {
      throw new IllegalStateException("Unable to deserialize object (key: " + objectKey + ")", e);
    }
  }

  private <T> T deserialize(OSSObject ossObject, TypeReference typeReference) throws IOException {
    return objectMapper.readValue(ossObject.getObjectContent(), typeReference);
  }

  private void checkForDuplicateCanaryConfig(
      CanaryConfig canaryConfig,
      String canaryConfigId,
      AliCloudNamedAccountCredentials credentials) {
    String canaryConfigName = canaryConfig.getName();
    List<String> applications = canaryConfig.getApplications();
    String existingCanaryConfigId =
        canaryConfigIndex.getIdFromName(credentials, canaryConfigName, applications);

    // We want to avoid creating a naming collision due to the renaming of an existing canary
    // config.
    if (!StringUtils.isEmpty(existingCanaryConfigId)
        && !existingCanaryConfigId.equals(canaryConfigId)) {
      throw new IllegalArgumentException(
          "Canary config with name '"
              + canaryConfigName
              + "' already exists in the scope of applications "
              + applications
              + ".");
    }
  }

  private String buildOSSKey(
      AliCloudNamedAccountCredentials credentials,
      ObjectType objectType,
      String group,
      String objectKey,
      String metadataFilename) {
    if (metadataFilename == null) {
      metadataFilename = objectType.getDefaultFilename();
    }

    if (objectKey.endsWith(metadataFilename)) {
      return objectKey;
    }

    return (buildTypedFolder(credentials, group) + "/" + objectKey + "/" + metadataFilename)
        .replace("//", "/");
  }

  @Override
  public <T> void storeObject(
      String accountName,
      ObjectType objectType,
      String objectKey,
      T obj,
      String filename,
      boolean isAnUpdate) {
    AliCloudNamedAccountCredentials credentials =
        (AliCloudNamedAccountCredentials)
            accountCredentialsRepository
                .getOne(accountName)
                .orElseThrow(
                    () ->
                        new IllegalArgumentException(
                            "Unable to resolve account " + accountName + "."));

    OSS ossClient = credentials.getOssClient();
    String bucket = credentials.getBucket();
    String group = objectType.getGroup();
    String path = buildOSSKey(credentials, objectType, group, objectKey, filename);

    ensureBucketExists(accountName);

    long updatedTimestamp = -1;
    String correlationId = null;
    String canaryConfigSummaryJson = null;
    final String originalPath;

    if (objectType == ObjectType.CANARY_CONFIG) {
      updatedTimestamp = canaryConfigIndex.getRedisTime();

      CanaryConfig canaryConfig = (CanaryConfig) obj;

      checkForDuplicateCanaryConfig(canaryConfig, objectKey, credentials);

      if (isAnUpdate) {
        // Storing a canary config while not checking for naming collisions can only be a PUT (i.e.
        // an update to an existing config).
        originalPath = resolveSingularPath(objectType, objectKey, credentials, ossClient, bucket);
      } else {
        originalPath = null;
      }

      correlationId = UUID.randomUUID().toString();

      Map<String, Object> canaryConfigSummary =
          new ImmutableMap.Builder<String, Object>()
              .put("id", objectKey)
              .put("name", canaryConfig.getName())
              .put("updatedTimestamp", updatedTimestamp)
              .put("updatedTimestampIso", Instant.ofEpochMilli(updatedTimestamp).toString())
              .put("applications", canaryConfig.getApplications())
              .build();

      try {
        canaryConfigSummaryJson = objectMapper.writeValueAsString(canaryConfigSummary);
      } catch (JsonProcessingException e) {
        throw new IllegalArgumentException(
            "Problem serializing canaryConfigSummary -> " + canaryConfigSummary, e);
      }

      canaryConfigIndex.startPendingUpdate(
          credentials,
          updatedTimestamp + "",
          CanaryConfigIndexAction.UPDATE,
          correlationId,
          canaryConfigSummaryJson);
    } else {
      originalPath = null;
    }
    try {
      byte[] bytes = objectMapper.writeValueAsBytes(obj);
      ObjectMetadata objectMetadata = new ObjectMetadata();
      objectMetadata.setContentLength(bytes.length);
      objectMetadata.setContentMD5(
          new String(org.apache.commons.codec.binary.Base64.encodeBase64(DigestUtils.md5(bytes))));

      retry.retry(
          () -> ossClient.putObject(bucket, path, new ByteArrayInputStream(bytes), objectMetadata),
          MAX_RETRIES,
          RETRY_BACKOFF);

      if (objectType == ObjectType.CANARY_CONFIG) {
        // This will be true if the canary config is renamed.
        if (originalPath != null && !originalPath.equals(path)) {
          retry.retry(
              () -> ossClient.deleteObject(bucket, originalPath), MAX_RETRIES, RETRY_BACKOFF);
        }

        canaryConfigIndex.finishPendingUpdate(
            credentials, CanaryConfigIndexAction.UPDATE, correlationId);
      }
    } catch (Exception e) {
      log.error("Update failed on path {}: {}", buildTypedFolder(credentials, group), e);

      if (objectType == ObjectType.CANARY_CONFIG) {
        canaryConfigIndex.removeFailedPendingUpdate(
            credentials,
            updatedTimestamp + "",
            CanaryConfigIndexAction.UPDATE,
            correlationId,
            canaryConfigSummaryJson);
      }

      throw new IllegalArgumentException(e);
    }
  }

  private String resolveSingularPath(
      ObjectType objectType,
      String objectKey,
      AliCloudNamedAccountCredentials credentials,
      OSS ossClient,
      String bucket) {
    String rootFolder = daoRoot(credentials, objectType.getGroup()) + "/" + objectKey;
    ObjectListing objectListing =
        ossClient.listObjects(new ListObjectsRequest(bucket, rootFolder, null, null, 1000));
    List<OSSObjectSummary> objectSummaries = objectListing.getObjectSummaries();

    if (objectSummaries != null && objectSummaries.size() == 1) {
      return objectSummaries.get(0).getKey();
    } else {
      throw new IllegalArgumentException(
          "Unable to resolve singular "
              + objectType
              + " at "
              + daoRoot(credentials, objectType.getGroup())
              + '/'
              + objectKey
              + ".");
    }
  }

  private String daoRoot(AliCloudNamedAccountCredentials credentials, String daoTypeName) {
    return credentials.getRootFolder() + '/' + daoTypeName;
  }

  @Override
  public void deleteObject(String accountName, ObjectType objectType, String objectKey) {
    AliCloudNamedAccountCredentials credentials =
        (AliCloudNamedAccountCredentials)
            accountCredentialsRepository
                .getOne(accountName)
                .orElseThrow(
                    () ->
                        new IllegalArgumentException(
                            "Unable to resolve account " + accountName + "."));

    OSS ossClient = credentials.getOssClient();
    String bucket = credentials.getBucket();
    String path = resolveSingularPath(objectType, objectKey, credentials, ossClient, bucket);

    long updatedTimestamp = -1;
    String correlationId = null;
    String canaryConfigSummaryJson = null;

    if (objectType == ObjectType.CANARY_CONFIG) {
      updatedTimestamp = canaryConfigIndex.getRedisTime();

      Map<String, Object> existingCanaryConfigSummary =
          canaryConfigIndex.getSummaryFromId(credentials, objectKey);

      if (existingCanaryConfigSummary != null) {
        String canaryConfigName = (String) existingCanaryConfigSummary.get("name");
        List<String> applications = (List<String>) existingCanaryConfigSummary.get("applications");

        correlationId = UUID.randomUUID().toString();

        Map<String, Object> canaryConfigSummary =
            new ImmutableMap.Builder<String, Object>()
                .put("id", objectKey)
                .put("name", canaryConfigName)
                .put("updatedTimestamp", updatedTimestamp)
                .put("updatedTimestampIso", Instant.ofEpochMilli(updatedTimestamp).toString())
                .put("applications", applications)
                .build();

        try {
          canaryConfigSummaryJson = objectMapper.writeValueAsString(canaryConfigSummary);
        } catch (JsonProcessingException e) {
          throw new IllegalArgumentException(
              "Problem serializing canaryConfigSummary -> " + canaryConfigSummary, e);
        }

        canaryConfigIndex.startPendingUpdate(
            credentials,
            updatedTimestamp + "",
            CanaryConfigIndexAction.DELETE,
            correlationId,
            canaryConfigSummaryJson);
      }
    }

    try {
      retry.retry(() -> ossClient.deleteObject(bucket, path), MAX_RETRIES, RETRY_BACKOFF);

      if (correlationId != null) {
        canaryConfigIndex.finishPendingUpdate(
            credentials, CanaryConfigIndexAction.DELETE, correlationId);
      }
    } catch (Exception e) {
      log.error("Failed to delete path {}: {}", path, e);

      if (correlationId != null) {
        canaryConfigIndex.removeFailedPendingUpdate(
            credentials,
            updatedTimestamp + "",
            CanaryConfigIndexAction.DELETE,
            correlationId,
            canaryConfigSummaryJson);
      }

      throw new IllegalArgumentException(e);
    }
  }

  @Override
  public List<Map<String, Object>> listObjectKeys(
      String accountName, ObjectType objectType, List<String> applications, boolean skipIndex) {
    AliCloudNamedAccountCredentials credentials =
        (AliCloudNamedAccountCredentials)
            accountCredentialsRepository
                .getOne(accountName)
                .orElseThrow(
                    () ->
                        new IllegalArgumentException(
                            "Unable to resolve account " + accountName + "."));
    if (!skipIndex && objectType == ObjectType.CANARY_CONFIG) {
      Set<Map<String, Object>> canaryConfigSet =
          canaryConfigIndex.getCanaryConfigSummarySet(credentials, applications);

      return Lists.newArrayList(canaryConfigSet);
    } else {
      OSS ossClient = credentials.getOssClient();
      String group = objectType.getGroup();
      String prefix = buildTypedFolder(credentials, group);
      int skipToOffset = prefix.length() + 1; // + Trailing slash
      List<Map<String, Object>> result = new ArrayList<>();
      String bucket = credentials.getBucket();
      String nextMarker = null;

      ensureBucketExists(accountName);

      ListObjectsRequest listObjectsRequest = new ListObjectsRequest();
      listObjectsRequest.setBucketName(bucket);
      listObjectsRequest.setPrefix(prefix);
      listObjectsRequest.setMaxKeys(200);
      listObjectsRequest.setMarker(nextMarker);

      ObjectListing objectListing = ossClient.listObjects(listObjectsRequest);
      List<OSSObjectSummary> summaries = objectListing.getObjectSummaries();
      while (objectListing.isTruncated()) {
        nextMarker = objectListing.getNextMarker();
        listObjectsRequest.setMarker(nextMarker);
        summaries.addAll(ossClient.listObjects(listObjectsRequest).getObjectSummaries());
      }

      if (summaries != null) {
        for (OSSObjectSummary summary : summaries) {
          String itemName = summary.getKey();
          int indexOfLastSlash = itemName.lastIndexOf("/");
          Map<String, Object> objectMetadataMap = new HashMap<>();
          long updatedTimestamp = summary.getLastModified().getTime();
          if (skipToOffset >= indexOfLastSlash) {
            continue;
          }
          objectMetadataMap.put("id", itemName.substring(skipToOffset, indexOfLastSlash));
          objectMetadataMap.put("updatedTimestamp", updatedTimestamp);
          objectMetadataMap.put(
              "updatedTimestampIso", Instant.ofEpochMilli(updatedTimestamp).toString());

          if (objectType == ObjectType.CANARY_CONFIG) {
            String name = itemName.substring(indexOfLastSlash + 1);

            if (name.endsWith(".json")) {
              name = name.substring(0, name.length() - 5);
            }

            objectMetadataMap.put("name", name);
          }

          result.add(objectMetadataMap);
        }
      }
      return result;
    }
  }

  private String buildTypedFolder(AliCloudNamedAccountCredentials credentials, String type) {
    return daoRoot(credentials, type).replaceAll("//", "/");
  }
}
