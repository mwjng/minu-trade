package com.minupay.trade.common.money;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MoneyTest {

    @Test
    void 음수로_생성하면_예외() {
        assertThatThrownBy(() -> Money.of(-1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 덧셈과_뺄셈은_새_인스턴스를_반환() {
        Money a = Money.of(1000);
        Money b = Money.of(300);

        assertThat(a.add(b)).isEqualTo(Money.of(1300));
        assertThat(a.subtract(b)).isEqualTo(Money.of(700));
    }

    @Test
    void 뺄셈_결과가_음수이면_예외() {
        assertThatThrownBy(() -> Money.of(100).subtract(Money.of(200)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 정수배_곱셈() {
        assertThat(Money.of(75000).multiply(10)).isEqualTo(Money.of(750000));
    }

    @Test
    void 스케일이_달라도_값이_같으면_equal() {
        Money a = Money.of(new BigDecimal("1000"));
        Money b = Money.of(new BigDecimal("1000.00"));
        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    @Test
    void 비교_연산() {
        Money a = Money.of(1000);
        Money b = Money.of(500);

        assertThat(a.isGreaterThan(b)).isTrue();
        assertThat(a.isGreaterThanOrEqualTo(Money.of(1000))).isTrue();
        assertThat(b.isGreaterThan(a)).isFalse();
    }
}
