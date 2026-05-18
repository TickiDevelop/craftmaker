package com.example.craftmaker.client.gui;

import com.example.craftmaker.network.CraftMakerPackets;
import com.example.craftmaker.recipe.RecipeFileManager;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Экран списка рецептов с улучшенным визуальным поиском.
 * Отображает все рецепты из crafts/add/ и crafts/remove/ с кнопками удаления.
 */
public class RecipeListScreen extends Screen {
    private final Screen parent;
    private List<RecipeFileManager.RecipeFile> addRecipes;
    private List<RecipeFileManager.RecipeFile> removeRecipes;
    private EditBox searchField;
    private String searchText = "";
    private int scrollOffset = 0;
    private int itemsPerPage = 10;
    private int columns = 1;
    private int rowSpacing = 28;
    private int columnSpacing = 240;
    private List<RecipeEntry> filteredRecipes;


    // Цветовая схема (минимализм, темная тема с прозрачностью)
    private static final int SEARCH_BAR_BG = 0xCC1A1A1A;
    private static final int SEARCH_BAR_BORDER = 0x80333333;
    private static final int SEARCH_BAR_BORDER_FOCUSED = 0xCC6366F1;
    private static final int TEXT_COLOR = 0xFFFFFFFF;
    private static final int TEXT_MUTED = 0x80FFFFFF;

    public RecipeListScreen(Screen parent) {
        super(Component.translatable("gui.craftmaker.recipe_list.title"));
        this.parent = parent;
        loadRecipes();
        updateFilteredRecipes();
    }

    private void loadRecipes() {
        // Проверяем, находимся ли мы на сервере
        boolean isRemoteServer = this.minecraft != null && 
                              this.minecraft.player != null && 
                              this.minecraft.player.connection != null && 
                              !this.minecraft.isSingleplayer();
        
        if (isRemoteServer) {
            // На сервере НЕ загружаем локальные рецепты, только отправляем запрос
            addRecipes = new ArrayList<>();
            CraftMakerPackets.sendRequestRecipesToServer();
        } else {
            // В одиночной игре загружаем локальные рецепты
            addRecipes = RecipeFileManager.loadAllRecipes();
        }
        
        removeRecipes = new ArrayList<>(); // Убрана подпапка remove
    }

    /** Публичный метод для перезагрузки списка рецептов (вызывается из ScreenEventHandler) */
    public void reload() {
        loadRecipes();
        updateFilteredRecipes();
        this.clearWidgets();
        this.init();
    }

    /** Метод для установки рецептов, полученных от сервера */
    public void setServerRecipes(List<RecipeFileManager.RecipeFile> recipes) {
        addRecipes = recipes;
        updateFilteredRecipes();
        this.clearWidgets();
        this.init();
    }

    private void updateFilteredRecipes() {
        if (filteredRecipes == null) {
            filteredRecipes = new ArrayList<>();
        }
        filteredRecipes = getFilteredRecipes();
    }

    private void calculateLayout() {
        // Рассчитываем количество колонок на основе ширины экрана
        int availableWidth = this.width - 50;
        columns = Math.max(1, availableWidth / columnSpacing);

        // Рассчитываем количество строк на основе высоты экрана
        int startY = 100;
        int bottomY = this.height - 60;
        int availableHeight = bottomY - startY;
        int rows = Math.max(1, availableHeight / rowSpacing);

        itemsPerPage = columns * rows;

        // Корректируем страницу если вышли за пределы
        int totalItems = filteredRecipes.size();
        int maxPage = Math.max(0, (totalItems - 1) / itemsPerPage);
        if (scrollOffset > maxPage) {
            scrollOffset = maxPage;
        }
    }

