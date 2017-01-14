package com.github.jbrixhe.reactiveclient.request;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.http.HttpMethod;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.AbstractMap.SimpleEntry;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@RunWith(MockitoJUnitRunner.class)
public class RequestTemplateAnnotationVisitorTest {
    @InjectMocks
    private RequestTemplateAnnotationVisitor requestTemplateAnnotationVisitor;

    @Test
    public void processRootRequestTemplate_withSingleInterface() {
        RequestTemplate requestTemplate = requestTemplateAnnotationVisitor.processRootRequestTemplate(ParentReactiveClient.class);
        assertThat(requestTemplate).isNotNull();
        assertThat(requestTemplate.getRequestPath().getPathSegments()).hasSize(1);
    }

    @Test
    public void processRootRequestTemplate_withOneParentInterface() {
        RequestTemplate requestTemplate = requestTemplateAnnotationVisitor.processRootRequestTemplate(ChildReactiveClient.class);
        assertThat(requestTemplate).isNotNull();
        assertThat(requestTemplate.getRequestPath().getPathSegments()).hasSize(2);
    }

    @Test
    public void processRootRequestTemplate_withNoRequestMappingOnClass() {
        RequestTemplate requestTemplate = requestTemplateAnnotationVisitor.processRootRequestTemplate(SimpleInterface.class);
        assertThat(requestTemplate).isNotNull();
        assertThat(requestTemplate.getRequestPath().getPathSegments()).isEmpty();
    }

