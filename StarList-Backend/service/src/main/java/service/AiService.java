package service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openai.client.OpenAIClient;
import com.openai.core.JsonValue;
import com.openai.models.ChatModel;
import com.openai.models.FunctionDefinition;
import com.openai.models.FunctionParameters;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionAssistantMessageParam;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import com.openai.models.chat.completions.ChatCompletionMessage;
import com.openai.models.chat.completions.ChatCompletionMessageToolCall;
import com.openai.models.chat.completions.ChatCompletionTool;
import com.openai.models.chat.completions.ChatCompletionToolMessageParam;
import com.openai.models.chat.completions.ChatCompletionUserMessageParam;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import model.enums.ConversationType;
import model.enums.DifficultyLevel;
import model.enums.HabitFrequency;
import model.enums.TaskStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import repository.api.AiConversationRepository;
import repository.entity.AiConversationEntity;
import repository.entity.UserEntity;
import service.dto.AddHabitRequest;
import service.dto.AddTaskRequest;
import service.dto.AiChatRequest;
import service.dto.AiChatResponse;
import service.dto.AiConversationHistoryResponse;
import service.dto.UpdateHabitRequest;
import service.dto.UpdateTaskRequest;

@Slf4j
@Service
public class AiService {

    private static final int HISTORY_TURNS = 10;
    private static final int MAX_TOOL_ITERATIONS = 5;

    private final OpenAIClient openAIClient;
    private final TaskService taskService;
    private final HabitService habitService;
    private final UserService userService;
    private final AiConversationRepository aiConversationRepository;
    private final ObjectMapper objectMapper = new ObjectMapper()
            .findAndRegisterModules()
            .disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @Value("${openai.model:gpt-5-nano}")
    private String model;

    public AiService(OpenAIClient openAIClient, TaskService taskService, HabitService habitService,
                     UserService userService, AiConversationRepository aiConversationRepository) {
        this.openAIClient = openAIClient;
        this.taskService = taskService;
        this.habitService = habitService;
        this.userService = userService;
        this.aiConversationRepository = aiConversationRepository;
    }

    // ─────────────────────────── Tool argument POJOs ───────────────────────────

    static class CreateTaskArgs {
        public String title;
        public String description;
        public String difficultyLevel;
        public Integer durationMinutes;
        public String dueDate;
    }

    static class UpdateTaskArgs {
        public Long taskId;
        public String title;
        public String description;
        public String difficultyLevel;
        public Integer durationMinutes;
    }

    static class DeleteTaskArgs {
        public Long taskId;
    }

    static class CreateHabitArgs {
        public String title;
        public String description;
        public String frequency;
        public String difficultyLevel;
    }

    static class UpdateHabitArgs {
        public Long habitId;
        public String title;
        public String description;
        public String frequency;
        public String difficultyLevel;
    }

    static class DeleteHabitArgs {
        public Long habitId;
    }

    // ─────────────────────────── Tool definitions ───────────────────────────

    private static FunctionParameters noParams() {
        return FunctionParameters.builder()
                .putAdditionalProperty("type", JsonValue.from("object"))
                .putAdditionalProperty("properties", JsonValue.from(Map.of()))
                .build();
    }

    private static FunctionParameters params(Map<String, Object> properties, List<String> required) {
        return FunctionParameters.builder()
                .putAdditionalProperty("type", JsonValue.from("object"))
                .putAdditionalProperty("properties", JsonValue.from(properties))
                .putAdditionalProperty("required", JsonValue.from(required))
                .build();
    }

    private static ChatCompletionTool tool(String name, String description, FunctionParameters parameters) {
        return ChatCompletionTool.builder()
                .function(FunctionDefinition.builder()
                        .name(name)
                        .description(description)
                        .parameters(parameters)
                        .build())
                .build();
    }

