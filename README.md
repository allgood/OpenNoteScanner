OpenNoteScanner
===============

This little application provides a way on scanning handwritten notes and printed documents.

It automatically detect the edge of the paper over a contrastant surface.

If you have RocketBook Wave notebook or home printed pages it automatically detects the qrcode printed on the bottom right corner and scans the page immediately.

After the page is detected, it compensates any perspective from the image adjusting it to a 90 degree top view and saves it on a folder on the device.

It is also possible to launch the application from any other application that asks for a picture, just make sure that there is no default application associated with this action.

**TODO:** place some screenshots here

Requirements
------------

Because of the version of OpenCV that is used in the project it needs to run on Android 5.0 (lollipop) or newer.

In order to capture and manipulate images Open Note Scanner depends on having the OpenCV Manager application installed. If not installed Open Note Scanner will ask to download it from https://github.com/ctodobom/OpenCV-3.1.0-Android or from Google Play Store.


How to Install
--------------

Open Note Scanner is available for simplified installation on [Google Play Store](https://play.google.com/store/apps/details?id=com.todobom.opennotescanner) and also from [F-Droid Android Open Source Repository](https://f-droid.org/repository/browse/?fdfilter=open+note+scanner&fdid=com.todobom.opennotescanner).

Binary APK file is available in the [releases section](https://github.com/ctodobom/OpenNoteScanner/releases) of the project. 


Instructions for building
-------------------------

Open Note Scanner needs to include the java library for OpenCV. This library is responsible in interfacing the services from OpenCV Manager (see Installation) with the Android Java code.

### Android Studio

Import the project from GitHub using File -> New -> Project from Version Control -> GitHub, fill the URL https://github.com/ctodobom/OpenNoteScanner.git

It will ask for a base directory, normally AndroidStudioProjects, you can change it to your preference.

After this the Open Note Scanner can be built.


### Command Line

Go to your base folder and import it using ```git```:

```
$ git clone https://github.com/ctodobom/OpenNoteScanner.git
```

This should import the Open Note Scanner repository in OpenNoteScanner folder

You need to point the environment variable ```ANDROID_HOME``` to your Android SDK folder and run ```gradle``` to build the project:

```
$ cd OpenNoteScanner
$ export ANDROID_HOME=~/android-sdk-linux
$ ./gradlew assembleDebug
```


History
-------

I've started this app on a brazilian holyday "extended weekend" based on the fact that I was unable to find any open source application that does this job. I was mainly inspired on the RocketBook Wave closed source application.

I really do not know if I will extend more the application, but I am writing bellow some objectives to make it better.

Roadmap
-------

* enhance the image gallery of scanned documents
* register a share action in order to obtain documents already pictured through standard camera apps
* implement automatic action based on the RocketBook Wave marking of the page

Donations
---------

My job is on enterprise servers administration and some development consulting. I do collect money from my customers. I am well paid for that.

For being part of open source projects and documenting my work here I really do not charge anything. I am trying to avoid any type of ads also.

If you think that any information you obtained here is worth of some money and are willing to pay for it, feel free to send any amount through paypal or bitcoin, or dogecoin (wow!).

| Paypal | Bitcoin | Dogecoin |
| ------ | ------- | -------- |
| [![](https://www.paypalobjects.com/en_US/i/btn/btn_donateCC_LG.gif)](https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=X6XHVCPMRQEL4) |  <center> ![1H5tqKZoWdqkR54PGe9w67EzBnLXHBFmt9](http://todobom.com/images/bitcoin-donations.png)<br />1H5tqKZoWdqkR54PGe9w67EzBnLXHBFmt9</center> | <center> ![DFBaP724XR3rfs9wFahBd353yFkgkqatvd](http://todobom.com/images/dogecoin-donations.png)<br />DFBaP724XR3rfs9wFahBd353yFkgkqatvd</center> |


Contributing
------------

If you have any idea, feel free to fork it and submit your changes back to me.

It is possible to backport this project to [OpenCV 2.4.8](https://github.com/vRallev/OpenCV) in a way that it can run on Android 4.4.2 (KitKat), this will demand a lot of rework on user interface code. If you know how to do it, it is a great contribution.


License
-------

Copyright 2016 - Claudemir Todo Bom

Software licensed under the GPL version 2 available in GPLv3.TXT and
online on http://www.gnu.org/licenses/gpl.txt.

Use parts from other developers, sometimes with small changes,
references on autorship and specific licenses are on individual
source files.
