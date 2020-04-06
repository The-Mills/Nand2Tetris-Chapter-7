package virtualmachine;

public class RegexTest 
{
	public static void main(String[] args)
	{
		String test = "push constant 29";
		System.out.println(test.matches("push \\S++ \\S++"));
	}
}
