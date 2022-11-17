package io.github.itzgonza;

import io.github.itzgonza.impl.JsonParser;

public class Start {

	public static void main(String[] argument) throws Exception {
		if (JsonParser.instance == null)
		    JsonParser.instance = new JsonParser();
		JsonParser.instance.initialize();
	}
	
}