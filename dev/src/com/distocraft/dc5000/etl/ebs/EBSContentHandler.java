/**
 * This EBS content handler parses the input data, 
 * creates new MeasurementFile/s and adds data into those
 * files.
 *  
 */
package com.distocraft.dc5000.etl.ebs;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.distocraft.dc5000.etl.parser.Main;
import com.distocraft.dc5000.etl.parser.MeasurementFile;
import com.distocraft.dc5000.etl.parser.SourceFile;

/**
 * @author epetrmi
 * 
 */
public class EBSContentHandler extends DefaultHandler {

  // ##TODO## Verify logger name
  private Logger log = null;

  // Virtual machine timezone unlikely changes during execution of JVM
  private static final String JVM_TIMEZONE = 
    (new SimpleDateFormat("Z")).format(new Date());


  private SimpleDateFormat simpleDateFormat = new SimpleDateFormat(
      "yyyy-MM-dd'T'HH:mm:ssZ");

  /*
   * "TAGS" are String constants that represent tag-names
   * in the input-xml-files. They are used when capturing
   * start/endElements during parsing.
   */
  // General info
  public final String TAG_FILE_HEADER = "mfh";
  public final String TAG_FILE_FORMAT_VERSION = "ffv";
  public final String TAG_VENDOR_NAME = "vn";
  public final String TAG_SN = "sn";
  public final String TAG_ST = "st";
  public final String TAG_CBT = "cbt";//##TODO## Is this collectionBeginTime?
  public final String TAG_NEUN = "neun";
  public final String TAG_NEDN = "nedn";
  public final String TAG_MEAS_DATA = "md";
  public final String TAG_TS = "ts";

  // Measurement info/value spesific info
  public final String TAG_MEAS_INFO = "mi";
  public final String TAG_MTS = "mts";
  public final String TAG_JOB_ID = "jobid";
  public final String TAG_GRANULARITY_PERIOD_DURATION = "gp";
  public final String TAG_REP_PERIOD_DURATION = "rp";

  public final String TAG_MEAS_TYPE = "mt";
  public final String TAG_MEAS_VALUE = "mv";
  public final String TAG_MEAS_MOID = "moid";
  public final String TAG_MEAS_RESULT = "r";
  // ..."TAGS"

  /*
   * MeasurementFile mapping "KEYS" are String constants that 
   * are used as keys when parsed data is added to MeasurementFile.
   */ 
  //GENERAL INFO
  public final String KEY_FILE_FORMAT_VERSION = "fileFormatVersion";
  public final String KEY_VENDOR_NAME = "vendorName";
  public final String KEY_COLLECTION_BEGIN_TIME = "collectionBeginTime";
  public final String KEY_SN = TAG_SN;
  public final String KEY_ST = TAG_ST;
  public final String KEY_NEUN = TAG_NEUN;
  public final String KEY_NEDN = TAG_NEDN;
  public final String KEY_TS = "ts";
  //...GENERAL INFO
  
  //MEAS INFO...
  public final String KEY_MTS = "mts";
  public final String KEY_JOB_ID = "jobId";
  public final String KEY_MEAS_INFO_ID = "measInfoId";
  public final String KEY_MOID = "MOID";
  public final String KEY_OBJECT_CLASS = "objectClass";
  //...MEAS INFO
  
  //OTHER INFO...
  public final String KEY_DATETIME_ID = "DATETIME_ID";
  public final String KEY_DC_SUSPECTFLAG = "DC_SUSPECTFLAG";
  public final String KEY_FILENAME = "filename";
  public final String KEY_JVM_TIMEZONE = "JVM_TIMEZONE";
  public final String KEY_DIRNAME = "DIRNAME";
 //...OTHER INFO

  //UNKNOWN INFO...
//  public final String KEY_DN_PREFIX = "dnPrefix";
//  public final String KEY_LOCAL_DN = "localDn";
//  public final String KEY_MANAGED_ELEMENT_LOCAL_DN = "managedElementLocalDn";
//  public final String KEY_ELEMENT_TYPE = "elementType";
//  public final String KEY_USER_LABEL = "userLabel";
//  public final String KEY_SW_VERSION = "swVersion";
//  public final String KEY_END_TIME = "endTime";
  //...UNKNOWN INFO
  
