package in.rob.notification;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Calendar;

public class Logger
{
	public static void v(String prefix, String message)
	{
		o("V", prefix, message);
	}

	public static void e(String prefix, String message)
	{
		o("E", prefix, message);
	}

	public static void e(String prefix, Exception e)
	{
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		e.printStackTrace(pw);
		String out = sw.toString();
		String[] parts = out.split(System.getProperty("line.separator"));

		if (parts != null)
		{
			for (String s : parts)
			{
				o("E", prefix, s);
			}
		}
		else
		{
			o("E", prefix, out);
		}
	}

	private static void o(String level, String prefix, String message)
	{
		Calendar c = Calendar.getInstance();
		String date = c.get(Calendar.YEAR) + "-"
					+ padTo("" + c.get(Calendar.MONTH), 2, "0", true)
					+ "-" + padTo("" + c.get(Calendar.DAY_OF_MONTH), 2, "0", true)
					+ " " + padTo("" + c.get(Calendar.HOUR_OF_DAY), 2, "0", true)
					+ ":" + padTo("" + c.get(Calendar.MINUTE), 2, "0", true)
					+ ":" + padTo("" + c.get(Calendar.SECOND), 2, "0", true);

		System.out.println(date + "/" + level + " [" + prefix + "] " + message);
	}

	public static String padTo(String str, int maxSize, String chr, boolean padLeft)
	{
		int strLen = str.length();
		String newStr = str;

		if (strLen < maxSize)
		{
			String pad = "";
			for (int padCount = 0; padCount < maxSize - strLen; padCount++)
			{
				pad += chr;
			}

			if (padLeft)
			{
				newStr = pad + newStr;
			}
			else
			{
				newStr += pad;
			}
		}

		return newStr;
	}
}