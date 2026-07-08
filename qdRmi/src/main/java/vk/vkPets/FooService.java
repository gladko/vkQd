package vk.vkPets;

import com.devexperts.connector.proto.EndpointId;
import com.devexperts.rmi.RMIServiceInterface;
import com.devexperts.rmi.task.RMITask;

import java.util.Map;

@RMIServiceInterface
public interface FooService {
    String foo(String x);

    class Impl implements FooService {
        private final EndpointId endpointId;

        public Impl(EndpointId endpointId) {
            this.endpointId = endpointId;
        }

        @Override
        public String foo(String x) {
            RMITask<?> rmiTask = RMITask.current();
            Map<String, String> properties = rmiTask.getRequestMessage().getProperties();
            System.out.println(properties);

            return "foo-" + endpointId;
        }
    }
}

