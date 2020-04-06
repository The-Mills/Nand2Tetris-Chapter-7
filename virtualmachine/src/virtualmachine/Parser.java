package virtualmachine;

import java.io.FileNotFoundException;
import java.io.File;
import java.util.Scanner;
import java.util.ArrayList;
import java.util.List;

public class Parser 
{
	private Scanner reader = null;
	private List<String> vmCode = new ArrayList<String>();
	private int[] cTypes;	
	private int currentCommand = 0;
	private int numberCommands = 0;
	
	public void parseFile(String filePath) throws FileNotFoundException, IllegalArgumentException
	{
		if(!filePath.endsWith(".vm"))
			throw new IllegalArgumentException("ERROR: The file at " + filePath + " is not a .vm file. Skipping");
		
		File fileObject = new File(filePath);
		reader = new Scanner(fileObject);
		readAndTrimAll();
		reader.close();
		
		numberCommands = vmCode.size();
		cTypes = new int[numberCommands];
		getCommandTypes();
	}
	
	public boolean hasMoreCommands()
	{
		return currentCommand < numberCommands;
	}
	
	public void advanceCurrentCommand()
	{
		currentCommand++;
	}
	
	public String getCurrentCommand()
	{
		return vmCode.get(currentCommand);
	}
	
	public int getCurrentCommandType()
	{
		return cTypes[currentCommand];
	}
	
	public void resetParser()
	{
		if(reader != null)
			reader.close();
		vmCode.clear();
		numberCommands = 0;
		currentCommand = 0;
		cTypes = null;
	}
	
	/*
	 * This uses a 'dynamic' (entire code stored in RAM at once)
	 * approach. May also be prudent to implement single-line or hybrid
	 * approach for larger files. (maybe read one function at a time?)
	 */
	private void readAndTrimAll()
	{
		while(reader.hasNextLine())
		{
			String currentLine = reader.nextLine();						// nextLine() excludes the new line character at end of line. \n
			currentLine = currentLine.replaceAll("//.*+", "");          // Remove comments
			currentLine = currentLine.trim();                           // Remove leading and trailing whitespace
			currentLine = currentLine.replaceAll("\\s++", " ");         // Replace each possessive substring of whitespace with a single space.
			if(!currentLine.equals(""))									// Removes blank lines
				vmCode.add(currentLine);
		}
	}
	
	/*
	 * To do: optimize by calling it in readAndTrimAll?
	 */
	private void getCommandTypes()
	{
		int currentCommandIndex = 0;
		int upperCommandIndex = -1;
		String command = "";
		
		for(String x : vmCode)
		{
			upperCommandIndex = x.indexOf(" ");				// Get the upper index of the command substring
			if(upperCommandIndex == -1)
				upperCommandIndex = x.length();
			
			command = x.substring(0, upperCommandIndex);   // Get the command
			
			switch(command)
			{
				case "add": case "sub": case "neg": case "eq": case "gt": case "lt": case "and": case "or": case "not":
					cTypes[currentCommandIndex] = HackVM.C_ARITHMETIC;
					break;
				case "push":
					cTypes[currentCommandIndex] = HackVM.C_PUSH;
					break;
				case "pop":
					cTypes[currentCommandIndex] = HackVM.C_POP;
					break;
				case "label":
					cTypes[currentCommandIndex] = HackVM.C_LABEL;
					break;
				case "goto":
					cTypes[currentCommandIndex] = HackVM.C_GOTO;
					break;
				case "if-goto":
					cTypes[currentCommandIndex] = HackVM.C_IF;
					break;
				case "function":
					cTypes[currentCommandIndex] = HackVM.C_FUNCTION;
					break;
				case "call":
					cTypes[currentCommandIndex] = HackVM.C_CALL;
					break;
				case "return":
					cTypes[currentCommandIndex] = HackVM.C_RETURN;
					break;
				default:
					cTypes[currentCommandIndex] = HackVM.C_INVALID;
			}
			currentCommandIndex++;
		}
	}
}