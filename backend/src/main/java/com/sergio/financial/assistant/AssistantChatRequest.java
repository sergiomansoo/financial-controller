package com.sergio.financial.assistant;

import java.time.YearMonth;
import java.util.List;

public record AssistantChatRequest(String message, YearMonth month, List<AssistantHistoryMessage> history) {
}
