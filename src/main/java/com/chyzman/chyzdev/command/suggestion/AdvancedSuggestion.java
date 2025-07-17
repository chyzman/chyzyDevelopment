package com.chyzman.chyzdev.command.suggestion;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.mojang.brigadier.Message;
import com.mojang.brigadier.context.StringRange;
import com.mojang.brigadier.suggestion.Suggestion;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static net.minecraft.command.CommandSource.shouldSuggest;

public class AdvancedSuggestion extends Suggestion {
    private final String completion;
    private final List<String> aliases;

    public AdvancedSuggestion(
        StringRange range,
        String display,
        String completion,
        List<String> aliases,
        @Nullable Message tooltip
    ) {
        super(range, display, tooltip);
        this.completion = completion;
        this.aliases = aliases;
    }

    public AdvancedSuggestion(StringRange range, String display, String completion, List<String> aliases) {
        this(range, display, completion, aliases, null);
    }

    public String getCompletion() {
        return completion;
    }

    public List<String> getAliases() {
        return aliases;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AdvancedSuggestion that)) return false;
        if (!super.equals(o)) return false;
        return Objects.equals(completion, that.completion) && Objects.equals(aliases, that.aliases);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), completion, aliases);
    }

//    @Override
//    public int compareTo(Suggestion o) {
//        if (o instanceof AdvancedSuggestion advanced) {
//            return aliases.stream()
//                .map(alias -> advanced.aliases.stream()
//                    .map(alias::compareTo)
//                    .sorted()
//                    .findFirst()
//                    .orElse(0))
//                .sorted()
//                .findFirst()
//                .orElse(0);
//        }
//        return aliases.stream()
//            .map(alias -> alias.compareTo(o.getText()))
//            .sorted()
//            .findFirst()
//            .orElse(0);
//    }

//    @Override
//    public int compareToIgnoreCase(Suggestion b) {
//        if (b instanceof AdvancedSuggestion advanced) {
//            return aliases.stream()
//                .map(alias -> advanced.aliases.stream()
//                    .map(alias::compareToIgnoreCase)
//                    .sorted()
//                    .findFirst()
//                    .orElse(0))
//                .sorted()
//                .findFirst()
//                .orElse(0);
//        }
//        return aliases.stream()
//            .map(alias -> alias.compareToIgnoreCase(b.getText()))
//            .sorted()
//            .findFirst()
//            .orElse(0);
//    }

    public boolean matches(String remaining) {
        if (completion.equals(remaining)) return true;
        return aliases.stream().anyMatch(string -> shouldSuggest(remaining, string));
    }

    //--

    public static Builder builder(String completion) {
        return new Builder(completion);
    }

    public static class Builder {
        private final String completion;
        private String display = null;
        private List<String> aliases;
        private Message tooltip = null;

        private Builder(String completion) {
            this.completion = completion;
            this.aliases = new ArrayList<>();
            this.aliases.add(completion);
        }

        public Builder display(String display) {
            this.display = display;
            return this;
        }

        public Builder display(DisplayModifier display) {
            return display(display.apply(completion));
        }

        public Builder alias(String alias) {
            this.aliases.add(alias);
            return this;
        }

        public Builder aliases(List<String> aliases) {
            this.aliases = new ArrayList<>(aliases);
            return this;
        }

        public Builder aliases(String... aliases) {
            return aliases(List.of(aliases));
        }

        public Builder tooltip(Message tooltip) {
            this.tooltip = tooltip;
            return this;
        }

        public AdvancedSuggestion build(StringRange range) {
            return new AdvancedSuggestion(
                range,
                display != null ? display : completion,
                completion,
                aliases,
                tooltip
            );
        }

        @FunctionalInterface
        public interface DisplayModifier {
            String apply(String completion);
        }

    }

    public record Encoder(
        String completion,
        String display,
        List<String> aliases
    ) {
        private static final Gson GSON = new Gson();
        private static final String PREFIX = "chyzdev$advanced_suggestion";

        public static Encoder from(AdvancedSuggestion suggestion) {
            return new Encoder(
                suggestion.getCompletion(),
                suggestion.getText(),
                suggestion.getAliases()
            );
        }

        public String toJson() {
            return PREFIX + GSON.toJson(this);
        }

        public static boolean isValid(String json) {
            return json.startsWith(PREFIX) && fromJson(json) != null;
        }

        @Nullable
        public static AdvancedSuggestion.Encoder fromJson(String json) {
            json = json.substring(PREFIX.length());
            try {
                return GSON.fromJson(json, Encoder.class);
            } catch (JsonSyntaxException ignored) {
                return null;
            }
        }
    }
}
