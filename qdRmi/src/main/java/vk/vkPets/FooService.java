package vk.vkPets;

import com.devexperts.connector.proto.EndpointId;
import com.devexperts.rmi.RMIServiceInterface;
import com.devexperts.rmi.message.RMIRequestMessage;
import com.devexperts.rmi.task.RMITask;

@RMIServiceInterface
public interface FooService {
    String foo(String param);

    class Impl implements FooService {
        private final EndpointId endpointId;

        public Impl(EndpointId endpointId) {
            this.endpointId = endpointId;
        }

        @Override
        public String foo(String param) {
//            examineRmiTask();

            return endpointId.toString();
        }

        private void examineRmiTask() {
            RMITask<?> rmiTask = RMITask.current();
            RMIRequestMessage<?> requestMessage = rmiTask.getRequestMessage();
            System.out.println("FooService endpointId: " + endpointId
              + ", route: " + requestMessage.getRoute()
              + ", properties: " + requestMessage.getProperties()
              + ", parameters: " + requestMessage.getParameters());
        }
    }
}

