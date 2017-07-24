package com.webfluxclient;

import com.webfluxclient.codec.ErrorDecoder;
import com.webfluxclient.codec.ExtendedClientCodecConfigurer;
import com.webfluxclient.codec.HttpClientErrorDecoder;
import com.webfluxclient.codec.HttpErrorReader;
import com.webfluxclient.codec.HttpServerErrorDecoder;
import com.webfluxclient.handler.ReactiveInvocationHandlerFactory;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.HttpStatus;

import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class DefaultClientBuilderTest {

    @Mock
    private ReactiveInvocationHandlerFactory reactiveInvocationHandlerFactory;

    @Captor
    private ArgumentCaptor<ExtendedClientCodecConfigurer> codecConfigurerArgumentCaptor;

    @Captor
    private ArgumentCaptor<List<RequestProcessor>> requestProcessorsArgumentCaptor;

    @Captor
    private ArgumentCaptor<List<ResponseProcessor>> responseProcessorsArgumentCaptor;


    @Test
    public void registerDefaultCodecs_withDefaultCodecsDisable(){
        URI targetUri = URI.create("http://example.ca");

        when(reactiveInvocationHandlerFactory.build(codecConfigurerArgumentCaptor.capture(), anyList(), anyList(), isNull(), isNull(), eq(TestClient.class), same(targetUri))).thenReturn(new MockInvocationHandler());

        createBuilder().registerDefaultCodecs(false)
                .build(TestClient.class, targetUri);

        ExtendedClientCodecConfigurer codecConfigurer = codecConfigurerArgumentCaptor.getValue();
        assertThat(codecConfigurer.getReaders())
                .isEmpty();
        assertThat(codecConfigurer.getWriters())
                .isEmpty();
        assertThat(codecConfigurer.getErrorReaders())
                .isEmpty();

        verify(reactiveInvocationHandlerFactory).build(same(codecConfigurer), anyList(), anyList(), isNull(), isNull(), eq(TestClient.class), same(targetUri));
        verifyNoMoreInteractions(reactiveInvocationHandlerFactory);
    }

    @Test
    public void registerDefaultCodecs_withDefaultCodecsEnable(){
        URI targetUri = URI.create("http://example.ca");

        when(reactiveInvocationHandlerFactory.build(codecConfigurerArgumentCaptor.capture(), anyList(), anyList(), isNull(), isNull(), eq(TestClient.class), same(targetUri))).thenReturn(new MockInvocationHandler());

        createBuilder().build(TestClient.class, targetUri);

        ExtendedClientCodecConfigurer codecConfigurer = codecConfigurerArgumentCaptor.getValue();
        assertThat(codecConfigurer.getReaders())
                .isNotEmpty();
        assertThat(codecConfigurer.getWriters())
                .isNotEmpty();
        assertThat(codecConfigurer.getErrorReaders())
                .hasSize(2);

        verify(reactiveInvocationHandlerFactory).build(same(codecConfigurer), anyList(), anyList(), isNull(), isNull(), eq(TestClient.class), same(targetUri));
        verifyNoMoreInteractions(reactiveInvocationHandlerFactory);
    }

    @Test
    public void registerDefaultCodecs_withCustomErrorDecoder(){
        URI targetUri = URI.create("http://example.ca");

        when(reactiveInvocationHandlerFactory.build(codecConfigurerArgumentCaptor.capture(), anyList(), anyList(), isNull(), isNull(), eq(TestClient.class), same(targetUri))).thenReturn(new MockInvocationHandler());

        createBuilder()
                .registerDefaultCodecs(false)
                .customCodecs(customCodecsConfigurer -> customCodecsConfigurer.errorDecoder(ErrorDecoder.of(HttpStatus.BAD_REQUEST::equals, (httpStatus, dataBuffer) -> new IllegalArgumentException())))
                .build(TestClient.class, targetUri);

        ExtendedClientCodecConfigurer codecConfigurer = codecConfigurerArgumentCaptor.getValue();
        assertThat(codecConfigurer.getErrorReaders())
                .hasSize(1);
        assertThat(findReader(codecConfigurer.getErrorReaders(), HttpStatus.BAD_REQUEST))
                .isNotEmpty();
        verify(reactiveInvocationHandlerFactory).build(same(codecConfigurer), anyList(), anyList(), isNull(), isNull(), eq(TestClient.class), same(targetUri));
        verifyNoMoreInteractions(reactiveInvocationHandlerFactory);
    }

    @Test
    public void registerDefaultCodecs_withDefaultErrorDecoderOverride(){
        URI targetUri = URI.create("http://example.ca");
        OverrideHttpClientErrorDecoder clientErrorDecoder = new OverrideHttpClientErrorDecoder();
        OverrideHttpServerErrorDecoder serverErrorDecoder = new OverrideHttpServerErrorDecoder();

        when(reactiveInvocationHandlerFactory.build(codecConfigurerArgumentCaptor.capture(), anyList(), anyList(), isNull(), isNull(), eq(TestClient.class), same(targetUri))).thenReturn(new MockInvocationHandler());

        createBuilder()
                .defaultCodecs(defaultCodecsConfigurerConsumer -> {
                    defaultCodecsConfigurerConsumer.httpClientErrorDecoder(clientErrorDecoder);
                    defaultCodecsConfigurerConsumer.httpServerErrorDecoder(serverErrorDecoder);
                })
                .build(TestClient.class, targetUri);

        ExtendedClientCodecConfigurer codecConfigurer = codecConfigurerArgumentCaptor.getValue();
        assertThat(codecConfigurer.getErrorReaders())
                .hasSize(2)
                .extracting("errorDecoder")
                .containsExactlyInAnyOrder(clientErrorDecoder, serverErrorDecoder);

        verify(reactiveInvocationHandlerFactory).build(same(codecConfigurer), anyList(), anyList(), isNull(), isNull(), eq(TestClient.class), same(targetUri));
        verifyNoMoreInteractions(reactiveInvocationHandlerFactory);
    }

    @Test
    public void registerRequestInterceptor(){
        URI targetUri = URI.create("http://example.ca");
        RequestProcessor requestProcessor = clientRequest -> {System.out.println(clientRequest); return clientRequest;};
        when(reactiveInvocationHandlerFactory.build(any(ExtendedClientCodecConfigurer.class), requestProcessorsArgumentCaptor.capture(),  anyList(), isNull(), isNull(), eq(TestClient.class), same(targetUri))).thenReturn(new MockInvocationHandler());

        createBuilder()
                .requestProcessor(requestProcessor)
                .build(TestClient.class, targetUri);

        List<RequestProcessor> requestProcessors = requestProcessorsArgumentCaptor.getValue();
        assertThat(requestProcessors)
                .hasSize(1)
                .containsExactlyInAnyOrder(requestProcessor);

        verify(reactiveInvocationHandlerFactory).build(any(ExtendedClientCodecConfigurer.class), same(requestProcessors),  anyList(), isNull(), isNull(), eq(TestClient.class), same(targetUri));
        verifyNoMoreInteractions(reactiveInvocationHandlerFactory);
    }

    @Test
    public void registerMultipleRequestInterceptors(){
        URI targetUri = URI.create("http://example.ca");
        RequestProcessor requestProcessor1 = clientRequest -> clientRequest;
        RequestProcessor requestProcessor2 = clientRequest -> clientRequest;
        RequestProcessor requestProcessor3 = clientRequest -> clientRequest;
        when(reactiveInvocationHandlerFactory.build(any(ExtendedClientCodecConfigurer.class), requestProcessorsArgumentCaptor.capture(),  anyList(), isNull(), isNull(), eq(TestClient.class), same(targetUri))).thenReturn(new MockInvocationHandler());

        createBuilder()
                .requestProcessor(requestProcessor1)
                .requestProcessors(requestInterceptors -> {
                    requestInterceptors.add(requestProcessor2);
                    requestInterceptors.add(requestProcessor3);
                })
                .build(TestClient.class, targetUri);

        List<RequestProcessor> requestProcessors = requestProcessorsArgumentCaptor.getValue();
        assertThat(requestProcessors)
                .hasSize(3)
                .containsExactly(requestProcessor1, requestProcessor2, requestProcessor3);

        verify(reactiveInvocationHandlerFactory).build(any(ExtendedClientCodecConfigurer.class), same(requestProcessors),  anyList(), isNull(), isNull(), eq(TestClient.class), same(targetUri));
        verifyNoMoreInteractions(reactiveInvocationHandlerFactory);
    }

    @Test
    public void notRegisterAnyRequestInterceptor(){
        URI targetUri = URI.create("http://example.ca");
        when(reactiveInvocationHandlerFactory.build(any(ExtendedClientCodecConfigurer.class), requestProcessorsArgumentCaptor.capture(),  anyList(), isNull(), isNull(), eq(TestClient.class), same(targetUri))).thenReturn(new MockInvocationHandler());

        createBuilder()
                .build(TestClient.class, targetUri);

        List<RequestProcessor> requestProcessors = requestProcessorsArgumentCaptor.getValue();
        assertThat(requestProcessors)
                .isEmpty();

        verify(reactiveInvocationHandlerFactory).build(any(ExtendedClientCodecConfigurer.class), same(requestProcessors),  anyList(), isNull(), isNull(), eq(TestClient.class), same(targetUri));
        verifyNoMoreInteractions(reactiveInvocationHandlerFactory);
    }

    @Test
    public void registerResponseInterceptor(){
        URI targetUri = URI.create("http://example.ca");
        ResponseProcessor responseProcessor = clientResponse -> {System.out.println(clientResponse); return clientResponse;};
        when(reactiveInvocationHandlerFactory.build(any(ExtendedClientCodecConfigurer.class), anyList(), responseProcessorsArgumentCaptor.capture(), isNull(), isNull(), eq(TestClient.class), same(targetUri))).thenReturn(new MockInvocationHandler());

        createBuilder()
                .responseProcessor(responseProcessor)
                .build(TestClient.class, targetUri);

        List<ResponseProcessor> responseProcessors = responseProcessorsArgumentCaptor.getValue();
        assertThat(responseProcessors)
                .hasSize(1)
                .containsExactly(responseProcessor);

        verify(reactiveInvocationHandlerFactory).build(any(ExtendedClientCodecConfigurer.class), anyList(), same(responseProcessors), isNull(), isNull(), eq(TestClient.class), same(targetUri));
        verifyNoMoreInteractions(reactiveInvocationHandlerFactory);
    }

    @Test
    public void registerMultipleResponseInterceptors(){
        URI targetUri = URI.create("http://example.ca");
        ResponseProcessor responseProcessor1 = clientResponse -> clientResponse;
        ResponseProcessor responseProcessor2 = clientResponse -> clientResponse;
        ResponseProcessor responseProcessor3 = clientResponse -> clientResponse;
        when(reactiveInvocationHandlerFactory.build(any(ExtendedClientCodecConfigurer.class), anyList(), responseProcessorsArgumentCaptor.capture(), isNull(), isNull(), eq(TestClient.class), same(targetUri))).thenReturn(new MockInvocationHandler());

        createBuilder()
                .responseProcessor(responseProcessor1)
                .responseProcessors(responseInterceptors -> {
                    responseInterceptors.add(responseProcessor2);
                    responseInterceptors.add(responseProcessor3);
                })
                .build(TestClient.class, targetUri);

        List<ResponseProcessor> responseProcessors = responseProcessorsArgumentCaptor.getValue();
        assertThat(responseProcessors)
                .hasSize(3)
                .containsExactly(responseProcessor1, responseProcessor2, responseProcessor3);

        verify(reactiveInvocationHandlerFactory).build(any(ExtendedClientCodecConfigurer.class), anyList(), same(responseProcessors), isNull(), isNull(), eq(TestClient.class), same(targetUri));
        verifyNoMoreInteractions(reactiveInvocationHandlerFactory);
    }

    @Test
    public void notRegisterAnyResponseInterceptor(){
        URI targetUri = URI.create("http://example.ca");
        when(reactiveInvocationHandlerFactory.build(any(ExtendedClientCodecConfigurer.class), anyList(), responseProcessorsArgumentCaptor.capture(), isNull(), isNull(), eq(TestClient.class), same(targetUri))).thenReturn(new MockInvocationHandler());

        createBuilder()
                .build(TestClient.class, targetUri);

        List<ResponseProcessor> responseProcessors = responseProcessorsArgumentCaptor.getValue();
        assertThat(responseProcessors)
                .isEmpty();

        verify(reactiveInvocationHandlerFactory).build(any(ExtendedClientCodecConfigurer.class), anyList(), same(responseProcessors), isNull(), isNull(), eq(TestClient.class), same(targetUri));
        verifyNoMoreInteractions(reactiveInvocationHandlerFactory);
    }

    @Test
    public void logLevel(){
        URI targetUri = URI.create("http://example.ca");
        when(reactiveInvocationHandlerFactory.build(any(ExtendedClientCodecConfigurer.class), anyList(), anyList(), isNull(), same(LogLevel.HEADERS), eq(TestClient.class), same(targetUri))).thenReturn(new MockInvocationHandler());

        createBuilder()
                .logLevel(LogLevel.HEADERS)
                .build(TestClient.class, targetUri);

        verify(reactiveInvocationHandlerFactory).build(any(ExtendedClientCodecConfigurer.class), anyList(), anyList(), isNull(), same(LogLevel.HEADERS), eq(TestClient.class), same(targetUri));
        verifyNoMoreInteractions(reactiveInvocationHandlerFactory);
    }

    @Test
    public void logger(){
        Logger logger = new DummyLogger();
        URI targetUri = URI.create("http://example.ca");
        when(reactiveInvocationHandlerFactory.build(any(ExtendedClientCodecConfigurer.class), anyList(), anyList(), same(logger), isNull(), eq(TestClient.class), same(targetUri))).thenReturn(new MockInvocationHandler());

        createBuilder()
                .logger(logger)
                .build(TestClient.class, targetUri);

        verify(reactiveInvocationHandlerFactory).build(any(ExtendedClientCodecConfigurer.class), anyList(), anyList(), same(logger), isNull(), eq(TestClient.class), same(targetUri));
        verifyNoMoreInteractions(reactiveInvocationHandlerFactory);
    }

    Optional<HttpErrorReader> findReader(List<HttpErrorReader> httpErrorReaders, HttpStatus httpStatus) {
        return httpErrorReaders.stream().filter(httpErrorReader -> httpErrorReader.canRead(httpStatus)).findFirst();
    }

    private static class DummyLogger implements Logger {

        @Override
        public void log(Supplier<String> messageSupplier) {

        }

        @Override
        public void log(String message) {

        }
    }

    private DefaultClientBuilder createBuilder(){
        return new DefaultClientBuilder(reactiveInvocationHandlerFactory);
    }

    interface TestClient{}

    class OverrideHttpClientErrorDecoder extends HttpClientErrorDecoder {
    }

    class OverrideHttpServerErrorDecoder extends HttpServerErrorDecoder {
    }
}