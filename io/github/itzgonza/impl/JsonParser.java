package io.github.itzgonza.impl;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.filechooser.FileSystemView;

import org.apache.commons.io.FileUtils;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

/**
 * @author ItzGonza
 */
public class JsonParser {
	
	private static final Pattern CARD_NUMBER = Pattern.compile("((?:(?:4\\d{3})|(?:5[1-5]\\d{2})|6(?:011|5[0-9]{2}))(?:-?|\\040?)(?:\\d{4}(?:-?|\\040?)){3}|(?:3[4,7]\\d{2})(?:-?|\\040?)\\d{6}(?:-?|\\040?)\\d{5})");
	private static final Pattern CARD_DATE = Pattern.compile("(^|\\D)((\\d{1,2})[\\\\\\.\\ \\/\\-]{1}(\\d{2}|\\d{4}))(\\D|$)");
	private static final Pattern CARD_CVV = Pattern.compile("(^|\\D)(\\d{3})(\\D|$)");

	private List<String> cards = new ArrayList<>();
	private boolean binStatus;
	
	public static transient JsonParser instance;
	
	public void initialize() throws Exception {
		System.out.println(getAscii());
		System.out.println("\n[1] with Binlist\n" + "[2] without Binlist\n");

		String choices = new Scanner(System.in).nextLine();

		if (!Stream.of("1", "2").anyMatch(choices::equals)) {
			System.out.println("\nyou entered the wrong format\nSTOPPED!");
			Runtime.getRuntime().halt(1337);
			return;
		}

		binStatus = choices.equals("1");

		String stringPath = "empty";
	    
		JFileChooser choose = new JFileChooser(FileSystemView.getFileSystemView().getHomeDirectory());
		JFrame frame = new JFrame();
	    
		frame.setAlwaysOnTop(true);
		choose.setVisible(true);
		choose.setDialogTitle("Select file (*.json only)");
		choose.setFileSelectionMode(JFileChooser.FILES_ONLY);
		choose.setAcceptAllFileFilterUsed(false);

		if (choose.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
			stringPath = choose.getSelectedFile().getAbsolutePath();
		}

		if (!stringPath.endsWith(".json")) {
			System.out.println("ERROR: Wrong file type (*.json only)");
			Thread.sleep(1000);
			System.out.println("STOPPED!");
			Runtime.getRuntime().halt(1337);
			return;
		}

		if (!"empty".equals(stringPath)) {
			System.out.println("\nFile (" + choose.getSelectedFile().getName() + " | " + (new File(choose.getSelectedFile().getAbsolutePath()).length() / 1024) / 1024 + "mb) compiling..\n");

			String reader = FileUtils.readFileToString(new File(stringPath), StandardCharsets.UTF_8.toString());
			JsonObject object = com.google.gson.JsonParser.parseString(reader).getAsJsonObject();
			JsonArray array = object.get("conversations").getAsJsonArray();

			array.forEach(conversation -> {
				JsonObject object2 = conversation.getAsJsonObject();
				JsonArray array2 = object2.get("MessageList").getAsJsonArray();

				array2.forEach(message -> {
					String content = message.getAsJsonObject().get("content").getAsString();
					try {
						findCard(content);
					} catch (Exception ignore) {}
				});
			});

			if (cards.isEmpty()) {
				System.out.println("nothing found card");
				return;
			}
			String fileName = stringPath.replace(".json", "") + "-output" + ".txt";

			cards = cards.stream().distinct().collect(Collectors.toList());
			cards.forEach(System.out::println);

			System.out.println("\ntotal found cards (" + cards.size() + ")");

			FileUtils.writeLines(new File(fileName), StandardCharsets.UTF_8.toString(), cards);
			FileUtils.writeStringToFile(new File(fileName), "\ntotal found cards (" + cards.size() + ")", StandardCharsets.UTF_8.toString(), true);

			System.out.println("\nSUCCESS: " + new File(fileName).getAbsolutePath());
        
			Runtime.getRuntime().halt(1337);
		}
	}

