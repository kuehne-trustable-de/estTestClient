# ESTClientWrapper
A wrapper around os-specific versions of libest (https://github.com/cisco/libest) client.
The pre-build native versions for x86-Windows and -Linux are packaged in this wrapper.
This enables testing of an EST server (https://www.rfc-editor.org/rfc/rfc7030.html) on different platforms.
The binaries were copied into a temporary directory and executed there.This has security drawbacks. So use this wrapper for testing in non-productive environments, only.

## License
This code is published under the [European Union Public Licence (EUPL-1.2)]{https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12}
The included code of libest confirms with these [licences]{https://github.com/cisco/libest/blob/main/LICENSE}

## Usage

### Command line
The wrapper has a call level interface forwarding all arguments to the binary unchanged.
Here is a sample call showing the libest client help:

```
java  -jar .\ESTClientWrapper-{verion}.jar -?
```

The client expects a set of trust anchors provided in an environment variable 'EST_OPENSSL_CACERT'. This variable must be set for all relevant actions.
Providing this variable may be os specific. To simplify it the wrapper provides an option to set this environment variable. Multiple values are separated by commas.

| value of 'CA_CERT' | remarks                                                                                                            |
|--------------------|--------------------------------------------------------------------------------------------------------------------|
| 'java-truststore'  | forward all certificates of the Java runtime truststore                                                            |
| 'server-certs'     | read the certificates from the EST server. Useful if the server provides its trust anchor during the TLS handshake |
| a filename         | the filename will be set as value of 'EST_OPENSSL_CACERT'                                                          |

A sample call looks like this:
```
java -DCA_CERT=java-truststore -jar .\ESTClientWrapper-{verion}.jar -?
```

Other options are

| name            | value        | remarks                                                                      |
|-----------------|--------------|------------------------------------------------------------------------------|
| WRAPPER_VERBOSE | true / false | log some details of the wrapper internals                                    |
| KEEP_CODE_DIR   | true / false | keep the directory with the copied binaries and the trust store (if created) |

### Test class

```
	@BeforeAll
	public static void setUpBeforeClass() throws IOException {
	
	    // Init the Wrapper class once, only
        estClientWrapper = new ESTClientWrapper();
        estClientWrapper.setVerbose(true);
    }

    @Test
    public void testGetCaCerts() throws Exception{

        List<String> argList = new ArrayList<String>();

	    // Provide the client arguments
        argList.add("-v");
        
        argList.add("-g");
        
        argList.add("-s");
        argList.add(host);
        
        argList.add("-p");
        argList.add( "" + serverPort);
        
        argList.add("-o" );
        argList.add(".");

        // convenience method to grab certificates from the TLS endpoint. Only useful in simple, 'one root, only' setups
        estClientWrapper.buildCaCertForServer(host, serverPort);

        // execute the request
        OutcomeInfo outcomeInfo = estClientWrapper.execute(argList);

        // dump the response streams. For more qualified testing do some evaluation on that content 
        LOG.info("out: {}", outcomeInfo.getOut());
        LOG.info("err: {}", outcomeInfo.getErr());

        // check the ecxit code. 
        // Beware: Errors may habǘe occured even with 'exitcode == 0' 
        Assertions.assertEquals(0, outcomeInfo.getExitCode());
        
        // perform other tests with the same estClientWrapper instance ...
    }
```

## libest building hints

There may be reasons to rebuild the binaries, e.g. extending a buffer size.
Place the new artefacts in the corresponding src/main/resources directories, update the version in pom.xml and rebuild the package using

```
mvn clean package
```
Find the new artefact under /target .


### Linux
A perfect guidance document leads you thru building the libest in a Ubuntu 18 docker image:
https://kevinsaye.wordpress.com/2021/05/18/creating-a-simple-enrollment-over-secure-transport-est-server/

Just watch it build! Done!

### Windows
Install Visual Studio, the Community Edition is sufficient for the job.

We need openssl 1.1 and zlib as prerequisite for building libest. Luckily the are handy repos containing VC projects available on guthub:
https://github.com/kiyolee/zlib-win-build.git
https://github.com/kiyolee/openssl1_1-win-build.git

Change the build target to 32 bit, that's what the libest build script expects.

Install the prerequites mentioned in the 'Windows install' section of the libest readme.

start gradle, copy/rename the missing libs to the expected locations

start gradle again. A est.dll was build

change to the example directory. Rename 'build_example.gradle' to 'build.gradle'. gradle does not accepted other files as build files anymore.
drop all model/components other than 'estclient(NativeExecutableSpec)'. The dropped components require e.g. the getopt module. 

start gradle and find the relevant artefacts in 'example/build/exe/estclient'

Done!

