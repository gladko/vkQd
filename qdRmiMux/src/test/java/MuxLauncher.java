import com.devexperts.qd.tools.Multiplexor;

// only for debugging
public class MuxLauncher {
    public static void main(String[] args) {
        Multiplexor.main(new String[] {":8001", ":9001[advertise=none]", "--stat", "10s", "-R"});
    }
}