  //..."KEYS"
  

  
  /**
   * charValue contains the textContent part 
   * when the xml is parsed.
   * 
   * (Ex. <sometag>textContent</sometag>)
   * 
   */
  private String charValue;

   
  // Needed when creating a new Measurement file
  private SourceFile sourceFile;
  private String techPack;
  private String setType;
  private String setName;
  private String workerName = "";
  private MeasurementFile measFile = null;

  /*
   * Variables in which the parsed data is 
   * stored temporarily
   */
  //GENERAL DATA
  private String fileFormatVersion;
  private String vendorName;
  private String sn = "sn";
  private String st = "st";
  private String neun = "neun";
  private String nedn = "nedn";
  private String collectionBeginTime;
  private String ts;
  // private String collectionEndTime; //received so late, that migth not be
  // used
  //...GENERAL DATA
  
  //MEAS INFO DATA...
  private String measInfoId;
  private String mts;
  private String jobId;
  private String granularityPeriodDuration;
  private String repPeriodDuration;
  private String granularityPeriodEndTime;//##TODO## Where is this?
  
  /**
   * Stores measurement type indexes and values (MEAS_TYPE) of current MEAS_INFO
   * section. Index matches with measValueMapIndex.
   */
  private Map<String, String> measNameMap;

  /**
   * Stores measurement indexes and values (MEAS_RESULT) of current MEAS_VALUE
   * section. Index matches with measNameMapIndex.
   */
  private Map<String, String> measValueMap;
  
  private String suspectFlag = "";//##TODO## What is this?
  private String measIndex;
  private String measValueIndex;
  private String measObjLdn;//MOID?
  private String objectClass;
  private String oldObjClass;
  private String objectMask;
  private String readVendorIDFrom;
  private boolean fillEmptyMoid = true;
  private String fillEmptyMoidStyle = "";
  private String fillEmptyMoidValue = "";
  private String fillMissingResultValueWith ;

  //...MEAS INFO DATA (or related)
  
  
  //UNKNOWN INFO...
  //##TODO## These were some parameters that existed in old 3GPP-parser :)
//  private String dnPrefix;
//  private String fsLocalDN;
//  private String elementType;
//  private String meLocalDN;
//  private String userLabel;
//  private String swVersion;

  // private int status = 0;
  // private Main mainParserObject = null;
  // final private List errorList = new ArrayList();
  //UNKNOWN INFO
  
  
  // ####################
  // Constructors
  // ####################

  public EBSContentHandler() {
    super();
    log = Logger.getLogger("etl." + techPack + "." + setType + "." + setName
            + ".parser.EBSParser" + workerName);
  }

  public EBSContentHandler(final SourceFile sf, final String techPack,
      final String setType, final String setName, final String workerName) {
    this();
    
    this.setSourceFile(sf);
    this.techPack = techPack;
    this.setType = setType;
    this.setName = setName;
    this.workerName = workerName;

    // ##TODO## Find out a better way to give these params
    objectMask = sf.getProperty("x3GPPParser.vendorIDMask", ".+,(.+)=.+");
    readVendorIDFrom = sf.getProperty("EBSParser.readVendorIDFrom", "measInfoId");
    fillEmptyMoid = "true".equalsIgnoreCase(sf.getProperty(
        "x3GPPParser.FillEmptyMOID", "true"));
    fillEmptyMoidStyle = sf
        .getProperty("x3GPPParser.FillEmptyMOIDStyle", "inc");
    fillEmptyMoidValue = sf.getProperty("x3GPPParser.FillEmptyMOIDValue", "0");
  }

  // ####################
  // SAX Event handlers
  // ####################

  public void startDocument() {  }

  public void endDocument() throws SAXException {
    // close last meas file
    if (measFile != null) {
      try {
        measFile.close();
      } catch (Exception e) {
        log.log(Level.FINEST, "Worker parser failed to exception", e);
        throw new SAXException("Error closing measurement file");
      }
    }
  }

