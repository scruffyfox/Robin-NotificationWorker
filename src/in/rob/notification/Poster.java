package in.rob.notification;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.Charset;

import javax.net.ssl.HttpsURLConnection;

import org.apache.http.HttpEntity;
import org.apache.http.entity.StringEntity;

public class Poster
{
	public static String postData(String uri, String sendData)
	{
		try
		{
			HttpsURLConnection url = (HttpsURLConnection)new URL(uri).openConnection();
			url.addRequestProperty("Authorization", "key=" + Server.notificationToken);
			url.addRequestProperty("Content-Type", "application/json");
			url.setRequestProperty("Connection", "close");

			System.setProperty("http.keepAlive", "false");

			HttpEntity postData = new StringEntity(sendData, Charset.forName("utf-8"));

			url.setRequestMethod("POST");
			url.setDoInput(true);
			url.setDoOutput(true);
			url.setUseCaches(false);
			url.setInstanceFollowRedirects(true);

			// send the data
			if (true)
			{
				BufferedInputStream content = new BufferedInputStream(postData.getContent(), 8192);
				BufferedOutputStream wr = new BufferedOutputStream(url.getOutputStream(), 8192);

				byte[] buffer = new byte[8192];
				int len = 0;

				while ((len = content.read(buffer)) != -1)
				{
					wr.write(buffer, 0, len);
					wr.flush();
				}

				wr.close();
			}

			// Get the response
			InputStream i;
			int responseCode = url.getResponseCode();

			if ((responseCode / 100) == 2)
			{
				i = url.getInputStream();
			}
			else
			{
				i = url.getErrorStream();
			}

			InputStream is = new BufferedInputStream(i, 8192);
			byte[] buffer = new byte[8192];

			int len = 0;
			ByteArrayOutputStream bos = new ByteArrayOutputStream(Math.max(8192, url.getContentLength()));
			while ((len = is.read(buffer)) > -1)
			{
				bos.write(buffer, 0, len);
			}

			String test = new String(bos.toByteArray());

			is.close();
			i.close();
			url.disconnect();

			return test;
		}
		catch (Exception e)
		{
			Logger.e("LOGGER", e);
			Server.traces.add(e.getMessage());
		}

		return null;
	}
}