    private static List<ChatCompletionTool> buildTools() {
        return List.of(
                tool("get_tasks", "Get all active (non-deleted) tasks for the user", noParams()),
                tool("get_habits", "Get all active habits for the user with streaks", noParams()),
                tool("get_user_stats", "Get the user's coin balance and lifetime stats", noParams()),
                tool("create_task", "Create a new task for the user",
                        params(Map.of(
                                "title", Map.of("type", "string", "description", "Task title (required)"),
                                "description", Map.of("type", "string", "description", "Optional description"),
                                "difficultyLevel", Map.of("type", "string",
                                        "description", "NONE, EASY, MEDIUM, HARD, VERY_HARD, or EXTREME"),
                                "durationMinutes", Map.of("type", "integer", "description", "Duration in minutes, positive"),
                                "dueDate", Map.of("type", "string", "description", "Local ISO-8601 datetime with the user's UTC offset (e.g. 2026-05-09T21:35:00+03:00). Must be in the future in the user's local time.")
                        ), List.of("title", "difficultyLevel"))),
                tool("update_task", "Update an existing task's fields",
                        params(Map.of(
                                "taskId", Map.of("type", "integer", "description", "ID of the task to update"),
                                "title", Map.of("type", "string"),
                                "description", Map.of("type", "string"),
                                "difficultyLevel", Map.of("type", "string"),
                                "durationMinutes", Map.of("type", "integer")
                        ), List.of("taskId"))),
                tool("delete_task", "Soft-delete a task by ID",
                        params(Map.of("taskId", Map.of("type", "integer", "description", "ID to delete")),
                                List.of("taskId"))),
                tool("create_habit", "Create a new habit for the user",
                        params(Map.of(
                                "title", Map.of("type", "string", "description", "Habit title"),
                                "description", Map.of("type", "string"),
                                "frequency", Map.of("type", "string", "description", "DAILY, WEEKLY, or CUSTOM"),
                                "difficultyLevel", Map.of("type", "string", "description", "NONE, EASY, MEDIUM, HARD, VERY_HARD, or EXTREME")
                        ), List.of("title", "frequency", "difficultyLevel"))),
                tool("update_habit", "Update an existing habit's fields",
                        params(Map.of(
                                "habitId", Map.of("type", "integer", "description", "ID of the habit to update"),
                                "title", Map.of("type", "string"),
                                "description", Map.of("type", "string"),
                                "frequency", Map.of("type", "string"),
                                "difficultyLevel", Map.of("type", "string")
                        ), List.of("habitId"))),
                tool("delete_habit", "Soft-delete a habit by ID",
                        params(Map.of("habitId", Map.of("type", "integer", "description", "ID to delete")),
                                List.of("habitId")))
        );
    }

    // ─────────────────────────── Public API ───────────────────────────

    /**
     * Handles a user chat message: builds context, runs the agentic tool-call loop,
     * persists the turn, and returns the AI's final response.
     */
    @Transactional
    public AiChatResponse chat(Long userId, AiChatRequest request) {
        log.info("AI chat for user {} (newConversation={})", userId, request.newConversation());

        UserEntity userEntity = userService.findEntityById(userId);
        var tasks = taskService.getUserTasks(userId);
        var habits = habitService.getUserHabits(userId, YearMonth.now(), request.userTimezone());

        ChatCompletionCreateParams.Builder paramsBuilder = ChatCompletionCreateParams.builder()
                .model(ChatModel.of(model))
                .maxCompletionTokens(1024)
                .addSystemMessage(buildSystemPrompt(userEntity, tasks, habits, request.userTimezone()))
                .tools(buildTools());

        if (!Boolean.TRUE.equals(request.newConversation())) {
            appendHistory(paramsBuilder, userId);
        }
        paramsBuilder.addUserMessage(request.message());

        List<Long> createdTaskIds = new ArrayList<>();
        List<Long> createdHabitIds = new ArrayList<>();
        Set<String> toolsUsed = new HashSet<>();
        String aiMessage = runAgenticLoop(paramsBuilder, userId, userEntity, createdTaskIds, createdHabitIds,
                toolsUsed, request.userTimezone());

        ConversationType conversationType = classifyConversation(toolsUsed);

        AiConversationEntity saved = aiConversationRepository.save(
                AiConversationEntity.builder()
                        .user(userEntity)
                        .userMessage(request.message())
                        .aiResponse(aiMessage)
                        .conversationType(conversationType)
                        .tasksCreated(createdTaskIds.size())
                        .build());

        createdTaskIds.forEach(taskId -> taskService.linkToAiConversation(taskId, saved));

        log.info("AI chat done for user {}: type={}, tasksCreated={}, habitsCreated={}", userId, conversationType, createdTaskIds.size(), createdHabitIds.size());
        return AiChatResponse.builder()
                .conversationId(saved.getId())
                .aiMessage(aiMessage)
                .conversationType(conversationType)
                .tasksCreated(createdTaskIds.size())
                .habitsCreated(createdHabitIds.size())
                .createdAt(saved.getCreatedAt())
                .build();
    }

