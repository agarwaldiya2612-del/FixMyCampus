import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

enum Category { ELECTRICAL, PLUMBING, CLEANLINESS, INTERNET, FURNITURE, OTHER }
enum Status { REPORTED, UNDER_REVIEW, ASSIGNED, IN_PROGRESS, RESOLVED }

class User implements Serializable {
    private static final long serialVersionUID = 1L;
    String name;
    String email;
    User(String name, String email) { this.name = name; this.email = email; }
}

class Student extends User {
    private static final long serialVersionUID = 1L;
    Student(String name, String email) { super(name, email); }
}

class Technician extends User {
    private static final long serialVersionUID = 1L;
    Category skill;
    int openJobs;
    boolean available = true;
    Technician(String name, String email, Category skill) { super(name, email); this.skill = skill; }
}

class Issue implements Serializable {
    private static final long serialVersionUID = 1L;
    int id;
    String title;
    String description;
    String location;
    String imagePath;
    Category category;
    int severity;
    Status status = Status.REPORTED;
    String technicianEmail = "";
    Map<String, Integer> votes = new LinkedHashMap<>();
    List<String> messages = new ArrayList<>();
    List<String> signatures = new ArrayList<>();
    Map<String, Integer> ratings = new LinkedHashMap<>();
    String application = "";
    Issue(int id, String title, String description, String location, String imagePath, Category category, int severity) {
        this.id = id; this.title = title; this.description = description; this.location = location;
        this.imagePath = imagePath; this.category = category; this.severity = severity;
    }
    int support() { return votes.values().stream().mapToInt(Integer::intValue).sum(); }
    double averageRating() { return ratings.isEmpty() ? 0 : ratings.values().stream().mapToInt(Integer::intValue).average().orElse(0); }
    int priority() { return severity * 10 + support(); }
}

class CampusData implements Serializable {
    private static final long serialVersionUID = 1L;
    Map<String, Student> students = new LinkedHashMap<>();
    Map<String, Technician> technicians = new LinkedHashMap<>();
    List<Issue> issues = new ArrayList<>();
    int nextId = 1;
}

public class Main {
    private static final String DATA_FILE = "data/fixmycampus.dat";
    private static final Scanner input = new Scanner(System.in);
    private static CampusData data = loadData();
    private static User currentUser;
    private static final DateTimeFormatter timeFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public static void main(String[] args) {
        while (true) {
            System.out.println("\n=== FixMyCampus ===");
            System.out.println("1. Student login or register\n2. Technician login\n3. Exit");
            int choice = readInt("Choose: ", 1, 3);
            if (choice == 1) studentLogin();
            else if (choice == 2) technicianLogin();
            else { saveData(); System.out.println("Goodbye."); return; }
        }
    }

    private static void studentLogin() {
        String email = readText("College email: ").toLowerCase();
        if (!email.endsWith("@jiit.ac.in")) { System.out.println("Use a college email ending in @jiit.ac.in."); return; }
        Student student = data.students.get(email);
        if (student == null) {
            student = new Student(readText("Your name: "), email);
            data.students.put(email, student);
            saveData();
            System.out.println("Account created.");
        }
        currentUser = student;
        studentMenu();
    }

    private static void technicianLogin() {
        String email = readText("Technician email: ").toLowerCase();
        Technician tech = data.technicians.get(email);
        if (tech == null) {
            System.out.println("Technician not found. Create a demo technician account? (y/n)");
            if (!readText("").equalsIgnoreCase("y")) return;
            String name = readText("Name: ");
            Category skill = chooseCategory();
            tech = new Technician(name, email, skill);
            data.technicians.put(email, tech);
            saveData();
        }
        currentUser = tech;
        technicianMenu(tech);
    }

