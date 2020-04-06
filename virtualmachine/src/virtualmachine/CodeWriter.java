package virtualmachine;

import java.io.PrintWriter;
import java.io.FileNotFoundException;
import java.io.File;

public class CodeWriter 
{
	private static final String RAEQLABEL    = "RET_ADDRESS_EQ";
	private static final String RAGTLABEL    = "RET_ADDRESS_GT";
	private static final String RALTLABEL    = "RET_ADDRESS_LT";
	private static final String RAFLABEL     = "RET_ADDRESS_F";
	private int raEQCount                    = 0;
	private int raGTCount                    = 0;
	private int raLTCount                    = 0;
	private int raFCount                     = 0;
	
	private final String INITIALIZESPOS      = "@256\nD=A\n@SP\nM=D\n";
	private final String EQOS                = "(OS_EQ)\n@OS_EQ_FALSE\nD;JNE\n@SP\nA=M\nM=-1\n@SP\nM=M+1\n@R15\nA=M\n0;JMP\n(OS_EQ_FALSE)\n@SP\nA=M\nM=0\n@SP\nM=M+1\n@R15\nA=M\n0;JMP\n";
	private final String GTOS                = "(OS_GT)\n@OS_GT_FALSE\nD;JLE\n@SP\nA=M\nM=-1\n@SP\nM=M+1\n@R15\nA=M\n0;JMP\n(OS_GT_FALSE)\n@SP\nA=M\nM=0\n@SP\nM=M+1\n@R15\nA=M\n0;JMP\n";
	private final String LTOS				 = "(OS_LT)\n@OS_LT_FALSE\nD;JGE\n@SP\nA=M\nM=-1\n@SP\nM=M+1\n@R15\nA=M\n0;JMP\n(OS_LT_FALSE)\n@SP\nA=M\nM=0\n@SP\nM=M+1\n@R15\nA=M\n0;JMP\n";
	private final String OS                  = INITIALIZESPOS + "@Sys.init\n0;JMP\n" +  EQOS + GTOS + LTOS +"(Sys.init)\n";
	
	private static final String SUB          = "@SP\nM=M-1\nA=M\nD=!M\nD=D+1\n@SP\nM=M-1\nA=M\nM=D+M\n@SP\nM=M+1\n";

	private PrintWriter writer = null;
	private String currentFileName = "";
	private String currentFunction = "";
	private String currentCommand = "";			// The entire command, not just the command word.
	private String currentFirstParameter = "";
	private int currentSecondParameter = -1;
	private int currentType = -1;
	private int numberOfErrors = 0;
	
	public String createFile(String filePath) throws FileNotFoundException
	{
		String asmFilePath = filePath.substring(0, filePath.length() - 2) + "asm";
		File asmFileObject = new File(asmFilePath);
		writer = new PrintWriter(asmFileObject);
		
		writer.print(OS);
		
		return asmFilePath;
	}
	
	public void setCurrentFile(String filePath)
	{
		int lastSlashIndex = filePath.lastIndexOf("\\") + 1;
		currentFileName = filePath.substring(lastSlashIndex, filePath.lastIndexOf(".vm"));
	}
	
	public int getNumberOfErrors()
	{
		return numberOfErrors;
	}
	
	public void writeCommand(String command, int type)
	{
		currentCommand = command;
		currentType = type;
		switch(currentType)
		{
			case HackVM.C_PUSH: case HackVM.C_POP: case HackVM.C_FUNCTION: case HackVM.C_CALL:
				writeTwoArgs();
				break;
			case HackVM.C_ARITHMETIC:
				writeArithmetic();
				break;
			case HackVM.C_LABEL: case HackVM.C_GOTO: case HackVM.C_IF:
				writeOneArg();
				break;
			case HackVM.C_RETURN:
				writeReturn();
				break;
			case HackVM.C_INVALID:
				writer.println("ERROR: Cannot translate [" + currentCommand + "] - Reason: Invalid command.");
				numberOfErrors++;
				break;
			default:
				writer.println("ERROR: Cannot translate [" + currentCommand + "] - Reason: Unknown.");
				numberOfErrors++;
		}
	}
	
