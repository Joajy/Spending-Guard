package com.joajy.spendingguard.spendevent.application;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.joajy.spendingguard.outbox.OutboxEvent;
import com.joajy.spendingguard.outbox.OutboxEventRepository;
import com.joajy.spendingguard.spendevent.domain.DeduplicationKeyGenerator;
import com.joajy.spendingguard.spendevent.domain.MessageSanitizer;
import com.joajy.spendingguard.spendevent.domain.SpendEventStatus;
import com.joajy.spendingguard.spendevent.persistence.RawSpendEvent;
import com.joajy.spendingguard.spendevent.persistence.RawSpendEventRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class SpendEventService {

    private final RawSpendEventRepository rawSpendEventRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final MessageSanitizer messageSanitizer;
    private final DeduplicationKeyGenerator deduplicationKeyGenerator;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public SpendEventService(
            RawSpendEventRepository rawSpendEventRepository,
            OutboxEventRepository outboxEventRepository,
            MessageSanitizer messageSanitizer,
            DeduplicationKeyGenerator deduplicationKeyGenerator,
            ObjectMapper objectMapper,
            Clock clock
    ) {
        this.rawSpendEventRepository = rawSpendEventRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.messageSanitizer = messageSanitizer;
        this.deduplicationKeyGenerator = deduplicationKeyGenerator;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Transactional
    public SpendEventReceipt submit(SubmitSpendEventCommand command) {
        UUID eventId = UUID.randomUUID();
        Instant receivedAt = clock.instant();
        String externalEventId = normalizeExternalEventId(command.externalEventId());
        String deduplicationKey = degn���$z{-���jםmport org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SpendEventController.class)
class SpendEventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SpendEventService spendEventService;

    @Test
    void acceptsValidSpendEvent() throws Exception {
        UUID eventId = UUID.fromString("9bbd364c-a952-42aa-91cb-f607603aa7d5");
        Instant receivedAt = Instant.parse("2026-08-13T01:30:00Z");
        given(spendEventService.submit(any())).willReturn(
                new SpendEventReceipt(eventId, SpendEventStatus.RECEIVED, receivedAt)
        );

        mockMvc.perform(post("/api/v1/spend-events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "source": "MANUAL_TEXT",
                                  "message": "테스트상점 12,800원 결제",
                                  "occurredAt": "2026-08-13T01:00:00Z"
                                }
                                """))
                .andExpect(status().isAccepted())
                .andExpect(header().string("Location", "http://localhost/api/v1/spend-events/" + eventId))
                .andExpect(jsonPath("$.eventId").value(eventId.toString()))
                .andExpect(jsonPath("$.status").value("RECEIVED"))
                .andExpect(jsonPath("$.receivedAt").value("2026-08-13T01:30:00Z"));
    }

    @Test
    void rejectsBlankMessage() throws Exception {
        mockMvc.perform(post("/api/v1/spend-events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "source": "MANUAL_TEXT",
                                  "message": " "
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid request"))
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.startsWith("message:")));

        verifyNoInteractions(spendEventService);
    }

    @Test
    void returnsConflictForDuplicateEvent() throws Exception {
        given(spendEventService.submit(any())).willThrow(new DuplicateSpendEventException());

        mockMvc.perform(post("/api/v1/spend-events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "source": "SIMULATOR",
                                  "externalEventId": "event-100",
                                  "message": "테스트상점 12,800원 결제"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Duplicate spend event"));
    }
}
