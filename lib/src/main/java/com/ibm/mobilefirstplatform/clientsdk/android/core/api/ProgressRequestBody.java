package com.ibm.mobilefirstplatform.clientsdk.android.core.api;


import com.ibm.mobilefirstplatform.clientsdk.android.logger.api.Logger;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.internal.Util;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import okio.BufferedSink;
import okio.Okio;
import okio.Source;

public class ProgressRequestBody extends RequestBody {

    // The size of buffer segments used by Okio (2 KiB)
    protected final int SEGMENT_SIZE = 2048;

    private Object payload;
    private ProgressListener listener;
    private RequestBody requestBody;
    private String requestURL;

    private static Logger logger = Logger.getLogger(Logger.INTERNAL_PREFIX + ProgressRequestBody.class.getSimpleName());

    public ProgressRequestBody(Object payload, RequestBody requestBody, String requestURL, ProgressListener listener) {
        this.payload = payload;
        this.listener = listener;
        this.requestBody = requestBody;
        this.requestURL = requestURL;
    }

    @Override
    public long contentLength() {
        try {
            return requestBody.contentLength();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return -1;
    }

    @Override
    public MediaType contentType() {
        return requestBody.contentType();
    }

    @Override
    public void writeTo(BufferedSink sink) throws IOException {
        Source source = getSourceFromPayload(payload);
        if (source == null) {
            logger.error("Cannot upload this");
        }

        try {
            long bytesRead = 0;
            long segment;

            while ((segment = source.read(sink.buffer(), SEGMENT_SIZE)) != -1) {
                bytesRead += segment;
                sink.flush();
                this.listener.onProgress(bytesRead, contentLength(), requestURL);
            }
        } finally {
            Util.closeQuietly(source);
        }
    }

    protected Source getSourceFromPayload(Object payload) throws IOException {
        if (payload instanceof String) {
            InputStream stringStream = new ByteArrayInputStream(((String) payload).getBytes("UTF-8"));
            return Okio.source(stringStream);
        }
        else if (payload instanceof File) {
            return Okio.source((File)payload);
        }
        else if (payload instanceof InputStream) {
            return Okio.source((InputStream)payload);
        }
        else if (payload instanceof byte[]) {
            ByteArrayInputStream byteStream = new ByteArrayInputStream((byte[])payload);
            return Okio.source(byteStream);
        }
        else {
            return null;
        }
    }
}