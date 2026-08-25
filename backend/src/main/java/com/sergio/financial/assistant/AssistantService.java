package com.sergio.financial.assistant;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sergio.financial.error.AiUnavailableException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class AssistantService {
    private static final int MAX_TOOL_CALLS = 3;
    private static final int MAX_GROQ_REQUESTS = 4;
    private static final String SYSTEM_INSTRUCTIONS = """
            Você é o assistente do Financial Controller. Responda somente em português do Brasil.
            Use valores e fatos somente quando recebidos nas ferramentas. Não invente dados.
            Você só pode analisar; nunca afirma ter alterado transações, categorias ou orçamentos.
            Quando não houver dados suficientes, explique qual dado está faltando.
            Suas respostas são informativas e não constituem aconselhamento financeiro profissional.
            Para saudações, dicas gerais e explicações que não dependam dos dados da conta, responda diretamente em texto e não chame ferramentas.
            Use ferramentas somente para consultar fatos financeiros reais da conta selecionada.
            Responda em texto simples, com parágrafos e listas curtas quando necessário. Não use Markdown: não use **, tabelas com | ou blocos de código.
            Quando uma ferramenta fornecer um cartão visual, faça apenas uma síntese curta e não replique a tabela ou a lista completa de valores.
            """.stripTrailing();

    private final GroqClient groqClient;
    private final FinancialContextTools contextTools;
    private final ObjectMapper objectMapper;
    private final ArrayNode toolSchemas;

    public AssistantService(GroqClient groqClient, FinancialContextTools contextTools, ObjectMapper objectMapper) {
        this.groqClient = groqClient;
        this.contextTools = contextTools;
        this.objectMapper = objectMapper;
        this.toolSchemas = schemas();
    }

    public AssistantChatResponse answer(Long userId, AssistantChatRequest request) {
        List<ObjectNode> messages = initialMessages(request);
        int toolCallCount = 0;
        boolean transactionListUsed = false;
        ToolExecution lastTool = null;

        for (int requestNumber = 1; requestNumber <= MAX_GROQ_REQUESTS; requestNumber++) {
            ObjectNode response = groqClient.complete(List.copyOf(messages), toolSchemas.deepCopy());
            if (response == null) {
                throw new AiUnavailableException();
            }
            if (!"assistant".equals(response.path("role").asText())) {
                throw new AiUnavailableException();
            }
            JsonNode toolCalls = response.get("tool_calls");
            if (toolCalls != null && !toolCalls.isNull()) {
                if (!toolCalls.isArray()) {
                    throw new AiUnavailableException();
                }
                if (!toolCalls.isEmpty()) {
                    if (requestNumber == MAX_GROQ_REQUESTS) {
                        throw new AiUnavailableException();
                    }
                    ObjectNode assistantToolMessage = response.deepCopy();
                    assistantToolMessage.put("role", "assistant");
                    messages.add(assistantToolMessage);
                    for (JsonNode toolCall : toolCalls) {
                        if (toolCallCount >= MAX_TOOL_CALLS) {
                            throw new AiUnavailableException();
                        }
                        ToolExecution execution = executeTool(userId, request.month(), toolCall, messages,
                                transactionListUsed);
                        transactionListUsed = transactionListUsed || execution.listsTransactions();
                        lastTool = execution;
                        toolCallCount++;
                    }
                    continue;
                }
            }

            JsonNode content = response.get("content");
            if (content == null || !content.isTextual() || content.textValue().isBlank()) {
                throw new AiUnavailableException();
            }
            return response(content.textValue().trim(), request.month(), lastTool);
        }
        throw new AiUnavailableException();
    }

    private ToolExecution executeTool(Long userId, java.time.YearMonth selectedMonth, JsonNode toolCall,
                                List<ObjectNode> messages, boolean transactionListUsed) {
        if (toolCall == null || !toolCall.isObject()) {
            throw new AiUnavailableException();
        }
        if (!"function".equals(requiredText(toolCall.get("type")))) {
            throw new AiUnavailableException();
        }
        String id = requiredText(toolCall.get("id"));
        JsonNode function = toolCall.get("function");
        if (function == null || !function.isObject()) {
            throw new AiUnavailableException();
        }
        String name = requiredText(function.get("name"));
        boolean listsTransactions = "list_month_transactions".equals(name);
        if (listsTransactions && transactionListUsed) {
            throw new AiUnavailableException();
        }
        String rawArguments = requiredText(function.get("arguments"));
        try {
            JsonNode arguments = objectMapper.readTree(rawArguments);
            if (!arguments.isObject()) {
                throw new AiUnavailableException();
            }
            JsonNode result = contextTools.execute(userId, selectedMonth, name, arguments);
            ObjectNode toolMessage = objectMapper.createObjectNode();
            toolMessage.put("role", "tool");
            toolMessage.put("tool_call_id", id);
            toolMessage.put("content", objectMapper.writeValueAsString(result));
            messages.add(toolMessage);
            return new ToolExecution(listsTransactions, name, result);
        } catch (AiUnavailableException exception) {
            throw exception;
        } catch (JsonProcessingException exception) {
            throw new AiUnavailableException();
        }
    }

    private AssistantChatResponse response(String message, java.time.YearMonth month, ToolExecution tool) {
        if (tool == null) return new AssistantChatResponse(message, null, null);
        ObjectNode data = objectMapper.createObjectNode().put("month", month.toString());
        if ("get_monthly_budgets".equals(tool.name())) {
            ArrayNode categories = data.putArray("categories");
            for (JsonNode budget : tool.result()) {
                java.math.BigDecimal limit = budget.path("limit").decimalValue();
                java.math.BigDecimal spent = budget.path("spent").decimalValue();
                String status = limit == null || limit.signum() <= 0 ? "no_limit"
                        : spent == null || spent.signum() == 0 ? "no_spending"
                        : spent.compareTo(limit) > 0 ? "over_limit" : "within_limit";
                ObjectNode category = categories.addObject();
                category.put("name", budget.path("categoryName").asText());
                category.set("spent", budget.path("spent"));
                if (limit == null || limit.signum() <= 0) category.putNull("limit"); else category.set("limit", budget.path("limit"));
                category.put("status", status);
            }
            return new AssistantChatResponse(budgetSummaryMessage(tool.result()), "budget_summary", data);
        }
        if ("get_monthly_dashboard".equals(tool.name())) return new AssistantChatResponse(message, "monthly_summary", tool.result());
        if ("list_month_transactions".equals(tool.name())) return new AssistantChatResponse(message, "transactions_list", tool.result());
        return new AssistantChatResponse(message, null, null);
    }

    private String budgetSummaryMessage(JsonNode budgets) {
        List<String> exceededCategories = new ArrayList<>();
        for (JsonNode budget : budgets) {
            if (budget.path("exceeded").asBoolean()) {
                exceededCategories.add(budget.path("categoryName").asText());
            }
        }
        if (exceededCategories.isEmpty()) {
            return "Confira o resumo visual dos seus orçamentos abaixo. Nenhuma categoria ultrapassou o limite.";
        }
        if (exceededCategories.size() == 1) {
            return "Confira o resumo visual dos seus orçamentos abaixo. Há 1 categoria acima do limite: "
                    + exceededCategories.getFirst() + ".";
        }
        return "Confira o resumo visual dos seus orçamentos abaixo. Há " + exceededCategories.size()
                + " categorias acima do limite: " + String.join(", ", exceededCategories) + ".";
    }

    private record ToolExecution(boolean listsTransactions, String name, JsonNode result) {
    }

    private List<ObjectNode> initialMessages(AssistantChatRequest request) {
        if (request == null || request.message() == null || request.message().isBlank() || request.month() == null) {
            throw new AiUnavailableException();
        }
        List<ObjectNode> messages = new ArrayList<>();
        messages.add(message("system", SYSTEM_INSTRUCTIONS));
        if (request.history() != null) {
            for (AssistantHistoryMessage historyMessage : request.history()) {
                if (historyMessage == null || historyMessage.content() == null || historyMessage.content().isBlank()
                        || !("user".equals(historyMessage.role()) || "assistant".equals(historyMessage.role()))) {
                    throw new AiUnavailableException();
                }
                messages.add(message(historyMessage.role(), historyMessage.content()));
            }
        }
        messages.add(message("user", "Mês selecionado: " + request.month() + "\nPergunta: " + request.message()));
        return messages;
    }

    private ObjectNode message(String role, String content) {
        return objectMapper.createObjectNode().put("role", role).put("content", content);
    }

    private String requiredText(JsonNode value) {
        if (value == null || !value.isTextual() || value.textValue().isBlank()) {
            throw new AiUnavailableException();
        }
        return value.textValue();
    }

    private ArrayNode schemas() {
        ArrayNode tools = objectMapper.createArrayNode();
        tools.add(tool("get_monthly_dashboard", "Obtém o painel financeiro agregado do mês.",
                monthParameters()));
        tools.add(tool("get_monthly_budgets", "Lista os orçamentos por categoria do mês.",
                monthParameters()));

        ObjectNode transactionParameters = monthParameters();
        transactionParameters.withObject("properties").putObject("limit")
                .put("type", "integer").put("minimum", 1).put("maximum", 50);
        transactionParameters.withArray("required").add("limit");
        tools.add(tool("list_month_transactions", "Lista até 50 transações do mês.",
                transactionParameters));
        tools.add(tool("compare_monthly_totals", "Compara receitas e despesas com o mês anterior.",
                monthParameters()));
        return tools;
    }

    private ObjectNode tool(String name, String description, ObjectNode parameters) {
        ObjectNode tool = objectMapper.createObjectNode().put("type", "function");
        ObjectNode function = tool.putObject("function");
        function.put("name", name);
        function.put("description", description);
        function.set("parameters", parameters);
        return tool;
    }

    private ObjectNode monthParameters() {
        ObjectNode parameters = objectMapper.createObjectNode();
        parameters.put("type", "object");
        parameters.putObject("properties").putObject("month")
                .put("type", "string")
                .put("pattern", "^\\d{4}-(0[1-9]|1[0-2])$")
                .put("description", "Mês no formato YYYY-MM.");
        parameters.putArray("required").add("month");
        parameters.put("additionalProperties", false);
        return parameters;
    }
}
