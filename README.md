# Private Extractor 

Find your COVID-19 risk.

## Inspiration

I wanted the ability to continously monitor my exposure as new cases came out. Doing this manually or with existing systems was not that easy.

## What it does

Private Extractor allows you to build a simple exposure report from your day to day movements using MIT PrivateKit's information.

-   Import your data from a plugged in Android device (in debug mode)
-   Import your data from a text file.

## How I built it

Using a mixture of Android Studio (for the Android Extractor) and Eclipse (for the Desktop GUI)

## Challenges I ran into

UI Automator kept having issues exporting the MIT Private Kit files. MIT Private Kit changed the format halfway through the hackathon. JLR would not use pdflatex correctly from Miktex Portable so I had to hack it together.

## Accomplishments that I'm proud of

I was very excited to have a working version people can download and use.
I feel this will be useful for medical professionals processing exports from people.

## What I learned

UI Automator on Android can be a very interesting source of fun.

## What's next for Private Extractor

I am currently trying to build an enhancement to make this command line driven.
Once this is done, I will be hosting it somewhere where a web page can accept your text file directly.

## Built With

Android, Android Debug Bridge, Android Backup Extractor, JLR, Gson, GeoTools, tar, MIT PrivateKit, and Miktex Portable Edition

## Requirements

File Extractions: Modern version of Windows (with Java and tar in PATH)
Android Extractions: Add a version of adb in PATH

## Pictures

![Basic UI](./Auxiliary/GUI.png)
![Report Initial Page](./Auxiliary/Initial Report Page.png)
![Exposure Report Page](./Auxiliary/Exposure Report Page.png)
![Export Log Page](./Auxiliary/Export Log Page.png)
![Export Log Page 2](./Auxiliary/Export Log Page 2.png)
![Export Log Page 2](./Auxiliary/Exposure Report Page.png)

## Videos
[![Demo Reel](https://img.youtube.com/vi/glnkhx6CnHU/0.jpg)](https://www.youtube.com/watch?v=glnkhx6CnHU)