  public void startElement(final String uri, final String name,
      final String qName, final Attributes atts) throws SAXException {

    charValue = "";

    if (TAG_FILE_HEADER.equals(qName)) {
      // // this.dnPrefix = atts.getValue("dnPrefix");
      // } else if (qName.equals("fileSender")) {
      // // this.fsLocalDN = atts.getValue("localDn");
      // // this.elementType = atts.getValue("elementType");
      // } else if (qName.equals("measCollec")) {
      // if (atts.getValue("beginTime") != null) {
      // // // header
      // // collectionBeginTime = atts.getValue("beginTime");
      // } else if (atts.getValue("endTime") != null) {
      // // footer
      // // collectionEndTime = atts.getValue("endTime");
      // }
      // } else if (MEAS_DATA.equals(qName)) {
      // } else if (qName.equals("managedElement")) {
      // // this.meLocalDN = atts.getValue("localDn");
      // // this.userLabel = atts.getValue("userLabel");
      // // this.swVersion = atts.getValue("swVersion");
      // } else if (qName.equals("measTypes")) {
//    } else if (MEAS_VALUE.equals(qName)) {
//    } else if (qName.equals("measResults")) {
//    } else if (qName.equals("suspect")) {
//    } else if (qName.equals("fileFooter")) {
      
    } else if (TAG_MEAS_INFO.equals(qName)) {
        // ##When matching MEAS_INFO element we create a map that stores
        // the MEAS_TYPE data. MEAS_TYPE data is referenced when the
        // actual MEAS_VALUE data is handled.
        this.measInfoId = atts.getValue("measInfoId");
        log.log(Level.FINEST, "mi measInfoId=" + measInfoId);
        measNameMap = new HashMap<String, String>();
        measValueMap = new HashMap<String, String>();
        handleTAGmoid(measInfoId);
        createNewMeasurementFile();
        
    } else if (TAG_MEAS_TYPE.equals(qName)) {
        measIndex = atts.getValue("p");
        log.log(Level.FINEST, "meastype p=" + measIndex);
   
    } else if (TAG_MEAS_TYPE.equals(qName)) {
      //initialize measValueMap
      measValueMap=new HashMap<String,String>(measNameMap.size());
        
    } else if (TAG_MEAS_RESULT.equals(qName)) {
        this.measValueIndex = atts.getValue("p");
        log.log(Level.FINEST, "meas result p=" + measValueIndex);
    }
  }

  


