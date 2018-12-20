# PolyFold
PolyFold is a crowdsourcing computer game for three dimensional (3D) protein folding. 
The goal is to involve citizen scientists to play an interactive, intuitive, and visually 
appealing computer game where they can make moves to change 3D conformations of proteins and
get scored based on experimentally determined protein contact maps. Gamers strive to achieve 
the best score by folding protein structures such that they agree with the contact map data. 
Folding puzzles are available at different levels of complexities for beginner, intermediate, as 
well as advanced users. No prior scientific background is needed to play PolyFold. It is 
based entirely on human intuition and passion for solving puzzles with important implications 
in science and medicine. Play PolyFold!

## Demo
A small demo video for the alpha version of Polyfold is available for download above. Simply click on the file named "Polyfold Demo.mp4," then click on "Raw" to download the video.

## How to Download, Compile, and Run the Source Code
### From Terminal:
1. Run the following line is your desired directory:
```
$ git clone https://github.com/andrewjmcgehee/PolyFold.git
```
2. Run the following line to compile the code:
```
$ javac PolyFold.java
```
3. Run the following line to run the application:
```
$ java PolyFold
```


### From Github:
1. Navigate to the [PolyFold](https://github.com/andrewjmcgehee/polyfold) github page in your browser.
2. Click on the green **Clone or Download** button.
3. Select either **Open in Desktop** to open the code in the Github Desktop Application or **Download ZIP** to download a simple .zip file.
4. If you selected to download a .zip file, unzip it in your directory of choice.
5. Run the following line to compile the code:
```
$ javac PolyFold.java
```
6. Run the following line to run the application:
```
$ java PolyFold
```

## Contributing to the Code Base
When contributing to the code base, our best practices are as follows:

* Create new feature branches from the develop branch.
* Create initial pull requests from your feature branch to the develop branch.
* Test your code prior to submitting pull requests.
* After testing, delete .class files prior to commiting the changes to a feature branch.
* When reviewing pull requests, carefully review the git diff before approving the changes.
* Make small, frequent commits that accomplish a single, identifiable change.
* Use descriptive, brief, present tense commit messages.
* When changes are merged from a feature branch to the develop branch, they should be 'squashed' and merged to keep a relatively clean develop branch commit log and a very tidy master commit log.
* Log your changes to the changelog in the following format:
    ```
    x.x.x
    -----
    - [Luke Skywalker] - Add feature to log last location of lightsaber.
    - [Han Solo] - Add feature to hide Luke's lightsaber.
    ```
    **NOTE:**
    * Present Tense
    * Punctuated
    * Version Placeholder Included *(this will be updated upon each push from develop to master)*
    * Roughly One Line per Change *(keep it brief!)*

## Conventions Used
* A line preceded by '$' indicates a line run at the command line prompt
