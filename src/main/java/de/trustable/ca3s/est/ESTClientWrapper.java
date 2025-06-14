package de.trustable.ca3s.est;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.*;

/**
 * Wrapper for the libest client in an os-independent way
 * All program arguments are forwarded to the est client
 *
 */
public class ESTClientWrapper {

    boolean verbose;
    Path clientCodeDirectory;
    String os;
    boolean isWindows = false;

    String cacert = null;

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
                    "estclient " + String.join(" ", argList));

            builder.environment().put("LD_LIBRARY_PATH", clientCodeDirectory.toAbsolutePath().toString());
        }

        if (getCacert() != null){
            builder.environment().put("EST_OPENSSL_CACERT", getCacert());
            if(verbose) {
                System.out.println("environment : EST_OPENSSL_CACERT to " + getCacert());
            }
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

        String cacert = System.getProperty("CA_CERT", null);
        System.err.println("--- cacert :" + cacert);
        if("java-truststore".equalsIgnoreCase(cacert)){
            estClientWrapper.buildCaCertFromTruststore();
        } else if("server-certs".equalsIgnoreCase(cacert)){
            estClientWrapper.buildCaCertForServer(args);
        } else if(cacert != null){
            estClientWrapper.setCacert(cacert);
        }

        estClientWrapper.buildCaCertForServer(args);

        OutcomeInfo outcomeInfo = estClientWrapper.execute(Arrays.asList(args));
        System.err.println("### err stream:\n" + outcomeInfo.getErr());
        System.out.println("### out stream:\n" + outcomeInfo.getOut());
        System.exit(outcomeInfo.getExitCode());
    }

    /**
     * retrieve host and port from arguments.
     * if present, get the certificate chain from the server and write it to a temp file
     * and preset the value of EST_OPENSSL_CACERT.
     * Useful for test where the server certs are not known in advance and includes the full chain.
     *
     * @param args the arguments as expected by the libest client
     */
    void buildCaCertForServer(String[] args){
        String host = null;
        int port = 0;
        for(int i = 0; i < args.length -1; i++){
            String arg = args[i];
            if ("-s".equals(arg)){
                host = args[i+1];
            }
            if ("-p".equals(arg)){
                port = Integer.parseInt(args[i+1]);
            }
        }
        if (host == null || port == 0){
            return;
        }
        try {
            String cacert = TLSServerHelper.getServerCertificates(host, port);

            File cacertFile = File.createTempFile("cacert_server", ".crt");
            try (FileOutputStream fos = new FileOutputStream(cacertFile)) {
                fos.write(cacert.getBytes(StandardCharsets.UTF_8));
            }

            setCacert(cacertFile.getAbsolutePath());
            if(verbose) {
                System.out.println("### server certs for " + host + ":" + port + " written successfully: \n" + cacert);
            }
        } catch (Exception e) {
            System.err.println("### retrieval of server certs for " + host + ":" + port + " failed: " + e.getMessage() );
        }
    }

    /**
     * retrieve all trusted certs from Java runtime
     */
    void buildCaCertFromTruststore(){
        try {
            String cacert = TLSServerHelper.getTrustedCerts();

            File cacertFile = File.createTempFile("cacert_truststore", ".crt");
            try (FileOutputStream fos = new FileOutputStream(cacertFile)) {
                fos.write(cacert.getBytes(StandardCharsets.UTF_8));
            }

            setCacert(cacertFile.getAbsolutePath());
            if(verbose) {
                System.out.println("### truststore certs written successfully: \n" + cacert);
            }
        } catch (Exception e) {
            System.err.println("### retrieval of truststore certs failed: " + e.getMessage() );
        }
    }

    public String getCacert() {
        return cacert;
    }

    public void setCacert(String cacert) {
        this.cacert = cacert;
    }
}
