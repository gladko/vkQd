package com.devexperts.qd.tools;

import com.devexperts.qd.*;
import com.devexperts.qd.kit.DecimalField;
import com.devexperts.qd.kit.StringField;
import com.devexperts.qd.ng.*;
import com.devexperts.qd.util.Decimal;

import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

public class TestDataGenerator extends AbstractRecordProvider {
    enum Mode {
        RANDOM,
        SEQ // sequential
    }

    private static final int DATA_VARIANTS = 1000;

    private final Mode mode;
    private final SymbolList symbols;
    private final int max_records;
    private final Random random = new Random(1); // fixed seed for consistent unit-testing
    private final int[] random_decimals = new int[10000];

    private int[][][] int_fields;
    private Object[][][] obj_fields;

    { // pre-generate a list of random decimals to generate data fast (Decimal.compose _is_ slow)
        for (int i = 0; i < random_decimals.length; i++)
            random_decimals[i] = Decimal.compose(random.nextInt(1000) / 100.0);
    }

    private int sequence;
    private final AtomicLong retrievedCount;
    private final ThreadLocal<List<RecordCursor.Owner>> localOwners;
    private final ThreadLocal<Seq> localSeq;


    static class Seq {
        private int prevSymbolIndex;
        private int prevRecordIndex;
        private int prevVariantIndex;
    }

    public TestDataGenerator(List<DataRecord> records, List<String> symbols, Mode mode, int max_records,
                             AtomicLong retrievedCount)
    {
        this.mode = mode;
        this.symbols =  new SymbolList(symbols.toArray(new String[0]), records.get(0).getScheme().getCodec());
        this.max_records = max_records;
        this.retrievedCount = retrievedCount;
        localOwners = ThreadLocal.withInitial(() -> records.stream()
                        .map(RecordCursor::allocateOwner)
                        .toList());
        localSeq = ThreadLocal.withInitial(Seq::new);
        initFieldGens(records);
    }

    @Override
    public void setRecordListener(RecordListener listener) {}

    @Override
    public RecordMode getMode() {
        return RecordMode.DATA;
    }

    @Override
    public boolean retrieve(RecordSink sink) {
        int count = 0;
        List<RecordCursor.Owner> recordOwners = localOwners.get();
        Seq seq = localSeq.get();

        for (int i = max_records; --i >= 0;) {
            if (!sink.hasCapacity()) {
                break;
            }

            RecordCursor.Owner owner = buildRecordItem(recordOwners, seq);
            sink.append(owner.cursor());
            count++;
        }

        retrievedCount.addAndGet(count);
        return false;
    }


    private RecordCursor.Owner buildRecordItem(List<RecordCursor.Owner> recordOwners, Seq seq) {
        int symbol_index;
        int variant_index;
        int rec_index;

        if (mode == Mode.RANDOM) {
            symbol_index = random.nextInt(symbols.size());
            variant_index = random.nextInt(DATA_VARIANTS);
            rec_index = random.nextInt(recordOwners.size());
        } else {
            symbol_index = ++seq.prevSymbolIndex % symbols.size();
            variant_index = ++seq.prevVariantIndex % DATA_VARIANTS;
            rec_index = ++seq.prevRecordIndex % recordOwners.size();
        }

        RecordCursor.Owner owner = recordOwners.get(rec_index);
        owner.setSymbol(symbols.getCipher(symbol_index), symbols.getSymbol(symbol_index));
        owner.setArrays(int_fields[rec_index][variant_index], obj_fields[rec_index][variant_index]);
        return owner;
    }

    private void initFieldGens(List<DataRecord> records) {
        int m = records.size();
        int_fields = new int[m][DATA_VARIANTS][];
        obj_fields = new Object[m][DATA_VARIANTS][];

        for (int i = 0; i < m; i++) {
            DataRecord record = records.get(i);

            for (int j = 0; j < DATA_VARIANTS; j++) {
                int_fields[i][j] = new int[record.getIntFieldCount()];
                for (int k = 0; k < record.getIntFieldCount(); k++) {
                    int_fields[i][j][k] = nextIntFieldValue(record.getIntField(k));
                }

                obj_fields[i][j] = new Object[record.getObjFieldCount()];
                for (int k = 0; k < record.getObjFieldCount(); k++) {
                    obj_fields[i][j][k] = nextObjectFieldValue(record.getObjField(k));
                }
            }
        }
    }

    private int nextIntFieldValue(DataIntField field) {
        if (field.getName().endsWith(".Time"))
            return (int) (System.currentTimeMillis() / 1000) + random.nextInt(10) - 9;
        if (field.getName().endsWith(".Stub"))
            return 0;
        if (field.getName().endsWith(".Sequence")) // always increasing number for unit-tests
            return sequence++;
        if (field instanceof DecimalField)
            return random_decimals[random.nextInt(random_decimals.length)];
        return random.nextInt(100);
    }

    private Object nextObjectFieldValue(DataObjField field) {
        if (field instanceof StringField)
            return "X" + (100 + random.nextInt(900));
        return null;
    }
}
