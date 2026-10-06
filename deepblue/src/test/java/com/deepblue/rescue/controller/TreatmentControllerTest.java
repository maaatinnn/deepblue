package com.deepblue.rescue.controller;

import com.deepblue.rescue.domain.TreatmentType;
import com.deepblue.rescue.dto.request.CreateTreatmentRequest;
import com.deepblue.rescue.dto.response.TreatmentResponse;
import com.deepblue.rescue.exception.BusinessRuleException;
import com.deepblue.rescue.exception.GlobalExceptionHandler;
import com.deepblue.rescue.exception.ResourceNotFoundException;
import com.deepblue.rescue.service.TreatmentService;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TreatmentController.class)
@Import(GlobalExceptionHandler.class)
class TreatmentControllerTest {

    private static final String VALID_BODY = """
            {
              "animalCode": "AN-001",
              "specialistCode": "SPEC-001",
              "performedAt": "2026-08-21T09:00:00",
              "type": "WOUND_CARE",
              "description": "Cleaning and treatment of flipper injury."
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TreatmentService service;

    @Test
    void shouldCreateTreatment() throws Exception {

        when(service.register(any(CreateTreatmentRequest.class)))
                .thenReturn(new TreatmentResponse(
                        100L,
                        "AN-001",
                        "SPEC-001",
                        LocalDateTime.of(2026, 8, 21, 9, 0),
                        TreatmentType.WOUND_CARE,
                        "Cleaning and treatment of flipper injury."));

        mockMvc.perform(post("/api/treatments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.animalCode").value("AN-001"))
                .andExpect(jsonPath("$.specialistCode").value("SPEC-001"))
                .andExpect(jsonPath("$.performedAt").value("2026-08-21T09:00:00"))
                .andExpect(jsonPath("$.type").value("WOUND_CARE"))
                .andExpect(jsonPath("$.description")
                        .value("Cleaning and treatment of flipper injury."));

        ArgumentCaptor<CreateTreatmentRequest> captor =
                ArgumentCaptor.forClass(CreateTreatmentRequest.class);
        verify(service).register(captor.capture());

        CreateTreatmentRequest sent = captor.getValue();
        assertThat(sent.animalCode()).isEqualTo("AN-001");
        assertThat(sent.specialistCode()).isEqualTo("SPEC-001");
        assertThat(sent.performedAt()).isEqualTo(LocalDateTime.of(2026, 8, 21, 9, 0));
        assertThat(sent.type()).isEqualTo(TreatmentType.WOUND_CARE);
    }

    @Test
    void shouldReturn400WhenRequiredFieldsAreInvalid() throws Exception {

        mockMvc.perform(post("/api/treatments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "animalCode": "",
                                  "specialistCode": "",
                                  "type": null,
                                  "description": ""
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Request validation failed"))
                .andExpect(jsonPath("$.details.animalCode").value("Animal code is required"))
                .andExpect(jsonPath("$.details.specialistCode")
                        .value("Specialist code is required"))
                .andExpect(jsonPath("$.details.performedAt").value("Treatment date is required"))
                .andExpect(jsonPath("$.details.type").value("Treatment type is required"))
                .andExpect(jsonPath("$.details.description").exists());

        verify(service, never()).register(any());
    }

    @Test
    void shouldReturn400WhenPerformedAtIsInTheFuture() throws Exception {

        mockMvc.perform(post("/api/treatments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY.replace("2026-08-21T09:00:00", "2999-01-01T00:00:00")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Request validation failed"))
                .andExpect(jsonPath("$.details.performedAt")
                        .value("Treatment date cannot be in the future"));

        verify(service, never()).register(any());
    }

    @Test
    void shouldReturn400WhenDescriptionIsTooShort() throws Exception {

        mockMvc.perform(post("/api/treatments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY.replace(
                                "Cleaning and treatment of flipper injury.", "Too short")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.description")
                        .value("Description must contain between 10 and 500 characters"));

        verify(service, never()).register(any());
    }

    @Test
    void shouldReturn400WhenDescriptionIsTooLong() throws Exception {

        mockMvc.perform(post("/api/treatments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY.replace(
                                "Cleaning and treatment of flipper injury.", "x".repeat(501))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.description")
                        .value("Description must contain between 10 and 500 characters"));

        verify(service, never()).register(any());
    }

    @Test
    void shouldReturn400WhenTreatmentTypeIsNotAnEnumValue() throws Exception {

        mockMvc.perform(post("/api/treatments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY.replace("WOUND_CARE", "MAGIC")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Malformed or invalid JSON request"))
                .andExpect(jsonPath("$.details.body").exists());

        verify(service, never()).register(any());
    }

    @Test
    void shouldReturn404WhenAnimalDoesNotExist() throws Exception {

        when(service.register(any(CreateTreatmentRequest.class)))
                .thenThrow(new ResourceNotFoundException("Animal not found: AN-999"));

        mockMvc.perform(post("/api/treatments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY.replace("AN-001", "AN-999")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Animal not found: AN-999"))
                .andExpect(jsonPath("$.details").isMap());
    }

    @Test
    void shouldReturn409WhenBusinessRuleIsViolated() throws Exception {

        when(service.register(any(CreateTreatmentRequest.class)))
                .thenThrow(new BusinessRuleException(
                        "Released animals cannot receive treatments"));

        mockMvc.perform(post("/api/treatments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message")
                        .value("Released animals cannot receive treatments"))
                .andExpect(jsonPath("$.details").isMap());
    }

    @Test
    void shouldReturn500OnUnexpectedError() throws Exception {

        when(service.register(any(CreateTreatmentRequest.class)))
                .thenThrow(new RuntimeException("connection refused: jdbc:postgresql://..."));

        mockMvc.perform(post("/api/treatments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.error").value("Internal Server Error"))
                .andExpect(jsonPath("$.message").value("An unexpected error occurred"))
                .andExpect(jsonPath("$.details").isMap());
    }
}
