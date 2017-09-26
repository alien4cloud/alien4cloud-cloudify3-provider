package alien4cloud.paas.cloudify3.util;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;

/**
 * A thin wrapper to convert spring future to guava. As Guava future is more power-full
 *
 * @param <T>
 */
public class SpringToGuavaListenableFuture<T> implements com.google.common.util.concurrent.ListenableFuture<T> {

    private ListenableFuture<T> springListenableFuture;

    public SpringToGuavaListenableFuture(ListenableFuture<T> springListenableFuture) {
        this.springListenableFuture = springListenableFuture;
    }

    @Override
    public void addListener(final Runnable listener, final Executor executor) {
        this.springListenableFuture.addCallback(new ListenableFutureCallback<T>() {
            @Override
            public void onSuccess(T result) {
                executor.execute(listener);
            }

            @Override
            public void onFailure(Throwable ex) {
                executor.execute(listener);
            }
        });
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return this.springListenableFuture.cancel(mayInterruptIfRunning);
    }

    @Override
    public boolean isCancelled() {
        return this.springListenableFuture.isCancelled();
    }

    @Override
    public boolean isDone() {
        return this.springListenableFuture.isDone();
    }

    @Override
    public T get() throws InterruptedException, ExecutionException {
        return this.springListenableFuture.get();
    }

    @Override
    public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return this.springListenableFuture.get(timeout, unit);
    }
}
