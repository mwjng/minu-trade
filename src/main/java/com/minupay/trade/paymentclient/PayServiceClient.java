package com.minupay.trade.paymentclient;

import com.minupay.trade.paymentclient.dto.WalletTxRequest;
import com.minupay.trade.paymentclient.dto.WalletTxResponse;

public interface PayServiceClient {

    WalletTxResponse deduct(Long userId, WalletTxRequest request);

    WalletTxResponse credit(Long userId, WalletTxRequest request);
}
