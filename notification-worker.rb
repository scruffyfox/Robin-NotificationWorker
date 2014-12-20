if (ARGV.length > 0 && ARGV[0] == "-v")
	system('rainbow --magenta=\'.*\[FOLLOWER\].*\' --cyan=\'.*\[PATTER MENTION\].*\' --yellow=\'.*\[MESSAGES\].*\' --green=\'.*\[MENTIONS\].*\' --red=\'.*/\[MENTIONS\].*\' -- tail -n 500 -f logs/notification.log')
else
	command = "nohup java -jar /var/www/notifications.robinapp.net/versions/v1.2/notification.jar -s mentions -s pms -s follows -s patter-pms -e production > /var/www/notifications.robinapp.net/versions/v1.2/logs/notification.log 2>&1 &"
	check = "ps aux | grep notification.jar | grep -v grep | awk '{print $2}'"

	if (ARGV.length > 0 && ARGV[0] == "--restart")
		pid =`#{check}`
		if (pid.length > 0)
			puts "Killing server process #{pid}"
			system("sudo kill #{pid}")
		end
	else
		pid =`#{check}`
		if (pid.length > 0)
			puts "Status: server is running process #{pid}"
			exit(1)
		end
	end

	system("rm -f /var/www/notifications.robinapp.net/versions/v1.2/logs/notification.log; touch /var/www/notifications.robinapp.net/versions/v1.2/logs/notification.log")
	res = `#{command}`
	puts "Status: server is not running. Starting server."
	puts res
end
