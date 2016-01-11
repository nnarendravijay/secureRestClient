package com.nnarendravijay.securerestclienttest;

import org.mockserver.model.Header;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

public class TestUtils {

  private static final Logger LOGGER = LoggerFactory.getLogger(TestUtils.class);

  public static byte[] compress(String input) throws IOException {
    byte[] dataToCompress = input.getBytes();

      ByteArrayOutputStream byteStream = new ByteArrayOutputStream(dataToCompress.length);
      try
      {
        GZIPOutputStream zipStream = new GZIPOutputStream(byteStream);
        try
        {
          zipStream.write(dataToCompress);
        }
        finally
        {
          zipStream.close();
        }
      }
      finally
      {
        byteStream.close();
      }

      return byteStream.toByteArray();
  }

  public static List<Header> convertHeaders(Map<String, String> headers) {
    List<Header> resultHeaders = new ArrayList<>();
    if (headers != null) {
      headers.forEach((p, v) -> {
        resultHeaders.add(new Header(p, v));
      });
    }
    return resultHeaders;
  }

}
