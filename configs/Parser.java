import org.yaml.snakeyaml.Yaml;

import java.io.FileInputStream;
import java.util.List;
import java.util.stream.Collectors;

/**
 *  javac -cp "snakeyaml-2.1.jar" -d build Parser.java
 *  java -cp "snakeyaml-2.1.jar;build" Parser "muxconfig.yaml" "agentAddress"
 *
 *  $JAVA_25/bin/java -cp "snakeyaml-2.1.jar;." Parser.java muxconfig.yaml agentAddress
 */
public class Parser {
    public static void main(String[] args) {
        String configFile = args[0];
        String prop = args[1];
//        String configFile = "muxconfig.yaml";
//        String prop = "agentAddress";

        Yaml yaml = new Yaml();
        try (FileInputStream in = new FileInputStream(configFile)) {
            QdConfig qdConfig = yaml.loadAs(in, QdConfig.class);

            String x = switch (prop) {
                case "agentAddress" -> format(qdConfig.agentAddress);
                case "distributorAddress" -> format(qdConfig.distributorAddress);
                default -> "";
            };

            System.out.println(x);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static String format(List<QdAddress> agentAddress) {
        return agentAddress.stream()
                .map(QdAddress::format)
                .collect(Collectors.joining(")(", "(", ")"));
    }

    static class QdConfig {
        public List<QdAddress> distributorAddress;
        public List<QdAddress> agentAddress;
    }

    static class QdAddress {
        public String name;
        public String address;
        public String filter;
        public Codecs codecs;
        public Params params;

        public String format() {
            return getFilter() + getCodecs() + address + getParams();
        }

        private String getParams() {
            KV nameKV = new KV("name", name);
            return params != null ? params.format(nameKV) : "[" + nameKV.format() + "]";
        }

        private String getCodecs() {
            return codecs != null ? codecs.format() : "";
        }

        private String getFilter() {
            return isBlank(filter) ? "" : filter + "@";
        }

        boolean isBlank(String string) {
            return string == null || string.isBlank();
        }
    }

    static class Codecs {
        public DelayedCodec delayed;
        public SslCodec ssl;

        public String format() {
            return (delayed != null ? delayed.format() : "")
                    + (ssl != null ? ssl.format() : "");
        }
    }

    static class DelayedCodec {
        public Integer delay;
        public Integer bufferLimit;

        public String format() {
            List<KV> props = List.of(
                    new KV("delay", delay),
                    new KV("bufferLimit", bufferLimit));

            return "delayed" + formatProps(props) + "+";
        }
    }

    static class SslCodec {
        public String protocols;

        public String format() {
            List<KV> props = List.of(new KV("protocols", protocols));
            return "ssl" + formatProps(props) + "+";
        }
    }

    static class Params {
        public Integer subscriptionThreads;
        public Integer subscriptionKeepAlive;

        public String format(KV nameKV) {
            return formatProps(List.of(nameKV,
                    new KV("subscriptionThreads", subscriptionThreads),
                    new KV("subscriptionKeepAlive", subscriptionKeepAlive)));
        }
    }

    static String formatProps(List<KV> props) {
        return props.stream()
                .map(KV::format)
                .collect(Collectors.joining(",", "[", "]"));
    }

    record KV(String key, Object value) {
        public String format() {
            return value == null ? "" : key + "=" + value;
        }
    }
}
