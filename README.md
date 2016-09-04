OpenNoteScanner
===============

[![Build Status](https://travis-ci.org/ctodobom/OpenNoteScanner.svg)](https://travis-ci.org/ctodobom/OpenNoteScanner)

This little application provides a way on scanning handwritten notes and printed documents.

It automatically detect the edge of the paper over a contrastant surface.

When using the [printed special page template](https://github.com/ctodobom/OpenNoteScanner/raw/master/Page%20Templates/A4%20with%202%20pages.pdf) it automatically detects the QR Code printed on the bottom right corner and scans the page immediately.

After the page is detected, it compensates any perspective from the image adjusting it to a 90 degree top view and saves it on a folder on the device.

It is also possible to launch the application from any other application that asks for a picture, just make sure that there is no default application associated with this action.

Screenshots
-----------

[![screenshot1](http://i.imgur.com/1MDisD3m.jpg)](http://imgur.com/a/ypytF/embed#0)
[![screenshot1](http://i.imgur.com/ksvmOlym.png)](http://imgur.com/a/ypytF/embed#3)
[![screenshot1](http://i.imgur.com/Ayy8GGgm.jpg)](http://imgur.com/a/ypytF/embed#1)
[![screenshot1](http://i.imgur.com/tzMLas3m.jpg)](http://imgur.com/a/ypytF/embed#2)

Requirements
------------

Because of the version of OpenCV that is used in the project it needs to run on Android 5.0 (lollipop) or newer.

In order to capture and manipulate images Open Note Scanner depends on having the OpenCV Manager application installed. If not installed Open Note Scanner will ask to download it from https://github.com/ctodobom/OpenCV-3.1.0-Android or from Google Play Store.


How to Install
--------------

Open Note Scanner is available for simplified installation on [Google Play Store](https://play.google.com/store/apps/details?id=com.todobom.opennotescanner), from [Amazon App Store](http://www.amazon.com/Claudemir-Todo-Bom-Open-Scanner/dp/B01EUAU924) and also from [F-Droid Android Open Source Repository](https://f-droid.org/repository/browse/?fdid=com.todobom.opennotescanner).

[<img alt="Get it on Google Play" height="60" src="https://play.google.com/intl/en_us/badges/images/generic/en-play-badge.png" />](https://play.google.com/store/apps/details?id=com.todobom.opennotescanner&utm_source=global_co&utm_medium=prtnr&utm_content=Mar2515&utm_campaign=PartBadge&pcampaignid=MKT-Other-global-all-co-prtnr-py-PartBadge-Mar2515-1) [![Get it on F-Droid](https://f-droid.org/wiki/images/0/06/F-Droid-button_get-it-on.png)](https://f-droid.org/repository/browse/?fdid=com.todobom.opennotescanner) [<img alt="Available at Amazon App Store" height="60" src="https://images-na.ssl-images-amazon.com/images/G/01/mobile-apps/devportal2/res/images/amazon-underground-app-us-black.png" />](http://www.amazon.com/Claudemir-Todo-Bom-Open-Scanner/dp/B01EUAU924)

Binary APK file is available [directly from GitHub in the releases section](https://github.com/ctodobom/OpenNoteScanner/releases) of the project. 

### Source code releases

Starting on 1.0.26, small enhancements will be made available only as a source code release. F-Droid should automatically build the source code releases and offer it to installation. Binary releases will be made available on Google Play, Amazon App Store and direct `apk` download from the [releases page](https://github.com/ctodobom/OpenNoteScanner/releases).

Instructions for building
-------------------------

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
$ ./gradlew assembleRelease
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

Thanks
------

### Contributors

As an open source application, contribution are always welcome. Everyone that submits any code will be listed here.

* Nicolas Raoul - English corrections
* Claudio Arseni - Italian translation
* Francisco Toca - Spanish translation
* [@nebulon42](https://github.com/nebulon42) - German translation
* Ondřej Míchal - Czech translation
* [@nigelinux](https://github.com/nigelinux) - Traditional Chinese (zh-rTW) Translation

Other people helped submitting [Issue Reports](https://github.com/ctodobom/OpenNoteScanner/issues) and giving info through the [Telegram Group](https://telegram.me/joinchat/CGzsxQgjl8CyAZNrTG0qZg). Their help is appreciated as well.

### External code

This application wouldn't be possible without the great material produced by the community. I would like to give special thanks to the authors of essencial parts I've got on the internet and used in the code:

* [Android-er / GridView code sample](http://android-er.blogspot.com.br/2012/07/gridview-loading-photos-from-sd-card.html)
* [Android Hive / Full Screen Image pager](http://www.androidhive.info/2013/09/android-fullscreen-image-slider-with-swipe-and-pinch-zoom-gestures/)
* [Adrian Rosebrock from pyimagesearch.com for the excellent tutorial on how to handle the images](http://www.pyimagesearch.com/2014/09/01/build-kick-ass-mobile-document-scanner-just-5-minutes/)
* [Gabriele Mariotti / On how to implement sections in the RecyclerView](https://gist.github.com/gabrielemariotti/e81e126227f8a4bb339c)


License
-------

Copyright 2016 - Claudemir Todo Bom

Software licensed under the GPL version 2 available in GPLv3.TXT and
online on http://www.gnu.org/licenses/gpl.txt.

Use parts from other developers, sometimes with small changes,
references on autorship and specific licenses are on individual
source files.
