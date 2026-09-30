package dev.javamind.tokenization;

import java.util.List;


public interface Tokenizer {

    List<Integer> encode(String text);

    String decode(List<Integer> tokens);

    default int count(String text) {
        return encode(text).size();
    }
}
