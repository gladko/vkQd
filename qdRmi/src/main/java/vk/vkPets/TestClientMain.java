package vk.vkPets;

import com.devexperts.connector.proto.EndpointId;
import com.devexperts.rmi.RMIEndpoint;
import com.devexperts.rmi.RMIException;
import com.devexperts.rmi.RMIOperation;
import com.devexperts.rmi.RMIRequest;
import com.devexperts.rmi.impl.RMIEndpointImpl;
import com.devexperts.rmi.message.RMIRequestMessage;
import com.devexperts.rmi.message.RMIRequestType;

import java.util.Map;
import java.util.concurrent.TimeUnit;

public class TestClientMain {
    public static final RMIOperation<String> FOO =
            RMIOperation.valueOf("vk.vkPets.FooService", String.class, "foo", String.class);

    //    private static final String SERVER_ADDRESS = "localhost:8888";

    public static void main(String[] args) throws InterruptedException, RMIException {
        if (args.length != 1) {
            throw new IllegalArgumentException("address must be set in JVM args");
        }
        String address = args[0];

        try (RMIEndpoint endpoint = RMIEndpoint.createEndpoint(RMIEndpoint.Side.CLIENT)) {
            endpoint.connect(address);

            EndpointId endpointId = ((RMIEndpointImpl) endpoint).getEndpointId();

            FooService fooService = endpoint.getClient().getProxy(FooService.class);
            BarService barService = endpoint.getClient().getProxy(BarService.class);

            while (true) {
//                String fooResult = fooService.foo("1");
                String fooResult = callFoo(endpoint, Map.of("x-param", "aaa"));
                String barResult = barService.bar("1");

                System.out.println(endpointId + ": " + fooResult + " / " + barResult);

                TimeUnit.SECONDS.sleep(1);
            }
        }
    }


    private static String callFoo(RMIEndpoint endpoint, Map<String, String> requestProps) throws RMIException {
        RMIRequestMessage<String> message = new RMIRequestMessage<>(RMIRequestType.DEFAULT, FOO, "test")
                        .changeProperties(requestProps);
        RMIRequest<String> request = endpoint.getClient().getPort("").createRequest(message);
        request.send();
        return request.getBlocking();
    }
}
