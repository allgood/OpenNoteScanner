OpenNoteScanner
===============
## Table of Contents
- [Overview](#overview)
- [Features](#features)
- [Screenshots](#screenshots)
- [Requirements](#requirements)
- [How to Install](#how-to-install)
- [Usage](#usage)
- [Building from Source](#building-from-source)
- [Contributing](#contributing)
- [History](#history)
- [Roadmap](#roadmap)
- [Donations](#donations)
- [Acknowledgements](#acknowledgements)
- [License](#license)

Overview
-----
[![Build Status](https://travis-ci.org/ctodobom/OpenNoteScanner.svg)](https://travis-ci.org/ctodobom/OpenNoteScanner)

OpenNoteScanner is an open-source application that provides a way to scan handwritten notes and printed documents using your mobile device.

### Features

* Automatic edge detection of documents on contrasting surfaces.
* Perspective correction to a 90-degree top-down view.
* Support for a [printed special page template](https://github.com/ctodobom/OpenNoteScanner/raw/master/Page%20Templates/A4%20with%202%20pages.pdf) with a QR code for instant scanning.
* Ability to launch from other applications requesting images.
* Organize scanned documents in a local gallery.
* Export scans as a PDF or share as an image.

### Screenshots

[![screenshot1](http://i.imgur.com/1MDisD3m.jpg)](http://imgur.com/a/ypytF/embed#0)
[![screenshot1](http://i.imgur.com/ksvmOlym.png)](http://imgur.com/a/ypytF/embed#3)
[![screenshot1](http://i.imgur.com/Ayy8GGgm.jpg)](http://imgur.com/a/ypytF/embed#1)
[![screenshot1](http://i.imgur.com/tzMLas3m.jpg)](http://imgur.com/a/ypytF/embed#2)

Requirements
------------

- Due to the version of OpenCV that is used, this project requires Android 5.0 (lollipop) or newer to run.

How to Install
--------------

Open Note Scanner is available for simplified installation from [Google Play Store](https://play.google.com/store/apps/details?id=com.todobom.opennotescanner), from [Amazon App Store](http://www.amazon.com/Claudemir-Todo-Bom-Open-Scanner/dp/B01EUAU924) and also from [F-Droid Android Open Source Repository](https://f-droid.org/repository/browse/?fdid=com.todobom.opennotescanner).

[<img alt="Get it on Google Play" height="60" src="https://play.google.com/intl/en_us/badges/images/generic/en-play-badge.png" />](https://play.google.com/store/apps/details?id=com.todobom.opennotescanner&utm_source=global_co&utm_medium=prtnr&utm_content=Mar2515&utm_campaign=PartBadge&pcampaignid=MKT-Other-global-all-co-prtnr-py-PartBadge-Mar2515-1)
[<img src="https://f-droid.org/badge/get-it-on.png"
    alt="Get it on F-Droid"
    height="60"/>](https://f-droid.org/repository/browse/?fdid=com.todobom.opennotescanner)

**Disclaimer:** Google Play version is ad-supported.

Binary APK file is available directly from the [releases section](https://github.com/ctodobom/OpenNoteScanner/releases) on GitHub.

Usage
-----
For detailed usage instructions, please refer to the [FAQ](FAQ.md).

Building from Source
-------------------------

### Android Studio

1. Import the project from GitHub using File -> New -> Project from Version Control -> GitHub, enter the URL https://github.com/ctodobom/OpenNoteScanner.git
2. Choose the base directory (usually AndroidStudioProjects, but you can change it to your preference).
3. Build the project.

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

Contributing
-------------------------
Contributions are always welcome! If you're new to open-source, please check our [Contributing Guidelines](https://github.com/ctodobom/OpenNoteScanner/blob/master/CONTRIBUTING.md) and [Setup Guidelines](https://github.com/ctodobom/OpenNoteScanner/blob/master/SETUP_GUIDELINES.md). Feel free to fork the project and submit pull requests.

History
-------

I started this app while on a brazilian holiday (that is, an "extended weekend") based on the fact that I was unable to find any open source application that does this job. I was mainly inspired by the RocketBook Wave closed source application.

I really do not know if I will extend the application more, but I have written some objectives below on how to make it better.

Roadmap
-------

* Enhance the image gallery of scanned documents.
* Register a share action in order to obtain documents already pictured through standard camera apps.
* Implement an automatic action based on the RocketBook Wave marking of the page.

Donations
---------

My job is on enterprise servers administration and some development consulting. I do collect money from my customers. I am well paid for that.

For being part of open source projects and documenting my work here I really do not charge anything. I am trying to avoid any type of ads also.

If you think that any information you obtained here is worth of some money and are willing to pay for it, feel free to send any amount through PayPal or Bitcoin.

| Paypal | Bitcoin |
| ------ | ------- |
| [![](https://www.paypalobjects.com/en_US/i/btn/btn_donateCC_LG.gif)](https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=X6XHVCPMRQEL4) |  <center> [![](http://api.qrserver.com/v1/create-qr-code/?color=000000&bgcolor=FFFFFF&data=bitcoin%3A1H5tqKZoWdqkR54PGe9w67EzBnLXHBFmt9&qzone=1&margin=0&size=200x200&ecc=L)](bitcoin:1H5tqKZoWdqkR54PGe9w67EzBnLXHBFmt9)<br />[1H5tqKZoWdqkR54PGe9w67EzBnLXHBFmt9](bitcoin:1H5tqKZoWdqkR54PGe9w67EzBnLXHBFmt9)</center> |

In order to fund the development of Open Note Scanner, since version 1.0.36 when downloaded from Google Play Store the app will show ads on some screens and will also allow for donation through Google Play in-app purchase. There is no difference on the features available from Google Play version and F-Droid releases.

Acknowledgements
------

### Contributors

As an open source application, contributions are always welcome.

Most translation contributions are listed in the [CONTRIBUTORS.md](https://github.com/ctodobom/OpenNoteScanner/blob/master/CONTRIBUTORS.md) file and code contributors are listed on [the Changelog file](https://github.com/ctodobom/OpenNoteScanner/blob/master/CHANGELOG.md) and the [commits history](https://github.com/ctodobom/OpenNoteScanner/commits)

We also thank those who have submitted [Issue Reports](https://github.com/ctodobom/OpenNoteScanner/issues) and provided feedback through the [Telegram Group](https://t.me/OpenNoteScanner).

### External code

This application wouldn't be possible without the great material produced by the community. Special thanks to the authors of these essential components:

* [Android-er / GridView code sample](http://android-er.blogspot.com.br/2012/07/gridview-loading-photos-from-sd-card.html)
* [Android Hive / Full Screen Image pager](http://www.androidhive.info/2013/09/android-fullscreen-image-slider-with-swipe-and-pinch-zoom-gestures/)
* [Adrian Rosebrock from pyimagesearch.com for the excellent tutorial on how to handle the images](http://www.pyimagesearch.com/2014/09/01/build-kick-ass-mobile-document-scanner-just-5-minutes/)
* [Gabriele Mariotti / On how to implement sections in the RecyclerView](https://gist.github.com/gabrielemariotti/e81e126227f8a4bb339c)


License
-------

Copyright 2016 - Claudemir Todo Bom

This software is licensed under the GPL version 3, which can be found in the file GPLv3.TXT in this repository and
online at http://www.gnu.org/licenses/gpl.txt.

Some parts of this software use code from other developers. References to the original authors and specific licenses can be found in the individual source files.
