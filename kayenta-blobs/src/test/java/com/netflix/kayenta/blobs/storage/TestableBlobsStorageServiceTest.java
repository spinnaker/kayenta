package com.netflix.kayenta.blobs.storage;

import com.netflix.spinnaker.kork.web.exceptions.NotFoundException;
import org.junit.Test;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.kayenta.azure.security.AzureCredentials;
import com.netflix.kayenta.azure.security.AzureNamedAccountCredentials;
import com.netflix.kayenta.canary.CanaryConfig;
import com.netflix.kayenta.index.CanaryConfigIndex;
import com.netflix.kayenta.security.AccountCredentials;
import com.netflix.kayenta.security.AccountCredentialsRepository;
import com.netflix.kayenta.security.MapBackedAccountCredentialsRepository;
import com.netflix.kayenta.storage.ObjectType;
import com.tngtech.java.junit.dataprovider.*;

import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import java.util.*;
import org.junit.runner.*;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.springframework.beans.factory.annotation.Autowired;
import static org.mockito.Mockito.*;

@RunWith(DataProviderRunner.class)
@Slf4j
public class TestableBlobsStorageServiceTest {

    private TestableBlobsStorageService testBlobsStorageService;
    private String kayenataAccountName = "Kayenta_Account_1";
    private String azureAccountName = "AzDev_Testing_Account_1";
    private AccountCredentials accountCredentials;
    private String accountAccessKey = "testAccessKey";
    private String rootFolder = "testRootFolder";
    private String testItemKey = "some(GUID)";
    private List<String> testAccountNames ;
    private AccountCredentialsRepository credentialsRepository;

    @Autowired
    private ObjectMapper kayentaObjectMapper;

    @Autowired
    AccountCredentialsRepository accountCredentialsRepository;

    @Autowired
    CanaryConfigIndex canaryConfigIndex;

    @Before
    public void setUp() throws Exception {
        this.testAccountNames = Arrays.asList("AzDev_Testing_Account_1","AzDev_Testing_Account_2");
        AzureNamedAccountCredentials.AzureNamedAccountCredentialsBuilder credentialsBuilder = AzureNamedAccountCredentials.builder();
        credentialsBuilder.name(kayenataAccountName);
        credentialsBuilder.credentials(new AzureCredentials(azureAccountName,accountAccessKey,"core.windows.net"));
        credentialsBuilder.rootFolder(rootFolder);
        credentialsBuilder.azureContainer(null);
        accountCredentials = credentialsBuilder.build();

        credentialsRepository = new MapBackedAccountCredentialsRepository();
        credentialsRepository.save(kayenataAccountName, accountCredentials);

        this.kayentaObjectMapper = new ObjectMapper();
        this.canaryConfigIndex = mock(CanaryConfigIndex.class);

        this.testBlobsStorageService = new TestableBlobsStorageService(kayentaObjectMapper,testAccountNames,credentialsRepository,canaryConfigIndex);
    }



    @After
    public void tearDown() throws Exception {}

   @DataProvider
    public static Object[][] servicesAccountDataset() {
        return new Object[][] {
                {"AzDev_Testing_Account_1",true}, {"AzDev_Testing_Account_2",true},{"AzDev_Testing_Account_3",false}, {"AzDev_Testing_Account_4",false}
        };
    }

    @DataProvider
    public static Object[][] loadObjectDataset() {
        return new Object[][] {
                {"Kayenta_Account_1",ObjectType.CANARY_CONFIG,"some(GUID)",Arrays.asList("app1"),"0"},
                {"Kayenta_Account_2",ObjectType.CANARY_CONFIG,"some(GUID",Arrays.asList("app2"),"0"},
                {"Kayenta_Account_1",ObjectType.METRIC_SET_LIST,"some(GUID)",Arrays.asList("app3"),"0"},
                {"Kayenta_Account_1",ObjectType.METRIC_SET_PAIR_LIST,"some(GUID)",Arrays.asList("app4"),"0"},
                {"Kayenta_Account_1",ObjectType.CANARY_CONFIG,"fake(GUID)",Arrays.asList("app5"),"1"},
                {"Kayenta_Account_1",ObjectType.CANARY_CONFIG,"some(GUID)",Arrays.asList("app6"),"2"}
        };
    }

