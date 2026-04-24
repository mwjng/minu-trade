package com.minupay.trade.stock.infrastructure.search;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ChosungConverterTest {

    @Test
    void 한글_초성_추출() {
        assertThat(ChosungConverter.toChosung("삼성전자")).isEqualTo("ㅅㅅㅈㅈ");
        assertThat(ChosungConverter.toChosung("카카오")).isEqualTo("ㅋㅋㅇ");
    }

    @Test
    void 한글외_문자는_그대로() {
        assertThat(ChosungConverter.toChosung("SK하이닉스")).isEqualTo("SKㅎㅇㄴㅅ");
        assertThat(ChosungConverter.toChosung("LG 전자")).isEqualTo("LG ㅈㅈ");
    }

    @Test
    void 빈_문자열_처리() {
        assertThat(ChosungConverter.toChosung("")).isEqualTo("");
        assertThat(ChosungConverter.toChosung(null)).isEqualTo("");
    }

    @Test
    void 자소와_음절_혼합된_입력_처리() {
        assertThat(ChosungConverter.toChosung("삼ㅅ")).isEqualTo("ㅅㅅ");
        assertThat(ChosungConverter.toChosung("삼성ㅈ")).isEqualTo("ㅅㅅㅈ");
    }
}