    private static void studentMenu() {
        while (currentUser instanceof Student) {
            System.out.println("\nStudent: " + currentUser.name);
            System.out.println("1. Report issue\n2. View and support issues\n3. Issue details and discussion\n4. Draft application and sign\n5. Update issue status\n6. Rate or reopen resolved issue\n7. View maintenance history\n8. Log out");
            int choice = readInt("Choose: ", 1, 8);
            if (choice == 1) reportIssue();
            else if (choice == 2) viewIssues();
            else if (choice == 3) discussIssue();
            else if (choice == 4) applicationMenu();
            else if (choice == 5) updateStatus();
            else if (choice == 6) rateOrReopen();
            else if (choice == 7) history();
            else currentUser = null;
            saveData();
        }
    }

    private static void technicianMenu(Technician tech) {
        while (currentUser == tech) {
            System.out.println("\nTechnician: " + tech.name + " | Skill: " + tech.skill + " | Open jobs: " + tech.openJobs);
            System.out.println("1. View assigned issues\n2. Update assigned issue\n3. Change availability\n4. Log out");
            int choice = readInt("Choose: ", 1, 4);
            if (choice == 1) {
                for (Issue assignedIssue : data.issues) if (assignedIssue.technicianEmail.equals(tech.email)) printIssue(assignedIssue);
            } else if (choice == 2) {
                Issue issue = findIssue(readInt("Issue ID: ", 1, Integer.MAX_VALUE));
                if (issue != null && issue.technicianEmail.equals(tech.email)) changeStatus(issue);
                else System.out.println("That issue is not assigned to you.");
            } else if (choice == 3) { tech.available = !tech.available; System.out.println("Availability: " + (tech.available ? "available" : "unavailable")); }
            else currentUser = null;
            saveData();
        }
    }

    private static void reportIssue() {
        String title = readText("Issue title: ");
        String description = readText("Description: ");
        String location = readText("Campus location: ");
        String image = readText("Image file path (leave blank if none): ");
        Category category = chooseCategory();
        int severity = readInt("Severity (1 low to 5 critical): ", 1, 5);
        Issue issue = new Issue(data.nextId++, title, description, location, image, category, severity);
        data.issues.add(issue);
        System.out.println("Issue #" + issue.id + " reported.");
        assignTechnician(issue);
    }

    private static void assignTechnician(Issue issue) {
        Technician best = data.technicians.values().stream().filter(t -> t.available && t.skill == issue.category)
                .min(Comparator.comparingInt(t -> t.openJobs)).orElse(null);
        if (best == null) { System.out.println("No available matching technician. The issue remains unassigned."); return; }
        issue.technicianEmail = best.email;
        best.openJobs++;
        issue.status = Status.ASSIGNED;
        System.out.println("Assigned to " + best.name + ".");
    }

    private static void viewIssues() {
        if (data.issues.isEmpty()) { System.out.println("No issues have been reported."); return; }
        data.issues.stream().sorted(Comparator.comparingInt(Issue::priority).reversed()).forEach(Main::printIssue);
        Issue issue = findIssue(readInt("Issue ID to vote on, or 0 to return: ", 0, Integer.MAX_VALUE));
        if (issue == null) return;
        int old = issue.votes.getOrDefault(currentUser.email, 0);
        int vote = readInt("Vote: 1 upvote, -1 downvote, 0 remove vote: ", -1, 1);
        if (vote == 0) issue.votes.remove(currentUser.email); else issue.votes.put(currentUser.email, vote);
        System.out.println("Vote recorded. Net support: " + issue.support() + (old == vote ? " (unchanged)" : ""));
    }

    private static void discussIssue() {
        Issue issue = findIssue(readInt("Issue ID: ", 1, Integer.MAX_VALUE));
        if (issue == null) return;
        printIssue(issue);
        issue.messages.forEach(System.out::println);
        String message = readText("Message (blank to return): ");
        if (!message.isBlank()) issue.messages.add(stamp() + " | " + currentUser.name + ": " + message);
    }

    private static void applicationMenu() {
        Issue issue = findIssue(readInt("Issue ID: ", 1, Integer.MAX_VALUE));
        if (issue == null) return;
        if (issue.application.isBlank() || readText("Replace existing draft? (y/n): ").equalsIgnoreCase("y"))
            issue.application = readText("Application draft: ");
        System.out.println("Draft: " + issue.application);
        if (!issue.signatures.contains(currentUser.email) && readText("Sign this application? (y/n): ").equalsIgnoreCase("y"))
            issue.signatures.add(currentUser.email);
        System.out.println("Signatures: " + issue.signatures.size());
    }