  public void endElement(final String uri, final String name, final String qName)
      throws SAXException {
    if (TAG_FILE_HEADER.equals(qName)) {
      // if (qName.equals("fileHeader")) { // ##TODO## Probably "mfh"
      // } else if (qName.equals("fileSender")) {
      // } else if (qName.equals("measCollec")) {
      // } else if (MEAS_DATA.equals(qName)) {
      // } else if (qName.equals("managedElement")) {
      // } else if (MEAS_INFO.equals(qName)) {
      // } else if (qName.equals("measTypes")) {
      // measNameMap = strToMap(charValue);
//    } else if (qName.equals("suspect")) {
//      this.suspectFlag = charValue;
//    } else if (qName.equals("fileFooter")) {
//    } else if (qName.equals("measResults")) {
      // final Map measValues = strToMap(charValue);
      // if (measValues.keySet().size() == measNameMap.keySet().size()) {
      // Iterator it = measValues.keySet().iterator();
      // while (it.hasNext()) {
      // String s = (String)it.next();
      // String origValue = (String)measValues.get(s);
      // if (origValue != null && origValue.equalsIgnoreCase("NIL")) {
      // origValue = null;
      // }
      // if (measFile == null) {
      // log.log(Level.FINEST,(String)measNameMap.get(s)+ ": "+
      // origValue);
      // } else {
      // measFile.addData((String)measNameMap.get(s), origValue);
      // log.log(Level.FINEST, (String)measNameMap.get(s)+": "+
      // origValue);
      // }
      // }
      // } else {
      // log.warning("Data contains one or more r-tags than mt-tags");
      // }
      
    
      //
     
      //GENERAL...
    } else if (TAG_SN.equals(qName)) {
      this.sn = charValue;
      log.log(Level.FINEST, "Tag parsed: "+TAG_SN+"="+sn);
      
    } else if (TAG_ST.equals(qName)) {
      this.st = charValue;
      log.log(Level.FINEST, "Tag parsed: "+TAG_ST+"="+st);
      
    } else if (TAG_CBT.equals(qName)) {
      this.collectionBeginTime = charValue;
      log.log(Level.FINEST, "Tag parsed: "+TAG_CBT+"="+collectionBeginTime);
      
    } else if (TAG_NEUN.equals(qName)) {
      this.neun = charValue;
      log.log(Level.FINEST, "Tag parsed: "+TAG_NEUN+"="+neun);
      
    } else if (TAG_NEDN.equals(qName)) {
      this.nedn = charValue;
      log.log(Level.FINEST, "Tag parsed: "+TAG_NEDN+"="+nedn);
      
    } else if (TAG_FILE_FORMAT_VERSION.equals(qName)) {
      this.fileFormatVersion = charValue;
      log.log(Level.FINEST, "Tag parsed: "+TAG_FILE_FORMAT_VERSION+"="+fileFormatVersion);
      
    } else if (TAG_VENDOR_NAME.equals(qName)) {
      this.vendorName = charValue;
      log.log(Level.FINEST, "Tag parsed: "+TAG_VENDOR_NAME+"="+vendorName);
      
    } else if (TAG_MTS.equals(qName)) {
      this.mts = charValue;
      log.log(Level.FINEST, "Tag parsed: "+TAG_MTS+"="+mts);
      
    } else if (TAG_TS.equals(qName)) {
      this.ts = charValue;  
      log.log(Level.FINEST, "Tag parsed: "+TAG_TS+"="+ts);
      log.log(Level.WARNING, 
          "NOTICE: This ts-value is not saved currently because of the way sax parser works." +
          "To get this value into measFile we have to " +
          "1) load the whole xml into memory, " +
          "2) change the location of tag in xml file (at least before mv-element) " +
          "3) parse the xml document twice");
      //...GENERAL
      
      
    } else if (TAG_JOB_ID.equals(qName)) {
      this.jobId = charValue;
      log.log(Level.FINEST, "Tag parsed: "+TAG_JOB_ID+"="+jobId);

    } else if (TAG_GRANULARITY_PERIOD_DURATION.equals(qName)) {
      this.granularityPeriodDuration = charValue;
      log.log(Level.FINEST, "Tag parsed: "+TAG_GRANULARITY_PERIOD_DURATION+"="+granularityPeriodDuration);

    } else if (TAG_REP_PERIOD_DURATION.equals(qName)) {
      this.repPeriodDuration = charValue;
      log.log(Level.FINEST, "Tag parsed: "+TAG_REP_PERIOD_DURATION+"="+repPeriodDuration);

    } else if (TAG_MEAS_MOID.equals(qName)) {
      this.measObjLdn = charValue;
      log.log(Level.FINEST, "Tag parsed: "+TAG_MEAS_MOID+"="+measObjLdn);
//      this.suspectFlag = "";
//      handleTAGmoid(measObjLdn);
//     createNewMeasurementFile();
      
    } else if (TAG_MEAS_TYPE.equals(qName)) {
      measNameMap.put(measIndex, charValue);
      log.log(Level.FINEST, "measNameMap.put( " + measIndex + "," + charValue
          + " )");

    } else if (TAG_MEAS_VALUE.equals(qName)) {
      try {
        // ##In the end of mv-section we combine the counters and
        // the actual data and add it to measurementFile
        // ##Traverse through the counter keys
        
        final String strNullValue = fillMissingResultValueWith==null ? "null" : fillMissingResultValueWith;
        for (String measNameIndexKey : measNameMap.keySet()) {
          // Maps share the same key (index)
          String addedValue = measValueMap.get(measNameIndexKey);
          if (addedValue == null) {
            addedValue = strNullValue;
            log.log(Level.FINEST, "Non-existing result (measIndex,measName)"
                + "=(" + measNameIndexKey + ","
                + measNameMap.get(measNameIndexKey)
                + ") was replaced by given nullValue=" + strNullValue);
          }

          // ##Data is added here
          measFile.addData(measNameMap.get(measNameIndexKey), addedValue);
          log.log(Level.FINEST, "measFile.addData( "
              + measNameMap.get(measNameIndexKey) + ", " + addedValue + " )");
        }// ..End-of-keySet-FOR

        // change file when object class changes
        if (measFile == null) {
          log.log(Level.SEVERE, "Measurement file is null. Data is NOT stored!");
          
          //GENERAL INFO...
          log.log(Level.FINEST, "vendorName: " + vendorName);
          log.log(Level.FINEST, "fileFormatVersion: " + fileFormatVersion);
          log.log(Level.FINEST, "collectionBeginTime: " + collectionBeginTime);
          log.log(Level.FINEST, KEY_SN+": "+sn );
          log.log(Level.FINEST, KEY_ST+": "+st );
          log.log(Level.FINEST, KEY_NEUN+": "+neun );
          log.log(Level.FINEST, KEY_NEDN+": "+nedn);
          log.log(Level.FINEST, KEY_TS+": "+ts);
          //..GENERAL INFO
          
          //MEAS INFO...
          log.log(Level.FINEST, "mts: " + mts);
          log.log(Level.FINEST, "jobId: " + jobId);
          log
              .log(Level.FINEST, "PERIOD_DURATION: "
                  + granularityPeriodDuration);
          log.log(Level.FINEST, "repPeriodDuration: " + repPeriodDuration);
          log.log(Level.FINEST, "measInfoId: " + measInfoId);
          log.log(Level.FINEST, "MOID: " + measObjLdn);
          log.log(Level.FINEST, "objectClass: " + objectClass);
          //...MEAS INFO
          
          //OTHER INFO...
          // DATETIME_ID calculated from end time

          //String begin = calculateBegintime();
          //if (begin != null) {
            log.log(Level.FINEST, "DATETIME_ID: " + collectionBeginTime);
          //}

          log.log(Level.FINEST, "DC_SUSPECTFLAG: " + suspectFlag);
          log.log(Level.FINEST, "filename: "
              + (sourceFile == null ? "dummyfile" : sourceFile.getName()));
          log.log(Level.FINEST, "JVM_TIMEZONE: " + JVM_TIMEZONE);
          log.log(Level.FINEST, "DIRNAME: "
              + (sourceFile == null ? "dummydir" : sourceFile.getDir()));
          //...OTHER INFO
          
//          //UNKNOWN INFO...
//          log.log(Level.FINEST, "dnPrefix: " + dnPrefix);
//          log.log(Level.FINEST, "localDn: " + fsLocalDN);
//          log.log(Level.FINEST, "managedElementLocalDn: " + meLocalDN);
//          log.log(Level.FINEST, "elementType: " + elementType);
//          log.log(Level.FINEST, "userLabel: " + userLabel);
//          log.log(Level.FINEST, "swVersion: " + swVersion);
//          // collectionEndTime received so late, that migth not be
//          // used
//          log.log(Level.FINEST, "endTime: " + granularityPeriodEndTime);
//          //...UNKNOWN INFO
 
        } else {
          
          //GENERAL INFO...
          measFile.addData(KEY_VENDOR_NAME, vendorName);
          log.log(Level.FINEST, "Added to measFile: "+"vendorName: " + vendorName);
          measFile.addData(KEY_FILE_FORMAT_VERSION, fileFormatVersion);
          log.log(Level.FINEST, "Added to measFile: "+"fileFormatVersion: " + fileFormatVersion);
          measFile.addData(KEY_COLLECTION_BEGIN_TIME, collectionBeginTime);
          log.log(Level.FINEST, "Added to measFile: "+"collectionBeginTime: " + collectionBeginTime);
          measFile.addData(KEY_SN, sn );
          log.log(Level.FINEST, "Added to measFile: "+KEY_SN+": "+sn );
          measFile.addData(KEY_ST, st );
          log.log(Level.FINEST, "Added to measFile: "+KEY_ST+": "+st );
          measFile.addData(KEY_NEUN, neun );
          log.log(Level.FINEST, "Added to measFile: "+KEY_NEUN+": "+neun );
          measFile.addData(KEY_NEDN, nedn );
          log.log(Level.FINEST, "Added to measFile: "+KEY_NEDN+": "+nedn);
          measFile.addData(KEY_TS, ts );
          log.log(Level.FINEST, "Added to measFile: "+KEY_TS+": "+ts);
          //..GENERAL INFO
                    
          //MEAS INFO...
          measFile.addData(KEY_MTS, mts);
          log.log(Level.FINEST, "Added to measFile: "+"mts: " + mts);
          measFile.addData(KEY_JOB_ID, jobId);
          log.log(Level.FINEST, "Added to measFile: "+"jobId: " + jobId);
          measFile.addData("PERIOD_DURATION", granularityPeriodDuration);
          log
              .log(Level.FINEST, "Added to measFile: "+"PERIOD_DURATION: "
                  + granularityPeriodDuration);
          measFile.addData("repPeriodDuration", repPeriodDuration);
          log.log(Level.FINEST, "Added to measFile: "+"repPeriodDuration: " + repPeriodDuration);
          measFile.addData(KEY_MEAS_INFO_ID, measInfoId);
          log.log(Level.FINEST, "Added to measFile: "+"measInfoId: " + measInfoId);
          measFile.addData(KEY_MOID, measObjLdn);
          log.log(Level.FINEST, "Added to measFile: "+"MOID: " + measObjLdn);
          measFile.addData(KEY_OBJECT_CLASS, objectClass);
          log.log(Level.FINEST, "Added to measFile: "+"objectClass: " + objectClass);
          //...MEAS INFO
          
          //OTHER INFO...
          // DATETIME_ID calculated from end time
          //String begin = calculateBegintime();
          //if (begin != null) {
            measFile.addData(KEY_DATETIME_ID, collectionBeginTime);
            log.log(Level.FINEST, "Added to measFile: "+"DATETIME_ID: " + collectionBeginTime);
          //}
          measFile.addData(KEY_DC_SUSPECTFLAG, suspectFlag);
          log.log(Level.FINEST, "Added to measFile: "+"DC_SUSPECTFLAG: " + suspectFlag);
          measFile.addData(KEY_FILENAME, (sourceFile == null ? "dummyfile"
              : sourceFile.getName()));
          log.log(Level.FINEST, "Added to measFile: "+"filename: "
              + (sourceFile == null ? "dummyfile" : sourceFile.getName()));
          measFile.addData(KEY_JVM_TIMEZONE, JVM_TIMEZONE);
          log.log(Level.FINEST, "Added to measFile: "+"JVM_TIMEZONE: " + JVM_TIMEZONE);
          measFile.addData(KEY_DIRNAME, (sourceFile == null ? "dummydir"
              : sourceFile.getDir()));
          log.log(Level.FINEST, "Added to measFile: "+"DIRNAME: "
              + (sourceFile == null ? "dummydir" : sourceFile.getDir()));
          //...OTHER INFO
          
//          //UNKNOWN INFO...
//          measFile.addData(KEY_DN_PREFIX, dnPrefix);
//          log.log(Level.FINEST, "dnPrefix: " + dnPrefix);
//          measFile.addData(KEY_LOCAL_DN, fsLocalDN);
//          log.log(Level.FINEST, "localDn: " + fsLocalDN);
//          measFile.addData(KEY_MANAGED_ELEMENT_LOCAL_DN, meLocalDN);
//          log.log(Level.FINEST, "managedElementLocalDn: " + meLocalDN);
//          measFile.addData(KEY_ELEMENT_TYPE, elementType);
//          log.log(Level.FINEST, "elementType: " + elementType);
//          measFile.addData(KEY_USER_LABEL, userLabel);
//          log.log(Level.FINEST, "userLabel: " + userLabel);
//          measFile.addData(KEY_SW_VERSION, swVersion);
//          log.log(Level.FINEST, "swVersion: " + swVersion);
//          // collectionEndTime received so late, that migth not be
//          // used
//          measFile.addData(KEY_END_TIME, granularityPeriodEndTime);
//          log.log(Level.FINEST, "endTime: " + granularityPeriodEndTime);
//          //...UNKNOWN INFO

          measFile.saveData();
     
        }

      } catch (Exception e) {
        log.log(Level.FINEST, "Error saving measurement data", e);
        e.printStackTrace();
        throw new SAXException("Error saving measurement data: "
            + e.getMessage(), e);
      }
   
    } else if (TAG_MEAS_RESULT.equals(qName)) {
        // ##Get the actual value (text content)
        String origValue = charValue;
        
        //##TODO## Is this a valid check!?
        if (origValue != null && origValue.equalsIgnoreCase("NIL")) {
          origValue = null;//##TODO## Should this be put as "null" -string instead?
        }
        // ##Add value to measurement value map
        measValueMap.put(measValueIndex, origValue);
    }
  }

  
  /* 
   * Handles the textContent -part in xml
   * 
   * (non-Javadoc)
   * @see org.xml.sax.helpers.DefaultHandler#characters(char[], int, int)
   */
  public void characters(char[] ch, int start, int length) throws SAXException {
    if(charValue==null) {
      //NOTE: null+=ch[1] (where ch[1]='2') -> "null2" -String
      charValue="";
    }
    for (int i = start; i < start + length; i++) {
      // If no control char
      if (ch[i] != '\\' && ch[i] != '\n' && ch[i] != '\r' && ch[i] != '\t') {
        charValue += ch[i];
      }
    }
    log.log(Level.FINEST, "Current charValue="+charValue);
  }

