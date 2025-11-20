package intro.jmx;

import com.devexperts.management.Management;
import com.devexperts.qd.monitoring.JMXEndpoint;

import static com.devexperts.qd.monitoring.JMXEndpoint.JMX_HTML_PORT_PROPERTY;
import static com.devexperts.qd.monitoring.JMXEndpoint.JMX_RMI_PORT_PROPERTY;
import static java.lang.Thread.sleep;

public class JmxSample {
    public static void main(String[] args) throws Exception {
        JMXEndpoint jmxEndpoint = JMXEndpoint.newBuilder()
                .withProperty(JMX_HTML_PORT_PROPERTY, "7077")
                .withProperty(JMX_RMI_PORT_PROPERTY, "8088")
                .acquire();

//        String mBeanName = Management.getMBeanNameForClass(Foo.class);
//        Management.wrapMBean(new Foo(), )


        Management.registerMBean(new Foo(), FooMBean.class,
                Management.getMBeanNameForClass(JmxSample.class));
        sleep(Long.MAX_VALUE);
    }

    static class Foo implements FooMBean {
        @Override
        public String ping(String ar1, int arg2) {
            return "Pong: " + ar1 + " / " + arg2;
        }
    }
}
