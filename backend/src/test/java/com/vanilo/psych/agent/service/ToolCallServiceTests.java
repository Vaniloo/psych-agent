package com.vanilo.psych.agent.service;

import com.vanilo.psych.agent.dto.ToolCallRequest;
import com.vanilo.psych.agent.dto.ToolExecutionResponse;
import com.vanilo.psych.agent.dto.ToolInfoResponse;
import com.vanilo.psych.agent.dto.ToolParameterInfo;
import com.vanilo.psych.agent.tool.ToolExecutor;
import com.vanilo.psych.agent.tool.ToolRegistry;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ToolCallServiceTests {

    @Test
    void shouldWrapToolExecutionResult() {
        ToolCallService service = new ToolCallService(new ToolRegistry(List.of(new EchoTool())));

        ToolExecutionResponse response = service.call(new ToolCallRequest("echo_tool", Map.of("message", "hello")));

        assertEquals("echo_tool", response.getTool());
        assertTrue(response.isSuccess());
        assertEquals("hello", response.getResult());
        assertNotNull(response.getExecutedAt());
    }

    @Test
    void shouldRejectMissingRequiredParameter() {
        ToolCallService service = new ToolCallService(new ToolRegistry(List.of(new EchoTool())));

        RuntimeException error = assertThrows(RuntimeException.class,
                () -> service.call(new ToolCallRequest("echo_tool", Map.of())));

        assertTrue(error.getMessage().contains("message不能为空"));
    }

    @Test
    void shouldRejectWrongParameterType() {
        ToolCallService service = new ToolCallService(new ToolRegistry(List.of(new IntegerTool())));

        RuntimeException error = assertThrows(RuntimeException.class,
                () -> service.call(new ToolCallRequest("integer_tool", Map.of("limit", "3"))));

        assertTrue(error.getMessage().contains("limit类型错误"));
    }

    private static class EchoTool implements ToolExecutor {
        @Override
        public String getName() {
            return "echo_tool";
        }

        @Override
        public ToolInfoResponse getToolInfo() {
            return new ToolInfoResponse(
                    getName(),
                    "echo",
                    List.of(new ToolParameterInfo("message", "string", true, "message"))
            );
        }

        @Override
        public Object execute(Map<String, Object> arguments) {
            return arguments.get("message");
        }
    }

    private static class IntegerTool implements ToolExecutor {
        @Override
        public String getName() {
            return "integer_tool";
        }

        @Override
        public ToolInfoResponse getToolInfo() {
            return new ToolInfoResponse(
                    getName(),
                    "integer",
                    List.of(new ToolParameterInfo("limit", "integer", true, "limit"))
            );
        }

        @Override
        public Object execute(Map<String, Object> arguments) {
            return arguments.get("limit");
        }
    }
}
