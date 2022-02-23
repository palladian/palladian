package ws.palladian.persistence.cloud;

import java.util.Objects;

/**
 * Configure access to a certain backblaze bucket.
 *
 * @author David Urbansky
 * @since 23-Feb-22 at 11:48
 **/
public class BackblazeConfig {
    private final String accountId;
    private final String applicationKey;
    private final String bucketId;
    private final int numberOfThreads;

    public BackblazeConfig(String accountId, String applicationKey, String bucketId, int numberOfThreads) {
        this.accountId = accountId;
        this.applicationKey = applicationKey;
        this.bucketId = bucketId;
        this.numberOfThreads = numberOfThreads;
    }

    public String getAccountId() {
        return accountId;
    }

    public String getApplicationKey() {
        return applicationKey;
    }

    public String getBucketId() {
        return bucketId;
    }

    public int getNumberOfThreads() {
        return numberOfThreads;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        BackblazeConfig that = (BackblazeConfig) o;
        return accountId.equals(that.accountId) && applicationKey.equals(that.applicationKey) && bucketId.equals(that.bucketId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(accountId, applicationKey, bucketId);
    }
}
