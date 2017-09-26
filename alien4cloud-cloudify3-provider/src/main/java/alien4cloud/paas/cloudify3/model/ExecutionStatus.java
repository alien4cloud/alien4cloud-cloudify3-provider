package alien4cloud.paas.cloudify3.model;

public class ExecutionStatus {

    private ExecutionStatus() {

    }

    public static final String TERMINATED = "terminated";

    public static final String FAILED = "failed";

    public static final String CANCELLED = "cancelled";

    public static final String PENDING = "pending";

    public static final String STARTED = "started";

    public static final String CANCELLING = "cancelling";

    public static final String FORCE_CANCELLING = "force_cancelling";

    public static boolean isTerminated(String status) {
        switch (status) {
        case TERMINATED:
        case FAILED:
        case CANCELLED:
            return true;
        default:
            return false;
        }
    }

    public static boolean isTerminatedSuccessfully(String status) {
        return TERMINATED.equals(status);
    }

    public static boolean isTerminatedWithFailure(String status) {
        return FAILED.equals(status);
    }

    public static boolean isCancelled(String status) {
        return CANCELLING.equals(status) || FORCE_CANCELLING.equals(status) || CANCELLED.equals(status);
    }

    public static boolean isInProgress(String status) {
        return PENDING.equals(status) || STARTED.equals(status);
    }
}
