package intro.jmx;

import com.devexperts.annotation.Description;

public interface FooMBean {
    @Description(value = "my ping description")
    String ping(
            @Description(value = "arg1 description", name = "arg1") String arg1,
            @Description(value = "arg2 description", name = "arg2") int arg2);
}