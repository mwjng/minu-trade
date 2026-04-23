package com.minupay.trade.holding.application;

import com.minupay.trade.common.exception.ErrorCode;
import com.minupay.trade.common.exception.MinuTradeException;
import com.minupay.trade.common.money.Money;
import com.minupay.trade.holding.application.dto.HoldingInfo;
import com.minupay.trade.holding.domain.Holding;
import com.minupay.trade.holding.domain.HoldingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HoldingServiceTest {

    @Mock HoldingRepository holdingRepository;

    HoldingService service;

    @BeforeEach
    void setup() {
        service = new HoldingService(holdingRepository);
    }

    private static Money won(String value) {
        return Money.of(new BigDecimal(value));
    }

    @Test
    void applyBuy_최초면_openForBuy_저장() {
        when(holdingRepository.findByUserIdAndStockCode(1L, "005930")).thenReturn(Optional.empty());
        when(holdingRepository.save(any(Holding.class))).thenAnswer(inv -> inv.getArgument(0));

        HoldingInfo info = service.applyBuy(1L, "005930", 10, won("70000"));

        ArgumentCaptor<Holding> captor = ArgumentCaptor.forClass(Holding.class);
        verify(holdingRepository).save(captor.capture());
        assertThat(captor.getValue().getQuantity()).isEqualTo(10);
        assertThat(info.quantity()).isEqualTo(10);
        assertThat(info.avgPrice()).isEqualByComparingTo("70000");
    }

    @Test
    void applyBuy_기존보유있으면_가중평균_갱신() {
        Holding existing = Holding.openForBuy(1L, "005930", 10, won("70000"));
        when(holdingRepository.findByUserIdAndStockCode(1L, "005930")).thenReturn(Optional.of(existing));

        HoldingInfo info = service.applyBuy(1L, "005930", 10, won("80000"));

        assertThat(info.quantity()).isEqualTo(20);
        assertThat(info.avgPrice()).isEqualByComparingTo("75000");
        verify(holdingRepository, never()).save(any());
    }

    @Test
    void reserveSell_보유있으면_예약수량_증가() {
        Holding existing = Holding.openForBuy(1L, "005930", 10, won("70000"));
        when(holdingRepository.findByUserIdAndStockCode(1L, "005930")).thenReturn(Optional.of(existing));

        HoldingInfo info = service.reserveSell(1L, "005930", 3);

        assertThat(info.quantity()).isEqualTo(10);
        assertThat(info.reservedQuantity()).isEqualTo(3);
        assertThat(info.availableQuantity()).isEqualTo(7);
    }

    @Test
    void reserveSell_보유없으면_HOLDING_NOT_FOUND() {
        when(holdingRepository.findByUserIdAndStockCode(1L, "005930")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.reserveSell(1L, "005930", 1))
                .isInstanceOf(MinuTradeException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.HOLDING_NOT_FOUND);
    }

    @Test
    void releaseSell_예약_해제() {
        Holding existing = Holding.openForBuy(1L, "005930", 10, won("70000"));
        existing.reserve(5);
        when(holdingRepository.findByUserIdAndStockCode(1L, "005930")).thenReturn(Optional.of(existing));

        HoldingInfo info = service.releaseSell(1L, "005930", 2);

        assertThat(info.reservedQuantity()).isEqualTo(3);
        assertThat(info.availableQuantity()).isEqualTo(7);
    }

    @Test
    void settleSell_전량_매도면_레코드_삭제() {
        Holding existing = Holding.openForBuy(1L, "005930", 10, won("70000"));
        existing.reserve(10);
        when(holdingRepository.findByUserIdAndStockCode(1L, "005930")).thenReturn(Optional.of(existing));

        HoldingInfo info = service.settleSell(1L, "005930", 10);

        assertThat(info.quantity()).isZero();
        verify(holdingRepository).delete(existing);
    }

    @Test
    void settleSell_부분_매도면_레코드_유지() {
        Holding existing = Holding.openForBuy(1L, "005930", 10, won("70000"));
        existing.reserve(5);
        when(holdingRepository.findByUserIdAndStockCode(1L, "005930")).thenReturn(Optional.of(existing));

        HoldingInfo info = service.settleSell(1L, "005930", 3);

        assertThat(info.quantity()).isEqualTo(7);
        assertThat(info.reservedQuantity()).isEqualTo(2);
        verify(holdingRepository, never()).delete(any());
    }

    @Test
    void settleSell_보유없으면_HOLDING_NOT_FOUND() {
        when(holdingRepository.findByUserIdAndStockCode(1L, "005930")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.settleSell(1L, "005930", 1))
                .isInstanceOf(MinuTradeException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.HOLDING_NOT_FOUND);
    }

    @Test
    void getMyHoldings_userId_기준_조회() {
        Holding h1 = Holding.openForBuy(1L, "005930", 10, won("70000"));
        Holding h2 = Holding.openForBuy(1L, "035420", 5, won("200000"));
        when(holdingRepository.findAllByUserIdOrderByStockCodeAsc(1L)).thenReturn(List.of(h1, h2));

        List<HoldingInfo> infos = service.getMyHoldings(1L);

        assertThat(infos).hasSize(2);
        assertThat(infos).extracting(HoldingInfo::stockCode).containsExactly("005930", "035420");
    }
}
