package io.github.sahinemirhan;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.aop.support.AopUtils;
import io.github.sahinemirhan.controller.HelloController;
import io.github.sahinemirhan.starter.aop.RiskAspect;
import io.github.sahinemirhan.starter.aop.internal.RiskEvaluationProcessor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class RiskAspectIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    @SpyBean
    private RiskEvaluationProcessor evaluationProcessor;

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private HelloController helloController;

    @Autowired
    private RiskAspect riskAspect;

    @Test
    void riskAspectIsTriggeredForAnnotatedController() throws Throwable {
        mockMvc.perform(get("/hello").param("user", "alice"))
                .andExpect(status().isOk())
                .andExpect(content().string("Helloalice"));

        assertThat(applicationContext.containsBean("org.springframework.aop.config.internalAutoProxyCreator"))
                .as("auto proxy creator should be registered")
                .isTrue();

        assertThat(applicationContext.getBeansWithAnnotation(org.aspectj.lang.annotation.Aspect.class))
                .as("aspect beans should include RiskAspect")
                .containsValue(riskAspect);

        assertThat(AopUtils.isAopProxy(helloController))
                .as("helloController should be proxied by Spring AOP")
                .isTrue();

        verify(evaluationProcessor, atLeastOnce()).evaluate(any(), any());
    }
}
