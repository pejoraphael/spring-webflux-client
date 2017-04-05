package com.reactiveclient.client;

import com.reactiveclient.ErrorDecoder;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpServerErrorException;

public class HttpServerErrorDecoder implements ErrorDecoder<HttpServerErrorException> {
    @Override
    public boolean canDecode(HttpStatus httpStatus) {
        return httpStatus.is5xxServerError();
    }

    @Override
    public HttpServerErrorException decode(HttpStatus httpStatus, DataBuffer inputMessage) {
        return new HttpServerErrorException(httpStatus, DataBuffers.readToString(inputMessage));
    }
}