	private void writeReturn()
	{
		writer.print("@LCL\nD=M\n@5\nD=D-A\nA=D\nD=M\n@R15\nM=D\n");          // Save return address in R15
		writer.print("@SP\nAM=M-1\nD=M\n@ARG\nA=M\nM=D\n");                   // Move up return value
		writer.print("@ARG\nD=M+1\n@SP\nM=D\n");							  // Restore SP
		writer.print("@LCL\nD=M\n@1\nD=D-A\nA=D\nD=M\n@THAT\nM=D\n");		  // Restore THAT
		writer.print("@LCL\nD=M\n@2\nD=D-A\nA=D\nD=M\n@THIS\nM=D\n");		  // Restore THIS
		writer.print("@LCL\nD=M\n@3\nD=D-A\nA=D\nD=M\n@ARG\nM=D\n");		  // Restore ARG
		writer.print("@LCL\nD=M\n@4\nD=D-A\nA=D\nD=M\n@LCL\nM=D\n");          // Restore LCL
		writer.print("@R15\nA=M\n0;JMP\n");
	}
	
	private void writeArithmetic()
	{
		switch(currentCommand)
		{
			case "add":
				writer.print("@SP\nM=M-1\nA=M\nD=M\n@SP\nM=M-1\nA=M\nM=D+M\n@SP\nM=M+1\n");
				break;
			case "sub":
				writer.print(SUB);
				break;
			case "neg":
				writer.print("@SP\nM=M-1\nA=M\nM=!M\nM=M+1\n@SP\nM=M+1\n");
				break;
			case "and":
				writer.print("@SP\nM=M-1\nA=M\nD=M\n@SP\nM=M-1\nA=M\nM=D&M\n@SP\nM=M+1\n");
				break;
			case "or":
				writer.print("@SP\nM=M-1\nA=M\nD=M\n@SP\nM=M-1\nA=M\nM=D|M\n@SP\nM=M+1\n");
				break;
			case "not":
				writer.print("@SP\nM=M-1\nA=M\nM=!M\n@SP\nM=M+1\n");
				break;
			case "eq":
				writer.print(SUB + "@" + RAEQLABEL + raEQCount + "\nD=A\n@R15\nM=D\n@SP\nAM=M-1\nD=M\n@OS_EQ\n0;JMP\n(" + RAEQLABEL + raEQCount + ")\n");
				raEQCount++;
				break;
			case "gt":
				writer.print(SUB + "@" + RAGTLABEL + raGTCount + "\nD=A\n@R15\nM=D\n@SP\nAM=M-1\nD=M\n@OS_GT\n0;JMP\n(" + RAGTLABEL + raGTCount + ")\n");
				raGTCount++;
				break;
			case "lt":
				writer.print(SUB + "@" + RALTLABEL + raLTCount + "\nD=A\n@R15\nM=D\n@SP\nAM=M-1\nD=M\n@OS_LT\n0;JMP\n(" + RALTLABEL + raLTCount + ")\n");
				raLTCount++;
				break;
			default:
				writer.println("ERROR: Cannot translate [" + currentCommand + "] - Reason: Unknown");
				numberOfErrors++;
		}
	}
	
	private void writeOneArg()
	{
		if(!currentCommand.matches("\\S++ \\S++"))
		{
			writer.println("ERROR: Cannot translate [" + currentCommand + "] - Reason: Invalid number of parameters");
			numberOfErrors++;
			return;
		}
		currentFirstParameter = currentCommand.substring(currentCommand.indexOf(" ") + 1, currentCommand.length());
		
		switch(currentType)
		{
			case HackVM.C_LABEL:
				writer.print("(" + currentFunction + "$" + currentFirstParameter + ")\n");
				break;
			case HackVM.C_GOTO:
				writer.print("@" + currentFunction + "$" + currentFirstParameter + "\n0;JMP\n");
				break;
			case HackVM.C_IF:
				writer.print("@SP\nAM=M-1\nD=M\n@" + currentFunction + "$" + currentFirstParameter + "\nD;JNE\n");
				break;
			default:
				writer.println("ERROR: Cannot translate [" + currentCommand + "] - Reason: Unknown");
		}
	}
	
	private void writeTwoArgs()
	{
		if(!currentCommand.matches("\\S++ \\S++ \\S++"))
		{
			writer.println("ERROR: Cannot translate [" + currentCommand + "] - Reason: Invalid number of parameters.");
			numberOfErrors++;
			return;
		}
		
		int secondSpace = currentCommand.lastIndexOf(" ");
		currentFirstParameter = currentCommand.substring(currentCommand.indexOf(" ") + 1, secondSpace);
		currentSecondParameter = Integer.parseInt(currentCommand.substring(secondSpace + 1, currentCommand.length()));
		switch(currentType)
		{
			case HackVM.C_PUSH:
				writePush();
				break;
			case HackVM.C_POP:
				writePop();
				break;
			case HackVM.C_FUNCTION:
				writeFunction();
				currentFunction = currentFirstParameter;
				break;
			case HackVM.C_CALL:
				writeCall();
				raFCount++;
				break;
			default:
				writer.println("ERROR: Cannot translate [" + currentCommand + "] - Reason: Unknown");
				
		}
	}
	
