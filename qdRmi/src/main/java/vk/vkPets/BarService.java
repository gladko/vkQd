package vk.vkPets;

import com.devexperts.connector.proto.EndpointId;
import com.devexperts.rmi.RMIServiceInterface;

@RMIServiceInterface
public interface BarService {
    String bar(String param);

    class Impl implements BarService {
        private final EndpointId endpointId;

        public Impl(EndpointId endpointId) {
            this.endpointId = endpointId;
        }

        @Override
        public String bar(String param) {
            return endpointId.toString();
        }
    }
}

