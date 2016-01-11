This library provides support to make HTTP or HTTPS requests to RESTful services.

Use the following for Maven/Gradle Dependencies:

<dependency>
  <groupId>com.nnarendravijay</groupId>
  <artifactId>secureRestClient</artifactId>
  <version>0.0.1</version>
</dependency>

The SSL variant accepts certificates in the JKS format. You can provide multiple certificates as part of one keystore and simplify cert management. The client also by default uses SSL reuse and reduces the overhead of establishing TCP and SSL session for multiple requests and hence is faster.

To get a client with SSL capabilities, use the following constructor:

	RestClient(String keyStore, String keyStorePassword, String keyStoreAlias, String tlsVersion);

To get a client with just HTTP and NO SSL capabilities, use the no-arg constructor:

	RestClient();

Unit tests are part of the project and you can refer to the tests for sample usage of the library. It internally uses Jersey and JSSE and hides all the details of acquiring an SSL or non-SSL connection to a server.

To send a HTTP Post request, invoke the following method:

Response sendPostRequest(Object object, URI uri, MediaType mediaType); 
Where 	1. Object is any java object that needs to be sent across as the HTTP Body. 
		2. URI is the URL to which the request needs to be sent to.
		3. MediaType is the application mediaType; for example, MediaType.APPLICATION_XML_TYPE or APPLICATION_JSON_TYPE or MULTIPART_FORM_DATA or TEXT_PLAIN.
The library internally takes care of marshalling and unmarshalling when the mediaType is APPLICATION_XML(provided the schema has been provided). Also, internally takes care of serialization and de-serialization when the mediaType is APPLICATION_JSON. 

Example: client.sendPostRequest(account, uri, MediaType.APPLICATION_XML_TYPE);

To send a HTTP Get request, invoke the following method:

Response sendGetRequest(URI uri);

To send a HTTP Put request, invoke the following method:

Response sendPutRequest(Object object, URI uri);

To send a HTTP Delete request, invoke the following method:

Response sendDeleteRequest(URI uri);

The library supports adding different HTTP headers. To add a header, use:

void addHeader(String header, String value);

To remove a header, use 

void removeHeader(String header);

When the client needs "Accept-Encoding" of GZip, the library internally takes care of reading a GZipped response when the "Content-Encoding" is set to GZIP and provides the unzipped response to the caller.