  // ##TODO## If this is needed, try to add it in a helper class + clean
  private String calculateBegintime() {
    
    String result = null;
    
    //##Quick-check pre-conditions
    //  Now just trying to avoid NPEs
    boolean preconditionsAreOk = false;
    if(collectionBeginTime==null){
      log.log(Level.WARNING,"pre-condition: collectionBeginTime is missing");
    } else if(granularityPeriodDuration==null){
      log.log(Level.WARNING,"pre-condition: collectionBeginTime is missing");
    }else{
      preconditionsAreOk = true;
    }
    
    //Try to calculate begin time
    if(preconditionsAreOk){
    
    try {
      String granPeriodETime = collectionBeginTime;
      if (granPeriodETime.matches(".+\\+\\d\\d(:)\\d\\d")) {
        granPeriodETime = collectionBeginTime.substring(0,
        		collectionBeginTime.lastIndexOf(":"))
            + collectionBeginTime.substring(collectionBeginTime
                .lastIndexOf(":") + 1);
      }
      granPeriodETime = granPeriodETime.replaceAll("[.]\\d{3}", "");
      Date end = simpleDateFormat.parse(granPeriodETime);
      Calendar cal = Calendar.getInstance();
      cal.setTime(end);
      int period = Integer.parseInt(granularityPeriodDuration);
      cal.add(Calendar.SECOND, -period);
      result = simpleDateFormat.format(cal.getTime());
    } catch (ParseException e) {
      log.log(Level.WARNING, "Worker parser failed to exception", e);
    } catch (NumberFormatException e) {
      log.log(Level.WARNING, "Worker parser failed to exception", e);
    } catch (NullPointerException e) {
      log.log(Level.WARNING, "Worker parser failed to exception", e);
    }
    }else{
      //##Failed to calculate begin time
      log.log(Level.WARNING, "Begintime could not be calculated, " +
      		"because pre-conditions were not set correctly!");
    }
    
    return result;
  }
  
