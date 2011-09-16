package com.vaguehope.mikuru;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

public class FileHelper {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private FileHelper () {}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	static public List<String> fileToList (File file) throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader(file));
		List<String> ret = new LinkedList<String>();
		String line;
		while ((line = reader.readLine()) != null) {
			if (line.length() > 0) ret.add(line);
		}
		return ret;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
