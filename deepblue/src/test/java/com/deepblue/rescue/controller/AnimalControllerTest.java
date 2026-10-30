package com.deepblue.rescue.controller;

import com.deepblue.rescue.domain.AnimalSex;
import com.deepblue.rescue.domain.RescueStatus;
import com.deepblue.rescue.domain.TreatmentType;
import com.deepblue.rescue.dto.response.AnimalResponse;
import com.deepblue.rescue.dto.response.TreatmentResponse;
import com.deepblue.rescue.exception.GlobalExceptionHandler;
import com.deepblue.rescue.exception.ResourceNotFoundException;
import com.deepblue.rescue.service.AnimalService;
import com.deepblue.rescue.service.TreatmentService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AnimalController.class)
@Import(GlobalExceptionHandler.class)
class AnimalControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AnimalService animalService;

    @MockitoBean
    private TreatmentService treatmentService;

    // ------------------------------------------------------------------
    // GET /api/animals/{animalCode}
    // ------------------------------------------------------------------

    @Test
    void shouldReturnAnimalByCode() throws Exception {

        when(animalService.findByCode("AN-001"))
                .thenReturn(animal(1L, "AN-001"));

        mockMvc.perform(get("/api/animals/{code}", "AN-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.animalCode").value("AN-001"))
                .andExpect(jsonPath("$.commonName").value("Green Sea Turtle"))
                .andExpect(jsonPath("$.scientificName").value("Chelonia mydas"))
                .andExpect(jsonPath("$.sex").value("FEMALE"))
                .andExpect(jsonPath("$.caseCode").value("RES-2026-001"))
                .andExpect(jsonPath("$.rescueStatus").value("IN_REHABILITATION"));

        verify(animalService).findByCode("AN-001");
    }

    @Test
    void shouldReturn404WhenAnimalDoesNotExist() throws Exception {

        when(animalService.findByCode("AN-999"))
                .thenThrow(new ResourceNotFoundException("Animal not found: AN-999"));

        mockMvc.perform(get("/api/animals/{code}", "AN-999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Animal not found: AN-999"))
                .andExpect(jsonPath("$.details").isMap());
    }

    // ------------------------------------------------------------------
    // GET /api/animals/in-rehabilitation
    // ------------------------------------------------------------------

    @Test
    void shouldReturnAnimalsInRehabilitation() throws Exception {

        when(animalService.findAnimalsInRehabilitation())
                .thenReturn(List.of(animal(1L, "AN-001"), animal(2L, "AN-002")));

        mockMvc.perform(get("/api/animals/in-rehabilitation"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].animalCode").value("AN-001"))
                .andExpect(jsonPath("$[1].animalCode").value("AN-002"));

        verify(animalService).findAnimalsInRehabilitation();
    }

    @Test
    void shouldNotTreatInRehabilitationAsAnAnimalCode() throws Exception {

        when(animalService.findAnimalsInRehabilitation()).thenReturn(List.of());

        mockMvc.perform(get("/api/animals/in-rehabilitation"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        verify(animalService, never()).findByCode(anyString());
    }

    // ------------------------------------------------------------------
    // GET /api/animals/{animalCode}/treatments
    // ------------------------------------------------------------------

    @Test
    void shouldReturnAnimalTreatments() throws Exception {

        when(treatmentService.findByAnimalCode("AN-001"))
                .thenReturn(List.of(
                        new TreatmentResponse(
                                10L,
                                "AN-001",
                                "SPEC-001",
                                LocalDateTime.of(2026, 8, 21, 9, 0),
                                TreatmentType.WOUND_CARE,
                                "Cleaning of left front flipper injury."),
                        new TreatmentResponse(
                                11L,
                                "AN-001",
                                "SPEC-002",
                                LocalDateTime.of(2026, 8, 22, 10, 30),
                                TreatmentType.HYDRATION,
                                "Fluid therapy after admission.")));

        mockMvc.perform(get("/api/animals/{code}/treatments", "AN-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(10))
                .andExpect(jsonPath("$[0].type").value("WOUND_CARE"))
                .andExpect(jsonPath("$[0].performedAt").value("2026-08-21T09:00:00"))
                .andExpect(jsonPath("$[1].type").value("HYDRATION"));

        verify(treatmentService).findByAnimalCode("AN-001");
    }

    @Test
    void shouldReturnEmptyListWhenAnimalHasNoTreatments() throws Exception {

        when(treatmentService.findByAnimalCode("AN-001")).thenReturn(List.of());

        mockMvc.perform(get("/api/animals/{code}/treatments", "AN-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // ------------------------------------------------------------------
    // GET /api/animals/{animalCode}/treatment-eligibility
    // ------------------------------------------------------------------

    @Test
    void shouldReturnTreatmentEligibility() throws Exception {

        when(animalService.canReceiveTreatment("AN-001")).thenReturn(true);

        mockMvc.perform(get("/api/animals/{code}/treatment-eligibility", "AN-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.animalCode").value("AN-001"))
                .andExpect(jsonPath("$.eligible").value(true));

        verify(animalService).canReceiveTreatment("AN-001");
    }

    @Test
    void shouldReturnNotEligibleWhenAnimalCannotReceiveTreatment() throws Exception {

        when(animalService.canReceiveTreatment("AN-002")).thenReturn(false);

        mockMvc.perform(get("/api/animals/{code}/treatment-eligibility", "AN-002"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.animalCode").value("AN-002"))
                .andExpect(jsonPath("$.eligible").value(false));
    }

    @Test
    void shouldReturn404WhenCheckingEligibilityOfUnknownAnimal() throws Exception {

        when(animalService.canReceiveTreatment("AN-999"))
                .thenThrow(new ResourceNotFoundException("Animal not found: AN-999"));

        mockMvc.perform(get("/api/animals/{code}/treatment-eligibility", "AN-999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Animal not found: AN-999"))
                .andExpect(jsonPath("$.details").isMap());

        verify(animalService).canReceiveTreatment("AN-999");
    }

    // ------------------------------------------------------------------

    private static AnimalResponse animal(Long id, String code) {
        return new AnimalResponse(
                id,
                code,
                "Green Sea Turtle",
                "Chelonia mydas",
                AnimalSex.FEMALE,
                "RES-2026-001",
                RescueStatus.IN_REHABILITATION);
    }
}