    @Override
    protected void init() {
        // Убедимся, что filteredRecipes инициализирован
        if (filteredRecipes == null) {
            updateFilteredRecipes();
        }
        calculateLayout();

        int centerX = this.width / 2;
        int startY = 90;

        // Поисковая строка и кнопки
        int searchBarWidth = 210;
        int searchBarHeight = 20;
        int buttonWidth = 70;
        int buttonSpacing = 8;
        int textFieldWidth = searchBarWidth - 20;
        int searchBarY = 39;
        int buttonsY = 33;

        // Инициализация поля поиска
        if (searchField == null) {
            searchField = new EditBox(this.font, centerX - textFieldWidth / 2, searchBarY, textFieldWidth, searchBarHeight, Component.literal(""));
            searchField.setMaxLength(128);
            searchField.setValue(searchText);
            searchField.setResponder(value -> {
                searchText = value;
                scrollOffset = 0;
                updateFilteredRecipes();
                this.clearWidgets();
                this.init();
            });
            searchField.setBordered(false);
            searchField.setTextColor(TEXT_COLOR);
            searchField.setTextColorUneditable(TEXT_MUTED);
            searchField.setHint(Component.literal("Search..."));
        } else {
            searchField.setPosition(centerX - textFieldWidth / 2, searchBarY);
            searchField.setWidth(textFieldWidth);
            searchField.setHeight(searchBarHeight);
        }
        this.addRenderableWidget(searchField);

        // Кнопки Refresh и Back
        int refreshX = centerX - searchBarWidth / 2 - buttonWidth - buttonSpacing;
        int backX = centerX + searchBarWidth / 2 + buttonSpacing;
        
        this.addRenderableWidget(Button.builder(Component.translatable("gui.craftmaker.refresh"), button -> {
            this.minecraft.player.connection.sendCommand("cm reload");
            loadRecipes();
            updateFilteredRecipes();
            this.clearWidgets();
            this.init();
        }).bounds(refreshX, buttonsY, buttonWidth, searchBarHeight).build());

        this.addRenderableWidget(Button.builder(Component.translatable("gui.craftmaker.back"), button -> {
            this.minecraft.setScreen(parent);
        }).bounds(backX, buttonsY, buttonWidth, searchBarHeight).build());

        // Компактные кнопки прокрутки
        this.addRenderableWidget(Button.builder(Component.literal("◀"), button -> {
            if (scrollOffset > 0) {
                scrollOffset--;
                this.clearWidgets();
                this.init();
            }
        }).bounds(centerX - 60, this.height - 40, 30, 22).build());

        this.addRenderableWidget(Button.builder(Component.literal("▶"), button -> {
            int totalItems = filteredRecipes.size();
            int maxPage = Math.max(0, (totalItems - 1) / itemsPerPage);
            if (scrollOffset < maxPage) {
                scrollOffset++;
                this.clearWidgets();
                this.init();
            }
        }).bounds(centerX + 30, this.height - 40, 30, 22).build());

        // Отображаем рецепты
        displayRecipes(startY);
    }

    private void displayRecipes(int startY) {
        int centerX = this.width / 2;
        int totalItems = filteredRecipes.size();

        int startIndex = scrollOffset * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, totalItems);

        int row = 0;
        int col = 0;