  private void handleTAGmoid(String value) {
	    // TypeClassID is determined from the moid
	    // of the first mv of the md

	    this.objectClass = "";

	    // where to read objectClass (moid)
	    if ("file".equalsIgnoreCase(readVendorIDFrom)) {
	      // read vendor id from file
	      objectClass = parseFileName(sourceFile.getName(), objectMask);

	    } else if ("data".equalsIgnoreCase(readVendorIDFrom)) {

	      // if moid is empty and empty moids are filled.
	      if (fillEmptyMoid && value.length() <= 0) {
	        if (fillEmptyMoidStyle.equalsIgnoreCase("static")) {
	          value = fillEmptyMoidValue;
	        } else {
	          value = measValueIndex + "";
	        }
	      }

	      // read vendor id from data
	      objectClass = parseFileName(value, objectMask);
	    } else if ("measInfoId".equalsIgnoreCase(readVendorIDFrom)){
	        objectClass = this.measInfoId;
	      }
	  }
  /**
   * Extracts a substring from given string based on given regExp
   * 
   */
  public String parseFileName(final String str, final String regExp) {

    final Pattern pattern = Pattern.compile(regExp);
    final Matcher matcher = pattern.matcher(str);

    if (matcher.matches()) {
      final String result = matcher.group(1);
      log.finest(" regExp (" + regExp + ") found from " + str + "  :" + result);
      return result;
    } else {
      log.warning("String " + str + " doesn't match defined regExp " + regExp);
    }

    return "";

  }