    /** Returns a short daily briefing of tasks due soon, habits to complete, and a motivational note. */
    @Transactional
    public AiChatResponse getDailyBriefing(Long userId) {
        log.info("Daily briefing for user {}", userId);
        return chat(userId, AiChatRequest.builder()
                .message("Give me a concise daily briefing: list tasks due today or tomorrow, "
                        + "habits I still need to complete today, and one short motivational note. "
                        + "Keep it under 150 words.")
                .newConversation(false)
                .build());
    }

    /** Returns the full conversation history for a user, newest-first. */
    @Transactional(readOnly = true)
    public List<AiConversationHistoryResponse> getHistory(Long userId) {
        log.info("Fetching AI history for user {}", userId);
        return aiConversationRepository.findAllByUser_IdOrderByCreatedAtDesc(userId)
                .stream()
                .map(AiConversationHistoryResponse::from)
                .toList();
    }

    // ─────────────────────────── Private helpers ───────────────────────────

    private String buildSystemPrompt(UserEntity user,
                                     List<service.dto.TaskResponse> taskList,
                                     List<service.dto.HabitResponse> habitList,
                                     String userTimezone) {
        java.time.ZoneId zone = ZoneResolver.resolve(userTimezone);
        java.time.ZonedDateTime nowLocal = java.time.ZonedDateTime.now(zone);
        java.time.ZonedDateTime nowUtc = nowLocal.withZoneSameInstant(ZoneOffset.UTC);
        LocalDate today = nowLocal.toLocalDate();

        StringBuilder sb = new StringBuilder();
        sb.append("You are a personal productivity assistant for StarList, a gamified task and habit tracker.\n");
        String offsetStr = nowLocal.getOffset().getId(); // e.g. "+03:00"
        sb.append("User's local time: ").append(nowLocal.toLocalTime().withSecond(0).withNano(0))
                .append(" (").append(zone).append(", UTC").append(offsetStr).append("). ")
                .append("UTC now: ").append(nowUtc.toLocalTime().withSecond(0).withNano(0)).append(" UTC.\n");
        sb.append("When the user mentions a time, interpret it in their local timezone. ")
                .append("For dueDate, send a local ISO-8601 string with the user's offset (e.g. 2026-05-09T21:35:00").append(offsetStr).append("). ")
                .append("Validate that the time is in the future relative to the user's current local time above.\n\n");
        sb.append("Today is ").append(today).append(". User: ").append(user.getDisplayName())
                .append(". Coins: ").append(user.getTotalCoins())
                .append(", Lifetime earned: ").append(user.getLifetimeCoinsEarned()).append(".\n\n");

        sb.append("=== TASKS ===\n");
        if (taskList.isEmpty()) {
            sb.append("No active tasks.\n");
        } else {
            taskList.forEach(t -> sb.append(String.format(
                    "- [id:%d] \"%s\" | %s | %s | due:%s\n",
                    t.taskId(), t.title(), t.status(), t.difficultyLevel(),
                    t.dueDate() != null ? t.dueDate() : "none")));
        }

        sb.append("\n=== HABITS ===\n");
        if (habitList.isEmpty()) {
            sb.append("No active habits.\n");
        } else {
            habitList.forEach(h -> sb.append(String.format(
                    "- [id:%d] \"%s\" | %s | streak:%d | best:%d | lastDone:%s\n",
                    h.habitId(), h.title(), h.frequency(), h.currentStreak(), h.bestStreak(),
                    h.lastCompletedDate() != null ? h.lastCompletedDate() : "never")));
        }

        List<String> notices = buildCoachNotices(taskList, habitList, today);
        if (!notices.isEmpty()) {
            sb.append("\n=== COACH NOTICES ===\n");
            notices.forEach(n -> sb.append("⚠ ").append(n).append("\n"));
        }

        sb.append("\n=== INSTRUCTIONS ===\n");
        sb.append("Help manage tasks and habits. Suggest appropriate difficulty and duration when creating tasks. Be concise and encouraging.\n");
        sb.append("IMPORTANT: Before calling any create, update, or delete tool, always describe what you are about to do in plain, friendly language and ask the user to confirm. ")
                .append("Never show ISO dates, UTC offsets, or timezone codes to the user — use natural phrasing like 'tonight at 10pm' or 'tomorrow morning'. ")
                .append("Example: \"Got it! Shall I add a task 'Go to bed' due tonight at 22:00?\" — only call the tool after the user explicitly approves.");

        return sb.toString();
    }

