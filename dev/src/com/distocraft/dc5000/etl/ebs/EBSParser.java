/*
 * Created on 15.9.2008
 *
 */
package com.distocraft.dc5000.etl.ebs;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

import com.distocraft.dc5000.etl.parser.Main;
import com.distocraft.dc5000.etl.parser.Parser;
import com.distocraft.dc5000.etl.parser.SourceFile;
import com.ericsson.eniq.common.ENIQEntityResolver;

/**
 * EBS Parser <br/>
 * <br/>
 * Configuration: <br/>
 * <br/>
 * Database usage: Not directly <br/>
 * <br/>
 * <br/>
 * Version supported: <br/>
 * <br/>
 * Copyright Ericsson 2008 <br/>
 * <br/>
 * $id$ <br/>
 * 
 * <br/>
 * <br/>
 * <table border="1" width="100%" cellpadding="3" cellspacing="0">
 * <tr bgcolor="#CCCCFF" class="TableHeasingColor">
 * <td colspan="4"><font size="+2"><b>Parameter Summary</b></font></td>
 * </tr>
 * <tr>
 * <td><b>Name</b></td>
 * <td><b>Key</b></td>
 * <td><b>Description</b></td>
 * <td><b>Default</b></td>
 * </tr>
 * <tr>
 * <td>fillMissingEbsResultValueWith</td>
 * <td>contains value that is used when EBS-file does not contain R-resultvalue. 
 *     If this parameter is not set, the missing parameter is filled/replaced with "null"-string
 * </td>
 * </tr>
 * <tr>
 * <td>Vendor ID mask</td>
 * <td>3GPP32435Parser.vendorIDMask</td>
 * <td>Defines how to parse the vendorID</td>
 * <td>.+,(.+)=.+</td>
 * </tr>
 * <tr>
 * <td>Vendor ID from</td>
 * <td>3GPP32435Parser.readVendorIDFrom</td>
 * <td>Defines where to parse vendor ID (file/data supported)</td>
 * <td>data</td>
 * </tr>
 * <tr>
 * <td>Fill empty MOID</td>
 * <td>3GPP32435Parser.FillEmptyMOID</td>
 * <td>Defines whether empty moid is filled or not (true/ false)</td>
 * <td>true</td>
 * </tr>
 * <tr>
 * <td>Fill empty MOID style</td>
 * <td>3GPP32435Parser.FillEmptyMOIDStyle</td>
 * <td>Defines the style how moid is filled (static/inc supported)</td>
 * <td>inc</td>
 * </tr>
 * <tr>
 * <td>Fill empty MOID value</td>
 * <td>3GPP32435Parser.FillEmptyMOIDValue</td>
 * <td>Defines the value for the moid that is filled</td>
 * <td>0</td>
 * </tr>
 * </table> <br/>
 * <br/>
 * <table border="1" width="100%" cellpadding="3" cellspacing="0">
 * <tr bgcolor="#CCCCFF" class="TableHeasingColor">
 * <td colspan="2"><font size="+2"><b>Added DataColumns</b></font></td>
 * </tr>
 * <tr>
 * <td><b>Column name</b></td>
 * <td><b>Description</b></td>
 * </tr>
 * <tr>
 * <td>collectionBeginTime</td>
 * <td>contains the begin time of the whole collection</td>
 * </tr>
 * <tr>
 * <td>objectClass</td>
 * <td>contains the vendor id parsed from MOID</td>
 * </tr>
 * <tr>
 * <td>MOID</td>
 * <td>contains the measured object id</td>
 * </tr>
 * <tr>
 * <td>filename</td>
 * <td> contains the filename of the inputdatafile.</td>
 * </tr>
 * <tr>
 * <td>PERIOD_DURATION</td>
 * <td>contains the parsed duration of this measurement</td>
 * </tr>
 * <tr>
 * <td>DIRNAME</td>
 * <td>Contains full path to the inputdatafile.</td>
 * </tr>
 * <tr>
 * <td>JVM_TIMEZONE</td>
 * <td>contains the JVM timezone (example. +0200) </td>
 * </tr>
 * <tr>
 * <td>vendorName</td>
 * <td>contains the vendor name</td>
 * </tr> 
 * <tr>
 * <td>fileFormatVersion</td>
 * <td>contains the version of file format</td>
 * </tr>
 * <tr>
 * <td>measInfoId</td>
 * <td>contains the measInfoId</td>
 * </tr>
 * <tr>
 * <td>jobId</td>
 * <td>contains the jobId</td>
 * </tr>
 * <tr>
 * <td>&lt;measType&gt; (amount varies based on measurement executed)</td>
 * <td>&lt;measValue&gt; (amount varies based on measurement executed)</td>
 * </tr>

 * <!-- THESE ARE NOT USED ANYMORE
 * <tr>
 * <td>DATETIME_ID</td>
 * <td>contains the counted starttime of this measurement</td>
 * </tr>
 * <tr>
 * <td>DC_SUSPECTFLAG</td>
 * <td>contains the suspected flag value</td>
 * </tr>
 * <tr>
 * <td>dnPrefix</td>
 * <td>contains the dn prefix</td>
 * </tr>
 * <tr>
 * <td>localDn</td>
 * <td>contains the local dn</td>
 * </tr>
 * <tr>
 * <td>managedElementLocalDn</td>
 * <td>contains the local dn of managedElement element</td>
 * </tr>
 * <tr>
 * <td>elementType</td>
 * <td>contains the element type</td>
 * </tr>
 * <tr>
 * <td>userLabel</td>
 * <td>contains the user label</td>
 * </tr>
 * <tr>
 * <td>swVersion</td>
 * <td>contains the software version</td>
 * </tr>
 * <tr>
 * <td>endTime</td>
 * <td>contains the granularity period end time</td>
 * </tr>
 * THESE ARE NOT USED ANYMORE -->
 * 
 * </table> <br/>
 * <br/>
 * 
 * @author epetrmi <br/>
 *         <br/>
 * 
 */
