package com.nnarendravijay;

import com.nnarendravijay.securerestclienttest.ResourceA;
import com.nnarendravijay.securerestclienttest.TestUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.Resources;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockserver.client.server.MockServerClient;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.Header;
import org.mockserver.model.Parameter;
import org.mockserver.model.ParameterBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.Form;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;
import static org.junit.Assert.assertEquals;

public class SecureRestClientTest {

  private static final Logger LOGGER = LoggerFactory.getLogger(SecureRestClientTest.class);

  private static RestClient client;
  private static RestClient secureClient;
  private static ClientAndServer mockServer;
  private static ResourceA resourceA;
  private static String json;

  @BeforeClass
  public static void setup() throws JsonProcessingException {

    resourceA = new ResourceA();
    resourceA.setParam1("value1");
    resourceA.setParam2("value2");
    ObjectMapper mapper = new ObjectMapper();
    json = mapper.writeValueAsString(resourceA);

    secureClient = new RestClient("keyStore.jks", "testing", "sampleAlias", "TLSv1.2");
    client = new RestClient();

    mockServer = startClientAndServer(1080);
  }

  private static void stubMockServerBehavior(String method, String path, String reqBody,
      Map<String, String> requestHeaders, Map<String, List<String>> queryParams,
      int responseCode, String respBody, Map<String, String> responseHeaders) throws JsonProcessingException {

    List<Header> rqstHeaders = TestUtils.convertHeaders(requestHeaders);
    List<Header> rspnseHeaders = TestUtils.convertHeaders(responseHeaders);
    if (queryParams == null) {
      queryParams = new HashMap<>();
    }

    new MockServerClient("localhost", 1080)
        .when(request()
            .withMethod(method)
            .withPath(path)
            .withQueryStringParameters(queryParams)
            .withHeaders(rqstHeaders)
            .withBody(reqBody))
        .respond(response()
                .withStatusCode(responseCode)
                .withHeaders(rspnseHeaders)
                .withBody(respBody)
        );

  }

  @AfterClass
  public static void tearDown() {
    mockServer.stop();
  }

  @Test
  public void testPost() throws InterruptedException, JsonProcessingException {

    stubMockServerBehavior("POST", "/ResourceA", json, null, null, 201, null,
        ImmutableMap.of("Location", "https://www.mock-server.com"));

    client = new RestClient();
    Response response = client.sendPostRequest(resourceA, URI.create("http://localhost:1080/ResourceA"), MediaType.APPLICATION_JSON_TYPE);
    assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
  }

  @Test
  public void testSecurePost() throws InterruptedException, JsonProcessingException {

    stubMockServerBehavior("POST", "/ResourceA", json, null, null, 201, null,
        ImmutableMap.of("Location", "https://www.mock-server.com"));

    Response response = secureClient.sendPostRequest(resourceA, URI.create("https://localhost:1080/ResourceA"), MediaType.APPLICATION_JSON_TYPE);
    assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
  }

  @Test
  public void testFormPost() throws InterruptedException {

    Parameter parameter = new Parameter("Parameter1", "Value1");
    new MockServerClient("localhost", 1080)
        .when(request()
            .withMethod("POST")
            .withPath("/ResourceA")
            .withBody(new ParameterBody(parameter)))
        .respond(response()
                .withStatusCode(201)
                .withHeader("Location", "https://www.mock-server.com")
        );

    client = new RestClient();
    Response response = client.sendPostRequest(new Form("Parameter1", "Value1"), URI.create("http://localhost:1080/ResourceA"),
        MediaType.APPLICATION_FORM_URLENCODED_TYPE);
    assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
  }

  @Test
  public void testFormDataMultiPartPost() throws InterruptedException {

    new MockServerClient("localhost", 1080)
        .when(request()
            .withMethod("POST")
            .withPath("/MultiFileUpload"))
        .respond(response()
                .withStatusCode(201)
                .withHeader("Location", "https://www.mock-server.com")
        );


    File file = new File(Resources.getResource("keyStore.jks").getFile());
    FormDataMultiPart multiPart = new FormDataMultiPart();
    multiPart.bodyPart(new FormDataBodyPart("file1", file, MediaType.APPLICATION_OCTET_STREAM_TYPE));
    multiPart.bodyPart(new FormDataBodyPart("file2", file, MediaType.APPLICATION_OCTET_STREAM_TYPE));

    client = new RestClient();
    Response response = client.sendPostRequest(multiPart, URI.create("http://localhost:1080/MultiFileUpload"), MediaType.MULTIPART_FORM_DATA_TYPE);
    assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
  }

  @Test
  public void testGet() throws InterruptedException, IOException {

    stubMockServerBehavior("GET", "/ResourceA/1", null, null, null, 200, json,
        ImmutableMap.of("Content-Type", "application/json"));

    Response response = client.sendGetRequest(URI.create("http://localhost:1080/ResourceA/1"), MediaType.APPLICATION_JSON_TYPE);
    assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    ResourceA resourceA = response.readEntity(ResourceA.class);
    assertEquals(this.resourceA.getParam1(), resourceA.getParam1());
    assertEquals(this.resourceA.getParam2(), resourceA.getParam2());
  }

  @Test
  public void testSecureGet() throws InterruptedException, JsonProcessingException {

    stubMockServerBehavior("GET", "/ResourceA/1", null, null, null, 200, json,
        ImmutableMap.of("Content-Type", "application/json"));

    Response response = secureClient.sendGetRequest(URI.create("https://localhost:1080/ResourceA/1"), MediaType.APPLICATION_JSON_TYPE);
    assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    ResourceA resourceA = response.readEntity(ResourceA.class);
    assertEquals(this.resourceA.getParam1(), resourceA.getParam1());
    assertEquals(this.resourceA.getParam2(), resourceA.getParam2());
  }

