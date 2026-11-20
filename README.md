# FixMyCampus

FixMyCampus is a Java OOP lab project based on the submitted campus issue reporting synopsis. The repository contains a console version and a lightweight browser version. The browser version has a responsive HTML/CSS/JavaScript interface connected to a Java HTTP server.

## Project scope

The synopsis describes a full web application using Java, a web frontend, and MySQL. This beginner-friendly project runs locally with a JDK and saves records in local data files. The web version supports issue reporting, voting, discussion, filtering, sorting, and status display. The console version also demonstrates technician assignment, application drafts and signatures, ratings, and status updates. Image paths are not uploaded in the web version. MySQL, secure passwords, verified email, and production deployment remain future extensions.

## OOP concepts

- Encapsulation: issue information and actions are grouped in classes.
- Inheritance: `Student` and `Technician` extend `User`.
- Abstraction: issue and user behavior is represented through domain classes and enums.
- Polymorphism: student and technician accounts have different menus and behavior.
- Collections: lists and maps store issues, votes, users, messages, and ratings.
- File handling: Java serialization saves records in `data/fixmycampus.dat`.

## Requirements

- JDK installed (Java 17 or newer recommended)
- Terminal or command prompt
- Git, if you want to publish the project on GitHub

Check Java installation:

```text
java -version
javac -version
```

## Run the project

Open a terminal in the `FixMyCampus` folder and run:

```text
javac -d out src/Main.java
java -cp out Main
```

On Windows, you can also run `run.bat` by double-clicking it or typing `run.bat` in Command Prompt.

## Run the web frontend

1. Open the project folder that contains `run-web.bat`.
2. Double-click `run-web.bat` and leave the black window open while using the site.
3. Open a browser and visit `http://localhost:8080`.
4. Report an issue, vote, or add a discussion message. Refreshing the page keeps saved issues.
5. Close the server by clicking its Command Prompt window and pressing `Ctrl+C`.

If you compile manually, run `javac -d out src\Main.java src\WebServer.java` then `java -cp out WebServer` from the project folder. The server and browser must run on the same computer for the local address to work.

## First use

1. Choose student login or registration.
2. Enter an email ending in `@jiit.ac.in` and a name.
3. Report an issue by selecting a category, location, severity, and description.
4. Choose view and support issues to see the priority order and vote.
5. Open issue details to add messages to its discussion.
6. Draft an application and sign it with your student account.
7. Student status updates are available in this lab version to demonstrate progress tracking.
8. To try technician assignment, log out and choose technician login. Create a demo technician with a category matching a reported issue. Future matching issues can then be automatically assigned to the available technician with the fewest open jobs.
9. Technician accounts can view assigned issues and update their status.
10. Rate or reopen resolved issues from the student menu.

Records are saved automatically under `data`. Keep this folder out of GitHub if it contains real personal information. The included `.gitignore` excludes it.

## GitHub upload steps

1. Create a new empty repository on GitHub. Do not add a README there because this folder already has one.
2. Install Git and open a terminal in this project folder.
3. Run these commands, replacing the URL with your repository URL:

```text
git init
git add .
git commit -m "Add FixMyCampus Java OOP project"
git branch -M main
git remote add origin https://github.com/YOUR-USERNAME/YOUR-REPOSITORY.git
git push -u origin main
```

## Suggested project explanation

FixMyCampus organizes campus complaints in one place. A student reports an issue with its category, location, description, and severity. Students can vote and discuss the issue. A priority value is calculated from severity and net votes. An available technician with the matching skill and the fewest open jobs is selected. Students can prepare and sign an application, track status, rate a resolution, or reopen an issue. Records persist between runs using Java file handling.

## Possible improvements

- Replace local serialization with MySQL and JDBC.
- Add a web frontend and Java web backend.
- Add secure authentication and verified college email.
- Save actual image uploads and application documents.
- Add representative elections, recurring issue reports, and a proper audit history.
- Add role permissions so only technicians or administrators can update official progress.
