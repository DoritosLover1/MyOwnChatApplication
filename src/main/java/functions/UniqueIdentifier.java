package functions;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

public interface UniqueIdentifier {
	
	static final AtomicLong currentTimestamp = new AtomicLong(System.currentTimeMillis());
	
	public static String getUniqueIdentifier() {
		return Long.toString(currentTimestamp.getAndIncrement()) + "-" + UUID.randomUUID().toString();
	}
}
