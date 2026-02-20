package com.example.aiec.modules.inventory.adapter.rest;

import com.example.aiec.modules.inventory.application.port.AvailabilityDto;
import com.example.aiec.modules.inventory.application.port.InventoryCommandPort;
import com.example.aiec.modules.inventory.application.port.InventoryQueryPort;
import com.example.aiec.modules.inventory.application.port.ReservationDto;
import com.example.aiec.modules.shared.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class InventoryControllerContractTest {

    @Mock
    InventoryQueryPort inventoryQuery;

    @Mock
    InventoryCommandPort inventoryCommand;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        InventoryController controller = new InventoryController(inventoryQuery, inventoryCommand);
        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void createReservation_withoutSessionHeader_shouldReturnInvalidRequest() throws Exception {
        mockMvc.perform(post("/api/inventory/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "productId": 1,
                                  "quantity": 2
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"));
    }

    @Test
    void createReservation_shouldReturnCreatedReservation() throws Exception {
        ReservationDto reservation = new ReservationDto(11L, 1L, 2, "TENTATIVE", Instant.parse("2026-02-20T00:00:00Z"), 8);
        when(inventoryCommand.createReservation("session-1", 1L, 2)).thenReturn(reservation);

        mockMvc.perform(post("/api/inventory/reservations")
                        .header("X-Session-Id", "session-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "productId": 1,
                                  "quantity": 2
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.reservationId").value(11));
    }

    @Test
    void releaseReservation_shouldReturnSuccess() throws Exception {
        mockMvc.perform(delete("/api/inventory/reservations")
                        .header("X-Session-Id", "session-1")
                        .param("productId", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(inventoryCommand).releaseReservation("session-1", 5L);
    }

    @Test
    void releaseCommittedReservations_shouldForwardOrderId() throws Exception {
        mockMvc.perform(post("/api/inventory/reservations/release")
                        .header("X-Session-Id", "session-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "orderId": 99
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(inventoryCommand).releaseCommittedReservations(99L);
    }

    @Test
    void getAvailability_shouldReturnAvailability() throws Exception {
        when(inventoryQuery.getAvailableStock(1L)).thenReturn(new AvailabilityDto(1L, 20, 3, 2, 15));

        mockMvc.perform(get("/api/inventory/availability/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.availableStock").value(15));
    }
}
