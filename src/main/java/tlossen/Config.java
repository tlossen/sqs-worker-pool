package tlossen;

import com.amazonaws.regions.Regions;

public class Config
{
    public final String queueName;
    public Regions region = Regions.EU_CENTRAL_1;
    public int poolSize = 2;
    public int visibilityTimeout = 5;

    public Config(String queueName) {
        this.queueName = queueName;
    }

    public Config withRegion(final Regions region) {
        this.region = region;
        return this;
    }

    public Config withPoolSize(final int poolSize) {
        this.poolSize = poolSize;
        return this;
    }

    public Config withVisibilityTimeout(final int timeout) {
        this.visibilityTimeout = timeout;
        return this;
    }

}
