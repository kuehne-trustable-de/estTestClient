package de.trustable.ca3s.est;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.*;

public class ESTClientWrapper {

    Path clientCodeDirectory;
    String os;
    boolean isWindows = false;

    ExecutorService executorService = Executors.newFixedThreadPool(10);

    public ESTClientWrapper() throws IOException {

        clientCodeDirectory = Files.createTempDirectory("wrapperDir");

        os = System.getProperty("os.name");
        if( os.toLowerCase(Locale.ROOT).startsWith("windows" )){
            isWindows = true;
            String resourcePath = "/win/";
            ResourceHelper.copyResourceToFile(resourcePath + "estclient.exe", clientCodeDirectory);
            ResourceHelper.copyResourceToFile(resourcePath + "libcrypto-1_1.dll", clientCodeDirectory);
            ResourceHelper.copyResourceToFile(resourcePath + "libssl-1_1.dll", clientCodeDirectory);
            ResourceHelper.copyResourceToFile(resourcePath + "est.dll", clientCodeDirectory);
        }else{
            String resourcePath = "/linux/";
            File estClientFile = ResourceHelper.copyResourceToFile(resourcePath + "estclient", clientCodeDirectory);
            //noinspection ResultOfMethodCallIgnored
            estClientFile.setExecutable(true);

//            ResourceHelper.copyResourceToFile(resourcePath + "ld-linux-x86-64.so.2", clientCodeDirectory);
//            ResourceHelper.copyResourceToFile(resourcePath + "libc.so.6", clientCodeDirectory);
//            ResourceHelper.copyResourceToFile(resourcePath + "libdl.so.2", clientCodeDirectory);
//            ResourceHelper.copyResourceToFile(resourcePath + "libpthread.so.0", clientCodeDirectory);
            ResourceHelper.copyResourceToFile(resourcePath + "libcrypto.so.1.1", clientCodeDirectory);
            ResourceHelper.copyResourceToFile(resourcePath + "libest-3.2.0p.so", clientCodeDirectory);
            ResourceHelper.copyResourceToFile(resourcePath + "libssl.so.1.1", clientCodeDirectory);

        }
    }

    public int execute(List<String> argList) throws IOException, InterruptedException, ExecutionException, TimeoutException {
        ProcessBuilder builder = new ProcessBuilder();

        List<String> cmdList = new ArrayList<>();
        if (isWindows) {
            cmdList.add("cmd.exe");
            cmdList.add("/c");
            cmdList.add("estclient.exe");

            cmdList.addAll(argList);
        } else {
            cmdList.add("sh");
            cmdList.add("-c");
            cmdList.add(clientCodeDirectory.toAbsolutePath() + File.separator +
                    "estclient \"" + String.join(" ",argList) + "\"");

            builder.environment().put("LD_LIBRARY_PATH", clientCodeDirectory.toAbsolutePath().toString());
        }

        System.out.println("args: " + String.join(", ", cmdList));
        builder.command(cmdList);
        builder.directory(clientCodeDirectory.toFile());

        Process process = builder.start();

        Future<?> futureOut = executorService.submit(new StreamGobbler(process.getInputStream(), System.out::println));
        Future<?> futureErr = executorService.submit(new StreamGobbler(process.getInputStream(), System.err::println));
        futureErr.get(10, TimeUnit.SECONDS);
        futureOut.get(10, TimeUnit.SECONDS);

        return process.waitFor();
    }

    public static void main(String[] args) throws IOException, ExecutionException, InterruptedException, TimeoutException {

        ESTClientWrapper estClientWrapper = new ESTClientWrapper();
        int exitCode = estClientWrapper.execute(Arrays.asList(args));
        System.exit(exitCode);
    }
}