	private void writeFunction()
	{
		writer.print("(" + currentFirstParameter + ")\n");
		for(int i = 0; i < currentSecondParameter; i++)
			writer.print("@SP\nM=M+1\nA=M-1\nM=0\n");
	}
	
	private void writeCall()
	{
		writer.print("@" + RAFLABEL + raFCount + "\nD=A\n@SP\nM=M+1\nA=M-1\nM=D\n");                // Store RA
		writer.print("@LCL\nD=M\n@SP\nM=M+1\nA=M-1\nM=D\n");                                        // Store LCL
		writer.print("@ARG\nD=M\n@SP\nM=M+1\nA=M-1\nM=D\n");							            // Store ARG
		writer.print("@THIS\nD=M\n@SP\nM=M+1\nA=M-1\nM=D\n");							            // Store THIS
		writer.print("@THAT\nD=M\n@SP\nM=M+1\nA=M-1\nM=D\n");							            // Store THAT
		writer.print("@SP\nD=M\n@" + currentSecondParameter + "\nD=D-A\n@5\nD=D-A\n@ARG\nM=D\n");   // Set ARG value
		writer.print("@SP\nD=M\n@LCL\nM=D\n");														// Set LCL value
		writer.print("@" + currentFirstParameter + "\n0;JMP\n");									// Jump to function
		writer.print("(" + RAFLABEL + raFCount + ")\n");								            // Return Location
	}
	
	private void writePush()
	{
		switch(currentFirstParameter)
		{
			case "local":
				if(currentSecondParameter < 0)
				{
					writer.println("ERROR: Cannot translate [" + currentCommand + "] - Reason: Index value is out of bounds.");
					numberOfErrors++;
				}
				else
					writer.print("@" + currentSecondParameter + "\nD=A\n@LCL\nA=M+D\nD=M\n@SP\nM=M+1\nA=M-1\nM=D\n");
				break;
				
				
			
			case "argument":
				if(currentSecondParameter < 0)
				{
					writer.println("ERROR: Cannot translate [" + currentCommand + "] - Reason: Index value is out of bounds.");
					numberOfErrors++;
				}
				else
					writer.print("@" + currentSecondParameter + "\nD=A\n@ARG\nA=M+D\nD=M\n@SP\nM=M+1\nA=M-1\nM=D\n");
				break;
			
			
			
			case "this":
				if(currentSecondParameter < 0)
				{
					writer.println("ERROR: Cannot translate [" + currentCommand + "] - Reason: Index value is out of bounds.");
					numberOfErrors++;
				}
				else
					writer.print("@" + currentSecondParameter + "\nD=A\n@THIS\nA=M+D\nD=M\n@SP\nM=M+1\nA=M-1\nM=D\n");
				break;
			
			
			
			case "that":
				if(currentSecondParameter < 0)
				{
					writer.println("ERROR: Cannot translate [" + currentCommand + "] - Reason: Index value is out of bounds.");
					numberOfErrors++;
				}
				else
					writer.print("@" + currentSecondParameter + "\nD=A\n@THAT\nA=M+D\nD=M\n@SP\nM=M+1\nA=M-1\nM=D\n");
				break;
				
				
		
			case "constant":
				if(currentSecondParameter < 0 || currentSecondParameter > 32767)
				{
					writer.println("ERROR: Cannot translate [" + currentCommand + "] - Reason: Constant value is outside the acceptable ranges of values.");
					numberOfErrors++;
				}
				else
					writer.print("@" + currentSecondParameter + "\nD=A\n@SP\nA=M\nM=D\n@SP\nM=M+1\n");
				break;
			
				
			case "temp":
				if(currentSecondParameter < 0 || currentSecondParameter > 7)
				{
					writer.println("ERROR: Cannot translate [" + currentCommand + "] - Reason: Index value is out of bounds.");
					numberOfErrors++;
				}
				else
					writer.print("@R" + (currentSecondParameter + 5) + "\nD=M\n@SP\nM=M+1\nA=M-1\nM=D\n");
				break;
			
				
				
			case "pointer":
				if(currentSecondParameter < 0 || currentSecondParameter > 1)
				{
					writer.println("ERROR: Cannot translate [" + currentCommand + "] - Reason: Index value is out of bounds.");
					numberOfErrors++;
				}
				else
					writer.print("@R" + (currentSecondParameter + 3) + "\nD=M\n@SP\nM=M+1\nA=M-1\nM=D\n");
				break;
			
			case "static":
				if(currentSecondParameter < 0)
				{
					writer.println("ERROR: Cannot translate [" + currentCommand + "] - Reason: Index value is out of bounds.");
					numberOfErrors++;
				}
				else
					writer.print("@" + currentFileName + "." + currentSecondParameter + "\nD=M\n@SP\nM=M+1\nA=M-1\nM=D\n");
				break;
				
				
			default:
				writer.println("ERROR: Cannot translate [" + currentCommand + "] - Reason: Invalid first parameter.");
				numberOfErrors++;
		}
	}
	