  /**
   * @throws SAXException
   */
  private void createNewMeasurementFile() throws SAXException {
    try {
      if (sourceFile != null) {
        if (oldObjClass == null || !oldObjClass.equals(objectClass)) {
          // close meas file
          if (measFile != null) {
            measFile.close();
          }
          // create new measurementFile
          measFile = Main.createMeasurementFile(sourceFile, objectClass,
              techPack, setType, setName, workerName, log);
          oldObjClass = objectClass;
        }
      }
    } catch (Exception e) {
      log.log(Level.FINEST, "Error opening measurement data", e);
      e.printStackTrace();
      throw new SAXException("Error opening measurement data: "
          + e.getMessage(), e);
    }
  }
  
  
  // ####################
  // GETTERS AND SETTERS
  // ####################

  public MeasurementFile getMeasFile() {
    return measFile;
  }

  public void setMeasFile(MeasurementFile measFile) {
    this.measFile = measFile;
  }

  public Logger getLog() {
    return log;
  }

  public void setLog(Logger log) {
    this.log = log;
  }

  public SimpleDateFormat getSimpleDateFormat() {
    return simpleDateFormat;
  }

  public void setSimpleDateFormat(SimpleDateFormat simpleDateFormat) {
    this.simpleDateFormat = simpleDateFormat;
  }

