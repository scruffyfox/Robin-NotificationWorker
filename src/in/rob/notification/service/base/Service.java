package in.rob.notification.service.base;

import in.rob.notification.Logger;
import in.rob.notification.Poster;
import in.rob.notification.Server;
import in.rob.notification.ServerCallback;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.Charset;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class Service
{
	protected ServerCallback callback;
	protected Connection connection;

	protected int ENABLE_INT = 0;
	protected String LOGPREFIX = "";
	protected String PRODUCTION_STREAM = "";
	protected String DEVELOPMENT_STREAM = "";

	private Thread handler;

	public Service(ServerCallback callback)
	{
		this.callback = callback;
	}

	public void connect()
	{
		try
		{
			Logger.v(LOGPREFIX, "Loading mysql driver...");

			Class.forName("com.mysql.jdbc.Driver");
			connection = null;

			Logger.v(LOGPREFIX, "Connecting to database database...");
			connection = DriverManager.getConnection(Server.db_url, Server.db_username, Server.db_password);
			Logger.v(LOGPREFIX, "Database connected!");
		}
		catch (Exception e)
		{
			Logger.e(LOGPREFIX, e);
			Server.traces.add(e.getMessage());

			callback.onClose();
			throw new RuntimeException("Cannot find the driver in the classpath!", e);
		}
	}

	public void execute()
	{
		handler = new Thread(new Runnable()
		{
			public void run()
			{
				try
				{
					final SSLContext sslContext = SSLContext.getInstance("SSL");
					sslContext.init(null, Server.trustAllCerts, new java.security.SecureRandom());
					final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

					HttpsURLConnection urlCon;

					if (Server.production)
					{
						urlCon = (HttpsURLConnection)new URL(PRODUCTION_STREAM).openConnection();
					}
					else
					{
						urlCon = (HttpsURLConnection)new URL(DEVELOPMENT_STREAM).openConnection();
					}

					urlCon.setSSLSocketFactory(sslSocketFactory);
					InputStream input = urlCon.getInputStream();

					BufferedReader in = new BufferedReader(new InputStreamReader(input, Charset.forName("utf-8")));

					String inputLine = "";
					int i = 0;
					char[] buffer = new char[8192];
					File f = new File("restart.txt");
					long startTime = f.lastModified();

					while ((i = in.read(buffer)) != -1)
					{
						inputLine += new String(buffer, 0, i);

						if (inputLine.endsWith("\r\n"))
						{
							if (f.lastModified() - startTime > 0)
							{
								break;
							}

							String[] parts = inputLine.split("(\r\n)");

							for (String p : parts)
							{
								sendNotification(p);
							}

							inputLine = "";
						}
					}

					in.close();
					input.close();
					urlCon.disconnect();
					connection.close();

					Logger.v(LOGPREFIX, "Database connection closed");
					callback.onClose();
					Server.sendClosedEmail();
				}
				catch (Exception e)
				{
					Logger.e(LOGPREFIX, e);
					Server.traces.add(e.getMessage());
				}
			}
		});

		handler.start();
	}

	public void stop()
	{
		try
		{
			handler.interrupt();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	public boolean isUserFollowing(String accessToken, String checkid)
	{
		try
		{
			Server.TrustManagerManipulator.allowAllSSL();
			HttpsURLConnection url = (HttpsURLConnection)new URL("https://alpha-api.app.net/stream/0/users/" + checkid + "?access_token=" + accessToken).openConnection();
			url.setRequestProperty("Connection", "close");
			System.setProperty("http.keepAlive", "false");

			url.setRequestMethod("GET");
			url.setDoInput(true);
			url.setUseCaches(false);
			url.setInstanceFollowRedirects(true);

			// get response
			if (true)
			{
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
				int readCount = 0;
				ByteArrayOutputStream bos = new ByteArrayOutputStream(Math.max(8192, url.getContentLength()));
				while ((len = is.read(buffer)) > -1)
				{
					bos.write(buffer, 0, len);
					readCount += len;
				}

				String test = new String(bos.toByteArray());

				is.close();
				i.close();
				url.disconnect();

				JsonObject user = new JsonParser().parse(test).getAsJsonObject().get("data").getAsJsonObject();
				return user.get("you_follow").getAsBoolean();
			}
		}
		catch (Exception e)
		{
			Logger.e(LOGPREFIX, e);
			Server.traces.add(e.getMessage());
		}

		return true;
	}

	public void sendNotification(final String notificationObject)
	{
		try
		{
			if (connection == null || connection.isClosed())
			{
				connect();
			}
		}
		catch (Exception e)
		{
			Logger.e(LOGPREFIX, e);
			stop();
		}
	}

	public void postNotification(String sendData, String userId, List<String> deviceIds)
	{
		String response = Poster.postData("https://android.googleapis.com/gcm/send", sendData.toString());
		Server.traces.add(response);

		try
		{
			JsonObject resp = new JsonParser().parse(response).getAsJsonObject();
			if (resp.get("failure").getAsInt() > 0)
			{
				JsonArray items = resp.get("results").getAsJsonArray();
				int index = 0;
				for (JsonElement f : items)
				{
					if (f.getAsJsonObject().has("error"))
					{
						Statement del = connection.createStatement();
						del.execute("DELETE FROM devices WHERE user_id = '" + userId + "' AND id = '" + deviceIds.get(index) + "'");
						del.close();
					}

					index++;
				}
			}
		}
		catch (Exception e2)
		{
			Logger.e(LOGPREFIX, e2);
			Logger.e(LOGPREFIX, response);
		}
	}
}