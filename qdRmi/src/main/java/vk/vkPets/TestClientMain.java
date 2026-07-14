package vk.vkPets;

import com.devexperts.connector.proto.EndpointId;
import com.devexperts.rmi.*;
import com.devexperts.rmi.impl.RMIEndpointImpl;
import com.devexperts.rmi.message.RMIRequestMessage;
import com.devexperts.rmi.message.RMIRequestType;

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

public class TestClientMain {
    public static final RMIOperation<String> FOO =
            RMIOperation.valueOf("vk.vkPets.FooService", String.class, "foo", String.class);

    //    private static final String SERVER_ADDRESS = "localhost:8888";
    private static EndpointId clientId;
    private static RMIClientPort clientPort;

    private static BarService barService;


    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            throw new IllegalArgumentException("address must be set in JVM args");
        }
        String address = args[0];

        try (RMIEndpoint endpoint = RMIEndpoint.createEndpoint(RMIEndpoint.Side.CLIENT)) {
            endpoint.connect(address);

            clientId = ((RMIEndpointImpl) endpoint).getEndpointId();
            clientPort = endpoint.getClient().getPort("testSubject");

            barService = endpoint.getClient().getProxy(BarService.class);

            while (true) {
                try {
                    doIteration();

                    TimeUnit.SECONDS.sleep(1);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static void doIteration() throws Exception {
        int xValue = 1; // may be accountId, userId  or smth else
        Map<String, String> requestProps = Map.of("x-header", Integer.toString(xValue));
        String fooResult = callFoo(clientPort, requestProps, new Object[]{"test"});

        String barResult = barService.bar("test");
//      TODO dxFeed: wanted API
//      String barResult2 = callWithProps(() -> fooService.foo("test"), requestProps);

        System.out.println("client:" + clientId
                + ",\tcustom routing response from: " + fooResult
                + ",\tdefault routing response from: " + barResult);
    }

    private static String callFoo(RMIClientPort clientPort, Map<String, String> requestProps, Object[] params)
            throws RMIException
    {
        RMIRequestMessage<String> message = new RMIRequestMessage<>(RMIRequestType.DEFAULT, FOO, params)
                        .changeProperties(requestProps);
        RMIRequest<String> request = clientPort.createRequest(message);
        request.send();
        return request.getBlocking();
    }

    // todo workaround: create dynamic proxy to use FooService through its java interface
    private static <V> V callWithProps(Callable<V> callable, Map<String, String> requestProps) throws Exception {
        // todo: put requestProps to some ThreadLocal storage and use it for RMIRequestMessage building
        return callable.call();
    }
}
