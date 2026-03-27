package com.dbmetadoc.app.exception;

import com.dbmetadoc.common.dto.DocumentRequest;
import com.dbmetadoc.common.enums.ResultCode;
import com.dbmetadoc.common.exception.BusinessException;
import jakarta.validation.Valid;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GlobalExceptionHandlerTest {

    private final MockMvc mockMvc = MockMvcBuilders
            .standaloneSetup(new TestController())
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();

    @Test
    void shouldWrapBusinessException() throws Exception {
        mockMvc.perform(post("/test/business"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ResultCode.BUSINESS_ERROR.getCode()))
                .andExpect(jsonPath("$.message").value("业务失败"));
    }

    @Test
    void shouldWrapValidationException() throws Exception {
        mockMvc.perform(post("/test/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"format\":\"HTML\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ResultCode.VALIDATION_FAILED.getCode()));
    }

    @RestController
    static class TestController {

        @PostMapping("/test/business")
        public void business() {
            throw new BusinessException("业务失败");
        }

        @PostMapping("/test/validate")
        public void validate(@Valid @RequestBody DocumentRequest request) {
            // validation only
        }
    }
}
