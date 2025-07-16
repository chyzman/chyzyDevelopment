package com.chyzman.chyzdev.command.api;

import com.chyzman.chyzdev.pond.CommandSourceDuck;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

import java.util.*;

@SuppressWarnings("UnusedReturnValue")
public class DumpBuilder {
    private static final int INDENT_SIZE = 2;

    private final List<MutableText> content = new ArrayList<>(List.of(Text.empty()));
    private final List<Text> prefix = new ArrayList<>();

    //region INTERNAL

    private <T> TextMapper<T> getDefaultMapper() {
        return o -> {
            if (o instanceof Text t) return t;
            else return Text.literal(String.valueOf(o));
        };
    }

    private DumpBuilder writeRaw(Text text) {
        content.getLast().append(text);
        return this;
    }

    private DumpBuilder writePrefix() {
        return write(prefix);
    }

    //endregion

    //region WRITE

    private <T> DumpBuilder safeWrite(Iterable<T> objects, TextMapper<T> mapper) {
        objects.forEach(object -> writeRaw(mapper.map(object)));
        return this;
    }

    public <T> DumpBuilder write(Iterable<T> objects, TextMapper<T> mapper) {
        return safeWrite(objects, mapper);
    }

    public <T> DumpBuilder write(Iterable<T> text) {
        return safeWrite(text, getDefaultMapper());
    }

    public DumpBuilder write(Object... texts) {
        return safeWrite(List.of(texts), getDefaultMapper());
    }

    //--

    public DumpBuilder writeLn() {
        content.add(Text.empty());
        return writePrefix();
    }

    private <T> DumpBuilder safeWriteLn(Collection<T> objects, TextMapper<T> mapper) {
        return writeLn().safeWrite(objects, mapper);
    }

    public <T> DumpBuilder writeLn(Collection<T> objects, TextMapper<T> mapper) {
        return safeWriteLn(objects, mapper);
    }

    public DumpBuilder writeLn(Collection<?> texts) {
        return safeWriteLn(texts, getDefaultMapper());
    }

    public DumpBuilder writeLn(Object... texts) {
        return safeWriteLn(List.of(texts), getDefaultMapper());
    }

    //--

    private <T> DumpBuilder safeWriteLns(Collection<T> objects, TextMapper<T> mapper) {
        objects.forEach(object -> safeWriteLn(Collections.singleton(object), mapper));
        return this;
    }

    public <T> DumpBuilder writeLns(Collection<T> objects, TextMapper<T> mapper) {
        return safeWriteLns(objects, mapper);
    }

    public DumpBuilder writeLns(Collection<?> texts) {
        return safeWriteLns(texts, getDefaultMapper());
    }

    public DumpBuilder writeLns(Object... texts) {
        return safeWriteLns(List.of(texts), getDefaultMapper());
    }

    //endregion

    //region INDENT

    public DumpBuilder push(Text text) {
        prefix.add(text);
        return this;
    }

    public DumpBuilder push(String string) {
        return push(Text.literal(string));
    }

    public DumpBuilder push() {
        return push(Text.literal("".repeat(INDENT_SIZE)));
    }

    //--

    public DumpBuilder pop(int count) {
        for (int i = 0; i < count; i++) {
            if (prefix.isEmpty()) return this;
            prefix.removeLast();
        }
        return this;
    }

    public DumpBuilder pop() {
        return pop(1);
    }

    public DumpBuilder fullPop() {
        prefix.clear();
        return this;
    }

    //endregion

    //region SPECIAL

    public DumpBuilder header(Text title) {
        return writeLn(
            "-".repeat(5),
            "[",
            title,
            "]",
            "-".repeat(5)
        );
    }

    public DumpBuilder separator() {
        return writeLn("-".repeat(20));
    }

    public <T> DumpBuilder writeList(Text title, Collection<T> items, TextMapper<T> mapper) {
        return writeLn(title, " - ", items.size())
            .push(" - ")
            .writeLns(items, mapper)
            .pop();
    }

    public <T> DumpBuilder writeList(Text title, Collection<T> items) {
        return this.writeList(title, items, getDefaultMapper());
    }

    //region BUILD

    public List<Text> build() {
        return Collections.unmodifiableList(content);
    }

    public void sendAsFeedback(CommandSourceDuck source) {
        build().forEach(source::chyzdev$sendFeedback);
    }

    //endregion

    @FunctionalInterface
    public interface TextMapper<T> {
        Text map(T object);
    }

}
