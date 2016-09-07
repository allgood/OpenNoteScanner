Change Log
==========

version 1.0.27
--------------

* [@nigelinux](https://github.com/nigelinux) - Traditional Chinese (zh-rTW) Translation
* Allow to customize the storage folder location

version 1.0.26
--------------

This is a source code only release F-Droid should build and make it available for installation
 
* Added tagging support on image viewer
* Czech translation (contributed by Ondřej Míchal)
* Updated Italian translation (contributed by Claudio Arseni)

version 1.0.25
--------------

* Handle calls from external apps that uses Content Handlers (fix #37)
* Toolbar icon to disable image processing (fix #38)
* Spanish translation (contributed by Francisco Toca)
* German translation (contributed by [@nebulon42](https://github.com/nebulon42))

version 1.0.24
--------------

* Telegram Group link changed to new "Supergroup"
* Work with other apps that requests ".nomedia" suffix on files

version 1.0.23
--------------

* enhanced quality and performance of color algorithm
* better messages when asking for OpenCV Manager
* updated portuguese translation

version 1.0.22
--------------

* fixed first run dialog

version 1.0.21
--------------

* removed permission to READ_PHONE_STATE
* started settings activity
* setting to rotate the camera 180 degrees
* keep image frozen until picture processing is done
* some code cleanup
* piwik monitoring
* opt-in dialog on first run

version 1.0.20
--------------

* better images in color mode
* enhancements on main screen interface
* flash state persist between activities changes
* better handling of devices without flash and/or autofocus
* "share app" button on About window

version 1.0.19
--------------

* translation of About window to italian (Claudio Arseni)
* Out-of-memory bug in image viewer on some devices
* asynchronous loading of images on the full screen viewer

version 1.0.18
--------------

* translation of interface messages to italian (Claudio Arseni)
* quick fix of UI bug

version 1.0.17
--------------

* implement asynchronous loading of the image gallery
* started to use a own fork of DragSelectRecyclerView
* removed the need of autofocus on the device from manifest
* some code clean up

version 1.0.16.1
----------------

* fix bug on Android 6 when installing OpenCV Manager from GitHub
* using Camera API directly
* Main screen is always on portrait orientation
* new canvas provides visual clues of what is happening
* about window available on main screen
* new green shutter button - **Green is the new black**

version 1.0.15
--------------

* new gallery, now accepts drag selection
* theme color - green

version 1.0.14
--------------

* fixed OOM errors in some devices caused by high resolution images

version 1.0.13
--------------

* image processing in separated thread

version 1.0.12
--------------

* fixed resolution selection on some devices
* fixed gallery display of large images
* fixed gallery garbage when moving/resizing on some devices

version 1.0.11
--------------

* some code cleanup on main activity class
* use higher resolution for capturing
* enhance contour detection
* second click on shutter button to capture whole image

version 1.0.10 - a.k.a. f-droid debut
-------------------------------------

* removed unnecessary maven references
* changed license to GPLv3
* enhancements on downloading OpenCV binaries


version 1.0.9
-------------

* Download and install OpenCV Manager if needed on systems without the Play Store 

version 1.0.8
-------------

* changed the markdown component used in About window

version 1.0.7
-------------

* Compatibility with android 6.0


version 1.0.6 (failed)
----------------------

* Compatibility with Android 6.0 (Marshmallow)
* About dialog
* Translation for Portuguese

version 1.0.5
-------------

* Deletion of images on gallery and image viewer
* More information on README.md

version 1.0.4
-------------

* Animation of the scanned document
* Toolbars on gallery and image viewer
* Share images to other applications from gallery and image viewer

version 1.0.3
-------------

* Flash support
* Color / Black&White scans
* Better checking of repeating pages with a 15 second timer
* Gallery with pinch-and-zoom

version 1.0.2
-------------

* Avoid continuous automatic re-scan of the same page

version 1.0.1
-------------

* Basic gallery

version 1.0.0
-------------

* Initial version