  public Map<String, String> getMeasNameMap() {
    return measNameMap;
  }

  public void setMeasNameMap(Map<String, String> measNameMap) {
    this.measNameMap = measNameMap;
  }

  public Map<String, String> getMeasValueMap() {
    return measValueMap;
  }

  public void setMeasValueMap(Map<String, String> measValueMap) {
    this.measValueMap = measValueMap;
  }

  public String getFileFormatVersion() {
    return fileFormatVersion;
  }

  public void setFileFormatVersion(String fileFormatVersion) {
    this.fileFormatVersion = fileFormatVersion;
  }

  public String getVendorName() {
    return vendorName;
  }

  public void setVendorName(String vendorName) {
    this.vendorName = vendorName;
  }

  public String getCollectionBeginTime() {
    return collectionBeginTime;
  }

  public void setCollectionBeginTime(String collectionBeginTime) {
    this.collectionBeginTime = collectionBeginTime;
  }

  public String getGranularityPeriodDuration() {
    return granularityPeriodDuration;
  }

  public void setGranularityPeriodDuration(String granularityPeriodDuration) {
    this.granularityPeriodDuration = granularityPeriodDuration;
  }

  public String getGranularityPeriodEndTime() {
    return granularityPeriodEndTime;
  }

  public void setGranularityPeriodEndTime(String granularityPeriodEndTime) {
    this.granularityPeriodEndTime = granularityPeriodEndTime;
  }

  public String getRepPeriodDuration() {
    return repPeriodDuration;
  }

  public void setRepPeriodDuration(String repPeriodDuration) {
    this.repPeriodDuration = repPeriodDuration;
  }

  public String getJobId() {
    return jobId;
  }

  public void setJobId(String jobId) {
    this.jobId = jobId;
  }

  public String getMeasInfoId() {
    return measInfoId;
  }

  public void setMeasInfoId(String measInfoId) {
    this.measInfoId = measInfoId;
  }

  public String getSuspectFlag() {
    return suspectFlag;
  }

  public void setSuspectFlag(String suspectFlag) {
    this.suspectFlag = suspectFlag;
  }

  public String getMeasIndex() {
    return measIndex;
  }

  public void setMeasIndex(String measIndex) {
    this.measIndex = measIndex;
  }

  public String getMeasValueIndex() {
    return measValueIndex;
  }

  public void setMeasValueIndex(String measValueIndex) {
    this.measValueIndex = measValueIndex;
  }

  public String getMeasObjLdn() {
    return measObjLdn;
  }

  public void setMeasObjLdn(String measObjLdn) {
    this.measObjLdn = measObjLdn;
  }

  public String getCharValue() {
    return charValue;
  }

  public void setCharValue(String charValue) {
    this.charValue = charValue;
  }

  public SourceFile getSourceFile() {
    return sourceFile;
  }

  public void setSourceFile(SourceFile sourceFile) {
    this.sourceFile = sourceFile;
  }

  public String getObjectClass() {
    return objectClass;
  }

  public void setObjectClass(String objectClass) {
    this.objectClass = objectClass;
  }

  public String getOldObjClass() {
    return oldObjClass;
  }

  public void setOldObjClass(String oldObjClass) {
    this.oldObjClass = oldObjClass;
  }

  public static String getJVM_TIMEZONE() {
    return JVM_TIMEZONE;
  }

  public String getJOB_ID() {
    return TAG_JOB_ID;
  }

  public void setMts(String mts) {
    this.mts = mts;
  }

  public String getMts() {
    return mts;
  }

  public String getSn() {
    return sn;
  }

  public void setSn(String sn) {
    this.sn = sn;
  }

  public String getSt() {
    return st;
  }

  public void setSt(String st) {
    this.st = st;
  }

  public String getNeun() {
    return neun;
  }

  public void setNeun(String neun) {
    this.neun = neun;
  }

  public String getNedn() {
    return nedn;
  }

  public void setNedn(String nedn) {
    this.nedn = nedn;
  }

  public void setTs(String ts) {
    this.ts = ts;
  }

  public String getTs() {
    return ts;
  }

  public void setFillMissingResultValueWith(String fillMissingEbsResultValueWith) {
    this.fillMissingResultValueWith = fillMissingEbsResultValueWith;
  }

  public String getFillMissingResultValueWith() {
    return this.fillMissingResultValueWith;
  }
  
  
}