        for (int i = startIndex; i < endIndex; i++) {
            RecipeEntry entry = filteredRecipes.get(i);
            String displayName = entry.file.filename;

            // Вычисляем позицию в сетке (смещение влево относительно центра)
            int xOffset = (int) ((col - (columns - 1) / 2.0) * columnSpacing);
            int x = centerX + xOffset - 80;
            int y = startY + row * rowSpacing;

            // Кнопка с именем файла
            String buttonLabel = displayName;

            // Add relevance indicator if search is active
            Component buttonComponent = Component.literal(buttonLabel);
            if (!searchText.isEmpty() && entry.relevanceScore() > 0) {
                int matchPercent = (int) Math.min(100, entry.relevanceScore() * 20);
                buttonComponent = Component.literal(String.format("[%d%%] ", matchPercent) + buttonLabel);
            }

            this.addRenderableWidget(Button.builder(buttonComponent, button -> {
                if (entry.isAdd) {
                    this.minecraft.setScreen(new AddRecipeCustomScreen(this.minecraft.player.getInventory(), this, entry.file));
                }
            }).bounds(x, y, 140, 20).build());

            // Кнопка удаления (привязана к кнопке рецепта)
            final String filename = entry.file.filename;
            this.addRenderableWidget(Button.builder(Component.literal("✕"), button -> {
                // Проверяем, находимся ли мы на сервере
                boolean isRemoteServer = this.minecraft != null && 
                                      this.minecraft.player != null && 
                                      this.minecraft.player.connection != null && 
                                      !this.minecraft.isSingleplayer();
                
                if (isRemoteServer) {
                    // Отправляем сетевой пакет на сервер для удаления
                    CraftMakerPackets.sendDeleteRecipeToServer(filename);
                } else {
                    // Локальное удаление для одиночной игры
                    RecipeFileManager.deleteRecipe(filename);
                    loadRecipes();
                    updateFilteredRecipes();
                }
                this.clearWidgets();
                this.init();
            }).bounds(x + 145, y, 24, 20).build());

            col++;
            if (col >= columns) {
                col = 0;
                row++;
            }
        }
    }

    private List<RecipeEntry> getFilteredRecipes() {
        List<RecipeEntry> entries = new ArrayList<>();
        String query = searchText.trim().toLowerCase(Locale.ROOT);

        if (query.isEmpty()) {
            // Return all recipes when no search query
            for (RecipeFileManager.RecipeFile recipe : addRecipes) {
                entries.add(new RecipeEntry(recipe, true, 0.0));
            }
            for (RecipeFileManager.RecipeFile recipe : removeRecipes) {
                entries.add(new RecipeEntry(recipe, false, 0.0));
            }
        } else {
            // Score and filter recipes
            List<ScoredEntry> scoredEntries = new ArrayList<>();

            for (RecipeFileManager.RecipeFile recipe : addRecipes) {
                double score = calculateSearchScore(recipe, true, query);
                if (score > 0) {
                    scoredEntries.add(new ScoredEntry(recipe, true, score));
                }
            }
            for (RecipeFileManager.RecipeFile recipe : removeRecipes) {
                double score = calculateSearchScore(recipe, false, query);
                if (score > 0) {
                    scoredEntries.add(new ScoredEntry(recipe, false, score));
                }
            }

            // Sort by relevance score (descending)
            scoredEntries.sort((a, b) -> Double.compare(b.score, a.score));

            // Convert to RecipeEntry
            for (ScoredEntry scored : scoredEntries) {
                entries.add(new RecipeEntry(scored.file, scored.isAdd, scored.score));
            }
        }

        return entries;
    }

    /**
     * Advanced search scoring algorithm with fuzzy matching and field weighting
     */
    private double calculateSearchScore(RecipeFileManager.RecipeFile recipe, boolean isAdd, String query) {
        String[] queryWords = query.split("\\s+");
        if (queryWords.length == 0) return 0.0;

        double totalScore = 0.0;

        // Field weights (higher = more important)
        final double FILENAME_WEIGHT = 2.0;
        final double RECIPE_ID_WEIGHT = 1.5;
        final double RESULT_WEIGHT = 1.8;
        final double INGREDIENT_WEIGHT = 1.2;

        // Search in filename (highest priority)
        String filename = recipe.filename.toLowerCase(Locale.ROOT);
        double filenameScore = calculateFieldScore(filename, queryWords, true);
        totalScore += filenameScore * FILENAME_WEIGHT;

        // Search in recipe_id (for remove recipes)
        if (!isAdd && recipe.json.has("recipe_id")) {
            String recipeId = recipe.json.get("recipe_id").getAsString().toLowerCase(Locale.ROOT);
            double recipeIdScore = calculateFieldScore(recipeId, queryWords, false);
            totalScore += recipeIdScore * RECIPE_ID_WEIGHT;
        }

        // Search in result (for add recipes)
        if (isAdd && recipe.json.has("result")) {
            var result = recipe.json.getAsJsonObject("result");
            String resultId = result.has("id") ? result.get("id").getAsString()
                    : result.has("item") ? result.get("item").getAsString() : "";
            if (!resultId.isEmpty()) {
                String resultIdLower = resultId.toLowerCase(Locale.ROOT);
                double resultScore = calculateFieldScore(resultIdLower, queryWords, false);
                totalScore += resultScore * RESULT_WEIGHT;
            }
        }

        // Search in ingredients (for add recipes)
        if (isAdd && recipe.json.has("key")) {
            var key = recipe.json.getAsJsonObject("key");
            double maxIngredientScore = 0.0;
            for (var entry : key.entrySet()) {
                var ingredient = entry.getValue().getAsJsonObject();
                String itemId = ingredient.has("item") ? ingredient.get("item").getAsString() : "";
                if (!itemId.isEmpty()) {
                    String itemIdLower = itemId.toLowerCase(Locale.ROOT);
                    double ingredientScore = calculateFieldScore(itemIdLower, queryWords, false);
                    maxIngredientScore = Math.max(maxIngredientScore, ingredientScore);
                }
            }
            totalScore += maxIngredientScore * INGREDIENT_WEIGHT;
        }

        return totalScore;
    }

    /**
     * Calculate score for a single field with fuzzy matching
     */
    private double calculateFieldScore(String field, String[] queryWords, boolean isFilename) {
        if (field.isEmpty()) return 0.0;

        double fieldScore = 0.0;
        int matchedWords = 0;

        for (String word : queryWords) {
            if (word.isEmpty()) continue;

            // Exact match bonus
            if (field.equals(word)) {
                fieldScore += 3.0;
                matchedWords++;
                continue;
            }

            // Starts with match
            if (field.startsWith(word)) {
                fieldScore += 2.0;
                matchedWords++;
                continue;
            }

            // Contains match
            if (field.contains(word)) {
                fieldScore += 1.0;
                matchedWords++;
                continue;
            }

            // Fuzzy match (Levenshtein distance)
            double fuzzyScore = calculateFuzzyScore(field, word);
            if (fuzzyScore > 0.5) {
                fieldScore += fuzzyScore;
                matchedWords++;
            }
        }

        // Bonus for matching all words
        if (matchedWords == queryWords.length && queryWords.length > 1) {
            fieldScore *= 1.5;
        }

        // Bonus for filename exact matches
        if (isFilename && matchedWords == queryWords.length) {
            fieldScore *= 1.2;
        }

        return fieldScore;
    }

    /**
     * Calculate fuzzy match score using Levenshtein distance
     * Returns a score between 0.0 (no match) and 1.0 (perfect match)
     */
    private double calculateFuzzyScore(String text, String pattern) {
        if (pattern.length() == 0) return 0.0;
        if (text.length() == 0) return 0.0;

        // If pattern is much longer than text, unlikely to match
        if (pattern.length() > text.length() * 2) return 0.0;

        int distance = levenshteinDistance(text, pattern);
        int maxLength = Math.max(text.length(), pattern.length());

        // Convert distance to similarity score (0.0 to 1.0)
        double similarity = 1.0 - ((double) distance / maxLength);

        // Only return score if similarity is above threshold
        return similarity >= 0.6 ? similarity : 0.0;
    }

    /**
     * Calculate Levenshtein distance between two strings
     */
    private int levenshteinDistance(String a, String b) {
        int[][] dp = new int[a.length() + 1][b.length() + 1];

        for (int i = 0; i <= a.length(); i++) {
            dp[i][0] = i;
        }
        for (int j = 0; j <= b.length(); j++) {
            dp[0][j] = j;
        }

        for (int i = 1; i <= a.length(); i++) {
            for (int j = 1; j <= b.length(); j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                dp[i][j] = Math.min(
                    Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                    dp[i - 1][j - 1] + cost
                );
            }
        }

        return dp[a.length()][b.length()];
    }

    @Override
    public void render(net.minecraft.client.gui.GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Заголовок
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 12, TEXT_COLOR);

        // Минималистичная поисковая строка с прозрачностью
        int searchBarWidth = 210;
        int searchBarHeight = 20;
        int searchBarX = this.width / 2 - searchBarWidth / 2;
        int searchBarY = 33;
        boolean isFocused = searchField != null && searchField.isFocused();

        int borderColor = isFocused ? SEARCH_BAR_BORDER_FOCUSED : SEARCH_BAR_BORDER;

        // Полупрозрачный фон
        graphics.fill(searchBarX, searchBarY, searchBarX + searchBarWidth, searchBarY + searchBarHeight, SEARCH_BAR_BG);

        // Тонкая рамка
        graphics.fill(searchBarX, searchBarY, searchBarX + searchBarWidth, searchBarY + 1, borderColor);
        graphics.fill(searchBarX, searchBarY + searchBarHeight - 1, searchBarX + searchBarWidth, searchBarY + searchBarHeight, borderColor);
        graphics.fill(searchBarX, searchBarY, searchBarX + 1, searchBarY + searchBarHeight, borderColor);
        graphics.fill(searchBarX + searchBarWidth - 1, searchBarY, searchBarX + searchBarWidth, searchBarY + searchBarHeight, borderColor);

        // Статистика поиска (компактная)
        int totalItems = addRecipes.size() + removeRecipes.size();
        int filteredItems = filteredRecipes.size();
        String statsText;
        if (searchText.isEmpty()) {
            statsText = String.format("Total: %d", totalItems);
        } else {
            statsText = String.format("%d/%d", filteredItems, totalItems);
        }
        graphics.drawCenteredString(this.font, Component.literal(statsText), this.width / 2, 65, TEXT_MUTED);

        // Информация о страницах
        int totalPages = (filteredItems + itemsPerPage - 1) / itemsPerPage;
        String pageInfo = String.format("%d/%d", scrollOffset + 1, Math.max(1, totalPages));
        graphics.drawCenteredString(this.font, Component.literal(pageInfo), this.width / 2, this.height - 35, TEXT_COLOR);

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
        calculateLayout();
        this.clearWidgets();
        this.init();
    }
    
    @Override
    public void removed() {
        super.removed();
    }
    
    @Override
    public boolean isPauseScreen() {
        return true;
    }

    private record RecipeEntry(RecipeFileManager.RecipeFile file, boolean isAdd, double relevanceScore) {}

    private record ScoredEntry(RecipeFileManager.RecipeFile file, boolean isAdd, double score) {}
}