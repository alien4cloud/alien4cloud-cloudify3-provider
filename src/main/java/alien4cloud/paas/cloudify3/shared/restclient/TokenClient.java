package alien4cloud.paas.cloudify3.shared.restclient;

import com.google.common.util.concurrent.ListenableFuture;

import alien4cloud.paas.cloudify3.model.Token;
import alien4cloud.paas.cloudify3.util.FutureUtil;
import lombok.SneakyThrows;

public class TokenClient {
    private static final String TOKEN_PATH = "/api/v3/tokens";
    private final ApiHttpClient client;

    public TokenClient(ApiHttpClient apiHttpClient) {
        this.client = apiHttpClient;
    }

    public ListenableFuture<Token> asyncRead() {
        return FutureUtil.unwrapRestResponse(client.getForEntity(client.buildRequestUrl(TOKEN_PATH), Token.class));
    }

    @SneakyThrows
    public Token get() {
        return asyncRead().get();
    }
}
