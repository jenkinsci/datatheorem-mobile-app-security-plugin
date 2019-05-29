package com.datatheorem.mobileappsecurity.jenkins.plugin;

import org.apache.http.*;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.impl.DefaultHttpResponseFactory;
import org.apache.http.message.BasicStatusLine;
import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.UnknownHostException;

import static org.easymock.EasyMock.partialMockBuilder;
import static org.easymock.EasyMock.replay;


/**
 * Unit Test of the sendBuild Action that is responsible of calling the Upload API of Data Theorem
 */
public class SendBuildTest {

    /**
     * Test the return message of uploadInit when IOException is raised
     * <p>
     * Mock the HttpResponse to raise an IOException
     * Verify that testUploadInit returns success = false
     * </p>
     */
    @Test()
    public void testUploadInitIOException() throws IOException {

        SendBuildAction uploadMock = partialMockBuilder(SendBuildAction.class)
                .withConstructor(String.class, PrintStream.class)
                .withArgs("toto", new PrintStream("test-logger.log"))
                .addMockedMethod("uploadInitRequest")
                .createMock();

        EasyMock.expect(uploadMock.uploadInitRequest())
                .andThrow(new IOException());

        replay(uploadMock);

        SendBuildMessage uploadInitMessage = uploadMock.uploadInit();
        Assert.assertEquals(uploadInitMessage.message, "Data Theorem upload_init call error: IOException null");
        Assert.assertFalse(uploadInitMessage.success);

        EasyMock.verify(uploadMock);
    }

    /**
     * Test the return message of uploadInit when UnknownHostException is raised
     * <p>
     * Mock the HttpResponse to raise an UnknownHostException
     * Verify that testUploadInit returns success = false
     * </p>
     */
    @Test()
    public void testUploadInitUnknownHost() throws IOException {
        SendBuildAction uploadMock = partialMockBuilder(SendBuildAction.class)
                .withConstructor(String.class, PrintStream.class)
                .withArgs("toto", new PrintStream("test-logger.log"))
                .addMockedMethod("uploadInitRequest")
                .createMock();

        EasyMock.expect(uploadMock.uploadInitRequest())
                .andThrow(new UnknownHostException());

        replay(uploadMock);
        SendBuildMessage uploadInitMessage = uploadMock.uploadInit();

        Assert.assertEquals(
                uploadInitMessage.message,
                "Data Theorem upload_init call error: UnknownHostException \n" +
                        "Please contact Data Theorem support: null");

        Assert.assertFalse(uploadInitMessage.success);

        EasyMock.verify(uploadMock);
    }

    /**
     * Test the return message of uploadInit when Data Theorem endpoint return 401 forbidden
     * <p>
     * Mock the HttpResponse to return a 401 with a payload
     * Verify that testUploadInit returns success = false and the httpResponse payload
     * </p>
     */
    @Test()
    public void testUploadInitForbidden() throws IOException {
        SendBuildAction uploadMock = partialMockBuilder(SendBuildAction.class)
                .withConstructor(String.class, PrintStream.class)
                .withArgs("toto", new PrintStream("test-logger.log"))
                .addMockedMethod("uploadInitRequest")
                .createMock();

        // Create an http response with unauthorized access statuscode
        HttpResponseFactory factory = new DefaultHttpResponseFactory();
        BasicHttpEntity entity = new BasicHttpEntity();
        HttpResponse response = factory.newHttpResponse(new BasicStatusLine(HttpVersion.HTTP_1_1, HttpStatus.SC_UNAUTHORIZED, "trototo"), null);
        InputStream inputStream = new ByteArrayInputStream("{new content}".getBytes());
        entity.setContent(inputStream);
        response.setEntity(entity);
        EasyMock.expect(uploadMock.uploadInitRequest())
                .andReturn(response);

        replay(uploadMock);
        SendBuildMessage uploadInitMessage = uploadMock.uploadInit();

        Assert.assertEquals(
                uploadInitMessage.message,
                "Data Theorem upload_init call Forbidden Access: {new content}");

        Assert.assertFalse(uploadInitMessage.success);

        EasyMock.verify(uploadMock);
    }

    /**
     * Test the return message of uploadInit when Data Theorem endpoint returns 200 but with a wrong payload
     * <p>
     * Mock the HttpResponse to return a 200 with a wrong  payload
     * Verify that testUploadInit returns success = false and the wrong payload
     * </p>
     */
    @Test()
    public void testUploadInitSuccessWrongPayload() throws IOException {
        SendBuildAction uploadMock = partialMockBuilder(SendBuildAction.class)
                .withConstructor(String.class, PrintStream.class)
                .withArgs("toto", new PrintStream("test-logger.log"))
                .addMockedMethod("uploadInitRequest")
                .createMock();

        // Create an http response with unauthorized access statuscode
        HttpResponseFactory factory = new DefaultHttpResponseFactory();
        BasicHttpEntity entity = new BasicHttpEntity();
        HttpResponse response = factory.newHttpResponse(new BasicStatusLine(HttpVersion.HTTP_1_1, HttpStatus.SC_OK, "tototo"), null);
        InputStream inputStream = new ByteArrayInputStream("I don't have data".getBytes());
        entity.setContent(inputStream);
        response.setEntity(entity);
        EasyMock.expect(uploadMock.uploadInitRequest())
                .andReturn(response);

        replay(uploadMock);
        SendBuildMessage uploadInitMessage = uploadMock.uploadInit();

        Assert.assertEquals(
                uploadInitMessage.message,
                "Data Theorem upload_init wrong payload: I don't have data");

        Assert.assertFalse(uploadInitMessage.success);

        EasyMock.verify(uploadMock);
    }

