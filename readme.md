# Classcharts for Android

This is a *native* android client for [classcharts](https://www.tes.com/en-gb/for-schools/class-charts) (the horrendous student management system my school switched to this year) written in kotlin with the jetpack compose framework for hack club's [midnight](http://midnight.hackclub.com)!

The minimum SDK for this app is 26, and to you that means that the minimum android version is android 8 (oreo)

## Features

Login with classcharts code and date of birth

Functional fetching of homework and timetable

Highlights the current lesson on timetable

Homeworks can be opened to be viewed, with formatting

Homeworks can be ticked off

Filtering of only completed or all homeworks

[Material 3](https://m3.material.io/) design


## Install

To download the apk, go to [releases](https://github.com/GingerGigiCat/ClassChartsAndroid/releases), expand the assets section on the latest release, and download `classchartsandroid.apk` (or whichever file ends in .apk) to your android phone.

Once the apk file has downloaded, tap it to open it, click through any complaints from your phone about being allowed to install from unknown sources (this isn't sketchy! It's [normal for sideloading](https://www.wikihow.com/Allow-Apps-from-Unknown-Sources-on-Android), I promise), and it should install!

You can then open it from your normal home launcher!

## For reviewers for a demo

On the login page, login using the classcharts code `demo` and the date of birth doesn't matter

Do note that ticking homeworks may cause a crash in demo (but not before you get to see the fun animation!) and changing the filtering of ticked homeworks does not work in the demo, due to not having an actual demo user account. I would create a real user, but i can't, only a school can create users. Also, opening pdfs won't work because you have to be logged in for the link from classcharts to work otherwise it expires (The links that the demo attachments have are expired)

## Tech notes

[I've since improved on this!!] This app (currently) has so many ui thread blocking things, which is bad, and has network on main thread which i have to bypass to let the app even run. There is currently not much error handling (but I'm in the process of adding more), so it is likely it can crash or give no output if something weird happens internally.

This is my first time ever using kotlin and jetpack compose, so make of that what you will

I've ported most of the code to kotlin multiplatform but have not yet persuaded it to build for desktop because the theme code is android specific. The hardest parts about porting it were fixing dependencies (so many dependencies) and switching networking systems.

Here is my todo list for this app, in no particular order:

Make ticking a homework work on the actual homework page

~~Add swiping between timetable days~~ done!

Ability to add a note on homework (needs a database - in the process of adding a database)

Offline mode (needs a database - in the process of adding a database) - kind of done but sometimes it still likes to crash for mysterious reasons

Shade incomplete homeworks a different colour in the list

Fix ui thread blocking - improved on!

Figure out why the calendar is jittery to expand and collapse

Add loading wheels and nice error handling

Export timetable and import someone else's?

Animations, like for ticking off homework so it doesn't just disappear - done!

Make opening microsoft documents not crash it - seems it only happens for word docs, will need more investigation

Login with microsoft once you've linked your classcharts code?

Make timetable show free periods? Harder than it seems

User addable tasks


## Gallery

<img width="1080" height="2400" alt="Screenshot_20251213-224827" src="https://github.com/user-attachments/assets/4f6ecf1b-fe37-4dbe-b44a-4935399f20e5" />
<img width="1080" height="2400" alt="Screenshot_20251213-224907" src="https://github.com/user-attachments/assets/b4ba9674-19fc-44f8-80f0-87542fff1a9a" />
<img width="1080" height="2400" alt="Screenshot_20251213-224832" src="https://github.com/user-attachments/assets/65f0826b-2be6-4244-962b-daac33688daf" />