    @Test
    @UseDataProvider("servicesAccountDataset")
    public void servicesAccount(String accountName, boolean expected) {
        log.info(String.format("Running servicesAccountTest(%s)",accountName));
        Assert.assertEquals(expected,testBlobsStorageService.servicesAccount(accountName));
    }

    @Test
    @UseDataProvider("loadObjectDataset")
    public void loadObject(String accountName, ObjectType objectType, String testItemKey, List<String> applications, String exceptionKey) {
        AccountCredentialsRepository mockCredentialsRepository = PowerMockito.mock(AccountCredentialsRepository.class);
            doReturn(credentialsRepository.getOne(accountName)).when(mockCredentialsRepository).getOne(anyString());
       try {
           log.info(String.format("Running loadObjectTest with accountName(%s) and itemKey(%s) for application(%s)",accountName,testItemKey,applications));
           testBlobsStorageService.blobStored.put("exceptionKey",exceptionKey);
           testBlobsStorageService.blobStored.put("application",applications.get(0));

           CanaryConfig result = testBlobsStorageService.loadObject(accountName, objectType, testItemKey);
           Assert.assertEquals(applications.get(0), result.getApplications().get(0));
           }
       catch (IllegalArgumentException e)
          {
           Assert.assertEquals("Unable to resolve account "+accountName+".",e.getMessage());
          }
       catch (NotFoundException e)
       {
           Assert.assertEquals("Could not fetch items from Azure Cloud Storage: Item not found at "+rootFolder+"/canary_config/"+testItemKey,e.getMessage());
       }
       catch (IllegalStateException e)
       {
           Assert.assertEquals("Unable to deserialize object (key: " + testItemKey + ")",e.getMessage());
       }
    }
    @DataProvider
    public static Object[][] storeObjectDataset() {
        return new Object[][] {
                {"Kayenta_Account_1",ObjectType.CANARY_CONFIG,"Test_Canary","Test_App",false},
                {"Kayenta_Account_2",ObjectType.CANARY_CONFIG,"Test_Canary","Test_App",false},
                {"Kayenta_Account_1",ObjectType.METRIC_SET_LIST,"Test_Canary","Test_App",false},
                {"Kayenta_Account_1",ObjectType.METRIC_SET_PAIR_LIST,"Test_Canary","Test_App",false},
                {"Kayenta_Account_1",ObjectType.CANARY_CONFIG,"Test_Canary","Test_App",true},
        };
    }

    @Test
    @UseDataProvider("storeObjectDataset")
    public void storeObject(String accountName,ObjectType objectType, String canaryConfigName,String application, boolean isAnUpdate) {
        AccountCredentialsRepository mockCredentialsRepository = mock(AccountCredentialsRepository.class);
        doReturn(credentialsRepository.getOne(accountName)).when(mockCredentialsRepository).getOne(anyString());

        when(canaryConfigIndex.getRedisTime()).thenReturn(1163643740L);
        when(canaryConfigIndex.getIdFromName(accountCredentials,canaryConfigName,Arrays.asList(application))).thenReturn(null);

        String fakeFileName = "canary_config.json";
        String fakeBlobName = keytoPath(rootFolder,objectType.getGroup(),testItemKey,fakeFileName);

        CanaryConfig.CanaryConfigBuilder canaryConfigBuilder = CanaryConfig.builder();
        canaryConfigBuilder.name(canaryConfigName).applications(Arrays.asList(application));

        CanaryConfig canaryConfig = canaryConfigBuilder.build();
       try
       {
           log.info(String.format("Running storeObjectTest for (%s)",fakeBlobName));
           testBlobsStorageService.storeObject(accountName,objectType,testItemKey,canaryConfig,fakeFileName,isAnUpdate);
           HashMap<String,String> result = testBlobsStorageService.blobStored;
           Assert.assertEquals(fakeBlobName, result.get("blob"));
       }
       catch (IllegalArgumentException e)
       {
           Assert.assertEquals("Unable to resolve account "+accountName+".",e.getMessage());
       }
    }

