package com.reactiveclient.client;

import com.reactiveclient.HttpErrorReader;
import org.springframework.http.codec.HttpMessageReader;
import org.springframework.http.codec.HttpMessageWriter;
import org.springframework.web.reactive.function.client.ExchangeStrategies;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;

class DefaultExtendedExchangeStrategies implements ExtendedExchangeStrategies {

    private List<HttpMessageWriter<?>> httpMessageWriters;
    private List<HttpMessageReader<?>> httpMessageReaders;
    private List<HttpErrorReader> httpExceptionReaders;

    DefaultExtendedExchangeStrategies(List<HttpMessageWriter<?>> httpMessageWriters, List<HttpMessageReader<?>> httpMessageReaders, List<HttpErrorReader> httpExceptionReaders) {
        this.httpMessageWriters = httpMessageWriters;
        this.httpMessageReaders = httpMessageReaders;
        this.httpExceptionReaders = httpExceptionReaders;
    }

    @Override
    public Supplier<Stream<HttpErrorReader>> exceptionReader() {
        return httpExceptionReaders::stream;
    }

    @Override
    public Supplier<Stream<HttpMessageReader<?>>> messageReaders() {
        return httpMessageReaders::stream;
    }

    @Override
    public Supplier<Stream<HttpMessageWriter<?>>> messageWriters() {
        return httpMessageWriters::stream;
    }
}