    @Test
    public void processRootRequestTemplate_withTooManyParent() {
        assertThatThrownBy(() -> requestTemplateAnnotationVisitor.processRootRequestTemplate(ChildReactiveClientWithTwoDirectParents.class))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void visit() {
        assertThat(requestTemplateAnnotationVisitor.visit(ChildReactiveClient.class))
                .hasSize(2)
                .extracting("requestPath.pathSegments.size")
                .containsExactlyInAnyOrder(2, 3);
    }

    @Test
    public void parsePath() {
        RequestTemplate requestTemplate = new RequestTemplate();
        requestTemplateAnnotationVisitor.parsePath(Collections.singletonMap("value", new String[]{"/api"}), requestTemplate);
        assertThat(requestTemplate.getRequestPath().getPathSegments()).hasSize(1);
    }

    @Test
    public void parsePath_withNoValue() {
        RequestTemplate requestTemplate = new RequestTemplate();
        requestTemplateAnnotationVisitor.parsePath(Collections.singletonMap("value", new String[]{}), requestTemplate);
        assertThat(requestTemplate.getRequestPath().getPathSegments()).isEmpty();
    }

    @Test
    public void parsePath_withTooManyValue() {
        RequestTemplate requestTemplate = new RequestTemplate();
        assertThatThrownBy(() -> requestTemplateAnnotationVisitor.parsePath(Collections.singletonMap("value", new String[]{"/parent", "/child"}), requestTemplate))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void parseMethod() {
        RequestTemplate requestTemplate = new RequestTemplate();
        requestTemplateAnnotationVisitor.parseMethod(Collections.singletonMap("method", new RequestMethod[]{RequestMethod.GET}), requestTemplate);
        assertThat(requestTemplate.method).isEqualTo(HttpMethod.GET);
    }

    @Test
    public void parseMethod_withNoValue() {
        RequestTemplate requestTemplate = new RequestTemplate();
        requestTemplateAnnotationVisitor.parseMethod(Collections.singletonMap("method", new RequestMethod[]{}), requestTemplate);
        assertThat(requestTemplate.method).isEqualTo(HttpMethod.GET);
    }

    @Test
    public void parseMethod_withTooManyValue() {
        RequestTemplate requestTemplate = new RequestTemplate();
        assertThatThrownBy(() -> requestTemplateAnnotationVisitor.parseMethod(Collections.singletonMap("method", new RequestMethod[]{RequestMethod.PUT, RequestMethod.POST}), requestTemplate))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void parseHeaders() {
        RequestTemplate requestTemplate = new RequestTemplate();
        requestTemplateAnnotationVisitor.parseHeaders(Collections.singletonMap("headers", new String[]{"header1=value1", "header2=value2"}), requestTemplate);
        assertThat(requestTemplate.headerTemplates)
                .containsOnlyKeys("header1", "header2");
    }

    @Test
    public void extractHeader_withoutEquals() {
        RequestTemplate requestTemplate = new RequestTemplate();
        assertThatThrownBy(() -> requestTemplateAnnotationVisitor.extractHeader("namevalue", requestTemplate))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void extractHeader_withEmptyName() {
        RequestTemplate requestTemplate = new RequestTemplate();
        assertThatThrownBy(() -> requestTemplateAnnotationVisitor.extractHeader("   =value", requestTemplate))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void extractHeader_withNoName() {
        RequestTemplate requestTemplate = new RequestTemplate();
        assertThatThrownBy(() -> requestTemplateAnnotationVisitor.extractHeader("=value", requestTemplate))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void extractHeader_withEmptyValue() {
        RequestTemplate requestTemplate = new RequestTemplate();
        assertThatThrownBy(() -> requestTemplateAnnotationVisitor.extractHeader("name=   ", requestTemplate))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void extractHeader_withNoValue() {
        RequestTemplate requestTemplate = new RequestTemplate();
        assertThatThrownBy(() -> requestTemplateAnnotationVisitor.extractHeader("name=", requestTemplate))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void parameterAnnotationProcessing_withRequestParameter() {
        List<RequestTemplate> visit = requestTemplateAnnotationVisitor.visit(ReactiveClientWithRequestParameters.class);
        assertThat(visit)
                .hasSize(1);
        RequestTemplate requestTemplate = visit.get(0);
        assertThat(requestTemplate.indexToParameterNames)
                .contains(new SimpleEntry<>(0, "requestParameter1"),
                        new SimpleEntry<>(1, "requestParameter2"));
    }

    @Test
    public void parameterAnnotationProcessing_withPathParameter() {
        List<RequestTemplate> visit = requestTemplateAnnotationVisitor.visit(ReactiveClientWithPathParameters.class);
        assertThat(visit)
                .hasSize(1);
        RequestTemplate requestTemplate = visit.get(0);
        assertThat(requestTemplate.indexToParameterNames)
                .contains(new SimpleEntry<>(0, "pathVariable1"),
                        new SimpleEntry<>(1, "pathVariable2"));
    }

    @Test
    public void parameterAnnotationProcessing_withRequestAndPathParameters() {
        List<RequestTemplate> visit = requestTemplateAnnotationVisitor.visit(ReactiveClientWithRequestAndPathParameters.class);
        assertThat(visit)
                .hasSize(1);
        RequestTemplate requestTemplate = visit.get(0);
        assertThat(requestTemplate.indexToParameterNames)
                .contains(new SimpleEntry<>(0, "requestParameter1"),
                        new SimpleEntry<>(1, "pathVariable1"));
    }

    interface SimpleInterface {
    }

    @RequestMapping("/parent")
    interface ParentReactiveClient {
    }

    @RequestMapping("/parentbis")
    interface ParentReactiveClientBis {
    }

    @RequestMapping("/child")
    interface ChildReactiveClient extends ParentReactiveClient {

        void testGet();

        @RequestMapping("/get2")
        void testGet2(String test);

    }

    @RequestMapping("/grandChild")
    interface ChildReactiveClientWithTwoDirectParents extends ChildReactiveClient, ParentReactiveClient {
    }

    interface ReactiveClientWithRequestParameters {
        void testGet2(@RequestParam("requestParameter1") String requestParameter1, @RequestParam("requestParameter2") String requestParameter2);
    }

    interface ReactiveClientWithPathParameters {
        void testGet2(@PathVariable("pathVariable1") String pathVariable1, @PathVariable("pathVariable2") String pathVariable2);
    }

    interface ReactiveClientWithRequestAndPathParameters {
        void testGet2(@RequestParam("requestParameter1") String requestParameter1, @PathVariable("pathVariable1") String pathVariable1);
    }
}