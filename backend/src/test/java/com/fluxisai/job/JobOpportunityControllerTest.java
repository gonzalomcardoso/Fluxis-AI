package com.fluxisai.job;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import com.fluxisai.common.error.GlobalExceptionHandler;
import com.fluxisai.common.web.PageResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(JobOpportunityController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class JobOpportunityControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JobOpportunityService service;

    @Test
    void postValidRequestReturnsCreatedResponseAndLocation() throws Exception {
        when(service.create(any(CreateJobOpportunityRequest.class))).thenReturn(response());

        mockMvc.perform(post("/api/job-opportunities")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "companyName": "Acme",
                                  "title": "Backend Developer",
                                  "url": "https://jobs.example.test/acme",
                                  "technologies": ["Java"],
                                  "status": "INTERESTING"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location",
                        org.hamcrest.Matchers.endsWith("/api/job-opportunities/" + response().id())))
                .andExpect(jsonPath("$.companyName").value("Acme"))
                .andExpect(jsonPath("$.title").value("Backend Developer"))
                .andExpect(jsonPath("$.status").value("INTERESTING"));
    }

    @Test
    void postInvalidRequestReturnsProblemDetailWithFieldErrors() throws Exception {
        mockMvc.perform(post("/api/job-opportunities")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "companyName": " ",
                                  "title": "",
                                  "url": "javascript:alert(1)"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validación fallida"))
                .andExpect(jsonPath("$.errors.companyName").exists())
                .andExpect(jsonPath("$.errors.title").exists())
                .andExpect(jsonPath("$.errors.url")
                        .value("La URL debe empezar por http:// o https://"));
    }

    @Test
    void postInvalidEnumReturnsProblemDetail() throws Exception {
        mockMvc.perform(post("/api/job-opportunities")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "companyName": "Acme",
                                  "title": "Backend Developer",
                                  "status": "NOPE"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Solicitud no válida"))
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void getListReturnsPagedResponse() throws Exception {
        JobOpportunityResponse opportunity = response();
        PageRequest pageable = PageRequest.of(0, 20);
        when(service.list(any())).thenReturn(PageResponse.from(
                new PageImpl<>(List.of(opportunity), pageable, 1)));

        mockMvc.perform(get("/api/job-opportunities"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(opportunity.id().toString()))
                .andExpect(jsonPath("$.content[0].companyName").value("Acme"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    private JobOpportunityResponse response() {
        OffsetDateTime now = OffsetDateTime.of(2026, 11, 4, 10, 0, 0, 0, ZoneOffset.UTC);
        return new JobOpportunityResponse(
                UUID.fromString("6b7332f1-cfea-4c12-a64d-69f98965e126"),
                "Acme",
                "Backend Developer",
                "https://jobs.example.test/acme",
                null,
                null,
                null,
                null,
                List.of("Java"),
                JobStatus.INTERESTING,
                now,
                now);
    }
}
