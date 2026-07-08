package vk.vkPets;

import com.devexperts.connector.proto.EndpointId;
import com.devexperts.rmi.RMIServiceInterface;
import com.devexperts.rmi.task.RMITask;

import java.util.Map;

@RMIServiceInterface
public interface BarService {
    String bar(String x);

    class Impl implements BarService {
        private final EndpointId endpointId;

        public Impl(EndpointId endpointId) {
            this.endpointId = endpointId;
        }

        @Override
        public String bar(String x) {
            return "bar-" + endpointId;
        }
    }
}