    /**
     * Test the return message of uploadInit when Data Theorem endpoint returns 200
     * <p>
     * Mock the HttpResponse to return a 200 with a payload containing the upload_url
     * Verify that testUploadInit returns success = true with the url
     * </p>
     */
    @Test()
    public void testUploadInitSuccess() throws IOException {
        SendBuildAction uploadMock = partialMockBuilder(SendBuildAction.class)
                .withConstructor(String.class, PrintStream.class)
                .withArgs("toto", new PrintStream("test-logger.log"))
                .addMockedMethod("uploadInitRequest")
                .createMock();

        // Create an http response with unauthorized access statuscode
        HttpResponseFactory factory = new DefaultHttpResponseFactory();
        BasicHttpEntity entity = new BasicHttpEntity();
        HttpResponse response = factory.newHttpResponse(new BasicStatusLine(HttpVersion.HTTP_1_1, HttpStatus.SC_OK, "tototo"), null);
        InputStream inputStream = new ByteArrayInputStream("{\"upload_url\": \"fake url\"}".getBytes());
        entity.setContent(inputStream);
        response.setEntity(entity);
        EasyMock.expect(uploadMock.uploadInitRequest())
                .andReturn(response);

        replay(uploadMock);
        SendBuildMessage uploadInitMessage = uploadMock.uploadInit();

        Assert.assertEquals(
                uploadInitMessage.message,
                "Successfully retrieved the download URL from Data Theorem: {\"upload_url\": \"fake url\"}");

        Assert.assertTrue(uploadInitMessage.success);

        EasyMock.verify(uploadMock);
    }

    /**
     * Test the return message of uploadBuild when Data Theorem endpoint return 200
     * <p>
     * Mock the HttpResponse to return a 200 with a payload contaning status and name
     * Verify that testUpload returns success = true with the payload
     * </p>
     */
    @Test()
    public void testUploadBuildSuccess() throws IOException {
        SendBuildAction uploadMock = partialMockBuilder(SendBuildAction.class)
                .withConstructor(String.class, PrintStream.class)
                .withArgs("toto", new PrintStream("test-logger.log"))
                .addMockedMethod("uploadBuildRequest")
                .createMock();

        // Create an http response with unauthorized access statuscode
        HttpResponseFactory factory = new DefaultHttpResponseFactory();
        BasicHttpEntity entity = new BasicHttpEntity();
        HttpResponse response = factory.newHttpResponse(new BasicStatusLine(HttpVersion.HTTP_1_1, HttpStatus.SC_OK, "tototo"), null);
        InputStream inputStream = new ByteArrayInputStream("{\"status\":\"ok\",\"name\":\"AndroidCodingExercise\"}".getBytes());
        entity.setContent(inputStream);
        response.setEntity(entity);
        EasyMock.expect(uploadMock.uploadBuildRequest("AndroidCodingExercise"))
                .andReturn(response);

        replay(uploadMock);
        SendBuildMessage uploadBuildMessage = uploadMock.uploadBuild("AndroidCodingExercise");

        Assert.assertEquals(
                uploadBuildMessage.message,
                "Successfully uploaded build to Data Theorem : {\"status\":\"ok\",\"name\":\"AndroidCodingExercise\"}");

        Assert.assertTrue(uploadBuildMessage.success);

        EasyMock.verify(uploadMock);
    }

    /**
     * Test the return message of uploadBuild when Data Theorem endpoint return 400 bad request
     * <p>
     * Mock the HttpResponse to return a 400 with a payload containing the error message
     * Verify that testUpload returns success = false
     * </p>
     */
    @Test()
    public void testUploadBuildUploadLinkExpire() throws IOException {
        SendBuildAction uploadMock = partialMockBuilder(SendBuildAction.class)
                .withConstructor(String.class, PrintStream.class)
                .withArgs("toto", new PrintStream("test-logger.log"))
                .addMockedMethod("uploadBuildRequest")
                .createMock();

        // Create an http response with unauthorized access statuscode
        HttpResponseFactory factory = new DefaultHttpResponseFactory();
        BasicHttpEntity entity = new BasicHttpEntity();
        HttpResponse response = factory.newHttpResponse(new BasicStatusLine(HttpVersion.HTTP_1_1, HttpStatus.SC_BAD_REQUEST, "tototo"), null);
        InputStream inputStream = new ByteArrayInputStream("{\"status\":\"ok\",\"name\":\"AndroidCodingExercise\"}".getBytes());
        entity.setContent(inputStream);
        response.setEntity(entity);
        EasyMock.expect(uploadMock.uploadBuildRequest("AndroidCodingExercise"))
                .andReturn(response);

        replay(uploadMock);
        SendBuildMessage uploadBuildMessage = uploadMock.uploadBuild("AndroidCodingExercise");

        Assert.assertFalse(uploadBuildMessage.success);

        EasyMock.verify(uploadMock);
    }


