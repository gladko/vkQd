package vk.vkPets;


import com.devexperts.connector.proto.EndpointId;
import com.devexperts.rmi.RMIEndpoint;
import com.devexperts.rmi.impl.RMIEndpointImpl;

public class TestServerMain {

//    private static final String SERVER_ADDRESS = ":8888";

    public static void main(String[] args) throws InterruptedException {
        if (args.length != 1) {
            throw new IllegalArgumentException("address must be set in JVM args");
        }
        String address = args[0];

        try (RMIEndpoint endpoint = RMIEndpoint.createEndpoint()) {
            EndpointId endpointId = ((RMIEndpointImpl) endpoint).getEndpointId();

            endpoint.getServer().export(new FooService.Impl(endpointId), FooService.class);
            endpoint.getServer().export(new BarService.Impl(endpointId), BarService.class);

            endpoint.connect(address);
//            log.info("started");
            System.out.println("server started");
            Thread.sleep(Long.MAX_VALUE);
        }
    }
}