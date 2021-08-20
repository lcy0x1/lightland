package com.hikarishima.lightland.command;

import com.google.common.collect.Maps;
import com.lcy0x1.core.util.ExceptionHandler;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.command.ISuggestionProvider;
import net.minecraft.util.text.TranslationTextComponent;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class EnumParser<T extends Enum<T>> implements ArgumentType<T> {

    private static final Map<Class<?>, EnumParser<?>> CACHE = Maps.newLinkedHashMap();

    @SuppressWarnings("unchecked")
    public static <T extends Enum<T>> EnumParser<T> getParser(Class<T> cls) {
        if (CACHE.containsKey(cls))
            return (EnumParser<T>) CACHE.get(cls);
        EnumParser<T> parser = new EnumParser<>(cls);
        CACHE.put(cls, parser);
        return parser;
    }

    public final Class<T> cls;

    private EnumParser(Class<T> cls) {
        this.cls = cls;
    }

    @Override
    public T parse(StringReader reader) throws CommandSyntaxException {
        String str = reader.readUnquotedString();
        T val = ExceptionHandler.ignore(() -> Enum.valueOf(cls, str));
        if (val == null) {
            throw new DynamicCommandExceptionType((obj) -> new TranslationTextComponent("lightland:argument.invalid_id", obj)).createWithContext(reader, str);
        }
        return val;
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        List<String> list = Arrays.stream(cls.getEnumConstants()).map(Enum::name).collect(Collectors.toList());
        return ISuggestionProvider.suggest(list, builder);
    }

}