	private void writePop()
	{
		switch(currentFirstParameter)
		{
			case "local":
				if(currentSecondParameter < 0)
				{
					writer.println("ERROR: Cannot translate [" + currentCommand + "] - Reason: Index value is out of bounds.");
					numberOfErrors++;
				}
				else
					writer.print("@" + currentSecondParameter + "\nD=A\n@LCL\nA=M+D\nD=A\n@R15\nM=D\n@SP\nAM=M-1\nD=M\n@R15\nA=M\nM=D\n");
				break;
			
				
				
			case "argument":
				if(currentSecondParameter < 0)
				{
					writer.println("ERROR: Cannot translate [" + currentCommand + "] - Reason: Index value is out of bounds.");
					numberOfErrors++;
				}
				else
					writer.print("@" + currentSecondParameter + "\nD=A\n@ARG\nA=M+D\nD=A\n@R15\nM=D\n@SP\nAM=M-1\nD=M\n@R15\nA=M\nM=D\n");
				break;
			
				
				
			case "this":
				if(currentSecondParameter < 0)
				{
					writer.println("ERROR: Cannot translate [" + currentCommand + "] - Reason: Index value is out of bounds.");
					numberOfErrors++;
				}
				else
					writer.print("@" + currentSecondParameter + "\nD=A\n@THIS\nA=M+D\nD=A\n@R15\nM=D\n@SP\nAM=M-1\nD=M\n@R15\nA=M\nM=D\n");
				break;
			
				
				
			case "that":
				if(currentSecondParameter < 0)
				{
					writer.println("ERROR: Cannot translate [" + currentCommand + "] - Reason: Index value is out of bounds.");
					numberOfErrors++;
				}
				else
					writer.print("@" + currentSecondParameter + "\nD=A\n@THAT\nA=M+D\nD=A\n@R15\nM=D\n@SP\nAM=M-1\nD=M\n@R15\nA=M\nM=D\n");
				break;
				
				
				
			case "temp":
				if(currentSecondParameter < 0 || currentSecondParameter > 7)
				{
					writer.println("ERROR: Cannot translate [" + currentCommand + "] - Reason: Index value is out of bounds.");
					numberOfErrors++;
				}
				else
					writer.print("@SP\nAM=M-1\nD=M\n@R" + (currentSecondParameter + 5) + "\nM=D\n");
				break;
			
				
				
			case "pointer":
				if(currentSecondParameter < 0 || currentSecondParameter > 1)
				{
					writer.println("ERROR: Cannot translate [" + currentCommand + "] - Reason: Index value is out of bounds.");
					numberOfErrors++;
				}
				else
					writer.print("@SP\nAM=M-1\nD=M\n@R" + (currentSecondParameter + 3) + "\nM=D\n");
				break;
			
				
				
			case "static":
				if(currentSecondParameter < 0)
				{
					writer.println("ERROR: Cannot translate [" + currentCommand + "] - Reason: Index value is out of bounds.");
					numberOfErrors++;
				}
				else
					writer.print("@SP\nAM=M-1\nD=M\n@" + currentFileName + "." + currentSecondParameter + "\nM=D\n");
				break;
					
				
				
			default:
				writer.println("ERROR: Cannot translate [" + currentCommand + "] - Reason: Invalid first parameter.");
				numberOfErrors++;
		}
	}
	
	public void closeWriter()
	{
		if(writer != null)
			writer.close();
	}

}