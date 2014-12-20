package in.rob.notification.service;

import in.rob.notification.Logger;
import in.rob.notification.Server;
import in.rob.notification.ServerCallback;
import in.rob.notification.service.base.Service;

import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

public class MentionsService extends Service
{
	public MentionsService(ServerCallback callback)
	{
		super(callback);

		ENABLE_INT = 1;
		LOGPREFIX = "MENTIONS";
		PRODUCTION_STREAM = "https://stream-channel.app.net/channel/1/?purge=1";
		DEVELOPMENT_STREAM = "https://stream-channel.app.net/channel/1/?purge=1";

		connect();
	}

	@Override public void sendNotification(final String notificationObject)
	{
		super.sendNotification(notificationObject);

		if (notificationObject == null)
		{
			return;
		}

		new Thread()
		{
			@Override public void run()
			{
				try
				{
					JsonElement element = new JsonParser().parse(notificationObject);
					if (element == null || element == JsonNull.INSTANCE || element.equals("null"))
					{
						return;
					}

					JsonObject meta = element.getAsJsonObject().get("meta").getAsJsonObject();
					JsonObject men = element.getAsJsonObject().get("data").getAsJsonObject();
					if (men.has("repost_of")
					|| (meta.has("suppress_notifications_all") && meta.get("suppress_notifications_all").getAsBoolean())
					|| (meta.has("is_deleted") && meta.get("is_deleted").getAsBoolean()))
					{
						return;
					}

					JsonArray users = men.get("entities").getAsJsonObject().get("mentions").getAsJsonArray();

					for (JsonElement e : users)
					{
						// mention user
						String userid = e.getAsJsonObject().get("id").getAsString();

						// send user
						String senderId = men.get("user").getAsJsonObject().get("id").getAsString();

						// dont send self notifications
						if (userid.equals(senderId)) continue;

						JsonArray ids = new JsonArray();
						ArrayList<String> deviceIds = new ArrayList<String>();

						try
						{
							Statement stmt = connection.createStatement();
							ResultSet rs = stmt.executeQuery("SELECT * FROM devices INNER JOIN users u ON u.id = devices.user_id WHERE devices.user_id = " + userid + " AND (devices.enabled & " + ENABLE_INT+ ") = " + ENABLE_INT);

							if (rs == null)
							{
								continue;
							}

							Boolean isFollowing = null;
							while (rs.next())
							{
								// following enabled
								if (rs.getBoolean(5))
								{
									if (isFollowing == null)
									{
										isFollowing = isUserFollowing(rs.getString(7), senderId);
									}

									if (isFollowing)
									{
										ids.add(new JsonPrimitive(rs.getString(3)));
										deviceIds.add(rs.getString(1));
									}
								}
								else
								{
									ids.add(new JsonPrimitive(rs.getString(3)));
									deviceIds.add(rs.getString(1));
								}
							}

							stmt.close();
						}
						catch (Exception e2)
						{
							Logger.e(LOGPREFIX, e2);
							Server.traces.add(e2.getMessage());
							continue;
						}

						if (ids.size() < 1) continue;

						JsonArray notif = new JsonArray();
						JsonObject object = new JsonObject();

						object.add("post_id", new JsonPrimitive(men.get("id").getAsString()));
						object.add("thread_id", new JsonPrimitive(men.get("thread_id").getAsString()));
						object.add("account_id", new JsonPrimitive(userid));
						object.add("user_id", new JsonPrimitive(men.get("user").getAsJsonObject().get("id").getAsString()));
						object.add("text", new JsonPrimitive(men.get("text").getAsString()));
						object.add("username", new JsonPrimitive(men.get("user").getAsJsonObject().get("username").getAsString()));
						object.add("name", new JsonPrimitive(men.get("user").getAsJsonObject().get("name").getAsString()));
						object.add("date", new JsonPrimitive(men.get("created_at").getAsString()));
						object.add("you_follow", new JsonPrimitive(true));
						object.add("follows_you", new JsonPrimitive(true));
						object.addProperty("users_size", men.get("entities").getAsJsonObject().get("mentions").getAsJsonArray().size());
						notif.add(object);

						String type = "mention";

						if (men.has("reply_to"))
						{
							type = "reply";
						}

						JsonObject res = new JsonObject();
						res.add("total", new JsonPrimitive(1));
						res.add("mentions", notif);

						JsonObject data = new JsonObject();
						data.add("message", res);
						data.addProperty("id", System.currentTimeMillis());
						data.addProperty("type", type);

						JsonObject sendData = new JsonObject();
						sendData.add("registration_ids", ids);
						sendData.add("data", data);

						Logger.v(LOGPREFIX, "From: " + senderId + " - User: " + userid + " - Devices: " + ids.size());
						postNotification(sendData.toString(), userid, deviceIds);
					}
				}
				catch (Exception e)
				{
					Logger.e(LOGPREFIX, e);
					Logger.e(LOGPREFIX, notificationObject);
					Server.traces.add(e.getMessage());
				}
			}
		}.start();
	}
}