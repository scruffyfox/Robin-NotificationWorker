package in.rob.notification;

import in.rob.notification.service.FollowerService;
import in.rob.notification.service.MentionsService;
import in.rob.notification.service.MessagesService;
import in.rob.notification.service.PatterMentionsService;
import in.rob.notification.type.FIFOArrayList;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import com.sun.mail.smtp.SMTPTransport;

public class Server
{
	public static final String db_url = "jdbc:mysql://localhost:3306/robin_notifications_v1_2?useOldUTF8Behavior=true";
	public static final String db_username = "root";
	public static final String db_password = "";

	public static boolean testService = false, mentionsService = false, pmService = false, patterPmService = false, follows = false;
	public static boolean production = false;

	public static final String notificationToken = "";
	public static final FIFOArrayList<String> traces = new FIFOArrayList<String>(100);

	public static MessagesService _messages;
	public static MentionsService _mentions;
	public static PatterMentionsService _patter;
	public static FollowerService _follow;

	public static void main(String[] args) throws Exception
	{
		ServerCallback callback = new ServerCallback()
		{
			public void onClose()
			{
				Logger.e("SERVER", "Server has been closed");

				if (_messages != null)
				{
					_messages.stop();
				}

				if (_mentions != null)
				{
					_mentions.stop();
				}

				if (_patter != null)
				{
					_patter.stop();
				}

				if (_follow != null)
				{
					_follow.stop();
				}

				sendClosedEmail();
				System.exit(1);
			}
		};

		for (int index = 0; index < args.length; index++)
		{
			String arg = args[index];

			if (arg.equals("-e"))
			{
				production = args[index + 1].equals("production");
			}

			if (arg.equals("-s"))
			{
				String serviceStr = args[index + 1];
				String[] services = serviceStr.split(" ");

				for (String service : services)
				{
					if (service.equals("test"))
					{
						testService = true;
					}
					else if (service.equals("mentions"))
					{
						mentionsService = true;
					}
					else if (service.equals("pms"))
					{
						pmService = true;
					}
					else if (service.equals("patter-pms"))
					{
						patterPmService = true;
					}
					else if (service.equals("follows"))
					{
						follows = true;
					}
				}
			}
		}

		if (pmService)
		{
			_messages = new MessagesService(callback);
			_messages.execute();
		}

		if (mentionsService)
		{
			_mentions = new MentionsService(callback);
			_mentions.execute();
		}

		if (patterPmService)
		{
			_patter = new PatterMentionsService(callback);
			_patter.execute();
		}

		if (follows)
		{
			_follow = new FollowerService(callback);
			_follow.execute();
		}
	}

	public static void send(final String username, final String password, String recipientEmail, String ccEmail, String title, String message) throws AddressException, MessagingException
	{
		final String SSL_FACTORY = "javax.net.ssl.SSLSocketFactory";

		// Get a Properties object
		Properties props = System.getProperties();
		props.setProperty("mail.smtps.host", "smtp.gmail.com");
		props.setProperty("mail.smtp.socketFactory.class", SSL_FACTORY);
		props.setProperty("mail.smtp.socketFactory.fallback", "false");
		props.setProperty("mail.smtp.port", "465");
		props.setProperty("mail.smtp.socketFactory.port", "465");
		props.setProperty("mail.smtps.auth", "true");
		props.put("mail.smtps.quitwait", "false");

		Session session = Session.getInstance(props, null);

		// -- Create a new message --
		final MimeMessage msg = new MimeMessage(session);

		// -- Set the FROM and TO fields --
		msg.setFrom(new InternetAddress(username + "@gmail.com"));
		msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipientEmail, false));

		if (ccEmail.length() > 0)
		{
			msg.setRecipients(Message.RecipientType.CC, InternetAddress.parse(ccEmail, false));
		}

		msg.setSubject(title);
		msg.setText(message, "utf-8");
		msg.setSentDate(new Date());

		SMTPTransport t = (SMTPTransport)session.getTransport("smtps");

		t.connect("smtp.gmail.com", username, password);
		t.sendMessage(msg, msg.getAllRecipients());
		t.close();
	}

	public static void sendClosedEmail()
	{
		try
		{
			String messageStr = "";

			for (String s : traces)
			{
				messageStr += "\r\n" + s;
			}

			send("", "", "", "", "*** NOTIFICATION OFFLINE ***", "Notification server is offline. " + messageStr);
			System.out.println("Sent message successfully....");
		}
		catch (MessagingException mex)
		{
			mex.printStackTrace();
		}
	}

	public final static TrustManager[] trustAllCerts = new TrustManager[]
	{
		new X509TrustManager()
		{
			public void checkClientTrusted(final X509Certificate[] chain, final String authType)
			{
			}

			public void checkServerTrusted(final X509Certificate[] chain, final String authType)
			{
			}

			public X509Certificate[] getAcceptedIssuers()
			{
				return null;
			}
		}
	};

	public static class TrustManagerManipulator implements X509TrustManager
	{
		private static TrustManager[] trustManagers;
		private static final X509Certificate[] acceptedIssuers = new X509Certificate[]{};

		public boolean isClientTrusted(X509Certificate[] chain)
		{
			return true;
		}

		public boolean isServerTrusted(X509Certificate[] chain)
		{
			return true;
		}

		public static void allowAllSSL()
		{
			HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier()
			{
				public boolean verify(String hostname, SSLSession session)
				{
					return true;
				}
			});

			SSLContext context = null;

			if (trustManagers == null)
			{
				trustManagers = new TrustManager[]{new TrustManagerManipulator()};
			}
			try
			{
				context = SSLContext.getInstance("TLS");
				context.init(null, trustManagers, new SecureRandom());
			}
			catch (NoSuchAlgorithmException e)
			{
				e.printStackTrace();
			}
			catch (KeyManagementException e)
			{
				e.printStackTrace();
			}

			HttpsURLConnection.setDefaultSSLSocketFactory(context.getSocketFactory());
		}

		public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException
		{
		}

		public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException
		{
		}

		public X509Certificate[] getAcceptedIssuers()
		{
			return acceptedIssuers;
		}
	}
}
