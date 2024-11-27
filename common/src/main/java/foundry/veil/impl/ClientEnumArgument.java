package foundry.veil.impl;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;

import java.util.Arrays;
import java.util.Collection;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class ClientEnumArgument<T extends Enum<T>> implements ArgumentType<T> {

    private static final Dynamic2CommandExceptionType INVALID_ENUM = new Dynamic2CommandExceptionType((found, constants) -> Component.translatable("commands.veil.arguments.enum.invalid", found, constants));
    private final Class<T> enumClass;

    private ClientEnumArgument(final Class<T> enumClass) {
        this.enumClass = enumClass;
    }

    public static <R extends Enum<R>> ClientEnumArgument<R> enumArgument(Class<R> enumClass) {
        return new ClientEnumArgument<>(enumClass);
    }

    @Override
    public T parse(final StringReader reader) throws CommandSyntaxException {
        String name = reader.readUnquotedString();
        try {
            return Enum.valueOf(this.enumClass, name.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw INVALID_ENUM.createWithContext(reader, name, Arrays.stream(this.enumClass.getEnumConstants()).map(value -> value.name().toLowerCase(Locale.ROOT)).collect(Collectors.joining(", ")));
        }
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(final CommandContext<S> context, final SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggest(Arrays.stream(this.enumClass.getEnumConstants()).map(value -> value.name().toLowerCase(Locale.ROOT)), builder);
    }

    @Override
    public Collection<String> getExamples() {
        return Arrays.stream(this.enumClass.getEnumConstants()).map(Enum::name).collect(Collectors.toList());
    }
}
