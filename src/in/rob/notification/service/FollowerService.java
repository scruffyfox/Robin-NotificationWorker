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

public class FollowerService extends Service
{
	public FollowerService(ServerCallback callback)
	{
		super(callback);

		ENABLE_INT = 8;
		LOGPREFIX = "FOLLOWER";
		PRODUCTION_STREAM = "https://stream-channel.app.net/channel/1/---?purge=1";
		DEVELOPMENT_STREAM = "https://stream-channel.app.net/channel/1/---?purge=1";

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
					JsonObject follow = element.getAsJsonObject().get("data").getAsJsonObject();
					if ((meta.has("is_deleted")&& meta.get("is_deleted").getAsBoolean())
					|| (meta.has("suppress_notifications_all") && meta.get("suppress_notifications_all").getAsBoolean()))
					{
						return;
					}

					JsonObject user = follow.get("follows_user").getAsJsonObject();
					String senderId = follow.get("user").getAsJsonObject().get("id").getAsString();
					String userId = user.get("id").getAsString();

					JsonArray ids = new JsonArray();
					ArrayList<String> deviceIds = new ArrayList<String>();
					Boolean isFollowing = null;

					try
					{
						Statement stmt = connection.createStatement();
						ResultSet rs = stmt.executeQuery("SELECT * FROM devices INNER JOIN users u ON u.id = devices.user_id WHERE devices.user_id = " + userId + " AND (devices.enabled & " + ENABLE_INT + ") = " + ENABLE_INT);

						if (rs == null)
						{
							return;
						}

						while (rs.next())
						{
							if (isFollowing == null)
							{
								isFollowing = isUserFollowing(rs.getString(7), senderId);
							}

							ids.add(new JsonPrimitive(rs.getString(3)));
							deviceIds.add(rs.getString(1));
						}

						stmt.close();
					}
					catch (Exception e2)
					{
						Logger.e(LOGPREFIX, e2);
						Server.traces.add(e2.getMessage());
						return;
					}

					if (ids.size() > 0)
					{
						JsonArray notif = new JsonArray();
						JsonObject object = new JsonObject();

						object.addProperty("account_id", userId);
						object.addProperty("user_id", senderId);
						object.addProperty("user_name", follow.get("user").getAsJsonObject().get("name").getAsString());
						object.addProperty("mention_name", follow.get("user").getAsJsonObject().get("username").getAsString());
						object.addProperty("you_follow", isFollowing == null ? false : isFollowing);
						notif.add(object);

						String type = "follow";

						JsonObject res = new JsonObject();
						res.add("total", new JsonPrimitive(1));
						res.add("follows", notif);

						JsonObject data = new JsonObject();
						data.add("message", res);
						data.addProperty("id", System.currentTimeMillis());
						data.addProperty("type", type);

						JsonObject sendData = new JsonObject();
						sendData.add("registration_ids", ids);
						sendData.add("data", data);

						Logger.v(LOGPREFIX, "From: " + senderId + " - User: " + userId + " - Devices: " + ids.size());
						postNotification(sendData.toString(), userId, deviceIds);
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