package misc;

import com.devexperts.qd.kit.DefaultRecord;
import com.devexperts.qd.kit.DefaultScheme;
import com.dxfeed.api.impl.DXFeedScheme;

import java.util.function.Predicate;

/**
 * Analogue of native qds SchemeDump created for simplify debugging and research
 */
public class QdSchemaViewer {
    public static void printMainRecords(DefaultScheme scheme, boolean showFields) {
        Predicate<DefaultRecord> predicate = record -> !record.getName().contains("&")
                && !record.getName().contains("#")
                && !record.getName().contains(".");
        printRecords(scheme, showFields, predicate);
    }

    public static void printRecords(DefaultScheme scheme, boolean showFields,
                                    Predicate<DefaultRecord> predicate)
    {
        System.out.println("scheme: " + scheme.toString());
        System.out.println("records count: " + scheme.getRecordCount());
        for (int rIndex = 0; rIndex < scheme.getRecordCount(); rIndex++) {
            DefaultRecord record = scheme.getRecord(rIndex);
            if (predicate.test(record)) {
                System.out.println(record);
                System.out.println("\tint fields count:" + record.getIntFieldCount());
                if (showFields) {
                    for (int fIndex = 0; fIndex < record.getIntFieldCount(); fIndex++) {
                        System.out.println("\t" + record.getIntField(fIndex));
                    }
                }
            }
        }
    }

    public static void main(String[] args) {
        System.out.println("main records:");
        printMainRecords(DXFeedScheme.getInstance(), true);

        System.out.println("\n\nall records:");
        printRecords(DXFeedScheme.getInstance(), false, r -> true);
    }
}