    private List<String> buildCoachNotices(List<service.dto.TaskResponse> tasks,
                                           List<service.dto.HabitResponse> habits, LocalDate today) {
        List<String> notices = new ArrayList<>();

        tasks.stream()
                .filter(t -> t.status() == TaskStatus.PENDING
                        && t.dueDate() != null
                        && t.dueDate().isBefore(Instant.now()))
                .forEach(t -> notices.add("Task \"" + t.title() + "\" is overdue!"));

        habits.stream()
                .filter(h -> h.frequency() == HabitFrequency.DAILY
                        && Boolean.TRUE.equals(h.isActive())
                        && (h.lastCompletedDate() == null || h.lastCompletedDate().isBefore(today)))
                .forEach(h -> notices.add("Daily habit \"" + h.title()
                        + "\" hasn't been done today (streak: " + h.currentStreak() + ")"));

        return notices;
    }

    private void appendHistory(ChatCompletionCreateParams.Builder paramsBuilder, Long userId) {
        List<AiConversationEntity> history = aiConversationRepository
                .findAllByUser_IdOrderByCreatedAtDesc(userId, PageRequest.of(0, HISTORY_TURNS));
        Collections.reverse(history);
        history.stream()
                .filter(turn -> !turn.getAiResponse().isBlank())
                .forEach(turn -> {
                    paramsBuilder.addMessage(ChatCompletionUserMessageParam.builder()
                            .content(turn.getUserMessage()).build());
                    paramsBuilder.addMessage(ChatCompletionAssistantMessageParam.builder()
                            .content(turn.getAiResponse()).build());
                });
    }

    private String runAgenticLoop(ChatCompletionCreateParams.Builder paramsBuilder,
                                  Long userId,
                                  UserEntity userEntity,
                                  List<Long> createdTaskIds,
                                  List<Long> createdHabitIds,
                                  Set<String> toolsUsed,
                                  String userTimezone) {
        String finalResponse = "I'm sorry, I couldn't process your request. Please try again.";

        for (int iteration = 0; iteration < MAX_TOOL_ITERATIONS; iteration++) {
            ChatCompletion completion = openAIClient.chat().completions()
                    .create(paramsBuilder.build());

            ChatCompletionMessage assistantMessage = completion.choices().get(0).message();
            paramsBuilder.addMessage(assistantMessage);

            List<ChatCompletionMessageToolCall> toolCalls = assistantMessage.toolCalls()
                    .stream().flatMap(Collection::stream).toList();

            if (toolCalls.isEmpty()) {
                log.info("AI final response: finishReason={}, contentPresent={}, content='{}'",
                        completion.choices().get(0).finishReason(),
                        assistantMessage.content().isPresent(),
                        assistantMessage.content().orElse("<empty>"));
                finalResponse = assistantMessage.content()
                        .filter(s -> !s.isBlank())
                        .orElse(finalResponse);
                break;
            }

            if (iteration == MAX_TOOL_ITERATIONS - 1) {
                log.warn("AI tool-call loop hit max iterations ({}) for user {}", MAX_TOOL_ITERATIONS, userId);
            }

            for (ChatCompletionMessageToolCall toolCall : toolCalls) {
                String toolName = toolCall.function().name();
                toolsUsed.add(toolName);
                log.debug("Executing AI tool: {}", toolName);

                Object result = dispatchTool(toolName, toolCall.function(), userId,
                        userEntity, createdTaskIds, createdHabitIds, userTimezone);

                String resultJson;
                try {
                    resultJson = objectMapper.writeValueAsString(result);
                } catch (JsonProcessingException e) {
                    log.error("Failed to serialize tool result for {}", toolName, e);
                    resultJson = "{\"error\":\"serialization failed\"}";
                }

                paramsBuilder.addMessage(ChatCompletionToolMessageParam.builder()
                        .toolCallId(toolCall.id())
                        .content(resultJson)
                        .build());
            }
        }

        return finalResponse;
    }

