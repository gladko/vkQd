package vk.vkPets;

import com.devexperts.logging.Logging;
import com.devexperts.rmi.message.RMIRequestMessage;
import com.devexperts.rmi.task.ConsistentLoadBalancer;


public class VkLoadBalancer extends ConsistentLoadBalancer {
    private static final Logging log = Logging.getLogging(VkLoadBalancer.class);

//    private final RMILoadBalancer delegate = new ConsistentLoadBalancer();
//
//    @Override
//    public Promise<BalanceResult> balance(RMIRequestMessage<?> request) {
//        return ...;
//    }
//
//    @Override
//    public void updateServiceDescriptor(RMIServiceDescriptor descriptor) {
//        delegate.updateServiceDescriptor(descriptor);
//    }
//
//    @Override
//    public void close() {
//        delegate.close();
//    }

    @Override
    public int getRequestKey(RMIRequestMessage<?> request) {
//        log.info("routing " + request);

        if ("foo".equals(request.getOperation().getMethodName())) {
            String xHeader = request.getProperties().get("x-header");
            if (xHeader == null) {
                return super.getRequestKey(request);
            }

            // custom routing key
            int key = xHeader.hashCode();
            log.info("RequestKey: " + key);
            return key;
        }
        return super.getRequestKey(request);
    }
}