  @Test
  public void testGetWithEncoding() throws InterruptedException, IOException {

    new MockServerClient("localhost", 1080)
        .when(request()
            .withMethod("GET")
            .withPath("/ResourceA/Encoding/1")
            .withHeader("Accept-Encoding", "deflate,gzip"))
        .respond(response()
                .withStatusCode(200)
                .withBody(TestUtils.compress(json))
                .withHeader("Content-Type", "application/json")
                .withHeader("Content-Encoding", "gzip")
        );

    Response response = client.sendGetRequestWithEncoding(URI.create("http://localhost:1080/ResourceA/Encoding/1"),
        MediaType.APPLICATION_JSON_TYPE);
    assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    ResourceA resourceA = response.readEntity(ResourceA.class);
    assertEquals(this.resourceA.getParam1(), resourceA.getParam1());
    assertEquals(this.resourceA.getParam2(), resourceA.getParam2());
  }

  @Test
  public void testGetWithQueryParams() throws InterruptedException, IOException {

    Map<String, List<String>> queryParams = new HashMap<>();
    queryParams.put("queryParam1", Arrays.asList("value1"));
    stubMockServerBehavior("GET", "/ResourceA/QueryParams/1", null, null, queryParams, 200, json,
        ImmutableMap.of("Content-Type", "application/json"));

    Response response = client.sendGetRequest(
        URI.create("http://localhost:1080/ResourceA/QueryParams/1?queryParam1=value1"),
        MediaType.APPLICATION_JSON_TYPE);
    assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    ResourceA resourceA = response.readEntity(ResourceA.class);
    assertEquals(this.resourceA.getParam1(), resourceA.getParam1());
    assertEquals(this.resourceA.getParam2(), resourceA.getParam2());
  }

  @Test
  public void testPut() throws InterruptedException, JsonProcessingException {

    stubMockServerBehavior("PUT", "/ResourceA/1", json, null, null, 200, json,
        ImmutableMap.of("Content-Type", "application/json"));

    Response response = client.sendPutRequest(resourceA, URI.create("http://localhost:1080/ResourceA/1"),
        MediaType.APPLICATION_JSON_TYPE);
    assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    ResourceA resourceA = response.readEntity(ResourceA.class);
    assertEquals(this.resourceA.getParam1(), resourceA.getParam1());
    assertEquals(this.resourceA.getParam2(), resourceA.getParam2());
  }

  @Test
  public void testSecurePut() throws InterruptedException, JsonProcessingException {

    stubMockServerBehavior("PUT", "/ResourceA/1", json, null, null, 200, json,
        ImmutableMap.of("Content-Type", "application/json"));

    Response response = secureClient.sendPutRequest(resourceA, URI.create("https://localhost:1080/ResourceA/1"),
        MediaType.APPLICATION_JSON_TYPE);
    assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    ResourceA resourceA = response.readEntity(ResourceA.class);
    assertEquals(this.resourceA.getParam1(), resourceA.getParam1());
    assertEquals(this.resourceA.getParam2(), resourceA.getParam2());
  }

  @Test
  public void testDelete() throws InterruptedException, IOException {

    stubMockServerBehavior("DELETE", "/ResourceA/1", null, null, null, 204, null, null);

    Response response = client.sendDeleteRequest(URI.create("http://localhost:1080/ResourceA/1"));
    assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());
  }

  @Test
  public void testSecureDelete() throws InterruptedException, JsonProcessingException {

    stubMockServerBehavior("DELETE", "/ResourceA/1", null, null, null, 204, null, null);

    Response response = secureClient.sendDeleteRequest(URI.create("https://localhost:1080/ResourceA/1"));
    assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());
  }

  @Test
  public void testHeaderManagement() throws JsonProcessingException {

    stubMockServerBehavior("GET", "/ResourceA/2", null, ImmutableMap.of("Authorization", "Yes"), null, 200, null, null);

    client.addHeader("Authorization", "Yes");
    Response response = client.sendGetRequest(URI.create("http://localhost:1080/ResourceA/2"),
        MediaType.APPLICATION_JSON_TYPE);
    assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

    client.removeHeader("Authorization");
    response = client.sendGetRequest(URI.create("http://localhost:1080/ResourceA/2"), MediaType.APPLICATION_JSON_TYPE);
    assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());

    client.addHeaders(ImmutableMap.of("Header1", "Value1", "Header2", "Value2"));
    assertEquals(2, client.getHeaders().size());
  }

  @Test
  public void testBuildUri() throws URISyntaxException {
    Map<String, String> queryParams = ImmutableMap.of(
        "p1", "v1",
        "p2", "v2"
    );
    URI uri = client.buildUri("https://localhost:443/base1", "base2/base3", queryParams);
    assertEquals("https://localhost:443/base1/base2/base3?p1=v1&p2=v2", uri.toString());
  }

  @Test
  public void testBuildUriFromTemplate() throws URISyntaxException {
    Map<String, String> queryParams = ImmutableMap.of(
        "p1", "v1",
        "p2", "v2"
    );
    URI uri = client.buildUri("https://localhost:443/base1", "account/{acId}/user/{uId}?222", queryParams, "123", "456");
    assertEquals("https://localhost:443/base1/account/123/user/456?222&p1=v1&p2=v2", uri.toString());
  }

}
