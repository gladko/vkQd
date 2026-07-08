package vk.vkPets;

import com.devexperts.logging.Logging;
import com.devexperts.rmi.task.RMILoadBalancer;
import com.devexperts.rmi.task.RMILoadBalancerFactory;
import com.devexperts.services.ServiceProvider;

@ServiceProvider(order = -1)
public class VkLoadBalancerFactory implements RMILoadBalancerFactory {
    private static final Logging log = Logging.getLogging(VkLoadBalancerFactory.class);
    @Override
    public RMILoadBalancer createLoadBalancer(String serviceName) {
        log.info("return VkLoadBalancer");

        return new VkLoadBalancer();
    }
}
