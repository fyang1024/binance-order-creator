package fund.threebox.boc;

import com.binance.api.client.BinanceApiCallback;
import com.binance.api.client.BinanceApiClientFactory;
import com.binance.api.client.BinanceApiRestClient;
import com.binance.api.client.BinanceApiWebSocketClient;
import com.binance.api.client.domain.account.Account;
import com.binance.api.client.domain.account.NewOrder;
import com.binance.api.client.domain.account.NewOrderResponse;
import com.binance.api.client.domain.event.AggTradeEvent;
import com.binance.api.client.domain.event.DepthEvent;
import com.binance.api.client.domain.event.UserDataUpdateEvent;

public class OrderCreator {

    public static void main(String[] args) throws InterruptedException {
        if (args == null || args.length != 4) {
            throw new IllegalArgumentException("Invalid arguments");
        }
        String apiKey = args[0];
        String secret = args[1];
        String asset = args[2];
        final String symbol = args[3].toLowerCase();
        BinanceApiClientFactory factory = BinanceApiClientFactory.newInstance(apiKey, secret);
        final BinanceApiRestClient restClient = factory.newRestClient();
        Account account = restClient.getAccount();
        boolean shouldTryAgain = true;
        while(shouldTryAgain) {
            final String balance = account.getAssetBalance(asset).getFree();
            System.out.println(asset + ": " + balance);
            if ("0".equals(balance)) {
                Thread.sleep(1000);
                System.out.println("Slept 1 sec");
            } else {
                shouldTryAgain = false;
                BinanceApiWebSocketClient webSocketClient = factory.newWebSocketClient();
                webSocketClient.onAggTradeEvent(symbol, new BinanceApiCallback<AggTradeEvent>() {
                    public void onResponse(final AggTradeEvent response) {
                        System.out.println(response);
                        if (response.isBuyerMaker()) {
                            NewOrderResponse newOrderResponse = restClient.newOrder(NewOrder.marketSell(symbol.toUpperCase(), balance));
                            System.out.println(newOrderResponse);
                        }
                    }
                    public void onFailure(final Throwable cause) {
                        System.err.println("Web socket failed");
                        cause.printStackTrace(System.err);
                    }
                });
                webSocketClient.onDepthEvent(symbol, new BinanceApiCallback<DepthEvent>() {
                    public void onResponse(DepthEvent depthEvent) {
                        System.out.println(depthEvent);
                    }
                });
                webSocketClient.onUserDataUpdateEvent(symbol, new BinanceApiCallback<UserDataUpdateEvent>() {
                    public void onResponse(UserDataUpdateEvent userDataUpdateEvent) {
                        System.out.println(userDataUpdateEvent);
                    }
                });
            }
        }
    }
}