    private static void updateStatus() {
        Issue issue = findIssue(readInt("Issue ID: ", 1, Integer.MAX_VALUE));
        if (issue != null) changeStatus(issue);
    }

    private static void changeStatus(Issue issue) {
        System.out.println("1 REPORTED  2 UNDER_REVIEW  3 ASSIGNED  4 IN_PROGRESS  5 RESOLVED");
        Status next = Status.values()[readInt("New status: ", 1, 5) - 1];
        if (issue.status != Status.RESOLVED && next == Status.RESOLVED) {
            Technician tech = data.technicians.get(issue.technicianEmail);
            if (tech != null && tech.openJobs > 0) tech.openJobs--;
        }
        issue.status = next;
        System.out.println("Status updated to " + next + ".");
    }

    private static void rateOrReopen() {
        Issue issue = findIssue(readInt("Issue ID: ", 1, Integer.MAX_VALUE));
        if (issue == null || issue.status != Status.RESOLVED) { System.out.println("Choose a resolved issue."); return; }
        int rating = readInt("Rating from 1 to 5 (0 to skip): ", 0, 5);
        if (rating > 0) issue.ratings.put(currentUser.email, rating);
        if (readText("Reopen issue? (y/n): ").equalsIgnoreCase("y")) issue.status = Status.UNDER_REVIEW;
    }

    private static void history() {
        String search = readText("Location or issue title to search (blank for all): ").toLowerCase();
        for (Issue issue : data.issues) if ((issue.status == Status.RESOLVED || issue.status == Status.REPORTED)
                && (search.isBlank() || issue.location.toLowerCase().contains(search) || issue.title.toLowerCase().contains(search))) printIssue(issue);
    }

    private static void printIssue(Issue i) {
        System.out.println("#" + i.id + " | " + i.title + " | " + i.category + " | " + i.status + " | priority " + i.priority()
                + " | support " + i.support() + " | " + i.location + " | technician " + (i.technicianEmail.isBlank() ? "none" : i.technicianEmail)
                + " | rating " + String.format("%.1f", i.averageRating()));
        System.out.println("  " + i.description + (i.imagePath.isBlank() ? "" : " | image: " + i.imagePath));
    }

    private static Issue findIssue(int id) {
        if (id == 0) return null;
        for (Issue issue : data.issues) if (issue.id == id) return issue;
        System.out.println("Issue not found.");
        return null;
    }

    private static Category chooseCategory() {
        Category[] values = Category.values();
        for (int i = 0; i < values.length; i++) System.out.println((i + 1) + ". " + values[i]);
        return values[readInt("Category: ", 1, values.length) - 1];
    }

    private static int readInt(String prompt, int min, int max) {
        while (true) {
            try { System.out.print(prompt); int value = Integer.parseInt(input.nextLine().trim()); if (value >= min && value <= max) return value; }
            catch (NumberFormatException ignored) { }
            System.out.println("Enter a number from " + min + " to " + max + ".");
        }
    }

    private static String readText(String prompt) { System.out.print(prompt); return input.nextLine().trim(); }
    private static String stamp() { return LocalDateTime.now().format(timeFormat); }

    private static CampusData loadData() {
        File file = new File(DATA_FILE);
        if (!file.exists()) return new CampusData();
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(file))) { return (CampusData) in.readObject(); }
        catch (Exception e) { System.out.println("Could not read saved data. Starting with empty records."); return new CampusData(); }
    }

    private static void saveData() {
        File file = new File(DATA_FILE);
        File parent = file.getParentFile();
        if (!parent.exists()) parent.mkdirs();
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(file))) { out.writeObject(data); }
        catch (IOException e) { System.out.println("Could not save data: " + e.getMessage()); }
    }
}