	private void findCard(String string) throws Exception {
		Matcher cardNumber = CARD_NUMBER.matcher(string), cardDate = CARD_DATE.matcher(string), cardCVV = CARD_CVV.matcher(string);
		if (cardNumber.find() && cardDate.find() && cardCVV.find() && !isExpired(monthFix(cardDate.group(3)), yearFix(cardDate.group(4)))) {
			String card = cardNumber.group();
			String month = monthFix(cardDate.group(3));
			String year = yearFix(cardDate.group(4));
			String cvv = cardCVV.group(2);

			String total = Stream.of(card, month, year, cvv)
					.map(this::o)
					.map(str -> isNumeric(str) ? str : "")
					.collect(Collectors.joining("|"));

			int cardLength = card.length();
			int monthValue = Integer.parseInt(month);
			int yearLength = year.length();
			int cvvLength = cvv.length();

			if (cardLength == 16 && monthValue > 0 && monthValue <= 12 && yearLength == 4 && Integer.parseInt(year) <= 2044 && cvvLength == 3 && Integer.parseInt(cvv) > 0) {
				if (binStatus) {
					for (String binLine : binList().split("\n")) {
						if (binLine.startsWith(card.substring(0, 6))) {
							total += " > " + binLine.substring(7).substring(binLine.substring(7).indexOf(",") + 1).replace(",", " ").replace(".", "");
							break;
						}
					}
				}
				cards.add(new String(total.getBytes(), StandardCharsets.UTF_8));
			}
		}
	}

	private String monthFix(String string) {
		return string.length() == 2 ? string : "0" + string;
	}

	private String yearFix(String string) {
		return string.length() == 4 ? string : "20" + string;
	}

	private boolean isExpired(String month, String year) {
		int cardYear = Integer.parseInt(year);
		int cardMonth = Integer.parseInt(month);
        
		int currentYear = Calendar.getInstance().get(Calendar.YEAR);
		int currentMonth = Calendar.getInstance().get(Calendar.MONTH) + 1;

		return cardYear < currentYear | (cardYear == currentYear && cardMonth < currentMonth);
	}

	private boolean isNumeric(String string) {
		return string.matches("-?\\d+(\\.\\d+)?");
	}

	private String binList() throws Exception {
		URL url = new URL("https://gist.githubusercontent.com/berkayunal/1595676/raw/debaa41dfd85d57b7681bcc46341f9bc0fa55682/Kredi%2520Kart%25C4%25B1%2520BIN%2520Listesi%2520-%2520CSV");
		return new BufferedReader(new InputStreamReader(url.openStream())).lines().collect(Collectors.joining("\n"));
	}

	private String o(String string) {
		return string.replaceAll("[\\s\\-+/.,\\\\\r\n\t]", "");
	}
	
	private String getAscii() {
		String ascii = 
				"   ___ _____  _____ _   _  ______  ___  ______  _____ ___________ \r\n" + 
				"  |_  /  ___||  _  | \\ | | | ___ \\/ _ \\ | ___ \\/  ___|  ___| ___ \\\r\n" + 
				"    | \\ `--. | | | |  \\| | | |_/ / /_\\ \\| |_/ /\\ `--.| |__ | |_/ /\r\n" + 
				"    | |`--. \\| | | | . ` | |  __/|  _  ||    /  `--. \\  __||    / \r\n" + 
				"/\\__/ /\\__/ /\\ \\_/ / |\\  | | |   | | | || |\\ \\ /\\__/ / |___| |\\ \\ \r\n" + 
				"\\____/\\____/  \\___/\\_| \\_/ \\_|   \\_| |_/\\_| \\_|\\____/\\____/\\_| \\_|\r\n" + 
				"                                                                  ";
		return new String(ascii);
	}
}