    /**
     * Test the return message of uploadInit when IOException is raised
     * <p>
     * Mock the HttpResponse to raise an IOException
     * Verify that testUploadInit returns success = false
     * </p>
     */
    @Test()
    public void testUploadBuildIOException() throws IOException {
        SendBuildAction uploadMock = partialMockBuilder(SendBuildAction.class)
                .withConstructor(String.class, PrintStream.class)
                .withArgs("toto", new PrintStream("test-logger.log"))
                .addMockedMethod("uploadBuildRequest")
                .createMock();

        // Create an http response with unauthorized access statuscode
        HttpResponseFactory factory = new DefaultHttpResponseFactory();
        BasicHttpEntity entity = new BasicHttpEntity();
        HttpResponse response = factory.newHttpResponse(new BasicStatusLine(HttpVersion.HTTP_1_1, HttpStatus.SC_BAD_REQUEST, "tototo"), null);
        InputStream inputStream = new ByteArrayInputStream("{\"status\":\"ok\",\"name\":\"AndroidCodingExercise\"}".getBytes());
        entity.setContent(inputStream);
        response.setEntity(entity);
        EasyMock.expect(uploadMock.uploadBuildRequest("AndroidCodingExercise"))
                .andThrow(new IOException());

        replay(uploadMock);
        SendBuildMessage uploadBuildMessage = uploadMock.uploadBuild("AndroidCodingExercise");
        Assert.assertEquals(uploadBuildMessage.message, "Data Theorem upload build returned an error: IOException: null");

        Assert.assertFalse(uploadBuildMessage.success);

        EasyMock.verify(uploadMock);
    }


    /**
     * Test the return message of SendBuildAction when the env var DATA_THEOREM_UPLOAD_API_KEY is not set
     * <p>
     * Verify that UploadInit returns success = false and an empty APIKey message
     * </p>
     */
    @Test()
    public void testMissingEnvVariable() throws IOException {
        SendBuildAction uploadMock = partialMockBuilder(SendBuildAction.class)
                .withConstructor(String.class, PrintStream.class)
                .withArgs(null, new PrintStream("test-logger.log"))
                .createMock();

        replay(uploadMock);
        SendBuildMessage sendBuildMessage = uploadMock.perform("foo.apk");

        Assert.assertEquals(
                sendBuildMessage.message,
                "Missing Data Theorem upload APIKey:\n" +
                        "Ensure \"DATA_THEOREM_UPLOAD_API_KEY\" is set in Credentials Binding");

        Assert.assertFalse(sendBuildMessage.success);

        EasyMock.verify(uploadMock);
    }


    /**
     * Test the return message of SendBuildAction when the env var DATA_THEOREM_UPLOAD_API_KEY is not set
     * <p>
     * Verify that UploadInit returns success = false
     * </p>
     */
    @Test()
    public void testEmptyApiKey() throws IOException {
        SendBuildAction uploadMock = partialMockBuilder(SendBuildAction.class)
                .withConstructor(String.class, PrintStream.class)
                .withArgs("", new PrintStream("test-logger.log"))
                .createMock();

        replay(uploadMock);
        SendBuildMessage sendBuildMessage = uploadMock.perform("foo.apk");

        Assert.assertEquals(
                sendBuildMessage.message,
                "Upload APIKey secret key is empty");

        Assert.assertFalse(sendBuildMessage.success);

        EasyMock.verify(uploadMock);
    }


    /**
     * Test the return message of uploadInit when the APIKey begins with "APIKey"
     * <p>
     * Verify that UploadInit returns success = false
     * </p>
     */
    @Test()
    public void testApiKeyBeginWithApiKey() throws IOException {
        SendBuildAction uploadMock = partialMockBuilder(SendBuildAction.class)
                .withConstructor(String.class, PrintStream.class)
                .withArgs("APIKey", new PrintStream("test-logger.log"))
                .createMock();

        replay(uploadMock);
        SendBuildMessage sendBuildMessage = uploadMock.perform("foo.apk");

        Assert.assertEquals(
                sendBuildMessage.message,
                "Error your upload APIKey shouldn't start with \"APIKey\""
        );

        Assert.assertFalse(sendBuildMessage.success);

        EasyMock.verify(uploadMock);
    }
}
