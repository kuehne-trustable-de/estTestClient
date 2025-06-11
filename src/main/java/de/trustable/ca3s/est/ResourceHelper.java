package de.trustable.ca3s.est;

import java.io.*;
import java.nio.file.Path;

public class ResourceHelper {

    public static File copyResourceToFile(String resourcePath, Path targetPath, boolean verbose) {

        String fileName = resourcePath.substring(resourcePath.lastIndexOf("/") + 1);
        File targetFile = new File(targetPath.toFile(), fileName);

        try (InputStream in = ResourceHelper.class.getResourceAsStream(resourcePath);
             OutputStream out = new FileOutputStream(targetFile)) {

            if (in == null) {
                throw new RuntimeException("Ressource not found: " + resourcePath);
            }

            byte[] buffer = new byte[8*1024];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
        } catch (IOException e) {
            throw new RuntimeException("Problem copying " + fileName + ": " + e.getMessage());
        }
        if(verbose) {
            System.out.println("Copied " + fileName + " to " + targetFile.getAbsolutePath());
        }
        return targetFile;
    }

}

/*
public class ResourceCopier {

    public static void main(String[] args) {
        // Liste der Ressourcen, die kopiert werden sollen
        String[] resources = {
                "/resourcen/datei1.txt",
                "/resourcen/datei2.txt",
                "/resourcen/datei3.txt"
        };

        // Zielverzeichnis
        String zielVerzeichnis = "zielordner";

        // Zielordner erstellen, falls er nicht existiert
        File zielDir = new File(zielVerzeichnis);
        if (!zielDir.exists()) {
            boolean created = zielDir.mkdirs();
            if (created) {
                System.out.println("Zielverzeichnis wurde erstellt: " + zielVerzeichnis);
            } else {
                System.out.println("Fehler beim Erstellen des Zielverzeichnisses.");
                return;
            }
        }

        // Ressourcen kopieren
        for (String resource : resources) {
            copyResourceToFile(resource, zielVerzeichnis);
        }
    }

    private static void copyResourceToFile(String resourcePath, String zielVerzeichnis) {
        // Name der Datei aus dem Ressourcenpfad extrahieren
        String dateiName = resourcePath.substring(resourcePath.lastIndexOf("/") + 1);
        File zielDatei = new File(zielVerzeichnis, dateiName);

        try (InputStream in = ResourceCopier.class.getResourceAsStream(resourcePath);
             OutputStream out = new FileOutputStream(zielDatei)) {

            if (in == null) {
                System.out.println("Ressource nicht gefunden: " + resourcePath);
                return;
            }

            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
            System.out.println("Kopiert: " + dateiName);
        } catch (IOException e) {
            System.out.println("Fehler beim Kopieren von " + dateiName + ": " + e.getMessage());
        }
    }
*/
