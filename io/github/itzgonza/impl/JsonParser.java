package io.github.itzgonza.impl;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.filechooser.FileSystemView;

import org.apache.commons.io.FileUtils;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class JsonParser {

	// # REGEX
	private transient final Pattern number = Pattern.compile("((?:(?:4\\d{3})|(?:5[1-5]\\d{2})|6(?:011|5[0-9]{2}))(?:-?|\\040?)(?:\\d{4}(?:-?|\\040?)){3}|(?:3[4,7]\\d{2})(?:-?|\\040?)\\d{6}(?:-?|\\040?)\\d{5})");
	private transient final Pattern date = Pattern.compile("(^|\\D)((\\d{1,2})[\\\\\\.\\ \\/\\-]{1}(\\d{2}|\\d{4}))(\\D|$)");
	private transient final Pattern cvv = Pattern.compile("(^|\\D)(\\d{3})(\\D|$)");

	// # SWING
	private transient final JFileChooser choose = new JFileChooser(FileSystemView.getFileSystemView().getHomeDirectory());
	private transient final JFrame jra = new JFrame();
	
	// # LISTS
	private transient List<String> cards = new ArrayList<String>();
	
	// # CHOICES
	private transient boolean binStatus;
	
	public static transient JsonParser instance;
	
	public void initialize() throws Exception {
		System.out.println(getAscii());
		System.out.println("\n[1] with Binlist\n" + "[2] without Binlist\n");
		
        String choices = new Scanner(System.in).nextLine();
        
        switch(choices) {
        	case "1":
        		binStatus=Boolean.TRUE; 
        		break;
        	case "2":
        		binStatus=Boolean.FALSE;
        		break;
        }
        
		if ((choices).equals("1") | (choices).equals("2")) {
			String stringPath = "empty";

			jra.setAlwaysOnTop(true);
			choose.setVisible(true);
			choose.setDialogTitle("Select file (*.json only)");
			choose.setFileSelectionMode(JFileChooser.FILES_ONLY);
			choose.setAcceptAllFileFilterUsed(false);

			if (choose.showOpenDialog(jra) == JFileChooser.APPROVE_OPTION) {
				stringPath = choose.getSelectedFile().getAbsolutePath();
			}
			
			if (!(stringPath).endsWith(".json")) {
				System.out.println("ERROR: Wrong file type (*.json only)");
				Thread.sleep(1000);
				System.out.println("STOPPED!");
				Runtime.getRuntime().halt(1337);
			}
			
			if (!(stringPath).equals("empty")) {
				System.out.println("\nFile (" + choose.getSelectedFile().getName() + " | " + (new File(choose.getSelectedFile().getAbsolutePath()).length() / 1024) / 1024 + "mb) compiling..\n");
				
				String reader = FileUtils.readFileToString(new File(stringPath), StandardCharsets.UTF_8.toString());
				JsonObject object = new com.google.gson.JsonParser().parse(reader).getAsJsonObject();
				JsonArray array = object.get("conversations").getAsJsonArray();
				
				for (int i = 0; i < array.size(); ++i) {
					JsonObject object2 = array.get(i).getAsJsonObject();
					JsonArray array2 = object2.get("MessageList").getAsJsonArray();
					
					for (int a = 0; a < array2.size(); ++a) {
						String messages = array2.get(a).getAsJsonObject().get("content").getAsString();
						findCard(messages);
					}
				}
				
				if ((cards).size() > 0) {
					
					cards.stream().distinct().collect(Collectors.toList()).stream().forEach(x -> System.out.println(x));
					
					System.out.println("\ntotal found cards (" + cards.size() + ")");
					
					FileUtils.writeLines(new File(stringPath.replace(".json", "") + "-output" + ".txt"), StandardCharsets.UTF_8.toString(), cards);
					FileUtils.writeStringToFile(new File(stringPath.replace(".json", "") + "-output" + ".txt"), "\ntotal found cards (" + cards.size() + ")", StandardCharsets.UTF_8.toString(), true);
					
					System.out.println("\nSUCCESS: " + new File(stringPath.replace(".json", "") + "-output" + ".txt").getAbsolutePath());
					
					Runtime.getRuntime().halt(1337);
					
				} else {
					System.out.println("nothing found card");
					Runtime.getRuntime().halt(1337);
				}
			}
		} else {
			System.out.println("\nyou entered the wrong format\nSTOPPED!");
			Runtime.getRuntime().halt(1337);
		}
	}

	private void findCard(String string) throws Exception {
		Matcher cardNumber = number.matcher(string);
		Matcher cardDate = date.matcher(string);
		Matcher cardCVV = cvv.matcher(string);
		if (cardNumber.find() && cardDate.find() && cardCVV.find() && isExpired(monthFix(cardDate.group(3)), yearFix(cardDate.group(4)))) {
			String card, month, year, cvv;

			card = cardNumber.group();
			month = monthFix(cardDate.group(3));
			year = yearFix(cardDate.group(4));
			cvv = cardCVV.group(2);

			card = o(card);
			month = o(month);
			year = o(year);
			cvv = o(cvv);
			
			card = isNumeric(card) ? card : "";
			month = isNumeric(month) ? month : "";
			year = isNumeric(year) ? year : "";
			cvv = isNumeric(cvv) ? cvv : "";

			card = card.length() != 16 ? "" : card;
			month = month.length() != 2 || Double.parseDouble(month) > 12 ? "" : month;
			year = year.length() != 4 || Double.parseDouble(year) > 2042 ? "" : year;
			cvv = cvv.length() != 3 || Double.parseDouble(cvv) <= 0 ? "" : cvv;

			String total = (card + "|" + month + "|" + year + "|" + cvv);
			
			String[] binLines = binList().split("\n");

			if (binStatus) {
				for (int a = 0; a < binLines.length; ++a) {
					if (binLines[a].startsWith(total.substring(0, 6))) {
						total = total + " > " + binLines[a].substring(7).substring(binLines[a].substring(7).indexOf(",") + 1).replace(",", " ").replace(".", "");
					}
				}
			}
			if ((card + "|" + month + "|" + year + "|" + cvv).length() == 28) {
				cards.add(new String(total.getBytes(), StandardCharsets.UTF_8.toString()));
			}
		}
	}

	private String monthFix(String string) {
		boolean check = string.length() != 2;
		String string2;
		if (check) {
			string2 = "0" + string;
		}
		else {
			string2 = string;
		}
		return string2;
	}

	private String yearFix(String string) {
		boolean check = string.length() != 4;
		String string2;
		if (check) {
			string2 = "20" + string;
		}
		else {
			string2 = string;
		}
		return string2;
	}

	private boolean isExpired(String string, String string2) {
		boolean check, check2, check3, status;
		check = Integer.parseInt(string2) < 2022;
		check2 = Integer.parseInt(string2) == 2022;
		check3 = Integer.parseInt(string) < 11;
		if (check) {
			status = false;
		}
		else {
			if (check2) status = !check3;
			else status = true;
		}
		return status;
	}

	private boolean isNumeric(String string) {
		try {
			Double.parseDouble(string);
			return true;
		} catch(NumberFormatException e) { return false; }
	}

	private String binList() throws Exception {
		URL url = new URL("https://gist.githubusercontent.com/berkayunal/1595676/raw/debaa41dfd85d57b7681bcc46341f9bc0fa55682/Kredi%2520Kart%25C4%25B1%2520BIN%2520Listesi%2520-%2520CSV");
		BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(url.openStream()));
		StringBuilder stringBuilder = new StringBuilder();
		int C;
		while ((C = bufferedReader.read()) != -1) stringBuilder.append(((char)C));
		bufferedReader.close();
		return new String(stringBuilder);
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
	
	private String o(String string) {
		return string.replace(" ", "").replace("-", "").replace("+", "").replace("/", "").replace(".", "").replace(",", "").replace("\\", "").replace("\r", "").replace("\n", "").replace("\t", "");
	}
	
}