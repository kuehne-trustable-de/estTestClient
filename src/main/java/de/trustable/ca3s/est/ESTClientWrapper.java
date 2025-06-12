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

    boolean verbose;
    Path clientCodeDirectory;
    String os;
    boolean isWindows = false;

    ExecutorService executorService = Executors.newFixedThreadPool(10);

    public ESTClientWrapper() throws IOException {

        verbose = Boolean.parseBoolean(System.getProperty("WRAPPER_VERBOSE", "false"));
//        System.out.println("WRAPPER_VERBOSE: " + verbose);
        boolean keepCodeDir = Boolean.parseBoolean(System.getProperty("KEEP_CODE_DIR", "false"));
//        System.out.println("KEEP_CODE_DIR: " + keepCodeDir);
        clientCodeDirectory = Files.createTempDirectory("wrapperDir");
        if(keepCodeDir){
            if(verbose) {
                System.out.println("clientCodeDirectory: " + clientCodeDirectory.toString());
            }
        }else{
            clientCodeDirectory.toFile().deleteOnExit();
        }

        os = System.getProperty("os.name");
        if( os.toLowerCase(Locale.ROOT).startsWith("windows" )){
            isWindows = true;
            String resourcePath = "/win/";
            ResourceHelper.copyResourceToFile(resourcePath + "estclient.exe", clientCodeDirectory, verbose);
            ResourceHelper.copyResourceToFile(resourcePath + "libcrypto-1_1.dll", clientCodeDirectory, verbose);
            ResourceHelper.copyResourceToFile(resourcePath + "libssl-1_1.dll", clientCodeDirectory, verbose);
            ResourceHelper.copyResourceToFile(resourcePath + "est.dll", clientCodeDirectory, verbose);
        }else{
            String resourcePath = "/linux/";
            File estClientFile = ResourceHelper.copyResourceToFile(resourcePath + "estclient", clientCodeDirectory, verbose);
            //noinspection ResultOfMethodCallIgnored
            estClientFile.setExecutable(true);

            ResourceHelper.copyResourceToFile(resourcePath + "libcrypto.so.1.1", clientCodeDirectory, verbose);
            ResourceHelper.copyResourceToFile(resourcePath + "libest-3.2.0p.so", clientCodeDirectory, verbose);
            ResourceHelper.copyResourceToFile(resourcePath + "libssl.so.1.1", clientCodeDirectory, verbose);
        }
    }

    public OutcomeInfo execute(List<String> argList) throws IOException, InterruptedException, ExecutionException, TimeoutException {
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
                    "estclient " + String.join(" ",argList) );

            builder.environment().put("LD_LIBRARY_PATH", clientCodeDirectory.toAbsolutePath().toString());
        }

        if(verbose) {
            System.out.println("args: " + String.join(", ", cmdList));
        }
        builder.command(cmdList);
        builder.directory(clientCodeDirectory.toFile());

        Process process = builder.start();

        StringBuffer outBuffer = new StringBuffer();
        StringBuffer errBuffer = new StringBuffer();
        Future<?> futureOut = executorService.submit(new StreamGobbler(process.getInputStream(),
                s -> outBuffer.append(s).append(System.lineSeparator())));
        Future<?> futureErr = executorService.submit(new StreamGobbler(process.getErrorStream(),
                s -> errBuffer.append(s).append(System.lineSeparator())));
        futureErr.get(10, TimeUnit.SECONDS);
        futureOut.get(10, TimeUnit.SECONDS);

        int exitCode = process.waitFor();
        return new OutcomeInfo(outBuffer.toString(), errBuffer.toString(), exitCode);
    }

    public static void main(String[] args) throws IOException, ExecutionException, InterruptedException, TimeoutException {

        ESTClientWrapper estClientWrapper = new ESTClientWrapper();
        OutcomeInfo outcomeInfo = estClientWrapper.execute(Arrays.asList(args));
        System.err.println("### err stream:\n" + outcomeInfo.getErr());
        System.out.println("### out stream:\n" + outcomeInfo.getOut());
        System.exit(outcomeInfo.getExitCode());
    }
}
