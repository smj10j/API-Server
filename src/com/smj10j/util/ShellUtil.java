package com.smj10j.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;

import com.smj10j.conf.FatalException;

public abstract class ShellUtil {
	
	/**
	 * @param cmd - Executes this command 
	 * @param input - If provided, will enter the items of the list one by one as input into the opened process
	 * @return
	 * @throws FatalException
	 */
	public static List<String> exec(String cmd, List<String> input) throws FatalException {
		try {
			List<String> output = new ArrayList<String>();
			Runtime run = Runtime.getRuntime();
			Process pr = run.exec(cmd);

			if(input != null) {
				BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(pr.getOutputStream()));
				for(String line : input) {
					writer.write(line);
					writer.newLine();
				}
				writer.close();
			}
			
			BufferedReader buf = new BufferedReader(new InputStreamReader(pr.getInputStream()));
			String line = "";
			while ((line=buf.readLine()) != null) {
				output.add(line);
			}
			return output;
		}catch(IOException e) {
			throw new FatalException(e);			
		} 
	}
	
}
