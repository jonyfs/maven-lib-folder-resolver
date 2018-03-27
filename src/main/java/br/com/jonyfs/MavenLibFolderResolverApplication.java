package br.com.jonyfs;

import com.google.common.base.Strings;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.JarURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.Properties;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import lombok.extern.slf4j.Slf4j;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@Slf4j
@SpringBootApplication
public class MavenLibFolderResolverApplication implements CommandLineRunner {

    @Value("${dir}")
    String directory;

    @Value("${repository}")
    String repository;

    @Override
    public void run(String... args) throws IOException {
        LOGGER.info("Analyzing directory {}", directory);
        if (Strings.isNullOrEmpty(directory)) {
            LOGGER.error("dir var is expected", directory);
        }
        if (Files.isDirectory(Paths.get(directory))) {
            if (Strings.isNullOrEmpty(repository)) {
                repository = directory;
            }
            LOGGER.info("<!-- Suggestion for not recognized dependencies -->");
            LOGGER.info("<!-- <repositories><repository><id>local-repo</id><url>file://{}</url></repository></repositories> -->", repository);

            Files.newDirectoryStream(Paths.get(directory),
                path -> path.toString().endsWith(".jar"))
                .forEach((jar) -> analyze(jar));
        } else {
            LOGGER.error("{} is not a directory", directory);
        }
    }

    public static void main(String[] args) {
        SpringApplication.run(MavenLibFolderResolverApplication.class, args);
    }

    private void analyze(Path file) {

        try {

            Model model = readMaven(file.toString());
            if (model == null) {
                LOGGER.info("<!-- {} --> <!-- WITHOUT MAVEN -->", file.getFileName());
                LOGGER.info("<!-- Suggestion for {} -->", file.getFileName());
                LOGGER.info("<!-- mvn install:install-file  -Dfile={} -DgroupId={} -DartifactId={} -Dversion={} -Dpackaging=jar -DlocalRepositoryPath=file://{} -->", file.toString(), file.getFileName(), file.getFileName(), file.getFileName(), repository);
                LOGGER.info("<!-- <dependency><groupId>{}</groupId><artifactId>{}</artifactId><version>{}</version></dependency> -->", file.getFileName(), file.getFileName(), file.getFileName());
            } else {
                LOGGER.info("<!-- {} -->", file.getFileName());
                LOGGER.info("<dependency><groupId>{}</groupId><artifactId>{}</artifactId><version>{}</version></dependency>", model.getGroupId(), model.getArtifactId(), model.getVersion());
            }

        } catch (Exception e) {
            LOGGER.error("Fail to read {}", file, e);
        }
    }

    private void readManifest(Path file) throws Exception {
        String manifestPath = "META-INF/MANIFEST.MF";

        LOGGER.info("reading {}!{}", file, manifestPath);
        InputStream in = null;
        URL inputURL = null;
        String inputFile = new StringBuilder().append("jar:").append("file:/").append(file.toString()).append("!").append(manifestPath).toString();
        if (inputFile.startsWith("jar:")) {
            inputURL = new URL(inputFile);
            JarURLConnection conn = (JarURLConnection) inputURL.openConnection();
            in = conn.getInputStream();
            LOGGER.info("{} recognized", inputFile);
            return;
        }
    }

    public static String readManifest(String sourceJARFile) throws IOException {
        ZipFile zipFile = new ZipFile(sourceJARFile);
        Enumeration entries = zipFile.entries();

        while (entries.hasMoreElements()) {
            ZipEntry zipEntry = (ZipEntry) entries.nextElement();
            if (zipEntry.getName().equals("META-INF/MANIFEST.MF")) {
                return toString(zipFile.getInputStream(zipEntry));
            }
        }

        throw new IllegalStateException("Manifest not found");
    }

    private static String toString(InputStream inputStream) throws IOException {
        StringBuilder stringBuilder = new StringBuilder();
        try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                stringBuilder.append(line);
                stringBuilder.append(System.lineSeparator());
            }
        }

        return stringBuilder.toString().trim() + System.lineSeparator();
    }

    public Model readMaven(String jar) throws Exception {
        InputStream pathPomXml = getInputStream("pom.xml", jar);
        InputStream pathPomProperties = getInputStream("pom.properties", jar);

        if (pathPomXml == null) {
            if (pathPomProperties == null) {
                return null;
            } else {
                return readPomProperties(pathPomProperties);
            }
        } else {
            Model model = readPom(pathPomXml);
            if (model != null) {
                if (Strings.isNullOrEmpty(model.getGroupId()) || Strings.isNullOrEmpty(model.getArtifactId()) || Strings.isNullOrEmpty(model.getVersion())) {
                    if (pathPomProperties == null) {
                        model = null;
                    } else {
                        model = readPomProperties(pathPomProperties);
                    }
                }
            }
            return model;
        }
    }

    public InputStream getInputStream(String fileName, String jar) throws Exception {
        JarFile jarFile = new JarFile(jar);

        final Enumeration<JarEntry> entries = jarFile.entries();
        while (entries.hasMoreElements()) {
            final JarEntry entry = entries.nextElement();
            if (entry.getName().contains(".")) {
                //System.out.println("File : " + entry.getName());
                JarEntry fileEntry = jarFile.getJarEntry(entry.getName());
                if (fileEntry.getName().endsWith(fileName)) {
                    return jarFile.getInputStream(entry);
                }
                //InputStream input = jarFile.getInputStream(fileEntry);
                //process(input);
            }
        }
        return null;
    }

    public Model readPomProperties(InputStream inputStream) throws Exception {
        Model model = new Model();
        java.util.Properties p = new Properties();
        p.load(inputStream);
        model.setGroupId(p.getProperty("groupId"));
        model.setArtifactId(p.getProperty("artifactId"));
        model.setVersion(p.getProperty("version"));
        return model;
    }

    public Model readPom(InputStream inputStream) throws Exception {
        MavenXpp3Reader reader = new MavenXpp3Reader();
        Model model;
        model = reader.read(inputStream);
        return model;
    }
}
