package com.minupay.trade.paymentclient;

import com.minupay.trade.paymentclient.dto.*;

public interface PayServiceClient {

    ChargeResponse charge(ChargeRequest request);

    CancelResponse cancel(Long paymentId, CancelRequest request);

    CancelResponse partialCancel(Long paymentId, PartialCancelRequest request);

    WalletChargeResponse creditWallet(Long walletId, WalletChargeRequest request);
}
