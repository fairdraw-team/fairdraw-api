package com.fairdraw.api.draw;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class DrawServiceTest {

    @Test
    void 제비_id_세_개의_해시가_미리_계산한_값과_같다() {
        String expected = "ea7fb08b7a2dc4619ffb7c7bb38d95a2047935fa165d71b12efd3852a2e6d0cc";
        assertEquals(expected, DrawService.hashOf(List.of("a", "b", "c")));
    }

    @Test
    void 제비가_없으면_빈_글자의_해시가_나온다() {
        String expected = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";
        assertEquals(expected, DrawService.hashOf(List.of()));
    }
}