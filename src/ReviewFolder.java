import java.io.File;

public class ReviewFolder {
    private final File tempDir;
    private final String before;
    private final String after;

    private ReviewFolder(File tempDir, String before, String after) {
        this.tempDir = tempDir;
        this.before = before;
        this.after = after;
    }

    public static final ReviewFolder create(File tempDir, String before, String after) {
        return new ReviewFolder(tempDir, before, after);
    }

    public File tempDir() {return tempDir;}
    public String before() {return before;}
    public String after() {return after;}
}
