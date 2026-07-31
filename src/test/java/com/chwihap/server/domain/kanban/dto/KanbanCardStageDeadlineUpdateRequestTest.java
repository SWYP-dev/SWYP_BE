package com.chwihap.server.domain.kanban.dto;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class KanbanCardStageDeadlineUpdateRequestTest {

    @Test
    void 지원_마감일에_명시적인_null을_전달할_수_없다() {
        ObjectMapper objectMapper = new ObjectMapper();

        assertThatThrownBy(() -> objectMapper.readValue(
                """
                        {
                          "stageId": 3,
                          "deadline": null
                        }
                        """,
                KanbanCardStageDeadlineUpdateRequest.class
        ))
                .isInstanceOf(JsonMappingException.class)
                .hasMessageContaining("deadline");
    }
}
