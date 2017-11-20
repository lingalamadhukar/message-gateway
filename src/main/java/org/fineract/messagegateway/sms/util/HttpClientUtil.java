/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.fineract.messagegateway.sms.util;

import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContextBuilder;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.fineract.messagegateway.sms.constants.SmsConstants;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

@SuppressWarnings("deprecation")
public class HttpClientUtil {

    private static final Map<String, String> authenticationMap = new HashMap<>();

    public static ResponseEntity<String> sendInboundSMSRequest(final String tenantIdentifier, final String baseURL,
            final String authenticationURI, final String smsURI, final String payload, final String username, final String password) {
        final HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Fineract-Platform-TenantId", tenantIdentifier);
        headers.set("Authorization", "Basic " + getAuthenticationKey(tenantIdentifier, baseURL + authenticationURI, username, password));
        SSLConnectionSocketFactory socketFactory = null;
        CloseableHttpClient httpClient = null;
        try {
            socketFactory = new SSLConnectionSocketFactory(
                    new SSLContextBuilder().loadTrustMaterial(null, new TrustSelfSignedStrategy()).build(), NoopHostnameVerifier.INSTANCE);

            httpClient = HttpClients.custom().setSSLSocketFactory(socketFactory).build();
            final HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory();
            requestFactory.setHttpClient(httpClient);
            final HttpEntity<String> entity = new HttpEntity<>(payload, headers);
            final ResponseEntity<String> response = new RestTemplate(requestFactory).exchange(baseURL + smsURI, HttpMethod.POST, entity,
                    String.class);
            return response;
        } catch (final Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    public static String getAuthenticationKey(final String tenantIdentifier, final String authenticationURL, final String username,
            final String password) {
        if (!authenticationMap.containsKey(tenantIdentifier)) {
            authenticationMap.put(tenantIdentifier, loginAndGetAuthenticatedKey(tenantIdentifier, authenticationURL, username, password));
        }
        return authenticationMap.get(tenantIdentifier);
    }

    public static String loginAndGetAuthenticatedKey(final String tenantIdentifier, final String authenticationURL, final String username,
            final String password) {

        final HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Fineract-Platform-TenantId", tenantIdentifier);

        SSLConnectionSocketFactory socketFactory = null;
        CloseableHttpClient httpClient = null;
        String base64EncodedAuthenticationKey = null;
        try {
            socketFactory = new SSLConnectionSocketFactory(
                    new SSLContextBuilder().loadTrustMaterial(null, new TrustSelfSignedStrategy()).build(), NoopHostnameVerifier.INSTANCE);

            httpClient = HttpClients.custom().setSSLSocketFactory(socketFactory).build();
            final HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory();
            requestFactory.setHttpClient(httpClient);
            final JsonObject payload = new JsonObject();
            payload.addProperty(SmsConstants.USERNAME, username);
            payload.addProperty(SmsConstants.PASSWORD, password);
            final HttpEntity<String> entity = new HttpEntity<>(payload.toString(), headers);
            final ResponseEntity<String> response = new RestTemplate(requestFactory).exchange(authenticationURL, HttpMethod.POST, entity,
                    String.class);
            if (response.getStatusCode().equals(HttpStatus.OK)) {
                final JsonObject responseBody = new JsonParser().parse(response.getBody()).getAsJsonObject();
                base64EncodedAuthenticationKey = responseBody.get("base64EncodedAuthenticationKey").getAsString();
            } else {
                throw new RuntimeException("Exception while authenticating to server with status: " + response.getStatusCode().name());
            }
        } catch (KeyManagementException | NoSuchAlgorithmException | KeyStoreException e2) {
            e2.printStackTrace();
        }

        return base64EncodedAuthenticationKey;
    }
}
