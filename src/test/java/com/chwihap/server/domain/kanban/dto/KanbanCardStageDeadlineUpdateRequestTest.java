package com.chwihap.server.domain.kanban.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;
import org.openapitools.jackson.nullable.JsonNullableModule;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class KanbanCardStageDeadlineUpdateRequestTest {

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .registerModule(new JsonNullableModule());

    @Test
    void deadline_필드를_생략하면_기존_값_유지를_의미한다() throws Exception {
        KanbanCardStageDeadlineUpdateRequest request = objectMapper.readValue(
                """
                        {
                          "stageId": 3
                        }
                        """,
                KanbanCardStageDeadlineUpdateRequest.class
        );

        assertThat(request.deadline().isPresent()).isFalse();
    }

    @Test
    void deadline에_명시적_null을_전달하면_상시채용을_의미한다() throws Exception {
        KanbanCardStageDeadlineUpdateRequest request = objectMapper.readValue(
                """
                        {
                          "stageId": 3,
                          "deadline": null
                        }
                        """,
                KanbanCardStageDeadlineUpdateRequest.class
        );

        assertThat(request.deadline().isPresent()).isTrue();
        assertThat(request.deadline().get()).isNull();
    }

    @Test
    void deadline에_날짜를_전달하면_해당_값으로_변경된다() throws Exception {
        KanbanCardStageDeadlineUpdateRequest request = objectMapper.readValue(
                """
                        {
                          "deadline": "2026-08-20"
                        }
                        """,
                KanbanCardStageDeadlineUpdateRequest.class
        );

        assertThat(request.deadline().isPresent()).isTrue();
        assertThat(request.deadline().get()).isEqualTo(LocalDate.of(2026, 8, 20));
    }

    @Test
    void stageId를_생략해도_예외없이_생략_처리된다() throws Exception {
        KanbanCardStageDeadlineUpdateRequest request = objectMapper.readValue(
                """
                        {
                          "deadline": "2026-08-20"
                        }
                        """,
                KanbanCardStageDeadlineUpdateRequest.class
        );

        assertThat(request.stageId()).isNull();
    }

    @Test
    void stageId에_명시적_null을_전달해도_생략과_동일하게_처리된다() throws Exception {
        KanbanCardStageDeadlineUpdateRequest request = objectMapper.readValue(
                """
                        {
                          "stageId": null,
                          "deadline": "2026-08-20"
                        }
                        """,
                KanbanCardStageDeadlineUpdateRequest.class
        );

        assertThat(request.stageId()).isNull();
    }
}