    private Object dispatchTool(String name,
                                ChatCompletionMessageToolCall.Function fn,
                                Long userId,
                                UserEntity userEntity,
                                List<Long> createdTaskIds,
                                List<Long> createdHabitIds,
                                String userTimezone) {
        try {
            return switch (name) {
                case "get_tasks" -> taskService.getUserTasks(userId);
                case "get_habits" -> habitService.getUserHabits(userId, YearMonth.now(), userTimezone);
                case "get_user_stats" -> Map.of(
                        "displayName", userEntity.getDisplayName(),
                        "totalCoins", userEntity.getTotalCoins(),
                        "lifetimeCoinsEarned", userEntity.getLifetimeCoinsEarned());
                case "create_task" -> {
                    var args = objectMapper.readValue(fn.arguments(), CreateTaskArgs.class);
                    var req = AddTaskRequest.builder()
                            .title(args.title)
                            .description(args.description)
                            .difficultyLevel(parseDifficulty(args.difficultyLevel))
                            .durationMinutes(args.durationMinutes)
                            .dueDate(args.dueDate != null ? java.time.OffsetDateTime.parse(args.dueDate).toInstant() : null)
                            .build();
                    var created = taskService.addTask(userId, req);
                    createdTaskIds.add(created.taskId());
                    yield created;
                }
                case "update_task" -> {
                    var args = objectMapper.readValue(fn.arguments(), UpdateTaskArgs.class);
                    yield taskService.updateTask(args.taskId, UpdateTaskRequest.builder()
                            .title(args.title)
                            .description(args.description)
                            .difficultyLevel(args.difficultyLevel != null ? parseDifficulty(args.difficultyLevel) : null)
                            .durationMinutes(args.durationMinutes)
                            .build());
                }
                case "delete_task" -> {
                    var args = objectMapper.readValue(fn.arguments(), DeleteTaskArgs.class);
                    taskService.deleteTask(args.taskId);
                    yield Map.of("deleted", true, "taskId", args.taskId);
                }
                case "create_habit" -> {
                    var args = objectMapper.readValue(fn.arguments(), CreateHabitArgs.class);
                    var created = habitService.addHabit(userId, AddHabitRequest.builder()
                            .title(args.title)
                            .description(args.description)
                            .frequency(HabitFrequency.valueOf(args.frequency.toUpperCase()))
                            .difficultyLevel(parseDifficulty(args.difficultyLevel))
                            .build());
                    createdHabitIds.add(created.habitId());
                    yield created;
                }
                case "update_habit" -> {
                    var args = objectMapper.readValue(fn.arguments(), UpdateHabitArgs.class);
                    yield habitService.updateHabit(args.habitId, UpdateHabitRequest.builder()
                            .title(args.title)
                            .description(args.description)
                            .frequency(args.frequency != null ? HabitFrequency.valueOf(args.frequency.toUpperCase()) : null)
                            .difficultyLevel(args.difficultyLevel != null ? parseDifficulty(args.difficultyLevel) : null)
                            .build());
                }
                case "delete_habit" -> {
                    var args = objectMapper.readValue(fn.arguments(), DeleteHabitArgs.class);
                    habitService.deleteHabit(args.habitId);
                    yield Map.of("deleted", true, "habitId", args.habitId);
                }
                default -> {
                    log.warn("Unknown AI tool: {}", name);
                    yield Map.of("error", "Unknown tool: " + name);
                }
            };
        } catch (Exception e) {
            log.error("Tool '{}' execution failed", name, e);
            return Map.of("error", e.getMessage());
        }
    }

    private ConversationType classifyConversation(Set<String> toolsUsed) {
        if (toolsUsed.contains("create_task") || toolsUsed.contains("update_task") || toolsUsed.contains("delete_task")) {
            return ConversationType.TASK_CREATION;
        }
        if (toolsUsed.contains("create_habit") || toolsUsed.contains("update_habit") || toolsUsed.contains("delete_habit")) {
            return ConversationType.HABIT_SUGGESTION;
        }
        if (toolsUsed.contains("get_user_stats")) {
            return ConversationType.STUDY_PLAN;
        }
        return ConversationType.GENERAL_CHAT;
    }

    private DifficultyLevel parseDifficulty(String value) {
        if (value == null) return DifficultyLevel.NONE;
        try {
            return DifficultyLevel.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("Unknown difficulty '{}', defaulting to MEDIUM", value);
            return DifficultyLevel.MEDIUM;
        }
    }
}