public class EBSParser implements Parser {

  private Logger log;
  
  private SourceFile sourceFile;
  private String techPack;
  private String setType;
  private String setName;
  
  private Main mainParserObject = null;
  private String workerName = "";
  final private List errorList = new ArrayList();

  //##TODO## Are these defined somehow/where?
  private int status = 0;
  //In this class it seems that 
  //0 = parser created
  //1 = initializing
  //2 = running
  //3 = finished (success/failure)
  
  /**
   * Parameters for throughput measurement.
   */
  private long parseStartTime;
  private long totalParseTime;
  private long fileSize;
  private int fileCount;

  /**
   * Initialize parser
   */
  public void init(final Main main, final String techPack,
      final String setType, final String setName, final String workerName) {
    this.mainParserObject = main;
    this.techPack = techPack;
    this.setType = setType;
    this.setName = setName;
    this.status = 1;
    this.workerName = workerName;

    String logWorkerName = "";
    if (workerName.length() > 0) {
      logWorkerName = "." + workerName;
    }

    log = Logger.getLogger("etl." + techPack + "." + setType + "." + setName
        + ".parser.EBSParser" + logWorkerName);
  }

  public int status() {
    return status;
  }

  /* Run
   * @see java.lang.Runnable#run()
   */
  public void run() {

	  try {

		  this.status = 2;
		  SourceFile sf = null;
		  parseStartTime = System.currentTimeMillis();
		  while ((sf = mainParserObject.nextSourceFile()) != null) {
			  try {
				  fileCount++;
				  fileSize += sf.fileSize();
				  mainParserObject.preParse(sf);
				  parse(sf, techPack, setType, setName);
				  mainParserObject.postParse(sf);
			  } catch (Exception e) {
				  mainParserObject.errorParse(e, sf);
			  } finally {
				  mainParserObject.finallyParse(sf);
			  }
		  }
		  totalParseTime = System.currentTimeMillis() - parseStartTime;
		  if (totalParseTime != 0) {
			  log.info("Parsing Performance :: " + fileCount + " files parsed in " + totalParseTime
					  + " milliseconds, filesize is " + fileSize + " bytes and throughput : " + (fileSize / totalParseTime)
					  + " bytes/ms.");
		  }
	  } catch (Exception e) {
		  // Exception catched at top level. No good.
		  log.log(Level.WARNING, "Worker parser failed to exception", e);
		  errorList.add(e);
	  } finally {
		  this.status = 3;
		  log.log(Level.INFO, "EBS parser run finished.");
	  }
  }

  /**
   * Parses the given xml-file using the ebs content handler
   */
  public void parse(final SourceFile sf, final String techPack,
      final String setType, final String setName) throws Exception {

    final long start = System.currentTimeMillis();
    this.sourceFile = sf;

    SAXParserFactory spf = SAXParserFactory.newInstance();
    // spf.setValidating(validate);

    SAXParser parser = spf.newSAXParser();
    final XMLReader xmlReader = parser.getXMLReader();

    String fillMissingEbsResultValueWith = sf.getProperty(
        "fillMissingEbsResultValueWith", "0");
    
    //Flag for old handler
    String useOldHandler = sf.getProperty("useOldHandler", "false");
    boolean useInMemHandler = useOldHandler!=null && "true".equals(useOldHandler)? false : true;
    
    //Select correct xml content handler
    if (techPack.contains("INTF_PM_E_EBSS") )
    {
    if(useInMemHandler){
    	
      EBSDocInMemContentHandler ebs = new EBSDocInMemContentHandler(sf, techPack, setType,
          setName, workerName);
      if (fillMissingEbsResultValueWith != null) {
        ebs.setFillMissingResultValueWith(fillMissingEbsResultValueWith);
      }
      xmlReader.setContentHandler(ebs);
      xmlReader.setErrorHandler(ebs);

    }else{
      EBSContentHandler ebs = new EBSContentHandler(sf, techPack, setType,
          setName, workerName);
      if (fillMissingEbsResultValueWith != null) {
        ebs.setFillMissingResultValueWith(fillMissingEbsResultValueWith);
      }
      xmlReader.setContentHandler(ebs);
      xmlReader.setErrorHandler(ebs);

    }
    }
    else
    {
        if(useInMemHandler){
        	
            EBSDocInMemContentHandlerwithmoid ebs = new EBSDocInMemContentHandlerwithmoid(sf, techPack, setType,
                setName, workerName);
            if (fillMissingEbsResultValueWith != null) {
              ebs.setFillMissingResultValueWith(fillMissingEbsResultValueWith);
            }
            xmlReader.setContentHandler(ebs);
            xmlReader.setErrorHandler(ebs);

          }else{
            EBSContentHandlerwithmoid ebs = new EBSContentHandlerwithmoid(sf, techPack, setType,
                setName, workerName);
            if (fillMissingEbsResultValueWith != null) {
              ebs.setFillMissingResultValueWith(fillMissingEbsResultValueWith);
            }
            xmlReader.setContentHandler(ebs);
            xmlReader.setErrorHandler(ebs);

          }
    }

    

    xmlReader.setEntityResolver(new ENIQEntityResolver(log.getName()));
    final long middle = System.currentTimeMillis();
    xmlReader.parse(new InputSource(sourceFile.getFileInputStream()));
    final long end = System.currentTimeMillis();
    log.log(Level.FINER, "Data parsed. Parser initialization took "
        + (middle - start) + " ms, parsing " + (end - middle) + " ms. Total: "
        + (end - start) + " ms.");
  }
  
}
