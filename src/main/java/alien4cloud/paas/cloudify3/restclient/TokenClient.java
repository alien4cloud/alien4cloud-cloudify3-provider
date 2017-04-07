package alien4cloud.paas.cloudify3.restclient;

import com.google.common.util.concurrent.ListenableFuture;

import alien4cloud.paas.cloudify3.model.Token;
import alien4cloud.paas.cloudify3.util.FutureUtil;
import lombok.SneakyThrows;
import org.springframework.stereotype.Component;

@Component
public class TokenClient extends AbstractClient {

    public static final String TOKEN_PATH = "/tokens";

    @Override
    protected String getPath() {
        return TOKEN_PATH;
    }

    public ListenableFuture<Token> asyncRead() {
        return FutureUtil.unwrapRestResponse(getForEntity(getBaseUrl(), Token.class));
    }

    @SneakyThrows
    public Token get() {
        return asyncRead().get();
    }
}
