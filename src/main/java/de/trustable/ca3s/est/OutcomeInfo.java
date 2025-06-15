package de.trustable.ca3s.est;

public class OutcomeInfo {

    final private String out;
    final private String err;
    final private int exitCode;

    public OutcomeInfo(String out, String err, int exitCode){
        this.out = out;
        this.err = err;
        this.exitCode = exitCode;
    }

    /**
     * The string content of the standard output stream
     *
     * @return stdout content
     */
    public String getOut() {
        return out;
    }

    /**
     * The string content of the standard error stream
     *
     * @return error content
     */
    public String getErr() {
        return err;
    }

    /**
     * The exit code of the libest client process
     * @return 0 for OK, > 0 marks a problem
     */
    public int getExitCode() {
        return exitCode;
    }
}
