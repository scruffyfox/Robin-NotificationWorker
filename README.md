#Robin - Notification worker

This is the notification worker used to send push notifications to the devices running Robin app for App.net

##Disclaimer

I do not claim for any of this work to be good, or useful, simply open sourcing it in case other people wish to make use of it.

##How it works

It is a jar that works on unix based servers (not tested with windows) and is executed via command line using `notification-worker.rb` with the options `--restart`, and can accept `-v` to view the logs in the current TTY window.

The jar can take 4 options

1. `-s mentions|pms|follows|patter-pms`
1. `-e production`

Where `-s` means "server" and `-e` means "environment.

The worker relies on a notification API and server which can be found in the git project [here](https://github.com/scruffyfox/Robin-NotificationAPI)

##License
Copyright (C) 2012-2014 Callum Taylor

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.

*************

As with the GPL, you may use the code for commercial use, but dont just fork the entire project and re-release it under a different name, that would make you a dick.

If we meet some day, and you find this stuff is worth it, you can buy me a beer in return.

##Warrenty

The code is provided as-is without warrenty. Use and execute at your own risk, please do not contact me about problems.