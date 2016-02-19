OpenNoteScanner
===============

This little application provides a way on scanning handwritten notes and printed documents.

It automatically detect the edge of the paper over a contrastant surface.

If you have RocketBook Wave notebook or home printed pages it automatically detects the qrcode printed on the bottom right corner and scans the page immediately.

After the page is detected, it compensates any perspective from the image adjusting it to a 90 degree top view and saves it on a folder on the device.

It is also possible to launch the application from any other application that asks for a picture, just make sure that there is no default application associated with this action.

**TODO:** place some screenshots here

How to Install
--------------

It is possible to install it directly from Google Play Store through [this link](https://play.google.com/store/apps/details?id=com.todobom.opennotescanner).

Binary APK file is available in the [releases section](https://github.com/ctodobom/OpenNoteScanner/releases) of the project. If you install the binary manually you will need also the OpenCV Manager. The best way on doing this manually is downloading the [OpenCV SDK for Android](http://sourceforge.net/projects/opencvlibrary/files/opencv-android/3.1.0/OpenCV-3.1.0-android-sdk.zip/download) on a computer, unzip it and pick the correct APK for your device in the ```apk``` folder. Normally for phone devices, the armeabi-v7a is the correct one. If you have Play Store access, Open Note Scanner will direct you to it in order to download OpenCV Manager if it is not found in the device.


Instructions for building
-------------------------

Open Note Scanner needs to include the java library for OpenCV. This library is responsible in interfacing the services from OpenCV Manager (see Installation) with the Android Java code.

### Android Studio

Import the project from GitHub using File -> New -> Project from Version Control -> GitHub, fill the URL https://github.com/ctodobom/OpenNoteScanner.git

It will ask for a base directory, normally AndroidStudioProjects, you can change it to your preference.

Repeat the process and import also the OpenCV for Android project, giving this time the URL https://github.com/ctodobom/OpenCV-3.1.0-Android.git

Having both projects imported on Android Studio, on the Open Note Scanner project go to File -> Project Structure, click on the + sign, select Import Gradle Project, on the source directory select the other imported project path.

After this the Open Note Scanner can be built.


### Command Line

TODO: Include instructions for command line building

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

If you think that any information you obtained here is worth of some money and are willing to pay for it, feel free to send any amount through paypal or bitcoin.

| Paypal | Bitcoin |
| ------ | ------- |
| [![](https://www.paypalobjects.com/en_US/i/btn/btn_donateCC_LG.gif)](https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=X6XHVCPMRQEL4) | <center> ![1H5tqKZoWdqkR54PGe9w67EzBnLXHBFmt9](http://todobom.com/images/bitcoin-donations.png)<br />1H5tqKZoWdqkR54PGe9w67EzBnLXHBFmt9</center> |


License
-------

Copyright 2016 - Claudemir Todo Bom

Software licensed under the GPL version 2 available in GPLv2.TXT and
online on http://www.gnu.org/licenses/old-licenses/gpl-2.0.txt.

Use parts from other developers, sometimes with small changes,
references on autorship and specific licenses are on individual
source files.
