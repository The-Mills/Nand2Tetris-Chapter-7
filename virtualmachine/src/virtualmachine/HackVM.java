package virtualmachine;

import java.io.FileNotFoundException;

public class HackVM 
{
	public static final int C_ARITHMETIC = 0;
	public static final int C_PUSH = 1;
	public static final int C_POP = 2;
	public static final int C_LABEL = 3;
	public static final int C_GOTO = 4;
	public static final int C_IF = 5;
	public static final int C_FUNCTION = 6;
	public static final int C_RETURN = 7;
	public static final int C_CALL = 8;
	public static final int C_INVALID = -1;
	
	public static void main(String[] args)
	{
		String[] filePaths = new String[] {"D:\\3650 Projects\\nand2tetris\\projects\\07\\MemoryAccess\\BasicTest\\BasicTest.vm"};
		if(filePaths.length == 0)
		{
			System.out.println("ERROR: Please enter at least one .vm file path.");
			System.exit(0);
		}
		
		Parser theParser = new Parser();
		CodeWriter theWriter = new CodeWriter();
		
		String currentPath = "";
		boolean asmFileCreated = false;
		for(int i = 0; i < filePaths.length; i++)
		{
			try
			{
				currentPath = filePaths[i];
				theParser.parseFile(currentPath);
				if(!asmFileCreated)
				{
					String asmFilePath = theWriter.createFile(currentPath);
					asmFileCreated = true;
					System.out.println("File successfully created at " + asmFilePath);
				}
				theWriter.setCurrentFile(currentPath);
				while(theParser.hasMoreCommands())
				{
					theWriter.writeCommand(theParser.getCurrentCommand(), theParser.getCurrentCommandType());
					theParser.advanceCurrentCommand();
				}
			}
			catch(FileNotFoundException e)
			{
				System.out.println("ERROR: File at " + currentPath + " does not exist. Skipping.");
			}
			catch(IllegalArgumentException e)
			{
				System.out.println(e);	
			}
			finally
			{
				theParser.resetParser();
			}
		}
		int numberOfErrors = theWriter.getNumberOfErrors();
		if(numberOfErrors > 0)
			System.out.println(theWriter.getNumberOfErrors() + " lines contained errors or could not be translated properly. This code will not execute.");
		else
			System.out.println("No errors detected.");
		theWriter.closeWriter();
	}
}