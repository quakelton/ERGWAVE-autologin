package org.dyndns.pawitp.muwifiautologin;

import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.protocol.HttpContext;

import java.io.IOException;
import java.net.ConnectException;
import java.net.UnknownHostException;

import javax.net.ssl.SSLHandshakeException;

// Retry handler allowing the retry of POST requests
public class PostRetryHandler implements HttpRequestRetryHandler {

    static final int RETRY_COUNT = 2;

    @Override
    public boolean retryRequest(IOException exception, int executionCount,
            HttpContext context) {
        if (executionCount >= RETRY_COUNT) {
            // Do not retry if over max retry count
            return false;
        }
        if (exception instanceof UnknownHostException) {
            // Unknown host
            return false;
        }
        if (exception instanceof ConnectException) {
            // Connection refused
            return false;
        }
        if (exception instanceof SSLHandshakeException) {
            // SSL handshake exception
            return false;
        }

        return true;
    }

}
