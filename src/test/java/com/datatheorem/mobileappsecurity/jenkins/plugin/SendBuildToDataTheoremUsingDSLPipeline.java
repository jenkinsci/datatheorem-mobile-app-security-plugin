//package com.datatheorem.mobileappsecurity.jenkins.plugin;
//
//
//import static com.lesfurets.jenkins.unit.MethodSignature.method;
//import static java.util.Arrays.stream;
//import static java.util.stream.Stream.concat;
//
//import java.util.function.Consumer;
//import java.util.stream.Stream;
//
//import org.junit.Before;
//import org.junit.Test;
//
//import com.lesfurets.jenkins.unit.BasePipelineTest;
//
//import groovy.lang.Script;
//public class SendBuildToDataTheoremUsingDSLPipeline extends BasePipelineTest {
//
//    @Override
//    @Before
//    public void setUp() throws Exception {
//        super.setUp();
//        Consumer println = System.out::println;
//        getHelper().registerAllowedMethod(method("step", String.class), println);
//    }
//
//        @Test
//        public void should_execute_without_errors() throws Exception {
//            Script script = (Script) loadScript("dsl-pipeline.jenkins");
//            script.run();
//
//            assertJobStatusSuccess();
//        }
//}
