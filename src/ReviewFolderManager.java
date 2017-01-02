import com.smartbear.CollabClientException;
import com.smartbear.beans.ConfigUtils;
import com.smartbear.ccollab.client.CollabClientInvalidInputException;
import com.smartbear.ccollab.client.GuiLauncher;
import com.smartbear.collab.filetypes.CollabDiffConfigManifest;
import com.smartbear.util.PathUtils;
import com.smartbear.util.ShutdownHooks;
import com.smartbear.util.SmartBearUtils;
import com.smartbear.util.commons.ArrayUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.SystemUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ReviewFolderManager {
    private static final Log log = LogFactory.getLog(ReviewFolderManager.class);

    public ReviewFolder createReviewFolder(String fileString) throws CollabClientException, IOException {
        File e = new File(fileString);
        if(!e.exists()) {
            throw new CollabClientInvalidInputException("External Diff Config file \'" + e + "\' does not exist");
        }

        if(!e.isFile()) {
            throw new CollabClientInvalidInputException("External Diff Config file \'" + e + "\' must be a file");
        }

        if(!e.canRead()) {
            throw new CollabClientInvalidInputException("External Diff Config file \'" + e + "\' is not readable");
        }

        return launchExternalDiffViewer(e);
    }

    private static void assertDir(File dir) throws CollabClientException {
        if(!dir.exists()) {
            boolean success = dir.mkdirs();
            if(!success || !dir.exists()) {
                throw new CollabClientException("Could not create directory \'" + dir + "\'");
            }
        }

        if(!dir.canWrite()) {
            throw new CollabClientException("Directory \'" + dir + "\' is not writable");
        }
    }

    protected static ReviewFolder launchExternalDiffViewer(File externalDiffConfig) throws CollabClientException, IOException {
        log.debug("Launching external diff");
        log.debug("Unzipping external diff file");
        ZipFile zipFile = null;

        File tempDir;
        CollabDiffConfigManifest manifest;
        File afterFile;
        try {
            zipFile = new ZipFile(externalDiffConfig);
            tempDir = PathUtils.createTempDir("collabDiff", "", SystemUtils.getJavaIoTmpDir());
            if(tempDir == null) {
                throw new CollabClientException("Could not create temp directory");
            }

            assertDir(tempDir);
            ZipEntry externalDiffCommand = zipFile.getEntry("collabdiff.mf");
            if(externalDiffCommand == null) {
                throw new CollabClientException("External diff file missing manifest");
            }

            manifest = CollabDiffConfigManifest.fromStream(zipFile.getInputStream(externalDiffCommand));
            Iterator externalDiffCommandExe = Collections.list(zipFile.entries()).iterator();

            while(externalDiffCommandExe.hasNext()) {
                ZipEntry externalDiffCommandArgs = (ZipEntry)externalDiffCommandExe.next();
                String beforeFile = externalDiffCommandArgs.getName();
                beforeFile = toSystemPath(manifest, beforeFile);
                afterFile = new File(tempDir, beforeFile);
                log.debug("Unzipping " + beforeFile);
                if(externalDiffCommandArgs.isDirectory()) {
                    assertDir(afterFile);
                } else {
                    assertDir(afterFile.getParentFile());
                    FileOutputStream e = null;

                    try {
                        e = new FileOutputStream(afterFile);
                        IOUtils.copy(zipFile.getInputStream(externalDiffCommandArgs), e);
                    } finally {
                        if(e != null) {
                            e.close();
                        }

                    }

                    if(!afterFile.setReadOnly() && log.isInfoEnabled()) {
                        log.info("Couldn\'t set \'" + afterFile + "\' as read-only, continuing");
                    }
                }
            }
        } catch (IOException var33) {
            throw new CollabClientException("Could not read external diff file", var33);
        } finally {
            if(zipFile != null) {
                try {
                    zipFile.close();
                } catch (IOException var28) {
                    ;
                }
            }

        }

        try {
            GuiLauncher.ExternalDiffConfigPrompt.launch();
        } catch (IOException var30) {
            throw new CollabClientException("Could not launch external diff config prompt", var30);
        }

        String externalDiffCommand1 = manifest.isSingleFileDiff()? ConfigUtils.getExternalDiffSingleFileCommand():ConfigUtils.getExternalDiffMultipleFilesCommand();
        if(StringUtils.isBlank(externalDiffCommand1)) {
            throw new CollabClientInvalidInputException("External diff command is not configured");
        } else {
            File externalDiffCommandExe1 = new File(externalDiffCommand1);
            if(!externalDiffCommandExe1.exists()) {
                log.debug("Warning: External Diff command does not appear to exist");
            }

            String externalDiffCommandArgs1;
            try {
                externalDiffCommandArgs1 = CollabDiffConfigManifest.validateExternalDiffCommandArgs(StringUtils.defaultString(ConfigUtils.getExternalDiffSingleFileCommandArgs()), StringUtils.defaultString(ConfigUtils.getExternalDiffMultipleFilesCommandArgs()));
                if(!StringUtils.isEmpty(externalDiffCommandArgs1)) {
                    log.debug("Warning: " + externalDiffCommandArgs1);
                }
            } catch (Exception var32) {
                throw new CollabClientException(var32.getMessage());
            }

            externalDiffCommandArgs1 = manifest.isSingleFileDiff()?ConfigUtils.getExternalDiffSingleFileCommandArgs():ConfigUtils.getExternalDiffMultipleFilesCommandArgs();
            File beforeFile1 = new File(tempDir, getBeforePathOnSystem(manifest));
            afterFile = new File(tempDir, getAfterPathOnSystem(manifest));
            externalDiffCommandArgs1 = StringUtils.replace(externalDiffCommandArgs1, CollabDiffConfigManifest.BEFORE_LOCAL_PATH.getFormattedKey(), beforeFile1.getAbsolutePath());
            externalDiffCommandArgs1 = StringUtils.replace(externalDiffCommandArgs1, CollabDiffConfigManifest.AFTER_LOCAL_PATH.getFormattedKey(), afterFile.getAbsolutePath());

            CollabDiffConfigManifest.SubstitutionVariable variable;
            String value;
            for(Iterator e1 = CollabDiffConfigManifest.ALL_SUBSTITUTION_VARIABLES.iterator(); e1.hasNext(); externalDiffCommandArgs1 = StringUtils.replace(externalDiffCommandArgs1, variable.getFormattedKey(), value)) {
                variable = (CollabDiffConfigManifest.SubstitutionVariable)e1.next();
                value = manifest.props.getProperty(variable.key);
                value = formatForCmdLine(value);
            }

            log.debug("Launching external diff command:");
            log.debug(externalDiffCommandExe1 + " " + externalDiffCommandArgs1);

            return ReviewFolder.create(tempDir, beforeFile1.getAbsolutePath(), afterFile.getAbsolutePath());
        }
    }

    private static String formatForCmdLine(String value) {
        value = StringUtils.defaultString(value);
        value = StringUtils.replaceChars(value, '\"', '\'');
        return value;
    }

    private static String getBeforePathOnSystem(CollabDiffConfigManifest manifest) {
        String beforePath = manifest.getBeforePathInZip();
        beforePath = PathUtils.cleanPathForOS(beforePath);
        if(FilenameUtils.equalsOnSystem(beforePath, PathUtils.cleanPathForOS(manifest.getAfterPathInZip()))) {
            beforePath = "before" + File.separator + beforePath;
        }

        return beforePath;
    }

    private static String getAfterPathOnSystem(CollabDiffConfigManifest manifest) {
        String afterPath = manifest.getAfterPathInZip();
        afterPath = PathUtils.cleanPathForOS(afterPath);
        if(StringUtils.equals(afterPath, PathUtils.cleanPathForOS(manifest.getBeforePathInZip()))) {
            afterPath = "after" + File.separator + afterPath;
        }

        return afterPath;
    }

    private static String toSystemPath(CollabDiffConfigManifest manifest, String zipPath) throws CollabClientException {
        if("collabdiff.mf".equals(zipPath)) {
            return zipPath;
        } else {
            String beforePathInZip = manifest.getBeforePathInZip();
            if(beforePathInZip != null && zipPath.startsWith(beforePathInZip)) {
                return PathUtils.joinPaths(getBeforePathOnSystem(manifest), zipPath.substring(beforePathInZip.length()), File.separatorChar);
            } else {
                String afterPathInZip = manifest.getAfterPathInZip();
                if(afterPathInZip != null && zipPath.startsWith(afterPathInZip)) {
                    return PathUtils.joinPaths(getAfterPathOnSystem(manifest), zipPath.substring(afterPathInZip.length()), File.separatorChar);
                } else {
                    throw new CollabClientException("Unexpected path in .collabdiff file: " + zipPath);
                }
            }
        }
    }
}
