package com.nnarendravijay;

import com.google.common.base.Preconditions;
import com.google.common.io.Resources;
import org.apache.commons.lang3.StringUtils;
import org.glassfish.jersey.SslConfigurator;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.ext.ReaderInterceptor;
import javax.ws.rs.ext.ReaderInterceptorContext;
import javax.ws.rs.ext.WriterInterceptor;
import javax.ws.rs.ext.WriterInterceptorContext;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.security.Key;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.junit.Assert.assertEquals;

@SuppressWarnings({"PMD.CyclomaticComplexity", "PMD.ModifiedCyclomaticComplexity", "PMD.StdCyclomaticComplexity"})
public class RestClient {
  private static final Logger LOGGER = LoggerFactory.getLogger(RestClient.class);

  private final Client client;
  private final MultivaluedMap<String, Object> multivaluedMap = new MultivaluedHashMap<>();

  private Response response;

  public RestClient() {
    client = createClientBuilder().build();
  }

  public RestClient(String keyStore, String keyStorePasswd, String keyStoreAlias, String tlsVersion) {

    Preconditions.checkState(StringUtils.isNotBlank(keyStore), "Keystore cannot be null or blank");
    Preconditions.checkState(StringUtils.isNotBlank(keyStoreAlias), "keyStoreAlias cannot be null or blank");
    Preconditions.checkState(StringUtils.isNotBlank(tlsVersion), "tlsVersion cannot be null or blank");
    Preconditions.checkNotNull(keyStorePasswd, "KeystorePasswd cannot be null");
    Preconditions.checkNotNull(Resources.getResource(keyStore), "keyStore file does NOT exist");

    client = createSSLClient(keyStore, keyStorePasswd, keyStoreAlias, tlsVersion);
  }

  public MultivaluedMap<String, Object> getHeaders() {
    return multivaluedMap;
  }

  public Response sendPostRequest(Object object, URI uri, MediaType mediaType) {
    response = client.target(uri).request().headers(multivaluedMap)
        .post(Entity.entity(object, mediaType));
    response.bufferEntity();
    return response;
  }

  public Response sendGetRequestWithEncoding(URI uri, MediaType mediaType, final String... encodings) {
    response = client.target(uri).request(mediaType).headers(multivaluedMap)
        .acceptEncoding("deflate", "gzip").get();
    response.bufferEntity();
    return response;
  }

  public Response sendGetRequest(URI uri, MediaType mediaType) {
    response = client.target(uri).request(mediaType).headers(multivaluedMap).get();
    response.bufferEntity();
    return response;
  }

  public Response sendPutRequest(Object object, URI uri, MediaType mediaType) {
    response = client.target(uri).request().headers(multivaluedMap)
        .put(Entity.entity(object, mediaType));
    response.bufferEntity();
    return response;
  }

  public Response sendDeleteRequest(URI uri) {
    response = client.target(uri).request().headers(multivaluedMap).delete();
    response.bufferEntity();
    return response;
  }

  public void updateClientProperty(String property, boolean enabled) {
    client.property(property, enabled);
  }

  @SuppressWarnings("PMD.CyclomaticComplexity")
  private Client createSSLClient(String keyStoreFile, String keyStorePasswd, String httpsAlias, String tlsVersion) {
    try {
      URL keyStoreFileURL = Resources.getResource(keyStoreFile);
      SslConfigurator sslConfig = SslConfigurator.newInstance().keyStoreFile(keyStoreFileURL.getFile())
          .keyPassword(keyStorePasswd);

      TrustManager[] trustAllCerts = new TrustManager[] {
          new X509TrustManager() {
        public X509Certificate[] getAcceptedIssuers() {
          return new X509Certificate[0];
        }

        public void checkClientTrusted(X509Certificate[] certs, String authType) {
          //Empty
        }

        public void checkServerTrusted(X509Certificate[] certs, String authType) {
          //Empty
        }
      }
      };

      KeyManagerFactory keyManagerFactory;
      KeyStore keyStore, jks;

      keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
      try (InputStream fin = keyStoreFileURL.openStream();) {
        keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        jks = KeyStore.getInstance(KeyStore.getDefaultType());
        keyStore.load(fin, keyStorePasswd.toCharArray());
        jks.load(null);

        if (keyStore.containsAlias(httpsAlias)) {
          if (keyStore.isKeyEntry(httpsAlias)) {
            Certificate[] certChain = keyStore.getCertificateChain(httpsAlias);
            Key key = keyStore.getKey(httpsAlias, keyStorePasswd.toCharArray());
            for (Certificate cert : certChain) {
              jks.setCertificateEntry(httpsAlias, cert);
              jks.setKeyEntry(httpsAlias, key, keyStorePasswd.toCharArray(), certChain);
            }
          } else if (keyStore.isCertificateEntry(httpsAlias)) {
            Certificate[] certChain = keyStore.getCertificateChain(httpsAlias);
            Key key = keyStore.getKey(httpsAlias, keyStorePasswd.toCharArray());
            for (Certificate cert : certChain) {
              jks.setCertificateEntry(httpsAlias, cert);
              jks.setKeyEntry(httpsAlias, key, keyStorePasswd.toCharArray(), certChain);
            }
          }

        }

        keyManagerFactory.init(jks, keyStorePasswd.toCharArray());

        SSLContext sslContext = sslConfig.createSSLContext().getInstance(tlsVersion);
        sslContext.init(keyManagerFactory.getKeyManagers(), trustAllCerts, new SecureRandom());

        return createClientBuilder().sslContext(sslContext).hostnameVerifier((hostname, session) -> true).build();
      }

    } catch (NoSuchAlgorithmException | KeyStoreException | IOException | CertificateException |
        UnrecoverableKeyException | KeyManagementException e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }
  }

