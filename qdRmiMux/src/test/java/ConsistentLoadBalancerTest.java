import com.devexperts.connector.proto.EndpointId;
import com.devexperts.io.Marshalled;
import com.devexperts.rmi.RMIOperation;
import com.devexperts.rmi.message.RMIRequestMessage;
import com.devexperts.rmi.message.RMIRequestType;
import com.devexperts.rmi.message.RMIRoute;
import com.devexperts.rmi.task.BalanceResult;
import com.devexperts.rmi.task.ConsistentLoadBalancer;
import com.devexperts.rmi.task.RMIServiceDescriptor;
import com.devexperts.rmi.task.RMIServiceId;
import com.dxfeed.promise.Promise;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

public class ConsistentLoadBalancerTest {
    /**
     * Verified if SecureRandom.getInstance("SHA1PRNG") returns the same sequence of numbers on WINDOWS
     * It's used in ConsistentLoadBalancer.addInRing.
     */
    @Test
    public void test_SHA1PRNG_SecureRandom() {
        SecureRandom random;
        try {
            // Load balancer requires a stable consistent random generator defined by seed.
            // It is verified that SecureRandom.getInstance("SHA1PRNG") returns the same sequence of numbers for
            // the same seed across different JVMs and different OSes (checked MacOS and Linux).
            random = SecureRandom.getInstance("SHA1PRNG");
        } catch (NoSuchAlgorithmException e) {
            throw new AssertionError(e);
        }
        // NOTE: SHA1PRNG instance cannot be reused because only the first setSeed defines a deterministic sequence
        random.setSeed(1);
        int[] data = new int[10];

        for (int i = 0; i < data.length; i++) {
            data[i] = random.nextInt();
        }

        int[] expected = {-1765061395, -958952890, 1714755920, -585300305, 1432426612,
                554064022, 1614405352, 861636861, -605868439, -401229925};
        Assertions.assertArrayEquals(expected, data);
    }

    public static void main(String[] args) {
        ConsistentLoadBalancer loadBalancer = new ConsistentLoadBalancer();

        for (int i = 0; i < 5; i++) {
            RMIServiceDescriptor descriptor = RMIServiceDescriptor.createDescriptor(
                    RMIServiceId.newServiceId("service_" + i), 1, null, null);
            loadBalancer.updateServiceDescriptor(descriptor);
        }

        RMIOperation<String> operation = RMIOperation.valueOf(
                "vk.vkPets.FooService", String.class, "foo", String.class);
        Marshalled params = Marshalled.forObject(new Object[]{"xxx"}, operation.getParametersMarshaller());
        RMIRoute rmiRoute = RMIRoute.createRMIRoute(EndpointId.newEndpointId("client"));
        RMIRequestMessage<String> message = new RMIRequestMessage<>(RMIRequestType.DEFAULT, operation,
                params, rmiRoute, null);

        int resultCollector = 0; // to avoid optimisation
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < 100_000_000; i++) {
            Promise<BalanceResult> result = loadBalancer.balance(message);
            resultCollector += result.getResult().hashCode();
        }

        System.out.println("time: " + (System.currentTimeMillis() - startTime));
        System.out.println("trash_" + resultCollector);
    }
}
