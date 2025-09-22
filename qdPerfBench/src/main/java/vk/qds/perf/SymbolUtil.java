package vk.qds.perf;

import com.devexperts.qd.SymbolCodec;
import com.devexperts.qd.kit.DefaultRecord;
import com.devexperts.qd.kit.DefaultScheme;
import com.devexperts.qd.kit.PentaCodec;
import com.devexperts.util.Timing;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SymbolUtil {
	public static final SymbolCodec CODEC = PentaCodec.INSTANCE;
	private static final int TODAY_ID = Timing.CST.today().day_id;
	private static final NumberFormat NUMBER_FORMAT = new DecimalFormat("##");
	private static final List<String> STRIKES = Stream.of(10, 20, 30, 40, 50)
			.map(NUMBER_FORMAT::format)
			.collect(Collectors.toList());
	private static final List<String> EXPIRATIONS = Stream.of(30, 60, 90, 120, 150)
			.map(e -> String.valueOf(Timing.CST.getById(TODAY_ID + e).year_month_day_number).substring(2))
			.collect(Collectors.toList());

	@NotNull
	public static List<String> getStocks() {
		List<String> stocks = loadObject("src/main/resources", "instruments-stockSymbols");
		// filter out weird symbols
		List<String> filteredStocks = stocks.stream()
				.filter(symbol -> 3 <= symbol.length() && symbol.length() <= 4)
				.filter(symbol -> symbol.charAt(0) > 'A')
				.collect(Collectors.toList());
		return filteredStocks;
	}

	public static List<String> createRealSymbols(int instrListCount) {
		List<String> filteredStocks = getStocks();
		filteredStocks = filteredStocks.subList(0, instrListCount);
		return createSymbolsImpl(filteredStocks);
	}

	private static List<String> createSymbolsImpl(List<String> underlyings) {
		List<String> result = new ArrayList<>();
		for (String underlying : underlyings) {
			result.addAll(createInstrumentList(underlying));
		}

		return result;
	}

	public static List<String> createFakeSymbols(int instrListCount) {
		List<String> underlyings = new ArrayList<>();
		for (int i = 0; i < instrListCount; i++) {
			underlyings.add(Long.toString(i, Character.MAX_RADIX).toUpperCase());
		}
		return createSymbolsImpl(underlyings);
	}

	public static List<String> createFakeSymbols3(int underlyingCount) {
		List<String> underlyings = new ArrayList<>();
		for (int i = 0; true; i ++ ) {
			if (0 < i % Character.MAX_RADIX && i % Character.MAX_RADIX < 10)
				continue;
			for (int c = 10; c <= Character.MAX_RADIX; c++) {
				underlyings.add(Long.toString((long) i * Character.MAX_RADIX + c, Character.MAX_RADIX).toUpperCase());

				if (underlyings.size() == underlyingCount) {
					return createSymbolsImpl(underlyings);
				}
			}
		}
	}

	/*
	public static List<String> createFakeSymbols2(int underlyingCount) {
		List<String> result = new ArrayList<>();

		int underlyingIndex = 0;
		for (char c1 = 'a'; c1 < 'z'; c1++) {
			String underlying = Character.toString(c1).toUpperCase();
			createSymbols(result, underlying);
			if (++underlyingIndex == underlyingCount) {
				return result;
			}
			for (char c2 = 'a'; c2 < 'z'; c2++) {
				underlying = new String(new char[]{c1, c2}).toUpperCase();
				createSymbols(result, underlying);
				if (++underlyingIndex == underlyingCount) {
					return result;
				}
				for (char c3 = 'a'; c3 < 'z'; c3++) {
					underlying = new String(new char[]{c1, c2, c3}).toUpperCase();
					createSymbols(result, underlying);
					if (++underlyingIndex == underlyingCount) {
						return result;
					}
					for (char c4 = 'a'; c4 < 'z'; c4++) {
						underlying = new String(new char[]{c1, c2, c3, c4}).toUpperCase();
						createSymbols(result, underlying);
						if (++underlyingIndex == underlyingCount) {
							return result;
						}
					}
				}
			}
		}
		return result;
	}
	 */


	private static List<String> createInstrumentList(String underlying) {
		List<String> instrumentList = new ArrayList<>();
		instrumentList.add(underlying);
		StringBuilder builder = new StringBuilder();

		for (String strike : STRIKES) {
			for (String expirationCode : EXPIRATIONS) {
				instrumentList.add(createOptionSymbol(builder, underlying, strike, expirationCode, 'C'));
				instrumentList.add(createOptionSymbol(builder, underlying, strike, expirationCode, 'P'));
			}
		}
		return instrumentList;
	}

	@NotNull
	private static String createOptionSymbol(StringBuilder builder, String underlying, String strike,
											 String expirationCode, char optionType)
	{
		builder.setLength(0);
		return builder.append(".").append(underlying).append(expirationCode).append(optionType).append(strike).toString();
	}

	public static List<String> createRandomSymbols(int count) {
		List<String> result = new ArrayList<>(count);

		Random random = ThreadLocalRandom.current();
		byte[] array = new byte[10];

		for (int i = 0; i < count; i++) {
			random.nextBytes(array);
			result.add(new String(array, StandardCharsets.UTF_8));
		}
		return result;
	}

	public static <T> T loadObject(String dir, String objId) {
		try (ObjectInputStream in = new ObjectInputStream(new BufferedInputStream(
				new FileInputStream(dir + "/" + objId))))
		{
			return (T) in.readObject();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static void storeObject(String dir, String objId, Object data) throws IOException {
		new File(dir).mkdirs();
		try (ObjectOutputStream out = new ObjectOutputStream(new BufferedOutputStream(
				new FileOutputStream(dir + "/" + objId))))
		{
			out.writeObject(data);
			out.flush();
		}
	}


	public static void printMainRecords(DefaultScheme scheme) {
		Predicate<DefaultRecord> predicate = record -> !record.getName().contains("&")
				&& !record.getName().contains("#")
				&& !record.getName().contains(".");
		printRecords(scheme, predicate);
	}

	public static void printRecords(DefaultScheme scheme, Predicate<DefaultRecord> predicate) {
		System.out.println(scheme.toString());
		System.out.println(scheme.getRecordCount());
		for (int rIndex = 0; rIndex < scheme.getRecordCount(); rIndex++) {
			DefaultRecord record = scheme.getRecord(rIndex);
			if (predicate.test(record)) {
				System.out.println(record);
				System.out.println("\tint records count:" + record.getIntFieldCount());
				for (int fIndex = 0; fIndex < record.getIntFieldCount(); fIndex++) {
					System.out.println("\t" + record.getIntField(fIndex));
				}
			}
		}
	}
}