  public ClientBuilder createClientBuilder() {
    return ClientBuilder.newBuilder().register(MultiPartFeature.class)
        .register(new GZIPReaderInterceptor()); //.register(new DecodingWriterInterceptor());
  }

  public static class DecodingWriterInterceptor implements WriterInterceptor {
    @Override
    public void aroundWriteTo(WriterInterceptorContext context) throws IOException, WebApplicationException {

      LOGGER.debug("In the writer interceptor");
    }
  }

  public static class GZIPReaderInterceptor implements ReaderInterceptor {

    @Override
    public Object aroundReadFrom(ReaderInterceptorContext context) throws IOException, WebApplicationException {

      MultivaluedMap<String, String> headers = context.getHeaders();
      if (headers.containsKey(HttpHeaders.CONTENT_ENCODING) &&
          headers.getFirst(HttpHeaders.CONTENT_ENCODING).equalsIgnoreCase("GZip")) {
          LOGGER.debug("Content-Encoding is set to GZip in the response, unzipping first");
          final InputStream originalInputStream = context.getInputStream();
          context.setInputStream(new GZIPInputStream(originalInputStream));
      }
      return context.proceed();
    }
  }

  @SuppressWarnings("PMD.AvoidReassigningParameters")
  public URI buildUri(String baseUri, String pathTemplate, Map<String, String> queryParams, Object... pathParams) {

    if (!baseUri.endsWith("/") && !pathTemplate.startsWith("/")) {
      baseUri += "/";
    }
    UriBuilder builder = UriBuilder.fromUri(baseUri + pathTemplate);

    if (queryParams != null && !queryParams.isEmpty()) {
      queryParams.entrySet().stream().forEach(e -> {
        builder.queryParam(e.getKey(), e.getValue());
      });
    }

    return builder.build(pathParams, true);
  }

  public void addHeader(String header, String value) {
    multivaluedMap.putSingle(header, value);
  }

  public void putIfAbsent(String header, List<Object> value) {
    multivaluedMap.putIfAbsent(header, value);
  }

  public void addHeaders(Map<String, String> headers) {
    for (Map.Entry<String, String> entry : headers.entrySet()) {
      multivaluedMap.putSingle(entry.getKey(), entry.getValue());
    }
  }

  public void removeHeader(String header) {
    multivaluedMap.remove(header);
  }

  public void removeEtagRelatedHeaders() {
    multivaluedMap.remove(HttpHeaders.IF_MATCH);
    multivaluedMap.remove(HttpHeaders.IF_NONE_MATCH);
    multivaluedMap.remove(HttpHeaders.IF_MODIFIED_SINCE);
  }

  public void removeAuthHeader() {
    multivaluedMap.remove(HttpHeaders.AUTHORIZATION);
  }

  public void addAuthHeader(String assertion) {
    multivaluedMap.putSingle(HttpHeaders.AUTHORIZATION, "SAML2 assertion=" + assertion);
  }

  private void clearAllHeaders() {
    if (multivaluedMap != null) {
      multivaluedMap.clear();
    }
  }

  public Response getResponse() {
    checkNotNull(response);
    return response;
  }

  public boolean validateHttpResponseCode(int status) {
    checkNotNull(response);
    assertEquals(status, response.getStatus());
    return status == response.getStatus();
  }

}
