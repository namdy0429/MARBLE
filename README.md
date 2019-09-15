MARBLE: Mining for Boilerplate Code to Identify API Usability Problems 
================
MARBLE (Mining API Repositories for Boilerplate Lessening Effort) is an automated technique for identifying instances of boilerplate API client code. MARBLE adapts existing techniques, including an API usage mining algorithm, an AST comparison algorithm, and a graph partitioning algorithm.

Further documentations for the source code and scripts to run end-to-end mining will be available soon. 
Currently, runnables for MARBLE are available for use, and you can find them under ```runnables/``` or release tab. Example client code files and intermediate results for ```javax.xml.transform``` mining are available under ```data/```.

Quickstart with Runnables 
------------

#### Step 0: Collecting Client Code
This step collects client code files of target APIs from large repository. Before this step, place GitHub (or other) projects into one directory (e.g., data/repos in the example). Then, ```generateCorpora.py``` only find client java files importing target APIs in ```api_list.txt``` such as:
```
javax.xml.transform
javax.swing.JFrame
...
```
After this step, you will see directories under [output directory], ```data/source```, for each target API, each of which contains symlinks to client code files using the target API.

```
$ python3 generateCorpora.py -i data/repos/ -o data/source/ -p 8 -a data/api_list.txt
```
* **-i**  &nbsp;  Repository directory containing a number of java projects
* **-o**  &nbsp;  Output directory
* **-p** &nbsp; Number of threads to use
* **-a** &nbsp; API list file


#### Step 1: Extracting API Calls
This step is to extract API calls from the set of client code files using the target API, such as ```javax.xml.transform```. (We adjusted [PAM's public implementation](https://github.com/mast-group/api-mining))
```
$ java -jar runnables/APICallExtractor.jar -sd data/source/ -od data/calls/ -pn javax.xml.transform -sn 123
```
* **-sd**  &nbsp;  Client code source directory (output directory of Step 0)
* **-od**  &nbsp;  Output directory
* **-pn**  &nbsp;  Package name
* **-sn**  &nbsp;  Sampling number (optional)

It will return ```javax_xml_transform.arff``` which contains data about 1) caller file, 2) caller package, 3) caller method, and 4) sequences of API calls, like:
```
'jenkinsci.jenkins.WebAppMain','hudson','hudson.WebAppMain.contextInitialized(javax.servlet.ServletContextEvent)','javax.xml.transform.TransformerFactory.newInstance javax.xml.transform.TransformerFactory.getName javax.xml.transform.TransformerFactory.newInstance'
'hibernate.hibernate-orm.LocalSchemaLocator','org.hibernate.boot.jaxb.internal.stax','org.hibernate.boot.jaxb.internal.stax.LocalSchemaLocator.resolveLocalSchema(java.net.URL)','javax.xml.transform.stream.StreamSource.<init>'
'deeplearning4j.deeplearning4j.Configuration','org.datavec.api.conf','org.datavec.api.conf.Configuration.writeXml(java.io.OutputStream)','javax.xml.transform.dom.DOMSource.<init> javax.xml.transform.stream.StreamResult.<init> javax.xml.transform.TransformerFactory.newInstance javax.xml.transform.TransformerFactory.newTransformer javax.xml.transform.Transformer.transform'
```

#### Step 2: Frequent API Usage Mining
This step runs PAM (Probabilistic API Miner) to mine interesting API patterns from the list of API call sequences (We adjusted [PAM's public implementation](https://github.com/mast-group/api-mining)). It returns two files, ```PAM_seqs.txt``` and ```MARBLE_logs.log```. ```PAM_seqs.txt``` is the result file of PAM containing a list of usage patterns (i.e., sequences of API calls), and ```MARBLE_logs.log``` has richer information about the usgae patterns, which will be used in the following steps.
```
$ java -jar runnables/PAM.jar -f data/calls/javax_xml_transform.arff -sd data/source/ -od data/output/
```
* **-f**  &nbsp;  arff file from API extraction (output file from Step 1)
* **-sd**  &nbsp;  Source directory
* **-od**  &nbsp;  Output directory

#### Step 3: Removing Spurious Patterns
This step filters API usage patterns that are redundant or rare to reduce the false positive boilerplate candidates returned later.
```
$ python3 filterSpuriousPatterns.py -i data/output/javax_xml_transform/MARBLE_logs.log -o data/output/javax_xml_transform/reduced_MARBLE_logs.log -mn 6
```
* **-i**  &nbsp;  Raw PAM log file (from step 2)
* **-o**  &nbsp;  Output log file after removing spurious patterns
* **-mn** &nbsp;  Minimum support (we use [number of client code files] * 0.05 in the paper)

#### Step 4: AST Comparision
This step compares ASTs around API usage patterns to consider the structural context. It returns  structural similarity scores between all pairs of client code files for each API usage pattern.
```
$ java -jar runnables/ASTComparison.jar -f data/output/javax_xml_transform/reduced_MARBLE_logs.log -sd data/source/javax_xml_transform/ -od data/output/javax_xml_transform/diff/ -ps 0 -pl 6 -p 2
```
* **-f**  &nbsp;  PAM log file after removing spurious patterns (from step 3)
* **-sd**  &nbsp;  Client code source directory
* **-od**  &nbsp;  Output directory
* **-ps, -pl**  &nbsp;  Start and the last index of PAM patterns to set the range of AST comparision (optional)
* **p** &nbsp; Number of threads to use (optional)

#### Step 5: Graph Partitioning
This step partitions graphs to capture the different contexts (structures) in which an API call sequence is being used. 
```
$ python3 partitionGraph.py -i data/output/javax_xml_transform/
```
* **-i**  &nbsp;  Input directory containing AST diffs

#### Step 6: Generate Viewer
This step generates a html file containing the final boilerplate candidates and client code examples using them. 
```
$ python3 generateViewer.py -i data/output/javax_xml_transform/ -o data/output/ -s data/source/javax_xml_transform -a javax_xml_transform
```

* **-i**  &nbsp;  Input directory containint 
* **-o**  &nbsp;  Output directory
* **-s**  &nbsp;  Client code source directory
* **-a**  &nbsp;  API name with '\_'