    @DataProvider
    public static Object[][] deleteObjectDataset() {
        return new Object[][] {
                {"Kayenta_Account_1",ObjectType.CANARY_CONFIG,"some(GUID)"},
                {"Kayenta_Account_2",ObjectType.CANARY_CONFIG,"some(GUID"},
                {"Kayenta_Account_1",ObjectType.METRIC_SET_LIST,"some(GUID)"},
                {"Kayenta_Account_1",ObjectType.METRIC_SET_PAIR_LIST,"some(GUID)"},
                {"Kayenta_Account_1",ObjectType.METRIC_SET_PAIR_LIST,"fake(GUID)"},

        };
    }

    @Test
    @UseDataProvider("deleteObjectDataset")
    public void deleteObject(String accountName,ObjectType objectType, String testItemKey) {
        AccountCredentialsRepository mockCredentialsRepository = mock(AccountCredentialsRepository.class);
        doReturn(credentialsRepository.getOne(accountName)).when(mockCredentialsRepository).getOne(anyString());

        String fakeBlobName = rootFolder+"/"+objectType.getGroup()+"/"+testItemKey+"/canary_test.json";
        when(canaryConfigIndex.getRedisTime()).thenReturn(1163643740L);

        HashMap<String ,Object> map = new HashMap<String, Object>();
        List<String> applications = new ArrayList<String>();
        applications.add("Test_App");
        map.put("name",new String(fakeBlobName));
        map.put("applications",applications);

        when(canaryConfigIndex.getSummaryFromId(accountCredentials,testItemKey)).thenReturn(map);
        doNothing().when(canaryConfigIndex).finishPendingUpdate(Mockito.any(),Mockito.any(),Mockito.any());

        try
        {
         log.info("Running deleteObjectTest for rootFolder/"+objectType.getGroup()+"/"+testItemKey);
         testBlobsStorageService.deleteObject(accountName,objectType,testItemKey);
         HashMap<String,String> result = testBlobsStorageService.blobStored;
         Assert.assertEquals("invoked",result.get(String.format("deleteIfexists(%s)",fakeBlobName)));
        }
        catch (IllegalArgumentException e)
        {
            Assert.assertEquals("Unable to resolve account "+accountName+".",e.getMessage());
        }
    }

    @DataProvider
    public static Object[][] listObjectKeysDataset() {
        return new Object[][] {
                {"Kayenta_Account_1",ObjectType.CANARY_CONFIG,Arrays.asList("Test_App"),true},
                {"Kayenta_Account_2",ObjectType.CANARY_CONFIG,Arrays.asList("Test_App"),true},
                {"Kayenta_Account_1",ObjectType.METRIC_SET_LIST,Arrays.asList("Test_App"),false},
                {"Kayenta_Account_1",ObjectType.METRIC_SET_PAIR_LIST,Arrays.asList("Test_App"),true}
        };
    }

    @UseDataProvider("listObjectKeysDataset")
    @Test
    public void listObjectKeys(String accountName, ObjectType objectType, List<String> applications, boolean skipIndex) {
        AccountCredentialsRepository mockCredentialsRepository = mock(AccountCredentialsRepository.class);
        doReturn(credentialsRepository.getOne(accountName)).when(mockCredentialsRepository).getOne(anyString());

        try{
            log.info("Running listObjectKeysTest for rootFolder"+"/"+objectType.getGroup()+"/");
            List<Map<String, Object>> result =  testBlobsStorageService.listObjectKeys(accountName,objectType,applications,skipIndex);
            if(objectType==ObjectType.CANARY_CONFIG)
                Assert.assertEquals("canary_test",result.get(0).get("name"));
            else
                Assert.assertEquals(6,result.size());
        }
        catch (IllegalArgumentException e)
        {
            Assert.assertEquals("Unable to resolve account "+accountName+".",e.getMessage());
        }
    }

    private String keytoPath(String rootFolder, String daoTypeName, String objectKey, String filename) {
        return rootFolder + '/' + daoTypeName + '/' + objectKey + '/' + filename;
    }
}