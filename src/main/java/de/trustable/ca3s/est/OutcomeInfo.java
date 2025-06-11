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

    public String getOut() {
        return out;
    }

    public String getErr() {
        return err;
    }

    public int getExitCode() {
        return exitCode;
    }
}
